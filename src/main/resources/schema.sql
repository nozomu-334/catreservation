-- ================================
-- 初期化（依存関係ごと削除）
-- ================================

DROP TABLE IF EXISTS reservations CASCADE;
DROP TABLE IF EXISTS shifts CASCADE;
DROP TABLE IF EXISTS cat_attendance CASCADE;
DROP TABLE IF EXISTS favorite_cats CASCADE;
DROP TABLE IF EXISTS cats CASCADE;
DROP TABLE IF EXISTS seats CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ================================
-- users（ユーザ）
-- ================================
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    line_id VARCHAR(255),
    google_token TEXT
);

-- ================================
-- seats（席マスタ）
-- ================================
CREATE TABLE seats (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255)
);

-- ================================
-- cats（猫マスタ）
-- ================================
CREATE TABLE cats (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    breed VARCHAR(100),
    age INT,
    image_url VARCHAR(255)
);

-- ================================
-- favorite_cats（推し猫）
-- ================================
CREATE TABLE favorite_cats (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    cat_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (cat_id) REFERENCES cats(id)
);

-- ================================
-- cat_attendance（猫の出勤管理）
-- ================================
CREATE TABLE cat_attendance (
    id SERIAL PRIMARY KEY,
    cat_id INT NOT NULL,
    record_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    attendance VARCHAR(20) NOT NULL,
    note VARCHAR(255),
    FOREIGN KEY (cat_id) REFERENCES cats(id)
);

-- ================================
-- reservations（予約）
-- ================================
CREATE TABLE reservations (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    seat_id INT NOT NULL,
    cat_id INT,
    record_date DATE NOT NULL,
    time_slot TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'RESERVED',
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (seat_id) REFERENCES seats(id),
    FOREIGN KEY (cat_id) REFERENCES cats(id)
);

-- ================================
-- shifts（スタッフシフト）
-- ================================
CREATE TABLE shifts (
    id SERIAL PRIMARY KEY,
    staff_id INT NOT NULL,
    record_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    FOREIGN KEY (staff_id) REFERENCES users(id)
);