/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.roomdirectory

import im.vector.app.core.platform.VectorSharedAction

/**
 * Supported navigation actions for [RoomDirectoryActivity].
 */
sealed class RoomDirectorySharedAction : VectorSharedAction {
    object Back : RoomDirectorySharedAction()
    object CreateRoom : RoomDirectorySharedAction()
    object Close : RoomDirectorySharedAction()
    data class CreateRoomSuccess(val createdRoomId: String) : RoomDirectorySharedAction()
    object ChangeProtocol : RoomDirectorySharedAction()
}
