package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CanvasSize
import com.example.model.MirrorMode
import com.example.model.NetworkMode
import com.example.model.Room
import com.example.model.RoomTheme
import com.example.viewmodel.PaintViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: PaintViewModel) {
    val rooms by viewModel.rooms.collectAsState()
    val localName by viewModel.localUserName.collectAsState()
    val localColor by viewModel.localUserColor.collectAsState()

    var showCreateBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Form states
    var roomFormName by remember { mutableStateOf("") }
    var selectedCanvasSize by remember { mutableStateOf(CanvasSize.STANDARD) }
    var selectedTheme by remember { mutableStateOf(RoomTheme.DARK_SPACE) }
    var selectedMirrorMode by remember { mutableStateOf(MirrorMode.NONE) }
    var isNeonMode by remember { mutableStateOf(true) }
    var isGuestsEnabled by remember { mutableStateOf(true) }
    var networkMode by remember { mutableStateOf(NetworkMode.SOLO) }
    var hostIpAddress by remember { mutableStateOf("192.168.1.") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateBottomSheet = true },
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White,
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .testTag("create_room_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать комнату")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Создать комнату", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color(0xFFF7F9FC) // Soft light blue-gray background from Vibrant Palette
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- HEADER BANNER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_creative_header),
                    contentDescription = "Творческая студия",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
                    contentScale = ContentScale.Crop
                )
                // Overlay Gradient fading into F7F9FC
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xE8F7F9FC)),
                                startY = 100f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon),
                            contentDescription = "Лого",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, Color(0xFF6750A4), RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "PaintRooms",
                            color = Color(0xFF1C1B1F),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Совместное рисование в реальном времени ✨",
                        color = Color(0xFF49454F),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // --- PROFILE SETTINGS CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .border(1.dp, Color(0xFFE1E3E8), RoundedCornerShape(24.dp))
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Ваш профиль художника 🎨",
                        color = Color(0xFF1C1B1F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = localName,
                            onValueChange = { viewModel.localUserName.value = it },
                            label = { Text("Имя художника") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFE1E3E8),
                                focusedTextColor = Color(0xFF1C1B1F),
                                unfocusedTextColor = Color(0xFF1C1B1F),
                                focusedLabelColor = Color(0xFF6750A4),
                                unfocusedLabelColor = Color(0xFF49454F)
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF6750A4))
                            },
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Randomize Name Button
                        IconButton(
                            onClick = {
                                val adjectives = listOf("Быстрый", "Яркий", "Космический", "Арт", "Милый", "Мудрый", "Креативный")
                                val nouns = listOf("Пикассо", "Живописец", "Художник", "Мазок", "Карандаш", "Радуга", "Эскиз")
                                viewModel.localUserName.value = "${adjectives.random()}_${nouns.random()}"
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFF0F2F6), RoundedCornerShape(16.dp))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Случайное имя", tint = Color(0xFF6750A4))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Цвет вашей подписи и курсора:",
                        color = Color(0xFF49454F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Colorful Palette Dots for user pointer
                    val pointerColors = listOf(
                        0xFFEC4899.toInt(), // Hot Pink
                        0xFFF43F5E.toInt(), // Rose Red
                        0xFF3B82F6.toInt(), // Ocean Blue
                        0xFF10B981.toInt(), // Emerald Green
                        0xFFEAB308.toInt(), // Gold Yellow
                        0xFFA855F7.toInt(), // Neon Purple
                        0xFFFF7849.toInt()  // Vibrant Orange
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        pointerColors.forEach { argb ->
                            val color = Color(argb)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (localColor == argb) 3.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.localUserColor.value = argb }
                            ) {
                                if (localColor == argb) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Выбран",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- MAIN ROOM LIST TITLE ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .background(Color(0xFF6750A4), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Активные Арт-Комнаты 🎨",
                    color = Color(0xFF1C1B1F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Grid of Rooms
            if (rooms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет доступных комнат.\nСоздайте новую комнату ниже! ✨",
                        color = Color(0xFF49454F),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rooms) { room ->
                        RoomCard(room = room, onJoin = { viewModel.enterRoom(room) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
        }

        // --- CREATE ROOM DIALOG SHEET ---
        if (showCreateBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCreateBottomSheet = false },
                sheetState = bottomSheetState,
                containerColor = Color.White,
                contentColor = Color(0xFF1C1B1F),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Создание Новой Комнаты 🛠️",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1C1B1F),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Room name
                    OutlinedTextField(
                        value = roomFormName,
                        onValueChange = { roomFormName = it },
                        label = { Text("Название Арт-Пространства") },
                        placeholder = { Text("Космическая Студия") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("room_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFE1E3E8),
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            focusedLabelColor = Color(0xFF6750A4),
                            unfocusedLabelColor = Color(0xFF49454F)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Theme selector
                    Text("Выберите стиль и холст:", fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoomTheme.values().forEach { t ->
                            val isSelected = selectedTheme == t
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFE8DDFF) else Color(0xFFF0F2F6))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = Color(0xFF6750A4),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedTheme = t }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(t.displayName, color = if (isSelected) Color(0xFF6750A4) else Color(0xFF49454F), fontSize = 13.sp)
                            }
                        }
                    }

                    // Canvas size selector
                    Text("Размер и пропорции холста:", fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CanvasSize.values().forEach { s ->
                            val isSelected = selectedCanvasSize == s
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFE8DDFF) else Color(0xFFF0F2F6))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = Color(0xFF6750A4),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCanvasSize = s }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(s.displayName, color = if (isSelected) Color(0xFF6750A4) else Color(0xFF49454F), fontSize = 13.sp)
                            }
                        }
                    }

                    // Symmetry / Mirror mode
                    Text("Эффекты симметрии (Калейдоскоп):", fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MirrorMode.values().forEach { m ->
                            val isSelected = selectedMirrorMode == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFFE8DDFF) else Color(0xFFF0F2F6))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = Color(0xFF6750A4),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedMirrorMode = m }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (m) {
                                        MirrorMode.NONE -> "Выкл ❌"
                                        MirrorMode.HORIZONTAL -> "Гориз. ↔️"
                                        MirrorMode.VERTICAL -> "Верт. ↕️"
                                        MirrorMode.FOUR_WAY -> "4-Оси 🎴"
                                    },
                                    color = if (isSelected) Color(0xFF6750A4) else Color(0xFF49454F),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Неоновое Свечение Кистей ⚡", fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F), fontSize = 14.sp)
                            Text("Эффект сияющих световых линий", color = Color(0xFF49454F), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isNeonMode,
                            onCheckedChange = { isNeonMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6750A4),
                                uncheckedThumbColor = Color(0xFFE1E3E8),
                                uncheckedTrackColor = Color(0xFFF0F2F6)
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AI Виртуальные Соавторы 👨‍🎨", fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F), fontSize = 14.sp)
                            Text("Дмитрий и Елена рисуют в комнате", color = Color(0xFF49454F), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isGuestsEnabled,
                            onCheckedChange = { isGuestsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6750A4),
                                uncheckedThumbColor = Color(0xFFE1E3E8),
                                uncheckedTrackColor = Color(0xFFF0F2F6)
                            )
                        )
                    }

                    // Connection mode
                    Text("Сетевой режим совместного рисования:", fontWeight = FontWeight.Bold, color = Color(0xFF6750A4))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NetworkMode.values().forEach { mode ->
                            val isSelected = networkMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFE8DDFF) else Color(0xFFF7F9FC))
                                    .border(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFE1E3E8), RoundedCornerShape(12.dp))
                                    .clickable { networkMode = mode }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF6750A4) else Color.Transparent)
                                        .border(2.dp, Color(0xFF8F8A9F), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = mode.displayName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF6750A4) else Color(0xFF1C1B1F),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = when (mode) {
                                            NetworkMode.SOLO -> "Автономное рисование без подключения других устройств."
                                            NetworkMode.HOST -> "Создает локальный сервер. Другие могут войти по вашему IP."
                                            NetworkMode.JOIN -> "Подключиться к холсту другого устройства по IP."
                                        },
                                        color = Color(0xFF49454F),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // IP input if joining
                    AnimatedVisibility(
                        visible = networkMode == NetworkMode.JOIN,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        OutlinedTextField(
                            value = hostIpAddress,
                            onValueChange = { hostIpAddress = it },
                            label = { Text("IP адрес создателя (Host IP)") },
                            placeholder = { Text("192.168.1.15") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFE1E3E8),
                                focusedTextColor = Color(0xFF1C1B1F),
                                unfocusedTextColor = Color(0xFF1C1B1F)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Final Create Button
                    Button(
                        onClick = {
                            viewModel.createRoom(
                                name = roomFormName,
                                canvasSize = selectedCanvasSize,
                                theme = selectedTheme,
                                mirrorMode = selectedMirrorMode,
                                isNeon = isNeonMode,
                                isGuests = isGuestsEnabled,
                                netMode = networkMode,
                                hostIp = hostIpAddress
                            )
                            showCreateBottomSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("confirm_create_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Text("Создать и войти в арт-пространство 🚀", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RoomCard(room: Room, onJoin: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onJoin() }
            .border(1.dp, Color(0xFFE1E3E8), RoundedCornerShape(16.dp))
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Room Theme Icon / Decoration
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(android.graphics.Color.parseColor(room.theme.bgColorHex))),
                contentAlignment = Alignment.Center
            ) {
                if (room.theme == RoomTheme.DARK_SPACE || room.theme == RoomTheme.NEON_CYBER) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFCD34D), modifier = Modifier.size(26.dp))
                } else {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(26.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    color = Color(0xFF1C1B1F),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Feature badges
                    FeatureBadge(text = room.canvasSize.displayName, color = Color(0xFF3B82F6))
                    if (room.isNeonMode) FeatureBadge(text = "Неон ⚡", color = Color(0xFF10B981))
                    if (room.mirrorMode != MirrorMode.NONE) FeatureBadge(text = "Симметрия 🎴", color = Color(0xFFEC4899))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Enter Button Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F2F6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Войти", tint = Color(0xFF6750A4), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun FeatureBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
