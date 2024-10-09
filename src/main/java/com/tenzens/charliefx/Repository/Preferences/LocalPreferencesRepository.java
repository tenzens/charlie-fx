package com.tenzens.charliefx.Repository.Preferences;

public class LocalPreferencesRepository {
    private Preferences preferences;

    public Preferences loadPreferences() {
        if (preferences == null) {
            preferences = new Preferences();
        }

        return preferences;
    }
}
