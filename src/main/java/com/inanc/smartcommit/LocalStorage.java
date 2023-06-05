package com.inanc.smartcommit;

import java.util.prefs.Preferences;

public class LocalStorage {

    private Preferences prefs = Preferences.userNodeForPackage(LocalStorage.class);

    // Save a value
    public void saveValue(String key, String value) {
        prefs.put(key, value);
    }

    public void removeValue(String key){
        prefs.remove(key);
    }

    // Load a value
    public String loadValue(String key) {
        return prefs.get(key, null);  // null is the default value if key is not found
    }

    public void saveTerms(String key, Boolean value) {
        prefs.putBoolean(key, value);
    }

    public Boolean loadValueAcceptTerms(String acceptTerms) {
      return   prefs.getBoolean(acceptTerms,false);
    }
}
