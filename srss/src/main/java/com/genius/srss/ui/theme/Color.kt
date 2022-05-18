package com.genius.srss.ui.theme

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TextColorDark = Color(0xFFFBFBFB)
val TextColorLight = Color(0xFF333333)

val StrokeColorDark = Color(0x33FBFBFB)
val StrokeColorLight = Color(0x33333333)

val Primary = Color(0xFFE3484A)

val PlaceholderBackground = Color(0x33808080)
val FeedBackground = Color(0xCCFBFBFB)
val DefaultBackground = Color(0xFFFBFBFB)
val DefaultBackgroundInverted = Color(0xFF333333)
val DefaultBackgroundInvertedInactive = Color(0x33333333)
val Inactive = Color(0xFF828282)
val ActiveElement = Color(0xFFDC1B1E)
val PrimaryDark = Color(0xFF9A1215)
val OnPrimary = Color(0xFFFFFFFF)
val ButtonTextColor = Color(0xFFFBFBFB)
val Secondary = Color(0xFF000000)

val PlaceholderBackgroundDark = Color(0x33a6a6a6)
val FeedBackgroundDark = Color(0xCC1E1E1E)
val DefaultBackgroundDark = Color(0xFF333333)
val DefaultBackgroundInvertedDark = Color(0xFFFBFBFB)
val DefaultBackgroundInvertedInactiveDark = Color(0x33FBFBFB)
val OnPrimaryDark = Color(0xFFFFFFFF)
val ButtonTextColorDark = Color(0xFFFBFBFB)

val Colors.strokeColor: Color
    @Composable
    get() = if (isLight) StrokeColorLight else StrokeColorDark