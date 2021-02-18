package com.genius.srss.di.services.database.dao

import androidx.room.*
import com.genius.srss.di.services.database.models.*

@Dao
interface SubscriptionsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSubscription(subscription: SubscriptionDatabaseModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFolder(folder: SubscriptionFolderDatabaseModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSubscriptionFolderCrossRef(crossRef: SubscriptionFolderCrossRefDatabaseModel)

    @Query("SELECT * FROM subscriptions")
    suspend fun loadSubscriptions(): List<SubscriptionDatabaseModel>

    @Query("SELECT * FROM subscriptions WHERE urlToLoad == :urlToLoad")
    suspend fun loadSubscriptionById(urlToLoad: String): SubscriptionDatabaseModel?

    @Query("SELECT * FROM subscriptions LEFT OUTER JOIN subscription_folder ON subscriptions.urlToLoad = subscription_folder.urlOfSource WHERE subscription_folder.urlOfSource IS NULL")
    suspend fun loadSubscriptionsWithoutFolders(): List<SubscriptionDatabaseModel>

    @Query("SELECT * FROM folders")
    suspend fun loadAllFolders(): List<SubscriptionFolderDatabaseModel>

    @Transaction
    @Query("SELECT * FROM folders WHERE id == :folderId LIMIT 1")
    suspend fun loadFolderWithSubscriptionsById(folderId: String): FolderWithSubscriptions?

    @Transaction
    @Query("SELECT * FROM subscriptions WHERE urlToLoad == :urlToLoad LIMIT 1")
    suspend fun loadSubscriptionWithFoldersByUrl(urlToLoad: String): SubscriptionWithFolders?

    @Query("SELECT COUNT(*) FROM subscription_folder WHERE folderId == :folderId")
    suspend fun getCrossRefCountByFolderId(folderId: String): Int

    @Query("DELETE FROM subscription_folder WHERE :folderId IS NULL OR folderId == :folderId AND :urlToLoad IS NULL OR urlOfSource == :urlToLoad")
    suspend fun removeCrossRefsById(folderId: String? = null, urlToLoad: String? = null)

    /**
     * Do not must be used directly from the code, only in complex function [complexRemoveSubscriptionByUrl]
     */
    @Query("DELETE FROM subscriptions WHERE urlToLoad == :urlToLoad")
    suspend fun removeSubscriptionByUrl(urlToLoad: String)

    /**
     * Do not must be used directly from the code, only in complex function [complexRemoveFolderById]
     */
    @Query("DELETE FROM folders WHERE id == :folderId")
    suspend fun removeFolderById(folderId: String)

    @Transaction
    suspend fun complexRemoveSubscriptionByUrl(urlToLoad: String) {
        removeCrossRefsById(urlToLoad = urlToLoad)
        removeSubscriptionByUrl(urlToLoad = urlToLoad)
    }

    @Transaction
    suspend fun complexRemoveFolderById(folderId: String) {
        removeCrossRefsById(folderId = folderId)
        removeFolderById(folderId = folderId)
    }
}