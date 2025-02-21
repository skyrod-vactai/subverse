package org.skyrod.subverse

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
                            history = history + input
                            input = ""
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Rest of the UI remains the same
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(history) { item ->
                    Text(
                        text = item,
                        modifier = Modifier
                            .clickable { input += item }
                            .padding(16.dp)
                            .fillMaxWidth()
                    )
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("Enter code") }
            )

            OutlinedTextField(
                value = output,
                onValueChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("Output") },
                readOnly = true
            )
        }
    }
}
