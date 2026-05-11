package edu.pompier.tools;

import java.util.*;

public class LocalisationData {

    private static final Map<String, List<String[]>> DATA = new LinkedHashMap<>();

    static {

        // ── 1. Tunis ──
        List<String[]> tunis = new ArrayList<>();
        tunis.add(new String[]{"Centre Tunis",          "36.8190", "10.1658"});
        tunis.add(new String[]{"La Marsa",              "36.8781", "10.3247"});
        tunis.add(new String[]{"Carthage",              "36.8528", "10.3233"});
        tunis.add(new String[]{"Le Bardo",              "36.8092", "10.1400"});
        tunis.add(new String[]{"Cite El Khadra",        "36.8300", "10.1900"});
        tunis.add(new String[]{"Ettadhamen",            "36.8500", "10.1300"});
        tunis.add(new String[]{"Sidi Hassine",          "36.8200", "10.1000"});
        DATA.put("Tunis", tunis);

        // ── 2. Ariana ──
        List<String[]> ariana = new ArrayList<>();
        ariana.add(new String[]{"Centre Ariana",        "36.8625", "10.1956"});
        ariana.add(new String[]{"Raoued",               "36.8900", "10.1500"});
        ariana.add(new String[]{"Kalaat El Andalous",   "37.0200", "10.0700"});
        ariana.add(new String[]{"Sidi Thabet",          "36.9200", "10.0500"});
        ariana.add(new String[]{"La Soukra",            "36.9000", "10.2200"});
        DATA.put("Ariana", ariana);

        // ── 3. Ben Arous ──
        List<String[]> benArous = new ArrayList<>();
        benArous.add(new String[]{"Centre Ben Arous",   "36.7533", "10.2281"});
        benArous.add(new String[]{"Rades",              "36.7700", "10.2800"});
        benArous.add(new String[]{"Hammam Lif",         "36.7300", "10.3300"});
        benArous.add(new String[]{"Bou Mhel",           "36.7200", "10.2600"});
        benArous.add(new String[]{"Megrine",            "36.7600", "10.2200"});
        benArous.add(new String[]{"Hammam Chott",       "36.7000", "10.3600"});
        DATA.put("Ben Arous", benArous);

        // ── 4. Manouba ──
        List<String[]> manouba = new ArrayList<>();
        manouba.add(new String[]{"Centre Manouba",      "36.8092", "10.0978"});
        manouba.add(new String[]{"Douar Hicher",        "36.8300", "10.0700"});
        manouba.add(new String[]{"Oued Ellil",          "36.8500", "10.0400"});
        manouba.add(new String[]{"Tebourba",            "36.8300", "9.8400"});
        manouba.add(new String[]{"Jedaida",             "36.8200", "9.9100"});
        DATA.put("Manouba", manouba);

        // ── 5. Bizerte ──
        List<String[]> bizerte = new ArrayList<>();
        bizerte.add(new String[]{"Centre Bizerte",      "37.2746", "9.8737"});
        bizerte.add(new String[]{"Zarzouna",            "37.2850", "9.8630"});
        bizerte.add(new String[]{"Menzel Bourguiba",    "37.1570", "9.7895"});
        bizerte.add(new String[]{"Mateur",              "37.0395", "9.6648"});
        bizerte.add(new String[]{"Sejnane",             "37.0575", "9.2396"});
        bizerte.add(new String[]{"Ghezala",             "37.1200", "9.5300"});
        bizerte.add(new String[]{"Ras Jebel",           "37.2200", "10.1200"});
        bizerte.add(new String[]{"Menzel Jemil",        "37.2400", "9.9800"});
        bizerte.add(new String[]{"Utique",              "37.0500", "10.0500"});
        DATA.put("Bizerte", bizerte);

        // ── 6. Nabeul ──
        List<String[]> nabeul = new ArrayList<>();
        nabeul.add(new String[]{"Centre Nabeul",        "36.4513", "10.7352"});
        nabeul.add(new String[]{"Kelibia",              "36.8467", "11.0969"});
        nabeul.add(new String[]{"Hammamet",             "36.4000", "10.6167"});
        nabeul.add(new String[]{"Grombalia",            "36.6000", "10.5000"});
        nabeul.add(new String[]{"Korba",                "36.5700", "10.8600"});
        nabeul.add(new String[]{"Menzel Temime",        "36.7800", "10.9800"});
        nabeul.add(new String[]{"Beni Khalled",         "36.6500", "10.6000"});
        nabeul.add(new String[]{"Soliman",              "36.7000", "10.4900"});
        DATA.put("Nabeul", nabeul);

        // ── 7. Zaghouan ──
        List<String[]> zaghouan = new ArrayList<>();
        zaghouan.add(new String[]{"Centre Zaghouan",    "36.4020", "10.1430"});
        zaghouan.add(new String[]{"Zriba",              "36.3600", "10.2900"});
        zaghouan.add(new String[]{"El Fahs",            "36.3800", "9.9100"});
        zaghouan.add(new String[]{"Nadhour",            "36.3500", "10.0700"});
        zaghouan.add(new String[]{"Bir Mcherga",        "36.5000", "9.8500"});
        DATA.put("Zaghouan", zaghouan);

        // ── 8. Beja ──
        List<String[]> beja = new ArrayList<>();
        beja.add(new String[]{"Centre Beja",            "36.7256", "9.1817"});
        beja.add(new String[]{"Nefza",                  "37.0230", "9.0350"});
        beja.add(new String[]{"Testour",                "36.5500", "9.4500"});
        beja.add(new String[]{"Thibar",                 "36.7300", "9.0900"});
        beja.add(new String[]{"Amdoun",                 "36.7800", "8.9200"});
        beja.add(new String[]{"Medjez El Bab",          "36.6500", "9.6100"});
        beja.add(new String[]{"Teboursouk",             "36.4600", "9.2500"});
        DATA.put("Beja", beja);

        // ── 9. Jendouba ──
        List<String[]> jendouba = new ArrayList<>();
        jendouba.add(new String[]{"Centre Jendouba",    "36.5011", "8.7757"});
        jendouba.add(new String[]{"Ain Draham",         "36.7800", "8.6900"});
        jendouba.add(new String[]{"Tabarka",            "36.9544", "8.7578"});
        jendouba.add(new String[]{"Bou Salem",          "36.6200", "8.9700"});
        jendouba.add(new String[]{"Fernana",            "36.6500", "8.7000"});
        jendouba.add(new String[]{"Ghardimaou",         "36.4500", "8.4300"});
        jendouba.add(new String[]{"Oued Meliz",         "36.6000", "8.5500"});
        DATA.put("Jendouba", jendouba);

        // ── 10. Le Kef ──
        List<String[]> kef = new ArrayList<>();
        kef.add(new String[]{"Centre Le Kef",           "36.1826", "8.7145"});
        kef.add(new String[]{"Sakiet Sidi Youssef",     "36.2300", "8.3500"});
        kef.add(new String[]{"Tajerouine",              "35.8900", "8.5600"});
        kef.add(new String[]{"Kalaat Khasba",           "35.9700", "8.6300"});
        kef.add(new String[]{"Nebeur",                  "36.4600", "8.8200"});
        kef.add(new String[]{"Dahmani",                 "35.9500", "8.8300"});
        DATA.put("Le Kef", kef);

        // ── 11. Siliana ──
        List<String[]> siliana = new ArrayList<>();
        siliana.add(new String[]{"Centre Siliana",      "36.0847", "9.3703"});
        siliana.add(new String[]{"Bou Arada",           "36.3600", "9.6200"});
        siliana.add(new String[]{"Makthar",             "35.8500", "9.2100"});
        siliana.add(new String[]{"Kesra",               "35.8200", "9.3700"});
        siliana.add(new String[]{"Gaafour",             "36.3100", "9.3200"});
        siliana.add(new String[]{"El Aroussa",          "36.4000", "9.6800"});
        DATA.put("Siliana", siliana);

        // ── 12. Sousse ──
        List<String[]> sousse = new ArrayList<>();
        sousse.add(new String[]{"Centre Sousse",        "35.8288", "10.6399"});
        sousse.add(new String[]{"Msaken",               "35.7300", "10.5700"});
        sousse.add(new String[]{"Enfidha",              "36.1300", "10.3800"});
        sousse.add(new String[]{"Hammam Sousse",        "35.8600", "10.5900"});
        sousse.add(new String[]{"Akouda",               "35.8700", "10.5700"});
        sousse.add(new String[]{"Kalaa Kebira",         "35.8700", "10.5300"});
        sousse.add(new String[]{"Sidi Bou Ali",         "35.9800", "10.4900"});
        DATA.put("Sousse", sousse);

        // ── 13. Monastir ──
        List<String[]> monastir = new ArrayList<>();
        monastir.add(new String[]{"Centre Monastir",    "35.7643", "10.8113"});
        monastir.add(new String[]{"Moknine",            "35.6400", "10.9000"});
        monastir.add(new String[]{"Ksar Hellal",        "35.6400", "10.8900"});
        monastir.add(new String[]{"Jemmal",             "35.6200", "10.7600"});
        monastir.add(new String[]{"Bembla",             "35.7200", "10.7700"});
        monastir.add(new String[]{"Teboulba",           "35.6700", "10.9600"});
        monastir.add(new String[]{"Sahline",            "35.7700", "10.7400"});
        DATA.put("Monastir", monastir);

        // ── 14. Mahdia ──
        List<String[]> mahdia = new ArrayList<>();
        mahdia.add(new String[]{"Centre Mahdia",        "35.5047", "11.0622"});
        mahdia.add(new String[]{"El Jem",               "35.2900", "10.7100"});
        mahdia.add(new String[]{"Chebba",               "35.2400", "11.1100"});
        mahdia.add(new String[]{"Ksour Essef",          "35.0700", "11.0000"});
        mahdia.add(new String[]{"Bou Merdes",           "35.6400", "10.9000"});
        mahdia.add(new String[]{"Sidi Alouane",         "35.3500", "10.9000"});
        DATA.put("Mahdia", mahdia);

        // ── 15. Kairouan ──
        List<String[]> kairouan = new ArrayList<>();
        kairouan.add(new String[]{"Centre Kairouan",    "35.6781", "10.0963"});
        kairouan.add(new String[]{"Sbikha",             "35.9300", "9.9700"});
        kairouan.add(new String[]{"Haffouz",            "35.6300", "9.6700"});
        kairouan.add(new String[]{"El Alaa",            "35.5500", "9.5700"});
        kairouan.add(new String[]{"Nasrallah",          "35.6700", "9.9700"});
        kairouan.add(new String[]{"Oueslatia",          "35.8500", "9.6000"});
        DATA.put("Kairouan", kairouan);

        // ── 16. Kasserine ──
        List<String[]> kasserine = new ArrayList<>();
        kasserine.add(new String[]{"Centre Kasserine",  "35.1676", "8.8365"});
        kasserine.add(new String[]{"Sbeitla",           "35.2300", "9.1200"});
        kasserine.add(new String[]{"Thala",             "35.5700", "8.6700"});
        kasserine.add(new String[]{"Foussana",          "35.2000", "8.7800"});
        kasserine.add(new String[]{"Feriana",           "34.9500", "8.5700"});
        kasserine.add(new String[]{"Majel Bel Abbes",   "35.5000", "8.8300"});
        kasserine.add(new String[]{"Hidra",             "35.4000", "8.5500"});
        DATA.put("Kasserine", kasserine);

        // ── 17. Sidi Bouzid ──
        List<String[]> sidiBouzid = new ArrayList<>();
        sidiBouzid.add(new String[]{"Centre Sidi Bouzid", "35.0381", "9.4858"});
        sidiBouzid.add(new String[]{"Regueb",           "34.9700", "9.8000"});
        sidiBouzid.add(new String[]{"Meknassy",         "34.9800", "9.9800"});
        sidiBouzid.add(new String[]{"Bir El Hafey",     "34.7200", "9.5700"});
        sidiBouzid.add(new String[]{"Jelma",            "35.2900", "9.5700"});
        sidiBouzid.add(new String[]{"Cebbala",          "34.8500", "9.7000"});
        DATA.put("Sidi Bouzid", sidiBouzid);

        // ── 18. Sfax ──
        List<String[]> sfax = new ArrayList<>();
        sfax.add(new String[]{"Centre Sfax",            "34.7406", "10.7603"});
        sfax.add(new String[]{"Sakiet Ezzit",           "34.7800", "10.7200"});
        sfax.add(new String[]{"Chihia",                 "34.6900", "10.6900"});
        sfax.add(new String[]{"Bir Ali Ben Khalifa",    "34.7200", "10.0900"});
        sfax.add(new String[]{"El Hencha",              "34.9200", "10.2900"});
        sfax.add(new String[]{"Agareb",                 "34.7400", "10.5500"});
        sfax.add(new String[]{"Jebeniana",              "35.0200", "10.9000"});
        sfax.add(new String[]{"Mahres",                 "34.5300", "10.5000"});
        DATA.put("Sfax", sfax);

        // ── 19. Gabes ──
        List<String[]> gabes = new ArrayList<>();
        gabes.add(new String[]{"Centre Gabes",          "33.8881", "10.0975"});
        gabes.add(new String[]{"El Hamma",              "33.8900", "9.7900"});
        gabes.add(new String[]{"Mareth",                "33.6600", "10.2900"});
        gabes.add(new String[]{"Matmata",               "33.5400", "9.9700"});
        gabes.add(new String[]{"Nouvelle Matmata",      "33.6400", "10.0200"});
        gabes.add(new String[]{"Menzel El Habib",       "34.0200", "10.0000"});
        DATA.put("Gabes", gabes);

        // ── 20. Medenine ──
        List<String[]> medenine = new ArrayList<>();
        medenine.add(new String[]{"Centre Medenine",    "33.3549", "10.5055"});
        medenine.add(new String[]{"Zarzis",             "33.5000", "11.1100"});
        medenine.add(new String[]{"Ben Gardane",        "33.1400", "11.2200"});
        medenine.add(new String[]{"Houmt Souk (Djerba)","33.8700", "10.8600"});
        medenine.add(new String[]{"Midoun (Djerba)",    "33.8100", "11.0000"});
        medenine.add(new String[]{"Beni Khedache",      "33.0700", "10.0900"});
        DATA.put("Medenine", medenine);

        // ── 21. Tataouine ──
        List<String[]> tataouine = new ArrayList<>();
        tataouine.add(new String[]{"Centre Tataouine",  "32.9211", "10.4511"});
        tataouine.add(new String[]{"Ghomrassen",        "32.9900", "10.2000"});
        tataouine.add(new String[]{"Remada",            "32.3200", "10.3900"});
        tataouine.add(new String[]{"Bir Lahmar",        "32.7700", "10.1200"});
        tataouine.add(new String[]{"Dehiba",            "32.0000", "10.7100"});
        DATA.put("Tataouine", tataouine);

        // ── 22. Gafsa ──
        List<String[]> gafsa = new ArrayList<>();
        gafsa.add(new String[]{"Centre Gafsa",          "34.4250", "8.7842"});
        gafsa.add(new String[]{"Metlaoui",              "34.3300", "8.4000"});
        gafsa.add(new String[]{"Redeyef",               "34.3800", "8.1500"});
        gafsa.add(new String[]{"Moulares",              "34.4800", "8.2800"});
        gafsa.add(new String[]{"El Ksar",               "34.4200", "8.8200"});
        gafsa.add(new String[]{"Sidi Aich",             "34.6200", "8.7200"});
        DATA.put("Gafsa", gafsa);

        // ── 23. Tozeur ──
        List<String[]> tozeur = new ArrayList<>();
        tozeur.add(new String[]{"Centre Tozeur",        "33.9197", "8.1335"});
        tozeur.add(new String[]{"Nefta",                "33.8700", "7.8800"});
        tozeur.add(new String[]{"Degache",              "33.9700", "8.2000"});
        tozeur.add(new String[]{"Hazoua",               "33.8900", "7.9700"});
        tozeur.add(new String[]{"Tamerza",              "34.3800", "7.9300"});
        DATA.put("Tozeur", tozeur);

        // ── 24. Kebili ──
        List<String[]> kebili = new ArrayList<>();
        kebili.add(new String[]{"Centre Kebili",        "33.7050", "8.9694"});
        kebili.add(new String[]{"Douz",                 "33.4600", "9.0200"});
        kebili.add(new String[]{"Souk Lahad",           "33.5200", "9.1700"});
        kebili.add(new String[]{"Faouar",               "33.1800", "9.0100"});
        kebili.add(new String[]{"El Golaa",             "33.9700", "9.0200"});
        DATA.put("Kebili", kebili);
    }

    public static List<String> getVilles() {
        return new ArrayList<>(DATA.keySet());
    }

    public static List<String> getZones(String ville) {
        List<String[]> zones = DATA.get(ville);
        if (zones == null) return new ArrayList<>();
        List<String> noms = new ArrayList<>();
        for (String[] z : zones) noms.add(z[0]);
        return noms;
    }

    public static double[] getCoordonnees(String ville, String zone) {
        List<String[]> zones = DATA.get(ville);
        if (zones == null) return null;
        for (String[] z : zones) {
            if (z[0].equals(zone)) {
                return new double[]{
                        Double.parseDouble(z[1]),
                        Double.parseDouble(z[2])
                };
            }
        }
        return null;
    }
}
