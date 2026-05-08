package utils;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import dao.AlerteDAO;
import dao.EquipementDAO;
import model.Equipement;
import model.Intervention;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Générateur de rapports PDF pour les interventions ForestGuard
 */
public class PDFGenerator {

    private static final Color VERT_FORESTGUARD = new Color(22, 163, 74);
    private static final Color TEXTE_PRINCIPAL = new Color(30, 41, 59);
    private static final Color FOND_SECTION = new Color(240, 253, 244);
    private static final Color GRIS_BORDURE = new Color(226, 232, 240);
    
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_SIMPLE = 
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Génère un rapport PDF complet pour une intervention
     * 
     * @param intervention L'intervention à documenter
     * @param destination Le fichier PDF de destination
     * @throws Exception Si erreur lors de la génération
     */
    public static void genererRapportIntervention(Intervention intervention, File destination) 
            throws Exception {
        
        if (intervention == null) {
            throw new IllegalArgumentException("L'intervention ne peut pas être null");
        }
        
        Document document = new Document(PageSize.A4, 36, 36, 36, 50);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(destination));
        
        // Pied de page
        writer.setPageEvent(new PiedDePageEvent());
        
        document.open();
        
        // 1. EN-TÊTE
        ajouterEnTete(document, intervention);
        
        // 2. SECTION IDENTITÉ
        ajouterSectionIdentite(document, intervention);
        
        // 3. SECTION CONTEXTE ALERTE
        ajouterSectionContexteAlerte(document, intervention);
        
        // 4. SECTION ÉQUIPEMENTS
        ajouterSectionEquipements(document, intervention);
        
        // 5. SECTION RÉSULTAT
        ajouterSectionResultat(document, intervention);
        
        // 6. SECTION ANALYSE IA & MÉTÉO
        ajouterSectionAnalyse(document, intervention);
        
        // 7. SECTION SIGNATURE
        ajouterSectionSignature(document, intervention);
        
        document.close();
    }

    /**
     * Ajoute l'en-tête du rapport
     */
    private static void ajouterEnTete(Document document, Intervention intervention) 
            throws DocumentException {
        
        // Logo et titre
        Font fontLogo = new Font(Font.HELVETICA, 18, Font.BOLD, VERT_FORESTGUARD);
        Paragraph logo = new Paragraph("🌲 ForestGuard", fontLogo);
        logo.setAlignment(Element.ALIGN_LEFT);
        document.add(logo);
        
        // Titre rapport
        Font fontTitre = new Font(Font.HELVETICA, 14, Font.BOLD, TEXTE_PRINCIPAL);
        Paragraph titre = new Paragraph("RAPPORT D'INTERVENTION", fontTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingBefore(10);
        document.add(titre);
        
        // Numéro et agent
        Font fontInfo = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXTE_PRINCIPAL);
        Paragraph info = new Paragraph(
            "N° : " + intervention.getAlertZone() + 
            " | Agent : " + intervention.getAgentName(),
            fontInfo
        );
        info.setAlignment(Element.ALIGN_CENTER);
        info.setSpacingBefore(5);
        document.add(info);
        
        // Date génération
        Paragraph dateGen = new Paragraph(
            "Généré le : " + LocalDateTime.now().format(DATE_FORMATTER),
            fontInfo
        );
        dateGen.setAlignment(Element.ALIGN_CENTER);
        dateGen.setSpacingBefore(3);
        dateGen.setSpacingAfter(10);
        document.add(dateGen);
        
        // Ligne verte épaisse
        Paragraph ligneSeparateur = new Paragraph("_________________________________________________________________");
        ligneSeparateur.getFont().setColor(VERT_FORESTGUARD);
        ligneSeparateur.getFont().setSize(14);
        ligneSeparateur.setAlignment(Element.ALIGN_CENTER);
        document.add(ligneSeparateur);
        document.add(new Paragraph(" ")); // Espace
    }

    /**
     * Ajoute la section identité de l'intervention
     */
    private static void ajouterSectionIdentite(Document document, Intervention intervention) 
            throws DocumentException {
        
        ajouterTitreSection(document, "📋 IDENTITÉ DE L'INTERVENTION");
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);
        
        Font fontLabel = new Font(Font.HELVETICA, 10, Font.BOLD, TEXTE_PRINCIPAL);
        Font fontValeur = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXTE_PRINCIPAL);
        
        // Zone d'alerte
        ajouterLigneTableau(table, "Zone d'alerte", intervention.getAlertZone(), 
                           fontLabel, fontValeur);
        
        // Statut avec couleur
        String statut = intervention.getStatut();
        boolean enCours = "In Progress".equals(statut);
        String statutFr = enCours ? "En cours" : "Terminée";
        Color couleurStatut = enCours ? new Color(249, 115, 22) : VERT_FORESTGUARD;
        Font fontStatut = new Font(Font.HELVETICA, 10, Font.BOLD, couleurStatut);
        ajouterLigneTableau(table, "Statut", statutFr, fontLabel, fontStatut);
        
        // Agent responsable
        ajouterLigneTableau(table, "Agent responsable", intervention.getAgentName(), 
                           fontLabel, fontValeur);
        
        // Date de début
        String dateDebut = intervention.getStartDate() != null ? 
                          intervention.getStartDate().format(DATE_FORMATTER) : "-";
        ajouterLigneTableau(table, "Date de début", dateDebut, fontLabel, fontValeur);
        
        // Date de fin
        String dateFin = intervention.getEndDate() != null ? 
                        intervention.getEndDate().format(DATE_FORMATTER) : "En cours";
        ajouterLigneTableau(table, "Date de fin", dateFin, fontLabel, fontValeur);
        
        // Durée
        String duree = calculerDuree(intervention);
        ajouterLigneTableau(table, "Durée", duree, fontLabel, fontValeur);
        
        document.add(table);
    }

    /**
     * Ajoute la section contexte de l'alerte
     */
    private static void ajouterSectionContexteAlerte(Document document, Intervention intervention) 
            throws DocumentException {
        
        ajouterTitreSection(document, "🚨 CONTEXTE DE L'ALERTE");
        
        AlerteDAO alerteDAO = new AlerteDAO();
        String[] niveauEtType = alerteDAO.getNiveauEtType(intervention.getAlerteId());
        String niveau = niveauEtType[0] != null ? niveauEtType[0] : "Non spécifié";
        String type = niveauEtType[1] != null ? niveauEtType[1] : "Non spécifié";
        String localisation = intervention.getAlerteLocalisation() != null ? 
                             intervention.getAlerteLocalisation() : "Non spécifiée";
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);
        
        Font fontLabel = new Font(Font.HELVETICA, 10, Font.BOLD, TEXTE_PRINCIPAL);
        Font fontValeur = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXTE_PRINCIPAL);
        
        ajouterLigneTableau(table, "Type d'alerte", type, fontLabel, fontValeur);
        
        // Niveau avec couleur selon criticité
        Color couleurNiveau = getCouleurNiveau(niveau);
        Font fontNiveau = new Font(Font.HELVETICA, 10, Font.BOLD, couleurNiveau);
        ajouterLigneTableau(table, "Niveau", niveau, fontLabel, fontNiveau);
        
        ajouterLigneTableau(table, "Localisation", localisation, fontLabel, fontValeur);
        
        document.add(table);
    }

    /**
     * Ajoute la section équipements mobilisés
     */
    private static void ajouterSectionEquipements(Document document, Intervention intervention) 
            throws DocumentException {
        
        ajouterTitreSection(document, "🚁 ÉQUIPEMENTS MOBILISÉS");
        
        EquipementDAO equipementDAO = new EquipementDAO();
        List<Equipement> equipements = equipementDAO.getByIntervention(
            intervention.getAlertZone(), 
            intervention.getAgentName()
        );
        
        if (equipements == null || equipements.isEmpty()) {
            Font fontItalic = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);
            Paragraph aucun = new Paragraph("Aucun équipement enregistré", fontItalic);
            aucun.setSpacingBefore(10);
            aucun.setSpacingAfter(15);
            document.add(aucun);
            return;
        }
        
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(15);
        
        try {
            table.setWidths(new float[]{3, 2});
        } catch (DocumentException e) {
            // Ignore, utilise largeurs par défaut
        }
        
        Font fontHeader = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        Font fontCell = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXTE_PRINCIPAL);
        
        // En-têtes
        PdfPCell headerNom = creerCelluleHeader("Nom de l'équipement", fontHeader);
        PdfPCell headerType = creerCelluleHeader("Type", fontHeader);
        table.addCell(headerNom);
        table.addCell(headerType);
        
        // Lignes équipements
        for (Equipement eq : equipements) {
            PdfPCell cellNom = new PdfPCell(new Phrase(eq.getNom(), fontCell));
            cellNom.setPadding(8);
            cellNom.setBorder(Rectangle.BOX);
            cellNom.setBorderColor(GRIS_BORDURE);
            
            PdfPCell cellType = new PdfPCell(new Phrase(eq.getType(), fontCell));
            cellType.setPadding(8);
            cellType.setBorder(Rectangle.BOX);
            cellType.setBorderColor(GRIS_BORDURE);
            
            table.addCell(cellNom);
            table.addCell(cellType);
        }
        
        document.add(table);
    }

    /**
     * Ajoute la section résultat de l'intervention
     */
    private static void ajouterSectionResultat(Document document, Intervention intervention) 
            throws DocumentException {
        
        ajouterTitreSection(document, "📝 RÉSULTAT DE L'INTERVENTION");
        
        String resultat = intervention.getResultat();
        boolean enCours = "In Progress".equals(intervention.getStatut());
        
        Font fontTexte = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXTE_PRINCIPAL);
        
        if (resultat == null || resultat.trim().isEmpty()) {
            if (enCours) {
                Font fontItalic = new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY);
                Paragraph texte = new Paragraph("Intervention en cours...", fontItalic);
                texte.setSpacingBefore(10);
                texte.setSpacingAfter(15);
                document.add(texte);
            } else {
                Paragraph texte = new Paragraph("Aucun résultat enregistré", fontTexte);
                texte.setSpacingBefore(10);
                texte.setSpacingAfter(15);
                document.add(texte);
            }
        } else {
            Paragraph texte = new Paragraph(resultat, fontTexte);
            texte.setSpacingBefore(10);
            texte.setSpacingAfter(15);
            texte.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(texte);
        }
    }

    /**
     * Ajoute la section analyse IA et météo
     */
    private static void ajouterSectionAnalyse(Document document, Intervention intervention) 
            throws DocumentException {
        
        ajouterTitreSection(document, "🧠 ANALYSE CONTEXTUELLE");
        
        Font fontLabel = new Font(Font.HELVETICA, 10, Font.BOLD, TEXTE_PRINCIPAL);
        Font fontValeur = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXTE_PRINCIPAL);
        Font fontItalic = new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY);
        
        // Météo
        String localisation = intervention.getAlerteLocalisation();
        String meteo = "Non disponible";
        if (localisation != null && !localisation.isEmpty()) {
            try {
                MeteoService meteoService = new MeteoService();
                MeteoService.DonneesMeteo donneesMeteo = meteoService.getMeteo(localisation);
                if (donneesMeteo != null && donneesMeteo.succes) {
                    meteo = donneesMeteo.toString();
                } else {
                    meteo = "Données météo non disponibles pour cette zone";
                }
            } catch (Exception e) {
                meteo = "Erreur lors de la récupération des données météo";
            }
        }
        
        Paragraph pMeteo = new Paragraph();
        pMeteo.add(new Chunk("Météo de la zone : ", fontLabel));
        pMeteo.add(new Chunk(meteo, fontValeur));
        pMeteo.setSpacingBefore(10);
        document.add(pMeteo);
        
        // Score de risque
        AlerteDAO alerteDAO = new AlerteDAO();
        String[] niveauEtType = alerteDAO.getNiveauEtType(intervention.getAlerteId());
        String niveau = niveauEtType[0];
        String type = niveauEtType[1];
        
        if (niveau != null && type != null) {
            try {
                // Récupérer les données météo pour le calcul
                MeteoService meteoService = new MeteoService();
                MeteoService.DonneesMeteo donneesMeteo = meteoService.getMeteo(localisation);
                
                if (donneesMeteo != null && donneesMeteo.succes) {
                    int scoreRisque = IAService.calculerScoreRisque(
                        donneesMeteo.temperature,
                        donneesMeteo.humidite,
                        donneesMeteo.vent,
                        niveau
                    );
                    String niveauRisque = IAService.getNiveauRisque(scoreRisque);
                    
                    Paragraph pScore = new Paragraph();
                    pScore.add(new Chunk("Score de risque : ", fontLabel));
                    pScore.add(new Chunk(scoreRisque + "/100", fontValeur));
                    pScore.setSpacingBefore(5);
                    document.add(pScore);
                    
                    Paragraph pNiveau = new Paragraph();
                    pNiveau.add(new Chunk("Niveau de risque : ", fontLabel));
                    Color couleurRisque = getCouleurNiveau(niveauRisque);
                    Font fontRisque = new Font(Font.HELVETICA, 10, Font.BOLD, couleurRisque);
                    pNiveau.add(new Chunk(niveauRisque, fontRisque));
                    pNiveau.setSpacingBefore(5);
                    document.add(pNiveau);
                } else {
                    Paragraph pErreur = new Paragraph("Analyse de risque non disponible (données météo manquantes)", fontItalic);
                    pErreur.setSpacingBefore(5);
                    document.add(pErreur);
                }
                
            } catch (Exception e) {
                Paragraph pErreur = new Paragraph("Analyse de risque non disponible", fontItalic);
                pErreur.setSpacingBefore(5);
                document.add(pErreur);
            }
        }
        
        // Recommandation IA
        if (niveau != null && type != null) {
            try {
                GeminiService geminiService = new GeminiService(intervention.getAgentName());
                String recommandation = geminiService.conseilsEquipements(type, niveau);
                
                if (recommandation != null && !recommandation.isEmpty() 
                    && !recommandation.startsWith("❌") && !recommandation.startsWith("ERREUR")) {
                    Paragraph pReco = new Paragraph();
                    pReco.add(new Chunk("Recommandation IA : ", fontLabel));
                    pReco.setSpacingBefore(10);
                    document.add(pReco);
                    
                    Paragraph pTexteReco = new Paragraph(recommandation, fontValeur);
                    pTexteReco.setSpacingBefore(5);
                    pTexteReco.setAlignment(Element.ALIGN_JUSTIFIED);
                    document.add(pTexteReco);
                }
            } catch (Exception e) {
                // Ignore si service IA non disponible
            }
        }
        
        document.add(new Paragraph(" ")); // Espace
        document.add(new Paragraph(" "));
    }

    /**
     * Ajoute la section signature
     */
    private static void ajouterSectionSignature(Document document, Intervention intervention) 
            throws DocumentException {
        
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        
        Font fontLabel = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXTE_PRINCIPAL);
        
        Paragraph pSignature = new Paragraph(
            "Signature de l'agent : _________________________________", 
            fontLabel
        );
        pSignature.setSpacingBefore(20);
        document.add(pSignature);
        
        Paragraph pValidation = new Paragraph(
            "Validé par : _________________________________", 
            fontLabel
        );
        pValidation.setSpacingBefore(15);
        document.add(pValidation);
        
        Paragraph pDate = new Paragraph(
            "Date : _________________________________", 
            fontLabel
        );
        pDate.setSpacingBefore(15);
        document.add(pDate);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Ajoute un titre de section
     */
    private static void ajouterTitreSection(Document document, String titre) 
            throws DocumentException {
        
        Font fontTitre = new Font(Font.HELVETICA, 11, Font.BOLD, VERT_FORESTGUARD);
        Paragraph p = new Paragraph(titre, fontTitre);
        p.setSpacingBefore(15);
        
        // Fond coloré
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(15);
        
        PdfPCell cell = new PdfPCell(new Phrase(titre, fontTitre));
        cell.setBackgroundColor(FOND_SECTION);
        cell.setPadding(8);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
        
        document.add(table);
    }

    /**
     * Ajoute une ligne dans un tableau (label + valeur)
     */
    private static void ajouterLigneTableau(PdfPTable table, String label, String valeur,
                                           Font fontLabel, Font fontValeur) {
        
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, fontLabel));
        cellLabel.setPadding(8);
        cellLabel.setBorder(Rectangle.BOX);
        cellLabel.setBorderColor(GRIS_BORDURE);
        cellLabel.setBackgroundColor(new Color(248, 250, 252));
        
        PdfPCell cellValeur = new PdfPCell(new Phrase(valeur, fontValeur));
        cellValeur.setPadding(8);
        cellValeur.setBorder(Rectangle.BOX);
        cellValeur.setBorderColor(GRIS_BORDURE);
        
        table.addCell(cellLabel);
        table.addCell(cellValeur);
    }

    /**
     * Crée une cellule d'en-tête de tableau
     */
    private static PdfPCell creerCelluleHeader(String texte, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(VERT_FORESTGUARD);
        cell.setPadding(8);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(VERT_FORESTGUARD);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    /**
     * Calcule la durée de l'intervention
     */
    private static String calculerDuree(Intervention intervention) {
        if (intervention.getStartDate() == null) {
            return "-";
        }
        
        LocalDateTime debut = intervention.getStartDate();
        LocalDateTime fin = intervention.getEndDate();
        
        if (fin == null) {
            // Intervention en cours
            Duration duree = Duration.between(debut, LocalDateTime.now());
            return formatDuree(duree) + " (en cours)";
        } else {
            Duration duree = Duration.between(debut, fin);
            return formatDuree(duree);
        }
    }

    /**
     * Formate une durée en texte lisible
     */
    private static String formatDuree(Duration duree) {
        long heures = duree.toHours();
        long minutes = duree.toMinutesPart();
        
        if (heures > 0) {
            return heures + "h " + minutes + "min";
        } else {
            return minutes + " minutes";
        }
    }

    /**
     * Retourne la couleur selon le niveau d'alerte
     */
    private static Color getCouleurNiveau(String niveau) {
        if (niveau == null) return Color.GRAY;
        
        switch (niveau.toLowerCase()) {
            case "critique":
                return new Color(220, 38, 38); // Rouge
            case "haute":
            case "élevée":
            case "elevee":
            case "high":
                return new Color(249, 115, 22); // Orange
            case "moyenne":
            case "medium":
                return new Color(234, 179, 8); // Jaune
            case "faible":
            case "basse":
            case "low":
                return VERT_FORESTGUARD; // Vert
            default:
                return Color.GRAY;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLASSE INTERNE : PIED DE PAGE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gère le pied de page de chaque page du PDF
     */
    static class PiedDePageEvent extends PdfPageEventHelper {
        
        public void onEndPage(PdfWriter writer, Document document) {
            Font fontPied = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);
            
            PdfPTable footer = new PdfPTable(2);
            try {
                footer.setWidths(new int[]{60, 40});
                footer.setTotalWidth(document.getPageSize().getWidth() - 72);
                footer.setLockedWidth(true);
                
                // Texte gauche
                PdfPCell leftCell = new PdfPCell(
                    new Phrase("ForestGuard — Document confidentiel", fontPied)
                );
                leftCell.setBorder(Rectangle.NO_BORDER);
                leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                footer.addCell(leftCell);
                
                // Numéro de page droite
                PdfPCell rightCell = new PdfPCell(
                    new Phrase("Page " + writer.getPageNumber(), fontPied)
                );
                rightCell.setBorder(Rectangle.NO_BORDER);
                rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                footer.addCell(rightCell);
                
                // Position du pied de page
                footer.writeSelectedRows(0, -1, 36, 30, writer.getDirectContent());
                
            } catch (DocumentException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
