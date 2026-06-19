package com.example.eatwell;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotesActivity extends AppCompatActivity {

    private TextView tvCurrentDate;
    private TextView tvWaterCount;
    private Button btnDecreaseWater;
    private Button btnIncreaseWater;
    private ProgressBar waterProgressBar;
    private LinearLayout foodEntriesContainer;
    private EditText etFoodDescription;
    private Button btnAddFood;
    private Spinner mealTypeSpinner;
    private ImageButton btnBack;
    private Button btnSaveToDatabase;
    private Button btnChangeDate;

    private Calendar selectedDate;
    private int waterCount = 0;
    private List<FoodEntry> foodEntries = new ArrayList<>();
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        // Initialize views
        initViews();

        // Set up Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        } else {
            Toast.makeText(this, "Please log in to use this feature", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ImageButton backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> finish());

        // Set up current date
        selectedDate = Calendar.getInstance();
        updateDateDisplay();

        // Set up meal type spinner
        setupMealTypeSpinner();

        // Set up click listeners
        setupClickListeners();

        // Load data for current date
        loadDataForSelectedDate();
    }

    private void initViews() {
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvWaterCount = findViewById(R.id.tvWaterCount);
        btnDecreaseWater = findViewById(R.id.btnDecreaseWater);
        btnIncreaseWater = findViewById(R.id.btnIncreaseWater);
        waterProgressBar = findViewById(R.id.waterProgressBar);
        foodEntriesContainer = findViewById(R.id.foodEntriesContainer);
        etFoodDescription = findViewById(R.id.etFoodDescription);
        btnAddFood = findViewById(R.id.btnAddFood);
        mealTypeSpinner = findViewById(R.id.mealTypeSpinner);
        btnBack = findViewById(R.id.btnBack);
        btnSaveToDatabase = findViewById(R.id.btnSaveToDatabase);
        btnChangeDate = findViewById(R.id.btnChangeDate);
    }

    private void setupMealTypeSpinner() {
        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, mealTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mealTypeSpinner.setAdapter(adapter);
    }


private void setupClickListeners() {
    btnDecreaseWater.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (waterCount > 0) {
                waterCount--;
                updateWaterUI();
            }
        }
    });

    btnIncreaseWater.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            waterCount++;
            updateWaterUI();
        }
    });

    btnAddFood.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String foodDescription = etFoodDescription.getText().toString().trim();
            String mealType = mealTypeSpinner.getSelectedItem().toString();

            if (!foodDescription.isEmpty()) {
                addFoodEntry(mealType, foodDescription);
                etFoodDescription.setText("");
            } else {
                Toast.makeText(NotesActivity.this, "Please enter food description", Toast.LENGTH_SHORT).show();
            }
        }
    });

    btnBack.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    });

    btnSaveToDatabase.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            saveDataToFirebase();
        }
    });

    btnChangeDate.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showDatePickerDialog();
        }
    });
}

private void updateWaterUI() {
    tvWaterCount.setText(String.valueOf(waterCount) + " glasses");

    // Update progress bar (assuming 8 glasses daily goal)
    int maxWater = 8;
    int progress = (int) ((waterCount / (float) maxWater) * 100);
    waterProgressBar.setProgress(Math.min(progress, 100));
}

private void showDatePickerDialog() {
    DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    selectedDate.set(year, month, dayOfMonth);
                    updateDateDisplay();
                    loadDataForSelectedDate();
                }
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
    );
    datePickerDialog.show();
}

private void updateDateDisplay() {
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault());
    String formattedDate = dateFormat.format(selectedDate.getTime());
    tvCurrentDate.setText(formattedDate);
}

// Firebase implementation
private FirebaseDatabase database;
private DatabaseReference userRef;
private String userId;

// Add this to your onCreate method after initViews()
private void setupFirebase() {
    // Initialize Firebase
    database = FirebaseDatabase.getInstance();

    // Get current user ID (you'll need Firebase Auth for this)
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    if (currentUser != null) {
        userId = currentUser.getUid();
        userRef = database.getReference("users").child(userId);
    } else {
        // Handle not logged in state
        Toast.makeText(this, "Please log in to save data", Toast.LENGTH_LONG).show();
        finish();
    }
}

    private void loadDataForSelectedDate() {
        // Check if user is logged in
        if (auth.getCurrentUser() == null) return;

        String dateKey = getDateKey(selectedDate.getTime());
        DatabaseReference dateRef = FirebaseDatabase.getInstance()
                .getReference("users").child(auth.getUid()).child("dailyNotes").child(dateKey);

        dateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear current data
                waterCount = 0;
                foodEntries.clear();
                foodEntriesContainer.removeAllViews();

                if (dataSnapshot.exists()) {
                    // Load water count
                    if (dataSnapshot.hasChild("waterCount")) {
                        Integer waterValue = dataSnapshot.child("waterCount").getValue(Integer.class);
                        if (waterValue != null) {
                            waterCount = waterValue;
                        }
                    }
                    updateWaterUI();

                    // Load food entries
                    if (dataSnapshot.hasChild("foodEntries")) {
                        DataSnapshot foodEntriesSnapshot = dataSnapshot.child("foodEntries");
                        for (DataSnapshot entrySnapshot : foodEntriesSnapshot.getChildren()) {
                            String mealType = entrySnapshot.child("mealType").getValue(String.class);
                            String description = entrySnapshot.child("description").getValue(String.class);

                            if (mealType != null && description != null) {
                                FoodEntry entry = new FoodEntry(mealType, description);
                                foodEntries.add(entry);
                                addFoodEntryToUI(entry);
                            }
                        }
                    }
                } else {
                    // No data exists for this date
                    updateWaterUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(NotesActivity.this, "Failed to load data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void saveDataToFirebase() {
        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(NotesActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String dateKey = getDateKey(selectedDate.getTime());

        // Get reference to the correct location in the database
        DatabaseReference dateRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("dailyNotes").child(dateKey);

        // Create a map for all data to save
        Map<String, Object> updates = new HashMap<>();
        updates.put("waterCount", waterCount);

        // Save each food entry with index as key
        if (!foodEntries.isEmpty()) {
            for (int i = 0; i < foodEntries.size(); i++) {
                FoodEntry entry = foodEntries.get(i);
                updates.put("foodEntries/" + i + "/mealType", entry.getMealType());
                updates.put("foodEntries/" + i + "/description", entry.getDescription());
            }
        }

        // Save to Firebase
        dateRef.updateChildren(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(NotesActivity.this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(NotesActivity.this, "Failed to save data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }



// Helper method to format date as a Firebase safe key
private String getDateKey(Date date) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    return dateFormat.format(date);
}

// Food entry model class
public static class FoodEntry {
    private String mealType;
    private String description;

    // Required empty constructor for Firebase
    public FoodEntry() {}

    public FoodEntry(String mealType, String description) {
        this.mealType = mealType;
        this.description = description;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

private void addFoodEntry(String mealType, String description) {
    FoodEntry entry = new FoodEntry(mealType, description);
    foodEntries.add(entry);
    addFoodEntryToUI(entry);
}

private void addFoodEntryToUI(final FoodEntry entry) {
    View foodEntryView = LayoutInflater.from(this).inflate(R.layout.item_food_entry, null);
    TextView tvMealType = foodEntryView.findViewById(R.id.tvMealType);
    TextView tvFoodDescription = foodEntryView.findViewById(R.id.tvFoodDescription);
    ImageButton btnRemove = foodEntryView.findViewById(R.id.btnRemoveFood);

    tvMealType.setText(entry.getMealType());
    tvFoodDescription.setText(entry.getDescription());

    // Set up remove button
    final int index = foodEntries.indexOf(entry);
    btnRemove.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            foodEntries.remove(index);
            foodEntriesContainer.removeViewAt(index);
        }
    });

    foodEntriesContainer.addView(foodEntryView);
}}