package com.pipelinedetector;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Manages user authentication and registration
 */
public class UserManager {
    private static final String TAG = "UserManager";
    private static final String PREFS_NAME = "PipelineDetectorPrefs";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_USERNAME = "username";
    
    private static UserManager instance;
    private final SharedPreferences sharedPreferences;
    
    /**
     * Private constructor for singleton pattern
     * 
     * @param context application context
     */
    private UserManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get singleton instance
     * 
     * @param context application context
     * @return UserManager instance
     */
    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Register a new user
     * 
     * @param username username
     * @param password password
     * @param email email
     * @return true if registration successful, false otherwise
     */
    public boolean registerUser(String username, String password, String email) {
        // In a real app, you would store this in a secure database
        // For this simple example, we'll just log it and pretend it succeeded
        Log.d(TAG, "Registering user: " + username);
        
        // In a real app, you would check if username already exists
        // and hash the password before storing
        
        // Create user
        User user = new User(username, password, email);
        
        // Simulate successful registration
        return true;
    }
    
    /**
     * Login a user
     * 
     * @param username username
     * @param password password
     * @return true if login successful, false otherwise
     */
    public boolean loginUser(String username, String password) {
        // In a real app, you would validate against a secure database
        // For this simple example, we'll just pretend it succeeded if username and password are not empty
        
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            Log.d(TAG, "Login successful for user: " + username);
            
            // Save login state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_LOGGED_IN, true);
            editor.putString(KEY_USERNAME, username);
            editor.apply();
            
            return true;
        }
        
        Log.d(TAG, "Login failed for user: " + username);
        return false;
    }
    
    /**
     * Logout the current user
     */
    public void logoutUser() {
        Log.d(TAG, "Logging out user");
        
        // Clear login state
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_LOGGED_IN, false);
        editor.remove(KEY_USERNAME);
        editor.apply();
    }
    
    /**
     * Check if a user is logged in
     * 
     * @return true if a user is logged in, false otherwise
     */
    public boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
    }
    
    /**
     * Get the currently logged in username
     * 
     * @return username or null if no user is logged in
     */
    public String getCurrentUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }
}
