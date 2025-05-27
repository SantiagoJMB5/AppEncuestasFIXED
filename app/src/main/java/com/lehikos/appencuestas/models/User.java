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

    // Experience required for each level (can be adjusted)
    private static final int[] EXPERIENCE_PER_LEVEL = {
        0,      // Level 1
        100,    // Level 2
        250,    // Level 3
        500,    // Level 4
        1000,   // Level 5
        2000,   // Level 6
        4000,   // Level 7
        8000,   // Level 8
        16000,  // Level 9
        32000   // Level 10
    };

    // Empty constructor for Firestore
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