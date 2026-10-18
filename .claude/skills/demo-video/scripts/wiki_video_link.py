"""Add or replace a video in the wiki's Video Library, and in the written article for the same topic.

usage: python wiki_video_link.py --module "Laboratory (LIMS)" --topic "Set up a two-page lab report" \
           --url https://youtu.be/XXXX --lang English --audience "Lab staff" --length "about 5 min" \
           [--article LIMS-Two-Page-Lab-Reports] [--date 2026-10-18] [--wiki ../hmis.wiki] [--dry-run]

Row key = topic + language. A row with the same key is REPLACED (new link, new Updated date),
so the library never lists two versions of one topic. Otherwise a row is added under the
module heading (a new heading is created if the module has none yet).

With --article, the article gets the same link: the old video URL (read from the replaced row)
is swapped for the new one everywhere in the article; when the article has no video link yet,
a "**Video:**" line is inserted under its title.

It only edits files. Commit and push the wiki yourself afterwards, in one commit.
"""
import argparse, datetime, os, re, sys

sys.stdout.reconfigure(encoding='utf-8')

YT_URL = r'https?://(?:www\.)?(?:youtu\.be/|youtube\.com/(?:watch\?v=|shorts/|live/))'


def vid(u):
    """The 11-character video id of a YouTube link, or None."""
    m = re.search(YT_URL + r'([\w-]{11})', u or '')
    return m.group(1) if m else None


ap = argparse.ArgumentParser()
ap.add_argument('--module', required=True, help='heading in Video-Library.md, e.g. "Laboratory (LIMS)"')
ap.add_argument('--topic', required=True)
ap.add_argument('--url', required=True)
ap.add_argument('--lang', default='English')
ap.add_argument('--audience', default='')
ap.add_argument('--length', default='')
ap.add_argument('--article', default='', help='wiki article file name without .md')
ap.add_argument('--date', default=datetime.date.today().isoformat())
ap.add_argument('--wiki', default=os.environ.get('HMIS_WIKI', '../hmis.wiki'))
ap.add_argument('--dry-run', action='store_true')
a = ap.parse_args()
video_id = vid(a.url)
if video_id is None:
    ap.error(f'--url does not look like a YouTube video link: {a.url}')

lib_path = os.path.join(a.wiki, 'Video-Library.md')
raw = open(lib_path, encoding='utf-8', newline='').read()
nl = '\r\n' if '\r\n' in raw else '\n'
lines = raw.replace('\r\n', '\n').split('\n')

HEADER = '| Topic | Video | Language | Audience | Written article | Updated |'
SEP = '|---|---|---|---|---|---|'
video_cell = f'[Watch]({a.url})' + (f' ({a.length})' if a.length else '')
article_cell = f'[{a.article.replace("-", " ")}]({a.article})' if a.article else '—'
row = f'| {a.topic} | {video_cell} | {a.lang} | {a.audience} | {article_cell} | {a.date} |'


def cells(line):
    return [c.strip() for c in line.strip().strip('|').split('|')]


old_url = None
heading = f'## {a.module}'
if heading in lines:
    start = lines.index(heading)
    end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith('## ') or lines[i] == '---'), len(lines))
    section = range(start + 1, end)
    hit = None
    for i in section:
        if lines[i].startswith('|'):
            c = cells(lines[i])
            if len(c) >= 3 and c[0] == a.topic and c[2] == a.lang:
                hit = i
    if hit is not None:
        m = re.search(r'\((https?://[^)\s]+)\)', cells(lines[hit])[1])
        old_url = m.group(1) if m else None
        # keep the old article link and audience when the caller did not give them
        old = cells(lines[hit])
        if not a.article and len(old) > 4:
            row = row.replace(f'| {article_cell} |', f'| {old[4]} |')
        if not a.audience and len(old) > 3:
            row = row.replace(f'| {a.lang} |  |', f'| {a.lang} | {old[3]} |')
        lines[hit] = row
        action = 'replaced'
    else:
        table = [i for i in section if lines[i].startswith('|')]
        if table:
            lines.insert(table[-1] + 1, row)
        else:   # "No videos yet." section: start a table
            body = [i for i in section if lines[i].strip().lower().startswith('no videos yet')]   # only the placeholder, not other text
            for i in reversed(body):
                del lines[i]
            lines[start + 1:start + 1] = ['', HEADER, SEP, row]
        action = 'added'
else:
    marker = next((i for i, l in enumerate(lines) if l.startswith('## How this page is kept')), len(lines))
    rule = max((i for i in range(marker) if lines[i] == '---'), default=marker)   # the '---' before the maintenance note
    lines[rule:rule] = [heading, '', HEADER, SEP, row, '']
    action = 'added under a new heading'

out = nl.join(lines)
print(f'Video-Library.md: {action}: {a.topic} ({a.lang})')
if old_url and old_url != a.url:
    print('  old link:', old_url)

art_msg = ''
art_path = os.path.join(a.wiki, a.article + '.md') if a.article else ''
art_out = None
if art_path:
    if not os.path.exists(art_path):
        art_msg = f'article {a.article}.md not found: nothing changed there'
    else:
        t = open(art_path, encoding='utf-8', newline='').read()
        anl = '\r\n' if '\r\n' in t else '\n'
        old_id = vid(old_url)
        if old_id and old_id in t:
            # swap the video id and drop tracking parameters that belonged to the old link
            t2 = re.sub('(' + YT_URL + re.escape(old_id) + r')(\?si=[\w-]+|&si=[\w-]+)?', a.url, t)
            art_msg = f'article {a.article}.md: old link replaced'
        elif video_id in t:
            t2 = t
            art_msg = f'article {a.article}.md: already links this video'
        elif re.search(r'youtu(?:\.be|be\.com)/', t):
            # another video (maybe another language's): never add a second line or overwrite it
            t2 = t
            art_msg = f'article {a.article}.md: already links a different video; left unchanged, edit it by hand if it is the old version of this one'
        else:
            ls = t.split(anl)
            at = next((i for i, line in enumerate(ls) if line.startswith('# ')), -1) + 1
            ls[at:at] = ['', f'**Video:** [{a.topic}]({a.url})' + (f' ({a.length}). ' if a.length else '. ') + 'More videos are in the [Video Library](Video-Library).']
            t2 = anl.join(ls)
            art_msg = f'article {a.article}.md: Video line added'
        art_out = t2
    print(' ', art_msg)

if a.dry_run:
    print('dry run: nothing written'); sys.exit(0)
open(lib_path, 'w', encoding='utf-8', newline='').write(out)
if art_out is not None:
    open(art_path, 'w', encoding='utf-8', newline='').write(art_out)
print('written. Commit and push the wiki.')
