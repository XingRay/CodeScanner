package com.ray.lib.camera;

/**
 * @author : leixing
 * @date : 2018-01-05
 * <p>
 * Email       : leixing@hecom.cn
 * Version     : 0.0.1
 * <p>
 * Description : xxx
 */

public interface PreviewCallback {
    void onPreviewFrame(byte[] data, int width, int height);
}
