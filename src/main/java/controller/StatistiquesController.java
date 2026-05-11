package controller;

import dao.InterventionDAO;
import model.Intervention;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class StatistiquesController implements Initializable {

    // ── Stat cards ────────────────────────────────────────────────────────
    @FXML private Label lblTotal, lblMax, lblMoyenne, lblTaux;
    @FXML private VBox  cardTotal, cardMax, cardMoyenne, cardTaux;

    // ── LineChart ─────────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis              xAxis;
    @FXML private NumberAxis                yAxis;

    // ── BarChart ──────────────────────────────────────────────────────────
    @FXML private BarChart<String, Number>  barChart;
    @FXML private CategoryAxis              xAxisBar;
    @FXML private NumberAxis                yAxisBar;

    // ── PieChart ──────────────────────────────────────────────────────────
    @FXML private PieChart pieChart;

    private final InterventionDAO dao         = new InterventionDAO();
    private final String          currentAgent = "John Martinez";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupCardHover();
        chargerTout();
    }

    private void chargerTout() {
        List<Intervention> toutes = dao.getByAgent(currentAgent);
        styliserCharts();
        chargerStats(toutes);
        chargerLineChart();
        chargerBarChart(toutes);
        chargerPieChart(toutes);
    }

    // ── Style charts ──────────────────────────────────────────────────────
    private void styliserCharts() {
        lineChart.setStyle("-fx-background-color: white;");
        xAxis.setStyle("-fx-tick-label-fill: #374151;");
        yAxis.setStyle("-fx-tick-label-fill: #374151;");
        xAxis.setLabel("Date");
        yAxis.setLabel("Nombre");

        barChart.setStyle("-fx-background-color: white;");
        xAxisBar.setStyle("-fx-tick-label-fill: #374151;");
        yAxisBar.setStyle("-fx-tick-label-fill: #374151;");
        xAxisBar.setLabel("Zone");
        yAxisBar.setLabel("Nombre");
        yAxisBar.setTickUnit(1);

        pieChart.setStyle("-fx-background-color: white;");
    }

    // ── Stats cards ───────────────────────────────────────────────────────
    private void chargerStats(List<Intervention> toutes) {
        int total    = toutes.size();
        int termines = (int) toutes.stream()
                .filter(i -> "Completed".equals(i.getStatut())).count();

        Map<String, Long> parJour = dao.countByDay(currentAgent);
        int max = parJour.values().stream()
                .mapToLong(Long::longValue)
                .mapToInt(l -> (int) l)
                .max().orElse(0);
        double moyenne = parJour.values().stream()
                .mapToLong(Long::longValue)
                .average().orElse(0);
        double taux = total > 0 ? (termines * 100.0 / total) : 0;

        lblTotal.setText(String.valueOf(total));
        lblMax.setText(String.valueOf(max));
        lblMoyenne.setText(String.format("%.1f", moyenne));
        lblTaux.setText(String.format("%.0f%%", taux));
    }

    // ── LineChart ─────────────────────────────────────────────────────────
    private void chargerLineChart() {
        lineChart.getData().clear();
        lineChart.setAnimated(true);
        lineChart.setLegendVisible(false);
        yAxis.setTickUnit(1);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Interventions");

        dao.countByDay(currentAgent).forEach((date, count) ->
                series.getData().add(new XYChart.Data<>(date, count)));

        lineChart.getData().add(series);

        Platform.runLater(() -> {
            if (series.getNode() != null) {
                series.getNode().setStyle(
                        "-fx-stroke: #16a34a; -fx-stroke-width: 3;");
            }
            series.getData().forEach(d -> {
                if (d.getNode() != null) {
                    d.getNode().setStyle(
                            "-fx-background-color: #16a34a, white;" +
                                    "-fx-background-radius: 5px; -fx-padding: 4px;");
                }
            });

            // Labels X lisibles
            lineChart.lookupAll(".axis-label").forEach(n ->
                    n.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px;"));
            lineChart.lookupAll(".tick-mark").forEach(n ->
                    n.setStyle("-fx-stroke: #374151;"));
        });
    }

    // ── BarChart ──────────────────────────────────────────────────────────
    private void chargerBarChart(List<Intervention> toutes) {
        barChart.getData().clear();
        barChart.setAnimated(true);
        barChart.setLegendVisible(false);
        yAxisBar.setTickUnit(1);

        Map<String, Long> parZone = toutes.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getAlertZone().length() > 15
                                ? i.getAlertZone().substring(0, 15) + "..."
                                : i.getAlertZone(),
                        Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Interventions");

        parZone.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .forEach(e -> series.getData().add(
                        new XYChart.Data<>(e.getKey(), e.getValue())));

        barChart.getData().add(series);

        Platform.runLater(() -> {
            series.getData().forEach(d -> {
                if (d.getNode() != null)
                    d.getNode().setStyle("-fx-bar-fill: #16a34a;");
            });

            barChart.lookupAll(".axis-label").forEach(n ->
                    n.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px;"));
            barChart.lookupAll(".chart-plot-background").forEach(n ->
                    n.setStyle("-fx-background-color: #f8fafc;"));
        });
    }

    // ── PieChart ──────────────────────────────────────────────────────────
    private void chargerPieChart(List<Intervention> toutes) {
        long enCours  = toutes.stream()
                .filter(i -> "In Progress".equals(i.getStatut())).count();
        long termines = toutes.stream()
                .filter(i -> "Completed".equals(i.getStatut())).count();
        long total    = toutes.size();

        pieChart.getData().clear();
        pieChart.setAnimated(true);
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);

        if (termines > 0) {
            double pct = total > 0 ? (termines * 100.0 / total) : 0;
            pieChart.getData().add(new PieChart.Data(
                    String.format("Terminées (%d) — %.0f%%", termines, pct),
                    termines));
        }
        if (enCours > 0) {
            double pct = total > 0 ? (enCours * 100.0 / total) : 0;
            pieChart.getData().add(new PieChart.Data(
                    String.format("En Cours (%d) — %.0f%%", enCours, pct),
                    enCours));
        }

        Platform.runLater(() -> {
            if (!pieChart.getData().isEmpty())
                pieChart.getData().get(0).getNode()
                        .setStyle("-fx-pie-color: #16a34a;");
            if (pieChart.getData().size() > 1)
                pieChart.getData().get(1).getNode()
                        .setStyle("-fx-pie-color: #f97316;");

            // Labels noirs
            pieChart.lookupAll(".chart-pie-label").forEach(n ->
                    n.setStyle("-fx-fill: #374151; -fx-font-size: 11px;"));
            pieChart.lookupAll(".chart-legend-item").forEach(n ->
                    n.setStyle("-fx-text-fill: #374151; -fx-font-size: 11px;"));
        });
    }

    // ── Card hover ────────────────────────────────────────────────────────
    private void setupCardHover() {
        VBox[] cards = {cardTotal, cardMax, cardMoyenne, cardTaux};
        for (VBox card : cards) {
            if (card == null) continue;
            String base = card.getStyle();
            card.setOnMouseEntered(e -> card.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.18);" +
                            "-fx-background-radius: 20;" +
                            "-fx-border-color: rgba(255,255,255,0.35);" +
                            "-fx-border-radius: 20; -fx-border-width: 1;" +
                            "-fx-padding: 20 24; -fx-cursor: hand;" +
                            "-fx-scale-x: 1.04; -fx-scale-y: 1.04;"));
            card.setOnMouseExited(e -> card.setStyle(base));
        }
    }

    @FXML
    private void handleRetour() {
        lineChart.getScene().getWindow().hide();
    }
}
