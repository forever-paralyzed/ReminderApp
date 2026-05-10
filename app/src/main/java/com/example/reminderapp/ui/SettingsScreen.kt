package com.example.reminderapp.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reminderapp.MainActivity
import com.example.reminderapp.R
import com.example.reminderapp.getRingtone
import com.example.reminderapp.saveRingtone
import com.example.reminderapp.ui.theme.ThemeMode
import com.example.reminderapp.ui.theme.getThemeColor
import com.example.reminderapp.ui.theme.getThemeMode
import com.example.reminderapp.ui.theme.saveThemeColor
import com.example.reminderapp.ui.theme.saveThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen() {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeColorsDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current as Activity
    var ringtoneName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    fun loadRingtoneName() {
        coroutineScope.launch {
            val title = withContext(Dispatchers.IO) {
                val uri = getRingtone(context)
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone?.getTitle(context) ?: ""
            }
            ringtoneName = title
        }
    }

    LaunchedEffect(Unit) {
        loadRingtoneName()
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                saveRingtone(context, uri)
                Toast.makeText(context, context.getString(R.string.ringtone_selected), Toast.LENGTH_SHORT).show()
                loadRingtoneName()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(id = R.string.settings), style = MaterialTheme.typography.titleLarge)
        
        // Language Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showLanguageDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Language, contentDescription = stringResource(id = R.string.language))
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(id = R.string.language), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        // Theme Mode Selection (Light, Dark, Custom)
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showThemeModeDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.BrightnessMedium, contentDescription = stringResource(id = R.string.select_theme_mode))
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(id = R.string.select_theme_mode), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        // Custom Theme Colors Selection (Only visible if custom theme is active, or always accessible)
        // Let's make it always accessible so user can configure it even if not currently selected
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showThemeColorsDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ColorLens, contentDescription = stringResource(id = R.string.select_theme_colors))
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(id = R.string.select_theme_colors), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        // Ringtone Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val currentRingtone = getRingtone(context)
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentRingtone)
                }
                ringtonePickerLauncher.launch(intent)
            }
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = stringResource(id = R.string.ringtone))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(id = R.string.select_ringtone), style = MaterialTheme.typography.bodyLarge)
                    if (ringtoneName.isNotEmpty()) {
                        Text(
                            text = ringtoneName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = {
                showLanguageDialog = false
                MainActivity.setLanguage(context, it)
                context.recreate()
            }
        )
    }
    
    if (showThemeModeDialog) {
        ThemeModeSelectorDialog(
            onDismiss = { showThemeModeDialog = false },
            onThemeModeSelected = { mode ->
                saveThemeMode(context, mode)
                showThemeModeDialog = false
                context.recreate()
            }
        )
    }

    if (showThemeColorsDialog) {
        ThemeColorsSelectorDialog(
            onDismiss = { showThemeColorsDialog = false },
            onThemeSelected = { primary, secondary, tertiary ->
                saveThemeColor(context, "primary_color", primary)
                saveThemeColor(context, "secondary_color", secondary)
                saveThemeColor(context, "tertiary_color", tertiary)
                // If user explicitly sets colors, switch mode to CUSTOM automatically
                saveThemeMode(context, ThemeMode.CUSTOM)
                showThemeColorsDialog = false
                context.recreate()
            }
        )
    }
}

@Composable
fun ThemeModeSelectorDialog(
    onDismiss: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    // This state is just for initial selection in UI if we wanted radio buttons, 
    // but here we just use simple clickable items.
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.select_theme_mode)) },
        text = {
            Column {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeModeSelected(ThemeMode.LIGHT) }
                    .padding(vertical = 12.dp)) {
                    Text(stringResource(id = R.string.theme_mode_light))
                }
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeModeSelected(ThemeMode.DARK) }
                    .padding(vertical = 12.dp)) {
                    Text(stringResource(id = R.string.theme_mode_dark))
                }
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeModeSelected(ThemeMode.CUSTOM) }
                    .padding(vertical = 12.dp)) {
                    Text(stringResource(id = R.string.theme_mode_custom))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } }
    )
}

@Composable
fun ThemeColorsSelectorDialog(
    onDismiss: () -> Unit,
    onThemeSelected: (Color, Color, Color) -> Unit
) {
    val context = LocalContext.current
    val predefinedColors = listOf(
        Color(0xFF000000), // Black
        Color(0xFFFFFFFF), // White
        Color(0xFF808080), // Gray
        Color(0xFFD3D3D3), // Light Gray
        Color(0xFFA9A9A9), // Dark Gray
        Color(0xFF6200EE), // Purple
        Color(0xFFBB86FC), // Light Purple
        Color(0xFF03DAC5), // Teal
        Color(0xFF018786), // Dark Teal
        Color(0xFFB00020), // Red
        Color(0xFFCF6679), // Light Red
        Color(0xFF4CAF50), // Green
        Color(0xFFFFC107), // Amber
        Color(0xFF2196F3), // Blue
        Color(0xFF3F51B5)  // Indigo
    )

    // Загружаем текущие сохраненные цвета, чтобы при открытии диалога они отображались правильно
    var selectedPrimary by remember { mutableStateOf(getThemeColor(context, "primary_color", predefinedColors[5])) } 
    var selectedSecondary by remember { mutableStateOf(getThemeColor(context, "secondary_color", predefinedColors[7])) } 
    var selectedTertiary by remember { mutableStateOf(getThemeColor(context, "tertiary_color", predefinedColors[12])) } 

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.select_theme_colors)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(id = R.string.primary_color), style = MaterialTheme.typography.titleMedium)
                ColorPicker(colors = predefinedColors, selectedColor = selectedPrimary) { selectedPrimary = it }

                Text(stringResource(id = R.string.secondary_color), style = MaterialTheme.typography.titleMedium)
                ColorPicker(colors = predefinedColors, selectedColor = selectedSecondary) { selectedSecondary = it }
                
                Text(stringResource(id = R.string.tertiary_color), style = MaterialTheme.typography.titleMedium)
                ColorPicker(colors = predefinedColors, selectedColor = selectedTertiary) { selectedTertiary = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onThemeSelected(selectedPrimary, selectedSecondary, selectedTertiary) }) {
                Text(stringResource(id = R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun ColorPicker(colors: List<Color>, selectedColor: Color, onColorSelected: (Color) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (color == selectedColor) 2.dp else 1.dp, 
                        color = if (color == selectedColor) MaterialTheme.colorScheme.onSurface else Color.LightGray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun LanguageSelectorDialog(onDismiss: () -> Unit, onLanguageSelected: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.language)) },
        text = {
            Column {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLanguageSelected("ru") }
                    .padding(vertical = 12.dp)) {
                    Text(stringResource(id = R.string.russian))
                }
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLanguageSelected("en") }
                    .padding(vertical = 12.dp)) {
                    Text(stringResource(id = R.string.english))
                }
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLanguageSelected("pt") }
                    .padding(vertical = 12.dp)) {
                    Text(stringResource(id = R.string.portuguese))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } }
    )
}
