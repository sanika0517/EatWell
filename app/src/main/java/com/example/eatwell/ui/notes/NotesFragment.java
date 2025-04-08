package com.example.eatwell.ui.notes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.eatwell.NotesActivity;
import com.example.eatwell.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NotesFragment extends Fragment {

    private LinearLayout entriesContainer;
    private ProgressBar progressBar;
    private TextView tvNoEntries;
    private ImageButton btnBack;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;

    // Model class for date entries
    private static class DateEntry {
        String date;
        int waterCount;
        List<NotesActivity.FoodEntry> foodEntries;

        DateEntry(String date) {
            this.date = date;
            this.waterCount = 0;
            this.foodEntries = new ArrayList<>();
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fregment_notes, container, false);

        // Initialize views
        entriesContainer = view.findViewById(R.id.entriesContainer);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoEntries = view.findViewById(R.id.tvNoEntries);
        btnBack = view.findViewById(R.id.btnBack);
        FloatingActionButton fab = view.findViewById(R.id.fab_add);


        NestedScrollView scrollView = view.findViewById(R.id.nestedScrollView); // or findViewById(R.id.scrollView)
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Set up Firebase
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please log in to view entries", Toast.LENGTH_LONG).show();
        }

        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(currentUser.getUid()).child("dailyNotes");

        // Set up back button
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        fab.setOnClickListener(v -> {
            Toast.makeText(getContext(), "FAB Clicked!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), NotesActivity.class);
            startActivity(intent);
        });

        // Load all entries
        loadAllEntries();
        return view;
    }

    private void loadAllEntries() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoEntries.setVisibility(View.GONE);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    showNoEntriesMessage();
                    return;
                }

                List<DateEntry> dateEntries = new ArrayList<>();

                // Process all date entries
                for (DataSnapshot dateSnapshot : dataSnapshot.getChildren()) {
                    String dateKey = dateSnapshot.getKey();
                    if (dateKey == null) continue;

                    DateEntry dateEntry = new DateEntry(dateKey);

                    // Get water count
                    if (dateSnapshot.hasChild("waterCount")) {
                        Integer waterValue = dateSnapshot.child("waterCount").getValue(Integer.class);
                        if (waterValue != null) {
                            dateEntry.waterCount = waterValue;
                        }
                    }

                    // Get food entries
                    if (dateSnapshot.hasChild("foodEntries")) {
                        DataSnapshot foodEntriesSnapshot = dateSnapshot.child("foodEntries");
                        for (DataSnapshot entrySnapshot : foodEntriesSnapshot.getChildren()) {
                            String mealType = entrySnapshot.child("mealType").getValue(String.class);
                            String description = entrySnapshot.child("description").getValue(String.class);

                            if (mealType != null && description != null) {
                                NotesActivity.FoodEntry foodEntry = new NotesActivity.FoodEntry(mealType, description);
                                dateEntry.foodEntries.add(foodEntry);
                            }
                        }
                    }

                    // Add to list if has any content
                    if (dateEntry.waterCount > 0 || !dateEntry.foodEntries.isEmpty()) {
                        dateEntries.add(dateEntry);
                    }
                }

                // Sort entries by date (most recent first)
                Collections.sort(dateEntries, new Comparator<DateEntry>() {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

                    @Override
                    public int compare(DateEntry o1, DateEntry o2) {
                        try {
                            Date date1 = sdf.parse(o1.date);
                            Date date2 = sdf.parse(o2.date);

                            if (date1 != null && date2 != null) {
                                return date2.compareTo(date1); // Descending order
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        return 0;
                    }
                });

                // Display entries
                displayEntries(dateEntries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Failed to load entries: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayEntries(List<DateEntry> dateEntries) {
        progressBar.setVisibility(View.GONE);
        entriesContainer.removeAllViews();

        if (dateEntries.isEmpty()) {
            showNoEntriesMessage();
            return;
        }

        SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (DateEntry entry : dateEntries) {
            CardView dateCard = (CardView) LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_date_entry, entriesContainer, false);

            TextView tvDate = dateCard.findViewById(R.id.tvDate);
            TextView tvWaterCount = dateCard.findViewById(R.id.tvWaterCount);
            LinearLayout foodContainer = dateCard.findViewById(R.id.foodEntriesContainer);

            // Format the date
            try {
                Date date = parseFormat.parse(entry.date);
                if (date != null) {
                    tvDate.setText(displayFormat.format(date));
                } else {
                    tvDate.setText(entry.date);
                }
            } catch (ParseException e) {
                tvDate.setText(entry.date);
            }

            // Display water count
            tvWaterCount.setText(String.valueOf(entry.waterCount) + " glasses of water");

            // Display food entries
            for (NotesActivity.FoodEntry foodEntry : entry.foodEntries) {
                View foodEntryView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_food_entry_readonly, foodContainer, false);

                TextView tvMealType = foodEntryView.findViewById(R.id.tvMealType);
                TextView tvFoodDescription = foodEntryView.findViewById(R.id.tvFoodDescription);

                tvMealType.setText(foodEntry.getMealType());
                tvFoodDescription.setText(foodEntry.getDescription());

                foodContainer.addView(foodEntryView);
            }

            entriesContainer.addView(dateCard);
        }
    }

    private void showNoEntriesMessage() {
        progressBar.setVisibility(View.GONE);
        tvNoEntries.setVisibility(View.VISIBLE);
        entriesContainer.removeAllViews();
    }
}