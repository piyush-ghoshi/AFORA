package com.afora.android.ui.theme

import androidx.compose.ui.graphics.Color

// Academic Precision tokens (uidesign / "stitch ui design images" DESIGN.md).
// Indigo carries brand and primary actions; slate neutrals carry everything else.

// Primary: indigo
val Primary = Color(0xFF3730A3)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFE2DFFF)
val OnPrimaryContainer = Color(0xFF0F0069)

// Secondary
val Secondary = Color(0xFF4648D4)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE1E0FF)
val OnSecondaryContainer = Color(0xFF07006C)

// Tertiary: warm accent, used sparingly
val Tertiary = Color(0xFF7A3003)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFDBCC)
val OnTertiaryContainer = Color(0xFF351000)

// Error
val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

// Canvas and surfaces. Cards are white (Surface) on a tinted canvas (Background).
val Background = Color(0xFFF8F9FF)
val OnBackground = Color(0xFF0B1C30)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF0B1C30)
val SurfaceVariant = Color(0xFFE5EEFF)
val OnSurfaceVariant = Color(0xFF464553)

// Outline. CardBorder is the 1px card border from the design (#E2E8F0).
val Outline = Color(0xFF777584)
val OutlineVariant = Color(0xFFC8C4D5)
val CardBorder = Color(0xFFE2E8F0)

// Generic status colors (kept for existing callers)
val Success = Color(0xFF16A34A)
val Warning = Color(0xFFEAB308)
val Info = Color(0xFF3B82F6)

// Attendance / leave semantic colors. Text colors are darkened to meet WCAG AA on
// their tinted containers; the *Bar colors are the brighter variants for the 4dp
// left edge, where only the color (not text) has to be seen.
val StatusPresent = Color(0xFF15803D)
val StatusPresentContainer = Color(0xFFDCFCE7)
val StatusPresentBar = Color(0xFF16A34A)

val StatusLate = Color(0xFF92400E)
val StatusLateContainer = Color(0xFFFEF3C7)
val StatusLateBar = Color(0xFFEAB308)

val StatusAbsent = Color(0xFFB91C1C)
val StatusAbsentContainer = Color(0xFFFEE2E2)
val StatusAbsentBar = Color(0xFFDC2626)

val StatusLeave = Color(0xFF3730A3)
val StatusLeaveContainer = Color(0xFFE0E7FF)
val StatusLeaveBar = Color(0xFF4F46E5)

val StatusNeutral = Color(0xFF475569)
val StatusNeutralContainer = Color(0xFFE2E8F0)
val StatusNeutralBar = Color(0xFF94A3B8)