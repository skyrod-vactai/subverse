package org.skyrod.subverse

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import android.util.Log
import kotlinx.coroutines.CoroutineScope
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
                CoroutineScope(Dispatchers.Main).launch {
                    val result = withContext(Dispatchers.IO) {
                        kcatsEval(interpreterPtr, inputCode)
                    }
                    withContext(Dispatchers.Main) {
                        outputEditText.setText(result)
                        inputEditText.setText("")
                    }
                }

                // Switch to main thread for UI updates

            } else {
                outputEditText.setText("Error: Environment not initialized.")
            }
        }


    }
}
