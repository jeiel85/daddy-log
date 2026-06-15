package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hair_photo_records")
data class HairPhotoRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format: yyyy-MM-dd
    val angleType: String, // "정면", "위쪽", "양쪽 측면", "정수리"
    val imagePath: String,
    val memo: String
)

@Entity(tableName = "scalp_condition_records")
data class ScalpConditionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format: yyyy-MM-dd (one unique record per day)
    val scalpState: String, // comma-separated, e.g., "건조함,가려움"
    val hairState: String, // comma-separated, e.g., "얇아짐,힘 없음"
    val stressScore: Int, // 1~5
    val sleepHours: Float,
    val alcohol: Boolean,
    val exercise: Boolean,
    val memo: String
)

@Entity(tableName = "hair_care_routines")
data class HairCareRoutine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "SHAMPOO", "TONIC", "HOSPITAL", "MEDICATION", "SUPPLEMENTS", "MASSAGE", "OTHER"
    val isActive: Boolean = true
)

@Entity(tableName = "hair_care_checks")
data class HairCareCheck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val routineId: Int,
    val date: String, // format: yyyy-MM-dd
    val isDone: Boolean
)
