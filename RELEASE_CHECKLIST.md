# Release Checklist — Action Driving School

> Use this checklist every time you prepare a new release for Google Play Console.

---

## 1. Prerequisites

- [ ] **Keystore present** — `app/upload-keystore.jks` exists (PKCS12 format)
- [ ] **Gradle version catalog ready** — `gradle/libs.versions.toml` is the single source of truth for all dependency versions
- [ ] **`keystore.properties` present** at project root (`DRIVINGSCHOOL2/keystore.properties`) with:
  ```properties
  storeFile=upload-keystore.jks
  storePassword=<your-store-password>
  keyAlias=upload
  keyPassword=<your-key-password>
  ```
- [ ] **Upload key active** — Google Play Console upload key matches the keystore (verify fingerprints)
- [ ] **google-services.json** — present at `app/google-services.json` (not committed, use `google-services.json.example` template)
- [ ] **Clean working tree** — `git status` shows no uncommitted changes (except intentional ones)

---

## 2. Bump Version

- [ ] Open `app/build.gradle.kts` and update:
  ```
  versionCode = 77  →  increment by 1
  versionName = "1.0.2"  →  update following semver
  ```
- [ ] Commit: `git add app/build.gradle.kts && git commit -m "Bump version to X.Y.Z (build N)"`

---

## 3. Pre-Build Checks

- [ ] **google-services.json** present and pointing to the **production** Firebase project
- [ ] `./gradlew lint` passes — catches resource format issues, unused resources, etc.
  - ✅ Currently clean (~8 compiler-level annotations only)

---

## 4. Build & Verify

### 4.1 Clean build
```bash
cd D:/software/DRIVINGSCHOOL2
./gradlew clean
```

### 4.2 Build Release App Bundle (AAB)
```bash
./gradlew bundleRelease
```
- [ ] **BUILD SUCCESSFUL** — no warnings about string resources, missing files, or locked R.jar

### 4.3 Build Release APK
```bash
./gradlew assembleRelease
```
- [ ] **BUILD SUCCESSFUL** — APK generated at `app/build/outputs/apk/release/app-release.apk`

### 4.4 Verify signing
```bash
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```
- [ ] Certificate shows `CN=Nomiso`
- [ ] All entries show `sm` (signed with manifest digest)
- [ ] No `unsigned entries` warnings
- ⚠️ `"signed in JarFile but is not signed in JarInputStream"` warnings are **harmless** (AAB artifact)
- ⚠️ `"PKIX path building failed"` is **expected** (self-signed upload key)

### 4.5 Verify fingerprints match Play Console
```bash
keytool -list -v -keystore app/upload-keystore.jks -storepass <password> -alias upload
```
- [ ] **SHA1:** `AD:14:AD:F8:8B:2B:11:C5:37:99:D7:29:09:42:D0:25:9D:25:D3:11`
- [ ] **MD5:** `8A:12:73:7F:B9:44:CE:52:58:DC:8E:5E:4F:E8:3D:65`

---

## 5. Package for Delivery

- [ ] Create `release/` directory in project root (if not exists)
- [ ] Copy AAB with descriptive name:
  ```bash
  cp app/build/outputs/bundle/release/app-release.aab release/ActionDrivingSchool-v{X.Y.Z}-build{N}.aab
  ```
- [ ] Optionally copy keystore, properties, and PEM certificate alongside:
  ```bash
  cp app/upload-keystore.jks release/
  cp keystore.properties release/   # ⚠️ contains passwords — keep secure
  cp app/upload_certificate.pem release/
  ```
- [ ] Verify all files in `release/`:
  ```bash
  ls -lh release/
  ```

---

## 6. Upload to Google Play Console

- [ ] Go to [Google Play Console](https://play.google.com/console/)
- [ ] Navigate to **Release > Production / Internal testing / Closed testing**
- [ ] Upload the `.aab` file from `release/` folder
- [ ] Fill in **Release notes** (What's new) in all supported languages — see template below
- [ ] Review and roll out

### Release Notes Template

Paste the relevant language into the "What's New" field for each locale in Play Console.

#### English

```
- Improved exam UI with clearer option indicators and real-time feedback
- Enhanced results screen with summary statistics and scrollable history
- Progress bar and percentage tracking during exams
- Better exam numbering for easier navigation
- Performance and stability improvements
- Updated signing key
```

#### Français

```
- Interface d'examen améliorée avec des indicateurs d'options plus clairs
- Écran des résultats amélioré avec statistiques et historique
- Barre de progression et suivi en pourcentage pendant les examens
- Meilleure numérotation des examens
- Corrections de performance et de stabilité
- Mise à jour de la clé de signature
```

#### Kinyarwanda

```
- Ikiganiro cy'ikizamini cyarushijeho kuba cyiza
- Urubuga rw'ibisubizo rwarushijeho hamwe n'imibare n'amateka
- Umurongo w'amajyambere n'indanganturo y'ijanisha mugihe cy'ikizamini
- Imibare myiza y'ibizamini
- Gukosora imikorere n'ubuzima
- Uruhushya rushya rwo gusinya
```

---

## 7. Post-Release

- [ ] Create a git tag for the release:
  ```bash
  git tag -a v{X.Y.Z} -m "Release v{X.Y.Z}"
  git push origin v{X.Y.Z}
  ```
- [ ] Update `RELEASE_CHECKLIST.md` if any steps change
- [ ] Clean up old build artifacts (optional):
  ```bash
  rm -rf app/build/
  ```

---

## Quick Reference

| Item | Location |
|---|---|
| Keystore | `app/upload-keystore.jks` |
| Keystore properties | `keystore.properties` (project root, **gitignored**) |
| Upload cert (PEM) | `app/upload_certificate.pem` |
| Keystore password | set in `keystore.properties` or env vars |
| Key alias | `upload` |
| Version catalog | `gradle/libs.versions.toml` |
| Release AAB | `release/ActionDrivingSchool-v{X.Y.Z}-build{N}.aab` |
| Release APK | `app/build/outputs/apk/release/app-release.apk` |
| .gitignore | `/release/`, `app/release/`, `app/build/`, `*.aab`, `*.apk`, `*.jks`, `*.keystore` |

---

## One-Time Setup

### Create `keystore.properties` (before first release)

Create `DRIVINGSCHOOL2/keystore.properties`:

```properties
storeFile=upload-keystore.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=upload
keyPassword=YOUR_KEY_PASSWORD
```

> ⚠️ **Do not commit this file.** It is gitignored. The CI (GitHub Actions) uses env vars instead.
