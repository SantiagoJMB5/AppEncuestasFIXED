package com.lehikos.appencuestas.firebase;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirebaseInitializer {
    private static final String TAG = "FirebaseInitializer";
    private static FirebaseInitializer instance;
    private final FirebaseFirestore db;

    private FirebaseInitializer() {
        db = FirebaseFirestore.getInstance();
    }

    public static FirebaseInitializer getInstance() {
        if (instance == null) {
            instance = new FirebaseInitializer();
        }
        return instance;
    }

    public void initializeCollections() {
        Log.d(TAG, "Initializing Firestore collections");
        
        // Verificar y crear colección de usuarios
        initializeCollection("users");
        
        // Verificar y crear colección de usuarios no autorizados
        initializeCollection("non_authorized_users");
        
        // Verificar y crear colección de encuestas
        initializeCollection("surveys");
    }

    private void initializeCollection(String collectionName) {
        db.collection(collectionName)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, collectionName + " collection exists");
            })
            .addOnFailureListener(e -> {
                Log.d(TAG, "Creating " + collectionName + " collection");
                // Crear un documento vacío para inicializar la colección
                Map<String, Object> emptyDoc = new HashMap<>();
                db.collection(collectionName)
                    .document("_init")
                    .set(emptyDoc)
                    .addOnSuccessListener(aVoid -> 
                        Log.d(TAG, collectionName + " collection created successfully"))
                    .addOnFailureListener(error -> 
                        Log.e(TAG, "Error creating " + collectionName + " collection", error));
            });
    }
} 