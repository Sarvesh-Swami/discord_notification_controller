# Discord Quiet Hours

Personal Android app that hides Discord notifications on a daily schedule. By default they're shown from 9:00 AM and hidden from 8:00 PM; both times can be changed in the app.

- `www/` is the HTML/CSS/JS screen. `www/schedule.js` holds the ON/OFF rule.
- `native/` has the three small Java files Android needs. JavaScript can't read or remove another app's notifications.
- `scripts/setup-android.mjs` copies them into `android/`, removes the INTERNET permission, turns off backups, adds the battery permission and registers the listener service.
- `.github/workflows/build-apk.yml` builds the APK on GitHub (Actions tab → Build APK → artifact `discord-quiet-hours-apk`).

## Commands

- `npm test`: schedule unit tests
- `npm run web`: browser preview at http://localhost:8080 (phone features simulated)
- `npm run apk`: local build (needs JDK 21 and the Android SDK)

## Permissions

| What | Why | How it's granted |
|---|---|---|
| Notification access (required) | Read and remove Discord's notifications | In-app button → turn on "Discord Quiet Hours" |
| Allow restricted settings (Android 13+, APK installs only) | Android greys out the switch above for apps not from the Play Store | In-app "Open App info" → ⋮ → Allow restricted settings |
| Unrestricted battery (recommended) | Stops the phone from killing the listener overnight | In-app "Allow" button |
| Package visibility for `com.discord` | Shows whether Discord is installed | Automatic (manifest) |

The app has no internet permission. It never changes Discord's own settings; leave Discord's notifications on.

## Installing an update

Every GitHub build signs the APK with a new debug key, so Android refuses to install it over the old one ("App not installed"). Uninstall the old version first, then grant the permissions again.
