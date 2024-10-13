package com.vibs.webviewapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.vibs.webviewapp.ui.theme.WebviewAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebviewAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                WebViewScreen()
            }
        }
    }
}

@Composable
fun WebViewScreen() {
    val context = LocalContext.current
    var csvUri by remember { mutableStateOf<Uri?>(null) }

    // File picker launcher for saving the CSV file
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            csvUri = uri
            uri?.let {
                // Use coroutine to download the file in the background
                CoroutineScope(Dispatchers.IO).launch {
                    fetchAndSaveCsv(context, uri)
                }
            }
        }
    )

    Box {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?
                        ): Boolean {
                            if (url != null && url.endsWith(".csv")) {
                                handleCsvDownload(url, createDocumentLauncher)
                                return true // Intercept CSV download
                            }
                            return false // Load all other URLs in WebView
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl("https://google.com")
                }
            },
            update = { webView ->
                webView.loadUrl("https://google.com")
            }
        )
    }
}

private fun handleCsvDownload(
    url: String,
    createDocumentLauncher: ManagedActivityResultLauncher<String, Uri?>
) {
    // Launch the file picker to save the CSV file
    createDocumentLauncher.launch("downloaded_file.csv")
}

private suspend fun fetchAndSaveCsv(context: Context, uri: Uri) {
    val csvUrl = "https://your-csv-url.com/file.csv" // Modify this dynamically based on the WebView URL
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(csvUrl)
        .build()

    try {
        val response: Response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val inputStream: InputStream? = response.body?.byteStream()
            inputStream?.let { saveCsvFile(context, uri, it) }
        } else {
            Log.e("CSV Fetch Error", "Non Ok response from server: ${response.code}")
        }
    } catch (e: Exception) {
        Log.e("CSV Download Error", "Error downloading CSV: ${e.message}")
    }
}

private fun saveCsvFile(context: Context, uri: Uri, inputStream: InputStream) {
    val outputStream = context.contentResolver.openOutputStream(uri)
    outputStream?.use { outStream ->
        inputStream.copyTo(outStream)
        outStream.flush()
        Log.i("CSV Save", "CSV file saved successfully")
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
    WebviewAppTheme {
        Greeting("Android")
    }
}

