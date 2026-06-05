"""
Generate modern minimalist tabBar icons for the mini-program.

Design language:
  - Clean line icons with consistent 2px stroke weight
  - Rounded line caps and joins for a softer, modern feel
  - Minimal detail, maximum clarity
  - Inspired by iOS SF Symbols and Material Design icons
"""
import os
from PIL import Image, ImageDraw

# ── Configuration ────────────────────────────────────────────
SCALE = 4                           # supersample factor
FINAL = 81                          # output size (px)
CS    = FINAL * SCALE               # canvas = 324px

PAD   = 18 * SCALE                  # edge padding
SW    = 3 * SCALE                   # stroke width (~12px @4x, crisp 3px final)

NORMAL_COLOR = (156, 163, 175, 255)   # #9CA3AF – modern gray
ACTIVE_COLOR = (107,  66,  38, 255)   # #6B4226 – theme brown

HERE = os.path.dirname(os.path.abspath(__file__))


# ── Helpers ──────────────────────────────────────────────────
def _new():
    """Create a transparent RGBA canvas and its draw context."""
    img = Image.new('RGBA', (CS, CS), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def _save(img, filename):
    """Downscale to final size (anti-alias) and save."""
    out = img.resize((FINAL, FINAL), Image.LANCZOS)
    out.save(os.path.join(HERE, filename))
    print(f'  [ok] {filename}')


# ── Icon Drawings ────────────────────────────────────────────

def icon_book(d, c):
    """
    Minimalist open book – simple V-shape spine with two page outlines.
    Clean and iconic.
    """
    cx = CS // 2
    top = PAD
    bot = CS - PAD
    margin = 8 * SCALE
    
    # Left page - simple rectangle
    d.rounded_rectangle(
        [PAD, top + margin, cx - 4 * SCALE, bot],
        radius=4 * SCALE,
        outline=c,
        width=SW
    )
    
    # Right page - simple rectangle  
    d.rounded_rectangle(
        [cx + 4 * SCALE, top + margin, CS - PAD, bot],
        radius=4 * SCALE,
        outline=c,
        width=SW
    )
    
    # Center spine line
    d.line([(cx, top), (cx, bot)], fill=c, width=SW)


def icon_note(d, c):
    """
    Minimalist note/document – rounded rectangle with simple lines.
    """
    left = PAD + 8 * SCALE
    right = CS - PAD - 8 * SCALE
    top = PAD
    bot = CS - PAD
    
    # Main outline
    d.rounded_rectangle(
        [left, top, right, bot],
        radius=6 * SCALE,
        outline=c,
        width=SW
    )
    
    # Simple horizontal lines representing text
    line_left = left + 12 * SCALE
    line_right = right - 12 * SCALE
    
    for ratio in (0.35, 0.50, 0.65):
        y = int(top + (bot - top) * ratio)
        d.line([(line_left, y), (line_right, y)], fill=c, width=SW)


def icon_chart(d, c):
    """
    Minimalist bar chart – three clean vertical bars.
    """
    bot = CS - PAD
    top = PAD
    bar_w = 14 * SCALE
    gap = 12 * SCALE
    total = 3 * bar_w + 2 * gap
    start = (CS - total) // 2
    
    heights = (0.45, 0.75, 0.55)
    max_h = bot - top
    
    for i, h_ratio in enumerate(heights):
        x = start + i * (bar_w + gap)
        h = int(max_h * h_ratio)
        d.rounded_rectangle(
            [x, bot - h, x + bar_w, bot],
            radius=4 * SCALE,
            outline=c,
            width=SW
        )


def icon_user(d, c):
    """
    Minimalist user avatar – circle head + curved shoulders.
    """
    cx = CS // 2
    
    # Head circle
    head_r = 12 * SCALE
    head_cy = PAD + head_r + 2 * SCALE
    d.ellipse(
        [cx - head_r, head_cy - head_r, cx + head_r, head_cy + head_r],
        outline=c,
        width=SW
    )
    
    # Body arc (shoulders)
    body_top = head_cy + head_r + 8 * SCALE
    body_w = 24 * SCALE
    body_h = 40 * SCALE
    d.arc(
        [cx - body_w, body_top, cx + body_w, body_top + body_h],
        180, 0,
        fill=c,
        width=SW
    )


# ── Generate ─────────────────────────────────────────────────
ICONS = [
    ('icon-book',  icon_book),
    ('icon-note',  icon_note),
    ('icon-chart', icon_chart),
    ('icon-user',  icon_user),
]

if __name__ == '__main__':
    print('Generating modern minimalist tabBar icons ...')
    for name, draw_fn in ICONS:
        for suffix, color in [('', NORMAL_COLOR), ('-active', ACTIVE_COLOR)]:
            img, draw = _new()
            draw_fn(draw, color)
            _save(img, f'{name}{suffix}.png')
    
    print('\nAll icons generated successfully!')
