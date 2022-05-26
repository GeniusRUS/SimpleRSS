package com.genius.srss.util

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

/**
 * @author https://github.com/cp-radhika-s/Drag_and_drop_jetpack_compose
 */

internal val LocalDragTargetInfo = compositionLocalOf { DragTargetInfo() }

@Composable
fun LongPressDraggable(
    modifier: Modifier = Modifier,
    scaleX: Float = 1.0f,
    scaleY: Float = 1.0f,
    draggedAlpha: Float = 0.8f,
    content: @Composable BoxScope.() -> Unit
) {
    val state = remember { DragTargetInfo() }
    CompositionLocalProvider(
        LocalDragTargetInfo provides state
    ) {
        Box(modifier = modifier.fillMaxSize())
        {
            content()
            if (state.isDragging) {
                var targetSize by remember {
                    mutableStateOf(IntSize.Zero)
                }
                Box(modifier = Modifier
                    .graphicsLayer {
                        val offset = (state.dragPosition + state.dragOffset)
                        this.scaleX = scaleX
                        this.scaleY = scaleY
                        this.alpha = if (targetSize == IntSize.Zero) 0f else draggedAlpha
                        this.translationX = offset.x.minus(targetSize.width / 2)
                        this.translationY = offset.y.minus(targetSize.height / 2)
                    }
                    .onGloballyPositioned {
                        targetSize = it.size
                    }
                ) {
                    state.draggableComposable?.invoke()
                }
            }
        }
    }
}

@Composable
fun <T> DragTarget(
    dataToDrop: T,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit)
) {

    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    val currentState = LocalDragTargetInfo.current

    Box(
        modifier = modifier
            .onGloballyPositioned {
                currentPosition = it.localToWindow(Offset.Zero)
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        currentState.dataToDrop = dataToDrop
                        currentState.isDragging = true
                        currentState.dragPosition = currentPosition + it
                        currentState.draggableComposable = content
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentState.dragOffset += Offset(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        currentState.isDragging = false
                        currentState.dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        currentState.dragOffset = Offset.Zero
                        currentState.isDragging = false
                    }
                )
            }
    ) {
        content()
    }
}

@Composable
fun <T> DropTarget(
    modifier: Modifier = Modifier,
    content: @Composable (BoxScope.(isInBound: Boolean, data: T?) -> Unit)
) {

    val dragInfo = LocalDragTargetInfo.current
    val dragPosition = dragInfo.dragPosition
    val dragOffset = dragInfo.dragOffset
    var isCurrentDropTarget by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier.onGloballyPositioned {
            it.boundsInWindow().let { rect ->
                isCurrentDropTarget = rect.contains(dragPosition + dragOffset)
            }
        }
    ) {
        @Suppress("UNCHECKED_CAST") val data =
            if (isCurrentDropTarget && !dragInfo.isDragging) dragInfo.dataToDrop as T? else null
        content(isCurrentDropTarget, data)
    }
}

/**
 * Works but [detectDragGesturesAfterLongPress] in [DragTarget] canceled touch event on folder location
 */
fun Modifier.dropScroll(lazyGridState: LazyGridState, dropSizeHeight: Int): Modifier {
    return composed {
        val dragInfo = LocalDragTargetInfo.current
        val dragPosition = dragInfo.dragPosition
        val dragOffset = dragInfo.dragOffset

        val scrollPosition = dragPosition + dragOffset
        val offsetFromTop = scrollPosition.y
        val offsetFromBottom = dropSizeHeight - scrollPosition.y

        LaunchedEffect(key1 = scrollPosition) {
            if (offsetFromTop in 0F..250F) {
                when (offsetFromTop) {
                    in 0F..50F -> lazyGridState.scrollBy(-50F)
                    in 50F..150F -> lazyGridState.scrollBy(-25F)
                    in 150F..250F -> lazyGridState.scrollBy(-10F)
                }
            } else if (offsetFromBottom in 0F..250F) {
                when (offsetFromBottom) {
                    in 0F..50F -> lazyGridState.scrollBy(50F)
                    in 50F..150F -> lazyGridState.scrollBy(25F)
                    in 150F..250F -> lazyGridState.scrollBy(10F)
                }
            }
        }
        this
    }
}

internal class DragTargetInfo {
    var isDragging: Boolean by mutableStateOf(false)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    var draggableComposable by mutableStateOf<(@Composable () -> Unit)?>(null)
    var dataToDrop by mutableStateOf<Any?>(null)
}