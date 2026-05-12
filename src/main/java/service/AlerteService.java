package service;

import dao.AlerteDAO;
import model.Alerte;
import java.util.List;

public class AlerteService {

    private final AlerteDAO           dao                 = new AlerteDAO();
    private final NotificationService notificationService = new NotificationService();
    private final EmailService        emailService        = new EmailService();
    private final SmsService          smsService          = new SmsService();

    // ── CREATE — avec email + SMS automatiques ────────────────────────────────
    public boolean creerAlerte(Alerte a) {
        if (a.getTypeAlerte() == null || a.getTypeAlerte().isEmpty()) return false;
        if (a.getLocalisation() == null || a.getLocalisation().isEmpty()) return false;
        if (a.getStatut() == null || a.getStatut().isEmpty()) a.setStatut("Nouvelle");

        // 1. Insérer en BDD
        boolean ok = dao.create(a);
        if (!ok) return false;

        // 2. Notification console
        notificationService.notifier(a);

        // 3. Email à tous les utilisateurs (thread séparé)
        new Thread(() -> {
            System.out.println("📤 Envoi emails utilisateurs...");
            emailService.envoyerATous(a);
        }).start();

        // 4. SMS aux admins (thread séparé — seulement si Critique ou Haute)
        if ("Critique".equals(a.getNiveau()) || "Haute".equals(a.getNiveau())) {
            new Thread(() -> {
                System.out.println("📱 Envoi SMS admins...");
                smsService.envoyerSmsATous(a);
            }).start();
        }

        return true;
    }

    // ── READ ──────────────────────────────────────────────────────────────────
    public List<Alerte> getToutesAlertes()           { return dao.getAll(); }
    public List<Alerte> getAlertesByStatut(String s) { return dao.getByStatut(s); }
    public Alerte       getAlerteById(int id)         { return dao.getById(id); }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    public boolean validerAlerte(int id) {
        if (dao.getById(id) == null) return false;
        return dao.updateStatut(id, "Validée");
    }
    public boolean rejeterAlerte(int id) {
        if (dao.getById(id) == null) return false;
        return dao.updateStatut(id, "Rejetée");
    }
    public boolean modifierAlerte(Alerte a) {
        if (a.getLocalisation() == null || a.getLocalisation().isEmpty()) return false;
        return dao.update(a);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public boolean supprimerAlerte(int id) { return dao.delete(id); }

    // ── STATS ─────────────────────────────────────────────────────────────────
    public int countAll()                   { return dao.countAll(); }
    public int countByStatut(String statut) { return dao.countByStatut(statut); }

    // ── LOGIQUE MÉTIER ────────────────────────────────────────────────────────
    public String calculerNiveau(double temp, double fumee, double hum) {
        if (temp > 70 || fumee > 80 || hum < 10) return "Critique";
        if (temp > 55 || fumee > 50 || hum < 20) return "Haute";
        return "Moyenne";
    }
    public boolean seuilDepasse(double temp, double fumee, double hum) {
        return temp > 60 || fumee > 50 || hum < 20;
    }
    public String determinerType(double temp, double fumee) {
        if (fumee > 50) return "Fumée";
        if (temp > 65)  return "Incendie";
        return "Chaleur excessive";
    }
}
