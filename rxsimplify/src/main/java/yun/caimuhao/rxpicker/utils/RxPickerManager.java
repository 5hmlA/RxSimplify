package yun.caimuhao.rxpicker.utils;

import android.content.Intent;
import android.widget.ImageView;

import java.util.List;

import yun.caimuhao.rxpicker.bean.ImageItem;
import yun.caimuhao.rxpicker.ui.fragment.PickerFragment;
import yun.yalantis.ucrop.UCrop;

/**
 * @author Smile
 * @time 2017/4/18  下午6:08
 * @desc ${TODD}
 */
public class RxPickerManager {

    private PickerConfig config;
    private RxPickerImageLoader imageLoader;

    private static RxPickerManager manager;
    private boolean mCrop;
    private UCrop.Options mCropOptions;
    private boolean mTakePic;

    public static RxPickerManager getInstance(){
        if(manager == null) {
            synchronized(RxPickerManager.class) {
                if(manager == null) {
                    manager = new RxPickerManager();
                }
            }
        }
        return manager;
    }

    private RxPickerManager(){
    }

    public RxPickerManager setConfig(PickerConfig config){
        this.config = config;
        return this;
    }

    public PickerConfig getConfig(){
        return config;
    }

    public void init(RxPickerImageLoader imageLoader){
        this.imageLoader = imageLoader;
    }

    public void setMode(int mode){
        config.setMode(mode);
    }

    public void showCamera(boolean showCamera){
        config.setShowCamera(showCamera);
    }

    public void limit(int minValue, int maxValue){
        config.setLimit(minValue, maxValue);
    }

    public void display(ImageView imageView, String path, int width, int height){
        if(imageLoader == null) {
            throw new NullPointerException("You must fist of all call 'RxPicker.init()' to initialize");
        }
        imageLoader.display(imageView, path, width, height);
    }

    public List<ImageItem> getResult(Intent intent){
        return (List<ImageItem>)intent.getSerializableExtra(PickerFragment.MEDIA_RESULT);
    }

    public void setCrop(boolean crop){
        mCrop = crop;
    }

    public boolean isCrop(){
        return mCrop;
    }

    public void setCropOptions(UCrop.Options cropOptions){
        mCropOptions = cropOptions;
    }

    public UCrop.Options getCropOptions(){
        return mCropOptions;
    }

    public boolean isTakePic(){
        return mTakePic;
    }

    public void setTakePic(boolean takePic){
        mTakePic = takePic;
    }
}
