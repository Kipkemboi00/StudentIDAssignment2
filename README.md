# StudentIDAssignment2

This is my Android application for NIT3213 Assignment 2. I used Kotlin to build
three screens: Login, Dashboard and Details. The project is called
`StudentIDAssignment2` in Android Studio, and the installed app is called
**Collections**.

The app connects to the assignment API to sign in and display the collection
assigned to the user. I used forest green, ivory and lime colours, with cards for
the collection and a separate page for each item's full details.

## What the app does

- **Login:** checks the required inputs, signs in using the selected class
  location, and uses the returned `keypass` to load the dashboard. The form shows
  progress and errors, and a pending request can be cancelled.
- **Dashboard:** shows the returned entities in a RecyclerView. Each card gives a
  summary without the description. The screen includes the item count, refresh,
  sign out, and messages for empty results or a failed request.
- **Details:** opens the selected item and displays all its fields, including its
  description. Missing values have a placeholder, and nested data is formatted
  so it can still be read.

The API can return different fields for different topics, so I kept the entity
model flexible rather than building the screens around one fixed set of fields.

## Opening and running the project

You will need:

- Android Studio with support for Android Gradle Plugin 8.5.2.
- JDK 17 and above.
- Android SDK Platform 34 and Build Tools 34.0.0.
- An emulator or phone running Android 8.0 (API 26) or later.
- Internet access to download dependencies and use the API.

The project uses Kotlin 1.9.24 and Gradle 8.7. The Gradle wrapper is included.

1. Open the repository's root folder in Android Studio, where
   `settings.gradle.kts` is located.
2. Allow the Gradle sync to finish. Install any missing SDK components when
   Android Studio prompts for them.
3. In **Settings > Build, Execution, Deployment > Build Tools > Gradle**, select
   **JDK 17 or above** as the Gradle JDK.
4. Select the **app** run configuration and your emulator or connected phone,
   then click **Run**.

Android Studio normally creates `local.properties` with the SDK location. For a
command-line build, set `JAVA_HOME` to JDK 17 or above and either set `ANDROID_HOME` or
provide the SDK path in `local.properties`. Use forward slashes in Windows SDK
paths. No API key or extra account setup is needed for the app.

I also tested the app on a Samsung phone. To run it wirelessly on Android 11 or
later, connect the phone and computer to the same Wi-Fi network, enable
**Wireless debugging** in the phone's Developer options, and use **Pair Devices
Using Wi-Fi** in Android Studio. After pairing, select the phone as the run target.

## Signing in

Both input fields start blank. Enter your own class credentials:

- **Student ID:** digits only, without the `s` prefix.
- **Password:** your first name with the exact capitalization required by the
  class. The app sends it as entered, so check for accidental spaces too.
- **Class location:** select the location where you attend the class.

For my class, I select **Footscray**. Sydney and BR are also available. The
selector starts with **Select your class location** so the person using the app
can choose their own location.

The configuration is in
[AppConfig.kt](app/src/main/java/au/edu/vu/studentidassignment2/config/AppConfig.kt).
The current setting is `defaultLocation = null`. To preselect Footscray, change it
to `ClassLocation.FOOTSCRAY`; the other options are `ClassLocation.SYDNEY` and
`ClassLocation.BR`. The selector remains available on the login screen.

The API base URL is `https://nit3213apinew.onrender.com/`. Authentication uses
`POST /footscray/auth`, `/sydney/auth` or `/br/auth`, with `username` and `password`
in the JSON body. After authentication, the app requests
`GET /dashboard/{keypass}` and opens the dashboard when the data is ready.

Personal credentials are not included in the source code. The app does not save
or log the login inputs, and the password field is cleared after a successful
sign-in.

## How I organised the code

I used MVVM with Fragments, XML layouts and View Binding. The UI displays the
state from its ViewModel, while the repository handles API access and response
parsing.

| Folder | Responsibility |
| --- | --- |
| `config/` | API address, timeouts and class-location settings |
| `domain/` | Entity, dashboard and error models |
| `data/` | Retrofit service, repository and JSON parser |
| `di/` | Koin dependency definitions |
| `ui/login/` | Login screen and ViewModel |
| `ui/dashboard/` | Dashboard screen, ViewModel and RecyclerView adapter |
| `ui/details/` | Details screen and ViewModel |

Koin starts in `AssignmentApplication` and provides the OkHttp client, Retrofit
service, parser, repository and ViewModels. This also lets the tests replace the
repository with a fake.

ViewModels use `StateFlow` and `viewModelScope`. Fragments collect state while
their views are started and clear their bindings when the views are destroyed.
A request can continue through rotation without being submitted again.
`SavedStateHandle` keeps the dashboard keypass for restoration; signing out
clears it. The selected item's JSON is passed to the Details screen in Fragment
arguments.

## Building and testing

Run these commands from the project root in Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

On macOS or Linux, use `sh ./gradlew` instead of `.\gradlew.bat`.

The debug APK is saved to `app/build/outputs/apk/debug/app-debug.apk`. Unit-test
reports are in `app/build/reports/tests/testDebugUnitTest/`, and lint reports are
in `app/build/reports/`.

With an emulator running or a phone connected, run the screen tests with:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

The unit tests cover input validation, loading and success states, errors,
retries, cancellation, session restoration, JSON parsing and dependency
injection. API contract tests use MockWebServer to check the request paths,
methods, bodies and response handling.

The device tests cover blank login fields, validation, activity recreation,
sign-in navigation, timeout recovery, cancellation, collection cards and Details.
They use fake responses and do not depend on the live API. Gradle saves their
reports in `app/build/reports/androidTests/connected/debug/`.

For a manual check, sign in with valid class credentials, open an item, go back,
refresh the dashboard, rotate the device and sign out. Turning off the device's
internet connection also lets you check the network error message.

## If sign-in takes too long

The button shows **Signing in** during authentication and **Loading your
collection** after the API has accepted the login. This helps identify which
request is taking time. After 10 seconds, the screen shows a waiting message.
You can cancel and try again.

The hosted API can be slow to respond. The limits in `AppConfig.kt` are 30
seconds to connect, a 180-second read timeout and 210 seconds for each complete
HTTP call. A timeout alone does not mean the credentials are wrong.

- For a DNS or connection error, try opening the API base URL in the phone or
  emulator's browser and check that device's internet connection.
- For a rejected login, check the student ID, first-name capitalization and
  class location. The app includes the HTTP status when the server returns one.
- If the button has reached **Loading your collection**, authentication has
  succeeded and the delay is in the dashboard request.
- For a secure-connection error, check the device's date and time.
- If the emulator becomes unresponsive or shows a System UI error, restart it
  or run the app on a physical phone.
