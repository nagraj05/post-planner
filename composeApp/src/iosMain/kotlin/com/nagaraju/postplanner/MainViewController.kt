package com.nagaraju.postplanner

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    App(
        profiles = emptyList(),
        activeProfileId = null,
        onAddProfile = {},
        onSelectProfile = {},
        onAddPhotos = {},
        onChangePhoto = {},
        onCopyPhotos = {},
        onMovePhotos = { _, _ -> },
        onArchivePhotos = {},
        onRestoreArchivedPhotos = {},
        onDeleteArchivedPhotos = {},
        onDeletePhotos = {},
        onSwapPhotos = { _, _ -> },
        onReorderPhoto = { _, _ -> },
        onUpdateProfile = { _, _, _, _, _ -> },
        onPickProfilePicture = {},
    )
}
