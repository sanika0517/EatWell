package com.example.eatwell.utils;

import android.util.Log;
import com.example.eatwell.ui.diets.NutritionItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.*;

public class GeminiAPI {

    private static final String API_URL = "https://api.gemini.com/generate-diet";
    private static final String API_KEY = "AIzaSyAF9Xqbtsrex8HKasI334vL9K49OcroNx0";  // Replace with your actual API key

    public interface DietCallback {
        void onSuccess(List<NutritionItem> dietPlans);
        void onFailure(String error);
    }

    public static void getDietPlan(DietCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String requestBody = "{\"prompt\": \"Generate a balanced diet plan for breakfast, lunch, and dinner\"}";

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("Response failed: " + response.message());
                    return;
                }

                // Parse JSON response (example response parsing)
                String responseData = response.body().string();
                List<NutritionItem> dietPlans = parseDietPlans(responseData);
                callback.onSuccess(dietPlans);
            }
        });
    }

    private static List<NutritionItem> parseDietPlans(String jsonResponse) {
        List<NutritionItem> dietList = new ArrayList<>();

        // Example JSON Parsing (Modify as per API response)
        dietList.add(new NutritionItem("Breakfast", "Oatmeal with fruits and nuts", 350));
        dietList.add(new NutritionItem("Lunch", "Grilled chicken salad with quinoa", 500));
        dietList.add(new NutritionItem("Dinner", "Steamed salmon with roasted vegetables", 600));

        return dietList;
    }
}
