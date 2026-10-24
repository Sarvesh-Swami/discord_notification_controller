import { test } from 'node:test';
import assert from 'node:assert/strict';
import { isQuietTime, nextChange } from '../www/schedule.js';

const at = (h, m = 0, day = 24) => new Date(2026, 9, day, h, m);
const DEFAULT = { onAt: '09:00', offAt: '20:00' };

test('default: hidden from 8:00 PM until 8:59 AM', () => {
  for (const [h, m] of [[20, 0], [23, 59], [0, 0], [3, 30], [8, 59]]) {
    assert.equal(isQuietTime(at(h, m), DEFAULT), true, `${h}:${m}`);
  }
});

test('default: shown from 9:00 AM until 7:59 PM', () => {
  for (const [h, m] of [[9, 0], [12, 0], [19, 59]]) {
    assert.equal(isQuietTime(at(h, m), DEFAULT), false, `${h}:${m}`);
  }
});

test('custom minutes: ON 07:30, OFF 22:15', () => {
  const s = { onAt: '07:30', offAt: '22:15' };
  assert.equal(isQuietTime(at(7, 29), s), true);
  assert.equal(isQuietTime(at(7, 30), s), false);
  assert.equal(isQuietTime(at(22, 14), s), false);
  assert.equal(isQuietTime(at(22, 15), s), true);
});

test('quiet window that does not cross midnight: OFF 01:00, ON 09:00', () => {
  const s = { onAt: '09:00', offAt: '01:00' };
  assert.equal(isQuietTime(at(0, 30), s), false);
  assert.equal(isQuietTime(at(1, 0), s), true);
  assert.equal(isQuietTime(at(8, 59), s), true);
  assert.equal(isQuietTime(at(9, 0), s), false);
  assert.equal(isQuietTime(at(23, 0), s), false);
});

test('same ON and OFF time never hides anything', () => {
  const s = { onAt: '09:00', offAt: '09:00' };
  assert.equal(isQuietTime(at(3), s), false);
  assert.equal(nextChange(at(3), s), null);
});

test('next change', () => {
  assert.deepEqual(nextChange(at(10), DEFAULT), { at: at(20), quietAfter: true });
  assert.deepEqual(nextChange(at(7), DEFAULT), { at: at(9), quietAfter: false });
  assert.deepEqual(nextChange(at(21), DEFAULT), { at: at(9, 0, 25), quietAfter: false });
  assert.deepEqual(nextChange(at(20), DEFAULT), { at: at(9, 0, 25), quietAfter: false });
});
