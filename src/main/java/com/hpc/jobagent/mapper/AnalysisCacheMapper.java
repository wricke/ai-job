package com.hpc.jobagent.mapper;

import com.hpc.jobagent.domain.AnalysisCache;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AnalysisCacheMapper {

    @Select("SELECT * FROM analysis_cache WHERE cache_key = #{cacheKey}")
    AnalysisCache findByKey(String cacheKey);

    @Insert("""
            INSERT INTO analysis_cache
            (cache_key, match_score, summary, resume_insight, job_insight, match_detail,
             suggestions, interview_questions, agent_trace, created_at, updated_at)
            VALUES
            (#{cacheKey}, #{matchScore}, #{summary}, #{resumeInsight}, #{jobInsight}, #{matchDetail},
             #{suggestions}, #{interviewQuestions}, #{agentTrace}, #{createdAt}, #{updatedAt})
            """)
    int insert(AnalysisCache cache);

    @Update("""
            UPDATE analysis_cache
            SET match_score = #{matchScore},
                summary = #{summary},
                resume_insight = #{resumeInsight},
                job_insight = #{jobInsight},
                match_detail = #{matchDetail},
                suggestions = #{suggestions},
                interview_questions = #{interviewQuestions},
                agent_trace = #{agentTrace},
                updated_at = #{updatedAt}
            WHERE cache_key = #{cacheKey}
            """)
    int update(AnalysisCache cache);
}
