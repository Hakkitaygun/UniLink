package com.hakkitaygun.unilink.posting.mapper;

import com.hakkitaygun.unilink.posting.dto.PostingSummaryResponse;
import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.entity.PostingRole;
import com.hakkitaygun.unilink.posting.repository.PostingRoleRepository;
import com.hakkitaygun.unilink.posting.repository.SavedPostingRepository;
import com.hakkitaygun.unilink.user.entity.Profile;
import com.hakkitaygun.unilink.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostingSummaryMapper {

    private final ProfileRepository profileRepository;
    private final PostingRoleRepository postingRoleRepository;
    private final SavedPostingRepository savedPostingRepository;

    /** Sırayı korur. Lazy ilişkilere dokunduğu için bir @Transactional metodun içinden çağrılmalı. */
    public List<PostingSummaryResponse> toSummaries(List<Posting> postings, UUID viewerId) {
        Set<UUID> ownerIds = postings.stream()
                .map(posting -> posting.getOwner().getId())
                .collect(Collectors.toSet());
        Map<UUID, Profile> profiles = profileRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(Profile::getUserId, Function.identity()));

        Map<UUID, Integer> acceptedCounts = acceptedCounts(postings);
        Set<UUID> savedIds = savedIds(viewerId, postings.stream().map(Posting::getId).toList());

        return postings.stream()
                .map(posting -> PostingSummaryResponse.from(
                        posting,
                        profiles.get(posting.getOwner().getId()),
                        acceptedCounts,
                        savedIds.contains(posting.getId())))
                .toList();
    }

    public Map<UUID, Integer> acceptedCounts(Collection<Posting> postings) {
        Set<UUID> roleIds = postings.stream()
                .flatMap(posting -> posting.getRoles().stream())
                .map(PostingRole::getId)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return postingRoleRepository.countAcceptedByRoleIds(roleIds).stream()
                .collect(Collectors.toMap(
                        PostingRoleRepository.RoleAcceptedCount::getRoleId,
                        count -> (int) count.getAcceptedCount()));
    }

    /** Verilen ilanlardan, bu kullanıcının kaydettiklerinin kimlikleri. */
    public Set<UUID> savedIds(UUID viewerId, Collection<UUID> postingIds) {
        if (postingIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(savedPostingRepository.findSavedPostingIds(viewerId, postingIds));
    }

    public boolean isSaved(UUID viewerId, UUID postingId) {
        return !savedIds(viewerId, List.of(postingId)).isEmpty();
    }
}