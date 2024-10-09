package com.tenzens.charliefx.Repository.Preferences;

import com.tenzens.charliefx.Repository.Preferences.Model.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Preferences {

    private final ArrayList<Color> colors;

    public Preferences() {
        this.colors = new ArrayList<>(List.of(
                new Color("#FFFFFF", "#C4C4C4"),
                new Color("#FFFFD4", "#F6D053"),
                new Color("#F2F9FF", "#2081DA"),
                new Color("#FFD6D4", "#B78382")
        ));
    }

    public List<Color> getColors() {
        return colors;
    }

    public Color getRandomColor() {
        Random random = new Random();
        int index = random.nextInt(colors.size());

        return colors.get(index);
    }
}
