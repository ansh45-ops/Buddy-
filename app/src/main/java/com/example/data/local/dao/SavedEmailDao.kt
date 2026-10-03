package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SavedEmailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedEmailDao {

    @Query("SELECT * FROM saved_emails WHERE accountEmail = :accountEmail ORDER BY receivedAt DESC")
    fun getEmailsForAccount(accountEmail: String): Flow<List<SavedEmailEntity>>

    @Query("SELECT * FROM saved_emails WHERE isStarred = 1 ORDER BY receivedAt DESC")
    fun getStarredEmails(): Flow<List<SavedEmailEntity>>

    @Query("SELECT * FROM saved_emails WHERE id = :id LIMIT 1")
    suspend fun getEmailById(id: String): SavedEmailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmails(emails: List<SavedEmailEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: SavedEmailEntity)

    @Query("UPDATE saved_emails SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateStarred(id: String, isStarred: Boolean)

    @Query("UPDATE saved_emails SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM saved_emails WHERE id = :id")
    suspend fun deleteEmail(id: String)

    @Query("DELETE FROM saved_emails WHERE accountEmail = :accountEmail")
    suspend fun clearEmailsForAccount(accountEmail: String)
}
