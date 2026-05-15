DROP DATABASE IF EXISTS forestguard;
CREATE DATABASE IF NOT EXISTS forestguard CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE forestguard;

CREATE TABLE foret (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100) NOT NULL,
    localisation VARCHAR(150),
    superficie FLOAT,
    type_vegetation VARCHAR(50),
    niveau_risque VARCHAR(50),
    date_creation DATE,
    latitude DECIMAL(10,4),
    longitude DECIMAL(10,4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE zones (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(100),
    localisation VARCHAR(150),
    superficie FLOAT,
    id_foret INT,
    FOREIGN KEY (id_foret) REFERENCES foret(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE capteur (
    id           INT                                      NOT NULL AUTO_INCREMENT,
    nom          VARCHAR(100)       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    type         ENUM('temperature','fumee','humidite')   NOT NULL,
    localisation VARCHAR(200)       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci          DEFAULT NULL,
    statut       ENUM('actif','inactif','en_panne')                DEFAULT 'actif',
    foret_id     INT                                      NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY fk_capteur_foret (foret_id),
    CONSTRAINT fk_capteur_foret
        FOREIGN KEY (foret_id) REFERENCES foret (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE donnee_capteur (
  id           INT           NOT NULL AUTO_INCREMENT,
  type         VARCHAR(20)   NOT NULL,
  capteur      VARCHAR(50)   NOT NULL,
  foret        VARCHAR(100)  NULL    DEFAULT NULL,
  localisation VARCHAR(150)  NULL    DEFAULT NULL,
  temperature  DOUBLE        NOT NULL,
  humidite     DOUBLE        NOT NULL,
  fumee        DOUBLE        NOT NULL,
  horodatage   DATETIME      NULL DEFAULT CURRENT_TIMESTAMP,
  risque       VARCHAR(20)   NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE anomalie (
  id              INT           NOT NULL AUTO_INCREMENT,
  date_detection  DATETIME      NULL DEFAULT NULL,
  capteur         VARCHAR(50)   NULL DEFAULT NULL,
  foret           VARCHAR(100)  NULL DEFAULT NULL,
  type_anomalie   VARCHAR(100)  NULL DEFAULT NULL,
  valeur_detectee DOUBLE        NULL DEFAULT NULL,
  seuil_depasse   DOUBLE        NULL DEFAULT NULL,
  traite          TINYINT(1)    NULL DEFAULT 0,
  donnee_id       INT           NULL DEFAULT NULL,
  PRIMARY KEY (id),
  KEY donnee_id (donnee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE journal_action (
  id          INT           NOT NULL AUTO_INCREMENT,
  date_action DATETIME      NULL DEFAULT NULL,
  utilisateur VARCHAR(100)  NULL DEFAULT NULL,
  action      VARCHAR(30)   NULL DEFAULT NULL,
  details     TEXT          NULL DEFAULT NULL,
  donnee_id   INT           NULL DEFAULT NULL,
  PRIMARY KEY (id),
  KEY donnee_id (donnee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rapport_inspection (
  id            INT           NOT NULL AUTO_INCREMENT,
  date_rapport  DATETIME      NULL DEFAULT NULL,
  foret         VARCHAR(100)  NULL DEFAULT NULL,
  responsable   VARCHAR(100)  NULL DEFAULT NULL,
  statut        VARCHAR(20)  NULL DEFAULT NULL,
  niveau_risque VARCHAR(20)  NULL DEFAULT NULL,
  observations  TEXT          NULL DEFAULT NULL,
  donnee_id     INT           NULL DEFAULT NULL,
  PRIMARY KEY (id),
  KEY donnee_id (donnee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rapport_statistique (
  id                   INT           NOT NULL AUTO_INCREMENT,
  foret                VARCHAR(150)  NOT NULL,
  localisation         VARCHAR(200)  NULL,
  mois                 INT           NOT NULL,
  annee                INT           NOT NULL,
  pourcentage_incendie DOUBLE        NOT NULL DEFAULT 0,
  nb_danger            INT           NOT NULL DEFAULT 0,
  nb_attention         INT           NOT NULL DEFAULT 0,
  nb_sur               INT           NOT NULL DEFAULT 0,
  temp_moyenne         DOUBLE        NOT NULL DEFAULT 0,
  hum_moyenne          DOUBLE        NOT NULL DEFAULT 0,
  fumee_moyenne        DOUBLE        NOT NULL DEFAULT 0,
  date_calcul          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_foret_periode (foret, mois, annee)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE maintenance (
  id               INT           NOT NULL AUTO_INCREMENT,
  date_maintenance DATETIME      NULL DEFAULT CURRENT_TIMESTAMP,
  type_maintenance ENUM('preventive', 'corrective', 'remplacement')
                                 NOT NULL,
  statut           ENUM('planifiee', 'en_cours', 'terminee')
                                 NULL DEFAULT 'planifiee',
  description      VARCHAR(255)  NULL DEFAULT NULL,
  capteur_id       INT           NOT NULL,
  PRIMARY KEY (id),
  KEY capteur_id (capteur_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE incendie (
    id INT PRIMARY KEY AUTO_INCREMENT,
    id_zone INT NOT NULL,
    date_debut DATETIME NOT NULL,
    date_fin DATETIME,
    superficie_brulee DECIMAL(10, 2) DEFAULT 0,
    statut ENUM('actif', 'en_cours', 'maitrise', 'eteint', 'en_attente') DEFAULT 'actif',
    cause VARCHAR(255),
    niveau_gravite ENUM('faible', 'moyen', 'eleve', 'critique') DEFAULT 'moyen',

    INDEX idx_id_zone (id_zone),
    INDEX idx_date_debut (date_debut),
    INDEX idx_statut (statut),
    INDEX idx_niveau_gravite (niveau_gravite),
    INDEX idx_periode (date_debut, date_fin),

    FOREIGN KEY (id_zone) REFERENCES zones(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE evenements (
    id INT PRIMARY KEY AUTO_INCREMENT,
    titre VARCHAR(200) NOT NULL,
    foret_id INT NOT NULL,
    foret_nom VARCHAR(100) NOT NULL,
    date_evenement DATETIME NOT NULL,
    max_participants INT NOT NULL DEFAULT 50 CHECK (max_participants > 0),
    participants INT NOT NULL DEFAULT 0 CHECK (participants >= 0),
    statut ENUM('ouvert', 'ferme', 'complet', 'annule', 'termine') DEFAULT 'ouvert',
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_foret_id (foret_id),
    INDEX idx_foret_nom (foret_nom),
    INDEX idx_date_evenement (date_evenement),
    INDEX idx_statut (statut),
    INDEX idx_date_statut (date_evenement, statut),

    FOREIGN KEY (foret_id) REFERENCES zones(id) ON DELETE CASCADE,

    CONSTRAINT chk_participants CHECK (participants <= max_participants)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE participant_evenement (
    id INT PRIMARY KEY AUTO_INCREMENT,
    evenement_id INT NOT NULL,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    nb_personnes INT NOT NULL DEFAULT 1,
    date_inscription DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE avis_evenement (
    id INT PRIMARY KEY AUTO_INCREMENT,
    evenement_id INT NOT NULL,
    auteur VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    etoiles INT NOT NULL CHECK (etoiles BETWEEN 1 AND 5),
    commentaire TEXT,
    date_avis DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE utilisateur (
  id                  INT NOT NULL AUTO_INCREMENT,
  nom                 VARCHAR(120) NOT NULL,
  email               VARCHAR(180) NOT NULL,
  telephone           VARCHAR(25) NOT NULL,
  localisation        VARCHAR(180) NOT NULL,
  password_hash       VARCHAR(255) NOT NULL,
  created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  google_id           VARCHAR(255) DEFAULT NULL,
  google_picture_url  VARCHAR(512) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY email (email),
  UNIQUE KEY telephone (telephone),
  UNIQUE KEY uq_utilisateur_google_id (google_id(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fire_alerts (
    id                INT NOT NULL AUTO_INCREMENT,
    zone              VARCHAR(150) NOT NULL,
    specific_location VARCHAR(255) NULL,
    details           TEXT NOT NULL,
    photo_path        VARCHAR(255) NULL,
    reporter_name     VARCHAR(150) NULL,
    reporter_email    VARCHAR(150) NULL,
    alert_level       VARCHAR(20) NOT NULL DEFAULT 'Low',
    status            VARCHAR(20) NOT NULL DEFAULT 'Active',
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_fire_alerts_zone       (zone),
    INDEX idx_fire_alerts_status     (status),
    INDEX idx_fire_alerts_created_at (created_at)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pompier (
  id            INT NOT NULL AUTO_INCREMENT,
  nom           VARCHAR(255) NOT NULL,
  prenom        VARCHAR(255) NOT NULL,
  email         VARCHAR(100) NOT NULL UNIQUE,
  telephone     VARCHAR(20) NOT NULL,
  mot_de_passe  VARCHAR(255) NOT NULL,
  statut        ENUM('disponible','en_mission','inactif') DEFAULT 'disponible',
  zone_id       INT,
  ville         VARCHAR(100),
  zone_adresse  VARCHAR(150),
  latitude      DOUBLE,
  longitude     DOUBLE,
  photo_path    VARCHAR(500),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE certification (
  id                INT NOT NULL AUTO_INCREMENT,
  nom_certification VARCHAR(100) NOT NULL,
  description       TEXT,
  duree_validite    INT NOT NULL COMMENT 'Duree en mois (ex: 12, 24, 60)',
  niveau_requis     ENUM('debutant','intermediaire','avance','expert') DEFAULT 'debutant',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pompier_certification (
  id               INT NOT NULL AUTO_INCREMENT,
  id_pompier       INT NOT NULL,
  id_certification INT NOT NULL,
  date_obtention   DATE NOT NULL,
  date_expiration  DATE NOT NULL,
  note_obtenue     DECIMAL(5,2) COMMENT 'Note sur 20',
  organisme        VARCHAR(100) COMMENT 'Organisme formateur',
  PRIMARY KEY (id),
  FOREIGN KEY (id_pompier)       REFERENCES pompier(id),
  FOREIGN KEY (id_certification) REFERENCES certification(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE garde (
  id          INT NOT NULL AUTO_INCREMENT,
  id_pompier  INT NOT NULL,
  date_garde  DATE NOT NULL,
  creneau     ENUM('matin','apres-midi','nuit','24h') NOT NULL,
  disponible  TINYINT(1) DEFAULT 1,
  commentaire VARCHAR(255),
  PRIMARY KEY (id),
  FOREIGN KEY (id_pompier) REFERENCES pompier(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pompier_foret (
  id               INT NOT NULL AUTO_INCREMENT,
  id_pompier       INT NOT NULL,
  id_foret         INT NOT NULL,
  date_affectation DATE NOT NULL,
  date_detachement DATE,
  est_responsable  TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  FOREIGN KEY (id_pompier) REFERENCES pompier(id),
  FOREIGN KEY (id_foret)   REFERENCES foret(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE equipement (
    id   INT AUTO_INCREMENT PRIMARY KEY,
    nom  VARCHAR(100) NOT NULL,
    type VARCHAR(50)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE intervention (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    alert_zone          VARCHAR(100) NOT NULL,
    statut              ENUM('In Progress','Completed') DEFAULT 'In Progress',
    start_date          DATETIME     NOT NULL,
    end_date            DATETIME     NULL,
    agent_name          VARCHAR(100) NOT NULL,
    resultat            VARCHAR(200) NULL,
    alerte_id           INT          NULL,
    alerte_localisation VARCHAR(150) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE intervention_equipement (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    alert_zone    VARCHAR(100) NOT NULL,
    agent_name    VARCHAR(100) NOT NULL,
    start_date    DATETIME     NOT NULL,
    equipement_id INT          NOT NULL,
    FOREIGN KEY (equipement_id) REFERENCES equipement(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE affectation (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    id_alerte        INT      NOT NULL,
    id_pompier       INT      NOT NULL,
    date_affectation DATETIME NOT NULL,
    statut           ENUM('en_cours','annule') NOT NULL DEFAULT 'en_cours',
    score_selection  DOUBLE   DEFAULT NULL,
    distance_km      DOUBLE   DEFAULT NULL,
    mode_affectation ENUM('automatique','annule') NOT NULL DEFAULT 'automatique',
    FOREIGN KEY (id_pompier) REFERENCES pompier(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE evenement_forestier (
  id              INT NOT NULL AUTO_INCREMENT,
  titre           VARCHAR(255) NOT NULL,
  description     TEXT DEFAULT NULL,
  foret           VARCHAR(255) NOT NULL,
  date_evenement  DATE NOT NULL,
  capacite        INT NOT NULL DEFAULT 0,
  places_restantes INT NOT NULL DEFAULT 0,
  type_evenement  VARCHAR(80) NOT NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_evenement_date (date_evenement),
  KEY idx_evenement_type (type_evenement)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inscription_evenement (
  id               INT NOT NULL AUTO_INCREMENT,
  evenement_id     INT NOT NULL,
  nom_responsable  VARCHAR(150) NOT NULL,
  email            VARCHAR(150) NOT NULL,
  nb_participants  INT NOT NULL DEFAULT 1,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_inscription_evenement_eid (evenement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE avis_evenement_forestier (
  id           INT NOT NULL AUTO_INCREMENT,
  evenement_id INT NOT NULL,
  nom_auteur   VARCHAR(150) NOT NULL,
  note         TINYINT NOT NULL,
  commentaire  TEXT DEFAULT NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_avis_evenement_eid (evenement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE alerte (
    id            INT NOT NULL AUTO_INCREMENT,
    type_alerte   VARCHAR(50) NOT NULL,
    niveau        VARCHAR(20) NOT NULL,
    localisation  VARCHAR(150) NOT NULL,
    date_alerte   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut        VARCHAR(20) NOT NULL DEFAULT 'Nouvelle',
    source        VARCHAR(50) NOT NULL DEFAULT 'Manuelle',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE donnee_capteur d
JOIN capteur c ON d.capteur = c.nom COLLATE utf8mb4_unicode_ci
JOIN foret   f ON c.foret_id = f.id
SET d.foret        = f.nom COLLATE utf8mb4_unicode_ci,
    d.localisation = f.localisation COLLATE utf8mb4_unicode_ci
WHERE d.foret COLLATE utf8mb4_unicode_ci = ''
   OR d.foret IS NULL;

