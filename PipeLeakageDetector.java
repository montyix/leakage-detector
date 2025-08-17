package com.pipelinedetector;

/**
 * Main class that integrates the components of the pipeline leakage detection system
 * Note: This is a conceptual class to demonstrate the system architecture
 */
public class PipeLeakageDetector {
    // System constants
    private static final double PIPE_DIAMETER = 0.02; // 2cm diameter pipe in meters
    private static final double PIPE_RADIUS = PIPE_DIAMETER / 2.0;
    private static final double PIPE_AREA = Math.PI * PIPE_RADIUS * PIPE_RADIUS; // Cross-sectional area in m²
    private static final double WATER_DENSITY = 1000.0; // kg/m³
    private static final double GRAVITY = 9.81; // m/s²
    
    // Flow rate thresholds
    private static final double FLOW_RATE_DIFFERENCE_THRESHOLD = 0.5; // L/min
    
    /**
     * Calculate pressure at a point using Bernoulli's equation
     * 
     * @param flowRate Flow rate in L/min
     * @param height Height at the point in meters
     * @param referenceHeight Reference height for potential energy calculation
     * @return Pressure in kPa
     */
    public static double calculatePressure(double flowRate, double height, double referenceHeight) {
        // Convert flow rate from L/min to m³/s
        double flowRateM3ps = flowRate / 60000.0;
        
        // Calculate velocity (m/s)
        double velocity = flowRateM3ps / PIPE_AREA;
        
        // Calculate dynamic pressure component (Pa)
        double dynamicPressure = 0.5 * WATER_DENSITY * velocity * velocity;
        
        // Calculate potential energy component (Pa)
        double potentialEnergy = WATER_DENSITY * GRAVITY * (referenceHeight - height);
        
        // Calculate total pressure (Pa) and convert to kPa
        return (dynamicPressure + potentialEnergy) / 1000.0;
    }
    
    /**
     * Detect leakage in pipeline section based on flow rates
     * 
     * @param flowRate1 Flow rate at point 1 in L/min
     * @param flowRate2 Flow rate at point 2 in L/min
     * @return true if leakage detected, false otherwise
     */
    public static boolean detectLeakage(double flowRate1, double flowRate2) {
        return Math.abs(flowRate1 - flowRate2) > FLOW_RATE_DIFFERENCE_THRESHOLD;
    }
    
    /**
     * Calculate pressure drop between two points
     * 
     * @param pressure1 Pressure at point 1 in kPa
     * @param pressure2 Pressure at point 2 in kPa
     * @return Pressure drop in kPa
     */
    public static double calculatePressureDrop(double pressure1, double pressure2) {
        return pressure1 - pressure2;
    }
    
    /**
     * Analyze pressure drop pattern to detect leakage
     * 
     * @param pressureDropSection1 Pressure drop in section 1-2
     * @param pressureDropSection2 Pressure drop in section 2-3
     * @param threshold Percentage difference threshold
     * @return 1 for leak in section 1-2, 2 for leak in section 2-3, 0 for no leak, 3 for multiple leaks
     */
    public static int analyzePressureDropPattern(double pressureDropSection1, double pressureDropSection2, double threshold) {
        // Calculate percentage difference
        double avgDrop = (pressureDropSection1 + pressureDropSection2) / 2.0;
        double difference = Math.abs(pressureDropSection1 - pressureDropSection2);
        double percentDifference = (difference / avgDrop) * 100.0;
        
        if (percentDifference <= threshold) {
            return 0; // No leak detected
        }
        
        // Check which section has the higher pressure drop
        if (pressureDropSection1 > pressureDropSection2 * (1 + threshold/100.0)) {
            return 1; // Leak in section 1-2
        } else if (pressureDropSection2 > pressureDropSection1 * (1 + threshold/100.0)) {
            return 2; // Leak in section 2-3
        } else {
            return 3; // Multiple leaks or system error
        }
    }
}
