package yun.caimuhao.rxpicker.widget;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * @another 江祖赟
 * @date 2017/8/25 0025.
 */
public class ViewPagerFixed extends android.support.v4.view.ViewPager {

    private PointF mTdown;

    public ViewPagerFixed(Context context){
        super(context);
    }

    public ViewPagerFixed(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    float xOffset = 0;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event){
        try {
            return super.onInterceptTouchEvent(event);
        }catch(IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev){
        try {
            return super.onTouchEvent(ev);
        }catch(IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
