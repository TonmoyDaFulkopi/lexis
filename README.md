# Lexi

An Android app for collecting new words the moment you find them.

Copy a word in any app, pull down your quick settings and tap **Save word**. Lexi saves it
instantly and fetches the meaning, pronunciation and examples in the background.

## Features

- **Quick Settings tile** opens a small sheet over whatever you're reading, pre-filled from the clipboard
- **Automatic definitions** from [dictionaryapi.dev](https://dictionaryapi.dev), with
  [Datamuse](https://www.datamuse.com/api/) filling in missing pronunciations and entries;
  lookups retry in the background until they succeed
- **Word list** with search, filters, swipe to delete and per-word or resync-all refresh
- **Word detail** with text-to-speech, all senses, synonyms and a personal note
- **Flashcards** with flip cards and learned tracking
- **Home screen widget** showing a random saved word
- **Animated launch**: the Spine logo ("ləxy" on a bookmark) on every start, plus a how-it-works intro on first launch

## Tech

Kotlin, Jetpack Compose (Material 3), Room, WorkManager, Glance, kotlinx.serialization.
Min SDK 26, target SDK 35.

## Build

Requires JDK 17 and the Android SDK (platform 35). Create `local.properties` with your SDK path:

```
sdk.dir=/path/to/Android/sdk
```

Then:

```
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`. The release build is
signed with the debug key so it installs directly; use your own keystore before publishing.
