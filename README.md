# Cascade Launcher

A free, open-source, minimalist Android home screen in the style of list-based launchers
like Niagara Launcher. Every feature is included: no Pro tier, no ads, no tracking, no
network permission.

Cascade is an independent project written from scratch. It is not affiliated with,
derived from, or endorsed by Niagara Launcher or its developers.

## Features

- **One-handed alphabet wave** – slide a thumb along the letters on the right edge. The
  letters near your finger bulge out in a wave and the app list jumps as you move. You can
  start the gesture right from the home screen.
- **Favorites home screen** – a short, large-text list of the apps you actually use, with
  clock and date (tap the clock for alarms, the date for your calendar).
- **Notification dots and inline previews** – favorites show the latest message under the
  app name; tap the preview to open that notification directly. (Optional; needs
  notification access, and nothing leaves the phone.)
- **Fast search** – matches by name start, word start, substring or initials
  ("gm" → Google Maps). Press Enter to launch the top hit, or search the web.
- **App shortcuts** – long-press an app to see its shortcuts (e.g. "New message").
- **Per-app actions** – favorite, reorder favorites, rename, hide, app info, uninstall.
- **Gestures** – swipe up for all apps, swipe down for the notification shade, pull down
  at the top of the list to close it.
- **Dynamic theming** – the highlight color (letters, dots, switches) follows your
  wallpaper's dominant color on Android 8.1+ and updates when you change wallpaper.
  Can be turned off in settings.
- **Work profile** support, wallpaper dimming, toggle icons on/off for a text-only look.

## Install

Every push builds APKs with GitHub Actions. Open the **Actions** tab, pick the latest
"Build APK" run, download the `cascade-launcher-apks` artifact and install the APK on your
phone. Then open Cascade → long-press empty space → **Settings → Default home app**.

The release APK is signed with the debug key so it installs as-is; use your own signing
key before distributing it anywhere.

## Build it yourself

Requirements: JDK 17 and the Android SDK (platform 35).

```sh
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## How to use it

| Action | Gesture |
| --- | --- |
| Open all apps | Swipe up, or touch the letters on the right |
| Jump to a letter | Slide along the letters |
| Search | Tap the search bar at the bottom of the home screen |
| App menu (favorite, rename, hide…) | Long-press an app |
| Notification shade | Swipe down on the home screen |
| Launcher settings | Long-press empty space on the home screen, or ⚙ in the app list |

## Project layout

```
app/src/main/java/app/cascade/launcher/
  MainActivity.kt          home activity
  LauncherViewModel.kt     app list, favorites, search, settings state
  Actions.kt               launching apps, shortcuts, notifications, system intents
  data/                    app loading (LauncherApps) and preferences
  notifications/           optional NotificationListenerService for dots/previews
  ui/                      Jetpack Compose screens and the alphabet wave scroller
```

## License

MIT – see [LICENSE](LICENSE).
