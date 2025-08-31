package com.tvz.mediaapp.frontend.utils;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class WindowResizeHandler implements EventHandler<MouseEvent> {

    private final Stage stage;
    private final int border = 8;

    private Cursor cursorEvent = Cursor.DEFAULT;

    private double startX;
    private double startY;
    private double startWidth;
    private double startHeight;
    private double startScreenX;
    private double startScreenY;

    public WindowResizeHandler(Stage stage) {
        this.stage = stage;
    }

    public void addResizeListener(Region region) {
        region.setOnMouseMoved(this);
        region.setOnMousePressed(this);
        region.setOnMouseDragged(this);
        region.setOnMouseExited(e -> region.setCursor(Cursor.DEFAULT));
    }



    @Override
    public void handle(MouseEvent event) {
        if (event.getEventType() == MouseEvent.MOUSE_MOVED) {
            handleMouseMoved(event);
        } else if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            handleMousePressed(event);
        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            handleMouseDragged(event);
        }
    }

    private void handleMouseMoved(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        double width = stage.getWidth();
        double height = stage.getHeight();

        boolean top = y < border;
        boolean left = x < border;
        boolean bottom = y > height - border;
        boolean right = x > width - border;

        if (top && left) cursorEvent = Cursor.NW_RESIZE;
        else if (top && right) cursorEvent = Cursor.NE_RESIZE;
        else if (bottom && left) cursorEvent = Cursor.SW_RESIZE;
        else if (bottom && right) cursorEvent = Cursor.SE_RESIZE;
        else if (top) cursorEvent = Cursor.N_RESIZE;
        else if (left) cursorEvent = Cursor.W_RESIZE;
        else if (bottom) cursorEvent = Cursor.S_RESIZE;
        else if (right) cursorEvent = Cursor.E_RESIZE;
        else cursorEvent = Cursor.DEFAULT;

        ((Region) event.getSource()).setCursor(cursorEvent);
    }

    private void handleMousePressed(MouseEvent event) {
        if (cursorEvent != Cursor.DEFAULT) {
            startX = stage.getX();
            startY = stage.getY();
            startWidth = stage.getWidth();
            startHeight = stage.getHeight();
            startScreenX = event.getScreenX();
            startScreenY = event.getScreenY();
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (cursorEvent == Cursor.DEFAULT) {
            return;
        }

        double dx = event.getScreenX() - startScreenX;
        double dy = event.getScreenY() - startScreenY;

        if (cursorEvent == Cursor.E_RESIZE || cursorEvent == Cursor.NE_RESIZE || cursorEvent == Cursor.SE_RESIZE) {
            double newWidth = startWidth + dx;
            if (newWidth >= stage.getMinWidth()) {
                stage.setWidth(newWidth);
            }
        }
        if (cursorEvent == Cursor.W_RESIZE || cursorEvent == Cursor.NW_RESIZE || cursorEvent == Cursor.SW_RESIZE) {
            double newWidth = startWidth - dx;
            if (newWidth >= stage.getMinWidth()) {
                stage.setX(startX + dx);
                stage.setWidth(newWidth);
            }
        }
        if (cursorEvent == Cursor.S_RESIZE || cursorEvent == Cursor.SW_RESIZE || cursorEvent == Cursor.SE_RESIZE) {
            double newHeight = startHeight + dy;
            if (newHeight >= stage.getMinHeight()) {
                stage.setHeight(newHeight);
            }
        }
        if (cursorEvent == Cursor.N_RESIZE || cursorEvent == Cursor.NE_RESIZE || cursorEvent == Cursor.NW_RESIZE) {
            double newHeight = startHeight - dy;
            if (newHeight >= stage.getMinHeight()) {
                stage.setY(startY + dy);
                stage.setHeight(newHeight);
            }
        }
    }
}