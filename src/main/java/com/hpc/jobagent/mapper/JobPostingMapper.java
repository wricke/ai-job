package com.hpc.jobagent.mapper;

import java.util.List;

import com.hpc.jobagent.domain.JobPosting;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface JobPostingMapper {

    @Insert("""
            INSERT INTO job_posting (user_id, company, title, source, description, created_at, updated_at)
            VALUES (#{userId}, #{company}, #{title}, #{source}, #{description}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JobPosting job);

    @Select("SELECT * FROM job_posting WHERE id = #{id}")
    JobPosting findById(Long id);

    @Select("SELECT * FROM job_posting WHERE id = #{id} AND user_id = #{userId}")
    JobPosting findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            SELECT * FROM job_posting
            WHERE user_id = #{userId} AND title = #{title} AND source = #{source}
            ORDER BY id DESC
            LIMIT 1
            """)
    JobPosting findByTitleAndSource(@Param("userId") Long userId,
                                    @Param("title") String title,
                                    @Param("source") String source);

    @Select("SELECT * FROM job_posting ORDER BY id DESC")
    List<JobPosting> findAll();

    @Select("SELECT * FROM job_posting WHERE user_id = #{userId} ORDER BY id DESC")
    List<JobPosting> findAllByUserId(Long userId);

    @Update("""
            UPDATE job_posting
            SET company = #{company},
                title = #{title},
                source = #{source},
                description = #{description},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(JobPosting job);

    @Delete("DELETE FROM job_posting WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM job_posting WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
