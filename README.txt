# Pipeline Leak Detection App

An Android mobile app that connects to Arduino-based flowrate sensors via Bluetooth to detect oil pipeline leakages using Bernoulli's Equation.

## Overview

This application is designed to help detect leaks in pipelines by analyzing pressure differences between multiple flow rate sensors. The app connects to an Arduino-based hardware system via Bluetooth that has three flow rate sensors installed along a pipeline. By analyzing the flow rates and calculating pressure using Bernoulli's equation, the system can detect and locate leaks in the pipeline.

## Features

- Connect to Arduino-based hardware via Bluetooth
- Real-time monitoring of flow rates from three sensors
- Visualization of pressure data with MPAndroidChart library
- Automatic leak detection algorithm using Bernoulli's equation
- Visual representation of pipeline state and leak locations
- Historical data logging for past leak events
- Modern Material Design UI

## Screenshots

The app includes the following main screens:

1. **Main Screen** - Connection status and navigation options
2. **Graph Screen** - Real-time visualization of pressure data with leak detection
3. **State Screen** - Visual representation of the pipeline with sensor readings and leak indicators
4. **History Screen** - Log of past leak events with timestamps

## Technical Implementation

### Components

- **MainActivity**: Main entry point, manages Bluetooth connection
- **GraphActivity**: Displays real-time pressure graph using MPAndroidChart
- **StateActivity**: Shows visual representation of pipeline state
- **HistoryActivity**: Displays log of past leak events
- **BluetoothService**: Handles Bluetooth communication with Arduino
- **PipeLeakageDetector**: Core algorithm for leak detection

### Algorithm

The leak detection is based on Bernoulli's equation and pressure differences:

1. Flow rates are measured at three points along the pipeline
2. Pressure is calculated at each point using Bernoulli's equation
3. In normal conditions, pressure decreases linearly along the pipeline
4. Any significant deviation from the expected pressure gradient indicates a leak
5. The location of the deviation indicates the location of the leak

## Building the Project

### Prerequisites

- Android Studio 4.2+
- Android SDK 21+
- Arduino hardware with flow rate sensors (for actual usage)

### Steps

1. Clone this repository
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run on an Android device with Bluetooth capabilities

## Hardware Requirements

For the complete system, you will need:

- Arduino UNO or compatible board
- HC-05 or HC-06 Bluetooth module
- 3x Flow rate sensors compatible with Arduino
- Pipeline test setup

## Libraries Used

- **MPAndroidChart** - For graph visualization
- **AndroidX** - For modern Android components
- **org.json** - For data management



