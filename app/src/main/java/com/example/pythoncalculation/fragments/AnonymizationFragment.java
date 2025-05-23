package com.example.pythoncalculation.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.example.pythoncalculation.R;
import com.example.pythoncalculation.databinding.FragmentAnonymizationBinding;

import java.lang.ref.WeakReference;

/**
 * Fragment for anonymizing data with different K values.
 * Allows the user to select a K value and perform anonymization on the data.
 */
public class AnonymizationFragment extends Fragment {

    private static final String TAG = "AnonymizationFragment";
    private static final String PREF_NAME = "DataPreferences";
    private static final String PREF_USE_WEARABLE = "use_wearable";
    
    private FragmentAnonymizationBinding binding;
    private Python py;
    private PyObject mondrianModule;
    private boolean useWearableDataset = false;
    private String selectedDatasetFile = "dataset.csv";
    private RadioButton standardDatasetRadio;
    private RadioButton wearableDatasetRadio;
    private SharedPreferences sharedPreferences;

    private TextView resultLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnonymizationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get Python instance and modules
        py = Python.getInstance();
        mondrianModule = py.getModule("algorithm.mondrian");
        
        // Get shared preferences
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        useWearableDataset = sharedPreferences.getBoolean(PREF_USE_WEARABLE, false);
        selectedDatasetFile = useWearableDataset ? "wearable_input_raw.csv" : "dataset.csv";

        // Initialize radio buttons
        standardDatasetRadio = binding.standardDatasetRadio;
        wearableDatasetRadio = binding.wearableDatasetRadio;
        
        // Set initial radio button state based on preferences
        standardDatasetRadio.setChecked(!useWearableDataset);
        wearableDatasetRadio.setChecked(useWearableDataset);
        
        // Setup radio button listeners
        setupRadioButtonListeners();

        // Set up navigation back to home
        NavController navController = Navigation.findNavController(view);
        binding.backButton.setOnClickListener(v -> 
            navController.navigate(R.id.action_anonymization_to_home));

        // Set up K value buttons
        setupAnonymizationButtons();
        
        // Add a label to remind users that these are K values
        resultLabel = binding.resultLabel;
        updateResultLabel();
        
        // Set up fragment result listener for MQTT commands
        getParentFragmentManager().setFragmentResultListener("anonymize_request", this,
            (requestKey, result) -> {
                if (result.containsKey("k_value")) {
                    int kValue = result.getInt("k_value");
                    
                    // Check if we have dataset selection as well
                    if (result.containsKey("use_wearable")) {
                        boolean newUseWearable = result.getBoolean("use_wearable");
                        
                        // Update dataset selection if it's different
                        if (newUseWearable != useWearableDataset) {
                            setDataset(newUseWearable);
                        }
                    }
                    
                    Log.d(TAG, "Received fragment result with k_value = " + kValue + 
                            ", dataset = " + (useWearableDataset ? "wearable" : "standard"));
                    
                    Toast.makeText(getContext(), 
                            "Starting anonymization with K = " + kValue + 
                            " on " + (useWearableDataset ? "wearable" : "standard") + " dataset", 
                            Toast.LENGTH_SHORT).show();
                    
                    startAnonymization(kValue);
                }
            });
        
        // Listen for file selection changes from DataFragment
        getParentFragmentManager().setFragmentResultListener("file_selected", this,
            (requestKey, result) -> {
                boolean newUseWearable = result.getBoolean("use_wearable");
                String newSelectedFile = result.getString("selected_file");
                
                if (newUseWearable != useWearableDataset || 
                    (newSelectedFile != null && !newSelectedFile.equals(selectedDatasetFile))) {
                    
                    setDataset(newUseWearable);
                    
                    if (newSelectedFile != null) {
                        selectedDatasetFile = newSelectedFile;
                    }
                    
                    Toast.makeText(
                        getContext(), 
                        "Using " + (useWearableDataset ? "wearable dataset" : "standard dataset") + " for anonymization", 
                        Toast.LENGTH_SHORT
                    ).show();
                }
            });
        
        // Log that the fragment is ready
        Log.d(TAG, "AnonymizationFragment is now ready");
    }
    
    /**
     * Sets the dataset to use for anonymization based on the MQTT command.
     *
     * @param useWearable true to use wearable dataset, false to use standard dataset
     */
    public void setDataset(boolean useWearable) {
        this.useWearableDataset = useWearable;
        selectedDatasetFile = useWearable ? "wearable_input_raw.csv" : "dataset.csv";
        
        // Update radio buttons to match selection
        if (standardDatasetRadio != null && wearableDatasetRadio != null) {
            standardDatasetRadio.setChecked(!useWearable);
            wearableDatasetRadio.setChecked(useWearable);
        }
        
        // Save to preferences
        if (sharedPreferences != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(PREF_USE_WEARABLE, useWearable);
            editor.apply();
        }
        
        updateResultLabel();
    }
    
    private void setupRadioButtonListeners() {
        standardDatasetRadio.setOnClickListener(v -> {
            if (standardDatasetRadio.isChecked()) {
                setDataset(false);
                
                Toast.makeText(
                    getContext(), 
                    "Using standard dataset for anonymization", 
                    Toast.LENGTH_SHORT
                ).show();
            }
        });
        
        wearableDatasetRadio.setOnClickListener(v -> {
            if (wearableDatasetRadio.isChecked()) {
                setDataset(true);
                
                Toast.makeText(
                    getContext(), 
                    "Using wearable dataset for anonymization", 
                    Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
    
    private void updateResultLabel() {
        String datasetType = useWearableDataset ? "Wearable" : "Standard";
        resultLabel.setText("Anonymization Result (K = ?, Dataset: " + datasetType + "):");
    }

    private void setupAnonymizationButtons() {
        binding.anonymizeButtonK2.setOnClickListener(v -> startAnonymization(2));
        binding.anonymizeButtonK5.setOnClickListener(v -> startAnonymization(5));
        binding.anonymizeButtonK10.setOnClickListener(v -> startAnonymization(10));
        binding.anonymizeButtonK30.setOnClickListener(v -> startAnonymization(30));
        binding.anonymizeButtonK50.setOnClickListener(v -> startAnonymization(50));
        binding.anonymizeButtonK500.setOnClickListener(v -> startAnonymization(500));
    }

    public void startAnonymization(int kValue) {
        // Show progress bar
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Update result label with selected K value and dataset type
        String datasetType = useWearableDataset ? "Wearable" : "Standard";
        resultLabel.setText("Anonymization Result (K = " + kValue + ", Dataset: " + datasetType + "):");
        
        // Disable buttons while processing
        setButtonsEnabled(false);
        
        // Execute the anonymization
        new AnonymizeTask(this, kValue, selectedDatasetFile).execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        binding.anonymizeButtonK2.setEnabled(enabled);
        binding.anonymizeButtonK5.setEnabled(enabled);
        binding.anonymizeButtonK10.setEnabled(enabled);
        binding.anonymizeButtonK30.setEnabled(enabled);
        binding.anonymizeButtonK50.setEnabled(enabled);
        binding.anonymizeButtonK500.setEnabled(enabled);
        binding.backButton.setEnabled(enabled);
        standardDatasetRadio.setEnabled(enabled);
        wearableDatasetRadio.setEnabled(enabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * AsyncTask for anonymizing data in the background to prevent UI freezes
     */
    private static class AnonymizeTask extends AsyncTask<Void, Void, String> {
        private WeakReference<AnonymizationFragment> fragmentReference;
        private int kValue;
        private String datasetFile;

        AnonymizeTask(AnonymizationFragment fragment, int kValue, String datasetFile) {
            fragmentReference = new WeakReference<>(fragment);
            this.kValue = kValue;
            this.datasetFile = datasetFile;
        }

        @Override
        protected void onPreExecute() {
            AnonymizationFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.getActivity() == null || fragment.isDetached()) return;

            StringBuilder message = new StringBuilder()
                    .append("Anonymization with K=").append(kValue)
                    .append(" started on ").append(datasetFile);

            Toast.makeText(fragment.getContext(), message.toString(), Toast.LENGTH_LONG).show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            AnonymizationFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.getActivity() == null || fragment.isDetached()) return null;

            try (PyObject pyObjectAnonymizedDataResult = fragment.mondrianModule.callAttr("anonymize_execute", kValue, datasetFile)) {
                return pyObjectAnonymizedDataResult.toString();
            } catch (Exception e) {
                Log.e(TAG, "Error during anonymization", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            AnonymizationFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.getActivity() == null || fragment.isDetached()) return;

            // Hide progress bar
            fragment.binding.progressBar.setVisibility(View.GONE);
            
            // Re-enable buttons
            fragment.setButtonsEnabled(true);

            if (result != null) {
                fragment.binding.textViewOutput.setText(result);
                Toast.makeText(fragment.getContext(), "Anonymization completed!", Toast.LENGTH_SHORT).show();
            } else {
                fragment.binding.textViewOutput.setText(fragment.getString(R.string.error_message, "Anonymization failed"));
                Toast.makeText(fragment.getContext(), "Anonymization failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}