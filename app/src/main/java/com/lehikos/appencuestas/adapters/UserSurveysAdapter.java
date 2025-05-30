package com.lehikos.appencuestas.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.models.Survey;

import java.util.ArrayList;
import java.util.List;

public class UserSurveysAdapter extends RecyclerView.Adapter<UserSurveysAdapter.UserSurveyViewHolder> {

    private final OnSurveyActionsListener listener;
    private List<Survey> surveys;

    public interface OnSurveyActionsListener {
        void onSurveyEditClick(Survey survey);
        void onSurveyAnswerClick(Survey survey);
        void onSurveyViewStatsClick(Survey survey);
        // You could keep onSurveyClick(Survey survey) here if the whole item is clickable for a default action
    }

    public UserSurveysAdapter(List<Survey> initialSurveys, OnSurveyActionsListener listener) {
        this.surveys = new ArrayList<>(initialSurveys != null ? initialSurveys : new ArrayList<>());
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserSurveyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_survey, parent, false);
        return new UserSurveyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserSurveyViewHolder holder, int position) {
        Survey survey = surveys.get(position);
        if (survey == null) return;

        holder.surveyTitleTextView.setText(survey.getTitle());
        holder.surveyDescriptionTextView.setText(survey.getDescription());
        
        // Format tags
        if (survey.getTags() != null && !survey.getTags().isEmpty()) {
            String tagsText = holder.itemView.getContext().getString(R.string.tags_label) + " " + 
                String.join(", ", survey.getTags());
            holder.surveyTagsTextView.setText(tagsText);
            holder.surveyTagsTextView.setVisibility(View.VISIBLE);
        } else {
            holder.surveyTagsTextView.setVisibility(View.GONE);
        }

        // Set anonymity status
        holder.surveyAnonymityTextView.setText(survey.isAnonymous() ? 
            holder.itemView.getContext().getString(R.string.anonymous_label) : 
            holder.itemView.getContext().getString(R.string.public_label));

        // Set click listeners for buttons
        holder.editButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSurveyEditClick(survey);
            }
        });

        holder.answerButton.setOnClickListener(v -> {
             if (listener != null) {
                 listener.onSurveyAnswerClick(survey);
             }
        });

        holder.viewStatsButton.setOnClickListener(v -> {
            if (listener != null) {
                 listener.onSurveyViewStatsClick(survey);
            }
        });
    }

    @Override
    public int getItemCount() {
        return surveys != null ? surveys.size() : 0;
    }

    public void updateSurveys(List<Survey> newSurveys) {
        if (newSurveys == null) {
            newSurveys = new ArrayList<>();
        }
        this.surveys.clear();
        this.surveys.addAll(newSurveys);
        notifyDataSetChanged();
    }

    static class UserSurveyViewHolder extends RecyclerView.ViewHolder {
        TextView surveyTitleTextView;
        TextView surveyDescriptionTextView;
        TextView surveyTagsTextView;
        TextView surveyAnonymityTextView;
        Button editButton;
        Button answerButton;
        Button viewStatsButton;

        UserSurveyViewHolder(View itemView) {
            super(itemView);
            surveyTitleTextView = itemView.findViewById(R.id.user_survey_title);
            surveyDescriptionTextView = itemView.findViewById(R.id.user_survey_description);
            surveyTagsTextView = itemView.findViewById(R.id.user_survey_tags);
            surveyAnonymityTextView = itemView.findViewById(R.id.user_survey_anonymity);
            editButton = itemView.findViewById(R.id.user_survey_edit_button);
            answerButton = itemView.findViewById(R.id.user_survey_answer_button);
            viewStatsButton = itemView.findViewById(R.id.user_survey_view_button);
        }
    }
} 