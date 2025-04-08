package com.example.eatwell.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.eatwell.R;
import com.example.eatwell.databinding.FragmentDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private final DecimalFormat df = new DecimalFormat("#.#");
    private static final String TAG = "DashboardFragment";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        NestedScrollView scrollView = binding.nestedScrollView; // or findViewById(R.id.scrollView)
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Show loading overlay immediately
        showLoadingOverlay(true);

        // Set up UI elements
        setupUI();

        // Load user data from Firebase
        loadUserData();

        return root;
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnContinue.setOnClickListener(v -> {
            // Handle continue button click - navigate to next screen
            // For example:
            // Navigation.findNavController(v).navigate(R.id.action_dashboardFragment_to_nextFragment);
        });
    }

    private void showLoadingOverlay(boolean show) {
        // Add null check to prevent crashes
        if (binding == null) return;

        // Add logging for debugging
        Log.d(TAG, "Setting loading overlay visibility to: " + (show ? "VISIBLE" : "GONE"));

        if (show) {
            binding.loadingOverlay.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);

            // Force layout update
            binding.loadingOverlay.bringToFront();
            binding.loadingOverlay.invalidate();
        } else {
            binding.loadingOverlay.setVisibility(View.GONE);
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void loadUserData() {
        // Ensure loading overlay is visible
        showLoadingOverlay(true);

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(getContext(), "Please log in to view dashboard", Toast.LENGTH_SHORT).show();
            showLoadingOverlay(false);
            return;
        }

        Log.d(TAG, "Loading data for user: " + userId);

        mDatabase.child("users").child(userId).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.getValue(String.class);
                    Log.d(TAG, "User name found: " + name);
                    loadProfileData(userId); // Fetch profile after name is loaded
                    // Note: Loading overlay stays visible until profile data is loaded
                } else {
                    Log.e(TAG, "User name not found");
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    showLoadingOverlay(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                showLoadingOverlay(false);
            }
        });
    }

    private void loadProfileData(String userId) {
        // Loading overlay is still showing from loadUserData()

        mDatabase.child("users").child(userId).child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot profileSnapshot) {
                if (profileSnapshot.exists()) {
                    String gender = profileSnapshot.child("gender").getValue(String.class);
                    String ageStr = profileSnapshot.child("age").getValue(String.class);
                    String weightStr = profileSnapshot.child("weight").getValue(String.class);
                    String heightStr = profileSnapshot.child("height").getValue(String.class);
                    String dietType = profileSnapshot.child("dietType").getValue(String.class);
                    String goal = profileSnapshot.child("goal").getValue(String.class);

                    Integer age = (ageStr != null) ? Integer.parseInt(ageStr) : null;
                    Double weight = (weightStr != null) ? Double.parseDouble(weightStr) : null;
                    Double height = (heightStr != null) ? Double.parseDouble(heightStr) : null;

                    Log.d(TAG, "Profile loaded: Gender=" + gender + ", Age=" + age + ", Weight=" + weight);

                    updateProfileUI(gender, age, weight, goal, dietType, height);

                    // Hide loading overlay only when all data is loaded and UI is updated
                    showLoadingOverlay(false);
                } else {
                    Log.e(TAG, "Profile data not found");
                    Toast.makeText(getContext(), "Profile data not found", Toast.LENGTH_SHORT).show();
                    showLoadingOverlay(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(getContext(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
                showLoadingOverlay(false);
            }
        });
    }

    private void updateProfileUI(String gender, Integer age, Double weight, String goal,
                                 String dietType, Double height) {
        if (gender != null) {
            String genderFormatted = gender.trim().toLowerCase(); // Normalize gender

            if (genderFormatted.equals("male")) {
                binding.ivProfile.setImageResource(R.drawable.profile_man);
            } else if (genderFormatted.equals("female")) {
                binding.ivProfile.setImageResource(R.drawable.profile_woman);
            } else {
                binding.ivProfile.setImageResource(R.drawable.profile_other);
            }
        } else {
            binding.ivProfile.setImageResource(R.drawable.profile_other);
        }

        // Set age
        if (age != null) {
            binding.tvAge.setText(String.valueOf(age));
        }

        // Set weight
        if (weight != null) {
            binding.tvWeight.setText(df.format(weight) + " kg");
        }

        // Set target weight (using the same as current weight for now, or you could calculate a target)
        if (weight != null) {
            // Calculate a target weight based on the goal
            double targetWeight = weight;
            if (goal != null) {
                if (goal.toLowerCase().contains("lose weight")) {
                    targetWeight = weight * 0.9; // 10% reduction as an example
                } else if (goal.toLowerCase().contains("gain weight")) {
                    targetWeight = weight * 1.1; // 10% increase as an example
                }
            }
            binding.tvTargetWeight.setText(df.format(targetWeight) + " kg");
        }

        // Set height
        if (height != null) {
            binding.tvHeight.setText(df.format(height) + " cm");
        }

        // Set activity level (this is hardcoded, you may want to add this to your database)
        binding.tvActivityLevel.setText("Moderately active");

        // Set diet type
        if (dietType != null) {
            binding.tvDietType.setText(dietType);
        }

        // Set goal
        if (goal != null) {
            binding.tvGoal.setText(goal);
        }

        // Calculate and display BMI
        if (weight != null && height != null && height > 0) {
            double bmi = calculateBMI(weight, height);
            binding.tvBmiValue.setText(df.format(bmi));

            // Update BMI status, progress, and extra info
            updateBMIStatus(bmi);
        }

        // Set data source recommendations
        binding.tvRecommendationSourceBmi.setText("Source: WHO BMI Guidelines");
        binding.tvRecommendationSourceProfile.setText("Source: User Profile Data");
    }

    private double calculateBMI(double weightKg, double heightCm) {
        double heightM = heightCm / 100; // Convert height from cm to meters
        return weightKg / (heightM * heightM);
    }

    private void updateBMIStatus(double bmi) {
        String status;
        String extraInfo;
        int progressPercent;
        int backgroundResId;

        if (bmi < 18.5) {
            status = "Underweight";
            extraInfo = "Consider increasing calorie intake and maintaining a balanced diet.";
            progressPercent = 10;
            backgroundResId = R.drawable.bg_oval_red;
        } else if (bmi < 25) {
            status = "Healthy";
            extraInfo = "Great job! Maintain a balanced diet and regular exercise.";
            progressPercent = 30;
            backgroundResId = R.drawable.ic_circle_green;
        } else if (bmi < 30) {
            status = "Overweight";
            extraInfo = "Try increasing physical activity and monitoring calorie intake.";
            progressPercent = 60;
            backgroundResId = R.drawable.ic_circle_yellow;
        } else {
            status = "Obese";
            extraInfo = "Consult a healthcare provider for weight management strategies.";
            progressPercent = 90;
            backgroundResId = R.drawable.bg_oval_red;
        }

        // Update BMI status text and background
        binding.tvBmiStatus.setText(status);
        binding.tvBmiStatus.setBackgroundResource(backgroundResId);

        // Update progress bar
        binding.progressBmi.setProgress(progressPercent);

        // Set extra info text
        binding.tvBmiExtraInfo.setText(extraInfo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}