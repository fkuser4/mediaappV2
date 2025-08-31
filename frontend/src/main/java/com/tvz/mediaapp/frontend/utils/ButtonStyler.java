package com.tvz.mediaapp.frontend.utils;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.shape.SVGPath;

import java.util.Objects;


public class ButtonStyler {

    private final Button button;
    private String svgPath;
    private int iconSize = 16;

    private String normalSvgColor = "black";
    private String normalBgColor = "transparent";
    private String hoverSvgColor = "black";
    private String hoverBgColor = "#E0E0E0";
    private String selectedSvgColor = "white";
    private String selectedBgColor = "#007BFF";

    private ButtonStyler(Button button) {
        this.button = Objects.requireNonNull(button, "Button cannot be null");
    }

    public static ButtonStyler with(Button button) {
        return new ButtonStyler(button);
    }

    public ButtonStyler svgPath(String svgPath) {
        this.svgPath = svgPath;
        return this;
    }

    public ButtonStyler iconSize(int iconSize) {
        this.iconSize = iconSize;
        return this;
    }

    public ButtonStyler normalColors(String svgColor, String backgroundColor) {
        this.normalSvgColor = svgColor;
        this.normalBgColor = backgroundColor;
        return this;
    }

    public ButtonStyler hoverColors(String svgColor, String backgroundColor) {
        this.hoverSvgColor = svgColor;
        this.hoverBgColor = backgroundColor;
        return this;
    }

    public ButtonStyler selectedColors(String svgColor, String backgroundColor) {
        this.selectedSvgColor = svgColor;
        this.selectedBgColor = backgroundColor;
        return this;
    }

    public void apply() {
        if (svgPath != null && !svgPath.isEmpty()) {
            String svgContent = SVGPathExtractor.extractSVGPath(svgPath);
            SVGPath pathNode = new SVGPath();
            pathNode.setContent(svgContent);
            pathNode.getStyleClass().add("styled-button-svg");

            Group graphic = new Group(pathNode);
            Bounds bounds = graphic.getBoundsInParent();
            double scale = Math.min(iconSize / bounds.getWidth(), iconSize / bounds.getHeight());
            graphic.setScaleX(scale);
            graphic.setScaleY(scale);

            button.setGraphic(graphic);
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        button.getStyleClass().add("custom-styled-button");

        String style = String.format(
                "-fx-normal-svg-color: %s; " +
                        "-fx-normal-bg-color: %s; " +
                        "-fx-hover-svg-color: %s; " +
                        "-fx-hover-bg-color: %s; " +
                        "-fx-selected-svg-color: %s; " +
                        "-fx-selected-bg-color: %s;",
                normalSvgColor, normalBgColor,
                hoverSvgColor, hoverBgColor,
                selectedSvgColor, selectedBgColor
        );

        button.setStyle(style);
    }
}