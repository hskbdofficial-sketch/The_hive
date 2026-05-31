package com.example.data.database

import androidx.room.*
import com.example.data.models.LossRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface LossDao {
    @Query("SELECT * FROM loss_records ORDER BY dateOfLoss DESC")
    fun getAllLosses(): Flow<List<LossRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoss(lossRecord: LossRecord)

    @Delete
    suspend fun deleteLoss(lossRecord: LossRecord)

    @Query("DELETE FROM loss_records WHERE id = :id")
    suspend fun deleteLossById(id: String)
}
