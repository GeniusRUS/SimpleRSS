package com.genius.srss

import com.genius.srss.di.services.database.models.SubscriptionFolderDatabaseModel
import com.genius.srss.util.swapSortingToAscends
import org.junit.Test

import org.junit.Assert.*
import java.lang.IndexOutOfBoundsException

class FolderSortingSwapAscentTest {

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
    fun swapToAscentEmptyList() {
        val sortedFolders = emptyList<SubscriptionFolderDatabaseModel>().swapSortingToAscends(
            fromPosition = 0,
            toPosition = 0
        )
        assertEquals(0, sortedFolders.size)
    }

    @Test
    fun swapToAscentEmptyListWrongPosition() {
        val sortedFolders = emptyList<SubscriptionFolderDatabaseModel>().swapSortingToAscends(
            fromPosition = 2,
            toPosition = 1
        )
        assertEquals(0, sortedFolders.size)
    }

    @Test
    fun swapToAscentToOnePosition() {
        val sortedFolders = folders.swapSortingToAscends(fromPosition = 2, toPosition = 1)
        assertEquals(1L, sortedFolders[2].sortIndex)
    }

    @Test
    fun swapToAscentToOnePositionOnStartList() {
        val sortedFolders = folders.swapSortingToAscends(fromPosition = 1, toPosition = 0)
        assertEquals(0L, sortedFolders[1].sortIndex)
    }

    @Test
    fun swapToAscentOnStartList() {
        val sortedFolders = folders.swapSortingToAscends(fromPosition = 3, toPosition = 0)
        assertEquals(0L, sortedFolders[3].sortIndex)
    }

    @Test
    fun swapToAscentSingleList() {
        val sortedFolders = folders.take(1).swapSortingToAscends(fromPosition = 0, toPosition = 0)
        assertEquals(0L, sortedFolders[0].sortIndex)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun swapToAscentSingleListWrongPosition() {
        val sortedFolders = folders.take(1).swapSortingToAscends(fromPosition = 1, toPosition = 0)
        assertEquals(0L, sortedFolders[0].sortIndex)
    }

    @Test
    fun swapToAscentToOnePositionLastItems() {
        val sortedFolders = folders.swapSortingToAscends(fromPosition = 2, toPosition = 1)
        assertEquals(3L, sortedFolders[3].sortIndex)
    }

    @Test
    fun swapToAscentToOnePositionFirstItems() {
        val sortedFolders = folders.swapSortingToAscends(fromPosition = 2, toPosition = 1)
        assertEquals(0L, sortedFolders[0].sortIndex)
    }
}