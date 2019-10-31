package com.morphcast.poc_demo.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.support.annotation.NonNull;

/**
 * Created by Administrator on 8/7/2016.
 */
public class CameraSettingPreferences {

    private static  String capture_mode = "single_mode";
    private static final String FLASH_MODE = "squarecamera__flash_mode";

    private static SharedPreferences getCameraSettingPreferences(@NonNull final Context context) {
        return context.getSharedPreferences("com.desmond.squarecamera", Context.MODE_PRIVATE);
    }

    protected static void saveCameraFlashMode(@NonNull final Context context, @NonNull final String cameraFlashMode) {
        final SharedPreferences preferences = getCameraSettingPreferences(context);

        if (preferences != null) {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(FLASH_MODE, cameraFlashMode);
            editor.apply();
        }
    }

    protected static String getCameraFlashMode(@NonNull final Context context) {
        final SharedPreferences preferences = getCameraSettingPreferences(context);

        if (preferences != null) {
            return preferences.getString(FLASH_MODE, Camera.Parameters.FLASH_MODE_AUTO);
        }

        return Camera.Parameters.FLASH_MODE_AUTO;
    }
    protected static String getCaptureMode(@NonNull final Context context){

        return capture_mode;
    }
}
