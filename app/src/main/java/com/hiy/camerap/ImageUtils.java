package com.hiy.camerap;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author zhishui <a href="mailto:liusd@tuya.com">Contact me.</a>
 * @since 2021/3/30
 */
public class ImageUtils {
    private static final String tag = "ImageUtils";

    public static final boolean saveImage(Context context, Image image) {
        if (image == null) {
            return false;
        }

        int imageFormat = image.getFormat();
        if (imageFormat != ImageFormat.JPEG) {
            Log.d(tag, "onImageAvailable - imageFormat = " + imageFormat);
            return false;
        }


        Image.Plane[] planes = image.getPlanes();
        if (planes.length > 0) {
            ByteBuffer buffer = planes[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);


            File dir = context.getFilesDir();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            SimpleDateFormat formatter = new SimpleDateFormat("YYYY_MM_dd_HH_mm_ss_SSS");
            String filename = formatter.format(new Date());
            File file = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + "camera", filename + ".jpg");
            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                os.write(data);
                os.close();
                ContentResolver contentResolver = context.getContentResolver();
                String retFilePath = MediaStore.Images.Media.insertImage(
                        contentResolver,
                        file.getAbsolutePath(),
                        "name",
                        "desc"
                );
                Log.d(tag, "save to image path = " + retFilePath);
                Uri uri = Uri.fromFile(file);
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        image.close();

        return true;
    }

    public static final void addSystemImageDir(Context context, String dirName) {

    }
}
