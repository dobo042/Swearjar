# Handover

Last updated: 2026-11-16. Branch: `claude/bold-noether-1w57l9`. No pull request has been opened.

## State in one paragraph

The system plan is agreed. The rules engine in `domain/` is verified (25 unit tests) and the
Firestore security rules are verified in the emulator (9 tests). The Android app module in
`app/` **has never been compiled**: the machine it was written on had no Android SDK. It now
contains Google sign-in and Firebase wiring as well as the jar screen. Treat everything
under `app/` as unbuilt until `./gradlew :app:assembleDebug` succeeds, and expect compile
fixes. Nothing has been changed in the Firebase project `swear-jar-ec309` yet, because the
Firebase CLI was not signed in.

## Blocked on the user

1. **Firebase CLI sign-in.** `npx -y firebase-tools@latest login --no-localhost` needs the
   user to open a link and return a code. Until then no project-side step can run.
2. **Android SDK licence.** Installing the SDK in a cloud session means accepting Google's
   SDK licence, which is the user's decision. The alternative is the user building in
   Android Studio and reporting errors.
3. **Debug SHA-1.** Google sign-in on Android only works for builds signed with a key
   registered on the Firebase Android app. It comes from whichever machine builds the APK.
4. **Support email** for the Google sign-in consent screen, and the Firestore **location**
   (and edition) if no database exists yet.

## Firebase project steps still to run (in order, once signed in)

```bash
npx -y firebase-tools@latest apps:create ANDROID Swearjar --package-name com.swearjar --project swear-jar-ec309
```

Then add the SHA-1 (`apps:android:sha:create <APP_ID> <SHA1>`), add the `auth` block with
`googleSignIn` to `firebase.json` and `deploy --only auth`, check
`firestore:databases:list` and `deploy --only firestore`, and **last** fetch the config:

```bash
npx -y firebase-tools@latest apps:sdkconfig ANDROID <APP_ID> --project swear-jar-ec309
```

saved as `app/google-services.json`. It must be fetched after Google sign-in is enabled, or
it will lack the web client ID that `R.string.default_web_client_id` is generated from, and
the app will not compile. The file is gitignored; whether to commit it is undecided.

## Done

| Piece | Where | Verified |
|---|---|---|
| Gradle scaffold, version catalog, wrapper | root, `gradle/` | Configures; plugins resolve |
| Models and rules engine | `domain/` | 25 tests pass |
| Firestore rules for `users/{uid}`, everything else denied | `firestore.rules` | 9 emulator tests pass |
| Firebase config files | `firebase.json`, `.firebaserc`, `firestore.indexes.json` | Not deployed |
| App shell: theme, Hilt, activity, nav host, icon | `app/src/main` | Not compiled |
| Firebase SDK wiring, Hilt module | `app/build.gradle.kts`, `app/.../di` | Not compiled |
| Google sign-in, sign-out, profile write | `app/.../data/auth`, `ui/signin`, `ui/session` | Not compiled, never run |
| Jar drawing with fill, coin drop, confetti, overflow | `app/.../ui/jar/JarCanvas.kt` | Not compiled, never seen |
| Jar screen on an in-memory demo jar | `app/.../ui/jarhome` | Not compiled |

## Next steps, in order

1. Run the Firebase project steps above.
2. Get `:app:assembleDebug` green; fix what the first compile turns up. Sign in on a device.
   Look at `JarCanvas` at 0%, 50%, 100% and overflow and tune it.
3. **Jars**: "my jars", the create-jar wizard (name, currency, goal, the three settings),
   join by code or QR (`play-services-code-scanner`, ZXing core). Rules and rules tests for
   `jars`, `members`, `inviteCodes`.
4. **Topics, logging, ledger**: repositories in `app/.../data`, the log-entry transaction,
   rules and tests, the ledger screen. Replace the demo state in `JarHomeViewModel`.
5. **Disputes, settlements, rounds.**
6. Later: push notifications (Cloud Functions, Blaze plan), stats, reminders.

## Firestore data model

Implemented: `users/{uid}` — `displayName`, `photoUrl` (optional, https), `createdAt`,
`updatedAt`. Owner-only. No email is stored anywhere.

Agreed, not yet implemented:

```
inviteCodes/{code}          jarId
jars/{jarId}                name, ownerId, currency, inviteCode, memberIds[],
                            settings { loggingPolicy, pricingMode, escalation{…}, goalBehaviour },
                            currentRoundId, goalMinor, roundTotalMinor, status
  members/{uid}             role, displayName, photoUrl, owedMinor, paidMinor, joinedAt
  topics/{topicId}          name, emoji, priceMinor, archived
  rounds/{roundId}          goalMinor, totalMinor, startedAt, completedAt
  entries/{entryId}         roundId, topicId, topicName, offenderId, loggedById,
                            amountMinor, note, status(active|disputed|voided), createdAt
  settlements/{id}          memberId, amountMinor, recordedById, createdAt
```

Rules intent: only members read a jar; only owner/admins edit settings, topics and roles;
entry creation is checked against `settings.loggingPolicy`; entries cannot be deleted; a
member can never set or raise their own role.

## Things worth knowing

- **Firebase plugin lives in the session, not the repo.** It was installed into one cloud
  container with the two `claude plugin` commands in `CLAUDE.md`; a new session must
  reinstall it. Its skills are plain files under
  `~/.claude/plugins/marketplaces/firebase/skills` and can be read directly.
- **Firestore edition.** The Firebase skill defaults a new database to Enterprise edition;
  the app code so far uses only core APIs that work on either. Check
  `firestore:databases:list` and ask the user before creating one.
- **Demo jar is temporary.** `JarHomeViewModel` holds an in-memory jar and the screen shows
  a banner saying so. Remove both in step 4.
- **Escalation needs a count.** `PricingRules.priceFor` takes `priorOffences`: the
  offender's live entries on that topic inside `Escalation.windowDays`. The demo counts per
  round; the real version needs a query before the transaction.
- **No Cloud Functions means client-trusted writes.** Totals and escalating prices are
  computed on the phone and guarded only by security rules.
- **Rule decisions the user has not explicitly reviewed** (in `LoggingRules` /
  `PricingRules`, with tests): a disputed entry still counts until an admin voids it; an
  admin cannot resolve a dispute on an entry they logged; whoever logged an entry can
  retract it, except a self-report; a single entry is capped at 1,000,000 minor units;
  default escalation doubles every 3 offences in 7 days, at most 3 times.
- **Toolchain is pinned to late 2025** (see `CLAUDE.md`); Firebase and sign-in libraries are
  the latest and were checked to be compatible with it from their published metadata.
