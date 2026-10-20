package com.nagaraju.postplanner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.round
import kotlin.math.roundToInt

data class PhotoItem(
    val id: String,
    val bitmap: ImageBitmap,
    val storagePath: String? = null,
)

data class PlannerProfile(
    val id: String,
    val handle: String,
    val photos: List<PhotoItem>,
    val archivedPhotos: List<PhotoItem> = emptyList(),
    val displayName: String = "",
    val bio: String = "",
    val website: String = "",
    val profilePicture: ImageBitmap? = null,
    val profilePicturePath: String? = null,
    val followersCount: Long = 0,
    val followingCount: Long = 0,
)

enum class AppThemeMode(val label: String) {
    System("System default"),
    Light("Light"),
    Dark("Dark"),
}

@Composable
fun App(
    profiles: List<PlannerProfile>,
    activeProfileId: String?,
    themeMode: AppThemeMode,
    onThemeSelected: (AppThemeMode) -> Unit,
    onAddProfile: (handle: String) -> Unit,
    onSelectProfile: (profileId: String) -> Unit,
    onAddPhotos: () -> Unit,
    onAddPhotosFromSamsungGallery: (() -> Unit)?,
    onAddPhotosFromDevice: () -> Unit,
    onChangePhoto: (photoId: String) -> Unit,
    onCopyPhotos: (Set<String>) -> Unit,
    onMovePhotos: (photoIds: Set<String>, targetProfileId: String) -> Unit,
    onArchivePhotos: (Set<String>) -> Unit,
    onRestoreArchivedPhotos: (Set<String>) -> Unit,
    onDeleteArchivedPhotos: (Set<String>) -> Unit,
    onDeletePhotos: (Set<String>) -> Unit,
    onSwapPhotos: (firstId: String, secondId: String) -> Unit,
    onReorderPhoto: (draggedId: String, targetId: String) -> Unit,
    onUpdateProfile: (
        displayName: String,
        bio: String,
        website: String,
        followersCount: Long,
        followingCount: Long,
    ) -> Unit,
    onPickProfilePicture: () -> Unit,
    onDeleteProfile: (profileId: String) -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }

    MaterialTheme(colorScheme = if (useDarkTheme) darkColorScheme() else lightColorScheme()) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val selectedIds = remember { mutableStateListOf<String>() }
        var showAddProfileDialog by remember { mutableStateOf(false) }
        var showPhotoSourceDialog by remember { mutableStateOf(false) }
        var showEditProfileDialog by remember { mutableStateOf(false) }
        var showSettingsPage by remember { mutableStateOf(false) }
        var showArchive by remember { mutableStateOf(false) }
        var photosToMove by remember { mutableStateOf<Set<String>?>(null) }
        val activeProfile = profiles.firstOrNull { it.id == activeProfileId }

        LaunchedEffect(profiles.isEmpty()) {
            if (profiles.isEmpty()) {
                showAddProfileDialog = true
            }
        }

        fun toggleSelection(photoId: String) {
            if (photoId in selectedIds) {
                selectedIds.remove(photoId)
            } else {
                selectedIds.add(photoId)
            }
        }

        fun requestAddPhotos() {
            if (activeProfile == null) {
                showAddProfileDialog = true
            } else {
                showPhotoSourceDialog = true
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ProfileDrawer(
                    profiles = profiles,
                    activeProfileId = activeProfileId,
                    onSelectProfile = { profileId ->
                        selectedIds.clear()
                        showArchive = false
                        showSettingsPage = false
                        onSelectProfile(profileId)
                        scope.launch { drawerState.close() }
                    },
                    onAddProfile = {
                        scope.launch { drawerState.close() }
                        showAddProfileDialog = true
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        selectedIds.clear()
                        showArchive = false
                        showSettingsPage = true
                    },
                )
            },
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (showSettingsPage) {
                        // Settings is a full page and does not use the grid action bar.
                    } else if (showArchive && selectedIds.isNotEmpty()) {
                        ArchiveSelectionBar(
                            selectedCount = selectedIds.size,
                            onCancel = { selectedIds.clear() },
                            onRestore = {
                                onRestoreArchivedPhotos(selectedIds.toSet())
                                selectedIds.clear()
                            },
                            onDelete = {
                                onDeleteArchivedPhotos(selectedIds.toSet())
                                selectedIds.clear()
                            },
                        )
                    } else if (!showArchive && selectedIds.isEmpty()) {
                        AddPhotoBar(
                            archiveCount = activeProfile?.archivedPhotos?.size ?: 0,
                            onAddPhotos = ::requestAddPhotos,
                            onOpenArchive = {
                                if (activeProfile != null) {
                                    selectedIds.clear()
                                    showArchive = true
                                }
                            },
                        )
                    } else if (!showArchive) {
                        SelectionBar(
                            selectedCount = selectedIds.size,
                            canMove = profiles.size > 1,
                            onCancel = { selectedIds.clear() },
                            onChange = {
                                if (selectedIds.size == 1) {
                                    onChangePhoto(selectedIds.single())
                                    selectedIds.clear()
                                }
                            },
                            onCopy = {
                                onCopyPhotos(selectedIds.toSet())
                                selectedIds.clear()
                            },
                            onMove = { photosToMove = selectedIds.toSet() },
                            onArchive = {
                                onArchivePhotos(selectedIds.toSet())
                                selectedIds.clear()
                            },
                            onDelete = {
                                onDeletePhotos(selectedIds.toSet())
                                selectedIds.clear()
                            },
                            onSwap = {
                                if (selectedIds.size == 2) {
                                    onSwapPhotos(selectedIds[0], selectedIds[1])
                                    selectedIds.clear()
                                }
                            },
                        )
                    }
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (showSettingsPage) {
                        SettingsPage(
                            selectedTheme = themeMode,
                            onThemeSelected = onThemeSelected,
                            onBack = { showSettingsPage = false },
                        )
                    } else if (showArchive) {
                        ArchiveHeader(
                            handle = activeProfile?.handle.orEmpty(),
                            selectionCount = selectedIds.size,
                            onBack = {
                                selectedIds.clear()
                                showArchive = false
                            },
                        )
                        val archivedPhotos = activeProfile?.archivedPhotos.orEmpty()
                        if (archivedPhotos.isEmpty()) {
                            EmptyArchive(modifier = Modifier.weight(1f))
                        } else {
                            PhotoGrid(
                                photos = archivedPhotos,
                                selectedIds = selectedIds,
                                onToggleSelection = ::toggleSelection,
                                onReorderPhoto = null,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Header(
                            profile = activeProfile,
                            selectionCount = selectedIds.size,
                            canSwitchProfile = profiles.size > 1,
                            onOpenMenu = { scope.launch { drawerState.open() } },
                            onEditProfile = { showEditProfileDialog = true },
                            onSwitchProfile = {
                                val currentIndex = profiles.indexOfFirst { it.id == activeProfileId }
                                if (currentIndex >= 0 && profiles.size > 1) {
                                    val nextProfile = profiles[(currentIndex + 1) % profiles.size]
                                    selectedIds.clear()
                                    onSelectProfile(nextProfile.id)
                                }
                            },
                        )
                        activeProfile?.let {
                            ProfilePreview(profile = it, onEditProfile = { showEditProfileDialog = true })
                        }

                        val photos = activeProfile?.photos.orEmpty()
                        if (photos.isEmpty()) {
                            EmptyGrid(
                                hasProfile = activeProfile != null,
                                onAddPhotos = ::requestAddPhotos,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            PhotoGrid(
                                photos = photos,
                                selectedIds = selectedIds,
                                onToggleSelection = ::toggleSelection,
                                onReorderPhoto = onReorderPhoto,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        if (showEditProfileDialog && activeProfile != null) {
            EditProfileDialog(
                profile = activeProfile,
                onDismiss = { showEditProfileDialog = false },
                onPickProfilePicture = onPickProfilePicture,
                onSave = { displayName, bio, website, followers, following ->
                    onUpdateProfile(displayName, bio, website, followers, following)
                    showEditProfileDialog = false
                },
                onDelete = {
                    selectedIds.clear()
                    onDeleteProfile(activeProfile.id)
                    showEditProfileDialog = false
                },
            )
        }

        if (showPhotoSourceDialog && activeProfile != null) {
            PhotoSourceDialog(
                onDismiss = { showPhotoSourceDialog = false },
                onSamsungGallery = onAddPhotosFromSamsungGallery?.let { openSamsungGallery ->
                    {
                        showPhotoSourceDialog = false
                        openSamsungGallery()
                    }
                },
                onPhotoPicker = {
                    showPhotoSourceDialog = false
                    onAddPhotos()
                },
                onDeviceFolders = {
                    showPhotoSourceDialog = false
                    onAddPhotosFromDevice()
                },
            )
        }

        if (showAddProfileDialog) {
            AddProfileDialog(
                canDismiss = profiles.isNotEmpty(),
                existingHandles = profiles.map { it.handle },
                onDismiss = { showAddProfileDialog = false },
                onAdd = { handle ->
                    onAddProfile(handle)
                    selectedIds.clear()
                    showAddProfileDialog = false
                },
            )
        }

        photosToMove?.let { photoIds ->
            MoveProfileDialog(
                profiles = profiles.filterNot { it.id == activeProfileId },
                onDismiss = { photosToMove = null },
                onMove = { targetProfileId ->
                    onMovePhotos(photoIds, targetProfileId)
                    selectedIds.clear()
                    photosToMove = null
                },
            )
        }
    }
}

@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onSamsungGallery: (() -> Unit)?,
    onPhotoPicker: () -> Unit,
    onDeviceFolders: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add photos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Choose where you want to select images from.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (onSamsungGallery != null) {
                    PhotoSourceOption(
                        icon = Icons.Filled.Collections,
                        title = "Samsung Gallery",
                        description = "Open your phone's Gallery app directly",
                        onClick = onSamsungGallery,
                    )
                }
                PhotoSourceOption(
                    icon = Icons.Filled.PhotoLibrary,
                    title = "Photo picker",
                    description = "Google Photos and Android's recent photos",
                    onClick = onPhotoPicker,
                )
                PhotoSourceOption(
                    icon = Icons.Filled.FolderOpen,
                    title = "Device folders",
                    description = "Browse DCIM, Pictures, Downloads and phone storage",
                    onClick = onDeviceFolders,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun PhotoSourceOption(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ProfileDrawer(
    profiles: List<PlannerProfile>,
    activeProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onAddProfile: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalDrawerSheet {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = 28.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Profiles",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Local Instagram planners",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onAddProfile) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add profile",
                )
            }
        }

        if (profiles.isEmpty()) {
            Text(
                text = "Add your first Instagram handle.",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            profiles.forEach { profile ->
                NavigationDrawerItem(
                    label = {
                        Column {
                            Text(
                                text = profile.handle,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${profile.photos.size} grid • ${profile.archivedPhotos.size} archived",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    selected = profile.id == activeProfileId,
                    onClick = { onSelectProfile(profile.id) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        NavigationDrawerItem(
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = false,
            onClick = onOpenSettings,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Header(
    profile: PlannerProfile?,
    selectionCount: Int,
    canSwitchProfile: Boolean,
    onOpenMenu: () -> Unit,
    onEditProfile: () -> Unit,
    onSwitchProfile: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenMenu) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open profiles",
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (profile != null) profile.handle else "Choose a profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (profile != null) "Instagram planner" else "Add an Instagram handle to begin",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (profile != null && selectionCount == 0) {
            ProfileAvatarSwitcher(
                profile = profile,
                canSwitchProfile = canSwitchProfile,
                onEditProfile = onEditProfile,
                onSwitchProfile = onSwitchProfile,
            )
        }
    }
}

@Composable
private fun ProfileAvatarSwitcher(
    profile: PlannerProfile,
    canSwitchProfile: Boolean,
    onEditProfile: () -> Unit,
    onSwitchProfile: () -> Unit,
) {
    var dragDistance by remember(profile.id) { mutableStateOf(0f) }
    var isDragging by remember(profile.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(52.dp)
            .pointerInput(profile.id, canSwitchProfile) {
                detectVerticalDragGestures(
                    onDragStart = {
                        if (canSwitchProfile) {
                            isDragging = true
                            dragDistance = 0f
                        }
                    },
                    onVerticalDrag = { change, amount ->
                        if (canSwitchProfile) {
                            change.consume()
                            dragDistance = (dragDistance + amount).coerceAtLeast(0f)
                        }
                    },
                    onDragEnd = {
                        val shouldSwitch = canSwitchProfile && dragDistance >= 64.dp.toPx()
                        isDragging = false
                        dragDistance = 0f
                        if (shouldSwitch) {
                            onSwitchProfile()
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        dragDistance = 0f
                    },
                )
            }
            .clickable(onClick = onEditProfile),
        contentAlignment = Alignment.Center,
    ) {
        if (isDragging) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp,
            )
        }
        if (profile.profilePicture != null) {
            Image(
                bitmap = profile.profilePicture,
                contentDescription = "Edit or switch profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset { IntOffset(0, dragDistance.coerceAtMost(18f).roundToInt()) }
                    .size(36.dp)
                    .clip(CircleShape),
            )
        } else {
            Surface(
                modifier = Modifier
                    .offset { IntOffset(0, dragDistance.coerceAtMost(18f).roundToInt()) }
                    .size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Edit or switch profile",
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilePreview(
    profile: PlannerProfile,
    onEditProfile: () -> Unit,
) {
    Surface(
        onClick = onEditProfile,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    if (profile.profilePicture != null) {
                        Image(
                            bitmap = profile.profilePicture,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Add profile picture",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 20.dp),
                ) {
                    ProfileStat(profile.photos.size.toString(), "posts", Modifier.weight(1f))
                    ProfileStat(formatSocialCount(profile.followersCount), "followers", Modifier.weight(1f))
                    ProfileStat(formatSocialCount(profile.followingCount), "following", Modifier.weight(1f))
                }
            }
            if (profile.displayName.isNotBlank()) {
                Text(
                    text = profile.displayName,
                    modifier = Modifier.padding(top = 10.dp),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (profile.bio.isNotBlank()) {
                Text(text = profile.bio, style = MaterialTheme.typography.bodyMedium)
            }
            if (profile.website.isNotBlank()) {
                Text(
                    text = profile.website,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (profile.displayName.isBlank() && profile.bio.isBlank() && profile.website.isBlank()) {
                Text(
                    text = "Tap to add a profile picture, name, bio and website",
                    modifier = Modifier.padding(top = 10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatSocialCount(count: Long): String {
    if (count < 10_000) return count.toString()

    val (divisor, suffix) = when {
        count >= 1_000_000_000 -> 1_000_000_000.0 to "B"
        count >= 1_000_000 -> 1_000_000.0 to "M"
        else -> 1_000.0 to "K"
    }
    val roundedValue = round((count / divisor) * 10.0) / 10.0
    return "${roundedValue.toString().removeSuffix(".0")}$suffix"
}

@Composable
private fun ArchiveHeader(
    handle: String,
    selectionCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back to grid")
        }
        Column {
            Text(
                text = if (selectionCount > 0) "$selectionCount selected" else "Archived",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (selectionCount > 0) "Restore or permanently delete" else handle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<PhotoItem>,
    selectedIds: List<String>,
    onToggleSelection: (String) -> Unit,
    onReorderPhoto: ((draggedId: String, targetId: String) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(
            items = photos,
            key = { _, photo -> photo.id },
        ) { index, photo ->
            val isSelected = photo.id in selectedIds
            val isSelectionMode = selectedIds.isNotEmpty()
            var dragOffset by remember(photo.id) { mutableStateOf(Offset.Zero) }
            var dragDistance by remember(photo.id) { mutableStateOf(0f) }
            var isDragging by remember(photo.id) { mutableStateOf(false) }
            var suppressClick by remember(photo.id) { mutableStateOf(false) }
            var workingIndex by remember(photo.id) { mutableStateOf(index) }
            var dragOrder by remember(photo.id) { mutableStateOf<List<String>>(emptyList()) }
            val latestIndex by rememberUpdatedState(index)
            val latestPhotos by rememberUpdatedState(photos)
            val latestIsSelected by rememberUpdatedState(isSelected)
            val latestReorder by rememberUpdatedState(onReorderPhoto)
            val latestToggleSelection by rememberUpdatedState(onToggleSelection)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset {
                        IntOffset(
                            x = dragOffset.x.roundToInt(),
                            y = dragOffset.y.roundToInt(),
                        )
                    }
                    .scale(if (isDragging) 0.94f else 1f)
                    .alpha(if (isDragging) 0.72f else 1f)
                    .pointerInput(photo.id, photos.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isDragging = true
                                suppressClick = true
                                dragOffset = Offset.Zero
                                dragDistance = 0f
                                workingIndex = latestIndex
                                dragOrder = latestPhotos.map { it.id }
                                if (!latestIsSelected) {
                                    latestToggleSelection(photo.id)
                                }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount
                                dragDistance += amount.getDistance()

                                val reorder = latestReorder
                                if (reorder != null && dragOrder.isNotEmpty()) {
                                    val columnWidth = size.width + 2.dp.toPx()
                                    val rowHeight = size.height + 2.dp.toPx()
                                    val currentColumn = workingIndex % 3
                                    val currentRow = workingIndex / 3
                                    val targetColumn = (
                                        (currentColumn * columnWidth + size.width / 2f + dragOffset.x) /
                                            columnWidth
                                        ).toInt().coerceIn(0, 2)
                                    val targetRow = (
                                        (currentRow * rowHeight + size.height / 2f + dragOffset.y) /
                                            rowHeight
                                        ).toInt().coerceAtLeast(0)
                                    val targetIndex = (targetRow * 3 + targetColumn)
                                        .coerceIn(0, dragOrder.lastIndex)

                                    if (targetIndex != workingIndex) {
                                        val targetId = dragOrder[targetIndex]
                                        val oldColumn = workingIndex % 3
                                        val oldRow = workingIndex / 3
                                        val newColumn = targetIndex % 3
                                        val newRow = targetIndex / 3

                                        reorder(photo.id, targetId)

                                        val updatedOrder = dragOrder.toMutableList()
                                        val draggedId = updatedOrder.removeAt(workingIndex)
                                        updatedOrder.add(targetIndex, draggedId)
                                        dragOrder = updatedOrder
                                        workingIndex = targetIndex

                                        dragOffset -= Offset(
                                            x = (newColumn - oldColumn) * columnWidth,
                                            y = (newRow - oldRow) * rowHeight,
                                        )
                                    }
                                }
                            },
                            onDragEnd = {
                                if (dragDistance > 8f) {
                                    suppressClick = false
                                }
                                isDragging = false
                                dragOffset = Offset.Zero
                                dragDistance = 0f
                                dragOrder = emptyList()
                            },
                            onDragCancel = {
                                isDragging = false
                                suppressClick = false
                                dragOffset = Offset.Zero
                                dragDistance = 0f
                                dragOrder = emptyList()
                            }
                        )
                    }
                    .clickable {
                        if (suppressClick) {
                            suppressClick = false
                        } else if (selectedIds.isNotEmpty()) {
                            onToggleSelection(photo.id)
                        }
                    },
            ) {
                Image(
                    bitmap = photo.bitmap,
                    contentDescription = "Planned photo ${index + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.12f)),
                    )
                }
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Black.copy(alpha = 0.18f)
                                },
                            )
                            .border(
                                width = 1.5.dp,
                                color = Color.White.copy(alpha = if (isSelected) 1f else 0.9f),
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGrid(
    hasProfile: Boolean,
    onAddPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                onClick = onAddPhotos,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = if (hasProfile) "Add photos" else "Add profile",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Text(
                text = if (hasProfile) "Build your visual grid" else "Add your first profile",
                modifier = Modifier.padding(top = 20.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (hasProfile) {
                    "Add several photos at once and arrange your profile preview."
                } else {
                    "Enter an Instagram handle to keep each account's grid separate."
                },
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AddProfileDialog(
    canDismiss: Boolean,
    existingHandles: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var handle by remember { mutableStateOf("") }
    val normalizedHandle = handle.trim().removePrefix("@").trim()
    val isDuplicate = existingHandles.any { it.equals(normalizedHandle, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = {
            if (canDismiss) {
                onDismiss()
            }
        },
        title = { Text("Add Instagram profile") },
        text = {
            Column {
                Text("This creates a private local planner. It does not sign in to Instagram.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Instagram handle") },
                    prefix = { Text("@") },
                    singleLine = true,
                    supportingText = {
                        if (isDuplicate) {
                            Text("This profile already exists.")
                        }
                    },
                    isError = isDuplicate,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(normalizedHandle) },
                enabled = normalizedHandle.isNotBlank() && !isDuplicate,
            ) {
                Text("Add profile")
            }
        },
        dismissButton = {
            if (canDismiss) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}

@Composable
private fun SettingsPage(
    selectedTheme: AppThemeMode,
    onThemeSelected: (AppThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Customize your planner",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Choose how the app looks on this device.",
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            AppThemeMode.entries.forEach { mode ->
                Surface(
                    onClick = { onThemeSelected(mode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selectedTheme == mode) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedTheme == mode,
                            onClick = { onThemeSelected(mode) },
                        )
                        Text(
                            text = mode.label,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            Text(
                text = "More planner settings can be added to this page later.",
                modifier = Modifier.padding(top = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: PlannerProfile,
    onDismiss: () -> Unit,
    onPickProfilePicture: () -> Unit,
    onSave: (String, String, String, Long, Long) -> Unit,
    onDelete: () -> Unit,
) {
    var displayName by remember(profile.id) { mutableStateOf(profile.displayName) }
    var bio by remember(profile.id) { mutableStateOf(profile.bio) }
    var website by remember(profile.id) { mutableStateOf(profile.website) }
    var followers by remember(profile.id) { mutableStateOf(profile.followersCount.toString()) }
    var following by remember(profile.id) { mutableStateOf(profile.followingCount.toString()) }
    var showDeleteConfirmation by remember(profile.id) { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Delete ${profile.handle}?") },
            text = {
                Text(
                    "This permanently removes the profile, grid, archived posts, " +
                        "profile details and locally stored images from this device.",
                )
            },
            confirmButton = {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text("Delete", modifier = Modifier.padding(start = 8.dp))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Keep profile")
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Edit profile")
                Text(
                    text = profile.handle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            if (profile.profilePicture != null) {
                                Image(
                                    bitmap = profile.profilePicture,
                                    contentDescription = "Profile picture preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(38.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp),
                        ) {
                            Text(
                                text = displayName.ifBlank { profile.handle },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${formatSocialCount(followers.toLongOrNull() ?: 0)} followers",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = onPickProfilePicture) {
                                Text("Change photo")
                            }
                        }
                    }
                }

                Text(
                    text = "PROFILE DETAILS",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bio") },
                    minLines = 3,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Website") },
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = followers,
                        onValueChange = { value -> followers = value.filter(Char::isDigit).take(18) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Followers") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = following,
                        onValueChange = { value -> following = value.filter(Char::isDigit).take(18) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Following") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Delete profile",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Remove this profile and all of its locally saved planner images.",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "Delete ${profile.handle}",
                                modifier = Modifier.padding(start = 8.dp),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        displayName.trim(),
                        bio.trim(),
                        website.trim(),
                        followers.toLongOrNull() ?: 0,
                        following.toLongOrNull() ?: 0,
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EmptyArchive(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Archive,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No archived photos",
                modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Photos you archive from the grid will appear here.",
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AddPhotoBar(
    archiveCount: Int,
    onAddPhotos: () -> Unit,
    onOpenArchive: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FloatingActionButton(
                onClick = onAddPhotos,
                modifier = Modifier.clip(CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add photos",
                )
            }
            Column(
                modifier = Modifier.padding(start = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(onClick = onOpenArchive) {
                    Icon(
                        imageVector = Icons.Filled.Archive,
                        contentDescription = "View archived photos",
                    )
                }
                Text(
                    text = if (archiveCount == 0) "Archive" else "Archive ($archiveCount)",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ArchiveSelectionBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                }
                Text("$selectedCount selected", fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionAction(Icons.Filled.Restore, "Restore", true, onRestore)
                SelectionAction(
                    icon = Icons.Filled.Delete,
                    label = "Delete",
                    enabled = true,
                    onClick = onDelete,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SelectionBar(
    selectedCount: Int,
    canMove: Boolean,
    onCancel: () -> Unit,
    onChange: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onSwap: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$selectedCount selected", fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SelectionAction(Icons.Filled.Edit, "Change", selectedCount == 1, onChange)
                SelectionAction(Icons.Filled.SwapHoriz, "Swap", selectedCount == 2, onSwap)
                SelectionAction(Icons.Filled.Archive, "Archive", true, onArchive)
                SelectionAction(
                    icon = Icons.Filled.Delete,
                    label = "Delete",
                    enabled = true,
                    onClick = onDelete,
                    tint = MaterialTheme.colorScheme.error,
                )
                SelectionAction(Icons.Filled.ContentCopy, "Copy", true, onCopy)
                SelectionAction(Icons.Filled.DriveFileMove, "Move", canMove, onMove)
            }
        }
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val actionColor = if (enabled) tint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    Column(
        modifier = Modifier.width(68.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = actionColor,
            )
        }
        Text(
            text = label,
            color = actionColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun MoveProfileDialog(
    profiles: List<PlannerProfile>,
    onDismiss: () -> Unit,
    onMove: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                profiles.forEach { profile ->
                    TextButton(
                        onClick = { onMove(profile.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = profile.handle,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
