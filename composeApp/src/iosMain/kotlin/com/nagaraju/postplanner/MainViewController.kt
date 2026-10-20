package com.nagaraju.postplanner

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    App(
        profiles = emptyList(),
        activeProfileId = null,
        themeMode = AppThemeMode.System,
        onThemeSelected = {},
        onAddProfile = {},
        onSelectProfile = {},
        onAddPhotos = {},
        onAddPhotosFromSamsungGallery = null,
        onAddPhotosFromDevice = {},
        pendingUploadCount = 0,
        onChooseUploadMode = {},
        onCancelPendingUpload = {},
        onChangePhoto = {},
        onCopyPhotos = {},
        onMovePhotos = { _, _ -> },
        onArchivePhotos = {},
        onRestoreArchivedPhotos = {},
        onDeleteArchivedPhotos = {},
        onDeletePhotos = {},
        onSwapPhotos = { _, _ -> },
        onReorderPhoto = { _, _ -> },
        onAddCarouselImages = {},
        onChangeCarouselImage = { _, _ -> },
        onDeleteCarouselImage = { _, _ -> },
        onUpdateImageCaption = { _, _, _ -> },
        onReorderCarouselImage = { _, _, _ -> },
        onCopyCaption = {},
        onUpdateProfile = { _, _, _, _, _ -> },
        onPickProfilePicture = {},
        onDeleteProfile = {},
    )
}
