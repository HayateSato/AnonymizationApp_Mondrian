package com.example.pythoncalculation;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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

import java.lang.ref.WeakReference;

/**
 * MainActivity: The main entry point of the application.
 * 
 * This activity extends AppCompatActivity from the AndroidX library to provide 
 * compatibility features for different Android versions.
 * 
 * The class is responsible for:
 * 1. Initializing the Python environment using Chaquopy
 * 2. Setting up MQTT communication for remote control
 * 3. Managing the UI interactions for data anonymization
 * 4. Coordinating between Java and Python components
 */
public class MainActivity extends AppCompatActivity {

    // MQTT Configuration
    private static final String TAG = "MQTT";
//    private static final String MQTT_BROKER_URL = "tcp://172.23.219.123:1883"; // MQTT broker address (WSL)
    private static final String MQTT_BROKER_URL = "tcp://192.168.8.126:1883"; // MQTT broker address (Windows)
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
    private ActivityMainBinding binding; // View binding to access UI elements
    private TextView statusTextView; // TextView to display MQTT status
    
    // MQTT Client
    private IMqttAsyncClient mqttClient; // Interface for async MQTT communication
    
    // Python Components
    private Python py; // Main Python interpreter instance
    private PyObject inputReaderModule; // Python module for reading CSV files
    private PyObject mondrianModule; // Python module for anonymization algorithm

    /**
     * Called when the activity is first created.
     * Initializes the UI, Python environment, and MQTT connection.
     * 
     * This method is part of the Android Activity lifecycle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize UI with view binding (modern approach replacing findViewById)
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Get reference to the status display from binding
        statusTextView = binding.statusTextView;
        
        // Initialize Python environment for anonymization algorithm
        initializePython();
        
        // Hide progress indicator until needed
        binding.progressBar.setVisibility(View.GONE);
        
        // Check network connectivity before connecting to MQTT
        if (checkNetworkConnectivity()) {
            // Connect to MQTT broker for remote command handling
            connectToMqttBroker();
            
            // Set initial status text
            statusTextView.setText("Ready. Waiting for MQTT commands...");
        } else {
            statusTextView.setText("Network unavailable. Cannot connect to MQTT broker.");
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
            mqttClient = new MqttAsyncClient(MQTT_BROKER_URL, clientId, new org.eclipse.paho.client.mqttv3.persist.MemoryPersistence());

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
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Connection failed, log the error
                    Log.e(TAG, "Failed to connect", exception);
                }
            });

            // Set up callback for message handling
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // Connection to the broker was lost
                    Log.e(TAG, "Connection lost", cause);
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
                                // Update UI and start anonymization on UI thread
                                runOnUiThread(() -> {
                                    statusTextView.setText("Starting anonymization with K = " + kValue);
                                    // Start anonymization with the extracted k-value
                                    onAnonymizeButtonClick(null, kValue);
                                });
                            } else {
                                // Invalid k-value received
                                Log.e(TAG, "Invalid k-value received: " + kValue);
                                runOnUiThread(() -> {
                                    statusTextView.setText("Invalid K value: " + kValue + ". Must be one of: 2, 5, 10, 30, 50, 500");
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
            
            if (isConnected) {
                // Perform additional MQTT connection diagnostics
                MqttHelper.logConnectionDiagnostics(this, MQTT_BROKER_URL);
            }
            
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
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
        
        // Also show a toast for additional notification
        showToast(title + ": " + message);
    }

    /**
     * Handler for the "Read CSV" button click.
     * Starts an AsyncTask to read the CSV file in the background.
     * 
     * @param view The button that was clicked
     */
    public void readButtonPythonRun(View view) {
        new ReadCsvTask(this).execute();
    }

    /**
     * Starts the anonymization process with the specified k-value.
     * This method is called by the UI buttons and MQTT handler.
     * 
     * @param view The button that was clicked (can be null when called from MQTT)
     * @param kValue The k-value to use for anonymization
     */
    public void onAnonymizeButtonClick(View view, int kValue) {
        new AnonymizeTask(this, kValue).execute();
    }

    /**
     * Handler for the K=2 button click.
     * Delegates to onAnonymizeButtonClick with k=2.
     * 
     * @param view The button that was clicked
     */
    public void onAnonymizeButtonClick_k2(View view) {
        onAnonymizeButtonClick(view, 2);
    }

    /**
     * Handler for the K=5 button click.
     * Delegates to onAnonymizeButtonClick with k=5.
     * 
     * @param view The button that was clicked
     */
    public void onAnonymizeButtonClick_k5(View view) {
        onAnonymizeButtonClick(view, 5);
    }

    /**
     * Handler for the K=10 button click.
     * Delegates to onAnonymizeButtonClick with k=10.
     * 
     * @param view The button that was clicked
     */
    public void onAnonymizeButtonClick_k10(View view) {
        onAnonymizeButtonClick(view, 10);
    }

    /**
     * Handler for the K=30 button click.
     * Delegates to onAnonymizeButtonClick with k=30.
     * 
     * @param view The button that was clicked
     */
    public void onAnonymizeButtonClick_k30(View view) {
        onAnonymizeButtonClick(view, 30);
    }

    /**
     * Handler for the K=50 button click.
     * Delegates to onAnonymizeButtonClick with k=50.
     * 
     * @param view The button that was clicked
     */
    public void onAnonymizeButtonClick_k50(View view) {
        onAnonymizeButtonClick(view, 50);
    }

    /**
     * Handler for the K=500 button click.
     * Delegates to onAnonymizeButtonClick with k=500.
     * 
     * @param view The button that was clicked
     */
    public void onAnonymizeButtonClick_k500(View view) {
        onAnonymizeButtonClick(view, 500);
    }

    /**
     * AsyncTask for reading CSV files in the background.
     * 
     * AsyncTask is a helper class that allows performing background operations
     * and publishing results on the UI thread without having to manipulate threads.
     * 
     * Note: AsyncTask is deprecated in API level 30, but still works for this application.
     * In newer code, WorkManager or Kotlin coroutines would be preferred.
     */
    private static class ReadCsvTask extends AsyncTask<Void, Void, String> {
        private WeakReference<MainActivity> activityReference;

        /**
         * Constructor that takes a reference to the MainActivity.
         * Uses WeakReference to prevent memory leaks.
         * 
         * @param context The MainActivity instance
         */
        ReadCsvTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        /**
         * Performs work in a background thread.
         * Calls the Python module to read the CSV file.
         */
        @Override
        protected String doInBackground(Void... voids) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return null;

            try (PyObject pyObjectResult = activity.inputReaderModule.callAttr("get_csvfile", "dataset.csv")) {
                return pyObjectResult.toString();
            } catch (Exception e) {
                Log.e(TAG_MAIN, "Error reading CSV file", e);
                return null;
            }
        }

        /**
         * Executes on the UI thread after doInBackground completes.
         * Updates the UI with the CSV data or error message.
         * 
         * @param result The result returned from doInBackground
         */
        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            if (result != null) {
                activity.binding.textViewOutput.setText(result);
            } else {
                activity.binding.textViewOutput.setText(activity.getString(R.string.error_message, "Failed to read CSV"));
            }
        }
    }

    /**
     * AsyncTask for performing anonymization in the background.
     * 
     * This task calls the Python anonymization module and handles progress indication
     * and result display.
     */
    private static class AnonymizeTask extends AsyncTask<Void, Void, String> {
        private WeakReference<MainActivity> activityReference;
        private int kValue;
        private String buttonTag;

        /**
         * Constructor that takes a reference to the MainActivity and k-value.
         * 
         * @param context The MainActivity instance
         * @param kValue The k-value to use for anonymization
         */
        AnonymizeTask(MainActivity context, int kValue) {
            activityReference = new WeakReference<>(context);
            this.kValue = kValue;
            this.buttonTag = "k" + kValue;
        }

        /**
         * Executes on the UI thread before doInBackground.
         * Shows the progress bar and displays a message.
         */
        @Override
        protected void onPreExecute() {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            // Show the progress indicator
            activity.binding.progressBar.setVisibility(View.VISIBLE);

            // Estimate how long the process will take based on k-value
            int durationEstimate = 70 / kValue;
            
            // Create and show a detailed message about the process
            StringBuilder message = new StringBuilder()
                    .append("Anonymization button (K=").append(kValue).append(") clicked!\n")
                    .append("Processing in the backend. Please wait for a short moment\n")
                    .append("It might take ").append(durationEstimate).append("-").append(durationEstimate + 10).append(" seconds\n")
                    .append("Once the anonymization is completed, the result will be shown above");
            activity.showToast(message.toString());
        }

        /**
         * Performs work in a background thread.
         * Calls the Python anonymization module.
         */
        @Override
        protected String doInBackground(Void... voids) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return null;

            try (PyObject pyObjectAnonymizedDataResult = activity.mondrianModule.callAttr("anonymize_execute", kValue)) {
                return pyObjectAnonymizedDataResult.toString();
            } catch (Exception e) {
                Log.e(TAG_MAIN, "Error during anonymization", e);
                return null;
            }
        }

        /**
         * Executes on the UI thread after doInBackground completes.
         * Updates the UI with the anonymization result or error message.
         * 
         * @param result The result returned from doInBackground
         */
        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            // Hide the progress indicator
            activity.binding.progressBar.setVisibility(View.GONE);

            if (result != null) {
                // Anonymization completed successfully
                Log.d(TAG_MAIN, "MainActivity: Anonymization successfully completed");
                activity.showToast("Anonymization is completed!");
                activity.binding.textViewOutput.setText(result);
            } else {
                // Anonymization failed
                activity.binding.textViewOutput.setText(activity.getString(R.string.error_message, "Anonymization failed"));
                activity.showToast("Anonymization failed");
            }
        }
    }
}