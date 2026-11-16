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

## Status

Built: project scaffold, rules engine with tests, the animated jar, and a jar screen running
on an in-memory demo jar.

Next: Google sign-in and creating/joining jars (needs a Firebase project and its
`app/google-services.json`), then topics, logging and the ledger on Firestore, then
disputes, settlements and rounds.
