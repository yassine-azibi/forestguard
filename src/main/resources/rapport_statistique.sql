-- ============================================================
-- Table rapport_statistique — ForestGuard
-- Exécuter dans la base de données : forestguard
-- ============================================================
CREATE TABLE IF NOT EXISTS rapport_statistique (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    foret               VARCHAR(150)   NOT NULL,
    localisation        VARCHAR(200)   DEFAULT '',
    mois                INT            NOT NULL COMMENT '1=Janvier ... 12=Décembre',
    annee               INT            NOT NULL,
    pourcentage_incendie DOUBLE         NOT NULL DEFAULT 0,
    nb_danger           INT            NOT NULL DEFAULT 0,
    nb_attention        INT            NOT NULL DEFAULT 0,
    nb_sur              INT            NOT NULL DEFAULT 0,
    temp_moyenne        DOUBLE         NOT NULL DEFAULT 0,
    hum_moyenne         DOUBLE         NOT NULL DEFAULT 0,
    fumee_moyenne       DOUBLE         NOT NULL DEFAULT 0,
    date_calcul         DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_foret_periode (foret, mois, annee)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
