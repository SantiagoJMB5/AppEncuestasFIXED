package com.lehikos.appencuestas.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.lehikos.appencuestas.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public abstract class BaseActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FrameLayout contentFrame;
    private boolean isHomeActivity = false;

    protected static final String PREFS_NAME = "UserPrefs";
    protected static final String APP_PREFS = "AppPrefs";

    protected SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar sharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Mostrar pantalla de login en el primer inicio, aunque haya usuario autenticado
        SharedPreferences appPrefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
        boolean hasSeenLogin = appPrefs.getBoolean("hasSeenLoginScreen", false);
        if (!hasSeenLogin) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Si el usuario no está autenticado, redirigir a LoginActivity
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_base);

        // Inicializa la vista para la navegación
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        contentFrame = findViewById(R.id.content_frame);

        // Prepara la vista
        View contentView = getLayoutInflater().inflate(getLayoutResourceId(), contentFrame, false);
        contentFrame.addView(contentView);

        // Prepara la navegación inferior
        setupBottomNavigation();

        // Comprueba si esto es HomeActivity
        isHomeActivity = this instanceof HomeActivity;
    }

    protected abstract @LayoutRes int getLayoutResourceId();

    // Función que prepara la navegación inferior
    private void setupBottomNavigation() {
        // Establece el elemento seleccionado correcto en función de la actividad actual
        if (this instanceof HomeActivity) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        } else if (this instanceof StatisticsActivity) {
            bottomNavigationView.setSelectedItemId(R.id.Btn_statistics);
        } else if (this instanceof RewardsActivity) {
            bottomNavigationView.setSelectedItemId(R.id.Btn_rewards);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            MenuItem selectedItem = bottomNavigationView.getMenu().findItem(itemId);

            // Obtener la clase de la actividad actual
            Class<?> currentActivityClass = this.getClass();

            // Navegar solo si no estamos ya en la actividad de destino
            if (itemId == R.id.navigation_home && currentActivityClass != HomeActivity.class) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.Btn_statistics && currentActivityClass != StatisticsActivity.class) {
                startActivity(new Intent(this, StatisticsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.Btn_rewards && currentActivityClass != RewardsActivity.class) {
                if (!handleRewardsButtonClick()) {
                    //Si la función devuelve falso (lo que significa que el nivel fue demasiado bajo), deselecciona el elemento manualmente.
                    bottomNavigationView.setSelectedItemId(bottomNavigationView.getSelectedItemId());
                    return false;
                }
                return true;
            }

            return false;
        });
    }

    // Función para establecer el elemento seleccionado en la navegación inferior
    protected void setSelectedNavigationItem(int itemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(itemId);
        }
    }

    @Override
    public void onBackPressed() {
        if (!isHomeActivity) {
            // Si no estás en HomeActivity te lleva a HomeActivity
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Si ya está en HomeActivity, mostrar mensaje de salida y finalizar
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            super.onBackPressed();
        }
    }

    private boolean handleRewardsButtonClick() {
        // Comprobamos si se cumplen los requisitos para activar RewardsActivity (nivel 2)
        int currentLevel = 1;
        boolean isAuthorized = FirebaseAuth.getInstance().getCurrentUser() != null;
        if (isAuthorized) {
            // Usuario autenticado: obtener nivel de Firestore (sincrónico para navegación, pero idealmente asíncrono)
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(userId);
            final boolean[] allowed = {false};
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                int level = 1;
                if (documentSnapshot.exists() && documentSnapshot.contains("level")) {
                    level = documentSnapshot.getLong("level").intValue();
                }
                if (level >= 2) {
                    launchRewardsActivity();
                } else {
                    android.widget.Toast.makeText(this, "Alcanza el nivel 2 para desbloquear recompensas", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
            // Retornar falso para evitar navegación inmediata (esperar callback)
            return false;
        } else {
            // Usuario no autorizado: usar SharedPreferences
            currentLevel = sharedPreferences.getInt("level", 1);
            if (currentLevel >= 2) {
                launchRewardsActivity();
                return true;
            } else {
                android.widget.Toast.makeText(this, "Alcanza el nivel 2 para desbloquear recompensas", android.widget.Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }

    private void launchRewardsActivity() {
        Intent intent = new Intent(this, RewardsActivity.class);
        startActivity(intent);
    }
}