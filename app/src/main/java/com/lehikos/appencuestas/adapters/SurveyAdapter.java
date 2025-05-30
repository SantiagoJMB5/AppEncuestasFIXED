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
    private final OnSurveyClickListener listener; // Hacer el listener final si solo se establece en el constructor
    // Usar ArrayList para una manipulación más fácil con DiffUtil, o asegurarse de que el tipo List lo soporte
    private List<Survey> surveys;

    public interface OnSurveyClickListener {
        void onSurveyClick(Survey survey);
    }

    public SurveyAdapter(List<Survey> initialSurveys, OnSurveyClickListener listener) {
        // Inicializar con una nueva lista para evitar modificar la lista original pasada, si es necesario
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
        if (survey == null) return; // Verificación básica de nulo para el objeto encuesta

        holder.titleText.setText(survey.getTitle());
        holder.descriptionText.setText(survey.getDescription());
        // Considerar agregar un marcador de posición o manejar nulo para la recompensa de experiencia si puede ser opcional
        holder.experienceText.setText(String.format("+%d XP", survey.getExperienceReward()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSurveyClick(survey);
            }
        });
    }

    @Override
    public int getItemCount() {
        return surveys != null ? surveys.size() : 0; // Verificación de nulo para la lista de encuestas
    }

    /**
     * Actualiza la lista de encuestas mostradas por el adaptador usando DiffUtil para actualizaciones eficientes.
     *
     * @param newSurveys La nueva lista de encuestas a mostrar.
     */
    public void updateSurveys(List<Survey> newSurveys) {
        if (newSurveys == null) {
            newSurveys = new ArrayList<>(); // Asegurar que newSurveys no sea nulo
        }

        final SurveyDiffCallback diffCallback = new SurveyDiffCallback(this.surveys, newSurveys);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.surveys.clear();
        this.surveys.addAll(newSurveys);
        diffResult.dispatchUpdatesTo(this); // Esto actualiza eficientemente el RecyclerView
    }

    static class SurveyViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView descriptionText;
        TextView experienceText;

        SurveyViewHolder(View itemView) {
            super(itemView);
            // Asegurarse de que estos IDs coincidan con su archivo XML R.layout.item_survey
            titleText = itemView.findViewById(R.id.survey_title);
            descriptionText = itemView.findViewById(R.id.survey_description);
            experienceText = itemView.findViewById(R.id.survey_experience);
        }
    }

    /**
     * Implementación de DiffUtil.Callback para calcular diferencias entre dos listas de Encuestas.
     * Esto ayuda a que RecyclerView se actualice eficientemente.
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
         * Llamado por DiffUtil para decidir si dos objetos representan el mismo elemento.
         * Por ejemplo, si sus elementos tienen IDs únicos, este método debería verificar su igualdad.
         */
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // CRÍTICO: Su modelo Survey necesita un identificador único (por ejemplo, getId())
            // para que DiffUtil funcione correctamente.
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        /**
         * Llamado por DiffUtil solo si areItemsTheSame() devuelve true para verificar si
         * la representación visual de un elemento ha cambiado.
         * Este método se usa para detectar cambios en el contenido del elemento.
         */
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Survey oldSurvey = oldList.get(oldItemPosition);
            Survey newSurvey = newList.get(newItemPosition);
            // Comparar todos los campos que afectan la representación UI del elemento.
            // Usando .equals() si está implementado en Survey, o comparar campos manualmente.
            // Por ejemplo:
            return oldSurvey.getTitle().equals(newSurvey.getTitle()) &&
                    oldSurvey.getDescription().equals(newSurvey.getDescription()) &&
                    oldSurvey.getExperienceReward() == newSurvey.getExperienceReward();
            // Agregar otros campos relevantes si se muestran o afectan la apariencia del elemento.
            // Si su clase Survey tiene un método .equals() apropiado, podría simplemente usar:
            // return oldSurvey.equals(newSurvey);
        }
    }
}