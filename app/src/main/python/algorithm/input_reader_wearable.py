import pandas as pd
import os

def get_wearable_csvfile():
    """
    Load the wearable input CSV file with semicolon delimiter
    Return first 10 rows of the dataframe and file path
    """
    # Build the file path relative to the Python script
    current_dir = os.path.dirname(__file__)  # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/algorithm
    parent_dir = os.path.dirname(current_dir)  # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app
    input_path = os.path.join(parent_dir, "input/", "wearable_input_raw.csv")

    print(f"current: {current_dir}")
    print(f"parent: {parent_dir}")
    print(f"Constructed wearable file path: {input_path}")

    # Check if the file exists at the constructed path
    if not os.path.exists(input_path):
        return f"Error: Wearable data file not found at {input_path}"
    
    try:
        # Read the CSV file with semicolon delimiter
        df = pd.read_csv(input_path, sep=';')
        # df['date_time'] = pd.to_datetime(df['date_time'], unit='ms')

        # Display only the most relevant columns for preview
        relevant_columns = ['date_time', 'bosch_gyr_x', 'bosch_gyr_y', 'bosch_gyr_z', 'stress_level', 'patient_id']
        
        # Check which columns actually exist in the file
        existing_columns = [col for col in relevant_columns if col in df.columns]
        
        if existing_columns:
            # If relevant columns exist, show only those
            df_preview = df[existing_columns].head(10)
        else:
            # Otherwise show all columns
            df_preview = df.head(10)
            
        print(df_preview)
        return df_preview, input_path
    
    except Exception as e:
        return f"Error reading wearable data file: {str(e)}"