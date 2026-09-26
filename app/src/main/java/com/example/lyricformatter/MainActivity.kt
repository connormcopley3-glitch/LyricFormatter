package com.example.lyricformatter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LyricFormatterApp() }
    }
}

private fun readClipboard(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return if (clipboard.hasPrimaryClip()) {
        clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
    } else ""
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Formatted lyrics", text))
}

fun formatLyrics(input: String): String {
    // Normalize all common line endings before splitting into stanzas.
    val normalized = input.replace("\r\n", "\n").replace("\r", "\n")
    val stanzas = normalized.split(Regex("\n\\s*\n"))
    return stanzas.mapNotNull { raw ->
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) null else {
            val formatted = lines.mapIndexed { index, line ->
                if (index == lines.lastIndex) line.removeSuffix("/").trimEnd()
                else if (line.endsWith("/")) line else "$line /"
            }
            "\"${formatted.joinToString("\n")}\""
        }
    }.joinToString("\n\n")
}

@Composable
fun LyricFormatterApp() {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    // Automatically pull text from the clipboard when the app opens.
    LaunchedEffect(Unit) {
        val clipboardText = readClipboard(context)
        if (clipboardText.isNotBlank()) {
            input = clipboardText
            status = "Lyrics pasted from clipboard"
        }
    }

    fun formatAndCopy() {
        val formatted = formatLyrics(input)
        output = formatted
        if (formatted.isNotBlank()) {
            copyToClipboard(context, formatted)
            status = "Formatted lyrics copied to clipboard"
        } else {
            status = "Paste or enter lyrics first"
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Lyric Formatter", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Blank lines separate stanzas. Each stanza gets quotation marks; every non-final line gets a slash.")

                Text("Lyrics", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; status = "" },
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    placeholder = { Text("Paste lyrics here…") }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val pasted = readClipboard(context)
                        if (pasted.isNotBlank()) {
                            input = pasted
                            status = "Lyrics pasted from clipboard"
                        } else status = "Clipboard is empty"
                    }) { Text("Paste") }
                    OutlinedButton(onClick = { input = ""; output = ""; status = "" }) { Text("Clear") }
                }

                Button(
                    onClick = { formatAndCopy() },
                    enabled = input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Format & Copy") }

                Text("Formatted Lyrics", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = output,
                    onValueChange = { output = it },
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    readOnly = true
                )

                Button(
                    onClick = {
                        copyToClipboard(context, output)
                        status = "Copied to clipboard"
                    },
                    enabled = output.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Copy Again") }

                if (status.isNotBlank()) {
                    Text(status, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
