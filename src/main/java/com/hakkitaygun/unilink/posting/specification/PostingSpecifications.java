package com.hakkitaygun.unilink.posting.specification;

import com.hakkitaygun.unilink.application.entity.Application;
import com.hakkitaygun.unilink.common.util.TextNormalizer;
import com.hakkitaygun.unilink.posting.entity.Posting;
import com.hakkitaygun.unilink.posting.entity.PostingRole;
import com.hakkitaygun.unilink.posting.enums.PostingStatus;
import com.hakkitaygun.unilink.posting.enums.PostingType;
import com.hakkitaygun.unilink.technology.entity.Technology;
import com.hakkitaygun.unilink.user.entity.Profile;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PostingSpecifications {

    private static final int MAX_TEXT_LENGTH = 100;
    private static final int MAX_TECHNOLOGIES = 10;

    private PostingSpecifications() {
    }

    public static Specification<Posting> isOpen(Instant now) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), PostingStatus.OPEN),
                cb.greaterThan(root.<Instant>get("deadline"), now));
    }

    public static Specification<Posting> hasType(PostingType type) {
        if (type == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Posting> matchesQuery(String text) {
        String pattern = containsPattern(text);
        if (pattern == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.or(
                cb.like(folded(cb, root.<String>get("title")), pattern, '\\'),
                cb.like(folded(cb, root.<String>get("organizationName")), pattern, '\\'));
    }

    public static Specification<Posting> hasAnyTechnology(Collection<String> names) {
        Set<String> foldedNames = foldAll(names);
        if (foldedNames.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<Posting> posting = subquery.from(Posting.class);
            Join<Posting, Technology> technology = posting.join("technologies");
            subquery.select(posting.get("id"))
                    .where(folded(cb, technology.<String>get("name")).in(foldedNames));
            return root.get("id").in(subquery);
        };
    }

    public static Specification<Posting> hasRole(String text) {
        String pattern = containsPattern(text);
        if (pattern == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<PostingRole> role = subquery.from(PostingRole.class);
            subquery.select(role.get("posting").get("id"))
                    .where(cb.like(folded(cb, role.<String>get("role")), pattern, '\\'));
            return root.get("id").in(subquery);
        };
    }

    public static Specification<Posting> ownerDepartmentContains(String text) {
        String pattern = containsPattern(text);
        if (pattern == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<Profile> profile = subquery.from(Profile.class);
            subquery.select(profile.get("userId"))
                    .where(cb.like(folded(cb, profile.<String>get("department")), pattern, '\\'));
            return root.get("owner").get("id").in(subquery);
        };
    }

    public static Specification<Posting> ownerUniversityContains(String text) {
        String pattern = containsPattern(text);
        if (pattern == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            Root<Profile> profile = subquery.from(Profile.class);
            subquery.select(profile.get("userId"))
                    .where(cb.like(folded(cb, profile.<String>get("university")), pattern, '\\'));
            return root.get("owner").get("id").in(subquery);
        };
    }

    public static Specification<Posting> hasVacancy(Boolean hasVacancy) {
        if (hasVacancy == null || !hasVacancy) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Subquery<Integer> roleQuery = query.subquery(Integer.class);
            Root<PostingRole> role = roleQuery.from(PostingRole.class);

            Subquery<Long> acceptedQuery = roleQuery.subquery(Long.class);
            Root<Application> application = acceptedQuery.from(Application.class);
            acceptedQuery.select(cb.count(application))
                    .where(cb.equal(application.get("acceptedRole"), role));

            roleQuery.select(cb.literal(1))
                    .where(
                            cb.equal(role.get("posting"), root),
                            cb.gt(role.<Integer>get("requiredCount"), acceptedQuery));

            return cb.exists(roleQuery);
        };
    }

    private static Expression<String> folded(CriteriaBuilder cb, Expression<String> column) {
        Expression<String> translated = cb.function("translate", String.class, column,
                cb.literal(TextNormalizer.TURKISH_CHARS),
                cb.literal(TextNormalizer.ASCII_CHARS));
        return cb.lower(translated);
    }

    private static Set<String> foldAll(Collection<String> names) {
        if (names == null) {
            return Set.of();
        }
        return names.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> TextNormalizer.fold(name.trim()))
                .limit(MAX_TECHNOLOGIES)
                .collect(Collectors.toSet());
    }

    private static String containsPattern(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            trimmed = trimmed.substring(0, MAX_TEXT_LENGTH);
        }
        return "%" + escapeLike(TextNormalizer.fold(trimmed)) + "%";
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}