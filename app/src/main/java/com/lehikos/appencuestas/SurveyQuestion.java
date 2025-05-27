package com.lehikos.appencuestas;

import java.io.Serializable;
import java.util.List;

public class SurveyQuestion implements Serializable {
    private String question;
    private List<String> options;

    public SurveyQuestion(String question, List<String> options) {
        this.question = question;
        this.options = options;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getOptions() {
        return options;
    }
} 