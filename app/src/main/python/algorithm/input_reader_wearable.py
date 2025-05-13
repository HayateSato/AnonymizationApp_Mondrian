import pandas as pd
import os
import datetime

def get_wearable_csvfile():
    """
    Load the wearable input CSV file with semicolon delimiter
    Convert timestamps to human-readable format
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
        # First, read without conversion to analyze the timestamp format
        df = pd.read_csv(input_path, sep=';')
        
        # Check if timestamp column exists
        if 'timestamp' in df.columns:
            # Print a sample of raw timestamp values for debugging
            print("Raw timestamp samples:")
            print(df['timestamp'].head())
            
            # Convert the timestamp column format
            # First, replace comma with period in scientific notation
            df['timestamp'] = df['timestamp'].astype(str).str.replace(',', '.')
            
            # Convert to float, then to integer
            df['timestamp'] = df['timestamp'].astype(float).astype('int64')
            
            # Determine if timestamp is in seconds or milliseconds
            # If timestamps are very large (> 10^12), they're likely in milliseconds
            if df['timestamp'].iloc[0] > 10**12:
                # Convert milliseconds to seconds
                df['timestamp_seconds'] = df['timestamp'] / 1000
            else:
                df['timestamp_seconds'] = df['timestamp']
            
            # Convert to human readable datetime
            df['datetime'] = pd.to_datetime(df['timestamp_seconds'], unit='s')
            
            # Create a readable format
            df['time'] = df['datetime'].dt.strftime('%Y-%m-%d %H:%M:%S')
            
            print("Converted timestamps:")
            print(df[['timestamp', 'time']].head())
        
        # Display only the most relevant columns for preview
        relevant_columns = ['time', 'acc_x', 'acc_y', 'acc_z', 'stress_level', 'patient_id']
        
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
        error_message = f"Error reading wearable data file: {str(e)}"
        print(error_message)
        # Add more debug info
        try:
            # Try to read the first few lines directly
            with open(input_path, 'r', encoding='utf-8') as f:
                first_lines = ''.join([next(f) for _ in range(5)])
                print(f"First few lines of file:\n{first_lines}")
        except Exception as read_error:
            print(f"Error reading file directly: {str(read_error)}")
        
        return error_message