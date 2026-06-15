#!/usr/bin/env python3
"""Generate Daddy Log store + landing graphics (feature graphic, OG card).

Flat, on-brand design built from the same sprout mark as the app icon.
Run:  python scripts/gen_graphics.py   (after gen_icon.py)
"""
import os
from PIL import Image, ImageDraw, ImageFont

import gen_icon as gi

ROOT = gi.ROOT
DOCS = gi.DOCS
STORE = gi.STORE
SS = gi.SS

FONT_BOLD = r"C:/Windows/Fonts/malgunbd.ttf"
FONT_REG = r"C:/Windows/Fonts/malgun.ttf"
FONT_SL = r"C:/Windows/Fonts/malgunsl.ttf"

TITLE = "두피케어기록"
TAGLINE = "두피와 모발을 매일 차분히 기록하고, 변화를 비교하세요"
CHIP = "로컬 전용  ·  개인 맞춤 기록  ·  카메라 각도별 사진"
MINT = (214, 242, 230)
WHITE = (255, 255, 255)


def gradient_rect(w, h):
    """Vertical brand gradient at supersampled resolution."""
    s_w, s_h = w * SS, h * SS
    img = Image.new("RGB", (s_w, s_h))
    px = img.load()
    top, bot = gi.GREEN_TOP, gi.GREEN_BOT
    for y in range(s_h):
        t = y / (s_h - 1)
        te = t * t * (3 - 2 * t)
        r = int(top[0] + (bot[0] - top[0]) * te)
        g = int(top[1] + (bot[1] - top[1]) * te)
        b = int(top[2] + (bot[2] - top[2]) * te)
        for x in range(s_w):
            px[x, y] = (r, g, b)
    return img.convert("RGBA")


def radial_glow(base, cx, cy, radius, color=(255, 255, 255), max_alpha=46):
    """Soft radial highlight to lift the area behind the logo."""
    s = base.size
    glow = Image.new("RGBA", s, (0, 0, 0, 0))
    d = ImageDraw.Draw(glow)
    steps = 60
    for i in range(steps, 0, -1):
        rr = radius * i / steps
        a = int(max_alpha * (1 - i / steps))
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], fill=color + (a,))
    base.alpha_composite(glow)


def deco_leaves(base):
    """Large faint sprout bleeding off the bottom-right corner."""
    s = base.size[0] // SS
    sprout = gi.draw_sprout(int(s * 1.1), content_frac=0.95)
    # tint to translucent white
    alpha = sprout.split()[3].point(lambda a: int(a * 0.10))
    faint = Image.new("RGBA", sprout.size, (255, 255, 255, 0))
    faint.putalpha(alpha)
    faint = Image.composite(Image.new("RGBA", sprout.size, (255, 255, 255, 255)),
                            Image.new("RGBA", sprout.size, (255, 255, 255, 0)), alpha)
    base.alpha_composite(faint, (int(base.size[0] - faint.size[0] * 0.62),
                                 int(base.size[1] - faint.size[1] * 0.58)))


def font(path, size):
    return ImageFont.truetype(path, size * SS)


def build(w, h, icon_px, title_px, tag_px, show_chip, chip_px, layout="left"):
    base = gradient_rect(w, h)
    sw, shh = base.size

    deco_leaves(base)

    icon = gi.store_icon(icon_px, rounded=True).resize((icon_px * SS, icon_px * SS), Image.LANCZOS)

    if layout == "left":
        ix = int(sw * 0.07)
        iy = (shh - icon.size[1]) // 2
        radial_glow(base, ix + icon.size[0] // 2, iy + icon.size[1] // 2, icon.size[0] * 0.95)
        base.alpha_composite(icon, (ix, iy))
        tx = ix + icon.size[0] + int(sw * 0.05)
        d = ImageDraw.Draw(base)
        f_title = font(FONT_BOLD, title_px)
        f_tag = font(FONT_SL, tag_px)
        # vertically center the text block
        title_bb = d.textbbox((0, 0), TITLE, font=f_title)
        th = title_bb[3] - title_bb[1]
        block_h = th + int(tag_px * SS * 1.9) + (int(chip_px * SS * 2.6) if show_chip else 0)
        ty = (shh - block_h) // 2
        d.text((tx, ty), TITLE, font=f_title, fill=WHITE)
        ty2 = ty + th + int(tag_px * SS * 0.9)
        d.text((tx, ty2), TAGLINE, font=f_tag, fill=MINT)
        if show_chip:
            cy = ty2 + int(tag_px * SS * 1.7)
            f_chip = font(FONT_REG, chip_px)
            cbb = d.textbbox((0, 0), CHIP, font=f_chip)
            pad = int(14 * SS)
            d.rounded_rectangle(
                [tx - pad // 2, cy - pad // 2,
                 tx + (cbb[2] - cbb[0]) + pad, cy + (cbb[3] - cbb[1]) + pad + int(6 * SS)],
                radius=int(18 * SS), fill=(9, 58, 38, 150))
            d.text((tx, cy), CHIP, font=f_chip, fill=MINT)
    else:  # centered (OG)
        d = ImageDraw.Draw(base)
        iy = int(shh * 0.16)
        ix = (sw - icon.size[0]) // 2
        radial_glow(base, sw // 2, iy + icon.size[1] // 2, icon.size[0] * 1.1)
        base.alpha_composite(icon, (ix, iy))
        f_title = font(FONT_BOLD, title_px)
        f_tag = font(FONT_SL, tag_px)
        ty = iy + icon.size[1] + int(shh * 0.06)
        tbb = d.textbbox((0, 0), TITLE, font=f_title)
        d.text(((sw - (tbb[2] - tbb[0])) // 2, ty), TITLE, font=f_title, fill=WHITE)
        ty2 = ty + (tbb[3] - tbb[1]) + int(tag_px * SS * 1.1)
        gbb = d.textbbox((0, 0), TAGLINE, font=f_tag)
        d.text(((sw - (gbb[2] - gbb[0])) // 2, ty2), TAGLINE, font=f_tag, fill=MINT)

    return base.resize((w, h), Image.LANCZOS).convert("RGB")


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, quality=95)
    print("wrote", os.path.relpath(path, ROOT))


def main():
    # Play Store feature graphic (1024x500, no alpha)
    fg = build(1024, 500, icon_px=190, title_px=66, tag_px=26, show_chip=True, chip_px=20, layout="left")
    save(fg, os.path.join(STORE, "feature-graphic-1024x500.png"))
    save(fg, os.path.join(DOCS, "landing-hero.png"))

    # Social / Open Graph card (1200x630)
    og = build(1200, 630, icon_px=240, title_px=82, tag_px=32, show_chip=False, chip_px=24, layout="center")
    save(og, os.path.join(DOCS, "landing-og.png"))


if __name__ == "__main__":
    main()
