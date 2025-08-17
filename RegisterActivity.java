package com.pipelinedetector;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for user registration
 */
public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    
    private UserManager userManager;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private EditText emailEditText;
    private Button registerButton;
    private Button backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Initialize the UserManager
        userManager = UserManager.getInstance(getApplicationContext());
        
        // Set up UI elements
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        emailEditText = findViewById(R.id.emailEditText);
        registerButton = findViewById(R.id.registerButton);
        backButton = findViewById(R.id.backButton);
        
        // Set up button click listeners
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
        
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    /**
     * Register a new user with provided information
     */
    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        
        // Check if fields are empty
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check basic email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Attempt registration
        boolean success = userManager.registerUser(username, password, email);
        
        if (success) {
            Toast.makeText(this, "Registration successful. Please login.", Toast.LENGTH_SHORT).show();
            finish(); // Go back to the previous screen
        } else {
            Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
