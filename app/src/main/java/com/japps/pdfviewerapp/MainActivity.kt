package com.japps.pdfviewerapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.japps.pdfviewer.PDFViewer

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val webView:PDFViewer = findViewById(R.id.webview)
        webView.loadUrl("https://uat-mhb-mb.turingcbs.com/docs/DebugMode.html")

    }
}