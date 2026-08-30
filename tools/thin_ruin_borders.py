"""Thin the baked-in black hexagon border on the Ruin/Altar token art (issue #284).

Why this exists
---------------
The hexagonal Ruin tokens - the RUIN pile back (`backs/ruin.png`) and every individual
`ruin_*.png` face - get *both* their hexagon shape and their black outline from the PNG's own
alpha channel: the Enemy Picker draws them with no Compose clip/border at all (`PileBackFace`'s
`isHex` path in `app/.../ui/enemypicker/EnemyTokenArt.kt`), so the only place a border can live is
the bitmap. The source crops carry a ~16px black ring (~3.5% of the hexagon span), which reads far
heavier than the neighbouring tokens: the round enemy faces have no black ring and the faction
reward tiles use a 1dp (~1%) outline. Issue #284 asks to bring the ruin border down to roughly that
thinness so the Enemy Picker grid looks consistent.

What it does
------------
For each targeted PNG it erodes the opaque hexagon inward by however many pixels are needed to leave
a ~`TARGET_BORDER_PX` black ring, trimming the *outer* part of the ring and turning it transparent.
The new outline is given a 1px anti-aliased ramp (via a Euclidean distance transform) so the trimmed
edge stays smooth rather than jagged. The interior art and the 512x512 canvas are untouched; only the
outermost band of formerly-black pixels becomes transparent, so the hexagon ends up a few percent
smaller with a much thinner rim.

Idempotent by design
---------------------
It first *measures* each token's current border thickness and only erodes by `current - target`,
skipping any file already at/under the target (plus a small tolerance). So re-running it is a no-op,
and if new ruin art is added later with the same heavy border, running it once brings that art in
line too.

Deps: Pillow + numpy (required); scipy (optional - gives the cleanest anti-aliased edge, falls back
to a Pillow morphological erode if absent). Run from anywhere:

    python tools/thin_ruin_borders.py            # process the repo's ruin art in place
    python tools/thin_ruin_borders.py --dry-run  # measure + report, write nothing
"""

from __future__ import annotations

import argparse
import glob
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

# scipy's distance transform gives a clean sub-pixel anti-aliased edge; degrade gracefully without it.
try:
    from scipy.ndimage import distance_transform_edt

    _HAVE_SCIPY = True
except ImportError:  # pragma: no cover - scipy is present in the author's env; keep the tool portable.
    _HAVE_SCIPY = False

# Target remaining black-ring thickness, in source pixels on the 512x512 art. ~5px is ~1.1% of the
# hexagon span, matching the reward tiles' thin 1dp outline (the "match other tokens" ask in #284).
TARGET_BORDER_PX = 5
# Don't bother re-processing a token whose border is already within this many px of the target - keeps
# the pass idempotent (a second run finds ~5px borders and does nothing).
TOLERANCE_PX = 2

# Where the enemy-token art lives, relative to this script (tools/ -> repo root -> app assets).
_REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
_ASSET_DIR = os.path.join(_REPO_ROOT, "app", "src", "main", "assets", "enemy-tokens")


def _opaque_mask(alpha: np.ndarray) -> np.ndarray:
    """Boolean mask of the token body: pixels that are more opaque than transparent."""
    return alpha > 128


def measure_border_px(img: Image.Image) -> int:
    """Estimate the black outline thickness by scanning the horizontal centre row.

    The ruin hexagons are pointy-top with flat vertical left/right edges, so the black run inward
    from the leftmost/rightmost opaque pixel on the middle row is a faithful read of the ring width.
    """
    a = np.array(img.convert("RGBA"))
    alpha = a[:, :, 3]
    # "black-ish" = opaque AND low luminance (the ring), so interior art isn't mistaken for border.
    lum = a[:, :, :3].astype(int).mean(axis=2)
    black = _opaque_mask(alpha) & (lum < 60)

    row = black[a.shape[0] // 2]
    xs = np.where(_opaque_mask(alpha[a.shape[0] // 2]))[0]
    if xs.size == 0:
        return 0

    def run(start: int, step: int) -> int:
        n, i = 0, start
        while 0 <= i < row.size and row[i]:
            n += 1
            i += step
        return n

    # Min of the two sides guards against a stray dark art pixel touching one edge.
    return min(run(xs.min(), +1), run(xs.max(), -1))


def erode_alpha(img: Image.Image, erode_px: int) -> Image.Image:
    """Return a copy whose opaque region is shrunk inward by `erode_px`, with an anti-aliased edge.

    Trimming the outer band of the (black) ring is what thins the border; the RGB channels are left
    as-is, so the anti-aliased boundary fades the old black out to transparent for a smooth rim.
    """
    a = np.array(img.convert("RGBA"))
    mask = _opaque_mask(a[:, :, 3])

    if _HAVE_SCIPY:
        # Distance (px) from each opaque pixel to the nearest transparent one. Anything closer to the
        # edge than `erode_px` is trimmed; the +0.5 gives a 1px ramp instead of a hard cut = AA.
        dist = distance_transform_edt(mask)
        new_alpha = np.clip(dist - erode_px + 0.5, 0.0, 1.0)
    else:  # pragma: no cover - portability fallback when scipy isn't installed.
        # MinFilter shrinks the white (opaque) region by ~erode_px; a light blur re-softens the edge.
        m = Image.fromarray((mask * 255).astype(np.uint8))
        m = m.filter(ImageFilter.MinFilter(2 * erode_px + 1)).filter(ImageFilter.GaussianBlur(0.8))
        new_alpha = np.array(m).astype(np.float32) / 255.0

    out = a.copy()
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

    files = target_files()
    if not files:
        print(f"No ruin art found under {_ASSET_DIR}", file=sys.stderr)
        return 1

    print(f"scipy anti-aliasing: {'yes' if _HAVE_SCIPY else 'no (Pillow fallback)'}")
    changed = 0
    for path in files:
        img = Image.open(path)
        border = measure_border_px(img)
        erode_px = round(border - TARGET_BORDER_PX)
        rel = os.path.relpath(path, _REPO_ROOT)
        if erode_px <= TOLERANCE_PX:
            print(f"  skip  {rel}: border ~{border}px already thin enough")
            continue
        print(f"  thin  {rel}: border ~{border}px -> ~{TARGET_BORDER_PX}px (erode {erode_px}px)")
        if not args.dry_run:
            erode_alpha(img, erode_px).save(path, optimize=True)
        changed += 1

    verb = "would change" if args.dry_run else "changed"
    print(f"Done: {verb} {changed} of {len(files)} file(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
