package yun.caimuhao.rxpicker;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import yun.caimuhao.rxpicker.bean.ImageItem;
import yun.caimuhao.rxpicker.ui.RxPickerActivity;
import yun.caimuhao.rxpicker.ui.fragment.ResultHandlerFragment;
import yun.caimuhao.rxpicker.utils.CameraHelper;
import yun.caimuhao.rxpicker.utils.PickerConfig;
import yun.caimuhao.rxpicker.utils.RxPickerImageLoader;
import yun.caimuhao.rxpicker.utils.RxPickerManager;
import yun.tbruyelle.rxpermissions.RxPermissions;

import static yun.caimuhao.rxpicker.ui.fragment.PickerFragment.CAMERA_REQUEST;
import static yun.caimuhao.rxpicker.ui.fragment.ResultHandlerFragment.CAMERA_PERMISSION;

/**
 * @author Smile
 * @time 2017/4/18  下午6:01
 * @desc ${TODD}
 */
public class RxPicker {

    private RxPicker(PickerConfig config){
        RxPickerManager.getInstance().setConfig(config);
    }


    /**
     * init RxPicker
     */
    public static void init(RxPickerImageLoader imageLoader){
        RxPickerManager.getInstance().init(imageLoader);
    }


    /**
     * Using the custom config
     */

    static RxPicker of(PickerConfig config){
        return new RxPicker(config);
    }

    /**
     * Using the default config
     */
    public static RxPicker of(){
        return new RxPicker(new PickerConfig());
    }

    /**
     * Set the selection mode
     */
    public RxPicker single(boolean single){
        RxPickerManager.getInstance().setMode(single ? PickerConfig.SINGLE_IMG : PickerConfig.MULTIPLE_IMG);
        return this;
    }

    //因爲 配置管理類 是單利 必須設置 配置全部放進option裏面传进来
    public RxPicker crop(boolean crop){
        RxPickerManager.getInstance().setCrop(crop);
        return this;
    }


    public RxPicker takePic(boolean takePic){
        RxPickerManager.getInstance().setTakePic(takePic);
        return this;
    }

    /**
     * Set the show  Taking pictures;
     */
    public RxPicker camera(boolean showCamera){
        RxPickerManager.getInstance().showCamera(showCamera);
        return this;
    }

    /**
     * Set the select  image limit
     */
    public RxPicker limit(int minValue, int maxValue){
        RxPickerManager.getInstance().limit(minValue, maxValue);
        return this;
    }

    /**
     * start picker from activity
     */
    public Observable<List<ImageItem>> start(Activity activity){
        return start(activity.getFragmentManager());
    }

    /**
     * start picker from fragment
     */
    public Observable<List<ImageItem>> start(Fragment fragment){
        return start(fragment.getFragmentManager());
    }

    /**
     * start picker from fragment
     */
    private Observable<List<ImageItem>> start(FragmentManager fragmentManager){
        ResultHandlerFragment fragment = (ResultHandlerFragment)fragmentManager
                .findFragmentByTag(ResultHandlerFragment.class.getSimpleName());
        if(fragment == null) {
            fragment = ResultHandlerFragment.newInstance();
            fragmentManager.beginTransaction().add(fragment, fragment.getClass().getSimpleName()).commit();
        }else if(fragment.isDetached()) {
            fragmentManager.beginTransaction().attach(fragment).commit();
        }
        return getListItem(fragment);
    }


    private Observable<List<ImageItem>> getListItem(final ResultHandlerFragment finalFragment){
        return finalFragment.getAttachSubject().filter(new Predicate<Boolean>() {
            @Override
            public boolean test(@NonNull Boolean aBoolean) throws Exception{
                return aBoolean;
            }
        }).flatMap(new Function<Boolean,ObservableSource<List<ImageItem>>>() {
            @Override
            public ObservableSource<List<ImageItem>> apply(@NonNull Boolean aBoolean) throws Exception{
                if(RxPickerManager.getInstance().isTakePic()) {
                    if(ContextCompat.checkSelfPermission(finalFragment.getActivity(),
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        CameraHelper.take(finalFragment, CAMERA_REQUEST);
                    }else {
                        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                            finalFragment
                                    .requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
                        }
                        Toast.makeText(finalFragment.getActivity(), "请先申请权限", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Intent intent = new Intent(finalFragment.getActivity(), RxPickerActivity.class);
                    finalFragment.startActivityForResult(intent, ResultHandlerFragment.REQUEST_CODE);
                }
                return finalFragment.getResultSubject();

            }
        }).take(1);
    }

    public static class Options {

    }

}
