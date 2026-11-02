# Timer for Android

Same app as the web version, packaged in a WebView. Data lives in the app's own storage.

## Get the APK (about 10 minutes, free)
1. Create a free account at github.com and make a new **private** repository.
2. Upload everything in this folder (keep the `.github` folder) to the repository.
3. Open the **Actions** tab, choose **Build APK**, press **Run workflow**.
4. When it finishes (a few minutes), open the run and download **Timer-APK** from Artifacts. Unzip it to get `app-release.apk`.
5. Also download **KEEP-timer-keystore** and store it safely. Later versions must be signed with it to update over this one.

## Install on Samsung
1. Copy `app-release.apk` to the phone (Telegram, Drive, USB).
2. Open it from My Files. When asked, allow "Install unknown apps" for that app.
3. If Play Protect warns, choose "Install anyway" (the app is self-signed).
4. Open Timer from the app drawer.

Permissions: Internet (web font only) and Wake lock (keep screen on). Nothing else.
