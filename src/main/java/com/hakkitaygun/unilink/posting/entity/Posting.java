package com.hakkitaygun.unilink.posting.entity;

import com.hakkitaygun.unilink.common.entity.AuditableEntity;
import com.hakkitaygun.unilink.posting.enums.PostingStage;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import com.hakkitaygun.unilink.posting.enums.PostingType;
import com.hakkitaygun.unilink.technology.entity.Technology;
import com.hakkitaygun.unilink.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "postings")
@Getter
@Setter
@NoArgsConstructor
public class Posting extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostingType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(name = "organization_name", length = 150)
    private String organizationName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PostingStage stage;

    @Column(name = "benefits_note", length = 500)
    private String benefitsNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostingStatus status = PostingStatus.OPEN;

    @Column(nullable = false)
    private Instant deadline;

    @ManyToMany
    @JoinTable(
            name = "posting_technologies",
            joinColumns = @JoinColumn(name = "posting_id"),
            inverseJoinColumns = @JoinColumn(name = "technology_id"))
    private Set<Technology> technologies = new HashSet<>();

    @OneToMany(mappedBy = "posting", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostingRole> roles = new HashSet<>();

    public boolean isOpen(Instant now) {
        return status == PostingStatus.OPEN && deadline.isAfter(now);
    }

    public void addRole(PostingRole role) {
        roles.add(role);
        role.setPosting(this);
    }

    public void removeRole(PostingRole role) {
        roles.remove(role);
        role.setPosting(null);
    }
}