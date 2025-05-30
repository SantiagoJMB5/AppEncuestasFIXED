package com.lehikos.appencuestas.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.RewardManager;
import com.lehikos.appencuestas.models.Survey;
import com.lehikos.appencuestas.adapters.UserSurveysAdapter;
import com.lehikos.appencuestas.models.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

public class StatisticsActivity extends BaseActivity implements UserSurveysAdapter.OnSurveyActionsListener {
    private static final String TAG = "StatisticsActivity";
    private static final int XP_PER_LEVEL = 100; // Asumiendo que esto es constante
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;
    private static final int REQUEST_LEVEL_UP_DIALOG = 103; // New request code for LevelUpActivity

    // ----- NUEVO: Constantes para SharedPreferences del estado de subida de nivel -----
    private static final String LEVEL_UP_PREFS_NAME = "LevelUpStatePrefs";
    private static final String KEY_LAST_LEVEL_UP_SHOWN = "lastLevelUpShown";
    // ----- FIN NUEVO -----

    private TextView levelText;
    private TextView experienceText;
    private LinearProgressIndicator experienceBar;
    private TextView totalSurveysText;
    private TextView usernameText;
    // RewardManager no se usará directamente para la lógica de mostrar el diálogo de subida de nivel aquí,
    // pero se mantiene si se usa para otros cálculos de recompensas.
    private RewardManager rewardManager;
    private FirebaseFirestore db;
    private String userId;
    private ImageButton profileButton;
    // private Uri profileImageUri; // Parece no usarse, se puede quitar si es así
    private boolean isActivityJustStarted = true;
    private RecyclerView userSurveysRecyclerView;
    private MaterialCardView yourSurveysHeaderCard;
    private UserSurveysAdapter userSurveysAdapter;

    // Flag to prevent showing the level up dialog again immediately after returning
    private boolean shouldPreventLevelUpDialogOnResume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView se llama en BaseActivity a través de getLayoutResourceId()
        setSelectedNavigationItem(R.id.Btn_statistics); // Asumiendo que esto es de BaseActivity

        levelText = findViewById(R.id.level_text);
        experienceText = findViewById(R.id.experience_text);
        experienceBar = findViewById(R.id.experience_bar);
        totalSurveysText = findViewById(R.id.total_surveys_text);
        usernameText = findViewById(R.id.username_text);
        profileButton = findViewById(R.id.profile_button);
        yourSurveysHeaderCard = findViewById(R.id.your_surveys_header);
        userSurveysRecyclerView = findViewById(R.id.user_surveys_recycler_view);

        profileButton.setOnClickListener(v -> {
            Log.d(TAG, "Profile button clicked, attempting to open ProfileActivity");
            try {
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting ProfileActivity", e);
                Toast.makeText(this, "Error opening profile screen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        db = FirebaseFirestore.getInstance();
        // rewardManager se inicializa después de obtener el userId
        // rewardManager = new RewardManager(getSharedPreferences("UserPrefs", MODE_PRIVATE)); // Movido

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("non_authorized_user_id", null);
            if (userId == null) {
                generateAndSaveNonAuthorizedUser(); // Esto llamará a loadUserStatistics al final
            } else {
                // Inicializa rewardManager aquí también
                rewardManager = new RewardManager(getSharedPreferences("UserPrefs", MODE_PRIVATE));
                checkNonAuthorizedUserInFirestore(userId);
            }
        } else {
            userId = currentUser.getUid();
            rewardManager = new RewardManager(getSharedPreferences("UserPrefs", MODE_PRIVATE));
            loadUserStatistics();
            loadProfilePicture();
        }
        loadUserSurveys();

        // Setup RecyclerView
        userSurveysRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Adapter will be set in loadUserSurveys

        // Toggle visibility of the user surveys RecyclerView when the header card is clicked
        yourSurveysHeaderCard.setOnClickListener(v -> {
            if (userSurveysRecyclerView.getVisibility() == View.GONE) {
                userSurveysRecyclerView.setVisibility(View.VISIBLE);
            } else {
                userSurveysRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void checkNonAuthorizedUserInFirestore(String nonAuthUserId) {
        db.collection("non_authorized_users").document(nonAuthUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        createNonAuthorizedUserInFirestore(nonAuthUserId);
                    } else {
                        loadUserStatistics();
                        loadProfilePicture();
                        loadUserSurveys();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check non-authorized user, fallback to local load", e);
                    // Fallback: si falla la comprobación, intenta cargar con lo que haya localmente
                    // o considera un estado de error/reintento.
                    // Por ahora, se asume que si falla es mejor intentar cargar estadísticas igualmente.
                    loadUserStatistics();
                    loadProfilePicture();
                    loadUserSurveys();
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Recargar estadísticas cuando la actividad se reanuda,
        // solo si userId ya está determinado y we are not preventing the dialog
        if (userId != null && !userId.isEmpty() && !shouldPreventLevelUpDialogOnResume) {
            Log.d(TAG, "Reloading user statistics in onResume");
            loadUserStatistics();
            loadProfilePicture();
            loadUserSurveys();
        } else if (shouldPreventLevelUpDialogOnResume) {
             // Reset the flag after onResume has completed its checks without showing the dialog
             shouldPreventLevelUpDialogOnResume = false;
             Log.d(TAG, "Resetting shouldPreventLevelUpDialogOnResume flag.");
        }
        // Si userId es null, onCreate se encargará de la lógica de generación/carga inicial.
        isActivityJustStarted = false; // Now onResume is called after the initial onCreate
    }

    private void generateAndSaveNonAuthorizedUser() {
        // ... (tu código existente, sin cambios aquí)
        // Asegúrate de que al final de este método, o en createNonAuthorizedUserInFirestore,
        // se inicialice rewardManager y se llame a loadUserStatistics.
        // Tu código actual ya lo hace en createNonAuthorizedUserInFirestore.
        FirebaseFirestore db = FirebaseFirestore.getInstance(); // db ya es un miembro de la clase, puedes usarlo
        db.collection("non_authorized_users").orderBy("username", Query.Direction.DESCENDING).limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    String newUsername;
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot lastUser = task.getResult().getDocuments().get(0);
                        String lastUsername = lastUser.getString("username");
                        newUsername = generateNextUsername(lastUsername != null ? lastUsername : "user1000`"); // ` es menor que 'a'
                    } else {
                        newUsername = "user1000a";
                    }
                    getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            .edit()
                            .putString("non_authorized_user_id", newUsername)
                            .apply();
                    createNonAuthorizedUserInFirestore(newUsername);
                });
    }

    private String generateNextUsername(String lastUsername) {
        // ... (tu código existente, sin cambios aquí)
        if (lastUsername == null || !lastUsername.startsWith("user") || lastUsername.length() < 6) {
            return "user1000a"; // Fallback
        }
        try {
            int number = Integer.parseInt(lastUsername.substring(4, lastUsername.length() - 1));
            char letter = lastUsername.charAt(lastUsername.length() - 1);
            if (letter == 'z') {
                number++;
                letter = 'a';
            } else {
                letter++;
            }
            return "user" + number + letter;
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            Log.e(TAG, "Error generating next username from: " + lastUsername, e);
            return "user1000a"; // Fallback
        }
    }

    private void createNonAuthorizedUserInFirestore(String username) {
        // ... (tu código existente)
        // Asegúrate de que rewardManager esté inicializado antes de loadUserStatistics
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("experience", 0);
        userData.put("level", 1);
        userData.put("totalSurveys", 0);
        db.collection("non_authorized_users").document(username)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    this.userId = username; // Establece el userId de la clase
                    rewardManager = new RewardManager(getSharedPreferences("UserPrefs", MODE_PRIVATE)); // Inicializa aquí
                    loadUserStatistics();
                    loadProfilePicture();
                    loadUserSurveys();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error creating user profile", Toast.LENGTH_SHORT).show();
                    // Considera cómo manejar este error. ¿Finalizar? ¿Mostrar UI por defecto?
                    // finish();
                });
    }

    private void loadUserStatistics() {
        Log.d(TAG, "loadUserStatistics called");
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String localUsername = prefs.getString("user_username", null);
        Log.d(TAG, "Local username from SharedPreferences: " + localUsername);
        
        if (localUsername != null && !localUsername.isEmpty()) {
            usernameText.setText(localUsername);
            Log.d(TAG, "Set username from SharedPreferences: " + localUsername);
        }

        // Determine if user is authorized or not
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String collection = (currentUser == null) ? "non_authorized_users" : "users";
        Log.d(TAG, "User type: " + (currentUser == null ? "non-authorized" : "authorized"));
        
        db.collection(collection).document(userId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    Log.d(TAG, "Firebase document exists for user: " + userId);
                    // Firebase data exists, use it as primary source
                    String username = document.getString("username");
                    Log.d(TAG, "Username from Firebase: " + username);
                    
                    // For authorized users, check if username is null or empty
                    if (currentUser != null && (username == null || username.isEmpty())) {
                        Log.d(TAG, "Authorized user has no username, showing prompt");
                        showUsernameSuggestionDialog();
                    }
                    
                    updateUI(document);
                    // Only save to preferences if user is non-authorized
                    if (currentUser == null) {
                        Log.d(TAG, "Saving non-authorized user data to preferences");
                        saveUserDataToPreferences(document);
                    }
                } else {
                    Log.e(TAG, "User document does not exist in Firestore. UserID: " + userId);
                    // If Firebase document doesn't exist, fall back to preferences
                    loadUserDataFromPreferences();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user document", e);
                // On error, fall back to preferences
                loadUserDataFromPreferences();
            });
    }

    private void saveUserDataToPreferences(DocumentSnapshot document) {
        // Este método ahora solo se usa para usuarios no autorizados
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("user_level", document.getLong("level") != null ? document.getLong("level").intValue() : 1);
        editor.putInt("user_experience", document.getLong("experience") != null ? document.getLong("experience").intValue() : 0);
        editor.putInt("user_total_surveys", document.getLong("totalSurveys") != null ? document.getLong("totalSurveys").intValue() : 0);
        editor.putString("user_username", document.getString("username"));

        editor.apply();
        Log.d(TAG, "User data saved to preferences for non-authorized user");
    }

    private void loadUserDataFromPreferences() {
        // Este método ahora solo se usa como respaldo cuando los datos de Firebase no están disponibles
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        int level = prefs.getInt("user_level", 1);
        int experience = prefs.getInt("user_experience", 0);
        int totalSurveys = prefs.getInt("user_total_surveys", 0);
        String username = prefs.getString("user_username", userId);

        usernameText.setText(username != null ? username : "Usuario");
        levelText.setText(getString(R.string.level_format, level));

        int currentLevelXP = experience % XP_PER_LEVEL;
        int nextLevelXP = XP_PER_LEVEL;
        experienceText.setText(getString(R.string.experience_format, currentLevelXP, nextLevelXP));

        int progress = (nextLevelXP > 0) ? (currentLevelXP * 100) / nextLevelXP : 0;
        experienceBar.setProgress(progress);

        totalSurveysText.setText(getString(R.string.total_surveys_format, totalSurveys));

        // Verificar subida de nivel usando datos de preferencias
        SharedPreferences levelUpPrefs = getSharedPreferences(LEVEL_UP_PREFS_NAME, MODE_PRIVATE);
        int lastLevelUpShown = levelUpPrefs.getInt(KEY_LAST_LEVEL_UP_SHOWN, 0);

        if (level > lastLevelUpShown && level > 1) {
            Log.d(TAG, "Level up detected from Preferences data. Current Level: " + level + ", Last Level Shown: " + lastLevelUpShown);
             // Now handled via startActivityForResult in updateUI
            // showLevelUpDialog(level, experience);
            // SharedPreferences.Editor editor = levelUpPrefs.edit();
            // editor.putInt(KEY_LAST_LEVEL_UP_SHOWN, level);
            // editor.apply();
            // Log.d(TAG, "Updated lastLevelUpShown to: " + level + " in SharedPreferences (from preferences load path)");
        }
    }

    private void updateUI(DocumentSnapshot document) {
        Log.d(TAG, "updateUI called with document: " + document.getId());
        // Obtener datos de Firestore
        int levelFromFirestore = document.getLong("level") != null ? document.getLong("level").intValue() : 1;
        int experienceFromFirestore = document.getLong("experience") != null ? document.getLong("experience").intValue() : 0;
        int totalSurveys = document.getLong("totalSurveys") != null ? document.getLong("totalSurveys").intValue() : 0;
        String username = document.getString("username");
        Log.d(TAG, "Username from Firestore in updateUI: " + username);

        // Actualizar UI con datos de Firestore
        if (username != null && !username.isEmpty()) {
            Log.d(TAG, "Setting username from Firestore: " + username);
            usernameText.setText(username);
            // También actualizar SharedPreferences como respaldo
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().putString("user_username", username).apply();
            Log.d(TAG, "Updated username in SharedPreferences: " + username);
        } else {
            Log.d(TAG, "No username in Firestore, checking SharedPreferences");
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String backupUsername = prefs.getString("user_username", "Usuario");
            Log.d(TAG, "Using backup username from SharedPreferences: " + backupUsername);
            usernameText.setText(backupUsername);
        }

        levelText.setText(getString(R.string.level_format, levelFromFirestore));
        // Calculate XP within the current level and total XP needed for the next level
        int currentLevelBaseXP = 0;
        if (levelFromFirestore > 1 && levelFromFirestore - 1 < User.EXPERIENCE_PER_LEVEL.length) {
            currentLevelBaseXP = User.EXPERIENCE_PER_LEVEL[levelFromFirestore - 1];
        }
        int currentLevelXP = experienceFromFirestore - currentLevelBaseXP;

        int nextLevelTotalXP = 0;
        if (levelFromFirestore < User.EXPERIENCE_PER_LEVEL.length) {
            nextLevelTotalXP = User.EXPERIENCE_PER_LEVEL[levelFromFirestore];
        } else {
             // User is at max level or beyond the defined levels
             nextLevelTotalXP = experienceFromFirestore; // Display total XP if max level
        }
        int totalXpNeededForNextLevel = nextLevelTotalXP - currentLevelBaseXP;

        experienceText.setText(getString(R.string.experience_format, currentLevelXP, totalXpNeededForNextLevel));

        int progress = 0;
        if (totalXpNeededForNextLevel > 0) {
             // Calculate progress based on XP within the current level
             progress = (currentLevelXP * 100) / totalXpNeededForNextLevel;
        }

        experienceBar.setProgress(progress);
        totalSurveysText.setText(getString(R.string.total_surveys_format, totalSurveys));

        // Obtener último nivel mostrado de Firebase primero
        int lastLevelUpShown = document.getLong("lastLevelUpShown") != null ? 
            document.getLong("lastLevelUpShown").intValue() : 0;

        // Si no está en Firebase, intentar SharedPreferences como respaldo
        if (lastLevelUpShown == 0) {
            SharedPreferences prefs = getSharedPreferences(LEVEL_UP_PREFS_NAME, MODE_PRIVATE);
            lastLevelUpShown = prefs.getInt(KEY_LAST_LEVEL_UP_SHOWN, 0);
        }

        Log.d(TAG, "Checking for level up. Level from Firestore: " + levelFromFirestore + ", LastLevelUpShown: " + lastLevelUpShown);

        // Solo mostrar subida de nivel if not already shown for this level, level > 1, and not just started
        // And also not preventing the dialog on resume
        if (levelFromFirestore > lastLevelUpShown && levelFromFirestore > 1 && !isActivityJustStarted && !shouldPreventLevelUpDialogOnResume) {
            Log.d(TAG, "Level up detected! Showing dialog. New Level: " + levelFromFirestore);
            showLevelUpDialog(levelFromFirestore, experienceFromFirestore);
            
            // Set the flag to prevent showing the dialog again immediately on resume
            shouldPreventLevelUpDialogOnResume = true;
            Log.d(TAG, "Setting shouldPreventLevelUpDialogOnResume flag.");

            // The update to Firebase/SharedPreferences will happen in onActivityResult
        } else if (shouldPreventLevelUpDialogOnResume) {
             // If the flag is set, we just log that we prevented the dialog
             Log.d(TAG, "Prevented level up dialog on resume due to flag.");
             // The flag is reset at the end of onResume
        }
    }

    // createNewUserDocument() no necesita cambios para esta lógica
    private void createNewUserDocument() {
        // ... (tu código existente, sin cambios aquí)
        // Solo asegúrate de que rewardManager esté inicializado si es necesario para RewardManager.
        String collection = FirebaseAuth.getInstance().getCurrentUser() == null ? "non_authorized_users" : "users";
        Map<String, Object> userData = new HashMap<>();
        // Asegúrate de que userId (miembro de la clase) esté establecido antes de llamar a esto
        if (this.userId == null || this.userId.isEmpty()) {
            Log.e(TAG, "Cannot create new user document, userId is missing");
            return;
        }
        userData.put("username", this.userId); // Usa el userId de la clase
        userData.put("experience", 0);
        userData.put("level", 1);
        userData.put("totalSurveys", 0);
        userData.put("lastLevelUpShown", 1); // Initialize lastLevelUpShown for new users

        db.collection(collection).document(this.userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New user document created for userId: " + this.userId);
                    usernameText.setText(this.userId); // Actualiza la UI inmediatamente
                    // No es necesario volver a llamar a loadUserStatistics() aquí si la UI se actualiza directamente,
                    // o si se asume que la próxima llamada a onResume/loadUserStatistics manejará la carga completa.
                    // Por consistencia, podrías llamarlo, pero puede ser redundante.
                    // For a new user, initialize lastLevelUpShown to 1 in SharedPreferences as well
                    SharedPreferences levelUpPrefs = getSharedPreferences(LEVEL_UP_PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = levelUpPrefs.edit();
                    editor.putInt(KEY_LAST_LEVEL_UP_SHOWN, 1); // Initialize to 1 for new users
                    editor.apply();

                    loadUserStatistics(); // Carga las estadísticas después de crear el usuario
                    loadUserSurveys();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error creating user document", e));
    }


    private void showLevelUpDialog(int newLevel, int currentExperience) {
        Log.d(TAG, "Showing LevelUpActivity for Level: " + newLevel + " with XP: " + currentExperience);
        // Guardar nombre de usuario actual antes de mostrar el diálogo de subida de nivel
        String currentUsername = usernameText.getText().toString();
        Log.d(TAG, "Current username before level up: " + currentUsername);
        
        Intent intent = new Intent(this, LevelUpActivity.class);
        intent.putExtra(LevelUpActivity.EXTRA_LEVEL, newLevel);
        intent.putExtra(LevelUpActivity.EXTRA_EXPERIENCE, currentExperience);
        // Start Activity for Result
        startActivityForResult(intent, REQUEST_LEVEL_UP_DIALOG);
        
        // After showing the level up dialog, ensure the username is preserved
        // Moved this logic to onActivityResult
        // if (currentUsername != null && !currentUsername.isEmpty()) {
        //     Log.d(TAG, "Preserving username after level up: " + currentUsername);
        //     SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        //     prefs.edit().putString("user_username", currentUsername).apply();
        //
        //     // También actualizar en Firebase si el usuario está autorizado
        //     FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        //     if (currentUser != null) {
        //         Map<String, Object> data = new HashMap<>();
        //         data.put("username", currentUsername);
        //         db.collection("users").document(currentUser.getUid())
        //             .set(data, SetOptions.merge())
        //             .addOnSuccessListener(aVoid -> Log.d(TAG, "Username preserved in Firebase after level up"))
        //             .addOnFailureListener(e -> Log.e(TAG, "Error preserving username in Firebase after level up", e));
        //     }
        // }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            if (imageBitmap != null) {
                profileButton.setImageBitmap(imageBitmap);
                saveProfilePicture(imageBitmap);
            }
        } else if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImage = data.getData();
            try {
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                if (imageBitmap != null) {
                    profileButton.setImageBitmap(imageBitmap);
                    saveProfilePicture(imageBitmap);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error getting bitmap from gallery URI", e);
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_LEVEL_UP_DIALOG && resultCode == Activity.RESULT_OK) {
             // Level up dialog was successfully shown and dismissed
             Log.d(TAG, "Received RESULT_OK from LevelUpActivity.");
             // The level shown should ideally be passed back as an extra, but we can also
             // just rely on the current level in loadUserStatistics/updateUI after resume.
             // Fetch current level again to be sure
             FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
             String collection = (currentUser == null) ? "non_authorized_users" : "users";
             String currentUserId = (currentUser != null) ? currentUser.getUid() : this.userId;

             if (currentUserId != null) {
                  db.collection(collection).document(currentUserId).get()
                      .addOnSuccessListener(document -> {
                           if (document.exists()) {
                                int currentLevel = document.getLong("level") != null ? document.getLong("level").intValue() : 1;
                                // Update lastLevelUpShown to the current level in Firebase and SharedPreferences
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("lastLevelUpShown", currentLevel);

                                db.collection(collection).document(currentUserId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Updated lastLevelUpShown in Firebase to: " + currentLevel + " from onActivityResult");
                                        // Also update SharedPreferences as backup
                                        SharedPreferences prefs = getSharedPreferences(LEVEL_UP_PREFS_NAME, MODE_PRIVATE);
                                        prefs.edit().putInt(KEY_LAST_LEVEL_UP_SHOWN, currentLevel).apply();
                                        Log.d(TAG, "Updated lastLevelUpShown in SharedPreferences to: " + currentLevel + " from onActivityResult");

                                        // Reset the flag after successful update
                                        shouldPreventLevelUpDialogOnResume = false;
                                        Log.d(TAG, "Resetting shouldPreventLevelUpDialogOnResume flag after update.");

                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating lastLevelUpShown in Firebase from onActivityResult", e);
                                        // If Firebase update fails, at least save to SharedPreferences
                                        SharedPreferences prefs = getSharedPreferences(LEVEL_UP_PREFS_NAME, MODE_PRIVATE);
                                        prefs.edit().putInt(KEY_LAST_LEVEL_UP_SHOWN, currentLevel).apply();
                                        // Reset the flag even if Firebase update failed, rely on SP backup
                                        shouldPreventLevelUpDialogOnResume = false;
                                        Log.d(TAG, "Resetting shouldPreventLevelUpDialogOnResume flag after failed Firebase update.");
                                    });
                           } else {
                                Log.e(TAG, "User document not found after LevelUpActivity dismissal.");
                                // In case document is suddenly gone, reset the flag anyway
                                shouldPreventLevelUpDialogOnResume = false;
                                Log.d(TAG, "Resetting shouldPreventLevelUpDialogOnResume flag due to missing document.");
                           }
                      })
                      .addOnFailureListener(e -> {
                           Log.e(TAG, "Error fetching user data after LevelUpActivity dismissal", e);
                           // On failure to fetch user data, reset the flag anyway
                           shouldPreventLevelUpDialogOnResume = false;
                           Log.d(TAG, "Resetting shouldPreventLevelUpDialogOnResume flag due to fetch error.");
                      });

                   // Also handle username preservation here if needed after the dialog
                   String currentUsername = usernameText.getText().toString();
        if (currentUsername != null && !currentUsername.isEmpty()) {
                        Log.d(TAG, "Preserving username after level up (onActivityResult): " + currentUsername);
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().putString("user_username", currentUsername).apply();
            
                        // Update in Firebase if the user is authorized
            if (currentUser != null) {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("username", currentUsername);
                db.collection("users").document(currentUser.getUid())
                                .set(updates, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Username preserved in Firebase after level up (onActivityResult)"))
                                .addOnFailureListener(e -> Log.e(TAG, "Error preserving username in Firebase after level up (onActivityResult)", e));
            }
        }
             } else {
                  Log.e(TAG, "User ID is null in onActivityResult, cannot update lastLevelUpShown.");
                  // If userId is null, reset the flag
                  shouldPreventLevelUpDialogOnResume = false;
                  Log.d(TAG, "Resetting shouldPreventLevelUpDialogOnResume flag due to null userId.");
             }
        } else {
            Log.d(TAG, "onActivityResult: received request code " + requestCode + " and result code " + resultCode + ", no specific action taken.");
            // If the activity was cancelled or returned a different result, we still want to allow the dialog next time
            shouldPreventLevelUpDialogOnResume = false;
             Log.d(TAG, "Resetting shouldPreventLevelUpDialogOnResume flag for non-level-up result.");
        }
    }


    // Métodos de imagen de perfil (showProfilePictureDialog, loadProfilePicture, saveProfilePicture)
    // no necesitan cambios para la lógica de subida de nivel. Se mantienen como están.
    private void showProfilePictureDialog() {
        // ... (tu código existente)
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_from_gallery)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.change_profile_picture)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        // Comprueba si hay una app para manejar la acción
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        } else {
                            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 1) {
                        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        if (pickPhoto.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
                        } else {
                            Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private void loadProfilePicture() {
        // ... (tu código existente)
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (this.userId != null) { // Usa el userId de la clase para usuarios no autorizados
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                // Clave de SharedPreferences específica para la imagen del usuario no autorizado
                String base64 = prefs.getString("profile_picture_base64_" + this.userId, null);
                if (base64 != null) {
                    byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    profileButton.setImageBitmap(bitmap);
                } else {
                    profileButton.setImageResource(R.drawable.ic_profile); // Imagen por defecto
                }
            } else {
                profileButton.setImageResource(R.drawable.ic_profile);
            }
        } else {
            String firestoreUserId = currentUser.getUid();
            FirebaseFirestore.getInstance().collection("users").document(firestoreUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String base64 = documentSnapshot.getString("profile_picture_base64");
                        if (base64 != null) {
                            byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            profileButton.setImageBitmap(bitmap);
                        } else {
                            profileButton.setImageResource(R.drawable.ic_profile);
                        }
                    })
                    .addOnFailureListener(e -> profileButton.setImageResource(R.drawable.ic_profile));
        }
    }

    private void saveProfilePicture(Bitmap bitmap) {
        // ... (tu código existente)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            if (this.userId != null) { // Usa el userId de la clase para usuarios no autorizados
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                // Clave de SharedPreferences específica para la imagen del usuario no autorizado
                prefs.edit().putString("profile_picture_base64_" + this.userId, base64).apply();
                Log.d(TAG, "Profile picture saved to SharedPreferences for non-auth user: " + this.userId);
            } else {
                Log.e(TAG, "Cannot save profile picture for non-auth user, userId is null");
            }
        } else {
            String firestoreUserId = currentUser.getUid();
            FirebaseFirestore.getInstance().collection("users").document(firestoreUserId)
                    .update("profile_picture_base64", base64)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Profile picture updated in Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating profile picture in Firestore", e));
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_statistics;
    }

    private void showUsernameSuggestionDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_set_username, null);
        EditText editText = dialogView.findViewById(R.id.editTextUsername);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.confirm_username_title)
            .setView(dialogView)
            .setPositiveButton(R.string.yes, (d, which) -> {
                String enteredUsername = editText.getText().toString().trim();
                if (!enteredUsername.isEmpty()) {
                    confirmUsernameDialog(enteredUsername);
                }
            })
            .setNegativeButton(R.string.no, (d, which) -> d.dismiss())
            .setNeutralButton(R.string.skip, (d, which) -> d.dismiss())
            .show();
    }

    private void confirmUsernameDialog(String username) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm_username_title)
            .setMessage(getString(R.string.confirm_username_message, username))
            .setPositiveButton(R.string.yes, (d, which) -> saveUsername(username))
            .setNegativeButton(R.string.no, null)
            .show();
    }

    private void saveUsername(String username) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId;
        String collection;

        if (currentUser != null) {
            userId = currentUser.getUid();
            collection = "users";
        } else {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("non_authorized_user_id", null);
            collection = "non_authorized_users";
        }

        if (userId == null) {
            Log.e(TAG, "No user ID found, cannot save username");
            return;
        }

        // Guardar en Firebase primero
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        
        FirebaseFirestore.getInstance().collection(collection).document(userId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                // Después de guardar exitosamente en Firebase, actualizar SharedPreferences como respaldo
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                prefs.edit().putString("user_username", username).apply();
                Log.d(TAG, "Username saved successfully to Firebase and SharedPreferences");
                // Actualizar la UI con el nuevo nombre de usuario
                usernameText.setText(username);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving username to Firebase", e);
                // Si falla el guardado en Firebase, al menos guardar en SharedPreferences
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                prefs.edit().putString("user_username", username).apply();
                // Actualizar la UI con el nuevo nombre de usuario incluso si falla el guardado en Firebase
                usernameText.setText(username);
            });
    }

    private void loadUserSurveys() {
        Log.d(TAG, "loadUserSurveys: Attempting to load user surveys");
        if (userId == null) {
            Log.w(TAG, "loadUserSurveys: User ID is null, cannot load surveys.");
            // Clear the adapter or show empty state if userId becomes null dynamically
            if (userSurveysAdapter != null) {
                 userSurveysAdapter.updateSurveys(new ArrayList<>());
            }
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String collectionPath = (currentUser == null) ? "non_authorized_users" : "users";

        db.collection(collectionPath).document(userId).collection("user_surveys")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "loadUserSurveys: Successfully fetched user surveys.");

                    List<Survey> userSurveys = new ArrayList<>();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "loadUserSurveys: Found " + queryDocumentSnapshots.size() + " surveys.");
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                // Deserialize the full Survey object
                                Log.d(TAG, "loadUserSurveys: Attempting to deserialize document: " + document.getId());
                                Survey survey = document.toObject(Survey.class);
                                if (survey != null) {
                                    // Ensure the survey object has its ID set from the document ID
                                    survey.setId(document.getId());
                                    userSurveys.add(survey);
                                    Log.d(TAG, "loadUserSurveys: Deserialized and added survey: " + survey.getTitle());
                                } else {
                                    Log.e(TAG, "loadUserSurveys: Deserialized survey object is null for document: " + document.getId());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "loadUserSurveys: Error deserializing survey document: " + document.getId(), e);
                            }
                        }
                    }

                    if (userSurveys.isEmpty()) {
                        Log.d(TAG, "loadUserSurveys: No surveys found for user.");
                        // Handle no surveys case - maybe show a message in the RecyclerView or elsewhere
                        // For now, the adapter will show an empty list.
                        // You might want a dedicated TextView for this outside the RecyclerView.
                        return;
                    }

                    // Initialize adapter if null, otherwise update
                    if (userSurveysAdapter == null) {
                        userSurveysAdapter = new UserSurveysAdapter(userSurveys, this);
                        userSurveysRecyclerView.setAdapter(userSurveysAdapter);
                    } else {
                        userSurveysAdapter.updateSurveys(userSurveys);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadUserSurveys: Error fetching user surveys", e);
                    // Handle error loading surveys
                    Toast.makeText(this, "Error al cargar tus encuestas.", Toast.LENGTH_SHORT).show();
                    if (userSurveysAdapter != null) {
                         userSurveysAdapter.updateSurveys(new ArrayList<>()); // Clear list on error
                    }
                });
    }

    // Implementing the UserSurveysAdapter.OnSurveyActionsListener interface

    @Override
    public void onSurveyEditClick(Survey survey) {
        Log.d(TAG, "onSurveyEditClick called for survey: " + survey.getTitle() + " (ID: " + survey.getId() + ")");
        // Launch SurveyCreationActivity to edit the survey
        Intent intent = new Intent(this, SurveyCreationActivity.class);
        // Pass the survey ID to the creation activity so it can load the existing data
        intent.putExtra("EDIT_SURVEY_ID", survey.getId());
        startActivity(intent);
    }

    @Override
    public void onSurveyAnswerClick(Survey survey) {
        Log.d(TAG, "onSurveyAnswerClick called for survey: " + survey.getTitle() + " (ID: " + survey.getId() + ")");
        // Launch SurveyActivity to answer the survey
        Intent intent = new Intent(this, SurveyActivity.class);
        intent.putExtra(SurveyActivity.EXTRA_SURVEY_ID, survey.getId());
        intent.putExtra(SurveyActivity.EXTRA_SURVEY_TITLE, survey.getTitle());
        intent.putExtra(SurveyActivity.EXTRA_SURVEY_DESCRIPTION, survey.getDescription());
        intent.putExtra(SurveyActivity.EXTRA_SURVEY_EXPERIENCE, survey.getExperienceReward());
        intent.putExtra(SurveyActivity.EXTRA_SURVEY_REQUIRED_LEVEL, survey.getRequiredLevel());
        // Need to pass the questions as well - Survey object is Serializable
        intent.putExtra(SurveyActivity.EXTRA_SURVEY_QUESTIONS, (ArrayList<com.lehikos.appencuestas.models.Question>) survey.getQuestions());
        startActivity(intent);
    }

    @Override
    public void onSurveyViewStatsClick(Survey survey) {
        Log.d(TAG, "onSurveyViewStatsClick called for survey: " + survey.getTitle() + " (ID: " + survey.getId() + ")");
        // TODO: Implement logic to view survey statistics
        Toast.makeText(this, "View Stats clicked for: " + survey.getTitle(), Toast.LENGTH_SHORT).show();
    }

    // Optional: Implement if the whole item view is clickable
    // @Override
    // public void onSurveyClick(Survey survey) {
    //     Log.d(TAG, "onSurveyClick called for survey: " + survey.getTitle() + " (ID: " + survey.getId() + ")");
    //     // Default action for clicking the item if not handled by buttons
    //     // Maybe view stats or open a detail screen
    // }
}