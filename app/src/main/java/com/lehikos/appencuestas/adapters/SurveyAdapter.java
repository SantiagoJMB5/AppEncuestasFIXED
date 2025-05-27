package com.lehikos.appencuestas.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.models.Survey;

import java.util.ArrayList;
import java.util.List;

public class SurveyAdapter extends RecyclerView.Adapter<SurveyAdapter.SurveyViewHolder> {
    private final OnSurveyClickListener listener; // Make listener final if set only in constructor
    // Use ArrayList for easier manipulation with DiffUtil, or ensure your List type supports it
    private List<Survey> surveys;

    public interface OnSurveyClickListener {
        void onSurveyClick(Survey survey);
    }

    public SurveyAdapter(List<Survey> initialSurveys, OnSurveyClickListener listener) {
        // Initialize with a new list to avoid modifying the original list passed in, if necessary
        this.surveys = new ArrayList<>(initialSurveys != null ? initialSurveys : new ArrayList<>());
        this.listener = listener;
    }

    @NonNull
    @Override
    public SurveyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_survey, parent, false);
        return new SurveyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SurveyViewHolder holder, int position) {
        Survey survey = surveys.get(position);
        if (survey == null) return; // Basic null check for the survey object

        holder.titleText.setText(survey.getTitle());
        holder.descriptionText.setText(survey.getDescription());
        // Consider adding a placeholder or handling null for experience reward if it can be optional
        holder.experienceText.setText(String.format("+%d XP", survey.getExperienceReward()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSurveyClick(survey);
            }
        });
    }

    @Override
    public int getItemCount() {
        return surveys != null ? surveys.size() : 0; // Null check for surveys list
    }

    /**
     * Updates the list of surveys displayed by the adapter using DiffUtil for efficient updates.
     *
     * @param newSurveys The new list of surveys to display.
     */
    public void updateSurveys(List<Survey> newSurveys) {
        if (newSurveys == null) {
            newSurveys = new ArrayList<>(); // Ensure newSurveys is not null
        }

        final SurveyDiffCallback diffCallback = new SurveyDiffCallback(this.surveys, newSurveys);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.surveys.clear();
        this.surveys.addAll(newSurveys);
        diffResult.dispatchUpdatesTo(this); // This efficiently updates the RecyclerView
    }

    static class SurveyViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView descriptionText;
        TextView experienceText;

        SurveyViewHolder(View itemView) {
            super(itemView);
            // Ensure these IDs match your R.layout.item_survey XML file
            titleText = itemView.findViewById(R.id.survey_title);
            descriptionText = itemView.findViewById(R.id.survey_description);
            experienceText = itemView.findViewById(R.id.survey_experience);
        }
    }

    /**
     * DiffUtil.Callback implementation for calculating differences between two lists of Surveys.
     * This helps RecyclerView update efficiently.
     */
    private static class SurveyDiffCallback extends DiffUtil.Callback {
        private final List<Survey> oldList;
        private final List<Survey> newList;

        public SurveyDiffCallback(List<Survey> oldList, List<Survey> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        /**
         * Called by DiffUtil to decide whether two objects represent the same item.
         * For example, if your items have unique IDs, this method should check their equality.
         */
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // CRITICAL: Your Survey model needs a unique identifier (e.g., getId())
            // for DiffUtil to work correctly.
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        /**
         * Called by DiffUtil only if areItemsTheSame() returns true to check whether
         * the visual representation of an item has changed.
         * This method is used to detect changes in item content.
         */
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Survey oldSurvey = oldList.get(oldItemPosition);
            Survey newSurvey = newList.get(newItemPosition);
            // Compare all fields that affect the UI representation of the item.
            // Using .equals() if implemented in Survey, or compare fields manually.
            // For example:
            return oldSurvey.getTitle().equals(newSurvey.getTitle()) &&
                    oldSurvey.getDescription().equals(newSurvey.getDescription()) &&
                    oldSurvey.getExperienceReward() == newSurvey.getExperienceReward();
            // Add other relevant fields if they are displayed or affect the item's appearance.
            // If your Survey class has a proper .equals() method, you could just use:
            // return oldSurvey.equals(newSurvey);
        }
    }
}