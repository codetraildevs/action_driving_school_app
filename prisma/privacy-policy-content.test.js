/* eslint-disable @typescript-eslint/no-require-imports */
const assert = require("node:assert/strict");
const test = require("node:test");
const {
  APP_VERSION,
  POLICY_VERSION,
  policies,
} = require("./privacy-policy-content");

test("publishes the current policy for every supported language", () => {
  assert.equal(APP_VERSION, "1.5.14");
  assert.equal(POLICY_VERSION, "1.1.0");
  assert.deepEqual(Object.keys(policies).sort(), ["en", "fr", "rw"]);
});

for (const [language, policy] of Object.entries(policies)) {
  test(`${language} policy contains the required disclosures`, () => {
    assert.ok(policy.title.length > 10);
    assert.match(policy.content, /AdMob/);
    assert.match(policy.content, /Firebase Analytics/);
    assert.match(policy.content, /Crashlytics/);
    assert.match(policy.content, /Cloud Messaging/);
    assert.match(policy.content, /delete-account/);
    assert.match(policy.content, /info@amategekoyumuhanda\.rw/);
    assert.doesNotMatch(policy.content, /privacy@yourapp\.com/);
  });
}
