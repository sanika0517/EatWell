package com.example.eatwell.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.eatwell.LoginActivity; // Ensure you have a login activity
import com.example.eatwell.databinding.FragmentNotificationsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private boolean isEditing = false;

    private final String[] genderOptions = {"Male", "Female", "Other"};
    private final String[] dietOptions = {"Gain Weight", "Lose Weight", "Maintain Weight"};

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(auth.getUid()).child("profile");

        setupDropdowns();
        loadProfileData();
        setupButtons();

        NestedScrollView scrollView = binding.nestedScrollView; // or findViewById(R.id.scrollView)
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        return root;
    }

    private void setupDropdowns() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, genderOptions);
        binding.dropdownGender.setAdapter(genderAdapter);
        binding.dropdownGender.setOnClickListener(v -> binding.dropdownGender.showDropDown());

        ArrayAdapter<String> dietAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, dietOptions);
        binding.dropdownDietType.setAdapter(dietAdapter);
        binding.dropdownDietType.setOnClickListener(v -> binding.dropdownDietType.showDropDown());
    }

    private void loadProfileData() {
        // Show loading
        binding.progressBar.setVisibility(View.VISIBLE);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(auth.getUid());

        userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String username = snapshot.getValue(String.class);
                    binding.etName.setText(username);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load name", Toast.LENGTH_SHORT).show();
            }
        });

        userRef.child("profile").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String age = snapshot.child("age").exists() ? snapshot.child("age").getValue(String.class) : "";
                    String gender = snapshot.child("gender").exists() ? snapshot.child("gender").getValue(String.class) : "";
                    String height = snapshot.child("height").exists() ? snapshot.child("height").getValue(String.class) : "";
                    String weight = snapshot.child("weight").exists() ? snapshot.child("weight").getValue(String.class) : "";
                    String dietType = snapshot.child("dietType").exists() ? snapshot.child("dietType").getValue(String.class) : "";
                    String goal = snapshot.child("goal").exists() ? snapshot.child("goal").getValue(String.class) : "";

                    binding.etAge.setText(age);
                    binding.dropdownGender.setText(gender, false);
                    binding.etHeight.setText(height);
                    binding.etWeight.setText(weight);
                    binding.dropdownDietType.setText(dietType, false);
                    binding.etGoal.setText(goal);

                    setFieldsEditable(false);
                }

                // Hide loading after fetching all data
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
                binding.progressBar.setVisibility(View.GONE); // Hide loading on error
            }
        });
    }


    private void setupButtons() {
        binding.btnEditProfile.setOnClickListener(v -> {
            isEditing = !isEditing;
            if (isEditing) {
                setFieldsEditable(true);
                binding.btnEditProfile.setText("Cancel");
                binding.btnSaveProfile.setVisibility(View.VISIBLE);
            } else {
                setFieldsEditable(false);
                binding.btnEditProfile.setText("Edit");
                binding.btnSaveProfile.setVisibility(View.GONE);
            }
        });

        binding.btnSaveProfile.setOnClickListener(v -> saveProfileData());

        // Logout button functionality
        binding.btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void saveProfileData() {
        String name = binding.etName.getText().toString().trim();
        String age = binding.etAge.getText().toString().trim();
        String gender = binding.dropdownGender.getText().toString().trim();
        String height = binding.etHeight.getText().toString().trim();
        String weight = binding.etWeight.getText().toString().trim();
        String dietType = binding.dropdownDietType.getText().toString().trim();
        String goal = binding.etGoal.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(age) || TextUtils.isEmpty(height) || TextUtils.isEmpty(weight) || TextUtils.isEmpty(goal)) {
            Toast.makeText(getContext(), "All required fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(auth.getUid());
        userRef.child("name").setValue(name);

        Map<String, Object> userData = new HashMap<>();
        userData.put("age", age);
        userData.put("gender", gender);
        userData.put("height", height);
        userData.put("weight", weight);
        userData.put("dietType", dietType);
        userData.put("goal", goal);

        userRef.child("profile").updateChildren(userData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Profile Updated", Toast.LENGTH_SHORT).show();
                    setFieldsEditable(false);
                    binding.btnEditProfile.setText("Edit");
                    binding.btnSaveProfile.setVisibility(View.GONE);
                    isEditing = false;
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Update Failed", Toast.LENGTH_SHORT).show());
    }

    private void logoutUser() {
        auth.signOut();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
    }

    private void setFieldsEditable(boolean enabled) {
        binding.etName.setEnabled(enabled);
        binding.etAge.setEnabled(enabled);
        binding.etHeight.setEnabled(enabled);
        binding.etWeight.setEnabled(enabled);
        binding.etGoal.setEnabled(enabled);
        binding.dropdownGender.setEnabled(enabled);
        binding.dropdownDietType.setEnabled(enabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
