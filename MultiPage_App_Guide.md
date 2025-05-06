# AnonPy Multi-Page App User Guide

This document provides a comprehensive guide to the newly redesigned AnonPy app with multiple pages for better organization and user experience.

## App Overview

The app has been redesigned with a multi-page structure to improve organization and user experience:

1. **Home Page**: Main entry point with navigation to all sections
2. **Check Your Data Page**: For viewing and loading CSV data
3. **Anonymization Page**: For selecting K values and viewing anonymized results
4. **Settings Page**: For viewing connection status and app information

## Navigation Structure

The app uses the Android Navigation Component to handle transitions between pages. Each page is implemented as a Fragment, which improves memory usage and performance compared to using multiple Activities.

### Home Page

The Home Page provides:
- Welcome message explaining the app's purpose
- App logo and description
- Navigation buttons to all other pages
- Version information at the bottom

### Check Your Data Page

The Data Page includes:
- "Read CSV File" button that loads and displays your data
- Scrollable output area for viewing the data
- Progress indicator while loading data
- Back button to return to the Home Page

### Anonymization Page

The Anonymization Page features:
- Horizontal scrolling row of k-value buttons (2, 5, 10, 30, 50, 500)
- Result display area showing anonymized data
- Progress indicator during anonymization
- Back button to return to the Home Page

### Settings Page

The Settings Page displays:
- SmarKo Watch connection status (currently shows "Not Connected")
- MQTT broker connection status (updates in real-time)
- App version information
- Back button to return to the Home Page

## MQTT Integration

The app maintains the MQTT functionality from the previous version but enhances it with:

1. Better visual feedback on connection status in the Settings Page
2. Automatic navigation to the Anonymization Page when receiving valid commands
3. Error dialogs for invalid commands with specific error messages

Valid MQTT commands still follow the format `K Value = X` where X must be one of: 2, 5, 10, 30, 50, 500.

## Testing the Application

### Manual Testing

1. Launch the app and navigate through all pages using the buttons
2. On the Data Page, click "Read CSV File" to load and view your data
3. On the Anonymization Page, select different K values to view how they affect anonymization
4. Check the Settings Page to see the connection status of MQTT

### MQTT Command Testing

Use the previously provided Python script to test MQTT commands:

```
python mqtt_test_valid_invalid.py --valid    # Test only valid values
python mqtt_test_valid_invalid.py --invalid  # Test only invalid values
```

When receiving a valid command, the app will:
1. Navigate to the Anonymization Page
2. Automatically start anonymization with the received K value
3. Display the results

## Implementation Notes

- The app uses View Binding for type-safe access to UI elements
- Fragment Result API is used for communication between MainActivity and Fragments
- The Navigation Component handles fragment transitions and back stack management
- Connection status is displayed using color coding (green for connected, red for disconnected)

## Future Enhancements

Future versions could include:
- SmarKo Watch integration for collecting and anonymizing wearable data
- Additional anonymization algorithms beyond Mondrian
- Customizable anonymization parameters
- Data visualization tools for comparing anonymized vs. original data
- Export functionality for anonymized results
