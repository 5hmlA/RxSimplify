package yun.yalantis.ucrop.callback;

import android.support.annotation.NonNull;
import android.view.View;

/**
 * @author yun.
 * @date 2017/10/22
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
public class TransformListener {
  public void onLoadComplete(){}
  public void onLoadFailure(@NonNull Exception e){}
  public void onRotate(float currentAngle){}
  public void onScale(float currentScale){}
  public void onDoubleTap(View view){}
  public void onLongPress(View view){}
  public void onClicked(View view){}
}
