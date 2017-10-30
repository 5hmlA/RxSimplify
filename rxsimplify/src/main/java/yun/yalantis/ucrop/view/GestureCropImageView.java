package yun.yalantis.ucrop.view;

import android.content.Context;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.OverScroller;

import java.util.Arrays;

import yun.yalantis.ucrop.util.RectUtils;
import yun.yalantis.ucrop.util.RotationGestureDetector;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
public class GestureCropImageView extends CropImageView {

    protected static final int DOUBLE_TAP_ZOOM_DURATION = 150;

    protected ScaleGestureDetector mScaleDetector;
    protected RotationGestureDetector mRotateDetector;
    protected GestureDetector mGestureDetector;

    protected float mMidPntX, mMidPntY;

    protected boolean mIsRotateEnabled = true, mIsScaleEnabled = true;
    protected int mDoubleTapScaleSteps = 3;

    public GestureCropImageView(Context context){
        super(context);
    }

    public GestureCropImageView(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }

    public GestureCropImageView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    public GestureCropImageView setScaleEnabled(boolean scaleEnabled){
        mIsScaleEnabled = scaleEnabled;
        return this;
    }

    public boolean isScaleEnabled(){
        return mIsScaleEnabled;
    }

    public GestureCropImageView setRotateEnabled(boolean rotateEnabled){
        mIsRotateEnabled = rotateEnabled;
        return this;
    }

    public boolean isRotateEnabled(){
        return mIsRotateEnabled;
    }

    public GestureCropImageView setDoubleTapScaleSteps(int doubleTapScaleSteps){
        mDoubleTapScaleSteps = doubleTapScaleSteps;
        return this;
    }

    public int getDoubleTapScaleSteps(){
        return mDoubleTapScaleSteps;
    }

    /**
     * If it's ACTION_DOWN event - user touches the screen and all current animation must be canceled.
     * If it's ACTION_UP event - user removed all fingers from the screen and current image position must be corrected.
     * If there are more than 2 fingers - update focal point coordinates.
     * Pass the event to the gesture detectors if those are enabled.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(( event.getAction()&MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_DOWN) {
            cancelAllAnimations();
        }

        if(event.getPointerCount()>1) {
            mMidPntX = ( event.getX(0)+event.getX(1) )/2;
            mMidPntY = ( event.getY(0)+event.getY(1) )/2;
        }

        mGestureDetector.onTouchEvent(event);

        if(mIsScaleEnabled) {
            mScaleDetector.onTouchEvent(event);
        }

        if(mIsRotateEnabled) {
            mRotateDetector.onTouchEvent(event);
        }

        if(( event.getAction()&MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_UP) {
            setImageToWrapCropBounds();
        }
        return true;
    }

    @Override
    protected void init(){
        super.init();
        setupGestureListeners();
    }

    /**
     * This method calculates target scale value for double tap gesture.
     * User is able to zoom the image from min scale value
     * to the max scale value with {@link #mDoubleTapScaleSteps} double taps.
     */
    protected float getDoubleTapTargetScale(){
        return getCurrentScale()*(float)Math.pow(getMaxScale()/getMinScale(), 1.0f/mDoubleTapScaleSteps);
    }

    private void setupGestureListeners(){
        mGestureDetector = new GestureDetector(getContext(), new GestureListener(), null, true);
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        mRotateDetector = new RotationGestureDetector(new RotateListener());
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector){
            postScale(detector.getScaleFactor(), mMidPntX, mMidPntY);
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e){
            GestureCropImageView.this.onSingleTapConfirmed();
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e){
            zoomImageToPosition(getDoubleTapTargetScale(), e.getX(), e.getY(), DOUBLE_TAP_ZOOM_DURATION);
            return super.onDoubleTap(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
            onDrag(distanceX, distanceY);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e){
            GestureCropImageView.this.onLongPress();
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
            FlingRunnable mCurrentFlingRunnable = new FlingRunnable(getContext());
            mCurrentFlingRunnable.fling(getWidth(), getHeight(), -(int)velocityX, -(int)velocityY);
            post(mCurrentFlingRunnable);
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    protected void onSingleTapConfirmed(){
        if(mTransformListener != null && Math.abs(getCurrentScale()-getInitialMinScale())<=0.01) {
            mTransformListener.onClicked(this);
        }
    }

    protected void onLongPress(){
        if(mTransformListener != null) {
            mTransformListener.onLongPress(this);
        }
    }

    protected void onDrag(float distanceX, float distanceY){
        postTranslate(-distanceX, -distanceY);
    }

    private class RotateListener extends RotationGestureDetector.SimpleOnRotationGestureListener {

        @Override
        public boolean onRotation(RotationGestureDetector rotationDetector){
            postRotate(rotationDetector.getAngle(), mMidPntX, mMidPntY);
            return true;
        }

    }

    private class FlingRunnable implements Runnable {

        private final OverScroller mScroller;
        private int mCurrentX, mCurrentY;

        public FlingRunnable(Context context){
            mScroller = new OverScroller(context);
        }

        public void cancelFling(){
            mScroller.forceFinished(true);
        }

        public void fling(int viewWidth, int viewHeight, int velocityX, int velocityY){
            final RectF rect = getDisplayRect();
            if(rect == null) {
                return;
            }

            final int startX = Math.round(-rect.left);
            final int minX, maxX, minY, maxY;

            if(viewWidth<rect.width()) {
                minX = 0;
                maxX = Math.round(rect.width()-viewWidth);
            }else {
                minX = maxX = startX;
            }

            final int startY = Math.round(-rect.top);
            if(viewHeight<rect.height()) {
                minY = 0;
                maxY = Math.round(rect.height()-viewHeight);
            }else {
                minY = maxY = startY;
            }

            mCurrentX = startX;
            mCurrentY = startY;

            // If we actually can move, fling the scroller
            if(startX != maxX || startY != maxY) {
                mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, 0, 0);
            }
        }

        @Override
        public void run(){
            if(mScroller.isFinished()) {
                return; // remaining post that should not be handled
            }

            if(mScroller.computeScrollOffset()) {

                final int newX = mScroller.getCurrX();
                final int newY = mScroller.getCurrY();

                mCurrentImageMatrix.postTranslate(mCurrentX-newX, mCurrentY-newY);
                setImageToWrapCropBounds(false);
                setImageMatrix(mCurrentImageMatrix);
                mCurrentX = newX;
                mCurrentY = newY;

                // Post On animation
                ViewCompat.postOnAnimation(GestureCropImageView.this, this);
            }
        }
    }

    private RectF getDisplayRect(){
        float[] unrotatedImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.length);
        mTempMatrix.mapPoints(unrotatedImageCorners);
        return RectUtils.trapToRect(unrotatedImageCorners);
    }

}
