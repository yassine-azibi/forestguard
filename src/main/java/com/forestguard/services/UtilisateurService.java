package com.forestguard.services;

import com.forestguard.entities.Utilisateur;
import com.forestguard.interfaces.IService;
import com.forestguard.utils.MyConnection;
import com.forestguard.utils.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UtilisateurService implements IService<Utilisateur> {
    private Connection connection() {
        return MyConnection.getConnection();
    }

    @Override
    public void addEntity(Utilisateur utilisateur) {
        if (utilisateur == null) {
            throw new IllegalArgumentException("Utilisateur invalide");
        }

        String sql = "INSERT INTO utilisateur (nom, email, telephone, localisation, password_hash) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, utilisateur.getNom());
            statement.setString(2, utilisateur.getEmail());
            statement.setString(3, utilisateur.getTelephone());
            statement.setString(4, utilisateur.getLocalisation());
            statement.setString(5, PasswordHasher.hashPassword(utilisateur.getMotDePasse()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Echec de l'inscription", e);
        }
    }

    public Optional<Utilisateur> authenticate(String email, String password) {
        String sql = "SELECT * FROM utilisateur WHERE email = ?";

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String hash = resultSet.getString("password_hash");

                    if (PasswordHasher.verifyPassword(password, hash)) {
                        return Optional.of(mapUtilisateur(resultSet));
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Echec de connexion", e);
        }

        return Optional.empty();
    }

    public Optional<Utilisateur> findById(int id) {
        String sql = "SELECT * FROM utilisateur WHERE id = ?";

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUtilisateur(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de recuperer l'utilisateur", e);
        }

        return Optional.empty();
    }

    @Override
    public void deleteEntity(Utilisateur utilisateur) {
        String sql = "DELETE FROM utilisateur WHERE id = ?";

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setInt(1, utilisateur.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Echec de suppression du compte", e);
        }
    }

    @Override
    public void updateEntity(int id, Utilisateur utilisateur) {
        Utilisateur current = findById(id).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        String passwordHash = current.getPasswordHash();
        if (utilisateur.getMotDePasse() != null && !utilisateur.getMotDePasse().isBlank()) {
            passwordHash = PasswordHasher.hashPassword(utilisateur.getMotDePasse());
        }

        String sql = "UPDATE utilisateur SET nom = ?, email = ?, telephone = ?, localisation = ?, password_hash = ? WHERE id = ?";

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, utilisateur.getNom());
            statement.setString(2, utilisateur.getEmail());
            statement.setString(3, utilisateur.getTelephone());
            statement.setString(4, utilisateur.getLocalisation());
            statement.setString(5, passwordHash);
            statement.setInt(6, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Echec de modification du profil", e);
        }
    }

    @Override
    public List<Utilisateur> getData() {
        List<Utilisateur> utilisateurs = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur ORDER BY id DESC";

        try (PreparedStatement statement = connection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                utilisateurs.add(mapUtilisateur(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les utilisateurs", e);
        }

        return utilisateurs;
    }

    public boolean emailExists(String email) {
        return existsByColumn("email", email);
    }

    public boolean telephoneExists(String telephone) {
        return existsByColumn("telephone", telephone);
    }

    public boolean emailTakenByAnotherUser(int id, String email) {
        return existsByColumnExceptId("email", email, id);
    }

    public boolean telephoneTakenByAnotherUser(int id, String telephone) {
        return existsByColumnExceptId("telephone", telephone, id);
    }

    public Optional<Utilisateur> findByEmail(String email) {
        String sql = "SELECT * FROM utilisateur WHERE email = ?";

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUtilisateur(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de recuperer l'utilisateur", e);
        }

        return Optional.empty();
    }

    /**
     * Recherche un utilisateur par son Google ID unique ({@code sub}).
     *
     * @param googleId identifiant Google (champ {@code sub})
     * @return l'utilisateur correspondant, ou {@code Optional.empty()} si inconnu
     */
    public Optional<Utilisateur> findByGoogleId(String googleId) {
        String sql = "SELECT * FROM utilisateur WHERE google_id = ?";
        try (PreparedStatement st = connection().prepareStatement(sql)) {
            st.setString(1, googleId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return Optional.of(mapUtilisateur(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de rechercher par google_id", e);
        }
        return Optional.empty();
    }

    /**
     * Crée un compte utilisateur via Google OAuth2.
     *
     * <p>Génère automatiquement un mot de passe sécurisé de 12 caractères,
     * le hash en BCrypt et le stocke dans {@code password_hash}.
     * Le mot de passe en clair est disponible via {@link Utilisateur#getGeneratedPassword()}
     * uniquement jusqu'à l'envoi de l'email — il n'est jamais persisté.</p>
     *
     * @param googleUser   profil Google retourné par {@link com.forestguard.utils.GoogleAuthService}
     * @param telephone    numéro de téléphone saisi lors de l'inscription (format E164)
     * @param localisation gouvernorat sélectionné lors de l'inscription
     * @return l'utilisateur créé avec son {@code id} renseigné et
     *         {@code generatedPassword} rempli (transient)
     */
    public Utilisateur addGoogleUser(com.forestguard.utils.GoogleAuthService.GoogleUser googleUser,
                                     String telephone,
                                     String localisation) {
        // ── Génération du mot de passe sécurisé ──────────────────────────────
        String plainPassword = generateSecurePassword();
        String hashedPassword = PasswordHasher.hashPassword(plainPassword);

        String sql = "INSERT INTO utilisateur "
                + "(nom, email, telephone, localisation, password_hash, google_id, google_picture_url) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement st = connection().prepareStatement(sql,
                java.sql.Statement.RETURN_GENERATED_KEYS)) {
            st.setString(1, googleUser.name);
            st.setString(2, googleUser.email);
            st.setString(3, telephone);
            st.setString(4, localisation);
            // ── Hash stocké en BD (jamais le mot de passe en clair) ──────────
            st.setString(5, hashedPassword);
            st.setString(6, googleUser.googleId);
            st.setString(7, googleUser.pictureUrl.isBlank() ? null : googleUser.pictureUrl);
            st.executeUpdate();

            try (ResultSet keys = st.getGeneratedKeys()) {
                if (keys.next()) {
                    Utilisateur u = new Utilisateur();
                    u.setId(keys.getInt(1));
                    u.setNom(googleUser.name);
                    u.setEmail(googleUser.email);
                    u.setTelephone(telephone);
                    u.setLocalisation(localisation);
                    u.setGoogleId(googleUser.googleId);
                    u.setGooglePictureUrl(googleUser.pictureUrl);
                    // ── Mot de passe en clair disponible uniquement pour l'email ──
                    u.setGeneratedPassword(plainPassword);
                    return u;
                }
            }
            throw new IllegalStateException("Aucun ID généré après insertion Google.");
        } catch (SQLException e) {
            throw new IllegalStateException("Échec de la création du compte Google", e);
        }
    }

    /**
     * Génère un mot de passe aléatoire sécurisé de 12 caractères.
     *
     * <p>Garantit la présence d'au moins : 1 majuscule, 1 minuscule,
     * 1 chiffre, 1 symbole — puis complète avec des caractères aléatoires
     * et mélange le résultat avec Fisher-Yates.</p>
     *
     * @return mot de passe en clair de 12 caractères
     */
    private static String generateSecurePassword() {
        final String UPPER   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String LOWER   = "abcdefghijklmnopqrstuvwxyz";
        final String DIGITS  = "0123456789";
        final String SYMBOLS = "@#$%!&*?";
        final String ALL     = UPPER + LOWER + DIGITS + SYMBOLS;
        final int    LENGTH  = 12;

        java.security.SecureRandom rng = new java.security.SecureRandom();
        char[] pwd = new char[LENGTH];

        // Garantir au moins un caractère de chaque catégorie
        pwd[0] = UPPER  .charAt(rng.nextInt(UPPER.length()));
        pwd[1] = LOWER  .charAt(rng.nextInt(LOWER.length()));
        pwd[2] = DIGITS .charAt(rng.nextInt(DIGITS.length()));
        pwd[3] = SYMBOLS.charAt(rng.nextInt(SYMBOLS.length()));

        // Compléter avec des caractères aléatoires
        for (int i = 4; i < LENGTH; i++) {
            pwd[i] = ALL.charAt(rng.nextInt(ALL.length()));
        }

        // Mélanger (Fisher-Yates) pour éviter un pattern prévisible
        for (int i = LENGTH - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = pwd[i]; pwd[i] = pwd[j]; pwd[j] = tmp;
        }

        return new String(pwd);
    }

    /**
     * Lie un compte existant (email classique) à un Google ID.
     *
     * <p>Utilisé quand un utilisateur se connecte avec Google mais possède déjà
     * un compte avec le même email — on associe les deux sans créer de doublon.</p>
     *
     * @param userId     identifiant de l'utilisateur existant
     * @param googleId   identifiant Google à associer
     * @param pictureUrl URL de la photo de profil Google (peut être null)
     */
    public void linkGoogleId(int userId, String googleId, String pictureUrl) {
        String sql = "UPDATE utilisateur SET google_id = ?, google_picture_url = ? WHERE id = ?";
        try (PreparedStatement st = connection().prepareStatement(sql)) {
            st.setString(1, googleId);
            st.setString(2, pictureUrl != null && !pictureUrl.isBlank() ? pictureUrl : null);
            st.setInt(3, userId);
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de lier le compte Google", e);
        }
    }

    public void updatePasswordByEmail(String email, String newPlainPassword) {
        String sql = "UPDATE utilisateur SET password_hash = ? WHERE email = ?";

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, PasswordHasher.hashPassword(newPlainPassword));
            statement.setString(2, email);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de mettre a jour le mot de passe", e);
        }
    }

    private boolean existsByColumn(String column, String value) {
        String sql = "SELECT 1 FROM utilisateur WHERE " + column + " = ?";

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Verification impossible", e);
        }
    }

    private boolean existsByColumnExceptId(String column, String value, int id) {
        String sql = "SELECT 1 FROM utilisateur WHERE " + column + " = ? AND id <> ?";

        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setInt(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Verification impossible", e);
        }
    }

    private Utilisateur mapUtilisateur(ResultSet resultSet) throws SQLException {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(resultSet.getInt("id"));
        utilisateur.setNom(resultSet.getString("nom"));
        utilisateur.setEmail(resultSet.getString("email"));
        utilisateur.setTelephone(resultSet.getString("telephone"));
        utilisateur.setLocalisation(resultSet.getString("localisation"));
        utilisateur.setPasswordHash(resultSet.getString("password_hash"));
        // Colonnes Google — peuvent être null sur les anciens comptes
        try { utilisateur.setGoogleId(resultSet.getString("google_id")); }
        catch (SQLException ignored) {}
        try { utilisateur.setGooglePictureUrl(resultSet.getString("google_picture_url")); }
        catch (SQLException ignored) {}
        return utilisateur;
    }
}