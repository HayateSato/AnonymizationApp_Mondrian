package com.example.pythoncalculation;

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

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;

import java.lang.ref.WeakReference;


import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


// 11111111111111111111111111111111111111111111111111111111111111111111111111111111111
// -------------------- MQTT SERVER  ----------------------------------- FROM HERE ---
// 11111111111111111111111111111111111111111111111111111111111111111111111111111111111

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MQTT";
    private static final String MQTT_BROKER_URL = "tcp://172.23.219.123:1883"; ///////////////// Replace with your IP address where the MQTT broker is running
    private static final String MQTT_TOPIC = "anonymization/commands"; ///////////////////////// Replace with your MQTT topic you set when you started MQTT broker

    private IMqttAsyncClient mqttClient;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusTextView);

        connectToMqttBroker();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializePython();
        binding.progressBar.setVisibility(View.GONE);
    }



    private void connectToMqttBroker() {
        try {
            String clientId = MqttAsyncClient.generateClientId();
            mqttClient = new MqttAsyncClient(MQTT_BROKER_URL, clientId, null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Connected to MQTT Broker");
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "Failed to connect", exception);
                }
            });

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    Log.d(TAG, "Message arrived: " + payload);

                    runOnUiThread(() -> {
                        if (payload.equalsIgnoreCase("generalization")) {
                            statusTextView.setText("Now Generalization");
                        } else if (payload.equalsIgnoreCase("suppression")) {
                            statusTextView.setText("Now Suppression");
                        } else {
                            statusTextView.setText("Unknown command: " + payload);
                        }
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not needed for subscriber
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribeToTopic() {
        try {
            mqttClient.subscribe(MQTT_TOPIC, 0);
            Log.d(TAG, "Subscribed to topic: " + MQTT_TOPIC);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

// 11111111111111111111111111111111111111111111111111111111111111111111111111111111111
// -------------------- MQTT SERVER  ------------ UP TO HERE ------------------------
// 11111111111111111111111111111111111111111111111111111111111111111111111111111111111

    private static final String TAG_ = "MainActivity";
    private ActivityMainBinding binding;
    private Python py;
    private PyObject inputReaderModule;
    private PyObject mondrianModule;

// ---------------

    private void initializePython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        py = Python.getInstance();
        inputReaderModule = py.getModule("algorithm.input_reader");
        mondrianModule = py.getModule("algorithm.mondrian");
    }

// 000000000000000000000000000000000000000000000000000000000000000000000000000000000000
// -------------- INTERNAL CLASS CALLED  -------------------------------- FROM HERE ---
// 000000000000000000000000000000000000000000000000000000000000000000000000000000000000

    public void readButtonPythonRun(View view) {
        new ReadCsvTask(this).execute();
    }

    public void onAnonymizeButtonClick(View view, int kValue) {
        new AnonymizeTask(this, kValue).execute();
    }

    public void onAnonymizeButtonClick_k2(View view) {
        onAnonymizeButtonClick(view, 2);
    }

    public void onAnonymizeButtonClick_k5(View view) {
        onAnonymizeButtonClick(view, 5);
    }

    public void onAnonymizeButtonClick_k10(View view) {
        onAnonymizeButtonClick(view, 10);
    }

    public void onAnonymizeButtonClick_k30(View view) {
        onAnonymizeButtonClick(view, 30);
    }

    public void onAnonymizeButtonClick_k50(View view) {
        onAnonymizeButtonClick(view, 50);
    }

    public void onAnonymizeButtonClick_k500(View view) {
        onAnonymizeButtonClick(view, 500);
    }


    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

// 000000000000000000000000000000000000000000000000000000000000000000000000000000000000
// -------------- INTERNAL CLASS CALLED  ------------ UP TO HERE --------------------
// 000000000000000000000000000000000000000000000000000000000000000000000000000000000000


// 222222222222222222222222222222222222222222222222222222222222222222222222222222222222
// -------------- CLASS TO READ CSV   ----------------------------------- FROM HERE ---
// 222222222222222222222222222222222222222222222222222222222222222222222222222222222222

    private static class ReadCsvTask extends AsyncTask<Void, Void, String> {
        private WeakReference<MainActivity> activityReference;

        ReadCsvTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Void... voids) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return null;

            try (PyObject pyObjectResult = activity.inputReaderModule.callAttr("get_csvfile", "dataset.csv")) {
                return pyObjectResult.toString();
            } catch (Exception e) {
                Log.e(TAG_, "Error reading CSV file", e);
                return null;
            }
        }

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

// 222222222222222222222222222222222222222222222222222222222222222222222222222222222222
// -------------- CLASS TO READ CSV   ------------ UP TO HERE ------------------------
// 222222222222222222222222222222222222222222222222222222222222222222222222222222222222

// 333333333333333333333333333333333333333333333333333333333333333333333333333333333333
// ---CLASS TO ANONYMIZE (called from python/.../mondrian.py)  ---------- FROM HERE ---
// 333333333333333333333333333333333333333333333333333333333333333333333333333333333333
    private static class AnonymizeTask extends AsyncTask<Void, Void, String> {
        private WeakReference<MainActivity> activityReference;
        private int kValue;
        private String buttonTag;

        AnonymizeTask(MainActivity context, int kValue) {
            activityReference = new WeakReference<>(context);
            this.kValue = kValue;
            this.buttonTag = "k" + kValue;
        }

        @Override
        protected void onPreExecute() {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            activity.binding.progressBar.setVisibility(View.VISIBLE); // Show the ProgressBar

            int durationEstimate = 70 / kValue;
            StringBuilder message = new StringBuilder()
                    .append("Anonymization button (K=").append(kValue).append(") clicked!\n")
                    .append("Processing in the backend. Please wait for a short moment\n")
                    .append("It might take ").append(durationEstimate).append("-").append(durationEstimate + 10).append(" seconds\n")
                    .append("Once the anonymization is completed, the result will be shown above");
            activity.showToast(message.toString());
        }

        @Override
        protected String doInBackground(Void... voids) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return null;

            try (PyObject pyObjectAnonymizedDataResult = activity.mondrianModule.callAttr("anonymize_execute", kValue)) {
                return pyObjectAnonymizedDataResult.toString();
            } catch (Exception e) {
                Log.e(TAG_, "Error during anonymization", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            activity.binding.progressBar.setVisibility(View.GONE); // Hide the ProgressBar

            if (result != null) {
                Log.d(TAG_, "MainActivity: Anonymization successfully completed");
                activity.showToast("Anonymization is completed!");
                activity.binding.textViewOutput.setText(result);
            } else {
                activity.binding.textViewOutput.setText(activity.getString(R.string.error_message, "Anonymization failed"));
                activity.showToast("Anonymization failed");
            }
        }
    }
}