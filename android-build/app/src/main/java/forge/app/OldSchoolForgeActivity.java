package forge.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class OldSchoolForgeActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set fullscreen
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // Create a WebView and use it as the main content view
        webView = new WebView(this);
        setContentView(webView);
        
        // Enable JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        
        // Set WebView client to handle page navigation within the app
        webView.setWebViewClient(new WebViewClient());
        
        // Show a welcome message
        Toast.makeText(this, "Old School Forge v0.36.0", Toast.LENGTH_SHORT).show();
        
        // Load the Forge desktop launcher page
        webView.loadUrl("https://card-forge.org/oldschool/");
    }
    
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
} 