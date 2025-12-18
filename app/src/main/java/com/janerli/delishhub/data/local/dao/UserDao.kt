package com.janerli.delishhub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.janerli.delishhub.data.local.entity.UserLocalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUser(id: String): Flow<UserLocalEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUser(id: String): UserLocalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserLocalEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: String)
}
