# ⚠️ IMPORTANT NOTE

**This project is a ChatGPT AI experiment / side project — essentially an “AI slop” / proof-of-concept.**  
It exists because the Android TV platform *itself* fails to provide a usable solution for app switching.

The code works, the app is useful — but the need for this app highlights a platform-level UX regression.

---


# RecentAppSwitcher (RAS)

**RecentAppSwitcher** (short: **RAS**) is a utility app for **Android TV / Google TV (Android 11–14)**  
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
  - built on top of the official `UsageStatsManager` API
- 🔁 **Alt‑Tab–like behaviour**
  - jump back to the last used app instantly
- 🚀 **Launch apps directly**
  - click an entry → app opens
- 🚫 **Exclude apps**
  - long‑press → app is marked red and ignored
  - perfect for launchers & system apps
- 💾 **Persistent storage**
  - exclusions & last app survive reboots
- 🧩 **Intent‑based control**
  - ideal for automation & key remapping
- ♿ **Experimental: close running apps via Accessibility**
  - when the optional accessibility service is enabled a **Close all apps** and **Close other apps** button appears in the recents list
  - closing apps works by automating the Settings → App info screen; it remains fully optional and opt‑in
  - between each close the Android TV Settings app is also closed to improve reliability

---

## What this app intentionally does NOT do

Even with the new experimental closing features there are limits to what an unprivileged app can accomplish. RecentAppSwitcher **does not**:

- ❌ Kill or manage apps at the process level outside of the Settings UI.  Closing works by opening each app’s details screen and triggering the **Force stop** button via accessibility.
- ❌ Leverage hidden SystemUI intents or private APIs — everything is implemented via official public APIs and accessibility services.
- ❌ Require root or system privileges.

> 💡 Everything this app does is **Google‑compliant**, stable, and update‑safe.  Enabling the accessibility service is optional; if disabled the app behaves exactly as before (no close buttons).

---

## Requirements

- Android TV / Google TV (Android 11+ recommended)
- **Usage Access permission**
  - the app guides you there automatically on first launch

Optional:
- **Key Mapper** (open-source)

---

## Quick Start

1. Install the APK (via ADB or a file manager)
2. Launch the app
3. Grant **Usage Access** when prompted (this populates the recents list)
4. *(Optional)* Enable the **RecentsAppSwitcher Accessibility Service** in Settings → Accessibility → RecentsAppSwitcher.  This is required for the experimental closing features.
5. Done 🎉

---

## Using RecentAppSwitcher with Key Mapper (recommended)

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

## Experimental closing features

Beginning with this release you can optionally close running apps directly from the recents list.  To use this feature you **must** enable the **RecentsAppSwitcher Accessibility Service** in the system settings (Settings → Accessibility → RecentsAppSwitcher).  Once enabled, two extra buttons appear at the top of the recents list:

- **Close all apps** – closes every app in the list except the switcher itself and system settings apps.  After all apps are closed you are returned to your TV’s home screen.
- **Close other apps** – closes all apps except the one that was in the foreground before opening the recents list.  When only one app remains it will be automatically reopened.

Closing is implemented via accessibility automation.  For each app the switcher opens its *App Info* page in the Settings app, triggers the **Force stop** button and confirms the dialog.  Between each app the switcher also opens and force‑stops the Android TV Settings app (`com.android.tv.settings`) to ensure the next app’s info page loads reliably.  Because this uses public UI automation, timings are deliberately generous (about 6 seconds per app); on slower devices you may need to wait a moment for the sequence to complete.

These buttons remain hidden when the accessibility service is disabled, so the app’s default recents functionality is unchanged.  Individual apps can still be closed manually by pressing **DPAD‑LEFT** on an entry (when the service is enabled): this opens the app’s settings screen and automatically runs the force‑stop sequence.

There are currently no standalone intents for closing all/other apps.  To trigger closing actions via Key Mapper or automation you should map a key to open the recents list (`eu.ras.SHOW_RECENTS`) and then navigate to the appropriate button.

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
It is a **utility born out of necessity** to restore basic multitasking on Android TV —  
as far as the platform still allows.

---

## License

Open Source.  
Use it, fork it, improve it 🙂

PRs, ideas, and discussions are welcome.
