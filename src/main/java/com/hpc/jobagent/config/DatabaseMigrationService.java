package com.hpc.jobagent.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrate() {
        addColumnIfMissing("resume_profile", "user_id", "BIGINT NULL");
        addColumnIfMissing("job_posting", "user_id", "BIGINT NULL");
        addColumnIfMissing("analysis_task", "user_id", "BIGINT NULL");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE LOWER(table_name) = LOWER(?)
                  AND LOWER(column_name) = LOWER(?)
                """, Integer.class, tableName, columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }
}
