package com.example.pythoncalculation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.example.pythoncalculation.databinding.ActivityMainBinding;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * MainActivity: The main entry point of the application.
 * 
 * This activity extends AppCompatActivity from the AndroidX library to provide 
 * compatibility features for different Android versions.
 * 
 * The class is responsible for:
 * 1. Initializing the Python environment using Chaquopy
 * 2. Setting up MQTT communication for remote control
 * 3. Managing navigation between fragments
 */
public class MainActivity extends AppCompatActivity {

    // MQTT Configuration
    private static final String TAG = "MQTT";
    private static final String MQTT_BROKER_URL = "tcp://192.168.8.126:1883"; // MQTT broker address
    // Consider using SSL if available: "ssl://172.23.219.123:8883"
    private static final String MQTT_TOPIC = "anonymization/commands"; // Topic to listen for commands

    /**
     * List of valid K values for anonymization.
     * Only these values are accepted when processing MQTT commands.
     */
    private static final int[] VALID_K_VALUES = {2, 5, 10, 30, 50, 500};
    
    // Logging tag for non-MQTT related logs
    private static final String TAG_MAIN = "MainActivity";
    
    // UI Components
    private ActivityMainBinding binding;
    private TextView statusTextView;
    
    // MQTT Client
    private IMqttAsyncClient mqttClient;
    
    // Python Components
    private Python py;
    private PyObject inputReaderModule;
    private PyObject mondrianModule;
    
    // Navigation
    private NavController navController;

    /**
     * Called when the activity is first created.
     * Initializes the UI, Python environment, and MQTT connection.
     * 
     * This method is part of the Android Activity lifecycle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize UI with view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get reference to the status display
        statusTextView = binding.statusTextView;
        
        // Get the NavController for managing fragment navigation
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
        
        // Initialize Python environment for anonymization algorithm
        initializePython();
        
        // Check network connectivity before connecting to MQTT
        if (checkNetworkConnectivity()) {
            // Connect to MQTT broker for remote command handling
            connectToMqttBroker();
            
            // Set initial status text
            statusTextView.setText("Ready. Waiting for MQTT commands...");
            statusTextView.setVisibility(View.VISIBLE);
        } else {
            statusTextView.setText("Network unavailable. Cannot connect to MQTT broker.");
            statusTextView.setVisibility(View.VISIBLE);
            showToast("Network unavailable. MQTT connection not possible.");
        }
    }

    /**
     * Initializes the Python environment using Chaquopy.
     * Sets up access to the Python modules needed for CSV reading and anonymization.
     */
    private void initializePython() {
        // Start Python if not already running
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        
        // Get the Python interpreter instance
        py = Python.getInstance();
        
        // Load Python modules from the app's assets
        inputReaderModule = py.getModule("algorithm.input_reader");
        mondrianModule = py.getModule("algorithm.mondrian");
    }

    /**
     * Establishes connection to the MQTT broker and sets up message handling.
     * Uses the Eclipse Paho MQTT client library.
     */
    private void connectToMqttBroker() {
        try {
            // Generate a unique client ID for this connection
            String clientId = MqttAsyncClient.generateClientId();
            
            // Create MQTT client instance with MemoryPersistence to avoid file-based persistence issues
            // Using MemoryPersistence instead of null helps prevent permission issues on real devices
            mqttClient = new MqttAsyncClient(MQTT_BROKER_URL, clientId, new MemoryPersistence());

            // Configure connection options
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // Don't retain previous session state
            options.setConnectionTimeout(60); // Increase timeout for cellular networks
            options.setKeepAliveInterval(60); // Increase keep alive for cellular networks
            
            // Connect to the broker with callback handlers
            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // Connection successful, subscribe to the topic
                    Log.d(TAG, "Connected to MQTT Broker");
                    subscribeToTopic();
                    
                    // Update the status UI on the main thread
                    runOnUiThread(() -> {
                        statusTextView.setText("Connected to MQTT Broker");
                        statusTextView.setBackgroundColor(getResources().getColor(R.color.connected_green, null));
                    });
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Connection failed, log the error
                    Log.e(TAG, "Failed to connect", exception);
                    
                    // Update the status UI on the main thread
                    runOnUiThread(() -> {
                        statusTextView.setText("Failed to connect to MQTT Broker");
                        statusTextView.setBackgroundColor(getResources().getColor(R.color.disconnected_red, null));
                    });
                }
            });

            // Set up callback for message handling
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // Connection to the broker was lost
                    Log.e(TAG, "Connection lost", cause);
                    
                    // Update the status UI on the main thread
                    runOnUiThread(() -> {
                        statusTextView.setText("Connection to MQTT Broker lost");
                        statusTextView.setBackgroundColor(getResources().getColor(R.color.disconnected_red, null));
                    });
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // Extract the message content
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "Message arrived: " + payload);

                    // Check if message follows the format "K Value = X"
                    if (payload.startsWith("K Value =")) {
                        try {
                            // Extract the k-value from the message
                            String kValueStr = payload.substring("K Value =".length()).trim();
                            final int kValue = Integer.parseInt(kValueStr);
                            
                            // Log the extracted k-value
                            Log.d(TAG, "Extracted k-value: " + kValue);
                            
                            // Validate that the k-value is one of the acceptable values
                            if (isValidKValue(kValue)) {
                                // Update UI to show the message was received
                                runOnUiThread(() -> {
                                    statusTextView.setText("Received command: K Value = " + kValue);
                                    
                                    // Navigate to the anonymization fragment
                                    if (navController != null && 
                                        navController.getCurrentDestination().getId() != R.id.anonymizationFragment) {
                                        navController.navigate(R.id.anonymizationFragment);
                                    }
                                    
                                    // Small delay to allow fragment to load
                                    statusTextView.postDelayed(() -> {
                                        // Find and call the anonymization method on the fragment
                                        // This is handled through fragmentResultListener in the fragment
                                        Bundle result = new Bundle();
                                        result.putInt("k_value", kValue);
                                        getSupportFragmentManager().setFragmentResult("anonymize_request", result);
                                    }, 500);
                                });
                            } else {
                                // Invalid k-value received
                                Log.e(TAG, "Invalid k-value received: " + kValue);
                                runOnUiThread(() -> {
                                    statusTextView.setText("Invalid K value: " + kValue);
                                    showHeadsUpMessage("Invalid K Value", 
                                            "Received K = " + kValue + ", but only values 2, 5, 10, 30, 50, and 500 are allowed.");
                                });
                            }
                        } catch (NumberFormatException e) {
                            // Failed to parse the k-value
                            Log.e(TAG, "Invalid k-value format", e);
                            runOnUiThread(() -> {
                                statusTextView.setText("Invalid k-value format: " + payload);
                                showHeadsUpMessage("Invalid Format", 
                                        "The received message has an invalid format. Expected 'K Value = X' where X is a number.");
                            });
                        }
                    } else {
                        // Message format not recognized
                        runOnUiThread(() -> {
                            statusTextView.setText("Unknown command: " + payload);
                            showHeadsUpMessage("Unknown Command", 
                                    "Expected format: 'K Value = X' where X is one of: 2, 5, 10, 30, 50, 500");
                        });
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used for subscriber, only for publisher
                }
            });

        } catch (MqttException e) {
            // Log any MQTT-related errors
            Log.e(TAG, "MQTT Error", e);
            e.printStackTrace();
        }
    }

    /**
     * Subscribes to the MQTT topic to receive commands.
     */
    private void subscribeToTopic() {
        try {
            mqttClient.subscribe(MQTT_TOPIC, 0); // QoS level 0 - at most once delivery
            Log.d(TAG, "Subscribed to topic: " + MQTT_TOPIC);
        } catch (MqttException e) {
            Log.e(TAG, "Failed to subscribe", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Checks if the provided k-value is valid (one of the pre-defined values).
     * 
     * @param kValue The k-value to validate
     * @return true if the k-value is valid, false otherwise
     */
    private boolean isValidKValue(int kValue) {
        for (int validValue : VALID_K_VALUES) {
            if (kValue == validValue) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Shows a more prominent heads-up message as an AlertDialog.
     * This is used for important notifications like invalid K values.
     * 
     * @param title The dialog title
     * @param message The message to display
     */
    private void showHeadsUpMessage(String title, String message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
        
        // Also show a toast for additional notification
        showToast(title + ": " + message);
    }

    /**
     * Checks if the device has network connectivity.
     * This helps prevent attempting MQTT connections when no network is available.
     * 
     * @return true if network is available, false otherwise
     */
    private boolean checkNetworkConnectivity() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();
            
            Log.d(TAG, "Network connectivity: " + (isConnected ? "Available" : "Not available"));
            Log.d(TAG, "Network type: " + (activeNetworkInfo != null ? activeNetworkInfo.getTypeName() : "None"));
            
            return isConnected;
        }
        
        Log.e(TAG, "ConnectivityManager is null");
        return false;
    }

    /**
     * Displays a toast message to the user.
     * 
     * @param message The message to display
     */
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Get the MQTT client for other components to check connection status
     * @return The MQTT client instance
     */
    public IMqttAsyncClient getMqttClient() {
        return mqttClient;
    }

    /**
     * Get the MQTT broker URL for display in settings
     * @return The MQTT broker URL
     */
    public String getMqttBrokerUrl() {
        return MQTT_BROKER_URL;
    }

    /**
     * Get the Python instance for fragments to use
     * @return The Python instance
     */
    public Python getPythonInstance() {
        return py;
    }

    /**
     * Get the input reader module for fragments
     * @return The input reader module
     */
    public PyObject getInputReaderModule() {
        return inputReaderModule;
    }

    /**
     * Get the mondrian module for fragments
     * @return The mondrian module
     */
    public PyObject getMondrianModule() {
        return mondrianModule;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Disconnect from the MQTT broker if connected
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                Log.d(TAG, "Disconnected from MQTT broker");
            } catch (MqttException e) {
                Log.e(TAG, "Error disconnecting from MQTT broker", e);
            }
        }
    }
}
