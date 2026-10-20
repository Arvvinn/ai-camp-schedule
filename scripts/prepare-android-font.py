"""Build offline UI fonts from Google's OFL Noto Sans SC (requires fonttools)."""
from pathlib import Path
from urllib.request import urlopen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset
import hashlib

root = Path(__file__).resolve().parents[1]
cache = root / 'artifacts/android/font-source'
cache.mkdir(parents=True, exist_ok=True)
base = 'https://raw.githubusercontent.com/google/fonts/main/ofl/notosanssc/'
source = cache / 'NotoSansSC.ttf'
if not source.exists():
    source.write_bytes(urlopen(base + 'NotoSansSC%5Bwght%5D.ttf', timeout=90).read())
source_hash = hashlib.sha256(source.read_bytes()).hexdigest()
if source_hash != 'a3041811a78c361b1de50f953c805e0244951c21c5bd412f7232ef0d899af0da':
    raise RuntimeError('The upstream font changed; review and update its pinned hash before regenerating.')
assets = root / 'android/app/src/main/assets'
license_text = urlopen(base + 'OFL.txt', timeout=30).read().decode('utf-8')
assets.joinpath('font-license.txt').write_text('\n'.join(line.rstrip() for line in license_text.splitlines()) + '\n', encoding='utf-8', newline='\n')
codepoints = set(range(0x20, 0x250)) | set(range(0x2000, 0x2070)) | set(range(0x3000, 0x3040)) | set(range(0xFF00, 0xFFEF))
# GB2312 common Chinese plus all shipped UI/SOP text; unknown glyphs use Android fallback.
for high in range(0xB0, 0xF8):
    for low in range(0xA1, 0xFF):
        try: codepoints.add(ord(bytes([high, low]).decode('gb2312')))
        except UnicodeDecodeError: pass
for path in [*root.glob('android/app/src/main/java/**/*.kt'), assets / 'schedule.json']:
    codepoints.update(map(ord, path.read_text(encoding='utf-8')))
out = root / 'android/app/src/main/res/font'
out.mkdir(parents=True, exist_ok=True)
for weight, label in [(400, 'regular'), (600, 'semibold')]:
    font = instantiateVariableFont(TTFont(source), {'wght': weight}, inplace=True)
    opts = subset.Options()
    opts.name_IDs = ['*']
    tool = subset.Subsetter(options=opts)
    tool.populate(unicodes=codepoints)
    tool.subset(font)
    for name in font['name'].names:
        replacement = {1:'Schedule Sans SC', 2:label.title(), 4:'Schedule Sans SC '+label.title(),
                       6:'ScheduleSansSC-'+label.title(), 16:'Schedule Sans SC', 17:label.title()}.get(name.nameID)
        if replacement: name.string = replacement.encode(name.getEncoding())
    target = out / ('schedule_sans_' + label + '.ttf')
    font.save(target)
    print(target.name, target.stat().st_size)
print('Source SHA256:', hashlib.sha256(source.read_bytes()).hexdigest())
