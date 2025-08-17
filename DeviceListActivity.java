package com.pipelinedetector;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Set;

/**
 * Activity for listing and connecting to Bluetooth devices
 */
public class DeviceListActivity extends AppCompatActivity {
    private static final String TAG = "DeviceListActivity";
    
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> deviceList;
    private ArrayList<String> deviceNameList;
    private ArrayAdapter<String> deviceAdapter;
    
    private TextView scanStatusTextView;
    private ProgressBar scanProgressBar;
    private ListView deviceListView;
    private Button scanButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        
        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<>();
        deviceNameList = new ArrayList<>();
        
        // Set up UI elements
        scanStatusTextView = findViewById(R.id.scanStatusTextView);
        scanProgressBar = findViewById(R.id.scanProgressBar);
        deviceListView = findViewById(R.id.deviceListView);
        scanButton = findViewById(R.id.scanButton);
        
        // Set up list adapter
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNameList);
        deviceListView.setAdapter(deviceAdapter);
        
        // Set up button click listener
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isDiscovering()) {
                    // Cancel discovery if already discovering
                    bluetoothAdapter.cancelDiscovery();
                    scanButton.setText("Scan for Devices");
                    scanProgressBar.setVisibility(View.GONE);
                } else {
                    // Start discovery
                    scanForDevices();
                }
            }
        });
        
        // Set up item click listener
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Cancel discovery to save resources
                bluetoothAdapter.cancelDiscovery();
                
                // Get the selected device
                BluetoothDevice device = deviceList.get(position);
                
                // Start the GraphActivity with the device
                Intent intent = new Intent(DeviceListActivity.this, GraphActivity.class);
                intent.putExtra("device_address", device.getAddress());
                startActivity(intent);
            }
        });
        
        // Register for broadcasts when a device is found
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        
        // List paired devices when activity is created
        listPairedDevices();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Unregister broadcast receivers
        unregisterReceiver(receiver);
        
        // Cancel discovery
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }
    
    /**
     * List paired devices
     */
    private void listPairedDevices() {
        // Clear previous lists
        deviceList.clear();
        deviceNameList.clear();
        
        // Get paired devices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            
            // If there are paired devices, add each one to the ArrayAdapter
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    deviceList.add(device);
                    String deviceName = device.getName();
                    if (deviceName == null || deviceName.isEmpty()) {
                        deviceName = "Unknown Device";
                    }
                    deviceNameList.add(deviceName + "\n" + device.getAddress());
                }
            }
            
            // Update the status text
            scanStatusTextView.setText("Paired devices: " + deviceList.size());
            
            // Notify the adapter that the data has changed
            deviceAdapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Scan for Bluetooth devices
     */
    private void scanForDevices() {
        // Check if we have permission to scan
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth scan permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show scanning UI
        scanButton.setText("Stop Scanning");
        scanProgressBar.setVisibility(View.VISIBLE);
        scanStatusTextView.setText("Scanning for devices...");
        
        // Clear previous lists
        deviceList.clear();
        deviceNameList.clear();
        deviceAdapter.notifyDataSetChanged();
        
        // Get paired devices first
        listPairedDevices();
        
        // Start discovery
        bluetoothAdapter.startDiscovery();
    }
    
    /**
     * BroadcastReceiver for Bluetooth events
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                if (device != null && ActivityCompat.checkSelfPermission(DeviceListActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    // Add the device to the list if not already added
                    if (!deviceList.contains(device)) {
                        deviceList.add(device);
                        String deviceName = device.getName();
                        if (deviceName == null || deviceName.isEmpty()) {
                            deviceName = "Unknown Device";
                        }
                        deviceNameList.add(deviceName + "\n" + device.getAddress());
                        
                        // Update the adapter
                        deviceAdapter.notifyDataSetChanged();
                        
                        // Update the status text
                        scanStatusTextView.setText("Found " + deviceList.size() + " device(s)");
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Discovery has finished
                scanProgressBar.setVisibility(View.GONE);
                scanButton.setText("Scan for Devices");
                scanStatusTextView.setText("Found " + deviceList.size() + " device(s)");
            }
        }
    };
}
