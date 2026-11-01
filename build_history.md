# Astro Eleven Android Build History

This file tracks the builds created for the Astro Eleven Android application.

## [2026-11-01] - Release Build v1.1.7 (Code 53)

A release rebuild was performed containing:
1. **Ghost Call Fixes (Video & Audio Calls):**
   - Implemented backend handler for `cancel-call` event so when callers drop while ringing, the call immediately marks as ended in DB and sends an FCM push (`CANCEL_CALL`) to dismiss incoming call notifications on recipient astrologer devices.
   - Updated 30-second missed call timeout to persist `ended` status in DB and send FCM push to cancel ringing notifications.
   - Fixed `FCMService.kt` to clear dynamic call notification IDs (`callId.hashCode()`, `sessionId.hashCode()`, `callerId.hashCode()`) on call cancellation or session ended.
   - Added server acknowledgement verification in `IncomingCallActivity.kt` so expired/cancelled calls cannot launch `CallActivity`.
   - Added 25-second connection watchdog in `CallActivity.kt` that auto-terminates if the peer never connects, preventing fake timers or ghost call screens.
   - Added `ACTION_CANCEL_CALL` BroadcastReceiver in `CallActivity.kt`.
   - Fixed `CallActivity.kt` duration timer to show `00:00` until connection is established.
2. **Consultation & Chat History (Client Access):**
   - Added "Consultation & Chat History" card on Home feed directly under the Search bar.
   - Added "History" navigation drawer item with icon directly below "Home".
   - Added history shortcut icon button in `HomeTopBar.kt`.
   - Full support for Tamil and English chat transcript viewing modal dialog in `AstrologerHistoryActivity.kt`.
   - Unified client and astrologer consultation history API endpoints in `server.js` and `user.controller.js`.

* **Date & Time:** November 01, 2026 - 12:20:00 PM (IST)
* **File Name:** `app-release.apk` / `astroeleven.apk`
* **Version Code:** 53
* **Version Name:** 1.1.7
* **File Path:** `astroeleven/android/app/build/outputs/apk/release/app-release.apk`
* **Status:** Release Build & Deployment Successful (https://astroeleven.com/astroeleven.apk)

---

## [2026-11-01] - Release Build v1.1.6 (Code 52)

A release rebuild was performed containing:
1. **Chat Loud Ringtone & Vibration:** Chat requests now launch IncomingCallActivity with looping ringtone and vibration identical to audio/video calls.
2. **Audio Call Connection:** Immediate WebRTC offer exchange upon `billing-started` and multi-fallback recipient ID resolution.
3. **Astrologer History Chat Viewer:** Added full chat transcript viewer dialog in `AstrologerHistoryActivity.kt` and web view.

* **Date & Time:** November 01, 2026 - 10:13:00 AM (IST)
* **File Name:** `app-release.apk` / `astroeleven.apk`
* **Version Code:** 52
* **Version Name:** 1.1.6
* **File Path:** `astroeleven/android/app/build/outputs/apk/release/app-release.apk`
* **Size:** 71,242,006 Bytes (~68 MB)
* **Status:** Release Build & Deployment Successful (https://astroeleven.com/astroeleven.apk)

---

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

---

## [2026-07-15] - Release & Debug Builds - Version 1.7.0 (Rasi Classifications Integration)

A clean build of Debug APK, Release APK, and Release App Bundle (AAB) was compiled successfully.
- Version Code: 17
- Version Name: "1.7.0"

* **Date & Time:** July 15, 2026 - 05:47 PM (IST)
* **Debug APK File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk` (copied to root `app-debug.apk`)
* **Release APK File Path:** `astroeleven/android/app/build/outputs/apk/release/app-release.apk` (copied to root `app-release.apk`)
* **Release AAB File Path:** `astroeleven/android/app/build/outputs/bundle/release/app-release.aab` (copied to root `app-release.aab`)
* **Status:** Clean Rebuild Successful

---

## [2026-07-15] - Release & Debug Builds - Version 1.7.0 (Tamil Translation Updates)

A clean build of Debug APK, Release APK, and Release App Bundle (AAB) was compiled successfully after translating all chart activities, grids, planet tables, tabs, and popups purely to Tamil without grammatical mistakes.
- Version Code: 17
- Version Name: "1.7.0"

* **Date & Time:** July 15, 2026 - 08:19 PM (IST)
* **Debug APK File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk` (copied to root `app-debug.apk`)
* **Release APK File Path:** `astroeleven/android/app/build/outputs/apk/release/app-release.apk` (copied to root `app-release.apk`)
* **Release AAB File Path:** `astroeleven/android/app/build/outputs/bundle/release/app-release.aab` (copied to root `app-release.aab`)
* **Status:** Clean Rebuild Successful

---

## [2026-07-15] - Release & Debug Builds - Version 1.7.0 (Subha/Asubha Calculation Tab & Orange Theme Updates)

A clean build of Debug APK, Release APK, and Release App Bundle (AAB) was compiled successfully after:
1. Adding a new "சுப அசுப கிரகங்கள்" (Benefic/Malefic Planets) tab to calculate benefic, malefic, and maraka planets based dynamically on the user's lagna (ascendant).
2. Changing the green header/header backgrounds of the planet and transit tables in the Rasi chart screen to an orange/yellow theme.
3. Updating the Astrologer Profile screen color palette to orange (#E87A1E) instead of green, and adding a dynamic orange "Subscribe to [Astrologer Name]" button with a heart icon.
- Version Code: 17
- Version Name: "1.7.0"

* **Date & Time:** July 15, 2026 - 09:09 PM (IST)
* **Debug APK File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk` (copied to root `app-debug.apk`)
* **Release APK File Path:** `astroeleven/android/app/build/outputs/apk/release/app-release.apk` (copied to root `app-release.apk`)
* **Release AAB File Path:** `astroeleven/android/app/build/outputs/bundle/release/app-release.aab` (copied to root `app-release.aab`)
* **Status:** Clean Rebuild Successful

---

## [2026-07-15] - Release & Debug Builds - Version 1.8.0 (16KB Page Size Alignment & Version Code 18 Rebuild)

A clean build of Debug APK, Release APK, and Release App Bundle (AAB) was compiled successfully after:
1. Incrementing the build parameters to versionCode 18 and versionName "1.8.0".
2. Confirming that 16KB page size alignment support is fully configured (`useLegacyPackaging = false` in `packaging.jniLibs`).
- Version Code: 18
- Version Name: "1.8.0"

* **Date & Time:** July 15, 2026 - 10:17 PM (IST)
* **Debug APK File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk` (copied to root `app-debug.apk`)
* **Release APK File Path:** `astroeleven/android/app/build/outputs/apk/release/app-release.apk` (copied to root `app-release.apk`)
* **Release AAB File Path:** `astroeleven/android/app/build/outputs/bundle/release/app-release.aab` (copied to root `app-release.aab`)
* **Status:** Clean Rebuild Successful

---

## [2026-10-05] - Debug Build (Planet Star Lord Column, Astrologer Profile Share & TURN Server Update)

A clean build of Debug APK was compiled successfully after:
1. Adding "ந. அதிபதி" (Nakshatra Lord / Sara Nathan) column to the "கிரக நிலைகள் (நவக்கிரகங்கள்)" table in `VipChartActivity.kt`.
2. Fixing Astrologer Profile Share logic and direct web links in `AstrologerProfileActivity.kt`.
3. Updating WebRTC TURN/STUN configuration to `turn.astroeleven.in` in `CallActivity.kt`.

* **Date & Time:** October 05, 2026 - 05:24 PM (IST)
* **Debug APK File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk` (copied to root `app-debug.apk`)
* **Size:** ~114 MB
---

## [2026-10-16] - Debug Build (Chat & Call Multitasking, Ongoing Notifications, App Re-entry & Dashboard Return Banners)

A clean build of Debug APK was compiled successfully after:
1. Creating `ActiveChatSessionManager.kt` to manage both active chat and call sessions (audio & video) with persistent SharedPreferences state and ongoing high-priority system notifications.
2. Updating `ChatActivity.kt` and `CallActivity.kt` so back button presses minimize the activity (`moveTaskToBack(true)`) rather than destroying active sessions.
3. Hooking up `CallForegroundService.kt` with high-priority call notifications and direct action buttons ("அழைப்புக்கு திரும்பு") ensuring calls stay connected when switching apps.
4. Adding automatic session resume logic to `MainActivity.kt` and sticky active-session return cards to `AstrologerDashboardActivity.kt` and `HomeScreen.kt`.

* **Date & Time:** October 16, 2026 - 11:18 PM (IST)
* **Debug APK File Path:** `astroeleven/android/app/build/outputs/apk/debug/app-debug.apk` (copied to root `app-debug.apk` & `public/astroeleven.apk`)
* **Size:** ~120 MB (120,038,796 bytes)
* **Status:** Build Successful

---

## [2026-10-31] - Release APK Build - Version 1.0.7 (Version Code 43)

A signed release APK build was compiled successfully:
- Application ID: `com.astroeleven.app`
- Version Code: `43`
- Version Name: `"1.0.7"`
- Target SDK: 36, Min SDK: 24
- Signed with release keystore (`astrocust.jks`, V1, V2, V3, V4 enabled)
- Proguard / R8 minified with 16KB page alignment support

* **Date & Time:** October 31, 2026 - 10:35 AM (IST)
* **Release APK File Path:** `astroeleven/android/app/build/outputs/apk/release/app-release.apk`
* **Size:** ~68 MB (71,225,616 bytes)
* **Status:** Build Successful

---

## [2026-10-31] - Release APK Build - Version 1.0.8 (Version Code 44) - Rate Limit ₹50 & Super Admin Controls

A signed release APK build was compiled successfully:
- Application ID: `com.astroeleven.app`
- Version Code: `44`
- Version Name: `"1.0.8"`
- Features & Fixes:
  1. Astrologer Dashboard Rate Setting Safeguard: Direct rates capped at ₹50/min. Any rate entered > ₹50 shows a permission alert dialog directing the astrologer to obtain Super Admin approval.
  2. Super Admin Dashboard Rate Control: Super Admin can set custom Chat, Audio Call, and Video Call rates without limit for any astrologer via dedicated "Set Rates" modal and edit form.
  3. Real-time rate sync & server-side validation.
- Signed with release keystore (`astrocust.jks`, V1, V2, V3, V4 enabled)
- Proguard / R8 minified with 16KB page alignment support

* **Date & Time:** October 31, 2026 - 04:02 PM (IST)
* **Release APK File Path:** `astroeleven/android/app/build/outputs/apk/release/app-release.apk` (copied to root `app-release.apk` & `public/astroeleven.apk`)
* **Size:** ~68 MB (71,225,616 bytes)
* **Status:** Build Successful

---

## [2026-10-31] - Release APK Build - Version 1.0.9 (Version Code 45) - Low Balance (< ₹1) Call Termination, Alert & Recharge Flow

A signed release APK build was compiled successfully:
- Application ID: `com.astroeleven.app`
- Version Code: `45`
- Version Name: `"1.0.9"`
- Features & Fixes:
  1. Low Balance Call Cut: If client wallet balance drops below ₹1 during active call, connection, or ticker billing, call is cut immediately (`insufficient_balance`).
  2. Low Balance Alert Dialog: Custom alert dialog displayed informing the client that the call was disconnected because the balance dropped below ₹1.
  3. One-Click Recharge Flow: Clicking "ரீசார்ஜ் செய்க (Recharge Now)" on the alert or summary dialog directly launches `WalletActivity`.
  4. Real-time Socket Event: Listens to `wallet-update` to terminate WebRTC media stream and display low balance alert instantly.
- Signed with release keystore (`astrocust.jks`, V1, V2, V3, V4 enabled)
- Proguard / R8 minified with 16KB page alignment support

* **Date & Time:** October 31, 2026 - 06:00 PM (IST)
* **Release APK File Path:** [astroeleven/android/app/build/outputs/apk/release/app-release.apk](file:///Users/wohozo/Documents/astroeleven/astroeleven/android/app/build/outputs/apk/release/app-release.apk) (copied to [app-release.apk](file:///Users/wohozo/Documents/astroeleven/app-release.apk) & [public/astroeleven.apk](file:///Users/wohozo/Documents/astroeleven/public/astroeleven.apk))
* **Size:** ~68 MB (71,226,380 bytes)
* **Status:** Build Successful

---

## [2026-10-31] - Release APK Build - Version 1.1.0 (Version Code 46) - Consultation Time Duration Fix (0s Fix)

A signed release APK build was compiled successfully:
- Application ID: `com.astroeleven.app`
- Version Code: `46`
- Version Name: `"1.1.0"`
- Features & Fixes:
  1. Consultation Time 0s Fix: In `AstrologerHistoryActivity.kt` and `HomeScreen.kt`, `item.duration / 1000` was inadvertently dividing the server's duration (which was already in seconds) by 1000 again, converting all durations under 1000s into 0s. Updated duration calculation to `if (item.duration > 50000) item.duration / 1000 else item.duration` so that both seconds and milliseconds are formatted accurately as e.g. "14m 0s", "5m 0s", "1m 0s".
  2. Chart Loading Fix: Resolved `dashaStarLordTamil` ReferenceError in `/api/rasi-eng/charts/full` on live server.
- Signed with release keystore (`astrocust.jks`, V1, V2, V3, V4 enabled)
- Proguard / R8 minified with 16KB page alignment support

* **Date & Time:** October 31, 2026 - 09:10 PM (IST)
* **Release APK File Path:** [astroeleven/android/app/build/outputs/apk/release/app-release.apk](file:///Users/wohozo/Documents/astroeleven/astroeleven/android/app/build/outputs/apk/release/app-release.apk) (copied to [app-release.apk](file:///Users/wohozo/Documents/astroeleven/app-release.apk) & [public/astroeleven.apk](file:///Users/wohozo/Documents/astroeleven/public/astroeleven.apk))
* **Status:** Build Successful

---

## [2026-10-31] - Release APK Build - Version 1.1.1 (Version Code 47) - Upfront Permissions on Login for User & Astrologer (Zero Delay Call Connection)

A signed release APK build was compiled successfully:
- Application ID: `com.astroeleven.app`
- Version Code: `47`
- Version Name: `"1.1.1"`
- Features & Fixes:
  1. Upfront Permissions Request for Users (Clients): When logging in or entering `HomeActivity` / `HomeScreen`, the app immediately requests runtime permissions for Notifications (`POST_NOTIFICATIONS`), Audio/Microphone (`RECORD_AUDIO`), and Camera (`CAMERA`) sequentially in one flow.
  2. Zero Delay Call Connection: Prevents system permission dialogs from popping up during call initiation (`CallActivity`), ensuring instant WebRTC media initialization and connection without delays or freezes.
  3. Upfront Permissions Request for Astrologers: When an astrologer logs in or opens `AstrologerDashboardActivity`, the app immediately requests runtime permissions (Notifications, Audio, Camera) upfront.
  4. 1-Click Permissions & Repair Flow in `PermissionActivity`: Native runtime permission prompt launcher integrated directly into `PermissionActivity` and `AstrologerDashboardActivity`, allowing astrologers to grant all missing permissions with a single click instead of being sent to Android settings app.
  5. Permission Warning Cards: Dynamic, friendly warning banners added to both `HomeScreen.kt` and `AstrologerDashboardActivity.kt` allowing users and astrologers to re-trigger permissions in one click if previously dismissed.
- Signed with release keystore (`astrocust.jks`, V1, V2, V3, V4 enabled)
- Proguard / R8 minified with 16KB page alignment support

* **Date & Time:** October 31, 2026 - 10:20 PM (IST)
* **Release APK File Path:** [astroeleven/android/app/build/outputs/apk/release/app-release.apk](file:///Users/wohozo/Documents/astroeleven/astroeleven/android/app/build/outputs/apk/release/app-release.apk) (copied to [app-release.apk](file:///Users/wohozo/Documents/astroeleven/app-release.apk) & [public/astroeleven.apk](file:///Users/wohozo/Documents/astroeleven/public/astroeleven.apk))
* **Status:** Build Successful

---

## [2026-11-01] - Release APK Build - Version 1.1.2 (Version Code 48) - Astrologer Withdrawal Flow & Upfront Permissions Rebuild

A signed release APK build was compiled successfully:
- Application ID: `com.astroeleven.app`
- Version Code: `48`
- Version Name: `"1.1.2"`
- Features & Fixes:
  1. Astrologer Withdrawal Instant Deduction: Astrologer dashboard immediately deducts the requested withdrawal amount from wallet balance upon request, with instant UI feedback and toast confirmation.
  2. Real-Time Socket Synchronization: Connected socket session for astrologers with listeners for `wallet-update` and `app-notification`.
  3. Super Admin Real-Time & Persistent Notifications: Real-time socket notification and persistent database record for Super Admin on withdrawal requests, complete with badge count and push notifications.
  4. Upfront Permissions Request on Login: Zero delay call connection by requesting Microphone, Camera, and Notification permissions upfront upon login for both Users and Astrologers.
- Signed with release keystore (`astrocust.jks`, V1, V2, V3, V4 enabled)
- Proguard / R8 minified with 16KB page alignment support

* **Date & Time:** November 01, 2026 - 12:12 AM (IST)
* **Release APK File Path:** [astroeleven/android/app/build/outputs/apk/release/app-release.apk](file:///Users/wohozo/Documents/astroeleven/astroeleven/android/app/build/outputs/apk/release/app-release.apk) (copied to [app-release.apk](file:///Users/wohozo/Documents/astroeleven/app-release.apk) & [public/astroeleven.apk](file:///Users/wohozo/Documents/astroeleven/public/astroeleven.apk))
* **Status:** Build Successful

---

## [2026-11-01] - Release APK Build - Version 1.1.3 (Version Code 49) - Withdrawal Minimum Threshold (₹100) & findOneAndUpdate Atomic Query Fix

A signed release APK build was compiled successfully:
- Application ID: `com.astroeleven.app`
- Version Code: `49`
- Version Name: `"1.1.3"`
- Features & Fixes:
  1. Minimum Withdrawal Limit Lowered to ₹100: Updated Astrologer dashboard (`AstrologerDashboardActivity.kt`) and Web views (`desktop-view.html`) from previous ₹500 limit to ₹100, aligning with server-side validation.
  2. Mongoose-MySQL `findOneAndUpdate` Atomic Lookup Fix: Resolved critical query condition bug where re-querying by original `{ walletBalance: { $gte: amt } }` returned null after deduction, ensuring withdrawal records and super admin notifications are always created and delivered reliably.
  3. Real-Time Super Admin Notification & Audio Alert: Verified instant notification bell counter increment, persistent DB entry, and live card in "Withdrawals" tab.
- Signed with release keystore (`astrocust.jks`, V1, V2, V3, V4 enabled)
- Proguard / R8 minified with 16KB page alignment support

* **Date & Time:** November 01, 2026 - 02:00 AM (IST)
* **Release APK File Path:** [astroeleven/android/app/build/outputs/apk/release/app-release.apk](file:///Users/wohozo/Documents/astroeleven/astroeleven/android/app/build/outputs/apk/release/app-release.apk) (copied to [app-release.apk](file:///Users/wohozo/Documents/astroeleven/app-release.apk) & [public/astroeleven.apk](file:///Users/wohozo/Documents/astroeleven/public/astroeleven.apk))
* **Status:** Build Successful

---

## [2026-11-01] - Release APK Build - Version 1.1.8 (Version Code 54) - Search Box Text Alignment Fix & Ghost Call Termination

A signed release APK build was compiled successfully:
- Application ID: `com.astroeleven.app`
- Version Code: `54`
- Version Name: `"1.1.8"`
- Features & Fixes:
  1. Search Box Typing & Text Alignment Fix: Fixed letter clipping and vertical hiding of Tamil matras and characters with descenders (g, j, p, q, y) in `SearchBarSection` (`HomeScreen.kt`) by replacing fixed-height Material 3 `OutlinedTextField` with custom `BasicTextField`, `PlatformTextStyle(includeFontPadding = false)`, and center-aligned content.
  2. Ghost Call Termination & Signal Fixes: Added cancellation of dynamic call notification IDs in `FCMService.kt`; added pre-answer validation in `IncomingCallActivity.kt`; integrated 25-second connection watchdog and cancel broadcast receiver in `CallActivity.kt`.
  3. Chat History & Session Duration Reconciled: Fixed `endChat()` in `ChatActivity.kt` and `cancel-call` in `callHandler.js` to prevent active session cancellation; added `endReason` support to MySQL database table `sessions`.
- Signed with release keystore (`astrocust.jks`, V1, V2, V3, V4 enabled)
- Proguard / R8 minified with 16KB page alignment support

* **Date & Time:** November 01, 2026 - 01:10 PM (IST)
* **Release APK File Path:** [astroeleven/android/app/build/outputs/apk/release/app-release.apk](file:///Users/wohozo/Documents/astroeleven/astroeleven/android/app/build/outputs/apk/release/app-release.apk) (copied to [public/astroeleven.apk](file:///Users/wohozo/Documents/astroeleven/public/astroeleven.apk))
* **Direct Download URL:** `https://astroeleven.com/astroeleven.apk`
* **Size:** ~68 MB (71,258,383 bytes)
* **Status:** Build & Deployment Successful

---

## [2026-11-01] - Release APK Build - Version 1.1.9 (Version Code 55) - Header Overlap Fix & Proportional UI Sizing

A signed release APK build was compiled successfully:
- Application ID: `com.astroeleven.app`
- Version Code: `55`
- Version Name: `"1.1.9"`
- Features & Fixes:
  1. HomeTopBar Overlap & Word-Breaking Fix: Fixed "Ast ro Ele ven" text squishing into vertical lines by enforcing `maxLines = 1`, `softWrap = false`, and `TextOverflow.Ellipsis` on title and greeting, removing forced filling on the left row (`weight(1f, fill = false)`), and expanding horizontal breathing room.
  2. Proportional Action Buttons: Sized all top bar action buttons (Wallet Pill, Consultation History, Language Translate, and Notification Bell) to harmonious 30dp compact luxury pills with 5dp spacing and 12dp horizontal padding, eliminating any overlapping across all mobile screens.
  3. Drawer Navigation to History: Wired up "history" in `HomeActivity.kt`'s `onDrawerItemClick` to open `AstrologerHistoryActivity` directly from the side navigation drawer.
- Signed with release keystore (`astrocust.jks`, V1, V2, V3, V4 enabled)
- Proguard / R8 minified with 16KB page alignment support

* **Date & Time:** November 01, 2026 - 02:20 PM (IST)
* **Release APK File Path:** [astroeleven/android/app/build/outputs/apk/release/app-release.apk](file:///Users/wohozo/Documents/astroeleven/astroeleven/android/app/build/outputs/apk/release/app-release.apk) (copied to [public/astroeleven.apk](file:///Users/wohozo/Documents/astroeleven/public/astroeleven.apk))
* **Direct Download URL:** `https://astroeleven.com/astroeleven.apk`
* **Size:** ~68 MB (71,258,386 bytes)
* **Status:** Build & Deployment Successful







