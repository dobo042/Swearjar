# Handover

Last updated: 2026-11-16. Branch: `claude/bold-noether-1w57l9`. No pull request has been opened.

## State in one paragraph

The system plan is agreed and the foundations are in. The app **builds** into a debug APK.
The rules engine is verified (28 unit tests), the Firestore security rules are verified in
the emulator (32 tests), and every screen has been rendered from its Compose preview and
looked at (`docs/screens.png`, `docs/jar-states.png`). The Firebase project
`swear-jar-ec309` has an Android app registered, Google sign-in enabled and a Firestore
database in London. What has **never been run** is the app itself on a device: sign-in,
creating a jar and joining one are untested end to end. The user will build in Android
Studio but does not have access to it yet.

**Check first:** the jar rules may not be deployed. The deploy was being retried when this
was written. Run `npx -y firebase-tools@latest deploy --only firestore:rules --project
swear-jar-ec309` (it is safe to repeat); until it succeeds, the live project only allows
`users/{uid}` and creating a jar will fail with permission denied.

## Blocked on the user

1. **Debug SHA-1.** Google sign-in on Android only works for builds signed with a key
   registered on the Firebase Android app. None is registered yet. It must come from
   whichever machine builds the APK the user installs (`./gradlew signingReport`), then:

   ```bash
   npx -y firebase-tools@latest apps:android:sha:create 1:496502258154:android:379e72c625afcf2eb61a93 <SHA1> --project swear-jar-ec309
   ```

   Re-fetch `app/google-services.json` afterwards (command below).

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
| Models and rules engine, `JarDraft` validation | `domain/` | 28 tests pass |
| Firestore rules for users, jars, members, rounds, invite codes | `firestore.rules` | 32 emulator tests pass |
| Jar repository: list, observe, create, join | `app/.../data/jar` | Compiles; never run |
| Screens: jar list, create jar, join jar (code or QR), jar home with invite QR and members | `app/.../ui` | Rendered and checked; never run |
| App shell: theme, Hilt, activity, nav host, icon | `app/src/main` | Compiles |
| Firebase SDK wiring, Hilt module | `app/build.gradle.kts`, `app/.../di` | Compiles |
| Google sign-in, sign-out, profile write | `app/.../data/auth`, `ui/signin`, `ui/session` | Compiles; never run |
| Jar drawing: flat fill to a dashed goal line, overflow up the neck | `app/.../ui/jar/JarCanvas.kt` | Rendered and checked (`docs/jar-states.png`) |
| Jar animations: rising fill, confetti | same | Compiles; never seen |

## Next steps, in order

1. Register a SHA-1, install the app on a device, sign in, create a jar, join it from a
   second account. Fix what that turns up.
2. **Topics, logging, ledger**: topics for admins to manage, the log-entry transaction
   (uses `LoggingRules`, `PricingRules`, `GoalRules`), rules and tests, the ledger screen.
   The jar home screen currently says topics are "coming next"; replace that line.
3. **Jar settings**: editing name, goal and the three settings after creation; promoting
   to admin; leaving a jar. No rule allows any of these yet.
4. **Disputes, settlements, rounds.**
5. Later: push notifications (Cloud Functions, Blaze plan), stats, reminders.

## Firestore data model

Implemented (the header comment in `firestore.rules` is the authoritative field list):

```
users/{uid}                 displayName, photoUrl?, createdAt, updatedAt   (owner-only, no email)
inviteCodes/{code}          jarId                                          (get by code only, no listing)
jars/{jarId}                name, ownerId, currency, inviteCode, memberIds[],
                            settings { loggingPolicy, pricingMode, goalBehaviour, escalation{…} },
                            currentRoundId, goalMinor, roundTotalMinor, status, createdAt
  members/{uid}             role, displayName, photoUrl?, owedMinor, paidMinor, joinCode, joinedAt
  rounds/{roundId}          goalMinor, totalMinor, startedAt
```

Enums are stored as their Kotlin names (`ANYONE`, `OWNER`, `ACTIVE`). Creating a jar is one
batch of four documents; joining is one batch of two (append own uid to `memberIds`, create
own member document carrying the invite code). `joinCode` on the member document is how the
rules check the joiner actually had the code.

Agreed, not yet implemented:

```
jars/{jarId}/topics/{topicId}       name, emoji, priceMinor, archived
jars/{jarId}/entries/{entryId}      roundId, topicId, topicName, offenderId, loggedById,
                                    amountMinor, note, status(active|disputed|voided), createdAt
jars/{jarId}/settlements/{id}       memberId, amountMinor, recordedById, createdAt
rounds: completedAt
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
- **The demo jar is gone.** Jar home shows a real jar, which stays at zero until logging
  exists.
- **A jar holds at most 50 members** (`Jar.MAX_MEMBERS`, enforced in the rules).
- **Shell commands sometimes fail with "classifier gave no verdict".** It is transient;
  retry, and use file edits in the meantime.
- **Escalation needs a count.** `PricingRules.priceFor` takes `priorOffences`: the
  offender's live entries on that topic inside `Escalation.windowDays`. It needs a Firestore
  query before the logging transaction.
- **No Cloud Functions means client-trusted writes.** Totals and escalating prices are
  computed on the phone and guarded only by security rules.
- **Rule decisions the user has not explicitly reviewed** (in `LoggingRules` /
  `PricingRules`, with tests): a disputed entry still counts until an admin voids it; an
  admin cannot resolve a dispute on an entry they logged; whoever logged an entry can
  retract it, except a self-report; a single entry is capped at 1,000,000 minor units;
  default escalation doubles every 3 offences in 7 days, at most 3 times.
