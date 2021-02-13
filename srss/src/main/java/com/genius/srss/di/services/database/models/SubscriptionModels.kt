package com.genius.srss.di.services.database.models

import androidx.room.*

@Entity(tableName = "subscriptions")
data class SubscriptionDatabaseModel(
    @PrimaryKey
    val urlToLoad: String,
    val title: String,
    val dateOfSubscription: Long
)

@Entity(tableName = "folders")
data class SubscriptionFolderDatabaseModel(
    @PrimaryKey
    val folderId: String,
    val name: String,
    val dateOfCreation: Long
)

@Entity(
    tableName = "subscription_folder",
    primaryKeys = ["urlToLoad", "folderId"]
)
data class SubscriptionFolderCrossRefDatabaseModel(
    @ColumnInfo(index = true)
    val urlToLoad: String,
    @ColumnInfo(index = true)
    val folderId: String
)

data class SubscriptionWithFolders (
    @Embedded
    val subscription: SubscriptionDatabaseModel,
    @Relation(
        parentColumn = "urlToLoad",
        entity = SubscriptionFolderDatabaseModel::class,
        entityColumn = "folderId",
        associateBy = Junction(
            value = SubscriptionFolderCrossRefDatabaseModel::class,
            parentColumn = "urlToLoad",
            entityColumn = "folderId"
        )
    )
    val folders: List<SubscriptionFolderDatabaseModel>
)

data class FolderWithSubscriptions (
    @Embedded
    val folder: SubscriptionFolderDatabaseModel,
    @Relation(
        parentColumn = "folderId",
        entity = SubscriptionDatabaseModel::class,
        entityColumn = "urlToLoad",
        associateBy = Junction(
            value = SubscriptionFolderCrossRefDatabaseModel::class,
            parentColumn = "folderId",
            entityColumn = "urlToLoad"
        )
    )
    val subscriptions: List<SubscriptionDatabaseModel>
)