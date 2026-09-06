package com.wallkraft.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * A user-created favorites collection ("Beach", "Dark", …).
 *
 * Membership lives in [CollectionItemEntity] (many-to-many): one wallpaper
 * can belong to several collections. Names are unique — creating a duplicate
 * is a no-op that resolves to the existing row.
 */
@Entity(
    tableName = "collections",
    indices = [Index(value = ["name"], unique = true)],
)
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "wallpaperId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FavoriteEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallpaperId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("wallpaperId")],
)
data class CollectionItemEntity(
    val collectionId: Long,
    val wallpaperId: String,
)

/** A collection with its member item rows. Covers resolve from favorites. */
data class CollectionWithItems(
    @Embedded val collection: CollectionEntity,
    @Relation(parentColumn = "id", entityColumn = "collectionId")
    val items: List<CollectionItemEntity>,
)
