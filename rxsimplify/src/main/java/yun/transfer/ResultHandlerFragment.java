package yun.transfer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.Toast;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import yun.caimuhao.rxpicker.bean.ImageItem;
import yun.caimuhao.rxpicker.utils.CameraHelper;
import yun.caimuhao.rxpicker.utils.RxPickerManager;
import yun.caimuhao.rxpicker.utils.T;
import yun.picker.simplify.R;
import yun.yalantis.ucrop.UCrop;

import static yun.caimuhao.rxpicker.ui.fragment.PickerFragment.CAMERA_REQUEST;
import static yun.yalantis.ucrop.UCrop.RESULT_ERROR;

/**
 * @author Smile
 * @time 2017/4/18  下午6:38
 * @desc ${TODD}
 */
public class ResultHandlerFragment extends Fragment {
    public static final int CAMERA_PERMISSION = 0;
    private static final String SAMPLE_CROPPED_IMAGE_NAME = "CropImage.png";
    PublishSubject<List<ImageItem>> resultSubject = PublishSubject.create();
    BehaviorSubject<Boolean> attachSubject = BehaviorSubject.create();

    public static final int REQUEST_CODE = 0x00100;
    private List<ImageItem> mResult;

    public static ResultHandlerFragment newInstance(){
        return new ResultHandlerFragment();
    }

    public PublishSubject<List<ImageItem>> getResultSubject(){
        return resultSubject;
    }

    public BehaviorSubject<Boolean> getAttachSubject(){
        return attachSubject;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE && data != null) {
            mResult = RxPickerManager.getInstance().getResult(data);

            if(RxPickerManager.getInstance().isCrop() && mResult.size() == 1) {
                //执行剪切任务
                startCropActivity(mResult.get(0));
            }else {
                resultSubject.onNext(mResult);
            }
        }else if(requestCode == UCrop.REQUEST_CROP) {
            handleCropResult(data);
        }else if(requestCode == RESULT_ERROR) {
            resultSubject.onNext(new ArrayList<ImageItem>());
        }else if(requestCode == CAMERA_REQUEST) {
            handleTakeAphoto(data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CAMERA_PERMISSION) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CameraHelper.take(this, CAMERA_REQUEST);
            }else {
                T.show(getActivity(), getString(R.string.permissions_error));
            }
        }
    }

    private void handleTakeAphoto(Intent data){
        File file = CameraHelper.getTakeImageFile();
        ImageItem item = new ImageItem(0, file.getAbsolutePath(), file.getName(), System.currentTimeMillis());
        mResult = new ArrayList<>();
        mResult.add(item);
        if(RxPickerManager.getInstance().isCrop() && mResult.size() == 1) {
            //执行剪切任务
            startCropActivity(mResult.get(0));
        }else {
            resultSubject.onNext(mResult);
        }
    }

    private void handleCropResult(@NonNull Intent result){
        if(result != null) {
            String outputCropPath = UCrop.getOutputCropPath(result);
            if(outputCropPath != null) {
                ImageItem imageItem = mResult.get(0);
                imageItem.setPath(outputCropPath);
                resultSubject.onNext(mResult);
            }else {
                Toast.makeText(getActivity(), "裁剪失败", Toast.LENGTH_SHORT).show();
                resultSubject.onNext(new ArrayList<ImageItem>());
            }
        }else {
            resultSubject.onNext(new ArrayList<ImageItem>());
        }
    }

    private void startCropActivity(@NonNull ImageItem path){
        String destinationFileName = SAMPLE_CROPPED_IMAGE_NAME;
        //        Uri input = Uri.parse(path);
        Uri input = Uri.fromFile(new File(path.getPath()));
        File externalCacheDir = getActivity().getExternalCacheDir();
        if(externalCacheDir == null) {
            externalCacheDir = getActivity().getCacheDir();
        }
        UCrop uCrop = UCrop.of(input, Uri.fromFile(new File(externalCacheDir, destinationFileName)));
        uCrop.withAspectCircle(RxPickerManager.getInstance().getCropOptions()).start(this);
    }

    @TargetApi(23)
    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        attachSubject.onNext(true);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        if(Build.VERSION.SDK_INT<23) {
            attachSubject.onNext(true);
        }
    }
}
