package com.genius.srss.util

import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genius.srss.R

enum class MultiFabState {
    COLLAPSED, EXPANDED
}

class MultiFabItem(
    val identifier: String,
    val icon: Painter,
    val label: String
)

/**
 * @author [https://github.com/ComposeAcademy/ComposeCompanion]
 */
@Composable
fun MultiFloatingActionButton(
    fabIcon: Painter,
    contentDescription: String,
    items: List<MultiFabItem>,
    toState: MultiFabState,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    stateChanged: (fabstate: MultiFabState) -> Unit,
    onFabItemClicked: (item: MultiFabItem) -> Unit
) {
    val transition: Transition<MultiFabState> = updateTransition(targetState = toState)
    val scale: Float by transition.animateFloat { state ->
        if (state == MultiFabState.EXPANDED) 1f else 0f
    }
    val alpha: Float by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 50)
        }
    ) { state ->
        if (state == MultiFabState.EXPANDED) 1f else 0f
    }
    val shadow: Dp by transition.animateDp(
        transitionSpec = {
            tween(durationMillis = 50)
        }
    ) { state ->
        if (state == MultiFabState.EXPANDED) 2.dp else 0.dp
    }
    val rotation: Float by transition.animateFloat { state ->
        if (state == MultiFabState.EXPANDED) 45f else 0f
    }
    Column(horizontalAlignment = Alignment.End) {
        items.forEach { item ->
            MiniFabItem(item, alpha, shadow, scale, showLabels, onFabItemClicked)
            Spacer(modifier = Modifier.height(20.dp))
        }
        FloatingActionButton(
            modifier = modifier,
            onClick = {
                stateChanged(
                    if (transition.currentState == MultiFabState.EXPANDED) {
                        MultiFabState.COLLAPSED
                    } else MultiFabState.EXPANDED
                )
            }
        ) {
            Icon(
                painter = fabIcon,
                modifier = Modifier.rotate(rotation),
                contentDescription = contentDescription
            )
        }
    }
}

@Composable
private fun MiniFabItem(
    item: MultiFabItem,
    alpha: Float,
    shadow: Dp,
    scale: Float,
    showLabel: Boolean,
    onFabItemClicked: (item: MultiFabItem) -> Unit
) {
    val fabColor = MaterialTheme.colorScheme.secondary
    val shadowColor = colorResource(id = R.color.transparent_black)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        if (showLabel) {
            Text(
                item.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .alpha(animateFloatAsState(alpha).value)
                    .shadow(animateDpAsState(shadow).value)
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        FloatingActionButton(
            modifier = Modifier
                .size(40.dp)
                .alpha(alpha)
                .scale(scale),
            onClick = {
                onFabItemClicked(item)
            }
        ) {
            Icon(
                painter = item.icon,
                contentDescription = item.label
            )
        }
        /*Canvas(
            modifier = Modifier
                .size(32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { onFabItemClicked(item) },
                    indication = rememberRipple(
                        bounded = false,
                        radius = 20.dp,
                        color = MaterialTheme.colors.onSecondary
                    )
                )
        ) {
            drawCircle(
                shadowColor,
                center = Offset(this.center.x + 2f, this.center.y + 7f),
                radius = scale
            )
            drawCircle(color = fabColor, scale)
            item.icon.apply {
                draw(
                    size = intrinsicSize,
                    alpha = alpha
                )
            }
            *//*drawImage(
                item.icon,
                topLeft = Offset(
                    (this.center.x) - (item.icon.width / 2),
                    (this.center.y) - (item.icon.width / 2)
                ),
                alpha = alpha
            )*//*
        }*/
    }
}

@Composable
private fun Minimal(
    item: MultiFabItem,
    alpha: Float,
    scale: Float,
    onFabItemClicked: (item: MultiFabItem) -> Unit
) {
    val fabColor = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = Modifier
            .size(32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onFabItemClicked(item) },
                indication = rememberRipple(
                    bounded = false,
                    radius = 20.dp,
                    color = MaterialTheme.colorScheme.onSecondary
                )
            )
    ) {
        drawCircle(color = fabColor, scale)
        item.icon.apply {
            draw(
                size = intrinsicSize,
                alpha = alpha
            )
        }
        /*drawImage(
            item.icon,
            topLeft = Offset(
                (this.center.x) - (item.icon.width / 2),
                (this.center.y) - (item.icon.width / 2)
            ),
            alpha = alpha
        )*/
    }
}