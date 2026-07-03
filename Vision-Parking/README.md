# VisionPark - Smart Parking Android App

Complete guide to setup and run the VisionPark Android application on Linux (Ubuntu) and Windows.

## Overview

VisionPark is an Android mobile application that allows users to find and book parking spots in real-time. The app integrates with Google Maps for location services and connects to a Flask backend for parking management.

## Tech Stack

- **Language:** Java/Kotlin
- **Build System:** Gradle with Kotlin DSL
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35 (Android 15)
- **Maps:** Google Maps SDK for Android
- **Location:** Google Play Services Location 21.0.1
- **Places:** Google Places API
- **HTTP Client:** Retrofit 2.9.0 with Gson converter
- **Testing:** JUnit, Espresso, Appium

## Documentation

### 📚 Quick Links

- **[Product Requirements Document (PRD)](./parking-app-prd.md)** - Complete product specifications and UI/UX guidelines
- **[User App REST API Specs](../REST_API_Specs/USER_APP_REST_API_SPECS.md)** - API endpoints and request/response formats
- **[Backend Setup Guide](../Backend/README.md)** - Backend installation and configuration
- **[Switch to EC2 Guide](./README_SwitchToEC2Guide.md)** - Configure app for AWS EC2 backend
- **[Test Plan & Setup](./README_Test_Plan_Setup.md)** - E2E testing with Appium
- **[Test Report](./README_Test_Report.md)** - Test execution results and coverage

## Quick Start

### Prerequisites
- Android Studio (latest version)
- JDK 17 or higher
- Android SDK (API 24-35)
- Google Maps API key
- Backend server running (see [Backend Setup](../Backend/README.md))

### Setup Steps
1. Clone the repository
2. Open project in Android Studio
3. Add Google Maps API key to `local.properties`:
   ```
   MAPS_API_KEY=your_api_key_here
   ```
4. Sync Gradle dependencies
5. Run on emulator or device

## Getting Started

This guide covers Android Studio setup, project configuration, and running the app on both **Linux (Ubuntu)** and **Windows**.

---

## 1. Steps to Setup Android Studio

### a. Navigate to the official website for downloading the Android Studio

[https://developer.android.com/studio](https://developer.android.com/studio)

### b. Download Android Studio:

- **Linux:** Click *Download*. You’ll get a `.tar.gz` archive.
- **Windows:** Click *Download*. You’ll get an `.exe` installer.

### c. Install Android Studio

#### **Linux**

Open your terminal and navigate to Downloads folder.

```bash
cd Downloads
tar -zxvf android-studio-*.tar.gz
```
*(replace `android-studio-*.tar.gz` with the actual filename)*

#### **Windows**

- Double-click the downloaded `.exe` file.
- Follow the wizard steps, accept the terms, and choose the install location (default is fine).
- Complete installation.

### d. Move the extracted folder to more permanent place (Linux only, optional)

```bash
sudo mv android-studio /opt/
```

### e. Run the Android Studio Startup Script

- **Linux:**
```bash
cd /opt/android-studio/bin
./studio.sh
```
- **Windows:** Launch *Android Studio* from Start Menu or Desktop.

### f. Check if it installed

- **Linux:**
```bash
android-studio
```
- **Windows:** Verify Android Studio launches successfully.

### g. Create a Desktop Entry (Linux only, optional/recommended for Easy Launching)

```bash
mkdir -p ~/.local/share/applications/
nano ~/.local/share/applications/android-studio.desktop
```

Paste the following content (adjust Path and Exec lines if not using /opt):

```ini
[Desktop Entry]
Name=Android Studio
Comment=The official IDE for Android development
Exec=/opt/android-studio/bin/studio.sh
Icon=/opt/android-studio/bin/studio.png
Type=Application
Terminal=false
Categories=Development;IDE;
StartupWMClass=jetbrains-studio
```

Save and exit (`Ctrl+O`, Enter, then `Ctrl+X`).  
Update desktop database:

```bash
sudo update-desktop-database ~/.local/share/applications/
```

### **Important Considerations**

#### a. **Java Development Kit (JDK)**

- **Linux:** Android Studio may prompt for JDK download. You can install manually:
```bash
sudo apt update
sudo apt install openjdk-17-jdk # Or a later version
```
- **Windows:** Android Studio includes OpenJDK. *(If needed, download from [https://adoptopenjdk.net](https://adoptopenjdk.net) and install manually.)*

#### b. **System Requirements**

- Ensure your Ubuntu/Windows system meets minimum requirements: RAM (8GB+ recommended), disk space (4GB+), compatible CPU.

---

## 2. Steps to Install Emulator Inside Android Studio

### a. Click the Device Manager icon (top-right).

### b. Click the **+** icon and select **Create Virtual Device**.

### c. Select a device definition, click Next, choose a system image, then Next.

### d. Name your emulator and click Finish (or Install if system image needs downloading).

### e. Find/emulate in Device Manager.

### **Recommended AVD configuration for this project**
- **Device profile:** Pixel 2 (or any 5.0" phone profile)
- **System image / API level:** API 30+ (Android 11+) with **Google APIs** (minSdk: 24, targetSdk: 35)
- **ABI:** x86_64
- **Screen resolution:** 1080 × 1920 (420 dpi)
- **Services:** Google APIs (required for Maps/location)
- **RAM & CPU cores:** 4GB RAM & max CPU cores
- **Options:** Enable hardware accelerated virtualization (Intel HAXM or KVM on Linux)

#### Why this configuration?
- Uses Google Maps/Places services (Google APIs required).
- API 30+ provides better compatibility with targetSdk 35.
- x86_64 images run faster under virtualization.
- Matches Pixel 2 layout for reliable UI testing.

#### **If system image isn’t downloaded:** Click **Install** and wait for download.

#### **If you wish to test multiple densities/versions:** Create extra AVDs and keep "Allow multiple instances" enabled in Run configuration.

---

#### **Emulator Performance Tips (Linux & Windows)**

##### **Linux: KVM Virtualization**
Check CPU support:
```bash
egrep -c '(vmx|svm)' /proc/cpuinfo
```
Install and enable KVM:
```bash
sudo apt update
sudo apt install qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils
sudo adduser $USER kvm
sudo adduser $USER libvirt
```
Log out and back in to apply groups  
Verify:
```bash
virsh -c qemu:///system list --all
```

##### **Windows: Hyper-V/Virtual Machine Platform**
- Enable Hyper-V in Windows Features or use Intel HAXM.

##### **Launch emulator from terminal**
**Linux:**
```bash
$ANDROID_SDK_ROOT/emulator/emulator -avd Your_AVD_Name -gpu host -memory 2048 -no-boot-anim
```
**Windows:**
```bash
emulator -avd Your_AVD_Name -gpu host -memory 2048 -no-boot-anim
```

---

## 3. Steps to Create a Basic Project

a. Click three horizontal bars (top left).

b. Go to **New > New Project** then select the **Empty View Activity**.

c. Click Next, name your project, and optionally provide base package name.

d. Select location, language (Kotlin/Java), click Finish.

e. Project build starts; click **Run** (top middle) after build finishes.

f. A Hello World activity will appear.

---

## 4. Steps to Clone the Car Parking Project

a. **Clone the Repository:**

- **Linux (Terminal):**
```bash
git clone https://github.com/neeraj975arora/parking_app_integration.git
```
- **Windows (Git Bash/Prompt):**
```bash
git clone https://github.com/neeraj975arora/parking_app_integration.git
```
*(GitHub Desktop also works for GUI cloning.)*

b. **Open Project in Android Studio:**
- Launch Android Studio.
- Select **"Open an existing project"** on the welcome screen.
- Navigate to the cloned repository folder.
- Click **"Open"**.

c. **Wait for Gradle Sync:**  
Android Studio will sync dependencies (check status bar).

d. **Build the Project:**  
Menu → **Build > Make Project**. Watch "Build" output for errors.

e. **Run the Application:**  
Use the Run (▶) button or **Run > Run 'app'**.

### Edit Run/Debug Configurations:

1. **Select Android App Configuration:**  
   Left pane → *Android App → app*. If missing, click + to add.

2. **Module:**  
   Set to correct module (e.g., `VisionPark.app` or just `app`).

3. **Installation Options:**
- Deploy: Default APK
- Install for all users: Optional
- Always install with package manager: Optional
- Clear app storage before deployment: Optional
- Install Flags: Advanced (e.g., `-r`, `-d`, `-t`)

4. **Launch Options:**
- Launch: Default Activity or specify exact activity
- Launch Flags: Optional

5. **Miscellaneous:**
- Allow multiple instances: Enables multi-device testing
- Store as project file: Shares config across team

6. **Before Launch:**
- Ensure Gradle-aware Make or Make/Assemble task present

7. **Debugger:**
- Default debugger settings suffice

### Examples / Tips

- Launch a specific activity: set *Specified Activity* field to full activity name.
- Enable *Clear app storage before deployment* for fresh testing.
- For ADB install errors: try *APK from app bundle* or add `-r` install flag.
- For multi-device runs, enable *Allow multiple instances*.

Pick your device/emulator and click **Run**.

---

## 5. Steps to Get the Google Map API Key

- Create Google Cloud account: [https://console.cloud.google.com/](https://console.cloud.google.com/)
- Create new project
- Enable the following APIs:
  - **Maps SDK for Android**
  - **Places API** (required for search functionality)
- Go to *Credentials* tab, create and copy API key
- Add API key to `local.properties` file:
  ```
  MAPS_API_KEY=your_api_key_here
  ```

---

## 6. Android Emulator & Build Performance Tips

Tips for Ubuntu, Windows, Android Studio, VS Code, and Android Emulator.

### 6.1 Quick Checklist

1. **Use a real device (fastest):** USB debug with `adb` or *scrcpy*.
2. **Enable virtualization (KVM/Hyper-V):**
- Linux: see above
- Windows: Enable Hyper-V in “Windows Features”
3. **Switch AVD to x86_64 with host GPU** (not ARM), enable snapshots/quick-boot.
4. **Reduce emulator RAM/resolution/features**.
5. **Tweak Android Studio & Gradle settings**.
6. **Close heavy apps/reduce IDE indexing**.

### 6.2 Concrete Steps & Commands

#### Linux: Virtualization (KVM)

```bash
egrep -c '(vmx|svm)' /proc/cpuinfo
```
Install KVM as shown in previous section

#### Windows: Hyper-V

- Enable Hyper-V/Virtual Machine Platform via Control Panel → “Turn Windows features on or off”.

#### Use x86_64 emulator image + host GPU (both OS)

- Use AVD Manager: Pick x86_64 image, host GPU, set memory 1536–2048 MB, enable quick boot snapshots.

#### If machine has low RAM (<= 8 GB)
- Prefer real device (USB debug).
  - Windows: Use *scrcpy* ([https://github.com/Genymobile/scrcpy](https://github.com/Genymobile/scrcpy)), installable via Windows installer.
  - Linux: `sudo apt install scrcpy`

#### Gradle performance tweaks

In `~/.gradle/gradle.properties` or project `gradle.properties`:
```bash
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx1536m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.caching=true
```
Increase/decrease `-Xmx` as needed by RAM.

#### Build from terminal (if Android Studio is slow):

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Improve Android Studio performance

- Increase IDE heap (Settings → Memory).
- Disable unused plugins.
- Exclude large folders from indexing (right-click → Mark Directory As → Excluded).
- Disable Instant Run/Apply Changes if slow.

#### AVD configuration tips

- Use small device profiles, disable Google Play if unnecessary.
- Disable animations for speed.
- Try fallback GPU on Linux (`-gpu swiftshader_indirect`) if host GPU causes issues.

#### Avoid double-indexing apps

- Use VS Code for editing, Android Studio for builds as needed.
- Turn off automatic Gradle sync on file changes.
- Close Chrome/other heavy apps while emulating/building.

#### System-level improvements

- Use SSD for huge speedup.
- Add RAM (16GB+ ideal).
- Use zram/increase swap only if desperate.

#### Diagnose freezes

- Use `htop`/System Monitor for real-time info.
- Check `dmesg` for errors.
- Use Android Studio Profiler for app issues.

---

## 7. Backend Integration

### 7.1 Backend Setup

The backend application is fully containerized with Docker. For complete setup instructions, see:
- **[Backend README](../Backend/README.md)** - Docker setup, database initialization, and API documentation

### 7.2 API Integration

For all API endpoints, request/response formats, and authentication details, see:
- **[User App REST API Specs](../REST_API_Specs/USER_APP_REST_API_SPECS.md)** - Complete API documentation

### 7.3 Switching Between Local and EC2 Backend

To configure the app to connect to AWS EC2 server instead of localhost, refer to:
- **[Switch to EC2 Guide](./README_SwitchToEC2Guide.md)** - Step-by-step backend switching guide

---

## 9. Steps to Setup PostgreSQL

### 1. Install PostgreSQL

#### **Linux**
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install postgresql postgresql-contrib -y
sudo systemctl start postgresql
sudo systemctl enable postgresql
sudo systemctl status postgresql
```

#### **Windows**
- Download and run the official installer: [https://www.postgresql.org/download/windows/](https://www.postgresql.org/download/windows/)
- Choose install directories, set postgres password, complete wizard.
- Service starts automatically. Check via **Services.msc** (“postgresql-x64-XX” running).
- Access PostgreSQL shell:
  - Run *SQL Shell (psql)* from Start Menu.
  - Set password during setup if prompted.

### 2. Configure PostgreSQL

#### **Linux**
```bash
sudo -i -u postgres
psql
ALTER USER postgres PASSWORD 'your_password';
\q
exit
```

#### **Windows**
- Run *SQL Shell (psql)*, login as user `postgres`.
- Enter password (set during install or `ALTER USER` command).
- Use SQL commands as above.

### 3. Installing the GUI Tool pgAdmin for PostgreSQL

#### **Linux**
```bash
curl https://www.pgadmin.org/static/packages_pgadmin_org.pub | sudo tee /etc/apt/trusted.gpg.d/pgadmin.asc
sudo sh -c 'echo "deb https://ftp.postgresql.org/pub/pgadmin/pgadmin4/apt/$(lsb_release -cs) pgadmin4 main" > /etc/apt/sources.list.d/pgadmin4.list'
sudo apt update
sudo apt install pgadmin4 -y
```

#### **Windows**
- Download & install from [https://www.pgadmin.org/download/](https://www.pgadmin.org/download/)
- Follow wizard, set email/password.

### 4. Configure And Access pgAdmin

#### **Linux**
```bash
sudo /usr/pgadmin4/bin/setup-web.sh
```
Browse to [**http://localhost/pgadmin4**](http://localhost/pgadmin4), login with setup credentials.

#### **Windows**
- Start *pgAdmin 4* from Start menu.

### 5. Connect to a New Server Connection

In pgAdmin (Linux & Windows):

a. Right-click *Servers* → Create → Server...

b. General tab: Name your server (e.g., MyDatabaseServer)
c. Connection tab:
- Host: `localhost` (or server IP)
- Port: `5432`
- Maintenance DB: your db name
- Username: `postgres` (or chosen user)
- Password: as set above
  d. Save password if desired.
  e. Click **Save**.

### 6. Access a Specific Database

a. Expand the server you just added.
b. Expand *Databases*, select your db.
c. Click SQL icon (⚡) to open Query Tool.
d. Run queries.

---

**Now we have done with all the necessary steps. You can successfully run the Car Parking app in Android Studio across Linux or Windows, with backend and database setup (local or AWS EC2).**

---
## 10. Testing

### 10.1 Unit Tests
Run unit tests using Android Studio or Gradle:

**Linux/Mac:**
```bash
./gradlew test
```

**Windows:**
```bash
gradlew.bat test
```

### 10.2 Instrumented Tests
Run instrumented tests on connected device/emulator:

**Linux/Mac:**
```bash
./gradlew connectedAndroidTest
```

**Windows:**
```bash
gradlew.bat connectedAndroidTest
```

### 10.3 Build APK

**Linux/Mac:**
```bash
./gradlew assembleDebug
```

**Windows:**
```bash
gradlew.bat assembleDebug
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

### 10.4 E2E Testing with Appium

For comprehensive E2E testing setup and execution:
- **[Test Plan & Setup Guide](./README_Test_Plan_Setup.md)** - Appium setup, test scenarios, and execution
- **[Test Report](./README_Test_Report.md)** - Test results and coverage

Run E2E tests:
```bash
# Linux/Mac
./run_e2e.sh

# Windows
pytest tests/ -v
```

## 11. Project Structure

```
Vision-Parking/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/visionpark/
│   │   │   │   ├── activities/          # Activity classes
│   │   │   │   ├── adapters/            # RecyclerView adapters
│   │   │   │   ├── models/              # Data models
│   │   │   │   ├── network/             # API client (Retrofit)
│   │   │   │   └── utils/               # Utility classes
│   │   │   ├── res/                        # Resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml      # App manifest
│   │   └── test/                        # Unit tests
│   └── build.gradle.kts                 # App-level build config
├── tests/                               # E2E tests (Appium)
├── gradle/                              # Gradle wrapper
├── build.gradle.kts                     # Project-level build config
├── settings.gradle.kts                  # Project settings
├── local.properties                     # API keys (not in git)
├── parking-app-prd.md                   # Product requirements
├── README.md                            # This file
├── README_SwitchToEC2Guide.md           # EC2 backend guide
├── README_Test_Plan_Setup.md            # Test setup guide
└── README_Test_Report.md                # Test results

```

## 12. References

### External Resources
- **Android Studio:** [https://developer.android.com/studio](https://developer.android.com/studio)
- **Git:** [https://git-scm.com/](https://git-scm.com/)
- **PostgreSQL:** [https://www.postgresql.org/](https://www.postgresql.org/)
- **pgAdmin:** [https://www.pgadmin.org/](https://www.pgadmin.org/)
- **Google Cloud Console:** [https://console.cloud.google.com/](https://console.cloud.google.com/)
- **Google Maps SDK:** [https://developers.google.com/maps/documentation/android-sdk](https://developers.google.com/maps/documentation/android-sdk)
- **Appium:** [https://appium.io/](https://appium.io/)

### Project Documentation
- **[Product Requirements (PRD)](./parking-app-prd.md)**
- **[API Specifications](../REST_API_Specs/USER_APP_REST_API_SPECS.md)**
- **[Backend Setup](../Backend/README.md)**
- **[EC2 Configuration](./README_SwitchToEC2Guide.md)**
- **[Testing Guide](./README_Test_Plan_Setup.md)**
