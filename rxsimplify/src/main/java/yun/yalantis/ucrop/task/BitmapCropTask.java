package yun.yalantis.ucrop.task;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import yun.yalantis.ucrop.callback.BitmapCropCallback;
import yun.yalantis.ucrop.model.CropParameters;
import yun.yalantis.ucrop.model.ExifInfo;
import yun.yalantis.ucrop.model.ImageState;
import yun.yalantis.ucrop.util.BitmapLoadUtils;
import yun.yalantis.ucrop.util.FileUtils;
import yun.yalantis.ucrop.util.ImageHeaderParser;

/**
 * Crops part of image that fills the crop bounds.
 * <p/>
 * First image is downscaled if max size was set and if resulting image is larger that max size.
 * Then image is rotated accordingly.
 * Finally new Bitmap object is created and saved to file.
 */
public class BitmapCropTask extends AsyncTask<Void,Void,Throwable> {

    private static final String TAG = "BitmapCropTask";

    private final WeakReference<Context> mContext;

    private Bitmap mViewBitmap;

    private final RectF mCropRect;
    private final RectF mCurrentImageRect;

    private float mCurrentScale, mCurrentAngle;
    private final int mMaxResultImageSizeX, mMaxResultImageSizeY;

    private final Bitmap.CompressFormat mCompressFormat;
    private final int mCompressQuality;
    private String mImageInputPath, mImageOutputPath;
    private final ExifInfo mExifInfo;
    private final BitmapCropCallback mCropCallback;

    private int mCroppedImageWidth, mCroppedImageHeight;
    private int cropOffsetX, cropOffsetY;
    private final boolean mCropCircle;


    public BitmapCropTask(
            @NonNull Context context,
            @Nullable Bitmap viewBitmap,
            @NonNull ImageState imageState,
            @NonNull CropParameters cropParameters, @Nullable BitmapCropCallback cropCallback){

        mContext = new WeakReference<>(context);

        mViewBitmap = viewBitmap;
        mCropRect = imageState.getCropRect();
        mCurrentImageRect = imageState.getCurrentImageRect();
        mCropCircle = imageState.isCropCircle();
        mCurrentScale = imageState.getCurrentScale();
        mCurrentAngle = imageState.getCurrentAngle();
        mMaxResultImageSizeX = cropParameters.getMaxResultImageSizeX();
        mMaxResultImageSizeY = cropParameters.getMaxResultImageSizeY();

        mCompressFormat = cropParameters.getCompressFormat();
        mCompressQuality = cropParameters.getCompressQuality();

        mImageInputPath = cropParameters.getImageInputPath();
        mImageOutputPath = cropParameters.getImageOutputPath();
        mExifInfo = cropParameters.getExifInfo();

        mCropCallback = cropCallback;
    }


    @Override
    @Nullable
    protected Throwable doInBackground(Void... params){
        if(mViewBitmap == null) {
            return new NullPointerException("ViewBitmap is null");
        }else if(mViewBitmap.isRecycled()) {
            return new NullPointerException("ViewBitmap is recycled");
        }else if(mCurrentImageRect.isEmpty()) {
            return new NullPointerException("CurrentImageRect is empty");
        }

        try {
            crop();
            mViewBitmap = null;
        }catch(Throwable throwable) {
            return throwable;
        }

        return null;
    }


    private boolean crop() throws IOException{
        // Downsize if needed
        if(mMaxResultImageSizeX>0 && mMaxResultImageSizeY>0) {
            float cropWidth = mCropRect.width()/mCurrentScale;
            float cropHeight = mCropRect.height()/mCurrentScale;

            if(cropWidth>mMaxResultImageSizeX || cropHeight>mMaxResultImageSizeY) {

                float scaleX = mMaxResultImageSizeX/cropWidth;
                float scaleY = mMaxResultImageSizeY/cropHeight;
                float resizeScale = Math.min(scaleX, scaleY);

                Bitmap resizedBitmap = Bitmap
                        .createScaledBitmap(mViewBitmap, Math.round(mViewBitmap.getWidth()*resizeScale),
                                Math.round(mViewBitmap.getHeight()*resizeScale), false);
                if(mViewBitmap != resizedBitmap) {
                    mViewBitmap.recycle();
                }
                mViewBitmap = resizedBitmap;

                mCurrentScale /= resizeScale;
            }
        }

        // Rotate if needed
        if(mCurrentAngle != 0) {
            Matrix tempMatrix = new Matrix();
            tempMatrix.setRotate(mCurrentAngle, mViewBitmap.getWidth()/2, mViewBitmap.getHeight()/2);

            //BitmapRegionDecoder regionDecoder = BitmapRegionDecoder.newInstance(mImageInputPath, false);

            Bitmap rotatedBitmap = Bitmap
                    .createBitmap(mViewBitmap, 0, 0, mViewBitmap.getWidth(), mViewBitmap.getHeight(), tempMatrix, true);
            if(mViewBitmap != rotatedBitmap) {
                mViewBitmap.recycle();
            }
            mViewBitmap = rotatedBitmap;
        }

        cropOffsetX = Math.round(( mCropRect.left-mCurrentImageRect.left )/mCurrentScale);
        cropOffsetY = Math.round(( mCropRect.top-mCurrentImageRect.top )/mCurrentScale);
        mCroppedImageWidth = Math.round(mCropRect.width()/mCurrentScale);
        mCroppedImageHeight = Math.round(mCropRect.height()/mCurrentScale);

        boolean shouldCrop = shouldCrop(mCroppedImageWidth, mCroppedImageHeight);
        Log.i(TAG, "Should crop: "+shouldCrop);

        if(shouldCrop) {
            ExifInterface originalExif = new ExifInterface(mImageInputPath);
            saveImage(getCircleImageIfNeed(
                    Bitmap.createBitmap(mViewBitmap, cropOffsetX, cropOffsetY, mCroppedImageWidth,
                            mCroppedImageHeight)));
            //saveImage(mViewBitmap);
            if(mCompressFormat.equals(Bitmap.CompressFormat.JPEG)) {
                ImageHeaderParser.copyExif(originalExif, mCroppedImageWidth, mCroppedImageHeight, mImageOutputPath);
            }
            return true;
        }else {
            FileUtils.copyFile(mImageInputPath, mImageOutputPath);
            return false;
        }
    }


    public Bitmap getCircleImageIfNeed(Bitmap source){
        if(!mCropCircle) {
            return source;
        }
        //int size = Math.min(source.getWidth(), source.getHeight());
        //Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        //Canvas canvas = new Canvas(bitmap);
        //Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //float r = size / 2f;
        //canvas.drawCircle(r, r, r, paint);
        //paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        //canvas.drawBitmap(source, 0, 0, paint);
        //paint.setXfermode(null);
        //source.recycle();
        //return bitmap;
        return getRCdImage31(source, Integer.MAX_VALUE);
    }


    public static Bitmap getRCdImage31(Bitmap source, float corner){
        float filterRadius = 0;
        float btLeft = 0;
        float btTop = 0;
        if(null == source) {
            Log.e(TAG, "the srcBitmap is null");
            return null;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        RectF rcBg = new RectF();
        if(width>height) {
            if(corner>height/2f) {
                float extra = corner-height/2f;
                btLeft -= extra = Math.min(extra, width/2f-height/2f);
                width -= 2*extra;
            }
            corner = Math.min(corner, height/2f);
            rcBg.set(filterRadius, filterRadius, width-filterRadius, height-filterRadius);
        }else {
            if(corner>width/2f) {
                float extra = corner-width/2f;
                btTop -= extra = Math.min(extra, height/2f-width/2f);
                height -= 2*extra;
            }
            corner = Math.min(corner, width/2f);
            rcBg.set(filterRadius, filterRadius, width-filterRadius, height-filterRadius);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if(filterRadius>0) {
            paint.setMaskFilter(new BlurMaskFilter(filterRadius, BlurMaskFilter.Blur.NORMAL));
        }
        paint.setDither(true);
        canvas.drawRoundRect(rcBg, corner, corner, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, btLeft, btTop, paint);//btLeft,btTop坐标表示画在画布的哪个位置而不是 画bitmap中的哪个位置
        paint.setXfermode(null);
        source.recycle();
        return bitmap;
    }


    private void saveImage(@NonNull Bitmap croppedBitmap) throws FileNotFoundException{
        Context context = mContext.get();
        if(context == null) {
            return;
        }
        OutputStream outputStream = null;
        try {
            outputStream = context.getContentResolver().openOutputStream(Uri.fromFile(new File(mImageOutputPath)));
            croppedBitmap.compress(mCompressFormat, mCompressQuality, outputStream);
            croppedBitmap.recycle();
        }finally {
            BitmapLoadUtils.close(outputStream);
        }
    }


    /**
     * Check whether an image should be cropped at all or just file can be copied to the destination path.
     * For each 1000 pixels there is one pixel of error due to matrix calculations etc.
     *
     * @param width
     *         - crop area width
     * @param height
     *         - crop area height
     * @return - true if image must be cropped, false - if original image fits requirements
     */
    private boolean shouldCrop(int width, int height){
        int pixelError = mCropCircle ? -1 : 1;
//        pixelError += Math.round(Math.max(width, height)/1000f);
        return ( mMaxResultImageSizeX>0 && mMaxResultImageSizeY>0 ) || Math
                .abs(mCropRect.left-mCurrentImageRect.left)>pixelError || Math
                .abs(mCropRect.top-mCurrentImageRect.top)>pixelError || Math
                .abs(mCropRect.bottom-mCurrentImageRect.bottom)>pixelError || Math
                .abs(mCropRect.right-mCurrentImageRect.right)>pixelError;
    }


    @Override
    protected void onPostExecute(@Nullable Throwable t){
        if(mCropCallback != null) {
            if(t == null) {
                Uri uri = Uri.fromFile(new File(mImageOutputPath));
                mCropCallback.onBitmapCropped(uri, cropOffsetX, cropOffsetY, mCroppedImageWidth, mCroppedImageHeight);
            }else {
                mCropCallback.onCropFailure(t);
            }
        }
    }
}
