package com.example.pythoncalculation;

/**
 * Model class for MQTT JSON message.
 * This class represents the structure of JSON messages received through MQTT.
 */
public class AnonymizationCommand {

    /**
     * The K value for anonymization.
     * Must be one of the predefined values: 2, 5, 10, 30, 50, 500.
     */
    private int kValue;

    /**
     * The dataset to be anonymized.
     * Can be either "standard" or "wearable".
     */
    private String dataset;

    /**
     * Default constructor for Gson deserialization.
     */
    public AnonymizationCommand() {
    }

    /**
     * Constructor with parameters.
     *
     * @param kValue The K value for anonymization
     * @param dataset The dataset to be anonymized
     */
    public AnonymizationCommand(int kValue, String dataset) {
        this.kValue = kValue;
        this.dataset = dataset;
    }

    /**
     * Get the K value.
     *
     * @return The K value
     */
    public int getKValue() {
        return kValue;
    }

    /**
     * Set the K value.
     *
     * @param kValue The K value to set
     */
    public void setKValue(int kValue) {
        this.kValue = kValue;
    }

    /**
     * Get the dataset.
     *
     * @return The dataset name
     */
    public String getDataset() {
        return dataset;
    }

    /**
     * Set the dataset.
     *
     * @param dataset The dataset name to set
     */
    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    /**
     * Validates if the dataset value is one of the allowed options.
     *
     * @return true if the dataset is valid, false otherwise
     */
    public boolean isValidDataset() {
        return "standard".equalsIgnoreCase(dataset) || "wearable".equalsIgnoreCase(dataset);
    }

    /**
     * Convert to string for debugging.
     *
     * @return A string representation of this object
     */
    @Override
    public String toString() {
        return "AnonymizationCommand{" +
                "kValue=" + kValue +
                ", dataset='" + dataset + '\'' +
                '}';
    }
}
