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
    private List<String> tags;
    private boolean anonymous;

    public Survey() {
        this.questions = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    public Survey(String id, String title, String description, int experienceReward, int requiredLevel) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.experienceReward = experienceReward;
        this.requiredLevel = requiredLevel;
        this.questions = new ArrayList<>();
        this.tags = new ArrayList<>();
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

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public void setExperienceReward(int experienceReward) {
        this.experienceReward = experienceReward;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public void addQuestion(Question question) {
        if (questions == null) {
            questions = new ArrayList<>();
        }
        questions.add(question);
    }
} 