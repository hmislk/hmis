"""Mux the recorded webm with each narration clip placed at its step's timestamp -> MP4.

usage: python build.py <out.mp4>          (run inside the video's work dir, after flow.js)
"""
import json, subprocess, sys
import imageio_ffmpeg

out = sys.argv[1] if len(sys.argv) > 1 else 'demo.mp4'
ff = imageio_ffmpeg.get_ffmpeg_exe()
tl = json.load(open('timeline.json'))
marks = tl['marks']

cmd = [ff, '-y', '-ss', f"{tl['trim']:.2f}", '-t', f"{tl['total']:.2f}", '-i', tl['video']]
for m in marks:
    cmd += ['-i', f"audio/{m['id']}.mp3"]
parts, labels = [], []
for i, m in enumerate(marks, start=1):
    ms = int(m['t'] * 1000 + 250)          # small lead-in after the caption appears
    parts.append(f"[{i}:a]adelay={ms}|{ms}[a{i}]")
    labels.append(f"[a{i}]")
parts.append(f"{''.join(labels)}amix=inputs={len(marks)}:normalize=0,apad[aout]")
cmd += ['-filter_complex', ';'.join(parts), '-map', '0:v', '-map', '[aout]',
        '-c:v', 'libx264', '-preset', 'medium', '-crf', '20', '-pix_fmt', 'yuv420p', '-r', '25',
        '-c:a', 'aac', '-b:a', '160k', '-shortest', '-movflags', '+faststart', out]
r = subprocess.run(cmd, capture_output=True, text=True)
if r.returncode:
    print(r.stderr[-3000:]); sys.exit(1)
print('wrote', out)
