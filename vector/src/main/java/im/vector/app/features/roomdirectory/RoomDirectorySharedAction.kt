/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
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
