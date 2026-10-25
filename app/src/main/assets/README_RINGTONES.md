# 25 Ringtones - Finch

All 25 files in ../res/raw/ are **CC0 / royalty-free** generated tones (no copyrighted samples).

| # | File | Display Name | Character |
|---|---|---|---|
|1|rooster.wav|Rooster Morning|Crowing chirp with harmonics|
|2|radar.wav|Radar|Classic iPhone-like radar ping|
|3|argon.wav|Argon|Long soft pad 880Hz|
|4|digital_alarm.wav|Digital Beep|1200Hz rapid beeps|
|5|morning_flower.wav|Morning Flower|Major arpeggio 523Hz|
|6|air_raid.wav|Air Raid|Siren sweep 600Hz|
|7|classic_bell.wav|Classic Bell|Bell with decay|
|8|military.wav|Military Reveille|March pattern|
|9|electronic.wav|Electronic Pulse|1400Hz staccato|
|10|extreme_alarm.wav|Extreme Loud|1500Hz urgent|
|11|harp.wav|Gentle Harp|659Hz harp-like|
|12|kettle.wav|Kettle|900Hz whistle|
|13|motivational_beat.wav|Motivational Beat|440Hz beat triple|
|14|inspiration.wav|Inspiration|550Hz uplifting|
|15|energizer.wav|Energizer|1100Hz double hit|
|16|sunrise.wav|Sunrise|400Hz warm|
|17|uplift.wav|Uplift|700Hz rising|
|18|workout_alarm.wav|Workout Alarm|1300Hz fast alternation|
|19|alarm_clock_old.wav|Old Clock|800Hz ticks|
|20|nuclear.wav|Nuclear Siren|500Hz slow siren|
|21|battle_alarm.wav|Battle Horn|300Hz horn|
|22|church_bell.wav|Church Bell|520Hz long bell|
|23|ocean_wave.wav|Ocean Wave + Alarm|250Hz low drone|
|24|bird_chirp.wav|Bird Chirp Loud|2000Hz chirps|
|25|gentle_wake.wav|Gentle Wake|432Hz zen|

## Replace with real royalty-free mp3s (optional)
Download from (all CC0/CC-BY):
- https://pixabay.com/sound-effects/search/alarm/
- https://mixkit.co/free-sound-effects/alarm/
- https://freesound.org/search/?q=alarm

Just overwrite the .wav keeping same names. Supported: wav/mp3/ogg. Keep <1MB each.

## Usage
RingtoneType enum in data/local/entity/AlarmEntity.kt maps resId -> R.raw.* automatically.
Custom user picker: RingtoneManager.kt handles both raw and content:// URIs.
