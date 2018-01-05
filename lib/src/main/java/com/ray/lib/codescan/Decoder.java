package com.ray.lib.codescan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Decoder {
    private final static int DEFAULT_WIDTH = 720;
    private final static int DEFAULT_HEIGHT = 1280;

    static {
        System.loadLibrary("qrscan");
    }

    public static String decodeFromFile(String filename, CodeFormat codeFormat) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap scanBitmap;
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        scanBitmap = BitmapFactory.decodeFile(filename, options);
        options.inJustDecodeBounds = false;

        int heightSampleSize = (int) Math.ceil((double)options.outHeight/DEFAULT_HEIGHT);
        int widhtSampleSize = (int) Math.ceil((double)options.outWidth /DEFAULT_WIDTH);
        int sampleSize = 1;
        if (heightSampleSize >= 1 || widhtSampleSize >= 1 ){
            sampleSize = heightSampleSize > widhtSampleSize? heightSampleSize:widhtSampleSize;
        }

        options.inSampleSize = sampleSize;
        scanBitmap = BitmapFactory.decodeFile(filename, options);
        return getPixelsByBitmap(scanBitmap, codeFormat);
    }

    public static String getPixelsByBitmap(Bitmap bitmap, CodeFormat codeFormat) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = width * height;

        int pixels[] = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        bitmap.recycle();
        if (codeFormat != null) {
            return decodeFileFromJNI(codeFormat.get(), pixels, width, height);
        } else {
            return decodeFileFromJNI(CodeFormat.BAR_CODE | CodeFormat.QR_CODE, pixels, width, height);
        }
    }

    public static String getDecodeResult(CodeFormat codeFormat, byte[] data, int dataWidth,
                                         int dataHeight, int left, int top, int width, int height) {
        if (codeFormat != null) {
            return decodeFromJNI(codeFormat.get(), data, dataWidth, dataHeight, left, top, width, height);
        } else {
            return decodeFromJNI(CodeFormat.BAR_CODE | CodeFormat.QR_CODE, data, dataWidth, dataHeight, left, top, width, height);
        }
    }

    public native static String decodeFromJNI(int decodeCode, byte[] data, int dataWidth,
                                              int dataHeight, int left, int top, int width, int height);

    public native static String decodeFileFromJNI(int decodeCode, int[] pixels, int width, int height);

}
