# Discord Quiet Hours

Personal Android app: Discord notifications show between 9:00 AM and 8:00 PM and are removed between 8:00 PM and 9:00 AM.

- `www/` is the HTML/CSS/JS screen. `www/schedule.js` holds the hours.
- `native/` has the three small Java files Android needs. JavaScript can't read or remove another app's notifications.
- `scripts/setup-android.mjs` copies them into `android/`, removes the INTERNET permission, turns off backups and registers the listener service.

## Commands

- `npm test`: schedule unit tests
- `npm run web`: browser preview at http://localhost:8080 (phone features simulated)
- `npm run apk`: build `android/app/build/outputs/apk/debug/app-debug.apk` (needs JDK 21 and the Android SDK, but not Android Studio)

## On the phone

1. Install the APK (allow "install unknown apps" for the file manager you open it from).
2. Open the app and tap **Open notification access settings**, then turn on **Discord Quiet Hours**.
3. Leave Discord's own notification settings on. This app hides them during quiet hours.

To change the hours, edit both `www/schedule.js` and `native/DiscordBlockerService.java`.
