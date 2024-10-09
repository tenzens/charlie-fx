package com.tenzens.charliefx.Repository.Preferences.Model;

public class Color {
    private final String color;
    private final String accentColor;

    public Color(String color, String accentColor) {
        this.color = color;
        this.accentColor = accentColor;
    }

    public String getColor() {
        return color;
    }

    public String getAccentColor() {
        return accentColor;
    }
}
