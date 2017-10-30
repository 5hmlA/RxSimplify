package yun.yalantis.ucrop.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import yun.picker.simplify.R;
import yun.yalantis.ucrop.callback.CropBoundsChangeListener;
import yun.yalantis.ucrop.callback.TransformListener;
import yun.yalantis.ucrop.util.CubicEasing;
import yun.yalantis.ucrop.util.RectUtils;
import yun.yalantis.ucrop.util.RotationGestureDetector;

import static android.content.ContentValues.TAG;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 * <p/>
 * This class adds crop feature, methods to draw crop guidelines, and keep image in correct state.
 * Also it extends parent class methods to add checks for scale; animating zoom in/out.
 */
public class GestureWraper implements View.OnTouchListener, View.OnLayoutChangeListener {

    public static final int DEFAULT_MAX_BITMAP_SIZE = 0;
    public static final int DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION = 500;
    public static final float DEFAULT_MAX_SCALE_MULTIPLIER = 6.0f;
    public static final float SOURCE_IMAGE_ASPECT_RATIO = 0f;
    public static final float DEFAULT_ASPECT_RATIO = SOURCE_IMAGE_ASPECT_RATIO;
    private static final int RECT_CORNER_POINTS_COORDS = 8;
    private static final int RECT_CENTER_POINT_COORDS = 2;
    private static final int MATRIX_VALUES_COUNT = 9;
    private final float[] mMatrixValues = new float[MATRIX_VALUES_COUNT];
    protected Matrix mCurrentImageMatrix = new Matrix();
    protected final RectF mCropRect = new RectF();

    protected final Matrix mTempMatrix = new Matrix();
    protected final float[] mCurrentImageCorners = new float[RECT_CORNER_POINTS_COORDS];
    protected final float[] mCurrentImageCenter = new float[RECT_CENTER_POINT_COORDS];

    private float mTargetAspectRatio;
    private float mMaxScaleMultiplier = DEFAULT_MAX_SCALE_MULTIPLIER;

    private CropBoundsChangeListener mCropBoundsChangeListener;

    private Runnable mWrapCropBoundsRunnable, mZoomImageToPositionRunnable = null;

    private float mMaxScale, mMinScale;
    protected int mMaxResultImageSizeX = 0, mMaxResultImageSizeY = 0;
    private long mImageToWrapCropBoundsAnimDuration = DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION;
    private View mWraperGestureView;
    protected TransformListener mTransformListener;
    private float[] mInitialImageCorners;
    private float[] mInitialImageCenter;
    protected boolean mBitmapLaidOut = false;
    protected int mThisWidth, mThisHeight;
    private static final int DOUBLE_TAP_ZOOM_DURATION = 150;

    private ScaleGestureDetector mScaleDetector;
    private RotationGestureDetector mRotateDetector;
    private GestureDetector mGestureDetector;

    private float mMidPntX, mMidPntY;

    private boolean mIsRotateEnabled = true, mIsScaleEnabled = true;
    private int mDoubleTapScaleSteps = 3;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            postScale(detector.getScaleFactor(), mMidPntX, mMidPntY);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {


        @Override
        public boolean onDoubleTap(MotionEvent e) {
            zoomImageToPosition(getDoubleTapTargetScale(), e.getX(), e.getY(), DOUBLE_TAP_ZOOM_DURATION);
            if (mTransformListener != null) {
                mTransformListener.onDoubleTap(mWraperGestureView);
            }
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return GestureWraper.this.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public void onLongPress(MotionEvent e){
            if (mTransformListener != null) {
                mTransformListener.onLongPress(mWraperGestureView);
            }
            super.onLongPress(e);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e){
            if (mTransformListener != null) {
                mTransformListener.onClicked(mWraperGestureView);
            }
            return super.onSingleTapConfirmed(e);
        }

    }

    private class RotateListener extends RotationGestureDetector.SimpleOnRotationGestureListener {

        @Override
        public boolean onRotation(RotationGestureDetector rotationDetector) {
            postRotate(rotationDetector.getAngle(), mMidPntX, mMidPntY);
            return true;
        }

    }

    public GestureWraper(View view){
        mWraperGestureView = view;
        mWraperGestureView.setOnTouchListener(this);
        mWraperGestureView.addOnLayoutChangeListener(this);
        setupGestureListeners();
        post(new Runnable() {
            @Override
            public void run(){
                bindViewMatrix(mWraperGestureView.getMatrix());
            }
        });
    }

    private void setupGestureListeners(){
        mGestureDetector = new GestureDetector(getContext(), new GestureListener(), null, true);
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        mRotateDetector = new RotationGestureDetector(new RotateListener());
    }

    private Context getContext(){
        return mWraperGestureView.getContext();
    }

    public void post(Runnable runnable){
        mWraperGestureView.post(runnable);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event){
        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            cancelAllAnimations();
        }

        if (event.getPointerCount() > 1) {
            mMidPntX = (event.getX(0) + event.getX(1)) / 2;
            mMidPntY = (event.getY(0) + event.getY(1)) / 2;
        }

        mGestureDetector.onTouchEvent(event);

        if (mIsScaleEnabled) {
            mScaleDetector.onTouchEvent(event);
        }

        if (mIsRotateEnabled) {
            mRotateDetector.onTouchEvent(event);
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            setImageToWrapCropBounds();
        }
        return true;
    }


    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        postTranslate(-distanceX, -distanceY);
        return true;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom){
        if ((!mBitmapLaidOut)) {
            left = mWraperGestureView.getPaddingLeft();
            top = mWraperGestureView.getPaddingTop();
            right = mWraperGestureView.getWidth() - mWraperGestureView.getPaddingRight();
            bottom = mWraperGestureView.getHeight() - mWraperGestureView.getPaddingBottom();
            mThisWidth = right - left;
            mThisHeight = bottom - top;
            onViewLaidOut();
        }
    }

    /**
     * This method calculates target scale value for double tap gesture.
     * User is able to zoom the image from min scale value
     * to the max scale value with {@link #mDoubleTapScaleSteps} double taps.
     */
    protected float getDoubleTapTargetScale() {
        return getCurrentScale() * (float) Math.pow(getMaxScale() / getMinScale(), 1.0f / mDoubleTapScaleSteps);
    }

    /**
     * When image is laid out {@link #mInitialImageCenter} and {@link #mInitialImageCenter}
     * must be set.
     */
    protected void onViewLaidOut() {
        float w = mWraperGestureView.getMeasuredWidth();
        float h = mWraperGestureView.getMeasuredHeight();

        Log.d(TAG, String.format("Image size: [%d:%d]", (int) w, (int) h));

        RectF initialImageRect = new RectF(0, 0, w, h);
        mInitialImageCorners = RectUtils.getCornersFromRect(initialImageRect);
        mInitialImageCenter = RectUtils.getCenterFromRect(initialImageRect);

        mBitmapLaidOut = true;


        if (mTargetAspectRatio == SOURCE_IMAGE_ASPECT_RATIO) {
            mTargetAspectRatio = w / h;
        }

        int height = (int) (mThisWidth / mTargetAspectRatio);
        if (height > mThisHeight) {
            int width = (int) (mThisHeight * mTargetAspectRatio);
            int halfDiff = (mThisWidth - width) / 2;
            mCropRect.set(halfDiff, 0, width + halfDiff, mThisHeight);
        } else {
            int halfDiff = (mThisHeight - height) / 2;
            mCropRect.set(0, halfDiff, mThisWidth, height + halfDiff);
        }

        calculateImageScaleBounds(w, h);
        setupInitialImagePosition(w, h);

        if (mCropBoundsChangeListener != null) {
            mCropBoundsChangeListener.onCropAspectRatioChanged(mTargetAspectRatio);
        }
        if (mTransformListener != null) {
            mTransformListener.onScale(getCurrentScale());
            mTransformListener.onRotate(getCurrentAngle());
            mTransformListener.onLoadComplete();
        }

    }

    /**
     * This method translates current image.
     *
     * @param deltaX - horizontal shift
     * @param deltaY - vertical shift
     */
    public void postTranslate(float deltaX, float deltaY) {
        if (deltaX != 0 || deltaY != 0) {
            mCurrentImageMatrix.postTranslate(deltaX, deltaY);
            bindViewMatrix(mCurrentImageMatrix);
        }
    }

    /**
     * This method scales current image.
     *
     * @param deltaScale - scale value
     * @param px         - scale center X
     * @param py         - scale center Y
     */
    public void toPostScale(float deltaScale, float px, float py) {
        if (deltaScale != 0) {
            mCurrentImageMatrix.postScale(deltaScale, deltaScale, px, py);
            bindViewMatrix(mCurrentImageMatrix);
            if (mTransformListener != null) {
                mTransformListener.onScale(getMatrixScale(mCurrentImageMatrix));
            }
        }
    }

    public void bindViewMatrix(Matrix matrix) {
//        mWraperGestureView.getMatrix()
        ( (ImageView)mWraperGestureView ).setImageMatrix(matrix);
        mCurrentImageMatrix.set(matrix);
        updateCurrentImagePoints();
    }


    /**
     * This method updates current image corners and center points that are stored in
     * {@link #mCurrentImageCorners} and {@link #mCurrentImageCenter} arrays.
     * Those are used for several calculations.
     */
    private void updateCurrentImagePoints() {
        mCurrentImageMatrix.mapPoints(mCurrentImageCorners, mInitialImageCorners);
        mCurrentImageMatrix.mapPoints(mCurrentImageCenter, mInitialImageCenter);
    }

    /**
     * This method rotates current image.
     *
     * @param deltaAngle - rotation angle
     * @param px         - rotation center X
     * @param py         - rotation center Y
     */
    public void postRotate(float deltaAngle, float px, float py) {
        if (deltaAngle != 0) {
            mCurrentImageMatrix.postRotate(deltaAngle, px, py);
            bindViewMatrix(mCurrentImageMatrix);
            if (mTransformListener != null) {
                mTransformListener.onRotate(getMatrixAngle(mCurrentImageMatrix));
            }
        }
    }

    /**
     * @return - current image scale value.
     * [1.0f - for original image, 2.0f - for 200% scaled image, etc.]
     */
    public float getCurrentScale() {
        return getMatrixScale(mCurrentImageMatrix);
    }

    /**
     * This method calculates scale value for given Matrix object.
     */
    public float getMatrixScale(@NonNull Matrix matrix) {
        return (float) Math.sqrt(Math.pow(getMatrixValue(matrix, Matrix.MSCALE_X), 2)
                + Math.pow(getMatrixValue(matrix, Matrix.MSKEW_Y), 2));
    }


    /**
     * This method returns Matrix value for given index.
     *
     * @param matrix     - valid Matrix object
     * @param valueIndex - index of needed value. See {@link Matrix#MSCALE_X} and others.
     * @return - matrix value for index
     */
    protected float getMatrixValue(@NonNull Matrix matrix, @IntRange(from = 0, to = MATRIX_VALUES_COUNT) int valueIndex) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[valueIndex];
    }

    /**
     * This method logs given matrix X, Y, scale, and angle values.
     * Can be used for debug.
     */
    @SuppressWarnings("unused")
    protected void printMatrix(@NonNull String logPrefix, @NonNull Matrix matrix) {
        float x = getMatrixValue(matrix, Matrix.MTRANS_X);
        float y = getMatrixValue(matrix, Matrix.MTRANS_Y);
        float rScale = getMatrixScale(matrix);
        float rAngle = getMatrixAngle(matrix);
        Log.d(TAG, logPrefix + ": matrix: { x: " + x + ", y: " + y + ", scale: " + rScale + ", angle: " + rAngle + " }");
    }


    /**
     * @return - current image rotation angle.
     */
    public float getCurrentAngle() {
        return getMatrixAngle(mCurrentImageMatrix);
    }

    /**
     * This method calculates rotation angle for given Matrix object.
     */
    public float getMatrixAngle(@NonNull Matrix matrix) {
        return (float) -(Math.atan2(getMatrixValue(matrix, Matrix.MSKEW_X),
                getMatrixValue(matrix, Matrix.MSCALE_X)) * (180 / Math.PI));
    }

    /**
     * @return - maximum scale value for current image and crop ratio
     */
    public float getMaxScale() {
        return mMaxScale;
    }

    /**
     * @return - minimum scale value for current image and crop ratio
     */
    public float getMinScale() {
        return mMinScale;
    }

    /**
     * @return - aspect ratio for crop bounds
     */
    public float getTargetAspectRatio() {
        return mTargetAspectRatio;
    }

    /**
     * Updates current crop rectangle with given. Also recalculates image properties and position
     * to fit new crop rectangle.
     *
     * @param cropRect - new crop rectangle
     */
    public void setCropRect(RectF cropRect) {
        mTargetAspectRatio = cropRect.width() / cropRect.height();
        mCropRect.set(cropRect.left - getPaddingLeft(), cropRect.top - getPaddingTop(),
                cropRect.right - getPaddingRight(), cropRect.bottom - getPaddingBottom());
        calculateImageScaleBounds();
        setImageToWrapCropBounds();
    }

    private float getPaddingBottom(){
        return mWraperGestureView.getPaddingBottom();
    }

    private float getPaddingTop(){
        return mWraperGestureView.getPaddingTop();
    }

    private float getPaddingRight(){
        return mWraperGestureView.getPaddingRight();
    }

    private float getPaddingLeft(){
        return mWraperGestureView.getPaddingLeft();
    }

    /**
     * This method sets aspect ratio for crop bounds.
     * If {@link #SOURCE_IMAGE_ASPECT_RATIO} value is passed - aspect ratio is calculated
     * based on current image width and height.
     *
     * @param targetAspectRatio - aspect ratio for image crop (e.g. 1.77(7) for 16:9)
     */
    public void setTargetAspectRatio(float targetAspectRatio) {
        if (mWraperGestureView == null) {
            mTargetAspectRatio = targetAspectRatio;
            return;
        }

        if (targetAspectRatio == SOURCE_IMAGE_ASPECT_RATIO) {
            mTargetAspectRatio = mWraperGestureView.getMeasuredWidth() / (float) mWraperGestureView.getMeasuredHeight();
        } else {
            mTargetAspectRatio = targetAspectRatio;
        }

        if (mCropBoundsChangeListener != null) {
            mCropBoundsChangeListener.onCropAspectRatioChanged(mTargetAspectRatio);
        }
    }

    @Nullable
    public CropBoundsChangeListener getCropBoundsChangeListener() {
        return mCropBoundsChangeListener;
    }

    public void setCropBoundsChangeListener(@Nullable CropBoundsChangeListener cropBoundsChangeListener) {
        mCropBoundsChangeListener = cropBoundsChangeListener;
    }

    /**
     * This method sets maximum width for resulting cropped image
     *
     * @param maxResultImageSizeX - size in pixels
     */
    public void setMaxResultImageSizeX(@IntRange(from = 10) int maxResultImageSizeX) {
        mMaxResultImageSizeX = maxResultImageSizeX;
    }

    /**
     * This method sets maximum width for resulting cropped image
     *
     * @param maxResultImageSizeY - size in pixels
     */
    public void setMaxResultImageSizeY(@IntRange(from = 10) int maxResultImageSizeY) {
        mMaxResultImageSizeY = maxResultImageSizeY;
    }

    /**
     * This method sets animation duration for image to wrap the crop bounds
     *
     * @param imageToWrapCropBoundsAnimDuration - duration in milliseconds
     */
    public void setImageToWrapCropBoundsAnimDuration(@IntRange(from = 100) long imageToWrapCropBoundsAnimDuration) {
        if (imageToWrapCropBoundsAnimDuration > 0) {
            mImageToWrapCropBoundsAnimDuration = imageToWrapCropBoundsAnimDuration;
        } else {
            throw new IllegalArgumentException("Animation duration cannot be negative value.");
        }
    }

    /**
     * This method sets multiplier that is used to calculate max image scale from min image scale.
     *
     * @param maxScaleMultiplier - (minScale * maxScaleMultiplier) = maxScale
     */
    public void setMaxScaleMultiplier(float maxScaleMultiplier) {
        mMaxScaleMultiplier = maxScaleMultiplier;
    }

    /**
     * This method scales image down for given value related to image center.
     */
    public void zoomOutImage(float deltaScale) {
        zoomOutImage(deltaScale, mCropRect.centerX(), mCropRect.centerY());
    }

    /**
     * This method scales image down for given value related given coords (x, y).
     */
    public void zoomOutImage(float scale, float centerX, float centerY) {
        if (scale >= getMinScale()) {
            postScale(scale / getCurrentScale(), centerX, centerY);
        }
    }

    /**
     * This method scales image up for given value related to image center.
     */
    public void zoomInImage(float deltaScale) {
        zoomInImage(deltaScale, mCropRect.centerX(), mCropRect.centerY());
    }

    /**
     * This method scales image up for given value related to given coords (x, y).
     */
    public void zoomInImage(float scale, float centerX, float centerY) {
        if (scale <= getMaxScale()) {
            postScale(scale / getCurrentScale(), centerX, centerY);
        }
    }

    /**
     * This method changes image scale for given value related to point (px, py) but only if
     * resulting scale is in min/max bounds.
     *
     * @param deltaScale - scale value
     * @param px         - scale center X
     * @param py         - scale center Y
     */
    public void postScale(float deltaScale, float px, float py) {
        if (deltaScale > 1 && getCurrentScale() * deltaScale <= getMaxScale()) {
            toPostScale(deltaScale, px, py);
        } else if (deltaScale < 1 && getCurrentScale() * deltaScale >= getMinScale()) {
            toPostScale(deltaScale, px, py);
        }
    }

    /**
     * This method rotates image for given angle related to the image center.
     *
     * @param deltaAngle - angle to rotate
     */
    public void postRotate(float deltaAngle) {
        postRotate(deltaAngle, mCropRect.centerX(), mCropRect.centerY());
    }

    /**
     * This method cancels all current Runnable objects that represent animations.
     */
    public void cancelAllAnimations() {
        mWraperGestureView.removeCallbacks(mWrapCropBoundsRunnable);
        mWraperGestureView.removeCallbacks(mZoomImageToPositionRunnable);
    }

    public void setImageToWrapCropBounds() {
        setImageToWrapCropBounds(true);
    }

    /**
     * If image doesn't fill the crop bounds it must be translated and scaled properly to fill those.
     * <p/>
     * Therefore this method calculates delta X, Y and scale values and passes them to the
     * {@link WrapCropBoundsRunnable} which animates image.
     * Scale value must be calculated only if image won't fill the crop bounds after it's translated to the
     * crop bounds rectangle center. Using temporary variables this method checks this case.
     */
    public void setImageToWrapCropBounds(boolean animate) {
        if (mBitmapLaidOut && !isImageWrapCropBounds()) {

            float currentX = mCurrentImageCenter[0];
            float currentY = mCurrentImageCenter[1];
            float currentScale = getCurrentScale();

            float deltaX = mCropRect.centerX() - currentX;
            float deltaY = mCropRect.centerY() - currentY;
            float deltaScale = 0;

            mTempMatrix.reset();
            mTempMatrix.setTranslate(deltaX, deltaY);

            final float[] tempCurrentImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
            mTempMatrix.mapPoints(tempCurrentImageCorners);

            boolean willImageWrapCropBoundsAfterTranslate = isImageWrapCropBounds(tempCurrentImageCorners);

            if (willImageWrapCropBoundsAfterTranslate) {
                final float[] imageIndents = calculateImageIndents();
                deltaX = -(imageIndents[0] + imageIndents[2]);
                deltaY = -(imageIndents[1] + imageIndents[3]);
            } else {
                RectF tempCropRect = new RectF(mCropRect);
                mTempMatrix.reset();
                mTempMatrix.setRotate(getCurrentAngle());
                mTempMatrix.mapRect(tempCropRect);

                final float[] currentImageSides = RectUtils.getRectSidesFromCorners(mCurrentImageCorners);

                deltaScale = Math.max(tempCropRect.width() / currentImageSides[0],
                        tempCropRect.height() / currentImageSides[1]);
                deltaScale = deltaScale * currentScale - currentScale;
            }

            if (animate) {
                post(mWrapCropBoundsRunnable = new WrapCropBoundsRunnable(
                        GestureWraper.this, mImageToWrapCropBoundsAnimDuration, currentX, currentY, deltaX, deltaY,
                        currentScale, deltaScale, willImageWrapCropBoundsAfterTranslate));
            } else {
                postTranslate(deltaX, deltaY);
                if (!willImageWrapCropBoundsAfterTranslate) {
                    zoomInImage(currentScale + deltaScale, mCropRect.centerX(), mCropRect.centerY());
                }
            }
        }
    }

    /**
     * First, un-rotate image and crop rectangles (make image rectangle axis-aligned).
     * Second, calculate deltas between those rectangles sides.
     * Third, depending on delta (its sign) put them or zero inside an array.
     * Fourth, using Matrix, rotate back those points (indents).
     *
     * @return - the float array of image indents (4 floats) - in this order [left, top, right, bottom]
     */
    private float[] calculateImageIndents() {
        mTempMatrix.reset();
        mTempMatrix.setRotate(-getCurrentAngle());

        float[] unrotatedImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
        float[] unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRect);

        mTempMatrix.mapPoints(unrotatedImageCorners);
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners);

        RectF unrotatedImageRect = RectUtils.trapToRect(unrotatedImageCorners);
        RectF unrotatedCropRect = RectUtils.trapToRect(unrotatedCropBoundsCorners);

        float deltaLeft = unrotatedImageRect.left - unrotatedCropRect.left;
        float deltaTop = unrotatedImageRect.top - unrotatedCropRect.top;
        float deltaRight = unrotatedImageRect.right - unrotatedCropRect.right;
        float deltaBottom = unrotatedImageRect.bottom - unrotatedCropRect.bottom;

        float indents[] = new float[4];
        indents[0] = (deltaLeft > 0) ? deltaLeft : 0;
        indents[1] = (deltaTop > 0) ? deltaTop : 0;
        indents[2] = (deltaRight < 0) ? deltaRight : 0;
        indents[3] = (deltaBottom < 0) ? deltaBottom : 0;

        mTempMatrix.reset();
        mTempMatrix.setRotate(getCurrentAngle());
        mTempMatrix.mapPoints(indents);

        return indents;
    }


    /**
     * This method checks whether current image fills the crop bounds.
     */
    protected boolean isImageWrapCropBounds() {
        return isImageWrapCropBounds(mCurrentImageCorners);
    }

    /**
     * This methods checks whether a rectangle that is represented as 4 corner points (8 floats)
     * fills the crop bounds rectangle.
     *
     * @param imageCorners - corners of a rectangle
     * @return - true if it wraps crop bounds, false - otherwise
     */
    protected boolean isImageWrapCropBounds(float[] imageCorners) {
        mTempMatrix.reset();
        mTempMatrix.setRotate(-getCurrentAngle());

        float[] unrotatedImageCorners = Arrays.copyOf(imageCorners, imageCorners.length);
        mTempMatrix.mapPoints(unrotatedImageCorners);

        float[] unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRect);
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners);

        return RectUtils.trapToRect(unrotatedImageCorners).contains(RectUtils.trapToRect(unrotatedCropBoundsCorners));
    }

    /**
     * This method changes image scale (animating zoom for given duration), related to given center (x,y).
     *
     * @param scale      - target scale
     * @param centerX    - scale center X
     * @param centerY    - scale center Y
     * @param durationMs - zoom animation duration
     */
    protected void zoomImageToPosition(float scale, float centerX, float centerY, long durationMs) {
        if (scale > getMaxScale()) {
            scale = getMinScale();
        }

        final float oldScale = getCurrentScale();
        final float deltaScale = scale - oldScale;

        post(mZoomImageToPositionRunnable = new ZoomImageToPosition(GestureWraper.this,
                durationMs, oldScale, deltaScale, centerX, centerY));
    }

    private void calculateImageScaleBounds() {
        if (mWraperGestureView == null) {
            return;
        }
        calculateImageScaleBounds(mWraperGestureView.getMeasuredWidth(), mWraperGestureView.getMeasuredHeight());
    }

    /**
     * This method calculates image minimum and maximum scale values for current {@link #mCropRect}.
     *
     * @param drawableWidth  - image width
     * @param drawableHeight - image height
     */
    private void calculateImageScaleBounds(float drawableWidth, float drawableHeight) {
        float widthScale = Math.min(mCropRect.width() / drawableWidth, mCropRect.width() / drawableHeight);
        float heightScale = Math.min(mCropRect.height() / drawableHeight, mCropRect.height() / drawableWidth);

        mMinScale = Math.min(widthScale, heightScale);
        mMaxScale = mMinScale * mMaxScaleMultiplier;
    }

    /**
     * This method calculates initial image position so it is positioned properly.
     * Then it sets those values to the current image matrix.
     *
     * @param drawableWidth  - image width
     * @param drawableHeight - image height
     */
    private void setupInitialImagePosition(float drawableWidth, float drawableHeight) {
        float cropRectWidth = mCropRect.width();
        float cropRectHeight = mCropRect.height();

        float widthScale = mCropRect.width() / drawableWidth;
        float heightScale = mCropRect.height() / drawableHeight;

        float initialMinScale = Math.max(widthScale, heightScale);

        float tw = (cropRectWidth - drawableWidth * initialMinScale) / 2.0f + mCropRect.left;
        float th = (cropRectHeight - drawableHeight * initialMinScale) / 2.0f + mCropRect.top;

        mCurrentImageMatrix.reset();
        mCurrentImageMatrix.postScale(initialMinScale, initialMinScale);
        mCurrentImageMatrix.postTranslate(tw, th);
        bindViewMatrix(mCurrentImageMatrix);
    }

    /**
     * This method extracts all needed values from the styled attributes.
     * Those are used to configure the view.
     */
    @SuppressWarnings("deprecation")
    protected void processStyledAttributes(@NonNull TypedArray a) {
        float targetAspectRatioX = Math.abs(a.getFloat(R.styleable.ucrop_UCropView_ucrop_aspect_ratio_x, DEFAULT_ASPECT_RATIO));
        float targetAspectRatioY = Math.abs(a.getFloat(R.styleable.ucrop_UCropView_ucrop_aspect_ratio_y, DEFAULT_ASPECT_RATIO));

        if (targetAspectRatioX == SOURCE_IMAGE_ASPECT_RATIO || targetAspectRatioY == SOURCE_IMAGE_ASPECT_RATIO) {
            mTargetAspectRatio = SOURCE_IMAGE_ASPECT_RATIO;
        } else {
            mTargetAspectRatio = targetAspectRatioX / targetAspectRatioY;
        }
    }


    /**
     * This Runnable is used to animate an image so it fills the crop bounds entirely.
     * Given values are interpolated during the animation time.
     * Runnable can be terminated either vie {@link #cancelAllAnimations()} method
     * or when certain conditions inside {@link WrapCropBoundsRunnable#run()} method are triggered.
     */
    private static class WrapCropBoundsRunnable implements Runnable {

        private final WeakReference<GestureWraper> mCropImageView;

        private final long mDurationMs, mStartTime;
        private final float mOldX, mOldY;
        private final float mCenterDiffX, mCenterDiffY;
        private final float mOldScale;
        private final float mDeltaScale;
        private final boolean mWillBeImageInBoundsAfterTranslate;

        public WrapCropBoundsRunnable(GestureWraper cropImageView,
                                      long durationMs,
                                      float oldX, float oldY,
                                      float centerDiffX, float centerDiffY,
                                      float oldScale, float deltaScale,
                                      boolean willBeImageInBoundsAfterTranslate) {

            mCropImageView = new WeakReference<>(cropImageView);

            mDurationMs = durationMs;
            mStartTime = System.currentTimeMillis();
            mOldX = oldX;
            mOldY = oldY;
            mCenterDiffX = centerDiffX;
            mCenterDiffY = centerDiffY;
            mOldScale = oldScale;
            mDeltaScale = deltaScale;
            mWillBeImageInBoundsAfterTranslate = willBeImageInBoundsAfterTranslate;
        }

        @Override
        public void run() {
            GestureWraper cropImageView = mCropImageView.get();
            if (cropImageView == null) {
                return;
            }

            long now = System.currentTimeMillis();
            float currentMs = Math.min(mDurationMs, now - mStartTime);

            float newX = CubicEasing.easeOut(currentMs, 0, mCenterDiffX, mDurationMs);
            float newY = CubicEasing.easeOut(currentMs, 0, mCenterDiffY, mDurationMs);
            float newScale = CubicEasing.easeInOut(currentMs, 0, mDeltaScale, mDurationMs);

            if (currentMs < mDurationMs) {
                cropImageView.postTranslate(newX - (cropImageView.mCurrentImageCenter[0] - mOldX), newY - (cropImageView.mCurrentImageCenter[1] - mOldY));
                if (!mWillBeImageInBoundsAfterTranslate) {
                    cropImageView.zoomInImage(mOldScale + newScale, cropImageView.mCropRect.centerX(), cropImageView.mCropRect.centerY());
                }
                if (!cropImageView.isImageWrapCropBounds()) {
                    cropImageView.post(this);
                }
            }
        }
    }

    /**
     * This Runnable is used to animate an image zoom.
     * Given values are interpolated during the animation time.
     * Runnable can be terminated either vie {@link #cancelAllAnimations()} method
     * or when certain conditions inside {@link ZoomImageToPosition#run()} method are triggered.
     */
    private static class ZoomImageToPosition implements Runnable {

        private final WeakReference<GestureWraper> mCropImageView;

        private final long mDurationMs, mStartTime;
        private final float mOldScale;
        private final float mDeltaScale;
        private final float mDestX;
        private final float mDestY;

        public ZoomImageToPosition(GestureWraper cropImageView,
                                   long durationMs,
                                   float oldScale, float deltaScale,
                                   float destX, float destY) {

            mCropImageView = new WeakReference<>(cropImageView);

            mStartTime = System.currentTimeMillis();
            mDurationMs = durationMs;
            mOldScale = oldScale;
            mDeltaScale = deltaScale;
            mDestX = destX;
            mDestY = destY;
        }

        @Override
        public void run() {
            GestureWraper cropImageView = mCropImageView.get();
            if (cropImageView == null) {
                return;
            }

            long now = System.currentTimeMillis();
            float currentMs = Math.min(mDurationMs, now - mStartTime);
            float newScale = CubicEasing.easeInOut(currentMs, 0, mDeltaScale, mDurationMs);

            if (currentMs < mDurationMs) {
                cropImageView.zoomInImage(mOldScale + newScale, mDestX, mDestY);
                cropImageView.post(this);
            } else {
                cropImageView.setImageToWrapCropBounds();
            }
        }
    }

    public GestureWraper setTransformListener(TransformListener transformListener){
        mTransformListener = transformListener;
        return this;
    }

    public GestureWraper setScaleEnabled(boolean scaleEnabled) {
        mIsScaleEnabled = scaleEnabled;
        return this;
    }

    public boolean isScaleEnabled() {
        return mIsScaleEnabled;
    }

    public GestureWraper setRotateEnabled(boolean rotateEnabled) {
        mIsRotateEnabled = rotateEnabled;
        return this;
    }

    public boolean isRotateEnabled() {
        return mIsRotateEnabled;
    }

    public GestureWraper setDoubleTapScaleSteps(int doubleTapScaleSteps) {
        mDoubleTapScaleSteps = doubleTapScaleSteps;
        return this;}

    public int getDoubleTapScaleSteps() {
        return mDoubleTapScaleSteps;
    }

}
