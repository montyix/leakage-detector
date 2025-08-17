package com.pipelinedetector;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Class for managing pipeline data using JSON format
 */
public class JSONDataManager {
    private static final String TAG = "JSONDataManager";
    private static final String FILE_NAME_PREFIX = "pipeline_data_";
    private static final String FILE_EXTENSION = ".json";
    
    private Context context;
    
    public JSONDataManager(Context context) {
        this.context = context;
    }
    
    /**
     * Save sensor data to a JSON file
     * 
     * @param data JSON array containing sensor data
     * @return true if save was successful, false otherwise
     */
    public boolean saveData(JSONArray data) {
        try {
            // Create a timestamped filename
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String filename = FILE_NAME_PREFIX + timestamp + FILE_EXTENSION;
            
            // Write data to file
            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(data.toString(2).getBytes());
            fos.close();
            
            Log.d(TAG, "Data saved successfully to " + filename);
            return true;
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error saving data: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Load data from a specific file
     * 
     * @param filename Name of the file to load
     * @return JSONArray containing the loaded data
     */
    public JSONArray loadData(String filename) {
        try {
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            bufferedReader.close();
            
            return new JSONArray(sb.toString());
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error loading data: " + e.getMessage());
            return new JSONArray();
        }
    }
    
    /**
     * List all saved data files
     * 
     * @return List of filenames
     */
    public List<String> listDataFiles() {
        List<String> fileList = new ArrayList<>();
        File directory = context.getFilesDir();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith(FILE_NAME_PREFIX) && file.getName().endsWith(FILE_EXTENSION)) {
                    fileList.add(file.getName());
                }
            }
        }
        return fileList;
    }
    
    /**
     * Delete a data file
     * 
     * @param filename Name of the file to delete
     * @return true if file was deleted successfully, false otherwise
     */
    public boolean deleteData(String filename) {
        File file = new File(context.getFilesDir(), filename);
        return file.delete();
    }
    
    /**
     * Create a JSON object for a flow reading
     * 
     * @param flow1 Flow rate at point 1
     * @param flow2 Flow rate at point 2
     * @param flow3 Flow rate at point 3
     * @return JSONObject containing flow data
     */
    public static JSONObject createFlowData(double flow1, double flow2, double flow3) {
        try {
            JSONObject data = new JSONObject();
            data.put("timestamp", System.currentTimeMillis());
            data.put("flow1", flow1);
            data.put("flow2", flow2);
            data.put("flow3", flow3);
            return data;
        } catch (JSONException e) {
            Log.e(TAG, "Error creating flow data: " + e.getMessage());
            return new JSONObject();
        }
    }
    
    /**
     * Create a JSON object for a leakage detection result
     * 
     * @param leakageBetween12 Whether there's leakage between points 1 and 2
     * @param leakageBetween23 Whether there's leakage between points 2 and 3
     * @param pressureDrop1 Pressure drop between points 1 and 2
     * @param pressureDrop2 Pressure drop between points 2 and 3
     * @return JSONObject containing leakage detection data
     */
    public static JSONObject createLeakageData(boolean leakageBetween12, boolean leakageBetween23, 
                                             double pressureDrop1, double pressureDrop2) {
        try {
            JSONObject data = new JSONObject();
            data.put("timestamp", System.currentTimeMillis());
            data.put("leakage_between_1_2", leakageBetween12);
            data.put("leakage_between_2_3", leakageBetween23);
            data.put("pressure_drop_1_2", pressureDrop1);
            data.put("pressure_drop_2_3", pressureDrop2);
            return data;
        } catch (JSONException e) {
            Log.e(TAG, "Error creating leakage data: " + e.getMessage());
            return new JSONObject();
        }
    }
    
    /**
     * Extract flow rates from a JSON array
     * 
     * @param jsonArray JSONArray containing data
     * @return List of arrays containing [flow1, flow2, flow3] values
     */
    public static List<double[]> extractFlowRates(JSONArray jsonArray) {
        List<double[]> flowRates = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject dataPoint = jsonArray.getJSONObject(i);
                double flow1 = dataPoint.getDouble("flow1");
                double flow2 = dataPoint.getDouble("flow2");
                double flow3 = dataPoint.getDouble("flow3");
                flowRates.add(new double[]{flow1, flow2, flow3});
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error extracting flow rates: " + e.getMessage());
        }
        return flowRates;
    }
    
    /**
     * Extract pressures from a JSON array
     * 
     * @param jsonArray JSONArray containing data
     * @return List of arrays containing pressure values at each point
     */
    public static List<double[]> extractPressures(JSONArray jsonArray) {
        List<double[]> pressures = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject dataPoint = jsonArray.getJSONObject(i);
                if (dataPoint.has("pressures")) {
                    JSONArray pressureArray = dataPoint.getJSONArray("pressures");
                    double[] pressureValues = new double[pressureArray.length()];
                    for (int j = 0; j < pressureArray.length(); j++) {
                        JSONObject pressurePoint = pressureArray.getJSONObject(j);
                        pressureValues[j] = pressurePoint.getDouble("pressure");
                    }
                    pressures.add(pressureValues);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error extracting pressures: " + e.getMessage());
        }
        return pressures;
    }
}
