package com.example.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.BrushType
import com.example.model.CanvasSize
import com.example.model.DrawPoint
import com.example.model.MirrorMode
import com.example.model.NetworkMode
import com.example.model.Painter
import com.example.model.Room
import com.example.model.RoomTheme
import com.example.network.PaintConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.math.cos
import kotlin.math.sin

class PaintViewModel : ViewModel() {

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms = _rooms.asStateFlow()

    private val _activeRoom = MutableStateFlow<Room?>(null)
    val activeRoom = _activeRoom.asStateFlow()

    private val _points = MutableStateFlow<List<DrawPoint>>(emptyList())
    val points = _points.asStateFlow()

    private val _activePainters = MutableStateFlow<List<Painter>>(emptyList())
    val activePainters = _activePainters.asStateFlow()

    // Local brush configurations
    val brushColor = MutableStateFlow(0xFF6366F1.toInt()) // Indigo Default
    val brushThickness = MutableStateFlow(12f)
    val selectedBrushType = MutableStateFlow(BrushType.BRUSH)

    // Local user information
    val localUserName = MutableStateFlow("Artist_${(100..999).random()}")
    val localUserColor = MutableStateFlow(0xFFEC4899.toInt()) // Pink

    // Network manager
    private var connectionManager: PaintConnectionManager? = null

    // Jobs
    private var virtualGuestsJob: Job? = null
    private var moveBroadcastJob: Job? = null

    // Undo/Redo stacks
    private val strokeHistory = mutableListOf<List<DrawPoint>>()
    private val redoStack = mutableListOf<List<DrawPoint>>()

    // Temporary storage for current stroke points
    private val currentStrokePoints = mutableListOf<DrawPoint>()

    init {
        // Initialize with 2 beautifully customized default rooms as requested!
        _rooms.value = listOf(
            Room(
                id = "room_space_gallery",
                name = "🌌 Космическая Галерея (Space Gallery)",
                canvasSize = CanvasSize.STANDARD,
                theme = RoomTheme.DARK_SPACE,
                mirrorMode = MirrorMode.NONE,
                isNeonMode = true,
                isVirtualGuestsEnabled = true,
                networkMode = NetworkMode.SOLO
            ),
            Room(
                id = "room_neon_maze",
                name = "⚡ Киберпанк Скетч-Борд (Cyberpunk Board)",
                canvasSize = CanvasSize.SQUARE,
                theme = RoomTheme.NEON_CYBER,
                mirrorMode = MirrorMode.FOUR_WAY,
                isNeonMode = true,
                isVirtualGuestsEnabled = true,
                networkMode = NetworkMode.SOLO
            )
        )
    }

    fun createRoom(
        name: String,
        canvasSize: CanvasSize,
        theme: RoomTheme,
        mirrorMode: MirrorMode,
        isNeon: Boolean,
        isGuests: Boolean,
        netMode: NetworkMode,
        hostIp: String = ""
    ) {
        val newRoom = Room(
            id = "room_${System.currentTimeMillis()}",
            name = name.ifEmpty { "Комната #${(10..99).random()}" },
            canvasSize = canvasSize,
            theme = theme,
            mirrorMode = mirrorMode,
            isNeonMode = isNeon,
            isVirtualGuestsEnabled = isGuests,
            networkMode = netMode,
            hostIp = hostIp
        )
        _rooms.value = _rooms.value + newRoom
        enterRoom(newRoom)
    }

    fun enterRoom(room: Room) {
        _activeRoom.value = room
        _points.value = emptyList()
        strokeHistory.clear()
        redoStack.clear()

        // Configure connection manager if networked
        if (room.networkMode != NetworkMode.SOLO) {
            setupNetworking(room)
        } else {
            // Solo but we can enable local virtual painters
            _activePainters.value = listOf(
                Painter("me", "${localUserName.value} (Вы)", localUserColor.value)
            )
        }

        if (room.isVirtualGuestsEnabled) {
            startVirtualGuests()
        }
    }

    fun leaveRoom() {
        stopVirtualGuests()
        stopNetworkBroadcasts()
        connectionManager?.stopAll()
        connectionManager = null
        _activeRoom.value = null
        _points.value = emptyList()
        _activePainters.value = emptyList()
        strokeHistory.clear()
        redoStack.clear()
    }

    private fun setupNetworking(room: Room) {
        val manager = PaintConnectionManager(
            scope = viewModelScope,
            onPointReceived = { receivedPoint ->
                viewModelScope.launch(Dispatchers.Main) {
                    addRemotePoint(receivedPoint)
                }
            },
            onClearReceived = {
                viewModelScope.launch(Dispatchers.Main) {
                    clearCanvasLocal()
                }
            },
            onPainterJoined = { painter ->
                viewModelScope.launch(Dispatchers.Main) {
                    _activePainters.value = (_activePainters.value + painter).distinctBy { it.id }
                }
            },
            onPainterMoved = { userId, x, y, isDrawing ->
                viewModelScope.launch(Dispatchers.Main) {
                    updatePainterPosition(userId, x, y, isDrawing)
                }
            }
        )
        connectionManager = manager

        // Set local active painters
        _activePainters.value = listOf(
            Painter("me", "${localUserName.value} (Вы)", localUserColor.value)
        )

        if (room.networkMode == NetworkMode.HOST) {
            manager.startHost()
        } else if (room.networkMode == NetworkMode.JOIN) {
            manager.startJoin(room.hostIp)
        }

        // Start broadcasting current user cursor position periodically
        startNetworkBroadcasts()
    }

    fun getConnectionStateFlow() = connectionManager?.connectionState
    fun getLocalIpFlow() = connectionManager?.localIp

    private fun startNetworkBroadcasts() {
        moveBroadcastJob?.cancel()
        moveBroadcastJob = viewModelScope.launch {
            while (true) {
                delay(100) // Broadcast our movement cursor 10 times a second
                val localPainter = _activePainters.value.find { it.id == "me" } ?: continue
                if (localPainter.currentX >= 0) {
                    connectionManager?.broadcastMove(
                        userId = "me",
                        x = localPainter.currentX,
                        y = localPainter.currentY,
                        isDrawing = localPainter.isDrawing
                    )
                }
            }
        }
    }

    private fun stopNetworkBroadcasts() {
        moveBroadcastJob?.cancel()
        moveBroadcastJob = null
    }

    // --- Drawing Actions ---

    fun onLocalDrawStart(x: Float, y: Float) {
        val color = if (selectedBrushType.value == BrushType.ERASER) {
            android.graphics.Color.parseColor(_activeRoom.value?.theme?.bgColorHex ?: "#FFFFFF")
        } else {
            brushColor.value
        }

        val startPoint = DrawPoint(
            x = x,
            y = y,
            isStart = true,
            isEnd = false,
            color = color,
            thickness = brushThickness.value,
            brushType = selectedBrushType.value,
            userId = "me"
        )

        currentStrokePoints.clear()
        currentStrokePoints.add(startPoint)

        addLocalPointsAndSync(listOf(startPoint))
        updatePainterPosition("me", x, y, isDrawing = true)
    }

    fun onLocalDrawDrag(x: Float, y: Float) {
        if (currentStrokePoints.isEmpty()) return
        val lastPoint = currentStrokePoints.last()
        // Threshold check to avoid redundant points and keep performance blistering fast
        val distSqr = (lastPoint.x - x) * (lastPoint.x - x) + (lastPoint.y - y) * (lastPoint.y - y)
        if (distSqr < 1.5f && selectedBrushType.value != BrushType.SPRAY) return

        val color = if (selectedBrushType.value == BrushType.ERASER) {
            android.graphics.Color.parseColor(_activeRoom.value?.theme?.bgColorHex ?: "#FFFFFF")
        } else {
            brushColor.value
        }

        val dragPoint = DrawPoint(
            x = x,
            y = y,
            isStart = false,
            isEnd = false,
            color = color,
            thickness = brushThickness.value,
            brushType = selectedBrushType.value,
            userId = "me"
        )
        currentStrokePoints.add(dragPoint)

        addLocalPointsAndSync(listOf(dragPoint))
        updatePainterPosition("me", x, y, isDrawing = true)
    }

    fun onLocalDrawEnd() {
        if (currentStrokePoints.isEmpty()) return
        val lastPoint = currentStrokePoints.last()
        val endPoint = lastPoint.copy(isEnd = true)
        currentStrokePoints.add(endPoint)

        addLocalPointsAndSync(listOf(endPoint))
        updatePainterPosition("me", -1f, -1f, isDrawing = false)

        // Save stroke to local history
        strokeHistory.add(currentStrokePoints.toList())
        redoStack.clear() // Clear redo stack on new drawing
        currentStrokePoints.clear()
    }

    fun undoLastStroke() {
        if (strokeHistory.isEmpty()) return
        val removed = strokeHistory.removeLast()
        redoStack.add(removed)
        rebuildPointsFromHistory()
    }

    fun redoLastStroke() {
        if (redoStack.isEmpty()) return
        val restored = redoStack.removeLast()
        strokeHistory.add(restored)
        rebuildPointsFromHistory()
    }

    private fun rebuildPointsFromHistory() {
        val newPoints = mutableListOf<DrawPoint>()
        for (stroke in strokeHistory) {
            newPoints.addAll(stroke)
        }
        _points.value = newPoints
        // Optionally send full clear and redraw commands over network in complex apps,
        // but here we simply sync locally to avoid messing other peers.
    }

    private fun addLocalPointsAndSync(localPoints: List<DrawPoint>) {
        val room = _activeRoom.value ?: return
        val pointsToAdd = mutableListOf<DrawPoint>()

        for (point in localPoints) {
            pointsToAdd.add(point)
            // Handle Mirror Mode symmetry expansion
            if (room.mirrorMode != MirrorMode.NONE) {
                pointsToAdd.addAll(generateMirroredPoints(point, room.mirrorMode))
            }
        }

        _points.value = _points.value + pointsToAdd

        // Sync points over network connection
        scopeNetworkSync(pointsToAdd)
    }

    private fun scopeNetworkSync(syncList: List<DrawPoint>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (p in syncList) {
                connectionManager?.broadcastPoint(p)
            }
        }
    }

    private fun addRemotePoint(point: DrawPoint) {
        _points.value = _points.value + point
        // Check if we need to declare this user in active painter list
        val exists = _activePainters.value.any { it.id == point.userId }
        if (!exists) {
            val randomColors = listOf(0xFF22C55E, 0xFFEAB308, 0xFF3B82F6, 0xFFA855F7, 0xFFEF4444)
            _activePainters.value = _activePainters.value + Painter(
                id = point.userId,
                name = if (point.userId.startsWith("guest_")) "Guest Painter" else "Co-Artist",
                color = randomColors.random().toInt()
            )
        }
    }

    private fun updatePainterPosition(userId: String, x: Float, y: Float, isDrawing: Boolean) {
        _activePainters.value = _activePainters.value.map { painter ->
            if (painter.id == userId) {
                painter.copy(currentX = x, currentY = y, isDrawing = isDrawing)
            } else {
                painter
            }
        }
    }

    private fun generateMirroredPoints(point: DrawPoint, mode: MirrorMode): List<DrawPoint> {
        val list = mutableListOf<DrawPoint>()
        val mx = 1000f - point.x
        val my = 1000f - point.y

        when (mode) {
            MirrorMode.HORIZONTAL -> {
                list.add(point.copy(x = mx))
            }
            MirrorMode.VERTICAL -> {
                list.add(point.copy(y = my))
            }
            MirrorMode.FOUR_WAY -> {
                list.add(point.copy(x = mx))
                list.add(point.copy(y = my))
                list.add(point.copy(x = mx, y = my))
            }
            else -> {}
        }
        return list
    }

    fun clearCanvas() {
        clearCanvasLocal()
        viewModelScope.launch(Dispatchers.IO) {
            connectionManager?.broadcastClear()
        }
    }

    private fun clearCanvasLocal() {
        _points.value = emptyList()
        strokeHistory.clear()
        redoStack.clear()
    }

    // --- Virtual Co-Painters (Simulation) Loops ---

    private fun startVirtualGuests() {
        stopVirtualGuests()

        // Create 2 virtual painter entries in painter list
        val dmitry = Painter("guest_dmitry", "Дмитрий (AI) 👨‍🎨", 0xFF3B82F6.toInt(), isVirtual = true)
        val elena = Painter("guest_elena", "Елена (AI) 👩‍🎨", 0xFF10B981.toInt(), isVirtual = true)

        _activePainters.value = (_activePainters.value + dmitry + elena).distinctBy { it.id }

        virtualGuestsJob = viewModelScope.launch(Dispatchers.Default) {
            var tick = 0f
            var dmitryStart = true
            var elenaStart = true

            while (true) {
                delay(35) // Elegant ticks for fluid drawing
                tick += 0.04f

                val room = _activeRoom.value ?: break
                if (!room.isVirtualGuestsEnabled) break

                // --- Dmitry's math: Gorgeous Star Mandala ---
                val dmitryRadius = 250f * sin(3f * tick) + 300f
                val dx = 500f + dmitryRadius * cos(tick)
                val dy = 500f + dmitryRadius * sin(tick)

                // Clamp coordinates
                val clDmitryX = dx.coerceIn(50f, 950f)
                val clDmitryY = dy.coerceIn(50f, 950f)

                val dp = DrawPoint(
                    x = clDmitryX,
                    y = clDmitryY,
                    isStart = dmitryStart,
                    isEnd = false,
                    color = dmitry.color,
                    thickness = 8f,
                    brushType = BrushType.BRUSH,
                    userId = dmitry.id
                )
                dmitryStart = false

                // Dmitry occasionally finishes his stroke and lifts his brush to make it look realistic!
                val sendDmitryPoints = mutableListOf(dp)
                if ((1..120).random() == 42) {
                    sendDmitryPoints.add(dp.copy(isEnd = true))
                    dmitryStart = true
                }

                // --- Elena's math: Floating Sine Ribbon ---
                val ex = 100f + (tick * 80f) % 800f
                val ey = 300f + 180f * sin(tick * 1.5f) + 100f * cos(tick * 0.4f)

                val ep = DrawPoint(
                    x = ex,
                    y = ey,
                    isStart = elenaStart,
                    isEnd = false,
                    color = elena.color,
                    thickness = 15f,
                    brushType = BrushType.NEON,
                    userId = elena.id
                )
                elenaStart = false

                val sendElenaPoints = mutableListOf(ep)
                if (ex > 880f) {
                    sendElenaPoints.add(ep.copy(isEnd = true))
                    elenaStart = true
                }

                // Append points to local list in main thread
                withContext(Dispatchers.Main) {
                    _points.value = _points.value + sendDmitryPoints + sendElenaPoints
                    updatePainterPosition(dmitry.id, clDmitryX, clDmitryY, isDrawing = true)
                    updatePainterPosition(elena.id, ex, ey, isDrawing = true)
                }
            }
        }
    }

    private fun stopVirtualGuests() {
        virtualGuestsJob?.cancel()
        virtualGuestsJob = null
    }

    // --- Exquisite High-Resolution Saving Engine ---

    fun saveDrawingToDevice(context: Context) {
        val room = _activeRoom.value ?: return
        val drawingPoints = _points.value

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Determine bitmap size
                val width = 1080
                val height = when (room.canvasSize) {
                    CanvasSize.STANDARD -> 1920
                    CanvasSize.SQUARE -> 1080
                    CanvasSize.WIDESCREEN -> 608 // 16:9 ratio matches approx
                    CanvasSize.PORTRAIT -> 1440 // 3:4 ratio matches approx
                }

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Fill theme background color
                val paintBg = Paint().apply {
                    color = Color.parseColor(room.theme.bgColorHex)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintBg)

                // Draw standard subtle grid if theme has grid
                if (room.theme.hasGrid) {
                    val paintGrid = Paint().apply {
                        color = Color.parseColor(room.theme.gridColorHex)
                        strokeWidth = 2f
                        style = Paint.Style.STROKE
                    }
                    val step = 60f
                    var x = 0f
                    while (x < width) {
                        canvas.drawLine(x, 0f, x, height.toFloat(), paintGrid)
                        x += step
                    }
                    var y = 0f
                    while (y < height) {
                        canvas.drawLine(0f, y, width.toFloat(), y, paintGrid)
                        y += step
                    }
                }

                // Draw points on the canvas
                val paintBrush = Paint().apply {
                    isAntiAlias = true
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }

                // To draw lines, we keep track of previous points per user
                val lastPointMap = mutableMapOf<String, DrawPoint>()

                for (point in drawingPoints) {
                    val px = (point.x / 1000f) * width
                    val py = (point.y / 1000f) * height

                    if (point.isStart) {
                        lastPointMap[point.userId] = point
                        // Draw start dot
                        paintBrush.color = point.color
                        paintBrush.strokeWidth = point.thickness
                        canvas.drawCircle(px, py, point.thickness / 2f, paintBrush)
                    } else {
                        val prev = lastPointMap[point.userId]
                        if (prev != null) {
                            val prevX = (prev.x / 1000f) * width
                            val prevY = (prev.y / 1000f) * height

                            // Setup brush style
                            paintBrush.color = point.color
                            paintBrush.strokeWidth = point.thickness

                            when (point.brushType) {
                                BrushType.PENCIL -> {
                                    paintBrush.alpha = 180 // semi-transparent
                                    canvas.drawLine(prevX, prevY, px, py, paintBrush)
                                }
                                BrushType.BRUSH, BrushType.ERASER -> {
                                    paintBrush.alpha = 255
                                    canvas.drawLine(prevX, prevY, px, py, paintBrush)
                                }
                                BrushType.NEON -> {
                                    // Neon glow: 1st draw thick glowing halo
                                    paintBrush.strokeWidth = point.thickness * 2.8f
                                    paintBrush.color = point.color
                                    paintBrush.alpha = 60
                                    canvas.drawLine(prevX, prevY, px, py, paintBrush)

                                    // 2nd draw thinner center core
                                    paintBrush.strokeWidth = point.thickness
                                    paintBrush.color = Color.WHITE
                                    paintBrush.alpha = 255
                                    canvas.drawLine(prevX, prevY, px, py, paintBrush)
                                }
                                BrushType.SPRAY -> {
                                    // Draw spray spots
                                    paintBrush.alpha = 255
                                    val radius = point.thickness * 1.5f
                                    for (i in 0..6) {
                                        val rx = px + ((-100..100).random() / 100f) * radius
                                        val ry = py + ((-100..100).random() / 100f) * radius
                                        canvas.drawCircle(rx, ry, 2f, paintBrush)
                                    }
                                }
                            }
                        }
                        // Update cache
                        if (point.isEnd) {
                            lastPointMap.remove(point.userId)
                        } else {
                            lastPointMap[point.userId] = point
                        }
                    }
                }

                // Write bitmap to system media store (Pictures gallery folder)
                val fileName = "PaintRoom_${room.name.take(8).trim()}_${System.currentTimeMillis()}.png"
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PaintRooms")
                    }
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.close()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Рисунок успешно сохранен в Галерею! 🎉", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                }
                throw Exception("Failed to create file stream")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка сохранения: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
