package com.lehikos.appencuestas.models;

import java.io.Serializable;

public class Answer implements Serializable {

    private String questionId;
    private String questionText;
    private Object answerValue;

    public Answer() {
    }

    public Answer(String questionId, String questionText, Object answerValue) {
        this.questionId = questionId;
        this.questionText = questionText; // Almacena el texto de la pregunta para contexto histórico
        this.answerValue = answerValue;
    }

    // --- Getters ---
    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public Object getAnswerValue() {
        return answerValue;
    }

    public void setAnswerValue(Object answerValue) {
        this.answerValue = answerValue;
    }

    // Opcional: método toString() para depuración
    @Override
    public String toString() {
        return "Answer{" +
                "questionId='" + questionId + '\'' +
                ", questionText='" + questionText + '\'' +
                ", answerValue=" + (answerValue != null ? answerValue.toString() : "null") +
                '}';
    }
}