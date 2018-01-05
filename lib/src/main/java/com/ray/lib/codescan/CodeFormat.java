package com.ray.lib.codescan;


public class CodeFormat {

    private int requestCode = 0;
    /**
     * 条形码
     */
    public static final int QR_CODE = 1;
    /**
     * 二维码
     */
    public static final int BAR_CODE = 2;

    public void add(int code) {
        requestCode = requestCode | code;
    }

    public void set(int code) {
        requestCode = code;
    }

    public int get() {
        return requestCode;
    }
}
