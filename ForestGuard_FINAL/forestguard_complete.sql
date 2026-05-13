-- ============================================================
--  ForestGuard – Base de données complète
--  Instructions :
--    1. Ouvrir phpMyAdmin (WAMP) → http://localhost/phpmyadmin
--    2. Onglet "Importer" → choisir ce fichier → Exécuter
-- ============================================================

DROP DATABASE IF EXISTS forestguard;
CREATE DATABASE forestguard CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE forestguard;

-- ============================================================
--  TABLE foret
-- ============================================================
CREATE TABLE foret (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    nom             VARCHAR(100)  NOT NULL,
    localisation    VARCHAR(100)  NOT NULL,
    superficie      DOUBLE        NOT NULL DEFAULT 0,
    type_vegetation VARCHAR(50)   NOT NULL DEFAULT 'mixte',
    niveau_risque   VARCHAR(20)   NOT NULL DEFAULT 'faible',
    date_creation   DATE          NOT NULL,
    latitude        DOUBLE        NOT NULL DEFAULT 0,
    longitude       DOUBLE        NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  TABLE capteur
-- ============================================================
CREATE TABLE capteur (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    nom          VARCHAR(100) NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    localisation VARCHAR(150) NOT NULL,
    statut       VARCHAR(30)  NOT NULL DEFAULT 'actif',
    foret_id     INT          NOT NULL,
    CONSTRAINT fk_capteur_foret
        FOREIGN KEY (foret_id) REFERENCES foret(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  TABLE maintenance
-- ============================================================
CREATE TABLE maintenance (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    date_maintenance DATE         NOT NULL,
    type_maintenance VARCHAR(50)  NOT NULL,
    statut           VARCHAR(30)  NOT NULL DEFAULT 'planifiee',
    description      VARCHAR(255),
    capteur_id       INT          NOT NULL,
    CONSTRAINT fk_maintenance_capteur
        FOREIGN KEY (capteur_id) REFERENCES capteur(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  TABLE mesure_capteur
--  Seuils de panne :
--    Temperature : normale < 40C | attention 40-55C | critique > 55C
--    Humidite    : normale < 75% | attention 75-90% | critique > 90%
-- ============================================================
CREATE TABLE mesure_capteur (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    capteur_id  INT           NOT NULL,
    temperature DECIMAL(5,2)  NOT NULL COMMENT 'Temperature ambiante en degres Celsius',
    humidite    INT           NOT NULL COMMENT 'Humidite relative en pourcentage',
    date_mesure DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mesure_capteur
        FOREIGN KEY (capteur_id) REFERENCES capteur(id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  DONNEES : 10 forets tunisiennes
-- ============================================================
INSERT INTO foret (nom, localisation, superficie, type_vegetation, niveau_risque, date_creation, latitude, longitude) VALUES
('Foret de Ain Draham',  'Jendouba',  12500.0, 'chene-liege',  'eleve',  '2010-03-15', 36.7823,  8.6897),
('Foret de Beni Mtir',   'Jendouba',   8200.0, 'pin maritime', 'modere', '2008-06-20', 36.7200,  8.7500),
('Foret de Feija',       'Jendouba',   6800.0, 'chene-liege',  'eleve',  '2012-01-10', 36.5500,  8.3200),
('Foret de Amdoun',      'Beja',        9400.0, 'pin d Alep',  'modere', '2009-09-05', 36.6800,  9.0200),
('Foret de Nefza',       'Beja',        7600.0, 'eucalyptus',  'faible', '2011-04-22', 37.0300,  9.0500),
('Foret de Babouch',     'Bizerte',    5300.0, 'pin maritime', 'modere', '2013-07-18', 37.1200,  9.5400),
('Foret de Ichkeul',     'Bizerte',    8900.0, 'mixte',        'eleve',  '2007-11-30', 37.1500,  9.6800),
('Foret de Zaghouan',    'Zaghouan',   4200.0, 'pin d Alep',  'modere', '2014-02-14', 36.4027, 10.1434),
('Foret de Siliana',     'Siliana',    6100.0, 'pin d Alep',  'faible', '2010-08-08', 36.0844,  9.3700),
('Foret de Kasserine',   'Kasserine',  7800.0, 'pin d Alep',  'eleve',  '2006-05-25', 35.1676,  8.8365);

-- ============================================================
--  DONNEES : 30 capteurs (3 par foret)
-- ============================================================
INSERT INTO capteur (nom, type, localisation, statut, foret_id) VALUES
-- Foret Ain Draham (1)
('Capteur-AD-01', 'temperature', 'Jendouba',  'actif',    1),
('Capteur-AD-02', 'humidite',    'Jendouba',  'actif',    1),
('Capteur-AD-03', 'fumee',       'Jendouba',  'en_panne', 1),
-- Foret Beni Mtir (2)
('Capteur-BM-01', 'temperature', 'Jendouba',  'actif',    2),
('Capteur-BM-02', 'humidite',    'Jendouba',  'inactif',  2),
('Capteur-BM-03', 'fumee',       'Jendouba',  'actif',    2),
-- Foret Feija (3)
('Capteur-FJ-01', 'temperature', 'Jendouba',  'actif',    3),
('Capteur-FJ-02', 'humidite',    'Jendouba',  'actif',    3),
('Capteur-FJ-03', 'fumee',       'Jendouba',  'en_panne', 3),
-- Foret Amdoun (4)
('Capteur-AM-01', 'temperature', 'Beja',      'actif',    4),
('Capteur-AM-02', 'humidite',    'Beja',      'actif',    4),
('Capteur-AM-03', 'fumee',       'Beja',      'inactif',  4),
-- Foret Nefza (5)
('Capteur-NF-01', 'temperature', 'Beja',      'actif',    5),
('Capteur-NF-02', 'humidite',    'Beja',      'actif',    5),
('Capteur-NF-03', 'fumee',       'Beja',      'actif',    5),
-- Foret Babouch (6)
('Capteur-BB-01', 'temperature', 'Bizerte',   'actif',    6),
('Capteur-BB-02', 'humidite',    'Bizerte',   'en_panne', 6),
('Capteur-BB-03', 'fumee',       'Bizerte',   'actif',    6),
-- Foret Ichkeul (7)
('Capteur-IC-01', 'temperature', 'Bizerte',   'actif',    7),
('Capteur-IC-02', 'humidite',    'Bizerte',   'actif',    7),
('Capteur-IC-03', 'fumee',       'Bizerte',   'inactif',  7),
-- Foret Zaghouan (8)
('Capteur-ZG-01', 'temperature', 'Zaghouan',  'actif',    8),
('Capteur-ZG-02', 'humidite',    'Zaghouan',  'actif',    8),
('Capteur-ZG-03', 'fumee',       'Zaghouan',  'actif',    8),
-- Foret Siliana (9)
('Capteur-SL-01', 'temperature', 'Siliana',   'actif',    9),
('Capteur-SL-02', 'humidite',    'Siliana',   'en_panne', 9),
('Capteur-SL-03', 'fumee',       'Siliana',   'actif',    9),
-- Foret Kasserine (10)
('Capteur-KS-01', 'temperature', 'Kasserine', 'actif',    10),
('Capteur-KS-02', 'humidite',    'Kasserine', 'actif',    10),
('Capteur-KS-03', 'fumee',       'Kasserine', 'inactif',  10);

-- ============================================================
--  DONNEES : 15 maintenances
-- ============================================================
INSERT INTO maintenance (date_maintenance, type_maintenance, statut, description, capteur_id) VALUES
('2026-04-28', 'preventive',   'en_cours',  'Nettoyage capteur',          1),
('2026-04-15', 'corrective',   'terminee',  'Reparation panne',           3),
('2026-05-02', 'preventive',   'planifiee', 'Inspection capteur',         2),
('2026-03-20', 'remplacement', 'terminee',  'Remplacement capteur',       5),
('2026-05-10', 'preventive',   'planifiee', 'Verification connexions',    4),
('2026-04-01', 'corrective',   'terminee',  'Reparation cablage',         9),
('2026-05-15', 'preventive',   'planifiee', 'Recalibrage capteur',        7),
('2026-04-22', 'corrective',   'en_cours',  'Correction anomalie mesure', 17),
('2026-05-20', 'preventive',   'planifiee', 'Controle boitier',           10),
('2026-03-10', 'remplacement', 'terminee',  'Remplacement module',        26),
('2026-05-25', 'preventive',   'planifiee', 'Test fonctionnement',        13),
('2026-04-18', 'corrective',   'terminee',  'Remise en service',          12),
('2026-06-01', 'preventive',   'planifiee', 'Inspection capteur',         19),
('2026-04-30', 'corrective',   'en_cours',  'Reparation panne',           25),
('2026-05-05', 'preventive',   'planifiee', 'Nettoyage capteur',          22);

-- ============================================================
--  DONNEES : mesures temperature/humidite
--
--  Scenarios de test :
--  NORMAL    : temp < 40C  ET humidite < 75%
--  ATTENTION : temp 40-55C OU humidite 75-90%
--  CRITIQUE  : temp > 55C  OU humidite > 90%
-- ============================================================

-- Capteur 1 - NORMAL (temp 28-35, humidite 61-68)
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(1, 28.50, 62, '2026-05-11 08:00:00'),
(1, 31.20, 65, '2026-05-11 10:00:00'),
(1, 33.80, 68, '2026-05-11 12:00:00'),
(1, 35.10, 64, '2026-05-11 14:00:00'),
(1, 30.40, 61, '2026-05-11 16:00:00');

-- Capteur 2 - ATTENTION humidite (humidite 77-85)
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(2, 29.00, 78, '2026-05-11 08:00:00'),
(2, 30.50, 82, '2026-05-11 10:00:00'),
(2, 32.10, 85, '2026-05-11 12:00:00'),
(2, 31.80, 80, '2026-05-11 14:00:00'),
(2, 28.90, 77, '2026-05-11 16:00:00');

-- Capteur 3 - CRITIQUE temperature (temp 48-61, EN PANNE)
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(3, 48.20, 55, '2026-05-11 08:00:00'),
(3, 52.70, 52, '2026-05-11 10:00:00'),
(3, 57.30, 48, '2026-05-11 12:00:00'),
(3, 61.00, 45, '2026-05-11 14:00:00'),
(3, 58.50, 47, '2026-05-11 16:00:00');

-- Capteur 4 - ATTENTION temperature (temp 41-47)
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(4, 41.30, 58, '2026-05-11 08:00:00'),
(4, 44.80, 60, '2026-05-11 10:00:00'),
(4, 47.20, 57, '2026-05-11 12:00:00'),
(4, 45.90, 55, '2026-05-11 14:00:00'),
(4, 42.10, 59, '2026-05-11 16:00:00');

-- Capteur 5 - CRITIQUE humidite (humidite 90-95, INACTIF)
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(5, 27.50, 91, '2026-05-11 08:00:00'),
(5, 26.80, 93, '2026-05-11 10:00:00'),
(5, 25.90, 95, '2026-05-11 12:00:00'),
(5, 26.20, 92, '2026-05-11 14:00:00'),
(5, 27.10, 90, '2026-05-11 16:00:00');

-- Capteur 6 - NORMAL
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(6, 26.00, 55, '2026-05-11 08:00:00'),
(6, 28.30, 58, '2026-05-11 10:00:00'),
(6, 30.10, 60, '2026-05-11 12:00:00'),
(6, 29.50, 57, '2026-05-11 14:00:00'),
(6, 27.20, 54, '2026-05-11 16:00:00');

-- Capteur 7 - CRITIQUE temperature ET humidite (temp 55-62, humidite 87-93)
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(7, 55.10, 88, '2026-05-11 08:00:00'),
(7, 58.40, 91, '2026-05-11 10:00:00'),
(7, 62.00, 93, '2026-05-11 12:00:00'),
(7, 59.70, 89, '2026-05-11 14:00:00'),
(7, 56.30, 87, '2026-05-11 16:00:00');

-- Capteur 8 - NORMAL
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(8, 24.50, 50, '2026-05-11 08:00:00'),
(8, 26.10, 53, '2026-05-11 10:00:00'),
(8, 27.80, 55, '2026-05-11 12:00:00'),
(8, 27.00, 52, '2026-05-11 14:00:00'),
(8, 25.30, 49, '2026-05-11 16:00:00');

-- Capteur 9 - CRITIQUE humidite (humidite 92-96, EN PANNE)
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(9, 32.00, 92, '2026-05-11 08:00:00'),
(9, 33.50, 94, '2026-05-11 10:00:00'),
(9, 34.10, 96, '2026-05-11 12:00:00'),
(9, 33.80, 95, '2026-05-11 14:00:00'),
(9, 32.70, 93, '2026-05-11 16:00:00');

-- Capteurs 10 a 30 : une mesure chacun
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(10, 30.20, 60, '2026-05-11 12:00:00'),
(11, 28.70, 58, '2026-05-11 12:00:00'),
(12, 31.50, 63, '2026-05-11 12:00:00'),
(13, 29.80, 61, '2026-05-11 12:00:00'),
(14, 27.30, 55, '2026-05-11 12:00:00'),
(15, 32.10, 64, '2026-05-11 12:00:00'),
(16, 33.40, 66, '2026-05-11 12:00:00'),
(17, 43.20, 79, '2026-05-11 12:00:00'),
(18, 28.90, 57, '2026-05-11 12:00:00'),
(19, 30.50, 62, '2026-05-11 12:00:00'),
(20, 29.10, 59, '2026-05-11 12:00:00'),
(21, 31.80, 65, '2026-05-11 12:00:00'),
(22, 27.60, 53, '2026-05-11 12:00:00'),
(23, 34.20, 67, '2026-05-11 12:00:00'),
(24, 28.40, 56, '2026-05-11 12:00:00'),
(25, 56.80, 88, '2026-05-11 12:00:00'),
(26, 29.70, 60, '2026-05-11 12:00:00'),
(27, 31.20, 63, '2026-05-11 12:00:00'),
(28, 30.80, 61, '2026-05-11 12:00:00'),
(29, 32.50, 65, '2026-05-11 12:00:00'),
(30, 28.10, 54, '2026-05-11 12:00:00');

-- ============================================================
--  VUE : etat des capteurs avec derniere mesure et risque
-- ============================================================
CREATE OR REPLACE VIEW v_capteurs_mesures AS
SELECT
    c.id,
    c.nom,
    c.type,
    c.statut,
    c.localisation,
    f.nom                AS foret,
    m.temperature,
    m.humidite,
    m.date_mesure,
    CASE
        WHEN m.temperature > 55 OR m.humidite > 90 THEN 'CRITIQUE'
        WHEN m.temperature > 40 OR m.humidite > 75 THEN 'ATTENTION'
        ELSE 'NORMAL'
    END                  AS risque_panne
FROM capteur c
JOIN foret f ON c.foret_id = f.id
LEFT JOIN mesure_capteur m ON m.capteur_id = c.id
    AND m.date_mesure = (
        SELECT MAX(date_mesure)
        FROM mesure_capteur
        WHERE capteur_id = c.id
    )
ORDER BY c.id;

-- ============================================================
--  VERIFICATION
-- ============================================================
SELECT 'forets'      AS table_name, COUNT(*) AS nb FROM foret
UNION ALL
SELECT 'capteurs',     COUNT(*) FROM capteur
UNION ALL
SELECT 'maintenances', COUNT(*) FROM maintenance
UNION ALL
SELECT 'mesures',      COUNT(*) FROM mesure_capteur;

SELECT nom, type, statut, temperature, humidite, risque_panne
FROM v_capteurs_mesures
ORDER BY risque_panne DESC, id;
