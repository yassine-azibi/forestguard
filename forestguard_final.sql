-- ══════════════════════════════════════════════════════════════════════════════
--  forestguard_final.sql — Version fusionnée complète
--  Garde TOUTES les colonnes des deux versions
-- ══════════════════════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS forestguard
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE forestguard;

-- ── 1. UTILISATEURS ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS utilisateur (
    id                 INT           NOT NULL AUTO_INCREMENT,
    nom                VARCHAR(100)  NOT NULL,
    email              VARCHAR(150)  NOT NULL,
    telephone          VARCHAR(30)   NOT NULL DEFAULT '',
    localisation       VARCHAR(150)  NOT NULL DEFAULT '',
    password_hash      VARCHAR(255)  NOT NULL DEFAULT '',
    google_id          VARCHAR(255)      DEFAULT NULL,
    google_picture_url VARCHAR(512)      DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_email     (email),
    UNIQUE KEY uq_google_id (google_id(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO utilisateur (nom, email, telephone, localisation, password_hash) VALUES
('Admin ForestGuard', 'ybrinis6@gmail.com',      '+21624597114', 'Tunis',    'hash1'),
('Chef Surveillance', 'mboukhriss34@gmail.com',  '+21653900510', 'Bizerte',  'hash2'),
('Operateur Nord',    'operateur@forestguard.tn','+21655000001', 'Jendouba', 'hash3');

-- ── 2. FORÊTS ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS foret (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(255) NOT NULL,
    localisation    VARCHAR(255),
    superficie      DOUBLE,
    type_vegetation VARCHAR(100),
    niveau_risque   VARCHAR(50),
    date_creation   DATE,
    latitude        DOUBLE,
    longitude       DOUBLE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO foret (nom, localisation, superficie, type_vegetation, niveau_risque, date_creation, latitude, longitude) VALUES
('Foret de Kroumirie',  'Jendouba', 87000, 'Chene-liege', 'Eleve',   '2020-01-01', 36.8065,  8.7636),
('Foret de Ain Draham', 'Jendouba', 45000, 'Chene',       'Moyen',   '2020-01-01', 36.7833,  8.6833),
('Foret du Cap Bon',    'Nabeul',   12000, 'Pin',         'Eleve',   '2020-01-01', 36.8000, 10.8330),
('Foret de Tabarka',    'Jendouba', 32000, 'Chene-liege', 'Critique','2020-01-01', 36.9544,  8.7576),
('Foret de Beja',       'Beja',     21000, 'Eucalyptus',  'Moyen',   '2020-01-01', 36.7256,  9.1817);

-- ── 3. CAPTEURS IoT ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS donnee_capteur (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(50)  NOT NULL DEFAULT 'Automatique',
    capteur     VARCHAR(50)  NOT NULL,
    zone        VARCHAR(100) NOT NULL,
    temperature DOUBLE       NOT NULL DEFAULT 0,
    humidite    DOUBLE       NOT NULL DEFAULT 0,
    fumee       DOUBLE       NOT NULL DEFAULT 0,
    horodatage  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    risque      VARCHAR(20)  DEFAULT 'Sur'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO donnee_capteur (type, capteur, zone, temperature, humidite, fumee, risque) VALUES
('Automatique', 'C-001', 'Zone Nord-A, Cap Bon',  28.5, 54.2,  5.1, 'Sur'),
('Automatique', 'C-002', 'Zone Est-B, Kroumirie', 31.2, 47.8,  8.3, 'Sur'),
('Automatique', 'C-003', 'Zone Sud-C, Jendouba',  25.1, 62.0,  3.2, 'Sur');

-- ── 4. ALERTE — VERSION FUSIONNÉE (garde id_capteur + source) ────────────────
CREATE TABLE IF NOT EXISTS alerte (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    type_alerte  VARCHAR(50)  NOT NULL,
    niveau       VARCHAR(20)  NOT NULL,
    localisation VARCHAR(150) NOT NULL,
    date_alerte  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut       VARCHAR(20)  NOT NULL DEFAULT 'Nouvelle',
    source       VARCHAR(50)  NOT NULL DEFAULT 'Manuelle',
    id_capteur   INT              DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ajouter les colonnes manquantes si la table existe deja
ALTER TABLE alerte ADD COLUMN IF NOT EXISTS source     VARCHAR(50) NOT NULL DEFAULT 'Manuelle';
ALTER TABLE alerte ADD COLUMN IF NOT EXISTS id_capteur INT DEFAULT NULL;

INSERT IGNORE INTO alerte (type_alerte, niveau, localisation, statut, source) VALUES
('Incendie',          'Critique', 'Zone Nord-A, Cap Bon [36.80000, 10.83300]',  'Nouvelle', 'C-001'),
('Fumee',             'Haute',    'Zone Est-B, Kroumirie [36.80650, 8.76360]',   'Validee',  'C-002'),
('Chaleur excessive', 'Moyenne',  'Zone Sud-C, Jendouba [36.72560, 9.18170]',   'Rejetee',  'Manuelle'),
('Incendie',          'Critique', 'Zone Nord-B, Bizerte',                        'Nouvelle', 'Manuelle'),
('Fumee',             'Haute',    'Zone Ouest-A, Ain Draham',                    'Nouvelle', 'Manuelle');

-- ── 5. FIRE_ALERTS ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS fire_alerts (
    id                INT           NOT NULL AUTO_INCREMENT,
    zone              VARCHAR(150)  NOT NULL,
    specific_location VARCHAR(255)      DEFAULT NULL,
    details           TEXT          NOT NULL,
    photo_path        VARCHAR(255)      DEFAULT NULL,
    reporter_name     VARCHAR(150)      DEFAULT NULL,
    reporter_email    VARCHAR(150)      DEFAULT NULL,
    alert_level       VARCHAR(20)   NOT NULL DEFAULT 'Low',
    status            VARCHAR(20)   NOT NULL DEFAULT 'Active',
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO fire_alerts (zone, specific_location, details, reporter_name, reporter_email, alert_level, status) VALUES
('Cap Bon',   'Foret Nord secteur A', 'Fumee epaisse detectee, vent fort',  'Mohamed Ali',   'ybrinis6@gmail.com',     'High',   'Active'),
('Kroumirie', 'Secteur B est',        'Temperature anormalement elevee',     'Sami Trabelsi', 'mboukhriss34@gmail.com', 'Medium', 'Active'),
('Jendouba',  'Zone centrale',        'Secheresse prolongee, risque eleve', 'Leila Ben',     'operateur@forestguard.tn','Low',   'Resolved');

-- ── 6. POMPIERS ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pompier (
    id           INT          NOT NULL AUTO_INCREMENT,
    nom          VARCHAR(100) NOT NULL,
    prenom       VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    telephone    VARCHAR(20)  NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    statut       ENUM('disponible','en_mission') NOT NULL DEFAULT 'disponible',
    zone_id      INT              DEFAULT NULL,
    ville        VARCHAR(100)     DEFAULT NULL,
    zone_adresse VARCHAR(150)     DEFAULT NULL,
    latitude     DECIMAL(10,7)    DEFAULT NULL,
    longitude    DECIMAL(10,7)    DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO pompier (nom, prenom, email, telephone, mot_de_passe, statut, ville, latitude, longitude) VALUES
('Ben Ali',  'Mohamed', 'ybrinis6@gmail.com', '+21624597114', 'hash1', 'disponible', 'Tunis',    36.8190, 10.1658),
('Trabelsi', 'Ahmed',   'mboukhriss34@gmail.com', '+21653900510', 'hash2', 'disponible', 'Bizerte',  37.2744,  9.8739),
('Chaabane', 'Sami',    'pompier3@forestguard.tn', '+21698001003', 'hash3', 'en_mission', 'Jendouba', 36.5011,  8.7803),
('Mansouri', 'Khaled',  'pompier4@forestguard.tn', '+21698001004', 'hash4', 'disponible', 'Nabeul',   36.4560, 10.7376),
('Gharbi',   'Yassine', 'pompier5@forestguard.tn', '+21698001005', 'hash5', 'disponible', 'Tabarka',  36.9544,  8.7576);

-- ── 7. AFFECTATIONS ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS affectation (
    id               INT      NOT NULL AUTO_INCREMENT,
    id_alerte        INT      NOT NULL,
    id_pompier       INT      NOT NULL,
    date_affectation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut           ENUM('en_cours','annule') NOT NULL DEFAULT 'en_cours',
    score_selection  DOUBLE       DEFAULT NULL,
    distance_km      DOUBLE       DEFAULT NULL,
    mode_affectation ENUM('automatique','annule') NOT NULL DEFAULT 'automatique',
    PRIMARY KEY (id),
    FOREIGN KEY (id_pompier) REFERENCES pompier(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

