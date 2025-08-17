nb package com.pipelinedetector;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity - first screen of the app for login or registration
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    private UserManager userManager;
    private Button loginButton;
    private Button registerButton;
    private Button continueAsGuestButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize the UserManager
        userManager = UserManager.getInstance(getApplicationContext());
        
        // Set up UI elements
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        continueAsGuestButton = findViewById(R.id.guestButton);
        
        // Set up button click listeners
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });
        
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });
        
        continueAsGuestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check and request Bluetooth permissions before proceeding
                checkBluetoothPermissions();
            }
        });
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // If user is already logged in, skip to the device list
        if (userManager.isUserLoggedIn()) {
            checkBluetoothPermissions();
        }
    }
    
    /**
     * Check and request Bluetooth permissions
     */
    private void checkBluetoothPermissions() {
        // Check if Bluetooth is supported
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                // Request permission if not granted
                requestBluetoothPermissions();
            }
            return;
        }
        
        // Check for required permissions on Android 6.0+
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires BLUETOOTH_SCAN and BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            // Older versions need location permission for Bluetooth scanning
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            requestBluetoothPermissions();
        } else {
            // All permissions granted, proceed to device list
            proceedToDeviceList();
        }
    }
    
    /**
     * Request Bluetooth permissions based on Android version
     */
    private void requestBluetoothPermissions() {
        List<String> permissions = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires BLUETOOTH_SCAN and BLUETOOTH_CONNECT
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            // Older versions need location permission for Bluetooth scanning
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        
        ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
            } else {
                allGranted = false;
            }
            
            if (allGranted) {
                // All permissions granted, check if Bluetooth is enabled
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    proceedToDeviceList();
                }
            } else {
                Toast.makeText(this, "Permissions required to scan for devices", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is now enabled, proceed to device list
                proceedToDeviceList();
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to use this app", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Proceed to the Bluetooth device list activity
     */
    private void proceedToDeviceList() {
        startActivity(new Intent(MainActivity.this, DeviceListActivity.class));
    }
}
