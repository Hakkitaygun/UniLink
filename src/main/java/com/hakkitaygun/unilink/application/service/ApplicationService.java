package com.hakkitaygun.unilink.application.service;

import com.hakkitaygun.unilink.application.dto.ApplicationResponse;
import com.hakkitaygun.unilink.application.dto.ApplyRequest;
import com.hakkitaygun.unilink.application.dto.IncomingApplicationResponse;
import com.hakkitaygun.unilink.application.dto.PendingCountResponse;
import com.hakkitaygun.unilink.application.entity.Application;
import com.hakkitaygun.unilink.application.enums.ApplicationStatus;
import com.hakkitaygun.unilink.application.exception.*;
import com.hakkitaygun.unilink.application.repository.ApplicationRepository;
import com.hakkitaygun.unilink.common.dto.PageResponse;
import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.entity.PostingRole;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import com.hakkitaygun.unilink.posting.exception.PostingAccessDeniedException;
import com.hakkitaygun.unilink.posting.exception.PostingNotFoundException;
import com.hakkitaygun.unilink.posting.repository.PostingRepository;
import com.hakkitaygun.unilink.posting.repository.PostingRoleRepository;
import com.hakkitaygun.unilink.user.entity.Profile;
import com.hakkitaygun.unilink.user.exception.ProfileNotFoundException;
import com.hakkitaygun.unilink.user.repository.ProfileRepository;
import com.hakkitaygun.unilink.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final PostingRepository postingRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PostingRoleRepository postingRoleRepository;

    @Transactional
    public ApplicationResponse apply(UUID postingId, UUID applicantId, ApplyRequest request) {
        Posting posting = postingRepository.findWithDetailsById(postingId)
                .orElseThrow(PostingNotFoundException::new);

        if (posting.getStatus() == PostingStatus.CANCELLED) {
            throw new PostingNotFoundException();
        }
        if (posting.getOwner().getId().equals(applicantId)) {
            throw new InvalidApplicationException("Kendi ilanınıza başvuramazsınız.");
        }
        if (!posting.isOpen(Instant.now())) {
            throw new InvalidApplicationException("Bu ilan artık başvuru kabul etmiyor.");
        }

        Set<PostingRole> roles = posting.getRoles().stream()
                .filter(role -> request.roleIds().contains(role.getId()))
                .collect(Collectors.toSet());
        if (roles.size() != request.roleIds().size()) {
            throw new InvalidApplicationException("Seçilen roller bu ilana ait değil.");
        }

        Optional<Application> existing = applicationRepository
                .findByPostingIdAndApplicantId(postingId, applicantId);
        if (existing.isPresent()) {
            Application application = existing.get();
            if (application.getStatus() != ApplicationStatus.WITHDRAWN) {
                throw new ApplicationAlreadyExistsException(application.getStatus());
            }
            application.reapply(request.message(), roles);
            return ApplicationResponse.from(application);
        }

        Application application = new Application();
        application.setPosting(posting);
        application.setApplicant(userRepository.getReferenceById(applicantId));
        application.setMessage(request.message());
        application.getAppliedRoles().addAll(roles);

        applicationRepository.saveAndFlush(application);
        return ApplicationResponse.from(application);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> getMine(UUID applicantId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by("id")));

        Page<Application> applications = applicationRepository.findByApplicantId(applicantId, pageable);

        List<ApplicationResponse> content = applications.getContent().stream()
                .map(ApplicationResponse::from)
                .toList();
        return PageResponse.of(applications, content);
    }

    @Transactional(readOnly = true)
    public PageResponse<IncomingApplicationResponse> getForPosting(UUID postingId, UUID currentUserId,
                                                                   ApplicationStatus status,
                                                                   int page, int size) {
        Posting posting = postingRepository.findById(postingId)
                .orElseThrow(PostingNotFoundException::new);

        if (!posting.getOwner().getId().equals(currentUserId)) {
            if (posting.getStatus() == PostingStatus.CANCELLED) {
                throw new PostingNotFoundException();
            }
            throw new PostingAccessDeniedException();
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by("id")));

        Page<Application> applications = status == null
                ? applicationRepository.findByPostingId(postingId, pageable)
                : applicationRepository.findByPostingIdAndStatus(postingId, status, pageable);

        Set<UUID> applicantIds = applications.getContent().stream()
                .map(application -> application.getApplicant().getId())
                .collect(Collectors.toSet());
        Map<UUID, Profile> profiles = profileRepository.findAllByUserIdIn(applicantIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, Function.identity()));

        List<IncomingApplicationResponse> content = applications.getContent().stream()
                .map(application -> IncomingApplicationResponse.from(
                        application, profiles.get(application.getApplicant().getId())))
                .toList();

        return PageResponse.of(applications, content);
    }

    @Transactional(readOnly = true)
    public PendingCountResponse countPending(UUID ownerId) {
        long count = applicationRepository.countByOwnerAndStatus(
                ownerId, ApplicationStatus.PENDING, PostingStatus.CANCELLED);
        return new PendingCountResponse(count);
    }

    @Transactional
    public IncomingApplicationResponse accept(UUID applicationId, UUID currentUserId, UUID roleId) {
        Application application = loadForDecision(applicationId, currentUserId);

        boolean applied = application.getAppliedRoles().stream()
                .anyMatch(role -> role.getId().equals(roleId));
        if (!applied) {
            throw new InvalidApplicationException("Seçilen rol, başvuranın başvurduğu roller arasında değil.");
        }

        PostingRole role = postingRoleRepository.findByIdForUpdate(roleId)
                .orElseThrow(() -> new InvalidApplicationException("Rol bulunamadı."));

        long acceptedCount = applicationRepository
                .countByAcceptedRoleIdAndStatus(roleId, ApplicationStatus.ACCEPTED);
        if (acceptedCount >= role.getRequiredCount()) {
            throw new RoleQuotaFullException();
        }

        application.accept(role);
        return toIncoming(application);
    }

    @Transactional
    public IncomingApplicationResponse reject(UUID applicationId, UUID currentUserId) {
        Application application = loadForDecision(applicationId, currentUserId);
        application.reject();
        return toIncoming(application);
    }

    @Transactional
    public ApplicationResponse withdraw(UUID applicationId, UUID currentUserId) {
        Application application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);

        if (!application.getApplicant().getId().equals(currentUserId)) {
            throw new ApplicationNotFoundException();
        }

        ApplicationStatus status = application.getStatus();
        if (status != ApplicationStatus.PENDING && status != ApplicationStatus.ACCEPTED) {
            throw new ApplicationStateException("Bu başvuru geri çekilemez.");
        }

        application.withdraw();
        return ApplicationResponse.from(application);
    }

    private Application loadForDecision(UUID applicationId, UUID currentUserId) {
        Application application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(ApplicationNotFoundException::new);

        Posting posting = application.getPosting();
        if (!posting.getOwner().getId().equals(currentUserId)) {
            throw new ApplicationNotFoundException();
        }
        if (posting.getStatus() == PostingStatus.CANCELLED) {
            throw new InvalidApplicationException("İptal edilmiş ilandaki başvurular sonuçlandırılamaz.");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ApplicationStateException();
        }
        return application;
    }

    private IncomingApplicationResponse toIncoming(Application application) {
        Profile profile = profileRepository
                .findWithTechnologiesByUserId(application.getApplicant().getId())
                .orElseThrow(ProfileNotFoundException::new);
        return IncomingApplicationResponse.from(application, profile);
    }
}