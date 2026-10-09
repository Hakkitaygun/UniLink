package com.hakkitaygun.unilink.posting.entity;

import com.hakkitaygun.unilink.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posting_roles")
@Getter
@Setter
@NoArgsConstructor
public class PostingRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posting_id", nullable = false)
    private Posting posting;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(name = "required_count", nullable = false)
    private int requiredCount;
}