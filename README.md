# ⚠️ IMPORTANT NOTE

**This project is a ChatGPT AI experiment / side project — essentially an “AI slop” / proof-of-concept.**  
It exists because the Android TV platform *itself* fails to provide a usable solution for app switching.

The code works, the app is useful — but the need for this app highlights a platform-level UX regression.

---

# Recents‑App‑Switcher (RAS)

**Recents‑App‑Switcher** (short: **RAS**) is a utility app for **Android TV / Google TV (Android 11–14)**  
that solves a problem intentionally left unsolved by Google and many device manufacturers:

> 👉 **On many Android TV devices, the system “Recent Apps / App Switcher” simply does not exist.**

This app provides a **clean, non-root, system-compliant workaround**.

---

## Why does this app exist?

On phones, app switching is trivial.  
On **Android TV / Google TV**, reality looks like this:

- ❌ No system “Recents” UI on many devices  
- ❌ `KEYCODE_APP_SWITCH` / `RECENTS` does nothing  
- ❌ `com.android.systemui.TOGGLE_RECENTS` is removed or blocked  
- ❌ Android 14 forbids third-party apps from killing or managing other apps  
- ❌ Launchers intentionally hide multitasking to simplify remote control UX  

**Result:**  
👉 No fast way to switch between two apps (e.g. YouTube ↔ Jellyfin ↔ Browser)

**Recents‑App‑Switcher exists solely to fix that gap.**

---

## What does Recents‑App‑Switcher do?

### ✅ Core Features

- 📜 **Shows recently used apps**
  - based on `UsageStatsManager` (official, allowed API)
- 🔁 **Alt-Tab–like behavior**
  - jump back to the last used app instantly
- 🚀 **Launch apps directly**
  - click an entry → app opens
- 🚫 **Exclude apps**
  - long-press → app is marked red and ignored
  - perfect for launchers & system apps
- 💾 **Persistent storage**
  - exclusions & last app survive reboots
- 🧩 **Intent-based control**
  - ideal for automation & key remapping
- ♿ **Accessibility optional**
  - not required for core functionality

---

## What this app intentionally does NOT do

Due to **Android 14 platform restrictions**:

- ❌ No “Close all apps”
- ❌ No killing or force-stopping other apps
- ❌ No hidden SystemUI intents
- ❌ No root access required

> 💡 Everything this app does is **Google-compliant**, stable, and update-safe.

---

## Requirements

- Android TV / Google TV (Android 11+ recommended)
- **Usage Access permission**
  - the app guides you there automatically on first launch

Optional:
- **Key Mapper** (open-source)

---

## Quick Start

1. Install APK (ADB or file manager)
2. Launch the app
3. Grant **Usage Access**
4. Done 🎉

---

## Using Recents‑App‑Switcher with Key Mapper (recommended)

With **Key Mapper**, RAS behaves like a real system feature.

---
### 🔁 Last App (Alt‑Tab behaviour)

To map a physical button (or IR remote key) to the **"Switch to last app"** action using Key Mapper, create a custom intent with the following fields:

| Field    | Value                                   |
|---------|-----------------------------------------|
| **Package** | `eu.recentsopener`                      |
| **Action**  | `eu.ras.SHOW_LAST_APP`                  |

The intent action `eu.ras.SHOW_LAST_APP` is defined in the app’s manifest and will immediately switch back to the last used application.  After you save the mapping in Key Mapper, pressing the configured key acts like an Alt‑Tab on Android TV (e.g. quickly toggling between YouTube and Jellyfin).

*(Placeholder for Key Mapper screenshot showing the Last App intent configuration — insert image here)*

---

### 📜 Open Recent Apps List

To open the Recent App Switcher list itself via Key Mapper, configure another custom intent:

| Field    | Value                                   |
|---------|-----------------------------------------|
| **Package** | `eu.recentsopener`                      |
| **Action**  | `eu.ras.SHOW_RECENTS`                   |

Invoking the action `eu.ras.SHOW_RECENTS` launches the recents list UI.  From there you can scroll through your recently used apps, press **Enter**/**OK** to launch an app, or **long‑press** to exclude it.  Excluded apps appear in red and are ignored by the switcher.

*(Placeholder for Key Mapper screenshot showing the Recents List intent configuration — insert image here)*

You can install **Key Mapper** from [F‑Droid](https://f-droid.org/de/packages/io.github.sds100.keymapper/) or download the latest APK from the [GitHub releases page](https://github.com/keymapperorg/KeyMapper/releases).  Refer to the Key Mapper documentation for details on creating custom intent mappings.

---

## App List UI Explained

Each entry shows:

- 📱 App icon  
- 🏷 App name  
- 📦 Package ID (in parentheses)

### Actions

- **Short press:** Launch app  
- **Long press:**  
  Mark app red → excluded from switching & lists

---

## Accessibility — why does it still exist?

Currently:
- 🔧 **Not required**
- 📴 Can stay disabled

Potential future use:
- Automated navigation inside system settings
- Support for extremely restricted TV firmwares
- Experimental UI automation (optional only)

---

## Build & Releases

- GitHub Actions automatically build:
  - Debug APK
  - Release APK
- Release APK is published under **GitHub Releases**
- No Android Studio required

---

## Project Goal

**Recents‑App‑Switcher is not a hack.**  
It is a **utility born out of necessity** to restore basic multitasking on Android TV —  
as far as the platform still allows.

---

## License

Open Source.  
Use it, fork it, improve it 🙂

PRs, ideas, and discussions are welcome.
