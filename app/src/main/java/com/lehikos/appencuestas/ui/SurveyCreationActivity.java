package com.lehikos.appencuestas.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.KeyEvent;
import android.app.ProgressDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.DocumentReference;
import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.models.Question;
import com.lehikos.appencuestas.models.Survey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class SurveyCreationActivity extends AppCompatActivity {
    private TextInputEditText titleEdit;
    private TextInputEditText descriptionEdit;
    private TextInputEditText tagsEdit;
    private ChipGroup questionTypeChipGroup;
    private RecyclerView questionsRecyclerView;
    private SwitchMaterial anonymousSwitch;
    private QuestionsAdapter questionsAdapter;
    private List<Question> questions;
    private FirebaseFirestore db;
    private String editingSurveyId = null; // To store the ID if editing an existing survey

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_creation);

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        initializeViews();
        setupQuestionTypeChips();
        setupRecyclerView();

        // Configurar el botón de guardar
        MaterialButton saveButton = findViewById(R.id.save_survey_button);
        saveButton.setOnClickListener(v -> {
            Log.d("SurveyCreation", "Save button clicked");
            saveSurvey();
        });

        // Check if we are editing an existing survey
        if (getIntent().hasExtra("EDIT_SURVEY_ID")) {
            editingSurveyId = getIntent().getStringExtra("EDIT_SURVEY_ID");
            Log.d("SurveyCreation", "Editing existing survey with ID: " + editingSurveyId);
            loadSurveyForEdit(editingSurveyId);
        } else {
            Log.d("SurveyCreation", "Creating a new survey.");
            // Initialize with an empty question list for a new survey
            questions = new ArrayList<>();
            setupRecyclerView();
        }
    }

    private void initializeViews() {
        Log.d("SurveyCreation", "initializeViews: Inicializando vistas...");
        titleEdit = findViewById(R.id.title_edit);
        descriptionEdit = findViewById(R.id.description_edit);
        tagsEdit = findViewById(R.id.tags_edit);
        questionTypeChipGroup = findViewById(R.id.question_type_chip_group);
        if (questionTypeChipGroup == null) {
            Log.e("SurveyCreation", "initializeViews: questionTypeChipGroup es null!");
        } else {
            Log.d("SurveyCreation", "initializeViews: questionTypeChipGroup encontrado.");
        }
        questionsRecyclerView = findViewById(R.id.questions_recycler_view);
        anonymousSwitch = findViewById(R.id.anonymous_switch);
        
        Log.d("SurveyCreation", "initializeViews: Vistas inicializadas.");
    }

    private void setupQuestionTypeChips() {
        // Configurar listeners individuales para cada chip de tipo de pregunta
        Log.d("SurveyCreation", "setupQuestionTypeChips: Configurando listeners individuales para chips.");
        if (questionTypeChipGroup != null) {
            Log.d("SurveyCreation", "setupQuestionTypeChips: questionTypeChipGroup no es nulo.");

            Chip ratingChip = questionTypeChipGroup.findViewById(R.id.rating_chip);
            Chip multipleChip = questionTypeChipGroup.findViewById(R.id.multiple_chip);
            Chip textChip = questionTypeChipGroup.findViewById(R.id.text_chip);

            if (ratingChip != null) {
                ratingChip.setOnClickListener(v -> {
                    Log.d("SurveyCreation", "onClick: Chip Calificación clickeado.");
                    addNewQuestion(Question.QuestionType.RATING);
                    Log.d("SurveyCreation", "onClick: Llamando a addNewQuestion para RATING.");
                    // Opcional: deseleccionar chips si se desea un comportamiento de selección única
                     // questionTypeChipGroup.clearCheck();
                });
            }
            if (multipleChip != null) {
                multipleChip.setOnClickListener(v -> {
                    Log.d("SurveyCreation", "onClick: Chip Opción Múltiple clickeado.");
                    addNewQuestion(Question.QuestionType.MULTIPLE_CHOICE);
                    Log.d("SurveyCreation", "onClick: Llamando a addNewQuestion para MULTIPLE_CHOICE.");
                    // Opcional: deseleccionar chips
                     // questionTypeChipGroup.clearCheck();
                });
            }
            if (textChip != null) {
                textChip.setOnClickListener(v -> {
                    Log.d("SurveyCreation", "onClick: Chip Texto clickeado.");
                    addNewQuestion(Question.QuestionType.TEXT);
                    Log.d("SurveyCreation", "onClick: Llamando a addNewQuestion para TEXT.");
                    // Opcional: deseleccionar chips
                     // questionTypeChipGroup.clearCheck();
                });
            }

            // Remueve el listener anterior del ChipGroup si existía
            questionTypeChipGroup.setOnCheckedChangeListener(null);
            Log.d("SurveyCreation", "setupQuestionTypeChips: Listener de ChipGroup removido.");

        } else {
            Log.e("SurveyCreation", "setupQuestionTypeChips: questionTypeChipGroup es nulo, no se pueden establecer listeners.");
        }
        Log.d("SurveyCreation", "setupQuestionTypeChips: Configuración de listeners terminada.");
    }

    private Question.QuestionType getQuestionTypeFromChip(Chip chip) {
        String text = chip.getText().toString();
        Log.d("SurveyCreation", "getQuestionTypeFromChip: Texto del chip = " + text);
        if (text.equals(getString(R.string.question_type_rating))) {
            Log.d("SurveyCreation", "getQuestionTypeFromChip: Tipo = RATING");
            return Question.QuestionType.RATING;
        } else if (text.equals(getString(R.string.question_type_multiple))) {
            Log.d("SurveyCreation", "getQuestionTypeFromChip: Tipo = MULTIPLE_CHOICE");
            return Question.QuestionType.MULTIPLE_CHOICE;
        } else {
            Log.d("SurveyCreation", "getQuestionTypeFromChip: Tipo = TEXT");
            return Question.QuestionType.TEXT;
        }
    }

    private void setupRecyclerView() {
        // Only initialize the adapter and RecyclerView if questions list is ready
        if (questions != null) {
            questionsAdapter = new QuestionsAdapter(questions);
            questionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            questionsRecyclerView.setAdapter(questionsAdapter);
        } else {
             Log.e("SurveyCreation", "setupRecyclerView: Questions list is null, cannot set up RecyclerView.");
        }
    }

    private void addNewQuestion(Question.QuestionType type) {
        Log.d("SurveyCreation", "addNewQuestion: Añadiendo pregunta de tipo " + type.name());
        Question question = new Question();
        question.setType(type);
        questions.add(question);
        Log.d("SurveyCreation", "addNewQuestion: Tamaño de la lista de preguntas después de añadir = " + questions.size());
        questionsAdapter.notifyItemInserted(questions.size() - 1);
        Log.d("SurveyCreation", "addNewQuestion: notifyItemInserted llamado para posición " + (questions.size() - 1));
    }

    private void saveSurvey() {
        Log.d("SurveyCreation", "saveSurvey: Starting survey save process");
        
        // Mostrar indicador de carga
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Guardando encuesta...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Obtener datos de la UI
        String title = titleEdit.getText().toString().trim();
        String description = descriptionEdit.getText().toString().trim();
        String tagsText = tagsEdit.getText().toString().trim();
        boolean isAnonymous = anonymousSwitch.isChecked();

        Log.d("SurveyCreation", "saveSurvey: Collected UI data - Title: " + title + 
            ", Description: " + description + ", Tags: " + tagsText + 
            ", Anonymous: " + isAnonymous);

        // Validar campos requeridos
        if (title.isEmpty()) {
            Log.w("SurveyCreation", "saveSurvey: Title is empty");
            progressDialog.dismiss();
            Toast.makeText(this, R.string.survey_title_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            Log.w("SurveyCreation", "saveSurvey: Description is empty");
            progressDialog.dismiss();
            Toast.makeText(this, R.string.survey_description_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (questions.isEmpty()) {
            Log.w("SurveyCreation", "saveSurvey: No questions added");
            progressDialog.dismiss();
            Toast.makeText(this, R.string.survey_questions_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (tagsText.isEmpty()) {
            Log.w("SurveyCreation", "saveSurvey: No tags added");
            progressDialog.dismiss();
            Toast.makeText(this, R.string.survey_tags_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // Procesar tags
        List<String> tags = Arrays.asList(tagsText.split(","));
        tags = tags.stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());

        Log.d("SurveyCreation", "saveSurvey: Processed tags: " + tags);

        // Ensure questions list is not null before using it
        if (questions == null) {
            Log.e("SurveyCreation", "saveSurvey: Questions list is null!");
            progressDialog.dismiss();
            Toast.makeText(this, "Error interno: lista de preguntas nula.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear objeto Survey
        Survey survey = new Survey();
        survey.setTitle(title);
        survey.setDescription(description);
        survey.setAnonymous(isAnonymous);
        survey.setTags(tags);
        survey.setQuestions(questions);
        survey.setExperienceReward(50); // Recompensa base por crear una encuesta
        survey.setRequiredLevel(1); // Nivel requerido base

        // Obtener el usuario actual
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId;
        String collectionPath;

        if (currentUser != null) {
            userId = currentUser.getUid();
            collectionPath = "users";
            Log.d("SurveyCreation", "saveSurvey: Authenticated user, ID: " + userId);
        } else {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("non_authorized_user_id", null);
            collectionPath = "non_authorized_users";
            Log.d("SurveyCreation", "saveSurvey: Non-authenticated user, ID: " + userId);
        }

        if (userId == null) {
            Log.e("SurveyCreation", "saveSurvey: No user ID found");
            progressDialog.dismiss();
            Toast.makeText(this, "Error: No se pudo identificar al usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generar un ID único para la encuesta si es nueva, usar el existente si se está editando
        String targetSurveyId = (editingSurveyId != null && !editingSurveyId.isEmpty()) ? editingSurveyId : 
                                db.collection(collectionPath).document(userId).collection("user_surveys").document().getId();
        survey.setId(targetSurveyId);
        Log.d("SurveyCreation", "saveSurvey: Target survey ID: " + targetSurveyId + " (Editing: " + (editingSurveyId != null) + ")");

        DocumentReference surveyRef = db.collection(collectionPath).document(userId)
                                          .collection("user_surveys").document(targetSurveyId);

        surveyRef.set(survey) // Use set with the specific document ID
                 .addOnSuccessListener(aVoid -> {
                     Log.d("SurveyCreation", "saveSurvey: Survey saved/updated successfully in user's subcollection. ID: " + targetSurveyId);
                     progressDialog.dismiss();
                     Toast.makeText(SurveyCreationActivity.this, 
                         (editingSurveyId != null ? "Encuesta actualizada" : getString(R.string.survey_saved)), 
                         Toast.LENGTH_SHORT).show(); // Use localized string for new survey, hardcoded for update for now
                     finish(); // Close the activity after saving
                 })
                 .addOnFailureListener(e -> {
                     Log.e("SurveyCreation", "Error saving/updating survey in user's subcollection", e);
                     progressDialog.dismiss();
                     Toast.makeText(SurveyCreationActivity.this, 
                         (editingSurveyId != null ? "Error al actualizar la encuesta" : getString(R.string.error_saving_survey)), 
                         Toast.LENGTH_SHORT).show(); // Use localized string for new survey, hardcoded for update for now
                 });
    }

    private void loadSurveyForEdit(String surveyId) {
        Log.d("SurveyCreation", "loadSurveyForEdit: Attempting to load survey with ID: " + surveyId);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String collectionPath = (currentUser == null) ? "non_authorized_users" : "users";
        String userId = (currentUser == null) ? getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("non_authorized_user_id", null) : currentUser.getUid();

        if (userId == null) {
            Log.e("SurveyCreation", "loadSurveyForEdit: User ID is null.");
            Toast.makeText(this, "Error: No se pudo cargar la encuesta para editar (usuario no identificado).", Toast.LENGTH_SHORT).show();
            // Initialize with empty state if loading fails
            questions = new ArrayList<>();
            setupRecyclerView();
            return;
        }

        db.collection(collectionPath).document(userId).collection("user_surveys").document(surveyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("SurveyCreation", "loadSurveyForEdit: Survey document found.");
                        Survey survey = documentSnapshot.toObject(Survey.class);
                        if (survey != null) {
                            Log.d("SurveyCreation", "loadSurveyForEdit: Survey object deserialized successfully.");
                            // Populate UI fields
                            titleEdit.setText(survey.getTitle());
                            descriptionEdit.setText(survey.getDescription());
                            // Format tags for the EditText
                            if (survey.getTags() != null && !survey.getTags().isEmpty()) {
                                tagsEdit.setText(String.join(", ", survey.getTags()));
                            }
                            anonymousSwitch.setChecked(survey.isAnonymous());
                            
                            // Populate questions and set up adapter
                            questions = survey.getQuestions() != null ? survey.getQuestions() : new ArrayList<>();
                            setupRecyclerView();
                            
                            Log.d("SurveyCreation", "loadSurveyForEdit: UI populated with survey data.");
                        } else {
                            Log.e("SurveyCreation", "loadSurveyForEdit: Survey object is null after deserialization.");
                            Toast.makeText(this, "Error al cargar la encuesta para editar.", Toast.LENGTH_SHORT).show();
                            // Initialize with empty state if deserialization fails
                            questions = new ArrayList<>();
                            setupRecyclerView();
                        }
                    } else {
                        Log.e("SurveyCreation", "loadSurveyForEdit: Survey document not found.");
                        Toast.makeText(this, "Error: Encuesta no encontrada para editar.", Toast.LENGTH_SHORT).show();
                        // Initialize with empty state if document not found
                        questions = new ArrayList<>();
                        setupRecyclerView();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SurveyCreation", "loadSurveyForEdit: Error fetching survey document.", e);
                    Toast.makeText(this, "Error al cargar la encuesta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Initialize with empty state on error
                    questions = new ArrayList<>();
                    setupRecyclerView();
                });
    }

    private class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.QuestionViewHolder> {
        private List<Question> questions;

        public QuestionsAdapter(List<Question> questions) {
            this.questions = questions;
        }

        @NonNull
        @Override
        public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_question_creation, parent, false);
            view.setVisibility(View.VISIBLE); // Ensure the view is visible
            return new QuestionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
            // Obtener la pregunta actual
            Question question = questions.get(position);

            // Verificar si la pregunta es nula (aunque con notifyItemInserted no debería pasar)
            if (question == null) {
                 Log.e("SurveyCreation", "onBindViewHolder: La pregunta en la posición " + position + " es nula.");
                 return;
            }

            // Configurar el texto de la pregunta
            holder.questionTextEdit.setText(question.getText());
            holder.questionTextEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    // Actualizar el texto de la pregunta en el modelo
                    question.setText(s.toString());
                }
            });

            // Configurar el switch de obligatorio
            holder.mandatorySwitch.setChecked(question.isMandatory());
            holder.mandatorySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Actualizar el estado de obligatorio en el modelo
                question.setMandatory(isChecked);
            });

            // Configurar el contenedor de opciones basado en el tipo de pregunta
            holder.optionsContainer.removeAllViews(); // Limpiar vistas anteriores
            holder.addOptionButton.setVisibility(View.GONE); // Ocultar el botón Añadir Opción por defecto

            if (question.getType() == Question.QuestionType.MULTIPLE_CHOICE) {
                // Si es de opción múltiple, gestionar las opciones
                Log.d("SurveyCreation", "onBindViewHolder: Configurando opciones para pregunta de opción múltiple.");

                // Asegurarse de que la lista de opciones no sea nula
                if (question.getOptions() == null) {
                    question.setOptions(new ArrayList<>());
                }

                // Si no hay opciones existentes, añadir la primera
                if (question.getOptions().isEmpty()) {
                    Log.d("SurveyCreation", "onBindViewHolder: La lista de opciones está vacía, añadiendo la primera.");
                    addOptionInputView(holder, question, ""); // Añadir una opción vacía inicial
                } else {
                    // Añadir vistas para las opciones existentes
                    Log.d("SurveyCreation", "onBindViewHolder: Añadiendo vistas para " + question.getOptions().size() + " opciones existentes.");
                    for (String option : question.getOptions()) {
                        addOptionInputView(holder, question, option);
                    }
                }

            } else {
                // Para otros tipos de pregunta, el contenedor de opciones está oculto (por defecto)
                holder.optionsContainer.setVisibility(View.GONE);
                Log.d("SurveyCreation", "onBindViewHolder: Ocultando contenedor de opciones para tipo de pregunta no múltiple.");
            }
        }

        // Método para añadir una nueva vista de input de opción y configurar listeners
        private void addOptionInputView(QuestionViewHolder holder, Question question, String initialText) {
            // Inflar el layout de un ítem de opción
            View optionView = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.item_option, holder.optionsContainer, false);

            // Encontrar vistas dentro del ítem de opción
            TextInputEditText optionEdit = optionView.findViewById(R.id.option_edit);
            ImageButton removeButton = optionView.findViewById(R.id.remove_option_button);

            // Establecer texto inicial y añadir a la lista de opciones si es nuevo
            optionEdit.setText(initialText);
            if (!question.getOptions().contains(initialText) && initialText != null && !initialText.isEmpty()) {
                 question.getOptions().add(initialText);
                 Log.d("SurveyCreation", "addOptionInputView: Añadida opción inicial '" + initialText + "' al modelo.");
            }

            // Añadir la vista de opción al contenedor
            holder.optionsContainer.addView(optionView);
            Log.d("SurveyCreation", "addOptionInputView: Vista de opción añadida al contenedor.");

            // Configurar TextWatcher para actualizar el modelo mientras se escribe
            optionEdit.addTextChangedListener(new TextWatcher() {
                // Variable para almacenar la posición inicial del texto (antes del cambio)
                private String originalText = "";

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    originalText = s.toString(); // Guardar el texto original
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String newText = s.toString();
                    // Encontrar y actualizar la opción en la lista de la pregunta
                    List<String> options = question.getOptions();
                    int index = options.indexOf(originalText); // Buscar por el texto original
                    if (index != -1) {
                        // Si la opción original se encuentra, actualizarla con el nuevo texto
                        options.set(index, newText);
                         Log.d("SurveyCreation", "afterTextChanged: Opción actualizada de '" + originalText + "' a '" + newText + "'.");
                    } else if (!newText.isEmpty()) {
                         // Si la opción original no se encuentra (ej. es una nueva opción inicial) y el nuevo texto no está vacío,
                         // intentar añadir el nuevo texto si aún no está en la lista
                         if (!options.contains(newText)) {
                             options.add(newText); // Esto puede ocurrir si el TextWatcher se adjunta a una vista nueva
                              Log.d("SurveyCreation", "afterTextChanged: Nueva opción añadida al modelo: '" + newText + "'.");
                         }
                    }
                    originalText = newText; // Actualizar el texto original para el próximo cambio
                }
            });

            // Configurar OnKeyListener para detectar la tecla Enter
            optionEdit.setOnKeyListener((v, keyCode, event) -> {
                // Verificar si la tecla es Enter y la acción es KEY_DOWN
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Prevenir el salto de línea en el EditText
                    Log.d("SurveyCreation", "onKeyListener: Tecla Enter presionada.");
                    // Verificar si es el último EditText en el contenedor
                    int lastChildIndex = holder.optionsContainer.getChildCount() - 1;
                    if (holder.optionsContainer.indexOfChild(optionView) == lastChildIndex) {
                        Log.d("SurveyCreation", "onKeyListener: Es el último EditText.");
                        // Verificar si se pueden añadir más opciones (máximo 4)
                        if (question.getOptions().size() < 4) {
                             Log.d("SurveyCreation", "onKeyListener: Añadiendo nueva opción. Total actual: " + question.getOptions().size());
                            addOptionInputView(holder, question, ""); // Añadir una nueva opción vacía
                             // Solicitar foco al nuevo EditText para facilitar la entrada continua
                             View nextOptionView = holder.optionsContainer.getChildAt(lastChildIndex + 1);
                             if (nextOptionView != null) {
                                 TextInputEditText nextOptionEdit = nextOptionView.findViewById(R.id.option_edit);
                                 if (nextOptionEdit != null) {
                                     nextOptionEdit.requestFocus();
                                     Log.d("SurveyCreation", "onKeyListener: Foco movido al nuevo EditText.");
                                 }
                             }
                        } else {
                            Log.d("SurveyCreation", "onKeyListener: Número máximo de opciones alcanzado.");
                            // Opcional: Mostrar un mensaje al usuario
                            Toast.makeText(holder.itemView.getContext(), "Máximo de 4 opciones alcanzado", Toast.LENGTH_SHORT).show();
                        }
                    }
                    return true; // Indica que el evento fue manejado
                }
                return false; // Permite que otros listeners manejen el evento
            });

            // Configurar listener para el botón de eliminar opción
            removeButton.setOnClickListener(v -> {
                // Obtener el texto actual de la opción antes de eliminar la vista
                String optionTextToRemove = optionEdit.getText().toString();
                Log.d("SurveyCreation", "onClick: Botón eliminar clickeado para opción: '" + optionTextToRemove + "'.");

                // Eliminar la vista del contenedor
                holder.optionsContainer.removeView(optionView);
                Log.d("SurveyCreation", "onClick: Vista de opción eliminada del contenedor.");

                // Eliminar la opción de la lista en el modelo
                question.getOptions().remove(optionTextToRemove);
                 Log.d("SurveyCreation", "onClick: Opción eliminada del modelo. Tamaño de la lista después de eliminar = " + question.getOptions().size());

                // Si se eliminaron todas las opciones, añadir una vacía de nuevo (comportamiento deseado)
                 if (question.getOptions().isEmpty() && question.getType() == Question.QuestionType.MULTIPLE_CHOICE) {
                     Log.d("SurveyCreation", "onClick: Todas las opciones eliminadas, añadiendo una opción vacía inicial.");
                     addOptionInputView(holder, question, "");
                 }
            });

             // Ajustar visibilidad inicial del botón de eliminar
             // Solo mostrar el botón de eliminar si hay más de 1 opción, a menos que sea la única opción y el usuario deba poder eliminarla.
             // Para este caso, permitiremos eliminar incluso la única opción, y si se elimina la última, se añade una vacía.
             // removeButton.setVisibility(holder.optionsContainer.getChildCount() > 1 ? View.VISIBLE : View.GONE);
             removeButton.setVisibility(View.VISIBLE); // Siempre visible para permitir eliminar

             // Si se añade una opción inicial vacía después de eliminar todas, el botón de eliminar estará visible, lo cual es correcto.
        }

        @Override
        public int getItemCount() {
            // Retorna el número de preguntas en la lista
            return questions.size();
        }

        // Clase ViewHolder para los ítems de pregunta
        class QuestionViewHolder extends RecyclerView.ViewHolder {
            // Vistas dentro de un ítem de pregunta
            private final TextInputEditText questionTextEdit;
            private final LinearLayout optionsContainer;
            private final SwitchMaterial mandatorySwitch;
            private final ImageButton closeButton;
            private final View addOptionButton; // Mantener referencia si existe en el layout, aunque lo ocultemos

            QuestionViewHolder(View itemView) {
                super(itemView);
                // Inicializar vistas encontrándolas por su ID
                questionTextEdit = itemView.findViewById(R.id.question_text_edit);
                optionsContainer = itemView.findViewById(R.id.options_container);
                mandatorySwitch = itemView.findViewById(R.id.mandatory_switch);
                closeButton = itemView.findViewById(R.id.close_button);
                addOptionButton = itemView.findViewById(R.id.add_option_button); // Encontrar el botón (aunque se oculte)

                // Configurar listener para el botón de cerrar/eliminar pregunta
                closeButton.setOnClickListener(v -> {
                    // Obtener la posición del ítem en el adaptador
                    int position = getAdapterPosition();
                    // Verificar si la posición es válida
                    if (position != RecyclerView.NO_POSITION) {
                        Log.d("SurveyCreation", "onClick: Botón cerrar clickeado para pregunta en posición " + position);
                        // Eliminar la pregunta de la lista de datos
                        questions.remove(position);
                        // Notificar al adaptador que el ítem ha sido removido
                        notifyItemRemoved(position);
                         // Opcional: notificar que el rango de ítems ha cambiado si es necesario actualizar posiciones visualmente
                         // notifyItemRangeChanged(position, questions.size());
                         Log.d("SurveyCreation", "onClick: Pregunta eliminada. Tamaño de la lista después de eliminar = " + questions.size());
                    }
                });
            }
        }
    }
} 