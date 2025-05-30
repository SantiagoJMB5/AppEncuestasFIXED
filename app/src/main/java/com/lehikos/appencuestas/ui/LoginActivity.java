package com.lehikos.appencuestas.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lehikos.appencuestas.R;
import android.app.AlertDialog;
import android.content.SharedPreferences;

import com.lehikos.appencuestas.models.User;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    private EditText editTextEmailLogin, editTextPasswordLogin;
    private Button buttonLogin;
    private TextView textViewGoToRegister, textViewForgotPassword;
    private ProgressBar progressBar;
    private SignInButton buttonGoogleSignIn;
    private Button buttonContinueWithoutLogin;
    private Button buttonLightMode, buttonDarkMode;
    private static final String APP_PREFS = "AppPrefs";
    private static final String THEME_KEY = "theme";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configurar inicio de sesión con Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        try {
            String clientId = getString(R.string.default_web_client_id);
            if (clientId != null && !clientId.isEmpty()) {
                gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(clientId)
                        .requestEmail()
                        .build();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error al obtener el ID del cliente, continuando sin inicio de sesión con Google", e);
        }
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Inicializar vistas
        editTextEmailLogin = findViewById(R.id.editTextEmailLogin);
        editTextPasswordLogin = findViewById(R.id.editTextPasswordLogin);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewGoToRegister = findViewById(R.id.textViewGoToRegister);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        progressBar = findViewById(R.id.progressBarLogin);
        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn);
        buttonContinueWithoutLogin = findViewById(R.id.buttonContinueWithoutLogin);
        buttonLightMode = findViewById(R.id.buttonLightMode);
        buttonDarkMode = findViewById(R.id.buttonDarkMode);

        // Lógica de alternancia de tema
        String currentTheme = getSharedPreferences(APP_PREFS, MODE_PRIVATE).getString(THEME_KEY, "light");
        updateThemeToggleSelection(currentTheme);

        buttonLightMode.setOnClickListener(v -> {
            setThemePreference("light");
            updateThemeToggleSelection("light");
            restartActivity();
        });
        buttonDarkMode.setOnClickListener(v -> {
            setThemePreference("dark");
            updateThemeToggleSelection("dark");
            restartActivity();
        });

        // Set click listeners
        buttonLogin.setOnClickListener(v -> loginUser());
        textViewGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
        textViewForgotPassword.setOnClickListener(v -> showResetPasswordDialog());
        buttonGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        buttonContinueWithoutLogin.setOnClickListener(v -> continueWithoutLogin());
    }

    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.reset_password);

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton(R.string.reset_password, (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!TextUtils.isEmpty(email)) {
                resetPassword(email);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void resetPassword(String email) {
        progressBar.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, R.string.reset_email_sent, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.error_reset_password, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(LoginActivity.this, R.string.google_sign_in_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressBar.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String email = user.getEmail();
                            
                            // Verificar si existe el documento del usuario
                            db.collection("users").document(user.getUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (!documentSnapshot.exists()) {
                                        // Crear nuevo documento de usuario si no existe
                                        Map<String, Object> userMap = new HashMap<>();
                                        userMap.put("id", user.getUid());
                                        userMap.put("email", email);
                                        userMap.put("username", null); // Establecer nombre de usuario como nulo inicialmente
                                        userMap.put("experience", 0);
                                        userMap.put("level", 1);
                                        userMap.put("totalSurveys", 0);
                                        
                                        db.collection("users").document(user.getUid())
                                            .set(userMap)
                                            .addOnSuccessListener(aVoid -> {
                                                setHasSeenLoginScreen();
                                                navigateToHome();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error al crear documento de usuario", e);
                                                Toast.makeText(LoginActivity.this, 
                                                    "Error al crear perfil de usuario", Toast.LENGTH_SHORT).show();
                                            });
                                    } else {
                                        // El documento de usuario existe, proceder a inicio
                                        setHasSeenLoginScreen();
                                        navigateToHome();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error al verificar documento de usuario", e);
                                    Toast.makeText(LoginActivity.this, 
                                        "Error al verificar perfil de usuario", Toast.LENGTH_SHORT).show();
                                });
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.google_sign_in_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void continueWithoutLogin() {
        // Generar un ID único para el usuario no autorizado
        String nonAuthUserId = "user_" + System.currentTimeMillis();
        
        // Guardar el ID en SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().putString("non_authorized_user_id", nonAuthUserId).apply();

        // Crear un documento en Firestore para el usuario no autorizado
        db.collection("non_authorized_users").document(nonAuthUserId)
                .set(new User(nonAuthUserId, "non_authorized_user", 0, 1, 0))
                .addOnSuccessListener(aVoid -> {
                    setHasSeenLoginScreen();
                    navigateToHome();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al crear usuario no autorizado", e);
                    Toast.makeText(LoginActivity.this, "Error al crear usuario temporal", Toast.LENGTH_SHORT).show();
                });
    }

    private void loginUser() {
        String email = editTextEmailLogin.getText().toString().trim();
        String password = editTextPasswordLogin.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmailLogin.setError(getString(R.string.error_email_required));
            editTextEmailLogin.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmailLogin.setError(getString(R.string.error_email_invalid));
            editTextEmailLogin.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPasswordLogin.setError(getString(R.string.error_password_required));
            editTextPasswordLogin.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Verificar si existe el documento del usuario
                            db.collection("users").document(user.getUid())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (!documentSnapshot.exists()) {
                                        // Crear nuevo documento de usuario si no existe
                                        Map<String, Object> userMap = new HashMap<>();
                                        userMap.put("id", user.getUid());
                                        userMap.put("email", email);
                                        userMap.put("username", null); // Establecer nombre de usuario como nulo inicialmente
                                        userMap.put("experience", 0);
                                        userMap.put("level", 1);
                                        userMap.put("totalSurveys", 0);
                                        
                                        db.collection("users").document(user.getUid())
                                            .set(userMap)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(LoginActivity.this, 
                                                    "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show();
                                                setHasSeenLoginScreen();
                                                navigateToHome();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error al crear documento de usuario", e);
                                                Toast.makeText(LoginActivity.this, 
                                                    "Error al crear perfil de usuario", Toast.LENGTH_SHORT).show();
                                            });
                                    } else {
                                        // El documento de usuario existe, proceder a inicio
                                        Toast.makeText(LoginActivity.this, 
                                            "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show();
                                        setHasSeenLoginScreen();
                                        navigateToHome();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error al verificar documento de usuario", e);
                                    Toast.makeText(LoginActivity.this, 
                                        "Error al verificar perfil de usuario", Toast.LENGTH_SHORT).show();
                                });
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, 
                            getString(R.string.authentication_failed, task.getException().getMessage()),
                            Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finishAffinity();
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Verificar si el usuario ya ha visto la pantalla de inicio de sesión
        SharedPreferences appPrefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
        boolean hasSeenLoginScreen = appPrefs.getBoolean("hasSeenLoginScreen", false);

        if (hasSeenLoginScreen) {
            // Verificar estado de autenticación de Firebase
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // El usuario ha iniciado sesión con Firebase, ir a inicio
                navigateToHome();
                return;
            }

            // Verificar usuario no autorizado
            SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String nonAuthUserId = userPrefs.getString("non_authorized_user_id", null);
            if (nonAuthUserId != null) {
                // Verificar que el usuario no autorizado aún existe en Firestore
                db.collection("non_authorized_users").document(nonAuthUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // El usuario no autorizado existe, ir a inicio
                            navigateToHome();
                        } else {
                            // El documento del usuario no autorizado fue eliminado, limpiar preferencias
                            userPrefs.edit().remove("non_authorized_user_id").apply();
                            appPrefs.edit().remove("hasSeenLoginScreen").apply();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al verificar usuario no autorizado", e);
                        // En caso de error, permanecer en la pantalla de inicio de sesión
                    });
            }
        }
        // Si llegamos aquí, ya sea:
        // 1. El usuario no ha visto la pantalla de inicio de sesión antes
        // 2. No se encontró un estado de usuario válido
        // En ambos casos, permanecer en la pantalla de inicio de sesión
    }

    private void setThemePreference(String themeName) {
        SharedPreferences.Editor editor = getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit();
        editor.putString(THEME_KEY, themeName);
        editor.apply();
        int nightMode = themeName.equals("dark") ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    private void updateThemeToggleSelection(String theme) {
        if (theme.equals("dark")) {
            buttonLightMode.setSelected(false);
            buttonDarkMode.setSelected(true);
        } else {
            buttonLightMode.setSelected(true);
            buttonDarkMode.setSelected(false);
        }
    }

    private void restartActivity() {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private void setHasSeenLoginScreen() {
        SharedPreferences appPrefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
        appPrefs.edit().putBoolean("hasSeenLoginScreen", true).apply();
    }
}
