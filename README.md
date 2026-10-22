# BlueMice — BLE HID Remote for Android

<p align="center"><b>Kotlin · Bluetooth LE · HID · Jetpack Compose</b></p>

Turn your Android phone into a **wireless mouse, keyboard, gamepad and presentation remote**. BlueMice speaks real **BLE HID**, so your computer sees it as a standard input device — no companion app needed on the other side.

## ✨ Features

- 🖱️ **Mouse mode** — touchpad pointer with clicks and scroll
- ⌨️ **Keyboard mode** — multi-language layouts plus an inline text editor (`LanguageKeyboard`, `InlineEditor`)
- 🎮 **Gamepad mode** — configurable gamepad layout and mapping (`GamepadScreen`, `GamepadConfig`)
- 📊 **Presenter mode** — next / previous slide controls for meetings
- 🛠️ **Custom layouts** — build and edit your own control layouts (`LayoutManager`, `LayoutDefinition`)
- 🎨 **Design system** — colour picker and high-contrast, accessible themes (`DesignSystem`, `ColorContrast`)
- ⚙️ **Settings** — persistent preferences (`SettingsManager`)

## 🛠️ Tech Stack

- Kotlin + Jetpack Compose
- BLE HID stack — `BleHidService`, `HidReportBuilder`, `BluetoothHidService`
- Connection handling — `ConnectionManager`, `HidDeviceManager`, `ConnectionState`
- Screen-per-mode UI with a shared design system

## 📦 Build & Install

1. Open the project in [Android Studio](https://developer.android.com/studio)
2. Build: `./gradlew assembleDebug`
3. Install: `adb install app/build/outputs/apk/debug/app-debug.apk`
4. Pair the phone with your computer over Bluetooth, open BlueMice and connect

## 🤖 Built with AI vibe coding

Designed, coded and tested end to end with **AI vibe coding** — AI prompt coding gives faster results than manual processing.
