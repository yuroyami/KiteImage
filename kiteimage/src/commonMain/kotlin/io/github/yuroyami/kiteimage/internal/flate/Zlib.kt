package io.github.yuroyami.kiteimage.internal.flate

/*
 * Vendored from KiteArchive (io.github.yuroyami.kitearchive.wrapper.Zlib), trimmed
 * to the decompress path — PNG only inflates here; the PNG *encoder* will bring the
 * deflate side when it lands. Swap for the `kitearchive` artifact once it is on
 * Maven Central.
 */

/**
 * zlib (RFC 1950) framing over [Inflate] — a 2-byte CMF/FLG header and a big-endian
 * Adler-32 trailer. This is the wrapper PNG `IDAT` uses.
 */
internal object Zlib {

    /**
     * Decompress a zlib stream. The Adler-32 trailer is not re-validated here.
     *
     * [maximumSize] is the decompression-bomb guard. PNG callers pass the exact
     * expected filtered-scanline size computed from IHDR, so anything larger is
     * malformed by definition.
     */
    fun decompress(input: ByteArray, maximumSize: Long): ByteArray {
        require(maximumSize > 0) { "maximumSize must be > 0" }
        if (input.size < 2) throw InflateException(InflateError.INVALID_ZLIB_HEADER)

        val cmf = input[0].toInt() and 0xFF
        val flg = input[1].toInt() and 0xFF
        if ((cmf and 0x0F) != 8 || ((cmf ushr 4) and 0x0F) > 7) {
            throw InflateException(InflateError.INVALID_ZLIB_HEADER)
        }
        if ((flg and 0x20) != 0) throw InflateException(InflateError.INVALID_ZLIB_HEADER) // FDICT
        if (((cmf shl 8) or flg) % 31 != 0) throw InflateException(InflateError.INVALID_ZLIB_HEADER)

        val out = Inflate.inflate(input.copyOfRange(2, input.size))
        if (out.size.toLong() > maximumSize) {
            throw InflateException(InflateError.INFLATED_DATA_TOO_LARGE)
        }
        return out
    }
}
