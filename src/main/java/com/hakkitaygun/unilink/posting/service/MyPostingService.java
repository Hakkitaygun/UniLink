package com.hakkitaygun.unilink.posting.service;

import com.hakkitaygun.unilink.application.enums.ApplicationStatus;
import com.hakkitaygun.unilink.application.repository.ApplicationRepository;
import com.hakkitaygun.unilink.common.dto.PageResponse;
import com.hakkitaygun.unilink.posting.dto.MyPostingResponse;
import com.hakkitaygun.unilink.posting.dto.PostingSummaryResponse;
import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import com.hakkitaygun.unilink.posting.mapper.PostingSummaryMapper;
import com.hakkitaygun.unilink.posting.repository.PostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class MyPostingService {

    private final PostingRepository postingRepository;
    private final ApplicationRepository applicationRepository;
    private final PostingSummaryMapper postingSummaryMapper;

    @Transactional(readOnly = true)
    public PageResponse<MyPostingResponse> list(UUID ownerId, PostingStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));

        Page<Posting> postings = status == null
                ? postingRepository.findByOwnerId(ownerId, pageable)
                : postingRepository.findByOwnerIdAndStatus(ownerId, status, pageable);

        List<Posting> list = postings.getContent();
        List<PostingSummaryResponse> summaries = postingSummaryMapper.toSummaries(list, ownerId);
        Map<UUID, Map<ApplicationStatus, Long>> counts = applicationCounts(list);

        List<MyPostingResponse> content = IntStream.range(0, list.size())
                .mapToObj(i -> new MyPostingResponse(
                        summaries.get(i),
                        list.get(i).getStatus(),
                        counts.get(list.get(i).getId())))
                .toList();

        return PageResponse.of(postings, content);
    }

    private Map<UUID, Map<ApplicationStatus, Long>> applicationCounts(List<Posting> postings) {
        Map<UUID, Map<ApplicationStatus, Long>> result = new HashMap<>();
        for (Posting posting : postings) {
            result.put(posting.getId(), emptyCounts());
        }
        if (postings.isEmpty()) {
            return result;
        }

        Set<UUID> ids = postings.stream().map(Posting::getId).collect(Collectors.toSet());
        for (ApplicationRepository.PostingApplicationCount row : applicationRepository.countByPostingIds(ids)) {
            result.get(row.getPostingId()).put(row.getStatus(), row.getTotal());
        }
        return result;
    }

    private Map<ApplicationStatus, Long> emptyCounts() {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, 0L);
        }
        return counts;
    }
}