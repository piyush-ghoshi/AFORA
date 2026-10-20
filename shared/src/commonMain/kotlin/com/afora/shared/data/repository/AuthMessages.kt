package com.afora.shared.data.repository

/** User-facing auth messages that other layers need to recognise by identity. */
object AuthMessages {
    /** The message produced when Firebase reports the email is already registered. */
    const val EMAIL_ALREADY_IN_USE = "An account with this email already exists"
}