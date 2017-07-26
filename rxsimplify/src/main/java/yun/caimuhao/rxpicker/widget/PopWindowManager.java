package yun.caimuhao.rxpicker.widget;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;

import yun.caimuhao.rxpicker.bean.ImageFolder;
import yun.caimuhao.rxpicker.ui.adapter.PickerAlbumAdapter;
import yun.caimuhao.rxpicker.utils.DensityUtil;

/**
 * @author Smile
 * @time 2017/4/19  下午4:53
 * @desc ${TODD}
 */
public class PopWindowManager {

  private PopupWindow mAlbumPopWindow;
  private PickerAlbumAdapter albumAdapter;

  public void init(final TextView title, final List<ImageFolder> data) {
    albumAdapter = new PickerAlbumAdapter(data, DensityUtil.dp2px(title.getContext(), 80));
    albumAdapter.setDismissListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        dismissAlbumWindow();
      }
    });

    title.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        showPopWindow(v, data, albumAdapter);
      }
    });
  }

  private void showPopWindow(View v, List<ImageFolder> data, PickerAlbumAdapter albumAdapter) {
    if (mAlbumPopWindow == null) {
      int height = DensityUtil.dp2px(v.getContext(), 300);
      View windowView = createWindowView(v, albumAdapter);
      mAlbumPopWindow =
          new PopupWindow(windowView, ViewGroup.LayoutParams.MATCH_PARENT, height, true);
      mAlbumPopWindow.setAnimationStyle(yun.picker.simplify.R.style.RxPicker_PopupAnimation);
      mAlbumPopWindow.setContentView(windowView);
      mAlbumPopWindow.setOutsideTouchable(true);
    }
    mAlbumPopWindow.showAsDropDown(v, 0, 0);
  }

  @NonNull private View createWindowView(View clickView, PickerAlbumAdapter albumAdapter) {
    View view =
        LayoutInflater.from(clickView.getContext()).inflate(yun.picker.simplify.R.layout.item_popwindow_album, null);
    RecyclerView recyclerView = (RecyclerView) view.findViewById(yun.picker.simplify.R.id.album_recycleview);
    recyclerView.setLayoutManager(
        new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
    View albumShadowLayout = view.findViewById(yun.picker.simplify.R.id.album_shadow);
    albumShadowLayout.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        dismissAlbumWindow();
      }
    });
    recyclerView.setAdapter(albumAdapter);
    return view;
  }

  private void dismissAlbumWindow() {
    if (mAlbumPopWindow != null && mAlbumPopWindow.isShowing()) {
      mAlbumPopWindow.dismiss();
    }
  }
}
