-- ============================================================
--  ForestGuard – Base complète : 10 forêts + 30 capteurs
--  1 capteur temperature + 1 humidite + 1 fumee par forêt
--  Chaque capteur a une forêt ET une localisation uniques
--  Instructions :
--    1. Ouvrir phpMyAdmin → http://localhost/phpmyadmin
--    2. Sélectionner la base "forestguard"
--    3. Onglet "SQL" → coller ce script → Exécuter
-- ============================================================

USE forestguard;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE mesure_capteur;
TRUNCATE TABLE maintenance;
TRUNCATE TABLE capteur;
TRUNCATE TABLE foret;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
--  10 forêts avec coordonnées GPS bien espacées en Tunisie
-- ============================================================
INSERT INTO foret (id, nom, localisation, superficie, type_vegetation, niveau_risque, date_creation, latitude, longitude) VALUES
(1,  'Forêt de Ain Draham',  'Jendouba',  12500.0, 'chene-liege',  'eleve',  '2010-03-15', 36.7823,  8.6897),
(2,  'Forêt de Tabarka',     'Jendouba',   9800.0, 'pin maritime', 'eleve',  '2009-05-10', 36.9543,  8.7567),
(3,  'Forêt de Feija',       'Jendouba',   6800.0, 'chene-liege',  'eleve',  '2012-01-10', 36.5500,  8.3200),
(4,  'Forêt de Nefza',       'Beja',        7600.0, 'eucalyptus',  'faible', '2011-04-22', 37.0300,  9.0500),
(5,  'Forêt de Amdoun',      'Beja',        9400.0, 'pin d Alep',  'modere', '2009-09-05', 36.6800,  9.0200),
(6,  'Forêt de Ichkeul',     'Bizerte',    8900.0, 'mixte',        'eleve',  '2007-11-30', 37.1500,  9.6800),
(7,  'Forêt de Zaghouan',    'Zaghouan',   4200.0, 'pin d Alep',  'modere', '2014-02-14', 36.4027, 10.1434),
(8,  'Forêt de Siliana',     'Siliana',    6100.0, 'pin d Alep',  'faible', '2010-08-08', 36.0844,  9.3700),
(9,  'Forêt de Kasserine',   'Kasserine',  7800.0, 'pin d Alep',  'eleve',  '2006-05-25', 35.1676,  8.8365),
(10, 'Forêt de Bou Hedma',   'Gafsa',      5200.0, 'acacia',      'modere', '2008-03-12', 34.5500,  9.5200);

-- ============================================================
--  30 capteurs : 3 par forêt (temperature + humidite + fumee)
--  Chaque capteur est unique : forêt différente + type différent
-- ============================================================
INSERT INTO capteur (nom, type, localisation, statut, foret_id) VALUES
-- Forêt 1 : Ain Draham
('Capteur Ain Draham - Temp',   'temperature', 'Ain Draham',  'actif',    1),
('Capteur Ain Draham - Humid',  'humidite',    'Ain Draham',  'actif',    1),
('Capteur Ain Draham - Fumee',  'fumee',       'Ain Draham',  'en_panne', 1),
-- Forêt 2 : Tabarka
('Capteur Tabarka - Temp',      'temperature', 'Tabarka',     'actif',    2),
('Capteur Tabarka - Humid',     'humidite',    'Tabarka',     'inactif',  2),
('Capteur Tabarka - Fumee',     'fumee',       'Tabarka',     'actif',    2),
-- Forêt 3 : Feija
('Capteur Feija - Temp',        'temperature', 'Feija',       'actif',    3),
('Capteur Feija - Humid',       'humidite',    'Feija',       'actif',    3),
('Capteur Feija - Fumee',       'fumee',       'Feija',       'en_panne', 3),
-- Forêt 4 : Nefza
('Capteur Nefza - Temp',        'temperature', 'Nefza',       'actif',    4),
('Capteur Nefza - Humid',       'humidite',    'Nefza',       'actif',    4),
('Capteur Nefza - Fumee',       'fumee',       'Nefza',       'actif',    4),
-- Forêt 5 : Amdoun
('Capteur Amdoun - Temp',       'temperature', 'Amdoun',      'actif',    5),
('Capteur Amdoun - Humid',      'humidite',    'Amdoun',      'en_panne', 5),
('Capteur Amdoun - Fumee',      'fumee',       'Amdoun',      'inactif',  5),
-- Forêt 6 : Ichkeul
('Capteur Ichkeul - Temp',      'temperature', 'Ichkeul',     'actif',    6),
('Capteur Ichkeul - Humid',     'humidite',    'Ichkeul',     'actif',    6),
('Capteur Ichkeul - Fumee',     'fumee',       'Ichkeul',     'en_panne', 6),
-- Forêt 7 : Zaghouan
('Capteur Zaghouan - Temp',     'temperature', 'Zaghouan',    'actif',    7),
('Capteur Zaghouan - Humid',    'humidite',    'Zaghouan',    'actif',    7),
('Capteur Zaghouan - Fumee',    'fumee',       'Zaghouan',    'actif',    7),
-- Forêt 8 : Siliana
('Capteur Siliana - Temp',      'temperature', 'Siliana',     'actif',    8),
('Capteur Siliana - Humid',     'humidite',    'Siliana',     'en_panne', 8),
('Capteur Siliana - Fumee',     'fumee',       'Siliana',     'actif',    8),
-- Forêt 9 : Kasserine
('Capteur Kasserine - Temp',    'temperature', 'Kasserine',   'actif',    9),
('Capteur Kasserine - Humid',   'humidite',    'Kasserine',   'actif',    9),
('Capteur Kasserine - Fumee',   'fumee',       'Kasserine',   'inactif',  9),
-- Forêt 10 : Bou Hedma
('Capteur Bou Hedma - Temp',    'temperature', 'Gafsa',       'actif',    10),
('Capteur Bou Hedma - Humid',   'humidite',    'Gafsa',       'actif',    10),
('Capteur Bou Hedma - Fumee',   'fumee',       'Gafsa',       'en_panne', 10);

-- ============================================================
--  Mesures pour les 30 capteurs (scénarios variés)
--  FAIBLE : temp<40 & humid<75
--  MODERE : temp 40-55 OU humid 75-90
--  ELEVE  : temp>55 OU humid>90
--  CRITIQUE: temp>55 ET humid>90
-- ============================================================
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
-- Forêt 1 : Ain Draham
(1,  33.8, 64, NOW()),   -- FAIBLE
(2,  30.5, 82, NOW()),   -- MODERE (humid)
(3,  58.0, 50, NOW()),   -- ELEVE  (temp)
-- Forêt 2 : Tabarka
(4,  44.8, 60, NOW()),   -- MODERE (temp)
(5,  27.0, 55, NOW()),   -- FAIBLE
(6,  60.0, 91, NOW()),   -- CRITIQUE
-- Forêt 3 : Feija
(7,  25.3, 55, NOW()),   -- FAIBLE
(8,  29.0, 78, NOW()),   -- MODERE (humid)
(9,  56.8, 88, NOW()),   -- ELEVE  (temp)
-- Forêt 4 : Nefza
(10, 35.0, 70, NOW()),   -- FAIBLE
(11, 42.0, 77, NOW()),   -- MODERE (temp+humid)
(12, 28.6, 57, NOW()),   -- FAIBLE
-- Forêt 5 : Amdoun
(13, 57.5, 92, NOW()),   -- CRITIQUE
(14, 45.2, 80, NOW()),   -- MODERE
(15, 32.1, 67, NOW()),   -- FAIBLE
-- Forêt 6 : Ichkeul
(16, 26.8, 54, NOW()),   -- FAIBLE
(17, 43.2, 79, NOW()),   -- MODERE
(18, 57.1, 89, NOW()),   -- ELEVE
-- Forêt 7 : Zaghouan
(19, 24.5, 50, NOW()),   -- FAIBLE
(20, 42.3, 78, NOW()),   -- MODERE
(21, 56.5, 87, NOW()),   -- ELEVE
-- Forêt 8 : Siliana
(22, 32.0, 66, NOW()),   -- FAIBLE
(23, 57.3, 90, NOW()),   -- ELEVE
(24, 28.5, 58, NOW()),   -- FAIBLE
-- Forêt 9 : Kasserine
(25, 35.5, 69, NOW()),   -- FAIBLE
(26, 29.2, 61, NOW()),   -- FAIBLE
(27, 59.0, 92, NOW()),   -- CRITIQUE
-- Forêt 10 : Bou Hedma
(28, 38.5, 45, NOW()),   -- FAIBLE
(29, 41.2, 48, NOW()),   -- MODERE (temp)
(30, 55.0, 85, NOW());   -- ELEVE

-- ============================================================
--  10 maintenances variées
-- ============================================================
INSERT INTO maintenance (date_maintenance, type_maintenance, statut, description, capteur_id) VALUES
('2026-04-28', 'preventive',   'en_cours',  'Nettoyage capteur',          1),
('2026-04-15', 'corrective',   'terminee',  'Reparation panne',           3),
('2026-05-02', 'preventive',   'planifiee', 'Inspection capteur',         6),
('2026-03-20', 'remplacement', 'terminee',  'Remplacement capteur',       9),
('2026-05-10', 'preventive',   'planifiee', 'Verification connexions',    12),
('2026-04-01', 'corrective',   'terminee',  'Reparation cablage',         14),
('2026-05-15', 'preventive',   'planifiee', 'Recalibrage capteur',        18),
('2026-04-22', 'corrective',   'en_cours',  'Correction anomalie mesure', 23),
('2026-05-20', 'preventive',   'planifiee', 'Controle boitier',           27),
('2026-05-05', 'preventive',   'planifiee', 'Nettoyage capteur',          30);

-- ============================================================
--  Vérification
-- ============================================================
SELECT 'forets'       AS table_name, COUNT(*) AS nb FROM foret
UNION ALL
SELECT 'capteurs',     COUNT(*) FROM capteur
UNION ALL
SELECT 'maintenances', COUNT(*) FROM maintenance
UNION ALL
SELECT 'mesures',      COUNT(*) FROM mesure_capteur;

-- Aperçu des capteurs avec leur forêt
SELECT c.id, c.nom, c.type, c.statut, f.nom AS foret, f.localisation,
       m.temperature, m.humidite
FROM capteur c
JOIN foret f ON c.foret_id = f.id
LEFT JOIN mesure_capteur m ON m.capteur_id = c.id
ORDER BY f.id, c.type;
