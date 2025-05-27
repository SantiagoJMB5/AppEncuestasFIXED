package com.lehikos.appencuestas.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.List;

public class SurveyCompletion {
    private String userId;
    private String surveyId;
    private Timestamp completedAt;
    private List<Answer> answers; // NUEVO: Campo para almacenar la lista de respuestas

    public SurveyCompletion() {
    }

    public SurveyCompletion(String userId, String surveyId, List<Answer> answers) {
        this.userId = userId;
        this.surveyId = surveyId;
        this.answers = answers; // Asigna las respuestas
        // 'completedAt' será establecido por el servidor de Firestore si se usa @ServerTimestamp
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(String surveyId) {
        this.surveyId = surveyId;
    }

    @ServerTimestamp
    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    // ----- NUEVOS GETTERS Y SETTERS para las respuestas -----
    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }
    // ----- FIN DE NUEVOS GETTERS Y SETTERS -----
}