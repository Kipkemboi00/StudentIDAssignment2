# StudentIDAssig
nment2

A native Kotlin Android application for NIT3213 Assignment 2. Sign in with your
class credentials, browse your assigned collection, and open an item for its full
details. The Android Studio/Gradle project name is exactly `StudentIDAssignment2`.

## Requirements and setup

- Android Studio with Android Gradle Plugin 8.5 support (Koala or newer).
- **JDK 17** selected under Settings → Build, Execution, Deployment → Build Tools
  → Gradle → Gradle JDK. For command-line builds, set `JAVA_HOME` to your JDK 17
  installation. No machine-specific JDK path is committed.
- Android SDK Platform **34** and Build Tools **34.0.0**, installed using SDK Manager.
- An emulator or Android device running Android 8.0 / API 26 or later.
- Internet access for the first Gradle sync and for the running application's API.

Open this repository's root folder in Android Studio and allow Gradle to sync.
Android Studio normally creates `local.properties` with the SDK directory. If
building outside the IDE, set `ANDROID_HOME` to your SDK directory or create an
ignored `local.properties` containing `sdk.dir=/path/to/your/Android/Sdk` (use
forward slashes on Windows). Select the `app` run configuration and a device,
then press Run.

The checked-in Gradle wrapper uses Gradle 8.7. Kotlin is 1.9.24; the app compiles
and targets API 34. You do not need a global Gradle installation, API key, or
additional service account.

## Login and class location

Both login fields start **blank**. Enter your own:

- **Student ID:** digits only, without the `s` prefix. Surrounding whitespace is
  removed; prefixed or nonnumeric IDs are rejected before any request.
- **Password:** your first name, with the exact capitalization required by your
  class. It is case sensitive and is sent as entered.
- **Class location:** choose Footscray, Sydney, or BR using the login selector.

Neither assignment reference identifies the student's actual class location.
Consequently the location selector starts with **Select your class location**;
no campus is silently assumed. The single configuration file is:

`app/src/main/java/au/edu/vu/studentidassignment2/config/AppConfig.kt`

Leave `defaultLocation = null` to require a choice, or set it to
`ClassLocation.FOOTSCRAY`, `ClassLocation.SYDNEY`, or `ClassLocation.BR` once your
actual location is known. The selector remains available to correct the choice.
The corresponding API paths are `/footscray/auth`, `/sydney/auth`, and `/br/auth`.
The base URL is configured in the same file:
`https://nit3213apinew.onrender.com/`.

## Application behavior

1. **Login:** validates input and POSTs a JSON object containing `username` and
   `password` to the selected class endpoint. Separate progress messages cover
   authentication and loading the collection. The returned `keypass` is used in
   `GET /dashboard/{keypass}` before navigating. Errors distinguish rejected
   credentials, connection problems, server failures, and unexpected responses.
   If the dashboard request fails after authentication, the login screen explains
   the failure and allows another sign-in attempt.
2. **Dashboard:** a RecyclerView displays entity summaries excluding the
   `description` field. It shows the number returned and the API's `entityTotal`.
   Refresh is in the toolbar; empty results and request failures include a retry
   action. The overflow menu provides Sign out. Tapping an item opens Details.
3. **Details:** shows every returned field, including description. Field labels
   are made readable; null/blank values use a placeholder, nested objects and
   arrays use formatted JSON, and a missing description is explained. The screen
   scrolls and supports text selection. System Back or the toolbar arrow returns
   to the collection.

No personal credentials are embedded in the application or configuration. Inputs
are not persisted or logged, network bodies are not logged, and application
backup is disabled. A password is cleared from the form on successful sign-in.
The non-credential keypass is retained through Android's saved-state mechanism
so the dashboard can reload after process recreation. Sign out clears it.
Closing and relaunching the app normally begins at Login.

## Architecture and dependency injection

The single `app` module uses MVVM with XML layouts and View Binding:

```text
config/          API URL and explicit class-location configuration
domain/          Entity, field, dashboard and typed failure models
data/            Retrofit API, repository interface/implementation, JSON parser
di/              Koin module for network, repository, parser and ViewModels
ui/login/        Login Fragment and ViewModel
ui/dashboard/    Dashboard Fragment, ViewModel and RecyclerView ListAdapter
ui/details/      Details Fragment and ViewModel
```

Koin starts in `AssignmentApplication`. It constructs the singleton OkHttp client,
Retrofit service, parser and repository, then injects the repository and Android
`SavedStateHandle` into the relevant ViewModels. Fragments obtain ViewModels
through Koin; MainActivity and Dashboard share an activity-scoped dashboard
ViewModel. Tests replace external behavior with fakes or a local mock server.

ViewModels expose immutable `StateFlow` interfaces and use `viewModelScope` for
requests. Fragments collect only while their **view** lifecycle is STARTED and
release View Binding/RecyclerView references when their views are destroyed.
Requests survive rotation, duplicate submissions are suppressed, cancellation
propagates, and error states can be retried. The dashboard keypass is restored
through `SavedStateHandle`; a selected entity's JSON travels in Fragment arguments
and is restored by the Details ViewModel. Large collections are not bundled or
persisted: only the selected item is passed to Details.

The parser preserves each entity's individual fields rather than imposing a
fixed topic schema. It validates the dashboard response and rejects malformed
responses instead of disguising them as an empty list.

## Build and test

From the repository root in Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

Or run all checks together:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On macOS/Linux use `sh ./gradlew` in place of `.\gradlew.bat`.
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
Unit-test reports are under `app/build/reports/tests/testDebugUnitTest/` and lint
reports under `app/build/reports/`.

With a running emulator or connected device, also run the launch regression tests:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

These device tests check real login-screen startup, blank fields, activity
recreation, and location validation without making any API requests. Their report
is under `app/build/reports/androidTests/connected/debug/`.

Tests cover login validation, loading/authentication/dashboard transitions,
failure and retry behavior, duplicate request suppression, empty dashboards,
sign-out cancellation, restored sessions, Details restoration, heterogeneous
fields, malformed payloads, HTTP methods/paths/JSON bodies for every campus,
error classification, cancellation, and construction of the production Koin
dependency graph. Coroutines use a controlled test dispatcher. All HTTP contract
tests use **MockWebServer on localhost**, never the live assignment API.

For a manual end-to-end check, run the app, select your actual class location,
enter your own credentials, inspect summaries and details, refresh, rotate the
device, navigate back, and sign out. Check the error state with networking disabled.
Successful live authentication requires valid class credentials; automated tests
do not establish live API availability or acceptance of those credentials.

## Version control and submission

The original Git history is retained. Source, tests, build scripts, Gradle wrapper
and this README belong in the submitted repository. Machine-specific IDE files,
SDK settings, build outputs and the two root-level assignment references are
ignored. The DOCX and rubric PDF stay on the local computer and are not part of
the application repository. Submit the repository URL after pushing the reviewed
commits to your own Git remote.
