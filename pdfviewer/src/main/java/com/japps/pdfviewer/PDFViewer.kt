package com.japps.pdfviewer
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.util.Base64
import android.webkit.WebView
import androidx.annotation.RawRes
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max


class PDFViewer : WebView {
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var pageCount = 0

    constructor(context: Context) : super(context) {
        initialize(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(context, attrs)
    }

    private fun initialize(context: Context, attrs: AttributeSet?) {
        val settings = settings
        settings.javaScriptEnabled = true // Enable JavaScript for PDF viewing
        settings.builtInZoomControls = true
        settings.displayZoomControls = false // Hide the zoom controls
        settings.setSupportZoom(true)
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        if (attrs != null) {
            // Read custom attributes if any (e.g., raw file id)
            val a = context.obtainStyledAttributes(attrs, R.styleable.PDFViewer)
            val rawFileId = a.getResourceId(R.styleable.PDFViewer_rawFile, 0)
            a.recycle()

            if (rawFileId != 0) {
                loadPdfFromRaw(context, rawFileId)
            }
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    @Throws(IOException::class)
    private fun openRenderer(context: Context, rawFileId: Int) {
        // Copy PDF file from res/raw to app's cache directory


        var file = File(context.cacheDir, "webview.pdf")

        file = copyRawFile(rawFileId, file) // Replace with your PDF file

        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
        pageCount = pdfRenderer!!.pageCount
    }

    @Throws(IOException::class)
    private fun copyRawFile(resourceId: Int, file: File): File {
        resources.openRawResource(resourceId).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(1024)
                var read: Int
                while ((inputStream.read(buffer).also { read = it }) != -1) {
                    outputStream.write(buffer, 0, read)
                }
            }
        }
        return file
    }


    fun loadPdfFromRaw(context: Context, @RawRes rawFileId: Int) {
        val bitmaps: MutableList<Bitmap> = ArrayList()

        // Load PDF file from res/raw
        try {
            openRenderer(context, rawFileId)
            //displayPages();
            for (i in 0 until pageCount) {
                bitmaps.add(pageBitmap(i))
            }

            /*   MyAdapter adapter = new MyAdapter(this,bitmaps,scrollViewLayout);
            scrollViewLayout.setAdapter(adapter);
            adapter.notifyDataSetChanged();*/

            // Prepare HTML content
            val htmlContent = StringBuilder()
            htmlContent.append("<html><head><style>")
                .append(".card {")
                .append("  border-radius: 8px;") // Rounded corners
                .append("  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);") // Shadow effect
                .append("  margin: 8px;") // Margin around each card
                .append("  position: relative;") // Positioning for page number
                .append("}")
                .append(".page-number {")
                .append("  position: absolute;") // Absolute positioning
                .append("  top: 0;") // Position at top
                .append("  right: 0;") // Position at right
                .append("  background-color: rgba(0, 0, 0, 0.5);") // Background color for contrast
                .append("  color: white;") // Text color
                .append("  padding: 2px 6px;") // Padding for spacing
                .append("  border-top-left-radius: 4px;") // Rounded corners
                .append("}")
                .append("</style><script>")
                .append("function addPageNumbers() {")
                .append("  var imgs = document.getElementsByTagName('img');")
                .append("  for (var i = 0; i < imgs.length; i++) {")
                .append("    var pageNumber = i + 1;")
                .append("    var cardDiv = imgs[i].parentNode;")
                .append("    var pageNumberDiv = document.createElement('div');")
                .append("    pageNumberDiv.className = 'page-number';")
                .append("    pageNumberDiv.innerHTML = 'Page ' + pageNumber;")
                .append("    cardDiv.appendChild(pageNumberDiv);")
                .append("  }")
                .append("}")
                .append("</script></head><body onload=\"addPageNumbers()\">") // Call addPageNumbers() on body load

            for (bitmap in bitmaps) {
                val base64Image = bitmapToBase64(bitmap)
                htmlContent.append("<div class=\"card\">")
                    .append("<img style=\"width: 100%; height: auto;\" src=\"data:image/png;base64,")
                    .append(base64Image).append("\"/>")
                    .append("</div>")
            }

            htmlContent.append("</body></html>")

            // Load HTML content into WebView
            loadDataWithBaseURL(null, htmlContent.toString(), "text/html", "UTF-8", null)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun pageBitmap(index: Int): Bitmap {
        val page = pdfRenderer!!.openPage(index)

        // Calculate scaled dimensions based on zoomScale
        val pdfWidth = page.width
        val pdfHeight = page.height
        val zoomScale = 1.0f

        val scaleX = zoomScale * resources.displayMetrics.density
        val scaleY = zoomScale * resources.displayMetrics.density

        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY)

        // Ensure scaled dimensions are valid
        val bitmapWidth =
            max(1.0, Math.round(pdfWidth * scaleX).toDouble()).toInt()
        val bitmapHeight =
            max(1.0, Math.round(pdfHeight * scaleY).toDouble()).toInt()

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        page.close()

        return bitmap
    }
}

