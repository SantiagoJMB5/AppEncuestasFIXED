package com.lehikos.appencuestas.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Survey implements Serializable {
    private String id;
    private String title;
    private String description;
    private List<Question> questions;
    private int experienceReward;
    private int requiredLevel;

    public Survey(String id, String title, String description, int experienceReward, int requiredLevel) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.experienceReward = experienceReward;
        this.requiredLevel = requiredLevel;
        this.questions = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public int getExperienceReward() {
        return experienceReward;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }
} 