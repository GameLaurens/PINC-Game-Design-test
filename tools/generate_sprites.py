"""Create the project's original pixel-art PNGs. Python stdlib only; no drawing UI.
8/16 are hand-authored palettes; 32 adds detail instead of only enlarging pixels.
Run: python tools/generate_sprites.py
"""
from pathlib import Path
import struct
import zlib

PALETTE = {
    '.': (0, 0, 0, 0), 'h': (57, 35, 47, 255), 'H': (112, 62, 55, 255),
    's': (246, 187, 139, 255), 'S': (207, 123, 102, 255), 'e': (24, 32, 48, 255),
    'c': (35, 177, 181, 255), 'C': (20, 111, 139, 255), 'l': (122, 239, 211, 255),
    'g': (255, 202, 99, 255), 'p': (57, 70, 116, 255), 'P': (35, 44, 77, 255),
    'b': (40, 31, 47, 255), 'w': (210, 230, 225, 255), 't': (0, 0, 0, 65),
}
ART8 = [
    '..hhhh..', '..hssh..', '..sees..', '...ss...',
    '.sccccs.', '..cppc..', '..p..p..', '.bb..bb.',
]
ART16 = [
    '.....hhhhhh.....', '....hhHHHHhh....', '....hssssssh....', '....hseSeshh....',
    '.....ssssss.....', '......SssS......', '....CclgglcC....', '...sCcllllcCs...',
    '...sCccccccCs...', '...sCccccccCs...', '....CCggggCC....', '.....ppPPpp.....',
    '.....pp..pp.....', '.....pP..Pp.....', '....bbb..bbb....', '....bwb..bwb....',
]

def chunk(kind, data):
    return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data) & 0xffffffff)

def png(path, pixels):
    h, w = len(pixels), len(pixels[0])
    raw = b''.join(b'\0' + bytes(channel for rgba in row for channel in rgba) for row in pixels)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', w, h, 8, 6, 0, 0, 0))
    path.write_bytes(data + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b''))

def main():
    root = Path(__file__).resolve().parents[1] / 'src/main/resources/sprites'
    root.mkdir(parents=True, exist_ok=True)
    for size, art in [(8, ART8), (16, ART16)]:
        assert all(len(row) == size for row in art)
        png(root / f'hero-{size}.png', [[PALETTE[p] for p in row] for row in art])
    art = [[ART16[y // 2][x // 2] for x in range(32)] for y in range(32)]
    # Hair strands, eye glints, jacket piping, zipper and sole details.
    for x, y, color in [(11, 2, 'g'), (15, 3, 'H'), (19, 2, 'H'), (13, 6, 'w'),
                        (17, 6, 'w'), (14, 9, 'S'), (17, 9, 'S')]:
        art[y][x] = color
    for y in range(14, 21):
        art[y][15] = 'g' if y % 2 == 0 else 'C'
        art[y][10] = 'l'
        art[y][21] = 'C'
    for x in range(8, 14): art[31][x] = 'w'
    for x in range(18, 24): art[31][x] = 'w'
    # Semi-transparent contact shadow provides an alpha-compositing example.
    for x in range(5, 27):
        if art[31][x] == '.': art[31][x] = 't'
    png(root / 'hero-32.png', [[PALETTE[p] for p in row] for row in art])
    print('Geschreven: hero-8.png, hero-16.png, hero-32.png')

if __name__ == '__main__':
    main()
