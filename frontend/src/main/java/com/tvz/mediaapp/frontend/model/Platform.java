package com.tvz.mediaapp.frontend.model;


public enum Platform {
    FACEBOOK("facebook.svg"),
    TIKTOK("tiktok.svg"),
    INSTAGRAM("instagram.svg"),
    YOUTUBE("youtube.svg"),
    X("x.svg");

    private final String iconName;

    Platform(String iconName) {
        this.iconName = iconName;
    }

    public String getIconName() {
        return iconName;
    }

    @Override
    public String toString() {
        return this.name().charAt(0) + this.name().substring(1).toLowerCase();
    }
}
