package com.hakkitaygun.unilink.application.entity;

import com.hakkitaygun.unilink.application.enums.ApplicationStatus;
import com.hakkitaygun.unilink.common.entity.AuditableEntity;
import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.entity.PostingRole;
import com.hakkitaygun.unilink.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
public class Application extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false)
    private Posting posting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(length = 1000)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_role_id")
    private PostingRole acceptedRole;

    @Column(name = "decided_at")
    private Instant decidedAt;

    public void accept(PostingRole role) {
        if (status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Sadece bekleyen başvuru kabul edilebilir");
        }
        this.status = ApplicationStatus.ACCEPTED;
        this.acceptedRole = role;
        this.decidedAt = Instant.now();
    }

    public void reject() {
        if (status != ApplicationStatus.PENDING) {
            throw new IllegalStateException("Sadece bekleyen başvuru reddedilebilir");
        }
        this.status = ApplicationStatus.REJECTED;
        this.decidedAt = Instant.now();
    }

    public void withdraw() {
        if (status != ApplicationStatus.PENDING && status != ApplicationStatus.ACCEPTED) {
            throw new IllegalStateException("Bu başvuru geri çekilemez");
        }
        this.status = ApplicationStatus.WITHDRAWN;
        this.acceptedRole = null;
        this.decidedAt = Instant.now();
    }
}
