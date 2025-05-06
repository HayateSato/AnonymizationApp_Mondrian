# PythonCalculation: Mobile Data Anonymization Using Mondrian Method

## 📋 Project Overview
This Android application demonstrates data anonymization in a mobile environment using the Mondrian method implemented in Python. The project focuses on:
- Implementing data anonymization algorithms that can run directly on mobile devices
- Evaluating the impact on mobile resources (battery/CPU/RAM consumption)
- Achieving an optimal balance between data privacy and utility for machine learning models

## ✨ Features
- **CSV Data Processing**: Reads CSV files and displays content within the app
- **Mondrian Anonymization**: Implements the Mondrian algorithm in Python to anonymize sensitive data
- **Cross-Language Integration**: Seamlessly executes Python code from Java using Chaquopy
- **Local Processing**: All anonymization happens on-device, enhancing privacy
- **Data Visualization**: Displays both original and anonymized data for comparison

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
  - Built-in libraries: os, glob, base64
  - Custom package: algorithm.hierarchy_tree

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
           // Other Python dependencies
       }
   }
   ```

## 📁 Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/           # Java source code
│   │   │   └── MainActivity.java  # Entry point calling Python code
│   │   ├── python/         # Python source code
│   │   │   └── mondrian/   # Mondrian algorithm implementation
│   │   ├── assets/         # CSV data files
│   │   └── res/            # Android resources
└── build.gradle.kts        # App-level build configuration
```

## 📚 Resources
For more information on integrating Python with Android:
- [Chaquopy Documentation](https://chaquo.com/chaquopy/doc/current/android.html)
- [Python on Android Tutorial](https://www.youtube.com/watch?v=QFEu1KGHnzU)

## 🔜 Future Enhancements
- Hardware impact measurement using external monitoring tools
- Additional anonymization algorithms for comparison
- Performance optimizations for large datasets
- UI/UX improvements for better data visualization

[//]: # (## 📄 License)

[//]: # ([Your license information here])

## 👤 Authors
- Hayate Sato

---
*Last Updated: May 2025*
