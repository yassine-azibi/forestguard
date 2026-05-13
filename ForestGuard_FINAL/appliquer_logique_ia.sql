-- ============================================================
--  ForestGuard – Appliquer la logique IA sur les capteurs actifs
--  Couvre les 7 cas de la logique validée :
--
--  CAS 1 : Temp > 55°C ET Humidité > 90% → CRITIQUE  (score ~100)
--  CAS 2 : Temp > 55°C seule             → ÉLEVÉ     (score ~60)
--  CAS 3 : Humidité > 90% seule          → ÉLEVÉ     (score ~45)
--  CAS 4 : Temp 40-55°C ET Humidité 75-90% → MODÉRÉ  (score ~30)
--  CAS 5 : Temp 40-55°C seule            → MODÉRÉ    (score ~10)
--  CAS 6 : Humidité 75-90% seule         → MODÉRÉ    (score ~12)
--  CAS 7 : Tout normal                   → FAIBLE    (score ~0)
--
--  Instructions : Exécuter dans phpMyAdmin → onglet SQL
-- ============================================================

USE forestguard;

-- Supprimer les anciennes mesures des capteurs actifs
DELETE FROM mesure_capteur
WHERE capteur_id IN (
    SELECT id FROM capteur WHERE statut = 'actif'
);

-- ============================================================
--  Insérer les nouvelles mesures selon la logique validée
--  en utilisant les IDs réels de tes capteurs actifs
-- ============================================================

-- Récupérer les IDs des capteurs actifs dans l'ordre
SET @ids = (SELECT GROUP_CONCAT(id ORDER BY id) FROM capteur WHERE statut = 'actif');

-- Insérer une mesure par capteur actif selon son rang
-- Rang 1 → CAS 7 : FAIBLE    (temp 33°C, humidité 64%)
-- Rang 2 → CAS 6 : MODÉRÉ H  (temp 30°C, humidité 82%)
-- Rang 3 → CAS 2 : ÉLEVÉ T   (temp 58°C, humidité 50%)
-- Rang 4 → CAS 5 : MODÉRÉ T  (temp 44°C, humidité 60%)
-- Rang 5 → CAS 3 : ÉLEVÉ H   (temp 27°C, humidité 93%)
-- Rang 6 → CAS 1 : CRITIQUE  (temp 60°C, humidité 91%)
-- Rang 7 → CAS 7 : FAIBLE    (temp 25°C, humidité 55%)
-- Rang 8 → CAS 4 : MODÉRÉ T+H(temp 42°C, humidité 78%)
-- Rang 9+ → CAS 7 : FAIBLE   (temp 29°C, humidité 60%)

INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure)
SELECT
    c.id,
    CASE c.rang
        WHEN 1 THEN 33.80   -- FAIBLE    : temp normale
        WHEN 2 THEN 30.50   -- MODÉRÉ H  : humidité attention
        WHEN 3 THEN 58.00   -- ÉLEVÉ T   : temp critique
        WHEN 4 THEN 44.80   -- MODÉRÉ T  : temp attention
        WHEN 5 THEN 27.00   -- ÉLEVÉ H   : humidité critique
        WHEN 6 THEN 60.00   -- CRITIQUE  : temp+humidité critiques
        WHEN 7 THEN 25.30   -- FAIBLE    : tout normal
        WHEN 8 THEN 42.00   -- MODÉRÉ T+H: double attention
        ELSE        29.00   -- FAIBLE    : capteurs supplémentaires
    END AS temperature,
    CASE c.rang
        WHEN 1 THEN 64      -- FAIBLE
        WHEN 2 THEN 82      -- MODÉRÉ H
        WHEN 3 THEN 50      -- ÉLEVÉ T
        WHEN 4 THEN 60      -- MODÉRÉ T
        WHEN 5 THEN 93      -- ÉLEVÉ H
        WHEN 6 THEN 91      -- CRITIQUE
        WHEN 7 THEN 55      -- FAIBLE
        WHEN 8 THEN 78      -- MODÉRÉ T+H
        ELSE        62      -- FAIBLE
    END AS humidite,
    NOW() AS date_mesure
FROM (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rang
    FROM capteur
    WHERE statut = 'actif'
) c;

-- ============================================================
--  Vérification : afficher le résultat avec le niveau prédit
-- ============================================================
SELECT
    c.id,
    c.nom,
    c.type,
    c.statut,
    m.temperature,
    m.humidite,
    CASE
        WHEN m.temperature > 55 AND m.humidite > 90
            THEN CONCAT('CRITIQUE (score ~100) — CAS 1 : Temp+Humidité critiques')
        WHEN m.temperature > 55
            THEN CONCAT('ÉLEVÉ    (score ~60)  — CAS 2 : Temp critique seule')
        WHEN m.humidite > 90
            THEN CONCAT('ÉLEVÉ    (score ~45)  — CAS 3 : Humidité critique seule')
        WHEN m.temperature > 40 AND m.humidite > 75
            THEN CONCAT('MODÉRÉ   (score ~30)  — CAS 4 : Temp+Humidité attention')
        WHEN m.temperature > 40
            THEN CONCAT('MODÉRÉ   (score ~10)  — CAS 5 : Temp attention seule')
        WHEN m.humidite > 75
            THEN CONCAT('MODÉRÉ   (score ~12)  — CAS 6 : Humidité attention seule')
        ELSE
            CONCAT('FAIBLE   (score ~0)   — CAS 7 : Conditions normales')
    END AS prediction_ia
FROM capteur c
JOIN mesure_capteur m ON m.capteur_id = c.id
WHERE c.statut = 'actif'
ORDER BY c.id;
