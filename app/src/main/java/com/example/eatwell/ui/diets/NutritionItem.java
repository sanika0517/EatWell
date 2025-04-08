package com.example.eatwell.ui.diets;

public class NutritionItem {
    private String mealType;
    private String description;
    private int calories;

    public NutritionItem() {
        // Default constructor required for Firebase
    }

    public NutritionItem(String mealType, String description, int calories) {
        this.mealType = mealType;
        this.description = description;
        this.calories = calories;
    }

    public String getMealType() {
        return mealType;
    }

    public String getDescription() {
        return description;
    }

    public int getCalories() {
        return calories;
    }
}
