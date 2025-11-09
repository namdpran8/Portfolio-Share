

# 📱 Portfolio Share (NFC-HCE)

[![Made with Kotlin](https://img.shields.io/badge/Made%20with-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/Platform-Android-green?logo=android&logoColor=white)](https://developer.android.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-29%2B-orange)]()

---

A simple Android application that uses **Host Card Emulation (HCE)** to share a **portfolio link (or any URL)** via NFC.  
Just save your link, and tap your phone to another NFC-enabled device to send it instantly! ⚡

---

## 🖼️ App Screenshot  

*(Add your app’s main screen here!)*  
![App Screenshot](screenshot.png)

---

## 🚀 About This Project  

**Portfolio Share (NFC-HCE)** was built as a project to explore and implement **Host Card Emulation (HCE)** on Android.  
Unlike traditional NFC reader apps, this app lets your phone **act as an NFC tag itself**, broadcasting a URL as a valid **NDEF (NFC Data Exchange Format)** message.  

When another NFC-enabled device (like a smartphone) comes near, it “reads” this phone as if it were a real NFC tag — automatically opening the shared URL.  

---

## 🛠️ Technology Used  

| Technology | Description |
|-------------|-------------|
| 🧑‍💻 **Kotlin** | 100% modern Kotlin used for development |
| 📱 **Android SDK** | Native Android app (Min SDK 29+) |
| 💳 **Host Card Emulation (HCE)** | Core NFC logic using `HostApduService` to handle APDU commands |
| 🧾 **NDEF (NFC Data Exchange Format)** | Emulates a Type 4 Tag to send NDEF messages |
| 🧩 **ViewBinding** | Type-safe and efficient access to UI components |
| 💾 **SharedPreferences** | Stores and retrieves the user's saved URL |

---

## ⚙️ How It Works  

1. The app uses **HCE** to emulate a virtual NFC tag.  
2. When another NFC reader (like another phone) comes near:
   - It selects the **NDEF AID** (`D2760000850101`)
   - Reads the **Capability Container (CC) file**
   - Fetches the **NDEF message** that contains your saved URL  
3. The receiver device automatically recognizes it as a valid NFC link and offers to open it in a browser.

---

## 🧭 How to Use  

1. **Clone this repository**
   ```bash
   git clone https://github.com/namdpran8/Portfolio-Share.git
   cd Portfolio-Share
````

2. **Open in Android Studio**

   * Launch Android Studio and open the project folder.

3. **Build the project**

   * Let Gradle sync automatically, then build the project.

4. **Run on a physical device**

   > ⚠️ *The Android emulator does not support NFC or HCE. Use a real NFC-enabled phone.*

5. **Save your link**

   * Enter your portfolio, LinkedIn, GitHub, or any URL.

6. **Tap to share**

   * Unlock your phone and tap it on another NFC-enabled device.
   * The other device will receive your link instantly!

---

## 🌟 Future Ideas

💡 This app is a strong foundation for advanced NFC-based sharing systems.
Here are some potential enhancements:

* 🪪 **Digital Business Card (vCard)**
  Emulate and share a full contact card (`text/vcard`) — including name, email, and phone.

* 📡 **NFC Reader Mode**
  Add `enableForegroundDispatch` to allow the app to read NFC tags and import links.

* 🎨 **Jetpack Compose UI**
  Rebuild the interface with Compose for a modern and reactive experience.

* 🔐 **Advanced Features**

  * Multi-link profile support
  * Tap history
  * QR code fallback
  * Encryption for sensitive data

---

## 🧠 Key Concepts in This Project

| Concept                          | Description                                                                |
| -------------------------------- | -------------------------------------------------------------------------- |
| **HCE (Host Card Emulation)**    | Allows Android devices to emulate NFC smartcards                           |
| **APDU Commands**                | Binary commands used to communicate between NFC readers and emulated cards |
| **AID (Application Identifier)** | Uniquely identifies an NFC applet (default: `D2760000850101`)              |
| **NDEF Message**                 | The data payload that holds the shared URL or text                         |
| **Type 4 Tag**                   | NFC tag specification used for data exchange between devices               |

---

## 🧰 Requirements

* Android 10 (API 29) or higher
* NFC-enabled Android device
* Android Studio (latest stable version)

---

## 🧑‍💻 Author

**👋 Project by [@namdpran8](https://github.com/namdpran8)**

If you found this project useful, please ⭐ **star** the repository — it helps a lot!
Feel free to fork and experiment with your own HCE-based ideas.

---

## 📜 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

````

---

### ✅ How to Use It:
1. Copy this entire block and paste it into a new file named `README.md` in your project root.  
2. Add your app screenshot as `screenshot.png` (or any name you prefer).  
3. Commit and push it:
   ```bash
   git add README.md screenshot.png
   git commit -m "Added professional project README"
   git push origin main
````


