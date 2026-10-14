![Android](https://img.shields.io/badge/Android-7.0%2B-success?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue?style=for-the-badge)
![Compose](https://img.shields.io/badge/Jetpack_Compose-M3-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-Apache--2.0-green?style=for-the-badge)

# tott

`tott` stands for "That one tool for tasks" and is a part of an open-source app series named `tot` which stands for "That one tool".

`tott` is an offline-first Android task manager. Quick-add tasks with priorities, real due dates and times, subtasks and photo attachments, arrange them on a calendar, get reminded, set repeats with streaks, reorder by drag, track projects and insights, pin a widget — all in one smooth Material 3 interface with a full theme studio.

I am trying to make apps that are better, smoother, offline and easy for new users to work with and just better overall experience. I encourage you to report all issues you find as i can't test it on every single phone solo (i daily-drive it on a Redmi Note 8, so anything newer should fly). I need your help to improve this app. You can also help by recommending features as i am not the most creative person, if you couldn't tell.

I started this one from an HTML mock of the whole UI and ported it one-to-one into Jetpack Compose, so what you see in `taskflow.html` is what you get on the phone. I know that doesnt explain anything but yesh.



## Features

* Tasks: quick-add with priority (High / Medium / Low), real due dates + optional times, subtasks with progress, photo attachments, repeat (Never / Daily / Weekly), per-task focus timer
* Reminders: exact-time notifications with subtask checklist, progress, Mark done + Snooze actions, rescheduled after reboot, tap opens the task
* Calendar: month grid with per-day dots, agenda, jump-to-today, add directly on any day
* Organize: projects with custom colors, filter chips (All / Today / High priority / Done), search, sort (Default / Priority / A–Z), hide completed, long-press drag to reorder, swipe right to complete / left to delete, undo everywhere
* Insights: completed + completion %, day streak with active repeats, open-tasks-by-day chart
* Theme studio: System / Light / Dark + AMOLED black, dynamic wallpaper color, six accents, text size, compact layout, reduce-motion — everything instant, no restart
* Widget: today's tasks on the home screen with tap-to-complete
* Fully offline: ObjectBox database, no account, no network calls, export-free — your tasks never leave the phone

## Install

Grab the APK from the [Releases page](https://github.com/emty-png/tott/releases) and tap it to install (`tott-<version>.apk`, signed, installs cleanly over older builds).

Heads up for MIUI / Xiaomi phones: the system may block the install with "didn't allow installing via USB". Either allow **Install via USB** in Developer options, or skip adb entirely — copy the APK to Downloads and tap it there (allow installs from your file manager when asked).

Or build from source. You need JDK 17 and the Android SDK (API 34):

```sh
git clone https://github.com/emty-png/tott.git
cd tott
export ANDROID_HOME=~/Android/Sdk   # must provide platform-34 + build-tools 34.0.0
./gradlew :app:assembleRelease
# find it at app/build/outputs/apk/release/app-release.apk
```

Don't have the SDK yet? The fastest path is the command-line tools:

```sh
mkdir -p ~/Android/Sdk/cmdline-tools
# unpack commandlinetools-linux-*_latest.zip so that
# ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager exists, then:
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

## Contributing

While gradle + android is not the most inviting stack for people to contribute in but if you still want to do it-

Open an issue or a PR, and run before pushing:

```sh
./gradlew :app:lintDebug :app:assembleDebug :app:assembleRelease
```

## License

Every single app in `tot` series will be open-source and Apache 2.0 license. So yeah, enjoy.

Apache-2.0 — see [`LICENSE`](LICENSE).
