package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM custom_tags ORDER BY createdAt ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM custom_tags ORDER BY createdAt ASC")
    suspend fun getAllTagsDirect(): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity)

    @Query("DELETE FROM custom_tags WHERE name = :name")
    suspend fun deleteTag(name: String)

    @Query("DELETE FROM custom_tags")
    suspend fun deleteAllTags()

    @Query("SELECT COUNT(*) FROM custom_tags")
    suspend fun getTagCount(): Int
}
