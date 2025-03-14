package org.skyrod.subverse


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
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
    val context = LocalContext.current
    val historyStorage = remember { HistoryStorage(context) }

    // Load history when screen starts
    LaunchedEffect(Unit) {
        history = historyStorage.loadHistory()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    input = input.trim()
                    if (input.isNotEmpty()) {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                onEvaluate(input)
                            }
                            output = result
                            if (! history.contains(input)) {
                                history = history + input
                                historyStorage.saveHistory(history)
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
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(history) { item ->
                    //val scope = rememberCoroutineScope()  // Moved inside the composable

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp, vertical = 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { input += " $item" }
                                    .padding(end = 8.dp)
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val newHistory = history.filter { it.trim() != item.trim() }
                                        history = newHistory
                                        historyStorage.saveHistory(newHistory)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Delete item",
                                    tint = MaterialTheme.colorScheme.error,
                                    //modifier = Modifier.padding(all = 1.dp).size(10.dp)
                                )
                            }
                        }
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


