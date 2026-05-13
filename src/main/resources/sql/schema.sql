CREATE TABLE IF NOT EXISTS utilisateur (
    id INT NOT NULL AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telephone VARCHAR(30) NOT NULL,
    localisation VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL DEFAULT '',
    google_id VARCHAR(255) DEFAULT NULL,
    google_picture_url VARCHAR(512) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_utilisateur_email (email),
    UNIQUE KEY uq_utilisateur_google_id (google_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fire_alerts (
    id INT NOT NULL AUTO_INCREMENT,
    zone VARCHAR(150) NOT NULL,
    specific_location VARCHAR(255) DEFAULT NULL,
    details TEXT NOT NULL,
    photo_path VARCHAR(255) DEFAULT NULL,
    reporter_name VARCHAR(150) DEFAULT NULL,
    reporter_email VARCHAR(150) DEFAULT NULL,
    alert_level VARCHAR(20) NOT NULL DEFAULT 'Low',
    status VARCHAR(20) NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_fire_alerts_zone (zone),
    INDEX idx_fire_alerts_status (status),
    INDEX idx_fire_alerts_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Événements Forestiers ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS evenement_forestier (
    id INT NOT NULL AUTO_INCREMENT,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    foret VARCHAR(255) NOT NULL,
    date_evenement DATE NOT NULL,
    capacite INT NOT NULL DEFAULT 0,
    places_restantes INT NOT NULL DEFAULT 0,
    type_evenement VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_evenement_date (date_evenement),
    INDEX idx_evenement_type (type_evenement)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inscription_evenement (
    id INT NOT NULL AUTO_INCREMENT,
    evenement_id INT NOT NULL,
    nom_responsable VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    nb_participants INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_inscription_evenement_eid (evenement_id),
    CONSTRAINT fk_inscription_evenement_event FOREIGN KEY (evenement_id)
        REFERENCES evenement_forestier(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS avis_evenement (
    id INT NOT NULL AUTO_INCREMENT,
    evenement_id INT NOT NULL,
    nom_auteur VARCHAR(150) NOT NULL,
    note TINYINT NOT NULL,
    commentaire TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_avis_evenement_eid (evenement_id),
    CONSTRAINT fk_avis_evenement_event FOREIGN KEY (evenement_id)
        REFERENCES evenement_forestier(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Exemples d'événements
INSERT INTO evenement_forestier (titre, description, foret, date_evenement, capacite, places_restantes, type_evenement)
VALUES
('Randonnée Rimel', 'Randonnée guidée dans la Forêt de Rimel, niveau facile.', 'Forêt de Rimel', DATE_ADD(CURDATE(), INTERVAL 7 DAY), 30, 30, 'Randonnée'),
('Atelier Reboisement Aïn Draham', 'Atelier participatif de reboisement.', 'Aïn Draham', DATE_ADD(CURDATE(), INTERVAL 14 DAY), 50, 50, 'Reboisement'),
('Sensibilisation Siliana', 'Conférence et sensibilisation sur la prévention des incendies.', 'Siliana', DATE_ADD(CURDATE(), INTERVAL 21 DAY), 200, 200, 'Sensibilisation');