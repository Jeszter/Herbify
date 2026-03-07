
# 🌿 EcoScanner

**EcoScanner** is an Android mobile application that turns discovering plants into an interactive exploration game.  
The player opens a map, finds nearby plants, scans them, builds a collection, earns **ECO tokens**, upgrades tools, and tracks progress through a profile system.

The project combines:

- map-based exploration
- plant scanning
- collection mechanics
- cooldown rules
- in-game economy
- profile and progression
- event-driven gameplay

---

# ✨ Overview

EcoScanner is designed as a game-like educational app where users interact with nature through a stylized interface and exploration flow.

The core gameplay loop is simple:

1. Open the map
2. Find nearby plants
3. Move into scanning range
4. Scan the plant
5. Add it to the collection
6. Earn ECO rewards
7. Upgrade equipment and continue exploring

---

# 🎯 Main Features

## 🗺 Interactive Map

- plant objects displayed on a live map
- nearby objects around the player
- object selection from the map
- scan radius logic
- distance display

## 📷 Scanning System

- plant scanning through scanner flow
- support for camera-based or simplified scanning logic
- scan result screen
- collection integration after successful scan

## 🌱 Collection System

- scanned plants are stored in a personal collection
- collectible gameplay loop
- duplicate prevention through cooldown logic

## ⏱ Cooldown Mechanics

- **global scanner cooldown:** 2 minutes
- **same plant cooldown:** 24 hours

Prevents repeated farming of the same object.

## 💰 ECO Economy

- player receives **ECO tokens**
- tokens can be used in the shop
- rewards tied to gameplay progress

## 🛒 Shop

- upgrades
- boosters
- progression-related mechanics

## 👤 Profile

- level
- XP
- streak
- player stats
- wallet and events access through profile

## 🌍 Events

- event screen for live or global activities
- separate from bottom navigation
- accessible from profile

---

# 🎮 Gameplay Loop

```
Map → Select Plant → Scan → Result → Collection → Reward → Upgrade → Repeat
```

The application is structured around repeated exploration and collection.

The user is encouraged to keep scanning different plants instead of repeatedly scanning the same one.

---

# 🧠 Game Mechanics

## Plant Spawn Logic

Plants appear as map objects around the player.

Possible logic includes:

- static predefined map objects
- biome-based generation
- dynamic spawn around player coordinates

## Scanning Rules

A plant can be scanned only if:

- the player is close enough
- the scanner is not on global cooldown
- the plant itself is not on individual cooldown

## Cooldowns

| Type | Duration | Purpose |
|-----|----------|---------|
| Global scanner cooldown | 2 minutes | prevents scan spam |
| Same plant cooldown | 24 hours | prevents rescanning the same plant repeatedly |

## Reward System

After a successful scan, the player:

- receives ECO tokens
- gets XP
- adds a new plant to the collection
- progresses in profile stats

---

# 🏗 Architecture

The project uses a screen-based Compose architecture with centralized gameplay state.

### Main flow

```
MainActivity
      ↓
EcoScannerApp
      ↓
MapScreen / ScannerScreen / CollectionScreen / ShopScreen / ProfileScreen
      ↓
GameState
```

### Core responsibility split

- **MainActivity.kt** — app entry point and root navigation
- **GameState.kt** — gameplay state, cooldowns, XP, rewards, progression
- **MapScreen.kt** — map UI, object interaction, object selection
- **ScannerScreen.kt** — scanner flow and scan result
- **CollectionScreen.kt** — collected plants UI
- **ShopScreen.kt** — upgrades and boosters
- **ProfileScreen.kt** — player stats and navigation to wallet/events
- **EventsScreen.kt** — live/global event presentation
- **MapObjects.kt** — map objects and location data
- **PlantIdService.kt** — plant recognition integration/service layer
- **ui/theme/** — design system and styling

---

# 📁 Project Structure

```
app/
├── src/
│   └── main/
│       ├── java/com/example/ecoscanner/
│       │   ├── MainActivity.kt
│       │   ├── GameState.kt
│       │   ├── MapObjects.kt
│       │   ├── MapScreen.kt
│       │   ├── ScannerScreen.kt
│       │   ├── CollectionScreen.kt
│       │   ├── ShopScreen.kt
│       │   ├── ProfileScreen.kt
│       │   ├── EventsScreen.kt
│       │   ├── PlantIdService.kt
│       │   └── ui/theme/
│       │       ├── Color.kt
│       │       ├── Theme.kt
│       │       └── Typography.kt
│       ├── AndroidManifest.xml
│       └── res/
└── build.gradle.kts
```

---

# 🛠 Tech Stack

### Language

- **Kotlin**

### UI

- **Jetpack Compose**
- **Material 3**

### Camera

- **CameraX**

### Maps

- **OpenStreetMap**
- **OSMDroid**

### Android Components

- AndroidX
- Lifecycle
- Activity Compose

### Networking / Services

- plant identification service integration
- optional location and map APIs

---

# 🎨 UI / Design Direction

EcoScanner follows a custom **dark eco / sci-fi visual style**.

### Design characteristics

- dark background
- neon green accents
- game-like interface
- collectible card style
- profile and progression screens
- radar/map exploration visual language

### Main UI sections

- Map
- Scanner
- Collection
- Shop
- Profile
- Wallet
- Events

---

# 📱 Navigation

### Bottom Navigation

- Map
- Scanner
- Collection
- Shop
- Profile

### From Profile

- Wallet
- Events

This keeps the main navigation cleaner and matches the intended app structure.

---

# 🚀 Getting Started

## Requirements

- Android Studio or IntelliJ IDEA
- Android SDK
- Gradle
- Android 7.0+ device or emulator

## 1. Clone the repository

```
git clone https://github.com/your-username/ecoscanner.git
```

## 2. Open the project

Open the project in:

```
Android Studio / IntelliJ IDEA
```

## 3. Sync Gradle

Allow the IDE to download and sync all dependencies.

## 4. Run the app

Start the application on:

- physical Android device
- emulator

---

# ⚙️ Example Dependencies

```
dependencies {

implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
implementation("androidx.activity:activity-compose:1.8.0")

implementation(platform("androidx.compose:compose-bom:2023.10.01"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

implementation("androidx.camera:camera-camera2:1.3.0")
implementation("androidx.camera:camera-lifecycle:1.3.0")
implementation("androidx.camera:camera-view:1.3.0")

implementation("org.osmdroid:osmdroid-android:6.1.18")
implementation("com.google.android.gms:play-services-location:21.3.0")

}
```

---

# 🔐 Permissions

The app may require the following permissions:

```
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

# 📌 Current Implemented Functionality

- dark custom theme
- bottom navigation
- map screen
- plant markers
- scan flow
- collection logic
- shop screen
- profile screen
- cooldown system
- ECO reward system
- events access through profile

---

# 🛣 Roadmap

### Planned improvements

- improved map stability and smoother interaction
- cleaner marker rendering
- better scan result animations
- wallet polish
- advanced events system
- AR support
- improved plant recognition flow
- achievements
- encyclopedia mode
- cloud save / backend integration

---

# 🧪 Project Status

This project is currently in **active prototype / development stage**.

The current focus is:

- improving UI consistency
- stabilizing the map screen
- polishing scanner interactions
- aligning all screens with the original design concept

---

# 👨‍💻 Author

**Kyrylo Tarasov**

Student developer  
Android • Kotlin • Game mechanics

---

# 📄 License

This project is created for **educational and experimental purposes**.