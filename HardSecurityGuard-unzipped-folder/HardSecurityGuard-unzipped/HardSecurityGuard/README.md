# Hard Security Guard (Android)

Native Android Kotlin app that runs local, offline-first security checks
and (optionally) syncs a scan summary to the existing Cloudflare Worker at
`https://apitocken.kumarpk12888.workers.dev/report`.

This folder is meant to be dropped into the existing repository as:

```
your-repo/
├── worker/              # existing Cloudflare Worker — untouched by this project
└── android/
    └── HardSecurityGuard/   ← this folder
```

## What it does

- Security dashboard with a 0–100 score
- Checks: USB debugging, Developer options, Accessibility services,
  Device Administrator apps, installed-app audit, sensitive-permission
  audit, VPN/network status
- Local scan history (Room database) — works fully offline
- Optional Cloudflare Worker sync of `{device, score, warnings, timestamp}`
- Emergency screen with shortcuts into system security settings
- Dark mode (Material 3, follows system theme)

## Architecture

```
security/          — modular SecurityCheck implementations + orchestrator
                      (SecurityScanner) + ScoreCalculator (pure, unit-tested)
data/local/        — Room DB: scan_history table, DAO
data/remote/       — Retrofit API, CloudflareSyncService, SyncTokenStore
viewmodel/         — SecurityViewModel (single source of UI state)
ui/                — Jetpack Compose screens (Dashboard, History,
                     Emergency, Settings) + Material 3 theme
```

Adding a new check later (e.g. root detection via a native library) means
writing one new class implementing `SecurityCheck` and adding it to the
list in `SecurityScanner` — nothing else needs to change.

## Security design — why there's no token in the APK

The Worker requires `Authorization: Bearer <API_TOKEN>` on `/health` and
`/report`. That token is **never** written into source, Gradle files, or
`BuildConfig`. Instead:

1. The app works fully offline by default — sync is optional.
2. The token, if the user chooses to enable sync, is entered in the
   Settings screen and stored only in **EncryptedSharedPreferences**
   (Android Keystore-backed AES-256), excluded from Android auto-backup.
3. `CloudflareSyncService` reads the token at call time via a lambda —
   it is never a compile-time constant.
4. Network logging redacts the `Authorization` header even in debug
   builds, and is fully disabled in release builds.

**Honest limitation:** this is still a bearer-token pattern. A user who
roots their own device could extract a token they entered themselves.
That's a materially smaller risk than shipping the Worker's admin secret
inside a publicly distributed APK (which anyone could extract via
`apktool`/`jadx` in minutes) — but it is not zero-risk.

### Production hardening (if this app moves from personal/trusted use to wider public distribution)

Recommended next step: replace the single long-lived admin token with a
per-device, short-lived token minted by an authenticated endpoint on the
Worker (e.g. sign in once, receive a scoped JWT with a few hours' TTL,
refresh it silently). That removes the single-shared-secret risk
entirely. This is **not implemented** in this version — flagged here per
your request rather than guessed at silently.

## Android API limitations (disclosed in-app too)

- **Installed-app / permission audits**: Android 11+ hides the full
  installed-package list unless an app holds `QUERY_ALL_PACKAGES`, which
  Google Play restricts to narrow use cases this app doesn't qualify
  for. This app deliberately does not request it. Instead it declares a
  `<queries>` filter for the LAUNCHER intent, so these two checks only
  see apps that appear on the home screen/app drawer — not background
  services or system components. The UI says this explicitly.
- **Accessibility / Device Admin checks**: the app can list which
  services/admins are *enabled*, but cannot judge whether a given one is
  malicious — that's left to the user, and the UI says so.
- The app never claims to detect rootkits, kernel-level spyware, or
  zero-day exploits — the dashboard states this limitation directly.

## Permissions requested

Only `INTERNET` and `ACCESS_NETWORK_STATE` — both normal (non-runtime)
permissions. No `QUERY_ALL_PACKAGES`, no camera/mic/location/contacts/SMS
access. The app does not collect passwords, private messages, photos, or
contacts.

## Building

Prerequisites: Android Studio (Koala or newer) or a standalone Gradle +
JDK 17 + Android SDK (compileSdk 34) setup.

```bash
# From android/HardSecurityGuard/
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK (unsigned unless you configure signing)
./gradlew bundleRelease          # release AAB for Play Store
```

Output locations:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

### Signing a release build

Add a signing config before `buildTypes { release { ... } }` in
`app/build.gradle.kts`, e.g.:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("HSG_KEYSTORE_PATH") ?: "release.keystore")
        storePassword = System.getenv("HSG_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("HSG_KEY_ALIAS")
        keyPassword = System.getenv("HSG_KEY_PASSWORD")
    }
}
```
then reference `signingConfig = signingConfigs.getByName("release")` in
the `release` build type. Never commit the keystore or passwords —
pass them as environment variables or CI secrets.

### Running tests

```bash
./gradlew testDebugUnitTest        # pure-Kotlin unit tests (ScoreCalculator, CloudflareSyncService)
./gradlew connectedDebugAndroidTest  # instrumented tests, needs a device/emulator
```

> **Note on this delivery**: this project was built and reviewed line-by-line
> in a sandboxed environment without network/Android-SDK access, so
> `./gradlew` itself could not be executed here to produce a live build
> log. Every file was hand-reviewed for API-level compatibility (minSdk
> 26), dependency version pairing (Kotlin/Compose/KSP), and null-safety.
> Run the two commands above once you pull this into Android Studio or
> CI — see `SECURITY_REVIEW.md` for the full review checklist and what
> to double check first.

## Dependencies (manual review — see SECURITY_REVIEW.md)

Retrofit 2.11.0, OkHttp 4.12.0, Room 2.6.1, Compose BOM 2024.06.00,
Navigation-Compose 2.7.7, security-crypto 1.1.0-alpha06 (still alpha
upstream — pin to a stable release once Google ships one).
