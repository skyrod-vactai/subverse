package org.skyrod.subverse

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.twotone.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    private lateinit var cache: Cache

    init {
        System.loadLibrary("kcats")
    }

    private external fun kcatsNew(cachePath: String, dbFile: String): Long
    private external fun kcatsEval(env: Long, program: String): String

    private var interpreterPtr: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize cache and interpreter as before
        cache = Cache(applicationContext)
        cache.init()
        val cachePath = cache.getPath().canonicalPath
        val dbPath = applicationContext.getDatabasePath("subverse.db").canonicalPath
        Log.d("init", "Initializing cache at $cachePath")
        Log.d("init", "Initializing database at $dbPath")
        interpreterPtr = kcatsNew(cachePath, dbPath)

        setContent {
            MaterialTheme {
                MainScreen(
                    onEvaluate = { code ->
                        if (interpreterPtr != 0L) {
                            kcatsEval(interpreterPtr, code)
                        } else {
                            "Error: Environment not initialized."
                        }
                    }
                )
            }
        }
    }


}
@Composable
fun MainScreen(onEvaluate: (String) -> String) {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(listOf<String>()) }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (input.isNotEmpty()) {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                onEvaluate(input)
                            }
                            output = result
                            if (! history.contains(input)) {
                                history = history + input
                            }

                            input = ""
                        }
                    }
                },
                modifier = Modifier.imePadding()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Execute",
                    tint = androidx.compose.ui.graphics.Color.Black
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp),

                verticalArrangement = Arrangement.spacedBy(2.dp) // Add small spacing between items
            ) {
                items(history) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier
                                .clickable { input += item }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()

                        )
                    }
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                label = { Text("Enter code") }
            )

            OutlinedTextField(
                value = output,
                onValueChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .heightIn(max = 600.dp),
                label = { Text("Output") },
                readOnly = true
            )
        }
    }
}
