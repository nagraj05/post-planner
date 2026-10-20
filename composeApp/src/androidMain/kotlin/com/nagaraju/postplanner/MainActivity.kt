package com.nagaraju.postplanner

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

class MainActivity : ComponentActivity() {
    private val profiles = mutableStateListOf<PlannerProfile>()
    private val activeProfileId = mutableStateOf<String?>(null)
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
                loadPreview(uri)?.let { bitmap ->
                    PhotoItem(
                        id = "${uri}#${System.nanoTime()}",
                        bitmap = bitmap,
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
            loadPreview(uri)?.let { bitmap ->
                updateProfile(profileId) { profile ->
                    profile.copy(
                        photos = profile.photos.map { photo ->
                            if (photo.id == photoId) photo.copy(bitmap = bitmap) else photo
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
            loadPreview(uri)?.let { bitmap ->
                updateProfile(profileId) { profile ->
                    profile.copy(profilePicture = bitmap)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(
                profiles = profiles,
                activeProfileId = activeProfileId.value,
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
                },
                onSelectProfile = { profileId ->
                    activeProfileId.value = profileId
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
        }
    }

    private fun loadPreview(uri: Uri): ImageBitmap? = runCatching {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.loadThumbnail(uri, Size(1200, 1600), null)
        } else {
            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }
        bitmap?.asImageBitmap()
    }.getOrNull()

}
