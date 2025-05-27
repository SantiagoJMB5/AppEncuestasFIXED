package com.lehikos.appencuestas.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.RewardManager;
import com.lehikos.appencuestas.firebase.FirestoreService;
import com.lehikos.appencuestas.models.Answer;
import com.lehikos.appencuestas.models.Question;
import com.lehikos.appencuestas.models.Survey;
import com.lehikos.appencuestas.models.User;

import java.util.ArrayList;
import java.util.List;


public class SurveyActivity extends AppCompatActivity {
    public static final String EXTRA_SURVEY_ID = "survey_id";
    public static final String EXTRA_SURVEY_TITLE = "survey_title";
    public static final String EXTRA_SURVEY_DESCRIPTION = "survey_description";
    public static final String EXTRA_SURVEY_EXPERIENCE = "survey_experience";
    public static final String EXTRA_SURVEY_REQUIRED_LEVEL = "survey_required_level";
    public static final String EXTRA_SURVEY_QUESTIONS = "survey_questions";

    private RecyclerView questionsRecyclerView;
    private TextView surveyTitleText;
    private TextView surveyDescriptionText;
    private TextView rewardText;
    private Button submitButton;
    private Survey currentSurvey;
    private RewardManager rewardManager;
    private SurveyQuestionsAdapter questionsAdapter;
    private FirestoreService firestoreService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("SurveyActivity", "onCreate started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        firestoreService = new FirestoreService();

        questionsRecyclerView = findViewById(R.id.questions_container);
        surveyTitleText = findViewById(R.id.survey_title);
        surveyDescriptionText = findViewById(R.id.survey_description);
        rewardText = findViewById(R.id.survey_experience);
        submitButton = findViewById(R.id.submit_button);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        rewardManager = new RewardManager(prefs); // Pasando SharedPreferences al constructor

        String id = getIntent().getStringExtra(EXTRA_SURVEY_ID);
        String title = getIntent().getStringExtra(EXTRA_SURVEY_TITLE);
        String description = getIntent().getStringExtra(EXTRA_SURVEY_DESCRIPTION);
        int experience = getIntent().getIntExtra(EXTRA_SURVEY_EXPERIENCE, 0);
        int requiredLevel = getIntent().getIntExtra(EXTRA_SURVEY_REQUIRED_LEVEL, 1);
        Log.d("SurveyActivity", "Received intent extras: id=" + id + ", title=" + title + ", description=" + description + ", experience=" + experience + ", requiredLevel=" + requiredLevel);
        currentSurvey = new Survey(id, title, description, experience, requiredLevel);

        List<Question> questions = (List<Question>) getIntent().getSerializableExtra(EXTRA_SURVEY_QUESTIONS);
        if (questions != null) {
            Log.d("SurveyActivity", "Received " + questions.size() + " questions from intent.");
            for (Question q : questions) {
                currentSurvey.addQuestion(q);
            }
        } else {
            Log.e("SurveyActivity", "Questions list from intent is null!");
            Toast.makeText(this, R.string.error_could_not_load_survey_questions, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        surveyTitleText.setText(currentSurvey.getTitle());
        surveyDescriptionText.setText(currentSurvey.getDescription());
        // Asegúrate de que R.string.survey_reward exista y acepte dos enteros
        rewardText.setText(getString(R.string.survey_reward,
                currentSurvey.getExperienceReward(),
                currentSurvey.getRequiredLevel()));

        if (currentSurvey.getQuestions() != null && !currentSurvey.getQuestions().isEmpty()) {
            questionsAdapter = new SurveyQuestionsAdapter(currentSurvey.getQuestions());
            questionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            questionsRecyclerView.setAdapter(questionsAdapter);
        } else {
            Log.e("SurveyActivity", "currentSurvey.getQuestions() is null or empty after loading from intent.");
            Toast.makeText(this, R.string.error_failed_to_init_questions_adapter, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        submitButton.setOnClickListener(v -> submitSurvey());
    }

    private List<Answer> collectAnswersFromAdapter() {
        if (questionsAdapter == null || currentSurvey == null || currentSurvey.getQuestions() == null) {
            Log.e("SurveyActivity", "Cannot collect answers: adapter or survey/questions are null.");
            return null;
        }

        List<Answer> collectedAnswers = new ArrayList<>();
        List<Question> questions = currentSurvey.getQuestions();
        List<Object> adapterRawAnswers = questionsAdapter.getAnswers();

        if (questions.size() != adapterRawAnswers.size()) {
            Log.e("SurveyActivity", "Mismatch between questions count (" + questions.size() +
                    ") and raw answers count (" + adapterRawAnswers.size() + ") in adapter.");
            Toast.makeText(this, R.string.error_collecting_answers_data_mismatch, Toast.LENGTH_SHORT).show();
            return null;
        }

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            Object rawAnswerValue = adapterRawAnswers.get(i);

            if (question.getId() == null || question.getText() == null) {
                Log.e("SurveyActivity", "Question ID or Text is null at index " + i + ". Cannot create Answer object.");
                Toast.makeText(this, "Error: Incomplete question data found.", Toast.LENGTH_SHORT).show();
                return null;
            }
            collectedAnswers.add(new Answer(question.getId(), question.getText(), rawAnswerValue));
        }
        return collectedAnswers;
    }

    private void submitSurvey() {
        if (questionsAdapter == null) {
            Toast.makeText(this, R.string.error_questions_not_loaded, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!questionsAdapter.areAllQuestionsAnswered()) {
            Toast.makeText(this, R.string.survey_answer_all, Toast.LENGTH_SHORT).show();
            return;
        }

        // ----- AÑADE LOS LOGS AQUÍ -----
        int currentLevelFromManager = rewardManager.getCurrentLevel();
        // Asegúrate de que currentSurvey y su requiredLevel no sean nulos o tengan valores por defecto si es posible
        int requiredLevelForSurvey = (currentSurvey != null) ? currentSurvey.getRequiredLevel() : Integer.MAX_VALUE; // Valor seguro si currentSurvey es null

        Log.d("SurveyLevelCheck", "Checking survey access:");
        Log.d("SurveyLevelCheck", "Current Level (from RewardManager): " + currentLevelFromManager);
        Log.d("SurveyLevelCheck", "Required Level (for this survey): " + requiredLevelForSurvey);

        if (currentLevelFromManager < requiredLevelForSurvey) {
            Log.d("SurveyLevelCheck", "Access DENIED. currentLevel < requiredLevel is TRUE.");
            Toast.makeText(this,
                    getString(R.string.survey_level_required, requiredLevelForSurvey),
                    Toast.LENGTH_SHORT).show();
            return;
        } else {
            Log.d("SurveyLevelCheck", "Access GRANTED. currentLevel < requiredLevel is FALSE.");
        }
        // ----- FIN DE LOS LOGS AÑADIDOS -----

        Log.d("SurveyActivity", "Initiating survey submission.");

        String userId;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            Log.d("SurveyActivity", "Authenticated user ID: " + userId);
        } else {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("non_authorized_user_id", null);
            if (userId == null) {
                Log.e("SurveyActivity", "User ID not found (non-authenticated).");
                Toast.makeText(this, R.string.error_saving_data, Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d("SurveyActivity", "Non-authenticated user ID: " + userId);
        }

        List<Answer> userAnswers = collectAnswersFromAdapter();
        if (userAnswers == null) {
            Log.e("SurveyActivity", "Failed to collect answers from adapter (returned null).");
            Toast.makeText(this, R.string.error_processing_answers, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.submitting_survey, Toast.LENGTH_SHORT).show();
        submitButton.setEnabled(false);

        if (currentSurvey.getId() == null) {
            Log.e("SurveyActivity", "currentSurvey ID is null. Cannot save completion.");
            Toast.makeText(this, R.string.error_survey_id_missing, Toast.LENGTH_SHORT).show();
            submitButton.setEnabled(true);
            return;
        }

        firestoreService.saveSurveyCompletion(currentSurvey.getId(), userAnswers,
                documentReference -> {
                    Log.d("SurveyActivity", "Survey completion with answers recorded. ID: " + documentReference.getId());
                    updateUserExperienceAndFinalize(userId, currentUser != null);
                },
                e -> {
                    Log.e("SurveyActivity", "Failed to record survey completion with answers.", e);
                    Toast.makeText(SurveyActivity.this, "Failed to submit survey answers: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    submitButton.setEnabled(true);
                });
    }

    private void updateUserExperienceAndFinalize(String userId, boolean isAuthenticatedUser) {
        FirebaseFirestore db = FirebaseFirestore.getInstance(); // Puedes usar la instancia de clase 'this.db' si ya está inicializada
        String collection = isAuthenticatedUser ? "users" : "non_authorized_users";
        Log.d("SurveyActivity", "Updating user XP. Collection: " + collection + ", UserID: " + userId);

        db.collection(collection).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.e("SurveyActivity", "User document for XP update not found. UserID: " + userId);
                        Toast.makeText(this, R.string.error_user_profile_not_found_for_reward, Toast.LENGTH_SHORT).show();
                        finalizeSurveyActivity(false);
                        return;
                    }
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.addExperience(currentSurvey.getExperienceReward());
                        user.incrementSurveys();
                        // Asegúrate de que user.toMap() esté implementado en tu clase User
                        db.collection(collection).document(userId)
                                .set(user.toMap())
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("SurveyActivity", "User data (XP) updated successfully in Firebase.");
                                    rewardManager.addExperience(currentSurvey.getExperienceReward());
                                    Log.d("SurveyActivity", "Local experience awarded: " + currentSurvey.getExperienceReward());
                                    finalizeSurveyActivity(true);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("SurveyActivity", "Error updating user data in Firebase.", e);
                                    Toast.makeText(this, R.string.error_saving_user_data, Toast.LENGTH_SHORT).show();
                                    finalizeSurveyActivity(false);
                                });
                    } else {
                        Log.e("SurveyActivity", "User object is null after Firestore deserialization. UserID: " + userId);
                        Toast.makeText(this, R.string.error_processing_user_profile_for_reward, Toast.LENGTH_SHORT).show();
                        finalizeSurveyActivity(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SurveyActivity", "Error fetching user document for XP update.", e);
                    Toast.makeText(this, R.string.error_fetching_user_data, Toast.LENGTH_SHORT).show();
                    finalizeSurveyActivity(false);
                });
    }

    private void finalizeSurveyActivity(boolean userUpdateSuccessful) {
        if (userUpdateSuccessful) {
            Toast.makeText(this, getString(R.string.survey_completed, currentSurvey.getExperienceReward()), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.survey_completed_user_update_failed, currentSurvey.getExperienceReward()), Toast.LENGTH_LONG).show();
        }
        Intent resultIntent = new Intent();
        resultIntent.putExtra("completed_survey_id", currentSurvey.getId());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // =====================================================================================
    // INNER CLASS: SurveyQuestionsAdapter
    // (El código del adaptador va aquí como lo tenías antes, asegurándote de que areAllQuestionsAnswered() esté presente y sea correcto)
    // =====================================================================================
    private class SurveyQuestionsAdapter extends RecyclerView.Adapter<SurveyQuestionsAdapter.QuestionViewHolder> {
        private List<Question> questions;
        private List<Object> answers;

        public SurveyQuestionsAdapter(List<Question> questions) {
            this.questions = questions != null ? questions : new ArrayList<>();
            this.answers = new ArrayList<>();
            for (int i = 0; i < this.questions.size(); i++) {
                this.answers.add(null);
            }
        }

        public List<Object> getAnswers() {
            return this.answers;
        }

        public boolean areAllQuestionsAnswered() {
            if (questions.size() != answers.size()) {
                Log.w("SurveyQuestionsAdapter", "areAllQuestionsAnswered: Mismatch between questions and answers list sizes.");
                return false;
            }
            for (int i = 0; i < questions.size(); i++) {
                Question question = questions.get(i);
                Object answer = answers.get(i);

                if (question == null) {
                    Log.e("SurveyQuestionsAdapter", "areAllQuestionsAnswered: Question object at index " + i + " is null.");
                    return false;
                }
                if (answer == null) {
                    Log.d("SurveyQuestionsAdapter", "Question '" + question.getText() + "' is unanswered (answer is null).");
                    return false;
                }

                String questionTypeStr = question.getType();
                if (questionTypeStr == null) {
                    Log.w("SurveyQuestionsAdapter", "areAllQuestionsAnswered: Null question type for question: " + question.getText());
                    return false;
                }

                switch (questionTypeStr) {
                    case "RATING":
                        if (!(answer instanceof Float) || ((Float) answer) <= 0f) {
                            Log.d("SurveyQuestionsAdapter", "Question '" + question.getText() + "' (RATING) is unanswered or rating is 0.");
                            return false;
                        }
                        break;
                    case "MULTIPLE_CHOICE":
                        if (!(answer instanceof Integer)) {
                            Log.d("SurveyQuestionsAdapter", "Question '" + question.getText() + "' (MULTIPLE_CHOICE) answer is not an Integer index.");
                            return false;
                        }
                        break;
                    case "TEXT":
                        if (!(answer instanceof String) || ((String) answer).trim().isEmpty()) {
                            Log.d("SurveyQuestionsAdapter", "Question '" + question.getText() + "' (TEXT) is unanswered.");
                            return false;
                        }
                        break;
                    default:
                        Log.w("SurveyQuestionsAdapter", "areAllQuestionsAnswered: Unhandled question type: " + questionTypeStr + " for validation.");
                        return false;
                }
            }
            return true;
        }

        @NonNull
        @Override
        public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_survey_question, parent, false);
            return new QuestionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
            Question question = questions.get(position);
            if (question == null) {
                Log.e("SurveyQuestionsAdapter", "onBindViewHolder: Question at position " + position + " is null.");
                return;
            }
            holder.questionText.setText(question.getText());

            holder.ratingBar.setVisibility(View.GONE);
            holder.radioGroup.setVisibility(View.GONE);
            holder.textInput.setVisibility(View.GONE);

            String questionTypeStr = question.getType();
            if (questionTypeStr == null) {
                Log.e("SurveyQuestionsAdapter", "onBindViewHolder: Question type is null for: " + question.getText());
                return;
            }

            switch (questionTypeStr) {
                case "RATING":
                    holder.ratingBar.setVisibility(View.VISIBLE);
                    Object storedRating = answers.get(position);
                    holder.ratingBar.setRating(storedRating instanceof Float ? (Float) storedRating : 0f);
                    holder.ratingBar.setOnRatingBarChangeListener(null);
                    holder.ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
                        if (fromUser) {
                            int adapterPos = holder.getAdapterPosition();
                            if (adapterPos != RecyclerView.NO_POSITION) {
                                answers.set(adapterPos, rating);
                            }
                        }
                    });
                    break;
                case "MULTIPLE_CHOICE":
                    holder.radioGroup.setVisibility(View.VISIBLE);
                    holder.radioGroup.removeAllViews();
                    List<String> options = question.getOptions();
                    if (options != null) {
                        Object storedChoiceIndex = answers.get(position);
                        holder.radioGroup.setOnCheckedChangeListener(null);
                        for (int i = 0; i < options.size(); i++) {
                            RadioButton radioButton = new RadioButton(SurveyActivity.this);
                            radioButton.setText(options.get(i));
                            radioButton.setId(View.generateViewId());
                            holder.radioGroup.addView(radioButton);
                            if (storedChoiceIndex instanceof Integer && ((Integer) storedChoiceIndex) == i) {
                                radioButton.setChecked(true);
                            }
                        }
                        holder.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                            int adapterPos = holder.getAdapterPosition();
                            if (adapterPos != RecyclerView.NO_POSITION) {
                                for (int i = 0; i < group.getChildCount(); i++) {
                                    RadioButton rb = (RadioButton) group.getChildAt(i);
                                    if (rb.getId() == checkedId) {
                                        answers.set(adapterPos, i);
                                        break;
                                    }
                                }
                            }
                        });
                    }
                    break;
                case "TEXT":
                    holder.textInput.setVisibility(View.VISIBLE);
                    Object storedText = answers.get(position);
                    holder.textInput.setText(storedText instanceof String ? (String) storedText : "");
                    if (holder.textInput.getTag() instanceof android.text.TextWatcher) {
                        holder.textInput.removeTextChangedListener((android.text.TextWatcher) holder.textInput.getTag());
                    }
                    android.text.TextWatcher textWatcher = new android.text.TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            int adapterPos = holder.getAdapterPosition();
                            if (adapterPos != RecyclerView.NO_POSITION) {
                                answers.set(adapterPos, s.toString());
                            }
                        }

                        @Override
                        public void afterTextChanged(android.text.Editable s) {
                        }
                    };
                    holder.textInput.addTextChangedListener(textWatcher);
                    holder.textInput.setTag(textWatcher);
                    break;
                default:
                    Log.w("SurveyQuestionsAdapter", "onBindViewHolder: Unhandled question type: " + questionTypeStr + " for: " + question.getText());
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return questions.size();
        }

        class QuestionViewHolder extends RecyclerView.ViewHolder {
            TextView questionText;
            RatingBar ratingBar;
            RadioGroup radioGroup;
            EditText textInput;

            QuestionViewHolder(View itemView) {
                super(itemView);
                questionText = itemView.findViewById(R.id.question_text);
                ratingBar = itemView.findViewById(R.id.rating_bar);
                radioGroup = itemView.findViewById(R.id.radio_group);
                textInput = itemView.findViewById(R.id.text_input);
            }
        }
    } // End of SurveyQuestionsAdapter inner class
} // End of SurveyActivity class