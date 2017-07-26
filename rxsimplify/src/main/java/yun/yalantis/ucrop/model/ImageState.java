package yun.yalantis.ucrop.model;

import android.graphics.RectF;

/**
 * Created by Oleksii Shliama [https://github.com/shliama] on 6/21/16.
 */
public class ImageState {

    private RectF mCropRect;
    private RectF mCurrentImageRect;
    private boolean mIsCropCircle;
    private float mCurrentScale, mCurrentAngle;


    public ImageState(RectF cropRect, RectF currentImageRect, float currentScale, float currentAngle, boolean isCropCircle) {
        mCropRect = cropRect;
        mCurrentImageRect = currentImageRect;
        mCurrentScale = currentScale;
        mCurrentAngle = currentAngle;
        mIsCropCircle = isCropCircle;
    }


    public boolean isCropCircle() {
        return mIsCropCircle;
    }


    public RectF getCropRect() {
        return mCropRect;
    }


    public RectF getCurrentImageRect() {
        return mCurrentImageRect;
    }


    public float getCurrentScale() {
        return mCurrentScale;
    }


    public float getCurrentAngle() {
        return mCurrentAngle;
    }
}
