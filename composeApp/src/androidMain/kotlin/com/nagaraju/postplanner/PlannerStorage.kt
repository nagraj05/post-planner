package com.nagaraju.postplanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

internal data class StoredImage(
    val bitmap: ImageBitmap,
    val path: String,
)

internal data class StoredPlannerState(
    val profiles: List<PlannerProfile>,
    val activeProfileId: String?,
    val themeMode: AppThemeMode,
)

internal class PlannerStorage(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mediaDirectory = File(context.filesDir, MEDIA_DIRECTORY_NAME).apply { mkdirs() }

    fun load(): StoredPlannerState {
        val rawState = preferences.getString(STATE_KEY, null)
            ?: return StoredPlannerState(emptyList(), null, AppThemeMode.System)

        return runCatching {
            val root = JSONObject(rawState)
            val profileArray = root.optJSONArray("profiles") ?: JSONArray()
            val profiles = buildList {
                for (index in 0 until profileArray.length()) {
                    add(profileFromJson(profileArray.getJSONObject(index)))
                }
            }
            val requestedActiveId = root.nullableString("activeProfileId")
            StoredPlannerState(
                profiles = profiles,
                activeProfileId = requestedActiveId?.takeIf { id -> profiles.any { it.id == id } }
                    ?: profiles.firstOrNull()?.id,
                themeMode = runCatching {
                    AppThemeMode.valueOf(root.optString("themeMode", AppThemeMode.System.name))
                }.getOrDefault(AppThemeMode.System),
            )
        }.getOrElse {
            StoredPlannerState(emptyList(), null, AppThemeMode.System)
        }
    }

    fun save(
        profiles: List<PlannerProfile>,
        activeProfileId: String?,
        themeMode: AppThemeMode,
    ) {
        val root = JSONObject().apply {
            put("version", STATE_VERSION)
            put("activeProfileId", activeProfileId)
            put("themeMode", themeMode.name)
            put("profiles", JSONArray().apply {
                profiles.forEach { put(it.toJson()) }
            })
        }
        preferences.edit().putString(STATE_KEY, root.toString()).commit()
        removeUnusedMedia(profiles)
    }

    fun importImage(uri: Uri, filePrefix: String): StoredImage? = runCatching {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(900, 1200), null)
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } ?: return null

        val safePrefix = filePrefix.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val imageFile = File(mediaDirectory, "${safePrefix}_${UUID.randomUUID()}.jpg")
        FileOutputStream(imageFile).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output))
            output.fd.sync()
        }
        StoredImage(bitmap.asImageBitmap(), imageFile.absolutePath)
    }.getOrNull()

    private fun profileFromJson(json: JSONObject): PlannerProfile {
        val profilePicturePath = json.nullableString("profilePicturePath")
        return PlannerProfile(
            id = json.getString("id"),
            handle = json.getString("handle"),
            photos = json.photoList("photos"),
            archivedPhotos = json.photoList("archivedPhotos"),
            displayName = json.optString("displayName"),
            bio = json.optString("bio"),
            website = json.optString("website"),
            profilePicture = profilePicturePath?.loadBitmap(),
            profilePicturePath = profilePicturePath,
            followersCount = json.optLong("followersCount"),
            followingCount = json.optLong("followingCount"),
        )
    }

    private fun PlannerProfile.toJson() = JSONObject().apply {
        put("id", id)
        put("handle", handle)
        put("photos", photos.toJson())
        put("archivedPhotos", archivedPhotos.toJson())
        put("displayName", displayName)
        put("bio", bio)
        put("website", website)
        put("profilePicturePath", profilePicturePath)
        put("followersCount", followersCount)
        put("followingCount", followingCount)
    }

    private fun List<PhotoItem>.toJson() = JSONArray().apply {
        forEach { photo ->
            photo.storagePath?.let { path ->
                put(JSONObject().apply {
                    put("id", photo.id)
                    put("storagePath", path)
                })
            }
        }
    }

    private fun JSONObject.photoList(key: String): List<PhotoItem> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val path = item.getString("storagePath")
                path.loadBitmap()?.let { bitmap ->
                    add(PhotoItem(id = item.getString("id"), bitmap = bitmap, storagePath = path))
                }
            }
        }
    }

    private fun String.loadBitmap(): ImageBitmap? {
        if (!File(this).isFile) return null
        return BitmapFactory.decodeFile(this)?.asImageBitmap()
    }

    private fun JSONObject.nullableString(key: String): String? {
        if (isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun removeUnusedMedia(profiles: List<PlannerProfile>) {
        val referencedPaths = buildSet {
            profiles.forEach { profile ->
                profile.profilePicturePath?.let(::add)
                profile.photos.mapNotNullTo(this) { it.storagePath }
                profile.archivedPhotos.mapNotNullTo(this) { it.storagePath }
            }
        }
        mediaDirectory.listFiles()?.forEach { file ->
            if (file.absolutePath !in referencedPaths) {
                file.delete()
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "post_planner_storage"
        const val STATE_KEY = "planner_state"
        const val STATE_VERSION = 1
        const val MEDIA_DIRECTORY_NAME = "planner_media"
        const val JPEG_QUALITY = 90
    }
}
