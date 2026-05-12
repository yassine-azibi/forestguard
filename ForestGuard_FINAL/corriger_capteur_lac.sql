-- ============================================================
--  Correction complète des capteurs orphelins + ajout mesures
--  Exécuter dans phpMyAdmin → base forestguard → onglet SQL
-- ============================================================

USE forestguard;

-- ============================================================
--  ÉTAPE 1 : Corriger les foret_id et localisations
-- ============================================================

-- Capteur Lac → Forêt de Ichkeul (lac Ichkeul, Bizerte)
UPDATE capteur
SET localisation = 'Bizerte',
    foret_id     = (SELECT id FROM foret WHERE nom LIKE '%Ichkeul%' LIMIT 1)
WHERE nom = 'Capteur Lac'
  AND (SELECT id FROM foret WHERE nom LIKE '%Ichkeul%' LIMIT 1) IS NOT NULL;

-- Capteur Sud → Forêt de Kasserine (sud Tunisie)
UPDATE capteur
SET localisation = 'Kasserine',
    foret_id     = (SELECT id FROM foret WHERE nom LIKE '%Kasserine%' LIMIT 1)
WHERE nom = 'Capteur Sud'
  AND (SELECT id FROM foret WHERE nom LIKE '%Kasserine%' LIMIT 1) IS NOT NULL;

-- Capteur Nord → Forêt de Tabarka (nord Tunisie)
UPDATE capteur
SET localisation = 'Tabarka',
    foret_id     = (SELECT id FROM foret WHERE nom LIKE '%Tabarka%' LIMIT 1)
WHERE nom = 'Capteur Nord'
  AND (SELECT id FROM foret WHERE nom LIKE '%Tabarka%' LIMIT 1) IS NOT NULL;

-- Capteur Est → Forêt de Zaghouan (est Tunisie)
UPDATE capteur
SET localisation = 'Zaghouan',
    foret_id     = (SELECT id FROM foret WHERE nom LIKE '%Zaghouan%' LIMIT 1)
WHERE nom = 'Capteur Est'
  AND (SELECT id FROM foret WHERE nom LIKE '%Zaghouan%' LIMIT 1) IS NOT NULL;

-- Capteur Ouest → Forêt de Feija (ouest Jendouba)
UPDATE capteur
SET localisation = 'Jendouba',
    foret_id     = (SELECT id FROM foret WHERE nom LIKE '%Feija%' LIMIT 1)
WHERE nom = 'Capteur Ouest'
  AND (SELECT id FROM foret WHERE nom LIKE '%Feija%' LIMIT 1) IS NOT NULL;

-- Capteur Central → Forêt de Siliana (centre Tunisie)
UPDATE capteur
SET localisation = 'Siliana',
    foret_id     = (SELECT id FROM foret WHERE nom LIKE '%Siliana%' LIMIT 1)
WHERE nom = 'Capteur Central'
  AND (SELECT id FROM foret WHERE nom LIKE '%Siliana%' LIMIT 1) IS NOT NULL;

-- Tous les autres capteurs orphelins restants → Forêt de Ain Draham
UPDATE capteur c
LEFT JOIN foret f ON c.foret_id = f.id
SET c.foret_id     = (SELECT MIN(id) FROM foret),
    c.localisation = 'Jendouba'
WHERE f.id IS NULL;

-- ============================================================
--  ÉTAPE 2 : Ajouter des mesures réelles pour ces capteurs
--  (sans mesures → pas de cartes temp/humid/risque dans le popup)
-- ============================================================

-- Supprimer les anciennes mesures de ces capteurs si elles existent
DELETE FROM mesure_capteur
WHERE capteur_id IN (
    SELECT id FROM capteur
    WHERE nom IN ('Capteur Lac','Capteur Sud','Capteur Nord',
                  'Capteur Est','Capteur Ouest','Capteur Central')
);

-- Ajouter des mesures variées pour chaque capteur
INSERT INTO mesure_capteur (capteur_id, temperature, humidite, date_mesure)
SELECT id,
    CASE nom
        WHEN 'Capteur Lac'     THEN 29.5   -- FAIBLE  : humidite normal
        WHEN 'Capteur Sud'     THEN 44.0   -- MODERE  : temp attention
        WHEN 'Capteur Nord'    THEN 57.5   -- ELEVE   : temp critique
        WHEN 'Capteur Est'     THEN 31.2   -- FAIBLE  : tout normal
        WHEN 'Capteur Ouest'   THEN 42.0   -- MODERE  : temp attention
        WHEN 'Capteur Central' THEN 26.8   -- FAIBLE  : tout normal
        ELSE 30.0
    END AS temperature,
    CASE nom
        WHEN 'Capteur Lac'     THEN 82     -- MODERE  : humid attention
        WHEN 'Capteur Sud'     THEN 60     -- MODERE  : temp seule
        WHEN 'Capteur Nord'    THEN 52     -- ELEVE   : temp seule
        WHEN 'Capteur Est'     THEN 63     -- FAIBLE
        WHEN 'Capteur Ouest'   THEN 77     -- MODERE  : temp+humid
        WHEN 'Capteur Central' THEN 58     -- FAIBLE
        ELSE 60
    END AS humidite,
    NOW() AS date_mesure
FROM capteur
WHERE nom IN ('Capteur Lac','Capteur Sud','Capteur Nord',
              'Capteur Est','Capteur Ouest','Capteur Central');

-- ============================================================
--  ÉTAPE 3 : Vérification finale
-- ============================================================
SELECT c.id, c.nom, c.type, c.statut,
       f.nom        AS foret,
       f.localisation AS region,
       m.temperature, m.humidite,
       CASE
           WHEN m.temperature > 55 OR m.humidite > 90 THEN 'CRITIQUE'
           WHEN m.temperature > 40 OR m.humidite > 75 THEN 'MODERE'
           ELSE 'FAIBLE'
       END AS risque
FROM capteur c
JOIN foret f ON c.foret_id = f.id
LEFT JOIN mesure_capteur m ON m.capteur_id = c.id
WHERE c.nom IN ('Capteur Lac','Capteur Sud','Capteur Nord',
                'Capteur Est','Capteur Ouest','Capteur Central')
ORDER BY c.nom;
