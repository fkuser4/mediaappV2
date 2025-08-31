package com.tvz.mediaapp.frontend.animation;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.util.Duration;

public class LoadingCircle extends Arc {
    private RotateTransition rotateTransition;

    public LoadingCircle() {
        setupCircle();
        setupAnimation();
    }

    private void setupCircle() {
        setRadiusX(20);
        setRadiusY(20);
        setStartAngle(0);
        setLength(270);
        setType(ArcType.OPEN);
        setFill(null);
        setStroke(Color.WHITE);
        setStrokeWidth(3);

        DropShadow glow = new DropShadow();
        glow.setColor(Color.WHITE);
        glow.setRadius(3);
        glow.setSpread(0.1);
        setEffect(glow);
    }

    private void setupAnimation() {
        rotateTransition = new RotateTransition();
        rotateTransition.setNode(this);
        rotateTransition.setDuration(Duration.seconds(1.2));
        rotateTransition.setFromAngle(0);
        rotateTransition.setToAngle(360);
        rotateTransition.setCycleCount(RotateTransition.INDEFINITE);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
        rotateTransition.play();
    }
}
