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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.RewardManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StatisticsActivity extends BaseActivity {
    private static final String TAG = "StatisticsActivity";
    private static final int XP_PER_LEVEL = 100; // Asumiendo que esto es constante
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;

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
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check non-authorized user, fallback to local load", e);
                    // Fallback: si falla la comprobación, intenta cargar con lo que haya localmente
                    // o considera un estado de error/reintento.
                    // Por ahora, se asume que si falla es mejor intentar cargar estadísticas igualmente.
                    loadUserStatistics();
                    loadProfilePicture();
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Recargar estadísticas cuando la actividad se reanuda,
        // solo si userId ya está determinado.
        if (userId != null && !userId.isEmpty()) {
            Log.d(TAG, "Reloading user statistics in onResume");
            loadUserStatistics();
            loadProfilePicture();
        }
        // Si userId es null, onCreate se encargará de la lógica de generación/carga inicial.
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
        // This method is now only used for non-authorized users
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
        // This method is now only used as a fallback when Firebase data is not available
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

        // Check for level up using preferences data
        SharedPreferences levelUpPrefs = getSharedPreferences(LEVEL_UP_PREFS_NAME, MODE_PRIVATE);
        int lastLevelUpShown = levelUpPrefs.getInt(KEY_LAST_LEVEL_UP_SHOWN, 0);

        if (level > lastLevelUpShown && level > 1) {
            Log.d(TAG, "Level up detected from Preferences data. Current Level: " + level + ", Last Level Shown: " + lastLevelUpShown);
            showLevelUpDialog(level, experience);
            SharedPreferences.Editor editor = levelUpPrefs.edit();
            editor.putInt(KEY_LAST_LEVEL_UP_SHOWN, level);
            editor.apply();
            Log.d(TAG, "Updated lastLevelUpShown to: " + level + " in SharedPreferences (from preferences load path)");
        }
    }

    private void updateUI(DocumentSnapshot document) {
        Log.d(TAG, "updateUI called with document: " + document.getId());
        // Get data from Firestore
        int levelFromFirestore = document.getLong("level") != null ? document.getLong("level").intValue() : 1;
        int experienceFromFirestore = document.getLong("experience") != null ? document.getLong("experience").intValue() : 0;
        int totalSurveys = document.getLong("totalSurveys") != null ? document.getLong("totalSurveys").intValue() : 0;
        String username = document.getString("username");
        Log.d(TAG, "Username from Firestore in updateUI: " + username);

        // Update UI with Firestore data
        if (username != null && !username.isEmpty()) {
            Log.d(TAG, "Setting username from Firestore: " + username);
            usernameText.setText(username);
            // Also update SharedPreferences as backup
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
        int currentLevelXP = experienceFromFirestore % XP_PER_LEVEL;
        int nextLevelXP = XP_PER_LEVEL;
        experienceText.setText(getString(R.string.experience_format, currentLevelXP, nextLevelXP));
        int progress = (nextLevelXP > 0) ? (currentLevelXP * 100) / nextLevelXP : 0;
        experienceBar.setProgress(progress);
        totalSurveysText.setText(getString(R.string.total_surveys_format, totalSurveys));

        // Check for level up using Firestore data
        SharedPreferences prefs = getSharedPreferences(LEVEL_UP_PREFS_NAME, MODE_PRIVATE);
        int lastLevelUpShown = prefs.getInt(KEY_LAST_LEVEL_UP_SHOWN, 0);

        Log.d(TAG, "Checking for level up. Level from Firestore: " + levelFromFirestore + ", LastLevelUpShown: " + lastLevelUpShown);

        if (levelFromFirestore > lastLevelUpShown && levelFromFirestore > 1) {
            Log.d(TAG, "Level up detected! Showing dialog. New Level: " + levelFromFirestore);
            showLevelUpDialog(levelFromFirestore, experienceFromFirestore);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_LAST_LEVEL_UP_SHOWN, levelFromFirestore);
            editor.apply();
            Log.d(TAG, "Updated lastLevelUpShown to: " + levelFromFirestore + " in SharedPreferences");
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

        db.collection(collection).document(this.userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New user document created for userId: " + this.userId);
                    usernameText.setText(this.userId); // Actualiza la UI inmediatamente
                    // No es necesario volver a llamar a loadUserStatistics() aquí si la UI se actualiza directamente,
                    // o si se asume que la próxima llamada a onResume/loadUserStatistics manejará la carga completa.
                    // Por consistencia, podrías llamarlo, pero puede ser redundante.
                    // Para la subida de nivel, 'lastLevelUpShown' se inicializaría a 0, y el nivel 1 sería > 0,
                    // así que si se muestra un LevelUp para el nivel 1 es una decisión de diseño.
                    // Normalmente, el nivel 1 no se "sube", se empieza en él.
                    // Podrías inicializar lastLevelUpShown a 1 para nuevos usuarios si no quieres pop-up para el nivel 1.
                    SharedPreferences levelUpPrefs = getSharedPreferences(LEVEL_UP_PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = levelUpPrefs.edit();
                    // Para un nuevo usuario, podrías establecer lastLevelUpShown al nivel inicial (1)
                    // para evitar que se muestre un "Level Up" por alcanzar el nivel 1.
                    editor.putInt(KEY_LAST_LEVEL_UP_SHOWN, 1); // Asumiendo que el nivel inicial es 1
                    editor.apply();

                    loadUserStatistics(); // Carga las estadísticas después de crear el usuario
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error creating user document", e));
    }


    private void showLevelUpDialog(int newLevel, int currentExperience) {
        Log.d(TAG, "Showing LevelUpActivity for Level: " + newLevel + " with XP: " + currentExperience);
        // Save current username before showing level up dialog
        String currentUsername = usernameText.getText().toString();
        Log.d(TAG, "Current username before level up: " + currentUsername);
        
        Intent intent = new Intent(this, LevelUpActivity.class);
        intent.putExtra(LevelUpActivity.EXTRA_LEVEL, newLevel);
        intent.putExtra(LevelUpActivity.EXTRA_EXPERIENCE, currentExperience);
        startActivity(intent);
        
        // After level up dialog is shown, ensure username is preserved
        if (currentUsername != null && !currentUsername.isEmpty()) {
            Log.d(TAG, "Preserving username after level up: " + currentUsername);
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().putString("user_username", currentUsername).apply();
            
            // Also update in Firebase if user is authorized
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("username", currentUsername);
                db.collection("users").document(currentUser.getUid())
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Username preserved in Firebase after level up"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error preserving username in Firebase after level up", e));
            }
        }
    }

    // Métodos de imagen de perfil (showProfilePictureDialog, loadProfilePicture, saveProfilePicture, onActivityResult)
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Bitmap imageBitmap = null;
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null && data.getExtras() != null) {
                imageBitmap = (Bitmap) data.getExtras().get("data");
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                Uri selectedImage = data.getData();
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                } catch (IOException e) {
                    Log.e(TAG, "Error getting bitmap from gallery URI", e);
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                }
            }

            if (imageBitmap != null) {
                profileButton.setImageBitmap(imageBitmap);
                saveProfilePicture(imageBitmap);
            }
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

        // Save to Firebase first
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        
        FirebaseFirestore.getInstance().collection(collection).document(userId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                // After successful Firebase save, update SharedPreferences as backup
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                prefs.edit().putString("user_username", username).apply();
                Log.d(TAG, "Username saved successfully to Firebase and SharedPreferences");
                // Update the UI with the new username
                usernameText.setText(username);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving username to Firebase", e);
                // If Firebase save fails, at least save to SharedPreferences
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                prefs.edit().putString("user_username", username).apply();
                // Update the UI with the new username even if Firebase save failed
                usernameText.setText(username);
            });
    }
}