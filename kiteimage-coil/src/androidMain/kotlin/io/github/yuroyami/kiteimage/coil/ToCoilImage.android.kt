package io.github.yuroyami.kiteimage.coil

import android.graphics.Bitmap
import coil3.Image
import coil3.asImage
import io.github.yuroyami.kiteimage.KiteBitmap

internal actual fun KiteBitmap.toCoilImage(shareable: Boolean): Image =
    Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888).asImage(shareable)
