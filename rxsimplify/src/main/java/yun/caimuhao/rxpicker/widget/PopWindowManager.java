package yun.caimuhao.rxpicker.widget;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

import yun.caimuhao.rxpicker.bean.ImageFolder;
import yun.caimuhao.rxpicker.ui.adapter.PickerAlbumAdapter;
import yun.caimuhao.rxpicker.utils.DensityUtil;
import yun.picker.simplify.R;

/**
 * @author Smile
 * @time 2017/4/19  下午4:53
 * @desc ${TODD}
 */
public class PopWindowManager implements PopupWindow.OnDismissListener {

    private PopupWindow mAlbumPopWindow;
    private PickerAlbumAdapter albumAdapter;
    private ImageView mIgtoggle;

    public void init(ImageView igtoggle, final TextView title, final List<ImageFolder> data){
        mIgtoggle = igtoggle;
        albumAdapter = new PickerAlbumAdapter(data, DensityUtil.dp2px(title.getContext(), 80));
        albumAdapter.setDismissListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                dismissAlbumWindow();
            }
        });

        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                showPopWindow(v, data, albumAdapter);
            }
        });
    }

    private void showPopWindow(View v, List<ImageFolder> data, PickerAlbumAdapter albumAdapter){
        if(mAlbumPopWindow == null) {
            int height = (int)v.getResources().getDimension(R.dimen.rxpick_album_popw_height);
            View windowView = createWindowView(v, albumAdapter);
            mAlbumPopWindow = new PopupWindow(windowView, ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.min(height, getMatchModeHeight(v)), true);
            mAlbumPopWindow.setAnimationStyle(yun.picker.simplify.R.style.RxPicker_PopupAnimation);
            mAlbumPopWindow.setContentView(windowView);
            mAlbumPopWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mAlbumPopWindow.setOutsideTouchable(true);
            mAlbumPopWindow.setOnDismissListener(this);
        }

        mIgtoggle.animate().rotation(-180).start();
        mAlbumPopWindow.showAsDropDown(v);//在7.0上会覆盖v，需要设置好pop的高度才有效.setHeight(600);
    }

    private int getMatchModeHeight(View v){
        Rect rect = new Rect();
        v.getGlobalVisibleRect(rect);
        return v.getResources().getDisplayMetrics().heightPixels-rect.bottom;
    }

    @NonNull
    private View createWindowView(View clickView, PickerAlbumAdapter albumAdapter){
        View view = LayoutInflater.from(clickView.getContext())
                .inflate(yun.picker.simplify.R.layout.item_popwindow_album, null);
        RecyclerView recyclerView = (RecyclerView)view.findViewById(yun.picker.simplify.R.id.album_recycleview);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        View albumShadowLayout = view.findViewById(yun.picker.simplify.R.id.album_shadow);
        albumShadowLayout.setBackgroundColor(Color.WHITE);
        recyclerView.setBackgroundColor(Color.WHITE);
        albumShadowLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                dismissAlbumWindow();
            }
        });
        recyclerView.setAdapter(albumAdapter);
        return view;
    }

    private void dismissAlbumWindow(){
        if(mAlbumPopWindow != null && mAlbumPopWindow.isShowing()) {
            mAlbumPopWindow.dismiss();
        }
    }

    @Override
    public void onDismiss(){
        mIgtoggle.animate().rotation(0).start();
    }
}
