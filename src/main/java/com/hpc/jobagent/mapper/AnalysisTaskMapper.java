package com.hpc.jobagent.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.hpc.jobagent.domain.AnalysisStatus;
import com.hpc.jobagent.domain.AnalysisTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AnalysisTaskMapper {

    @Insert("""
            INSERT INTO analysis_task
            (user_id, resume_id, job_id, status, match_score, summary, resume_insight, job_insight,
             match_detail, suggestions, interview_questions, agent_trace, error_message,
             cache_key, created_at, updated_at, completed_at)
            VALUES
            (#{userId}, #{resumeId}, #{jobId}, #{status}, #{matchScore}, #{summary}, #{resumeInsight}, #{jobInsight},
             #{matchDetail}, #{suggestions}, #{interviewQuestions}, #{agentTrace}, #{errorMessage},
             #{cacheKey}, #{createdAt}, #{updatedAt}, #{completedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnalysisTask task);

    @Select("SELECT * FROM analysis_task WHERE id = #{id}")
    AnalysisTask findById(Long id);

    @Select("SELECT * FROM analysis_task WHERE id = #{id} AND user_id = #{userId}")
    AnalysisTask findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT * FROM analysis_task
            <where>
              user_id = #{userId}
              <if test="resumeId != null">AND resume_id = #{resumeId}</if>
              <if test="jobId != null">AND job_id = #{jobId}</if>
              <if test="status != null">AND status = #{status}</if>
            </where>
            ORDER BY id DESC
            LIMIT #{limit}
            </script>
            """)
    List<AnalysisTask> search(@Param("resumeId") Long resumeId,
                              @Param("jobId") Long jobId,
                              @Param("status") AnalysisStatus status,
                              @Param("limit") int limit,
                              @Param("userId") Long userId);

    @Update("""
            UPDATE analysis_task
            SET status = #{status},
                updated_at = #{updatedAt},
                error_message = #{errorMessage}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") AnalysisStatus status,
                     @Param("errorMessage") String errorMessage,
                     @Param("updatedAt") LocalDateTime updatedAt);

    @Update("""
            UPDATE analysis_task
            SET status = 'COMPLETED',
                match_score = #{matchScore},
                summary = #{summary},
                resume_insight = #{resumeInsight},
                job_insight = #{jobInsight},
                match_detail = #{matchDetail},
                suggestions = #{suggestions},
                interview_questions = #{interviewQuestions},
                agent_trace = #{agentTrace},
                error_message = NULL,
                updated_at = #{updatedAt},
                completed_at = #{completedAt}
            WHERE id = #{id}
            """)
    int markCompleted(AnalysisTask task);

    @Delete("DELETE FROM analysis_task WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM analysis_task WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
