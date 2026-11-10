# Project Plan

Application de contrôle pour vélo classique connecté via ESP32 Bluetooth Serial. Thème ultra-contrasté néon/noir avec design géométrique low-poly/isométrique. 3 pages : Paramètres (Adresse MAC ESP32), Accueil (Connexion BT, état verrouillé/déverrouillé, historique alarmes, terminal de logs, aperçu vélo isométrique, boutons CMD:LOCK, CMD:UNLOCK, etc.), Dashboard (Paysage plein écran, compteur vitesse central, horloge, raccourcis phares/alarmes).

## Project Brief

# Project Brief: Smart Bike ESP32 Controller

## Features

1. **ESP32 Bluetooth Serial Connectivity & Security Control**
   - Scans, pairs, and manages RFCOMM/SPP Bluetooth Serial connection using the target ESP32 MAC address.
   - Dispatches instant lock and unlock commands (`CMD:LOCK`, `CMD:UNLOCK`) and parses incoming state telemetry (`state:locked`, `state:unlocked`, alarm triggers).

2. **Interactive Home Hub & Live Log Terminal**
   - Visualizes lock/unlock status alongside a stylized low-poly/isometric bike preview.
   - Provides quick action toggles (lights, alarm) and a real-time console log terminal for monitoring raw serial communications and system events.

3. **Full-Screen Landscape Speedometer Dashboard**
   - Optimized landscape display featuring a prominent central speedometer parsing real-time speed data (`speed:XX`), system clock, and high-contrast quick-access controls for lighting and alarm management while riding.

4. **Bluetooth & App Configuration**
   - Dedicated settings screen for setting and updating the ESP32 MAC address, auto-reconnect preferences, and serial baud/timeout parameters.

## High-Level Tech Stack

- **Language & Asynchronous Runtime:** Kotlin, Kotlin Coroutines, and `StateFlow`/`SharedFlow` for non-blocking Bluetooth Serial I/O streaming and reactive UI state management.
- **UI Framework:** Jetpack Compose with custom low-poly/isometric graphics and an ultra-contrast dark neon visual theme.
- **Navigation & Adaptive Layouts:** 
  - **Jetpack Navigation 3** for state-driven UI navigation.
  - **Compose Material Adaptive** library for handling screen configurations (portrait main hub vs. landscape full-screen dashboard).
- **Connectivity:** Android Bluetooth Classic (`BluetoothSocket` RFCOMM / SPP Serial Protocol) for direct ESP32 hardware communication.

## Implementation Steps
**Total Duration:** 22h 53m 18s

### Task_1_Bluetooth_And_Data_Layer: Implement Bluetooth Serial (SPP/RFCOMM) connection manager, telemetry parser for serial protocol ('state:', 'speed:'), command dispatch ('CMD:LOCK', 'CMD:UNLOCK', lights, alarm), and settings repository for MAC address & preferences.
- **Status:** COMPLETED
- **Updates:** Bluetooth Serial socket manager, telemetry parser ('state:', 'speed:'), command dispatch ('CMD:LOCK', 'CMD:UNLOCK', etc.), and SettingsRepository (DataStore) implemented and verified with unit tests.
- **Acceptance Criteria:**
  - Bluetooth Serial socket manager connects to target ESP32 MAC address
  - Parses incoming serial streams ('state:locked', 'state:unlocked', 'speed:XX') into StateFlow/SharedFlow
  - Sends commands ('CMD:LOCK', 'CMD:UNLOCK', light/alarm toggles) over SPP socket
  - Settings repository persists ESP32 MAC address and connection preferences
- **Duration:** 3m 18s

### Task_2_Theme_Navigation_And_Home_Settings_UI: Create dark neon visual theme, setup screen navigation, build Settings screen, and develop Home Hub screen with isometric bike preview, lock/unlock controls, and real-time serial log terminal.
- **Status:** COMPLETED
- **Updates:** Ultra-contrast dark neon theme, screen navigation (Home, Settings, Dashboard), Settings screen with MAC address persistence, and Home Hub screen with custom Canvas isometric bike preview, status card, action grid, terminal log section, and navigation switcher successfully implemented and verified.
- **Acceptance Criteria:**
  - Dark neon Material3 theme applied consistently
  - Navigation configured for Settings, Home Hub, and Fullscreen Dashboard
  - Settings screen allows updating ESP32 MAC address and connection settings
  - Home Hub displays lock status, stylized low-poly/isometric bike graphic, quick action toggles, and live raw console log terminal
- **Duration:** 5m 23s

### Task_3_Fullscreen_Dashboard_UI: Implement full-screen landscape speedometer dashboard with central speed gauge/display, real-time clock, and high-contrast quick action controls for riding.
- **Status:** COMPLETED
- **Updates:** Full-screen landscape dashboard screen, auto-landscape orientation effect, system bar hiding, central geometric speedometer canvas, live digital clock, triangular/isometric light and alarm shortcuts, lock state toggle, and discrete exit button successfully implemented and verified with unit tests.
- **Acceptance Criteria:**
  - Landscape dashboard screen with high-contrast UI for outdoor riding
  - Prominent central speedometer displaying real-time speed parsed from 'speed:XX'
  - System clock and quick-access buttons for lights, alarm, and lock status
- **Duration:** 3m 26s

### Task_4_Run_And_Verify: Build the project, verify application stability without crashes, confirm all features align with project brief, make sure all existing tests pass, build pass, and instruct critic_agent to verify UI and stability.
- **Status:** COMPLETED
- **Updates:** All 3 pages (Home, Settings, Landscape Dashboard), strict monochrome design, RX packet parsing (speed, distance, lock, break, light, lastalarme), TX command dispatch, presets grid, sliders (color:HEX, brightness:VAL), orientation handling, assembleDebug, and testDebugUnitTest completed and verified successfully.
- **Acceptance Criteria:**
  - build pass
  - app does not crash
  - make sure all existing tests pass
  - critic_agent verifies application stability, no crashes, alignment with user requirements, and no critical UI issues
- **Duration:** 22h 41m 11s

