package com.ray.lib.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author : leixing
 * @date : 2018-01-04
 * <p>
 * Email       : leixing@hecom.cn
 * Version     : 0.0.1
 * <p>
 * Description : xxx
 */

public class CameraHolder {
    private static final String TAG = "CameraHolder";

    private static final int TEN_DESIRED_ZOOM = 27;
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private static final int DELAY_MILLIS = 1500;

    private final Context mContext;
    private final SurfaceHolder mHolder;
    private Camera mCamera;
    private final InternalHandler mHandler;
    private PreviewCallback mPreviewCallback;
    private Point mResolution;
    private CameraStatus mStatus;

    public CameraHolder(Context context, SurfaceHolder holder) {
        mContext = context;
        mHolder = holder;
        mHandler = new InternalHandler(Looper.myLooper());
        mStatus = CameraStatus.CONSTRUCT;

        bindSurface();
    }

    private void bindSurface() {
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.i(TAG, "surfaceCreated: ");
                if (initCamera(holder)) {
                    mStatus = CameraStatus.OPEN;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.i(TAG, "surfaceChanged: "
                        + "\nformat: " + format
                        + "\nwidth:" + width
                        + "\nheight:" + height);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.i(TAG, "surfaceDestroyed: ");
                releaseCamera();
            }
        });
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private boolean initCamera(SurfaceHolder holder) {
        mCamera = Camera.open();
        if (mCamera == null) {
            return false;
        }
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
            releaseCamera();
            return false;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        int previewFormat = parameters.getPreviewFormat();
        String previewFormatString = parameters.get("preview-format");
        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point screenResolution = new Point(display.getWidth(), display.getHeight());
        Point screen = new Point();
        screen.x = screenResolution.y;
        screen.y = screenResolution.x;
        List<Camera.Size> a = parameters.getSupportedPreviewSizes();
        mResolution = getCameraResolution(parameters, screen);


        parameters.setPreviewSize(mResolution.x, mResolution.y);
        setFlash(parameters);
        setZoom(parameters);
        mCamera.setDisplayOrientation(90);
        if ((!Camera.Parameters.ANTIBANDING_50HZ.equals(parameters.getAntibanding())) && isSupported(Camera.Parameters.ANTIBANDING_50HZ, parameters.getSupportedAntibanding())) {
            parameters.setAntibanding(Camera.Parameters.ANTIBANDING_50HZ);
        }
        parameters.setPictureFormat(ImageFormat.NV21);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        startAutoFocus();
        return true;
    }

    private void startAutoFocus() {
        if (mCamera == null) {
            return;
        }
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                requestPreviewFrame();
            }
        });
    }

    private void requestPreviewFrame() {
        if (mCamera == null) {
            return;
        }
        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mPreviewCallback != null) {
                    mPreviewCallback.onPreviewFrame(data, mResolution.x, mResolution.y);
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startAutoFocus();
                    }
                }, DELAY_MILLIS);
            }
        });
    }

    public void setPreviewCallback(PreviewCallback callback) {
        mPreviewCallback = callback;
    }


    private void setFlash(Camera.Parameters parameters) {
        parameters.set("flash-value", 2);
        parameters.set("flash-mode", "off");
    }

    private void setZoom(Camera.Parameters parameters) {

        String zoomSupportedString = parameters.get("zoom-supported");
        if (zoomSupportedString != null
                && !Boolean.parseBoolean(zoomSupportedString)) {
            return;
        }

        int tenDesiredZoom = TEN_DESIRED_ZOOM;

        String maxZoomString = parameters.get("max-zoom");
        if (maxZoomString != null) {
            try {
                int tenMaxZoom = (int) (10.0 * Double
                        .parseDouble(maxZoomString));
                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }

        String takingPictureZoomMaxString = parameters
                .get("taking-picture-zoom-max");
        if (takingPictureZoomMaxString != null) {
            try {
                int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }

        String motZoomValuesString = parameters.get("mot-zoom-values");
        if (motZoomValuesString != null) {
            tenDesiredZoom = findBestMotZoomValue(motZoomValuesString,
                    tenDesiredZoom);
        }

        String motZoomStepString = parameters.get("mot-zoom-step");
        if (motZoomStepString != null) {
            try {
                double motZoomStep = Double.parseDouble(motZoomStepString
                        .trim());
                int tenZoomStep = (int) (10.0 * motZoomStep);
                if (tenZoomStep > 1) {
                    tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
                }
            } catch (NumberFormatException nfe) {
                // continue
            }
        }

        // Set zoom. This helps encourage the user to pull back.
        // Some devices like the Behold have a zoom parameter
        if (maxZoomString != null || motZoomValuesString != null) {
            parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
        }

        // Most devices, like the Hero, appear to expose this zoom parameter.
        // It takes on values like "27" which appears to mean 2.7x zoom
        if (takingPictureZoomMaxString != null) {
            parameters.set("taking-picture-zoom", tenDesiredZoom);
        }
    }

    private static int findBestMotZoomValue(CharSequence stringValues,
                                            int tenDesiredZoom) {
        int tenBestValue = 0;
        for (String stringValue : COMMA_PATTERN.split(stringValues)) {
            stringValue = stringValue.trim();
            double value;
            try {
                value = Double.parseDouble(stringValue);
            } catch (NumberFormatException nfe) {
                return tenDesiredZoom;
            }
            int tenValue = (int) (10.0 * value);
            if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom
                    - tenBestValue)) {
                tenBestValue = tenValue;
            }
        }
        return tenBestValue;
    }

    private boolean isSupported(String temp, List<String> list) {
        if (list != null) {
            for (String str : list) {
                if (temp.equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
        }
        mCamera = null;
        mStatus = CameraStatus.DESTROYED;
    }

    public void zoomIn() {
        if (mCamera != null && mCamera.getParameters().isZoomSupported()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getZoom() >= parameters.getMaxZoom()) {
                return;
            }
            parameters.setZoom(parameters.getZoom() + 1);
            mCamera.setParameters(parameters);
        }
    }

    public void zoomOut() {
        if (mCamera != null && mCamera.getParameters().isZoomSupported()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getZoom() <= 0) {
                return;
            }
            parameters.setZoom(parameters.getZoom() - 1);
            mCamera.setParameters(parameters);
        }
    }

    public static class InternalHandler extends Handler {
        InternalHandler(Looper looper) {
            super(looper);
        }
    }

    private static Point getCameraResolution(Camera.Parameters parameters, Point screenResolution) {
        String previewSizeValueString = parameters.get("preview-size-values");
        if (previewSizeValueString == null) {
            previewSizeValueString = parameters.get("preview-size-value");
        }
        Point cameraResolution = null;
        if (previewSizeValueString != null) {
            cameraResolution = findBestPreviewSizeValue(previewSizeValueString,
                    screenResolution);
        }
        if (cameraResolution == null) {
            cameraResolution = new Point((screenResolution.x >> 3) << 3,
                    (screenResolution.y >> 3) << 3);
        }
        return cameraResolution;
    }

    private static Point findBestPreviewSizeValue(
            CharSequence previewSizeValueString, Point screenResolution) {
        int bestX = 0;
        int bestY = 0;
        int diff = Integer.MAX_VALUE;
        //previewSizeValueString为包含所有预览尺寸的字符串
        for (String previewSize : COMMA_PATTERN.split(previewSizeValueString)) {
            previewSize = previewSize.trim();
            int dimPosition = previewSize.indexOf('x');
            if (dimPosition < 0) {
                continue;
            }
            try {
                int newX = Integer.parseInt(previewSize.substring(0, dimPosition));
                int newY = Integer.parseInt(previewSize.substring(dimPosition + 1));
                int newDiff = Math.abs(newX - screenResolution.x) + Math.abs(newY - screenResolution.y);
                if (newDiff == 0) {
                    bestX = newX;
                    bestY = newY;
                    break;
                } else if (newDiff < diff) {
                    bestX = newX;
                    bestY = newY;
                    diff = newDiff;
                }
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }
        if (bestX > 0 && bestY > 0) {
            return new Point(bestX, bestY);
        }
        return null;
    }
}
