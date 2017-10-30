package yun.caimuhao.rxpicker.ui.adapter;

import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.List;

import yun.caimuhao.rxpicker.bean.ImageItem;
import yun.caimuhao.rxpicker.utils.DensityUtil;
import yun.yalantis.ucrop.view.PhotoView;

/**
 * @author Smile
 * @time 2017/4/20  上午11:39
 * @desc ${TODD}
 */
public class PreviewAdapter extends PagerAdapter {

    private List<ImageItem> data;

    public PreviewAdapter(List<ImageItem> data){
        this.data = data;
    }

    @Override
    public int getCount(){
        return data.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object){
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position){
        ImageItem imageItem = data.get(position);
        int deviceWidth = DensityUtil.getDeviceWidth(container.getContext());

        //        AppCompatImageView imageView = new AppCompatImageView(container.getContext());
        //    UCropView imageView = new UCropView(container.getContext());
        //    imageView.getOverlayView().setShowCropFrame(false);
        //    imageView.getOverlayView().setShowCropGrid(false);
        //    imageView.getOverlayView().setDimmedColor(Color.TRANSPARENT);

        //        ViewPager.LayoutParams layoutParams = new ViewPager.LayoutParams();
        //        imageView.setLayoutParams(layoutParams);
        //        RxPickerManager.getInstance().display(imageView, imageItem.getPath(), deviceWidth, deviceWidth);
        PhotoView imageView = new PhotoView(container.getContext());
        try {
            imageView.setRotateEnabled(false).setImageUri(Uri.fromFile(new File(imageItem.getPath())), null);
            //              imageView.getCropImageView().setRotateEnabled(false).setImageUri(Uri.fromFile(new File(imageItem.getPath())), null);
        }catch(Exception e) {
            e.printStackTrace();
        }
        container.addView(imageView);
        return imageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object){
        container.removeView((View)object);
    }
}
