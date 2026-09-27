#!/usr/bin/env python3
"""Copy provider logo sources into the app drawable folder as square badges."""

from __future__ import annotations

import json
import shutil
from collections import deque
from pathlib import Path

from PIL import Image, ImageChops, ImageEnhance, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SOURCE_DIR = ROOT / "assets" / "provider-logos" / "source"
OUT_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable"
OUT_SIZE = 512
PAD_FRACTION = 0.06
PASSTHROUGH_PROVIDERS: frozenset[str] = frozenset()

# Provider id -> source filename in SOURCE_DIR (see mapping.json).
PROVIDER_SOURCES: dict[str, str] = {
    "anthropic": "anthropic.png",
    "cohere": "cohere.png",
    "deepseek": "deepseek.png",
    "dots": "dots.png",
    "google": "google.png",
    "inclusionai": "inclusionai.png",
    "liquid": "liquid.png",
    "mistral": "mistral.png",
    "nexagi": "nexagi.png",
    "nvidia": "nvidia.png",
    "openai": "openai.png",
    "poolside": "poolside.png",
    "qwen": "qwen.png",
    "thinkingmachines": "thinkingmachines.png",
    "xai": "xai.png",
    "zai": "zai.png",
}


def _is_near_black(r: int, g: int, b: int, a: int, threshold: int) -> bool:
    return a > 0 and r <= threshold and g <= threshold and b <= threshold


def _remove_edge_near_white_background(im: Image.Image, threshold: int = 245) -> Image.Image:
    """Remove white export backgrounds without erasing light logo marks."""
    im = im.convert("RGBA")
    pixels = im.load()
    w, h = im.size
    visited: set[tuple[int, int]] = set()
    queue: deque[tuple[int, int]] = deque()

    for x in range(w):
        queue.append((x, 0))
        queue.append((x, h - 1))
    for y in range(h):
        queue.append((0, y))
        queue.append((w - 1, y))

    while queue:
        x, y = queue.popleft()
        if (x, y) in visited:
            continue
        if x < 0 or x >= w or y < 0 or y >= h:
            continue
        visited.add((x, y))
        r, g, b, a = pixels[x, y]
        if a <= 0 or min(r, g, b) < threshold:
            continue
        pixels[x, y] = (0, 0, 0, 0)
        queue.append((x + 1, y))
        queue.append((x - 1, y))
        queue.append((x, y + 1))
        queue.append((x, y - 1))

    return im


def _remove_edge_black_background(im: Image.Image, threshold: int = 40) -> Image.Image:
    """Remove black export backgrounds without erasing dark logo strokes."""
    im = im.convert("RGBA")
    pixels = im.load()
    w, h = im.size
    visited: set[tuple[int, int]] = set()
    queue: deque[tuple[int, int]] = deque()

    for x in range(w):
        queue.append((x, 0))
        queue.append((x, h - 1))
    for y in range(h):
        queue.append((0, y))
        queue.append((w - 1, y))

    while queue:
        x, y = queue.popleft()
        if (x, y) in visited:
            continue
        if x < 0 or x >= w or y < 0 or y >= h:
            continue
        visited.add((x, y))
        r, g, b, a = pixels[x, y]
        if not _is_near_black(r, g, b, a, threshold):
            continue
        pixels[x, y] = (0, 0, 0, 0)
        queue.append((x + 1, y))
        queue.append((x - 1, y))
        queue.append((x, y + 1))
        queue.append((x, y - 1))

    return im


def _alpha_bbox(im: Image.Image) -> tuple[int, int, int, int] | None:
    if im.mode != "RGBA":
        im = im.convert("RGBA")
    return im.split()[3].getbbox()


def _needs_boost(im: Image.Image) -> bool:
    if im.mode != "RGBA":
        im = im.convert("RGBA")
    pixels = [p for p in im.getdata() if p[3] > 16]
    if not pixels:
        return True
    peak = max(max(p[0], p[1], p[2]) for p in pixels)
    avg = sum(max(p[0], p[1], p[2]) for p in pixels) / len(pixels)
    return avg < 72 and peak < 90


def _lift_shadows_for_light_ui(im: Image.Image, target_avg: int = 150) -> Image.Image:
    """Dark-gray marks on transparent PNGs need extra contrast on white cards."""
    im = im.convert("RGBA")
    pixels = list(im.getdata())
    visible = [p for p in pixels if p[3] > 16]
    if not visible:
        return im
    avg = sum(max(p[0], p[1], p[2]) for p in visible) / len(visible)
    if avg >= 85:
        return im
    scale = min(target_avg / max(avg, 1), 12.0)
    out = []
    for r, g, b, a in pixels:
        if a <= 16:
            out.append((r, g, b, a))
            continue
        out.append(
            (
                min(255, int(r * scale)),
                min(255, int(g * scale)),
                min(255, int(b * scale)),
                a,
            )
        )
    im.putdata(out)
    return im


def _recover_faint_line_art_on_black(im: Image.Image) -> Image.Image:
    """Some exports are wireframes encoded as ~black on black; recover for light UI."""
    gray = im.convert("RGB").convert("L")
    lifted = Image.eval(gray, lambda p: min(255, p * 120))
    mask = lifted.point(lambda p: 255 if p >= 8 else 0, mode="L")
    for _ in range(2):
        mask = mask.filter(ImageFilter.MaxFilter(7))
    stroke = Image.new("RGBA", im.size, (36, 40, 48, 255))
    stroke.putalpha(mask)
    return stroke


def _prepare_logo_rgba(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    prepared = _remove_edge_black_background(im)
    prepared = _remove_edge_near_white_background(prepared)
    if _alpha_bbox(prepared) is None:
        prepared = _recover_faint_line_art_on_black(im)
    prepared = _boost_low_contrast(prepared)
    return _lift_shadows_for_light_ui(prepared)


def _boost_low_contrast(im: Image.Image) -> Image.Image:
    if im.mode != "RGBA":
        im = im.convert("RGBA")
    if not _needs_boost(im):
        return im
    rgb = im.convert("RGB")
    boosted = ImageEnhance.Contrast(rgb).enhance(2.4)
    boosted = ImageEnhance.Brightness(boosted).enhance(1.35)
    out = boosted.convert("RGBA")
    out.putalpha(im.split()[3])
    return out


def fit_square(
    im: Image.Image,
    size: int,
    pad_fraction: float,
    prepare: object = _prepare_logo_rgba,
) -> Image.Image:
    im = prepare(im)  # type: ignore[operator]
    bbox = _alpha_bbox(im)
    if bbox:
        im = im.crop(bbox)
    w, h = im.size
    side = max(w, h)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(im, ((side - w) // 2, (side - h) // 2), im)
    inner = int(size * (1 - 2 * pad_fraction))
    canvas = canvas.resize((inner, inner), Image.Resampling.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset = (size - inner) // 2
    out.paste(canvas, (offset, offset), canvas)
    return out


def process_one(provider_id: str, filename: str) -> None:
    src = SOURCE_DIR / filename
    if not src.is_file():
        raise FileNotFoundError(f"Missing source for {provider_id}: {src}")
    dest = OUT_DIR / f"ic_provider_{provider_id}.png"
    if provider_id in PASSTHROUGH_PROVIDERS:
        shutil.copy2(src, dest)
        print(f"copied {dest.relative_to(ROOT)} (passthrough)")
        return

    im = Image.open(src)
    square = fit_square(im, OUT_SIZE, PAD_FRACTION, prepare=_prepare_logo_rgba)
    square.save(dest, format="PNG", optimize=True)
    print(f"wrote {dest.relative_to(ROOT)}")


def main() -> None:
    mapping_path = SOURCE_DIR / "mapping.json"
    if mapping_path.is_file():
        data = json.loads(mapping_path.read_text(encoding="utf-8"))
        sources = {k: v["file"] for k, v in data["providers"].items()}
    else:
        sources = PROVIDER_SOURCES

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for provider_id, filename in sources.items():
        process_one(provider_id, filename)
    print(f"Processed {len(sources)} provider logos.")


if __name__ == "__main__":
    main()
