package com.example.pythoncalculation.fragments;

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
    private FragmentDataBinding binding;
    private Python py;
    private PyObject inputReaderModule;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDataBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get Python instance and modules
        py = Python.getInstance();
        inputReaderModule = py.getModule("algorithm.input_reader");

        // Set up navigation back to home
        NavController navController = Navigation.findNavController(view);
        binding.backButton.setOnClickListener(v -> 
            navController.navigate(R.id.action_data_to_home));

        // Set up the Read CSV button
        binding.readCsvButton.setOnClickListener(v -> {
            binding.progressBar.setVisibility(View.VISIBLE);
            new ReadCsvTask(this).execute();
        });
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

        ReadCsvTask(DataFragment fragment) {
            fragmentReference = new WeakReference<>(fragment);
        }

        @Override
        protected String doInBackground(Void... voids) {
            DataFragment fragment = fragmentReference.get();
            if (fragment == null || fragment.getActivity() == null || fragment.isDetached()) return null;

            try (PyObject pyObjectResult = fragment.inputReaderModule.callAttr("get_csvfile", "dataset.csv")) {
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
