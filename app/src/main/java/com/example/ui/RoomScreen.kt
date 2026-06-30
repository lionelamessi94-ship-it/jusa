package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrushType
import com.example.model.CanvasSize
import com.example.model.DrawPoint
import com.example.model.MirrorMode
import com.example.model.NetworkMode
import com.example.viewmodel.PaintViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(viewModel: PaintViewModel) {
    val room by viewModel.activeRoom.collectAsState()
    val activeRoomSafe = room ?: return
    val points by viewModel.points.collectAsState()
    val painters by viewModel.activePainters.collectAsState()

    val context = LocalContext.current

    // Observe local brush properties
    val color by viewModel.brushColor.collectAsState()
    val thickness by viewModel.brushThickness.collectAsState()
    val brushType by viewModel.selectedBrushType.collectAsState()

    // Observe connection state
    val netStateFlow = viewModel.getConnectionStateFlow()
    val netState = netStateFlow?.collectAsState()?.value
    val localIpFlow = viewModel.getLocalIpFlow()
    val localIp = localIpFlow?.collectAsState()?.value ?: "Checking..."

    var showClearConfirmation by remember { mutableStateOf(false) }

    // Intercept hardware system back gesture
    BackHandler {
        viewModel.leaveRoom()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC)) // Soft light background from Vibrant Palette
    ) {
        // --- TOP TOOLBAR HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.leaveRoom() },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE1E3E8), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Выйти в лобби", tint = Color(0xFF1C1B1F))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeRoomSafe.name,
                    color = Color(0xFF1C1B1F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (activeRoomSafe.networkMode) {
                                    NetworkMode.SOLO -> Color(0xFF3B82F6) // Blue for Solo
                                    NetworkMode.HOST -> Color(0xFF10B981) // Green for active Server
                                    NetworkMode.JOIN -> Color(0xFFEAB308) // Orange for connection
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (activeRoomSafe.networkMode) {
                            NetworkMode.SOLO -> "Автономная Студия ✏️"
                            NetworkMode.HOST -> "Хост (Сервер запущен!) IP: $localIp 🖥️"
                            NetworkMode.JOIN -> "Клиент (Подключение по IP) 📲"
                        },
                        color = Color(0xFF49454F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Painters Avatars List (collaborators online)
            Row(
                modifier = Modifier.padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy((-8).dp)
            ) {
                painters.take(4).forEach { painter ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(painter.color))
                            .border(1.5.dp, Color(0xFFF7F9FC), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = painter.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (painters.size > 4) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8DDFF))
                            .border(1.5.dp, Color(0xFFF7F9FC), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${painters.size - 4}",
                            color = Color(0xFF6750A4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- CENTRAL ART CANVAS WRAPPER ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (activeRoomSafe.canvasSize.aspectRatio != null) {
                            Modifier.aspectRatio(activeRoomSafe.canvasSize.aspectRatio!!)
                        } else {
                            Modifier.fillMaxHeight()
                        }
                    )
                    .shadow(16.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(android.graphics.Color.parseColor(activeRoomSafe.theme.bgColorHex)))
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = constraints.maxWidth.toFloat()
                    val canvasHeight = constraints.maxHeight.toFloat()

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("drawing_canvas")
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        // Normalize coordinates to 0..1000 range
                                        val normX = (offset.x / canvasWidth) * 1000f
                                        val normY = (offset.y / canvasHeight) * 1000f
                                        viewModel.onLocalDrawStart(normX, normY)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val offset = change.position
                                        val normX = (offset.x / canvasWidth) * 1000f
                                        val normY = (offset.y / canvasHeight) * 1000f
                                        viewModel.onLocalDrawDrag(normX, normY)
                                    },
                                    onDragEnd = {
                                        viewModel.onLocalDrawEnd()
                                    }
                                )
                            }
                    ) {
                        // 1. Draw Grid lines if the theme supports it
                        if (activeRoomSafe.theme.hasGrid) {
                            val gridColor = Color(android.graphics.Color.parseColor(activeRoomSafe.theme.gridColorHex))
                            val step = 45.dp.toPx()
                            var cx = 0f
                            while (cx < canvasWidth) {
                                drawLine(gridColor, Offset(cx, 0f), Offset(cx, canvasHeight), strokeWidth = 1f)
                                cx += step
                            }
                            var cy = 0f
                            while (cy < canvasHeight) {
                                drawLine(gridColor, Offset(0f, cy), Offset(canvasWidth, cy), strokeWidth = 1f)
                                cy += step
                            }
                        }

                        // 2. Draw all historic collaborative lines
                        val lastPointMap = mutableMapOf<String, DrawPoint>()

                        for (point in points) {
                            val px = (point.x / 1000f) * canvasWidth
                            val py = (point.y / 1000f) * canvasHeight

                            if (point.isStart) {
                                lastPointMap[point.userId] = point
                                // Draw starting dot/cap
                                drawCircle(
                                    color = Color(point.color),
                                    radius = point.thickness / 2f,
                                    center = Offset(px, py)
                                )
                            } else {
                                val prev = lastPointMap[point.userId]
                                if (prev != null) {
                                    val prevX = (prev.x / 1000f) * canvasWidth
                                    val prevY = (prev.y / 1000f) * canvasHeight

                                    when (point.brushType) {
                                        BrushType.PENCIL -> {
                                            drawLine(
                                                color = Color(point.color).copy(alpha = 0.7f),
                                                start = Offset(prevX, prevY),
                                                end = Offset(px, py),
                                                strokeWidth = point.thickness,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                        BrushType.BRUSH, BrushType.ERASER -> {
                                            drawLine(
                                                color = Color(point.color),
                                                start = Offset(prevX, prevY),
                                                end = Offset(px, py),
                                                strokeWidth = point.thickness,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                        BrushType.NEON -> {
                                            // Draw blurred glow underlay
                                            drawLine(
                                                color = Color(point.color).copy(alpha = 0.25f),
                                                start = Offset(prevX, prevY),
                                                end = Offset(px, py),
                                                strokeWidth = point.thickness * 2.8f,
                                                cap = StrokeCap.Round
                                            )
                                            // Draw solid white laser core
                                            drawLine(
                                                color = Color.White,
                                                start = Offset(prevX, prevY),
                                                end = Offset(px, py),
                                                strokeWidth = point.thickness,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                        BrushType.SPRAY -> {
                                            // Spray dots instead of connected line segments
                                            val sprayRadius = point.thickness * 1.5f
                                            for (i in 0..5) {
                                                val rx = px + ((-100..100).random() / 100f) * sprayRadius
                                                val ry = py + ((-100..100).random() / 100f) * sprayRadius
                                                drawCircle(
                                                    color = Color(point.color),
                                                    radius = 1.5.dp.toPx(),
                                                    center = Offset(rx, ry)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (point.isEnd) {
                                    lastPointMap.remove(point.userId)
                                } else {
                                    lastPointMap[point.userId] = point
                                }
                            }
                        }

                        // 3. Draw active collaborative floating pointers cursors
                        painters.forEach { painter ->
                            if (painter.id != "me" && painter.currentX >= 0) {
                                val cx = (painter.currentX / 1000f) * canvasWidth
                                val cy = (painter.currentY / 1000f) * canvasHeight

                                // Draw glowing pointer cursor
                                drawCircle(
                                    color = Color(painter.color),
                                    radius = 7.dp.toPx(),
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.dp.toPx(),
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }

                    // Floating text label labels for active drawing guests
                    painters.forEach { painter ->
                        if (painter.id != "me" && painter.currentX >= 0) {
                            val labelX = (painter.currentX / 1000f) * canvasWidth
                            val labelY = (painter.currentY / 1000f) * canvasHeight

                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .align(Alignment.TopStart)
                                    .graphicsLayerTranslate(labelX, labelY)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(painter.color).copy(alpha = 0.9f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = painter.name,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- LOWER CONTROL BAR (PALETTES & SLIDERS) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF212121))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // BRUSH TYPE SELECTOR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrushTypeButton(type = BrushType.PENCIL, displayName = "Карандаш ✏️", activeType = brushType) { viewModel.selectedBrushType.value = it }
                    BrushTypeButton(type = BrushType.BRUSH, displayName = "Кисть 🖌️", activeType = brushType) { viewModel.selectedBrushType.value = it }
                    BrushTypeButton(type = BrushType.NEON, displayName = "Неон ⚡", activeType = brushType) { viewModel.selectedBrushType.value = it }
                    BrushTypeButton(type = BrushType.SPRAY, displayName = "Спрей 💨", activeType = brushType) { viewModel.selectedBrushType.value = it }
                    BrushTypeButton(type = BrushType.ERASER, displayName = "Ластик 🧹", activeType = brushType) { viewModel.selectedBrushType.value = it }
                }

                // COLOR PALETTE ROW (Custom Circle selectors)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val brightPalettes = listOf(
                        0xFFE2E8F0.toInt(), // Soft Gray
                        0xFFEF4444.toInt(), // Bright Red
                        0xFFF97316.toInt(), // Orange
                        0xFFF59E0B.toInt(), // Gold
                        0xFF10B981.toInt(), // Green
                        0xFF06B6D4.toInt(), // Cyan
                        0xFF3B82F6.toInt(), // Indigo Blue
                        0xFF8B5CF6.toInt(), // Purple
                        0xFFEC4899.toInt(), // Pink
                        0xFFFFFFFF.toInt()  // White
                    )

                    brightPalettes.forEach { argb ->
                        val itemColor = Color(argb)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(itemColor)
                                .border(
                                    width = if (color == argb && brushType != BrushType.ERASER) 2.5.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.brushColor.value = argb
                                    if (brushType == BrushType.ERASER) {
                                        viewModel.selectedBrushType.value = BrushType.BRUSH
                                    }
                                }
                        )
                    }
                }

                // THICKNESS SLIDER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Толщина: ${thickness.toInt()}px",
                        color = Color(0xFFE1E3E8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(90.dp)
                    )

                    Slider(
                        value = thickness,
                        onValueChange = { viewModel.brushThickness.value = it },
                        valueRange = 2f..50f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE8DDFF),
                            activeTrackColor = Color(0xFF6750A4),
                            inactiveTrackColor = Color(0xFF3E3A4E)
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Size Preview Dot
                    Box(
                        modifier = Modifier
                            .size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(thickness.dp.coerceAtMost(28.dp))
                                .clip(CircleShape)
                                .background(if (brushType == BrushType.ERASER) Color.White else Color(color))
                        )
                    }
                }

                // ACTIONS BAR (Undo, Redo, Clear, Save Gallery)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo & Redo group
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.undoLastStroke() },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF2E2E2E), RoundedCornerShape(10.dp))
                        ) {
                            Text("↩️", fontSize = 16.sp)
                        }
                        IconButton(
                            onClick = { viewModel.redoLastStroke() },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF2E2E2E), RoundedCornerShape(10.dp))
                        ) {
                            Text("↪️", fontSize = 16.sp)
                        }
                    }

                    // Clear Canvas Button
                    IconButton(
                        onClick = { showClearConfirmation = true },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF4E161D), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Очистить холст", tint = Color(0xFFF43F5E))
                    }

                    // Save Button
                    IconButton(
                        onClick = { viewModel.saveDrawingToDevice(context) },
                        modifier = Modifier
                            .height(42.dp)
                            .width(110.dp)
                            .background(Color(0xFF6750A4), RoundedCornerShape(12.dp))
                            .testTag("save_drawing_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💾  ", fontSize = 14.sp)
                            Text("Сохранить", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Clear Canvas Dialog sheet or simple Alert
    if (showClearConfirmation) {
        ModalBottomSheet(
            onDismissRequest = { showClearConfirmation = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚠️ Очистить весь холст?", color = Color(0xFF1C1B1F), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Это действие полностью очистит рисунки всех участников в комнате. Вы уверены?",
                    color = Color(0xFF49454F),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { showClearConfirmation = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(Color(0xFFF0F2F6), RoundedCornerShape(12.dp))
                    ) {
                        Text("Отмена", color = Color(0xFF1C1B1F), fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = {
                            viewModel.clearCanvas()
                            showClearConfirmation = false
                            Toast.makeText(context, "Холст успешно очищен! 🧹", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(Color(0xFFBA1A1A), RoundedCornerShape(12.dp))
                    ) {
                        Text("Да, очистить", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BrushTypeButton(
    type: BrushType,
    displayName: String,
    activeType: BrushType,
    onSelect: (BrushType) -> Unit
) {
    val isSelected = activeType == type
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF6750A4) else Color(0xFF2E2E2E))
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = Color(0xFFE8DDFF),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect(type) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = when (type) {
                BrushType.PENCIL -> "✏️"
                BrushType.BRUSH -> "🖌️"
                BrushType.NEON -> "⚡"
                BrushType.SPRAY -> "💨"
                BrushType.ERASER -> "🧹"
            },
            fontSize = 18.sp
        )
    }
}

// Custom Helper Modifier to slide cursor tags smoothly without recompositions
fun Modifier.graphicsLayerTranslate(x: Float, y: Float): Modifier = this.graphicsLayer {
    translationX = x
    translationY = y
}
