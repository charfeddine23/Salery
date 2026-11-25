# Security Summary

**Reviewed Files**: 92
**Issues Resolved**: 10

### Issues Resolved
| Area | File | Line | What Was Happening | Impact | Status | How It Was Fixed |
|---|---|---|---|---|---|---|
| Step-Up Biometric Authentication | `app/src/main/java/com/example/security/StepUpAuthHelper.kt` | 26 | When biometric step-up was required (`requireBiometric = true`), a `null` activity context or temporarily unavailable biometric hardware silently fell through to `onVerified()`. | High | Fixed | Switched to fail-closed behavior calling `onRejected(null)` when a host `FragmentActivity` or biometric verification is unavailable, with clear user toast feedback in `CardsHubScreen.kt` and `KeysScreen.kt`. |
| Screenshot & Modal Sheet Protection | `app/src/main/java/com/example/ui/screens/LocalAccountSetupScreen.kt` | 523, 689 | Wilaya and Commune `ModalBottomSheet` windows used default sheet properties without wiring `LocalScreenshotProtection.current` to `SecureFlagPolicy`, and `EditCardDialog`/`InAppContactsPickerDialog` defaulted `secureScreenshots` to `false`. | Medium | Fixed | Bound `ModalBottomSheetProperties(securePolicy = ...)` to `LocalScreenshotProtection.current` on both sheets and updated `EditCardDialog.kt` and `ContactsPickerSupport.kt` to always OR with `LocalScreenshotProtection.current`. Also updated stale `screenshotProtectionActive` strings in `AppStrings.kt`. |
| Lock-Screen Financial Notification Leaks | `app/src/main/java/com/example/sms/AlgPosteSmsReceiver.kt` | 316, 361, 400 | Incoming BaridiMob/ALG Poste notifications (including transaction suggestions, raw SMS bodies, tax detections, and failed transaction reversals) and debt reminders did not set `VISIBILITY_PRIVATE` or a redacted `publicVersion`. | Medium | Fixed | Applied `NotificationCompat.VISIBILITY_PRIVATE` alongside redacted generic `publicVersion` notifications across all three `AlgPosteSmsReceiver` notifications and `DebtReminderReceiver`. |
| BroadcastReceiver Lifecycle & Prefs Overhead | `app/src/main/java/com/example/notification/DebtReminderReceiver.kt` | 62 | `DebtReminderReceiver.onReceive` launched background Room DB verification via `CoroutineScope(Dispatchers.IO).launch` without calling `goAsync()`, risking premature process termination; receivers also instantiated full `PreferencesManager` objects just to check UI language. | Medium | Fixed | Wrapped async work with `val pendingResult = goAsync()` and `try { ... } finally { pendingResult.finish() }`, and added lightweight `PreferencesManager.readLanguageFast(context)` used by `DebtReminderReceiver` and `DebtReminderScheduler`. |
| Transient Keystore Decrypt Overwrite | `app/src/main/java/com/example/data/PreferencesManager.kt` | 246 | If `VaultCryptoManager.decryptSensitive` returned empty on a transient Keystore failure during settings load, subsequent unrelated `updateSettings` calls could overwrite unread encrypted vault blobs (`vaultKeysJson`, `ccpAccountsJson`, card numbers/CVCs) with `"[]"` or `""`. | High | Fixed | Added `VaultCryptoManager.decryptSensitiveOrNull` to distinguish transient decryption failures (`null`) from empty strings and tracked `preservedEncryptedBlobs` in `PreferencesManager` so unread encrypted ciphertext is preserved rather than overwritten. |
| Backup Sharing & Passphrase Floor | `app/src/main/java/com/example/ui/components/BackupRestoreDialog.kt` | 252, 274 | Share-to-app exported the entire JSON backup inside `Intent.EXTRA_TEXT` (exposing clipboard/text share targets and risking intent size limits), and encrypted backups accepted 4-character passphrases. | Medium | Fixed | Enforced a 6-character minimum passphrase for AES-256-GCM backups and switched share mode to write temporary `.json` files in `cache/backups/` (cleaning previous shares) and share a `FileProvider` `content://` URI via `Intent.EXTRA_STREAM` with read-only URI permission. |
| Full-Vault Backup Coverage & Redaction | `app/src/main/java/com/example/data/BackupData.kt` | 15, 45 | Backups previously excluded Visa/third cards, Vault keys, CCP accounts, savings/gold/Binance holdings, card order, and local account profile; SMS archive parsing also used non-null `getJSONObject`/`getDouble`. | Medium | Fixed | Added full-vault optional fields to `BackupPayload` and `VaultixViewModel` backup/restore, redacting all card PANs/CVCs, Vault keys, and CCP secrets in unencrypted JSON while preserving them in encrypted AES-256-GCM backups; hardened SMS archive JSON parsing. |
| Non-Transactional Restore & Destructive Migration Flag | `app/src/main/java/com/example/viewmodel/VaultixViewModel.kt`, `app/src/main/java/com/example/data/AppDatabase.kt` | 1664, 142 | `restoreFromBackupWithOutcome` cleared transactions and debts without a Room transaction, and `AppDatabase` used deprecated `fallbackToDestructiveMigration()` (defaulting to table destruction on missing migrations). | Medium | Fixed | Wrapped DB restore clears, inserts, and SMS archive merges inside `database.withTransaction { ... }` and changed Room builder to `.fallbackToDestructiveMigration(false)` so unmigrated schema changes cannot silently wipe local financial data. |
| Backup Enum Parsing Fragility | `app/src/main/java/com/example/data/BackupData.kt` | 245, 273 | `BackupPayload.fromJsonString` called `TransactionType.valueOf(...)` and `DebtType.valueOf(...)` directly, causing a single unrecognized enum or missing required field to abort the entire restore. | Low | Fixed | Hardened JSON object parsing with `optJSONObject`, safe `runCatching` enum parsing, and `isFinite()`/`>= 0.0` numeric guards so invalid entries are safely skipped. |
| Dead Code & Hardcoded Sample Cardholder | `app/src/main/java/com/example/viewmodel/VaultixViewModel.kt`, `app/src/main/java/com/example/ui/screens/VisaComingSoonScreen.kt` | 248 | Dormant pattern-lock state/methods, unused strings (`AppStrings.kt`), unused `resetCardHubOrder`/`autoAddUnloggedTaxes`, and hardcoded `"CHARFEDDINE KA"` / static card preview values remained in code and UI. | Low | Fixed | Removed dead pattern-lock settings/methods and unused strings, replaced hardcoded sample names with neutral `"CARDHOLDER"` or the user's configured local username, and wired `VisaComingSoonScreen` to live `UserSettings` card details. |

### Safeguards Verified
| Area | File | How It's Protected |
|---|---|---|
| Local Secret Encryption & Passphrase Backups | `app/src/main/java/com/example/security/VaultCryptoManager.kt` | Encrypts card PANs/CVCs, vault keys, and CCP accounts with Android Keystore AES-256-GCM when available, and encrypts password-protected backups with PBKDF2WithHmacSHA256 (120,000 iterations) + AES-256-GCM with AAD binding. |
| App-Wide Screenshot & Recents Blocking | `app/src/main/java/com/example/ui/screens/MainAppScreen.kt` | Enforces `WindowManager.LayoutParams.FLAG_SECURE` and Android 13+ `setRecentsScreenshotEnabled(false)` across the host window and `DialogProperties(securePolicy = ...)` across all modal dialogs when Screenshot Protection is active. |
| Auto-Update Integrity & URL Allowlist | `app/src/main/java/com/example/update/AppUpdateManager.kt` | Restricts update URLs and HTTP redirects strictly to HTTPS on official GitHub hosts, enforces APK size bounds, validates ZIP/APK structure, and verifies the downloaded APK's signing certificate matches the installed app signature before launching installation. |
| Sensitive Clipboard Auto-Clear | `app/src/main/java/com/example/security/ClipboardSecurityHelper.kt` | Marks copied secrets with `ClipDescription.EXTRA_IS_SENSITIVE` on Android 13+ and schedules automatic clipboard clearing after 45 seconds or app lock. |
| Backup & Manifest Exposure Controls | `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml` | Disables Android cloud/device auto-backup (`android:allowBackup="false"`) and excludes SharedPreferences, Room database, and update files; keeps internal receivers (`DebtReminderReceiver`) non-exported (`android:exported="false"`) and `AlgPosteSmsReceiver` gated by `BROADCAST_SMS`. |
| Room & SQL Query Safety | `app/src/main/java/com/example/data/AppDao.kt` | Uses compile-time verified Room `@Query` annotations and bound parameters exclusively, preventing SQL injection. |

## PoC Verification

### 1. Step-Up Authentication Fail-Open Bypass
#### Summary
| Field | Value |
|---|---|
| Issue Type | Authentication / Step-Up Verification Bypass |
| Impact | High |
| Affected File | `app/src/main/java/com/example/security/StepUpAuthHelper.kt:26` |
| Test Scenario | Call `StepUpAuthHelper.verifyIfEnabled` with `requireBiometric = true` from a non-`FragmentActivity` context or when biometric authentication is unavailable. |

#### How It Was Fixed
Removed the fallback call to `onVerified()` when `findFragmentActivity(context)` is `null` or `BiometricAuthManager.isBiometricAvailable(activity)` is `false`. The helper now fails closed by invoking `onRejected(null)`, and callers (`CardsHubScreen` and `KeysScreen`) display a clear rejection toast without revealing card numbers, CVCs, or vault passwords.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Caller invokes `StepUpAuthHelper.verifyIfEnabled(..., requireBiometric = true)` | Reaches `StepUpAuthHelper.kt:22` |
| 2 | `findFragmentActivity(context)` returns `null` or `BiometricAuthManager.isBiometricAvailable(activity)` returns `false` | Check at `StepUpAuthHelper.kt:27` triggers |
| 3 | Guard executes `onRejected(null); return` instead of `onVerified()` | **Blocked Safely** (Verified in `SecurityAuditUnitTest.stepUpAuth_failsClosedWhenBiometricRequiredAndHostActivityUnavailable`) |

#### Conclusion
**Fix Verified** - Protected vault actions no longer bypass biometric verification if the host activity context or biometric availability check fails.

---

### 2. Transient Keystore Failure Overwriting Encrypted Vault Blobs
#### Summary
| Field | Value |
|---|---|
| Issue Type | Unintended Data Overwrite on Decryption Error |
| Impact | High |
| Affected File | `app/src/main/java/com/example/data/PreferencesManager.kt:246`, `app/src/main/java/com/example/security/VaultCryptoManager.kt` |
| Test Scenario | Trigger a transient Keystore decryption failure during settings load, then update an unrelated setting such as language or theme. |

#### How It Was Fixed
Introduced `VaultCryptoManager.decryptSensitiveOrNull(stored)` returning `null` when Keystore decryption fails on an `"enc_v1:"` blob. `PreferencesManager` tracks unreadable blobs in `preservedEncryptedBlobs` and writes back the preserved ciphertext in `encodeSensitiveOrPreserve` instead of overwriting the preference key with `""` or `"[]"`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | `loadSettings()` encounters an `"enc_v1:"` secret that transiently fails decryption | `decryptSensitiveOrNull` returns `null` |
| 2 | Raw ciphertext key is recorded in `preservedEncryptedBlobs` while UI sees safe default | Original blob retained in memory |
| 3 | User toggles an unrelated preference via `updateSettings` | `encodeSensitiveOrPreserve` preserves existing ciphertext on disk |

#### Conclusion
**Fix Verified** - Unrelated preference writes no longer risk erasing encrypted card secrets, keys, or CCP accounts after transient Keystore errors.

---

### 3. Plaintext Backup Secret Redaction & FileProvider Sharing
#### Summary
| Field | Value |
|---|---|
| Issue Type | Sensitive Data Exposure in Unencrypted Backups & Share Intents |
| Impact | Medium |
| Affected File | `app/src/main/java/com/example/data/BackupData.kt:45`, `app/src/main/java/com/example/ui/components/BackupRestoreDialog.kt:274` |
| Test Scenario | Export an unencrypted backup containing Visa/third-card PANs, CVCs, Vault keys, and CCP accounts, or share a backup via `ACTION_SEND`. |

#### How It Was Fixed
Updated `BackupPayload.toJsonString(includeSensitiveCardFields = false)` to strip Classic/Visa/Third card numbers & CVCs, Vault keys, and CCP accounts in unencrypted JSON exports while preserving them inside PBKDF2 + AES-256-GCM encrypted backups (now requiring a 6-character minimum passphrase). Updated share mode to write to `cache/backups/` and pass a read-scoped `FileProvider` `content://` URI via `Intent.EXTRA_STREAM`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | User exports plaintext backup (`includeSensitiveCardFields = false`) | All card PANs/CVCs, `vaultKeysJson`, and `ccpAccountsJson` are omitted (`""` / `"[]"`) |
| 2 | User exports encrypted backup with >= 6-char passphrase | Full secrets are encrypted under AES-256-GCM envelope |
| 3 | User taps Share Backup | Temporary file written under `cache/backups/` and shared via `FileProvider` `EXTRA_STREAM` with `FLAG_GRANT_READ_URI_PERMISSION` |

#### Conclusion
**Fix Verified** - Sensitive secrets never leave the device in unencrypted share/export payloads and share intents use scoped `FileProvider` URIs.

---

### 4. Modal Bottom Sheet Screenshot Policy & Lock-Screen Notification Redaction
#### Summary
| Field | Value |
|---|---|
| Issue Type | Missing Screenshot Protection on Modal Windows & Lock-Screen Notification Leaks |
| Impact | Medium |
| Affected File | `app/src/main/java/com/example/ui/screens/LocalAccountSetupScreen.kt:526, 695`, `app/src/main/java/com/example/sms/AlgPosteSmsReceiver.kt:316` |
| Test Scenario | Open the Wilaya/Commune picker sheets while Screenshot Protection is enabled, or receive an ALG Poste SMS / debt reminder on the lock screen. |

#### How It Was Fixed
Bound `ModalBottomSheetProperties(securePolicy = ...)` to `LocalScreenshotProtection.current`, enforced `LocalScreenshotProtection.current` across `EditCardDialog` and `InAppContactsPickerDialog`, and applied `NotificationCompat.VISIBILITY_PRIVATE` + generic `publicVersion` notifications across all SMS alerts and debt reminders.

#### Conclusion
**Fix Verified** - Modal bottom sheets enforce `SecureFlagPolicy.SecureOn` when protection is active, and financial notifications remain redacted on locked screens.

---

### 5. Atomic Room Backup Restore & Safe Migration Policy
#### Summary
| Field | Value |
|---|---|
| Issue Type | Non-Atomic Database Restore & Destructive Migration Risk |
| Impact | Medium |
| Affected File | `app/src/main/java/com/example/viewmodel/VaultixViewModel.kt:1664`, `app/src/main/java/com/example/data/AppDatabase.kt:142` |
| Test Scenario | Restore a backup JSON with an unknown enum entry or encounter an insert error mid-restore. |

#### How It Was Fixed
Wrapped `dao.clearAllTransactions()`, `dao.clearAllDebts()`, `dao.insertTransactions()`, `dao.insertDebts()`, and `dao.insertArchivedSmsIfMissing()` in `database.withTransaction { ... }`, hardened `BackupPayload.fromJsonString` enum/numeric parsing, and configured `AppDatabase` with `.fallbackToDestructiveMigration(false)`.

#### Conclusion
**Fix Verified** - Failed restores roll back cleanly without wiping user history, and unmigrated schema changes will not silently drop tables.
