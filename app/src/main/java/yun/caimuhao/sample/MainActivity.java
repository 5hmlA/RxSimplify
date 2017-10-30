package yun.caimuhao.sample;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;
import yun.caimuhao.rxpicker.RxPicker;
import yun.caimuhao.rxpicker.bean.ImageItem;
import yun.caimuhao.rxpicker.utils.CameraHelper;
import yun.caimuhao.rxpicker.utils.RxPickerManager;
import yun.caimuhao.rxpicker.widget.DividerGridItemDecoration;
import yun.yalantis.ucrop.UCrop;
import yun.yalantis.ucrop.view.GestureWraper;

import static yun.caimuhao.rxpicker.ui.fragment.PickerFragment.CAMERA_REQUEST;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvSingleImg;
    private TextView tvMultiImg;

    private RecyclerView recyclerView;
    private PickerAdapter adapter;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("RxPicker");

        tvSingleImg = (TextView) findViewById(R.id.btn_single_img);
        tvSingleImg.setOnClickListener(this);

        tvMultiImg = (TextView) findViewById(R.id.btn_multi_img);
        tvMultiImg.setOnClickListener(this);
        GestureWraper gestureWraper = new GestureWraper(findViewById(R.id.iv));

        adapter = new PickerAdapter();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.addItemDecoration(new DividerGridItemDecoration(this));
        recyclerView.setAdapter(adapter);
//        Glide.with(this).load("http://www.zbjuran.com/uploads/allimg/170725/1506053064-0.jpg").
//                into((TransformImageView)findViewById(R.id.tfiv));
        Glide.with(this).load("https://avatars0.githubusercontent.com/u/9412501?s=460&v=4").placeholder(R.mipmap.ic_launcher).
                into((ImageView)findViewById(R.id.iv));
    }


    @Override protected void onResume() {
        super.onResume();
        List<ImageItem> imageItems = new ArrayList<>();
        imageItems.add(
                new ImageItem(1, "/storage/emulated/0/Android/data/yun.caimuhao.ucrop/cache/CropImage.png",
                        "ee", 3333));
        //        Picasso.with(this).load(new File("/storage/emulated/0/Android/data/yun.caimuhao.ucrop/cache/CropImage" +
        //                ".png")).skipMemoryCache()
        //                .into((ImageView)findViewById(R.id.temp));

        //        adapter.setData(imageItems);
    }
    //到底有没有某项权限，怎么检测呢，基于以往 Android 在这方面的不精细，
    // 很多人都不会太在意这方面的逻辑判断，新出的6.0系统也只是基于targetSdkVersion 23以上的app的判断，
    // 包括6.0以下的版本，怎样判断是不是被安全中心这种禁掉了呢，这就需要 AppOpsManager 这个类了
    public void check(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int checkResult = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_FINE_LOCATION, Binder.getCallingUid(), context.getPackageName());
            if (checkResult == AppOpsManager.MODE_ALLOWED) {
                Toast.makeText(context, "有权限", Toast.LENGTH_LONG).show();
                Log.e("jijiaxin", "有权限");
            }
            else if (checkResult == AppOpsManager.MODE_IGNORED) {
                // TODO: 只需要依此方法判断退出就可以了，这时是没有权限的。
                Toast.makeText(context, "被禁止了", Toast.LENGTH_LONG).show();
                Log.e("jijiaxin", "被禁止了");
            }
            else if (checkResult == AppOpsManager.MODE_ERRORED) {
                Toast.makeText(context, "出错了", Toast.LENGTH_LONG).show();
                Log.e("jijiaxin", "出错了");
            }
            else if (checkResult == 4) {
                Toast.makeText(context, "权限需要询问", Toast.LENGTH_LONG).show();
                Log.e("jijiaxin", "权限需要询问");
            }
        }
    }
    @Override public void onClick(View v) {
        if (tvSingleImg == v) {
            //RxPermissions.care(this)
            //             .request(Manifest.permission.CALL_PHONE)
            //             .subscribe(new Consumer<Boolean>() {
            //                 @Override public void accept(Boolean aBoolean) throws Exception {
            //                     Toast.makeText(MainActivity.this, aBoolean + "", 0).show();
            //                 }
            //             });
            adapter.setData(new ArrayList<ImageItem>());
            //RxPickerManager.getInstance().setCropOptions();
            RxPicker.of().crop(true).takePic(false).camera(false).start(this).subscribe(new Consumer<List<ImageItem>>
                    () {
                @Override public void accept(@NonNull List<ImageItem> imageItems) throws Exception {
                    adapter.setData(imageItems);
                }
            });
        }
        else {
            if (tvMultiImg == v) {
                RxPicker.of().single(false).crop(false).takePic(false).camera(true).start(this).subscribe(new
                                                                                                     Consumer<List<ImageItem>>
                        () {
                    @Override public void accept(@NonNull List<ImageItem> imageItems) throws Exception {
                        adapter.setData(imageItems);
                    }
                });
                //
                //            RxPicker.of().single(false).camera(true).limit(3, 9).start(this).subscribe(new Consumer<List<ImageItem>>() {
                //                @Override
                //                public void accept(@NonNull List<ImageItem> imageItems) throws Exception{
                //                    adapter.setData(imageItems);
                //                }
                //            });
            }
        }
    }


    public void takeCamera(View view) {
        CameraHelper.take(this, CAMERA_REQUEST);
    }


    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST) {
            handleCameraResult();
        }
        else if (requestCode == UCrop.REQUEST_CROP) {
            handleCropResult(data);
        }
    }


    private void handleCropResult(@NonNull Intent result) {
        if (result != null) {
            String outputCropPath = UCrop.getOutputCropPath(result);
            if (outputCropPath != null) {
                Picasso.with(this).load(new File(outputCropPath)).into((ImageView) findViewById(R.id.temp));
            }
            else {
                Toast.makeText(this, "裁剪失败", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void handleCameraResult() {
        File file = CameraHelper.getTakeImageFile();
        ImageItem item = new ImageItem(0, file.getAbsolutePath(), file.getName(), System.currentTimeMillis());
        startCropActivity(item);
    }


    private void startCropActivity(@NonNull ImageItem path) {
        String destinationFileName = "camera_clirle.png";
        //        Uri input = Uri.parse(path);
        Uri input = Uri.fromFile(new File(path.getPath()));
        UCrop uCrop = UCrop.of(input, Uri.fromFile(new File(getExternalCacheDir(), destinationFileName)));
        uCrop.withAspectCircle(RxPickerManager.getInstance().getCropOptions()).start(this);
        //        uCrop.withAspectCircle().start(getContext());
    }
}
