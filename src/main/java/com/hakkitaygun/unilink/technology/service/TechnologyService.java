package com.hakkitaygun.unilink.technology.service;

import com.hakkitaygun.unilink.technology.dto.TechnologyResponse;
import com.hakkitaygun.unilink.technology.entity.Technology;
import com.hakkitaygun.unilink.technology.repository.TechnologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TechnologyService {

    private final TechnologyRepository technologyRepository;

    @Transactional(readOnly = true)
    public List<TechnologyResponse> getAll() {
        return technologyRepository.findAll().stream()
                .map(TechnologyResponse::from)
                .sorted(Comparator.comparing(TechnologyResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TechnologyResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return getAll();
        }
        return technologyRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(query.trim()).stream()
                .map(TechnologyResponse::from)
                .toList();
    }

    @Transactional
    public Set<Technology> resolveOrCreate(Collection<String> names) {
        Set<Technology> result = new HashSet<>();
        for (String raw : names) {
            String name = raw.trim().replaceAll("\\s+", " ");
            Technology technology = technologyRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> technologyRepository.save(new Technology(name)));
            result.add(technology);
        }
        return result;
    }
}