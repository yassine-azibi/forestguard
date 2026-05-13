package com.forestguard.utils;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;

/**
 * Utilitaire pour les gouvernorats tunisiens.
 *
 * <p>Fournit la liste officielle des 24 gouvernorats et un système de
 * normalisation robuste capable de mapper les variantes retournées par
 * les APIs de géolocalisation (ip-api.com, Nominatim, ipinfo.io) vers
 * le nom officiel français.</p>
 *
 * <p>Les APIs retournent souvent des formes comme :
 * <ul>
 *   <li>"Nabeul Governorate" → "Nabeul"</li>
 *   <li>"Gouvernorat de Sfax" → "Sfax"</li>
 *   <li>"Wilayat Tunis" → "Tunis"</li>
 *   <li>"تونس" (arabe) → "Tunis"</li>
 * </ul>
 * </p>
 */
public final class GovernorateUtils {

    /** Liste officielle des 24 gouvernorats tunisiens. */
    private static final List<String> GOVERNORATES = List.of(
            "Tunis",
            "Ariana",
            "Ben Arous",
            "Manouba",
            "Nabeul",
            "Zaghouan",
            "Bizerte",
            "Béja",
            "Jendouba",
            "Kef",
            "Siliana",
            "Kairouan",
            "Kasserine",
            "Sidi Bouzid",
            "Sousse",
            "Monastir",
            "Mahdia",
            "Sfax",
            "Gafsa",
            "Tozeur",
            "Kébili",
            "Gabès",
            "Médenine",
            "Tataouine"
    );

    /**
     * Mapping exhaustif : variante (sans accents, minuscules) → nom officiel.
     *
     * <p>Couvre les formes retournées par ip-api.com, Nominatim et ipinfo.io :
     * suffixes "governorate" / "gouvernorat" / "wilayat", translittérations
     * anglaises, noms arabes translittérés.</p>
     */
    private static final Map<String, String> ALIASES = Map.ofEntries(
            // ── Tunis ──────────────────────────────────────────────────────
            Map.entry("tunis",                    "Tunis"),
            Map.entry("tunis governorate",        "Tunis"),
            Map.entry("gouvernorat de tunis",     "Tunis"),
            Map.entry("wilayat tunis",            "Tunis"),
            Map.entry("تونس",                     "Tunis"),

            // ── Ariana ────────────────────────────────────────────────────
            Map.entry("ariana",                   "Ariana"),
            Map.entry("ariana governorate",       "Ariana"),
            Map.entry("l'ariana",                 "Ariana"),
            Map.entry("l ariana",                 "Ariana"),
            Map.entry("أريانة",                   "Ariana"),

            // ── Ben Arous ─────────────────────────────────────────────────
            Map.entry("ben arous",                "Ben Arous"),
            Map.entry("ben arous governorate",    "Ben Arous"),
            Map.entry("benarous",                 "Ben Arous"),
            Map.entry("بن عروس",                  "Ben Arous"),

            // ── Manouba ───────────────────────────────────────────────────
            Map.entry("manouba",                  "Manouba"),
            Map.entry("manouba governorate",      "Manouba"),
            Map.entry("la manouba",               "Manouba"),
            Map.entry("منوبة",                    "Manouba"),

            // ── Nabeul ────────────────────────────────────────────────────
            Map.entry("nabeul",                   "Nabeul"),
            Map.entry("nabeul governorate",       "Nabeul"),
            Map.entry("gouvernorat de nabeul",    "Nabeul"),
            Map.entry("nabul",                    "Nabeul"),
            Map.entry("نابل",                     "Nabeul"),

            // ── Zaghouan ──────────────────────────────────────────────────
            Map.entry("zaghouan",                 "Zaghouan"),
            Map.entry("zaghouan governorate",     "Zaghouan"),
            Map.entry("زغوان",                    "Zaghouan"),

            // ── Bizerte ───────────────────────────────────────────────────
            Map.entry("bizerte",                  "Bizerte"),
            Map.entry("bizerte governorate",      "Bizerte"),
            Map.entry("banzart",                  "Bizerte"),
            Map.entry("بنزرت",                    "Bizerte"),

            // ── Béja ──────────────────────────────────────────────────────
            Map.entry("beja",                     "Béja"),
            Map.entry("beja governorate",         "Béja"),
            Map.entry("béja",                     "Béja"),
            Map.entry("باجة",                     "Béja"),

            // ── Jendouba ──────────────────────────────────────────────────
            Map.entry("jendouba",                 "Jendouba"),
            Map.entry("jendouba governorate",     "Jendouba"),
            Map.entry("جندوبة",                   "Jendouba"),

            // ── Kef ───────────────────────────────────────────────────────
            Map.entry("kef",                      "Kef"),
            Map.entry("le kef",                   "Kef"),
            Map.entry("kef governorate",          "Kef"),
            Map.entry("el kef",                   "Kef"),
            Map.entry("الكاف",                    "Kef"),

            // ── Siliana ───────────────────────────────────────────────────
            Map.entry("siliana",                  "Siliana"),
            Map.entry("siliana governorate",      "Siliana"),
            Map.entry("سليانة",                   "Siliana"),

            // ── Kairouan ──────────────────────────────────────────────────
            Map.entry("kairouan",                 "Kairouan"),
            Map.entry("kairouan governorate",     "Kairouan"),
            Map.entry("القيروان",                 "Kairouan"),

            // ── Kasserine ─────────────────────────────────────────────────
            Map.entry("kasserine",                "Kasserine"),
            Map.entry("kasserine governorate",    "Kasserine"),
            Map.entry("القصرين",                  "Kasserine"),

            // ── Sidi Bouzid ───────────────────────────────────────────────
            Map.entry("sidi bouzid",              "Sidi Bouzid"),
            Map.entry("sidi bouzid governorate",  "Sidi Bouzid"),
            Map.entry("سيدي بوزيد",               "Sidi Bouzid"),

            // ── Sousse ────────────────────────────────────────────────────
            Map.entry("sousse",                   "Sousse"),
            Map.entry("sousse governorate",       "Sousse"),
            Map.entry("سوسة",                     "Sousse"),

            // ── Monastir ──────────────────────────────────────────────────
            Map.entry("monastir",                 "Monastir"),
            Map.entry("monastir governorate",     "Monastir"),
            Map.entry("المنستير",                 "Monastir"),

            // ── Mahdia ────────────────────────────────────────────────────
            Map.entry("mahdia",                   "Mahdia"),
            Map.entry("mahdia governorate",       "Mahdia"),
            Map.entry("المهدية",                  "Mahdia"),

            // ── Sfax ──────────────────────────────────────────────────────
            Map.entry("sfax",                     "Sfax"),
            Map.entry("sfax governorate",         "Sfax"),
            Map.entry("صفاقس",                    "Sfax"),

            // ── Gafsa ─────────────────────────────────────────────────────
            Map.entry("gafsa",                    "Gafsa"),
            Map.entry("gafsa governorate",        "Gafsa"),
            Map.entry("قفصة",                     "Gafsa"),

            // ── Tozeur ────────────────────────────────────────────────────
            Map.entry("tozeur",                   "Tozeur"),
            Map.entry("tozeur governorate",       "Tozeur"),
            Map.entry("توزر",                     "Tozeur"),

            // ── Kébili ────────────────────────────────────────────────────
            Map.entry("kebili",                   "Kébili"),
            Map.entry("kebili governorate",       "Kébili"),
            Map.entry("kébili",                   "Kébili"),
            Map.entry("قبلي",                     "Kébili"),

            // ── Gabès ─────────────────────────────────────────────────────
            Map.entry("gabes",                    "Gabès"),
            Map.entry("gabes governorate",        "Gabès"),
            Map.entry("gabès",                    "Gabès"),
            Map.entry("قابس",                     "Gabès"),

            // ── Médenine ──────────────────────────────────────────────────
            Map.entry("medenine",                 "Médenine"),
            Map.entry("medenine governorate",     "Médenine"),
            Map.entry("médenine",                 "Médenine"),
            Map.entry("مدنين",                    "Médenine"),

            // ── Tataouine ─────────────────────────────────────────────────
            Map.entry("tataouine",                "Tataouine"),
            Map.entry("tataouine governorate",    "Tataouine"),
            Map.entry("تطاوين",                   "Tataouine")
    );

    private GovernorateUtils() {}

    /** Retourne la liste immuable des 24 gouvernorats officiels. */
    public static List<String> getGovernorates() {
        return GOVERNORATES;
    }

    /**
     * Normalise une chaîne quelconque vers le nom officiel du gouvernorat.
     *
     * <p>Algorithme en 3 passes :
     * <ol>
     *   <li>Correspondance exacte dans la table d'alias (sans accents, minuscules).</li>
     *   <li>Correspondance exacte dans la liste officielle (sans accents, minuscules).</li>
     *   <li>Correspondance partielle : le candidat contient le nom officiel
     *       (ex : "Nabeul Governorate" contient "nabeul").</li>
     * </ol>
     * </p>
     *
     * @param value valeur brute retournée par une API ou saisie par l'utilisateur
     * @return nom officiel du gouvernorat, ou chaîne vide si non reconnu
     */
    public static String normalize(String value) {
        if (value == null) return "";

        String candidate = value.trim();
        if (candidate.isBlank()) return "";

        // Passe 1 : alias exact (gère les suffixes "Governorate", noms arabes, etc.)
        String key = stripAccents(candidate).toLowerCase();
        String fromAlias = ALIASES.get(key);
        if (fromAlias != null) return fromAlias;

        // Passe 2 : correspondance exacte dans la liste officielle
        for (String gov : GOVERNORATES) {
            if (stripAccents(gov).equalsIgnoreCase(stripAccents(candidate))) {
                return gov;
            }
        }

        // Passe 3 : correspondance partielle — le candidat contient le nom du gouvernorat
        // (ex : "Nabeul Governorate" → "Nabeul", "Gouvernorat de Sfax" → "Sfax")
        for (String gov : GOVERNORATES) {
            String govStripped = stripAccents(gov).toLowerCase();
            if (key.contains(govStripped)) {
                return gov;
            }
        }

        return "";
    }

    /** Retourne {@code true} si la valeur correspond à un gouvernorat connu. */
    public static boolean isKnownGovernorate(String value) {
        return !normalize(value).isBlank();
    }

    /** Supprime les diacritiques (accents) d'une chaîne Unicode. */
    private static String stripAccents(String value) {
        String nfd = Normalizer.normalize(value, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "");
    }
}
