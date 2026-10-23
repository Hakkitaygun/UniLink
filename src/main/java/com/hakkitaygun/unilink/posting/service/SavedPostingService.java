package com.hakkitaygun.unilink.posting.service;

import com.hakkitaygun.unilink.common.dto.PageResponse;
import com.hakkitaygun.unilink.posting.dto.PostingSummaryResponse;
import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.entity.SavedPosting;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import com.hakkitaygun.unilink.posting.exception.PostingNotFoundException;
import com.hakkitaygun.unilink.posting.mapper.PostingSummaryMapper;
import com.hakkitaygun.unilink.posting.repository.PostingRepository;
import com.hakkitaygun.unilink.posting.repository.SavedPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedPostingService {

    private final SavedPostingRepository savedPostingRepository;
    private final PostingRepository postingRepository;
    private final PostingSummaryMapper postingSummaryMapper;

    @Transactional
    public void save(UUID userId, UUID postingId) {
        Posting posting = postingRepository.findById(postingId)
                .orElseThrow(PostingNotFoundException::new);
        if (posting.getStatus() == PostingStatus.CANCELLED) {
            throw new PostingNotFoundException();
        }
        savedPostingRepository.insertIfAbsent(userId, postingId);
    }

    @Transactional
    public void unsave(UUID userId, UUID postingId) {
        savedPostingRepository.deleteByUserAndPosting(userId, postingId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostingSummaryResponse> list(UUID userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Page<SavedPosting> saved = savedPostingRepository.findVisibleByUser(
                userId, PostingStatus.CANCELLED, PageRequest.of(safePage, safeSize));

        List<Posting> postings = saved.getContent().stream()
                .map(SavedPosting::getPosting)
                .toList();
        return PageResponse.of(saved, postingSummaryMapper.toSummaries(postings, userId));
    }
}