package yun.caimuhao.sample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import static yun.caimuhao.rxpicker.ui.fragment.PickerFragment.CAMERA_REQUEST;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvSingleImg;
    private TextView tvMultiImg;

    private RecyclerView recyclerView;
    private PickerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("RxPicker");

        tvSingleImg = (TextView)findViewById(R.id.btn_single_img);
        tvSingleImg.setOnClickListener(this);

        tvMultiImg = (TextView)findViewById(R.id.btn_multi_img);
        tvMultiImg.setOnClickListener(this);

        adapter = new PickerAdapter();
        recyclerView = (RecyclerView)findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.addItemDecoration(new DividerGridItemDecoration(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume(){
        super.onResume();
        List<ImageItem> imageItems = new ArrayList<>();
        imageItems.add(new ImageItem(1, "/storage/emulated/0/Android/data/yun.caimuhao.ucrop/cache/CropImage.png", "ee",
                3333));
//        Picasso.with(this).load(new File("/storage/emulated/0/Android/data/yun.caimuhao.ucrop/cache/CropImage" +
//                ".png")).skipMemoryCache()
//                .into((ImageView)findViewById(R.id.temp));

//        adapter.setData(imageItems);
    }

    @Override
    public void onClick(View v){
        if(tvSingleImg == v) {
            adapter.setData(new ArrayList<ImageItem>());

            RxPicker.of().crop(true).takePic(false).start(this).subscribe(new Consumer<List<ImageItem>>() {
                @Override
                public void accept(@NonNull List<ImageItem> imageItems) throws Exception{
                    adapter.setData(imageItems);
                }
            });
        }else if(tvMultiImg == v) {
            RxPicker.of().crop(true).takePic(true).start(this).subscribe(new Consumer<List<ImageItem>>() {
                @Override
                public void accept(@NonNull List<ImageItem> imageItems) throws Exception{
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

    public void takeCamera(View view){
        CameraHelper.take(this, CAMERA_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CAMERA_REQUEST) {
            handleCameraResult();
        }else if(requestCode == UCrop.REQUEST_CROP) {
            handleCropResult(data);
        }
    }

    private void handleCropResult(@NonNull Intent result){
        if(result != null) {
            String outputCropPath = UCrop.getOutputCropPath(result);
            if(outputCropPath != null) {
                Picasso.with(this).load(new File(outputCropPath)).into((ImageView)findViewById(R.id.temp));
            }else {
                Toast.makeText(this, "裁剪失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleCameraResult(){
        File file = CameraHelper.getTakeImageFile();
        ImageItem item = new ImageItem(0, file.getAbsolutePath(), file.getName(), System.currentTimeMillis());
        startCropActivity(item);
    }

    private void startCropActivity(@NonNull ImageItem path){
        String destinationFileName = "camera_clirle.png";
        //        Uri input = Uri.parse(path);
        Uri input = Uri.fromFile(new File(path.getPath()));
        UCrop uCrop = UCrop.of(input, Uri.fromFile(new File(getExternalCacheDir(), destinationFileName)));
        uCrop.withAspectCircle(RxPickerManager.getInstance().getCropOptions()).start(this);
        //        uCrop.withAspectCircle().start(getContext());
    }
}
