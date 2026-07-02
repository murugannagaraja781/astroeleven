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

## [2026-07-02] - Debug APK Build - Version 3.1.9 (Updated Launcher Icon)

A debug APK was built containing the latest code configuration and the new launcher icon logo.

* **Date & Time:** July 02, 2026 - 08:06:53 PM (IST)
* **File Name:** `app-debug.apk`
* **File Path:** `riseastro/android/app/build/outputs/apk/debug/app-debug.apk`
* **Size:** 123,112,151 Bytes (~117.41 MB)
* **Status:** Build Successful

---

## [2026-07-02] - Release APK Build - Version 3.1.9 (Updated Launcher Icon)

A release APK was compiled and signed using the configured release keystore, including the new launcher icon logo.

* **Date & Time:** July 02, 2026 - 08:06:53 PM (IST)
* **File Name:** `app-release.apk`
* **File Path:** `riseastro/android/app/build/outputs/apk/release/app-release.apk`
* **Size:** 97,538,614 Bytes (~93.02 MB)
* **Status:** Build Successful

