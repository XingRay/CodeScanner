package com.ray.util;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * @author : leixing
 * @date : 2018-01-04
 * <p>
 * Email       : leixing@hecom.cn
 * Version     : 0.0.1
 * <p>
 * Description : xxx
 */

public class IntentUtil {
    public static String getChooseFilePath(Intent data, Context context) {
        Uri selectedImage = data.getData();
        if (selectedImage == null) {
            return null;
        }
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            return cursor.getString(columnIndex);
        } finally {
            cursor.close();
        }
    }
}
