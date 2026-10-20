package com.nagaraju.postplanner

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
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
    private var replacementPostId: String? = null
    private var replacementImageId: String? = null
    private var profilePicturePickerProfileId: String? = null
    private val pendingUploadCount = mutableStateOf(0)
    private var pendingUploadProfileId: String? = null
    private var pendingUploadUris: List<Uri> = emptyList()
    private var carouselPickerProfileId: String? = null
    private var carouselPickerPostId: String? = null
    private val pickPhotos = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(50),
        ::addSelectedPhotos,
    )
    private val pickDevicePhotos = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
        ::addSelectedPhotos,
    )
    private val pickSamsungPhotos = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultIntent = result.data
            val selectedUris = buildList {
                resultIntent?.clipData?.let { clipData ->
                    for (index in 0 until clipData.itemCount) {
                        add(clipData.getItemAt(index).uri)
                    }
                }
                resultIntent?.data?.let { uri ->
                    if (uri !in this) add(uri)
                }
            }
            addSelectedPhotos(selectedUris)
        } else {
            pickerProfileId = null
        }
    }

    private fun addSelectedPhotos(selectedUris: List<Uri>) {
        val profileId = pickerProfileId ?: activeProfileId.value
        pickerProfileId = null

        if (profileId == null || selectedUris.isEmpty()) return
        if (selectedUris.size > 1) {
            pendingUploadProfileId = profileId
            pendingUploadUris = selectedUris
            pendingUploadCount.value = selectedUris.size
        } else {
            importSelectedPhotos(profileId, selectedUris, asCarousel = false)
        }
    }

    private fun importSelectedPhotos(
        profileId: String,
        selectedUris: List<Uri>,
        asCarousel: Boolean,
    ) {
        val newPhotos = selectedUris.mapNotNull { uri ->
                plannerStorage.importImage(uri, "post")?.let { image ->
                    PhotoItem(
                        id = "${uri}#${System.nanoTime()}",
                        bitmap = image.bitmap,
                        storagePath = image.path,
                    )
                }
            }
        if (newPhotos.isEmpty()) return
        val posts = if (asCarousel) {
            val cover = newPhotos.first()
            listOf(
                cover.copy(
                    carouselImages = newPhotos.drop(1).map { photo ->
                        CarouselImage(
                            id = photo.imageId,
                            bitmap = photo.bitmap,
                            storagePath = photo.storagePath,
                        )
                    },
                ),
            )
        } else {
            newPhotos
        }
        updateProfile(profileId) { profile ->
            profile.copy(photos = posts + profile.photos)
        }
    }

    private fun completePendingUpload(asCarousel: Boolean) {
        val profileId = pendingUploadProfileId
        val uris = pendingUploadUris
        clearPendingUpload()
        if (profileId != null && uris.isNotEmpty()) {
            importSelectedPhotos(
                profileId = profileId,
                selectedUris = if (asCarousel) uris.take(MAX_CAROUSEL_IMAGES) else uris,
                asCarousel = asCarousel,
            )
        }
    }

    private fun clearPendingUpload() {
        pendingUploadProfileId = null
        pendingUploadUris = emptyList()
        pendingUploadCount.value = 0
    }

    private val pickCarouselPhotos = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_CAROUSEL_IMAGES),
    ) { selectedUris ->
        val profileId = carouselPickerProfileId
        val postId = carouselPickerPostId
        carouselPickerProfileId = null
        carouselPickerPostId = null
        if (profileId != null && postId != null && selectedUris.isNotEmpty()) {
            val post = profiles.firstOrNull { it.id == profileId }
                ?.photos
                ?.firstOrNull { it.id == postId }
            val availableSlots = MAX_CAROUSEL_IMAGES - (post?.images()?.size ?: MAX_CAROUSEL_IMAGES)
            if (availableSlots <= 0) return@registerForActivityResult
            val addedImages = selectedUris.take(availableSlots).mapNotNull { uri ->
                plannerStorage.importImage(uri, "post")?.let { image ->
                    CarouselImage(
                        id = "${uri}#${System.nanoTime()}",
                        bitmap = image.bitmap,
                        storagePath = image.path,
                    )
                }
            }
            updateProfile(profileId) { profile ->
                profile.copy(
                    photos = profile.photos.map { photo ->
                        if (photo.id == postId) {
                            photo.withImages(photo.images() + addedImages)
                        } else {
                            photo
                        }
                    },
                )
            }
        }
    }

    private val pickReplacementPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val profileId = replacementProfileId
        val postId = replacementPostId
        val imageId = replacementImageId
        replacementProfileId = null
        replacementPostId = null
        replacementImageId = null

        if (uri != null && profileId != null && postId != null && imageId != null) {
            plannerStorage.importImage(uri, "post")?.let { image ->
                updateProfile(profileId) { profile ->
                    profile.copy(
                        photos = profile.photos.map { post ->
                            if (post.id == postId) {
                                post.withImages(
                                    post.images().map { carouselImage ->
                                        if (carouselImage.id == imageId) {
                                            carouselImage.copy(
                                                bitmap = image.bitmap,
                                                storagePath = image.path,
                                            )
                                        } else {
                                            carouselImage
                                        }
                                    },
                                )
                            } else {
                                post
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
                        launchPhotoPicker(profileId)
                    }
                },
                onAddPhotosFromSamsungGallery = {
                    activeProfileId.value?.let { profileId ->
                        launchSamsungGallery(profileId)
                    }
                },
                onAddPhotosFromDevice = {
                    activeProfileId.value?.let { profileId ->
                        pickerProfileId = profileId
                        pickDevicePhotos.launch(arrayOf("image/*"))
                    }
                },
                pendingUploadCount = pendingUploadCount.value,
                onChooseUploadMode = ::completePendingUpload,
                onCancelPendingUpload = ::clearPendingUpload,
                onChangePhoto = { photoId ->
                    activeProfileId.value?.let { profileId ->
                        profiles.firstOrNull { it.id == profileId }
                            ?.photos
                            ?.firstOrNull { it.id == photoId }
                            ?.let { post ->
                                launchReplacementPicker(profileId, post.id, post.imageId)
                            }
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
                onAddCarouselImages = { postId ->
                    activeProfileId.value?.let { profileId ->
                        val post = profiles.firstOrNull { it.id == profileId }
                            ?.photos
                            ?.firstOrNull { it.id == postId }
                        if (post != null && post.images().size < MAX_CAROUSEL_IMAGES) {
                            carouselPickerProfileId = profileId
                            carouselPickerPostId = postId
                            pickCarouselPhotos.launch(
                                PickVisualMediaRequest(
                                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        }
                    }
                },
                onChangeCarouselImage = { postId, imageId ->
                    activeProfileId.value?.let { profileId ->
                        launchReplacementPicker(profileId, postId, imageId)
                    }
                },
                onDeleteCarouselImage = { postId, imageId ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            val post = profile.photos.firstOrNull { it.id == postId }
                            if (post == null) {
                                profile
                            } else if (post.images().size == 1) {
                                profile.copy(photos = profile.photos.filterNot { it.id == postId })
                            } else {
                                profile.copy(
                                    photos = profile.photos.map { currentPost ->
                                        if (currentPost.id == postId) {
                                            currentPost.withImages(
                                                currentPost.images().filterNot { it.id == imageId },
                                            )
                                        } else {
                                            currentPost
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
                onUpdateImageCaption = { postId, imageId, caption ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            profile.copy(
                                photos = profile.photos.map { post ->
                                    if (post.id == postId) {
                                        post.withImages(
                                            post.images().map { image ->
                                                if (image.id == imageId) image.copy(caption = caption) else image
                                            },
                                        )
                                    } else {
                                        post
                                    }
                                },
                            )
                        }
                    }
                },
                onReorderCarouselImage = { postId, draggedImageId, targetImageId ->
                    activeProfileId.value?.let { profileId ->
                        updateProfile(profileId) { profile ->
                            profile.copy(
                                photos = profile.photos.map { post ->
                                    if (post.id != postId) return@map post
                                    val images = post.images().toMutableList()
                                    val draggedIndex = images.indexOfFirst { it.id == draggedImageId }
                                    val targetIndex = images.indexOfFirst { it.id == targetImageId }
                                    if (draggedIndex >= 0 && targetIndex >= 0 && draggedIndex != targetIndex) {
                                        val moved = images.removeAt(draggedIndex)
                                        images.add(targetIndex.coerceAtMost(images.size), moved)
                                        post.withImages(images)
                                    } else {
                                        post
                                    }
                                },
                            )
                        }
                    }
                },
                onCopyCaption = { caption ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Post caption", caption))
                    Toast.makeText(this, "Caption copied", Toast.LENGTH_SHORT).show()
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

    private fun launchPhotoPicker(profileId: String) {
        pickerProfileId = profileId
        pickPhotos.launch(
            PickVisualMediaRequest(
                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
            ),
        )
    }

    private fun launchReplacementPicker(
        profileId: String,
        postId: String,
        imageId: String,
    ) {
        replacementProfileId = profileId
        replacementPostId = postId
        replacementImageId = imageId
        pickReplacementPhoto.launch(
            PickVisualMediaRequest(
                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
            ),
        )
    }

    private fun launchSamsungGallery(profileId: String) {
        pickerProfileId = profileId
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            setPackage(SAMSUNG_GALLERY_PACKAGE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            pickSamsungPhotos.launch(galleryIntent)
        }.onFailure {
            launchPhotoPicker(profileId)
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
        const val SAMSUNG_GALLERY_PACKAGE = "com.sec.android.gallery3d"
        const val MAX_CAROUSEL_IMAGES = 20
    }

}
