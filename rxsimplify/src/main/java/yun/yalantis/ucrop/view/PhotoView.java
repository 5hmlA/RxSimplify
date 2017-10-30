package yun.yalantis.ucrop.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;

/**
 * @another 江祖赟
 * @date 2017/10/19 0019.
 */
public class PhotoView extends GestureCropImageView {

    private boolean mDragScale;

    public PhotoView(Context context){
        super(context);
    }

    public PhotoView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public PhotoView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        boolean handled = false;
        if(getDrawable() != null && mIsRotateEnabled || mIsScaleEnabled) {
            ViewParent parent = getParent();
            switch(event.getAction()) {
                case ACTION_DOWN:
                    // First, disable the Parent from intercepting the touch
                    // event
                    if(null != parent) {
                        //缩放小于1则父类处理拦截 否则imageview自己处理 //或者放大滑动到边缘了
                        //                        parent.requestDisallowInterceptTouchEvent(getCurrentScale()>getInitialMinScale());//false父类拦截
                        parent.requestDisallowInterceptTouchEvent(true);//false父类拦截

                    }
                    break;

                case ACTION_MOVE:

            }

            if(( event.getAction()&MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_DOWN) {
                cancelAllAnimations();
            }

            if(event.getPointerCount()>1) {
                mDragScale = true;
                mMidPntX = ( event.getX(0)+event.getX(1) )/2;
                mMidPntY = ( event.getY(0)+event.getY(1) )/2;
            }else {
                mDragScale = false;
            }

            handled = mGestureDetector.onTouchEvent(event);

            if(mIsScaleEnabled) {
                handled = mScaleDetector.onTouchEvent(event);
            }

            if(mIsRotateEnabled) {
                handled = mRotateDetector.onTouchEvent(event);
            }

            if(( event.getAction()&MotionEvent.ACTION_MASK ) == MotionEvent.ACTION_UP) {
                setImageToWrapCropBounds();
            }
        }
        return handled;
    }

    @Override
    protected void onDrag(float distanceX, float distanceY){
        super.onDrag(distanceX, distanceY);
        setImageToWrapCropBounds(false);
        calculateImageIndents();
        ViewParent parent = getParent();
        if(parent != null && !mDragScale) {
            if(mScrollEdge != EDGE_NAN && getCurrentScale()>getInitialMinScale()) {
                zoomImageToPosition(1, getWidth()/2, getHeight()/2, 300);
            }
            parent.requestDisallowInterceptTouchEvent(mScrollEdge == -1);//false父类处理
        }
    }
}
