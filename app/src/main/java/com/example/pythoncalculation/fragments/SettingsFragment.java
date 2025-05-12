package com.example.pythoncalculation.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.pythoncalculation.MainActivity;
import com.example.pythoncalculation.MqttHelper;
import com.example.pythoncalculation.R;
import com.example.pythoncalculation.databinding.FragmentSettingsBinding;
import com.google.android.material.textfield.TextInputEditText;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;

/**
 * Fragment for managing and viewing application settings.
 * Shows connection status to SmarKo watch and MQTT broker.
 */
public class SettingsFragment extends Fragment {

    private static final String PREF_NAME = "MqttPreferences";
    private static final String PREF_BROKER_URL = "broker_url";
    private static final String DEFAULT_BROKER_URL = "tcp://192.168.8.126:1883";

    private static final String TAG = "SettingsFragment";
    private FragmentSettingsBinding binding;
    private IMqttAsyncClient mqttClient;
    private String mqttBrokerUrl;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Set up navigation back to home
        NavController navController = Navigation.findNavController(view);
        binding.backButton.setOnClickListener(v -> 
            navController.navigate(R.id.action_settings_to_home));

        // Set up edit MQTT broker button
        binding.editMqttButton.setOnClickListener(v -> showMqttBrokerDialog());

        // Display app version
        displayAppVersion();
        
        // Check connection status
        updateConnectionStatus();
    }

    private void displayAppVersion() {
        try {
            PackageInfo packageInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            binding.appVersionValue.setText(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting package info", e);
            binding.appVersionValue.setText("Unknown");
        }
    }

    private void updateConnectionStatus() {
        // SmarKo watch is not connected in this version
        binding.smartkoStatusText.setText("Not Connected");
        binding.smartkoStatusText.setTextColor(getResources().getColor(R.color.disconnected_red, null));
        binding.bluetoothIcon.setColorFilter(getResources().getColor(R.color.disconnected_red, null));

        // Check MQTT connection status from MainActivity
        checkMqttStatus();
    }

    private void checkMqttStatus() {
        // Try to get MQTT client from MainActivity
        try {
            MainActivity activity = (MainActivity) requireActivity();
            mqttClient = activity.getMqttClient();
            mqttBrokerUrl = getSavedBrokerUrl();
            
            // Update UI based on connection status
            if (mqttClient != null && mqttClient.isConnected()) {
                binding.mqttStatusText.setText("Connected");
                binding.mqttStatusText.setTextColor(getResources().getColor(R.color.connected_green, null));
                binding.serverIcon.setColorFilter(getResources().getColor(R.color.connected_green, null));
                binding.mqttBrokerUrl.setText("Broker URL: " + mqttBrokerUrl);
            } else {
                binding.mqttStatusText.setText("Disconnected");
                binding.mqttStatusText.setTextColor(getResources().getColor(R.color.disconnected_red, null));
                binding.serverIcon.setColorFilter(getResources().getColor(R.color.disconnected_red, null));
                binding.mqttBrokerUrl.setText("Broker URL: " + (mqttBrokerUrl != null ? mqttBrokerUrl : "Not Available"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking MQTT status", e);
            binding.mqttStatusText.setText("Status Unknown");
            binding.mqttStatusText.setTextColor(getResources().getColor(R.color.gray, null));
            binding.serverIcon.setColorFilter(getResources().getColor(R.color.gray, null));
            binding.mqttBrokerUrl.setText("Broker URL: Not Available");
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh connection status on resume
        updateConnectionStatus();
    }

    /**
     * Shows a dialog for editing the MQTT broker URL.
     */
    private void showMqttBrokerDialog() {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_mqtt_broker, null);
        TextInputEditText brokerEditText = dialogView.findViewById(R.id.mqttBrokerEditText);
        
        // Set the current broker URL if available
        String currentUrl = getSavedBrokerUrl();
        brokerEditText.setText(currentUrl);
        
        // Create and show the dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // Set button click listeners
        dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
        
        dialogView.findViewById(R.id.saveButton).setOnClickListener(v -> {
            String newBrokerUrl = brokerEditText.getText().toString().trim();
            
            // Validate URL format
            if (!isValidMqttUrl(newBrokerUrl)) {
                Toast.makeText(requireContext(), "Invalid MQTT URL format. Use tcp://hostname:port", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Save the new URL
            saveBrokerUrl(newBrokerUrl);
            
            // Test connection to the new broker
            testNewConnection(newBrokerUrl);
            
            // Update the UI
            updateConnectionStatus();
            
            // Close the dialog
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    /**
     * Tests the connection to a new MQTT broker URL.
     * 
     * @param brokerUrl The URL to test
     */
    private void testNewConnection(String brokerUrl) {
        Toast.makeText(requireContext(), "Testing connection to new broker...", Toast.LENGTH_SHORT).show();
        
        // Check if we need to reconnect the main MQTT client
        MainActivity activity = (MainActivity) requireActivity();
        if (activity != null) {
            activity.reconnectMqttClient(brokerUrl);
        }
    }
    
    /**
     * Validates if the given string is a valid MQTT broker URL.
     * 
     * @param url The URL to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidMqttUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // Basic validation for MQTT URL format
        return url.startsWith("tcp://") && url.contains(":") && url.lastIndexOf(":") > 6;
    }
    
    /**
     * Saves the MQTT broker URL to SharedPreferences.
     * 
     * @param url The URL to save
     */
    private void saveBrokerUrl(String url) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_BROKER_URL, url);
        editor.apply();
        
        // Update local reference
        mqttBrokerUrl = url;
        
        Log.d(TAG, "Saved new broker URL: " + url);
    }
    
    /**
     * Gets the saved MQTT broker URL from SharedPreferences.
     * 
     * @return The saved URL or the default URL if none saved
     */
    private String getSavedBrokerUrl() {
        return sharedPreferences.getString(PREF_BROKER_URL, DEFAULT_BROKER_URL);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
