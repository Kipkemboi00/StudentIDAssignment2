package au.edu.vu.studentidassignment2.ui

import android.content.Context
import au.edu.vu.studentidassignment2.R
import au.edu.vu.studentidassignment2.domain.FailureReason
import java.util.Locale

fun Context.failureMessage(reason: FailureReason): String = getString(when (reason) {
    FailureReason.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
    FailureReason.NETWORK -> R.string.error_network
    FailureReason.SERVER -> R.string.error_server
    FailureReason.UNEXPECTED_RESPONSE -> R.string.error_unexpected
    FailureReason.SESSION_EXPIRED -> R.string.error_session
})

fun fieldLabel(name: String): String = name
    .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
    .replace('_', ' ')
    .replace('-', ' ')
    .trim()
    .replaceFirstChar { it.titlecase(Locale.getDefault()) }

fun Context.fieldValue(value: String?): String = value?.takeIf { it.isNotBlank() } ?: getString(R.string.no_value)
