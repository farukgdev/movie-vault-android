package com.farukg.movievault.feature.catalog.ui.catalog

import com.farukg.movievault.core.error.AppError
import com.farukg.movievault.core.error.AppErrorException

internal fun Throwable.toAppError(): AppError =
    (this as? AppErrorException)?.error ?: AppError.Unknown(this)
