-- ============================================================
--  ForestGuard – Nettoyage de la base de données
--  Garde : 10 capteurs + 5 maintenances + mesures associées
--  Instructions : Importer dans phpMyAdmin → Exécuter
-- ============================================================

USE forestguard;

-- Désactiver les contraintes FK temporairement
SET FOREIGN_KEY_CHECKS = 0;

-- ── 1. Supprimer TOUTES les mesures
TRUNCATE TABLE mesure_capteur;

-- ── 2. Supprimer TOUTES les maintenances
TRUNCATE TABLE maintenance;

-- ── 3. Supprimer TOUS les capteurs
TRUNCATE TABLE capteur;

-- Réactiver les contraintes FK
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
--  Réinsérer 10 capteurs propres (1 par forêt, statuts variés)
-- ============================================================
INSERT INTO capteur (nom, type, localisation, statut, foret_id) VALUES
('Capteur-AD-01', 'temperature', 'Jendouba',  'actif',    1),
('Capteur-BM-01', 'humidite',    'Jendouba',  'actif',    2),
('Capteur-FJ-01', 'fumee',       'Jendouba',  'actif',    3),
('Capteur-AM-01', 'temperature', 'Beja',      'actif',    4),
('Capteur-NF-01', 'humidite',    'Beja',      'actif',    5),
('Capteur-BB-01', 'fumee',       'Bizerte',   'actif',    6),
('Capteur-IC-01', 'temperature', 'Bizerte',   'inactif',  7),
('Capteur-ZG-01', 'humidite',    'Zaghouan',  'actif',    8),
('Capteur-SL-01', 'fumee',       'Siliana',   'en_panne', 9),
('Capteur-KS-01', 'temperature', 'Kasserine', 'actif',    10);

-- ============================================================
--  5 maintenances variées
-- ============================================================
INSERT INTO maintenance (date_maintenance, type_maintenance, statut, description, capteur_id) VALUES
('2026-04-28', 'preventive',   'en_cours',  'Nettoyage capteur',       1),
('2026-04-15', 'corrective',   'terminee',  'Reparation panne',        9),
('2026-05-02', 'preventive',   'planifiee', 'Inspection capteur',      2),
('2026-03-20', 'remplacement', 'terminee',  'Remplacement capteur',    7),
('2026-05-10', 'preventive',   'planifiee', 'Verification connexions', 4);

-- ============================================================
--  Mesures pour les capteurs ACTIFS uniquement
--  (les inactifs/en_panne n'ont pas de mesures récentes)
--
--  Scénarios de test :
--  Capteur 1 (AD-01) → NORMAL       : temp 33°C, humidité 64%
--  Capteur 2 (BM-01) → ATTENTION H  : temp 30°C, humidité 82%
--  Capteur 3 (FJ-01) → CRITIQUE T   : temp 58°C, humidité 50%
--  Capteur 4 (AM-01) → ATTENTION T  : temp 44°C, humidité 60%
--  Capteur 5 (NF-01) → CRITIQUE H   : temp 27°C, humidité 93%
--  Capteur 6 (BB-01) → CRITIQUE T+H : temp 60°C, humidité 91%
--  Capteur 8 (ZG-01) → NORMAL       : temp 25°C, humidité 55%
--  Capteur 10(KS-01) → MODÉRÉ T+H   : temp 42°C, humidité 78%
-- ============================================================
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure) VALUES
(1,  33.80, 64, NOW()),   -- NORMAL       → score ~0  → FAIBLE
(2,  30.50, 82, NOW()),   -- ATTENTION H  → score ~18 → FAIBLE/MODÉRÉ
(3,  58.00, 50, NOW()),   -- CRITIQUE T   → score ~60 → ÉLEVÉ
(4,  44.80, 60, NOW()),   -- ATTENTION T  → score ~15 → MODÉRÉ
(5,  27.00, 93, NOW()),   -- CRITIQUE H   → score ~45 → ÉLEVÉ
(6,  60.00, 91, NOW()),   -- CRITIQUE T+H → score ~100→ CRITIQUE
(8,  25.30, 55, NOW()),   -- NORMAL       → score ~0  → FAIBLE
(10, 42.00, 78, NOW());   -- MODÉRÉ T+H   → score ~30 → MODÉRÉ

-- ============================================================
--  Vérification finale
-- ============================================================
SELECT 'capteurs'     AS table_name, COUNT(*) AS nb FROM capteur
UNION ALL
SELECT 'maintenances', COUNT(*) FROM maintenance
UNION ALL
SELECT 'mesures',      COUNT(*) FROM mesure_capteur;

SELECT c.id, c.nom, c.type, c.statut, m.temperature, m.humidite,
    CASE
        WHEN m.temperature > 55 OR m.humidite > 90 THEN 'CRITIQUE'
        WHEN m.temperature > 40 OR m.humidite > 75 THEN 'ATTENTION'
        ELSE 'NORMAL'
    END AS risque
FROM capteur c
LEFT JOIN mesure_capteur m ON m.capteur_id = c.id
ORDER BY c.id;
