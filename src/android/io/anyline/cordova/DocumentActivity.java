package io.anyline.cordova;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import at.nineyards.anyline.camera.AnylineViewConfig;
import at.nineyards.anyline.camera.CameraController;
import at.nineyards.anyline.camera.CameraOpenListener;
import at.nineyards.anyline.models.AnylineImage;
import at.nineyards.anyline.modules.document.DocumentResultListener;
import at.nineyards.anyline.modules.document.DocumentResult;
import at.nineyards.anyline.modules.document.DocumentScanView;

/**
 * Example activity for the Anyline-Document-Detection-Module
 */
public class DocumentActivity extends AnylineBaseActivity implements CameraOpenListener {

    private static final long ERROR_MESSAGE_DELAY = 2000;
    private static final String TAG = DocumentActivity.class.getSimpleName();
    private DocumentScanView documentScanView;
    private Toast notificationToast;
    private ImageView imageViewResult;
    private ProgressDialog progressDialog;
    private List<PointF> lastOutline;
    private ObjectAnimator errorMessageAnimator;
    private FrameLayout errorMessageLayout;
    private TextView errorMessage;
    private long lastErrorRecieved = 0;

    private android.os.Handler handler = new android.os.Handler();

    // takes care of fading the error message out after some time with no error reported from the SDK
    private Runnable errorMessageCleanup = new Runnable() {
        @Override
        public void run() {
            if (DocumentActivity.this.isFinishing()) {
                return;
            }
            if (System.currentTimeMillis() > lastErrorRecieved + ERROR_MESSAGE_DELAY) {
                if (errorMessage == null || errorMessageAnimator == null) {
                    return;
                }
                if (errorMessage.getAlpha() == 0f) {
                    errorMessage.setText("");
                } else if (!errorMessageAnimator.isRunning()) {
                    errorMessageAnimator = ObjectAnimator.ofFloat(errorMessage, "alpha", errorMessage.getAlpha(), 0f);
                    errorMessageAnimator.setDuration(ERROR_MESSAGE_DELAY);
                    errorMessageAnimator.setInterpolator(new AccelerateInterpolator());
                    errorMessageAnimator.start();
                }
            }
            handler.postDelayed(errorMessageCleanup, ERROR_MESSAGE_DELAY);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getResources().getIdentifier("activity_scan_document", "layout", getPackageName()));
        //Set the flag to keep the screen on (otherwise the screen may go dark during scanning)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imageViewResult = (ImageView) findViewById(getResources().getIdentifier("image_result", "id", getPackageName()));
        errorMessageLayout = (FrameLayout) findViewById(getResources().getIdentifier("error_message_layout", "id", getPackageName()));
        errorMessage = (TextView) findViewById(getResources().getIdentifier("error_message", "id", getPackageName()));

        documentScanView = (DocumentScanView) findViewById(getResources().getIdentifier("document_scan_view", "id", getPackageName()));
        // add a camera open listener that will be called when the camera is opened or an error occurred
        //  this is optional (if not set a RuntimeException will be thrown if an error occurs)
        documentScanView.setCameraOpenListener(this);
        // the view can be configured via a json file in the assets, and this config is set here
        // (alternatively it can be configured via xml, see the Energy Example for that)
        JSONObject jsonObject;

        try {
            jsonObject = new JSONObject(configJson);
        } catch (Exception e) {
            //JSONException or IllegalArgumentException is possible, return it to javascript
            finishWithError(Resources.getString(this, "error_invalid_json_data") + "\n" + e.getLocalizedMessage());
            return;
        }

		documentScanView.setMaxDocumentRatioDeviation(3.15);

		
        documentScanView.setConfig(new AnylineViewConfig(this, jsonObject));

        // Optional: Set a ratio you want the documents to be restricted to. default is set to DIN_AX
        //documentScanView.setDocumentRatios(DocumentScanView.DocumentRatio.DIN_AX_PORTRAIT.getRatio(), DocumentScanView.DocumentRatio.DIN_AX_PORTRAIT.getRatio());

        // Optional: Set a maximum deviation for the ratio. 0.15 is the default
        //documentScanView.setMaxDocumentRatioDeviation(0.15);

        // initialize Anyline with the license key and a Listener that is called if a result is found
        documentScanView.initAnyline(licenseKey, new DocumentResultListener() {
            @Override
            public void onResult(DocumentResult documentResult) {

                // handle the result document images here
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                JSONObject jsonResult = new JSONObject();
				
				try {
					AnylineImage transformedImage = documentResult.getResult();
					String result = new String();
					File imageFile = new File(getFilesDir(), "test.jpg");
					transformedImage.save(imageFile,100);
					
					
					FileInputStream fis = new FileInputStream(imageFile);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					Base64OutputStream base64out = new Base64OutputStream(baos, Base64.NO_WRAP);
					byte[] buffer = new byte[1024];
					int len = 0;
					while ((len = fis.read(buffer)) >= 0) {
						base64out.write(buffer, 0, len);
					}
					base64out.flush();
					base64out.close();
					/*
					 * Why should we close Base64OutputStream before processing the data:
					 * http://stackoverflow.com/questions/24798745/android-file-to-base64-using-streaming-sometimes-missed-2-bytes
					 */
					 
					byte[] data = baos.toByteArray();
					
					
					// Apply contrast
					byte contrast = (byte)50;
					int factor = (259 * (contrast + 255)) / (255 * (259 - contrast));
					byte byteFactor = (byte)factor;
					int inbetween;
					int dataCounter = 0;
					int dataLength = data.length;
					while(dataCounter<dataLength)
					{
						inbetween = factor * (data[dataCounter] - 128) + 128;
						data[dataCounter] = (byte)inbetween;
						inbetween = factor * (data[dataCounter+1] - 128) + 128;
						data[dataCounter+1] = (byte)inbetween;
						inbetween = factor * (data[dataCounter+2] - 128) + 128;
						data[dataCounter+2] = (byte)inbetween;
						dataCounter+=4;
					}
					
					result = new String(data, "UTF-8");
					
					baos.close(); 
					fis.close();
					/**
					 * IMPORTANT: cache provided frames here, and release them at the end of this onResult. Because
					 * keeping them in memory (e.g. setting the full frame to an ImageView)
					 * will result in a OutOfMemoryError soon. This error is reported in {@link #onTakePictureError
					 * (Throwable)}
					 *
					 * Use a DiskCache http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html#disk-cache
					 * for example
					 *
					 */

					// release the images
					transformedImage.release(); 
					
                    jsonResult.put("imageData", result);
				}
				catch(Exception e) {
					String exceptionMessage = e.getMessage();
					try {

						jsonResult.put("imageData", "Error: "+exceptionMessage);

					} catch (Exception je) {
						Log.e(TAG, "Error while putting image data to json.", je);
					}
				}

                Boolean cancelOnResult = true;

                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(configJson);
                    cancelOnResult = jsonObject.getBoolean("cancelOnResult");
                } catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage());
                }

                if (cancelOnResult) {
                    ResultReporter.onResult(jsonResult, true);
                    setResult(AnylinePlugin.RESULT_OK);
                    finish();
                } else {
                    ResultReporter.onResult(jsonResult, false);
                }


            }


            @Override
            public void onPreviewProcessingSuccess(AnylineImage anylineImage) {
                // this is called after the preview of the document is completed, and a full picture will be
                // processed automatically
            }

            @Override
            public void onPreviewProcessingFailure(DocumentScanView.DocumentError documentError) {
                // this is called on any error while processing the document image
                // Note: this is called every time an error occurs in a run, so that might be quite often
                // An error message should only be presented to the user after some time

                //showErrorMessageFor(documentError);
            }

            @Override
            public void onPictureProcessingFailure(DocumentScanView.DocumentError documentError) {

            }

            @Override
            public boolean onDocumentOutlineDetected(List<PointF> list, boolean documentShapeAndBrightnessValid) {
                // is called when the outline of the document is detected. return true if the outline is consumed by
                // the implementation here, false if the outline should be drawn by the DocumentScanView
                lastOutline = list; // saving the outline for the animations
                return false;
            }

            @Override
            public void onTakePictureSuccess() {

                if (errorMessageAnimator != null && errorMessageAnimator.isRunning()) {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            errorMessageAnimator.cancel();
                            errorMessageLayout.setVisibility(View.GONE);
                        }
                    });

                }
            }

            @Override
            public void onTakePictureError(Throwable throwable) {
                // This is called if the image could not be captured from the camera (most probably because of an
                // OutOfMemoryError)
                throw new RuntimeException(throwable);
            }

            @Override
            public void onPictureCornersDetected(AnylineImage anylineImage, List<PointF> list) {
                // this is called after manual corner detection was requested
                // Note: not implemented in this example
            }

            @Override
            public void onPictureTransformed(AnylineImage anylineImage) {
                // this is called after a full frame image and 4 corners were passed to the SDK for
                // transformation (e.g. when a user manually selected the corners in an image)
                // Note: not implemented in this example
            }

            @Override
            public void onPictureTransformError(DocumentScanView.DocumentError documentError) {
                // this is called on any error while transforming the document image from the 4 corners
                // Note: not implemented in this example
            }

        });


        // optionally stop the scan once a valid result was returned
//        documentScanView.setCancelOnResult(cancelOnResult);

    }


    private void showErrorMessageFor(DocumentScanView.DocumentError documentError) {
        showErrorMessageFor(documentError, false);
    }

    private void showErrorMessageFor(DocumentScanView.DocumentError documentError, boolean highlight) {
        String text = getString(getResources().getIdentifier("document_picture_error", "string", getPackageName()));
        switch (documentError) {
            case DOCUMENT_NOT_SHARP:
                text += getString(getResources().getIdentifier("document_error_not_sharp", "string", getPackageName()));
                break;
            case DOCUMENT_SKEW_TOO_HIGH:
                text += getString(getResources().getIdentifier("document_error_skew_too_high", "string", getPackageName()));
                break;
            case DOCUMENT_OUTLINE_NOT_FOUND:
                //text += getString(R.string.document_error_outline_not_found);
                return; // exit and show no error message for now!
            case IMAGE_TOO_DARK:
                text += getString(getResources().getIdentifier("document_error_too_dark", "string", getPackageName()));
                break;
            case SHAKE_DETECTED:
                text += getString(getResources().getIdentifier("document_error_shake", "string", getPackageName()));
                break;
            case DOCUMENT_BOUNDS_OUTSIDE_OF_TOLERANCE:
                text += getString(getResources().getIdentifier("document_error_closer", "string", getPackageName()));
                break;
            case DOCUMENT_RATIO_OUTSIDE_OF_TOLERANCE:
                text += getString(getResources().getIdentifier("document_error_format", "string", getPackageName()));
                break;
            case UNKNOWN:
                break;
            default:
                text += getString(getResources().getIdentifier("document_error_unknown", "string", getPackageName()));
                return; // exit and show no error message for now!
        }

        if (highlight) {
            showHighlightErrorMessageUiAnimated(text);
        } else {
            showErrorMessageUiAnimated(text);
        }
    }

    private void showErrorMessageUiAnimated(String message) {
        if (lastErrorRecieved == 0) {
            // the cleanup takes care of removing the message after some time if the error did not show up again
            handler.post(errorMessageCleanup);
        }
        lastErrorRecieved = System.currentTimeMillis();
        if (errorMessageAnimator != null && (errorMessageAnimator.isRunning() || errorMessage.getText().equals
                (message))) {
            return;
        }

        errorMessageLayout.setVisibility(View.VISIBLE);
        errorMessage.setBackgroundColor(ContextCompat.getColor(this, getResources().getIdentifier("anyline_blue_darker", "color", getPackageName())));
        errorMessage.setAlpha(0f);
        errorMessage.setText(message);
        errorMessageAnimator = ObjectAnimator.ofFloat(errorMessage, "alpha", 0f, 1f);
        errorMessageAnimator.setDuration(ERROR_MESSAGE_DELAY);
        errorMessageAnimator.setInterpolator(new DecelerateInterpolator());
        errorMessageAnimator.start();
    }

    private void showHighlightErrorMessageUiAnimated(String message) {
        lastErrorRecieved = System.currentTimeMillis();
        errorMessageLayout.setVisibility(View.VISIBLE);
        errorMessage.setBackgroundColor(ContextCompat.getColor(this, getResources().getIdentifier("anyline_red", "color", getPackageName())));
        errorMessage.setAlpha(0f);
        errorMessage.setText(message);

        if (errorMessageAnimator != null && errorMessageAnimator.isRunning()) {
            errorMessageAnimator.cancel();
        }

        errorMessageAnimator = ObjectAnimator.ofFloat(errorMessage, "alpha", 0f, 1f);
        errorMessageAnimator.setDuration(ERROR_MESSAGE_DELAY);
        errorMessageAnimator.setInterpolator(new DecelerateInterpolator());
        errorMessageAnimator.setRepeatMode(ValueAnimator.REVERSE);
        errorMessageAnimator.setRepeatCount(1);
        errorMessageAnimator.start();
    }

    private void showToast(String text) {
        try {
            notificationToast.setText(text);
        } catch (Exception e) {
            notificationToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        }
        notificationToast.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //start the actual scanning
        documentScanView.startScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stop the scanning
        documentScanView.cancelScanning();
        //release the camera (must be called in onPause, because there are situations where
        // it cannot be auto-detected that the camera should be released)
        documentScanView.releaseCameraInBackground();
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(errorMessageCleanup);
        errorMessageCleanup = null;
        handler = null;
    }

    @Override
    public void onCameraOpened(CameraController cameraController, int width, int height) {
        //the camera is opened async and this is called when the opening is finished
        Log.d(TAG, "Camera opened successfully. Frame resolution " + width + " x " + height);
    }

    @Override
    public void onCameraError(Exception e) {
        //This is called if the camera could not be opened.
        // (e.g. If there is no camera or the permission is denied)
        // This is useful to present an alternative way to enter the required data if no camera exists.
        throw new RuntimeException(e);
    }
}
