package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ExportResolution
import com.example.model.ProjectData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "ss_studio_prefs")

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val clipCount: Int,
    val durationMs: Long,
    val thumbnailUri: String?,
    val aspectRatioLabel: String,
    val serializedJson: String,
    val updatedAtMs: Long
)

@Entity(tableName = "exports")
data class ExportEntity(
    @PrimaryKey val id: String,
    val projectTitle: String,
    val filePath: String,
    val durationMs: Long,
    val resolutionLabel: String,
    val fileSizeBytes: Long,
    val createdAtMs: Long
)

@Dao
interface StudioDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtMs DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("SELECT * FROM exports ORDER BY createdAtMs DESC")
    fun observeExports(): Flow<List<ExportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(entity: ExportEntity)

    @Query("DELETE FROM exports WHERE id = :id")
    suspend fun deleteExportById(id: String)
}

@Database(
    entities = [ProjectEntity::class, ExportEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "ss_studio_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

data class StudioPreferences(
    val defaultResolution: ExportResolution = ExportResolution.RES_1080P_PLUS,
    val defaultFps: Int = 60,
    val addEndingWatermark: Boolean = false,
    val autoSaveDrafts: Boolean = true,
    val batterySaverMode: Boolean = false,
    val autoSyncOnline: Boolean = true,
    val lastCloudSyncMs: Long = 0L,
    val appThemeMode: String = "dark",
    val customBgUri: String = "",
    val customBgBlur: Float = 2f,
    val linkedPhone: String = "",
    val isPakistaniPro: Boolean = false,
    val profileCoverStyle: String = "CYBER_CYAN",
    val profileLocalPhotoUri: String = "",
    val profileLocalBannerUri: String = "",
    val studioPasswordHash: String = "",
    val studioPasswordSalt: String = "",
    val registeredDevicesJson: String = "[]",
    val aiVideoTrialCount: Int = 0
)

class StudioRepository(
    private val context: Context,
    private val dao: StudioDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val projects: Flow<List<ProjectData>> = dao.observeProjects().map { list ->
        list.mapNotNull { entity ->
            runCatching { json.decodeFromString<ProjectData>(entity.serializedJson) }.getOrNull()
        }
    }

    val exports: Flow<List<ExportEntity>> = dao.observeExports()

    val preferences: Flow<StudioPreferences> = context.studioDataStore.data.map { prefs ->
        val resName = prefs[KEY_DEFAULT_RES] ?: ExportResolution.RES_1080P_PLUS.name
        val res = runCatching { ExportResolution.valueOf(resName) }.getOrDefault(ExportResolution.RES_1080P_PLUS)
        StudioPreferences(
            defaultResolution = res,
            defaultFps = prefs[KEY_DEFAULT_FPS] ?: 60,
            addEndingWatermark = prefs[KEY_WATERMARK] ?: false,
            autoSaveDrafts = prefs[KEY_AUTOSAVE] ?: true,
            batterySaverMode = prefs[KEY_BATTERY_SAVER] ?: false,
            autoSyncOnline = prefs[KEY_AUTO_SYNC] ?: true,
            lastCloudSyncMs = prefs[KEY_LAST_CLOUD_SYNC] ?: 0L,
            appThemeMode = prefs[KEY_THEME_MODE] ?: "dark",
            customBgUri = prefs[KEY_CUSTOM_BG_URI] ?: "",
            customBgBlur = prefs[KEY_CUSTOM_BG_BLUR] ?: 2f,
            linkedPhone = prefs[KEY_LINKED_PHONE] ?: "",
            isPakistaniPro = prefs[KEY_IS_PAKISTANI_PRO] ?: false,
            profileCoverStyle = prefs[KEY_PROFILE_COVER_STYLE] ?: "CYBER_CYAN",
            profileLocalPhotoUri = prefs[KEY_PROFILE_LOCAL_PHOTO] ?: "",
            profileLocalBannerUri = prefs[KEY_PROFILE_LOCAL_BANNER] ?: "",
            studioPasswordHash = prefs[KEY_STUDIO_PWD_HASH] ?: "",
            studioPasswordSalt = prefs[KEY_STUDIO_PWD_SALT] ?: "",
            registeredDevicesJson = prefs[KEY_REGISTERED_DEVICES] ?: "[]",
            aiVideoTrialCount = prefs[KEY_AI_VIDEO_TRIALS] ?: 0
        )
    }

    suspend fun saveProject(project: ProjectData) {
        val updated = project.copy(updatedAtMs = System.currentTimeMillis())
        val entity = ProjectEntity(
            id = updated.id,
            title = updated.title,
            clipCount = updated.clips.size,
            durationMs = updated.totalDurationMs,
            thumbnailUri = updated.clips.firstOrNull()?.uriString,
            aspectRatioLabel = updated.aspectRatio.label,
            serializedJson = json.encodeToString(updated),
            updatedAtMs = updated.updatedAtMs
        )
        dao.upsertProject(entity)
    }

    suspend fun getProject(id: String): ProjectData? {
        val entity = dao.getProjectById(id) ?: return null
        return runCatching { json.decodeFromString<ProjectData>(entity.serializedJson) }.getOrNull()
    }

    suspend fun deleteProject(id: String) {
        dao.deleteProjectById(id)
    }

    suspend fun recordExport(exportEntity: ExportEntity) {
        dao.insertExport(exportEntity)
    }

    suspend fun deleteExport(exportEntity: ExportEntity) {
        runCatching { java.io.File(exportEntity.filePath).delete() }
        dao.deleteExportById(exportEntity.id)
    }

    suspend fun updatePreferences(
        defaultResolution: ExportResolution? = null,
        defaultFps: Int? = null,
        addEndingWatermark: Boolean? = null,
        autoSaveDrafts: Boolean? = null,
        batterySaverMode: Boolean? = null,
        autoSyncOnline: Boolean? = null,
        lastCloudSyncMs: Long? = null,
        appThemeMode: String? = null,
        customBgUri: String? = null,
        customBgBlur: Float? = null,
        linkedPhone: String? = null,
        isPakistaniPro: Boolean? = null,
        profileCoverStyle: String? = null,
        profileLocalPhotoUri: String? = null,
        profileLocalBannerUri: String? = null,
        studioPasswordHash: String? = null,
        studioPasswordSalt: String? = null,
        registeredDevicesJson: String? = null,
        aiVideoTrialCount: Int? = null
    ) {
        context.studioDataStore.edit { prefs ->
            defaultResolution?.let { prefs[KEY_DEFAULT_RES] = it.name }
            defaultFps?.let { prefs[KEY_DEFAULT_FPS] = it }
            addEndingWatermark?.let { prefs[KEY_WATERMARK] = it }
            autoSaveDrafts?.let { prefs[KEY_AUTOSAVE] = it }
            batterySaverMode?.let { prefs[KEY_BATTERY_SAVER] = it }
            autoSyncOnline?.let { prefs[KEY_AUTO_SYNC] = it }
            lastCloudSyncMs?.let { prefs[KEY_LAST_CLOUD_SYNC] = it }
            appThemeMode?.let { prefs[KEY_THEME_MODE] = it }
            customBgUri?.let { prefs[KEY_CUSTOM_BG_URI] = it }
            customBgBlur?.let { prefs[KEY_CUSTOM_BG_BLUR] = it }
            linkedPhone?.let { prefs[KEY_LINKED_PHONE] = it }
            isPakistaniPro?.let { prefs[KEY_IS_PAKISTANI_PRO] = it }
            profileCoverStyle?.let { prefs[KEY_PROFILE_COVER_STYLE] = it }
            profileLocalPhotoUri?.let { prefs[KEY_PROFILE_LOCAL_PHOTO] = it }
            profileLocalBannerUri?.let { prefs[KEY_PROFILE_LOCAL_BANNER] = it }
            studioPasswordHash?.let { prefs[KEY_STUDIO_PWD_HASH] = it }
            studioPasswordSalt?.let { prefs[KEY_STUDIO_PWD_SALT] = it }
            registeredDevicesJson?.let { prefs[KEY_REGISTERED_DEVICES] = it }
            aiVideoTrialCount?.let { prefs[KEY_AI_VIDEO_TRIALS] = it }
        }
    }

    companion object {
        private val KEY_DEFAULT_RES = stringPreferencesKey("default_res")
        private val KEY_DEFAULT_FPS = intPreferencesKey("default_fps")
        private val KEY_WATERMARK = booleanPreferencesKey("ending_watermark")
        private val KEY_AUTOSAVE = booleanPreferencesKey("autosave_drafts")
        private val KEY_BATTERY_SAVER = booleanPreferencesKey("battery_saver_mode")
        private val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync_online")
        private val KEY_LAST_CLOUD_SYNC = longPreferencesKey("last_cloud_sync_ms")
        private val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
        private val KEY_CUSTOM_BG_URI = stringPreferencesKey("custom_bg_uri")
        private val KEY_CUSTOM_BG_BLUR = floatPreferencesKey("custom_bg_blur")
        private val KEY_LINKED_PHONE = stringPreferencesKey("linked_phone")
        private val KEY_IS_PAKISTANI_PRO = booleanPreferencesKey("is_pakistani_pro")
        private val KEY_PROFILE_COVER_STYLE = stringPreferencesKey("profile_cover_style")
        private val KEY_PROFILE_LOCAL_PHOTO = stringPreferencesKey("profile_local_photo")
        private val KEY_PROFILE_LOCAL_BANNER = stringPreferencesKey("profile_local_banner")
        private val KEY_STUDIO_PWD_HASH = stringPreferencesKey("studio_pwd_hash")
        private val KEY_STUDIO_PWD_SALT = stringPreferencesKey("studio_pwd_salt")
        private val KEY_REGISTERED_DEVICES = stringPreferencesKey("registered_devices_json")
        private val KEY_AI_VIDEO_TRIALS = intPreferencesKey("ai_video_trials_used")
    }
}
