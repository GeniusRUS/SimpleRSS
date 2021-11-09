package com.genius.srss.util

import android.content.Context
import android.content.res.TypedArray
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.genius.srss.di.services.database.models.SubscriptionFolderDatabaseModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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