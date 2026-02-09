**This is a ChatGPT AI flop**

# RecentAppSwitcher (RAS)

RecentAppSwitcher exists because many Android TV and Google TV devices ship without a working overview of recently used apps.  On Android TV 14 the system task switcher can be hidden and third‑party apps cannot force other apps to close.  RAS uses the Usage Stats API to build its own list of recently used packages and makes it easy to jump back and forth between them.

## Why this project?

* **No built‑in "Recents" on many TV devices.** Vendors remove the overview button and SystemUI disables the usual long‑press actions.  It can be frustrating to jump between apps with a remote control.
* **Android 14 restricts killing other apps.** The system API `killBackgroundProcesses()` only stops this app's own process on modern releases.  Without root or system privileges it is not possible to close other tasks.
* **Usage Stats as a workaround.** Android exposes anonymised statistics to apps with the "Usage access" privilege.  RAS reads this data to determine which app was most recently visible and presents them in a simple list.  No root is required.

## Features

* **Last app switcher:** a large button on the main screen launches the most recently foregrounded app.  If the optional Accessibility service is enabled the system Recents panel will appear instead.
* **Recent app list:** a secondary activity shows a scrollable list of recent apps, complete with icons and package names.  Tapping an entry launches the app.  Long‑press an entry to exclude it from future lists.
* **Blocked‑apps persistence:** excluded packages are stored in shared preferences.  They disappear from the list automatically until manually re‑added by long‑pressing again.
* **Dark/light theme:** uses the AppCompat DayNight theme and follows the system setting.

## Usage instructions

1. **Install the APK** compiled from this repository on your Android TV/Google TV device.
2. **Grant the Usage Access permission.** On first launch RAS prompts you to open the usage access settings.  Enable access for this app.
3. *(Optional)* **Enable the accessibility service.** This is only required if you want the button on the main screen to open the system "Recents" panel.  Without it the button will simply launch the most recent app directly.
4. **Open RAS** and tap the button to return to the last app or open the list of recents via the app menu.
5. **Exclude troublesome packages** by long‑pressing them in the list.  They will turn red and be ignored in the switcher.  Long‑press again to restore.

## KeyMapper integration

Many users of key remapping tools such as *KeyMapper* or *Button Mapper* want a shortcut that jumps back to the last app without opening RAS manually.  RAS exposes two exported broadcast intents that you can assign to a hardware button:

| Action | Effect |
|-------|-------|
| `eu.recentsopener.OPEN_LAST_APP` | Launches the most recently used app directly. |
| `eu.recentsopener.SHOW_RECENT_LIST` | Shows the full recents list activity. |

To configure in KeyMapper:

1. Create a new button mapping and choose **Custom Intent**.
2. Enter the package name `eu.recentsopener`.
3. Enter the action string from the table above.
4. Save and test.  The mapping will launch RAS in the background and perform the requested action.

## Limitations

RAS cannot close other apps or clear recent tasks due to platform restrictions.  Apps may reopen themselves or maintain state when launched from the recents list.  Some system apps such as the Settings app might refuse to launch via the standard launch intent on certain OEM TV builds.

## Building and releasing

The included GitHub Actions workflow automatically builds a debug APK when new commits land on `main`.  To publish a release and upload the APK to the Releases page, create a new tag or release in the repository and push it to GitHub.  The workflow will assemble the debug APK and attach it to the release assets.