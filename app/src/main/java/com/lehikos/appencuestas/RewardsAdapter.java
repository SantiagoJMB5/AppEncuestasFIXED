package com.lehikos.appencuestas;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RewardsAdapter extends RecyclerView.Adapter<RewardsAdapter.RewardViewHolder> {
    private final List<RewardManager.RewardData> rewards;
    private final RewardManager rewardManager;
    private final Context context;

    public RewardsAdapter(List<RewardManager.RewardData> rewards, RewardManager rewardManager, SharedPreferences prefs) {
        this.rewards = rewards;
        this.rewardManager = rewardManager;
        this.context = null;
    }

    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reward, parent, false);
        return new RewardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RewardViewHolder holder, int position) {
        RewardManager.RewardData reward = rewards.get(position);
        Context context = holder.itemView.getContext();
        
        holder.titleText.setText(reward.getTitle(context));
        holder.descriptionText.setText(reward.getDescription(context));
        holder.levelText.setText(context.getString(R.string.level_required, reward.streakThreshold));
        holder.iconImage.setImageResource(reward.iconResId);

        // Verificar si la recompensa está lograda
        boolean isAchieved = rewardManager.isRewardRemoved(reward.key);
        holder.itemView.setAlpha(isAchieved ? 0.5f : 1.0f);
        
        // Mostrar marca de verificación si está lograda
        holder.checkmarkImage.setVisibility(isAchieved ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return rewards.size();
    }

    static class RewardViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView descriptionText;
        TextView levelText;
        ImageView iconImage;
        ImageView checkmarkImage;

        RewardViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.reward_title);
            descriptionText = itemView.findViewById(R.id.reward_description);
            levelText = itemView.findViewById(R.id.reward_level);
            iconImage = itemView.findViewById(R.id.reward_icon);
            checkmarkImage = itemView.findViewById(R.id.reward_checkmark);
        }
    }
} 