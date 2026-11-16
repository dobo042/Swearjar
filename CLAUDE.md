# Swearjar

Android app for a shared swear jar. A group agrees on topics, breaking one costs money, and a
drawn jar fills as the round total approaches a goal. See `HANDOVER.md` for current state and
what to do next.

## Decisions already made (do not reopen without the user asking)

- **Ledger only.** The app records who owes what. It never moves or holds money; people
  settle outside the app and a settlement is recorded.
- **Firebase**: Auth (Google sign-in only) and Firestore. No Cloud Functions in v1, so no
  push notifications in v1.
- **Native Kotlin + Jetpack Compose**, Android only.
- **Joining** is by a 6-character invite code, also shown as a QR code.
- **Three per-jar settings**, chosen by the owner at setup:
  - who may log an offence (`LoggingPolicy`)
  - how an offence is priced (`PricingMode`, with `Escalation`)
  - what happens at the goal (`GoalBehaviour`)
- A user can be in several jars. Each jar has one currency. Roles are owner, admin, member.
- The jar fills by amount **logged** in the current round, not amount paid.

## Firebase

- Project ID: `swear-jar-ec309` (set in `.firebaserc`). Android package: `com.swearjar`.
- Use the Firebase agent skills (plugin `firebase@firebase`, from the `firebase/agent-skills`
  marketplace) for any Firebase task. If they are not loaded in a session, install them:
  `claude plugin marketplace add firebase/agent-skills` then
  `claude plugin install firebase@firebase`.
- Run the CLI as `npx -y firebase-tools@latest ...`, never a bare `firebase`.
- `firestore.rules` follows the `firestore-rules-creation` skill: default deny, a validator
  function used by both create and update, no email or other private data in `users`.
  Every rules change gets a test in `firestore-tests/` and is described to the user as a
  prototype to review, not as proven secure.

## Layout

- `domain/` — pure Kotlin/JVM module. Models (`domain/model`) and the rules engine
  (`domain/rules`). **No Android or Firebase imports here, ever.**
- `app/` — Android app, package `com.swearjar`.
  - `ui/` Compose screens and ViewModels (MVVM, `StateFlow`), `ui/nav` for routes
  - `data/` Firestore/Auth repositories exposing `Flow`s
  - `di/` Hilt modules
- `firebase.json`, `firestore.rules`, `firestore.indexes.json` — Firebase config, deployed
  with the CLI.
- `firestore-tests/` — security rules tests (Node, run in the Firestore emulator).
- `gradle/libs.versions.toml` — every dependency and plugin version lives here.

## Conventions

- Money is `Minor` (a `Long` of pence/cents). Never use floating point for money. Format and
  parse through `Money` in `domain/rules`.
- The three settings are interpreted **only** in `domain/rules` (`LoggingRules`,
  `PricingRules`, `GoalRules`). UI and data code call these; they do not re-implement the
  logic. `firestore.rules`, once written, must mirror `LoggingRules`.
- Entries are never deleted, only voided.
- Any change to a counter (member owed, round total, jar total) happens in the same
  Firestore transaction as the entry that causes it.
- New rule logic gets unit tests in `domain/src/test`.
- User-visible text goes in `app/src/main/res/values/strings.xml`, British English.
- `JarCanvas` is driven purely by its parameters; keep it free of ViewModel or Firebase
  knowledge.

## Commands

```bash
./gradlew :domain:test
```

```bash
./gradlew :app:assembleDebug
```

```bash
cd firestore-tests && npm install && npm test
```

The domain tests run without the Android SDK. Anything under `:app` needs the SDK
(platform 36) via `ANDROID_HOME` or `local.properties`. The user has accepted the Android SDK
licence, so in a cloud session it may be installed with the command-line tools.

There is no emulator in cloud sessions (no KVM). To look at UI, render Compose previews with
the Android CLI (plugin `android-cli@android-cli-plugin`, marketplace
`android/android-cli-plugin`):

```bash
android preview render -o <dir> --fqn com.swearjar.ui.jar.JarCanvasKt.JarHalfPreview '?:app'
```

## Toolchain

Gradle 8.14.3 (wrapper), AGP 8.13.2, Kotlin 2.3.21, KSP 2.3.14, Compose BOM 2025.11.01,
Hilt 2.58, Firebase BoM 35.2.0, google-services 4.6.0, Credentials 1.6.0, compileSdk/targetSdk 36, minSdk 26, JVM target 17. Kotlin must stay at 2.3 or later: the Firebase and Google ID
libraries ship Kotlin 2.4 metadata, which older compilers cannot read. This set builds; it is
not the newest. Upgrade as one deliberate step, with a
working build before and after.

## Do not commit

`local.properties`, keystores. Both are in `.gitignore`. `app/google-services.json` **is**
committed, at the user's request; it holds project identifiers, not secrets.
