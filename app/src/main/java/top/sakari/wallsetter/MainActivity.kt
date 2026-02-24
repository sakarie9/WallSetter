package top.sakari.wallsetter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    private var selectedImageUri: Uri? = null
    private var statusMessage: String = ""
    private var selectedTarget: String = WallpaperReceiver.TARGET_BOTH
    private var isSettingWallpaper: Boolean = false

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                statusMessage = getString(R.string.status_pick_cancelled)
                renderUi()
                return@registerForActivityResult
            }

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some document providers don't offer persistable grants; temporary access is enough.
            }
            onImageSelected(uri, fromShare = false)
        }

    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            statusMessage = if (granted) {
                getString(R.string.status_permission_granted)
            } else {
                getString(R.string.status_permission_denied)
            }
            renderUi()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        statusMessage = getString(R.string.status_idle)
        ensureStoragePermissionOnLaunch()
        handleIncomingIntent(intent)
        renderUi()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
        renderUi()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.type?.startsWith("image/") != true) return

        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        if (uri == null) {
            statusMessage = getString(R.string.status_share_missing_uri)
            return
        }

        onImageSelected(uri, fromShare = true)
    }

    private fun onImageSelected(uri: Uri, fromShare: Boolean) {
        selectedImageUri = uri
        statusMessage = if (fromShare) {
            getString(R.string.status_share_received)
        } else {
            getString(R.string.status_pick_success)
        }
        renderUi()
    }

    private fun requiredStoragePermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun hasRequiredStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val permission = requiredStoragePermission()
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureStoragePermissionOnLaunch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (hasRequiredStoragePermission()) return
        requestStoragePermissionLauncher.launch(requiredStoragePermission())
    }

    private fun setSelectedImageAsWallpaper() {
        val uri = selectedImageUri
        if (uri == null) {
            statusMessage = getString(R.string.status_no_image_selected)
            renderUi()
            return
        }

        val target = selectedTarget

        isSettingWallpaper = true
        statusMessage = getString(R.string.status_setting_wallpaper)
        renderUi()

        Thread {
            val message = try {
                WallpaperSetter.setFromUri(this, uri, target)
                getString(R.string.status_wallpaper_set_success)
            } catch (e: Exception) {
                getString(R.string.status_wallpaper_set_failed, e.message ?: "unknown")
            }

            runOnUiThread {
                isSettingWallpaper = false
                statusMessage = message
                renderUi()
            }
        }.start()
    }

    private fun renderUi() {
        setContent {
            WallSetterTheme {
                HomeScreen(
                    statusMessage = statusMessage,
                    selectedImageUri = selectedImageUri,
                    selectedTarget = selectedTarget,
                    isSettingWallpaper = isSettingWallpaper,
                    onPickImage = { pickImageLauncher.launch(arrayOf("image/*")) },
                    onTargetChange = { selectedTarget = it; renderUi() },
                    onSetWallpaper = { setSelectedImageAsWallpaper() },
                    onOpenUsageDetail = {
                        startActivity(Intent(this, UsageDetailActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    statusMessage: String,
    selectedImageUri: Uri?,
    selectedTarget: String,
    isSettingWallpaper: Boolean,
    onPickImage: () -> Unit,
    onTargetChange: (String) -> Unit,
    onSetWallpaper: () -> Unit,
    onOpenUsageDetail: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val deviceRatio = configuration.screenWidthDp.toFloat().coerceAtLeast(1f) /
        configuration.screenHeightDp.toFloat().coerceAtLeast(1f)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status message
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Image preview
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = stringResource(R.string.preview_content_desc),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .aspectRatio(deviceRatio, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Pick image button
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onPickImage
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_pick_image))
            }

            // Target selector
            Text(
                text = stringResource(R.string.label_target),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TargetSelector(
                selectedTarget = selectedTarget,
                onTargetChange = onTargetChange
            )

            // Set wallpaper button
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onSetWallpaper,
                enabled = !isSettingWallpaper,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_set_wallpaper),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            HorizontalDivider()

            // Usage section
            Text(
                text = stringResource(R.string.usage_section_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            UsageItem(
                icon = Icons.Default.Add,
                title = stringResource(R.string.usage_manual_title),
                description = stringResource(R.string.usage_manual_desc)
            )
            UsageItem(
                icon = Icons.Default.Share,
                title = stringResource(R.string.usage_share_title),
                description = stringResource(R.string.usage_share_desc)
            )
            UsageItem(
                icon = Icons.AutoMirrored.Filled.Send,
                title = stringResource(R.string.usage_broadcast_title),
                description = stringResource(R.string.usage_broadcast_desc)
            )

            // Detail button
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenUsageDetail
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.action_open_usage_detail),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun UsageItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetSelector(
    selectedTarget: String,
    onTargetChange: (String) -> Unit
) {
    val options = listOf(
        WallpaperReceiver.TARGET_BOTH to stringResource(R.string.target_both),
        WallpaperReceiver.TARGET_HOME to stringResource(R.string.target_home),
        WallpaperReceiver.TARGET_LOCK to stringResource(R.string.target_lock)
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selectedTarget == value,
                onClick = { onTargetChange(value) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                )
            ) {
                Text(label)
            }
        }
    }
}
