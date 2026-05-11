package com.forestguard.entities;

public class Utilisateur {
    private int id;
    private String nom;
    private String email;
    private String telephone;
    private String localisation;
    private String motDePasse;
    private String passwordHash;
    /** Identifiant Google unique (champ {@code sub}). Null si compte classique. */
    private String googleId;
    /** URL de la photo de profil Google. Null si compte classique. */
    private String googlePictureUrl;
    /**
     * Mot de passe généré automatiquement lors de la création via Google OAuth2.
     * Champ transient : jamais persisté en base, utilisé uniquement pour l'envoi
     * par email juste après la création du compte.
     */
    private transient String generatedPassword;

    public Utilisateur() {
    }

    public Utilisateur(String nom, String email, String telephone, String localisation, String motDePasse) {
        this.nom = nom;
        this.email = email;
        this.telephone = telephone;
        this.localisation = localisation;
        this.motDePasse = motDePasse;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public String getGooglePictureUrl() { return googlePictureUrl; }
    public void setGooglePictureUrl(String googlePictureUrl) { this.googlePictureUrl = googlePictureUrl; }

    /** Mot de passe généré (transient — non persisté). */
    public String getGeneratedPassword() { return generatedPassword; }
    public void setGeneratedPassword(String generatedPassword) { this.generatedPassword = generatedPassword; }

    /** Retourne {@code true} si ce compte a été créé ou lié via Google. */
    public boolean isGoogleAccount() {
        return googleId != null && !googleId.isBlank();
    }
}