package com.example.network

import android.util.Log
import com.example.model.BrushType
import com.example.model.DrawPoint
import com.example.model.Painter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class PaintConnectionManager(
    private val scope: CoroutineScope,
    private val onPointReceived: (DrawPoint) -> Unit,
    private val onClearReceived: () -> Unit,
    private val onPainterJoined: (Painter) -> Unit,
    private val onPainterMoved: (userId: String, x: Float, y: Float, isDrawing: Boolean) -> Unit
) {
    private val TAG = "PaintConnectionManager"
    private val PORT = 8888

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val clientList = mutableListOf<SocketWriterPair>()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState = _connectionState.asStateFlow()

    private val _localIp = MutableStateFlow<String>("Unknown")
    val localIp = _localIp.asStateFlow()

    private var networkJob: Job? = null

    init {
        determineLocalIp()
    }

    sealed class ConnectionState {
        object Idle : ConnectionState()
        object Searching : ConnectionState()
        data class Connected(val peerCount: Int) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    private data class SocketWriterPair(
        val socket: Socket,
        val writer: PrintWriter
    )

    private fun determineLocalIp() {
        scope.launch(Dispatchers.IO) {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is Inet4Address) {
                            _localIp.value = addr.hostAddress ?: "Unknown"
                            return@launch
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error determining IP", e)
            }
        }
    }

    fun startHost() {
        stopAll()
        _connectionState.value = ConnectionState.Searching
        networkJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(PORT).apply {
                    reuseAddress = true
                }
                Log.i(TAG, "Server started on port $PORT, waiting for clients...")

                while (true) {
                    val socket = serverSocket?.accept() ?: break
                    Log.i(TAG, "Client connected: ${socket.inetAddress}")
                    
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val pair = SocketWriterPair(socket, writer)
                    synchronized(clientList) {
                        clientList.add(pair)
                        _connectionState.value = ConnectionState.Connected(clientList.size)
                    }

                    // Start reading from this client
                    scope.launch(Dispatchers.IO) {
                        handleClientIncoming(socket)
                    }
                }
            } catch (e: Exception) {
                if (e !is SocketException) {
                    Log.e(TAG, "Server socket error", e)
                    _connectionState.value = ConnectionState.Error("Server error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun startJoin(hostIp: String) {
        stopAll()
        _connectionState.value = ConnectionState.Searching
        networkJob = scope.launch(Dispatchers.IO) {
            var attempt = 1
            while (attempt <= 3) {
                try {
                    Log.i(TAG, "Connecting to host $hostIp on port $PORT (attempt $attempt)...")
                    val socket = Socket(hostIp, PORT)
                    clientSocket = socket
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    
                    synchronized(clientList) {
                        clientList.add(SocketWriterPair(socket, writer))
                        _connectionState.value = ConnectionState.Connected(1)
                    }

                    // Introduce ourselves immediately
                    sendIntro()

                    Log.i(TAG, "Successfully connected to host!")
                    handleClientIncoming(socket)
                    break // break retry loop if successful
                } catch (e: Exception) {
                    Log.e(TAG, "Connection failed on attempt $attempt", e)
                    attempt++
                    if (attempt > 3) {
                        _connectionState.value = ConnectionState.Error("Failed to connect to $hostIp after 3 attempts.")
                    } else {
                        delay(1000)
                    }
                }
            }
        }
    }

    private fun handleClientIncoming(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            while (true) {
                val line = reader.readLine() ?: break
                parseAndDispatch(line, sourceSocket = socket)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Socket read error: ${socket.inetAddress}", e)
        } finally {
            closeSocket(socket)
        }
    }

    private fun parseAndDispatch(line: String, sourceSocket: Socket?) {
        try {
            val parts = line.split("|")
            if (parts.isEmpty()) return
            val command = parts[0]

            when (command) {
                "POINT" -> {
                    // POINT|x|y|isStart|isEnd|color|thickness|brushType|userId
                    if (parts.size >= 9) {
                        val x = parts[1].toFloatOrNull() ?: 0f
                        val y = parts[2].toFloatOrNull() ?: 0f
                        val isStart = parts[3].toBoolean()
                        val isEnd = parts[4].toBoolean()
                        val color = parts[5].toIntOrNull() ?: -1
                        val thickness = parts[6].toFloatOrNull() ?: 5f
                        val brushType = BrushType.valueOf(parts[7])
                        val userId = parts[8]

                        onPointReceived(DrawPoint(x, y, isStart, isEnd, color, thickness, brushType, userId))
                        
                        // If we are Host, broadcast to all other clients!
                        if (serverSocket != null) {
                            broadcast(line, sourceSocket)
                        }
                    }
                }
                "JOIN" -> {
                    // JOIN|userId|name|color
                    if (parts.size >= 4) {
                        val userId = parts[1]
                        val name = parts[2]
                        val color = parts[3].toIntOrNull() ?: -1

                        onPainterJoined(Painter(userId, name, color, isVirtual = false))

                        // If Host, broadcast joining message to others
                        if (serverSocket != null) {
                            broadcast(line, sourceSocket)
                        }
                    }
                }
                "MOVE" -> {
                    // MOVE|userId|x|y|isDrawing
                    if (parts.size >= 5) {
                        val userId = parts[1]
                        val x = parts[2].toFloatOrNull() ?: -1f
                        val y = parts[3].toFloatOrNull() ?: -1f
                        val isDrawing = parts[4].toBoolean()

                        onPainterMoved(userId, x, y, isDrawing)

                        if (serverSocket != null) {
                            broadcast(line, sourceSocket)
                        }
                    }
                }
                "CLEAR" -> {
                    onClearReceived()
                    if (serverSocket != null) {
                        broadcast(line, sourceSocket)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing packet: $line", e)
        }
    }

    fun broadcastPoint(point: DrawPoint) {
        val line = "POINT|${point.x}|${point.y}|${point.isStart}|${point.isEnd}|${point.color}|${point.thickness}|${point.brushType.name}|${point.userId}"
        broadcast(line)
    }

    fun broadcastMove(userId: String, x: Float, y: Float, isDrawing: Boolean) {
        val line = "MOVE|$userId|$x|$y|$isDrawing"
        broadcast(line)
    }

    fun broadcastClear() {
        broadcast("CLEAR")
    }

    fun sendLocalJoin(painter: Painter) {
        val line = "JOIN|${painter.id}|${painter.name}|${painter.color}"
        broadcast(line)
    }

    private fun sendIntro() {
        // Simple intro for local client
        val line = "JOIN|guest_${(1000..9999).random()}|Guest Painter|${-65536}" // Red color default for joined
        broadcast(line)
    }

    private fun broadcast(line: String, excludeSocket: Socket? = null) {
        synchronized(clientList) {
            val iterator = clientList.iterator()
            while (iterator.hasNext()) {
                val pair = iterator.next()
                if (pair.socket == excludeSocket) continue
                try {
                    pair.writer.println(line)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to broadcast to ${pair.socket.inetAddress}, removing client", e)
                    try { pair.socket.close() } catch (ignored: Exception) {}
                    iterator.remove()
                }
            }
            // Update counts
            if (serverSocket != null) {
                _connectionState.value = ConnectionState.Connected(clientList.size)
            }
        }
    }

    private fun closeSocket(socket: Socket) {
        synchronized(clientList) {
            val removed = clientList.removeAll { it.socket == socket }
            if (removed) {
                Log.i(TAG, "Removed disconnected socket: ${socket.inetAddress}")
                if (serverSocket != null) {
                    _connectionState.value = ConnectionState.Connected(clientList.size)
                }
            }
        }
        try {
            socket.close()
        } catch (ignored: Exception) {}
    }

    fun stopAll() {
        Log.i(TAG, "Stopping all network tasks...")
        networkJob?.cancel()
        networkJob = null

        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverSocket = null

        try {
            clientSocket?.close()
        } catch (ignored: Exception) {}
        clientSocket = null

        synchronized(clientList) {
            for (pair in clientList) {
                try { pair.socket.close() } catch (ignored: Exception) {}
            }
            clientList.clear()
        }

        _connectionState.value = ConnectionState.Idle
    }
}
