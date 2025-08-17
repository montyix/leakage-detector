package com.pipelinedetector;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for visualizing the state of the pipeline system
 */
public class StateActivity extends AppCompatActivity {
    private static final String TAG = "StateActivity";
    
    private ImageView pipeImageView;
    private TextView stateDescriptionTextView;
    private Button backButton;
    
    private double flow1, flow2, flow3;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state);
        
        // Get flow rates from intent
        flow1 = getIntent().getDoubleExtra("flow1", 0.0);
        flow2 = getIntent().getDoubleExtra("flow2", 0.0);
        flow3 = getIntent().getDoubleExtra("flow3", 0.0);
        
        // Set up UI elements
        pipeImageView = findViewById(R.id.pipeImageView);
        stateDescriptionTextView = findViewById(R.id.stateDescriptionTextView);
        backButton = findViewById(R.id.backButton);
        
        // Set up back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        // Update display
        updateDisplay();
    }
    
    /**
     * Update the pipeline state visualization
     */
    private void updateDisplay() {
        // Calculate flow rate differences to detect leaks
        double diff12 = Math.abs(flow1 - flow2);
        double diff23 = Math.abs(flow2 - flow3);
        
        // Threshold for detecting leaks (adjust based on real data)
        double threshold = 0.5; // 0.5 L/min
        
        boolean leak12 = diff12 > threshold;
        boolean leak23 = diff23 > threshold;
        
        // Update image and description based on leak location
        if (leak12 && leak23) {
            // Multiple leaks
            pipeImageView.setImageResource(R.drawable.leak_multiple);
            stateDescriptionTextView.setText("Multiple leaks detected in the pipeline!\n\n" +
                    "Flow rates (L/min):\n" +
                    String.format("Point 1: %.2f\n", flow1) +
                    String.format("Point 2: %.2f\n", flow2) +
                    String.format("Point 3: %.2f", flow3));
        } else if (leak12) {
            // Leak between points 1 and 2
            pipeImageView.setImageResource(R.drawable.leak_1_2);
            stateDescriptionTextView.setText("Leak detected between Points 1 and 2!\n\n" +
                    "Flow rates (L/min):\n" +
                    String.format("Point 1: %.2f\n", flow1) +
                    String.format("Point 2: %.2f\n", flow2) +
                    String.format("Point 3: %.2f", flow3));
        } else if (leak23) {
            // Leak between points 2 and 3
            pipeImageView.setImageResource(R.drawable.leak_2_3);
            stateDescriptionTextView.setText("Leak detected between Points 2 and 3!\n\n" +
                    "Flow rates (L/min):\n" +
                    String.format("Point 1: %.2f\n", flow1) +
                    String.format("Point 2: %.2f\n", flow2) +
                    String.format("Point 3: %.2f", flow3));
        } else {
            // No leaks
            pipeImageView.setImageResource(R.drawable.no_leak);
            stateDescriptionTextView.setText("No leaks detected in the pipeline.\n\n" +
                    "Flow rates (L/min):\n" +
                    String.format("Point 1: %.2f\n", flow1) +
                    String.format("Point 2: %.2f\n", flow2) +
                    String.format("Point 3: %.2f", flow3));
        }
    }
}
