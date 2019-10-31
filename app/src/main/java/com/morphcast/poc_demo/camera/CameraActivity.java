package com.morphcast.poc_demo.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.morphcast.poc_demo.R;
import com.morphcast.poc_demo.utils.WebAppInterface;


/**
 * Created by Administrator on 8/7/2016.
 */
public class CameraActivity extends AppCompatActivity {

    public WebView webView;
    private ProgressDialog mProgress;
    public static final String TAG = CameraActivity.class.getSimpleName();
    String url = "https://demo.morphcast.com/native-app-webview/webview_index_exec.html";
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setTheme(R.style.squarecamera_CameraFullScreenTheme);
        super.onCreate(savedInstanceState);

//        if (getSupportActionBar() != null) {
//            getSupportActionBar().hide();
//        }
        setContentView(R.layout.squarecamera_activity_camera);
        haveStoragePermission();
        this.webView = this.findViewById(R.id.web_view);
        mProgress = new ProgressDialog(this);
        this.webView.setWebViewClient(new CustomWebViewClient());
        WebSettings settings = this.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        this.webView.addJavascriptInterface(new WebAppInterface(this), "NativeApp");

        // To debug in chrome inspect
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        String local_url = "https://f60c5119.ngrok.io/index.html";
        loadUrl(url);

        if (haveStoragePermission() && savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, CameraFragment.newInstance(), CameraFragment.TAG)
                    .commit();
        }
    }

    private void loadUrl(String url) {
        if (haveStoragePermission()) {
            this.webView.loadUrl(url);
        }

    }

    public void errorActivity(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, "Cannot Capture Image, Please Try again.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }


    public void returnFlag(int mode){
        Intent data = new Intent();
        data.setFlags(mode);

        if (getParent() == null) {
            setResult(RESULT_OK, data);
        } else {
            getParent().setResult(RESULT_OK, data);
        }

        finish();
    }

    public void onCancel(View view) {
        getSupportFragmentManager().popBackStack();
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    public class CustomWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            //TODO: show progress bar here
            //setting progress and showing progress Text
            mProgress.setMessage("Loading...");
            mProgress.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            //TODO: hide progress bar here
            //shutting down progress when its complete
            mProgress.dismiss();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.e(TAG, url);
            loadUrl(url);
            return true;
        }


        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final Uri uri = request.getUrl();
            Log.e(TAG, "Hello Gui");
            loadUrl(uri.toString());
            return true;
        }

    }
    public  boolean haveStoragePermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.e("Permission","You have permission");
                return true;
            } else {

                Log.e("Permission error","You have asked for permission");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return false;
            }
        }
        else { //you dont need to worry about these stuff below api level 23
            Log.e("Permission error","You already have the permission");
            return true;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, CameraFragment.newInstance(), CameraFragment.TAG)
                            .commit();
                    this.webView.loadUrl(url);
                }
            }
        }
    }

}
