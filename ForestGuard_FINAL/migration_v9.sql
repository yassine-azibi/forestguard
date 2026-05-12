-- ============================================================
--  ForestGuard – Script de migration v8 → v9
--  À exécuter UNE SEULE FOIS dans votre base MySQL/WAMP
-- ============================================================

-- 1. S'assurer que la table foret existe (déjà créée par l'équipe forêts)
-- CREATE TABLE foret ( ... ) -- déjà faite ✅

-- 2. Ajouter la colonne foret_id dans la table capteur
ALTER TABLE capteur
    ADD COLUMN foret_id INT NOT NULL DEFAULT 1
        AFTER statut;

-- 3. Ajouter la clé étrangère
ALTER TABLE capteur
    ADD CONSTRAINT fk_capteur_foret
        FOREIGN KEY (foret_id)
        REFERENCES foret(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE;

-- 4. (Optionnel) Vérification rapide
SELECT c.id, c.nom, c.statut, f.nom AS foret_nom, f.localisation
FROM capteur c
JOIN foret f ON c.foret_id = f.id;
