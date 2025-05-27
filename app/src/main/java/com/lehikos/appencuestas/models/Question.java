package com.lehikos.appencuestas.models;

import java.io.Serializable;
import java.util.List;

public class Question implements Serializable {
    private String id;
    private String text;
    private QuestionType type;
    private List<String> options;
    private String answer;

    public enum QuestionType {
        TEXT,
        MULTIPLE_CHOICE,
        RATING
    }

    public Question() {
    }

    public Question(String id, String text, QuestionType type) {
        this.id = id;
        this.text = text;
        this.type = type;
    }

    public Question(String id, String text, QuestionType type, List<String> options) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.options = options;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getType() {
        if (this.type != null) {
            return this.type.name();
        }
        return null;
    }

    // Setter for type (useful for constructing the object)
    public void setType(QuestionType type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public QuestionType getQuestionTypeEnum() {
        return type;
    }
}