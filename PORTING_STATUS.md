# KiteImage — porting status

Updated 2026-07-22 (GIF landed same day). Tests: 50 on JVM (incl. two ImageIO
round-trip suites), 41 on JS/Node (the common set), all green. Targets compiling:
Android, iosArm64, iosSimulatorArm64, iosX64, JVM, JS (browser+node), wasmJs.

## Decoders

| Format | Feature | Status | Reference |
|---|---|---|---|
| **PNG** | signature + chunk walk, CRC on consumed chunks | ✅ | stb_image / PNG spec |
| | color types 0/2/3/4/6, depths 1/2/4/8/16 | ✅ (16-bit → high byte) | |
| | filters None/Sub/Up/Average/Paeth | ✅ | |
| | `tRNS` (palette alpha + color-key 0/2) | ✅ | |
| | Adam7 interlace | ❌ next — clean `UnsupportedImageException` today | |
| | `CgBI` (Apple) | ❌ detected + named, out of scope for now | |
| | ancillary color chunks (gAMA/iCCP/sRGB) | skipped, raw samples returned | |
| **BMP** | BITMAPINFOHEADER/V4/V5, BI_RGB 8/24/32-bit | ✅ | stb_image / commons-imaging |
| | bottom-up + top-down, 4-byte row padding | ✅ | |
| | all-zero-alpha 32-bit → opaque (stb rule) | ✅ | |
| | BI_RLE4/RLE8/BITFIELDS, 1/2/4/16-bit, OS/2 core header | ❌ rejected by name | |
| **GIF** | 87a/89a, LZW (growth, KwKwK, deferred clear, mid-stream CLEAR) | ✅ | stb_image, giflib |
| | global + local color tables, 4-pass interlace | ✅ | |
| | animation compositing: frame rects, transparency, disposal none/keep/bg/prev | ✅ | |
| | delays (browser ≤1 cs → 100 ms rule) + NETSCAPE/ANIMEXTS loop count | ✅ | |
| | `KiteAnimation`/`KiteFrame` API; static formats wrap as 1 frame | ✅ | |
| **JPEG** | baseline (SOF0) | ❌ planned | stb_image |
| | progressive (SOF2) | ❌ planned after baseline | |
| **WebP** | VP8/VP8L | ❌ phase 2, sniffed today | libwebp |
| **TIFF** | baseline subset | ❌ phase 2, sniffed today | commons-imaging |
| **AVIF / HEIC** | — | permanently out of scope (see REFERENCES.md) | |

## Encoders

None yet. Order: PNG (lodepng ref, deflate side vendors from KiteArchive when it
lands there) → JPEG baseline (stb_image_write/jpge) → GIF (commons-imaging writer).

## Infrastructure

| Piece | Status |
|---|---|
| `KiteBitmap` ARGB_8888 pixel buffer | ✅ |
| `ImageFormat.sniff` (PNG/JPEG/GIF/BMP/WEBP/TIFF) | ✅ |
| vendored flate (`internal.flate`: puff inflate, zlib framing, CRC-32) | ✅ — swap to `kitearchive` artifact when it's on Central |
| bomb guards (16M px/side, 268M px total, exact-size IDAT inflate cap) | ✅ |
| commonTest vectors (python3-zlib generated, filters/depths/tRNS pinned) | ✅ 28 |
| jvmTest ImageIO round-trip (organic adaptive-filter data) | ✅ 6 |
| `:kiteimage-compose` (ImageBitmap interop, animated playback) | ❌ planned with GIF |

## Excluded, with reasons

- **AVIF / HEIC** — AV1/HEVC entropy decoding is orders of magnitude past a sane
  pure-Kotlin port; HEVC adds patent baggage. A platform-backed opt-in module could
  exist someday; the core will never pretend.
- **JPEG XL** — no demand signal yet; revisit if one appears.
