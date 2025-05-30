package com.lehikos.appencuestas.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar; // Opcional: para indicación de carga
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lehikos.appencuestas.R;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText editTextEmailRegister, editTextPasswordRegister, editTextConfirmPasswordRegister;
    private Button buttonRegister;
    private TextView textViewGoToLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        editTextEmailRegister = findViewById(R.id.editTextEmailRegister);
        editTextPasswordRegister = findViewById(R.id.editTextPasswordRegister);
        editTextConfirmPasswordRegister = findViewById(R.id.editTextConfirmPasswordRegister);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewGoToLogin = findViewById(R.id.textViewGoToLogin);
        // progressBar = findViewById(R.id.progressBarRegister); // Agregar ProgressBar a tu XML si usas esto

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        textViewGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void registerUser() {
        String email = editTextEmailRegister.getText().toString().trim();
        String password = editTextPasswordRegister.getText().toString().trim();
        String confirmPassword = editTextConfirmPasswordRegister.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmailRegister.setError(getString(R.string.error_email_required));
            editTextEmailRegister.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmailRegister.setError(getString(R.string.error_email_invalid));
            editTextEmailRegister.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPasswordRegister.setError(getString(R.string.error_password_required));
            editTextPasswordRegister.requestFocus();
            return;
        }
        if (password.length() < 6) {
            editTextPasswordRegister.setError(getString(R.string.error_password_length));
            editTextPasswordRegister.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            editTextConfirmPasswordRegister.setError(getString(R.string.error_confirm_password_required));
            editTextConfirmPasswordRegister.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            editTextConfirmPasswordRegister.setError(getString(R.string.error_passwords_do_not_match));
            editTextConfirmPasswordRegister.requestFocus();
            return;
        }

        // if (progressBar != null) progressBar.setVisibility(View.VISIBLE); // Mostrar barra de progreso

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // if (progressBar != null) progressBar.setVisibility(View.GONE); // Ocultar barra de progreso
                        if (task.isSuccessful()) {
                            // Registro exitoso, actualizar UI con la información del usuario registrado
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(RegisterActivity.this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show();
                            // Opcionalmente puedes enviar un correo de verificación aquí
                            // user.sendEmailVerification();
                            // Navegar a la pantalla de inicio de sesión o directamente a la pantalla de inicio
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finishAffinity(); // Finalizar esta actividad y todas las actividades padre
                        } else {
                            // Si el registro falla, mostrar un mensaje al usuario
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, getString(R.string.authentication_failed, task.getException().getMessage()),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
