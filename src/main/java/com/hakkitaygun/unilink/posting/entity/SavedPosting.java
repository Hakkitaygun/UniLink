package com.hakkitaygun.unilink.posting.entity;

import com.hakkitaygun.unilink.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "saved_postings")
@Getter
@NoArgsConstructor
public class SavedPosting {

    @EmbeddedId
    private SavedPostingId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("postingId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id")
    private Posting posting;

    @CreationTimestamp
    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;

    public SavedPosting(User user, Posting posting) {
        this.user = user;
        this.posting = posting;
        this.id = new SavedPostingId(user.getId(), posting.getId());
    }
}
