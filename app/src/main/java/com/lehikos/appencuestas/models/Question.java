package com.lehikos.appencuestas.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Question implements Serializable {
    private String id;
    private String text;
    private QuestionType type;
    private List<String> options;
    private boolean mandatory;
    private int maxRating;

    public enum QuestionType {
        RATING,
        MULTIPLE_CHOICE,
        TEXT
    }

    public Question() {
        // Required empty constructor for Firestore
        this.mandatory = true; // Default to mandatory
        this.maxRating = 5; // Valor por defecto para calificación (ej. 5 estrellas)
    }

    public Question(String id, String text, QuestionType type) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.options = new ArrayList<>();
        this.mandatory = true; // Default to mandatory
        this.maxRating = 5; // Valor por defecto para calificación
    }

    public Question(String id, String text, QuestionType type, List<String> options) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.options = options != null ? options : new ArrayList<>();
        this.mandatory = true; // Default to mandatory
        this.maxRating = 5; // Valor por defecto para calificación
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public void addOption(String option) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        this.options.add(option);
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public int getMaxRating() {
        return maxRating;
    }

    public void setMaxRating(int maxRating) {
        this.maxRating = maxRating;
    }
}