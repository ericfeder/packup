package com.packup.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.packup.data.local.entity.FamilyMemberEntity
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagerSheet(
    members: List<FamilyMemberEntity>,
    onAddMember: (String, String) -> Unit,
    onRenameMember: (String, String) -> Unit,
    onDeleteMember: (String) -> Unit,
    onSetMemberIcon: (String, String) -> Unit,
    onSetMemberPhoto: (String, String) -> Unit,
    onMoveMemberUp: (String) -> Unit,
    onMoveMemberDown: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        MemberManagerContent(
            members = members,
            onAddMember = onAddMember,
            onRenameMember = onRenameMember,
            onDeleteMember = onDeleteMember,
            onSetMemberIcon = onSetMemberIcon,
            onSetMemberPhoto = onSetMemberPhoto,
            onMoveMemberUp = onMoveMemberUp,
            onMoveMemberDown = onMoveMemberDown,
            onDismiss = onDismiss
        )
    }
}

private fun saveCroppedBitmap(
    context: Context,
    sourceUri: Uri,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    viewSize: IntSize
): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val cropSize = 512
        val circleRadius = viewSize.width.coerceAtMost(viewSize.height) * 0.4f
        val circleDiameterPx = circleRadius * 2f
        val centerX = viewSize.width / 2f
        val centerY = viewSize.height / 2f

        val imgAspect = original.width.toFloat() / original.height.toFloat()
        val fitWidth: Float
        val fitHeight: Float
        if (imgAspect > 1f) {
            fitHeight = viewSize.height.toFloat()
            fitWidth = fitHeight * imgAspect
        } else {
            fitWidth = viewSize.width.toFloat()
            fitHeight = fitWidth / imgAspect
        }

        val scaledWidth = fitWidth * scale
        val scaledHeight = fitHeight * scale
        val imgLeft = centerX - scaledWidth / 2f + offsetX
        val imgTop = centerY - scaledHeight / 2f + offsetY

        val cropLeft = centerX - circleRadius
        val cropTop = centerY - circleRadius

        val srcLeft = ((cropLeft - imgLeft) / scaledWidth * original.width).toInt().coerceIn(0, original.width - 1)
        val srcTop = ((cropTop - imgTop) / scaledHeight * original.height).toInt().coerceIn(0, original.height - 1)
        val srcRight = (((cropLeft + circleDiameterPx) - imgLeft) / scaledWidth * original.width).toInt().coerceIn(srcLeft + 1, original.width)
        val srcBottom = (((cropTop + circleDiameterPx) - imgTop) / scaledHeight * original.height).toInt().coerceIn(srcTop + 1, original.height)

        val cropped = Bitmap.createBitmap(original, srcLeft, srcTop, srcRight - srcLeft, srcBottom - srcTop)
        val scaled = Bitmap.createScaledBitmap(cropped, cropSize, cropSize, true)

        val dir = File(context.filesDir, "member_photos")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        original.recycle()
        cropped.recycle()
        scaled.recycle()

        file.absolutePath
    } catch (_: Exception) {
        null
    }
}

@Composable
private fun PhotoCropDialog(
    imageUri: Uri,
    onConfirm: (scale: Float, offsetX: Float, offsetY: Float, viewSize: IntSize) -> Unit,
    onCancel: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.White)
                }
                Text(
                    "Move and Scale",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                TextButton(onClick = { onConfirm(scale, offsetX, offsetY, viewSize) }) {
                    Text("Done", color = colorScheme.primary)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onSizeChanged { viewSize = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Crop preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    contentScale = ContentScale.Fit
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
                ) {
                    val circleRadius = size.width.coerceAtMost(size.height) * 0.4f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawRect(Color.Black.copy(alpha = 0.6f))
                    drawCircle(
                        color = Color.Transparent,
                        radius = circleRadius,
                        center = center,
                        blendMode = BlendMode.Clear
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = circleRadius,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MemberManagerContent(
    members: List<FamilyMemberEntity>,
    onAddMember: (String, String) -> Unit,
    onRenameMember: (String, String) -> Unit,
    onDeleteMember: (String) -> Unit,
    onSetMemberIcon: (String, String) -> Unit,
    onSetMemberPhoto: (String, String) -> Unit,
    onMoveMemberUp: (String) -> Unit,
    onMoveMemberDown: (String) -> Unit,
    onDismiss: () -> Unit,
    showHeader: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var newMemberName by remember { mutableStateOf("") }
    var newMemberIcon by remember { mutableStateOf("person") }
    var editingMemberId by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }
    var pickingIconFor by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var pickingPhotoForMemberId by remember { mutableStateOf<String?>(null) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && pickingPhotoForMemberId != null) {
            pendingCropUri = uri
        } else {
            pickingPhotoForMemberId = null
        }
    }

    if (pendingCropUri != null && pickingPhotoForMemberId != null) {
        PhotoCropDialog(
            imageUri = pendingCropUri!!,
            onConfirm = { cropScale, cropOffsetX, cropOffsetY, cropViewSize ->
                val path = saveCroppedBitmap(
                    context, pendingCropUri!!, cropScale, cropOffsetX, cropOffsetY, cropViewSize
                )
                if (path != null) {
                    onSetMemberPhoto(pickingPhotoForMemberId!!, path)
                }
                pendingCropUri = null
                pickingPhotoForMemberId = null
            },
            onCancel = {
                pendingCropUri = null
                pickingPhotoForMemberId = null
            }
        )
    }

    Column(modifier = Modifier.imePadding().padding(bottom = 32.dp)) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.People,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Family Members",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, "Close")
                }
            }

            HorizontalDivider()
        }

        Column(
            modifier = Modifier
                .heightIn(max = 400.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            members.forEachIndexed { index, member ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (editingMemberId == member.id) {
                            BasicTextField(
                                value = editValue,
                                onValueChange = { editValue = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    val trimmed = editValue.trim()
                                    if (trimmed.isNotEmpty() && trimmed != member.name) {
                                        onRenameMember(member.id, trimmed)
                                    }
                                    editingMemberId = null
                                }),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorScheme.surfaceContainerHighest)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    val trimmed = editValue.trim()
                                    if (trimmed.isNotEmpty() && trimmed != member.name) {
                                        onRenameMember(member.id, trimmed)
                                    }
                                    editingMemberId = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Check, "Save", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { editingMemberId = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Outlined.Close, "Cancel", modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        pickingPhotoForMemberId = member.id
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                MemberAvatarCircle(
                                    member = member,
                                    size = 36,
                                    showCameraBadge = true
                                )
                            }
                            Text(
                                member.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { if (index > 0) onMoveMemberUp(member.id) },
                                modifier = Modifier.size(24.dp),
                                enabled = index > 0
                            ) {
                                if (index > 0) {
                                    Icon(
                                        Icons.Outlined.ArrowUpward, "Move Up",
                                        modifier = Modifier.size(14.dp),
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { if (index < members.size - 1) onMoveMemberDown(member.id) },
                                modifier = Modifier.size(24.dp),
                                enabled = index < members.size - 1
                            ) {
                                if (index < members.size - 1) {
                                    Icon(
                                        Icons.Outlined.ArrowDownward, "Move Down",
                                        modifier = Modifier.size(14.dp),
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    editingMemberId = member.id
                                    editValue = member.name
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Edit, "Rename",
                                    modifier = Modifier.size(14.dp),
                                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (confirmDelete == member.id) {
                                        onDeleteMember(member.id)
                                        confirmDelete = null
                                    } else {
                                        confirmDelete = member.id
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Close, "Delete",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (confirmDelete == member.id) colorScheme.error
                                    else colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = pickingIconFor == member.id,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        MemberIconPickerRow(
                            selectedKey = member.iconKey,
                            onSelect = { key ->
                                onSetMemberIcon(member.id, key)
                                pickingIconFor = null
                            }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceContainerHigh)
                    .then(
                        if (pickingIconFor == "__new_member__")
                            Modifier.border(2.dp, colorScheme.primary, CircleShape)
                        else Modifier
                    )
                    .clickable {
                        pickingIconFor = if (pickingIconFor == "__new_member__") null else "__new_member__"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    MemberIcons.getIcon(newMemberIcon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = newMemberName,
                onValueChange = { newMemberName = it },
                placeholder = { Text("New member...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
            FilledTonalButton(
                onClick = {
                    val trimmed = newMemberName.trim()
                    if (trimmed.isNotEmpty()) {
                        onAddMember(trimmed, newMemberIcon)
                        newMemberName = ""
                        newMemberIcon = "person"
                    }
                },
                enabled = newMemberName.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(" Add")
            }
        }

        AnimatedVisibility(
            visible = pickingIconFor == "__new_member__",
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            MemberIconPickerRow(
                selectedKey = newMemberIcon,
                onSelect = { key ->
                    newMemberIcon = key
                    pickingIconFor = null
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun MemberAvatarCircle(
    member: FamilyMemberEntity,
    size: Int,
    showCameraBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (member.photoUri.isNotEmpty()) {
            AsyncImage(
                model = member.photoUri,
                contentDescription = member.name,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (member.iconKey.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    MemberIcons.getIcon(member.iconKey),
                    contentDescription = member.name,
                    modifier = Modifier.size((size * 0.5f).dp),
                    tint = colorScheme.onPrimaryContainer
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.avatar,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        if (showCameraBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(16.dp)
                    .background(colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Change photo",
                    modifier = Modifier.size(9.dp),
                    tint = colorScheme.onPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberIconPickerRow(
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    FlowRow(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MemberIcons.allIcons.forEach { (key, icon) ->
            val isSelected = key == selectedKey
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) colorScheme.primary
                        else colorScheme.surfaceContainerHigh
                    )
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = key,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) colorScheme.onPrimary
                    else colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
