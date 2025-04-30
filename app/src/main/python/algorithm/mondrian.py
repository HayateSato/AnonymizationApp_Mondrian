# Multi-Dimensional Mondrian for k-anonymity
import os
import pandas as pd
import time
from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
import base64

# custom library
import algorithm.hierarchy_tree as h_tree

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


def generate_key(password):
    password = password.encode()
    salt = b'salt_'  # You can change this salt, but keep it constant
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=32,
        salt=salt,
        iterations=100000,
    )
    key = base64.urlsafe_b64encode(kdf.derive(password))
    return key

def encrypt_value(value, fernet):
    return fernet.encrypt(str(value).encode()).decode()

def decrypt_value(value, fernet):
    return fernet.decrypt(value.encode()).decode()

    return df


# def run_anonymize(qi_list, data_file, hierarchy_file_dir, k=5):  # original of old
# def run_anonymize(qi_list, sensitive_attributes, identifier, data_file, hierarchy_file_dir, k=5, password=None): # original of new
def run_anonymize(qi_list, identifiers, data_file, hierarchy_file_dir, k=5, password=None):
    # suppose n records(num of rows). k-anonymity. m quasi-identifiers. Calculate time complexity
    df = pd.read_csv(data_file)

    if password:
        key = generate_key(password)
        fernet = Fernet(key)
        for identifier in identifiers:
            if identifier in df.columns:
                df[identifier] = df[identifier].apply(lambda x: encrypt_value(x, fernet))

    hierarchy_tree_dict = h_tree.build_all_hierarchy_tree(hierarchy_file_dir)
    print(f"hirerarchy_tree_dict: {hierarchy_tree_dict})")


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



def decrypt_and_compare(anonymized_file, original_file, identifiers, password):
    """
    Decrypts the anonymized data and compares it with the original data.

    :param anonymized_file: Path to the anonymized CSV file
    :param original_file: Path to the original CSV file
    :param identifiers: List of identifier column names
    :param password: Password used for encryption
    """
    # Read the anonymized and original data
    anonymized_df = pd.read_csv(anonymized_file)
    original_df = pd.read_csv(original_file)

    # Generate the key from the password
    key = generate_key(password)
    fernet = Fernet(key)

    # Decrypt the identifiers in the anonymized data
    for identifier in identifiers:
        if identifier in anonymized_df.columns:
            anonymized_df[identifier] = anonymized_df[identifier].apply(lambda x: decrypt_value(x, fernet))

    # Compare the decrypted data with the original data
    for identifier in identifiers:
        if identifier in anonymized_df.columns and identifier in original_df.columns:
            print(f"\nComparing {identifier}:")
            print("Anonymized (Decrypted):")
            print(anonymized_df[identifier].head())
            print("\nOriginal:")
            print(original_df[identifier].head())

            # Check if the decrypted values match the original values
            match = (anonymized_df[identifier] == original_df[identifier]).all()
            print(f"\nAll values match: {match}")

    print("\nComparison complete.")



    #### DUMMY FUNCTION TO EXECUTE THE ANONYMIZATION #################################
    # the below function is called in the main function - MainActivity
    # above codes should not be changed
    #################################################################################


def anonymize_execute(k_value):
    tic = time.time()  # time count starts
    # dir/file path  #############################################################################
    current_dir = os.path.dirname(__file__)  # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/algorithm
    parent_dir = os.path.dirname(current_dir)  # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app
    input_dir = os.path.join(parent_dir, "input/") # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/input
    output_dir = os.path.join(parent_dir, "output/anonymized/") # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/output/anonymized
    # defining input #############################################################################
    qi_list = ['sex', 'age', 'race', 'marital-status', 'education', 'native-country', 'workclass', 'occupation']
    identifiers = ['ID', 'soc_sec_id', 'given_name', 'surname']
    input_path = os.path.join(input_dir, "dataset.csv") # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/input/dataset.csv
    hierarchy_file_path = os.path.join(current_dir, "hierarchy/")  # /data/data/com.example.pythoncalculation/files/chaquopy/AssetFinder/app/algorithm/hierarchy
    k = k_value
    password_received = " "
    # log ########################################################################################
    print("running anonymization")
    print(f"run_anonymize executing with K = {k}")
    if password_received == " ":
        print(f"run_anonymize executing with default password")
    else:
        print(f"run_anonymize executing with given password")
    # anonymize_execute function call  ###########################################################
    # passing the input to the function, saving the result to data_frame
    data_frame = run_anonymize(qi_list, identifiers, input_path, hierarchy_file_path, k=k, password=password_received)
    # output ####################################################################################
    os.makedirs(output_dir, exist_ok=True) # Create the directory if it doesn't exist
    # specifying the csv file name with k-value
    output_file_path = os.path.join(output_dir, f'k_{k}_anonymized_dataset.csv')
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
    df_short = data_frame[['age', 'race', 'marital-status', 'education', 'native-country', 'soc_sec_id']].iloc[850:890]
    print(df_short)
    return df_short