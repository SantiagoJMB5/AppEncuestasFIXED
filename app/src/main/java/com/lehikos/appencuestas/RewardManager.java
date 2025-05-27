package com.lehikos.appencuestas;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RewardManager {
    private static final String TAG = "RewardManager";
    private static final String REWARDS_KEY = "rewards";
    private static final String EXPERIENCE_KEY = "experience";
    private static final String LEVEL_KEY = "level";
    private static final String REMOVED_REWARDS_KEY = "removed_rewards";
    private static final String ANIMATION_COMPLETED_PREFIX = "animation_completed";

    private final SharedPreferences prefs;
    private final Map<String, RewardData> rewards = new HashMap<>();

    public RewardManager(SharedPreferences prefs) {
        this.prefs = prefs;
        initializeRewards();
    }

    private void initializeRewards() {
        rewards.put("novice", new RewardData(1, R.string.reward_novice_title, R.string.reward_novice_description, "novice_rewarded", R.drawable.default_icon, "novice"));
        rewards.put("explorer", new RewardData(5, R.string.reward_explorer_title, R.string.reward_explorer_description, "explorer_rewarded", R.drawable.default_icon, "explorer"));
        rewards.put("enthusiast", new RewardData(10, R.string.reward_enthusiast_title, R.string.reward_enthusiast_description, "enthusiast_rewarded", R.drawable.default_icon, "enthusiast"));
        rewards.put("expert", new RewardData(15, R.string.reward_expert_title, R.string.reward_expert_description, "expert_rewarded", R.drawable.default_icon, "expert"));
        rewards.put("master", new RewardData(20, R.string.reward_master_title, R.string.reward_master_description, "master_rewarded", R.drawable.default_icon, "master"));
        rewards.put("legend", new RewardData(25, R.string.reward_legend_title, R.string.reward_legend_description, "legend_rewarded", R.drawable.default_icon, "legend"));
    }

    public void addExperience(int experience) {
        int currentExp = prefs.getInt(EXPERIENCE_KEY, 0);
        int currentLevel = prefs.getInt(LEVEL_KEY, 1);

        currentExp += experience;
        int newLevel = calculateLevel(currentExp);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(EXPERIENCE_KEY, currentExp);

        if (newLevel > currentLevel) {
            editor.putInt(LEVEL_KEY, newLevel);
            Log.d(TAG, "¡Nuevo nivel alcanzado! Nivel " + newLevel);
        }

        editor.apply();
    }

    private int calculateLevel(int experience) {
        // Cálculo simple de nivel: 100 XP por nivel
        return (experience / 100) + 1;
    }

    public int getCurrentExperience() {
        return prefs.getInt(EXPERIENCE_KEY, 0);
    }

    public int getCurrentLevel() {
        return prefs.getInt(LEVEL_KEY, 1);
    }

    public List<String> checkAndGrantRewards(int currentStreak) {
        List<String> earnedRewards = new ArrayList<>();
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> loggedDays = prefs.getStringSet("logged_days", new HashSet<>());
        int totalDaysLogged = loggedDays.size();

        for (Map.Entry<String, RewardData> entry : rewards.entrySet()) {
            String rewardKey = entry.getKey();
            RewardData reward = entry.getValue();
            boolean requirementMet;
            if (rewardKey.equals("habit_master") || rewardKey.equals("iron_will") || rewardKey.equals("legend")) {
                requirementMet = totalDaysLogged >= reward.streakThreshold;
            } else {
                requirementMet = currentStreak >= reward.streakThreshold;
            }
            if (requirementMet) {
                boolean alreadyRewarded = prefs.getBoolean(reward.prefsKey, false);
                if (!alreadyRewarded) {
                    editor.putBoolean(reward.prefsKey, true);
                    earnedRewards.add(rewardKey);
                }
            }
        }
        editor.apply();
        return earnedRewards;
    }

    public RewardData getRewardDetails(String rewardKey) {
        return rewards.get(rewardKey);
    }

    public static class RewardData {
        public final int streakThreshold;
        public final int titleResId;
        public final int descriptionResId;
        public final String prefsKey;
        public final int iconResId;
        public final String key;

        public RewardData(int streakThreshold, int titleResId, int descriptionResId, String prefsKey, int iconResId, String key) {
            this.streakThreshold = streakThreshold;
            this.titleResId = titleResId;
            this.descriptionResId = descriptionResId;
            this.prefsKey = prefsKey;
            this.iconResId = iconResId;
            this.key = key;
        }

        private int getDescriptionResId(String key) {
            switch (key) {
                case "novice": return R.string.reward_novice_description;
                case "explorer": return R.string.reward_explorer_description;
                case "enthusiast": return R.string.reward_enthusiast_description;
                case "expert": return R.string.reward_expert_description;
                case "master": return R.string.reward_master_description;
                case "legend": return R.string.reward_legend_description;
                default: return R.string.reward_novice_description;
            }
        }

        public String getTitle(Context context) {
            return context.getString(titleResId);
        }

        public String getDescription(Context context) {
            return context.getString(descriptionResId);
        }
    }

    public int getNextRewardThreshold(int currentStreak) {
        List<RewardData> rewards = getAllRewards();
        int min = Integer.MAX_VALUE;

        for (RewardData r : rewards) {
            if (currentStreak < r.streakThreshold && r.streakThreshold < min) {
                min = r.streakThreshold;
            }
        }

        return min == Integer.MAX_VALUE ? currentStreak : min;
    }

    public List<RewardData> getAllRewards() {
        List<RewardData> rewards = new ArrayList<>();
        rewards.add(getRewardDetails("novice"));
        rewards.add(getRewardDetails("explorer"));
        rewards.add(getRewardDetails("enthusiast"));
        rewards.add(getRewardDetails("expert"));
        rewards.add(getRewardDetails("master"));
        rewards.add(getRewardDetails("legend"));
        return rewards;
    }

    public boolean isRewardRemoved(String rewardKey) {
        Set<String> removedRewards = prefs.getStringSet(REMOVED_REWARDS_KEY, new HashSet<>());
        return removedRewards.contains(rewardKey);
    }

    public void markRewardAsRemoved(String rewardKey) {
        Set<String> removedRewards = new HashSet<>(prefs.getStringSet(REMOVED_REWARDS_KEY, new HashSet<>()));
        removedRewards.add(rewardKey);
        prefs.edit().putStringSet(REMOVED_REWARDS_KEY, removedRewards).apply();
    }

    public RewardData getHighestAchievedReward() {
        List<RewardData> allRewards = getAllRewards();
        RewardData highestReward = null;

        for (RewardData reward : allRewards) {
            if (prefs.getBoolean(reward.prefsKey, false)) {
                if (highestReward == null || getRewardOrder(reward.key) > getRewardOrder(highestReward.key)) {
                    highestReward = reward;
                }
            }
        }

        return highestReward;
    }

    public int getRewardOrder(String rewardKey) {
        // Orden de las recompensas de menor a mayor
        switch (rewardKey) {
            case "week_streak":
                return 1;
            case "streak_15_days":
                return 2;
            case "habit_master":
                return 3;
            case "unstoppable":
                return 4;
            case "iron_will":
                return 5;
            case "veteran":
                return 6;
            case "legend":
                return 7;
            default:
                return 0;
        }
    }

    public boolean isSecondReward(String rewardKey) {
        return getRewardOrder(rewardKey) == 2;
    }

    public String getNextRewardDescription(Context context) {
        int currentLevel = getCurrentLevel();
        int nextLevel = getNextRewardThreshold(currentLevel);

        for (RewardData reward : getAllRewards()) {
            if (reward.streakThreshold == nextLevel) {
                return reward.getDescription(context);
            }
        }
        return null;
    }
}