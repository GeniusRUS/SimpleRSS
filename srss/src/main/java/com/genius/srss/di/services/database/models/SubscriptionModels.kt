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
    val id: String,
    val name: String,
    val dateOfCreation: Long
)

@Entity(
    tableName = "subscription_folder",
    primaryKeys = ["urlOfSource", "folderId"]
)
data class SubscriptionFolderCrossRefDatabaseModel(
    @ColumnInfo(index = true)
    val urlOfSource: String,
    @ColumnInfo(index = true)
    val folderId: String
)

data class SubscriptionWithFolders (
    @Embedded
    val subscription: SubscriptionDatabaseModel,
    @Relation(
        parentColumn = "urlToLoad",
        entity = SubscriptionFolderDatabaseModel::class,
        entityColumn = "id",
        associateBy = Junction(
            value = SubscriptionFolderCrossRefDatabaseModel::class,
            parentColumn = "urlOfSource",
            entityColumn = "folderId"
        )
    )
    val folders: List<SubscriptionFolderDatabaseModel>
)

data class FolderWithSubscriptions (
    @Embedded
    val folder: SubscriptionFolderDatabaseModel,
    @Relation(
        parentColumn = "id",
        entity = SubscriptionDatabaseModel::class,
        entityColumn = "urlToLoad",
        associateBy = Junction(
            value = SubscriptionFolderCrossRefDatabaseModel::class,
            parentColumn = "folderId",
            entityColumn = "urlOfSource"
        )
    )
    val subscriptions: List<SubscriptionDatabaseModel>
)