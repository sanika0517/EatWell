package com.example.eatwell;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

public class WeightLossActivity extends AppCompatActivity {

    private boolean isActionBarShown = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_weight_loss);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        NestedScrollView scrollView = findViewById(R.id.nestedScrollView);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Hide/show ActionBar on scroll
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > oldScrollY && isActionBarShown) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().hide();
                    isActionBarShown = false;
                }
            } else if (scrollY < oldScrollY && !isActionBarShown) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().show();
                    isActionBarShown = true;
                }
            }
        });
    }
}
