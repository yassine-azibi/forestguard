package com.forestguard.utils;

import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service d'authentification Google OAuth2 pour application JavaFX desktop.
 *
 * <h2>Flux OAuth2 PKCE (Proof Key for Code Exchange)</h2>
 * <ol>
 *   <li>Génère un {@code code_verifier} aléatoire et son {@code code_challenge} (SHA-256).</li>
 *   <li>Trouve automatiquement un port libre parmi une liste de ports candidats.</li>
 *   <li>Ouvre le navigateur système vers l'URL d'autorisation Google.</li>
 *   <li>Lance un serveur HTTP local sur {@code localhost:<port>} pour recevoir le callback.</li>
 *   <li>Échange le {@code code} contre un {@code access_token} via POST.</li>
 *   <li>Appelle l'API Google UserInfo pour récupérer {@code name}, {@code email},
 *       {@code picture} et {@code sub} (Google ID unique).</li>
 *   <li>Appelle {@code onSuccess} ou {@code onError} sur le thread appelant.</li>
 * </ol>
 *
 * <h2>Configuration requise dans Google Cloud Console</h2>
 * <ul>
 *   <li>Type d'application : <b>Application de bureau</b> (Desktop app)</li>
 *   <li>URIs de redirection autorisés — ajouter TOUS ces ports :</li>
 *   <li>{@code http://localhost:8080/callback}</li>
 *   <li>{@code http://localhost:8081/callback}</li>
 *   <li>{@code http://localhost:8082/callback}</li>
 *   <li>{@code http://localhost:8083/callback}</li>
 *   <li>{@code http://localhost:8084/callback}</li>
 *   <li>Scopes : {@code openid email profile}</li>
 * </ul>
 *
 * <h2>Configuration dans google.properties</h2>
 * <pre>
 * google.client.id=VOTRE_CLIENT_ID.apps.googleusercontent.com
 * google.client.secret=VOTRE_CLIENT_SECRET
 * </pre>
 *
 * <h2>Sécurité</h2>
 * <p>PKCE empêche les attaques par interception du code d'autorisation.
 * Le serveur callback s'arrête automatiquement après réception du premier callback
 * ou après un timeout de 2 minutes.</p>
 */
public final class GoogleAuthService {

    // ── Configuration OAuth2 ──────────────────────────────────────────────────

    private static final String PROPERTIES_FILE = "google.properties";

    private static final String AUTH_URL     = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL    = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String SCOPE        = "openid email profile";

    /**
     * Ports candidats essayés dans l'ordre.
     * Tous doivent être enregistrés dans Google Cloud Console comme redirect_uri.
     * Si WAMP occupe 8080, l'app utilisera automatiquement 8081, 8082, etc.
     */
    // FIX port dynamique : liste de ports candidats au lieu d'un port fixe.
    // Le premier port libre de cette liste sera utilisé pour le callback.
    private static final int[] CANDIDATE_PORTS = {8080, 8081, 8082, 8083, 8084};

    private static final int    CALLBACK_TIMEOUT = 120; // secondes
    private static final Duration HTTP_TIMEOUT   = Duration.ofSeconds(10);

    // ── Client HTTP partagé ───────────────────────────────────────────────────

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    private GoogleAuthService() {}

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Lance le flux OAuth2 Google de façon asynchrone.
     *
     * <p>Ouvre le navigateur, attend le callback, échange le code, récupère
     * le profil, puis appelle {@code onSuccess} ou {@code onError}.</p>
     *
     * <p>Les callbacks sont appelés sur un thread de fond — utiliser
     * {@code Platform.runLater()} dans les lambdas pour mettre à jour l'UI.</p>
     *
     * @param onSuccess appelé avec le {@link GoogleUser} si l'authentification réussit
     * @param onError   appelé avec un message d'erreur lisible si elle échoue
     */
    public static void authenticate(Consumer<GoogleUser> onSuccess, Consumer<String> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. Charger la configuration
                GoogleConfig config = loadConfig();

                // 2. Trouver un port libre parmi les candidats
                // FIX port dynamique : findFreePort() retourne le premier port
                // disponible de CANDIDATE_PORTS. Si WAMP occupe 8080, on prend 8081, etc.
                int port = findFreePort();

                // 3. Construire le redirect_uri avec le port trouvé
                // FIX port dynamique : redirect_uri construit dynamiquement
                // pour correspondre au port réellement utilisé.
                String redirectUri = "http://localhost:" + port + "/callback";

                // 4. Générer PKCE
                String codeVerifier  = generateCodeVerifier();
                String codeChallenge = generateCodeChallenge(codeVerifier);
                String state         = generateState();

                // 5. Construire l'URL d'autorisation avec le bon redirect_uri
                String authUrl = buildAuthUrl(config.clientId, codeChallenge, state, redirectUri);

                // 6. Ouvrir le navigateur
                openBrowser(authUrl);

                // 7. Attendre le callback sur le port trouvé
                String authCode = waitForAuthCode(state, port);

                // 8. Échanger le code contre un access_token
                // FIX port dynamique : passer redirectUri dynamique à l'échange de token.
                // Google vérifie que redirect_uri correspond exactement à celui de l'étape 5.
                String accessToken = exchangeCodeForToken(config, authCode, codeVerifier, redirectUri);

                // 9. Récupérer le profil utilisateur
                GoogleUser user = fetchUserInfo(accessToken);

                onSuccess.accept(user);

            } catch (GoogleAuthException e) {
                onError.accept(e.getMessage());
            } catch (Exception e) {
                onError.accept("Erreur inattendue lors de la connexion Google : " + e.getMessage());
            }
        });
    }

    // ── Recherche de port libre ───────────────────────────────────────────────

    /**
     * Trouve le premier port libre parmi {@link #CANDIDATE_PORTS}.
     *
     * <p>Utilise {@link ServerSocket} avec port 0 pour tester la disponibilité :
     * si {@code new ServerSocket(port)} réussit, le port est libre.</p>
     *
     * @return premier port disponible
     * @throws GoogleAuthException si aucun port candidat n'est libre
     */
    // FIX port dynamique : méthode centrale de sélection du port.
    private static int findFreePort() throws GoogleAuthException {
        for (int port : CANDIDATE_PORTS) {
            if (isPortFree(port)) {
                System.out.println("[GoogleAuth] Port callback sélectionné : " + port);
                return port;
            }
            System.out.println("[GoogleAuth] Port " + port + " occupé, essai suivant...");
        }
        throw new GoogleAuthException(
                "Aucun port disponible pour le callback Google OAuth.\n"
                + "Ports essayés : " + java.util.Arrays.toString(CANDIDATE_PORTS) + "\n"
                + "Libérez l'un de ces ports (WAMP, Tomcat, etc.) et réessayez.");
    }

    /**
     * Vérifie si un port est libre en tentant d'ouvrir un {@link ServerSocket}.
     *
     * @param port port à tester
     * @return {@code true} si le port est disponible
     */
    private static boolean isPortFree(int port) {
        try (ServerSocket ss = new ServerSocket(port)) {
            ss.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ── Étapes du flux ────────────────────────────────────────────────────────

    /**
     * Construit l'URL d'autorisation Google avec PKCE.
     *
     * @param redirectUri URI de redirection dynamique (port variable)
     */
    // FIX port dynamique : redirectUri passé en paramètre au lieu d'être une constante.
    private static String buildAuthUrl(String clientId, String codeChallenge,
                                        String state, String redirectUri) {
        return AUTH_URL
                + "?client_id="             + urlEncode(clientId)
                + "&redirect_uri="          + urlEncode(redirectUri)
                + "&response_type=code"
                + "&scope="                 + urlEncode(SCOPE)
                + "&code_challenge="        + codeChallenge
                + "&code_challenge_method=S256"
                + "&state="                 + state
                + "&access_type=offline"
                + "&prompt=select_account";
    }

    /** Ouvre l'URL dans le navigateur système par défaut. */
    private static void openBrowser(String url) throws GoogleAuthException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new GoogleAuthException(
                    "Impossible d'ouvrir le navigateur automatiquement. "
                    + "Veuillez ouvrir manuellement : " + url);
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException e) {
            throw new GoogleAuthException("Impossible d'ouvrir le navigateur : " + e.getMessage(), e);
        }
    }

    /**
     * Lance un serveur HTTP local sur {@code port} et attend le callback Google.
     *
     * @param expectedState valeur {@code state} attendue (protection CSRF)
     * @param port          port libre trouvé par {@link #findFreePort()}
     * @return le code d'autorisation reçu
     * @throws GoogleAuthException si timeout, erreur ou state invalide
     */
    // FIX port dynamique : port passé en paramètre au lieu d'utiliser CALLBACK_PORT.
    private static String waitForAuthCode(String expectedState, int port) throws GoogleAuthException {
        CompletableFuture<String> codeFuture = new CompletableFuture<>();
        HttpServer server;

        try {
            // FIX port dynamique : bind sur le port sélectionné dynamiquement.
            server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        } catch (IOException e) {
            throw new GoogleAuthException(
                    "Impossible de démarrer le serveur callback sur le port " + port
                    + " (le port a été libéré entre la vérification et le démarrage).", e);
        }

        // Executor dédié avec thread daemon — ne bloque pas la fermeture de l'app
        server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "google-oauth-callback-" + port);
            t.setDaemon(true);
            return t;
        }));

        // Créer le contexte /callback sans authentification
        com.sun.net.httpserver.HttpContext callbackContext =
                server.createContext("/callback", exchange -> {
            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryString(query);

                // ── Vérification CSRF ─────────────────────────────────────────
                String receivedState = params.get("state");
                if (!expectedState.equals(receivedState)) {
                    sendHtmlResponse(exchange, 400,
                            buildCallbackHtml("❌ Erreur de sécurité",
                                    "La valeur state ne correspond pas. Tentative d'attaque CSRF possible.",
                                    false));
                    codeFuture.completeExceptionally(
                            new GoogleAuthException("State invalide — possible attaque CSRF."));
                    return;
                }

                // ── Erreur retournée par Google ───────────────────────────────
                String error = params.get("error");
                if (error != null) {
                    String userMessage = mapGoogleError(error);
                    sendHtmlResponse(exchange, 200,
                            buildCallbackHtml("Connexion annulée", userMessage, false));
                    codeFuture.completeExceptionally(new GoogleAuthException(userMessage));
                    return;
                }

                // ── Code d'autorisation ───────────────────────────────────────
                String code = params.get("code");
                if (code == null || code.isBlank()) {
                    sendHtmlResponse(exchange, 400,
                            buildCallbackHtml("❌ Erreur", "Aucun code reçu de Google.", false));
                    codeFuture.completeExceptionally(
                            new GoogleAuthException("Aucun code d'autorisation reçu."));
                    return;
                }

                // ── Succès ────────────────────────────────────────────────────
                sendHtmlResponse(exchange, 200,
                        buildCallbackHtml("✅ Connexion réussie",
                                "Vous pouvez fermer cet onglet et revenir à ForestGuard.", true));
                codeFuture.complete(code);

            } finally {
                exchange.close();
            }
        });

        // Garantir l'absence d'authentification HTTP sur ce contexte
        callbackContext.setAuthenticator(null);

        server.start();
        System.out.println("[GoogleAuth] Serveur callback démarré sur http://localhost:" + port + "/callback");

        try {
            return codeFuture.get(CALLBACK_TIMEOUT, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new GoogleAuthException(
                    "Délai d'attente dépassé (" + CALLBACK_TIMEOUT + "s). "
                    + "Veuillez réessayer la connexion Google.");
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof GoogleAuthException gae) throw gae;
            throw new GoogleAuthException("Erreur lors de la réception du callback : " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GoogleAuthException("Connexion Google interrompue.");
        } finally {
            server.stop(0);
            System.out.println("[GoogleAuth] Serveur callback arrêté (port " + port + ").");
        }
    }

    /**
     * Échange le code d'autorisation contre un access_token via POST.
     *
     * @param redirectUri URI de redirection dynamique — doit correspondre exactement
     *                    à celui utilisé dans {@link #buildAuthUrl}
     * @return access_token Google
     */
    // FIX port dynamique : redirectUri passé en paramètre au lieu d'utiliser REDIRECT_URI.
    private static String exchangeCodeForToken(GoogleConfig config, String code,
                                                String codeVerifier, String redirectUri)
            throws GoogleAuthException {

        String body = "client_id="     + urlEncode(config.clientId)
                + "&client_secret="    + urlEncode(config.clientSecret)
                + "&code="             + urlEncode(code)
                + "&code_verifier="    + urlEncode(codeVerifier)
                + "&grant_type=authorization_code"
                // FIX port dynamique : redirect_uri dynamique, pas la constante 8080.
                + "&redirect_uri="     + urlEncode(redirectUri);

        String json = httpPost(TOKEN_URL, body, "application/x-www-form-urlencoded");

        String error = extractJsonString(json, "error");
        if (error != null) {
            String desc = extractJsonString(json, "error_description");
            throw new GoogleAuthException("Erreur d'échange de token : "
                    + (desc != null ? desc : error));
        }

        String accessToken = extractJsonString(json, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new GoogleAuthException("Aucun access_token reçu de Google.");
        }

        return accessToken;
    }

    /**
     * Appelle l'API Google UserInfo et retourne le profil.
     *
     * @return {@link GoogleUser} avec id, name, email, picture
     */
    private static GoogleUser fetchUserInfo(String accessToken) throws GoogleAuthException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(USERINFO_URL))
                    .timeout(HTTP_TIMEOUT)
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new GoogleAuthException(
                        "Erreur API UserInfo : HTTP " + response.statusCode());
            }

            String json = response.body();

            String sub     = extractJsonString(json, "sub");
            String name    = extractJsonString(json, "name");
            String email   = extractJsonString(json, "email");
            String picture = extractJsonString(json, "picture");

            if (email == null || email.isBlank()) {
                throw new GoogleAuthException(
                        "Google n'a pas fourni d'adresse email. "
                        + "Vérifiez que le scope 'email' est autorisé.");
            }

            return new GoogleUser(
                    sub     != null ? sub     : "",
                    name    != null ? name    : email,
                    email,
                    picture != null ? picture : ""
            );

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new GoogleAuthException(
                    "Impossible de récupérer le profil Google : " + e.getMessage(), e);
        }
    }

    // ── PKCE ─────────────────────────────────────────────────────────────────

    /** Génère un code_verifier aléatoire de 64 octets (Base64 URL-safe). */
    private static String generateCodeVerifier() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Calcule le code_challenge = BASE64URL(SHA256(code_verifier)). */
    private static String generateCodeChallenge(String codeVerifier) throws GoogleAuthException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new GoogleAuthException("SHA-256 non disponible.", e);
        }
    }

    /** Génère un state aléatoire pour la protection CSRF. */
    private static String generateState() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ── HTTP ─────────────────────────────────────────────────────────────────

    private static String httpPost(String url, String body, String contentType)
            throws GoogleAuthException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(HTTP_TIMEOUT)
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            return response.body();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new GoogleAuthException(
                    "Pas de connexion réseau. Vérifiez votre accès Internet.", e);
        }
    }

    private static void sendHtmlResponse(com.sun.net.httpserver.HttpExchange exchange,
                                          int statusCode, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private static Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key   = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String extractJsonString(String json, String field) {
        if (json == null || field == null) return null;
        Pattern p = Pattern.compile(
                "\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\/", "/");
        }
        return null;
    }

    /** Traduit les codes d'erreur Google OAuth2 en messages lisibles. */
    private static String mapGoogleError(String error) {
        return switch (error) {
            case "access_denied"              -> "Vous avez refusé l'accès à votre compte Google. "
                                                 + "Cliquez sur 'Continuer avec Google' et acceptez les permissions.";
            case "interaction_required"       -> "Une interaction est requise. Veuillez réessayer.";
            case "login_required"             -> "Connexion Google requise. Veuillez réessayer.";
            case "account_selection_required" -> "Veuillez sélectionner un compte Google.";
            default                           -> "Connexion Google annulée ou refusée (" + error + ").";
        };
    }

    /** Génère la page HTML affichée dans le navigateur après le callback. */
    private static String buildCallbackHtml(String title, String message, boolean success) {
        String color = success ? "#16a34a" : "#b91c1c";
        String icon  = success ? "✅" : "❌";
        return "<!DOCTYPE html><html lang='fr'><head>"
             + "<meta charset='UTF-8'>"
             + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
             + "<title>ForestGuard — " + title + "</title>"
             + "<style>body{font-family:Arial,sans-serif;display:flex;align-items:center;"
             + "justify-content:center;min-height:100vh;margin:0;background:#f0fdf4;}"
             + ".card{background:#fff;border-radius:16px;padding:40px 48px;text-align:center;"
             + "box-shadow:0 4px 24px rgba(0,0,0,.10);max-width:420px;}"
             + "h1{color:" + color + ";font-size:22px;margin-bottom:12px;}"
             + "p{color:#374151;font-size:15px;line-height:1.6;}"
             + "</style></head><body>"
             + "<div class='card'>"
             + "<div style='font-size:48px;margin-bottom:16px'>" + icon + "</div>"
             + "<h1>" + title + "</h1>"
             + "<p>" + message + "</p>"
             + "</div></body></html>";
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    private static GoogleConfig loadConfig() throws GoogleAuthException {
        try (var in = GoogleAuthService.class.getClassLoader()
                                             .getResourceAsStream(PROPERTIES_FILE)) {
            if (in == null) {
                throw new GoogleAuthException(
                        "Fichier de configuration manquant : " + PROPERTIES_FILE
                        + "\nCréez src/main/resources/google.properties avec :\n"
                        + "  google.client.id=VOTRE_CLIENT_ID\n"
                        + "  google.client.secret=VOTRE_CLIENT_SECRET");
            }
            java.util.Properties props = new java.util.Properties();
            props.load(in);

            String clientId     = props.getProperty("google.client.id",     "").trim();
            String clientSecret = props.getProperty("google.client.secret", "").trim();

            if (clientId.isBlank() || clientId.equals("VOTRE_CLIENT_ID")) {
                throw new GoogleAuthException(
                        "google.client.id non configuré dans " + PROPERTIES_FILE);
            }
            if (clientSecret.isBlank() || clientSecret.equals("VOTRE_CLIENT_SECRET")) {
                throw new GoogleAuthException(
                        "google.client.secret non configuré dans " + PROPERTIES_FILE);
            }

            return new GoogleConfig(clientId, clientSecret);

        } catch (IOException e) {
            throw new GoogleAuthException("Impossible de lire " + PROPERTIES_FILE, e);
        }
    }

    // ── Types de données ──────────────────────────────────────────────────────

    /** Profil utilisateur retourné par Google. */
    public static final class GoogleUser {
        /** Identifiant Google unique et stable (champ {@code sub}). */
        public final String googleId;
        /** Nom complet de l'utilisateur. */
        public final String name;
        /** Adresse email vérifiée par Google. */
        public final String email;
        /** URL de la photo de profil (peut être vide). */
        public final String pictureUrl;

        public GoogleUser(String googleId, String name, String email, String pictureUrl) {
            this.googleId   = googleId;
            this.name       = name;
            this.email      = email;
            this.pictureUrl = pictureUrl;
        }

        @Override
        public String toString() {
            return "GoogleUser{googleId='" + googleId + "', name='" + name
                    + "', email='" + email + "'}";
        }
    }

    private record GoogleConfig(String clientId, String clientSecret) {}

    // ── Exception dédiée ─────────────────────────────────────────────────────

    /** Levée à chaque étape du flux OAuth2 avec un message lisible par l'utilisateur. */
    public static final class GoogleAuthException extends Exception {
        public GoogleAuthException(String message) { super(message); }
        public GoogleAuthException(String message, Throwable cause) { super(message, cause); }
    }
}
