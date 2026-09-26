"""Upload a finished demo video to YouTube (YouTube Data API v3, OAuth desktop flow).

usage: python upload.py <video.mp4> [--dry-run] [--force]
       (run inside the video's work dir, after check_frames.py has passed)

Metadata comes from youtube.json in the work dir:
  {"title": "...",                       required, max 100 chars
   "description": "...",                 optional; default = the script.json narration
   "tags": ["HMIS", "Pharmacy"],         optional
   "privacy": "public",                  public | unlisted | private (default public)
   "language": "si",                     optional; sets default + audio language
   "playlistId": "PL...",                optional; video is appended to it
   "categoryId": "27"}                   optional; 27 = Education

Credentials live OUTSIDE the repo, in $YT_CREDENTIALS_DIR (default C:/Credentials/youtube):
  client_secret.json  Desktop OAuth client, downloaded once from Google Cloud Console
  token.json          written here after the first browser consent; delete it to re-consent

The result (video id + URL) is written to youtube-upload.json; its presence blocks a
second upload of the same video unless --force is given.
"""
import json, os, random, sys, time

sys.stdout.reconfigure(encoding='utf-8')     # Sinhala/Tamil metadata on a cp1252 console

args = [a for a in sys.argv[1:] if not a.startswith('--')]
flags = {a for a in sys.argv[1:] if a.startswith('--')}
if len(args) != 1:
    print(__doc__); sys.exit(2)
video = args[0]
DRY, FORCE = '--dry-run' in flags, '--force' in flags

CRED_DIR = os.environ.get('YT_CREDENTIALS_DIR', 'C:/Credentials/youtube')
SECRET, TOKEN = os.path.join(CRED_DIR, 'client_secret.json'), os.path.join(CRED_DIR, 'token.json')
SCOPES = ['https://www.googleapis.com/auth/youtube']   # upload + playlist insert
RESULT = 'youtube-upload.json'


def fail(msg):
    print('ERROR:', msg); sys.exit(1)


if not os.path.isfile(video):
    fail(f'{video} not found (run from the video work dir)')
if not os.path.isfile('youtube.json'):
    fail('youtube.json missing: create it with at least {"title": "..."}')
meta = json.load(open('youtube.json', encoding='utf-8'))
if not meta.get('title'):
    fail('youtube.json needs a "title"')
if len(meta['title']) > 100:
    fail(f'title is {len(meta["title"])} chars; YouTube allows 100')
privacy = meta.get('privacy', 'public')
if privacy not in ('public', 'unlisted', 'private'):
    fail(f'privacy "{privacy}" must be public, unlisted or private')

desc = meta.get('description')
if not desc and os.path.isfile('script.json'):
    desc = '\n\n'.join(s['say'] for s in json.load(open('script.json', encoding='utf-8')))
desc = desc or ''
if len(desc.encode('utf-8')) > 5000:
    fail('description is over 5000 bytes')
if any(c in desc + meta['title'] for c in '<>'):
    fail('YouTube rejects < and > in the title/description')

snippet = {'title': meta['title'], 'description': desc,
           'tags': meta.get('tags', []), 'categoryId': str(meta.get('categoryId', '27'))}
if meta.get('language'):
    snippet['defaultLanguage'] = snippet['defaultAudioLanguage'] = meta['language']
body = {'snippet': snippet,
        'status': {'privacyStatus': privacy, 'selfDeclaredMadeForKids': False}}

print(f'video      : {video} ({os.path.getsize(video) / 1e6:.1f} MB)')
print(json.dumps(body, ensure_ascii=False, indent=1))
if meta.get('playlistId'):
    print('playlist   :', meta['playlistId'])
if os.path.isfile(RESULT) and not FORCE:
    fail(f'{RESULT} exists: this video was already uploaded '
         f'({json.load(open(RESULT))["url"]}). Pass --force to upload a second copy.')
if DRY:
    print('dry run: nothing uploaded'); sys.exit(0)

# IPv6 routes to Google are black-holed on some office networks: every connect then hangs
# until WinError 10060. Resolve IPv4 only (YT_ALLOW_IPV6=1 to opt out).
import socket
if not os.environ.get('YT_ALLOW_IPV6'):
    _gai = socket.getaddrinfo
    socket.getaddrinfo = lambda host, *a, **k: [r for r in _gai(host, *a, **k)
                                                 if r[0] == socket.AF_INET] or _gai(host, *a, **k)
socket.setdefaulttimeout(120)

from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from googleapiclient.http import MediaFileUpload

if not os.path.isfile(SECRET):
    fail(f'{SECRET} missing: see "YouTube publishing setup" in the demo-video SKILL.md')
creds = Credentials.from_authorized_user_file(TOKEN, SCOPES) if os.path.isfile(TOKEN) else None
if creds and creds.expired and creds.refresh_token:
    try:
        creds.refresh(Request())
    except Exception as e:                      # Testing-mode refresh tokens die after 7 days
        print('token refresh failed, re-consenting:', e); creds = None
if not creds or not creds.valid:
    creds = InstalledAppFlow.from_client_secrets_file(SECRET, SCOPES).run_local_server(port=0)
    with open(TOKEN, 'w') as f:
        f.write(creds.to_json())
yt = build('youtube', 'v3', credentials=creds)

req = yt.videos().insert(part='snippet,status', body=body,
                         media_body=MediaFileUpload(video, mimetype='video/mp4',
                                                    chunksize=8 * 1024 * 1024, resumable=True))
print('uploading ...', flush=True)
resp, retry = None, 0
while resp is None:
    try:
        status, resp = req.next_chunk()
        if status:
            print(f'uploaded {status.progress() * 100:.0f}%')
    except HttpError as e:
        if e.resp.status not in (500, 502, 503, 504):
            if b'quotaExceeded' in e.content or b'uploadLimitExceeded' in e.content:
                fail('YouTube quota / upload limit hit; try again after midnight Pacific time')
            raise
        retry += 1
    except (ConnectionError, TimeoutError, OSError) as e:
        print('network error:', e); retry += 1
    else:
        continue
    if retry > 8:
        fail('giving up after 8 retries')
    wait = random.uniform(1, 2 ** retry)
    print(f'retry {retry} in {wait:.0f}s'); time.sleep(wait)

vid = resp['id']
url = f'https://youtu.be/{vid}'
got = resp['status']['privacyStatus']
json.dump({'id': vid, 'url': url, 'requestedPrivacy': privacy, 'privacy': got,
           'uploadedAt': time.strftime('%Y-%m-%dT%H:%M:%S')}, open(RESULT, 'w'), indent=1)
print('uploaded   :', url)
print('studio     :', f'https://studio.youtube.com/video/{vid}/edit')

if meta.get('playlistId'):
    yt.playlistItems().insert(part='snippet', body={'snippet': {
        'playlistId': meta['playlistId'],
        'resourceId': {'kind': 'youtube#video', 'videoId': vid}}}).execute()
    print('added to playlist', meta['playlistId'])

if got != privacy:
    print(f'\nWARNING: asked for "{privacy}" but YouTube set "{got}". Uploads from an unaudited '
          f'API project are locked to private. Change visibility by hand in Studio (link above), '
          f'or get the project audited: https://support.google.com/youtube/contact/yt_api_form')
