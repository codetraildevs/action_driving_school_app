import { test } from 'node:test';
import assert from 'node:assert/strict';
import { orderPhoneCandidates } from './phone-candidates.ts';

test('batched lookup preserves legacy variant and ID priority', () => {
  const rows = [
    { id: 1, phoneNumber: '+250700000001' },
    { id: 2, phoneNumber: '0700000001' },
    { id: 3, phoneNumber: '0700000001' },
  ];
  assert.deepEqual(orderPhoneCandidates(rows, ['0700000001', '+250700000001']).map(x => x.id), [2, 3, 1]);
});
test('repeated variants never duplicate identities', () => {
  assert.deepEqual(orderPhoneCandidates([{ id: 1, phoneNumber: 'a' }], ['a', 'a']), [{ id: 1, phoneNumber: 'a' }]);
});
test('unmatched identities are excluded', () => {
  assert.deepEqual(orderPhoneCandidates([{ id: 1, phoneNumber: 'a' }], ['b']), []);
});
