-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    login VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    email VARCHAR(255),
    phone VARCHAR(20),
    telegram_chat_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы конфигурации OTP (всегда будет только 1 запись)
CREATE TABLE IF NOT EXISTS otp_config (
    id SERIAL PRIMARY KEY,
    code_length INT DEFAULT 6,
    ttl_seconds INT DEFAULT 300,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Вставка дефолтной конфигурации
INSERT INTO otp_config (code_length, ttl_seconds) 
SELECT 6, 300 
WHERE NOT EXISTS (SELECT 1 FROM otp_config);

-- Создание таблицы OTP кодов
CREATE TABLE IF NOT EXISTS otp_codes (
    id SERIAL PRIMARY KEY,
    operation_id VARCHAR(100) NOT NULL,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    delivery_method VARCHAR(20)
);

-- Создание индексов для оптимизации
CREATE INDEX IF NOT EXISTS idx_otp_codes_user_status ON otp_codes(user_id, status);
CREATE INDEX IF NOT EXISTS idx_otp_codes_expires ON otp_codes(expires_at);
CREATE INDEX IF NOT EXISTS idx_otp_codes_operation ON otp_codes(operation_id);

-- Создание таблицы для логов операций (опционально)
CREATE TABLE IF NOT EXISTS operation_logs (
    id SERIAL PRIMARY KEY,
    operation_id VARCHAR(100) NOT NULL,
    user_id INT REFERENCES users(id),
    action VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание администратора по умолчанию (пароль: admin123)
-- Хэш пароля сгенерирован с помощью BCrypt
INSERT INTO users (login, password_hash, role, email) 
SELECT 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMy.Mr/.3KqVzV5GjFQ6QfV7Yx8GqC5qJQK', 'ADMIN', 'admin@promo.com'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE role = 'ADMIN');