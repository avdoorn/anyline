/*
 * Anyline Cordova Plugin
 * AnylineBaseActivity.java
 *
 * Copyright (c) 2015 Anyline GmbH
 *
 * Created by martin at 2015-12-09
 */
package io.anyline.cordova;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.hardware.Camera;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SensorManager.DynamicSensorCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import at.nineyards.anyline.camera.CameraController;
import at.nineyards.anyline.camera.CameraOpenListener;

public abstract class AnylineBaseActivity extends Activity
        implements CameraOpenListener, Thread.UncaughtExceptionHandler, SensorEventListener {

    private static final String TAG = AnylineBaseActivity.class.getSimpleName();
    
	private Toast notificationToast;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private float lightValue;
    
    protected String licenseKey;
    protected String configJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        licenseKey = getIntent().getExtras().getString(AnylinePlugin.EXTRA_LICENSE_KEY, "");
        configJson = getIntent().getExtras().getString(AnylinePlugin.EXTRA_CONFIG_JSON, "");
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        lightValue = event.values[0];
        // Do something with this sensor data.
    }
    
    /**
     * Always set this like this after the initAnyline: <br/>
     * scanView.getAnylineController().setWorkerThreadUncaughtExceptionHandler(this);<br/>
     * <br/>
     * This will forward background errors back to the plugin (and back to javascript from there)
     */
    @Override
    public void uncaughtException(Thread thread, Throwable e) {
        String msg = e.getMessage();
        Log.e(TAG, "Cached uncaught exception", e);

        String errorMessage;
        if (msg.contains("license") || msg.contains("License")) {
            errorMessage = Resources.getString(this, "error_licence_invalid") + "\n\n" + msg;
        } else {
            errorMessage = Resources.getString(this, "error_occured") + "\n\n" + e.getLocalizedMessage();
        }

        finishWithError(errorMessage);
    }

    protected void finishWithError(String errorMessage) {
        Intent data = new Intent();
        data.putExtra(AnylinePlugin.EXTRA_ERROR_MESSAGE, errorMessage);
        setResult(AnylinePlugin.RESULT_ERROR, data);
        finish();
    }

    @Override
    public void onCameraOpened(CameraController cameraController, int width, int height) {
        Log.d(TAG, "Camera opened. Frame size " + width + " x " + height + ".");
        if(lightValue < 50) {
            cameraController.setFlashOn(true);
        }
    }

    @Override
    public void onCameraError(Exception e) {
        finishWithError(Resources.getString(this, "error_accessing_camera") + "\n" + e.getLocalizedMessage());
    }
    
    @Override
    protected void onResume() {
      // Register a listener for the sensor.
      super.onResume();
      sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
  
    @Override
    protected void onPause() {
      // Be sure to unregister the sensor when the activity pauses.
      super.onPause();
      sensorManager.unregisterListener(this);
    }

    protected ArrayList getArrayListFromJsonArray(JSONArray jsonObject) {
        ArrayList<Double> listdata = new ArrayList<Double>();
        JSONArray jArray = jsonObject;
        try {
            for (int i = 0; i < jArray.length(); i++) {
                listdata.add(jArray.getDouble(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return listdata;
    }

    private void showToast(String text) {
		try {
			notificationToast.setText(text);
		} catch (Exception e) {
			notificationToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
		}
		notificationToast.show();
	}
    
    protected String jsonForOutline(List<PointF> pointList) {

        if (pointList == null || pointList.size() <= 0) {
            return "No Outline";
        }

        JSONObject upLeft = new JSONObject();
        JSONObject upRight = new JSONObject();
        JSONObject downRight = new JSONObject();
        JSONObject downLeft = new JSONObject();
        JSONObject outline = new JSONObject();

        try {
            upLeft.put("x", pointList.get(0).x);
            upLeft.put("y", pointList.get(0).y);

            upRight.put("x", pointList.get(1).x);
            upRight.put("y", pointList.get(1).y);

            downRight.put("x", pointList.get(2).x);
            downRight.put("y", pointList.get(2).y);

            downLeft.put("x", pointList.get(3).x);
            downLeft.put("y", pointList.get(3).y);

            outline.put("upLeft", upLeft);
            outline.put("upRight", upRight);
            outline.put("downRight", downRight);
            outline.put("downLeft", downLeft);


        } catch (JSONException e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }

        return outline.toString();
    }

}
