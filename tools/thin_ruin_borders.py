"""Thin the baked-in black hexagon border on the Ruin/Altar token art (issue #284).

The hexagonal Ruin tokens get *both* their hexagon shape and their black outline from the PNG's own
alpha channel - the Enemy Picker draws them with no Compose clip/border (the `isHex` path in
`app/.../ui/enemypicker/EnemyTokenArt.kt`), so the only place a border can live is the bitmap. The
source crops carry a heavy black ring; this trims it to a thin one so the ruin hexagons sit
comfortably next to the round enemy faces. See the `enemy-tokens` asset README for the surrounding
rationale.

How it works: for each targeted PNG it erodes the opaque hexagon inward by however many pixels are
needed to leave a ~`TARGET_BORDER_PX` black ring, trimming the *outer* part of the ring to
transparency with a 1px anti-aliased ramp (a Euclidean distance transform) so the new edge stays
smooth. The interior art and the 512x512 canvas are untouched, so the hexagon ends up a few percent
smaller with a much thinner rim.

It first *measures* each token's current border and only erodes by `current - target`, skipping any
file already at/under the target, so re-running is a no-op. The measurement assumes the ruin frame's
convention: a dark rim around a lighter interior (true of every ruin crop from the source mod); it is
not a general-purpose border detector.

Requires Pillow + numpy + scipy. Run from anywhere:

    python tools/thin_ruin_borders.py            # process the repo's ruin art in place
    python tools/thin_ruin_borders.py --dry-run  # measure + report, write nothing
"""

from __future__ import annotations

import argparse
import glob
import os
import sys

import numpy as np
from PIL import Image

try:
    from scipy.ndimage import distance_transform_edt
except ImportError:  # A clear, early failure beats silently eroding with a different algorithm.
    distance_transform_edt = None

# Target remaining black-ring thickness, in source pixels on the 512x512 art. ~5px is ~1.1% of the
# hexagon span, matching the reward tiles' thin 1dp outline (the "match other tokens" ask in #284).
TARGET_BORDER_PX = 5
# Don't re-process a token whose border is already within this many px of the target - keeps the pass
# idempotent (a second run finds ~5px borders and does nothing).
TOLERANCE_PX = 2
# Fractions of the image height to sample the border at. Several rows + a median guard against one
# anomalous scanline (a notch in the ring, a dark art detail grazing an edge) skewing the whole file.
_SAMPLE_ROW_FRACTIONS = (0.40, 0.45, 0.50, 0.55, 0.60)

# Where the enemy-token art lives, relative to this script (tools/ -> repo root -> app assets).
_REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_ASSET_DIR = os.path.join(_REPO_ROOT, "app", "src", "main", "assets", "enemy-tokens")


def load_rgba(path: str) -> np.ndarray:
    """Decode a PNG to an (H, W, 4) uint8 array, once, closing the file handle before we return.

    Using a `with` block matters on Windows: we later `.save()` over this same path, and a lingering
    read handle can make the overwrite fail. `np.array(...)` fully materialises the pixels inside the
    block, so nothing depends on the file staying open.
    """
    with Image.open(path) as im:
        return np.array(im.convert("RGBA"))


def measure_border_px(rgba: np.ndarray) -> int:
    """Estimate the black outline thickness from an RGBA array (see module docstring for the model).

    The ruin hexagons are pointy-top with flat vertical left/right edges, so the black run inward from
    the leftmost/rightmost opaque pixel on a near-centre row reads the ring width. We sample a few rows
    and take the median so no single scanline decides the erosion for the whole file.
    """
    height = rgba.shape[0]
    alpha = rgba[:, :, 3]
    opaque = alpha > 128
    # "black-ish" = opaque AND low luminance (the ring), so interior art isn't mistaken for border.
    lum = rgba[:, :, :3].astype(int).mean(axis=2)
    black = opaque & (lum < 60)

    def run(row: np.ndarray, start: int, step: int) -> int:
        n, i = 0, start
        while 0 <= i < row.size and row[i]:
            n += 1
            i += step
        return n

    samples: list[int] = []
    for frac in _SAMPLE_ROW_FRACTIONS:
        y = int(height * frac)
        xs = np.where(opaque[y])[0]
        if xs.size == 0:
            continue
        row = black[y]
        # Min of the two sides guards against a stray dark art pixel touching one edge on this row.
        samples.append(min(run(row, xs.min(), +1), run(row, xs.max(), -1)))

    return int(np.median(samples)) if samples else 0


def erode_alpha(rgba: np.ndarray, erode_px: int) -> Image.Image:
    """Return an image whose opaque region is shrunk inward by `erode_px`, with an anti-aliased edge.

    Trimming the outer band of the (black) ring is what thins the border; the RGB channels are left
    as-is, so the anti-aliased boundary fades the old black out to transparent for a smooth rim.
    """
    mask = rgba[:, :, 3] > 128
    # Distance (px) from each opaque pixel to the nearest transparent one. Anything closer to the edge
    # than `erode_px` is trimmed; the +0.5 gives a 1px ramp instead of a hard cut = anti-aliasing.
    dist = distance_transform_edt(mask)
    new_alpha = np.clip(dist - erode_px + 0.5, 0.0, 1.0)

    out = rgba.copy()
    out[:, :, 3] = (new_alpha * 255).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def target_files() -> list[str]:
    """The hexagon ruin PNGs: every `ruin_*.png` face plus the RUIN pile back. Nothing else."""
    faces = sorted(glob.glob(os.path.join(_ASSET_DIR, "ruin_*.png")))
    back = os.path.join(_ASSET_DIR, "backs", "ruin.png")
    return faces + ([back] if os.path.exists(back) else [])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dry-run", action="store_true", help="measure + report only, write nothing")
    args = parser.parse_args()

    if distance_transform_edt is None:
        print("This tool requires scipy (pip install scipy).", file=sys.stderr)
        return 1

    files = target_files()
    if not files:
        print(f"No ruin art found under {_ASSET_DIR}", file=sys.stderr)
        return 1

    changed = 0
    for path in files:
        rgba = load_rgba(path)
        border = measure_border_px(rgba)
        # Both operands are ints, so the difference is exact - no rounding needed.
        erode_px = border - TARGET_BORDER_PX
        rel = os.path.relpath(path, _REPO_ROOT)
        if erode_px <= TOLERANCE_PX:
            print(f"  skip  {rel}: border ~{border}px already thin enough")
            continue
        print(f"  thin  {rel}: border ~{border}px -> ~{TARGET_BORDER_PX}px (erode {erode_px}px)")
        if not args.dry_run:
            erode_alpha(rgba, erode_px).save(path, optimize=True)
        changed += 1

    verb = "would change" if args.dry_run else "changed"
    print(f"Done: {verb} {changed} of {len(files)} file(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
