# Swearjar

An Android app for a shared swear jar: a group agrees on topics, breaking one costs money,
and a jar fills as the group heads towards a goal. The app is a ledger: it tracks who owes
what, and people settle up outside it.

## Layout

- `domain/` — pure Kotlin. Models and the rules engine that interprets each jar's settings
  (who may log, how offences are priced, what happens at the goal). No Android or Firebase.
- `app/` — the Android app: Kotlin, Jetpack Compose, Hilt, Navigation Compose.

## Build

Needs JDK 17+ and the Android SDK (platform 36). Open in Android Studio, or:

```bash
./gradlew :domain:test
```

```bash
./gradlew :app:assembleDebug
```

## Firebase

The app uses the Firebase project `swear-jar-ec309` (Authentication with Google sign-in,
and Firestore, in `europe-west2`). Its config is committed at `app/google-services.json`.

Google sign-in only works for builds signed with a key whose SHA-1 is registered on the
Firebase Android app. Print yours with `./gradlew signingReport`.

Security rules are in `firestore.rules`, with tests that run in the local emulator:

```bash
cd firestore-tests && npm install && npm test
```

## Status

Built: project scaffold, rules engine with tests, the jar that fills towards its goal, a jar screen running on
an in-memory demo jar, Google sign-in, and Firestore rules for user profiles. The app builds;
sign-in has not yet been tried on a device. See `HANDOVER.md`.

![The jar empty, half full, full and overflowing](docs/jar-states.png)

Next: creating and joining jars, then topics, logging and the ledger on Firestore, then
disputes, settlements and rounds.
