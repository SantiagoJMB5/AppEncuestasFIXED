package com.lehikos.appencuestas.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;
import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.RewardManager;

import java.util.HashSet;
import java.util.Set;

public class RewardItemView extends MaterialCardView {
    private TextView rewardName;
    private ProgressBar rewardProgress;
    private ImageView rewardIcon;
    private LinearLayout rewardContainer;
    private RewardManager.RewardData rewardData;
    private SharedPreferences prefs;
    private static final String ANIMATION_COMPLETED_PREFIX = "animation_completed_";
    private OnAnimationEndListener animationEndListener;
    private RewardManager rewardManager;
    private final String TAG = "RewardItemView";

    public interface OnAnimationEndListener {
        void onAnimationEnd();
    }

    public RewardItemView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public RewardItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RewardItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
    }

    public void setRewardManager(RewardManager rewardManager) {
        this.rewardManager = rewardManager;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        rewardName = findViewById(R.id.reward_name);
        rewardProgress = findViewById(R.id.reward_progress);
        rewardIcon = findViewById(R.id.reward_icon);
        rewardContainer = findViewById(R.id.reward_item_container);
    }

    public void setRewardData(RewardManager.RewardData rewardData) {
        this.rewardData = rewardData;
        rewardName.setText(rewardData.getTitle(getContext()));
        rewardProgress.setMax(rewardData.streakThreshold);
    }

    public void setProgress(int progress) {
        rewardProgress.setProgress(progress);
    }

    public void setIcon(int drawable) {
        rewardIcon.setImageResource(drawable);
    }

    public void setClickListener() {
        rewardContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentLevel = prefs.getInt("level", 1);
                if(prefs.getBoolean(rewardData.prefsKey,false)){
                    if(rewardManager.isRewardRemoved(getRewardKey())){
                        markRewardAsNotRemoved();
                        setVisibility(View.VISIBLE);
                        Log.d(TAG, "onClick: Añadir recompensa " + getRewardKey());
                    }else{
                        markRewardAsRemoved();
                        startSlideOutAnimation();
                        Log.d(TAG, "onClick: Remover recompensa " + getRewardKey());
                    }
                }else{
                    if(rewardManager.getRewardOrder(getRewardKey()) > 2){
                        if(currentLevel < rewardData.streakThreshold){
                            Log.d(TAG, "onClick: Recompensa aún no desbloqueada " + getRewardKey());
                            return;
                        }
                        startAnimation();
                        prefs.edit().putBoolean(rewardData.prefsKey, true).apply();
                        setIcon(R.drawable.tick);
                    }else{
                        if(currentLevel < rewardData.streakThreshold){
                            Log.d(TAG, "onClick: Recompensa aún no desbloqueada " + getRewardKey());
                            return;
                        }
                        startAnimation();
                        prefs.edit().putBoolean(rewardData.prefsKey, true).apply();
                        setIcon(R.drawable.tick);
                    }
                }
            }
        });
    }

    private void startAnimation(){
        Log.d(TAG, "startAnimation: Started");
        if(rewardManager.getRewardOrder(getRewardKey()) > 2){
            ObjectAnimator fadeAnim = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f);
            fadeAnim.setDuration(1000);
            fadeAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    setVisibility(View.VISIBLE);
                }
            });
            fadeAnim.start();
        }
    }

    private void startSlideOutAnimation() {
        Log.d(TAG, "startSlideOutAnimation: Started");
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationX", 0f, getWidth());
        animator.setDuration(300);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "onAnimationEnd: Started");
                super.onAnimationEnd(animation);
                setAnimationCompleted(rewardData.key);
                RewardItemView.this.setVisibility(View.GONE);
                markRewardAsRemoved();
                if (animationEndListener != null) {
                    animationEndListener.onAnimationEnd();
                }
                Log.d(TAG, "onAnimationEnd: Ended");
            }
        });
        animator.start();
        Log.d(TAG, "startSlideOutAnimation: Ended");
    }

    private void markRewardAsRemoved(){
        rewardManager.markRewardAsRemoved(getRewardKey());
    }

    private void markRewardAsNotRemoved(){
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> removedRewards = new HashSet<>(prefs.getStringSet("removed_rewards", new HashSet<>()));
        removedRewards.remove(getRewardKey());
        editor.putStringSet("removed_rewards", removedRewards).apply();
    }

    private void setAnimationCompleted(String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ANIMATION_COMPLETED_PREFIX + key, true);
        editor.apply();
    }

    public String getRewardKey() {
        return rewardData.key;
    }

    public void setOnAnimationEndListener(OnAnimationEndListener listener) {
        this.animationEndListener = listener;
    }
} 