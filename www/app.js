import { isQuietTime, nextChange } from './schedule.js';

const native = window.Capacitor?.isNativePlatform?.() === true;

// On the phone this talks to QuietHoursPlugin.java. In a desktop browser there
// is no Android, so a stand-in keeps the toggle in localStorage for testing.
const QuietHours = native
  ? window.Capacitor.registerPlugin('QuietHours')
  : {
      async getStatus() {
        let enabled = true;
        try { enabled = localStorage.getItem('enabled') !== 'false'; } catch {}
        return { enabled, accessGranted: false, discordInstalled: false };
      },
      async setEnabled({ enabled }) {
        try { localStorage.setItem('enabled', String(enabled)); } catch {}
      },
      async openAccessSettings() {
        alert('On the phone this opens Settings > Notification access.');
      },
    };

const $ = (id) => document.getElementById(id);
const fmt = (d) => d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });

async function render() {
  const s = await QuietHours.getStatus();
  const quiet = isQuietTime();
  const blocking = s.enabled && quiet;

  const card = $('stateCard');
  card.classList.remove('on', 'off', 'paused');
  if (!s.enabled) {
    card.classList.add('paused');
    $('stateText').textContent = 'Schedule paused';
    $('nextText').textContent = 'Discord notifications are not touched.';
  } else {
    card.classList.add(blocking ? 'off' : 'on');
    $('stateText').textContent = blocking ? 'OFF' : 'ON';
    const n = nextChange();
    $('nextText').textContent = `Turns ${n.quietAfter ? 'off' : 'on'} at ${fmt(n.at)}`;
  }

  $('enabledToggle').checked = s.enabled;
  $('enabledHint').textContent = s.enabled ? 'Running every day' : 'Paused';

  $('accessCard').classList.toggle('ok', s.accessGranted);
  $('accessText').textContent = s.accessGranted
    ? 'Granted. The app can hide Discord notifications.'
    : 'Not granted yet. Turn on "Discord Quiet Hours" in the list, or the schedule cannot work.';

  $('discordText').textContent = s.discordInstalled
    ? 'Found (com.discord).'
    : native ? 'Discord is not installed on this phone.' : 'Not checked in the browser.';

  $('envText').textContent = native ? '' : 'Browser preview: phone features are simulated.';
}

$('enabledToggle').addEventListener('change', async (e) => {
  await QuietHours.setEnabled({ enabled: e.target.checked });
  render();
});
$('accessBtn').addEventListener('click', () => QuietHours.openAccessSettings());

// Re-check when the user comes back from the settings screen, and once a minute.
document.addEventListener('visibilitychange', () => { if (!document.hidden) render(); });
setInterval(render, 60_000);
render();
