package com.awab.ai.data.database

import androidx.room.*
import com.awab.ai.data.models.*

@Database(
    entities = [
        FaceEntity::class,
        ConversationEntity::class,
        VoiceProfileEntity::class,
        UserPreferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun faceDao(): FaceDao
    abstract fun conversationDao(): ConversationDao
    abstract fun voiceProfileDao(): VoiceProfileDao
    abstract fun userPreferenceDao(): UserPreferenceDao
}

@Entity(tableName = "faces")
data class FaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val embeddings: FloatArray,
    val registrationDate: Long,
    val lastSeenDate: Long = registrationDate,
    val confidence: Float = 0.0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FaceEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userMessage: String,
    val aiResponse: String,
    val timestamp: Long,
    val messageType: String,
    val context: String? = null
)

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val speakerName: String,
    val voiceFeatures: FloatArray,
    val registrationDate: Long,
    val lastUsed: Long = registrationDate,
    val accuracy: Float = 0.0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VoiceProfileEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val lastModified: Long
)

@Dao
interface FaceDao {
    @Query("SELECT * FROM faces ORDER BY lastSeenDate DESC")
    suspend fun getAllFaces(): List<FaceEntity>

    @Query("SELECT * FROM faces WHERE id = :id")
    suspend fun getFaceById(id: Long): FaceEntity?

    @Query("SELECT * FROM faces WHERE name = :name")
    suspend fun getFaceByName(name: String): FaceEntity?

    @Insert
    suspend fun insertFace(face: FaceEntity): Long

    @Update
    suspend fun updateFace(face: FaceEntity)

    @Query("DELETE FROM faces WHERE id = :id")
    suspend fun deleteFaceById(id: Long)

    @Query("DELETE FROM faces")
    suspend fun deleteAllFaces()
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentConversations(limit: Int = 50): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE messageType = :type ORDER BY timestamp DESC")
    suspend fun getConversationsByType(type: String): List<ConversationEntity>

    @Insert
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("DELETE FROM conversations WHERE timestamp < :cutoffTime")
    suspend fun deleteOldConversations(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int
}

@Dao
interface VoiceProfileDao {
    @Query("SELECT * FROM voice_profiles ORDER BY lastUsed DESC")
    suspend fun getAllVoiceProfiles(): List<VoiceProfileEntity>

    @Query("SELECT * FROM voice_profiles WHERE id = :id")
    suspend fun getVoiceProfileById(id: Long): VoiceProfileEntity?

    @Query("SELECT * FROM voice_profiles WHERE speakerName = :name")
    suspend fun getVoiceProfileByName(name: String): VoiceProfileEntity?

    @Insert
    suspend fun insertVoiceProfile(profile: VoiceProfileEntity): Long

    @Update
    suspend fun updateVoiceProfile(profile: VoiceProfileEntity)

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun deleteVoiceProfileById(id: Long)
}

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE key = :key")
    suspend fun getPreference(key: String): UserPreferenceEntity?

    @Query("SELECT * FROM user_preferences")
    suspend fun getAllPreferences(): List<UserPreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(preference: UserPreferenceEntity)

    @Query("DELETE FROM user_preferences WHERE key = :key")
    suspend fun deletePreference(key: String)
}

class Converters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        return if (value.isEmpty()) floatArrayOf() 
        else value.split(",").map { it.toFloat() }.toFloatArray()
    }
}