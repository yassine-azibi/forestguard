package service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import dao.AlerteDAO;
import model.Alerte;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SERVICE RAPPORT PDF — iText 5
 * Génère un rapport PDF professionnel avec :
 * - Page de titre
 * - Statistiques générales
 * - Répartition par niveau et statut
 * - Tableau complet des alertes
 */
public class RapportService {

    private final AlerteDAO dao = new AlerteDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Couleurs ForestGuard ──────────────────────────────────────────────────
    private static final BaseColor VERT_FONCE  = new BaseColor(13, 31, 20);
    private static final BaseColor VERT_ACCENT = new BaseColor(22, 163, 74);
    private static final BaseColor VERT_CLAIR  = new BaseColor(74, 222, 128);
    private static final BaseColor ROUGE       = new BaseColor(198, 40, 40);
    private static final BaseColor ORANGE      = new BaseColor(230, 81, 0);
    private static final BaseColor BLEU        = new BaseColor(21, 101, 192);
    private static final BaseColor GRIS_CLAIR  = new BaseColor(241, 250, 241);
    private static final BaseColor GRIS        = new BaseColor(148, 163, 184);
    private static final BaseColor BLANC       = BaseColor.WHITE;
    private static final BaseColor NOIR        = BaseColor.BLACK;

    // ── Polices ───────────────────────────────────────────────────────────────
    private static final Font TITRE_PRINCIPAL = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 28, VERT_ACCENT);
    private static final Font TITRE_SECTION   = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 14, VERT_ACCENT);
    private static final Font TEXTE_NORMAL    = FontFactory.getFont(
            FontFactory.HELVETICA, 10, NOIR);
    private static final Font TEXTE_BLANC     = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 10, BLANC);
    private static final Font TEXTE_GRIS      = FontFactory.getFont(
            FontFactory.HELVETICA, 9, GRIS);
    private static final Font TEXTE_BOLD      = FontFactory.getFont(
            FontFactory.HELVETICA_BOLD, 10, NOIR);
    private static final Font SOUS_TITRE      = FontFactory.getFont(
            FontFactory.HELVETICA, 13, GRIS);

    // ══════════════════════════════════════════════════════════════════════════
    //  METHODE PRINCIPALE — Génère le PDF et retourne le chemin
    // ══════════════════════════════════════════════════════════════════════════

    public String genererRapportPDF(String dossier) throws Exception {
        List<Alerte> alertes = dao.getAll();
        String dateGen = LocalDateTime.now().format(FMT);
        String nom = "rapport_forestguard_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            + ".pdf";
        String chemin = dossier + java.io.File.separator + nom;

        Document doc = new Document(PageSize.A4, 40, 40, 60, 60);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(chemin));

        // En-tête et pied de page sur chaque page
        writer.setPageEvent(new EntetePagePdf(dateGen));

        doc.open();

        // ── Page de titre ────────────────────────────────────────────────────
        ajouterPageTitre(doc, alertes.size(), dateGen);
        doc.newPage();

        // ── Statistiques générales ───────────────────────────────────────────
        ajouterStatistiques(doc, alertes);
        doc.newPage();

        // ── Tableau des alertes ──────────────────────────────────────────────
        ajouterTableauAlertes(doc, alertes);

        doc.close();
        return chemin;
    }

    // ── Page de titre ─────────────────────────────────────────────────────────
    private void ajouterPageTitre(Document doc, int total, String dateGen) throws Exception {
        // Fond vert foncé simulé avec un rectangle
        doc.add(new Chunk("\n\n\n"));

        // Logo textuel
        Paragraph logo = new Paragraph("ForestGuard", TITRE_PRINCIPAL);
        logo.setAlignment(Element.ALIGN_CENTER);
        doc.add(logo);

        Paragraph sousTitre = new Paragraph("Systeme de Surveillance Forestiere", SOUS_TITRE);
        sousTitre.setAlignment(Element.ALIGN_CENTER);
        sousTitre.setSpacingBefore(8);
        doc.add(sousTitre);

        // Ligne séparatrice verte
        com.itextpdf.text.pdf.draw.LineSeparator sep =
            new com.itextpdf.text.pdf.draw.LineSeparator(2, 60, VERT_ACCENT, Element.ALIGN_CENTER, -5);
        doc.add(new Chunk(sep));
        doc.add(new Chunk("\n\n\n"));

        // Titre du rapport
        Font titreRapport = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, VERT_FONCE);
        Paragraph titreDoc = new Paragraph("RAPPORT D'ALERTES FORESTIERES", titreRapport);
        titreDoc.setAlignment(Element.ALIGN_CENTER);
        titreDoc.setSpacingBefore(20);
        doc.add(titreDoc);

        doc.add(new Chunk("\n\n"));

        // Encadré statistiques
        PdfPTable tableInfo = new PdfPTable(2);
        tableInfo.setWidthPercentage(60);
        tableInfo.setHorizontalAlignment(Element.ALIGN_CENTER);
        tableInfo.setWidths(new float[]{1, 1});

        ajouterCellInfo(tableInfo, "Date de generation", dateGen);
        ajouterCellInfo(tableInfo, "Total alertes", String.valueOf(total));
        ajouterCellInfo(tableInfo, "Couverture", "Toutes zones Tunisie");
        ajouterCellInfo(tableInfo, "Systeme", "ForestGuard v1.0");

        doc.add(tableInfo);
    }

    // ── Statistiques ─────────────────────────────────────────────────────────
    private void ajouterStatistiques(Document doc, List<Alerte> alertes) throws Exception {
        long total    = alertes.size();
        long nouv     = alertes.stream().filter(a -> "Nouvelle".equals(a.getStatut())).count();
        long val      = alertes.stream().filter(a -> a.getStatut() != null
                            && a.getStatut().startsWith("Valid")).count();
        long rej      = alertes.stream().filter(a -> a.getStatut() != null
                            && a.getStatut().startsWith("Rejet")).count();
        long critique = alertes.stream().filter(a -> "Critique".equals(a.getNiveau())).count();
        long haute    = alertes.stream().filter(a -> "Haute".equals(a.getNiveau())).count();
        long moyenne  = alertes.stream().filter(a -> "Moyenne".equals(a.getNiveau())).count();

        // Titre section
        doc.add(titreSect("Statistiques Generales"));

        // Tableau stats statuts
        PdfPTable t1 = new PdfPTable(4);
        t1.setWidthPercentage(100);
        t1.setSpacingBefore(10);
        t1.setSpacingAfter(20);

        ajouterCellStat(t1, "TOTAL",     String.valueOf(total),    VERT_ACCENT);
        ajouterCellStat(t1, "NOUVELLES", String.valueOf(nouv),     BLEU);
        ajouterCellStat(t1, "VALIDEES",  String.valueOf(val),      VERT_ACCENT);
        ajouterCellStat(t1, "REJETEES",  String.valueOf(rej),      ROUGE);
        doc.add(t1);

        // Titre section niveaux
        doc.add(titreSect("Repartition par Niveau de Gravite"));

        PdfPTable t2 = new PdfPTable(3);
        t2.setWidthPercentage(100);
        t2.setSpacingBefore(10);
        t2.setSpacingAfter(20);

        ajouterCellStat(t2, "CRITIQUE", String.valueOf(critique), ROUGE);
        ajouterCellStat(t2, "HAUTE",    String.valueOf(haute),    ORANGE);
        ajouterCellStat(t2, "MOYENNE",  String.valueOf(moyenne),  BLEU);
        doc.add(t2);

        // Repartition par type
        doc.add(titreSect("Repartition par Type d'Alerte"));

        Map<String, Long> parType = alertes.stream()
            .collect(Collectors.groupingBy(Alerte::getTypeAlerte, Collectors.counting()));

        PdfPTable t3 = new PdfPTable(3);
        t3.setWidthPercentage(100);
        t3.setSpacingBefore(10);
        t3.setSpacingAfter(20);

        // Header
        ajouterCellHeader(t3, "Type d'alerte");
        ajouterCellHeader(t3, "Nombre");
        ajouterCellHeader(t3, "Pourcentage");

        boolean alt = false;
        for (Map.Entry<String, Long> e : parType.entrySet()) {
            BaseColor bg = alt ? GRIS_CLAIR : BLANC;
            ajouterCellData(t3, e.getKey(),   TEXTE_NORMAL, bg);
            ajouterCellData(t3, String.valueOf(e.getValue()), TEXTE_BOLD, bg);
            int pct = total > 0 ? (int)(e.getValue() * 100 / total) : 0;
            ajouterCellData(t3, pct + "%", TEXTE_NORMAL, bg);
            alt = !alt;
        }
        doc.add(t3);
    }

    // ── Tableau alertes ───────────────────────────────────────────────────────
    private void ajouterTableauAlertes(Document doc, List<Alerte> alertes) throws Exception {
        doc.add(titreSect("Liste Complete des Alertes (" + alertes.size() + ")"));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{0.5f, 1.5f, 1f, 2.5f, 1.2f, 1.2f});

        // Headers
        String[] headers = {"ID", "Type", "Niveau", "Localisation", "Date", "Statut"};
        for (String h : headers) ajouterCellHeader(table, h);

        // Données
        boolean alt = false;
        for (Alerte a : alertes) {
            BaseColor bg = alt ? GRIS_CLAIR : BLANC;

            // Couleur selon niveau
            BaseColor colNiveau = switch (a.getNiveau() != null ? a.getNiveau() : "") {
                case "Critique" -> ROUGE;
                case "Haute"    -> ORANGE;
                default         -> BLEU;
            };
            BaseColor colStatut = (a.getStatut() != null && a.getStatut().startsWith("Valid"))
                    ? VERT_ACCENT
                    : (a.getStatut() != null && a.getStatut().startsWith("Rejet"))
                    ? ROUGE : BLEU;

            ajouterCellData(table, String.valueOf(a.getId()), TEXTE_GRIS, bg);
            ajouterCellData(table, safe(a.getTypeAlerte()),   TEXTE_NORMAL, bg);

            // Badge niveau coloré
            PdfPCell cellNiv = new PdfPCell(new Phrase(safe(a.getNiveau()), TEXTE_BLANC));
            cellNiv.setBackgroundColor(colNiveau);
            cellNiv.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellNiv.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellNiv.setPadding(5);
            cellNiv.setBorder(Rectangle.NO_BORDER);
            table.addCell(cellNiv);

            // Localisation nettoyée
            String loc = safe(a.getLocalisation()).replaceAll("\\[.*\\]", "").trim();
            if (loc.length() > 35) loc = loc.substring(0, 35) + "...";
            ajouterCellData(table, loc, TEXTE_NORMAL, bg);
            ajouterCellData(table, safe(a.getDateFormatted()), TEXTE_GRIS, bg);

            // Badge statut coloré
            PdfPCell cellStat = new PdfPCell(new Phrase(safe(a.getStatut()), TEXTE_BLANC));
            cellStat.setBackgroundColor(colStatut);
            cellStat.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellStat.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellStat.setPadding(5);
            cellStat.setBorder(Rectangle.NO_BORDER);
            table.addCell(cellStat);

            alt = !alt;
        }
        doc.add(table);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Paragraph titreSect(String texte) {
        Paragraph p = new Paragraph(texte, TITRE_SECTION);
        p.setSpacingBefore(16);
        p.setSpacingAfter(6);
        // Ligne verte dessous
        p.add(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(1, 100, VERT_ACCENT, Element.ALIGN_LEFT, -3)));
        return p;
    }

    private void ajouterCellStat(PdfPTable t, String label, String val, BaseColor col) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(col);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(14);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font fontVal   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, BLANC);
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, new BaseColor(220,255,220));

        Paragraph p = new Paragraph();
        p.add(new Chunk(val + "\n", fontVal));
        p.add(new Chunk(label, fontLabel));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        t.addCell(cell);
    }

    private void ajouterCellHeader(PdfPTable t, String texte) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, TEXTE_BLANC));
        cell.setBackgroundColor(VERT_ACCENT);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorder(Rectangle.NO_BORDER);
        t.addCell(cell);
    }

    private void ajouterCellData(PdfPTable t, String texte, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderColorBottom(new BaseColor(220, 240, 220));
        t.addCell(cell);
    }

    private void ajouterCellInfo(PdfPTable t, String label, String val) {
        Font fl = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, GRIS);
        Font fv = FontFactory.getFont(FontFactory.HELVETICA, 10, VERT_FONCE);
        PdfPCell cl = new PdfPCell(new Phrase(label, fl));
        PdfPCell cv = new PdfPCell(new Phrase(val, fv));
        cl.setBorder(Rectangle.BOX);
        cv.setBorder(Rectangle.BOX);
        cl.setBackgroundColor(GRIS_CLAIR);
        cv.setBackgroundColor(BLANC);
        cl.setPadding(8);
        cv.setPadding(8);
        t.addCell(cl);
        t.addCell(cv);
    }

    private String safe(String s) { return s != null ? s : ""; }

    // ══════════════════════════════════════════════════════════════════════════
    //  EN-TÊTE ET PIED DE PAGE
    // ══════════════════════════════════════════════════════════════════════════

    static class EntetePagePdf extends PdfPageEventHelper {
        private final String dateGen;
        EntetePagePdf(String dateGen) { this.dateGen = dateGen; }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle page = document.getPageSize();

            // Barre verte en haut
            cb.setColorFill(new BaseColor(22, 163, 74));
            cb.rectangle(0, page.getHeight() - 30, page.getWidth(), 30);
            cb.fill();

            // Texte en-tête
            Font fHdr = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.WHITE);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                new Phrase("ForestGuard — Rapport d'Alertes Forestieres", fHdr),
                40, page.getHeight() - 18, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                new Phrase(dateGen, fHdr),
                page.getWidth() - 40, page.getHeight() - 18, 0);

            // Barre verte en bas
            cb.setColorFill(new BaseColor(13, 31, 20));
            cb.rectangle(0, 0, page.getWidth(), 25);
            cb.fill();

            // Numéro de page
            Font fFtr = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.WHITE);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                new Phrase("Page " + writer.getPageNumber()
                    + " | ForestGuard — Surveillance Forestiere Tunisie", fFtr),
                page.getWidth() / 2, 8, 0);
        }
    }
}
