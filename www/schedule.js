// Discord notifications are ON from `onAt` until `offAt`, and OFF (hidden) from
// `offAt` until `onAt`. Times are "HH:MM", 24-hour. The native service
// (DiscordBlockerService.java) applies the same rule.
export const DEFAULT_SCHEDULE = { onAt: '09:00', offAt: '20:00' };

export function toMinutes(hhmm) {
  const [h, m] = hhmm.split(':').map(Number);
  return h * 60 + m;
}

export function isQuietTime(date = new Date(), s = DEFAULT_SCHEDULE) {
  const on = toMinutes(s.onAt);
  const off = toMinutes(s.offAt);
  const t = date.getHours() * 60 + date.getMinutes();
  if (on === off) return false;            // same time: never hide anything
  if (off < on) return t >= off && t < on; // e.g. OFF 01:00, ON 09:00
  return t >= off || t < on;               // e.g. OFF 20:00, ON 09:00 (crosses midnight)
}

// Next moment the state flips, for the "next change" line in the UI.
export function nextChange(date = new Date(), s = DEFAULT_SCHEDULE) {
  if (s.onAt === s.offAt) return null;
  const t = date.getHours() * 60 + date.getMinutes();
  const minutesUntil = (m) => (m - t + 1440) % 1440 || 1440;
  const toOn = minutesUntil(toMinutes(s.onAt));
  const toOff = minutesUntil(toMinutes(s.offAt));
  const at = new Date(date);
  at.setSeconds(0, 0);
  at.setMinutes(at.getMinutes() + Math.min(toOn, toOff));
  return { at, quietAfter: toOff < toOn };
}
