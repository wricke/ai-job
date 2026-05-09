package com.hpc.jobagent.mapper;

import java.util.List;

import com.hpc.jobagent.domain.ResumeProfile;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ResumeProfileMapper {

    @Insert("""
            INSERT INTO resume_profile (user_id, title, owner_name, target_role, content, created_at, updated_at)
            VALUES (#{userId}, #{title}, #{ownerName}, #{targetRole}, #{content}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ResumeProfile resume);

    @Select("SELECT * FROM resume_profile WHERE id = #{id}")
    ResumeProfile findById(Long id);

    @Select("SELECT * FROM resume_profile WHERE id = #{id} AND user_id = #{userId}")
    ResumeProfile findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
            SELECT * FROM resume_profile
            WHERE user_id = #{userId} AND content = #{content}
            ORDER BY id DESC
            LIMIT 1
            """)
    ResumeProfile findLatestByContent(@Param("userId") Long userId, @Param("content") String content);

    @Select("SELECT * FROM resume_profile ORDER BY id DESC")
    List<ResumeProfile> findAll();

    @Select("SELECT * FROM resume_profile WHERE user_id = #{userId} ORDER BY id DESC")
    List<ResumeProfile> findAllByUserId(Long userId);

    @Update("""
            UPDATE resume_profile
            SET title = #{title},
                owner_name = #{ownerName},
                target_role = #{targetRole},
                content = #{content},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(ResumeProfile resume);

    @Delete("DELETE FROM resume_profile WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM resume_profile WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
