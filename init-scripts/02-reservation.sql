-- ============================================================
-- init-scripts/02-reservation.sql
-- Table reservation + séquence pour la gestion de réservations
-- Exécuté automatiquement par le conteneur postgres au premier
-- démarrage (docker-entrypoint-initdb.d)
-- ============================================================

-- ------------------------------------------------------------
-- 1. Séquence
-- ------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS reservation_seq START WITH 100 INCREMENT BY 1;

-- ------------------------------------------------------------
-- 2. Table reservation
--    status en VARCHAR (et non un ENUM Postgres natif) car Hibernate
--    envoie les valeurs @Enumerated(EnumType.STRING) sous forme de
--    character varying : un type ENUM natif ferait échouer toutes
--    les comparaisons ("operator does not exist: reservation_status
--    = character varying").
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservation (
    reservation_id INTEGER              PRIMARY KEY DEFAULT nextval('reservation_seq'),
    book_id        INTEGER              NOT NULL REFERENCES books (book_id) ON DELETE CASCADE,
    user_id        INTEGER              NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    status         VARCHAR(20)          NOT NULL DEFAULT 'EN_ATTENTE'
                        CHECK (status IN ('EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE')),
    date_reservation TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_expiration  TIMESTAMP          NOT NULL,
    created_at     TIMESTAMP            DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 4. Index
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_reservation_user   ON reservation (user_id);
CREATE INDEX IF NOT EXISTS idx_reservation_book   ON reservation (book_id);
CREATE INDEX IF NOT EXISTS idx_reservation_status ON reservation (status);
CREATE INDEX IF NOT EXISTS idx_reservation_user_status ON reservation (user_id, status);

-- ------------------------------------------------------------
-- 5. Mise à jour de la séquence
-- ------------------------------------------------------------
SELECT setval('reservation_seq', COALESCE((SELECT MAX(reservation_id) + 1 FROM reservation), 100));
