package com.nagaraju.postplanner

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val profiles = mutableStateListOf<PlannerProfile>()
    private val activeProfileId = mutableStateOf<String?>(null)
    private val themeMode = mutableStateOf(AppThemeMode.System)
    private lateinit var plannerStorage: PlannerStorage
    private val persistenceHandler = Handler(Looper.getMainLooper())
    private val persistenceRunnable = Runnable { persistStateNow() }
    private var pickerProfileId: String? = null
    private var replacementProfileId: String? = null
    private var replacementPhotoId: String? = null
    private var profilePicturePickerProfileId: String? = null
    private val pickPhotos = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(50),
    ) { selectedUris ->
        val profileId = pickerProfileId ?: activeProfileId.value
        pickerProfileId = null

        if (profileId != null) {
            val newPhotos = selectedUris.mapNotNull { uri ->
                plannerStorage.importImage(uri, "post")?.let { image ->
                    PhotoItem(
                        id = "${uri}#${System.nanoTime()}",
                        bitmap = image.bitmap,
                        storagePath = image.path,
                    )
                }
            }
            updateProfile(profileId) { profile ->
                profile.copy(photos = profile.photos + newPhotos)
            }
        }
    }

    private val pickReplacementPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val profileId = replacementProfileId
        val photoId = replacementPhotoId
        replacementProfileId = null
        replacementPhotoId = null

        if (uri != null && profileId != null && photoId != null) {
            plannerStorage.importImage(uri, "post")?.let { image ->
                updateProfile(profileId) { profile ->
                    profile.copy(
                        photos = profile.photos.map { photo ->
                            if (photo.id == photoId) {
                                photo.copy(bitmap = image.bitmap, storagePath = image.path)
                            } else {
                                photo
                            }
                        },
                    )
                }
            }
        }
    }

    private val pickProfilePicture = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val profileId = profilePicturePickerProfileId
        profilePicturePickerProfileId = null
        if (uri != null && profileId != null) {
            plannerStorage.importImage(uri, "profile")?.let { image ->
                updateProfile(profileId) { profile ->
                    profile.copy(
                        profilePicture = image.bitmap,
                        profilePicturePath = image.path,
                    )
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        plannerStorage = PlannerStorage(applicationContext)
        val storedState = plannerStorage.load()
        profiles.addAll(storedState.profiles)
        activeProfileId.value = storedState.activeProfileId
        themeMode.value = storedState.themeMode

        setContent {
            App(
                profiles = profiles,
                activeProfileId = activeProfileId.value,
                themeMode = themeMode.value,
                onThemeSelected = { selectedTheme ->
                    themeMode.value = selectedTheme
                    persistState()
                },
                onAddProfile = { handle ->
                    val profileId = "${handle.lowercase()}#${System.nanoTime()}"
                    profiles.add(
                        PlannerProfile(
                            id = profileId,
                            handle = handle,
                            photos = emptyList(),
                        ),
                    )
                    activeProfileId.value = profileId
                    persistState()
                },
                onSelectProfile = { profileId ->
                    activeProfileId.value = profileId
                    persistState()
                },
                onAddPhotos = {
                    activeProfileId.value?.let { profileId ->
                        pickerProfileId = profileId
                        pickPhotos.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    }
                },
                onChangePhoto = { photoId ->
                    activeProfileId.value?.let { profileId ->
                        replacementProfileId = profileId
                        replacementPhotoId = photoId
                        pickReplacementPhoto.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    }
                },
                onCopyPhotos = { selectedIds ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            var copyNumber = 0
                            profile.copy(
                                photos = profile.photos.flatMap { photo ->
                                    if (photo.id in selectedIds) {
                                        copyNumber += 1
                                        listOf(
                                            photo,
                                            photo.copy(
                                                id = "${photo.id}#copy${System.nanoTime()}-$copyNumber",
                                            ),
                                        )
                                    } else {
                                        listOf(photo)
                                    }
                                },
                            )
                        }
                    }
                },
                onMovePhotos = { selectedIds, targetProfileId ->
                    activeProfileId.value?.let { sourceProfileId ->
                        val photosToMove = profiles
                            .firstOrNull { it.id == sourceProfileId }
                            ?.photos
                            ?.filter { it.id in selectedIds }
                            .orEmpty()
                        updateProfile(sourceProfileId) { profile ->
                            profile.copy(photos = profile.photos.filterNot { it.id in selectedIds })
                        }
                        updateProfile(targetProfileId) { profile ->
                            profile.copy(photos = profile.photos + photosToMove)
                        }
                    }
                },
                onArchivePhotos = { selectedIds ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            val photosToArchive = profile.photos.filter { it.id in selectedIds }
                            profile.copy(
                                photos = profile.photos.filterNot { it.id in selectedIds },
                                archivedPhotos = profile.archivedPhotos + photosToArchive,
                            )
                        }
                    }
                },
                onRestoreArchivedPhotos = { selectedIds ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            val photosToRestore = profile.archivedPhotos.filter { it.id in selectedIds }
                            profile.copy(
                                photos = profile.photos + photosToRestore,
                                archivedPhotos = profile.archivedPhotos.filterNot { it.id in selectedIds },
                            )
                        }
                    }
                },
                onDeleteArchivedPhotos = { selectedIds ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            profile.copy(
                                archivedPhotos = profile.archivedPhotos.filterNot { it.id in selectedIds },
                            )
                        }
                    }
                },
                onDeletePhotos = { selectedIds ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            profile.copy(
                                photos = profile.photos.filterNot { photo ->
                                    photo.id in selectedIds
                                },
                            )
                        }
                    }
                },
                onSwapPhotos = { firstId, secondId ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            val reorderedPhotos = profile.photos.toMutableList()
                            val firstIndex = reorderedPhotos.indexOfFirst { it.id == firstId }
                            val secondIndex = reorderedPhotos.indexOfFirst { it.id == secondId }
                            if (firstIndex >= 0 && secondIndex >= 0) {
                                val firstPhoto = reorderedPhotos[firstIndex]
                                reorderedPhotos[firstIndex] = reorderedPhotos[secondIndex]
                                reorderedPhotos[secondIndex] = firstPhoto
                            }
                            profile.copy(photos = reorderedPhotos)
                        }
                    }
                },
                onReorderPhoto = { draggedId, targetId ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            val reorderedPhotos = profile.photos.toMutableList()
                            val draggedIndex = reorderedPhotos.indexOfFirst { it.id == draggedId }
                            val targetIndex = reorderedPhotos.indexOfFirst { it.id == targetId }
                            if (draggedIndex >= 0 && targetIndex >= 0 && draggedIndex != targetIndex) {
                                val draggedPhoto = reorderedPhotos.removeAt(draggedIndex)
                                reorderedPhotos.add(targetIndex.coerceAtMost(reorderedPhotos.size), draggedPhoto)
                            }
                            profile.copy(photos = reorderedPhotos)
                        }
                    }
                },
                onUpdateProfile = { displayName, bio, website, followers, following ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            profile.copy(
                                displayName = displayName,
                                bio = bio,
                                website = website,
                                followersCount = followers,
                                followingCount = following,
                            )
                        }
                    }
                },
                onPickProfilePicture = {
                    activeProfileId.value?.let { profileId ->
                        profilePicturePickerProfileId = profileId
                        pickProfilePicture.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    }
                },
                onDeleteProfile = { profileId ->
                    val profileIndex = profiles.indexOfFirst { it.id == profileId }
                    if (profileIndex >= 0) {
                        profiles.removeAt(profileIndex)
                        if (activeProfileId.value == profileId) {
                            activeProfileId.value = profiles
                                .getOrNull(profileIndex.coerceAtMost(profiles.lastIndex))
                                ?.id
                        }
                        persistState()
                    }
                },
            )
        }
    }

    private fun updateProfile(
        profileId: String,
        transform: (PlannerProfile) -> PlannerProfile,
    ) {
        val index = profiles.indexOfFirst { it.id == profileId }
        if (index >= 0) {
            profiles[index] = transform(profiles[index])
            persistState()
        }
    }

    private fun persistState() {
        persistenceHandler.removeCallbacks(persistenceRunnable)
        persistenceHandler.postDelayed(persistenceRunnable, PERSISTENCE_DELAY_MILLIS)
    }

    private fun persistStateNow() {
        if (!::plannerStorage.isInitialized) return
        plannerStorage.save(
            profiles = profiles.toList(),
            activeProfileId = activeProfileId.value,
            themeMode = themeMode.value,
        )
    }

    override fun onStop() {
        persistenceHandler.removeCallbacks(persistenceRunnable)
        persistStateNow()
        super.onStop()
    }

    private companion object {
        const val PERSISTENCE_DELAY_MILLIS = 250L
    }

}
