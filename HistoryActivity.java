package com.pipelinedetector;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for viewing historical data
 */
public class HistoryActivity extends AppCompatActivity {
    private static final String TAG = "HistoryActivity";
    
    private ListView historyListView;
    private LineChart historyChart;
    private Button backButton;
    
    private JSONDataManager dataManager;
    private List<String> fileList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        // Initialize data manager
        dataManager = new JSONDataManager(this);
        
        // Set up UI elements
        historyListView = findViewById(R.id.historyListView);
        historyChart = findViewById(R.id.historyChart);
        backButton = findViewById(R.id.backButton);
        
        // Set up back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        // Set up list view
        fileList = dataManager.listDataFiles();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, getDisplayNames(fileList));
        historyListView.setAdapter(adapter);
        
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                loadHistoryData(fileList.get(position));
            }
        });
        
        // Set up the chart
        setupChart();
    }
    
    /**
     * Convert file names to display format
     */
    private List<String> getDisplayNames(List<String> fileList) {
        List<String> displayNames = new ArrayList<>();
        for (String filename : fileList) {
            // Parse timestamp from filename (format: pipeline_data_yyyyMMdd_HHmmss.json)
            try {
                String timestampStr = filename.replace("pipeline_data_", "").replace(".json", "");
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(timestampStr);
                displayNames.add(outputFormat.format(date));
            } catch (Exception e) {
                // If parsing fails, use the original filename
                displayNames.add(filename);
            }
        }
        return displayNames;
    }
    
    /**
     * Set up the chart
     */
    private void setupChart() {
        // General chart settings
        historyChart.getDescription().setEnabled(false);
        historyChart.setTouchEnabled(true);
        historyChart.setDragEnabled(true);
        historyChart.setScaleEnabled(true);
        historyChart.setPinchZoom(true);
        
        // X-Axis settings
        XAxis xAxis = historyChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Convert index to time or distance as needed
                return String.valueOf((int)value);
            }
        });
        
        // Left Y-Axis settings
        YAxis leftAxis = historyChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        
        // Right Y-Axis settings (disabled)
        historyChart.getAxisRight().setEnabled(false);
        
        // Initialize with empty data
        historyChart.setData(new LineData());
        historyChart.invalidate();
    }
    
    /**
     * Load historical data from a file
     */
    private void loadHistoryData(String filename) {
        try {
            // Load data
            JSONArray jsonData = dataManager.loadData(filename);
            
            // Check if data is valid
            if (jsonData.length() == 0) {
                Toast.makeText(this, "No data found in file", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Process data
            List<Entry> flowEntries1 = new ArrayList<>();
            List<Entry> flowEntries2 = new ArrayList<>();
            List<Entry> flowEntries3 = new ArrayList<>();
            List<Entry> pressureEntries = new ArrayList<>();
            
            for (int i = 0; i < jsonData.length(); i++) {
                JSONObject dataPoint = jsonData.getJSONObject(i);
                
                // Extract flow rates
                flowEntries1.add(new Entry(i, (float) dataPoint.getDouble("flow1")));
                flowEntries2.add(new Entry(i, (float) dataPoint.getDouble("flow2")));
                flowEntries3.add(new Entry(i, (float) dataPoint.getDouble("flow3")));
                
                // Extract pressure data if available
                if (dataPoint.has("pressures")) {
                    JSONArray pressures = dataPoint.getJSONArray("pressures");
                    for (int j = 0; j < pressures.length(); j++) {
                        JSONObject pressure = pressures.getJSONObject(j);
                        float x = (float) pressure.getDouble("distance");
                        float y = (float) pressure.getDouble("pressure");
                        pressureEntries.add(new Entry(x, y));
                    }
                }
            }
            
            // Create datasets
            LineDataSet flowSet1 = new LineDataSet(flowEntries1, "Flow Rate 1");
            flowSet1.setColor(android.graphics.Color.RED);
            flowSet1.setCircleColor(android.graphics.Color.RED);
            flowSet1.setCircleRadius(3f);
            
            LineDataSet flowSet2 = new LineDataSet(flowEntries2, "Flow Rate 2");
            flowSet2.setColor(android.graphics.Color.GREEN);
            flowSet2.setCircleColor(android.graphics.Color.GREEN);
            flowSet2.setCircleRadius(3f);
            
            LineDataSet flowSet3 = new LineDataSet(flowEntries3, "Flow Rate 3");
            flowSet3.setColor(android.graphics.Color.BLUE);
            flowSet3.setCircleColor(android.graphics.Color.BLUE);
            flowSet3.setCircleRadius(3f);
            
            // Create chart data
            LineData lineData = new LineData(flowSet1, flowSet2, flowSet3);
            
            // Update the chart
            historyChart.setData(lineData);
            historyChart.invalidate();
            
            // Show success message
            Toast.makeText(this, "Loaded data from " + filename, Toast.LENGTH_SHORT).show();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error loading data: " + e.getMessage());
            Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
