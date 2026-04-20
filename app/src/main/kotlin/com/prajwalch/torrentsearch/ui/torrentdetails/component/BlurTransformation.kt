/**
 * Designed and developed by 2022 T8RIN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prajwalch.torrentsearch.ui.torrentdetails.component

import android.graphics.Bitmap
import androidx.core.graphics.scale

import coil3.size.Size
import coil3.transform.Transformation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

// Taken from: https://github.com/T8RIN/BlurTransformation
class BlurTransformation(
    private val radius: Int = 25,
    private val scale: Float = 0.5f,
) : Transformation() {
    override val cacheKey: String = "${javaClass.name}-$radius"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap =
        input.blur(scale = scale, radius = radius) ?: input

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is BlurTransformation && radius == other.radius
    }

    override fun hashCode(): Int = radius.hashCode()
}

private suspend fun Bitmap.blur(
    scale: Float,
    radius: Int,
): Bitmap? = withContext(Dispatchers.Default) {
    if (radius < 1) return@withContext null

    var inputBitmap = this@blur
    val scaleWidth = (inputBitmap.width * scale).roundToInt()
    val scaleHeight = (inputBitmap.height * scale).roundToInt()

    inputBitmap = inputBitmap.scale(scaleWidth, scaleHeight, false)
    val outputBitmap = inputBitmap.copy(inputBitmap.config ?: Bitmap.Config.ARGB_8888, true)

    val outputWidth = outputBitmap.width
    val outputHeight = outputBitmap.height
    val pixels = IntArray(outputWidth * outputHeight)
    outputBitmap.getPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)

    val wm = outputWidth - 1
    val hm = outputHeight - 1
    val wh = outputWidth * outputHeight
    val div = radius + radius + 1
    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    var rsum: Int
    var gsum: Int
    var bsum: Int
    var x: Int
    var y: Int
    var i: Int
    var p: Int
    var yp: Int
    var yi: Int
    val vmin = IntArray(outputWidth.coerceAtLeast(outputHeight))
    var divsum = div + 1 shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)

    i = 0
    while (i < 256 * divsum) {
        dv[i] = i / divsum
        i++
    }

    yi = 0
    var yw: Int = yi
    val stack = Array(div) {
        IntArray(
            3
        )
    }
    var stackpointer: Int
    var stackstart: Int
    var sir: IntArray
    var rbs: Int
    val r1 = radius + 1
    var routsum: Int
    var goutsum: Int
    var boutsum: Int
    var rinsum: Int
    var ginsum: Int
    var binsum: Int
    y = 0

    while (y < outputHeight) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum

        i = -radius

        while (i <= radius) {
            p = pixels[yi + wm.coerceAtMost(i.coerceAtLeast(0))]
            sir = stack[i + radius]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rbs = r1 - abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs

            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }

            i++
        }

        stackpointer = radius
        x = 0

        while (x < outputWidth) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]

            if (y == 0) {
                vmin[x] = (x + radius + 1).coerceAtMost(wm)
            }

            p = pixels[yw + vmin[x]]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer % div]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi++
            x++
        }
        yw += outputWidth
        y++
    }
    x = 0

    while (x < outputWidth) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        yp = -radius * outputWidth
        i = -radius

        while (i <= radius) {
            yi = 0.coerceAtLeast(yp) + x
            sir = stack[i + radius]
            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]
            rbs = r1 - abs(i)
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            if (i < hm) {
                yp += outputWidth
            }
            i++
        }

        yi = x
        stackpointer = radius
        y = 0

        while (y < outputHeight) {
            pixels[yi] =
                -0x1000000 and pixels[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]

            if (x == 0) {
                vmin[y] = (y + r1).coerceAtMost(hm) * outputWidth
            }

            p = x + vmin[y]
            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi += outputWidth
            y++
        }
        x++
    }

    outputBitmap.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
    inputBitmap.recycle()

    return@withContext outputBitmap
}