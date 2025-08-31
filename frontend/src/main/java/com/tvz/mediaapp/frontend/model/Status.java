package com.tvz.mediaapp.frontend.model;

public enum Status {
    IN_PROGRESS("In Progress", "status-in-progress"),
    DONE("Done", "status-done"),
    CANCELED("Canceled", "status-canceled");

    private final String displayName;
    private final String styleClass;

    Status(String displayName, String styleClass) {
        this.displayName = displayName;
        this.styleClass = styleClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStyleClass() {
        return styleClass;
    }
}
