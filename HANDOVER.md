# Handover

Last updated: 2026-11-16. Branch: `claude/bold-noether-1w57l9`. No pull request has been opened.

## State in one paragraph

The system plan is agreed and the first slice is written. The rules engine in `domain/` is
verified: 25 unit tests pass. The Android app module in `app/` **has never been compiled**,
because the machine it was written on had no Android SDK. Treat everything under `app/` as
unbuilt until `./gradlew :app:assembleDebug` succeeds, and expect some compile fixes.

## Blocked on the user

1. **Android SDK licence.** Installing the SDK in the cloud session means accepting Google's
   SDK licence, which must be the user's decision. Either the user says they accept it (then
   install `platforms/android-36`, `build-tools/36.0.0`, `platform-tools` with the
   command-line tools already unpacked at `/opt/android-sdk` if that container still exists),
   or the user builds in Android Studio and reports errors.
2. **Firebase project.** Needs Google sign-in and Firestore enabled, the debug SHA-1
   registered, and `google-services.json` placed at `app/google-services.json`. Nothing
   shared can be built or tested without it.

## Done

| Piece | Where | Verified |
|---|---|---|
| Gradle scaffold, version catalog, wrapper | root, `gradle/` | Configures; plugins resolve |
| Models | `domain/src/main/kotlin/com/swearjar/domain/model` | Compiles |
| Rules engine: logging, pricing, goal, invite codes, money | `domain/.../rules` | 25 tests pass |
| App shell: theme, Hilt application, activity, nav host, icon | `app/src/main` | Not compiled |
| Jar drawing with fill, coin drop, confetti, overflow | `app/.../ui/jar/JarCanvas.kt` | Not compiled, never seen on screen |
| Jar screen on an in-memory demo jar | `app/.../ui/jarhome` | Not compiled |

## Next steps, in order

1. Get `:app:assembleDebug` green. Fix whatever the first compile turns up. Look at
   `JarCanvas` in a preview or on a device at 0%, 50%, 100% and overflow, and tune the look.
2. **Auth and jars**: add Firebase BoM (Auth, Firestore), Credential Manager and `googleid`
   for Google sign-in, `play-services-code-scanner` and ZXing core for QR. Apply the
   `google-services` plugin. Build sign-in, "my jars", the create-jar wizard (name, currency,
   goal, the three settings) and join by code or QR.
3. **Topics, logging, ledger**: Firestore repositories in `app/.../data`, the log-entry
   transaction, `firestore.rules` with emulator tests, the ledger screen. Replace the demo
   state in `JarHomeViewModel` with the repository; `JarHomeState` is already the right shape.
4. **Disputes, settlements, rounds**: entry detail with dispute and void, recording
   settlements, round history, the reset/keep-filling/close behaviours end to end.
5. Later: push notifications (needs Cloud Functions and the Blaze plan), stats, reminders.

## Firestore data model (agreed, not yet implemented)

```
users/{uid}                 displayName, photoUrl
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

Security rules intent: only members read a jar; only owner/admins edit settings, topics and
roles; entry creation is checked against `settings.loggingPolicy`; entries cannot be deleted.

## Things worth knowing

- **Demo jar is temporary.** `JarHomeViewModel` holds an in-memory jar (escalating prices,
  £20 goal, reset at goal) and the screen shows a banner saying so. Remove both in step 3.
- **Escalation needs a count.** `PricingRules.priceFor` takes `priorOffences`: the offender's
  live entries on that topic inside `Escalation.windowDays`. The demo counts per round
  instead; the real version needs a Firestore query before the transaction.
- **No Cloud Functions means client-trusted writes.** Totals and escalating prices are
  computed on the phone and guarded only by security rules. Acceptable for groups of
  friends; revisit if that changes.
- **Rule decisions the user has not explicitly reviewed**, all in `LoggingRules` /
  `PricingRules` with tests:
  - a disputed entry still counts until an admin voids it
  - an admin cannot resolve a dispute on an entry they logged
  - whoever logged an entry can retract it, except a self-report
  - a single entry is capped at 1,000,000 minor units
  - default escalation: double every 3 offences in 7 days, at most 3 times
- **Toolchain is pinned to late 2025** (see `CLAUDE.md`). Newer AGP 9.x and Kotlin 2.4 exist
  but were not adopted because nothing could be built to check them.
- The domain module was split out of `app` (the plan said one module) so its tests run
  without the Android SDK.
