package com.pipelinedetector;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for user login
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    
    private UserManager userManager;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize the UserManager
        userManager = UserManager.getInstance(getApplicationContext());
        
        // Set up UI elements
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        backButton = findViewById(R.id.backButton);
        
        // Set up button click listeners
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
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
     * Login user with provided credentials
     */
    private void loginUser() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        // Check if fields are empty
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Attempt login
        boolean success = userManager.loginUser(username, password);
        
        if (success) {
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
            
            // Start the DeviceListActivity and clear the activity stack
            Intent intent = new Intent(LoginActivity.this, DeviceListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Login failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
        }
    }
}
