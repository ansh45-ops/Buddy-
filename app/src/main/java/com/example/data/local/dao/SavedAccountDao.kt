package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SavedAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedAccountDao {

    @Query("SELECT * FROM saved_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<SavedAccountEntity>>

    @Query("SELECT * FROM saved_accounts WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentAccount(): SavedAccountEntity?

    @Query("SELECT * FROM saved_accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): SavedAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(account: SavedAccountEntity)

    @Query("UPDATE saved_accounts SET isCurrent = 0")
    suspend fun clearCurrentFlags()

    @Query("UPDATE saved_accounts SET isCurrent = 1 WHERE email = :email")
    suspend fun setCurrentAccount(email: String)

    @Query("DELETE FROM saved_accounts WHERE email = :email")
    suspend fun deleteAccount(email: String)

    @Query("DELETE FROM saved_accounts")
    suspend fun clearAll()
}
