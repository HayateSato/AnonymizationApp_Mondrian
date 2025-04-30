# Anonymization in Python (Mondrian Method)

## Project Overview
Mobile Anonymization with mondrian method
  - Implementing anonymization in mobile environement and examining the impact on the mobile device (battery/CPU/RAM consumption).
  - Final goal is to achieve the good balacance between data privacy and data utiliy to feed to machine learning model.

## Features
Building an android app that 
  - Reads a csv file and display a part of the content
  - Anonymizes the input and the result is stored inside the app.
  - The used algorithm is Mondrian method and written in python.
  - The algorithm is called inside MainActiviy.java, hence Java.
  - Examination of the hardware impact is not yet measured. (requires external app)

## Reuqirments 
The detail is documented in `requirments.txt`. 
  -  Android Studio Ladybug | 2024.2.1 Patch 3
  -  Compiled API Level: 34
  -  com.chaquo.python: version 16.0.0 
  -  Python 3.8
  -  JavaVersion.VERSION 11
  -  Gradle version 8.9
  -  ABI filders arm64-v8a, x86_64

## Getting Started / Installation 
1. CLine the repo:
```
https://github.com/HayateSato/AnonymizeMondrian_Python.git
```
2. Navigate to project direcoty in your local machine:
```   
 cd AnonymizeMondrian_Python
```
3. 

## Getting Python running inside Android 
helpful instructions. 
- https://chaquo.com/chaquopy/doc/current/android.html
- https://www.youtube.com/watch?v=QFEu1KGHnzU

1. Open Project level build.gradle
```
plugins {
    --- below your existing code ---
    id("com.chaquo.python") version "16.0.0" apply false
}
```
 
2. Open App level build.grandle (Plugins)
```
plugins {
    --- below your existing code ---
    id("com.chaquo.python")
}
```

3. Stay in App level build.grandle (minSdk)
```
    defaultConfig {
    --- below your existing code ---
        minSdk = 31   <--- this number needs to be higher than 24
        targetSdk = 31
        versionCode = 1
    --- avbove your existing code ---
}
```



## Project Stucture

![image](https://github.com/user-attachments/assets/01724f82-7fa4-4ad0-b865-ad37a77c775f)







## Setting-up Python Environment 




Since algorithm is written in Python, we need to set up an environment where we can run Python inside an Android Phone. TO do so, following 
- Setting-Up Python Environemnt 
- plugins {
    alias(libs.plugins.android.application) apply false
    id("com.chaquo.python") version "16.0.0" apply false
}
