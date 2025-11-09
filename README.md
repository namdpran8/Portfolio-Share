Portfolio Share (NFC-HCE)

A simple Android application that uses Host Card Emulation (HCE) to share a portfolio link (or any URL) via NFC. Just save your link, and tap your phone to another NFC-enabled device to send it!




🚀 About This Project

This app was built as a project to learn and implement Host Card Emulation (HCE) on Android. Instead of just reading NFC tags, this app allows the phone to emulate a standard NDEF (NFC Data Exchange Format) tag.

When another NFC-enabled device (like another phone) comes near, it "reads" this phone as if it were a physical NFC tag and receives the URL saved in the app.

🛠️ Technology Used

Kotlin: The entire application is written in 100% modern Kotlin.

Android SDK: Built natively for Android (Min SDK 29+).

NFC Host Card Emulation (HCE): The core of the app, using HostApduService to respond to APDU commands from an NFC reader.

NDEF (NFC Data Exchange Format): The service emulates a standard NDEF Type 4 Tag, including:

Responding to the NDEF AID (D2760000850101).

Serving a Capability Container (CC) file.

Serving the NDEF message file containing the URL.

Android ViewBinding: Used in MainActivity for safe and easy access to UI components.

SharedPreferences: Used to persist the user's URL.

⚙️ How to Use

Clone the repository:

git clone [https://github.com/namdpran8/Portfolio-Share.git](https://github.com/namdpran8/Portfolio-Share.git)


Open in Android Studio: Open the cloned folder as a new project in Android Studio.

Build: Let Gradle sync, then build the project.

Run on a Physical Device: You must run this on a physical Android phone with NFC capabilities. The Android emulator does not support HCE.

Save Your Link: Open the app and enter the URL you want to share (e.g., your GitHub, LinkedIn, or personal portfolio).

Tap to Share: Unlock your phone and tap the back of it to another NFC-enabled phone. The other phone will receive your link!

🌟 Future Ideas

This project is a great foundation. Here are some features that could be added next:

Digital Business Card (vCard): Instead of just a URL, emulate a text/vcard NDEF record to share your full contact info (Name, Phone, Email, etc.).

NFC Reader Mode: Add enableForegroundDispatch to MainActivity to allow the app to read and copy links from other NFC tags.

Jetpack Compose UI: Since you're learning modern development, you could migrate the XML-based UI to Jetpack Compose.

Project by @namdpran8
