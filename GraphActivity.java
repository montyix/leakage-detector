package com.pipelinedetector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying flow rate data and detecting leakage using MPAndroidChart
 * and org.json for data management
 */
public class GraphActivity extends AppCompatActivity {
    private static final String TAG = "GraphActivity";
    
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    // Flow rates and pressure constants
    private static final double DENSITY = 1000.0; // water density in kg/m^3
    private static final double GRAVITY = 9.81; // acceleration due to gravity in m/s^2
    private static final double PIPE_RADIUS = 0.01; // pipe radius in meters
    private static final double PIPE_AREA = Math.PI * PIPE_RADIUS * PIPE_RADIUS; // pipe cross-sectional area
    
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothService bluetoothService;
    private String connectedDeviceAddress;
    
    private TextView statusTextView;
    private TextView leakageStatusTextView;
    private LineChart chart;
    private Button viewStateButton;
    private Button disconnectButton;
    
    // Flow rate data
    private double flow1 = 0.0; // flow rate at point 1
    private double flow2 = 0.0; // flow rate at point 2
    private double flow3 = 0.0; // flow rate at point 3
    private List<Double> pressurePoints = new ArrayList<>();
    
    // Data history for flow rates and pressures (stored as JSON)
    private JSONArray historyData = new JSONArray();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        
        // Get the device address from intent
        connectedDeviceAddress = getIntent().getStringExtra("device_address");
        if (connectedDeviceAddress == null) {
            Toast.makeText(this, "No device address provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // Set up UI elements
        statusTextView = findViewById(R.id.statusTextView);
        leakageStatusTextView = findViewById(R.id.leakageStatusTextView);
        chart = findViewById(R.id.chart);
        viewStateButton = findViewById(R.id.viewStateButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        
        // Set up button click listeners
        viewStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the StateActivity with the current flow rates
                Intent intent = new Intent(GraphActivity.this, StateActivity.class);
                intent.putExtra("flow1", flow1);
                intent.putExtra("flow2", flow2);
                intent.putExtra("flow3", flow3);
                startActivity(intent);
            }
        });
        
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Disconnect and go back to device list
                if (bluetoothService != null) {
                    bluetoothService.stop();
                }
                finish();
            }
        });
        
        // Set up the MPAndroidChart
        setupChart();
        
        // Initialize BluetoothService
        bluetoothService = new BluetoothService(handler);
    }
    
    /**
     * Set up the MPAndroidChart for displaying pressure data
     */
    private void setupChart() {
        // General chart settings
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawGridBackground(false);
        
        // X-Axis settings
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(3);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Label points by position (0, 20, 40 meters)
                if (value == 0) return "0m";
                if (value == 1) return "20m";
                if (value == 2) return "40m";
                return value + "m";
            }
        });
        
        // Left Y-Axis settings
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value + " kPa";
            }
        });
        
        // Right Y-Axis settings (disabled)
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Initialize with empty data
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        chart.setData(data);
        
        // Add legend
        chart.getLegend().setEnabled(true);
        
        // Refresh chart
        chart.invalidate();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // Connect to the device
        if (bluetoothService != null) {
            if (bluetoothService.getState() == BluetoothService.STATE_NONE) {
                connectToDevice();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Stop BluetoothService
        if (bluetoothService != null) {
            bluetoothService.stop();
        }
    }
    
    /**
     * Connect to the Bluetooth device
     */
    private void connectToDevice() {
        // Get the BluetoothDevice object
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(connectedDeviceAddress);
        
        // Attempt to connect to the device
        bluetoothService.connect(device);
    }
    
    /**
     * Process the data received from Arduino
     * 
     * @param data The data received
     */
    private void processData(String data) {
        Log.d(TAG, "Processing data: " + data);
        
        try {
            // Split the data by commas
            String[] values = data.trim().split(",");
            
            if (values.length >= 3) {
                // Parse flow rates
                flow1 = Double.parseDouble(values[0]);
                flow2 = Double.parseDouble(values[1]);
                flow3 = Double.parseDouble(values[2]);
                
                // Update UI with flow rates
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        statusTextView.setText(String.format("Flow Rates (L/min):\nPoint 1: %.2f\nPoint 2: %.2f\nPoint 3: %.2f",
                                flow1, flow2, flow3));
                    }
                });
                
                // Store data in JSON format
                storeDataAsJson(flow1, flow2, flow3);
                
                // Calculate pressures using Bernoulli's equation and plot them
                calculatePressuresAndPlot();
                
                // Detect leakage
                detectLeakage();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing data", e);
        }
    }
    
    /**
     * Store flow rate data as JSON
     */
    private void storeDataAsJson(double flow1, double flow2, double flow3) {
        try {
            JSONObject dataPoint = new JSONObject();
            dataPoint.put("timestamp", System.currentTimeMillis());
            dataPoint.put("flow1", flow1);
            dataPoint.put("flow2", flow2);
            dataPoint.put("flow3", flow3);
            
            // Calculate pressures
            double[] distances = {0, 20, 40}; // distances in meters
            double[] pressures = calculatePressures(flow1, flow2, flow3);
            
            // Add pressure data
            JSONArray pressureArray = new JSONArray();
            for (int i = 0; i < pressures.length; i++) {
                JSONObject pressurePoint = new JSONObject();
                pressurePoint.put("distance", distances[i]);
                pressurePoint.put("pressure", pressures[i]);
                pressureArray.put(pressurePoint);
            }
            dataPoint.put("pressures", pressureArray);
            
            // Add to history (limit to 100 points)
            historyData.put(dataPoint);
            if (historyData.length() > 100) {
                JSONArray newHistory = new JSONArray();
                for (int i = historyData.length() - 100; i < historyData.length(); i++) {
                    newHistory.put(historyData.get(i));
                }
                historyData = newHistory;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error storing JSON data", e);
        }
    }
    
    /**
     * Calculate pressures based on flow rates
     */
    private double[] calculatePressures(double flow1, double flow2, double flow3) {
        // Clear previous data
        pressurePoints.clear();
        
        // Convert flow rates from L/min to m^3/s
        double flow1_m3ps = flow1 / (60000);
        double flow2_m3ps = flow2 / (60000);
        double flow3_m3ps = flow3 / (60000);
        
        // Calculate velocities (m/s)
        double velocity1 = flow1_m3ps / PIPE_AREA;
        double velocity2 = flow2_m3ps / PIPE_AREA;
        double velocity3 = flow3_m3ps / PIPE_AREA;
        
        // Calculate dynamic pressures (kPa)
        double dynamicPressure1 = 0.5 * DENSITY * velocity1 * velocity1 / 1000; // divide by 1000 to convert to kPa
        double dynamicPressure2 = 0.5 * DENSITY * velocity2 * velocity2 / 1000;
        double dynamicPressure3 = 0.5 * DENSITY * velocity3 * velocity3 / 1000;
        
        // Assume some reasonable static pressures (kPa)
        double staticPressure1 = 150;
        double staticPressure2 = 130;
        double staticPressure3 = 110;
        
        // Calculate total pressures (kPa)
        double totalPressure1 = staticPressure1 + dynamicPressure1;
        double totalPressure2 = staticPressure2 + dynamicPressure2;
        double totalPressure3 = staticPressure3 + dynamicPressure3;
        
        // Store pressure points
        pressurePoints.add(totalPressure1);
        pressurePoints.add(totalPressure2);
        pressurePoints.add(totalPressure3);
        
        return new double[]{totalPressure1, totalPressure2, totalPressure3};
    }
    
    /**
     * Calculate pressures and update the chart
     */
    private void calculatePressuresAndPlot() {
        // Calculate pressures
        double[] pressures = calculatePressures(flow1, flow2, flow3);
        
        // Create entries for the chart
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, (float) pressures[0]));
        entries.add(new Entry(1, (float) pressures[1]));
        entries.add(new Entry(2, (float) pressures[2]));
        
        // Create dataset
        LineDataSet dataSet = new LineDataSet(entries, "Pressure (kPa)");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.BLUE);
        dataSet.setFillAlpha(50);
        dataSet.setDrawValues(true);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        
        // Set data to chart
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        
        // Refresh the chart
        chart.invalidate();
    }
    
    /**
     * Detect leakage based on pressure drop
     */
    private void detectLeakage() {
        if (pressurePoints.size() < 3) {
            return;
        }
        
        // A normal pressure line should have a steady negative slope
        // Leakage causes a sharper drop between points
        
        double pressureDrop1 = pressurePoints.get(0) - pressurePoints.get(1);
        double pressureDrop2 = pressurePoints.get(1) - pressurePoints.get(2);
        
        // Calculate the percentage difference between the pressure drops
        double difference = Math.abs(pressureDrop1 - pressureDrop2);
        double averageDrop = (pressureDrop1 + pressureDrop2) / 2;
        double percentDifference = (difference / averageDrop) * 100;
        
        // Store leak detection results in JSON
        try {
            JSONObject leakDetection = new JSONObject();
            leakDetection.put("timestamp", System.currentTimeMillis());
            leakDetection.put("pressureDrop1", pressureDrop1);
            leakDetection.put("pressureDrop2", pressureDrop2);
            leakDetection.put("percentDifference", percentDifference);
            
            // Add to most recent data point
            if (historyData.length() > 0) {
                JSONObject lastDataPoint = historyData.getJSONObject(historyData.length() - 1);
                lastDataPoint.put("leakDetection", leakDetection);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error adding leak detection to JSON", e);
        }
        
        // Determine leak locations
        boolean leak12 = false;
        boolean leak23 = false;
        
        // Threshold for detecting leakage (adjust as needed based on real data)
        double threshold = 15.0; // 15% difference threshold
        
        if (percentDifference > threshold) {
            if (pressureDrop1 > pressureDrop2) {
                leak12 = true;
            } else {
                leak23 = true;
            }
        }
        
        // Flow rate difference can also indicate leakage
        boolean flowRateDiff12 = Math.abs(flow1 - flow2) > 0.5; // 0.5 L/min threshold
        boolean flowRateDiff23 = Math.abs(flow2 - flow3) > 0.5;
        
        // Update leakage status
        final boolean finalLeak12 = leak12 || flowRateDiff12;
        final boolean finalLeak23 = leak23 || flowRateDiff23;
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (finalLeak12 && finalLeak23) {
                    leakageStatusTextView.setText("LEAKAGE DETECTED AT MULTIPLE POINTS");
                    leakageStatusTextView.setTextColor(Color.RED);
                } else if (finalLeak12) {
                    leakageStatusTextView.setText("LEAKAGE DETECTED BETWEEN POINTS 1 AND 2");
                    leakageStatusTextView.setTextColor(Color.RED);
                } else if (finalLeak23) {
                    leakageStatusTextView.setText("LEAKAGE DETECTED BETWEEN POINTS 2 AND 3");
                    leakageStatusTextView.setTextColor(Color.RED);
                } else {
                    leakageStatusTextView.setText("NO LEAKAGE DETECTED");
                    leakageStatusTextView.setTextColor(Color.GREEN);
                }
            }
        });
    }
    
    /**
     * Export data history as JSON string
     */
    public String exportDataHistory() {
        return historyData.toString();
    }
    
    /**
     * The Handler that gets information back from the BluetoothService
     */
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            statusTextView.setText("Connected to: " + connectedDeviceAddress);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            statusTextView.setText("Connecting...");
                            break;
                        case BluetoothService.STATE_NONE:
                            statusTextView.setText("Not connected");
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // Construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    processData(readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // Save the connected device's name
                    String deviceName = msg.obj.toString();
                    Toast.makeText(GraphActivity.this, "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(GraphActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}
