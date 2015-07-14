package bsscco.bssccofiltersample;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.edmodo.cropper.CropImageView;

import java.io.IOException;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageLookupFilter;

/**
 * @author bsscco
 */
public class FilterFragment extends Fragment
        implements OnSeekBarChangeListener {

    private GPUImage mGpuImage;
    private GPUImageLookupFilter mGpuImageFilter;
    private ImageView mSelectedFilterPreviewImage;
    private Bitmap mOriginSrcBitmap;
    private Bitmap mSelectedFilterBitmap;

    enum FilterType {
        origin, a6, c2, insta_1977, insta_lofi10, only_nashiville, vsco_a5, vsco_f1, white_f2, lookup
    }

    class FilterListAdapter extends RecyclerView.Adapter<FilterListAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder {
            View filterPreviewButton;
            ImageView filterPreviewImage;
            TextView filterName;

            public ViewHolder(View v) {
                super(v);
                filterPreviewButton = v.findViewById(R.id.filter_preview_button);
                filterPreviewImage = (ImageView) v.findViewById(R.id.filter_preview_image);
                filterName = (TextView) v.findViewById(R.id.filter_name);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_filter, viewGroup, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
            {
                AsyncTask imageLoadTask = (AsyncTask) viewHolder.filterPreviewImage.getTag();
                if (imageLoadTask != null) {
                    imageLoadTask.cancel(true);
                    imageLoadTask = null;
                }
                imageLoadTask = new AsyncTask<Object, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Object... params) {
                        Bitmap filterBitmap = getNewFilterBitmap(FilterType.values()[position]);
                        if (filterBitmap == null) {
                            return mOriginSrcBitmap;
                        } else {
                            GPUImageLookupFilter filter = new GPUImageLookupFilter();
                            filter.setBitmap(filterBitmap);
                            GPUImage gpuImage = new GPUImage(getActivity());
                            gpuImage.setFilter(filter);
                            return gpuImage.getBitmapWithFilterApplied(mOriginSrcBitmap);
                        }
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        viewHolder.filterPreviewImage.setImageBitmap(bitmap);
                    }
                };
                viewHolder.filterPreviewImage.setTag(imageLoadTask);
                imageLoadTask.execute();
            }
            viewHolder.filterPreviewButton.setOnClickListener(v -> {
                AsyncTask imageLoadTask = (AsyncTask) mSelectedFilterPreviewImage.getTag();
                if (imageLoadTask != null) {
                    imageLoadTask.cancel(true);
                    imageLoadTask = null;
                }
                imageLoadTask = new AsyncTask<Object, Void, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(Object... params) {
                        mSelectedFilterBitmap = getNewFilterBitmap(FilterType.values()[position]);
                        if (mSelectedFilterBitmap == null) {
                            mGpuImageFilter = null;
                            mGpuImage = null;
                            return mOriginSrcBitmap;
                        } else {
                            mGpuImageFilter = new GPUImageLookupFilter();
                            mGpuImageFilter.setBitmap(mSelectedFilterBitmap);
                            mGpuImage = new GPUImage(getActivity());
                            mGpuImage.setFilter(mGpuImageFilter);
                            return mGpuImage.getBitmapWithFilterApplied(mOriginSrcBitmap);
                        }
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        mSelectedFilterPreviewImage.setImageBitmap(bitmap);
                        SeekBar transparencySeekbar = (SeekBar) getView().findViewById(R.id.transparency_seekbar);
                        onProgressChanged(transparencySeekbar, transparencySeekbar.getProgress(), true);
                    }
                };
                mSelectedFilterPreviewImage.setTag(imageLoadTask);
                imageLoadTask.execute();
            });

            viewHolder.filterName.setText(FilterType.values()[position].toString());
        }

        private Bitmap getNewFilterBitmap(FilterType filterType) {
            if (filterType.equals(FilterType.origin)) {
                return null;
            } else {
                int filterDrawableResId = getResources().getIdentifier(filterType.toString(), "drawable", getActivity().getPackageName());
                return BitmapFactory.decodeResource(getResources(), filterDrawableResId);
            }
        }

        @Override
        public int getItemCount() {
            return FilterType.values().length;
        }
    }

    public static FilterFragment newInstance() {
        FilterFragment fragment = new FilterFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mGpuImage = new GPUImage(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_filter, container, false);

        mSelectedFilterPreviewImage = (ImageView) rootView.findViewById(R.id.selected_filter_preview_image);
        mOriginSrcBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.preview_image);
//        rotation= getExifOrientation(mPath);
        int rotation = 0;
        if (rotation != 0) {
            mOriginSrcBitmap = getRotatedBitmap(mOriginSrcBitmap, rotation);
        }
        ((SeekBar) rootView.findViewById(R.id.transparency_seekbar)).setOnSeekBarChangeListener(this);
        RecyclerView filterListView = (RecyclerView) rootView.findViewById(R.id.filter_listview);

        mSelectedFilterPreviewImage.setImageResource(R.drawable.preview_image);
        LinearLayoutManager layoutMgr = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        filterListView.setLayoutManager(layoutMgr);
        filterListView.setAdapter(new FilterListAdapter());

        rootView.findViewById(R.id.origin_button).setOnClickListener(v -> {
            mGpuImageFilter = null;
            mGpuImage = null;
            mSelectedFilterPreviewImage.setImageBitmap(mOriginSrcBitmap);
        });

        rootView.findViewById(R.id.rotate_button).setOnClickListener(v -> {
            mOriginSrcBitmap = getRotatedBitmap(mOriginSrcBitmap, 90);
            mSelectedFilterPreviewImage.setImageBitmap(mGpuImage.getBitmapWithFilterApplied(mOriginSrcBitmap));
            filterListView.getAdapter().notifyDataSetChanged();
        });

        rootView.findViewById(R.id.crop_button).setOnClickListener(v -> {
            setControllerMode("MODE_CROP");
            Bitmap filteredBitmap = ((BitmapDrawable) mSelectedFilterPreviewImage.getDrawable()).getBitmap();
            ((CropImageView) getView().findViewById(R.id.selected_crop_preview_image)).setImageBitmap(filteredBitmap);
        });

        rootView.findViewById(R.id.frame_1x1_button).setOnClickListener(v -> {
            ((CropImageView) getView().findViewById(R.id.selected_crop_preview_image)).setAspectRatio(1, 1);
        });

        rootView.findViewById(R.id.frame_3x4_button).setOnClickListener(v -> {
            ((CropImageView) getView().findViewById(R.id.selected_crop_preview_image)).setAspectRatio(3, 4);
        });

        rootView.findViewById(R.id.frame_4x3_button).setOnClickListener(v -> {
            ((CropImageView) getView().findViewById(R.id.selected_crop_preview_image)).setAspectRatio(4, 3);
        });

        rootView.findViewById(R.id.cancel_button).setOnClickListener(v -> {
            setControllerMode("MODE_FILTER");
        });

        rootView.findViewById(R.id.ok_button).setOnClickListener(v -> {
            setControllerMode("MODE_FILTER");
            mOriginSrcBitmap = ((CropImageView) getView().findViewById(R.id.selected_crop_preview_image)).getCroppedImage();
            mSelectedFilterPreviewImage.setImageBitmap(mOriginSrcBitmap);
            filterListView.getAdapter().notifyDataSetChanged();
        });

        return rootView;
    }

    private int getExifOrientation(String filepath) {
        int rotation = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (exif != null) {
            // We only recognize a subset of orientation tag values.
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 360;
                    break;
            }

        }
        return rotation;
    }

    private Bitmap getRotatedBitmap(Bitmap src, int rotation) {
        int width = src.getWidth();
        int height = src.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(src, 0, 0, width, height, matrix, true);
    }

    private void setControllerMode(String controllerMode) {
        if (controllerMode == null) {
            return;
        }

        int switch1 = 0;
        int switch2 = 0;
        if (controllerMode.equals("MODE_CROP")) {
            switch1 = View.GONE;
            switch2 = View.VISIBLE;
        } else if (controllerMode.equals("MODE_FILTER")) {
            switch1 = View.VISIBLE;
            switch2 = View.GONE;
        } else {
            return;
        }
        getView().findViewById(R.id.filter_bottom_controller).setVisibility(switch1);
        getView().findViewById(R.id.filter_listview).setVisibility(switch1);
        getView().findViewById(R.id.transparency_seekbar).setVisibility(switch1);
        getView().findViewById(R.id.transparency_text).setVisibility(switch1);

        getView().findViewById(R.id.crop_bottom_controller).setVisibility(switch2);
        getView().findViewById(R.id.crop_frame_list_layout).setVisibility(switch2);
        getView().findViewById(R.id.crop_frame_text_list_layout).setVisibility(switch2);
        getView().findViewById(R.id.selected_crop_preview_image).setVisibility(switch2);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mGpuImageFilter != null && mGpuImageFilter != null && fromUser) {
            TextView transparencyText = (TextView) getView().findViewById(R.id.transparency_text);
            transparencyText.setText("불투명도:" + (int) (progress / 255.0f * 100) + "%");
            mGpuImageFilter.setBitmap(getAlphaBitmap(mSelectedFilterBitmap, progress));
            mSelectedFilterPreviewImage.setImageBitmap(mGpuImage.getBitmapWithFilterApplied(mOriginSrcBitmap));
        }
    }

    private Bitmap getAlphaBitmap(Bitmap src, int alpha) {
        Bitmap destBitmap = Bitmap.createBitmap(mSelectedFilterBitmap.getWidth(), mSelectedFilterBitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(destBitmap);
        canvas.drawARGB(0, 0, 0, 0);
        final Paint paint = new Paint();
        paint.setAlpha(alpha);
        canvas.drawBitmap(src, 0, 0, paint);
        return destBitmap;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}