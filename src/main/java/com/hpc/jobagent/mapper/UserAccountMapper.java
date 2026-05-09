package com.hpc.jobagent.mapper;

import com.hpc.jobagent.domain.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

public interface UserAccountMapper {

    @Insert("""
            INSERT INTO user_account (username, password_hash, display_name, created_at, updated_at)
            VALUES (#{username}, #{passwordHash}, #{displayName}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount user);

    @Select("SELECT * FROM user_account WHERE username = #{username}")
    UserAccount findByUsername(String username);

    @Select("SELECT * FROM user_account WHERE id = #{id}")
    UserAccount findById(Long id);
}
