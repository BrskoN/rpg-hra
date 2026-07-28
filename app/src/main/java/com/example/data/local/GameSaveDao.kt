package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSaveDao {
    @Query("SELECT * FROM game_save WHERE id = 1 LIMIT 1")
    fun getSaveFlow(): Flow<GameSaveEntity?>

    @Query("SELECT * FROM game_save WHERE id = 1 LIMIT 1")
    suspend fun getSaveSync(): GameSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSave(save: GameSaveEntity)

    @Query("DELETE FROM game_save WHERE id = 1")
    suspend fun deleteSave()
}
