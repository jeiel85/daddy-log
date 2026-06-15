package com.jeiel.daddylog.data

import kotlinx.coroutines.flow.Flow

class ScalpRepository(private val scalpDao: ScalpDao) {

    // === Photos ===
    val allPhotoRecords: Flow<List<HairPhotoRecord>> = scalpDao.getAllPhotoRecords()

    fun getPhotoRecordsByAngle(angle: String): Flow<List<HairPhotoRecord>> {
        return scalpDao.getPhotoRecordsByAngle(angle)
    }

    suspend fun insertPhotoRecord(record: HairPhotoRecord): Long {
        return scalpDao.insertPhotoRecord(record)
    }

    suspend fun deletePhotoRecord(record: HairPhotoRecord) {
        scalpDao.deletePhotoRecord(record)
    }

    // === Scalp Condition ===
    val allScalpRecords: Flow<List<ScalpConditionRecord>> = scalpDao.getAllScalpRecords()

    suspend fun getScalpRecordByDate(date: String): ScalpConditionRecord? {
        return scalpDao.getScalpRecordByDate(date)
    }

    fun getScalpRecordFlowByDate(date: String): Flow<ScalpConditionRecord?> {
        return scalpDao.getScalpRecordFlowByDate(date)
    }

    suspend fun insertScalpRecord(record: ScalpConditionRecord): Long {
        return scalpDao.insertScalpRecord(record)
    }

    suspend fun deleteScalpRecord(record: ScalpConditionRecord) {
        scalpDao.deleteScalpRecord(record)
    }

    // === Routines ===
    val allRoutinesFlow: Flow<List<HairCareRoutine>> = scalpDao.getAllRoutines()
    val activeRoutinesFlow: Flow<List<HairCareRoutine>> = scalpDao.getActiveRoutinesFlow()

    suspend fun insertRoutine(routine: HairCareRoutine): Long {
        return scalpDao.insertRoutine(routine)
    }

    suspend fun updateRoutine(routine: HairCareRoutine) {
        scalpDao.updateRoutine(routine)
    }

    suspend fun deleteRoutine(routine: HairCareRoutine) {
        scalpDao.deleteRoutine(routine)
    }

    // === Checks ===
    fun getChecksByDate(date: String): Flow<List<HairCareCheck>> {
        return scalpDao.getChecksByDate(date)
    }

    suspend fun getChecksByDateDirect(date: String): List<HairCareCheck> {
        return scalpDao.getChecksByDateDirect(date)
    }

    suspend fun saveCheck(routineId: Int, date: String, isDone: Boolean) {
        if (isDone) {
            scalpDao.insertCheck(HairCareCheck(routineId = routineId, date = date, isDone = true))
        } else {
            scalpDao.deleteCheck(routineId, date)
        }
    }

    // === Reset Data ===
    suspend fun clearAllData() {
        scalpDao.clearAllScalpRecords()
        scalpDao.clearAllRoutines()
        scalpDao.clearAllChecks()
        // Photos are saved inside directories, DB drops mapping. 
        // We will make sure DB resets correctly.
    }
}
