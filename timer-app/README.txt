TIMER APP - TWO WAYS TO GET IT ON ANDROID

OPTION A: Install as an app (no APK, easiest)
1. Upload the "www" folder to any free static host (Netlify Drop: app.netlify.com/drop, GitHub Pages, Cloudflare Pages).
2. Open the resulting https link in Chrome on your phone.
3. Menu (3 dots) > "Install app" / "Add to Home screen".
It then opens full-screen, has its own icon, and works offline.

OPTION B: Build a real .apk (needs a PC with Node.js + Android Studio)
1. Open a terminal in this folder:
     npm install
     npx cap add android
     npx cap sync
     npx cap open android
2. In Android Studio: Build > Build Bundle(s) / APK(s) > Build APK(s).
3. Copy app-debug.apk to your phone and open it (allow "install unknown apps").

Notes: your data is stored on the device (IndexedDB/localStorage), so it stays in the app.
The font loads from Google Fonts when online and falls back to the system font offline.

OPTION C: Let GitHub build the APK for free (no PC tools needed)
1. Create a GitHub account and a new repository (e.g. "timer-app").
2. Upload ALL files from this folder, including the hidden ".github" folder.
   (If the .github folder does not upload: Add file > Create new file, type
   .github/workflows/build-apk.yml as the name and paste the file contents.)
3. Open the "Actions" tab. The "Build Android APK" job runs by itself (about 5-8 min).
   If not, click it > "Run workflow".
4. When it turns green, open the run and download "Timer-APK" under Artifacts. Unzip it to get app-debug.apk.
5. Send the APK to your phone and install it (allow "install unknown apps").
