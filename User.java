package com.pipelinedetector;

/**
 * User class to store user information
 */
public class User {
    private String username;
    private String password;
    private String email;
    
    /**
     * Constructor for creating a new user
     * 
     * @param username the username
     * @param password the password (should be hashed in a real application)
     * @param email the email address
     */
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
    
    /**
     * Gets the username
     * 
     * @return the username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Gets the password
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * Gets the email
     * 
     * @return the email
     */
    public String getEmail() {
        return email;
    }
}
