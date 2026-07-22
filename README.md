# KiteImage

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)
[![Targets](https://img.shields.io/badge/targets-Android%20|%20iOS%20|%20macOS%20|%20JVM%20|%20JS%20|%20WASM-success)](#install)
[![Core deps](https://img.shields.io/badge/core%20dependencies-kotlin--stdlib%20only-blue)](#why-kiteimage)
[![Ported from](https://img.shields.io/badge/ports-stb__image%20·%20commons--imaging-orange)](reference/REFERENCES.md)
[![License](https://img.shields.io/badge/license-Apache--2.0-lightgrey)](#license--credits)
[![Status](https://img.shields.io/badge/status-PNG%20%2F%20BMP%20%2F%20GIF%20decode-brightgreen)](PORTING_STATUS.md)
[![Tests](https://img.shields.io/badge/tests-50%20passing-brightgreen)](PORTING_STATUS.md)

**A pure-Kotlin image codec toolkit for Kotlin Multiplatform: from-scratch ports of the canonical references (stb_image, commons-imaging). The same `.kt` decodes on Android, iOS, desktop JVM, the browser and WASM — no BitmapFactory, no CoreGraphics, no native binary.**

```kotlin
// decode — sniffs the format from magic bytes, returns packed ARGB
val bitmap: KiteBitmap = KiteImage.decode(bytes)
bitmap.width; bitmap.height
bitmap[x, y]                    // 0xAARRGGBB

// animations — every frame arrives fully composited (disposal methods,
// transparency and frame offsets already applied); playback is just
// "draw frame N, wait delay N"
val anim: KiteAnimation = KiteImage.decodeAnimation(bytes)
anim.frames.forEach { frame -> frame.bitmap; frame.delayMillis }
anim.loopCount                  // NETSCAPE semantics: 0 = forever
anim.isAnimated                 // false for static formats (single frame)

// or just identify
KiteImage.detect(bytes)         // -> ImageFormat.PNG / JPEG / GIF / BMP / WEBP / TIFF
```

## Why KiteImage

Compose Multiplatform can *display* images everywhere, but *decoding* them everywhere
still leans on each platform: `BitmapFactory` on Android, CoreGraphics on iOS, Skia
codecs on desktop, the browser on web. The moment you need pixels in common code — a
PDF embedding its images, a thumbnail hash, a server-side resize, a wasm tool — you're
stitching per-platform decoders again.

KiteImage is one decoder for all of them. Pure computation on `ByteArray`, kotlin-stdlib
only, identical output on every target.

- **One pixel layout** — every format normalises to non-premultiplied ARGB_8888 in a
  plain `IntArray`. Grayscale, palette, BGR, 16-bit: gone before you see them.
- **Wide sniffing, honest decoding** — `detect` recognises more formats than `decode`
  handles, so "that's a WebP, and this build can't decode WebP yet" beats "unknown
  format". Unsupported features throw `UnsupportedImageException` naming the feature.
- **Hostile-input guards** — dimension and decompression-bomb limits, CRC verification
  on consumed PNG chunks, truncation surfaces as `ImageDecodeException` (never an
  index crash). Malformed-input behavior is part of the test suite.
- **Kite lineage** — the PNG inflate path is the same zlib/`puff` port that already
  ships in [KiteArchive](https://github.com/yuroyami/KiteArchive); references are
  ported clean-room from permissively-licensed canonical sources.

## Format support

Today, at v0.0.1:

| Format | Decode | Encode |
|---|---|---|
| PNG | ✅ all color types, all depths, all filters (Adam7 interlace: not yet) | roadmap |
| BMP | ✅ 8/24/32-bit BI_RGB, both row orders | roadmap |
| GIF | ✅ 87a/89a incl. animation: full LZW, interlace, disposal compositing, delays, loop count | roadmap |
| JPEG | on the roadmap (baseline, then progressive) | roadmap |
| WebP | recognised, not decoded | — |
| TIFF | recognised, not decoded | — |
| AVIF / HEIC | out of scope (see [REFERENCES.md](reference/REFERENCES.md)) | — |

The full matrix with per-feature detail lives in [PORTING_STATUS.md](PORTING_STATUS.md).

## Compose

`kiteimage-compose` ships one composable. It decides on its own whether to animate —
feed it anything:

```kotlin
KiteImage(
    data = bytes,                  // GIF → plays; PNG/BMP → draws. No branching.
    contentDescription = "avatar",
    modifier = Modifier.size(96.dp),
    // animate = false,            // escape hatch: pin the first frame (thumbnails)
    // onError = { log(it) },      // malformed input → draws nothing + callback
)
```

Decoding runs off the UI thread. Animated inputs honor per-frame delays (with the
browser ≤10 ms → 100 ms rule), disposal compositing, and the NETSCAPE loop count —
finite loops end holding the last frame. `KiteBitmap.toImageBitmap()` is public for
custom pipelines, and an overload takes an already-decoded `KiteBitmap`.

## Install

Not yet published — building from source works today:

```sh
./gradlew :kiteimage:publishToMavenLocal
```

```kotlin
commonMain.dependencies {
    implementation("io.github.yuroyami:kiteimage:0.0.1-SNAPSHOT")
}
```

## License & credits

Apache License 2.0. Ported clean-room from permissively-licensed references —
primarily [stb_image](https://github.com/nothings/stb) (public domain / MIT) and
[Apache Commons Imaging](https://github.com/apache/commons-imaging) (Apache-2.0);
the inflate path derives from Mark Adler's public-domain `puff` via KiteArchive.
Details in [reference/REFERENCES.md](reference/REFERENCES.md) and [NOTICE](NOTICE).
