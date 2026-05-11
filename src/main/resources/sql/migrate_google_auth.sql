-- Migration : ajout des colonnes Google OAuth2
-- À exécuter une seule fois sur la base de données existante.
-- Les colonnes sont ajoutées uniquement si elles n'existent pas déjà.

ALTER TABLE utilisateur
    MODIFY COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) DEFAULT NULL AFTER password_hash,
    ADD COLUMN IF NOT EXISTS google_picture_url VARCHAR(512) DEFAULT NULL AFTER google_id;

-- Index unique sur google_id (évite les doublons de compte Google)
CREATE UNIQUE INDEX IF NOT EXISTS uq_utilisateur_google_id
    ON utilisateur (google_id);
