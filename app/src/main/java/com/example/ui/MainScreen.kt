package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.ApkStage
import com.example.crypto.CryptoAction
import com.example.crypto.ProcessResult
import com.example.storage.FileSaver
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberBlueLight
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.LuxuryGoldLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var showHelpDialog by remember { mutableStateOf(false) }
    var previewResult by remember { mutableStateOf<ProcessResult?>(null) }

    // State from ViewModel
    val selectedFile by viewModel.selectedFile.collectAsState()
    val multipleFiles by viewModel.multipleFiles.collectAsState()
    val folderUri by viewModel.folderUri.collectAsState()
    val folderName by viewModel.folderName.collectAsState()
    val scannedFolderFiles by viewModel.scannedFolderFiles.collectAsState()
    val isScanningFolder by viewModel.isScanningFolder.collectAsState()

    val smartExtension by viewModel.smartExtension.collectAsState()
    val processingState by viewModel.processingState.collectAsState()

    val apkStage by viewModel.apkStage.collectAsState()
    val apkProgress by viewModel.apkProgress.collectAsState()
    val apkStatusDetails by viewModel.apkStatusDetails.collectAsState()
    val isConvertingApk by viewModel.isConvertingApk.collectAsState()
    val apkResult by viewModel.apkResult.collectAsState()
    val apkError by viewModel.apkError.collectAsState()

    val animatedApkProgress by animateFloatAsState(
        targetValue = apkProgress,
        label = "apk_progress_animation"
    )

    // File pickers
    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.clearFolder()
            viewModel.clearMultipleFiles()
            viewModel.setPickerMode(PickerMode.SINGLE)
            viewModel.onSingleFilePicked(uri, context)
        }
    }

    val multiplePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.clearSelectedFile()
            viewModel.clearFolder()
            viewModel.setPickerMode(PickerMode.MULTIPLE)
            viewModel.onMultipleFilesPicked(uris, context)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.clearSelectedFile()
            viewModel.clearMultipleFiles()
            viewModel.setPickerMode(PickerMode.FOLDER)
            viewModel.onFolderPicked(uri, context)
        }
    }

    // Generic Export Launcher
    var exportPayload by remember { mutableStateOf<Pair<String, ByteArray>?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null && exportPayload != null) {
            try {
                FileSaver.writeToUri(context, uri, exportPayload!!.second)
                Toast.makeText(context, "تم تصدير ${exportPayload!!.first} بنجاح ✅", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "فشل التصدير: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val hasSelection = (selectedFile != null || multipleFiles.isNotEmpty() || folderUri != null)
    val isBusy = (processingState is UiProcessingState.Processing || isConvertingApk)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(CyberBlue, EmeraldGreen))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "محمد | أداة التشفير الفاخرة",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "تشفير • فك تشفير • حزم APK فوري",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = EmeraldGreenLight
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier.testTag("help_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "معلومات عن الأداة",
                                tint = CyberBlueLight
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // -------------------------------------------------------------
                // 1. قسم اختيار الموارد (فاخر، نظيف، وبدون اختيارات معقدة)
                // -------------------------------------------------------------
                if (!hasSelection) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "اختر الملفات أو المجلد لبدء المعالجة:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LuxuryQuickPickButton(
                                    title = "ملف / ZIP",
                                    subtitle = ".lua, .457, .ffsz",
                                    icon = Icons.Default.Description,
                                    accentColor = CyberBlueLight,
                                    modifier = Modifier.weight(1f),
                                    onClick = { singlePickerLauncher.launch("*/*") }
                                )

                                LuxuryQuickPickButton(
                                    title = "ملفات متعددة",
                                    subtitle = "اختيار عدة ملفات",
                                    icon = Icons.Default.Source,
                                    accentColor = EmeraldGreenLight,
                                    modifier = Modifier.weight(1f),
                                    onClick = { multiplePickerLauncher.launch(arrayOf("*/*")) }
                                )

                                LuxuryQuickPickButton(
                                    title = "مجلد كامل",
                                    subtitle = "فحص مجلد اللعبة",
                                    icon = Icons.Default.FolderOpen,
                                    accentColor = LuxuryGoldLight,
                                    modifier = Modifier.weight(1f),
                                    onClick = { folderPickerLauncher.launch(null) }
                                )
                            }
                        }
                    }
                } else {
                    // Selected Resource Card (عرض العنصر المحدد بشكل فخم ومختصر)
                    LuxurySelectedCard(
                        selectedFile = selectedFile,
                        multipleFiles = multipleFiles,
                        folderName = folderName,
                        scannedFiles = scannedFolderFiles,
                        isScanning = isScanningFolder,
                        onClear = {
                            viewModel.clearSelectedFile()
                            viewModel.clearMultipleFiles()
                            viewModel.clearFolder()
                        },
                        onChange = {
                            if (folderUri != null) folderPickerLauncher.launch(null)
                            else if (multipleFiles.isNotEmpty()) multiplePickerLauncher.launch(arrayOf("*/*"))
                            else singlePickerLauncher.launch("*/*")
                        },
                        onToggleFile = { docId -> viewModel.toggleFileSelection(docId) }
                    )
                }

                // -------------------------------------------------------------
                // 2. إعداد ذكي ومبسط (فقط تحويل الامتداد تلقائياً كما طلب المستخدم)
                // -------------------------------------------------------------
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تحويل الامتداد تلقائيًا (.lua ⇋ .457 / .ffsz)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "التعرف الذكي على صيغ الملفات وتغيير الامتداد تلقائياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = smartExtension,
                            onCheckedChange = { viewModel.setSmartExtension(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldGreenLight,
                                checkedTrackColor = EmeraldGreen.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // -------------------------------------------------------------
                // 3. أزرار العمليات الرئيسية الفاخرة (Hero Actions)
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // فك التشفير
                    Button(
                        onClick = {
                            if (folderUri != null) viewModel.processProjectFolder(CryptoAction.DECRYPT, context)
                            else if (multipleFiles.isNotEmpty()) viewModel.processMultipleFiles(CryptoAction.DECRYPT, context)
                            else viewModel.processSingleFile(CryptoAction.DECRYPT, context)
                        },
                        enabled = hasSelection && !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.White,
                            disabledContainerColor = EmeraldGreen.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("decrypt_button")
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فك التشفير ✅",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // التشفير
                    Button(
                        onClick = {
                            if (folderUri != null) viewModel.processProjectFolder(CryptoAction.ENCRYPT, context)
                            else if (multipleFiles.isNotEmpty()) viewModel.processMultipleFiles(CryptoAction.ENCRYPT, context)
                            else viewModel.processSingleFile(CryptoAction.ENCRYPT, context)
                        },
                        enabled = hasSelection && !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberBlue,
                            contentColor = Color.White,
                            disabledContainerColor = CyberBlue.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("encrypt_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "التشفير 🔒",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // زر تحويل APK تمت إزالته بناءً على طلب المستخدم.

                // -------------------------------------------------------------
                // 4. شريط التقدم ومراحل البناء (Compressing, Signing, Finalizing)
                // -------------------------------------------------------------
                AnimatedVisibility(visible = isConvertingApk || apkStage != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, if (apkStage == ApkStage.COMPLETED) EmeraldGreen else LuxuryGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isConvertingApk) {
                                        CircularProgressIndicator(
                                            color = LuxuryGold,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldGreenLight,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Text(
                                        text = apkStage?.titleAr ?: "جارٍ المعالجة",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (apkStage == ApkStage.COMPLETED) EmeraldGreenLight else TextPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (apkStage) {
                                        ApkStage.COMPRESSING -> CyberBlue.copy(alpha = 0.25f)
                                        ApkStage.SIGNING -> EmeraldGreen.copy(alpha = 0.25f)
                                        ApkStage.FINALIZING -> LuxuryGold.copy(alpha = 0.25f)
                                        ApkStage.COMPLETED -> EmeraldGreen.copy(alpha = 0.35f)
                                        else -> DarkSurfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = apkStage?.stageTag ?: "Stage",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        ),
                                        color = when (apkStage) {
                                            ApkStage.COMPLETED -> EmeraldGreenLight
                                            ApkStage.SIGNING -> EmeraldGreenLight
                                            ApkStage.COMPRESSING -> CyberBlueLight
                                            else -> LuxuryGoldLight
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { animatedApkProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = if (apkStage == ApkStage.COMPLETED) EmeraldGreenLight else LuxuryGold,
                                trackColor = DarkSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = apkStatusDetails.ifEmpty { apkStage?.descriptionAr ?: "" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${(animatedApkProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = LuxuryGoldLight
                                )
                            }
                        }
                    }
                }

                // شريط التقدم عند معالجة الملفات العادية
                AnimatedVisibility(visible = processingState is UiProcessingState.Processing) {
                    val pState = processingState as? UiProcessingState.Processing
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, CyberBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = CyberBlueLight, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "جارٍ المعالجة: ${pState?.currentFile ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // -------------------------------------------------------------
                // 5. بطاقة النتيجة الفاخرة للـ APK أو الملفات
                // -------------------------------------------------------------
                apkResult?.let { res ->
                    LuxuryApkResultCard(
                        result = res,
                        onSave = {
                            try {
                                val path = FileSaver.saveToDownloads(context, res.fileName, res.apkData, false)
                                Toast.makeText(context, "تم الحفظ بنجاح: $path ✅", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "فشل الحفظ: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        onShare = {
                            val shareIntent = FileSaver.createShareIntent(context, res.fileName, res.apkData)
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة APK"))
                        },
                        onExport = {
                            exportPayload = res.fileName to res.apkData
                            exportLauncher.launch(res.fileName)
                        }
                    )
                }

                // نتيجة معالجة الملفات العادية
                AnimatedVisibility(visible = processingState is UiProcessingState.Success || processingState is UiProcessingState.BatchSuccess) {
                    when (val state = processingState) {
                        is UiProcessingState.Success -> {
                            val res = state.result
                            LuxuryFileResultCard(
                                result = res,
                                savedPath = state.savedPath,
                                onPreview = { previewResult = res },
                                onShare = {
                                    val shareIntent = FileSaver.createShareIntent(context, res.suggestedFileName, res.data)
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الملف"))
                                },
                                onExport = {
                                    exportPayload = res.suggestedFileName to res.data
                                    exportLauncher.launch(res.suggestedFileName)
                                }
                            )
                        }
                        is UiProcessingState.BatchSuccess -> {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, EmeraldGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreenLight)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "تمت المعالجة بنجاح (${state.processedCount} ملف) ✅",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldGreenLight
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "المسار: ${state.savedFolderPath ?: "مجلد Download"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                    if (state.zipResultData != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    val shareIntent = FileSaver.createShareIntent(context, state.zipFileName, state.zipResultData)
                                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة ZIP"))
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("مشاركة ZIP", fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    exportPayload = state.zipFileName to state.zipResultData
                                                    exportLauncher.launch(state.zipFileName)
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تصدير ZIP", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }

                Button(
                    onClick = {
                        val telegramUrl = "https://t.me/mm_ipp"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF229ED9), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("✈️ للتواصل مع المطور Telegram", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showHelpDialog) {
            HelpInfoDialog(onDismiss = { showHelpDialog = false })
        }

        previewResult?.let { res ->
            FilePreviewDialog(result = res, onDismiss = { previewResult = null })
        }
    }
}

// -------------------------------------------------------------
// مكونات واجهة المستخدم الفاخرة (Luxury UI Components)
// -------------------------------------------------------------

@Composable
fun LuxuryQuickPickButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceVariant,
        border = BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LuxurySelectedCard(
    selectedFile: SelectedFileInfo?,
    multipleFiles: List<SelectedFileInfo>,
    folderName: String,
    scannedFiles: List<com.example.storage.ScannedFileItem>,
    isScanning: Boolean,
    onClear: () -> Unit,
    onChange: () -> Unit,
    onToggleFile: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val title = when {
        selectedFile != null -> selectedFile.name
        multipleFiles.isNotEmpty() -> "${multipleFiles.size} ملفات برمجية محددة"
        folderName.isNotEmpty() -> "مجلد المشروع: $folderName"
        else -> "عنصر محدد"
    }

    val subtitle = when {
        selectedFile != null -> "الحجم: ${formatBytes(selectedFile.size)}"
        multipleFiles.isNotEmpty() -> "الإجمالي: ${formatBytes(multipleFiles.sumOf { it.size })}"
        folderName.isNotEmpty() -> {
            val count = scannedFiles.count { it.isSelected }
            "تم العثور على ${scannedFiles.size} ملف (المحدد: $count)"
        }
        else -> ""
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, CyberBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldGreen.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (folderName.isNotEmpty()) Icons.Default.FolderOpen else Icons.Default.Description,
                        contentDescription = null,
                        tint = EmeraldGreenLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                TextButton(onClick = onChange) {
                    Text("تغيير", fontSize = 12.sp, color = CyberBlueLight)
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "إلغاء", tint = ErrorRed)
                }
            }

            // Folder file list dropdown
            if (scannedFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (expanded) "إخفاء قائمة ملفات المشروع" else "عرض قائمة ملفات المشروع (${scannedFiles.size} ملف)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldGreenLight
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = EmeraldGreenLight
                    )
                }

                if (expanded) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(scannedFiles, key = { it.documentId }) { item ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleFile(item.documentId) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { onToggleFile(item.documentId) },
                                        colors = CheckboxDefaults.colors(checkedColor = EmeraldGreenLight),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.relativePath,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.detectedType,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = CyberBlueLight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LuxuryApkResultCard(
    result: com.example.crypto.ApkConversionResult,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.5.dp, LuxuryGold),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LuxuryGold, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تحميل ملف الـ APK جاهز الآن! 🚀",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = LuxuryGoldLight
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = DarkCardBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Text("اسم الحزمة: ${result.fileName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            Text("الحجم: ${formatBytes(result.apkSize)}", style = MaterialTheme.typography.bodySmall, color = CyberBlueLight)
            Text("الحالة: موقعة وجاهزة للتثبيت (Signed Valid)", style = MaterialTheme.typography.bodySmall, color = EmeraldGreenLight)

            if (result.savedPath != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("المسار: ${result.savedPath}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LuxuryGold, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حفظ وتنزيل", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onExport,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصدير", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun LuxuryFileResultCard(
    result: ProcessResult,
    savedPath: String?,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, EmeraldGreen),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreenLight, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تمت العملية بنجاح ✅",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldGreenLight
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("الملف الناتج: ${result.suggestedFileName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            Text("الصيغة: ${result.formatDetected} • الحجم: ${formatBytes(result.outputSize.toLong())}", style = MaterialTheme.typography.bodySmall, color = CyberBlueLight)
            if (savedPath != null) {
                Text("مسار الحفظ: $savedPath", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاينة", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة", fontSize = 12.sp)
                }
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تصدير", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HelpInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("أداة التشفير وحزم APK", fontWeight = FontWeight.Bold, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المميزات الرئيسية:", fontWeight = FontWeight.Bold, color = CyberBlueLight)
                Text(
                    text = "• اختيار ملف أو عدة ملفات أو مجلد كامل بضغطة واحدة.\n" +
                            "• فك وتشفير صيغ اللعبة 0x457 و FFSZ و XXTEA تلقائياً.\n" +
                            "• زر حزم وتوليد APK مباشر مع الضغط والتوقيع الرقمي.\n" +
                            "• حفظ تلقائي للمخرجات في مجلد التنزيلات بالجهاز.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("حسناً", color = CyberBlueLight) }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun FilePreviewDialog(
    result: ProcessResult,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val isText = remember(result) {
        val sampleLen = minOf(result.data.size, 512)
        var textChars = 0
        for (i in 0 until sampleLen) {
            val b = result.data[i].toInt() and 0xFF
            if (b in 9..13 || (b in 32..126) || b >= 128) textChars++
        }
        textChars.toFloat() / maxOf(sampleLen, 1) > 0.85f
    }

    val displayContent = remember(result, isText) {
        if (isText) {
            try {
                String(result.data.copyOf(minOf(result.data.size, 100_000)), Charsets.UTF_8)
            } catch (_: Exception) {
                String(result.data.copyOf(minOf(result.data.size, 100_000)), Charsets.ISO_8859_1)
            }
        } else {
            val sb = StringBuilder()
            val len = minOf(result.data.size, 512)
            for (i in 0 until len step 16) {
                sb.append("%04X  ".format(i))
                val lineLen = minOf(16, len - i)
                for (j in 0 until 16) {
                    if (j < lineLen) {
                        sb.append("%02X ".format(result.data[i + j]))
                    } else {
                        sb.append("   ")
                    }
                }
                sb.append(" ")
                for (j in 0 until lineLen) {
                    val b = result.data[i + j].toInt() and 0xFF
                    sb.append(if (b in 32..126) b.toChar() else '.')
                }
                sb.append("\n")
            }
            if (result.data.size > 512) sb.append("... [إجمالي الحجم: ${formatBytes(result.data.size.toLong())}]")
            sb.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "معاينة: ${result.suggestedFileName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(displayContent))
                    Toast.makeText(context, "تم النسخ 📋", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = CyberBlueLight)
                }
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(10.dp)) {
                    item {
                        Text(
                            text = displayContent,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق", color = CyberBlueLight) }
        },
        containerColor = DarkSurface
    )
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes بايت"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f ك.ب".format(kb)
    val mb = kb / 1024.0
    return "%.2f م.ب".format(mb)
}
