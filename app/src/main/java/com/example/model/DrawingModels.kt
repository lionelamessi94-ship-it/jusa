package com.example.model

enum class BrushType {
    PENCIL,
    BRUSH,
    NEON,
    SPRAY,
    ERASER
}

enum class MirrorMode {
    NONE,
    HORIZONTAL,
    VERTICAL,
    FOUR_WAY
}

enum class RoomTheme(
    val displayName: String,
    val bgColorHex: String,
    val hasGrid: Boolean,
    val gridColorHex: String,
    val isDark: Boolean
) {
    DARK_SPACE("Midnight Space 🌌", "#0F0C1B", false, "#000000", true),
    WARM_PARCHMENT("Parchment Paper 📜", "#F9F3E3", false, "#000000", false),
    BLUE_GRID("Engineer Grid 📐", "#1E293B", true, "#334155", true),
    NEON_CYBER("Cyberpunk Grid 🌆", "#05050A", true, "#1E1B4B", true)
}

enum class CanvasSize(val displayName: String, val aspectRatio: Float?) {
    STANDARD("Screen Fit 📱", null),
    SQUARE("Instagram Square ⏹️ (1:1)", 1f),
    WIDESCREEN("Cinema Wide 📺 (16:9)", 1.777f),
    PORTRAIT("Classic Portrait 🖼️ (3:4)", 0.75f)
}

enum class NetworkMode(val displayName: String) {
    SOLO("Offline Solo Drawing ✏️"),
    HOST("Local Network Host (Server) 🖥️"),
    JOIN("Join Local Room (Client) 📲")
}

data class DrawPoint(
    val x: Float, // Normalized 0..1000
    val y: Float, // Normalized 0..1000
    val isStart: Boolean = false,
    val isEnd: Boolean = false,
    val color: Int, // ARGB format
    val thickness: Float,
    val brushType: BrushType = BrushType.BRUSH,
    val userId: String = "local_user"
)

data class Room(
    val id: String,
    val name: String,
    val canvasSize: CanvasSize,
    val theme: RoomTheme,
    val mirrorMode: MirrorMode,
    val isNeonMode: Boolean,
    val isVirtualGuestsEnabled: Boolean,
    val networkMode: NetworkMode,
    val hostIp: String = ""
)

data class Painter(
    val id: String,
    val name: String,
    val color: Int,
    val isVirtual: Boolean = false,
    val currentX: Float = -1f, // Normalized 0..1000
    val currentY: Float = -1f, // Normalized 0..1000
    val isDrawing: Boolean = false
)
