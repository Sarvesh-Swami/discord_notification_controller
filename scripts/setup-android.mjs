// Run after `npx cap add android`: copies the Java files into the generated
// project and edits AndroidManifest.xml. Safe to run more than once.
import { copyFileSync, readFileSync, writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';

const root = new URL('..', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1');
const javaDir = join(root, 'android/app/src/main/java/com/quiethours/discord');
const manifestPath = join(root, 'android/app/src/main/AndroidManifest.xml');

if (!existsSync(manifestPath)) {
  console.error('android/ not found. Run `npx cap add android` first.');
  process.exit(1);
}

mkdirSync(javaDir, { recursive: true });
for (const f of ['MainActivity.java', 'QuietHoursPlugin.java', 'DiscordBlockerService.java']) {
  copyFileSync(join(root, 'native', f), join(javaDir, f));
}

let m = readFileSync(manifestPath, 'utf8');

// No internet: the app never talks to anything outside the phone.
m = m.replace(/\s*<uses-permission android:name="android\.permission\.INTERNET"\s*\/>/, '');

// Keep the app's data out of cloud/ADB backups.
m = m.replace('android:allowBackup="true"', 'android:allowBackup="false"');

// Lets the app see whether Discord is installed (Android 11+ package visibility).
if (!m.includes('<queries>')) {
  m = m.replace('<application', '<queries>\n        <package android:name="com.discord" />\n    </queries>\n\n    <application');
}

// Only the Android system can bind this service (BIND_NOTIFICATION_LISTENER_SERVICE).
if (!m.includes('DiscordBlockerService')) {
  m = m.replace('</application>', `
        <service
            android:name=".DiscordBlockerService"
            android:label="Discord Quiet Hours"
            android:exported="true"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>
    </application>`);
}

writeFileSync(manifestPath, m);
console.log('Android project patched.');
