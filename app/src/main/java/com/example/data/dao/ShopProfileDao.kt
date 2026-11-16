package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ShopProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopProfileDao {

    @Query("SELECT * FROM shop_profile WHERE id = 1 LIMIT 1")
    fun getShopProfile(): Flow<ShopProfileEntity?>

    @Query("SELECT * FROM shop_profile WHERE id = 1 LIMIT 1")
    suspend fun getShopProfileSync(): ShopProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ShopProfileEntity)

    @Update
    suspend fun updateProfile(profile: ShopProfileEntity)

    @Query("UPDATE shop_profile SET nextJobNumberSequence = nextJobNumberSequence + 1 WHERE id = 1")
    suspend fun incrementJobSequence()

    @Query("UPDATE shop_profile SET nextJobNumberSequence = :nextSequence, lastJobMonth = :monthKey, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateMonthlyJobSequence(nextSequence: Int, monthKey: String, updatedAt: Long)
}
