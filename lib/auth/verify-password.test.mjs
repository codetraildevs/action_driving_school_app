import { test } from 'node:test';
import assert from 'node:assert/strict';
import bcrypt from 'bcryptjs';
import { verifyLoginPassword } from './verify-password.ts';

test('accepts the matching credential', async () => {
  const hash = await bcrypt.hash('fixture-password', 4);
  assert.equal(await verifyLoginPassword('fixture-password', hash), true);
});
test('rejects an incorrect credential', async () => {
  const hash = await bcrypt.hash('fixture-password', 4);
  assert.equal(await verifyLoginPassword('android-device-value', hash), false);
});
test('rejects an empty credential for a nonempty password', async () => {
  const hash = await bcrypt.hash('fixture-password', 4);
  assert.equal(await verifyLoginPassword('', hash), false);
});
