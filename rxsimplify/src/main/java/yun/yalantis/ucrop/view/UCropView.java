package yun.yalantis.ucrop.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import yun.yalantis.ucrop.callback.CropBoundsChangeListener;
import yun.yalantis.ucrop.callback.OverlayViewChangeListener;

public class UCropView extends FrameLayout {

    private final GestureCropImageView mGestureCropImageView;
    private final OverlayView mViewOverlay;

    public UCropView(Context context ){
        this(context, null, 0);
    }

    public UCropView(Context context, AttributeSet attrs){
        this(context, attrs, 0);
    }

    public UCropView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(yun.picker.simplify.R.layout.ucrop_view, this, true);
        mGestureCropImageView = (GestureCropImageView)findViewById(yun.picker.simplify.R.id.image_view_crop);
        mViewOverlay = (OverlayView)findViewById(yun.picker.simplify.R.id.view_overlay);
        if(getPaddingBottom() != 0) {
            mViewOverlay.setPadding(mViewOverlay.getPaddingLeft(), mViewOverlay.getPaddingTop(),
                    mViewOverlay.getPaddingRight(), getPaddingBottom());
            setPadding(0, 0, 0, 0);
        }
        TypedArray a = context.obtainStyledAttributes(attrs, yun.picker.simplify.R.styleable.ucrop_UCropView);
        mViewOverlay.processStyledAttributes(a);
        mGestureCropImageView.processStyledAttributes(a);
        a.recycle();
        mGestureCropImageView.setCropBoundsChangeListener(new CropBoundsChangeListener() {
            @Override
            public void onCropAspectRatioChanged(float cropRatio){
                mViewOverlay.setTargetAspectRatio(cropRatio);
            }
        });
        mViewOverlay.setOverlayViewChangeListener(new OverlayViewChangeListener() {
            @Override
            public void onCropRectUpdated(RectF cropRect){
                mGestureCropImageView.setCropRect(cropRect);
            }
        });
    }

    @Override
    public boolean shouldDelayChildPressedState(){
        return false;
    }

    @NonNull
    public GestureCropImageView getCropImageView(){
        return mGestureCropImageView;
    }

    @NonNull
    public OverlayView getOverlayView(){
        return mViewOverlay;
    }

}