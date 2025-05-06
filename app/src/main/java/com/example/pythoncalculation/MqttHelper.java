package com.example.pythoncalculation;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Helper class for MQTT connections that can be used to test connectivity.
 * This class provides utility methods to diagnose MQTT connection issues.
 */
public class MqttHelper {
    private static final String TAG = "MqttHelper";
    
    /**
     * Tests if an MQTT connection can be established with the given broker URL.
     * Uses a separate client from the main application to avoid interference.
     * 
     * @param context Android context
     * @param brokerUrl The MQTT broker URL (e.g., tcp://172.23.219.123:1883)
     * @return true if connection test was successful, false otherwise
     */
    public static boolean testMqttConnection(Context context, String brokerUrl) {
        final boolean[] success = {false};
        final Object lock = new Object();
        
        try {
            // Generate a unique client ID for this test
            String clientId = MqttClient.generateClientId() + "_test";
            
            // Use memory persistence to avoid file access issues
            MemoryPersistence persistence = new MemoryPersistence();
            
            // Create a test MQTT client
            MqttClient mqttClient = new MqttClient(brokerUrl, clientId, persistence);
            
            // Set up callback handler
            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, "Connection test completed successfully. Server: " + serverURI);
                    synchronized (lock) {
                        success[0] = true;
                        lock.notify();
                    }
                }
                
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection test lost connection", cause);
                }
                
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // Not used for connection test
                }
                
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used for connection test
                }
            });
            
            // Try to connect with a timeout of 10 seconds
            mqttClient.connect();
            
            // Wait for connection result
            synchronized (lock) {
                try {
                    lock.wait(10000);  // Wait up to 10 seconds
                } catch (InterruptedException e) {
                    Log.e(TAG, "Connection test interrupted", e);
                }
            }
            
            // Clean up
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            
        } catch (MqttException e) {
            Log.e(TAG, "Error during connection test", e);
            return false;
        }
        
        return success[0];
    }
    
    /**
     * Logs detailed information about network and connection status that can be 
     * useful for diagnosing MQTT connection issues.
     *
     * @param context Android context
     * @param brokerUrl The MQTT broker URL to test
     */
    public static void logConnectionDiagnostics(Context context, String brokerUrl) {
        Log.d(TAG, "======== MQTT CONNECTION DIAGNOSTICS ========");
        Log.d(TAG, "Broker URL: " + brokerUrl);
        
        // Test a direct connection
        boolean connectionSuccess = testMqttConnection(context, brokerUrl);
        Log.d(TAG, "Direct connection test result: " + (connectionSuccess ? "SUCCESS" : "FAILED"));
        
        Log.d(TAG, "============================================");
    }
}
