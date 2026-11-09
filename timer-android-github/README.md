# Timer Android APK (GitHub Actions)

This repository wraps the supplied Timer HTML app in a native Android WebView using Capacitor. GitHub Actions builds a debug APK automatically.

## Build it on GitHub (no Android Studio needed)

1. Create a new repository on GitHub (for example, `timer-android`).
2. Upload **all files and folders** from this project to the repository. Keep the `.github/workflows/build-apk.yml` path intact.
3. Make sure the default branch is `main` (or `master`).
4. Open the repository's **Actions** tab. If prompted, enable GitHub Actions.
5. Select **Build Android APK** and click **Run workflow**, or push a commit to `main`.
6. Wait for the workflow to finish successfully.
7. Open the completed workflow run and download **Timer-Android-APK** under **Artifacts**.
8. Extract the downloaded ZIP and install `app-debug.apk` on your Android phone. You may need to allow installation from that source.

## Notes

- This produces a debug APK for personal testing/installation, not a Play Store release-signed APK.
- The app's task/session data is stored locally on the device. Clearing app data or uninstalling may erase it; use the app's backup/export feature regularly.
- The web app uses Google Fonts when internet access is available; its main functionality is bundled locally.
- The app's web service-worker registration and unused PWA icon/manifest references were removed from the Android copy because the Capacitor app loads its bundled files directly.
- To change the Android package ID, edit `appId` in `capacitor.config.json` before the first build.
