# Knight face avatars

Tight, face-centred square crops of the 8 Knights, one per `domain/.../Knight.kt` entry, shown as
circular avatars by `KnightFace` (`app/.../ui/components/KnightFace.kt`). Used by the Score
Calculator's scenario picker and the Scoreboard cards (issue #171 / #285).

Files are named by the lower-cased `Knight` enum constant (`arythea.jpg`, `tovak.jpg`, ...); the
composable and `KnightFaceAssetsTest` both derive that path from `Knight.faceAsset`, so a missing or
misnamed file fails the test.

## Source and provenance

Each is a square crop of the matching card in `../knight-cards/`, resized to 400x400 (JPEG q88). The
crop is centred on the character's face; because `KnightFace` clips to a **circle**, the square's
corners are discarded, so each box only needs the face centred on the disc. Crop boxes (fractions of
the 1000x1400 card, as `centre-x / centre-y / half-size`, the first and third of card width, the
second of card height):

| Knight    | cx    | cy    | half  |
|-----------|-------|-------|-------|
| tovak     | 0.67  | 0.179 | 0.19  |
| goldyx    | 0.70  | 0.229 | 0.21  |
| norowas   | 0.70  | 0.250 | 0.215 |
| wolfhawk  | 0.68  | 0.193 | 0.19  |
| arythea   | 0.66  | 0.186 | 0.18  |
| krang     | 0.68  | 0.336 | 0.22  |
| braevalar | 0.69  | 0.271 | 0.235 |
| coral     | 0.70  | 0.229 | 0.17  |

To regenerate, re-run the Pillow crop script with these boxes against the `knight-cards/` sources and
eyeball each output (the boxes were tuned by eye, so a source change needs a re-check).

## Licensing

Derived from the `knight-cards/` art, so identical posture: official WizKids/Vlaada Chvátil art,
kept under the stance in [ADR-0010](../../../../../docs/adr/0010-bundled-official-art-licensing-stance.md)
(issue #193) - bundled, mitigated by in-app attribution and a non-affiliation disclaimer. Cropping
changes nothing about the licence; revisit only if the project goes commercial or to an official app
store. See `../knight-cards/README.md` for the per-card source detail.
