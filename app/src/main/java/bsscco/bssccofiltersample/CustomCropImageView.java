package bsscco.bssccofiltersample;

import android.content.Context;

import com.edmodo.cropper.CropImageView;

/**
 * Created by bsscco on 2015-07-13.
 */
public class CustomCropImageView extends CropImageView {

    public CustomCropImageView(Context context) {
        super(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }
}
