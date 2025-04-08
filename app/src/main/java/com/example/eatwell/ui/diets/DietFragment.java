package com.example.eatwell.ui.diets;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.eatwell.R;
import com.example.eatwell.utils.GeminiAPI;

import java.util.ArrayList;
import java.util.List;

public class DietFragment extends Fragment {

    private RecyclerView recyclerView;
    private DietAdapter adapter;
    private List<NutritionItem> dietList;
    private DatabaseReference databaseReference;

    public DietFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diet, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        dietList = new ArrayList<>();
        adapter = new DietAdapter(dietList);
        recyclerView.setAdapter(adapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("diets");

        fetchDietPlans();

        return view;
    }

    private void fetchDietPlans() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dietList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        NutritionItem item = dataSnapshot.getValue(NutritionItem.class);
                        if (item != null) {
                            dietList.add(item);
                        }
                    }
                    updateUI();
                } else {
                    fetchAIPlans();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase_ERROR", "Database Error: " + error.getMessage());
                showToast("Database Error");
            }
        });
    }

    private void fetchAIPlans() {
        GeminiAPI.getDietPlan(new GeminiAPI.DietCallback() {
            @Override
            public void onSuccess(List<NutritionItem> aiDietPlans) {
                if (aiDietPlans != null && !aiDietPlans.isEmpty()) {
                    dietList.addAll(aiDietPlans);
                    updateUI();

                    // Save AI-generated diet plans to Firebase
                    for (NutritionItem item : aiDietPlans) {
                        String key = databaseReference.push().getKey();
                        if (key != null) {
                            databaseReference.child(key).setValue(item);
                        }
                    }
                } else {
                    Log.e("GeminiAPI_ERROR", "AI returned an empty diet plan");
                    showToast("AI generated an empty diet plan");
                }
            }

            @Override
            public void onFailure(String error) {
                Log.e("GeminiAPI_ERROR", "AI Diet Fetch Error: " + error);
                showToast("AI Diet Plan Fetch Failed");
            }
        });
    }

    private void updateUI() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
        }
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
