# PipTrack

Offline trading journal for Android.

## Build an APK with GitHub Actions

1. Create a GitHub repository named `PipTrack` (choose `main` as the default branch).
2. Extract this ZIP on your computer.
3. Upload the **contents** of the extracted `PipTrack-GitHub-Ready` folder to the repository root. Do not upload the outer folder itself.
4. Confirm that the repository root contains `app`, `.github`, `build.gradle.kts`, `settings.gradle.kts`, and `gradle.properties`.
5. Open the repository's **Actions** tab. Select **Build PipTrack APK** and run the workflow, or push a commit to `main`.
6. When the run finishes successfully, open it and download the `PipTrack-debug-apk` artifact. Extract the downloaded artifact ZIP to get `app-debug.apk`.

## App icon

The supplied black-and-white image is installed as the Android launcher icon in the `app/src/main/res/mipmap-*` resources, including adaptive icon resources for modern Android launchers.

## Important

This is a debug APK for testing, not a Play Store release. Keep backups of important trading records using the app's JSON backup feature. A different debug signing key can prevent installing a new build over an older build; export your data before uninstalling.
