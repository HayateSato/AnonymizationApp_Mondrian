# PythonCalculation: Mobile Data Anonymization Using Mondrian Method

## 📋 Project Overview
This Android application demonstrates data anonymization in a mobile environment using the Mondrian method implemented in Python. The project focuses on:
- Implementing data anonymization algorithms that can run directly on mobile devices
- Evaluating the impact on mobile resources (battery/CPU/RAM consumption)
- Achieving an optimal balance between data privacy and utility for machine learning models
- Supporting remote control via MQTT for automated testing and integration

## ✨ Features
- **CSV Data Processing**: Reads CSV files and displays content within the app
- **Mondrian Anonymization**: Implements the Mondrian algorithm in Python to anonymize sensitive data
- **Cross-Language Integration**: Seamlessly executes Python code from Java using Chaquopy
- **Local Processing**: All anonymization happens on-device, enhancing privacy
- **Multiple Dataset Support**: Works with both standard demographic data and wearable sensor data
- **Simple Data Masking**: Replaces sensitive identifiers with asterisks (****) for better privacy
- **Remote Control**: Accepts JSON commands via MQTT to trigger anonymization with specific parameters
- **Dataset Selection**: Allows users to choose between different datasets directly from the UI

## 🛠️ Technical Requirements
### Development Environment
- Android Studio Ladybug | 2024.2.1 Patch 3
- Gradle version 8.9
- JDK: JavaVersion.VERSION 11

### Android Configuration
- Compiled API Level: 34
- Minimum SDK: 31
- Target SDK: 31
- ABI filters: arm64-v8a, x86_64

### Python Environment
- Python 3.8 via Chaquopy (com.chaquo.python version 16.0.0)
- Dependencies:
  - NumPy
  - Pandas
  - Cryptography (for optional encryption features)
  - Built-in libraries: os, glob, base64
  - Custom package: algorithm.hierarchy_tree

### External Dependencies
- Eclipse Paho MQTT Client (org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5)
- Gson for JSON parsing (com.google.code.gson:gson:2.10.1)
- AndroidX Navigation Components

## 🚀 Getting Started

### Installation
1. Clone the repository:
   ```
   git clone https://github.com/HayateSato/AnonymizeMondrian_Python.git
   ```
2. Open the project in Android Studio
3. Sync Gradle and build the project
4. Run on an emulator or physical device with Android 12 (API 31) or higher

### Setting Up Python in Android

1. Add Chaquopy plugin to your project-level build.gradle:
   ```gradle
   plugins {
       // Your existing plugins
       id("com.chaquo.python") version "16.0.0" apply false
   }
   ```

2. Configure the app-level build.gradle:
   ```gradle
   plugins {
       // Your existing plugins
       id("com.chaquo.python")
   }
   
   android {
       defaultConfig {
           // Your existing config
           minSdk = 31
           // Other configurations
           
           python {
               // Python configuration options
           }
       }
   }
   ```

3. Configure Python dependencies in your app-level build.gradle:
   ```gradle
   python {
       pip {
           install "numpy"
           install "pandas"
           install "cryptography"
           // Other Python dependencies
       }
   }
   ```

## 📡 MQTT Integration

The app can be controlled remotely via MQTT messages to trigger anonymization with specific parameters. This is useful for automated testing or integration with other systems.

### MQTT Configuration

- Default broker: tcp://192.168.8.126:1883 (configurable in settings)
- Topic: anonymization/commands
- Message Format: JSON

### Message Format

Messages must be sent in JSON format with the following structure:

```json
{
  "kValue": 10,
  "dataset": "standard"
}
```

Where:
- `kValue`: The K value for anonymization (valid values: 2, 5, 10, 30, 50, 500)
- `dataset`: The dataset to use (valid values: "standard" or "wearable")

### Using the MQTT Sender Script

A Python script is provided to easily send MQTT commands to the app:

```
python mqtt_sender.py <k_value> [dataset_type]
```

Examples:
- `python mqtt_sender.py 10 wearable` - Anonymize wearable dataset with K=10
- `python mqtt_sender.py 5` - Anonymize standard dataset with K=5 (default)

## 📁 Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/                        # Java source code
│   │   │   ├── MainActivity.java        # Entry point and MQTT handler
│   │   │   ├── AnonymizationCommand.java # MQTT JSON message model
│   │   │   └── fragments/               # UI fragments
│   │   │       ├── AnonymizationFragment.java  # Handles anonymization
│   │   │       ├── DataFragment.java    # Displays CSV data
│   │   │       ├── HomeFragment.java    # Main navigation
│   │   │       └── SettingsFragment.java # App configuration
│   │   ├── python/                      # Python source code
│   │   │   └── algorithm/                # Anonymization algorithms
│   │   │       ├── hierarchy/           # Hierarchy trees for generalization
│   │   │       ├── hierarchy_tree.py    # Hierarchy tree implementation
│   │   │       ├── input_reader.py      # CSV file reader
│   │   │       ├── mondrian.py          # Mondrian anonymization algorithm
│   │   │       └── encryption.py        # Optional encryption utilities
│   │   ├── res/                         # Android resources
│   │   │   ├── layout/                  # UI layouts
│   │   │   ├── navigation/              # Navigation graphs
│   │   │   └── values/                  # Strings, colors, etc.
│   │   ├── assets/                      # CSV data files
│   │   └── AndroidManifest.xml          # App manifest
└── build.gradle.kts                     # App-level build configuration
```

## 🎯 Anonymization Workflow

1. **Dataset Selection**: Choose between standard demographic data or wearable sensor data
2. **K-Value Selection**: Select the desired K-anonymity level (2, 5, 10, 30, 50, or 500)
3. **Processing**: The app processes the data through the following steps:
   - Reading the CSV file
   - Applying data masking to identifiers (replacing with ****)
   - Building hierarchy trees for quasi-identifiers
   - Applying the Mondrian algorithm for K-anonymity
   - Checking if the result satisfies K-anonymity
   - Displaying the anonymized data

## 📊 Supported Datasets

### Standard Dataset
- Adult Census Income dataset with demographic information
- Quasi-identifiers: sex, age, race, marital-status, education, native-country, workclass, occupation
- Identifiers: ID, soc_sec_id, given_name, surname

### Wearable Dataset
- Sensor data from wearable devices with timestamps and measurements
- Quasi-identifiers: timestamp, acc_x, acc_y, acc_z, stress_level
- Identifiers: patient_id

## 📚 Resources
For more information on integrating Python with Android:
- [Chaquopy Documentation](https://chaquo.com/chaquopy/doc/current/android.html)
- [Python on Android Tutorial](https://www.youtube.com/watch?v=QFEu1KGHnzU)
- [Eclipse Paho MQTT Client](https://www.eclipse.org/paho/index.php?page=clients/android/index.php)

## 🔜 Future Enhancements
- Hardware impact measurement using external monitoring tools
- Additional anonymization algorithms for comparison
- Performance optimizations for large datasets
- Enhanced data visualization with graphs and charts
- Support for additional MQTT commands and parameters
- Real-time data streaming and anonymization

## 👤 Authors
- Hayate Sato

---
*Last Updated: May 13, 2025*
