package com.hakkitaygun.unilink.posting.dto;

import com.hakkitaygun.unilink.application.enums.ApplicationStatus;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;

import java.util.Map;

public record MyPostingResponse(
        PostingSummaryResponse posting,
        PostingStatus status,
        Map<ApplicationStatus, Long> applicationCounts
) {
}