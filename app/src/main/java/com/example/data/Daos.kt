package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScalpDao {
    // === HairPhotoRecord ===
    @Query("SELECT * FROM hair_photo_records ORDER BY date DESC, id DESC")
    fun getAllPhotoRecords(): Flow<List<HairPhotoRecord>>

    @Query("SELECT * FROM hair_photo_records WHERE date = :date")
    suspend fun getPhotoRecordsByDate(date: String): List<HairPhotoRecord>

    @Query("SELECT * FROM hair_photo_records WHERE angleType = :angleType ORDER BY date DESC")
    fun getPhotoRecordsByAngle(angleType: String): Flow<List<HairPhotoRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoRecord(record: HairPhotoRecord): Long

    @Delete
    suspend fun deletePhotoRecord(record: HairPhotoRecord)

    // === ScalpConditionRecord ===
    @Query("SELECT * FROM scalp_condition_records ORDER BY date DESC")
    fun getAllScalpRecords(): Flow<List<ScalpConditionRecord>>

    @Query("SELECT * FROM scalp_condition_records WHERE date = :date LIMIT 1")
    suspend fun getScalpRecordByDate(date: String): ScalpConditionRecord?

    @Query("SELECT * FROM scalp_condition_records WHERE date = :date")
    fun getScalpRecordFlowByDate(date: String): Flow<ScalpConditionRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScalpRecord(record: ScalpConditionRecord): Long

    @Delete
    suspend fun deleteScalpRecord(record: ScalpConditionRecord)

    @Query("DELETE FROM scalp_condition_records")
    suspend fun clearAllScalpRecords()

    // === HairCareRoutine ===
    @Query("SELECT * FROM hair_care_routines")
    fun getAllRoutines(): Flow<List<HairCareRoutine>>

    @Query("SELECT * FROM hair_care_routines WHERE isActive = 1")
    fun getActiveRoutinesFlow(): Flow<List<HairCareRoutine>>

    @Query("SELECT * FROM hair_care_routines WHERE isActive = 1")
    suspend fun getActiveRoutines(): List<HairCareRoutine>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: HairCareRoutine): Long

    @Update
    suspend fun updateRoutine(routine: HairCareRoutine)

    @Delete
    suspend fun deleteRoutine(routine: HairCareRoutine)

    @Query("DELETE FROM hair_care_routines")
    suspend fun clearAllRoutines()

    // === HairCareCheck ===
    @Query("SELECT * FROM hair_care_checks WHERE date = :date")
    fun getChecksByDate(date: String): Flow<List<HairCareCheck>>

    @Query("SELECT * FROM hair_care_checks WHERE date = :date")
    suspend fun getChecksByDateDirect(date: String): List<HairCareCheck>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheck(check: HairCareCheck): Long

    @Query("DELETE FROM hair_care_checks WHERE routineId = :routineId AND date = :date")
    suspend fun deleteCheck(routineId: Int, date: String)

    @Query("DELETE FROM hair_care_checks")
    suspend fun clearAllChecks()
}
