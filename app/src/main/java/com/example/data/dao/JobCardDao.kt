package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.JobCardEntity
import com.example.data.model.RepairStage
import kotlinx.coroutines.flow.Flow

@Dao
interface JobCardDao {

    @Query("SELECT * FROM job_cards ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<JobCardEntity>>

    @Query("SELECT * FROM job_cards WHERE id = :id")
    fun getJobById(id: Long): Flow<JobCardEntity?>

    @Query("SELECT * FROM job_cards WHERE id = :id")
    suspend fun getJobByIdSync(id: Long): JobCardEntity?

    @Query("SELECT * FROM job_cards ORDER BY createdAt DESC")
    suspend fun getAllJobsSync(): List<JobCardEntity>

    @Query("SELECT * FROM job_cards WHERE jobNumber = :jobNumber LIMIT 1")
    suspend fun getJobByNumberSync(jobNumber: String): JobCardEntity?

    @Query("SELECT * FROM job_cards WHERE recordUuid = :uuid LIMIT 1")
    suspend fun getJobByUuidSync(uuid: String): JobCardEntity?

    @Query("SELECT * FROM job_cards WHERE stage = :stage ORDER BY createdAt DESC")
    fun getJobsByStage(stage: RepairStage): Flow<List<JobCardEntity>>

    @Query("""
        SELECT * FROM job_cards 
        WHERE customerName LIKE '%' || :query || '%' 
           OR customerPhone LIKE '%' || :query || '%' 
           OR jobNumber LIKE '%' || :query || '%' 
           OR deviceModel LIKE '%' || :query || '%' 
           OR imeiOrSerial LIKE '%' || :query || '%' 
        ORDER BY createdAt DESC
    """)
    fun searchJobs(query: String): Flow<List<JobCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jobs: List<JobCardEntity>)

    @Update
    suspend fun updateJob(job: JobCardEntity)

    @Delete
    suspend fun deleteJob(job: JobCardEntity)

    @Query("DELETE FROM job_cards WHERE id = :id")
    suspend fun deleteJobById(id: Long)

    @Query("SELECT COUNT(*) FROM job_cards")
    suspend fun getJobCount(): Int
}
