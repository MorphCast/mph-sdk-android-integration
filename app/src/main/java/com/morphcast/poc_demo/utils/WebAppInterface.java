package com.morphcast.poc_demo.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class WebAppInterface {


    private Context mContext;
    private String age = "0";
    private String gender = "";
    public WebAppInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    /**
     * maxSize = Max size in px of the larger side of the frame. You should scale the image yourself before returning it (optional)
     */
    public String getFrameFromApp(int maxSize) {
        /*
            * App developer shall implement the following behaviour:

            1) Retrieve a frame from camera

            * We suggest to resize frames before passing them to the WebView and to encode them in base64 format
            * Strings are processed faster than binary data through the JavaScript Interface

            2) resize it to maxSize x maxSize (maintaining the aspect ratio)
            3) Convert the frame to Base64
            4) return the Base64 String
         */

        // return res;
        if (GlobalObject.g_data != null && GlobalObject.g_cam != null) {
            String baos = convertYuvToJpeg(GlobalObject.g_data, GlobalObject.g_cam,  maxSize);
            GlobalObject.g_flag = true;

            return "data:image/jpeg;base64," + baos;
        }
        return "";
    }

    @JavascriptInterface
    /*
     * @type: String-enum in ["AGE","GENDER","EMOTION","FEATURES","POSE","AROUSAL_VALENCE","ATTENTION"]
     * @value: Json-stringified of the result
     */
    public void onDataFromMphSdk(String type, final String value) {
        /*
            App developer can use the values returned from the webview to implement the desired behavior
            (e.g update the App view, send data to a custom db ...)
         */



        if (type.equals("AGE") && value != null) {
            age = value;
        }
        if (type.equals("GENDER") && value != null) {

            try {

                JSONObject obj = new JSONObject(value);

                float f_male = Float.parseFloat(obj.getString("Male"));
                float f_female = Float.parseFloat(obj.getString("Female"));

                if (f_male > f_female) {
                    gender = "Male";
                } else {
                    gender = "Female";
                }
            } catch (Throwable t) {
                Log.e("My App", "Could not parse malformed JSON: \"" + value + "\"");
            }
        }
        if (GlobalObject.g_tvAge != null) {

            final String finalAge = age;
            final String finalGender = gender;
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GlobalObject.g_tvAge.setText(finalAge);
                    GlobalObject.g_tvGender.setText(finalGender);
                }
            });

        }
    }
    public String convertYuvToJpeg(byte[] data, Camera camera, int maxSize) {

        YuvImage image = new YuvImage(data, ImageFormat.NV21,
                camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, null);

        int w = camera.getParameters().getPreviewSize().width;
        int h = camera.getParameters().getPreviewSize().height;
        float ratio = 1;
        float width = 0;
        float height = 0;
        if (w > 0 && h > 0) {
            if (w > h) {
                ratio = (float) w / (float)h;
            } else {
                ratio = (float)h / (float)w;
            }
        }
        width = (float)maxSize;
        height = (float)width / ratio;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 50; //set quality
        image.compressToJpeg(new Rect(0, 0, w, h), quality, baos);//this line decreases the image quality


        byte[] imageBytes = baos.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        Matrix matrix = new Matrix();

        matrix.postRotate(GlobalObject.g_rotation);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int)width, (int)height, true);

        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

        byte[] b = outputStream.toByteArray();
        return Base64.encode(b);

    }
}
