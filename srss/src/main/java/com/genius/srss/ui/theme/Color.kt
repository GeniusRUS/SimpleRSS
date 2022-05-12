package com.genius.srss.ui.theme

import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)
val TextColorDark = Color(0xFFFBFBFB)
val TextColorLight = Color(0xFF333333)

val StrokeColorDark = Color(0x33FBFBFB)
val StrokeColorLight = Color(0x33333333)

val Primary = Color(0xFFE3484A)

val Colors.strokeColor: Color
    @Composable
    get() = if (isLight) StrokeColorLight else StrokeColorDark