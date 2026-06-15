package com.jeiel.daddylog.ui

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.jeiel.daddylog.data.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: ScalpViewModel) {
    val context = LocalContext.current
    val isLocked by viewModel.isAppLocked.collectAsState()
    val isPinSetup by viewModel.isPinSetupRequired.collectAsState()
    
    var currentTab by remember { mutableStateOf("home") }
    val selectedDate by viewModel.selectedDate.collectAsState()

    // Date picker helper
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            viewModel.selectDate(formattedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLocked) {
            PinLockScreen(
                isSetupMode = false,
                onPinVerified = { entered -> viewModel.verifyPin(entered) },
                onCancel = { /* Locked! */ }
            )
        } else if (isPinSetup) {
            // First time PIN setup recommendation
            PinLockScreen(
                isSetupMode = true,
                onPinVerified = { newPin -> 
                    viewModel.setupPin(newPin) 
                    true
                },
                onCancel = { viewModel.removePin() }
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Spa,
                                    contentDescription = "앱 로고",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "두피케어기록",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }
                        },
                        actions = {
                            // Date selector in header
                            TextButton(
                                onClick = { datePickerDialog.show() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "날짜 선택",
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = selectedDate,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        val items = listOf(
                            Triple("home", "홈", Icons.Outlined.Home to Icons.Default.Home),
                            Triple("log", "기록", Icons.Outlined.EditCalendar to Icons.Default.EditCalendar),
                            Triple("compare", "비교", Icons.Outlined.Compare to Icons.Default.Compare),
                            Triple("stats", "통계", Icons.Outlined.BarChart to Icons.Default.BarChart),
                            Triple("settings", "설정", Icons.Outlined.Settings to Icons.Default.Settings)
                        )
                        items.forEach { (tab, label, icons) ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) icons.second else icons.first,
                                        contentDescription = label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    when (currentTab) {
                        "home" -> HomeScreen(viewModel, onNavigateToRecord = { currentTab = "log" })
                        "log" -> RecordScreenContainer(viewModel)
                        "compare" -> PhotoComparisonScreen(viewModel)
                        "stats" -> StatsScreen(viewModel)
                        "settings" -> SettingsScreen(viewModel)
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. PIN LOCK SCREEN
// ==========================================
@Composable
fun PinLockScreen(
    isSetupMode: Boolean,
    onPinVerified: (String) -> Boolean,
    onCancel: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var setupStep by remember { mutableIntStateOf(1) } // 1: enter pin, 2: confirm pin
    var tempPin by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "잠금",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    isSetupMode && setupStep == 1 -> "보안 암호 생성"
                    isSetupMode && setupStep == 2 -> "암호 확인"
                    else -> "보안 암호 입력"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = when {
                    isSetupMode -> "안전한 개인 데이터 관리를 위해 암호를 설정해주세요."
                    else -> "설정한 4자리 암호를 입력해 주세요."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            // Pin Dots Row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(48.dp)
            ) {
                for (i in 1..4) {
                    val filled = enteredPin.length >= i
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Traditional Lock Screen Keypad
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "지움", "0", "취소")
            Column(
                modifier = Modifier.widthIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val chunks = keys.chunked(3)
                chunks.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowKeys.forEach { key ->
                            Button(
                                onClick = {
                                    when (key) {
                                        "지움" -> {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                        "취소" -> {
                                            if (isSetupMode) {
                                                onCancel()
                                            } else {
                                                enteredPin = ""
                                            }
                                        }
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                if (enteredPin.length == 4) {
                                                    // Trigger Verification
                                                    if (isSetupMode) {
                                                        if (setupStep == 1) {
                                                            tempPin = enteredPin
                                                            enteredPin = ""
                                                            setupStep = 2
                                                            errorMessage = ""
                                                        } else {
                                                            if (enteredPin == tempPin) {
                                                                onPinVerified(enteredPin)
                                                            } else {
                                                                errorMessage = "암호가 일치하지 않습니다. 다시 시도하세요."
                                                                enteredPin = ""
                                                                setupStep = 1
                                                            }
                                                        }
                                                    } else {
                                                        val correct = onPinVerified(enteredPin)
                                                        if (!correct) {
                                                            errorMessage = "잘못된 암호입니다. 다시 확인해 주세요."
                                                            enteredPin = ""
                                                        } else {
                                                            errorMessage = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (key == "지움" || key == "취소") MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. HOME SCREEN (홈)
// ==========================================
@Composable
fun HomeScreen(viewModel: ScalpViewModel, onNavigateToRecord: () -> Unit) {
    val scalpRecords by viewModel.allScalpRecords.collectAsState()
    val photoRecords by viewModel.allPhotoRecords.collectAsState()
    val activeRoutines by viewModel.activeRoutines.collectAsState()
    val todayChecks by viewModel.currentChecks.collectAsState()
    val todayDateString = viewModel.getTodayString()

    // 1. Find recent scalp record date
    val recentDateText = if (scalpRecords.isNotEmpty()) {
        val mostRecent = scalpRecords.first()
        mostRecent.date
    } else {
        "기록 없음"
    }

    // 2. This month photo taken status
    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    val thisMonthPhotos = photoRecords.filter { it.date.startsWith(currentMonth) }
    val hasPhotosThisMonth = thisMonthPhotos.isNotEmpty()

    // 3. Recommended next photos schedule
    // Let's set it to be 1st day of next month, or some specific recurring recommendation 
    val nextPhotoAdvice = "매월 1일 (다음 정밀 비교 권장)"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Calm Header Panel
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "하루 한번, 소중한 두피 모발과 마주하는 시간.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "불안한 검색 대신, 오늘 나의 두피 변화를 차분히 기록하고 비교해보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            // Quick Status Summary Row
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f).height(120.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "최근 기록 날짜 📅", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = recentDateText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(modifier = Modifier.weight(1f).height(120.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "이달의 사진 기록 📸", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (hasPhotosThisMonth) "완료 (${thisMonthPhotos.size}장)" else "기록 없음",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasPhotosThisMonth) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }

            // Next Capture Notice
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CircleNotifications,
                        contentDescription = "알림",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "다음 정기 사진 촬영 권장일", fontSize = 13.sp, color = Color.Gray)
                        Text(text = nextPhotoAdvice, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Today's Quick Routines Checklist
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(
                        text = "오늘의 관리 체크리스트",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (activeRoutines.isEmpty()) {
                        Text(
                            text = "설정된 루틴이 없습니다.\n관리 루틴 설정 탭에서 복용약, 샴푸 등의 주기를 활성화해 보세요.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        )
                    } else {
                        activeRoutines.take(4).forEach { routine ->
                            val isChecked = todayChecks.any { it.routineId == routine.id && it.isDone }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleRoutineCheck(routine.id, !isChecked) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked -> viewModel.toggleRoutineCheck(routine.id, checked) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = routine.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isChecked) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                // Pill badge for category
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = getKoreanCategory(routine.category),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        if (activeRoutines.size > 4) {
                            TextButton(
                                onClick = onNavigateToRecord,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("루틴 전체보기 (${activeRoutines.size}개) >")
                            }
                        }
                    }
                }
            }

            // Big Quick Log Call-to-action
            Button(
                onClick = onNavigateToRecord,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = "기록",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("기록하러 가기 (사진 & 두피 케어)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Helper category translator
fun getKoreanCategory(cat: String): String = when(cat) {
    "SHAMPOO" -> "샴푸"
    "TONIC" -> "두피 토닉"
    "HOSPITAL" -> "병원 방문"
    "MEDICATION" -> "탈모약"
    "SUPPLEMENTS" -> "영양제"
    "MASSAGE" -> "두피 마사지"
    else -> "기타 관리"
}

// ==========================================
// 2 & 3 & 4. RECORD SCREEN CONTAINER & ALL RECORD SECTIONS
// ==========================================
@Composable
fun RecordScreenContainer(viewModel: ScalpViewModel) {
    var subTab by remember { mutableStateOf("state") } // "state", "photo", "routines"

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row
        TabRow(
            selectedTabIndex = when(subTab) {
                "state" -> 0
                "photo" -> 1
                else -> 2
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = subTab == "state",
                onClick = { subTab = "state" },
                text = { Text("두피·모발 상태", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            )
            Tab(
                selected = subTab == "photo",
                onClick = { subTab = "photo" },
                text = { Text("두피 사진 기록", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            )
            Tab(
                selected = subTab == "routines",
                onClick = { subTab = "routines" },
                text = { Text("루틴 관리 정돈", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (subTab) {
                "state" -> ScalpStateLogScreen(viewModel)
                "photo" -> PhotoLoggingScreen(viewModel)
                "routines" -> CareRoutineScreen(viewModel)
            }
        }
    }
}

// ------------------------------------------
// 3. SCALP STATE LOG SCREEN
// ------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScalpStateLogScreen(viewModel: ScalpViewModel) {
    val currentRecord by viewModel.currentScalpRecord.collectAsState()

    // Working local states
    val scalpList = listOf("보통", "건조함", "기름짐", "가려움", "비듬림", "따가움")
    val hairList = listOf("보통", "얇아짐", "빠짐 많음", "힘 없음")

    var selectedScalpSet by remember { mutableStateOf(setOf<String>()) }
    var selectedHairSet by remember { mutableStateOf(setOf<String>()) }
    var stressScore by remember { mutableIntStateOf(3) }
    var sleepHours by remember { mutableFloatStateOf(7f) }
    var alcohol by remember { mutableStateOf(false) }
    var exercise by remember { mutableStateOf(false) }
    var memo by remember { mutableStateOf("") }

    // Sync state when loaded
    LaunchedEffect(currentRecord) {
        currentRecord?.let { rec ->
            selectedScalpSet = rec.scalpState.split(",").filter { it.isNotEmpty() }.toSet()
            selectedHairSet = rec.hairState.split(",").filter { it.isNotEmpty() }.toSet()
            stressScore = rec.stressScore
            sleepHours = rec.sleepHours
            alcohol = rec.alcohol
            exercise = rec.exercise
            memo = rec.memo
        } ?: run {
            selectedScalpSet = setOf("보통")
            selectedHairSet = setOf("보통")
            stressScore = 3
            sleepHours = 7f
            alcohol = false
            exercise = false
            memo = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "오늘의 두피·모발 상태 및 습관 기록",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Scalp State Selection Checklist (Chips style)
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "두피 상태 (중복 선택)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        scalpList.forEach { state ->
                            val isSelected = selectedScalpSet.contains(state)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedScalpSet = if (isSelected) {
                                        selectedScalpSet - state
                                    } else {
                                        if (state == "보통") {
                                            setOf("보통")
                                        } else {
                                            (selectedScalpSet - "보통") + state
                                        }
                                    }
                                },
                                label = { Text(state, fontSize = 15.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Hair State Selection
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "모발 상태 (중복 선택)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        hairList.forEach { state ->
                            val isSelected = selectedHairSet.contains(state)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedHairSet = if (isSelected) {
                                        selectedHairSet - state
                                    } else {
                                        if (state == "보통") {
                                            setOf("보통")
                                        } else {
                                            (selectedHairSet - "보통") + state
                                        }
                                    }
                                },
                                label = { Text(state, fontSize = 15.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Stress Score 1~5 Slider
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "스트레스 강도", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = when(stressScore) {
                                1 -> "1점 - 편안함 😄"
                                2 -> "2점 - 가벼운 스트레스"
                                3 -> "3점 - 보통 😐"
                                4 -> "4점 - 긴장 및 피로"
                                else -> "5점 - 힘듦 😫"
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = stressScore.toFloat(),
                        onValueChange = { stressScore = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Sleep Hours Custom Picker / Slider
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "수면 시간", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("%.1f시간 😴", sleepHours),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = sleepHours,
                        onValueChange = { sleepHours = it },
                        valueRange = 2f..13f,
                        steps = 22,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Life Habits Toggle Habits Switchers Row
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "생활 습관 기록", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { alcohol = !alcohol }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("술을 섭취했습니다 🍻", fontSize = 16.sp)
                        Switch(checked = alcohol, onCheckedChange = { alcohol = it })
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { exercise = !exercise }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("충분히 운동했습니다 🏃‍♂️", fontSize = 16.sp)
                        Switch(checked = exercise, onCheckedChange = { exercise = it })
                    }
                }
            }

            // Custom memo editor
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "메모 작성 (복용 후기, 특이 사항 등)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = memo,
                        onValueChange = { memo = it },
                        placeholder = { Text("샴푸 변경 후 간지러움이 덜함, 토닉 꼼꼼히 바름 등...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )
                }
            }

            // Big Save Button
            Button(
                onClick = {
                    viewModel.saveScalpCondition(
                        scalpState = selectedScalpSet.toList(),
                        hairState = selectedHairSet.toList(),
                        stressScore = stressScore,
                        sleepHours = sleepHours,
                        alcohol = alcohol,
                        exercise = exercise,
                        memo = memo
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "저장")
                Spacer(modifier = Modifier.width(8.dp))
                Text("기록 저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ------------------------------------------
// 2. PHOTO LOGGING SCREEN
// ------------------------------------------
@Composable
fun PhotoLoggingScreen(viewModel: ScalpViewModel) {
    val context = LocalContext.current
    val currentAngle by viewModel.defaultAngleType.collectAsState()
    val allPhotosList by viewModel.allPhotoRecords.collectAsState()

    val angles = listOf("정면", "위쪽", "양쪽 측면", "정수리")
    var selectedAngle by remember { mutableStateOf(currentAngle) }
    var showCamera by remember { mutableStateOf(false) }
    var photoMemo by remember { mutableStateOf("") }

    // List of existing photos taken today
    val selectedDate by viewModel.selectedDate.collectAsState()
    val todayPhotos = allPhotosList.filter { it.date == selectedDate }

    // Activity launcher for choosing photo from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.savePhotoRecord(selectedAngle, it, photoMemo) {
                photoMemo = ""
            }
        }
    }

    if (showCamera) {
        CameraPreviewWithGuides(
            angleType = selectedAngle,
            onImageCaptured = { file ->
                val localPath = viewModel.saveCapturedImageFile(file, selectedAngle)
                if (localPath != null) {
                    viewModel.addDirectPhotoRecord(selectedAngle, localPath, photoMemo)
                    photoMemo = ""
                }
                showCamera = false
            },
            onClose = { showCamera = false }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "두피/모발 사진 기록 촬영",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Large Angle selector row
                Text(text = "기록할 각도 선택", fontSize = 14.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    angles.forEach { angle ->
                        val isSelected = selectedAngle == angle
                        val hasTakenToday = todayPhotos.any { it.angleType == angle }
                        
                        Button(
                            onClick = { selectedAngle = angle },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary 
                                                 else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                               else MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = angle, fontSize = 14.sp)
                                if (hasTakenToday) {
                                    Text(text = " 완료 ✔", fontSize = 10.sp, color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Active Angle Capture box
                val currentPhotoForAngle = todayPhotos.find { it.angleType == selectedAngle }

                if (currentPhotoForAngle != null) {
                    // Show taken photo with delete capability
                    Text(text = "오늘 측정한 $selectedAngle 사진", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = File(currentPhotoForAngle.imagePath),
                            contentDescription = "$selectedAngle 이미지",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.deletePhotoRecord(currentPhotoForAngle) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.White)
                        }
                    }
                    if (currentPhotoForAngle.memo.isNotEmpty()) {
                        Text(
                            text = "메모: ${currentPhotoForAngle.memo}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                } else {
                    // Prompt for taking photo
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "촬영 대기",
                            tint = Color.Gray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "오늘의 '$selectedAngle' 사진 기록 선택",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "가이드 선에 맞춰서 일정하게 기록하면 월별 변화를 확인하기가 더 쉬워집니다.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Short memo for this photo record (can be filled before capture/gallery action)
                if (currentPhotoForAngle == null) {
                    OutlinedTextField(
                        value = photoMemo,
                        onValueChange = { photoMemo = it },
                        label = { Text("사진 촬영시 추가 캡션 메모") },
                        placeholder = { Text("예: 샴푸 직후, 전반적인 정면 상태 기록") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons to select/photo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Gallary Select button
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "갤러리")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("갤러리 선택", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        // Capture Camera button
                        Button(
                            onClick = { showCamera = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "촬영하기")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("실시간 촬영", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ------------------------------------------
// CAMERA DIRECT VIEW COMPOSABLE USING CAMERAX
// ------------------------------------------
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPreviewWithGuides(
    angleType: String,
    onImageCaptured: (File) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Request permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Warning, contentDescription = "경고", tint = Color.Gray, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("실시간 카메라 촬영을 위해 카메라 권한이 필요합니다.", textAlign = TextAlign.Center, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("권한 허용 요청")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onClose) {
                    Text("촬영 취소")
                }
            }
        }
        return
    }

    // Camera Configuration
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Embed Camera previewView
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Guide overlay lines inside custom canvas drawing (Non-intrusive dotted guidelines)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            when (angleType) {
                "정면" -> {
                    // Render large Oval helper Guide
                    drawOval(
                        color = Color.White.copy(alpha = 0.5f),
                        topLeft = Offset(width * 0.15f, height * 0.2f),
                        size = Size(width * 0.7f, height * 0.45f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    )
                }
                "위쪽", "정수리" -> {
                    // Render center circle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        center = Offset(width / 2, height * 0.45f),
                        radius = width * 0.25f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    )
                }
                else -> {
                    // 양쪽 측면 Guide lines
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(width * 0.3f, height * 0.2f),
                        end = Offset(width * 0.3f, height * 0.7f),
                        strokeWidth = 6f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(width * 0.7f, height * 0.2f),
                        end = Offset(width * 0.7f, height * 0.7f),
                        strokeWidth = 6f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                }
            }
        }

        // Action panel at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "'$angleType' 촬영 가이드 선에 맞추어 찍어보세요.",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel
                TextButton(onClick = onClose) {
                    Text("취소", color = Color.White, fontSize = 16.sp)
                }

                // Capture Circle button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            val file = File.createTempFile("photo_capture_${angleType.hashCode()}", ".jpg", context.cacheDir)
                            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                            imageCapture.takePicture(
                                outputFileOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        onImageCaptured(file)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.Black, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(48.dp)) // balancing space
            }
        }
    }
}

// ------------------------------------------
// 4. CARE ROUTINE SCREEN
// ------------------------------------------
@Composable
fun CareRoutineScreen(viewModel: ScalpViewModel) {
    val routines by viewModel.allRoutines.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newRoutineTitle by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("OTHER") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "나의 전용 관리 루틴 목록",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "추가", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("추가", fontSize = 14.sp)
                }
            }

            Text(
                text = "복용하고 계신 약이나 일일 두피 마사지, 전용 샴푸 등을 활성화하고 관리하세요.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            if (routines.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("루틴 데이터가 비어있습니다.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(routines) { routine ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (routine.isActive) MaterialTheme.colorScheme.surface 
                                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = routine.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (routine.isActive) MaterialTheme.colorScheme.onSurface 
                                               else Color.Gray
                                    )
                                    Text(
                                        text = "카테고리: ${getKoreanCategory(routine.category)}",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Active checker switch
                                Text(
                                    text = if (routine.isActive) "사용 중" else "숨김",
                                    fontSize = 12.sp,
                                    color = if (routine.isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Switch(
                                    checked = routine.isActive,
                                    onCheckedChange = { active -> viewModel.toggleRoutineActive(routine, active) }
                                )

                                IconButton(onClick = { viewModel.deleteRoutine(routine) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal Pop to Add Routine
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("새로운 케어 루틴 추가", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newRoutineTitle,
                            onValueChange = { newRoutineTitle = it },
                            label = { Text("루틴 이름") },
                            placeholder = { Text("예: 아침 프로페시아 복용, 저녁 샴푸 등") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("카테고리 선택", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        val catList = listOf(
                            "MEDICATION" to "탈모약 복용",
                            "SHAMPOO" to "샴푸 세정",
                            "TONIC" to "두피 토닉 도포",
                            "SUPPLEMENTS" to "영양제 복용",
                            "MASSAGE" to "두피 마사지",
                            "HOSPITAL" to "병원 방문",
                            "OTHER" to "기타 관리"
                        )
                        
                        var expanded by remember { mutableStateOf(false) }
                        val currentLabel = catList.find { it.first == newCategory }?.second ?: "기타 관리"

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(currentLabel)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                catList.forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            newCategory = code
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newRoutineTitle.isNotEmpty()) {
                                viewModel.addNewRoutine(newRoutineTitle, newCategory)
                                newRoutineTitle = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("추가하기")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

// ==========================================
// 5. PHOTO COMPARISON SCREEN (비교 화면)
// ==========================================
@Composable
fun PhotoComparisonScreen(viewModel: ScalpViewModel) {
    val allPhotos by viewModel.allPhotoRecords.collectAsState()
    
    val angles = listOf("정면", "위쪽", "양쪽 측면", "정수리")
    var selectedAngle by remember { mutableStateOf("정면") }

    // Filtered list based on angle
    val anglePhotos = allPhotos.filter { it.angleType == selectedAngle }

    var leftPhoto by remember { mutableStateOf<HairPhotoRecord?>(null) }
    var rightPhoto by remember { mutableStateOf<HairPhotoRecord?>(null) }
    
    var showLeftDropdown by remember { mutableStateOf(false) }
    var showRightDropdown by remember { mutableStateOf(false) }

    // Automatically set default items when the photos list loads/changes
    LaunchedEffect(anglePhotos) {
        if (anglePhotos.size >= 2) {
            leftPhoto = anglePhotos[1]
            rightPhoto = anglePhotos[0]
        } else if (anglePhotos.isNotEmpty()) {
            leftPhoto = anglePhotos[0]
            rightPhoto = null
        } else {
            leftPhoto = null
            rightPhoto = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "날짜별 두피 변화 기록 비교",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "의학적 판단이나 진단 없이, 같은 위치에서 촬영된 기록을 좌우로 비교하며 스스로 상태의 유지를 확인하기만 합니다.",
            fontSize = 13.sp,
            color = Color.Gray
        )

        // Angle Selector Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            angles.forEach { angle ->
                val isSelected = selectedAngle == angle
                Button(
                    onClick = { selectedAngle = angle },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary 
                                         else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                       else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(text = angle, fontSize = 14.sp)
                }
            }
        }

        if (anglePhotos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "이해하기 편한 비교를 위해\n 먼저 '$selectedAngle' 사진 기록을 촬영/기록해 보세요.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        } else {
            // Dropdown Pickers Row for Date comparison selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Photo Date Picker
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showLeftDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = leftPhoto?.date ?: "기록 선택 (좌)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = showLeftDropdown,
                        onDismissRequest = { showLeftDropdown = false }
                    ) {
                        anglePhotos.forEach { photo ->
                            DropdownMenuItem(
                                text = { Text("${photo.date} (${photo.angleType})") },
                                onClick = {
                                    leftPhoto = photo
                                    showLeftDropdown = false
                                }
                            )
                        }
                    }
                }

                // Right Photo Date Picker
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showRightDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = rightPhoto?.date ?: "기록 선택 (우)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(
                        expanded = showRightDropdown,
                        onDismissRequest = { showRightDropdown = false }
                    ) {
                        anglePhotos.forEach { photo ->
                            DropdownMenuItem(
                                text = { Text("${photo.date} (${photo.angleType})") },
                                onClick = {
                                    rightPhoto = photo
                                    showRightDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Dual Photo Display Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Card View
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (leftPhoto != null) {
                            AsyncImage(
                                model = File(leftPhoto!!.imagePath),
                                contentDescription = "좌측 비교 이미지",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = leftPhoto!!.date,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Text("비교군 미선택", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                // Right Card View
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (rightPhoto != null) {
                            AsyncImage(
                                model = File(rightPhoto!!.imagePath),
                                contentDescription = "우측 비교 이미지",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = rightPhoto!!.date,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Text("비교군 미선택", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Captions comparing notes
            if (leftPhoto != null || rightPhoto != null) {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                        Text("기록된 특징 비교", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (leftPhoto != null && leftPhoto!!.memo.isNotEmpty()) {
                            Text(text = "• [${leftPhoto!!.date}]: ${leftPhoto!!.memo}", fontSize = 14.sp)
                        }
                        if (rightPhoto != null && rightPhoto!!.memo.isNotEmpty()) {
                            Text(text = "• [${rightPhoto!!.date}]: ${rightPhoto!!.memo}", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        if ((leftPhoto?.memo?.isEmpty() != false) && (rightPhoto?.memo?.isEmpty() != false)) {
                            Text("기록된 메모 캡션 정보가 없습니다.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. STATISTICS SCREEN (통계 화면)
// ==========================================
@Composable
fun StatsScreen(viewModel: ScalpViewModel) {
    val scalpRecords by viewModel.allScalpRecords.collectAsState()
    val allPhotos by viewModel.allPhotoRecords.collectAsState()
    val routines by viewModel.allRoutines.collectAsState()

    // 1. Calculations
    val totalLogsCount = scalpRecords.size
    val totalPhotosCount = allPhotos.size

    val avgStress = if (scalpRecords.isNotEmpty()) {
        scalpRecords.map { it.stressScore }.average()
    } else {
        0.0
    }

    val avgSleep = if (scalpRecords.isNotEmpty()) {
        scalpRecords.map { it.sleepHours }.average()
    } else {
        0.0
    }

    val alcoholCount = scalpRecords.count { it.alcohol }
    val exerciseCount = scalpRecords.count { it.exercise }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "나의 지표 및 활동 통계",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // General numbers Row
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("안정적 기록 횟수 ✏", fontSize = 14.sp, color = Color.Gray)
                    Text("${totalLogsCount}회", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("누적 보관 사진 📸", fontSize = 14.sp, color = Color.Gray)
                    Text("${totalPhotosCount}장", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Stress & Sleep Indicators
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("평균 일상 지표 측정", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                // Stress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("스트레스 평균 강도", fontSize = 15.sp)
                    Text(String.format("%.1f / 5.0", avgStress), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { if (avgStress > 0) (avgStress.toFloat() / 5f) else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp)
                )

                // Sleep indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("하루 평균 수면 수량", fontSize = 15.sp)
                    Text(String.format("%.1f시간", avgSleep), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { if (avgSleep > 0) (avgSleep.toFloat() / 12f).coerceAtMost(1f) else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
        }

        // Habits frequency
        Card(shape = RoundedCornerShape(20.dp)) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("음주 통산 일수 🍻", fontSize = 13.sp, color = Color.Gray)
                    Text("${alcoholCount}회", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("운동 달성 일수 🏃‍♂️", fontSize = 13.sp, color = Color.Gray)
                    Text("${exerciseCount}회", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        // Monthly Memos overview lists
        val recordsWithMemos = scalpRecords.filter { it.memo.isNotEmpty() }
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("월별 두피 일지 피드백 요약", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                if (recordsWithMemos.isEmpty()) {
                    Text(
                        text = "아직 요약할 만한 두피 일지 기록이 비어있습니다. 일지 기록 시 작성하는 커스텀 메모들이 이 공간에 피드로 모아집니다.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    recordsWithMemos.take(10).forEach { rec ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(rec.date, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = rec.memo,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.LightGray.copy(alpha = 0.5f)))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// 7. SETTINGS SCREEN (설정 화면)
// ==========================================
@Composable
fun SettingsScreen(viewModel: ScalpViewModel) {
    val context = LocalContext.current
    
    val notifications by viewModel.notificationsEnabled.collectAsState()
    val defaultAngle by viewModel.defaultAngleType.collectAsState()
    
    // Security PIN Lock Config states
    val isLocked by viewModel.isAppLocked.collectAsState()
    var appLockOn by remember { mutableStateOf(false) }

    // Check sharedPrefs to set initial switch helper
    val sharedPrefs = remember { context.getSharedPreferences("scalp_care_prefs", Context.MODE_PRIVATE) }
    LaunchedEffect(Unit) {
        val currentPin = sharedPrefs.getString("app_pin", "") ?: ""
        appLockOn = currentPin.isNotEmpty()
    }

    var showPinDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "앱 세부 구성 및 설정",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // General Notifications Setting Toggle
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("정밀 사진 촬영 주기 알림", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("매달 주기적인 두피 촬영 권장 알림을 전송합니다.", fontSize = 13.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = notifications,
                        onCheckedChange = { viewModel.toggleNotifications(it) }
                    )
                }
            }
        }

        // Camera Preferred Angle Setting Selector
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                Text("기본 촬영 선호 각도", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("사진 로그 탭 진입 시 가장 첫 순서로 자동 세팅되는 각도입니다.", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))
                
                val anglesList = listOf("정면", "위쪽", "양쪽 측면", "정수리")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    anglesList.forEach { angle ->
                        val isPreferred = defaultAngle == angle
                        Button(
                            onClick = { viewModel.setDefaultAngle(angle) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPreferred) MaterialTheme.colorScheme.primary 
                                                 else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                contentColor = if (isPreferred) MaterialTheme.colorScheme.onPrimary 
                                               else MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text(angle, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // App Lock PIN Code toggle settings
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("개인정보 보호 암호 잠금 (PIN)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("두피 사진과 로그 데이터의 타인 노출을 방지하기 위해 진입 차단 암호를 활성화합니다.", fontSize = 13.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = appLockOn,
                        onCheckedChange = { enable ->
                            if (enable) {
                                showPinDialog = true
                            } else {
                                viewModel.removePin()
                                appLockOn = false
                            }
                        }
                    )
                }
            }
        }

        // Database Reset wipe configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                Text("데이터 초기화", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("기재하신 두피 상태, 루틴, 촬영하신 내부 폴더 보관 사진까지 완벽히 소거하여 새 기기 상태로 초기화합니다. 작업은 영구히 돌릴 수 없습니다.", fontSize = 13.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedButton(
                    onClick = {
                        viewModel.clearDatabaseData()
                        appLockOn = false
                        viewModel.removePin()
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("모든 기록 영구히 초기화")
                }
            }
        }

        // Legal Medical Disclaimer Card compliance
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "안내", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("의료 고지 및 의학 책임 책임 한계", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "본 '두피케어기록' 앱은 어떠한 형태의 의료적 진단, 소견 제공, 또는 탈모 치료 방향을 추천/강요하지 않습니다.\n" +
                            "사용자가 단지 로컬 장치 내에서 두피 사진과 생활 패턴을 일정하게 저널링함으로써 장기적 궤적 변화를 스스로 모니터링하기 위해 만들어진 유틸리티 보조 도구입니다.\n" +
                            "만약 심각한 비듬, 염증, 지속적인 탈모 현상이 의심되시는 경우 반드시 전문 피부과 병원에 내원하여 올바른 안락 진단과 처방을 받아내실 것을 권고합니다.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    textAlign = TextAlign.Justify
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPinDialog = false
                appLockOn = false
            },
            title = { Text("4자리 PIN 암호 설정") },
            text = {
                var newPinText by remember { mutableStateOf("") }
                Column {
                    Text("안전하게 사용할 4자리 숫자 비밀번호를 입력해주세요.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPinText,
                        onValueChange = { 
                            if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                newPinText = it
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("비밀번호 4자리") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        enabled = newPinText.length == 4,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.setupPin(newPinText)
                            appLockOn = true
                            showPinDialog = false
                        }
                    ) {
                        Text("암호 활성화 확인")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPinDialog = false
                        appLockOn = false
                    }
                ) {
                    Text("취소")
                }
            }
        )
    }
}
