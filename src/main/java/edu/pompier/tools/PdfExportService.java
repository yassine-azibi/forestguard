package edu.pompier.tools;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import edu.pompier.entities.Pompier;
import edu.pompier.services.PerformanceIA;
import edu.pompier.services.PompierService;
import edu.pompier.controllers.GestionGardesController;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExportService {

    private static final BaseColor VERT_FONCE   = new BaseColor(27,  94,  32);
    private static final BaseColor VERT_MOYEN   = new BaseColor(46, 125,  50);
    private static final BaseColor VERT_CLAIR   = new BaseColor(200, 230, 201);
    private static final BaseColor VERT_PALE    = new BaseColor(232, 245, 233);
    private static final BaseColor ORANGE       = new BaseColor(230, 81,   0);
    private static final BaseColor ORANGE_CLAIR = new BaseColor(255, 224, 178);
    private static final BaseColor ROUGE        = new BaseColor(198,  40,  40);
    private static final BaseColor ROUGE_CLAIR  = new BaseColor(255, 205, 210);
    private static final BaseColor GRIS_FONCE   = new BaseColor( 55,  65,  81);
    private static final BaseColor GRIS_CLAIR   = new BaseColor(248, 250, 252);
    private static final BaseColor BLANC        = BaseColor.WHITE;

    private static final Font TITRE_PRINCIPAL = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, BaseColor.WHITE);
    private static final Font TITRE_SECTION   = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, new BaseColor(27, 94, 32));
    private static final Font TITRE_COLONNE   = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD, BaseColor.WHITE);
    private static final Font TEXTE_NORMAL    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(55, 65, 81));
    private static final Font TEXTE_BOLD      = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(55, 65, 81));
    private static final Font TEXTE_PETIT     = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, new BaseColor(55, 65, 81));
    private static final Font SOUS_TITRE      = new Font(Font.FontFamily.HELVETICA, 11, Font.ITALIC, new BaseColor(46, 125, 50));

    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══════════════════════════════════════════════════════════
    //  1. FICHE INDIVIDUELLE
    // ══════════════════════════════════════════════════════════
    public static String exportFicheIndividuelle(Pompier p, String chemin) {
        try {
            PompierService service = new PompierService();
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(chemin));
            writer.setPageEvent(new PiedDePage("Fiche Pompier — " + p.getPrenom() + " " + p.getNom()));
            doc.open();
            ajouterEnTete(doc, "FICHE INDIVIDUELLE", p.getPrenom() + " " + p.getNom().toUpperCase());

            ajouterTitreSection(doc, "Informations Personnelles");
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(6);
            infoTable.setWidths(new float[]{1f, 2f});
            ajouterLigneInfo(infoTable, "Nom complet",    p.getNom() + " " + p.getPrenom());
            ajouterLigneInfo(infoTable, "Email",          p.getEmail() != null ? p.getEmail() : "—");
            ajouterLigneInfo(infoTable, "Telephone",      p.getTelephone() != null ? p.getTelephone() : "—");
            ajouterLigneInfo(infoTable, "Statut",         p.getStatut() != null ? p.getStatut().replace("_", " ").toUpperCase() : "—");
            ajouterLigneInfo(infoTable, "Foret assignee", p.getNomForet() != null ? p.getNomForet() : "Aucune");
            ajouterLigneInfo(infoTable, "Localisation",   (p.getVille() != null ? p.getVille() : "—") + (p.getZoneAdresse() != null ? " — " + p.getZoneAdresse() : ""));
            ajouterLigneInfo(infoTable, "Certification",  p.getNiveauCertification() != null ? p.getNiveauCertification().toUpperCase() : "Aucune");
            doc.add(infoTable);

            doc.add(Chunk.NEWLINE);
            ajouterTitreSection(doc, "Gardes Planifiees");
            List<GestionGardesController.GardeInfo> gardes = service.getAllGardesInfo();
            List<GestionGardesController.GardeInfo> gardesPompier = gardes.stream()
                    .filter(g -> g.idPompier == p.getId()).toList();
            if (gardesPompier.isEmpty()) {
                doc.add(creerParagraphe("Aucune garde planifiee.", SOUS_TITRE));
            } else {
                PdfPTable gardeTable = new PdfPTable(3);
                gardeTable.setWidthPercentage(100);
                gardeTable.setSpacingBefore(6);
                gardeTable.setWidths(new float[]{1.5f, 2f, 1.5f});
                ajouterEnTeteTableau(gardeTable, "Creneau", "Date", "Statut");
                boolean alt = false;
                for (GestionGardesController.GardeInfo g : gardesPompier) {
                    BaseColor bg = alt ? GRIS_CLAIR : BLANC;
                    ajouterCellule(gardeTable, g.creneau != null ? g.creneau.toUpperCase() : "—", TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                    ajouterCellule(gardeTable, g.dateGarde != null ? g.dateGarde : "—", TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                    ajouterCelluleColoree(gardeTable, "PLANIFIEE", TEXTE_BOLD, VERT_CLAIR);
                    alt = !alt;
                }
                doc.add(gardeTable);
            }

            doc.add(Chunk.NEWLINE);
            ajouterTitreSection(doc, "Performance IA");
            PerformanceIA ia = new PerformanceIA();
            List<PerformanceIA.ResultatPerformance> resultats = ia.evaluer("mois");
            PerformanceIA.ResultatPerformance score = resultats.stream()
                    .filter(r -> r.idPompier == p.getId()).findFirst().orElse(null);
            if (score != null) {
                PdfPTable scoreTable = new PdfPTable(2);
                scoreTable.setWidthPercentage(100);
                scoreTable.setSpacingBefore(6);
                scoreTable.setWidths(new float[]{1f, 2f});
                ajouterLigneInfo(scoreTable, "Score total",        String.format("%.1f / 100 pts", score.scoreTotal));
                ajouterLigneInfo(scoreTable, "Missions traitees",  score.nbMissions + " mission(s)");
                ajouterLigneInfo(scoreTable, "Taux de reussite",   score.tauxReussite > 0 ? Math.round(score.tauxReussite * 100) + "%" : "N/A");
                ajouterLigneInfo(scoreTable, "Temps moyen",        score.tempsMoyenHeures > 0 ? String.format("%.1f h", score.tempsMoyenHeures) : "N/A");
                ajouterLigneInfo(scoreTable, "Missions critiques", score.nbCritiques + " incendie(s) CRITIQUE");
                ajouterLigneInfo(scoreTable, "Gardes honorees",    score.nbGardes + " garde(s)");
                ajouterLigneInfo(scoreTable, "Titre",              score.titre != null && !score.titre.isEmpty() ? score.titre : "—");
                doc.add(scoreTable);
                doc.add(Chunk.NEWLINE);
                Paragraph expl = new Paragraph("Analyse IA : " + score.explication, TEXTE_PETIT);
                expl.setIndentationLeft(10);
                doc.add(expl);
            } else {
                doc.add(creerParagraphe("Aucune donnee de performance disponible.", SOUS_TITRE));
            }
            ajouterPiedPage(doc);
            doc.close();
            return chemin;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }


    // ══════════════════════════════════════════════════════════
    //  2. RAPPORT COMPLET
    // ══════════════════════════════════════════════════════════
    public static String exportRapportComplet(String chemin) {
        try {
            PompierService service = new PompierService();
            List<Pompier> pompiers = service.getData();
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(chemin));
            writer.setPageEvent(new PiedDePage("Rapport Complet des Pompiers"));
            doc.open();
            ajouterEnTete(doc, "RAPPORT COMPLET", "Liste de tous les pompiers — " + LocalDateTime.now().format(FMT_DATE));

            long disponibles = pompiers.stream().filter(p -> "disponible".equals(p.getStatut())).count();
            long enMission   = pompiers.stream().filter(p -> "en_mission".equals(p.getStatut())).count();
            long inactifs    = pompiers.stream().filter(p -> "inactif".equals(p.getStatut())).count();

            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingBefore(8);
            statsTable.setSpacingAfter(14);
            ajouterStatCard(statsTable, "TOTAL",       String.valueOf(pompiers.size()), VERT_FONCE);
            ajouterStatCard(statsTable, "DISPONIBLES", String.valueOf(disponibles),     VERT_MOYEN);
            ajouterStatCard(statsTable, "EN MISSION",  String.valueOf(enMission),        ORANGE);
            ajouterStatCard(statsTable, "INACTIFS",    String.valueOf(inactifs),          GRIS_FONCE);
            doc.add(statsTable);

            ajouterTitreSection(doc, "Liste Detaillee des Pompiers");
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setSpacingBefore(6);
            table.setWidths(new float[]{2f, 2.5f, 1.5f, 2f, 2f, 1.5f, 1.5f});
            ajouterEnTeteTableau(table, "Nom", "Email", "Telephone", "Foret", "Localisation", "Certification", "Statut");
            boolean alt = false;
            for (Pompier p : pompiers) {
                String statut = p.getStatut() != null ? p.getStatut().replace("_", " ").toUpperCase() : "—";
                BaseColor couleurStatut = "DISPONIBLE".equals(statut) ? VERT_CLAIR :
                        "EN MISSION".equals(statut) ? ORANGE_CLAIR : GRIS_CLAIR;
                BaseColor bg = alt ? GRIS_CLAIR : BLANC;
                ajouterCellule(table, p.getNom() + " " + p.getPrenom(), TEXTE_BOLD, bg, Element.ALIGN_LEFT);
                ajouterCellule(table, p.getEmail() != null ? p.getEmail() : "—", TEXTE_NORMAL, bg, Element.ALIGN_LEFT);
                ajouterCellule(table, p.getTelephone() != null ? p.getTelephone() : "—", TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                ajouterCellule(table, p.getNomForet() != null ? p.getNomForet() : "—", TEXTE_NORMAL, bg, Element.ALIGN_LEFT);
                String loc = (p.getVille() != null ? p.getVille() : "—") + (p.getZoneAdresse() != null ? "\n" + p.getZoneAdresse() : "");
                ajouterCellule(table, loc, TEXTE_PETIT, bg, Element.ALIGN_LEFT);
                ajouterCellule(table, p.getNiveauCertification() != null ? p.getNiveauCertification().toUpperCase() : "—", TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                ajouterCelluleColoree(table, statut, TEXTE_BOLD, couleurStatut);
                alt = !alt;
            }
            doc.add(table);
            ajouterPiedPage(doc);
            doc.close();
            return chemin;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    // ══════════════════════════════════════════════════════════
    //  3. RAPPORT GARDES & AFFECTATIONS
    // ══════════════════════════════════════════════════════════
    public static String exportRapportGardesAffectations(String chemin) {
        try {
            PompierService service = new PompierService();
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(chemin));
            writer.setPageEvent(new PiedDePage("Rapport Gardes & Affectations"));
            doc.open();
            ajouterEnTete(doc, "GARDES & AFFECTATIONS", "Rapport du " + LocalDateTime.now().format(FMT_DATE));

            ajouterTitreSection(doc, "Gardes Planifiees");
            List<GestionGardesController.GardeInfo> gardes = service.getAllGardesInfo();
            if (gardes.isEmpty()) {
                doc.add(creerParagraphe("Aucune garde planifiee.", SOUS_TITRE));
            } else {
                PdfPTable gardeTable = new PdfPTable(5);
                gardeTable.setWidthPercentage(100);
                gardeTable.setSpacingBefore(6);
                gardeTable.setSpacingAfter(16);
                gardeTable.setWidths(new float[]{2.5f, 1.5f, 2f, 1.5f, 1.5f});
                ajouterEnTeteTableau(gardeTable, "Pompier", "Creneau", "Date", "Telephone", "Statut");
                boolean alt = false;
                for (GestionGardesController.GardeInfo g : gardes) {
                    BaseColor bg = alt ? GRIS_CLAIR : BLANC;
                    ajouterCellule(gardeTable, g.nomPompier != null ? g.nomPompier : "—", TEXTE_BOLD, bg, Element.ALIGN_LEFT);
                    ajouterCellule(gardeTable, g.creneau != null ? g.creneau.toUpperCase() : "—", TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                    ajouterCellule(gardeTable, g.dateGarde != null ? g.dateGarde : "—", TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                    ajouterCellule(gardeTable, g.telephone != null ? g.telephone : "—", TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                    ajouterCelluleColoree(gardeTable, "PLANIFIEE", TEXTE_BOLD, VERT_CLAIR);
                    alt = !alt;
                }
                doc.add(gardeTable);
            }

            ajouterTitreSection(doc, "Historique des Affectations");
            List<String[]> affectations = service.getAffectations();
            List<String[]> affAuto = affectations.stream()
                    .filter(a -> a.length <= 14 || !"intervention".equals(a[14])).toList();
            if (affAuto.isEmpty()) {
                doc.add(creerParagraphe("Aucune affectation enregistree.", SOUS_TITRE));
            } else {
                PdfPTable affTable = new PdfPTable(6);
                affTable.setWidthPercentage(100);
                affTable.setSpacingBefore(6);
                affTable.setWidths(new float[]{2f, 2f, 1.5f, 2f, 1.5f, 1.5f});
                ajouterEnTeteTableau(affTable, "Pompier", "Date", "Type Alerte", "Localisation", "Distance", "Statut");
                boolean alt = false;
                for (String[] a : affAuto) {
                    BaseColor bg = alt ? GRIS_CLAIR : BLANC;
                    String statutNorm = a[7] != null ? a[7].toLowerCase() : "";
                    BaseColor couleurStatut = statutNorm.contains("en_cours") || statutNorm.contains("en cours") ? ORANGE_CLAIR :
                            statutNorm.contains("termin") ? VERT_CLAIR :
                            statutNorm.contains("annul") ? ROUGE_CLAIR : GRIS_CLAIR;
                    ajouterCellule(affTable, a[1] + " " + a[2], TEXTE_BOLD, bg, Element.ALIGN_LEFT);
                    String dateAff = a[6] != null && a[6].length() >= 16 ? a[6].substring(0, 16) : (a[6] != null ? a[6] : "—");
                    ajouterCellule(affTable, dateAff, TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                    ajouterCellule(affTable, a[3] != null ? a[3] : "—", TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                    ajouterCellule(affTable, a[5] != null ? a[5] : "—", TEXTE_NORMAL, bg, Element.ALIGN_LEFT);
                    ajouterCellule(affTable, a[8] != null ? a[8] + " km" : "—", TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                    ajouterCelluleColoree(affTable, a[7] != null ? a[7].toUpperCase().replace("_", " ") : "—", TEXTE_BOLD, couleurStatut);
                    alt = !alt;
                }
                doc.add(affTable);
            }
            ajouterPiedPage(doc);
            doc.close();
            return chemin;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }


    // ══════════════════════════════════════════════════════════
    //  4. TABLEAU DE BORD
    // ══════════════════════════════════════════════════════════
    public static String exportTableauDeBord(String chemin) {
        try {
            PompierService service = new PompierService();
            List<Pompier> pompiers = service.getData();
            List<GestionGardesController.GardeInfo> gardes = service.getAllGardesInfo();
            List<String[]> affectations = service.getAffectations();
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(chemin));
            writer.setPageEvent(new PiedDePage("Tableau de Bord ForestGuard"));
            doc.open();
            ajouterEnTete(doc, "TABLEAU DE BORD", "ForestGuard — " + LocalDateTime.now().format(FMT));

            long disponibles  = pompiers.stream().filter(p -> "disponible".equals(p.getStatut())).count();
            long enMission    = pompiers.stream().filter(p -> "en_mission".equals(p.getStatut())).count();
            long inactifs     = pompiers.stream().filter(p -> "inactif".equals(p.getStatut())).count();
            long gardesActives = gardes.size();
            long affEnCours   = affectations.stream().filter(a -> { String s = a[7] != null ? a[7].toLowerCase() : ""; return s.contains("en_cours") || s.contains("en cours"); }).count();

            ajouterTitreSection(doc, "Statistiques Globales");
            PdfPTable statsTable = new PdfPTable(3);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingBefore(8);
            statsTable.setSpacingAfter(16);
            ajouterStatCard(statsTable, "TOTAL POMPIERS",       String.valueOf(pompiers.size()), VERT_FONCE);
            ajouterStatCard(statsTable, "DISPONIBLES",          String.valueOf(disponibles),     VERT_MOYEN);
            ajouterStatCard(statsTable, "EN MISSION",           String.valueOf(enMission),        ORANGE);
            ajouterStatCard(statsTable, "INACTIFS",             String.valueOf(inactifs),          GRIS_FONCE);
            ajouterStatCard(statsTable, "GARDES ACTIVES",       String.valueOf(gardesActives),    new BaseColor(180, 83, 9));
            ajouterStatCard(statsTable, "AFFECTATIONS EN COURS",String.valueOf(affEnCours),       ROUGE);
            doc.add(statsTable);

            ajouterTitreSection(doc, "Top 5 — Performance IA (Ce mois)");
            PerformanceIA ia = new PerformanceIA();
            List<PerformanceIA.ResultatPerformance> resultats = ia.evaluer("mois");
            List<PerformanceIA.ResultatPerformance> top5 = resultats.stream().limit(5).toList();
            if (top5.isEmpty()) {
                doc.add(creerParagraphe("Aucune donnee de performance disponible.", SOUS_TITRE));
            } else {
                PdfPTable iaTable = new PdfPTable(5);
                iaTable.setWidthPercentage(100);
                iaTable.setSpacingBefore(6);
                iaTable.setSpacingAfter(16);
                iaTable.setWidths(new float[]{0.5f, 2f, 1.5f, 1.5f, 2f});
                ajouterEnTeteTableau(iaTable, "#", "Pompier", "Score", "Missions", "Titre");
                for (int i = 0; i < top5.size(); i++) {
                    PerformanceIA.ResultatPerformance r = top5.get(i);
                    BaseColor bg = i == 0 ? new BaseColor(255, 248, 220) :
                                   i == 1 ? new BaseColor(240, 248, 255) :
                                   i == 2 ? new BaseColor(245, 245, 245) : BLANC;
                    ajouterCellule(iaTable, String.valueOf(i + 1), TEXTE_BOLD, bg, Element.ALIGN_CENTER);
                    ajouterCellule(iaTable, r.prenom + " " + r.nom, TEXTE_BOLD, bg, Element.ALIGN_LEFT);
                    ajouterCellule(iaTable, String.format("%.1f pts", r.scoreTotal), TEXTE_BOLD, bg, Element.ALIGN_CENTER);
                    ajouterCellule(iaTable, r.nbMissions + " mission(s)", TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                    ajouterCellule(iaTable, r.titre != null && !r.titre.isEmpty() ? r.titre : "—", TEXTE_NORMAL, bg, Element.ALIGN_LEFT);
                }
                doc.add(iaTable);
            }

            ajouterTitreSection(doc, "Repartition par Certification");
            long expert = pompiers.stream().filter(p -> "expert".equalsIgnoreCase(p.getNiveauCertification())).count();
            long avance = pompiers.stream().filter(p -> "avance".equalsIgnoreCase(p.getNiveauCertification())).count();
            long inter  = pompiers.stream().filter(p -> "intermediaire".equalsIgnoreCase(p.getNiveauCertification())).count();
            long debut  = pompiers.stream().filter(p -> "debutant".equalsIgnoreCase(p.getNiveauCertification())).count();
            long aucun  = pompiers.stream().filter(p -> p.getNiveauCertification() == null || p.getNiveauCertification().isBlank()).count();
            PdfPTable certifTable = new PdfPTable(5);
            certifTable.setWidthPercentage(100);
            certifTable.setSpacingBefore(6);
            certifTable.setSpacingAfter(16);
            ajouterStatCard(certifTable, "EXPERT",        String.valueOf(expert), new BaseColor(76, 29, 149));
            ajouterStatCard(certifTable, "AVANCE",        String.valueOf(avance), new BaseColor(30, 64, 175));
            ajouterStatCard(certifTable, "INTERMEDIAIRE", String.valueOf(inter),  new BaseColor(3, 105, 161));
            ajouterStatCard(certifTable, "DEBUTANT",      String.valueOf(debut),  GRIS_FONCE);
            ajouterStatCard(certifTable, "AUCUNE",        String.valueOf(aucun),  new BaseColor(100, 116, 139));
            doc.add(certifTable);

            ajouterTitreSection(doc, "5 Dernieres Affectations");
            List<String[]> recentes = affectations.stream()
                    .filter(a -> a.length <= 14 || !"intervention".equals(a[14])).limit(5).toList();
            if (!recentes.isEmpty()) {
                PdfPTable recTable = new PdfPTable(4);
                recTable.setWidthPercentage(100);
                recTable.setSpacingBefore(6);
                recTable.setWidths(new float[]{2f, 2f, 2f, 1.5f});
                ajouterEnTeteTableau(recTable, "Pompier", "Date", "Localisation", "Statut");
                boolean alt = false;
                for (String[] a : recentes) {
                    BaseColor bg = alt ? GRIS_CLAIR : BLANC;
                    String dateAff = a[6] != null && a[6].length() >= 16 ? a[6].substring(0, 16) : (a[6] != null ? a[6] : "—");
                    String statutNorm = a[7] != null ? a[7].toLowerCase() : "";
                    BaseColor couleurStatut = statutNorm.contains("en_cours") || statutNorm.contains("en cours") ? ORANGE_CLAIR :
                            statutNorm.contains("termin") ? VERT_CLAIR : GRIS_CLAIR;
                    ajouterCellule(recTable, a[1] + " " + a[2], TEXTE_BOLD, bg, Element.ALIGN_LEFT);
                    ajouterCellule(recTable, dateAff, TEXTE_NORMAL, bg, Element.ALIGN_CENTER);
                    ajouterCellule(recTable, a[5] != null ? a[5] : "—", TEXTE_NORMAL, bg, Element.ALIGN_LEFT);
                    ajouterCelluleColoree(recTable, a[7] != null ? a[7].toUpperCase().replace("_", " ") : "—", TEXTE_BOLD, couleurStatut);
                    alt = !alt;
                }
                doc.add(recTable);
            }
            ajouterPiedPage(doc);
            doc.close();
            return chemin;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }


    // ══════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════

    private static void ajouterEnTete(Document doc, String titre, String sousTitre) throws DocumentException {
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        header.setSpacingAfter(16);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(VERT_FONCE);
        cell.setPadding(18);
        cell.setBorder(Rectangle.NO_BORDER);
        Paragraph p = new Paragraph();
        p.add(new Chunk("ForestGuard  |  ", new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(165, 214, 167))));
        p.add(new Chunk(titre, TITRE_PRINCIPAL));
        cell.addElement(p);
        Paragraph sub = new Paragraph(sousTitre, new Font(Font.FontFamily.HELVETICA, 11, Font.ITALIC, new BaseColor(200, 230, 201)));
        sub.setSpacingBefore(4);
        cell.addElement(sub);
        header.addCell(cell);
        doc.add(header);
    }

    private static void ajouterTitreSection(Document doc, String titre) throws DocumentException {
        Paragraph p = new Paragraph(titre, TITRE_SECTION);
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        doc.add(p);
        LineSeparator line = new LineSeparator(1.5f, 100, VERT_CLAIR, Element.ALIGN_LEFT, -2);
        doc.add(new Chunk(line));
    }

    private static void ajouterEnTeteTableau(PdfPTable table, String... colonnes) {
        for (String col : colonnes) {
            PdfPCell cell = new PdfPCell(new Phrase(col, TITRE_COLONNE));
            cell.setBackgroundColor(VERT_FONCE);
            cell.setPadding(7);
            cell.setBorderColor(VERT_MOYEN);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private static void ajouterLigneInfo(PdfPTable table, String cle, String valeur) {
        PdfPCell cellCle = new PdfPCell(new Phrase(cle, TEXTE_BOLD));
        cellCle.setBackgroundColor(VERT_PALE);
        cellCle.setPadding(6);
        cellCle.setBorderColor(VERT_CLAIR);
        table.addCell(cellCle);
        PdfPCell cellVal = new PdfPCell(new Phrase(valeur != null ? valeur : "—", TEXTE_NORMAL));
        cellVal.setBackgroundColor(BLANC);
        cellVal.setPadding(6);
        cellVal.setBorderColor(VERT_CLAIR);
        table.addCell(cellVal);
    }

    private static void ajouterCellule(PdfPTable table, String texte, Font font, BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texte != null ? texte : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private static void ajouterCelluleColoree(PdfPTable table, String texte, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texte != null ? texte : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private static void ajouterStatCard(PdfPTable table, String label, String valeur, BaseColor couleur) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(couleur);
        cell.setPadding(12);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p = new Paragraph();
        p.add(new Chunk(valeur + "\n", new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.WHITE)));
        p.add(new Chunk(label, new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, new BaseColor(200, 230, 201))));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        table.addCell(cell);
    }

    private static Paragraph creerParagraphe(String texte, Font font) {
        Paragraph p = new Paragraph(texte, font);
        p.setSpacingBefore(6);
        p.setSpacingAfter(6);
        return p;
    }

    private static void ajouterPiedPage(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        LineSeparator line = new LineSeparator(0.5f, 100, VERT_CLAIR, Element.ALIGN_LEFT, -2);
        doc.add(new Chunk(line));
        Paragraph footer = new Paragraph(
                "Genere le " + LocalDateTime.now().format(FMT) + "  |  ForestGuard — Systeme de Surveillance Forestiere",
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, new BaseColor(148, 163, 184)));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(4);
        doc.add(footer);
    }

    // ── Pied de page avec numéro de page ──
    static class PiedDePage extends PdfPageEventHelper {
        private final String titre;
        PiedDePage(String titre) { this.titre = titre; }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            BaseFont bf;
            try { bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED); }
            catch (Exception e) { return; }
            cb.saveState();
            cb.setFontAndSize(bf, 8);
            cb.setColorFill(new BaseColor(148, 163, 184));
            cb.beginText();
            cb.showTextAligned(PdfContentByte.ALIGN_LEFT, titre,
                    document.leftMargin(), document.bottomMargin() - 10, 0);
            cb.showTextAligned(PdfContentByte.ALIGN_RIGHT,
                    "Page " + writer.getPageNumber(),
                    document.right(), document.bottomMargin() - 10, 0);
            cb.endText();
            cb.restoreState();
        }
    }
}
