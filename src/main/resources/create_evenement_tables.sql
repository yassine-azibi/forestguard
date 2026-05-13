-- Tables pour EvenementManager dans forest_guard

CREATE TABLE IF NOT EXISTS evenement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    foret_id INT,
    foret_nom VARCHAR(200),
    date_evenement VARCHAR(20),
    max_participants INT DEFAULT 50,
    participants INT DEFAULT 0,
    statut VARCHAR(20) DEFAULT 'OUVERT',
    description TEXT,
    FOREIGN KEY (foret_id) REFERENCES foret(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS participant_evenement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    evenement_id INT NOT NULL,
    nom VARCHAR(200),
    email VARCHAR(200),
    nb_personnes INT DEFAULT 1,
    date_inscription TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS avis_evenement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    evenement_id INT NOT NULL,
    auteur VARCHAR(200),
    email VARCHAR(200),
    etoiles INT CHECK (etoiles BETWEEN 1 AND 5),
    commentaire TEXT,
    date_avis TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE
);
