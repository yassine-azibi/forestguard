-- ═══════════════════════════════════════════════════════════════════════════
-- SCRIPT SQL - Création de la table FORETS
-- ═══════════════════════════════════════════════════════════════════════════
-- Date: 13 Mai 2026
-- Base de données: forestguard
-- Description: Table pour stocker les informations des forêts tunisiennes
-- ═══════════════════════════════════════════════════════════════════════════

USE forestguard;

-- Supprimer la table si elle existe déjà (ATTENTION: supprime les données!)
-- DROP TABLE IF EXISTS forets;

-- Créer la table forets
CREATE TABLE IF NOT EXISTS forets (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    superficie DOUBLE COMMENT 'Superficie en hectares',
    description TEXT,
    gouvernorat VARCHAR(100),
    risque ENUM('FAIBLE', 'MOYEN', 'ELEVE', 'CRITIQUE') DEFAULT 'FAIBLE',
    vegetation VARCHAR(100) COMMENT 'Type de végétation (chêne, pin, eucalyptus, etc.)',
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_gouvernorat (gouvernorat),
    INDEX idx_risque (risque)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ═══════════════════════════════════════════════════════════════════════════
-- DONNÉES DE TEST - Forêts tunisiennes
-- ═══════════════════════════════════════════════════════════════════════════

-- Forêts du Nord (Gouvernorat de Bizerte, Jendouba, Béja)
INSERT INTO forets (nom, latitude, longitude, superficie, description, gouvernorat, risque, vegetation) VALUES
('Forêt de Kroumirie', 36.7444, 8.7480, 15000, 'Grande forêt de chênes-lièges dans le nord-ouest de la Tunisie', 'Jendouba', 'MOYEN', 'Chêne-liège'),
('Forêt de Ain Draham', 36.7833, 8.6833, 8500, 'Forêt dense avec une biodiversité riche', 'Jendouba', 'ELEVE', 'Chêne zen'),
('Forêt de Feija', 36.5667, 8.5667, 6200, 'Parc national avec faune et flore protégées', 'Jendouba', 'FAIBLE', 'Chêne-liège'),
('Forêt de Tabarka', 36.9547, 8.7564, 4800, 'Forêt côtière avec pins maritimes', 'Jendouba', 'MOYEN', 'Pin maritime'),
('Forêt de Nefza', 37.0333, 9.2667, 3500, 'Forêt de pins et eucalyptus', 'Béja', 'FAIBLE', 'Pin d\'Alep');

-- Forêts du Centre (Gouvernorat de Kasserine, Kairouan, Siliana)
INSERT INTO forets (nom, latitude, longitude, superficie, description, gouvernorat, risque, vegetation) VALUES
('Forêt de Chambi', 35.1667, 8.6333, 6700, 'Parc national du Djebel Chambi, point culminant de Tunisie', 'Kasserine', 'CRITIQUE', 'Pin d\'Alep'),
('Forêt de Mghilla', 35.5000, 9.1333, 4200, 'Forêt de pins dans la région de Siliana', 'Siliana', 'ELEVE', 'Pin d\'Alep'),
('Forêt de Kesra', 35.8000, 9.3667, 3800, 'Forêt méditerranéenne avec oliviers sauvages', 'Siliana', 'MOYEN', 'Olivier sauvage'),
('Forêt de Bargou', 36.0333, 9.5833, 2900, 'Forêt de montagne avec sources naturelles', 'Siliana', 'FAIBLE', 'Chêne vert');

-- Forêts du Cap Bon (Gouvernorat de Nabeul)
INSERT INTO forets (nom, latitude, longitude, superficie, description, gouvernorat, risque, vegetation) VALUES
('Forêt de Dar Chichou', 36.8333, 10.7667, 2500, 'Forêt côtière du Cap Bon', 'Nabeul', 'MOYEN', 'Pin maritime'),
('Forêt de Korbous', 36.8333, 10.5667, 1800, 'Forêt thermale avec sources chaudes', 'Nabeul', 'FAIBLE', 'Eucalyptus');

-- Forêts du Sud (Gouvernorat de Gafsa, Tozeur)
INSERT INTO forets (nom, latitude, longitude, superficie, description, gouvernorat, risque, vegetation) VALUES
('Oasis de Tozeur', 33.9197, 8.1333, 1200, 'Palmeraie et oasis du désert', 'Tozeur', 'CRITIQUE', 'Palmier dattier'),
('Forêt de Bou Hedma', 34.5333, 9.6167, 16500, 'Parc national avec acacias et faune saharienne', 'Gafsa', 'ELEVE', 'Acacia'),
('Oasis de Nefta', 33.8833, 7.8833, 900, 'Oasis historique avec palmeraies', 'Tozeur', 'CRITIQUE', 'Palmier dattier');

-- Forêts de la région de Tunis
INSERT INTO forets (nom, latitude, longitude, superficie, description, gouvernorat, risque, vegetation) VALUES
('Forêt de Boukornine', 36.6667, 10.2333, 1900, 'Parc national près de Tunis', 'Ben Arous', 'MOYEN', 'Pin d\'Alep'),
('Forêt de Sidi Bou Said', 36.8667, 10.3333, 800, 'Petite forêt côtière touristique', 'Tunis', 'FAIBLE', 'Pin maritime');

-- ═══════════════════════════════════════════════════════════════════════════
-- VÉRIFICATION
-- ═══════════════════════════════════════════════════════════════════════════

-- Compter le nombre de forêts par gouvernorat
SELECT gouvernorat, COUNT(*) as nombre_forets, 
       ROUND(SUM(superficie), 2) as superficie_totale_ha
FROM forets 
GROUP BY gouvernorat 
ORDER BY superficie_totale_ha DESC;

-- Compter le nombre de forêts par niveau de risque
SELECT risque, COUNT(*) as nombre_forets
FROM forets 
GROUP BY risque 
ORDER BY FIELD(risque, 'CRITIQUE', 'ELEVE', 'MOYEN', 'FAIBLE');

-- Afficher toutes les forêts
SELECT id, nom, gouvernorat, superficie, risque, vegetation
FROM forets
ORDER BY gouvernorat, nom;

-- ═══════════════════════════════════════════════════════════════════════════
-- NOTES
-- ═══════════════════════════════════════════════════════════════════════════
-- 
-- 1. Les coordonnées (latitude, longitude) sont approximatives
-- 2. Les superficies sont en hectares
-- 3. Les niveaux de risque sont basés sur:
--    - CRITIQUE: Zones très sèches, risque d'incendie élevé
--    - ELEVE: Zones à surveiller de près
--    - MOYEN: Zones avec risque modéré
--    - FAIBLE: Zones bien protégées ou humides
-- 
-- 4. Pour ajouter plus de forêts, utilisez:
--    INSERT INTO forets (nom, latitude, longitude, superficie, description, gouvernorat, risque, vegetation)
--    VALUES ('Nom', lat, lng, superficie, 'Description', 'Gouvernorat', 'RISQUE', 'Type végétation');
-- 
-- ═══════════════════════════════════════════════════════════════════════════
