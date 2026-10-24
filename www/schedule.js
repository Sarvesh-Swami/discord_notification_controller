// Discord notifications are allowed from 09:00 until 20:00 and blocked otherwise.
// The native service (DiscordBlockerService.java) uses the same two hours.
export const ALLOW_FROM_HOUR = 9;   // 9:00 AM -> notifications ON
export const BLOCK_FROM_HOUR = 20;  // 8:00 PM -> notifications OFF

export function isQuietTime(date = new Date()) {
  const h = date.getHours();
  return h >= BLOCK_FROM_HOUR || h < ALLOW_FROM_HOUR;
}

// Next moment the state flips, for the "next change" line in the UI.
export function nextChange(date = new Date()) {
  const next = new Date(date);
  next.setSeconds(0, 0);
  next.setMinutes(0);
  if (isQuietTime(date)) {
    if (date.getHours() >= BLOCK_FROM_HOUR) next.setDate(next.getDate() + 1);
    next.setHours(ALLOW_FROM_HOUR);
    return { at: next, quietAfter: false };
  }
  next.setHours(BLOCK_FROM_HOUR);
  return { at: next, quietAfter: true };
}
