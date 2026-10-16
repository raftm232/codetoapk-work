package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoAction
import com.example.crypto.CryptoFormat
import com.example.crypto.FfsProcessor
import com.example.crypto.ProcessResult
import com.example.crypto.XxteaB64
import com.example.storage.FileSaver
import com.example.storage.FolderScanner
import com.example.storage.ScannedFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class PickerMode(val titleAr: String) {
    SINGLE("ملف واحد / ZIP"),
    MULTIPLE("ملفات برمجية متعددة"),
    FOLDER("مجلد مشروع كامل")
}

data class SelectedFileInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SelectedFileInfo
        return uri == other.uri && name == other.name && size == other.size
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }
}

sealed class UiProcessingState {
    object Idle : UiProcessingState()
    data class Processing(val progress: Float, val currentFile: String) : UiProcessingState()
    data class Success(val result: ProcessResult, val savedPath: String?) : UiProcessingState()
    data class BatchSuccess(
        val totalCount: Int,
        val processedCount: Int,
        val savedFolderPath: String?,
        val zipResultData: ByteArray?,
        val zipFileName: String
    ) : UiProcessingState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as BatchSuccess
            return totalCount == other.totalCount &&
                    processedCount == other.processedCount &&
                    savedFolderPath == other.savedFolderPath &&
                    zipFileName == other.zipFileName
        }

        override fun hashCode(): Int {
            var result = totalCount
            result = 31 * result + processedCount
            result = 31 * result + (savedFolderPath?.hashCode() ?: 0)
            result = 31 * result + zipFileName.hashCode()
            return result
        }
    }
    data class Error(val message: String) : UiProcessingState()
}

data class HistoryItem(
    val id: Long = System.currentTimeMillis(),
    val fileName: String,
    val action: CryptoAction,
    val format: String,
    val originalSize: Int,
    val outputSize: Int,
    val savedPath: String?,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _pickerMode = MutableStateFlow(PickerMode.SINGLE)
    val pickerMode: StateFlow<PickerMode> = _pickerMode.asStateFlow()

    // Single file
    private val _selectedFile = MutableStateFlow<SelectedFileInfo?>(null)
    val selectedFile: StateFlow<SelectedFileInfo?> = _selectedFile.asStateFlow()

    // Multiple files
    private val _multipleFiles = MutableStateFlow<List<SelectedFileInfo>>(emptyList())
    val multipleFiles: StateFlow<List<SelectedFileInfo>> = _multipleFiles.asStateFlow()

    // Project folder tree
    private val _folderUri = MutableStateFlow<Uri?>(null)
    val folderUri: StateFlow<Uri?> = _folderUri.asStateFlow()

    private val _folderName = MutableStateFlow("")
    val folderName: StateFlow<String> = _folderName.asStateFlow()

    private val _scannedFolderFiles = MutableStateFlow<List<ScannedFileItem>>(emptyList())
    val scannedFolderFiles: StateFlow<List<ScannedFileItem>> = _scannedFolderFiles.asStateFlow()

    private val _isScanningFolder = MutableStateFlow(false)
    val isScanningFolder: StateFlow<Boolean> = _isScanningFolder.asStateFlow()

    private val _scanProgressText = MutableStateFlow("")
    val scanProgressText: StateFlow<String> = _scanProgressText.asStateFlow()

    // Configurations
    private val _selectedFormat = MutableStateFlow(CryptoFormat.FFSZ)
    val selectedFormat: StateFlow<CryptoFormat> = _selectedFormat.asStateFlow()

    private val _customKey = MutableStateFlow("")
    val customKey: StateFlow<String> = _customKey.asStateFlow()

    private val _smartExtension = MutableStateFlow(true)
    val smartExtension: StateFlow<Boolean> = _smartExtension.asStateFlow()

    private val _autoSave = MutableStateFlow(true)
    val autoSave: StateFlow<Boolean> = _autoSave.asStateFlow()

    private val _processingState = MutableStateFlow<UiProcessingState>(UiProcessingState.Idle)
    val processingState: StateFlow<UiProcessingState> = _processingState.asStateFlow()

    // APK Conversion State
    private val _apkStage = MutableStateFlow<com.example.crypto.ApkStage?>(null)
    val apkStage: StateFlow<com.example.crypto.ApkStage?> = _apkStage.asStateFlow()

    private val _apkProgress = MutableStateFlow(0f)
    val apkProgress: StateFlow<Float> = _apkProgress.asStateFlow()

    private val _apkStatusDetails = MutableStateFlow("")
    val apkStatusDetails: StateFlow<String> = _apkStatusDetails.asStateFlow()

    private val _isConvertingApk = MutableStateFlow(false)
    val isConvertingApk: StateFlow<Boolean> = _isConvertingApk.asStateFlow()

    private val _apkResult = MutableStateFlow<com.example.crypto.ApkConversionResult?>(null)
    val apkResult: StateFlow<com.example.crypto.ApkConversionResult?> = _apkResult.asStateFlow()

    private val _apkError = MutableStateFlow<String?>(null)
    val apkError: StateFlow<String?> = _apkError.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history.asStateFlow()

    // Text Tab state
    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private val _textOutput = MutableStateFlow("")
    val textOutput: StateFlow<String> = _textOutput.asStateFlow()

    private val _textStatus = MutableStateFlow<String?>(null)
    val textStatus: StateFlow<String?> = _textStatus.asStateFlow()

    private val _textFormat = MutableStateFlow(CryptoFormat.B64)
    val textFormat: StateFlow<CryptoFormat> = _textFormat.asStateFlow()

    fun resetApkConversion() {
        _apkStage.value = null
        _apkProgress.value = 0f
        _apkStatusDetails.value = ""
        _isConvertingApk.value = false
        _apkResult.value = null
        _apkError.value = null
    }

    fun startApkConversion(
        context: Context,
        customApkName: String = "FarmSeaside_Modded.apk",
        encryptBeforePackaging: Boolean = true
    ) {
        if (_isConvertingApk.value) return

        _isConvertingApk.value = true
        _apkError.value = null
        _apkResult.value = null
        _apkStage.value = com.example.crypto.ApkStage.PREPARING
        _apkProgress.value = 0.05f
        _apkStatusDetails.value = "جارٍ جمع ملفات المشروع..."

        viewModelScope.launch {
            try {
                // Collect files from current active mode
                val filesToPack = mutableListOf<Pair<String, ByteArray>>()

                when (_pickerMode.value) {
                    PickerMode.FOLDER -> {
                        val selected = _scannedFolderFiles.value.filter { it.isSelected }
                        withContext(Dispatchers.IO) {
                            for (item in selected) {
                                try {
                                    val bytes = context.contentResolver.openInputStream(item.uri)?.use { it.readBytes() }
                                    if (bytes != null) filesToPack.add(item.relativePath to bytes)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    PickerMode.MULTIPLE -> {
                        for (item in _multipleFiles.value) {
                            filesToPack.add("src/${item.name}" to item.bytes)
                        }
                    }
                    PickerMode.SINGLE -> {
                        val single = _selectedFile.value
                        if (single != null) {
                            filesToPack.add("src/${single.name}" to single.bytes)
                        }
                    }
                }

                // If empty, pack a sample set of Lua scripts
                if (filesToPack.isEmpty()) {
                    filesToPack.add("src/main.lua" to "function main()\n    print('Farm Seaside Mod Loaded')\nend".toByteArray(Charsets.UTF_8))
                    filesToPack.add("src/config.json" to "{\"version\":\"1.0.0\",\"mod\":true}".toByteArray(Charsets.UTF_8))
                }

                val format = _selectedFormat.value
                val customKeyVal = _customKey.value.trim().ifEmpty { null }

                val result = withContext(Dispatchers.Default) {
                    com.example.crypto.ApkConverter.convertToApk(
                        context = context,
                        files = filesToPack,
                        apkName = customApkName,
                        format = format,
                        customKey = customKeyVal,
                        encryptBeforePackaging = encryptBeforePackaging,
                        onStageChanged = { stage, progress, details ->
                            _apkStage.value = stage
                            _apkProgress.value = progress
                            _apkStatusDetails.value = details
                        }
                    )
                }

                _apkResult.value = result

                // Add to history
                val historyItem = HistoryItem(
                    fileName = result.fileName,
                    action = CryptoAction.ENCRYPT,
                    format = "حزمة APK مجمعة",
                    originalSize = filesToPack.sumOf { it.second.size },
                    outputSize = result.apkSize.toInt(),
                    savedPath = result.savedPath
                )
                _history.value = listOf(historyItem) + _history.value

            } catch (e: Exception) {
                _apkError.value = "فشلت عملية تحويل الـ APK: ${e.message}"
            } finally {
                _isConvertingApk.value = false
            }
        }
    }

    fun setPickerMode(mode: PickerMode) {
        _pickerMode.value = mode
        _processingState.value = UiProcessingState.Idle
    }

    fun setSelectedFormat(format: CryptoFormat) {
        _selectedFormat.value = format
    }

    fun setCustomKey(key: String) {
        _customKey.value = key
    }

    fun setSmartExtension(enabled: Boolean) {
        _smartExtension.value = enabled
    }

    fun setAutoSave(enabled: Boolean) {
        _autoSave.value = enabled
    }

    fun setTextInput(text: String) {
        _textInput.value = text
        _textStatus.value = null
    }

    fun setTextFormat(format: CryptoFormat) {
        _textFormat.value = format
    }

    fun clearSelectedFile() {
        _selectedFile.value = null
        _processingState.value = UiProcessingState.Idle
    }

    fun removeMultipleFile(index: Int) {
        val current = _multipleFiles.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _multipleFiles.value = current
        }
    }

    fun clearMultipleFiles() {
        _multipleFiles.value = emptyList()
        _processingState.value = UiProcessingState.Idle
    }

    fun clearFolder() {
        _folderUri.value = null
        _folderName.value = ""
        _scannedFolderFiles.value = emptyList()
        _processingState.value = UiProcessingState.Idle
    }

    fun toggleFileSelection(documentId: String) {
        _scannedFolderFiles.value = _scannedFolderFiles.value.map { item ->
            if (item.documentId == documentId) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
    }

    fun selectAllFolderFiles(select: Boolean) {
        _scannedFolderFiles.value = _scannedFolderFiles.value.map {
            it.copy(isSelected = select)
        }
    }

    fun onSingleFilePicked(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val (name, size, bytes) = withContext(Dispatchers.IO) {
                    readFileFromUri(context, uri)
                }
                _selectedFile.value = SelectedFileInfo(uri, name, size, bytes)
                _processingState.value = UiProcessingState.Idle
            } catch (e: Exception) {
                _processingState.value = UiProcessingState.Error("فشل تحميل الملف: ${e.message}")
            }
        }
    }

    fun onMultipleFilesPicked(uris: List<Uri>, context: Context) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            try {
                val loadedList = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        try {
                            val (name, size, bytes) = readFileFromUri(context, uri)
                            SelectedFileInfo(uri, name, size, bytes)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
                _multipleFiles.value = loadedList
                _processingState.value = UiProcessingState.Idle
            } catch (e: Exception) {
                _processingState.value = UiProcessingState.Error("فشل تحميل الملفات: ${e.message}")
            }
        }
    }

    fun onFolderPicked(treeUri: Uri, context: Context) {
        _folderUri.value = treeUri
        _isScanningFolder.value = true
        _processingState.value = UiProcessingState.Idle

        viewModelScope.launch {
            try {
                // Try taking persistable permission
                try {
                    val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(treeUri, flags)
                } catch (_: Exception) {}

                val resolvedName = withContext(Dispatchers.IO) {
                    var fName = "Project_Folder"
                    val docId = DocumentsContract.getTreeDocumentId(treeUri)
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    context.contentResolver.query(docUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            fName = cursor.getString(0) ?: "Project_Folder"
                        }
                    }
                    fName
                }
                _folderName.value = resolvedName

                val scanned = withContext(Dispatchers.IO) {
                    FolderScanner.scanTree(context, treeUri) { count, name ->
                        _scanProgressText.value = "تم العثور على $count ملف: $name"
                    }
                }
                _scannedFolderFiles.value = scanned
            } catch (e: Exception) {
                _processingState.value = UiProcessingState.Error("فشل فحص المجلد: ${e.message}")
            } finally {
                _isScanningFolder.value = false
                _scanProgressText.value = ""
            }
        }
    }

    private fun readFileFromUri(context: Context, uri: Uri): Triple<String, Long, ByteArray> {
        var fileName = "file.bin"
        var fileSize = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "file.bin"
                if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
            }
        }

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("تعذر قراءة الملف المختار")
        val data = inputStream.use { it.readBytes() }
        if (fileSize <= 0) fileSize = data.size.toLong()

        return Triple(fileName, fileSize, data)
    }

    fun processSingleFile(action: CryptoAction, context: Context) {
        val fileInfo = _selectedFile.value
        if (fileInfo == null) {
            _processingState.value = UiProcessingState.Error("يرجى اختيار ملف أولاً")
            return
        }

        _processingState.value = UiProcessingState.Processing(0f, fileInfo.name)

        viewModelScope.launch {
            try {
                val format = _selectedFormat.value
                val customKeyVal = _customKey.value.trim().ifEmpty { null }
                val smartExt = _smartExtension.value
                val shouldAutoSave = _autoSave.value

                val result = withContext(Dispatchers.IO) {
                    FfsProcessor.process(
                        input = fileInfo.bytes,
                        originalName = fileInfo.name,
                        action = action,
                        format = format,
                        customKey = customKeyVal,
                        smartExtension = smartExt,
                        onProgress = { current, total, entryName ->
                            val progress = if (total > 0) current.toFloat() / total else 0f
                            _processingState.value = UiProcessingState.Processing(progress, entryName)
                        }
                    )
                }

                var savedPath: String? = null
                if (shouldAutoSave) {
                    savedPath = withContext(Dispatchers.IO) {
                        try {
                            FileSaver.saveToDownloads(
                                context = context,
                                fileName = result.suggestedFileName,
                                data = result.data,
                                isDecrypted = (action == CryptoAction.DECRYPT)
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                _processingState.value = UiProcessingState.Success(result, savedPath)

                // Add to history
                val historyItem = HistoryItem(
                    fileName = result.suggestedFileName,
                    action = action,
                    format = result.formatDetected,
                    originalSize = result.originalSize,
                    outputSize = result.outputSize,
                    savedPath = savedPath
                )
                _history.value = listOf(historyItem) + _history.value
            } catch (e: Exception) {
                _processingState.value = UiProcessingState.Error(e.message ?: "حدث خطأ غير متوقع أثناء المعالجة")
            }
        }
    }

    fun processMultipleFiles(action: CryptoAction, context: Context) {
        val files = _multipleFiles.value
        if (files.isEmpty()) {
            _processingState.value = UiProcessingState.Error("لم يتم اختيار أي ملفات")
            return
        }

        viewModelScope.launch {
            try {
                val format = _selectedFormat.value
                val customKeyVal = _customKey.value.trim().ifEmpty { null }
                val smartExt = _smartExtension.value
                val shouldAutoSave = _autoSave.value

                var successCount = 0
                val zipOut = ByteArrayOutputStream()

                withContext(Dispatchers.IO) {
                    ZipOutputStream(zipOut).use { zos ->
                        for ((index, fileInfo) in files.withIndex()) {
                            val progress = (index + 1).toFloat() / files.size
                            _processingState.value = UiProcessingState.Processing(progress, fileInfo.name)

                            try {
                                val processed = FfsProcessor.process(
                                    input = fileInfo.bytes,
                                    originalName = fileInfo.name,
                                    action = action,
                                    format = format,
                                    customKey = customKeyVal,
                                    smartExtension = smartExt
                                )

                                if (shouldAutoSave) {
                                    try {
                                        FileSaver.saveToDownloads(
                                            context = context,
                                            fileName = processed.suggestedFileName,
                                            data = processed.data,
                                            isDecrypted = (action == CryptoAction.DECRYPT)
                                        )
                                    } catch (_: Exception) {}
                                }

                                zos.putNextEntry(ZipEntry(processed.suggestedFileName))
                                zos.write(processed.data)
                                zos.closeEntry()
                                successCount++
                            } catch (_: Exception) {}
                        }
                    }
                }

                val zipData = zipOut.toByteArray()
                val zipName = "Batch_Processed_${System.currentTimeMillis()}.zip"
                var savedPath: String? = null
                if (shouldAutoSave && zipData.isNotEmpty()) {
                    savedPath = withContext(Dispatchers.IO) {
                        try {
                            FileSaver.saveToDownloads(context, zipName, zipData, action == CryptoAction.DECRYPT)
                        } catch (_: Exception) { null }
                    }
                }

                _processingState.value = UiProcessingState.BatchSuccess(
                    totalCount = files.size,
                    processedCount = successCount,
                    savedFolderPath = savedPath ?: "مجلد Download/${if (action == CryptoAction.DECRYPT) FileSaver.FOLDER_DECRYPTED else FileSaver.FOLDER_ENCRYPTED}",
                    zipResultData = zipData,
                    zipFileName = zipName
                )
            } catch (e: Exception) {
                _processingState.value = UiProcessingState.Error(e.message ?: "فشلت معالجة الملفات")
            }
        }
    }

    fun processProjectFolder(action: CryptoAction, context: Context) {
        val allItems = _scannedFolderFiles.value
        val selectedItems = allItems.filter { it.isSelected }
        if (selectedItems.isEmpty()) {
            _processingState.value = UiProcessingState.Error("الرجاء تحديد ملف واحد على الأقل داخل المجلد")
            return
        }

        viewModelScope.launch {
            try {
                val format = _selectedFormat.value
                val customKeyVal = _customKey.value.trim().ifEmpty { null }
                val smartExt = _smartExtension.value
                val shouldAutoSave = _autoSave.value

                var successCount = 0
                val zipOut = ByteArrayOutputStream()

                withContext(Dispatchers.IO) {
                    ZipOutputStream(zipOut).use { zos ->
                        for ((index, item) in selectedItems.withIndex()) {
                            val progress = (index + 1).toFloat() / selectedItems.size
                            _processingState.value = UiProcessingState.Processing(progress, item.relativePath)

                            try {
                                val bytes = context.contentResolver.openInputStream(item.uri)?.use {
                                    it.readBytes()
                                } ?: continue

                                val processed = FfsProcessor.process(
                                    input = bytes,
                                    originalName = item.name,
                                    action = action,
                                    format = format,
                                    customKey = customKeyVal,
                                    smartExtension = smartExt
                                )

                                val entryPath = if (item.relativePath.contains('/')) {
                                    val parent = item.relativePath.substringBeforeLast('/')
                                    "$parent/${processed.suggestedFileName}"
                                } else {
                                    processed.suggestedFileName
                                }

                                zos.putNextEntry(ZipEntry(entryPath))
                                zos.write(processed.data)
                                zos.closeEntry()
                                successCount++
                            } catch (_: Exception) {}
                        }
                    }
                }

                val zipData = zipOut.toByteArray()
                val folderBaseName = _folderName.value.ifEmpty { "Project" }
                val zipName = "${folderBaseName}_${if (action == CryptoAction.DECRYPT) "Decrypted" else "Encrypted"}.zip"

                var savedPath: String? = null
                if (shouldAutoSave && zipData.isNotEmpty()) {
                    savedPath = withContext(Dispatchers.IO) {
                        try {
                            FileSaver.saveToDownloads(context, zipName, zipData, action == CryptoAction.DECRYPT)
                        } catch (_: Exception) { null }
                    }
                }

                _processingState.value = UiProcessingState.BatchSuccess(
                    totalCount = selectedItems.size,
                    processedCount = successCount,
                    savedFolderPath = savedPath ?: "Download/${if (action == CryptoAction.DECRYPT) FileSaver.FOLDER_DECRYPTED else FileSaver.FOLDER_ENCRYPTED}/$zipName",
                    zipResultData = zipData,
                    zipFileName = zipName
                )
            } catch (e: Exception) {
                _processingState.value = UiProcessingState.Error(e.message ?: "فشلت معالجة المجلد")
            }
        }
    }

    fun processText(action: CryptoAction) {
        val input = _textInput.value
        if (input.isBlank()) {
            _textStatus.value = "الرجاء إدخال النص أولاً"
            return
        }

        viewModelScope.launch {
            try {
                val customKeyVal = _customKey.value.trim().ifEmpty { null }
                val format = _textFormat.value

                val result = withContext(Dispatchers.Default) {
                    if (action == CryptoAction.DECRYPT) {
                        val inputBytes = input.toByteArray(Charsets.UTF_8)
                        val (decrypted, formatName) = FfsProcessor.detectAndDecrypt(inputBytes, customKeyVal)
                        val text = try {
                            String(decrypted, Charsets.UTF_8)
                        } catch (_: Exception) {
                            String(decrypted, Charsets.ISO_8859_1)
                        }
                        Pair(text, "تم فك التشفير بنجاح ($formatName)")
                    } else {
                        val inputBytes = input.toByteArray(Charsets.UTF_8)
                        val encrypted = FfsProcessor.encrypt(inputBytes, format, customKeyVal)
                        val outStr = if (format == CryptoFormat.B64) {
                            String(encrypted, Charsets.US_ASCII)
                        } else {
                            val hex = encrypted.joinToString("") { "%02X".format(it) }
                            "HEX (${format.titleAr}):\n$hex"
                        }
                        Pair(outStr, "تم التشفير بنجاح بصيغة ${format.titleAr}")
                    }
                }

                _textOutput.value = result.first
                _textStatus.value = result.second
            } catch (e: Exception) {
                _textStatus.value = "خطأ: ${e.message}"
            }
        }
    }
}
