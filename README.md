# Focus Keeper

A small, **visible** digital-wellbeing app for **your own Android phone**. It helps you
cut down on distracting apps by letting you:

- **Track your own usage** — see how long you spent in each app today
  (via Android's official `UsageStatsManager`).
- **Block distracting apps during a focus session** — when a focus session is on and you
  open an app you've marked as blocked, a full-screen "stay focused" screen appears and
  sends you back to the home screen.
- **Uninstall apps in one tap** — a shortcut that opens Android's standard uninstall
  confirmation for the app you pick.
- **See an activity log** — a local, on-device record of what the app did (focus sessions
  started/stopped, apps blocked, uninstalls requested, block-list changes).

It is designed to be transparent: a normal launcher icon, a persistent notification while a
focus session runs, and permissions you grant yourself. It does **not** hide itself, and it
only reads *this* phone's own usage data. Nothing is sent off the device.

## What it can and can't do (by design / by Android)

- It cannot silently uninstall apps. Android always shows its own uninstall confirmation
  dialog — the in-app "Uninstall" button just opens that dialog.
- It cannot read the contents of other apps or log keystrokes. The "usage" it shows is the
  per-app foreground time Android itself exposes.
- Blocking works while the screen is on and a focus session is active, by watching which app
  is in the foreground and drawing a reminder over blocked ones.

## Permissions you grant

| Permission | Why | Where you grant it |
|---|---|---|
| Usage access | Read this phone's app-usage history and detect the foreground app | Settings → Usage access |
| Display over other apps | Show the focus reminder over a blocked app | Settings → Display over other apps |
| Notifications | Show the ongoing "focus session active" notification | In-app prompt |

## Build

### Easiest: GitHub Actions (no local setup)

Push to GitHub. The **Build APK** workflow (`.github/workflows/android.yml`) compiles a debug
APK and uploads it as a build artifact named `focus-keeper-debug-apk`. Download it from the
workflow run, copy it to your phone, and install (you'll need "install unknown apps" enabled
for your file manager/browser).

### Locally with Android Studio

Open the project folder in Android Studio (Koala or newer), let it sync, then
**Build → Build App Bundle(s) / APK(s) → Build APK(s)**, or connect your phone and press Run.

### Locally from the command line

Requires JDK 17 and the Android SDK (with `ANDROID_HOME`/`local.properties` set):

```bash
gradle :app:assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## Tech

- Kotlin, classic Android Views (no Compose), minSdk 26, targetSdk 34
- Android Gradle Plugin 8.5.2, Gradle 8.7
- `UsageStatsManager` for usage + foreground detection, a foreground `Service` + an overlay
  window for the focus block screen
