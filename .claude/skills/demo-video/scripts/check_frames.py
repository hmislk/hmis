"""Grab one frame per step (at 80% of its narration) and tile them into frames/sheet.png.
Read the sheet: every caption must match what is on screen at that moment.

usage: python check_frames.py <video.mp4> [fraction]
"""
import json, os, subprocess, sys
import imageio_ffmpeg

video = sys.argv[1]
frac = float(sys.argv[2]) if len(sys.argv) > 2 else 0.8
ff = imageio_ffmpeg.get_ffmpeg_exe()
tl = json.load(open('timeline.json'))
dur = json.load(open('audio/durations.json'))
os.makedirs('frames', exist_ok=True)
w, h = tl.get('W', 1600) // 2, tl.get('H', 900) // 2
files = []
for i, m in enumerate(tl['marks']):
    f = f"frames/{i:02d}_{m['id']}.png"
    t = m['t'] + dur[m['id']] * frac
    if os.path.exists(f):
        os.remove(f)           # never let a previous run's frame stand in for this one
    res = subprocess.run([ff, '-y', '-ss', f'{t:.2f}', '-i', video, '-frames:v', '1', '-vf', f'scale={w}:{h}', f], capture_output=True)
    if res.returncode == 0 and os.path.exists(f):
        files.append(f)
    else:
        print('no frame for step', m['id'], f'(t={t:.1f}s is past the end of the video?)')
if not files:
    sys.exit('no frames extracted')
if len(files) == 1:            # xstack needs 2+ inputs
    import shutil
    shutil.copyfile(files[0], 'frames/sheet.png'); print('frames/sheet.png'); sys.exit(0)
args = [ff, '-y']
for f in files:
    args += ['-i', f]
n = len(files)
layout = '|'.join(f'{(i % 3) * w}_{(i // 3) * h}' for i in range(n))
args += ['-filter_complex', ''.join(f'[{i}:v]' for i in range(n)) + f'xstack=inputs={n}:layout={layout}:fill=white', 'frames/sheet.png']
r = subprocess.run(args, capture_output=True, text=True)
if r.returncode:
    sys.exit(r.stderr[-1500:])
print('frames/sheet.png')
