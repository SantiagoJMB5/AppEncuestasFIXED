package com.lehikos.appencuestas.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.models.User;

import java.util.HashMap;
import java.util.Map;

public class LevelUpActivity extends BaseActivity {
    private static final String TAG = "LevelUpActivity";
    public static final String EXTRA_LEVEL = "level";
    public static final String EXTRA_EXPERIENCE = "experience";
    public static final String EXTRA_NEXT_REWARD = "next_reward";
    private static final String APP_PREFS = "AppPrefs";
    private static final String SOUND_EFFECTS_ENABLED_KEY = "sound_effects_enabled";
    private static final String CURRENCY_KEY = "user_currency";
    private MediaPlayer levelUpSound;
    private int gemReward;
    private boolean hasAwardedGems = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_up);

        // Check if sound effects are enabled
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
        boolean soundEffectsEnabled = prefs.getBoolean(SOUND_EFFECTS_ENABLED_KEY, true);

        // Play level up sound if enabled
        if (soundEffectsEnabled) {
            levelUpSound = MediaPlayer.create(this, R.raw.level_up_sfx);
            levelUpSound.setOnCompletionListener(mp -> {
                mp.release();
                levelUpSound = null;
            });
            levelUpSound.start();
        }

        int level = getIntent().getIntExtra(EXTRA_LEVEL, 1);
        int experience = getIntent().getIntExtra(EXTRA_EXPERIENCE, 0);
        String nextReward = getIntent().getStringExtra(EXTRA_NEXT_REWARD);

        // Calculate gem reward based on level
        gemReward = calculateGemReward(level);

        TextView titleText = findViewById(R.id.level_up_title);
        TextView messageText = findViewById(R.id.level_up_message);
        TextView nextRewardText = findViewById(R.id.level_up_next_reward);
        TextView tapToContinueText = findViewById(R.id.level_up_tap_to_continue);
        TextView gemRewardText = findViewById(R.id.gem_reward_text);

        titleText.setText(getString(R.string.level_up_title, level));
        messageText.setText(getString(R.string.level_up_message, experience));
        gemRewardText.setText("+" + gemReward);
        
        if (nextReward != null) {
            nextRewardText.setText(getString(R.string.level_up_next_reward, nextReward));
        } else {
            nextRewardText.setVisibility(View.GONE);
        }

        tapToContinueText.setText(R.string.level_up_tap_to_continue);

        // Cerrar actividad al tocar
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(v -> {
            if (!hasAwardedGems) {
                awardGems();
                showGemParticlesAnimation();
                hasAwardedGems = true;
            } else {
                finish();
            }
        });
    }

    private int calculateGemReward(int level) {
        if (level <= 10) {
            return level * 10; // 10 gems per level up to level 10
        } else {
            return 50; // 50 gems per level after level 10
        }
    }

    private void awardGems() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int currentGems = prefs.getInt(CURRENCY_KEY, 0);
        int newGems = currentGems + gemReward;
        prefs.edit().putInt(CURRENCY_KEY, newGems).apply();

        // Save to Firebase if user is logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(user.getUid());

            Map<String, Object> updates = new HashMap<>();
            updates.put("gems", newGems);

            userRef.update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Gems updated in Firebase successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating gems in Firebase", e));
        }
    }

    private void showGemParticlesAnimation() {
        ViewGroup parent = (ViewGroup) findViewById(android.R.id.content);
        View gemRewardContainer = findViewById(R.id.gem_reward_container);
        
        // Create gem text view
        TextView gemText = new TextView(this);
        gemText.setText("+" + gemReward);
        gemText.setTextColor(getResources().getColor(R.color.accent_color));
        gemText.setTextSize(20);
        
        // Add gem text to parent view
        parent.addView(gemText);
        
        // Position gem text
        int[] location = new int[2];
        gemRewardContainer.getLocationInWindow(location);
        gemText.setX(location[0] + gemRewardContainer.getWidth() / 2f);
        gemText.setY(location[1] + gemRewardContainer.getHeight() / 2f);
        
        // Create gem icons
        for (int i = 0; i < 5; i++) {
            ImageView gemIcon = new ImageView(this);
            gemIcon.setImageResource(R.drawable.ic_gem);
            gemIcon.setAlpha(0.8f);
            parent.addView(gemIcon);
            
            // Position gem icon
            gemIcon.setX(location[0] + gemRewardContainer.getWidth() / 2f);
            gemIcon.setY(location[1] + gemRewardContainer.getHeight() / 2f);
            
            // Animate gem icon
            float angle = (float) (i * Math.PI / 2.5f);
            float distance = 200f;
            float targetX = gemIcon.getX() + (float) (Math.cos(angle) * distance);
            float targetY = gemIcon.getY() + (float) (Math.sin(angle) * distance);
            
            ObjectAnimator translateX = ObjectAnimator.ofFloat(gemIcon, "translationX", 0f, targetX - gemIcon.getX());
            ObjectAnimator translateY = ObjectAnimator.ofFloat(gemIcon, "translationY", 0f, targetY - gemIcon.getY());
            ObjectAnimator alpha = ObjectAnimator.ofFloat(gemIcon, "alpha", 0.8f, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(gemIcon, "scaleX", 1f, 0.5f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(gemIcon, "scaleY", 1f, 0.5f);
            
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(translateX, translateY, alpha, scaleX, scaleY);
            animatorSet.setDuration(1000);
            animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
            
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    parent.removeView(gemIcon);
                }
            });
            
            animatorSet.start();
        }
        
        // Animate gem text
        ObjectAnimator translateY = ObjectAnimator.ofFloat(gemText, "translationY", 0f, -100f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(gemText, "alpha", 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(gemText, "scaleX", 1f, 1.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(gemText, "scaleY", 1f, 1.5f);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translateY, alpha, scaleX, scaleY);
        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                parent.removeView(gemText);
            }
        });
        
        animatorSet.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (levelUpSound != null) {
            levelUpSound.release();
            levelUpSound = null;
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_level_up;
    }
} 