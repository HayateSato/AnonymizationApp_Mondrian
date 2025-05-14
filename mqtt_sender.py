#!/usr/bin/env python3
"""
MQTT Sender Script

This script sends anonymization commands to the Android app via MQTT protocol.
It sends JSON-formatted messages to trigger anonymization with specific k values and dataset selection.

Usage:
  python mqtt_sender.py <k_value> [dataset_type]

Arguments:
  k_value      The K value for anonymization (2, 5, 10, 30, 50, or 500)
  dataset_type The dataset to use (standard or wearable, defaults to standard)

Example:
  python mqtt_sender.py 10 wearable  # Sends {"kValue": 10, "dataset": "wearable"} to the MQTT broker
  python mqtt_sender.py 5            # Sends {"kValue": 5, "dataset": "standard"} to the MQTT broker
"""

import sys
import time
import json
import paho.mqtt.client as mqtt

# MQTT Configuration - Update these to match your app settings
MQTT_BROKER = "192.168.8.126"  # Same IP as in your Android app
MQTT_PORT = 1883
MQTT_TOPIC = "anonymization/commands"  # Same topic as in your Android app

# Valid K values as defined in the app
VALID_K_VALUES = [2, 5, 10, 30, 50, 500]

def on_connect(client, userdata, flags, rc):
    """Callback for when the client connects to the broker."""
    if rc == 0:
        print(f"Connected to MQTT broker at {MQTT_BROKER}:{MQTT_PORT}")
    else:
        print(f"Failed to connect to MQTT broker, return code: {rc}")

def on_publish(client, userdata, mid):
    """Callback for when a message is published."""
    print(f"Message published successfully")

def send_mqtt_message(k_value, dataset):
    """Send a JSON-formatted MQTT message with the specified k-value and dataset."""
    # Create MQTT client instance
    client = mqtt.Client()
    
    # Set up callbacks
    client.on_connect = on_connect
    client.on_publish = on_publish
    
    # Connect to the broker
    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
    except Exception as e:
        print(f"Error connecting to MQTT broker: {e}")
        return False
    
    # Start the loop
    client.loop_start()
    
    # Wait for connection to establish
    time.sleep(1)
    
    # Create the JSON message
    message_data = {
        "kValue": k_value,
        "dataset": dataset
    }
    
    # Convert to JSON string
    message = json.dumps(message_data)
    
    # Publish the message
    print(f"Sending JSON message: '{message}' to topic: {MQTT_TOPIC}")
    result = client.publish(MQTT_TOPIC, message)
    
    # Check if the message was published
    if result.rc == mqtt.MQTT_ERR_SUCCESS:
        print("Message queued for delivery")
    else:
        print(f"Failed to publish message, return code: {result.rc}")
    
    # Wait for the message to be published
    time.sleep(2)
    
    # Clean up
    client.loop_stop()
    client.disconnect()
    return True

def validate_inputs(k_value, dataset):
    """Validate the input parameters."""
    # Validate k-value
    if k_value not in VALID_K_VALUES:
        print(f"Error: {k_value} is not a valid K value.")
        print(f"Valid K values are: {', '.join(map(str, VALID_K_VALUES))}")
        return False
    
    # Validate dataset
    if dataset.lower() not in ["standard", "wearable"]:
        print(f"Error: '{dataset}' is not a valid dataset type.")
        print("Valid dataset types are: standard, wearable")
        return False
    
    return True

if __name__ == "__main__":
    # Check command line arguments
    if len(sys.argv) < 2 or len(sys.argv) > 3:
        print("Usage: python mqtt_sender.py <k_value> [dataset_type]")
        print("  k_value      - The K value for anonymization (2, 5, 10, 30, 50, or 500)")
        print("  dataset_type - The dataset to use (standard or wearable, defaults to standard)")
        sys.exit(1)
    
    try:
        k_value = int(sys.argv[1])
    except ValueError:
        print(f"Error: K value must be a valid integer.")
        sys.exit(1)
    
    # Get dataset type, default to "standard" if not provided
    dataset = sys.argv[2].lower() if len(sys.argv) == 3 else "standard"
    
    # Validate inputs
    if not validate_inputs(k_value, dataset):
        sys.exit(1)
    
    # Send the message
    if send_mqtt_message(k_value, dataset):
        print("JSON message sent successfully")
        print(f"Command: Anonymize with K={k_value} using {dataset} dataset")
    else:
        print("Failed to send message")
