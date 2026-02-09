# ⚠️ IMPORTANT NOTE

**This project is a ChatGPT AI experiment / side project — essentially an “AI flop” / proof-of-concept.**  
It exists because the Android TV platform *itself* fails to provide a usable solution for app switching.

The code works, the app is useful — but the need for this app highlights a platform-level UX regression.

---

# RecentAppSwitcher (RAS)

**RecentAppSwitcher** (short: **RAS**) is a utility app for **Android TV / Google TV (Android 11–14)**  
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

**RecentAppSwitcher exists solely to fix that gap.**

---

## What does RecentAppSwitcher do?

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

## Using RecentAppSwitcher with Key Mapper (recommended)

With **Key Mapper**, RAS behaves like a real system feature.

---

### 🔁 Last App (Alt-Tab behavior)

**Custom Intent configuration:**

- **Package:**  
  `com.example.recentsopener`
- **Action:**  
  `com.example.recentsopener.SHOW_LAST_APP`

➡ Result:  
One button press → instantly switch back to the last app  
(e.g. YouTube ↔ Jellyfin)

---

### 📜 Open Recent Apps List

**Custom Intent configuration:**

- **Package:**  
  `com.example.recentsopener`
- **Action:**  
  `com.example.recentsopener.SHOW_RECENTS`

➡ Result:  
Sorted list of recently used apps  
(click = open, long-press = exclude)

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

**RecentAppSwitcher is not a hack.**  
It is a **utility born out of necessity** to restore basic multitasking on Android TV —  
as far as the platform still allows.

---

## License

Open Source.  
Use it, fork it, improve it 🙂

PRs, ideas, and discussions are welcome.
