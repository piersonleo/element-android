/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.call.webrtc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import javax.inject.Inject

class ScreenCaptureServiceConnection @Inject constructor(
        private val context: Context
) : ServiceConnection {

    private var isBound = false
    private var screenCaptureService: ScreenCaptureService? = null

    fun bind() {
        if (!isBound) {
            Intent(context, ScreenCaptureService::class.java).also { intent ->
                context.bindService(intent, this, 0)
            }
        }
    }

    fun stopScreenCapturing() {
        screenCaptureService?.stopService()
    }

    override fun onServiceConnected(className: ComponentName, binder: IBinder) {
        screenCaptureService = (binder as ScreenCaptureService.LocalBinder).getService()
        isBound = true
    }

    override fun onServiceDisconnected(className: ComponentName) {
        isBound = false
        screenCaptureService = null
    }
}
