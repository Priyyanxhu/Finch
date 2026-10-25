import wave, struct, math, os

OUT = r"D:\Doodle\app\src\main\res\raw"
os.makedirs(OUT, exist_ok=True)

SR = 44100
DURATION = 4.0  # 4 seconds loopable

# 25 distinct alarm profiles: (name, baseFreq, pattern)
# pattern: list of (freq_mult, duration, gap, waveform)
TONES = [
    ("rooster", 800, [(1,0.15,0.05),(1.5,0.15,0.05),(2,0.3,0.2)]*3),
    ("radar", 1000, [(1,0.2,0.3)]*6),
    ("argon", 880, [(1,0.5,0.5)]*4),
    ("digital_alarm", 1200, [(1,0.1,0.1)]*12),
    ("morning_flower", 523, [(1,0.4,0.1),(1.25,0.4,0.1),(1.5,0.6,0.2)]*2),
    ("air_raid", 600, [(1,1.0,0.3),(0.7,1.0,0.3)]*2), # rising/falling siren via freq slide
    ("classic_bell", 1000, [(1,0.8,0.4)]*3),
    ("military", 700, [(1,0.2,0.1),(1,0.2,0.1),(1,0.5,0.4)]*2),
    ("electronic", 1400, [(1,0.08,0.08)]*16),
    ("extreme_alarm", 1500, [(1,0.12,0.05)]*20),
    ("harp", 659, [(1,0.6,0.2),(1.5,0.6,0.2),(2,0.8,0.3)]*1),
    ("kettle", 900, [(1,0.3,0.1)]*8),
    ("motivational_beat", 440, [(1,0.2,0.1),(1.5,0.2,0.1),(2,0.2,0.4)]*3),
    ("inspiration", 550, [(1,0.5,0.2),(1.2,0.5,0.2),(1.5,0.7,0.3)]*2),
    ("energizer", 1100, [(1,0.15,0.05),(1,0.15,0.4)]*6),
    ("sunrise", 400, [(1,0.6,0.3),(1.3,0.6,0.3),(1.6,0.8,0.3)]*2),
    ("uplift", 700, [(1,0.3,0.1),(1.4,0.3,0.1),(1.8,0.4,0.3)]*3),
    ("workout_alarm", 1300, [(1,0.1,0.05),(0.9,0.1,0.05)]*16),
    ("alarm_clock_old", 800, [(1,0.05,0.05)]*24),
    ("nuclear", 500, [(1,0.8,0.2),(0.6,0.8,0.2)]*2),
    ("battle_alarm", 300, [(1,0.4,0.2),(1.5,0.4,0.2)]*4),
    ("church_bell", 520, [(1,1.0,0.5)]*2 + [(0.7,1.0,0.5)]*1),
    ("ocean_wave", 250, [(1,0.7,0.3)]*3), # low + filtered
    ("bird_chirp", 2000, [(1,0.07,0.07),(1.3,0.07,0.07),(1.6,0.07,0.2)]*5),
    ("gentle_wake", 432, [(1,0.8,0.4)]*3),
]

def gen_tone(path, freq, pattern):
    n = int(SR * DURATION)
    buf = [0]*n
    t = 0
    # Build pattern looping to fill DURATION
    seg_idx = 0
    pos = 0
    while pos < n:
        f_mult, dur, gap = pattern[seg_idx % len(pattern)]
        f = freq * f_mult
        # Special sliding siren for air_raid/nuclear: sweep
        sweep = f_mult==1 and freq in (600,500)
        tone_len = int(SR*dur)
        for i in range(tone_len):
            if pos+i >= n: break
            fi = f * (1 + 0.3*math.sin(2*math.pi*2*i/SR)) if "rooster" in path else f
            if sweep:
                fi = f * (1 + 0.4*math.sin(2*math.pi*0.5*i/SR))
            # envelope to avoid clicks
            env = min(1.0, i/200, (tone_len-i)/200)
            # add harmonics for richness
            v = math.sin(2*math.pi*fi*i/SR) * 0.6
            v += 0.3*math.sin(2*math.pi*fi*2*i/SR)
            v += 0.1*math.sin(2*math.pi*fi*3*i/SR)
            buf[pos+i] = int(max(-1,min(1,v*env)) * 16000)
        pos += tone_len
        gap_len = int(SR*gap)
        pos += gap_len
        seg_idx+=1

    # Normalize & write wav (mono 16-bit)
    with wave.open(path, "w") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-32767,min(32767,s)))) for s in buf))

for name, freq, pat in TONES:
    gen_tone(os.path.join(OUT, f"{name}.wav"), freq, pat)
    print(f"generated {name}.wav")

print(f"Done: {len(TONES)} wavs in {OUT}")
