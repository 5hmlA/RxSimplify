package yun.caimuhao.rxpicker.utils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Smile
 * @time 2017/4/19  上午11:39
 * @desc ${TODD}
 */
public class CameraHelper {

  private static File takeImageFile;

  public static void take(Fragment fragment, int requestCode) {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    takePictureIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    if (takePictureIntent.resolveActivity(fragment.getActivity().getPackageManager()) != null) {
      takeImageFile = new File(Environment.getExternalStorageDirectory(), "/DCIM/camera/");
      takeImageFile = createFile(takeImageFile, "IMG_", ".png");
      Uri uri;
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
        String authorities = ProviderUtil.getFileProviderName(fragment.getContext());
        uri = FileProvider.getUriForFile(fragment.getActivity(), authorities, takeImageFile);
      } else {
        uri = Uri.fromFile(takeImageFile);
      }
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
    }
    fragment.startActivityForResult(takePictureIntent, requestCode);
  }
  public static void take(Activity fragment, int requestCode) {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    takePictureIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    if (takePictureIntent.resolveActivity(fragment.getPackageManager()) != null) {
      takeImageFile = new File(Environment.getExternalStorageDirectory(), "/DCIM/camera/");
      takeImageFile = createFile(takeImageFile, "IMG_", ".png");
      Uri uri;
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
        String authorities = ProviderUtil.getFileProviderName(fragment);
        uri = FileProvider.getUriForFile(fragment, authorities, takeImageFile);
      } else {
        uri = Uri.fromFile(takeImageFile);
      }
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
    }
    fragment.startActivityForResult(takePictureIntent, requestCode);
  }

  public static File getTakeImageFile() {
    return takeImageFile;
  }

  private static File createFile(File folder, String prefix, String suffix) {
    if (!folder.exists() || !folder.isDirectory()) folder.mkdirs();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
    String filename = prefix + dateFormat.format(new Date(System.currentTimeMillis())) + suffix;
    return new File(folder, filename);
  }

  public static void scanPic(Context context, File file) {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    Uri contentUri = Uri.fromFile(file);
    mediaScanIntent.setData(contentUri);
    context.sendBroadcast(mediaScanIntent);
  }
  /**
   * 获取图片的旋转角度
   * @param imagePath 图片的绝对路径
   */
  public static int getBitmapDegree(String imagePath) {
    int degree = 0;
    try {
      // 从指定路径下读取图片，并获取其EXIF信息
      ExifInterface exifInterface = new ExifInterface(imagePath);
      // 获取图片的旋转信息
      int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
              ExifInterface.ORIENTATION_NORMAL);
      switch (orientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          degree = 90;
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          degree = 180;
          break;
        case ExifInterface.ORIENTATION_ROTATE_270:
          degree = 270;
          break;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return degree;
  }
}
