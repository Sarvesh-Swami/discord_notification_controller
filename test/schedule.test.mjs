import { test } from 'node:test';
import assert from 'node:assert/strict';
import { isQuietTime, nextChange } from '../www/schedule.js';

const at = (h, m = 0) => new Date(2026, 9, 24, h, m);

test('blocked from 8:00 PM until 8:59 AM', () => {
  for (const [h, m] of [[20, 0], [23, 59], [0, 0], [3, 30], [8, 59]]) {
    assert.equal(isQuietTime(at(h, m)), true, `${h}:${m}`);
  }
});

test('allowed from 9:00 AM until 7:59 PM', () => {
  for (const [h, m] of [[9, 0], [12, 0], [19, 59]]) {
    assert.equal(isQuietTime(at(h, m)), false, `${h}:${m}`);
  }
});

test('next change', () => {
  assert.deepEqual(nextChange(at(10)), { at: at(20), quietAfter: true });
  assert.deepEqual(nextChange(at(7)), { at: at(9), quietAfter: false });
  assert.deepEqual(nextChange(at(21)), { at: new Date(2026, 9, 25, 9), quietAfter: false });
});
