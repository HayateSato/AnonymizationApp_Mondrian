package com.example.pythoncalculation.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.example.pythoncalculation.R;
import com.example.pythoncalculation.databinding.FragmentDataBinding;

import java.lang.ref.WeakReference;

/**
 * Fragment for viewing and loading data from CSV files.
 * Allows the user to load a CSV file and display its contents.
 */
public class DataFragment extends Fragment {

    private static final String TAG = "DataFragment";
    private static final String PREF_NAME = "DataPreferences";
    private static final String PREF_USE_WEARABLE = "use_wearable";
    
    private FragmentDataBinding binding;
    private Python py;
    private PyObject inputReaderModule;
    private PyObject wearableReaderModule;
    private boolean useWearableDataset = false;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDataBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        useWearableDataset = sharedPreferences.getBoolean(PREF_USE_WEARABLE, false);

        // Get Python instance and modules
        py = Python.getInstance();
        inputReaderModule = py.getModule("algorithm.input_reader");
        wearableReaderModule = py.getModule("algorithm.input_reader_wearable");

        // Set up navigation back to home
        NavController navController = Navigation.findNavController(view);
        binding.backButton.setOnClickListener(v -> 
            navController.navigate(R.id.action_data_to_home));

        // Set initial state of radio buttons
        if (useWearableDataset) {
            binding.wearableDatasetRadio.setChecked(true);
            binding.datasetInfoText.setText("Using wearable dataset (semicolon-separated)");
        } else {
            binding.standardDatasetRadio.setChecked(true);
            binding.datasetInfoText.setText("Using standard dataset (comma-separated)");
        }

        // Set up radio buttons for dataset selection
        binding.standardDatasetRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                useWearableDataset = false;
                binding.datasetInfoText.setText("Using standard dataset (comma-separated)");
                saveDatasetPreference();
                notifyAnonymizationFragmentOfDataTypeChange();
            }
        });

        binding.wearableDatasetRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                useWearableDataset = true;
                binding.datasetInfoText.setText("Using wearable dataset (semicolon-separated)");
                saveDatasetPreference();
                notifyAnonymizationFragmentOfDataTypeChange();
            }
        });

        // Set up the Read CSV button
        binding.readCsvButton.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            new ReadCsvTask(this, useWearableDataset).execute();
        });
    }

    /**
     * Save dataset preference to SharedPreferences
     */
    private void saveDatasetPreference() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_USE_WEARABLE, useWearableDataset);
        editor.apply();
    }

    /**
     * Notify AnonymizationFragment of data type changes
     */
    private void notifyAnonymizationFragmentOfDataTypeChange() {
        // Create a bundle with information about the selected dataset
        Bundle result = new Bundle();
        result.putBoolean("use_wearable", useWearableDataset);
        result.putString("selected_file", useWearableDataset ? "wearable_input_raw.csv" : "dataset.csv");
        
        // Set the fragment result to communicate with AnonymizationFragment
        getParentFragmentManager().setFragmentResult("file_selected", result);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * AsyncTask for reading CSV files in the background to prevent UI freezes
     */
    private static class ReadCsvTask extends AsyncTask<Void, Void, String> {
        private WeakReference<DataFragment> fragmentReference;
        private boolean useWearable;

        ReadCsvTask(DataFragment fragment, boolean useWearable) {
            fragmentReference = new WeakReference<>(fragment);
            this.useWearable = useWearable;
        }

        @Override
        protected String doInBackground(Void... voids) {
            DataFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.getActivity() == null || fragment.isDetached()) return null;

            try {
                PyObject pyObjectResult;
                if (useWearable) {
                    // Use wearable reader module
                    pyObjectResult = fragment.wearableReaderModule.callAttr("get_wearable_csvfile");
                    Log.d(TAG, "Reading wearable dataset");
                } else {
                    // Use standard reader module
                    pyObjectResult = fragment.inputReaderModule.callAttr("get_csvfile", "dataset.csv");
                    Log.d(TAG, "Reading standard dataset");
                }
                return pyObjectResult.toString();
            } catch (Exception e) {
                Log.e(TAG, "Error reading CSV file", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            DataFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.getActivity() == null || fragment.isDetached()) return;

            // Hide progress bar
            fragment.binding.progressBar.setVisibility(View.GONE);

            if (result != null) {
                fragment.binding.textViewOutput.setText(result);
            } else {
                fragment.binding.textViewOutput.setText(fragment.getString(R.string.error_message, "Failed to read CSV"));
                Toast.makeText(fragment.getContext(), "Failed to read CSV file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
