from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent / 'src/main/resources/assets/guaniao/textures/item'
SRC = ROOT / 'macaw_spawn_egg.png'
OUT = ROOT / 'umbrella_cockatoo_spawn_egg.png'

src = Image.open(SRC).convert('RGBA')
mask = src.load()
out = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
dst = out.load()

BODY = (247, 246, 241)
CREST = (247, 205, 72)
CREST_DEEP = (219, 168, 42)
EDGE = (176, 174, 170)

opaque = [(x, y) for y in range(32) for x in range(32) if mask[x, y][3] > 128]


def neighbours(x, y):
    return sum(1 for dx in (-1, 0, 1) for dy in (-1, 0, 1)
               if not (dx == 0 and dy == 0) and mask[x + dx, y + dy][3] > 128)


def crest_strength(x, y):
    if y < 3 or y > 8:
        return 0.0
    centre = 16.5
    span = 8.6 - abs(y - 5) * 0.55
    if abs(x - centre) > span:
        return 0.0
    edge = span - abs(x - centre)
    return min(1.0, edge / 1.6)


spots = {(11, 19), (12, 19), (11, 20), (21, 15), (22, 15), (22, 16)}

for x, y in opaque:
    depth = (y - 2) / 27.0
    factor = 1.0 - depth * 0.20
    col = tuple(int(c * factor) for c in BODY)

    ring = neighbours(x, y)
    if ring <= 4:
        col = EDGE

    cs = crest_strength(x, y)
    if cs > 0:
        base = CREST if ring > 4 else CREST_DEEP
        col = tuple(int(col[i] * (1 - cs) + base[i] * cs) for i in range(3))

    if (x, y) in spots and ring > 4:
        col = CREST_DEEP

    dst[x, y] = (col[0], col[1], col[2], 255)

out.save(OUT)
print('wrote', OUT, out.size, 'opaque', len(opaque))
