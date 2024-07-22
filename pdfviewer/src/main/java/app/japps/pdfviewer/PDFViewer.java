package app.japps.pdfviewer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.AttributeSet;
import android.util.Base64;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.RawRes;

import com.japps.pdfviewer.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PDFViewer extends WebView {

    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer pdfRenderer;
    private int pageCount;

    public PDFViewer(Context context) {
        super(context);
        initialize(context, null);
    }

    public PDFViewer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public PDFViewer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true); // Enable JavaScript for PDF viewing
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false); // Hide the zoom controls
        settings.setSupportZoom(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        if (attrs != null) {
            // Read custom attributes if any (e.g., raw file id)
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PDFViewer);
            int rawFileId = a.getResourceId(R.styleable.PDFViewer_rawFile, 0);
            a.recycle();

            if (rawFileId != 0) {
                loadPdfFromRaw(context, rawFileId);
            }
        }
    }

    public String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void openRenderer(Context context, int rawFileId) throws IOException {
        // Copy PDF file from res/raw to app's cache directory


        File file = new File(context.getCacheDir(), "webview.pdf");;
        file =  copyRawFile(rawFileId, file); // Replace with your PDF file

        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        pageCount = pdfRenderer.getPageCount();
    }

    private File copyRawFile(int resourceId, File file) throws IOException {
        try (InputStream inputStream = getResources().openRawResource(resourceId);
             OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }
        return file;
    }


    public void loadPdfFromRaw(Context context, @RawRes int rawFileId) {
        List<Bitmap> bitmaps = new ArrayList<>();

        // Load PDF file from res/raw
        try {
            openRenderer(context,rawFileId);
            //displayPages();
            for(int i=0;i<pageCount;i++){
                bitmaps.add(pageBitmap(i));
            }

         /*   MyAdapter adapter = new MyAdapter(this,bitmaps,scrollViewLayout);
            scrollViewLayout.setAdapter(adapter);
            adapter.notifyDataSetChanged();*/

            // Prepare HTML content
            StringBuilder htmlContent = new StringBuilder();
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
                    .append("</script></head><body onload=\"addPageNumbers()\">"); // Call addPageNumbers() on body load

            for (Bitmap bitmap : bitmaps) {
                String base64Image = bitmapToBase64(bitmap);
                htmlContent.append("<div class=\"card\">")
                        .append("<img style=\"width: 100%; height: auto;\" src=\"data:image/png;base64,").append(base64Image).append("\"/>")
                        .append("</div>");
            }

            htmlContent.append("</body></html>");

            // Load HTML content into WebView
            loadDataWithBaseURL(null, htmlContent.toString(), "text/html", "UTF-8", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap pageBitmap(int index) {
        PdfRenderer.Page page = pdfRenderer.openPage(index);

        // Calculate scaled dimensions based on zoomScale
        int pdfWidth = page.getWidth();
        int pdfHeight = page.getHeight();
        float zoomScale = 1.0f;

        float scaleX = zoomScale * getResources().getDisplayMetrics().density;
        float scaleY = zoomScale * getResources().getDisplayMetrics().density;

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY);

        // Ensure scaled dimensions are valid
        int bitmapWidth = Math.max(1, Math.round(pdfWidth * scaleX));
        int bitmapHeight = Math.max(1, Math.round(pdfHeight * scaleY));

        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        page.close();

        return bitmap;
    }
}

