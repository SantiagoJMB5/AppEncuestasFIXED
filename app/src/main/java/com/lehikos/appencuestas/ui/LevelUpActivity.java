package com.lehikos.appencuestas.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.models.User;

public class LevelUpActivity extends BaseActivity {
    public static final String EXTRA_LEVEL = "level";
    public static final String EXTRA_EXPERIENCE = "experience";
    public static final String EXTRA_NEXT_REWARD = "next_reward";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_up);

        int level = getIntent().getIntExtra(EXTRA_LEVEL, 1);
        int experience = getIntent().getIntExtra(EXTRA_EXPERIENCE, 0);
        String nextReward = getIntent().getStringExtra(EXTRA_NEXT_REWARD);

        TextView titleText = findViewById(R.id.level_up_title);
        TextView messageText = findViewById(R.id.level_up_message);
        TextView nextRewardText = findViewById(R.id.level_up_next_reward);
        TextView tapToContinueText = findViewById(R.id.level_up_tap_to_continue);

        titleText.setText(getString(R.string.level_up_title, level));
        messageText.setText(getString(R.string.level_up_message, experience));
        
        if (nextReward != null) {
            nextRewardText.setText(getString(R.string.level_up_next_reward, nextReward));
        } else {
            nextRewardText.setVisibility(View.GONE);
        }

        tapToContinueText.setText(R.string.level_up_tap_to_continue);

        // Cerrar actividad al tocar
        View rootView = findViewById(android.R.id.content);
        rootView.setOnClickListener(v -> finish());
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_level_up;
    }
} 