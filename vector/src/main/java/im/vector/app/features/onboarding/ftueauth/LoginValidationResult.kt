/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

data class LoginValidationResult(
        val usernameOrId: String,
        val password: String,
        val usernameOrIdError: String?,
        val passwordError: String?
)
