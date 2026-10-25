# Finch — Smart Alarm ⏰

A modern, mission-driven Android alarm clock application built with **Jetpack Compose** and **Material 3**. Finch ensures you never accidentally snooze or fall back asleep by requiring you to complete interactive physical or cognitive challenges before the alarm can be silenced.

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-blue.svg)](https://developer.android.com/jetpack/compose)

---

## ✨ Features

### 📅 Multi-Alarm Scheduling
- **Customizable Schedules**: Configure multiple independent alarms with individual hours, minutes, custom labels (e.g. *Work Routine*, *Gym*, *Medicine*), and recurring day-of-week patterns (*Mon–Sun*, *Weekdays*, *Weekends*, or one-time *Once* alarms).
- **Hero Upcoming Alarm Card**: Dynamic gradient banner displaying your next scheduled alarm with a real-time countdown badge, active day pills, and assigned challenge mission.
- **Instant Toggles & Customization**: Clean Material 3 switches on every alarm card for quick enabling/disabling, editing, sound preview, and deletion.

### ⚡ 20 Wake-Up Missions & Challenges
When the alarm sounds, volume is locked and ringing cannot be stopped without solving your assigned challenge:
1. **Eye Tracking 60s**: Focus and keep your eyes open for 60 seconds.
2. **Math Easy**: Solve 3 single-digit arithmetic sums.
3. **Math Medium**: Solve 3 two-digit arithmetic equations.
4. **Math Hard**: Solve 10 mixed multi-operation problems.
5. **QR Code**: Scan a preset QR code placed across the room (e.g. bathroom/kitchen).
6. **NFC Tap**: Tap an NFC tag away from your bed.
7. **Shake 50x**: Vigorously shake your phone 50 times.
8. **Walk 30 Steps**: Step detection to ensure you get out of bed.
9. **Squats 10x**: Perform 10 full squats with pose/motion detection.
10. **Typing Phrase**: Accurately retype a morning motivation phrase.
11. **Memory Sequence**: Simon says 5-round sequence memory game.
12. **Puzzle Slider**: Solve a 3x3 sliding tile puzzle.
13. **Voice Repeat**: Speak the morning phrase clearly out loud.
14. **Smile 5s**: Detect a genuine smile on camera for 5 seconds.
15. **Photo Challenge**: Take a photo of a designated location (e.g., sink).
16. **Light Sensor**: Expose the ambient light sensor to bright light (>500 lux).
17. **Word Scramble**: Unscramble 3 morning vocabulary words.
18. **Pattern Draw**: Connect the dots following a target pattern.
19. **Trivia Quiz**: Answer 3 general knowledge trivia questions.
20. **Breath Hold**: Hold the device steady while taking a deep awakening breath.

### 🎵 Ringtone Audio Station
- **25 Bundled Offline Tones + System Default**: Ringtones ranging from gentle morning harps to energetic pulses and urgent sirens.
- **Categorized Browser**: Filter by *Gentle*, *Energetic*, *Loud*, or *Classic*.
- **Live In-App Audio Preview**: Tap any sound to test before assigning.

### 💡 Daily Morning Motivation
- Inspiring offline quote collection with author attribution.
- Offline **Text-to-Speech (TTS)** voice reader for hands-free morning encouragement.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: 100% Jetpack Compose with Material 3 design tokens, dark & light themes, and fluid animations.
- **Scheduling**: `AlarmManager.setAlarmClock()` with individual `PendingIntent` request codes for reliable, exact wakeups that bypass Doze mode.
- **Reboot Persistence**: `BootReceiver` automatically reschedules all active alarms after device reboots.
- **Ringing Service**: Android Foreground Service with `PowerManager` partial wake locks, audio focus management, and tamper-resistant volume enforcement.
- **Persistence**: Lightweight JSON serialization via SharedPreferences.

---

## 🚀 Building the Project

### Prerequisites
- Android Studio Iguana / Ladybug (or CLI with Android SDK 34)
- JDK 17 or higher

### Build Debug APK
```bash
# Clone the repository
git clone https://github.com/Priyyanxhu/Finch.git
cd Finch

# Build debug APK
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
This project is open-source under the MIT License.
