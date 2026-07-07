# Astro Eleven Android Build History

This file tracks the builds created for the Astro Eleven Android application.

## [2026-06-01] - Debug Build (Clean Rebuild)

A clean rebuild was performed. Permitted domains configured inside `network_security_config.xml` to allow cleartext HTTP traffic for server IPs (`167.71.226.248` and `159.89.167.222`).

* **Date & Time:** June 01, 2026 - 12:08:35 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Size:** 123,430,092 Bytes (~117.71 MB)
* **Status:** Clean Rebuild Successful

---

## [2026-06-01] - Debug Build (Updated Code Rebuild)

A rebuild was performed containing major chat fixes (Image & Voice notes), including server response formatting alignments (`fileUrl` vs `url`) and socket response parsing fixes (`isMe` bubbled alignments) in `ChatViewModel.kt`.

* **Date & Time:** June 01, 2026 - 01:06:42 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Size:** 123,430,092 Bytes (~117.71 MB)
* **Status:** Rebuild Successful

---

## [2026-06-01] - Debug Build (Ringtone & Notification Dismiss Fixes)

A clean rebuild was performed after fixing the persistent ringtone sound and top notification issue. `ChatActivity.kt` and `IncomingCallActivity.kt` were updated to dismiss call notifications (`callerId.hashCode()`, `callId.hashCode()`, `9999`) and stop background sound loops of `CallForegroundService`.

* **Date & Time:** June 01, 2026 - 01:37:12 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Size:** 123,430,092 Bytes (~117.71 MB)
* **Status:** Rebuild Successful

---

## [2026-06-01] - Debug Build (MediaPlayer Error -38 Fixes)

A clean rebuild was performed after fixing the MediaPlayer error -38 issue by adding preparation guards and wrapping playback methods in try-catch blocks within `ChatAudioPlayer.kt`.

* **Date & Time:** June 01, 2026 - 02:02:40 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Size:** 120,646,062 Bytes (~115.06 MB)
* **Status:** Rebuild Successful

---

## [2026-06-09] - Debug Build (Branding, Flow & Calculation Updates)

A clean rebuild was performed containing all 11 branding and functional changes, including top/bottom bar UI redesigns, splash screen updates, default consult tab, introductory ₹20 wallet options, astrologer profile click flows, and KP astrology significator union calculations.

* **Date & Time:** June 09, 2026 - 11:30:00 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Rebuild Successful

---

## [2026-06-10] - Debug Build (Super Admin Profile & Dynamic Feedback Email Routing)

A clean rebuild was performed after adding the Super Admin profile settings tab (allowing display name and notification email updates) and configuring dynamic email routing in the feedback system based on the database super admin email, alongside removing the dropdown for issue types to allow typing in Feedback & Support.

* **Date & Time:** June 10, 2026 - 12:10:00 AM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Rebuild Successful

---

## [2026-06-10] - Debug Build (SMTP Config Menu Integration)

A clean rebuild was performed containing the updated codebase after adding the SMTP configuration settings and menus to the Super Admin dashboard.

* **Date & Time:** June 10, 2026 - 08:50:00 AM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Clean Rebuild Successful

---

## [2026-06-10] - Debug Build (Avatar Menu Icon Overlay Removal)

A clean rebuild was performed after removing the small overlay menu (hamburger) icon from the top-left user avatar on the home screen.

* **Date & Time:** June 10, 2026 - 09:14:00 AM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Clean Rebuild Successful

---

## [2026-06-27] - Release Bundle Build (AAB)

A clean release App Bundle was compiled and signed using the configured release keystore.

* **Date & Time:** June 27, 2026 - 08:38:00 PM (IST)
* **File Name:** `app-release.aab`
* **File Path:** `astroeleven/android/app/build/outputs/bundle/release/app-release.aab`
* **Size:** 90,392,757 Bytes (~86.2 MB)
* **Status:** Release Build Successful

---

## [2026-06-30] - Release Bundle Build (AAB) - Version 3.1.7

A clean release App Bundle (versionName "3.1.7", versionCode 20) was compiled and signed using the configured release keystore.

* **Date & Time:** June 30, 2026 - 01:37:00 PM (IST)
* **File Name:** `app-release.aab`
* **File Path:** `astroeleven/android/app/build/outputs/bundle/release/app-release.aab`
* **Size:** 91,644,435 Bytes (~87.4 MB)
* **Status:** Release Build Successful

---

## [2026-07-02] - Debug APK Build - Version 3.1.9 (.env Integration & New Launcher Icon)

A debug APK was built containing the latest code configuration and the new launcher icon logo. The backend API URL is now dynamically configured through the root `.env` file (`SERVER_URL`) via `BuildConfig`.

* **Date & Time:** July 02, 2026 - 09:34:17 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Size:** 123,112,151 Bytes (~117.41 MB)
* **Status:** Build Successful

---

## [2026-07-02] - Release APK Build - Version 3.1.9 (.env Integration & New Launcher Icon)

A release APK was compiled and signed using the configured release keystore, including the new launcher icon logo. The backend API URL is now dynamically configured through the root `.env` file (`SERVER_URL`) via `BuildConfig`.

* **Date & Time:** July 02, 2026 - 09:34:17 PM (IST)
* **File Name:** `app-release.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/release/app-release.apk`
* **Size:** 97,538,624 Bytes (~93.02 MB)
* **Status:** Build Successful

---

## [2026-07-04] - Debug APK Build - Version 3.1.9 (Clean Rebuild with AstroEleven Branding)

A debug APK was built containing the clean codebase after renaming all references from `riseastro` to `astroeleven`. The API URL is dynamically configured to point to the live server.

* **Date & Time:** July 04, 2026 - 10:50 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Size:** 119,671,311 Bytes (~114 MB)
* **Status:** Build Successful

---

## [2026-07-06] - Debug APK Build - Version 3.1.9 (New Welcome Screen Logo)

A debug APK was built containing the new logo image for the welcome splash screen. The APK compiled successfully.

* **Date & Time:** July 06, 2026 - 08:21 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-06] - Debug APK Build - Version 3.1.9 (Profile Pic, Settings/Themes Removal & Low Balance Redirection Fixes)

A debug APK was built after applying the following fixes:
- Resolved the database `findOneAndUpdate` method mapping error to allow successful astrologer profile picture updates.
- Removed the "Settings/Themes" cards and drawer items from both the Astrologer and Client app dashboards.
- Mapped "Help & Support" to launch the FeedbackSupportActivity instead of SettingsActivity.
- Added automatic redirection to the WalletActivity on the home screen card clicks and intake form submit errors when the user has insufficient balance.

* **Date & Time:** July 06, 2026 - 08:45 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-06] - Debug APK Build - Version 3.1.9 (Socket Connection URL Correction)

A debug APK was built after applying the following fix:
- Replaced the hardcoded socket server connection URL `https://socket.astroeleven.com` with `Constants.SERVER_URL` to align client-server real-time WebRTC signaling and chat events with the live host.

* **Date & Time:** July 06, 2026 - 09:00 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-06] - Debug APK Build - Version 3.1.9 (Voice Message Playback Path Normalization)

A debug APK was built after applying the following fix:
- Updated `ChatAudioPlayer.kt` and `AudioPlayerBubble.kt` to normalize relative voice message server paths (like `uploads/...`) into full HTTP URLs using `Constants.SERVER_URL` before streaming.

* **Date & Time:** July 06, 2026 - 09:12 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-06] - Debug APK Build - Version 3.2.0 (Store WebView & Remedies Navigation Menu Adjustments)

A debug APK was built after applying the following UI/UX modifications:
- Replaced the bottom navigation "Remedies" tab with a new "Store" tab that opens the customer shop front `https://astroeleven.in/` in an embedded WebView.
- Added a dedicated "Remedies" list entry to both the left navigation drawer and the customer Account tab options, launching a new `RemediesActivity` showing the spiritual rituals list.
- Added a dedicated "Store" tab to the Super Admin Dashboard console, rendering an embedded WebView of the WordPress admin logout/login URL `https://astroeleven.in/wp-login.php?loggedout=true&wp_lang=en_US`.

* **Date & Time:** July 06, 2026 - 09:50 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-06] - Debug APK Build - Version 3.2.1 (Super Admin Store Tab Link Update)

A debug APK was built after updating the following:
- Updated the Super Admin Console's Store WebView target URL to `https://astroeleven.in/wp-admin/edit.php?post_type=product` for direct WordPress products management.

* **Date & Time:** July 06, 2026 - 10:00 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-06] - Debug APK Build - Version 3.2.2 (Store WebView Header Hide Fix)

A debug APK was built after applying the following UI tweak:
- Wrapped `HomeTopBar` header inside `HomeScreen.kt` in a conditional check `selectedTab != 2` to automatically hide it when the customer Store tab is active.

* **Date & Time:** July 06, 2026 - 10:11 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-06] - Debug APK Build - Version 3.2.3 (Media3 ExoPlayer Migration for Audio Voice Notes)

A debug APK was built after applying the following fix:
- Replaced the old Android `MediaPlayer` with the modern `androidx.media3:media3-exoplayer` in `ChatAudioPlayer.kt` to stream voice messages directly from server URLs with Google Chrome User-Agent header injection, avoiding media preparation/state errors.

* **Date & Time:** July 06, 2026 - 10:45 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-07] - Debug APK Build - Version 3.2.4 (Reverted Audio Player Changes)

A debug APK was built after reverting the audio changes:
- Restored `ChatAudioPlayer.kt` and `AudioPlayerBubble.kt` to their previous working states.

* **Date & Time:** July 07, 2026 - 09:30 AM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-07] - Debug APK Build - Version 3.2.5 (Store WebView Touch Scroll and Mobile User-Agent Fix)

A debug APK was built after applying the following fixes:
- Added a custom touch listener in `HomeScreen.kt` to call `v.requestFocus()` when a touch is received on the Store WebView to prevent Compose scroll state interception.
- Injected a standard Mobile Chrome User-Agent to force WordPress WooCommerce to render the touch-optimized responsive layout.

* **Date & Time:** July 07, 2026 - 03:20 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-07] - Debug APK Build - Version 3.2.6 (Premium Consultation Intake Form and Outgoing Ring Tone)

A debug APK was built after applying the following updates:
- Completely redesigned `IntakeActivity.kt` to introduce a beautiful dark-cosmic glassmorphic layout, segmented touch gender tab selectors, switch toggles, and glowing gold outlines.
- Integrated a native `ToneGenerator` in the wait session to synthesize a real phone call ringback tone (`TONE_SUP_RINGTONE`) through the media speaker when waiting for connection.

* **Date & Time:** July 07, 2026 - 03:57 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-07] - Debug APK Build - Version 3.2.7 (Telegram Chat UI Redesign)

A debug APK was built after applying the following fixes:
- Replaced the WhatsApp style green layout with a premium Telegram Dark Mode theme using a dark midnight space background and extremely subtle watermark overlays.
- Redesigned message bubbles with Telegram-inspired shapes (asymmetric corner rounding tails) and soft violet gradient (for sent) vs dark slate (for received) backgrounds.
- Converted quote blocks, text, status indicators, and typing status bubbles to fit seamlessly with the Telegram layout styles.

* **Date & Time:** July 07, 2026 - 04:05 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful

---

## [2026-07-07] - Debug APK Build - Version 3.2.8 (WhatsApp-Grade ExoPlayer Caching & Waveform UI)

A debug APK was built after applying the following fixes:
- Replaced Android MediaPlayer with Media3 ExoPlayer stream engine for zero-download voice note streaming.
- Integrated a thread-safe static Singleton `ExoPlayer` instance to enforce global single-play playback across screens.
- Set up a 250 MB LRU local cache using Media3 `SimpleCache` and optimized OkHttp media connection pool.
- Redesigned `AudioPlayerBubble.kt` to render a custom Compose Canvas Waveform with 32 rounded lines, highlighting played progress and supporting tap/drag seeking.
- Optimized Nginx configurations in `setup_nginx_deploy.sh` to support cross-origin streaming ranges.

* **Date & Time:** July 07, 2026 - 04:20 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk`
* **Status:** Build Successful


