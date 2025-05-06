package com.example.pythoncalculation.fragments;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.pythoncalculation.R;
import com.example.pythoncalculation.databinding.FragmentSettingsBinding;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;

/**
 * Fragment for managing and viewing application settings.
 * Shows connection status to SmarKo watch and MQTT broker.
 */
public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";
    private FragmentSettingsBinding binding;
    private IMqttAsyncClient mqttClient;
    private String mqttBrokerUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up navigation back to home
        NavController navController = Navigation.findNavController(view);
        binding.backButton.setOnClickListener(v -> 
            navController.navigate(R.id.action_settings_to_home));

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
            com.example.pythoncalculation.MainActivity activity = 
                    (com.example.pythoncalculation.MainActivity) requireActivity();
            mqttClient = activity.getMqttClient();
            mqttBrokerUrl = activity.getMqttBrokerUrl();
            
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
