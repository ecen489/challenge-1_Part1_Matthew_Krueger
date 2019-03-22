package android.example.challenge1;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;
import android.view.OrientationEventListener;
import android.support.v4.content.LocalBroadcastManager;

import static android.content.ContentValues.TAG;

class OrientationChangeListener extends OrientationEventListener {

    public static final String BROADCAST_ORIENTATION_CHANGED = "OrientationChanged";
    public static final String EXTRA_NEW_ORIENTATION_KEY = "NewOrientation";
    public static final int ORIENTATION_NONE = 0;
    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    public static final int ORIENTATION_PORTRAIT_FLIPPED = 3;
    public static final int ORIENTATION_LANDSCAPE_FLIPPED = 4;

    private Context context;
    private int previousOrientation = ORIENTATION_NONE;

    public OrientationChangeListener(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation == ORIENTATION_UNKNOWN) {
            return;
        }

        int newOrientation = ORIENTATION_NONE;
        if (orientation >= 0 && orientation < 45 || orientation >= 315 && orientation <= 360) {
            newOrientation = ORIENTATION_PORTRAIT;
        } else if (orientation >= 45 && orientation < 135) {
            newOrientation = ORIENTATION_LANDSCAPE;
        } else if (orientation >= 135 && orientation < 225) {
            newOrientation = ORIENTATION_PORTRAIT_FLIPPED;
        } else if (orientation >= 225 && orientation < 315) {
            newOrientation = ORIENTATION_LANDSCAPE_FLIPPED;
        } else {
            Log.e("ooo", orientation + " WRONG ORIENTATION");
        }
        if (newOrientation != ORIENTATION_NONE && newOrientation != previousOrientation) {
            previousOrientation = newOrientation;
            sendOrientationChangedBroadcast(newOrientation);
        }
    }

    private void sendOrientationChangedBroadcast(int newOrientation) {
        Intent intent = new Intent(BROADCAST_ORIENTATION_CHANGED);
        intent.putExtra(EXTRA_NEW_ORIENTATION_KEY, newOrientation);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}

public class cameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String LOG_TAG = "CameraPreview";
    private static final float DEFAULT_ASPECT_RATIO = 4f / 3f;

    private Context context;
    private Camera camera;
    private CameraIdProvider cameraIdProvider;
    private SurfaceHolder surfaceHolder;
    private List<Camera.Size> supportedPreviewSizes;
    private Camera.Size previewSize;
    private List<Camera.Size> supportedPictureSizes;

    public cameraPreview(Context context, Camera camera) {
        super(context);
        this.context = context;
        this.camera = camera;
        this.cameraIdProvider = cameraIdProvider;
        this.surfaceHolder = getHolder();
        this.surfaceHolder.addCallback(this);
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            supportedPictureSizes = parameters.getSupportedPictureSizes();
        }
    }

    /*public void setAspectRatio(float aspectRatio) {
        Log.e("xxx", "new aspect ratio = " + aspectRatio);
        this.aspectRatio = aspectRatio;
        requestLayout();
    }*/

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        if (camera != null) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error setting camera preview: " + e.getMessage());
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // TODO: set preview size and make any resize, rotate or reformatting changes here
        /*
        If you want to set a specific size for your camera preview, set this in the surfaceChanged()
        method as noted in the comments above. When setting preview size, you must use values
        from getSupportedPreviewSizes(). Do not set arbitrary values in the setPreviewSize() method.
         */
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            //requestLayout();
            Camera.Size pictureSize = getMaximalPictureSizeWithSameAspectRatio();
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
            camera.setParameters(parameters);
        }

        Log.e("xxx", "surfaceChanged: width=" + width + " height=" + height);

        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

        } catch (Exception e) {
            Log.d(LOG_TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        /*int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        int width = (int) (height * aspectRatio);*/

        if (supportedPreviewSizes != null) {
            previewSize = getOptimalPreviewSize(width, height);
        }
        setMeasuredDimension(width, height);
        Log.e("xxx", "onMeasure: width=" + width + " height=" + height);
    }

    private Camera.Size getOptimalPreviewSize(int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;

        if (supportedPreviewSizes == null) {
            return null;
        }
        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;

        // Find size
        for (Camera.Size size : supportedPreviewSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : supportedPreviewSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private Camera.Size getMaximalPictureSizeWithSameAspectRatio() {
        Camera.Size pictureSize = supportedPictureSizes.get(0);
        for (Camera.Size size : supportedPictureSizes) {
            Log.e("xxx", size.width + " x " + size.height);
            int resultArea = pictureSize.width * pictureSize.height;
            int newArea = size.width * size.height;
            if (newArea >= resultArea &&
                    (float) size.width / size.height == (float) previewSize.width / previewSize.height) {
                pictureSize = size;
            }
        }
        Log.e("xxx", "Result: " + pictureSize.width + " x " + pictureSize.height);
        return pictureSize;
    }

    public void updateCameraOrientation(int newOrientation) {
        int rotation = getOutputMediaRotation(newOrientation);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(rotation);
        camera.setParameters(parameters);
    }

    public int getOutputMediaRotation(int orientation) {
        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(cameraIdProvider.getCurrentCameraId(), info);
        Log.d(LOG_TAG, "updateCameraOrientation() newOrientation == " + orientation);
        Log.d(LOG_TAG, "info.orientation == " + info.orientation);
        int degrees = 0;

        switch (orientation) {
            case OrientationChangeListener.ORIENTATION_NONE:
                degrees = 0;
                break;
            case OrientationChangeListener.ORIENTATION_PORTRAIT:
                degrees = 0;
                break;
            case OrientationChangeListener.ORIENTATION_LANDSCAPE:
                degrees = 270;
                break;
            case OrientationChangeListener.ORIENTATION_PORTRAIT_FLIPPED:
                degrees = 180;
                break;
            case OrientationChangeListener.ORIENTATION_LANDSCAPE_FLIPPED:
                degrees = 90;
                break;

        }
        Log.d(LOG_TAG, "degrees == " + degrees);

        int result;
        if (cameraIdProvider.getCurrentCameraId() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            if (orientation == OrientationChangeListener.ORIENTATION_PORTRAIT) {
                degrees = 180;
            } else if (orientation == OrientationChangeListener.ORIENTATION_PORTRAIT_FLIPPED) {
                degrees = 0;
            }
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        Log.d(LOG_TAG, "result == " + result);
        return result;
    }

    public interface CameraIdProvider {
        int getCurrentCameraId();
    }
}