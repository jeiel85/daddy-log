#!/usr/bin/env python3
"""Generate the Daddy Log (두피케어기록) app icon set.

One sprout design -> adaptive foreground PNGs, legacy launcher icons,
Play Store icon, and web/landing favicons. Brand palette matches the app's
forest-green Compose theme (Color.kt): #28A06B .. #1B6A47.

Run:  python scripts/gen_icon.py
"""
import math
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
DOCS = os.path.join(ROOT, "docs", "assets")
STORE = os.path.join(ROOT, "store-graphics")

SS = 4  # supersample factor for crisp anti-aliasing

# Brand palette (top -> bottom gradient)
GREEN_TOP = (43, 170, 114)     # #2BAA72
GREEN_BOT = (20, 89, 58)       # #14593A
WHITE = (255, 255, 255, 255)
MINT = (214, 242, 230, 255)    # #D6EAE0 soft mint accent


def gradient_bg(size, radius_frac=0.0):
    """Vertical green gradient square. radius_frac>0 rounds the corners."""
    s = size * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    px = img.load()
    for y in range(s):
        t = y / (s - 1)
        # ease for a softer transition
        te = t * t * (3 - 2 * t)
        r = int(GREEN_TOP[0] + (GREEN_BOT[0] - GREEN_TOP[0]) * te)
        g = int(GREEN_TOP[1] + (GREEN_BOT[1] - GREEN_TOP[1]) * te)
        b = int(GREEN_TOP[2] + (GREEN_BOT[2] - GREEN_TOP[2]) * te)
        for x in range(s):
            px[x, y] = (r, g, b, 255)
    if radius_frac > 0:
        rad = int(s * radius_frac)
        mask = Image.new("L", (s, s), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, s - 1, s - 1], radius=rad, fill=255)
        img.putalpha(mask)
    return img


def circle_bg(size):
    s = size * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    grad = gradient_bg(size)
    mask = Image.new("L", (s, s), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, s - 1, s - 1], fill=255)
    img.paste(grad, (0, 0), mask)
    return img


def _leaf(length, width, color):
    """A single leaf (pointed ellipse) with a subtle center vein, upright."""
    pad = 6 * SS
    w, h = width + pad * 2, length + pad * 2
    leaf = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(leaf)
    cx = w / 2
    # pointed-oval leaf via two arcs -> approximate with polygon of an almond shape
    pts = []
    n = 60
    for i in range(n + 1):
        t = i / n
        y = pad + t * length
        # half-width follows sin profile, tapered to points at both ends
        hw = (width / 2) * math.sin(math.pi * t)
        pts.append((cx - hw, y))
    for i in range(n + 1):
        t = 1 - i / n
        y = pad + t * length
        hw = (width / 2) * math.sin(math.pi * t)
        pts.append((cx + hw, y))
    d.polygon(pts, fill=color)
    # center vein (slightly translucent gap for refinement)
    d.line([(cx, pad + length * 0.10), (cx, pad + length * 0.90)],
           fill=(0, 0, 0, 0), width=max(2, int(2.2 * SS)))
    return leaf


def draw_sprout(canvas_size, content_frac):
    """Draw a white sprout centered, fitting within content_frac of canvas."""
    s = canvas_size * SS
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = s / 2
    box = s * content_frac
    top = (s - box) / 2
    bot = top + box

    # --- soil / log baseline: a rounded horizontal bar (record hint) ---
    base_y = top + box * 0.86
    bar_w = box * 0.46
    bar_h = box * 0.085
    d.rounded_rectangle(
        [cx - bar_w / 2, base_y - bar_h / 2, cx + bar_w / 2, base_y + bar_h / 2],
        radius=bar_h / 2, fill=WHITE,
    )

    # --- stem: gentle curve from base up to where leaves meet ---
    stem_bottom = base_y - bar_h * 0.2
    stem_top = top + box * 0.34
    stem_w = max(3, int(box * 0.058))
    seg = []
    steps = 40
    for i in range(steps + 1):
        t = i / steps
        y = stem_bottom + (stem_top - stem_bottom) * t
        x = cx + math.sin(t * math.pi) * box * 0.012  # barely-there curve
        seg.append((x, y))
    d.line(seg, fill=WHITE, width=stem_w, joint="curve")
    # round the stem cap
    r = stem_w / 2
    d.ellipse([cx - r, stem_top - r, cx + r, stem_top + r], fill=WHITE)

    # --- two leaves sprouting from the top of the stem ---
    leaf_len = int(box * 0.50)
    leaf_w = int(box * 0.255)
    attach = (cx, stem_top + box * 0.02)

    # right leaf (slightly larger) angled up-right
    rl = _leaf(leaf_len, leaf_w, WHITE).rotate(-48, expand=True, resample=Image.BICUBIC)
    rx = int(attach[0] - rl.width * 0.16)
    ry = int(attach[1] - rl.height * 0.78)
    img.alpha_composite(rl, (rx, ry))

    # left leaf, mint-tinted for subtle depth, angled up-left
    ll = _leaf(int(leaf_len * 0.92), int(leaf_w * 0.95), MINT).rotate(48, expand=True, resample=Image.BICUBIC)
    lx = int(attach[0] - ll.width * 0.84)
    ly = int(attach[1] - ll.height * 0.74)
    img.alpha_composite(ll, (lx, ly))

    return img


def adaptive_foreground(size):
    """Full 108dp canvas, sprout within the central safe zone, transparent bg."""
    fg = draw_sprout(size, content_frac=0.62)
    return fg.resize((size, size), Image.LANCZOS)


def baked_icon(size, shape):
    s = size * SS
    if shape == "round":
        base = circle_bg(size)
        frac = 0.66
    else:  # squircle
        base = gradient_bg(size, radius_frac=0.22)
        frac = 0.70
    sprout = draw_sprout(size, content_frac=frac)
    base.alpha_composite(sprout)
    return base.resize((size, size), Image.LANCZOS)


def store_icon(size, rounded):
    s = size * SS
    base = gradient_bg(size, radius_frac=0.0 if not rounded else 0.22)
    base.alpha_composite(draw_sprout(size, content_frac=0.66))
    return base.resize((size, size), Image.LANCZOS)


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


def main():
    # 1) adaptive foreground PNGs per density (108dp base)
    fg_densities = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
    for d, px in fg_densities.items():
        save(adaptive_foreground(px), os.path.join(RES, f"mipmap-{d}", "ic_launcher_foreground.png"))

    # 2) legacy launcher icons (48dp base) as webp, square + round
    legacy = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for d, px in legacy.items():
        save(baked_icon(px, "squircle"), os.path.join(RES, f"mipmap-{d}", "ic_launcher.webp"))
        save(baked_icon(px, "round"), os.path.join(RES, f"mipmap-{d}", "ic_launcher_round.webp"))

    # 3) Play Store hi-res icon (full square, corners filled)
    save(store_icon(512, rounded=False), os.path.join(STORE, "icon-512.png"))

    # 4) web / landing assets (rounded squircle look)
    save(store_icon(512, rounded=True), os.path.join(DOCS, "app-icon.png"))
    save(store_icon(180, rounded=True), os.path.join(DOCS, "apple-touch-icon.png"))
    save(store_icon(48, rounded=True), os.path.join(DOCS, "favicon-48.png"))
    save(store_icon(32, rounded=True), os.path.join(DOCS, "favicon.png"))


if __name__ == "__main__":
    main()
