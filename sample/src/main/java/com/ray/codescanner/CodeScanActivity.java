package com.ray.codescanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.ray.lib.camera.CameraHolder;
import com.ray.lib.camera.PreviewCallback;
import com.ray.lib.codescan.CodeFormat;
import com.ray.lib.codescan.Decoder;
import com.ray.util.IntentUtil;
import com.ray.util.ThreadPools;


/**
 * @author : leixing
 * @date : 2018-01-04
 * <p>
 * Email       : leixing@hecom.cn
 * Version     : 0.0.1
 * <p>
 * Description : xxx
 */

public class CodeScanActivity extends Activity {
    public static final int REQUEST_CODE_CHOOSE_IMG = 100;
    private CameraHolder mCameraHolder;
    private CodeFormat mCodeFormat;

    public static void start(Activity activity, int requestCode) {
        Intent intent = new Intent();
        intent.setClass(activity, CodeScanActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_scan);
        mCodeFormat = new CodeFormat();
        mCodeFormat.add(CodeFormat.BAR_CODE);
        mCodeFormat.add(CodeFormat.QR_CODE);

        SurfaceView surfaceView = findViewById(R.id.sv_camera);
        mCameraHolder = new CameraHolder(getApplicationContext(), surfaceView.getHolder());
        mCameraHolder.setPreviewCallback(new PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] data, final int width, final int height) {
                ThreadPools.getDefault().execute(new Runnable() {
                    @Override
                    public void run() {
                        final String result = Decoder.getDecodeResult(mCodeFormat, data, width, height, 0, 420, height, height);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!TextUtils.isEmpty(result)) {
                                    showHint(result);
                                } else {
                                    showHint("解析失败");
                                }
                            }
                        });
                    }
                });
            }
        });

        findViewById(R.id.bt_choose_img).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_CODE_CHOOSE_IMG);
            }
        });
    }

    private void showHint(String hint) {
        Toast.makeText(this, hint, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                mCameraHolder.zoomIn();
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mCameraHolder.zoomOut();
                return true;
            default:
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE_IMG && resultCode == RESULT_OK && null != data) {
            final String path = IntentUtil.getChooseFilePath(data, this);
            if (TextUtils.isEmpty(path)) {
                showHint("请选择图片");
                return;
            }

            ThreadPools.getDefault().execute(new Runnable() {
                @Override
                public void run() {
                    final String result = Decoder.decodeFromFile(path, mCodeFormat);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!TextUtils.isEmpty(result)) {
                                showHint(result);
                            } else {
                                showHint("解析失败");
                            }
                        }
                    });
                }
            });
        }
    }
}
