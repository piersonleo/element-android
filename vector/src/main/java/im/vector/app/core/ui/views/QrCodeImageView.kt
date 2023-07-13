/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.vcard.vchat.utils.Utils.Companion.mergeBitmapLogoToQrCode
import im.vector.app.core.qrcode.toBitMatrix
import im.vector.app.core.qrcode.toBitMatrixMesh
import im.vector.app.core.qrcode.toBitmap
import timber.log.Timber

class QrCodeImageView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var data: String? = null
    private var overlayBitmap: Bitmap? = null

    init {
        setBackgroundColor(Color.WHITE)
    }

    fun setData(data: String) {
        this.data = data

        render()
    }

    fun setData2(data: String, overlayBitmap: Bitmap) {
        this.data = data
        this.overlayBitmap = overlayBitmap

        render()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        render()
    }

    private fun render() {
        if (overlayBitmap == null) {
            data
                    ?.takeIf { height > 0 }
                    ?.let {
                        val bitmap = it.toBitMatrix(height).toBitmap()
                        post { setImageBitmap(bitmap) }
                    }
        }else{
            data
                    ?.takeIf { height > 0 }
                    ?.let {
                        Timber.d("qrHeight: $height")
                        val bitmap = it.toBitMatrixMesh(height).toBitmap()
                        val logo = overlayBitmap!!
                        post { setImageBitmap(mergeBitmapLogoToQrCode(logo, bitmap)) }
                    }
        }
    }
}
