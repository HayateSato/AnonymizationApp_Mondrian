import pandas as pd
import os

def get_csvfile(filename):
    """
    input = csv file
    the input will be converted to pandas dataframe
    output = first 3 rows of the dataframe and file path
    """

    # Build the file path relative to the Python script
    current_dir = os.path.dirname(__file__)  # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/algorithm
    parent_dir = os.path.dirname(current_dir)  # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app
    input_path = os.path.join(parent_dir, "input/", filename) # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/input/dataset.csv

    print(f"current: {current_dir}")
    print(f"parent: {parent_dir}")
    print(f"Constructed file path: {input_path}") # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/dataset.csv

    # Check if the file exists at the constructed path
    if not os.path.exists(input_path):
        return f"Error: File not found at {input_path}"
    try:
        df = pd.read_csv(input_path)

        df_short = df.head(10)
        print(df_short)

        return df_short, input_path
    except Exception as e:
        return f"Error reading file: {str(e)}"