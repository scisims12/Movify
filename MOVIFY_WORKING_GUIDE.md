# Movify – Complete Application Working Guide

Movify is a native Android application for browsing movies, series, and anime. It provides a rich user interface, tracks watch progress, supports downloads, and plays video using Android's native player.

**Crucial Concepts:**
- **Movify does NOT host any videos.**
- **TMDB (The Movie Database)** is used for all title information, descriptions, and artwork.
- **Streaming sources** come from "extensions" (a JSON catalog of source providers).
- Some internal parts of the code might still use the name `OpenStream` (the upstream project it was built on) for technical reasons.
- Do not remove the required upstream attribution or license information.

---

## 1. SIMPLE APP OVERVIEW

When you use Movify, the journey looks like this:

```text
[Movify Starts]
       ↓
[Home Screen Loads] (Asks TMDB for trending movies/series)
       ↓
[User Taps a Title]
       ↓
[Details Screen Opens] (Shows episodes, cast, trailers from TMDB)
       ↓
[User Taps Play on an Episode]
       ↓
[Extensions Search Streams] (App checks installed JSON providers)
       ↓
[Best Stream Selected] (App picks the fastest/highest quality link)
       ↓
[Media3 Player Starts] (Video plays on screen)
       ↓
[Progress Saved] (App remembers where you stopped)
```

---

## 2. PROJECT FOLDER MAP

The code lives mainly in `app/src/main/java/com/ivor/movify/`. Here is what the folders do:

- **`di/`**
  Purpose: "Dependency Injection" (Hilt). This tells the app how to build and provide pieces like the database or internet tools to the rest of the app.
- **`ui/`**
  Purpose: The visual theme (colors, shapes, typography).
- **`data/`**
  Purpose: The engine room. It handles fetching data from the internet (TMDB, Github), saving to the local database, streaming extraction, extensions, and downloads.
- **`domain/`**
  Purpose: The blueprints. Contains data models (like `VideoServer`, `WatchProgress`) and rules (interfaces) the app must follow.
- **`presentation/`**
  Purpose: The user interface (what you see). Every screen has its own folder here (e.g., `home`, `player`, `search`).
  _You would edit this when:_ You want to change a button, text, layout, or screen behavior.
- **`extensions/` (in the project root)**
  Purpose: Holds the `index.json` catalog of official streaming sources.

---

## 3. APPLICATION STARTUP

When a user taps the Movify icon:
1. **`MovifyApp.kt`** starts first. It sets up crash reporting (diagnostics) and DNS settings.
2. **Hilt** (the dependency manager) automatically starts creating the database and internet connections in the background.
3. **`MainActivity.kt`** launches. This is the only "Activity" (window) in the app.
4. **Theme and Navigation:** `MainActivity` applies the app's theme and hands control over to Jetpack Compose Navigation (`AppNavigation.kt`), which loads the Home screen.

---

## 4. NAVIGATION

The app has several main screens, controlled by `presentation/navigation/AppNavigation.kt`:

- **Home:** The main feed of trending and popular titles.
- **Search:** Search for movies and series.
- **Saved:** Custom user lists and "Watch Later".
- **Downloads:** Manage offline downloaded episodes.
- **History:** Titles the user has watched.
- **Details:** The information page for a specific movie or series.
- **Player:** The video player screen.
- **Settings:** App preferences.
- **Marketplace:** Where users manage streaming extensions.
- **Update:** The screen that checks for and installs app updates.

Navigation simply tells the app to swap out the current screen's UI for the next one, passing along IDs (like the movie ID) so the new screen knows what to load.

---

## 5. HOME SCREEN / CATALOG

**Where does the data come from?**
The Home screen (`HomeScreen.kt`) gets its data from `HomeViewModel.kt`, which asks the `AnimeRepositoryImpl`.
In this app, the word "catalog" in the Home context means lists of titles (like "Trending" or "Popular"). These come entirely from the **TMDB API**, not from a bundled file.

**What happens on failure?**
If the app cannot connect to TMDB and has no previously saved local cache, the screen shows: `"Couldn't reach the catalog"`.
This usually means:
1. The user has no internet connection.
2. The TMDB API key is missing or invalid.
3. The user's internet provider is blocking TMDB.

---

## 6. TMDB / METADATA

TMDB (The Movie Database) provides all the names, descriptions, episode lists, and poster images.
**TMDB does NOT provide video streams.**

- **API Key:** The app requires a TMDB API key to work. It is read from `local.properties` during the app build.
- **DEMO_KEY:** If no key is provided, the app falls back to using `DEMO_KEY`, which is heavily rate-limited and will often fail.
- **How it's used:** `data/remote/TmdbApi.kt` uses Retrofit (an internet library) to talk to TMDB.

---

## 7. EXTENSION SYSTEM

**What is an extension?**
An extension is a set of instructions (in JSON format) that tells Movify where and how to find video streams for a specific movie or episode.

- **The Catalog:** The app reads a file called `index.json`. A copy is bundled inside the app (`assets/extensions/official-repo.json`), and the app also checks the internet for updates to this file.
- **Installed Extensions:** Users can install or disable sources in the Marketplace screen.
- **How it works:**
  User presses Play → The app looks at installed extensions (`StreamingRepositoryImpl.kt`) → It runs them all at the same time to search for the video → The extensions return links → The app's `ServerRanker` picks the best one → The video plays.

---

## 8. STREAMING FLOW

When the user taps an episode to play:
1. `PlayerViewModel` requests a stream.
2. `StreamingRepositoryImpl` asks the `ExtensionProviderRegistry` for all active providers.
3. Providers use different "engines" (like `vidking-direct`, `web-embed`, `anikoto`).
4. If a source uses a web embed (a video player on a website), the app uses a hidden web browser (`WebEmbedResolver`) to secretly load the page and extract the raw video file link.
5. The links are ranked.
6. The winning URL is passed to the video player.

*Note: You may see names like `vidking` or `OpenStream` in this code (`data/streaming/`). These are technical upstream identifiers for the stream engines and should generally not be changed.*

---

## 9. VIDEO PLAYER

The app uses **Media3 / ExoPlayer**, which is Google's official Android video player.

- **Files:** `presentation/player/PlayerScreen.kt` (the UI) and `PlayerViewModel.kt` (the logic).
- **Quality & Audio:** If the stream provides multiple qualities (e.g., 1080p, 4K) or different audio tracks (dubs), Media3 handles switching them.
- **Subtitles:** `SubtitleFetcher.kt` grabs subtitles either directly from the video stream, or searches OpenSubtitles and SubSource for matching text files.
- **Picture-in-Picture (PiP):** Handled in `PictureInPicture.kt`. If the user leaves the app while playing, it shrinks into a small floating window.

---

## 10. DOWNLOAD SYSTEM

Users can download videos to watch offline.

- **Streaming vs. Downloading:** Streaming plays the video immediately from the internet. Downloading saves the whole video file to the phone's storage first.
- **How it works:** Handled by `DownloadRepositoryImpl.kt` and `HlsDownloadService.kt`.
- **Background Service:** Because downloads take time, Android uses a background "Service" (`HlsDownloadService`) to keep the download running even if the user closes the app.

---

## 11. SEARCH

- **UI:** `SearchScreen.kt` and `SearchViewModel.kt`.
- **Where results come from:** When the user types, the app sends the text to TMDB.
- **Filters/Genres:** Users can tap genre buttons to see only specific types of content.

---

## 12. SAVED / HISTORY / CONTINUE WATCHING

- **History:** Automatically tracks what the user taps on.
- **Continue Watching:** `WatchProgressRepositoryImpl.kt` saves exactly how many minutes/seconds the user has watched of a video.
- **Saved:** Users can create custom lists (like a favorites list).
- **Storage:** All of this is saved on the user's phone in a local database called **Room**.

---

## 13. LOCAL DATABASE AND STORAGE

Movify saves user data strictly on the phone.

- **Room (`AppDatabase.kt`):** A robust local database. It stores:
  - Watch progress
  - Custom lists (Saved)
  - Download records
  - User profiles
- **SharedPreferences (`AppSettingsStore.kt`):** A simpler storage for basic toggles. It stores:
  - Theme choices (Dark/Light)
  - DNS settings
  - Download over Wi-Fi only toggle

---

## 14. SETTINGS SYSTEM

Settings allow the user to tweak app behavior.

- **UI:** `SettingsScreen.kt` and `SettingsViewModel.kt`.
- **Implementation:** When a user taps a setting (like changing the DNS), the `SettingsViewModel` saves the new choice into `AppSettingsStore`. The rest of the app immediately notices the change and updates.

---

## 15. BACKUP / RESTORE

- **File:** `data/backup/LibraryBackup.kt`
- **What it does:** Allows the user to export their Watch History, Progress, and Custom Lists into a single JSON file. They can later restore this file if they reinstall the app or get a new phone.

---

## 16. UPDATE SYSTEM

- **File:** `presentation/update/UpdateScreen.kt` and `UpdateViewModel.kt`.
- **How it works:** The app asks the GitHub API for the latest release. If the version on GitHub is higher than the installed version, it offers to download the new APK.
- **Important Note:** The updater currently points to `repos/ivorisnoob/openstream/releases/latest`. If this app is moved to a different GitHub repository, you will need to change this URL in `GithubApi.kt`.

---

## 17. ANDROID MANIFEST

`AndroidManifest.xml` is the master rulebook for the Android system. It tells the phone:
- **Permissions:** The app needs `INTERNET` to stream, and `POST_NOTIFICATIONS` for downloads.
- **Activities:** `MainActivity` is registered here.
- **Services:** `HlsDownloadService` is registered so downloads can run in the background.
- **Deep Links:** It tells Android that if the user clicks a `themoviedb.org` link in another app, Movify can open it.

---

## 18. BUILD.GRADLE.KTS

The `app/build.gradle.kts` file tells Android Studio how to build the app.

- **namespace & applicationId:** `com.ivor.movify`. This is the app's unique fingerprint on the phone and Play Store.
- **minSdk 26:** The app requires Android 8.0 or newer.
- **targetSdk 36:** The app is optimized for Android 15.
- **versionCode & versionName:** Numbers used to determine if an update is available.
- **dependencies:** A list of third-party tools the app uses.

---

## 19. API KEYS / SECRETS

- **TMDB_API_KEY:** Required to load posters, titles, and episode lists. Must be placed in a file named `local.properties` on your computer before building the app.
- **Keystore:** Used to digitally sign the app for official release.

*Never share your `local.properties` file or put your API keys directly into public code.*

---

## 20. NETWORKING

How the app talks to the internet:
- **Retrofit & OkHttp:** The libraries used to make internet requests.
- **JSON Parsing:** The app receives data as text (JSON) and uses `kotlinx.serialization` to turn that text into usable Kotlin objects (like turning a block of text into an `AnimeDto` object).
- **Flow:** Screen requests data → Repository uses Retrofit → Internet → TMDB replies → Repository gives data to Screen.

---

## 21. ARCHITECTURE FOR NON-CODERS

Movify is built like a restaurant:

- **UI / Screen (The Dining Area):** What the user sees (e.g., `HomeScreen.kt`).
- **ViewModel (The Waiter):** Takes orders from the Screen and fetches what is needed (e.g., `HomeViewModel.kt`).
- **Repository (The Kitchen Manager):** Decides where to get the food—either from the local Database or the API (e.g., `AnimeRepositoryImpl.kt`).
- **API (The Delivery Truck):** Gets fresh data from the internet (e.g., `TmdbApi.kt`).
- **Database (The Pantry):** Saves data locally on the phone (e.g., `AppDatabase.kt`).

---

## 22. DEPENDENCY INJECTION / HILT

Hilt automatically provides objects where the app needs them. Instead of every screen manually building a database connection and an internet connection, Hilt creates them once when the app starts and hands them to the ViewModels that ask for them.

---

## 23. IMPORTANT THIRD-PARTY LIBRARIES

| Library | Purpose | Where used |
| --- | --- | --- |
| **Jetpack Compose** | Builds the visual user interface using code instead of XML files. | All UI screens |
| **Material 3** | Google's design system (buttons, colors, styling). | All UI screens |
| **Hilt** | Wires the app together (Dependency Injection). | Everywhere |
| **Retrofit / OkHttp** | Handles fetching data from the internet. | TMDB, Github API |
| **Room** | Saves data locally on the phone. | History, Progress |
| **Media3 (ExoPlayer)**| The engine that actually plays the videos. | Video Player |
| **Coil** | Loads and displays images (like movie posters) from the internet. | UI screens |

---

## 24. MOVIFY VS OPENSTREAM

Movify is built on the open-source project OpenStream.
- **User-Facing Branding:** The app name, icon, and colors belong to Movify (`com.ivor.movify`).
- **Technical Logic:** You will see names like `OpenStream` or `vidking` in the code (like the GitHub updater URL or extension engine names). These are technical references and should usually be left alone so you don't break compatibility with the source extensions.
- **Credits:** OpenStream attribution remains in the `LICENSE` and `README.md`.

---

## 25. LICENSE AND CREDITS

Movify uses the **MIT License**.
- You are free to use, modify, and distribute the code.
- You must keep the original copyright notice in the code (`Copyright (c) 2026 Ivor`).
- TMDB and OpenSubtitles must be credited for their data.

---

## 26. HOW TO MODIFY MOVIFY SAFELY

| If I want to change... | Where should I look? |
| --- | --- |
| **App Name** | `app/src/main/res/values/strings.xml` |
| **App Icon** | `app/src/main/res/mipmap/` folders |
| **Theme / Colors** | `app/src/main/java/com/ivor/movify/ui/theme/Color.kt` |
| **Home Screen Layout** | `presentation/home/HomeScreen.kt` |
| **Player UI** | `presentation/player/PlayerScreen.kt` |
| **Update URL** | `data/remote/GithubApi.kt` |
| **Version Number** | `app/build.gradle.kts` (`versionName` and `versionCode`) |

---

## 27. THINGS I SHOULD NOT RANDOMLY CHANGE

Unless you know exactly what you are doing, do not change:
- **`applicationId` (`com.ivor.movify`):** Changing this makes the phone think it's a completely different app. Updates will fail, and user data will be lost.
- **Room Database Entities (`data/local/entity/`):** Changing variables here will crash the app for existing users unless a proper database migration is written.
- **Extension JSON formats (`index.json`):** Breaking this format will cause all video streams to stop working.

---

## 28. HOW TO BUILD THE APP

1. Open the folder in Android Studio.
2. Wait for the "Gradle Sync" to finish (a loading bar at the bottom).
3. Create a file named `local.properties` in the project root if it doesn't exist.
4. Add the line: `TMDB_API_KEY=your_actual_key_here`
5. Plug in an Android phone or start an Emulator.
6. Click the Green "Play" (Run) button at the top of Android Studio.

**Useful Terminal Commands:**
- `./gradlew assembleDebug` (Builds a test APK)
- `./gradlew assembleRelease` (Builds the final APK for users, requires signing keys)

---

## 29. COMMON PROBLEMS

**Problem:** "Couldn't reach the catalog" on Home screen.
**Cause:** TMDB API is failing. Check your internet or ensure your `TMDB_API_KEY` is valid.

**Problem:** Search works, but no streams are found when trying to play.
**Cause:** The extensions are failing, the websites they scrape are down, or your internet provider is blocking the video sources.

**Problem:** App builds, but images don't load.
**Cause:** `DEMO_KEY` is being used and it is rate-limited. Get a real TMDB API key.

---

## 30. FULL END-TO-END EXAMPLE

**User wants to watch Episode 1 of a Series:**
1. **Tap app icon:** `MainActivity` opens, starts `AppNavigation`.
2. **Home Screen:** `HomeViewModel` asks `AnimeRepositoryImpl` to ask TMDB for trending shows. TMDB replies. Posters are shown.
3. **Tap Show:** Navigation moves to `DetailsScreen`, passing the TMDB ID.
4. **Details Screen:** `DetailsViewModel` asks TMDB for seasons/episodes.
5. **Tap Episode 1:** Navigation moves to `PlayerScreen`.
6. **Find Stream:** `StreamingRepositoryImpl` asks the catalog for sources. It secretly runs a web browser if needed to extract the raw `.m3u8` video link.
7. **Play:** Media3 gets the link and plays the video.
8. **Save Progress:** As it plays, `WatchProgressRepositoryImpl` saves the time into the Room database.

---

## 31. GLOSSARY

- **API:** A bridge that lets the app talk to a website (like TMDB) to get data.
- **APK:** The actual app installation file you put on an Android phone.
- **Gradle:** The robot that builds the app code into an APK.
- **Kotlin:** The programming language the app is written in.
- **Compose:** The modern way Android apps draw buttons, text, and images on the screen.
- **JSON:** A simple text format used to send data over the internet.
- **HLS / .m3u8:** A type of streaming video format that adapts quality based on internet speed.

---

## 32. QUICK REFERENCE (Movify Cheat Sheet)

- **App name:** Movify
- **Package / Namespace:** `com.ivor.movify`
- **Application ID:** `com.ivor.movify`
- **Main Activity:** `MainActivity.kt`
- **Application class:** `MovifyApp.kt`
- **Home screen:** `presentation/home/HomeScreen.kt`
- **Search:** `presentation/search/SearchScreen.kt`
- **Player:** `presentation/player/PlayerScreen.kt`
- **Streaming:** `data/streaming/StreamingRepositoryImpl.kt`
- **Extensions:** `data/extensions/`
- **Downloads:** `data/repository/DownloadRepositoryImpl.kt`
- **Database:** `data/local/AppDatabase.kt`
- **Settings:** `presentation/settings/SettingsScreen.kt`
- **Updater:** `presentation/update/UpdateScreen.kt`
- **Manifest:** `app/src/main/AndroidManifest.xml`
- **Gradle file:** `app/build.gradle.kts`
