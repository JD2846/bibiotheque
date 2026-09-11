-- ============================================================
-- init-scripts/01-init.sql
-- Initialisation de la base "bibliotheque" (PostgreSQL 16)
-- Exécuté automatiquement par le conteneur postgres au premier
-- démarrage (docker-entrypoint-initdb.d)
-- ============================================================

-- ------------------------------------------------------------
-- 1. Séquences
-- ------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS role_seq  START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS users_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS books_seq START WITH 100 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS borrow_seq START WITH 100 INCREMENT BY 1;

-- ------------------------------------------------------------
-- 2. Table role
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
    role_id   INTEGER      PRIMARY KEY DEFAULT nextval('role_seq'),
    role_name VARCHAR(255) NOT NULL UNIQUE
);

-- ------------------------------------------------------------
-- 3. Table users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id  INTEGER      PRIMARY KEY DEFAULT nextval('users_seq'),
    username VARCHAR(255) NOT NULL UNIQUE,
    name     VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 4. Table de liaison users <-> role
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_role (
    user_id INTEGER NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES role  (role_id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ------------------------------------------------------------
-- 5. Table books
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS books (
    book_id     INTEGER      PRIMARY KEY DEFAULT nextval('books_seq'),
    book_name   VARCHAR(255) NOT NULL,
    book_author VARCHAR(255),
    book_genre  VARCHAR(255),
    no_of_copies INTEGER     NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 6. Table borrow (emprunts)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS borrow (
    borrow_id   INTEGER   PRIMARY KEY DEFAULT nextval('borrow_seq'),
    book_id     INTEGER   NOT NULL REFERENCES books (book_id) ON DELETE CASCADE,
    user_id     INTEGER   NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    issue_date  TIMESTAMP,
    return_date TIMESTAMP,
    due_date    TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- ------------------------------------------------------------
-- 7. Index
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_user_role_user  ON user_role (user_id);
CREATE INDEX IF NOT EXISTS idx_user_role_role  ON user_role (role_id);
CREATE INDEX IF NOT EXISTS idx_borrow_user     ON borrow (user_id);
CREATE INDEX IF NOT EXISTS idx_borrow_book     ON borrow (book_id);

-- ------------------------------------------------------------
-- 8. Rôles (Admin, User)
-- ------------------------------------------------------------
INSERT INTO role (role_id, role_name) VALUES (1, 'Admin')
    ON CONFLICT (role_id) DO NOTHING;
INSERT INTO role (role_id, role_name) VALUES (2, 'User')
    ON CONFLICT (role_id) DO NOTHING;

-- ------------------------------------------------------------
-- 9. Compte administrateur
--    Login    : admin
--    Password : admin123
--    Hash BCrypt ($2b$) généré pour "admin123"
-- ------------------------------------------------------------
INSERT INTO users (user_id, username, name, password)
VALUES (1, 'admin', 'Administrator', '$2b$10$RN5ij7XXjDpRBALhITW.2uzYGontX4U9c9ZRH5i3e.5l6RvkjZ696')
    ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_role (user_id, role_id) VALUES (1, 1)
    ON CONFLICT (user_id, role_id) DO NOTHING;

-- ------------------------------------------------------------
-- 10. Données de test : livres
--     (voir init-scripts/03-reservation-testdata.sql pour L1 à L5)
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 11. Mise à jour des séquences
-- ------------------------------------------------------------
SELECT setval('role_seq', COALESCE((SELECT MAX(role_id) + 1 FROM role), 100));
SELECT setval('users_seq', COALESCE((SELECT MAX(user_id) + 1 FROM users), 100));
SELECT setval('books_seq', COALESCE((SELECT MAX(book_id) + 1 FROM books), 100));
SELECT setval('borrow_seq', COALESCE((SELECT MAX(borrow_id) + 1 FROM borrow), 100));