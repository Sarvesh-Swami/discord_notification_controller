import { DEFAULT_SCHEDULE, isQuietTime, nextChange } from './schedule.js';

const $ = (id) => document.getElementById(id);

// Show any script error on screen instead of failing silently.
const showError = (msg) => { $('envText').textContent = `Error: ${msg}`; };
window.addEventListener('error', (e) => showError(e.message));
window.addEventListener('unhandledrejection', (e) => showError(e.reason?.message ?? e.reason));

const native = window.Capacitor?.isNativePlatform?.() === true;

// On the phone this calls QuietHoursPlugin.java through Capacitor's built-in
// bridge (no bundler needed). In a desktop browser there is no Android, so a
// stand-in keeps settings in localStorage for testing.
const QuietHours = native
  ? new Proxy({}, { get: (_, method) => (opts = {}) => window.Capacitor.nativePromise('QuietHours', method, opts) })
  : (() => {
      const read = (k, d) => { try { return localStorage.getItem(k) ?? d; } catch { return d; } };
      const write = (k, v) => { try { localStorage.setItem(k, v); } catch {} };
      const say = (what) => alert(`On the phone this opens ${what}.`);
      const log = (msg) => {
        const t = new Date().toISOString().replace('T', ' ').slice(0, 19);
        write('log', `${t}  ${msg}\n${read('log', '')}`);
      };
      return {
        async getStatus() {
          return {
            enabled: read('enabled', 'true') !== 'false',
            onAt: read('onAt', DEFAULT_SCHEDULE.onAt),
            offAt: read('offAt', DEFAULT_SCHEDULE.offAt),
            accessGranted: false, listenerConnected: false, discordInShade: -1,
            batteryUnrestricted: false, discordInstalled: false, sdkInt: 0,
          };
        },
        async logAppOpened() { log('App opened (browser preview)'); },
        async setEnabled({ enabled }) { write('enabled', String(enabled)); log(`Schedule switched ${enabled ? 'ON' : 'OFF (paused)'}`); },
        async setSchedule({ onAt, offAt }) { write('onAt', onAt); write('offAt', offAt); log(`Times saved | ON=${onAt} OFF=${offAt}`); },
        async getLog() { return { text: read('log', '') }; },
        async clearLog() { write('log', ''); },
        async copyLog() { await navigator.clipboard?.writeText(read('log', '')); },
        async shareLog() { say('the Android share sheet'); },
        async openAccessSettings() { say('Settings > Notification access'); },
        async openDiscordSettings() { say("Discord's notification settings"); },
        async openAppInfo() { say('App info'); },
        async requestBatteryUnrestricted() { say('the battery optimization prompt'); },
      };
    })();

const fmt = (d) => d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
const fmtHHMM = (hhmm) => { const [h, m] = hhmm.split(':').map(Number); return fmt(new Date(2000, 0, 1, h, m)); };

function setCheck(id, ok) {
  $(id).classList.toggle('ok', ok);
}

let formDirty = false;

async function render() {
  const s = await QuietHours.getStatus();
  const schedule = { onAt: s.onAt, offAt: s.offAt };
  const blocking = s.enabled && isQuietTime(new Date(), schedule);

  $('subText').textContent = `On at ${fmtHHMM(s.onAt)} · Off at ${fmtHHMM(s.offAt)}`;

  const card = $('stateCard');
  card.classList.remove('on', 'off', 'paused');
  const n = nextChange(new Date(), schedule);
  if (!s.enabled || !n) {
    card.classList.add('paused');
    $('stateText').textContent = s.enabled ? 'Always ON' : 'Schedule paused';
    $('nextText').textContent = s.enabled
      ? 'ON and OFF are the same time, so nothing is hidden.'
      : 'Discord notifications are not touched.';
  } else {
    card.classList.add(blocking ? 'off' : 'on');
    $('stateText').textContent = blocking ? 'OFF' : 'ON';
    $('nextText').textContent = `Turns ${n.quietAfter ? 'off' : 'on'} at ${fmt(n.at)}`;
  }

  $('enabledToggle').checked = s.enabled;
  $('enabledHint').textContent = s.enabled ? 'Running every day' : 'Paused';
  if (!formDirty) {
    $('onAt').value = s.onAt;
    $('offAt').value = s.offAt;
  }

  setCheck('accessCheck', s.accessGranted);
  $('accessText').textContent = s.accessGranted
    ? 'Granted. The app can hide Discord notifications.'
    : 'Not granted. Without it the schedule does nothing.';
  // Android 13+ blocks this switch for apps installed from an APK file until
  // "Allow restricted settings" is turned on.
  $('restrictedHint').hidden = s.accessGranted || (native && s.sdkInt < 33);

  setCheck('listenerCheck', s.listenerConnected);
  $('listenerText').textContent = s.listenerConnected
    ? 'Android has the app connected and is sending it notifications.'
    : s.accessGranted
      ? 'Access is on but Android has not connected the app yet. Turn notification access off and on again, or restart the phone.'
      : 'Needs notification access first.';
  $('shadeText').textContent = s.discordInShade >= 0
    ? `Discord notifications in your notification shade now: ${s.discordInShade}`
    : '';

  setCheck('batteryCheck', s.batteryUnrestricted);
  $('batteryText').textContent = s.batteryUnrestricted
    ? 'Allowed. The phone won’t stop the app overnight.'
    : 'Stops the phone from putting the app to sleep and missing the OFF time.';

  setCheck('discordCheck', s.discordInstalled);
  $('discordText').textContent = s.discordInstalled
    ? 'Found (com.discord).'
    : native ? 'Discord is not installed on this phone.' : 'Not checked in the browser.';

  if (!$('envText').textContent.startsWith('Error')) {
    $('envText').textContent = native ? '' : 'Browser preview: phone features are simulated.';
  }
  renderLog();
}

async function renderLog() {
  const { text } = await QuietHours.getLog();
  $('logText').textContent = text || 'Nothing logged yet.';
}

$('enabledToggle').addEventListener('change', async (e) => {
  await QuietHours.setEnabled({ enabled: e.target.checked });
  render();
});

for (const id of ['onAt', 'offAt']) {
  $(id).addEventListener('input', () => { formDirty = true; $('saveMsg').textContent = ''; });
}

$('scheduleForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const onAt = $('onAt').value;
  const offAt = $('offAt').value;
  try {
    await QuietHours.setSchedule({ onAt, offAt });
    formDirty = false;
    $('saveMsg').textContent = onAt === offAt
      ? 'Saved. ON and OFF are the same, so nothing will be hidden.'
      : `Saved. ON at ${fmtHHMM(onAt)}, OFF at ${fmtHHMM(offAt)}.`;
  } catch (err) {
    $('saveMsg').textContent = `Not saved: ${err.message ?? err}`;
  }
  render();
});

const opener = (method) => async () => {
  try { await QuietHours[method](); } catch (err) { showError(err.message ?? err); }
};
$('accessBtn').addEventListener('click', opener('openAccessSettings'));
$('appInfoBtn').addEventListener('click', opener('openAppInfo'));
$('batteryBtn').addEventListener('click', opener('requestBatteryUnrestricted'));
$('discordSettingsBtn').addEventListener('click', opener('openDiscordSettings'));
$('logShareBtn').addEventListener('click', opener('shareLog'));
$('logCopyBtn').addEventListener('click', async () => {
  await opener('copyLog')();
  $('logCopyBtn').textContent = 'Copied';
  setTimeout(() => { $('logCopyBtn').textContent = 'Copy'; }, 1500);
});
$('logRefreshBtn').addEventListener('click', () => render());
$('logClearBtn').addEventListener('click', async () => { await QuietHours.clearLog(); renderLog(); });

// Re-check when the user comes back from a settings screen, and once a minute.
document.addEventListener('visibilitychange', () => { if (!document.hidden) render(); });
setInterval(render, 60_000);
QuietHours.logAppOpened().finally(render);
