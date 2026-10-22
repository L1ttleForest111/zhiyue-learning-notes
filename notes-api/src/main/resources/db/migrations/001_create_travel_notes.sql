USE learning_notes;

CREATE TABLE IF NOT EXISTS travel_notes (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  destination VARCHAR(100) NOT NULL DEFAULT '',
  start_date DATE NULL,
  end_date DATE NULL,
  summary VARCHAR(1000) NOT NULL DEFAULT '',
  journal_content LONGTEXT NOT NULL,
  html_content LONGTEXT NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  import_key CHAR(36) NOT NULL UNIQUE,
  html_sha256 CHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_travel_list (deleted, updated_at, id),
  INDEX idx_travel_html_sha (html_sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
