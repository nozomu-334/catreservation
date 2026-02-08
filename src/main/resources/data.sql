-- ===============================
-- 初期ユーザデータ投入
-- ===============================

-- 管理者ユーザ
INSERT INTO users (name, email, password, role)
VALUES
('管理者', 'admin@example.com', 'admin', 'ADMIN');

-- スタッフユーザ
INSERT INTO users (name, email, password, role)
VALUES
('スタッフ太郎', 'staff@example.com', 'staff', 'STAFF');

-- 顧客ユーザ
INSERT INTO users (name, email, password, role)
VALUES
('顧客花子', 'user@example.com', 'user', 'CUSTOMER');

-- ===============================
-- ※ reservation / shift は
-- 画面操作・Service 実装後に登録するため
-- ここでは投入しない（テキスト準拠）
-- ===============================