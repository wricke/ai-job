package com.hpc.jobagent.service;

import java.time.LocalDateTime;

import com.hpc.jobagent.domain.AnalysisCache;
import com.hpc.jobagent.mapper.AnalysisCacheMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class AnalysisCacheService {

    private final AnalysisCacheMapper cacheMapper;

    public AnalysisCacheService(AnalysisCacheMapper cacheMapper) {
        this.cacheMapper = cacheMapper;
    }

    public AnalysisCache find(String cacheKey) {
        return cacheMapper.findByKey(cacheKey);
    }

    public void save(AnalysisCache cache) {
        LocalDateTime now = LocalDateTime.now();
        AnalysisCache existing = cacheMapper.findByKey(cache.getCacheKey());
        cache.setUpdatedAt(now);
        if (existing == null) {
            cache.setCreatedAt(now);
            try {
                cacheMapper.insert(cache);
                return;
            } catch (DuplicateKeyException ignored) {
                // Another analysis completed the same pair first.
            }
        }
        cacheMapper.update(cache);
    }
}
