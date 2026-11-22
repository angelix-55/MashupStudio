package com.mashupstudio.app

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AudioTrack(val uri: Uri, val name: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MashupStudioScreen() }
    }
}

@Composable
fun MashupStudioScreen() {
    val context = LocalContext.current
    val tracks = remember { mutableStateListOf<AudioTrack>() }
    var selectedA by remember { mutableStateOf<AudioTrack?>(null) }
    var selectedB by remember { mutableStateOf<AudioTrack?>(null) }
    var playing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Importa i tuoi brani audio per iniziare.") }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
                } ?: "Traccia audio"
                if (tracks.none { it.uri == uri }) tracks.add(AudioTrack(uri, name))
            }
            status = "${uris.size} file selezionati. Scegli Track A e Track B."
        } else {
            status = "Nessun file selezionato."
        }
    }

    fun playTrack(track: AudioTrack?) {
        if (track == null) {
            status = "Seleziona prima una traccia."
            return
        }
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(context, track.uri)
                setOnPreparedListener {
                    it.start()
                    playing = true
                    status = "In riproduzione: ${track.name}"
                }
                setOnCompletionListener {
                    playing = false
                    status = "Riproduzione terminata."
                }
                setOnErrorListener { _, _, _ ->
                    playing = false
                    status = "Impossibile riprodurre questo formato audio."
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            status = "Non riesco ad aprire questo file audio."
        }
    }

    val bg = Brush.verticalGradient(listOf(Color(0xFF111020), Color(0xFF20133D), Color(0xFF0B0B14)))
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF9A7BFF),
        secondary = Color(0xFF47D7D0),
        background = Color(0xFF111020),
        surface = Color(0xFF211D35)
    )) {
        Column(
            modifier = Modifier.fillMaxSize().background(bg).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("MASHUP STUDIO", color = Color(0xFFBBA8FF), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            Text("MATCH FINDER", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("La tua console musicale", color = Color.LightGray, fontSize = 14.sp)

            Button(
                onClick = { picker.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("＋  SCEGLI FILE AUDIO", fontWeight = FontWeight.Bold) }

            Text(status, color = Color(0xFFBDB7D0), fontSize = 12.sp)

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC211D35)), modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("LA TUA LIBRERIA  ·  ${tracks.size} BRANI", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (tracks.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("I brani importati appariranno qui.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tracks) { track ->
                                Row(
                                    Modifier.fillMaxWidth().background(Color(0xFF30294A), RoundedCornerShape(10.dp)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(track.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text("File audio importato", color = Color.LightGray, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { selectedA = track; status = "Track A: ${track.name}" }) { Text("A") }
                                    TextButton(onClick = { selectedB = track; status = "Track B: ${track.name}" }) { Text("B") }
                                    TextButton(onClick = { playTrack(track) }) { Text("▶") }
                                }
                            }
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC211D35))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("MIXER", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("TRACK A  ·  ${selectedA?.name ?: "Non selezionata"}", color = Color(0xFFBBA8FF), fontSize = 12.sp)
                    Text("TRACK B  ·  ${selectedB?.name ?: "Non selezionata"}", color = Color(0xFF47D7D0), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { playTrack(selectedA) }, modifier = Modifier.weight(1f)) { Text("▶ PLAY A") }
                        OutlinedButton(onClick = { playTrack(selectedB) }, modifier = Modifier.weight(1f)) { Text("▶ PLAY B") }
                    }
                }
            }
            Text("Versione iniziale: importazione Android nativa e anteprima audio. Analisi BPM/tonalità e mixdown arriveranno nei prossimi moduli.", color = Color.Gray, fontSize = 10.sp)
        }
    }
}
