package com.hakkitaygun.unilink.posting.service;

import com.hakkitaygun.unilink.posting.dto.CreatePostingRequest;
import com.hakkitaygun.unilink.posting.dto.PostingResponse;
import com.hakkitaygun.unilink.posting.dto.PostingRoleRequest;
import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.entity.PostingRole;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import com.hakkitaygun.unilink.posting.enums.PostingType;
import com.hakkitaygun.unilink.posting.exception.InvalidPostingException;
import com.hakkitaygun.unilink.posting.exception.PostingNotFoundException;
import com.hakkitaygun.unilink.posting.repository.PostingRepository;
import com.hakkitaygun.unilink.technology.service.TechnologyService;
import com.hakkitaygun.unilink.user.entity.Profile;
import com.hakkitaygun.unilink.user.exception.ProfileNotFoundException;
import com.hakkitaygun.unilink.user.repository.ProfileRepository;
import com.hakkitaygun.unilink.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    private void validate(CreatePostingRequest request) {
        if (request.stage() != null && request.type() != PostingType.STARTUP) {
            throw new InvalidPostingException("Aşama bilgisi sadece girişim ilanlarında kullanılabilir.");
        }

        if (request.deadline().isAfter(Instant.now().plus(365, ChronoUnit.DAYS))) {
            throw new InvalidPostingException("Son tarih en fazla 1 yıl sonrası olabilir.");
        }

        Set<String> seenRoles = new HashSet<>();
        for (PostingRoleRequest role : request.roles()) {
            if (!seenRoles.add(role.role().toLowerCase(Locale.ROOT))) {
                throw new InvalidPostingException("Aynı rol birden fazla kez eklenemez: " + role.role());
            }
        }
    }
}