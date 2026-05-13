-- ============================================================
--  ForestGuard – Réinitialisation des maintenances
--  Supprime toutes les maintenances existantes et en ajoute
--  15 nouvelles cohérentes avec les capteurs de la BD.
--
--  Types   : preventive | corrective | remplacement
--  Statuts : planifiee  | en_cours   | terminee
--  Descriptions : issues de DescriptionsMaintenance.java
--
--  Instructions : phpMyAdmin → base forestguard → onglet SQL
-- ============================================================

USE forestguard;

-- 1. Supprimer toutes les maintenances existantes
TRUNCATE TABLE maintenance;

-- 2. Vérifier les IDs des capteurs disponibles
-- SELECT id, nom, type, statut FROM capteur ORDER BY id;

-- 3. Insérer 15 maintenances variées sur les capteurs existants
--    (les capteur_id utilisés doivent exister dans ta table capteur)
INSERT INTO maintenance (date_maintenance, type_maintenance, statut, description, capteur_id)
SELECT
    m.date_maintenance,
    m.type_maintenance,
    m.statut,
    m.description,
    c.id AS capteur_id
FROM (
    SELECT 1 AS rang, '2026-05-01' AS date_maintenance, 'preventive'   AS type_maintenance, 'terminee'  AS statut, 'Inspection capteur'               AS description UNION ALL
    SELECT 2,         '2026-05-03',                     'corrective',                        'terminee',            'Réparation panne'                              UNION ALL
    SELECT 3,         '2026-05-05',                     'preventive',                        'en_cours',            'Nettoyage capteur'                             UNION ALL
    SELECT 4,         '2026-05-07',                     'remplacement',                      'terminee',            'Remplacement capteur'                          UNION ALL
    SELECT 5,         '2026-05-10',                     'preventive',                        'planifiee',           'Vérification connexions'                       UNION ALL
    SELECT 6,         '2026-05-12',                     'corrective',                        'terminee',            'Réparation câblage'                            UNION ALL
    SELECT 7,         '2026-05-14',                     'preventive',                        'planifiee',           'Recalibrage capteur'                           UNION ALL
    SELECT 8,         '2026-05-16',                     'corrective',                        'en_cours',            'Correction anomalie mesure'                    UNION ALL
    SELECT 9,         '2026-05-18',                     'preventive',                        'planifiee',           'Contrôle boîtier'                              UNION ALL
    SELECT 10,        '2026-05-20',                     'remplacement',                      'terminee',            'Remplacement module'                           UNION ALL
    SELECT 11,        '2026-05-22',                     'preventive',                        'planifiee',           'Test fonctionnement'                           UNION ALL
    SELECT 12,        '2026-05-24',                     'corrective',                        'terminee',            'Remise en service'                             UNION ALL
    SELECT 13,        '2026-05-26',                     'preventive',                        'planifiee',           'Mise à jour firmware'                          UNION ALL
    SELECT 14,        '2026-05-28',                     'corrective',                        'en_cours',            'Remplacement capteur défectueux'               UNION ALL
    SELECT 15,        '2026-05-30',                     'preventive',                        'planifiee',           'Remplacement batterie'
) m
JOIN (
    -- Associer chaque rang au capteur correspondant (par ordre d'ID)
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rang
    FROM capteur
) c ON c.rang = m.rang;

-- 4. Vérification
SELECT
    m.id,
    m.date_maintenance,
    m.type_maintenance,
    m.statut,
    m.description,
    c.nom  AS capteur,
    c.type AS type_capteur,
    f.nom  AS foret
FROM maintenance m
JOIN capteur c ON m.capteur_id = c.id
JOIN foret   f ON c.foret_id   = f.id
ORDER BY m.date_maintenance;
