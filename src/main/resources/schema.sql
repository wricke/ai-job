CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    display_name VARCHAR(80),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_user_account_username (username)
);

CREATE TABLE IF NOT EXISTS resume_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    title VARCHAR(120) NOT NULL,
    owner_name VARCHAR(80),
    target_role VARCHAR(120),
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS job_posting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    company VARCHAR(120),
    title VARCHAR(120) NOT NULL,
    source VARCHAR(120),
    description LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS analysis_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    resume_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    match_score INT,
    summary LONGTEXT,
    resume_insight LONGTEXT,
    job_insight LONGTEXT,
    match_detail LONGTEXT,
    suggestions LONGTEXT,
    interview_questions LONGTEXT,
    agent_trace LONGTEXT,
    error_message LONGTEXT,
    cache_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    CONSTRAINT fk_analysis_resume FOREIGN KEY (resume_id) REFERENCES resume_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_analysis_job FOREIGN KEY (job_id) REFERENCES job_posting(id) ON DELETE CASCADE,
    INDEX idx_analysis_resume (resume_id, id),
    INDEX idx_analysis_job (job_id, id),
    INDEX idx_analysis_status (status, id)
);

CREATE TABLE IF NOT EXISTS analysis_cache (
    cache_key VARCHAR(128) PRIMARY KEY,
    match_score INT NOT NULL,
    summary LONGTEXT NOT NULL,
    resume_insight LONGTEXT NOT NULL,
    job_insight LONGTEXT NOT NULL,
    match_detail LONGTEXT NOT NULL,
    suggestions LONGTEXT NOT NULL,
    interview_questions LONGTEXT NOT NULL,
    agent_trace LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
