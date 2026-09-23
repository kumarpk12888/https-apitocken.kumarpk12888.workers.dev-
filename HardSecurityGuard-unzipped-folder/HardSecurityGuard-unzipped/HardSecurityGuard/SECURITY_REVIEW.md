# Security Review — Hard Security Guard

Self-review performed while building this project (no network access in
this environment, so this is a manual/static review, not a live
`gradlew build` + scanner run — see the checklist at the bottom for what
to run yourself once you have Android Studio/CI).

## 1. Secrets

- ✅ No API token, key, or credential is hard-coded anywhere in Kotlin,
  Gradle, or resource files (verified with a grep pass — see below).
- ✅ `CLOUDFLARE_BASE_URL` in `BuildConfig` contains only the public
  Worker hostname, no secret.
- ✅ The Worker's bearer token is entered by the user at runtime and
  stored exclusively in `EncryptedSharedPreferences` (Keystore-backed),
  never in `BuildConfig`, `SharedPreferences` (plain), or a resource file.
- ✅ The encrypted token file is excluded from Android auto-backup via
  `data_extraction_rules.xml`.
- ✅ Network logging (`HttpLoggingInterceptor`) redacts the
  `Authorization` header and is fully OFF in release builds.
- ⚠️ Residual risk (disclosed, not hidden): a user with root access to
  their own device could extract a token they themselves entered. This
  is inherent to any bearer-token design where the client must present
  a secret to a backend it authenticates to. See README's "Production
  hardening" section for the recommended stronger design
  (short-lived, per-device tokens) — not implemented in this version.

```
grep -rniE "bearer [a-z0-9]|api_token\s*=\s*[\"'][a-z0-9]{10,}" --include="*.kt" --include="*.xml" --include="*.gradle.kts" .
# → no literal token values found
```

## 2. Permissions

Manifest requests exactly two permissions, both normal (no runtime
prompt required):
- `INTERNET` — required for Cloudflare sync
- `ACCESS_NETWORK_STATE` — required for the VPN/network check

No `QUERY_ALL_PACKAGES`, no camera, microphone, location, contacts, SMS,
call log, or storage permissions are requested by this app. (The
permission-audit *check* reads permissions of *other* apps via the
standard PackageManager APIs available to any app for LAUNCHER-visible
packages — it does not need a dangerous permission itself to do that.)

## 3. Data collection

- The app does not read or transmit passwords, private messages, photos,
  or contacts, and has no code path capable of doing so (no relevant
  permissions are declared, so the OS would block such access even if
  attempted).
- Data sent to Cloudflare (only if the user configures a token) is
  limited to: device label ("Android"), integer score, a list of warning
  *titles* (e.g. "USB Debugging"), and an ISO-8601 timestamp — no PII.
- Local Room database stores the same shape of data, on-device only.

## 4. Network security

- `network_security_config.xml` sets `cleartextTrafficPermitted="false"`
  at the base level — the app cannot make plaintext HTTP requests to any
  host, only HTTPS.
- Retrofit's `baseUrl` is `https://apitocken.kumarpk12888.workers.dev/`
  (HTTPS only).
- Connect/read timeouts are set (10s) so a hung connection can't block
  the UI indefinitely.

## 5. Honesty / no overclaiming (per your requirements)

- Dashboard copy explicitly states the app "cannot detect every possible
  compromise (e.g. kernel-level spyware or zero-day exploits)".
- Every check that has a real Android visibility limitation
  (installed-app audit, permission audit, accessibility-service audit)
  sets `limited = true` and the UI shows an explicit "Limited by Android
  API visibility rules" note rather than presenting the result as
  exhaustive.
- No check fabricates a result when the underlying API can't answer —
  each one queries a real `Settings`/`PackageManager`/`ConnectivityManager`
  value and reports exactly that.

## 6. Existing Worker

- Not modified. This project only *consumes* the documented contract you
  provided (`POST /report` with `{device, score, warnings, timestamp}`,
  `GET /health`, `Authorization: Bearer <token>`).
- No Worker source lives in this folder — it was out of scope and not
  provided to this session, so nothing here overwrites or duplicates it.

## 7. Dependency review (manual — see note in README)

| Dependency | Version | Note |
|---|---|---|
| Kotlin / AGP | 1.9.24 / 8.5.2 | Current stable pairing as of last knowledge update |
| Compose BOM | 2024.06.00 | Pulls matched, tested versions of all Compose artifacts |
| Room | 2.6.1 | Stable |
| Retrofit / OkHttp | 2.11.0 / 4.12.0 | Stable, widely used |
| Navigation-Compose | 2.7.7 | Stable |
| security-crypto | 1.1.0-alpha06 | ⚠️ Still alpha upstream (Google has not shipped a stable release of this artifact in the source data available to this review). Functionally fine and widely used in production apps despite the version label, but re-check for a stable release before a public Play Store submission. |

No dependency here is known-deprecated/abandoned as of this review. Since
this sandbox has no network access, an automated CVE scan
(`./gradlew dependencyCheckAnalyze` with OWASP Dependency-Check, or
GitHub Dependabot/CodeQL once this is pushed) was **not** run — do that
as your first CI step.

## 8. What to run yourself before shipping

```bash
./gradlew assembleDebug            # confirms it actually compiles in a real SDK environment
./gradlew testDebugUnitTest        # ScoreCalculator + CloudflareSyncService unit tests
./gradlew lint                     # Android Lint — will catch anything missed here
./gradlew dependencyCheckAnalyze   # if you add the OWASP plugin — CVE scan
```

If `assembleDebug` surfaces any error, the most likely spots (based on
what couldn't be verified without a live Gradle/SDK environment) are:
compileSdk/AGP version drift if your Android Studio ships a newer/older
default, and the `security-crypto` alpha artifact if Google has since
renamed or stabilized its API surface.
