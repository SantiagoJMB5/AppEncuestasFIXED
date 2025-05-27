
package com.lehikos.appencuestas.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lehikos.appencuestas.models.Answer;
import com.lehikos.appencuestas.models.SurveyCompletion;

import java.util.List;


public class FirestoreService {

    private static final String TAG = "FirestoreService";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public FirestoreService() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    // ----- MODIFICADO: Actualizada la firma del método y su lógica interna -----
    public void saveSurveyCompletion(String surveyId,
                                     @NonNull List<Answer> answers, // PARÁMETRO AÑADIDO
                                     OnSuccessListener<DocumentReference> successListener,
                                     OnFailureListener failureListener) {

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userIdToSave;

        if (currentUser != null) {
            userIdToSave = currentUser.getUid();
        } else {
            Log.e(TAG, "User not logged in (Firebase Auth). Cannot save survey completion without a user ID strategy here.");
            if (failureListener != null) {
                failureListener.onFailure(new Exception("User not logged in (Firebase Auth) and no alternative ID strategy in FirestoreService."));
            }
            return;
        }

        // Crea el objeto SurveyCompletion CON las respuestas
        SurveyCompletion completion = new SurveyCompletion(userIdToSave, surveyId, answers);
        // La anotación @ServerTimestamp en el modelo SurveyCompletion (si la tienes) se encargará de 'completedAt'

        db.collection("surveyCompletions") // Nombre de tu colección en Firestore
                .add(completion) // .add() crea un documento con un ID autogenerado
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Survey completion with answers recorded. Firestore ID: " + documentReference.getId());
                    if (successListener != null) {
                        successListener.onSuccess(documentReference);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error recording survey completion with answers to Firestore", e);
                    if (failureListener != null) {
                        failureListener.onFailure(e);
                    }
                });
    }
}