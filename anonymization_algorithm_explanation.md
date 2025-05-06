# Mobile Data Anonymization Algorithm: How It Works

This document explains how the Python algorithm in the PythonCalculation app anonymizes sensitive data using the Mondrian method.

## Overview of Files and Their Roles

### 1. input_reader.py
This file is responsible for loading CSV data files into the application. It:
- Takes a filename as input
- Constructs the proper file path relative to the application directory
- Reads the CSV file using pandas
- Returns the first 10 rows of the data (for preview) along with the file path

This serves as the entry point for getting data into the anonymization pipeline, making it easy to visualize a sample of the data before processing.

### 2. hierarchy_tree.py
This file manages the hierarchical relationships between data values that determine how data can be generalized. It contains:

- **HierarchyTreeNode class**: Represents a single node in a generalization hierarchy (e.g., "Doctor" is a child of "Professional" which is a child of "Employment")
- **HierarchyTree class**: Builds and manages complete hierarchies from CSV files
- **Functions to build trees**: Creates tree structures from the hierarchy definition CSV files

This hierarchy system is critical for data anonymization as it defines the "generalization pathways" - how specific values can be replaced with more general categories while preserving meaning.

### 3. mondrian.py
This is the core of the anonymization process, implementing the Mondrian algorithm for k-anonymity. Key functions include:

- **summarized()**: Replaces specific values with ranges or generalized values
- **anonymize()**: Recursively partitions data until each partition meets k-anonymity requirements
- **mondrian()**: The main algorithm implementation that determines how to best partition the data
- **map_text_to_num()** and **map_num_to_text()**: Convert between original values and numerical IDs used in the algorithm
- **check_k_anonymity()**: Verifies the anonymized data meets the k-anonymity requirement
- **anonymize_execute()**: The main entry point called from Java that orchestrates the entire anonymization process

### 4. hierarchy/ (directory)
Contains CSV files that define the generalization hierarchies for different attributes:
- adult_hierarchy_age.csv
- adult_hierarchy_education.csv
- adult_hierarchy_marital-status.csv
- And others...

These files define how specific values (like "PhD") can be generalized to broader categories (like "Higher Education").

## How The Anonymization Process Works

### Step 1: Data Loading
The process begins when `anonymize_execute()` is called from the Java application with a k-value parameter. The function:
- Sets up directory paths for input and output
- Defines which columns are quasi-identifiers (QI) that can identify individuals (e.g., age, sex, race)
- Defines which columns are direct identifiers (e.g., ID, social security number)
- Loads the CSV data using pandas

### Step 2: Identity Protection (Optional)
If a password is provided, the system:
- Generates an encryption key from the password
- Encrypts direct identifiers (like names, SSNs) using Fernet encryption
- Preserves the encrypted identifiers in the dataset

### Step 3: Hierarchy Loading
The system loads hierarchical relationships from CSV files:
- Each attribute (like "education") has a hierarchy defining generalization levels
- Hierarchies are loaded into HierarchyTree objects
- These determine how values can be replaced with more general categories

### Step 4: Data Preparation
Before anonymization:
- Text values are converted to numeric IDs using the hierarchy trees
- Missing values are handled by filling with default values
- The data is now ready for the Mondrian algorithm

### Step 5: Mondrian Algorithm Execution
The key steps of the Mondrian algorithm are:
1. Determine which quasi-identifier has the most unique values
2. Sort the data by that attribute
3. Split the data into two equal partitions
4. Recursively apply the algorithm to each partition until k-anonymity is achieved in each partition
5. When a partition cannot be split further while maintaining k-anonymity, generalize values in that partition

This recursive partitioning approach seeks to preserve as much data utility as possible while ensuring privacy.

### Step 6: K-Anonymity Verification
After anonymization:
- The system checks if all partitions contain at least k records with the same quasi-identifier values
- If any partition fails this test, an exception is raised

### Step 7: Data Restoration
Once anonymization is complete:
- Numeric IDs are converted back to meaningful text values using the hierarchy trees
- Where values were generalized, the common ancestor in the hierarchy is used (e.g., replacing "PhD" and "Masters" with "Graduate Degree")

### Step 8: Output and Verification
Finally:
- The anonymized data is saved to a CSV file
- Execution time is reported
- A sample of the anonymized data is returned for display in the app

## How the Files Interact

1. **Java MainActivity** calls `anonymize_execute()` from **mondrian.py** with a k-value parameter
2. **mondrian.py** loads the input data and calls **hierarchy_tree.py** to build generalization hierarchies
3. **hierarchy_tree.py** processes the CSV files in the **hierarchy/** directory to create hierarchical structures
4. **mondrian.py** uses these hierarchies to determine how to generalize data while preserving meaning
5. The anonymized data is returned to the Java application for display and saved as a CSV file

## Technical Notes

- **Time Complexity**: The algorithm has an overall time complexity of O(n log n), making it efficient for moderately sized datasets
- **Encryption**: Uses PBKDF2 with SHA256 for key derivation and Fernet for symmetric encryption
- **Hierarchy Management**: Uses a tree structure where each node represents a level of generalization
- **Parallelization**: The recursive nature of the algorithm could potentially be parallelized for larger datasets

This implementation of Mondrian ensures that any group of records with the same quasi-identifier values contains at least k records, making it difficult to re-identify individuals while preserving data utility for analysis.
