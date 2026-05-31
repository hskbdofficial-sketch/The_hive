package com.example.data.database

import androidx.room.*
import com.example.data.models.ReturnItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ReturnDao {
    @Query("SELECT * FROM return_items ORDER BY returnDate DESC")
    fun getAllReturns(): Flow<List<ReturnItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(returnItem: ReturnItem)

    @Delete
    suspend fun deleteReturn(returnItem: ReturnItem)

    @Query("DELETE FROM return_items WHERE id = :id")
    suspend fun deleteReturnById(id: String)
}
