package org.skyrod.subverse

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.skyrod.subverse.ui.theme.SubverseTheme
import android.util.Log

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
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Get UI elements
        val inputEditText = findViewById<EditText>(R.id.inputEditText)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val outputEditText = findViewById<EditText>(R.id.outputEditText)

        cache = Cache(applicationContext)

        cache.init()
        val cachePath = cache.getPath().canonicalPath
        val dbPath = applicationContext.getDatabasePath("subverse.db").canonicalPath
        Log.d("init" , "Initializing cache at $cachePath")
        Log.d("init", "Initializing database at $dbPath")
        // Initialize the interpreter environment

        interpreterPtr = kcatsNew(cachePath, dbPath)

        sendButton.setOnClickListener {

            val inputCode = inputEditText.text.toString()

            // Ensure the environment is set up properly
            if (interpreterPtr != 0L) {
                val result = kcatsEval(interpreterPtr, inputCode)
                outputEditText.setText(result)
                inputEditText.setText("")
            } else {
                outputEditText.setText("Error: Environment not initialized.")
            }
        }


    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SubverseTheme {
        Greeting("Android")
    }
}