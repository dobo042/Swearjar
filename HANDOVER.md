# Handover

Last updated: 2026-11-16. Branch: `claude/bold-noether-1w57l9`. No pull request has been opened.

## State in one paragraph

The system plan is agreed and the foundations are in. The app **builds** into a debug APK.
The rules engine is verified (25 unit tests), the Firestore security rules are verified in
the emulator (9 tests) and deployed, and the jar drawing has been rendered and looked at.
The Firebase project `swear-jar-ec309` has an Android app registered, Google sign-in enabled
and a Firestore database in London. What has **never been run** is the app itself on a
device: sign-in, the profile write and the jar screen are untested end to end.

## Blocked on the user

1. **Debug SHA-1.** Google sign-in on Android only works for builds signed with a key
   registered on the Firebase Android app. None is registered yet. It must come from
   whichever machine builds the APK the user installs (`./gradlew signingReport`), then:

   ```bash
   npx -y firebase-tools@latest apps:android:sha:create 1:496502258154:android:379e72c625afcf2eb61a93 <SHA1> --project swear-jar-ec309
   ```

   Re-fetch `app/google-services.json` afterwards (command below).
2. **How the user will run the app** (Android Studio on their machine, or an APK built in a
   cloud session) is not yet known. A cloud-built APK is signed with that container's
   throwaway debug key, so it would need a fixed signing key to be useful.

## Firebase project state

| Item | State |
|---|---|
| Android app `com.swearjar` | Registered. App ID `1:496502258154:android:379e72c625afcf2eb61a93` |
| Google sign-in provider | Enabled via `firebase.json` `auth` block and `deploy --only auth` |
| Firestore | `(default)` database, Standard edition, `europe-west2`. Rules and indexes deployed |
| `app/google-services.json` | Fetched and committed (user's choice). Contains the web client ID |
| SHA-1 fingerprints | None registered |

Re-fetch the config after any project change that affects it:

```bash
npx -y firebase-tools@latest apps:sdkconfig ANDROID 1:496502258154:android:379e72c625afcf2eb61a93 --project swear-jar-ec309 --out app/google-services.json
```

The Firebase CLI login lives only in the cloud container it was done in. A new session must
sign in again: run `npx -y firebase-tools@latest login`, give the user the link it prints,
and complete with `npx -y firebase-tools@latest login <code>`.

## Done

| Piece | Where | Verified |
|---|---|---|
| Gradle scaffold, version catalog, wrapper | root, `gradle/` | Builds |
| Models and rules engine | `domain/` | 25 tests pass |
| Firestore rules for `users/{uid}`, everything else denied | `firestore.rules` | 9 emulator tests pass; deployed |
| App shell: theme, Hilt, activity, nav host, icon | `app/src/main` | Compiles |
| Firebase SDK wiring, Hilt module | `app/build.gradle.kts`, `app/.../di` | Compiles |
| Google sign-in, sign-out, profile write | `app/.../data/auth`, `ui/signin`, `ui/session` | Compiles; never run |
| Jar drawing: flat fill to a dashed goal line, overflow up the neck | `app/.../ui/jar/JarCanvas.kt` | Rendered and checked (`docs/jar-states.png`) |
| Jar animations: rising fill, confetti | same | Compiles; never seen |
| Jar screen on an in-memory demo jar | `app/.../ui/jarhome` | Compiles; never run |

## Next steps, in order

1. Register a SHA-1, install the app on a device, sign in, confirm `users/{uid}` appears in
   Firestore. Fix what that turns up.
2. **Jars**: "my jars", the create-jar wizard (name, currency, goal, the three settings),
   join by code or QR (`play-services-code-scanner`, ZXing core). Rules and rules tests for
   `jars`, `members`, `inviteCodes`.
3. **Topics, logging, ledger**: repositories in `app/.../data`, the log-entry transaction,
   rules and tests, the ledger screen. Replace the demo state in `JarHomeViewModel`.
4. **Disputes, settlements, rounds.**
5. Later: push notifications (Cloud Functions, Blaze plan), stats, reminders.

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

- **Plugins live in the session, not the repo.** The Firebase and Android CLI plugins were
  installed into one cloud container (commands in `CLAUDE.md`); a new session must reinstall
  them. Their skills are plain files under `~/.claude/plugins/marketplaces/` and can be read
  directly even when the session has not loaded them as tools.
- **Cloud session setup takes a few minutes**: Android command-line tools, then
  `platforms/android-36`, `build-tools/36.0.0`, `platform-tools`. The user has accepted the
  SDK licence. Maven Central sometimes answers 429; wait and retry.
- **The user's support email is in `firebase.json`**, at their request, for the Google
  sign-in consent screen.
- **The jar is a flat fill, not coins**, at the user's request ("simply a line"). Do not
  reintroduce coins.
- **Demo jar is temporary.** `JarHomeViewModel` holds an in-memory jar and the screen shows
  a banner saying so. Remove both in step 3.
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
