package com.hakkitaygun.unilink.technology.entity;

import com.hakkitaygun.unilink.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "technologies")
@Getter
@Setter
@NoArgsConstructor
public class Technology extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Technology(String name) {
        this.name = name;
    }
}
