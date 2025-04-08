package com.example.eatwell.ui.diets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eatwell.R;
import java.util.List;

public class DietAdapter extends RecyclerView.Adapter<DietAdapter.DietViewHolder> {

    private List<NutritionItem> dietList;

    public DietAdapter(List<NutritionItem> dietList) {
        this.dietList = dietList;
    }

    @NonNull
    @Override
    public DietViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_diet, parent, false);
        return new DietViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DietViewHolder holder, int position) {
        NutritionItem item = dietList.get(position);
        holder.mealTypeTextView.setText(item.getMealType());
        holder.descriptionTextView.setText(item.getDescription());
        holder.caloriesTextView.setText(item.getCalories() + " kcal");
    }

    @Override
    public int getItemCount() {
        return dietList.size();
    }

    static class DietViewHolder extends RecyclerView.ViewHolder {
        TextView mealTypeTextView, descriptionTextView, caloriesTextView;

        public DietViewHolder(@NonNull View itemView) {
            super(itemView);
            mealTypeTextView = itemView.findViewById(R.id.text_meal_type);
            descriptionTextView = itemView.findViewById(R.id.text_description);
            caloriesTextView = itemView.findViewById(R.id.text_calories);
        }
    }
}
