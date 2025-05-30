package com.lehikos.appencuestas.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.coordinatorlayout.widget.CoordinatorLayout; // Para la Animación de XP
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.lehikos.appencuestas.R;
import com.lehikos.appencuestas.adapters.SurveyAdapter;
import com.lehikos.appencuestas.firebase.FirebaseInitializer; // Del nuevo código
import com.lehikos.appencuestas.models.Question;
import com.lehikos.appencuestas.models.Survey;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.EditText;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class HomeActivity extends BaseActivity {
    private static final String TAG = "HomeActivity"; // Reintroducido para logging consistente
    private static final String APP_PREFS = "AppPrefs";
    private static final String LANGUAGE_KEY = "language";
    private static final String THEME_KEY = "theme";
    private static final String MUSIC_ENABLED_KEY = "music_enabled";
    private static final String SOUND_EFFECTS_ENABLED_KEY = "sound_effects_enabled";
    private static final int REQUEST_CODE_SURVEY = 1; // Del nuevo código
    private static final String TUTORIAL_SHOWN_KEY = "survey_creation_tutorial_shown";

    private RecyclerView recyclerView;
    private SurveyAdapter adapter;
    private List<Survey> surveys;
    private List<String> completedSurveyIds = new ArrayList<>(); // Del nuevo código
    private FloatingActionButton createSurveyFab;
    private FirebaseAuth mAuth; // Reintroducido del original para logout y consistencia

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate iniciado");
        try {
            applySavedTheme();
            Log.d(TAG, "Tema aplicado");
            
            super.onCreate(savedInstanceState);
            Log.d(TAG, "Llamada a super.onCreate");
            
            setSelectedNavigationItem(R.id.navigation_home);
            Log.d(TAG, "Elemento de navegación establecido");

            // Inicializar Firebase Auth (reintroducido del original)
            mAuth = FirebaseAuth.getInstance();

            // Inicializar Firestore y crear colecciones si no existen (del nuevo código)
            FirebaseInitializer.getInstance().initializeCollections();
            Log.d(TAG, "Colecciones de FirebaseInitializer inicializadas");

            // Inicializar FAB
            createSurveyFab = findViewById(R.id.create_survey_fab);
            createSurveyFab.setOnClickListener(v -> handleCreateSurveyClick());

            recyclerView = findViewById(R.id.surveys_recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            Log.d(TAG, "RecyclerView inicializado");

            surveys = new ArrayList<>(); // Inicializado antes de loadSampleSurveys
            // loadSampleSurveys(); // Será llamado por loadCompletedSurveysAndDisplay -> displaySurveys
            // Log.d(TAG, "Encuestas de muestra (iniciales) cargadas");

            adapter = new SurveyAdapter(surveys, survey -> {
                Log.d(TAG, "Encuesta clickeada: ID=" + survey.getId() + ", Título=" + survey.getTitle());
                if (survey.getQuestions() != null) {
                    Log.d(TAG, "La encuesta tiene " + survey.getQuestions().size() + " preguntas.");
                } else {
                    Log.d(TAG, "¡Las preguntas de la encuesta son nulas!");
                }
                Intent intent = new Intent(HomeActivity.this, SurveyActivity.class);
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_ID, survey.getId());
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_TITLE, survey.getTitle());
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_DESCRIPTION, survey.getDescription());
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_EXPERIENCE, survey.getExperienceReward());
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_REQUIRED_LEVEL, survey.getRequiredLevel());
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_QUESTIONS, (Serializable) survey.getQuestions());
                Log.d(TAG, "Lanzando SurveyActivity con extras del intent.");
                startActivityForResult(intent, REQUEST_CODE_SURVEY); // Del nuevo código
            });
            recyclerView.setAdapter(adapter);
            Log.d(TAG, "Adaptador configurado");

            Button settingsButton = findViewById(R.id.settings_button);
            settingsButton.setOnClickListener(v -> showSettingsMenu(v)); // showSettingsMenu ahora incluye logout
            Log.d(TAG, "Botón de configuración configurado");


            // Manejo para usuarios no autorizados y autorizados (del nuevo código)
            if (mAuth.getCurrentUser() == null) { // Usando mAuth aquí
                checkAndCreateNonAuthorizedUser(); // Esto luego llamará a loadCompletedSurveysAndDisplay
            } else {
                loadCompletedSurveysAndDisplay();
            }

            List<Survey> tempSampleSurveys = new ArrayList<>();
            populateSampleSurveys(tempSampleSurveys); // Poblar una lista temporal
            saveSurveysToFirestore(tempSampleSurveys); // Guardar estas en Firestore
            Log.d(TAG, "Encuestas de muestra potencialmente guardadas en Firestore.");

            // Check if username is set, if not, show suggestion dialog
            checkAndPromptUsername();

        } catch (Exception e) {
            Log.e(TAG, "Error en onCreate: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult llamado - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == REQUEST_CODE_SURVEY && resultCode == RESULT_OK && data != null) {
            String completedSurveyId = data.getStringExtra("completed_survey_id");
            Log.d(TAG, "ID de encuesta completada recibido: " + completedSurveyId);

            if (completedSurveyId != null) {
                handleSurveyCompletion(completedSurveyId);
            }
        }
    }

    private void handleSurveyCompletion(String surveyId) {
        Log.d(TAG, "Manejando completación de encuesta para ID: " + surveyId);
        int surveyExperienceReward = 0; // Para almacenar la recompensa de XP de la encuesta completada

        int index = -1;
        for (int i = 0; i < surveys.size(); i++) {
            if (surveys.get(i).getId().equals(surveyId)) {
                index = i;
                surveyExperienceReward = surveys.get(i).getExperienceReward(); // Obtener XP antes de remover
                break;
            }
        }
        if (index != -1) {
            Log.d(TAG, "Removiendo encuesta en el índice: " + index);
            surveys.remove(index);
            adapter.notifyItemRemoved(index);
            adapter.notifyItemRangeChanged(index, surveys.size()); // Para actualizar las posiciones de los ítems subsecuentes

            // Mostrar animación de XP con el XP real de la encuesta
            showXpDropAnimation(surveyExperienceReward);
            Log.d(TAG, "Animación de caída de XP iniciada con " + surveyExperienceReward + " XP.");

            saveCompletedSurveyToFirebase(surveyId);
        } else {
            Log.e(TAG, "ID de encuesta no encontrado en la lista actual: " + surveyId + ". Podría haber sido ya removida o la lista actualizada.");
            // Es posible que la lista se haya refrescado; verificar si está en completedSurveyIds
            if (!completedSurveyIds.contains(surveyId)) {
                saveCompletedSurveyToFirebase(surveyId); // Guardar de todas formas si no está marcada como completada
            }
        }
    }

    private void showXpDropAnimation(int experiencePoints) {
        Log.d(TAG, "Creando animación de caída de XP para " + experiencePoints + " XP");
        final TextView xpDrop = new TextView(this);
        xpDrop.setText("+" + experiencePoints + " XP"); // XP dinámico
        xpDrop.setTextSize(32); // Considerar hacer esto un recurso de dimensión
        xpDrop.setTextColor(getResources().getColor(R.color.primary_color)); // Asegurar que este color exista
        xpDrop.setGravity(Gravity.CENTER);

        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.topMargin = (int) (getResources().getDisplayMetrics().density * 70); // Aproximadamente 70dp desde arriba

        CoordinatorLayout layout = findViewById(R.id.coordinator_layout); // Asegurar que este ID exista en tu activity_home.xml
        if (layout != null) {
            Log.d(TAG, "Añadiendo vista de XP al CoordinatorLayout");
            layout.addView(xpDrop, params);

            xpDrop.setAlpha(1f);
            xpDrop.setTranslationY(0f);

            Log.d(TAG, "Iniciando animación de XP");
            xpDrop.animate()
                    .translationY(getResources().getDisplayMetrics().density * 250) // Animar hacia abajo por 250dp
                    .alpha(0f)
                    .setDuration(1500) // 1.5 segundos
                    .withEndAction(() -> {
                        layout.removeView(xpDrop);
                        Log.d(TAG, "Animación de XP completada y vista removida");
                    })
                    .start();
        } else {
            Log.e(TAG, "CoordinatorLayout con ID R.id.coordinator_layout no encontrado. La animación de XP no se puede mostrar.");
        }
    }

    private void saveCompletedSurveyToFirebase(String surveyId) {
        Log.d(TAG, "Guardando encuesta completada en Firebase: " + surveyId);
        if (surveyId == null || surveyId.isEmpty()) {
            Log.e(TAG, "El ID de la encuesta es nulo o vacío. No se puede guardar la encuesta completada.");
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId;
        String collectionPath;

        if (currentUser != null) {
            userId = currentUser.getUid();
            collectionPath = "users";
        } else {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("non_authorized_user_id", null);
            collectionPath = "non_authorized_users";
        }

        if (userId == null) {
            Log.e(TAG, "El ID de usuario es nulo. No se puede guardar la encuesta completada.");
            return;
        }

        // Añadir a la lista local primero para prevenir que se muestre de nuevo si la llamada a Firebase es lenta
        if (!completedSurveyIds.contains(surveyId)) {
            completedSurveyIds.add(surveyId);
            
            // Guardar en SharedPreferences inmediatamente
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String json = new Gson().toJson(completedSurveyIds);
            prefs.edit().putString("completed_surveys", json).apply();
            
            Log.d(TAG, "Encuesta " + surveyId + " añadida a la lista local y guardada en SharedPreferences");
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(collectionPath).document(userId)
                .update("completed_surveys", FieldValue.arrayUnion(surveyId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Encuesta " + surveyId + " marcada como completada para el usuario " + userId);
                    // Actualizar la vista para reflejar el cambio
                    displaySurveys();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al marcar la encuesta " + surveyId + " como completada para el usuario " + userId, e);
                    // Si falla Firebase, al menos tenemos la copia local
                });
    }


    private void populateSampleSurveys(List<Survey> surveyListToPopulate) {
        surveyListToPopulate.clear();

        // Encuesta de Muestra: Satisfacción del Usuario
        Survey satisfactionSurvey = new Survey("1", "Encuesta de Satisfacción",
                "Ayúdanos a mejorar nuestros servicios", 100, 1);
        satisfactionSurvey.addQuestion(new Question("q1_satis",
                "¿Qué te parece la interfaz de la aplicación?",
                Question.QuestionType.RATING));
        satisfactionSurvey.addQuestion(new Question("q2_satis",
                "¿Con qué frecuencia usas la aplicación?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Diariamente", "Semanalmente", "Mensualmente", "Raramente")));
        satisfactionSurvey.addQuestion(new Question("q3_satis",
                "¿Qué características te gustaría ver en futuras actualizaciones?",
                Question.QuestionType.TEXT));
        surveyListToPopulate.add(satisfactionSurvey);

        // Encuesta de Muestra: Preferencias de Producto
        Survey productSurvey = new Survey("2", "Encuesta de Productos",
                "Cuéntanos sobre tus productos favoritos", 150, 2);
        productSurvey.addQuestion(new Question("q1_prod",
                "¿Qué tipo de productos prefieres?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Tecnología", "Ropa", "Alimentos", "Hogar", "Otros")));
        productSurvey.addQuestion(new Question("q2_prod",
                "¿Cuánto estás dispuesto a gastar en tus compras mensuales?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Menos de $100", "$100-$500", "$500-$1000", "Más de $1000")));
        surveyListToPopulate.add(productSurvey);

        // Encuesta de Muestra: Experiencia del Usuario
        Survey experienceSurvey = new Survey("3", "Encuesta de Experiencia",
                "Comparte tu experiencia con nosotros", 200, 3);
        experienceSurvey.addQuestion(new Question("q1_exp",
                "¿Qué te motivó a usar nuestra aplicación?",
                Question.QuestionType.TEXT));
        experienceSurvey.addQuestion(new Question("q2_exp",
                "¿Qué tan satisfecho estás con el sistema de recompensas?",
                Question.QuestionType.RATING));
        experienceSurvey.addQuestion(new Question("q3_exp",
                "¿Recomendarías la aplicación a otros?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Definitivamente sí", "Probablemente sí", "No estoy seguro", "Probablemente no", "Definitivamente no")));
        surveyListToPopulate.add(experienceSurvey);

        // NUEVAS ENCUESTAS

        // Encuesta 4: Estilo de Vida
        Survey lifestyleSurvey = new Survey("4", "Encuesta de Estilo de Vida",
                "Conoce tus hábitos y rutinas diarias", 120, 4);
        lifestyleSurvey.addQuestion(new Question("q1_life",
                "¿A qué hora sueles despertarte entre semana?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Antes de las 6 a.m.", "6–8 a.m.", "8–10 a.m.", "Después de las 10 a.m.")));
        lifestyleSurvey.addQuestion(new Question("q2_life",
                "¿Cuántas veces haces ejercicio a la semana?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("0", "1–2", "3–5", "Más de 5")));
        lifestyleSurvey.addQuestion(new Question("q3_life",
                "Describe brevemente tu rutina matutina.",
                Question.QuestionType.TEXT));
        lifestyleSurvey.addQuestion(new Question("q4_life",
                "¿Con qué frecuencia cocinas en casa?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Nunca", "Ocasionalmente", "Frecuentemente", "Siempre")));
        lifestyleSurvey.addQuestion(new Question("q5_life",
                "¿Cuál es tu nivel de estrés diario?",
                Question.QuestionType.RATING));
        surveyListToPopulate.add(lifestyleSurvey);

        // Encuesta 5: Educación
        Survey educationSurvey = new Survey("5", "Encuesta Educativa",
                "Dinos más sobre tu formación y aprendizaje", 130, 5);
        educationSurvey.addQuestion(new Question("q1_edu",
                "¿Cuál es tu nivel educativo más alto?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Primaria", "Secundaria", "Universidad", "Posgrado")));
        educationSurvey.addQuestion(new Question("q2_edu",
                "¿Con qué frecuencia tomas cursos online?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Nunca", "Ocasionalmente", "Regularmente", "Frecuentemente")));
        educationSurvey.addQuestion(new Question("q3_edu",
                "¿Qué plataforma usas más para estudiar?",
                Question.QuestionType.TEXT));
        educationSurvey.addQuestion(new Question("q4_edu",
                "¿Te gustaría recibir notificaciones sobre nuevos cursos?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Sí", "No")));
        educationSurvey.addQuestion(new Question("q5_edu",
                "Evalúa tu motivación para aprender nuevas habilidades.",
                Question.QuestionType.RATING));
        surveyListToPopulate.add(educationSurvey);

        // Encuesta 6: Medio Ambiente
        Survey ecoSurvey = new Survey("6", "Encuesta Ecológica",
                "Queremos saber tus hábitos sostenibles", 140, 6);
        ecoSurvey.addQuestion(new Question("q1_eco",
                "¿Reciclas regularmente en tu hogar?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Sí", "A veces", "No")));
        ecoSurvey.addQuestion(new Question("q2_eco",
                "¿Utilizas transporte público o compartido?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Diariamente", "Semanalmente", "Raramente", "Nunca")));
        ecoSurvey.addQuestion(new Question("q3_eco",
                "¿Qué harías para mejorar tu impacto ambiental?",
                Question.QuestionType.TEXT));
        ecoSurvey.addQuestion(new Question("q4_eco",
                "¿Con qué frecuencia compras productos ecológicos?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Siempre", "A menudo", "Ocasionalmente", "Nunca")));
        ecoSurvey.addQuestion(new Question("q5_eco",
                "Evalúa tu compromiso con el medio ambiente.",
                Question.QuestionType.RATING));
        surveyListToPopulate.add(ecoSurvey);

        // Encuesta 7: Entretenimiento
        Survey entertainmentSurvey = new Survey("7", "Encuesta de Entretenimiento",
                "Queremos conocer tus gustos en ocio", 125, 7);
        entertainmentSurvey.addQuestion(new Question("q1_ent",
                "¿Cuál es tu género de películas favorito?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Acción", "Comedia", "Drama", "Ciencia Ficción", "Terror")));
        entertainmentSurvey.addQuestion(new Question("q2_ent",
                "¿Qué plataformas de streaming utilizas?",
                Question.QuestionType.TEXT));
        entertainmentSurvey.addQuestion(new Question("q3_ent",
                "¿Cuántas horas a la semana dedicas al entretenimiento digital?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Menos de 5", "5–10", "10–20", "Más de 20")));
        entertainmentSurvey.addQuestion(new Question("q4_ent",
                "¿Cuál fue tu película o serie favorita del último año?",
                Question.QuestionType.TEXT));
        entertainmentSurvey.addQuestion(new Question("q5_ent",
                "Evalúa la calidad del contenido en las plataformas actuales.",
                Question.QuestionType.RATING));
        surveyListToPopulate.add(entertainmentSurvey);

        // Encuesta 8: Hábitos de Sueño
        Survey sleepSurvey = new Survey("8", "Encuesta de Sueño",
                "Cuéntanos sobre tus hábitos de descanso", 110, 8);
        sleepSurvey.addQuestion(new Question("q1_sleep",
                "¿A qué hora te acuestas habitualmente?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Antes de las 10 p.m.", "10–12 p.m.", "Después de las 12 a.m.")));
        sleepSurvey.addQuestion(new Question("q2_sleep",
                "¿Cuántas horas duermes por noche?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Menos de 5", "5–7", "7–9", "Más de 9")));
        sleepSurvey.addQuestion(new Question("q3_sleep",
                "¿Tienes dificultades para dormir?",
                Question.QuestionType.MULTIPLE_CHOICE,
                Arrays.asList("Frecuentemente", "A veces", "Rara vez", "Nunca")));
        sleepSurvey.addQuestion(new Question("q4_sleep",
                "¿Qué haces para relajarte antes de dormir?",
                Question.QuestionType.TEXT));
        sleepSurvey.addQuestion(new Question("q5_sleep",
                "Evalúa la calidad de tu sueño en general.",
                Question.QuestionType.RATING));
        surveyListToPopulate.add(sleepSurvey);

        Log.d(TAG, "Encuestas de muestra pobladas. Cantidad: " + surveyListToPopulate.size());
    }


    // Este método ahora toma una lista de encuestas para guardar.
    private void saveSurveysToFirestore(List<Survey> surveysToSave) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Intentando guardar " + surveysToSave.size() + " encuestas en Firestore.");
        for (Survey survey : surveysToSave) {
            Map<String, Object> surveyMap = new HashMap<>();
            surveyMap.put("id", survey.getId());
            surveyMap.put("title", survey.getTitle());
            surveyMap.put("description", survey.getDescription());
            surveyMap.put("experienceReward", survey.getExperienceReward());
            surveyMap.put("requiredLevel", survey.getRequiredLevel());

            List<Map<String, Object>> questionsList = new ArrayList<>();
            if (survey.getQuestions() != null) {
                for (com.lehikos.appencuestas.models.Question q : survey.getQuestions()) {
                    Map<String, Object> qMap = new HashMap<>();
                    qMap.put("id", q.getId());
                    qMap.put("text", q.getText());
                    qMap.put("type", q.getType().toString());
                    qMap.put("options", q.getOptions());
                    questionsList.add(qMap);
                }
            }
            surveyMap.put("questions", questionsList);

            // Verificación re-añadida del código original para robustez
            if (survey.getId() != null && !survey.getId().isEmpty()) {
                db.collection("surveys").document(survey.getId()).set(surveyMap)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Encuesta " + survey.getId() + " escrita exitosamente en Firestore."))
                        .addOnFailureListener(e -> Log.w(TAG, "Error al escribir la encuesta " + survey.getId() + " en Firestore.", e));
            } else {
                Log.w(TAG, "El ID de la encuesta es nulo o vacío, no se puede guardar en Firestore. Título: " + survey.getTitle());
            }
        }
    }

    private void checkAndCreateNonAuthorizedUser() {
        Log.d(TAG, "Verificando usuario no autorizado.");
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String existingUserId = prefs.getString("non_authorized_user_id", null);

        if (existingUserId != null) {
            FirebaseFirestore.getInstance().collection("non_authorized_users").document(existingUserId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            Log.d(TAG, "Usuario no autorizado existente encontrado en SharedPreferences y Firestore: " + existingUserId);
                            loadCompletedSurveysAndDisplay(); // El usuario existe, cargar sus datos
                        } else {
                            Log.d(TAG, "ID de usuario en SharedPreferences no encontrado en Firestore o error. Generando nuevo usuario no autorizado.");
                            prefs.edit().remove("non_authorized_user_id").apply(); // Limpiar ID inválido
                            generateAndSaveNonAuthorizedUser();
                        }
                    });
        } else {
            Log.d(TAG, "No existe ID de usuario no autorizado en SharedPreferences. Generando uno nuevo.");
            generateAndSaveNonAuthorizedUser();
        }
    }

    private void generateAndSaveNonAuthorizedUser() {
        Log.d(TAG, "Generando y guardando nuevo usuario no autorizado.");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Simplificado: Generar un ID único localmente para el documento, el nombre de usuario puede ser más simple o también derivado.
        // Para la generación de nombre de usuario como antes:
        db.collection("non_authorized_users")
                .orderBy("username", Query.Direction.DESCENDING).limit(1)
            .get()
            .addOnCompleteListener(task -> {
                    String newUsername;
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                    DocumentSnapshot lastUser = task.getResult().getDocuments().get(0);
                    String lastUsername = lastUser.getString("username");
                        newUsername = generateNextUsername(lastUsername);
                    } else {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Error al obtener el último nombre de usuario para usuario no autorizado.", task.getException());
                        }
                        newUsername = "user1000a"; // Predeterminado si no hay usuarios o error
                    }
                    saveNonAuthorizedUser(newUsername);
            });
    }

    private String generateNextUsername(String lastUsername) {
        if (lastUsername == null || !lastUsername.startsWith("user") || lastUsername.length() <= 5) {
            // Manejar lastUsername malformado o proveer un punto de inicio por defecto
            Log.w(TAG, "Formato de lastUsername inválido: " + lastUsername + ". Reiniciando a lógica de user1000a.");
            return "user1000a"; // O algún otro valor por defecto
        }
        try {
            // Extrae la parte numérica como "1000" de "user1000a"
            String numericPart = lastUsername.substring(4, lastUsername.length() - 1);
            int number = Integer.parseInt(numericPart);
        char letter = lastUsername.charAt(lastUsername.length() - 1);

        if (letter == 'z') {
            number++;
            letter = 'a';
        } else {
            letter++;
        }
        return "user" + number + letter;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            Log.e(TAG, "Error al parsear lastUsername: " + lastUsername, e);
            // Fallback si el parseo falla (ej. formato inesperado)
            // Podría ser un nuevo ID aleatorio o el inicio de una secuencia.
            // Por simplicidad, se devuelve un inicio fijo, pero podría necesitarse una generación de ID único más robusta.
            return "user" + (System.currentTimeMillis() % 10000) + "a"; // Ejemplo de fallback
        }
    }


    private void saveNonAuthorizedUser(String username) {
        Log.d(TAG, "Intentando guardar usuario no autorizado: " + username);
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        // El ID del documento será el nombre de usuario por simplicidad aquí, según la lógica aparente del nuevo código
        String userIdToSave = username;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("experience", 0);
        user.put("level", 1);
        user.put("totalSurveys", 0);
        user.put("completed_surveys", new ArrayList<String>()); // Inicializar lista vacía

        db.collection("non_authorized_users").document(userIdToSave).set(user)
            .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Usuario no autorizado guardado en Firestore con ID/Username: " + userIdToSave);
                    prefs.edit().putString("non_authorized_user_id", userIdToSave).apply();
                    Log.d(TAG, "ID de usuario no autorizado guardado en SharedPreferences: " + userIdToSave);
                    loadCompletedSurveysAndDisplay(); // Ahora cargar encuestas para este nuevo usuario
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar usuario no autorizado " + userIdToSave + " en Firestore.", e);
                    // Manejar fallo: quizás intentar generar un nombre de usuario diferente o notificar al usuario.
                });
    }


    private void loadCompletedSurveysAndDisplay() {
        Log.d(TAG, "Cargando encuestas completadas y luego mostrando.");
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId;
        String collectionPath;

        if (currentUser != null) {
            userId = currentUser.getUid();
            collectionPath = "users";
        } else {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("non_authorized_user_id", null);
            collectionPath = "non_authorized_users";
        }

        if (userId == null) {
            Log.e(TAG, "No se encontró ID de usuario (autenticado o no autenticado). No se pueden cargar las encuestas completadas.");
            this.completedSurveyIds = new ArrayList<>(); // Asegurar que esté vacía
            displaySurveys(); // Mostrar todas las encuestas de muestra
            return;
        }

        // Primero intentar cargar desde SharedPreferences como caché
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String completedSurveysJson = prefs.getString("completed_surveys", null);
        if (completedSurveysJson != null) {
            try {
                Type type = new TypeToken<List<String>>(){}.getType();
                List<String> cachedCompleted = new Gson().fromJson(completedSurveysJson, type);
                if (cachedCompleted != null) {
                    this.completedSurveyIds = new ArrayList<>(cachedCompleted);
                    Log.d(TAG, "Cargadas " + completedSurveyIds.size() + " IDs de encuestas completadas desde caché");
                    displaySurveys(); // Mostrar encuestas filtradas inmediatamente
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al cargar encuestas completadas desde caché", e);
            }
        }

        // Luego actualizar desde Firebase
        FirebaseFirestore.getInstance().collection(collectionPath).document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        List<String> completed = (List<String>) documentSnapshot.get("completed_surveys");
                        if (completed != null) {
                            this.completedSurveyIds = new ArrayList<>(completed); // Crear nueva lista
                            Log.d(TAG, "Cargadas " + completedSurveyIds.size() + " IDs de encuestas completadas desde Firebase para el usuario: " + userId);
                            
                            // Guardar en caché
                            String json = new Gson().toJson(completedSurveyIds);
                            prefs.edit().putString("completed_surveys", json).apply();
                            
                            displaySurveys(); // Actualizar la vista con las encuestas filtradas
                        } else {
                            this.completedSurveyIds = new ArrayList<>();
                            Log.d(TAG, "No se encontró el campo 'completed_surveys' para el usuario: " + userId + ". Inicializando como vacía.");
                            displaySurveys();
                        }
                    } else {
                        this.completedSurveyIds = new ArrayList<>();
                        Log.d(TAG, "No se encontró documento para el usuario: " + userId + " en " + collectionPath + ". Inicializando encuestas completadas como vacía.");
                    displaySurveys();
                    }
            })
            .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al cargar encuestas completadas para el usuario: " + userId, e);
                    this.completedSurveyIds = new ArrayList<>();
                    displaySurveys(); // Mostrar todas las encuestas de muestra en caso de fallo
                });
    }

    private void displaySurveys() {
        Log.d(TAG, "Preparando para mostrar encuestas.");
        List<Survey> allSampleSurveys = new ArrayList<>();
        populateSampleSurveys(allSampleSurveys); // Obtener todas las encuestas de muestra base
        Log.d(TAG, "Total de encuestas de muestra pobladas: " + allSampleSurveys.size());
        Log.d(TAG, "IDs de encuestas completadas: " + completedSurveyIds.toString());


        // Filtrar encuestas completadas
        List<Survey> filteredSurveys = new ArrayList<>();
        if (completedSurveyIds != null && !completedSurveyIds.isEmpty()) {
            for (Survey s : allSampleSurveys) {
                if (!completedSurveyIds.contains(s.getId())) {
                    filteredSurveys.add(s);
                } else {
                    Log.d(TAG, "La encuesta " + s.getId() + " está completada, filtrando.");
                }
            }
        } else {
            filteredSurveys.addAll(allSampleSurveys); // No hay encuestas completadas, mostrar todas
        }
        Log.d(TAG, "Encuestas para mostrar después de filtrar: " + filteredSurveys.size());

        this.surveys.clear();
        this.surveys.addAll(filteredSurveys);

        if (adapter != null) {
            adapter.updateSurveys(this.surveys); // Usar la lista principal 'surveys'
            Log.d(TAG, "Adaptador actualizado con encuestas filtradas.");
        } else {
            // Este caso idealmente no debería suceder si el adaptador se inicializa en onCreate
            Log.w(TAG, "El adaptador era nulo en displaySurveys. Reinicializando.");
            adapter = new SurveyAdapter(this.surveys, survey -> {
                // Mismo listener de click que en onCreate
                Intent intent = new Intent(HomeActivity.this, SurveyActivity.class);
                // ... (poblar extras del intent) ...
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_ID, survey.getId());
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_TITLE, survey.getTitle());
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_DESCRIPTION, survey.getDescription());
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_EXPERIENCE, survey.getExperienceReward());
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_REQUIRED_LEVEL, survey.getRequiredLevel());
                intent.putExtra(SurveyActivity.EXTRA_SURVEY_QUESTIONS, (Serializable) survey.getQuestions());
                startActivityForResult(intent, REQUEST_CODE_SURVEY);
            });
            recyclerView.setAdapter(adapter);
        }
    }


    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_home;
    }

    // --- Menú de Configuración Actualizado (incluye Logout) ---
    protected void showSettingsMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenu().add(getString(R.string.settings_languages)); // Ítem 0
        popupMenu.getMenu().add(getString(R.string.settings_themes));    // Ítem 1
        popupMenu.getMenu().add(getString(R.string.settings_sound));     // Ítem 2

        popupMenu.setOnMenuItemClickListener(item -> {
            String selectedItemTitle = item.getTitle().toString();

            if (selectedItemTitle.equals(getString(R.string.settings_languages))) {
                showSubMenu(v, selectedItemTitle);
                return true;
            } else if (selectedItemTitle.equals(getString(R.string.settings_themes))) {
                showSubMenu(v, selectedItemTitle);
                return true;
            } else if (selectedItemTitle.equals(getString(R.string.settings_sound))) {
                showSoundSettingsMenu(v);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showSoundSettingsMenu(View anchorView) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
        boolean musicEnabled = prefs.getBoolean(MUSIC_ENABLED_KEY, true);
        boolean soundEffectsEnabled = prefs.getBoolean(SOUND_EFFECTS_ENABLED_KEY, true);

        // Add music toggle
        popupMenu.getMenu().add(getString(R.string.settings_music))
            .setCheckable(true)
            .setChecked(musicEnabled);

        // Add sound effects toggle
        popupMenu.getMenu().add(getString(R.string.settings_sound_effects))
            .setCheckable(true)
            .setChecked(soundEffectsEnabled);

        popupMenu.setOnMenuItemClickListener(item -> {
            String selectedItemTitle = item.getTitle().toString();
            SharedPreferences.Editor editor = prefs.edit();

            if (selectedItemTitle.equals(getString(R.string.settings_music))) {
                boolean newState = !item.isChecked();
                editor.putBoolean(MUSIC_ENABLED_KEY, newState);
                item.setChecked(newState);
                // Notify RewardsActivity about music state change
                Intent intent = new Intent("com.lehikos.appencuestas.MUSIC_STATE_CHANGED");
                intent.putExtra("music_enabled", newState);
                sendBroadcast(intent);
            } else if (selectedItemTitle.equals(getString(R.string.settings_sound_effects))) {
                boolean newState = !item.isChecked();
                editor.putBoolean(SOUND_EFFECTS_ENABLED_KEY, newState);
                item.setChecked(newState);
            }

            editor.apply();
            return true;
        });

        popupMenu.show();
    }

    // --- Método Logout (Re-añadido del original) ---
    private void logoutUser() {
        Log.d(TAG, "Logout iniciado.");
        if (mAuth != null) {
            mAuth.signOut();
        }
        // También limpiar la preferencia de usuario no autorizado si existe, para asegurar un prompt de login fresco
        SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userPrefs.edit().remove("non_authorized_user_id").apply();
        Log.d(TAG, "non_authorized_user_id limpiado de SharedPreferences.");


        Intent intent = new Intent(HomeActivity.this, LoginActivity.class); // Asegurar que LoginActivity esté correctamente importado
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Log.d(TAG, "Usuario deslogueado. Navegando a LoginActivity.");
    }

    protected void showSubMenu(View anchorView, String category) {
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        if (category.equals(getString(R.string.settings_languages))) {
            popupMenu.getMenu().add(getString(R.string.settings_language_english)); // Inglés
            popupMenu.getMenu().add(getString(R.string.settings_language_spanish)); // Español
            popupMenu.setOnMenuItemClickListener(item -> {
                handleLanguageSelection(item.getTitle().toString());
                return true;
            });
        } else if (category.equals(getString(R.string.settings_themes))) {
            popupMenu.getMenu().add(getString(R.string.settings_theme_light)); // Claro
            popupMenu.getMenu().add(getString(R.string.settings_theme_dark));  // Oscuro
            popupMenu.setOnMenuItemClickListener(item -> {
                handleThemeSelection(item.getTitle().toString());
                return true;
            });
        }
        popupMenu.show();
    }

    private void handleLanguageSelection(String selectedLanguage) {
        Log.d(TAG, "Manejando selección de idioma: " + selectedLanguage);
        String languageCode = selectedLanguage.equals(getString(R.string.settings_language_english)) ? "en" : "es";
        setLocale(languageCode);
        // Reiniciar actividad para aplicar cambios
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleThemeSelection(String selectedTheme) {
        Log.d(TAG, "Manejando selección de tema: " + selectedTheme);
        String themeToSet = selectedTheme.equals(getString(R.string.settings_theme_dark)) ? "dark" : "light";
        setThemePreference(themeToSet); // Renombrado para claridad
        // Reiniciar actividad para aplicar cambios
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLocale(String languageCode) {
        Log.d(TAG, "Estableciendo locale a: " + languageCode);
        try {
            LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
            AppCompatDelegate.setApplicationLocales(appLocale);
            SharedPreferences.Editor editor = getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit();
            editor.putString(LANGUAGE_KEY, languageCode);
            editor.apply();
            Log.d(TAG, "Locale establecido y preferencia guardada: " + languageCode);
        } catch (Exception e) {
            Log.e(TAG, "Error al establecer locale: " + e.getMessage(), e);
        }
    }

    // Renombrado de setTheme a setThemePreference para evitar conflicto con Activity.setTheme()
    private void setThemePreference(String themeName) {
        Log.d(TAG, "Estableciendo preferencia de tema a: " + themeName);
        try {
            // La aplicación del tema es manejada por applySavedTheme en create
            SharedPreferences.Editor editor = getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit();
            editor.putString(THEME_KEY, themeName);
            editor.apply();
            Log.d(TAG, "Preferencia de tema guardada: " + themeName);

            // Aplicar inmediatamente para el próximo lanzamiento (AppCompatDelegate.setDefaultNightMode es lo que hace applySavedTheme)
            int nightMode = themeName.equals("dark") ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(nightMode);

        } catch (Exception e) {
            Log.e(TAG, "Error al establecer preferencia de tema: " + e.getMessage(), e);
        }
    }

    private void applySavedTheme() {
        Log.d(TAG, "Aplicando tema guardado.");
        try {
            SharedPreferences prefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
            String theme = prefs.getString(THEME_KEY, "light"); // Predeterminado a claro
            Log.d(TAG, "Tema guardado recuperado: " + theme);
            int nightMode = theme.equals("dark") ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(nightMode);
            Log.d(TAG, "Modo noche aplicado: " + nightMode);
        } catch (Exception e) {
            Log.e(TAG, "Error al aplicar tema guardado: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume llamado.");
        // Verificar si el usuario sigue siendo válido o si el usuario no autenticado necesita ser reestablecido
        // Por ejemplo, si los datos se borraron mientras la app estaba en segundo plano.
        // La verificación en onCreate usualmente maneja esto para inicios de actividad.
        // Si las encuestas no se muestran correctamente, podría ser necesario llamar a loadCompletedSurveysAndDisplay() aquí también.
        // Sin embargo, tener cuidado con cargas múltiples.
        setSelectedNavigationItem(R.id.navigation_home);
        updateCreateSurveyFabVisibility();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy llamado");
        super.onDestroy();
    }

    private void checkAndPromptUsername() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId;
        String collection;

        if (currentUser != null) {
            userId = currentUser.getUid();
            collection = "users";
        } else {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("non_authorized_user_id", null);
            collection = "non_authorized_users";
        }

        if (userId == null) {
            Log.e(TAG, "No user ID found, cannot check username");
            return;
        }

        // First check Firebase
        FirebaseFirestore.getInstance().collection(collection).document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                String username = documentSnapshot.getString("username");
                if (username == null || username.isEmpty()) {
                    // No username in Firebase, show prompt
                    showUsernameSuggestionDialog();
                } else {
                    // Username exists in Firebase, save to SharedPreferences as backup
                    SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    prefs.edit().putString("user_username", username).apply();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking username in Firebase", e);
                // On Firebase error, check SharedPreferences as fallback
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String localUsername = prefs.getString("user_username", null);
                if (localUsername == null || localUsername.isEmpty()) {
                    showUsernameSuggestionDialog();
                }
            });
    }

    private void showUsernameSuggestionDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_set_username, null);
        EditText editText = dialogView.findViewById(R.id.editTextUsername);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.confirm_username_title)
            .setView(dialogView)
            .setPositiveButton(R.string.yes, (d, which) -> {
                String enteredUsername = editText.getText().toString().trim();
                if (!enteredUsername.isEmpty()) {
                    confirmUsernameDialog(enteredUsername);
                }
            })
            .setNegativeButton(R.string.no, (d, which) -> d.dismiss())
            .setNeutralButton(R.string.skip, (d, which) -> d.dismiss())
            .show();
    }

    private void confirmUsernameDialog(String username) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm_username_title)
            .setMessage(getString(R.string.confirm_username_message, username))
            .setPositiveButton(R.string.yes, (d, which) -> saveUsername(username))
            .setNegativeButton(R.string.no, null)
            .show();
    }

    private void saveUsername(String username) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId;
        String collection;

        if (currentUser != null) {
            userId = currentUser.getUid();
            collection = "users";
        } else {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("non_authorized_user_id", null);
            collection = "non_authorized_users";
        }

        if (userId == null) {
            Log.e(TAG, "No user ID found, cannot save username");
            return;
        }

        // Save to Firebase first
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        
        FirebaseFirestore.getInstance().collection(collection).document(userId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                // After successful Firebase save, update SharedPreferences as backup
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                prefs.edit().putString("user_username", username).apply();
                Log.d(TAG, "Username saved successfully to Firebase and SharedPreferences");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving username to Firebase", e);
                // If Firebase save fails, at least save to SharedPreferences
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                prefs.edit().putString("user_username", username).apply();
            });
    }

    // Método para manejar el clic en el FAB de creación de encuestas
    private void handleCreateSurveyClick() {
        // Verificar si el tutorial ya se ha mostrado
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
        boolean tutorialShown = prefs.getBoolean(TUTORIAL_SHOWN_KEY, false);

        if (!tutorialShown) {
            showSurveyCreationTutorial();
        } else {
            startActivity(new Intent(this, SurveyCreationActivity.class));
        }
    }

    // Método para mostrar el tutorial de creación de encuestas
    private void showSurveyCreationTutorial() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.survey_creation_tutorial_title)
               .setMessage(getString(R.string.survey_creation_tutorial_message) + "\n\n" +
                         getString(R.string.survey_creation_tutorial_rating) + "\n\n" +
                         getString(R.string.survey_creation_tutorial_multiple) + "\n\n" +
                         getString(R.string.survey_creation_tutorial_text) + "\n\n" +
                         getString(R.string.survey_creation_tutorial_tags))
               .setPositiveButton(R.string.survey_creation_tutorial_continue, (dialog, which) -> {
                   // Marcar el tutorial como mostrado
                   SharedPreferences prefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
                   prefs.edit().putBoolean(TUTORIAL_SHOWN_KEY, true).apply();
                   startActivity(new Intent(this, SurveyCreationActivity.class));
               })
               .setCancelable(false)
               .show();
    }

    // Método para actualizar la visibilidad del FAB basado en el nivel del usuario
    private void updateCreateSurveyFabVisibility() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId;
        String collection;

        if (currentUser != null) {
            userId = currentUser.getUid();
            collection = "users";
        } else {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            userId = prefs.getString("non_authorized_user_id", null);
            collection = "non_authorized_users";
        }

        if (userId != null) {
            FirebaseFirestore.getInstance().collection(collection).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long level = documentSnapshot.getLong("level");
                        if (level != null && level >= 2) {
                            createSurveyFab.setVisibility(View.VISIBLE);
                        } else {
                            createSurveyFab.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user level", e);
                    createSurveyFab.setVisibility(View.GONE);
                });
        }
    }
} 