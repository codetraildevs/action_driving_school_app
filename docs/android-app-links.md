# Android App Links

The app claims the canonical host `amategekoyumuhanda.rw`. The `www` alias is intentionally
excluded because `https://www.amategekoyumuhanda.rw/.well-known/assetlinks.json` redirects to
the apex domain. Android requires the Digital Asset Links file to return `200 application/json`
directly and does not follow that redirect during ownership verification.

The production apex endpoint must remain available at:

`https://amategekoyumuhanda.rw/.well-known/assetlinks.json`

It must contain the Play App Signing SHA-256 certificate fingerprint for
`com.drivingschoolrwandaapp`. The Play Console App integrity page is the source of truth for
that fingerprint.

If `www` must become an App Link later, configure its HTTPS Nginx server block to serve the file
before the general redirect:

```nginx
location = /.well-known/assetlinks.json {
    default_type application/json;
    alias /var/www/amategekoyumuhanda/.well-known/assetlinks.json;
}
```

The endpoint must return the JSON directly without a `301` or `302`. Only after that is live and
validated should `www.amategekoyumuhanda.rw` be restored to the Android manifest.

After publishing an app update, validate on Android 12 or newer:

```text
adb shell pm set-app-links --package com.drivingschoolrwandaapp 0 all
adb shell pm verify-app-links --re-verify com.drivingschoolrwandaapp
adb shell pm get-app-links com.drivingschoolrwandaapp
```
