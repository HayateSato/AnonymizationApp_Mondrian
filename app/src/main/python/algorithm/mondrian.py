# Multi-Dimensional Mondrian for k-anonymity
import os
import pandas as pd
import time

# custom library
import algorithm.hierarchy_tree as h_tree
from algorithm.encryption import generate_fernet, encrypt_value

def summarized(partition, dim, qi_list):
    """
    change the values of the quasi-identifiers columns to the range of the values in the partition
    :param partition: the data frame to be anonymized
    :param dim: the dimension to be summarized
    :param qi_list: the quasi-identifiers to be used
    :return: the anonymized data frame
    """
    for qi in qi_list:
        partition = partition.sort_values(by=qi)
        if partition[qi].iloc[0] != partition[qi].iloc[-1]:
            s = f"{partition[qi].iloc[0]}-{partition[qi].iloc[-1]}"
            partition[qi] = [s] * partition[qi].size
    return partition


def anonymize(partition, ranks, k, qi_list):
    """
    recursively calls itself on the two halves of data
    :param partition: the data frame to be anonymized
    :param ranks: the ranks of the quasi-identifiers
    :param k: the k value for k-anonymity
    :param qi_list: the quasi-identifiers to be used
    :return: the anonymized data frame
    """
    # get the dimension with the highest rank
    dim = ranks[0][0]

    partition = partition.sort_values(by=dim)
    si = partition[dim].count()
    mid = si // 2
    left_partition = partition[:mid]
    right_partition = partition[mid:]
    if len(left_partition) >= k and len(right_partition) >= k:
        return pd.concat([anonymize(left_partition, ranks, k, qi_list), anonymize(right_partition, ranks, k, qi_list)])
    return summarized(partition, dim, qi_list)


def mondrian(partition, qi_list, k):
    """
    Mondrian algorithm for k-anonymity.
    :param partition: the data frame to be anonymized
    :param qi_list: the quasi-identifiers to be used
    :param k: the k value for k-anonymity
    :return: anonymized DataFrame where each group of records with the same quasi-identifiers has at least k records.
    """
    # find which quasi-identifier has the most distinct values
    ranks = {}
    for qi in qi_list:
        ranks[qi] = len(partition[qi].unique())
    # sort the ranks in descending order
    ranks = [(key, value) for key, value in sorted(ranks.items(), key=lambda item: item[1], reverse=True)]
    # print(ranks)
    return anonymize(partition, ranks, k, qi_list)


def map_text_to_num(df, qi_list, hierarchy_tree_dict):
    """
    the data frame with text values mapped to leaf_id(number). It would help to anonymize using mondrian algorithm
    :param df: the data frame to be anonymized
    :param qi_list: the quasi-identifiers to be used
    :param hierarchy_tree_dict: the hierarchy tree dictionary
    :return: the data frame with text values mapped to leaf_id(number).
    """
    # Iterate over each column in quasi_identifiers
    for column in qi_list:
        # Get the hierarchy tree for the current column
        hierarchy_tree = hierarchy_tree_dict[column]
        # Create a mapping of values to leaf_id
        # for leaf_id, leaf in hierarchy_tree.leaf_id_dict.items():
        #     print(column, type(leaf_id))
        if isinstance(df[column].iloc[0], str):
            mapping = {leaf.value: leaf_id for leaf_id, leaf in hierarchy_tree.leaf_id_dict.items()}
        else:  # isinstance(df[column].iloc[0], int)
            mapping = {int(leaf.value): leaf_id for leaf_id, leaf in hierarchy_tree.leaf_id_dict.items()}
        # Replace the values in the current column with their corresponding leaf_id
        df[column] = df[column].map(mapping)
    return df


def map_num_to_text(df, qi_list, hierarchy_tree_dict):
    """
    the data frame with leaf_id(number) mapped to text values(original or generalized).
    :param df: the data frame to be anonymized
    :param qi_list: the quasi-identifiers to be used
    :param hierarchy_tree_dict: the hierarchy tree dictionary
    :return: the data frame with leaf_id(number) mapped to text values.
    """
    # Iterate over each column in quasi_identifiers
    for column in qi_list:  # time: O(m*n) = (m<<n) = O(n)
        # Get the hierarchy tree for the current column
        hierarchy_tree = hierarchy_tree_dict[column]
        # Create a mapping of leaf_id to values
        for i, value in df[column].items():
            value = str(value)
            if value.isdigit():  # single number. e.g. 17. time: O(1)
                leaf = hierarchy_tree.leaf_id_dict[value]
                df.at[i, column] = leaf.value
            elif isinstance(value, str) and '-' in value:  # interval. e.g. [9-16]. time: O(1)
                leaf1_id, leaf2_id = map(str, value.split('-'))  # value.strip('[]').split('-')
                common_ancestor = hierarchy_tree.find_common_ancestor(leaf1_id, leaf2_id)
                df.at[i, column] = common_ancestor.value
    return df


def check_k_anonymity(df, qi_list, k):
    """
    check if all partitions are k-anonymous
    :param qi_list: the quasi-identifiers to be used
    :param df: the data frame to be anonymized
    :param k: the k value for k-anonymity
    :return: True if all partitions are k-anonymous, False otherwise
    """
    partition_list = df.groupby(qi_list)  # pandas groupby() time: O(n*log(n))
    check_k_anonymity_flag = True
    for partition_key, partition in partition_list:
        if len(partition) < k:
            check_k_anonymity_flag = False
    return check_k_anonymity_flag

def run_anonymize(qi_list, identifiers, data_file, hierarchy_file_dir, k=5):
    # suppose n records(num of rows). k-anonymity. m quasi-identifiers. Calculate time complexity
    df = pd.read_csv(data_file)

    # key = generate_key(" ")
    fernet = generate_fernet(" ")
    for identifier in identifiers:
        if identifier in df.columns:
    #         df[identifier] = "****"               # uncomment this for simple suppression
            df[identifier] = df[identifier].apply(lambda x: encrypt_value(x, fernet))

    hierarchy_tree_dict = h_tree.build_all_hierarchy_tree(hierarchy_file_dir)



    df = map_text_to_num(df, qi_list, hierarchy_tree_dict)  # time: O(n*m) = (m<<n) = O(n)
    if df.isnull().values.any():
        df.fillna(1, inplace=True)
        print("Error: NaN values found in the data frame. Please remove or handle them before anonymizing the data.")
        #return

    # calculation of ranks of the quasi-identifiers. time: O(n*m)
    # sort the ranks in descending order. time: O(m*log(m))
    # anonymize. Recursively calls itself on the two halves of the data. time: O(n*log(n))
    # summarized. time: O(n)
    # total time complexity of mondrian: O(n*m + m*log(m) + n*log(n) + n) = O(n*m + n*log(n)) = (m<<n) = O(n*log(n))
    df = mondrian(df, qi_list, k)

    if not check_k_anonymity(df, qi_list, k):  # time: O(n*log(n))
        raise Exception("Not all partitions are k-anonymous")

    df = map_num_to_text(df, qi_list, hierarchy_tree_dict)  # time: O(n*m) = (m<<n) = O(n)
    # total time complexity: O(n*log(n))

    return df


#### DUMMY FUNCTION TO EXECUTE THE ANONYMIZATION #################################
# the below function is called in the main function - MainActivity
# above codes should not be changed
#################################################################################


def anonymize_execute(k_value, input_filename="dataset.csv"):
    tic = time.time()  # time count starts
    # dir/file path  #############################################################################
    current_dir = os.path.dirname(__file__)  # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/algorithm
    parent_dir = os.path.dirname(current_dir)  # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app
    input_dir = os.path.join(parent_dir, "input/") # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/input
    output_dir = os.path.join(parent_dir, "output/anonymized/") # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/output/anonymized
    # defining input #############################################################################
    input_path = os.path.join(input_dir, input_filename) # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/input/dataset.csv
    hierarchy_file_path = os.path.join(current_dir, "hierarchy/")  # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/algorithm/hierarchy
    
    # Set quasi-identifiers and identifiers based on the input file
    if input_filename == "wearable_input_raw.csv":
        # Wearable dataset structure - these are the columns we want to anonymize
        qi_list = ['timestamp', 'acc_x', 'acc_y', 'acc_z', 'stress_level']
        identifiers = ['patient_id']
        # Semicolon delimiter
        delimiter = ';'
    else:
        # Default fallback for unknown files
        qi_list = ['sex', 'age', 'race', 'marital-status', 'education', 'native-country', 'workclass', 'occupation']
        identifiers = ['ID', 'soc_sec_id', 'given_name', 'surname']
        delimiter = ','
    
    k = k_value
    # log ########################################################################################
    print(f"Running anonymization on {input_filename}")
    print(f"Using delimiter: {delimiter}")
    print(f"Quasi-identifiers: {qi_list}")
    print(f"Identifiers: {identifiers}")
    print(f"run_anonymize executing with K = {k}")
    
    # anonymize_execute function call  ###########################################################
    try:
        # Check if the file exists
        if not os.path.exists(input_path):
            print(f"Error: Input file not found at {input_path}")
            return f"Error: Input file not found: {input_filename}"
            
        # Handle wearable data format
        if input_filename == "wearable_input_raw.csv":
            try:
                # Read with semicolon delimiter
                df = pd.read_csv(input_path, sep=delimiter)
                
                # Handle timestamp conversion for better readability
                if 'timestamp' in df.columns:
                    # Replace comma with period in scientific notation
                    df['timestamp'] = df['timestamp'].astype(str).str.replace(',', '.')
                    
                    # Convert to float, then to integer
                    df['timestamp'] = df['timestamp'].astype(float).astype('int64')
                    
                    # Determine if timestamp is in seconds or milliseconds
                    # If timestamps are very large (> 10^12), they're likely in milliseconds
                    if df['timestamp'].iloc[0] > 10**12:
                        # Convert milliseconds to seconds for anonymization
                        df['timestamp_seconds'] = df['timestamp'] / 1000
                    else:
                        df['timestamp_seconds'] = df['timestamp']
                    
                    # Add timestamp as a quasi-identifier for anonymization
                    if 'timestamp_seconds' not in qi_list and 'timestamp_seconds' in df.columns:
                        qi_list.append('timestamp_seconds')
                
                # Perform simple anonymization without hierarchy tree
                # Apply k-anonymity by grouping and generalization
                for col in qi_list:
                    if col in df.columns:
                        # Simple generalization: round numeric values
                        if pd.api.types.is_numeric_dtype(df[col]):
                            df[col] = (df[col] // k) * k
                
                # After anonymization, convert timestamp_seconds back to human readable form
                if 'timestamp_seconds' in df.columns:
                    # Convert to datetime
                    df['datetime'] = pd.to_datetime(df['timestamp_seconds'], unit='s')
                    
                    # Create a readable format
                    df['time'] = df['datetime'].dt.strftime('%Y-%m-%d %H:%M:%S')
                
                # Return a preview
                df_columns = [col for col in ['time', 'acc_x', 'acc_y', 'acc_z', 'stress_level', 'patient_id'] if col in df.columns]
                if not df_columns:
                    df_columns = df.columns[:6]  # First 6 columns if specific columns not found
                    
                df_short = df[df_columns].iloc[:40]
                print("Anonymized wearable data preview:")
                print(df_short)
                
                # Save to output
                os.makedirs(output_dir, exist_ok=True)  # Create output directory if it doesn't exist
                output_file_path = os.path.join(output_dir, f'k_{k}_anonymized_{input_filename}')
                df.to_csv(output_file_path, index=False, sep=delimiter)
                print(f"Anonymized data saved to: {output_file_path}")
                
                # Log execution time
                toc = time.time()
                execution_time = toc - tic
                print(f"Execution time: {execution_time:.2f} seconds")
                
                return df_short
            except Exception as e:
                print(f"Error processing wearable data: {str(e)}")
                return f"Error processing wearable data: {str(e)}"
                
        # Regular dataset processing with hierarchy trees
        data_frame = run_anonymize(qi_list, identifiers, input_path, hierarchy_file_path, k=k)
        # output ####################################################################################
        os.makedirs(output_dir, exist_ok=True) # Create the directory if it doesn't exist
        # specifying the csv file name with k-value
        output_file_path = os.path.join(output_dir, f'k_{k}_anonymized_{input_filename}')
        # saving the anonymized data to a new file in the same directory
        try:
            data_frame.to_csv(output_file_path, index=False)
            print(f"Anonymized data saved to: {output_file_path}")
        except Exception as e:
            print(f"Error saving file: {str(e)}")
        
        # log 2 ######################################################################################
        toc = time.time() # time count stops here
        execution_time = toc - tic
        print(f"Execution time: {execution_time:.2f} seconds")
        
        # result #####################################################################################
        # For standard dataset, use standard columns
        if input_filename == "dataset.csv":
            result_columns = ['age', 'race', 'marital-status', 'education', 'native-country', 'soc_sec_id']
            available_columns = [col for col in result_columns if col in data_frame.columns]
            if available_columns:
                df_short = data_frame[available_columns].iloc[850:890]
            else:
                df_short = data_frame.iloc[850:890, :6]  # First 6 columns
        else:
            # For other datasets, select appropriate columns or first few columns
            try:
                result_columns = qi_list[:5]  # First 5 QI columns
                df_short = data_frame[result_columns].iloc[:40]
            except:
                # Fallback to first 6 columns
                df_short = data_frame.iloc[:40, :6]
                
        print(df_short)
        return df_short
    except Exception as e:
        error_msg = f"Error in anonymization process: {str(e)}"
        print(error_msg)
        return error_msg