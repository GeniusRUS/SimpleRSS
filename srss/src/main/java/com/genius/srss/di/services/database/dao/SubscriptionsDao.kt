package com.genius.srss.di.services.database.dao

import androidx.room.*
import com.genius.srss.di.services.database.models.*
import com.genius.srss.util.swapSortingToDescends
import com.genius.srss.util.swapSortingToAscends
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface SubscriptionsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSubscription(subscription: SubscriptionDatabaseModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFolder(folder: SubscriptionFolderDatabaseModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFolders(folders: List<SubscriptionFolderDatabaseModel>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun saveSubscriptionFolderCrossRef(crossRef: SubscriptionFolderCrossRefDatabaseModel)

    @Query("SELECT * FROM subscriptions")
    fun loadSubscriptions(): Flow<List<SubscriptionDatabaseModel>>

    @Query("SELECT * FROM subscriptions WHERE urlToLoad == :urlToLoad")
    suspend fun loadSubscriptionById(urlToLoad: String): SubscriptionDatabaseModel?

    @Query("UPDATE subscriptions SET title = :newTitle WHERE urlToLoad = :urlToLoad")
    suspend fun updateSubscriptionTitleByUrl(urlToLoad: String, newTitle: String)

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM subscriptions LEFT OUTER JOIN subscription_folder ON subscriptions.urlToLoad = subscription_folder.urlOfSource WHERE subscription_folder.urlOfSource IS NULL")
    fun loadSubscriptionsWithoutFolders(): Flow<List<SubscriptionDatabaseModel>>

    @Query("SELECT * FROM folders ORDER BY sortIndex")
    fun loadAllFolders(): Flow<List<SubscriptionFolderDatabaseModel>>

    @Query("SELECT sortIndex FROM folders ORDER BY sortIndex DESC LIMIT 1")
    suspend fun getLastFolderSortIndex(): Long?

    /**
     * Loads all [SubscriptionFolderDatabaseModel], automatically sorts them
     * and saves if it hasn't been done before
     *
     * Sorting by [SubscriptionFolderDatabaseModel.sortIndex] field
     *
     * This is necessary for correct display of folders after database migration from version 2 to version 3
     */
    suspend fun loadAllFoldersWithAutoSortingIfNeeded(): Flow<List<SubscriptionFolderDatabaseModel>> {
        return loadAllFolders().map { folders ->
            if (folders.firstOrNull { it.sortIndex == -1L } != null) {
                val foldersSortedByDate = folders.sortedBy { it.dateOfCreation }
                val lastSortedId = foldersSortedByDate.maxOf { it.sortIndex }
                val foldersToUpdate = foldersSortedByDate
                    .filter {
                        it.sortIndex == -1L
                    }
                    .mapIndexed { index, folder ->
                        folder.copy(
                            sortIndex = lastSortedId + index + 1
                        )
                    }
                saveFolders(foldersToUpdate)
                foldersToUpdate
            } else {
                folders
            }
        }
    }

    suspend fun changeFolderSort(fromPosition: Int, toPosition: Int) {
        when {
            // dragging item from bottom to upper position
            fromPosition > toPosition -> {
                loadAllFolders().map { folders ->
                    folders.swapSortingToAscends(fromPosition, toPosition)
                }.collect { updatedFolders ->
                    saveFolders(updatedFolders)
                }
            }
            // dragging item from top to lower position
            fromPosition < toPosition -> {
                loadAllFolders().map { folders ->
                    folders.swapSortingToDescends(fromPosition, toPosition)
                }.collect { updatedFolders ->
                    saveFolders(updatedFolders)
                }
            }
            else -> return
        }
    }

    @Transaction
    @Query("SELECT * FROM folders WHERE id == :folderId LIMIT 1")
    suspend fun loadFolderWithSubscriptionsById(folderId: String): FolderWithSubscriptions?

    @Transaction
    @Query("SELECT * FROM folders WHERE id == :folderId LIMIT 1")
    fun loadFolderWithSubscriptionsByIdFlow(folderId: String): Flow<FolderWithSubscriptions?>

    @Query("UPDATE folders SET name = :newName WHERE id = :folderId")
    suspend fun updateFolderNameById(folderId: String, newName: String)

    @Transaction
    @Query("SELECT * FROM subscriptions WHERE urlToLoad == :urlToLoad LIMIT 1")
    suspend fun loadSubscriptionWithFoldersByUrl(urlToLoad: String): SubscriptionWithFolders?

    @Query("SELECT COUNT(*) FROM subscription_folder WHERE folderId == :folderId")
    suspend fun getCrossRefCountByFolderId(folderId: String): Int

    @Query("DELETE FROM subscription_folder WHERE urlOfSource == :urlToLoad AND folderId == :folderId")
    suspend fun removeSingleCrossRefsByParameters(folderId: String? = null, urlToLoad: String? = null)

    @Query("DELETE FROM subscription_folder WHERE folderId == :folderId")
    suspend fun removeCrossRefsByFolderId(folderId: String)

    @Query("DELETE FROM subscription_folder WHERE urlOfSource == :urlToLoad")
    suspend fun removeCrossRefsByUrlOfSource(urlToLoad: String)

    @Query("DELETE FROM subscriptions WHERE urlToLoad == :urlToLoad")
    @Deprecated(
        message = "Do not must be used directly from the code, only in complex function",
        replaceWith = ReplaceWith("complexRemoveSubscriptionByUrl(urlToLoad)"),
        level = DeprecationLevel.ERROR
    )
    suspend fun removeSubscriptionByUrl(urlToLoad: String)

    @Query("DELETE FROM folders WHERE id == :folderId")
    @Deprecated(
        message = "Do not must be used directly from the code, only in complex function",
        replaceWith = ReplaceWith("complexRemoveFolderById(folderId)"),
        level = DeprecationLevel.ERROR
    )
    suspend fun removeFolderById(folderId: String)

    @Suppress("DEPRECATION_ERROR")
    @Transaction
    suspend fun complexRemoveSubscriptionByUrl(urlToLoad: String) {
        removeCrossRefsByUrlOfSource(urlToLoad = urlToLoad)
        removeSubscriptionByUrl(urlToLoad = urlToLoad)
    }

    @Suppress("DEPRECATION_ERROR")
    @Transaction
    suspend fun complexRemoveFolderById(folderId: String) {
        removeCrossRefsByFolderId(folderId = folderId)
        removeFolderById(folderId = folderId)
    }
}