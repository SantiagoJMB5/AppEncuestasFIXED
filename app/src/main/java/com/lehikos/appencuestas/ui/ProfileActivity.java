package com.lehikos.appencuestas.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.lehikos.appencuestas.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import android.graphics.BitmapFactory;
import android.content.SharedPreferences;

import java.io.IOException;
import android.util.Log;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;
    private ImageButton profilePicButton;
    private Uri profileImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_profile);

        profilePicButton = findViewById(R.id.profile_pic_button);
        Log.d(TAG, "profilePicButton initialized");
        profilePicButton.setOnClickListener(v -> showProfilePictureDialog());

        Button changeUsernameButton = findViewById(R.id.change_username_button);
        changeUsernameButton.setOnClickListener(v -> showChangeUsernameDialog());

        Button viewDataUsageButton = findViewById(R.id.view_data_usage_button);
        viewDataUsageButton.setOnClickListener(v -> showDataUsageDialog());

        Button logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> logoutUser());

        Button deleteAccountButton = findViewById(R.id.delete_account_button);
        Log.d(TAG, "deleteAccountButton initialized");
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());

        // Load and display saved profile picture
        loadProfilePicture();
    }

    private void loadProfilePicture() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Non-authorized user: load from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String base64 = prefs.getString("profile_picture_base64", null);
            if (base64 != null) {
                byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                profilePicButton.setImageBitmap(bitmap);
            } else {
                profilePicButton.setImageResource(R.drawable.ic_profile);
            }
        } else {
            // Authorized user: load from Firestore
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String base64 = documentSnapshot.getString("profile_picture_base64");
                    if (base64 != null) {
                        byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        profilePicButton.setImageBitmap(bitmap);
                    } else {
                        profilePicButton.setImageResource(R.drawable.ic_profile);
                    }
                })
                .addOnFailureListener(e -> profilePicButton.setImageResource(R.drawable.ic_profile));
        }
    }

    private void saveProfilePicture(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Non-authorized user: save to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().putString("profile_picture_base64", base64).apply();
        } else {
            // Authorized user: save to Firestore
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .update("profile_picture_base64", base64);
        }
    }

    private void showProfilePictureDialog() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_from_gallery)};
        new AlertDialog.Builder(this)
            .setTitle(R.string.change_profile_picture)
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }
                } else if (which == 1) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
                }
            })
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                profilePicButton.setImageBitmap(imageBitmap);
                saveProfilePicture(imageBitmap);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                        profilePicButton.setImageBitmap(bitmap);
                        saveProfilePicture(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.delete_account)
            .setMessage(R.string.delete_account_warning)
            .setPositiveButton(R.string.delete_forever, (dialog, which) -> deleteAccount())
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void deleteAccount() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            auth.getCurrentUser().delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.account_deleted, Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.error_deleting_account, Toast.LENGTH_LONG).show();
                    }
                });
        }
    }

    private void showChangeUsernameDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            Toast.makeText(this, "Debes iniciar sesión para cambiar tu nombre de usuario.", Toast.LENGTH_LONG).show();
            return;
        }
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_username, null);
        final android.widget.EditText editText = dialogView.findViewById(R.id.editTextUsername);
        new AlertDialog.Builder(this)
            .setTitle(R.string.change_username)
            .setView(dialogView)
            .setPositiveButton(R.string.yes, (dialog, which) -> {
                String newUsername = editText.getText().toString().trim();
                if (!newUsername.isEmpty()) {
                    confirmUsernameDialog(newUsername);
                }
            })
            .setNegativeButton(R.string.no, null)
            .show();
    }

    private void confirmUsernameDialog(String username) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm_username_title)
            .setMessage(getString(R.string.confirm_username_message, username))
            .setPositiveButton(R.string.yes, (dialog, which) -> saveUsername(username))
            .setNegativeButton(R.string.no, null)
            .show();
    }

    private void saveUsername(String username) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().putString("user_username", username).apply();
        // Save to Firestore as well
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .update("username", username);
        } else {
            String userId = prefs.getString("non_authorized_user_id", null);
            if (userId != null) {
                FirebaseFirestore.getInstance().collection("non_authorized_users").document(userId)
                    .update("username", username);
            }
        }
        Toast.makeText(this, getString(R.string.confirm_username_title) + ": " + username, Toast.LENGTH_SHORT).show();
    }

    private void showDataUsageDialog() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = prefs.getString("user_username", getString(R.string.placeholder_user));
        int level = prefs.getInt("user_level", 1);
        int experience = prefs.getInt("user_experience", 0);
        int totalSurveys = prefs.getInt("user_total_surveys", 0);
        String message = getString(R.string.statistics_username_label) + ": " + username + "\n" +
                getString(R.string.level_format, level) + "\n" +
                getString(R.string.experience_format, experience, 100) + "\n" +
                getString(R.string.total_surveys_format, totalSurveys);
        new AlertDialog.Builder(this)
            .setTitle(R.string.view_data_usage)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void logoutUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth != null) {
            mAuth.signOut();
        }
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userPrefs.edit().remove("non_authorized_user_id").apply();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
} 