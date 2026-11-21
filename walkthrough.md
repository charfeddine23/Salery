# Security Summary

**Reviewed Files**: 78
**Issues Resolved**: 9

### Issues Resolved
| Area | File | Line | What Was Happening | Impact | Status | How It Was Fixed |
|---|---|---|---|---|---|---|
| Screenshot & Recents Exposure | `app/src/main/java/com/example/ui/screens/MainAppScreen.kt` & `CardsHubScreen.kt` | 311 / 146 | `FLAG_SECURE` and `setRecentsScreenshotEnabled(false)` were only applied while viewing `CardsHubScreen` and were explicitly cleared on navigation away, leaving `KeysScreen` passwords, `CcpsScreen` banking details, `BinanceWalletScreen`, `FinancialInsightsScreen`, `LockScreen`, and 11 modals exposed to screenshots and Recents previews. | High | Fixed | Centralized window-level `FLAG_SECURE` and `setRecentsScreenshotEnabled` inside `MainAppScreen` and enforced `SecureFlagPolicy` via `LocalScreenshotProtection` across all dialogs. |
| Auto-Backup & D2D Data Extraction | `app/src/main/AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml` | 70 / 8 / 6 | `android:allowBackup="true"` with commented-out rules allowed Android Cloud Auto-Backup and device transfers to copy `salerypelse_prefs.xml` and the local Room database unencrypted off-device. | High | Fixed | Disabled `android:allowBackup` and configured explicit exclusion rules for `salerypelse_prefs.xml`, `salerypelse_database`, and `updates/` in both XML rule sets. |
| Plaintext Credentials at Rest | `app/src/main/java/com/example/security/VaultCryptoManager.kt` & `PreferencesManager.kt` | 192 / 265 | Vault passwords (`vault_keys_json`), CCP/RIP profiles (`ccp_accounts_json`), and card numbers/CVCs were written directly to `SharedPreferences` in plaintext. | High | Fixed | Added Android Keystore AES-256-GCM encryption for stored credentials on write and transparent decryption/migration on read. |
| Unsalted Pattern Storage & Missing Lockout | `app/src/main/java/com/example/viewmodel/SaleryPelseViewModel.kt` | 914 / 977 | Unlock patterns were stored raw (`"0-1-2-5-8"`) and compared directly without brute-force throttling. | High | Fixed | Switched pattern persistence to salted SHA-256 (`sha256$<salt>$<digest>`) with constant-time `MessageDigest.isEqual`, transparent upgrade of legacy patterns, and a 30-second cooldown after 5 failed attempts. |
| Silent Global Clipboard Financial Leak | `app/src/main/java/com/example/ui/components/BackupRestoreDialog.kt` | 284 | Exporting a backup silently placed the entire unencrypted financial JSON into the system clipboard, and credential/CCP copies lacked the sensitive-content flag. | High | Fixed | Removed automatic clipboard copying of backups and routed credential/CCP copies through `ClipboardSecurityHelper` with `android.content.extra.IS_SENSITIVE = true`. |
| Card PAN/CVC in Unencrypted Backups | `app/src/main/java/com/example/data/BackupData.kt` | 38 / 107 | Plaintext JSON backups exported full classic card numbers and CVC codes and accepted unbounded JSON strings or negative amounts on import. | Medium | Fixed | Redacted card PAN and CVC during backup export, preserved local encrypted card credentials when restoring sanitized backups, and enforced a 10 MB size cap plus numeric validation on restore. |
| OTA APK Host & Signer Verification | `app/src/main/java/com/example/update/AppUpdateManager.kt` | 209 / 305 | Release downloads did not validate HTTPS GitHub hostnames before or after redirects, had no maximum byte limit, and checked only package name rather than signing certificates before launching the installer. | High | Fixed | Added strict HTTPS GitHub host allowlisting pre-request and post-redirect, a 200 MB stream limit, and SHA-256 signing-certificate matching against the installed package. |
| Financial PII in System Logs | `app/src/main/java/com/example/notification/DebtReminderReceiver.kt` | 40 | `Log.d` wrote contact names (`personName`) and owed balances (`remainingAmount`) to Android system logcat. | Low | Fixed | Redacted person name and amount so only non-PII `debtId` is logged. |
| Dead Code & Unreferenced Screenshots | `/public/assets/aistudio/`, `BinanceWalletScreen.kt`, `PatternLockView.kt`, `LockScreen.kt` | — | 12 unreferenced screenshot/upload files, ~293 lines of duplicated portrait cards in `BinanceWalletScreen`, and unused `PatternLockView` / `LockScreen` parameters cluttered the codebase. | Info | Fixed | Deleted `/public/assets/aistudio/`, removed duplicated portrait UI blocks, removed dead parameters/composables while preserving `toPatternHash()`. |

### Safeguards Verified
| Area | File | How It's Protected |
|---|---|---|
| Hardware-Backed Local Crypto | `app/src/main/java/com/example/security/VaultCryptoManager.kt` | Uses Android Keystore AES-256-GCM keys and salted SHA-256 verification without hardcoding secrets in source code. |
| SMS Receiver Broadcast Trust | `app/src/main/AndroidManifest.xml` | `AlgPosteSmsReceiver` enforces `android:permission="android.permission.BROADCAST_SMS"`, preventing spoofed third-party app broadcasts. |
| Exported Activity / FileProvider | `app/src/main/AndroidManifest.xml` | `DebtReminderReceiver` and `FileProvider` are explicitly `android:exported="false"`, isolating update files to granted installer intents only. |
| Database Queries | `app/src/main/java/com/example/data/AppDao.kt` | All Room database reads/writes use compile-time parameterized SQL queries. |
| Repository Signing Keystores (Manual Review) | `my-upload-key.jks`, `app/salerypelse-release.jks`, `.github/workflows/android-release.yml` | Preserved intact so local/CI builds do not break; recommended for follow-up rotation out of source control and replacing fallback workflow store passwords. |

## PoC Verification

### 1. Screenshot & Recents Leakage Across Password Vault
#### Summary
| Field | Value |
|---|---|
| Issue Type | Missing Window & Dialog `FLAG_SECURE` Protection |
| Impact | High |
| Affected File | `app/src/main/java/com/example/ui/screens/MainAppScreen.kt:312` |
| Test Scenario | Enable `screenshotProtectionEnabled`, navigate from `CardsHubScreen` to `KeysScreen`, press Android Recents, and open `AddDebtDialog` / `VaultHoldActionDialog`. |

#### How It Was Fixed
Moved `FLAG_SECURE` and `setRecentsScreenshotEnabled` enforcement to `MainAppScreen` across the entire lifecycle and propagated `LocalScreenshotProtection` into every dialog's `DialogProperties.securePolicy`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | User enables Screenshot Protection; `LocalScreenshotProtection` provides `true` | State updates |
| 2 | `MainAppScreen.kt:313` sets `WindowManager.LayoutParams.FLAG_SECURE` and `setRecentsScreenshotEnabled(false)` | Whole activity protected |
| 3 | User navigates off `CardsHubScreen` (`CardsHubScreen.kt` no longer strips flags on `onDispose`) | Flag remains active |
| 4 | Any modal (`BackupRestoreDialog`, `VaultHoldActionDialog`, etc.) opens with `SecureFlagPolicy.SecureOn` | **Blocked from Capture & Recents** |

#### Conclusion
**Fix Verified** - Navigating between tabs or opening modals no longer drops `FLAG_SECURE` or exposes sensitive views in Recents thumbnails.

### 2. Untrusted OTA Update Download & Unverified APK Signer
#### Summary
| Field | Value |
|---|---|
| Issue Type | Missing URL Allowlist & Package Signer Verification |
| Impact | High |
| Affected File | `app/src/main/java/com/example/update/AppUpdateManager.kt:211` |
| Test Scenario | Release JSON returns `"browser_download_url": "http://169.254.169.254/latest/meta-data/"` or `"https://github.com.evil.example/payload.apk"`, or a valid APK signed with a different key. |

#### How It Was Fixed
`AppUpdateManager.isTrustedUpdateUrl` enforces `https` and an exact GitHub/githubusercontent host allowlist both before fetching and after redirects (`response.request.url`), caps stream length at 200 MB, and `isValidUpdateApk` verifies matching SHA-256 signing certificates.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | `downloadApk` receives `release.apkDownloadUrl` | Enters validation |
| 2 | `AppUpdateManager.kt:211` runs `isTrustedUpdateUrl(downloadUrl)` | Non-HTTPS / non-GitHub hosts rejected |
| 3 | Even if an allowlisted host redirected elsewhere, line 244 checks `response.request.url` | Redirected host rejected |
| 4 | Validated stream enters `isValidUpdateApk` (`line 307`) -> `verifyApkMatchesInstalledSigner` | **Foreign-Signed APK Rejected** |

#### Conclusion
**Fix Verified** - Arbitrary HTTP/redirect hosts, oversized streams, and APKs not matching the installed app's signing certificate are blocked before installation.

### 3. Plaintext Pattern & Unencrypted Credential Exposure
#### Summary
| Field | Value |
|---|---|
| Issue Type | Plaintext Credential Storage & Global Backup Clipboard Leak |
| Impact | High |
| Affected File | `app/src/main/java/com/example/data/PreferencesManager.kt:190` |
| Test Scenario | User saves passwords in Keys, card CVCs in Cards Hub, or clicks Export Backup in `BackupRestoreDialog`. |

#### How It Was Fixed
`VaultCryptoManager` encrypts stored credentials using Android Keystore AES-256-GCM before persisting to `SharedPreferences`, hashes unlock patterns with a random 16-byte salt + SHA-256 + 30s brute-force lockout, redacts card PAN/CVC from backup exports, and `BackupRestoreDialog` no longer writes backup JSON to the clipboard.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Credentials saved via `PreferencesManager.updateSettings` | `VaultCryptoManager.encryptSensitive` wraps with `v1_gcm$` AES-GCM |
| 2 | Unlock attempts hit `SaleryPelseViewModel.unlockApp` | Verified with constant-time salted SHA-256; throttled after 5 failures |
| 3 | User triggers Export Backup (`BackupRestoreDialog.kt:279`) | Backup is written only via SAF / explicit Share with card PAN & CVC omitted (`""`) |
| 4 | Result | **Secrets Isolated From Disk, Cloud Auto-Backup, and Clipboard** |

#### Conclusion
**Fix Verified** - Credentials are encrypted at rest, excluded from Android cloud backups/device transfers, redacted from plaintext exports, and never silently copied to the global clipboard.
