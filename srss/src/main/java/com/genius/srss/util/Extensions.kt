package com.genius.srss.util

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.genius.srss.di.services.database.models.SubscriptionFolderDatabaseModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs

/**
 * Launches a new coroutine and repeats `block` every time the Fragment's viewLifecycleOwner
 * is in and out of `minActiveState` lifecycle state.
 */
inline fun Fragment.launchAndRepeatWithViewLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}

/**
 * Move sorting of items in range [toPosition]..[fromPosition] to previous position,
 * excluding [fromPosition] item
 *
 * **Important note:** [toPosition] must be greater than [fromPosition]
 *
 * @throws IndexOutOfBoundsException if difference between [fromPosition] and [toPosition] is more than list size
 */
fun List<SubscriptionFolderDatabaseModel>.swapSortingToDescends(
    fromPosition: Int,
    toPosition: Int
): List<SubscriptionFolderDatabaseModel> {
    return mapIndexed { index, folder ->
        when (index) {
            in (fromPosition + 1)..toPosition -> folder.copy(
                sortIndex = this@swapSortingToDescends[index - 1].sortIndex
            )
            fromPosition -> folder.copy(
                sortIndex = this@swapSortingToDescends[toPosition].sortIndex
            )
            else -> folder
        }
    }
}

/**
 * Move sorting of items in range [toPosition]..[fromPosition] to next position,
 * excluding [fromPosition] item
 *
 * **Important note:** [fromPosition] must be greater than [toPosition]
 *
 * @throws IndexOutOfBoundsException if difference between [fromPosition] and [toPosition] is more than list size
 */
fun List<SubscriptionFolderDatabaseModel>.swapSortingToAscends(
    fromPosition: Int,
    toPosition: Int
): List<SubscriptionFolderDatabaseModel> {
    return mapIndexed { index, folder ->
        when (index) {
            in toPosition until fromPosition -> folder.copy(
                sortIndex = this@swapSortingToAscends[index + 1].sortIndex
            )
            fromPosition -> folder.copy(
                sortIndex = this@swapSortingToAscends[toPosition].sortIndex
            )
            else -> folder
        }
    }
}

/**
 * Retrieving color int value from attributes
 */
@ColorInt
fun Context.getAttrColorValue(@AttrRes resId: Int): Int {
    val typedValue = TypedValue()
    val resultFlag = intArrayOf(resId)
    val indexOfAttrTextSize = 0
    val array: TypedArray = obtainStyledAttributes(typedValue.data, resultFlag)
    val result = array.getColor(indexOfAttrTextSize, -1)
    array.recycle()
    return result
}

/**
 * @author https://stackoverflow.com/questions/68686117/how-to-detect-the-end-of-transform-gesture-in-jetpack-compose
 * Need to additional improvements https://towardsdev.com/jetpack-compose-detect-the-number-of-fingers-touching-the-screen-253a1e1179f9
 */
fun Modifier.pointerInputDetectTransformGestures(
    panZoomLock: Boolean = false,
    isTransformInProgressChanged: (Boolean) -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
): Modifier {
    return pointerInput(Unit) {
        detectTransformGestures(
            panZoomLock = panZoomLock,
            onGesture = { offset, pan, gestureZoom, gestureRotate ->
                isTransformInProgressChanged(true)
                onGesture(offset, pan, gestureZoom, gestureRotate)
            }
        )
    }
        .pointerInput(Unit) {
            forEachGesture {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.consumed.positionChange }
                    } while (!canceled && event.changes.any { it.pressed })
                    isTransformInProgressChanged(false)
                }
            }
        }
}

/**
 * @author https://stackoverflow.com/questions/68686117/how-to-detect-the-end-of-transform-gesture-in-jetpack-compose
 */
suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
    isTransformInProgressChanged: (Boolean) -> Unit,
) {
    forEachGesture {
        awaitPointerEventScope {
            var rotation = 0f
            var zoom = 1f
            var pan = Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop
            var lockedToPanZoom = false
            var startGestureNotified = false // added

            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()
                    val panChange = event.calculatePan()

                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        rotation += rotationChange
                        pan += panChange

                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                        val panMotion = pan.getDistance()

                        if (zoomMotion > touchSlop ||
                            rotationMotion > touchSlop ||
                            panMotion > touchSlop
                        ) {
                            pastTouchSlop = true
                            lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                        }
                    }

                    if (pastTouchSlop) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                        if (effectiveRotation != 0f ||
                            zoomChange != 1f ||
                            panChange != Offset.Zero
                        ) {
                            onGesture(centroid, panChange, zoomChange, effectiveRotation)
                            if (!startGestureNotified) { // notify first gesture sent
                                isTransformInProgressChanged(true)
                                startGestureNotified = true
                            }
                        }
                        event.changes.forEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            } while (!canceled && event.changes.any { it.pressed })
            isTransformInProgressChanged(false) // notify last finger is up
        }
    }
}