CREATE TABLE tickets (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 title VARCHAR(160) NOT NULL,
 description VARCHAR(6000) NOT NULL,
 environment VARCHAR(120) NOT NULL,
 status VARCHAR(20) NOT NULL,
 category VARCHAR(30) NOT NULL,
 analysis TEXT,
 analysis_source VARCHAR(30),
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL
);
CREATE INDEX idx_tickets_created ON tickets(created_at);
