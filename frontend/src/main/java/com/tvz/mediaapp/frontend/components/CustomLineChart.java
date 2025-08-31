package com.tvz.mediaapp.frontend.components;

import com.tvz.mediaapp.frontend.viewmodel.HomeViewModel;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.WritableValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CustomLineChart extends Canvas {
    private List<Integer> data = new ArrayList<>();
    private Timeline animation;
    private HomeViewModel.Period currentPeriod = HomeViewModel.Period.WEEKLY;
    private static final double PADDING = 40;
    private static final Color GRID_COLOR = Color.web("#2b2c4b");
    private static final Color TEXT_COLOR = Color.web("#716996");
    private static final Color LINE_COLOR = Color.web("#56448e");

    public CustomLineChart() {
        widthProperty().addListener(evt -> draw(1.0));
        heightProperty().addListener(evt -> draw(1.0));
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    public void setData(List<Integer> newData) {
        this.data = newData;
        animateChart();
    }

    public void setPeriod(HomeViewModel.Period period) {
        this.currentPeriod = period;
        draw(1.0);
    }

    private void animateChart() {
        if (animation != null) animation.stop();
        animation = new Timeline();
        KeyValue kv = new KeyValue(new WritableValue<Double>() {
            private double progress = 0;
            @Override public Double getValue() { return progress; }
            @Override public void setValue(Double value) {
                progress = value;
                draw(progress);
            }
        }, 1.0, Interpolator.EASE_OUT);
        KeyFrame kf = new KeyFrame(Duration.millis(800), kv);
        animation.getKeyFrames().add(kf);
        animation.play();
    }

    private void draw(double progress) {
        double width = getWidth();
        double height = getHeight();
        if (width <= 0 || height <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        if (data == null || data.isEmpty()) {
            gc.setFill(TEXT_COLOR);
            gc.setFont(Font.font("Inter", 14));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Loading chart data...", width / 2, height / 2);
            return;
        }

        double chartWidth = width - (PADDING * 2);
        double chartHeight = height - (PADDING * 2);

        int dataMaxValue = Math.max(1, data.stream().max(Integer::compareTo).orElse(1));

        int yAxisMaxValue = (dataMaxValue <= 4) ? 4 : (dataMaxValue % 2 != 0 ? dataMaxValue + 1 : dataMaxValue);

        drawGrid(gc, chartWidth, chartHeight, yAxisMaxValue);

        double xStep = data.size() > 1 ? chartWidth / (data.size() - 1) : 0;
        List<Double> xPoints = new ArrayList<>();
        List<Double> yPoints = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            xPoints.add(PADDING + (i * xStep));
            yPoints.add(PADDING + chartHeight - (data.get(i) / (double) yAxisMaxValue) * chartHeight);
        }

        drawFill(gc, xPoints, yPoints, height, progress);
        drawLineAndPoints(gc, xPoints, yPoints, height, progress);
        drawXAxisLabels(gc, xPoints, height);
    }

    private void drawGrid(GraphicsContext gc, double chartWidth, double chartHeight, int yAxisMaxValue) {
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(1);
        gc.setFill(TEXT_COLOR);
        gc.setFont(Font.font("Inter", 10));
        gc.setTextAlign(TextAlignment.RIGHT);

        for (int i = 0; i <= yAxisMaxValue; i++) {
            double y = PADDING + chartHeight - ((double) i / yAxisMaxValue * chartHeight);
            gc.strokeLine(PADDING, y, PADDING + chartWidth, y);
            gc.fillText(String.valueOf(i), PADDING - 8, y + 4);
        }
    }

    private void drawFill(GraphicsContext gc, List<Double> xPoints, List<Double> yPoints, double height, double progress) {
        gc.setGlobalAlpha(0.2);
        gc.setFill(LINE_COLOR);
        gc.beginPath();
        gc.moveTo(xPoints.get(0), height - PADDING);
        for (int i = 0; i < xPoints.size(); i++) {
            double actualY = yPoints.get(i) + (1 - progress) * (height - PADDING - yPoints.get(i));
            gc.lineTo(xPoints.get(i), actualY);
        }
        gc.lineTo(xPoints.get(xPoints.size() - 1), height - PADDING);
        gc.closePath();
        gc.fill();
    }

    private void drawLineAndPoints(GraphicsContext gc, List<Double> xPoints, List<Double> yPoints, double height, double progress) {
        gc.setGlobalAlpha(1.0);
        gc.setStroke(LINE_COLOR);
        gc.setLineWidth(2.5);
        gc.beginPath();
        for (int i = 0; i < xPoints.size(); i++) {
            double actualY = yPoints.get(i) + (1 - progress) * (height - PADDING - yPoints.get(i));
            if (i == 0) gc.moveTo(xPoints.get(i), actualY);
            else gc.lineTo(xPoints.get(i), actualY);
        }
        gc.stroke();

        gc.setFill(LINE_COLOR);
        for (int i = 0; i < xPoints.size(); i++) {
            double actualY = yPoints.get(i) + (1 - progress) * (height - PADDING - yPoints.get(i));
            gc.fillOval(xPoints.get(i) - 4, actualY - 4, 8, 8);
            gc.setFill(Color.web("#19191d"));
            gc.fillOval(xPoints.get(i) - 2, actualY - 2, 4, 4);
            gc.setFill(LINE_COLOR);
        }
    }

    private void drawXAxisLabels(GraphicsContext gc, List<Double> xPoints, double height) {
        gc.setFill(TEXT_COLOR);
        gc.setFont(Font.font("Inter", 10));
        gc.setTextAlign(TextAlignment.CENTER);

        String[] labels = getLabelsForPeriod();
        for (int i = 0; i < Math.min(labels.length, xPoints.size()); i++) {
            gc.fillText(labels[i], xPoints.get(i), height - PADDING + 20);
        }
    }

    private String[] getLabelsForPeriod() {
        if (currentPeriod == null) currentPeriod = HomeViewModel.Period.WEEKLY;

        switch (currentPeriod) {
            case WEEKLY:
                return new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            case MONTHLY:
                return new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            case YEARLY:
                int currentYear = LocalDate.now().getYear();
                return new String[]{
                        String.valueOf(currentYear - 4),
                        String.valueOf(currentYear - 3),
                        String.valueOf(currentYear - 2),
                        String.valueOf(currentYear - 1),
                        String.valueOf(currentYear)
                };
            default:
                return new String[]{};
        }
    }
}