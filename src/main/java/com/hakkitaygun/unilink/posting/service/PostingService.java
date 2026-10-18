package com.hakkitaygun.unilink.posting.service;

import com.hakkitaygun.unilink.common.dto.PageResponse;
import com.hakkitaygun.unilink.posting.dto.*;
import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.entity.PostingRole;
import com.hakkitaygun.unilink.posting.enums.PostingSort;
import com.hakkitaygun.unilink.posting.enums.PostingStage;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import com.hakkitaygun.unilink.posting.enums.PostingType;
import com.hakkitaygun.unilink.posting.exception.InvalidPostingException;
import com.hakkitaygun.unilink.posting.exception.PostingAccessDeniedException;
import com.hakkitaygun.unilink.posting.exception.PostingNotFoundException;
import com.hakkitaygun.unilink.posting.repository.PostingRepository;
import com.hakkitaygun.unilink.posting.specification.PostingSpecifications;
import com.hakkitaygun.unilink.technology.service.TechnologyService;
import com.hakkitaygun.unilink.user.entity.Profile;
import com.hakkitaygun.unilink.user.exception.ProfileNotFoundException;
import com.hakkitaygun.unilink.user.repository.ProfileRepository;
import com.hakkitaygun.unilink.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostingService {

    private final PostingRepository postingRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final TechnologyService technologyService;

    @Transactional
    public PostingResponse create(UUID ownerId, CreatePostingRequest request) {
        validate(request);

        Profile ownerProfile = profileRepository.findById(ownerId)
                .orElseThrow(ProfileNotFoundException::new);


        Posting posting = new Posting();
        posting.setOwner(userRepository.getReferenceById(ownerId));
        posting.setType(request.type());
        posting.setTitle(request.title());
        posting.setDescription(request.description());
        posting.setOrganizationName(request.organizationName());
        posting.setStage(request.stage());
        posting.setBenefitsNote(request.benefitsNote());
        posting.setDeadline(request.deadline());
        posting.getTechnologies().addAll(technologyService.resolveOrCreate(request.technologies()));

        for (PostingRoleRequest roleRequest : request.roles()) {
            PostingRole role = new PostingRole();
            role.setRole(roleRequest.role());
            role.setRequiredCount(roleRequest.requiredCount());
            posting.addRole(role);
        }

        postingRepository.save(posting);
        return PostingResponse.from(posting, ownerProfile, Map.of());
    }

    @Transactional(readOnly = true)
    public PostingResponse getById(UUID postingId, UUID currentUserId) {
        Posting posting = postingRepository.findWithDetailsById(postingId)
                .orElseThrow(PostingNotFoundException::new);

        UUID ownerId = posting.getOwner().getId();
        if (posting.getStatus() == PostingStatus.CANCELLED && !ownerId.equals(currentUserId)) {
            throw new PostingNotFoundException();
        }

        Profile ownerProfile = profileRepository.findById(ownerId)
                .orElseThrow(ProfileNotFoundException::new);

        return PostingResponse.from(posting, ownerProfile, Map.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<PostingSummaryResponse> getFeed(int page, int size, PostingSort sort,
                                                        PostingFilter filter) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Sort order = switch (sort) {
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case DEADLINE -> Sort.by(Sort.Direction.ASC, "deadline");
        };
        Pageable pageable = PageRequest.of(safePage, safeSize, order.and(Sort.by("id")));

        Specification<Posting> spec = PostingSpecifications.isOpen(Instant.now())
                .and(PostingSpecifications.hasType(filter.type()))
                .and(PostingSpecifications.matchesQuery(filter.query()))
                .and(PostingSpecifications.hasAnyTechnology(filter.technologies()))
                .and(PostingSpecifications.hasRole(filter.role()))
                .and(PostingSpecifications.ownerDepartmentContains(filter.department()))
                .and(PostingSpecifications.ownerUniversityContains(filter.university()));

        Page<Posting> postings = postingRepository.findAll(spec, pageable);

        Set<UUID> ownerIds = postings.getContent().stream()
                .map(posting -> posting.getOwner().getId())
                .collect(Collectors.toSet());
        Map<UUID, Profile> profiles = profileRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, Function.identity()));

        List<PostingSummaryResponse> content = postings.getContent().stream()
                .map(posting -> PostingSummaryResponse.from(
                        posting, profiles.get(posting.getOwner().getId()), Map.of()))
                .toList();

        return PageResponse.of(postings, content);
    }

    @Transactional
    public PostingResponse update(UUID postingId, UUID currentUserId, UpdatePostingRequest request) {
        Posting posting = postingRepository.findWithDetailsById(postingId)
                .orElseThrow(PostingNotFoundException::new);

        UUID ownerId = posting.getOwner().getId();
        if (!ownerId.equals(currentUserId)) {
            if (posting.getStatus() == PostingStatus.CANCELLED) {
                throw new PostingNotFoundException();
            }
            throw new PostingAccessDeniedException();
        }
        if (posting.getStatus() == PostingStatus.CANCELLED) {
            throw new InvalidPostingException("İptal edilmiş ilan düzenlenemez.");
        }

        validateStageAndDeadline(posting.getType(), request.stage(), request.deadline());

        posting.setTitle(request.title());
        posting.setDescription(request.description());
        posting.setOrganizationName(request.organizationName());
        posting.setStage(request.stage());
        posting.setBenefitsNote(request.benefitsNote());
        posting.setDeadline(request.deadline());

        posting.getTechnologies().clear();
        posting.getTechnologies().addAll(technologyService.resolveOrCreate(request.technologies()));

        if (posting.getStatus() == PostingStatus.CLOSED) {
            posting.setStatus(PostingStatus.OPEN);
        }

        Profile ownerProfile = profileRepository.findById(ownerId)
                .orElseThrow(ProfileNotFoundException::new);
        return PostingResponse.from(posting, ownerProfile, Map.of());
    }

    @Transactional
    public void cancel(UUID postingId, UUID currentUserId) {
        Posting posting = postingRepository.findById(postingId)
                .orElseThrow(PostingNotFoundException::new);

        UUID ownerId = posting.getOwner().getId();
        if (!ownerId.equals(currentUserId)) {
            if (posting.getStatus() == PostingStatus.CANCELLED) {
                throw new PostingNotFoundException();
            }
            throw new PostingAccessDeniedException();
        }

        posting.setStatus(PostingStatus.CANCELLED);
    }

    @Transactional
    public int closeExpired() {
        return postingRepository.updateStatusWhenExpired(
                PostingStatus.OPEN, PostingStatus.CLOSED, Instant.now());
    }

    private void validate(CreatePostingRequest request) {
        validateStageAndDeadline(request.type(), request.stage(), request.deadline());

        Set<String> seenRoles = new HashSet<>();
        for (PostingRoleRequest role : request.roles()) {
            if (!seenRoles.add(role.role().toLowerCase(Locale.ROOT))) {
                throw new InvalidPostingException("Aynı rol birden fazla kez eklenemez: " + role.role());
            }
        }
    }

    private void validateStageAndDeadline(PostingType type, PostingStage stage, Instant deadline) {
        if (stage != null && type != PostingType.STARTUP) {
            throw new InvalidPostingException("Aşama bilgisi sadece girişim ilanlarında kullanılabilir.");
        }
        if (deadline.isAfter(Instant.now().plus(365, ChronoUnit.DAYS))) {
            throw new InvalidPostingException("Son tarih en fazla 1 yıl sonrası olabilir.");
        }
    }
}