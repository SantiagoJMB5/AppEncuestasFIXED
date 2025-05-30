package com.lehikos.appencuestas.models;

import com.google.firebase.firestore.Exclude;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String id;
    private String username;
    private int experience;
    private int level;
    private int totalSurveys;

    // Experiencia requerida para cada nivel (puede ser ajustada)
    public static final int[] EXPERIENCE_PER_LEVEL = {
        0,      // Nivel 1
        100,    // Nivel 2
        250,    // Nivel 3
        500,    // Nivel 4
        1000,   // Nivel 5
        2000,   // Nivel 6
        4000,   // Nivel 7
        8000,   // Nivel 8
        16000,  // Nivel 9
        32000   // Nivel 10
    };

    // Constructor vacío para Firestore
    public User() {}

    public User(String id, String username, int experience, int level, int totalSurveys) {
        this.id = id;
        this.username = username;
        this.experience = experience;
        this.level = level;
        this.totalSurveys = totalSurveys;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
        updateLevel();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getTotalSurveys() {
        return totalSurveys;
    }

    public void setTotalSurveys(int totalSurveys) {
        this.totalSurveys = totalSurveys;
    }

    public void addExperience(int amount) {
        experience += amount;
        updateLevel();
    }

    public void incrementSurveys() {
        totalSurveys++;
    }

    private void updateLevel() {
        for (int i = EXPERIENCE_PER_LEVEL.length - 1; i >= 0; i--) {
            if (experience >= EXPERIENCE_PER_LEVEL[i]) {
                level = i + 1;
                break;
            }
        }
    }

    public int getExperienceToNextLevel() {
        if (level >= EXPERIENCE_PER_LEVEL.length) {
            return 0;
        }
        return EXPERIENCE_PER_LEVEL[level] - experience;
    }

    public int getExperienceForCurrentLevel() {
        return EXPERIENCE_PER_LEVEL[level - 1];
    }

    public int getExperienceForNextLevel() {
        if (level >= EXPERIENCE_PER_LEVEL.length) {
            return EXPERIENCE_PER_LEVEL[EXPERIENCE_PER_LEVEL.length - 1];
        }
        return EXPERIENCE_PER_LEVEL[level];
    }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("username", username);
        map.put("experience", experience);
        map.put("level", level);
        map.put("totalSurveys", totalSurveys);
        return map;
    }
} 