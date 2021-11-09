package com.genius.srss

import com.genius.srss.di.services.database.models.SubscriptionFolderDatabaseModel
import com.genius.srss.util.swapSortingToDescends
import org.junit.Test

import org.junit.Assert.*
import java.lang.IndexOutOfBoundsException

class FolderSortingSwapDescentTest {

    private val folders: List<SubscriptionFolderDatabaseModel> = (0..3).map { position ->
        SubscriptionFolderDatabaseModel(
            id = "id$position",
            sortIndex = position.toLong(),
            name = "Folder #$position",
            dateOfCreation = position * 1000L,
            isInFeedMode = false
        )
    }

    @Test
    fun swapToDescentEmptyList() {
        val sortedFolders = emptyList<SubscriptionFolderDatabaseModel>().swapSortingToDescends(
            fromPosition = 0,
            toPosition = 0
        )
        assertEquals(0, sortedFolders.size)
    }

    @Test
    fun swapToDescentEmptyListWrongPosition() {
        val sortedFolders = emptyList<SubscriptionFolderDatabaseModel>().swapSortingToDescends(
            fromPosition = 1,
            toPosition = 2
        )
        assertEquals(0, sortedFolders.size)
    }

    @Test
    fun swapToDescentToOnePosition() {
        val sortedFolders = folders.swapSortingToDescends(fromPosition = 1, toPosition = 2)
        assertEquals(2L, sortedFolders[1].sortIndex)
    }

    @Test
    fun swapToDescentToOnePositionOnStartList() {
        val sortedFolders = folders.swapSortingToDescends(fromPosition = 0, toPosition = 1)
        assertEquals(1L, sortedFolders[0].sortIndex)
    }

    @Test
    fun swapToDescentOnStartList() {
        val sortedFolders = folders.swapSortingToDescends(fromPosition = 0, toPosition = 3)
        assertEquals(3L, sortedFolders[0].sortIndex)
    }

    @Test
    fun swapToDescentSingleList() {
        val sortedFolders = folders.take(1).swapSortingToDescends(fromPosition = 0, toPosition = 0)
        assertEquals(0L, sortedFolders[0].sortIndex)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun swapToDescentSingleListWrongPosition() {
        val sortedFolders = folders.take(1).swapSortingToDescends(fromPosition = 0, toPosition = 1)
        assertEquals(0L, sortedFolders[0].sortIndex)
    }

    @Test
    fun swapToDescentToOnePositionLastItems() {
        val sortedFolders = folders.swapSortingToDescends(fromPosition = 1, toPosition = 2)
        assertEquals(3L, sortedFolders[3].sortIndex)
    }

    @Test
    fun swapToDescentToOnePositionFirstItems() {
        val sortedFolders = folders.swapSortingToDescends(fromPosition = 2, toPosition = 1)
        assertEquals(0L, sortedFolders[0].sortIndex)
    }
}