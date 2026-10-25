"""Generate one narration clip per step of script.json and record each clip's duration.

usage: python tts.py [voice] [rate]      (run inside the video's work dir)
voices: en-GB-SoniaNeural (default), en-IN-NeerjaNeural, si-LK-ThiliniNeural,
        si-LK-SameeraNeural, ta-LK-SaranyaNeural, ta-LK-KumarNeural
        full list: python -m edge_tts --list-voices
"""
import json, os, re, subprocess, sys
import imageio_ffmpeg

voice = sys.argv[1] if len(sys.argv) > 1 else 'en-GB-SoniaNeural'
rate = sys.argv[2] if len(sys.argv) > 2 else '-5%'
ff = imageio_ffmpeg.get_ffmpeg_exe()
steps = json.load(open('script.json', encoding='utf-8'))
os.makedirs('audio', exist_ok=True)
out = {}
for s in steps:
    mp3 = f"audio/{s['id']}.mp3"
    subprocess.run([sys.executable, '-m', 'edge_tts', '--voice', voice, f'--rate={rate}',
                    '--text', s['say'], '--write-media', mp3], check=True, capture_output=True)
    err = subprocess.run([ff, '-i', mp3], capture_output=True, text=True).stderr
    h, m, sec = re.search(r'Duration: (\d+):(\d+):([\d.]+)', err).groups()
    out[s['id']] = round(int(h) * 3600 + int(m) * 60 + float(sec), 2)
json.dump(out, open('audio/durations.json', 'w'), indent=1)
print(f"{len(out)} clips, {sum(out.values()):.1f}s narration, voice {voice}")
