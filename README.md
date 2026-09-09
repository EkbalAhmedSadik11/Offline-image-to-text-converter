# Image to Text — Offline Android OCR

A native Android app that converts a photo or gallery image into editable
text, entirely on the device. Once installed, it never needs Wi-Fi, mobile
data, or a server — you can turn on Airplane Mode and it still works.

## What the app does

1. You pick an image (gallery) or take a photo (camera).
2. You can optionally clean it up (grayscale, brightness, contrast, rotate,
   crop).
3. Tap **Extract Text** — the on-device OCR engine reads the image and
   produces editable text, automatically handling whichever of the three
   supported languages appear in it, including mixed text.
4. You edit, copy, save as a `.txt` file, or share the result. Every result
   is also kept in a local **History** list.

## Supported languages

- **Bangla / Bengali** (`ben`)
- **English** (`eng`)
- **Arabic** (`ara`)

There is no language picker anywhere in the app. Every image is recognized
with all three languages loaded at once (`eng+ben+ara`), so English,
Bangla, Arabic, and any mixture of the three are handled by the same single
pass. After recognition, the app scans the resulting text's Unicode ranges
to show a "Detected: ..." label — this is just a label, not a second OCR
pass, and it is not claimed to be 100% accurate. Neither is OCR in general;
recognition quality depends on image quality, font, and handwriting is not
supported (only printed/typed text).

## How the offline OCR works

The app uses **[Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android)**,
an Android wrapper around the open-source **Tesseract OCR engine** (the
same engine used by many offline OCR tools). It compiles Tesseract's C++
code straight into native libraries (`.so` files) that ship inside the
APK, plus three small language model files (`ben.traineddata`,
`eng.traineddata`, `ara.traineddata`) that ship inside the APK's assets.

The first time the app runs, it copies those three files from its own
assets into its private app storage (Tesseract can only read language data
from a real file path, not from inside the APK — this copy is a pure
file-to-file operation with no network access at all). Every OCR run after
that reads the already-copied files straight from disk.

Nothing in this flow touches the network. There is no `INTERNET`
permission in the app's manifest at all, so the app is not even *able* to
make a network request even if some code tried to.

## Why this technology

| Requirement | How it's satisfied |
|---|---|
| Reliable offline OCR | Tesseract is a mature, widely used OCR engine with a real Android wrapper (no experimental WASM/JS OCR needed) |
| Bangla + English + Arabic in one pass | Tesseract supports loading multiple `.traineddata` language files together (`eng+ben+ara`) |
| Small APK size | The "fast" trained-data variants are used (~1-4 MB each) instead of the much larger "best" accuracy models |
| Beginner-friendly | Kotlin + Jetpack Compose (Google's current recommended, simplest UI toolkit) with a small, flat project structure and no advanced architecture (no DI framework, no multi-module setup) |
| No servers/API keys | Everything is a local library + local files; there is nothing to configure |

## Technology used

- **Kotlin** + **Jetpack Compose** (Material 3) — UI and app logic
- **Tesseract4Android** — the offline OCR engine (native Tesseract + Leptonica)
- **Room** — local SQLite database for OCR history
- **DataStore** — stores the light/dark theme preference locally
- **CameraX-free camera capture** via the system camera app (`ActivityResultContracts.TakePicture`) — simpler and smaller than bundling a custom camera
- **Android Photo Picker** (`ActivityResultContracts.PickVisualMedia`) for gallery selection — needs **no storage permission at all**
- **AndroidX ExifInterface** — corrects photo rotation automatically

## Project structure

```
image-to-text-converter/
├── app/
│   ├── build.gradle.kts              # App module build config & dependencies
│   ├── proguard-rules.pro            # Keep-rules for the OCR native bridge
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/tessdata/          # <- put ben/eng/ara.traineddata here (see below)
│       ├── java/com/imagetotext/converter/
│       │   ├── ImageToTextApp.kt             # Application class (kicks off tessdata copy)
│       │   ├── MainActivity.kt               # Single Activity, hosts Compose UI
│       │   ├── ocr/
│       │   │   ├── TesseractHelper.kt        # Wraps Tesseract4Android
│       │   │   └── LanguageDetector.kt       # Post-OCR "Detected: ..." label logic
│       │   ├── data/
│       │   │   ├── history/                  # Room database (OCR History)
│       │   │   └── settings/                 # DataStore (theme preference)
│       │   ├── util/
│       │   │   ├── ImageUtils.kt             # Decode/resize/rotate/adjust/crop bitmaps
│       │   │   ├── TextStats.kt              # Character/word counting
│       │   │   └── FileSaver.kt              # Save .txt / build the share Intent
│       │   ├── viewmodel/
│       │   │   └── OcrViewModel.kt           # All app state and actions
│       │   └── ui/
│       │       ├── theme/                    # Compose Material 3 theme (light/dark)
│       │       ├── navigation/                # Nav graph (Home/Preview/Result/History)
│       │       └── screens/                   # HomeScreen, PreviewScreen, ResultScreen, HistoryScreen
│       └── res/                       # Strings, adaptive icon, FileProvider paths, themes
├── build.gradle.kts                   # Root Gradle config
├── settings.gradle.kts                # Module list + JitPack repository (needed for the OCR library)
└── gradle.properties
```

## Required software

Install these once on your computer:

1. **[Android Studio](https://developer.android.com/studio)** (latest stable version). It bundles the Android SDK, a Gradle installation, and everything else needed — you do not need to install Gradle, the Android SDK, or Java separately.
2. An internet connection **only for the one-time setup below** (downloading the project's Gradle dependencies and the OCR language files). Once built, the installed app itself needs no internet.

## One-time setup

### 1. Download the OCR language files

The three Tesseract language model files are official open-source files
from the Tesseract project. They're binary files several megabytes each,
so they aren't included as source text — download them once:

**Option A — PowerShell (Windows), run from the project's root folder:**

```powershell
$dest = "app/src/main/assets/tessdata"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
$base = "https://github.com/tesseract-ocr/tessdata_fast/raw/main"
Invoke-WebRequest "$base/eng.traineddata" -OutFile "$dest/eng.traineddata"
Invoke-WebRequest "$base/ben.traineddata" -OutFile "$dest/ben.traineddata"
Invoke-WebRequest "$base/ara.traineddata" -OutFile "$dest/ara.traineddata"
```

**Option B — manually:** download each file below in your browser and save
it into `app/src/main/assets/tessdata/` (keep the exact file names):

- <https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata>
- <https://github.com/tesseract-ocr/tessdata_fast/raw/main/ben.traineddata>
- <https://github.com/tesseract-ocr/tessdata_fast/raw/main/ara.traineddata>

When done, `app/src/main/assets/tessdata/` should contain exactly:
`ben.traineddata`, `eng.traineddata`, `ara.traineddata` (the placeholder
`PUT_TRAINEDDATA_FILES_HERE.txt` file can stay or be deleted, it isn't
used by the app).

> This download happens once, on your development computer, while you're
> building the app. The **installed app on the phone never downloads
> anything** — these files get packaged inside the APK.

### 2. Open the project

1. Open Android Studio → **Open** → select the `image-to-text-converter` project folder (the one containing `settings.gradle.kts`).
2. Android Studio will automatically start a **Gradle sync**, downloading the project's dependencies (including the OCR library from JitPack). This needs internet and can take a few minutes the first time.
3. Wait for the status bar to show "Gradle sync finished" with no errors.

## Building the APK

### Debug APK (fastest way to test on your phone)

**In Android Studio:** click the green **Run ▶** button with a device/emulator selected, or use the menu **Build → Build APK(s)**.

**From the command line**, inside the project folder:

```bash
./gradlew assembleDebug
```

(On Windows PowerShell, use `.\gradlew.bat assembleDebug` if you have a
`gradlew.bat`; otherwise open the project in Android Studio at least once
first so it generates the Gradle wrapper, or run `gradle assembleDebug`
with a system-installed Gradle 8.7+.)

The debug APK is created at:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK (smaller, optimized, for real installation)

**Easiest way — Android Studio wizard:**

1. Menu **Build → Generate Signed Bundle / APK…**
2. Choose **APK**, click **Next**.
3. Click **Create new…** to create a new signing key (a one-time step —
   fill in a password and your name; keep the generated `.jks` keystore
   file somewhere safe, you'll reuse it for future updates).
4. Choose the **release** build variant, click **Finish**.

The release APK is created at:

```
app/release/app-release.apk
```

(the exact sub-path is shown in the "APK(s) generated" notification —
click **locate** to open the folder).

**From the command line** (only if you've already configured a signing
key in `app/build.gradle.kts` — the Android Studio wizard above is
simpler for a first build):

```bash
./gradlew assembleRelease
```

## Installing the APK on your phone

1. Copy the APK file to your phone (USB cable, or any file-transfer app — this is a local file transfer, not an upload to the internet).
2. On your phone, open the APK file with a file manager and tap **Install**. If it's the first time installing an app from outside the Play Store, Android will ask you to allow installs from that source — allow it just for this file.
3. Open the **Image to Text** app from your app drawer.

## Testing it fully offline

1. Install the APK as above.
2. Turn on **Airplane Mode** on the phone (and make sure Wi-Fi is off too, since airplane mode toggles can leave Wi-Fi on).
3. Open the app.
4. Tap **Select Image**, pick a photo containing Bangla text → **Extract Text**.
5. Repeat with an English image, an Arabic image, and an image containing a mix of the three.
6. Try **Copy**, **Save**, edit the text, **Share**, and check **History**.

Everything above should work with no network connection at all — if it
doesn't, something was set up incorrectly (most likely the language files
from step 1 above weren't downloaded before building).

## Known limitations

- OCR accuracy depends heavily on image quality (lighting, focus, skew) and font; this app does not and cannot claim 100% accuracy for any language.
- Only **printed/typed text** is supported — handwriting recognition is a very different, much harder problem that Tesseract does not handle well.
- The "Detected: ..." language label is a simple heuristic based on which Unicode ranges appear in the recognized text, not a certified language-identification model.
- The in-app camera flow launches the phone's own default camera app (via a standard Android intent) rather than a custom in-app camera preview — this keeps the app small and simple, and every stock camera app already provides its own retake/confirm step before handing the photo back.
- Very large images (e.g. 48MP photos) are automatically downscaled before processing to avoid out-of-memory crashes; this is expected behavior, not a bug.
- Saving on Android 9 and below writes to the app's own private folder instead of the shared Downloads folder (a scoped-storage limitation of very old Android versions); Android 10+ saves to the regular Downloads folder.
