package yun.caimuhao.sample;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import yun.caimuhao.rxpicker.utils.RxPickerImageLoader;

/**
 * @author Smile
 * @time 2017/4/19  下午3:38
 * @desc ${TODD}
 */
public class GlideImageLoader implements RxPickerImageLoader {

    @Override
    public void display(ImageView imageView, String path, int width, int height) {
        Glide.with(imageView.getContext()).load(path).error(R.drawable.ic_preview_image).diskCacheStrategy(
                DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop().override
                (width, height).into(imageView);
//        Picasso.with(imageView.getContext()).load(path).skipMemoryCache().error(R.drawable.ic_preview_image)
//                .centerCrop().resize(width,
//                height).into(imageView);
    }
}
