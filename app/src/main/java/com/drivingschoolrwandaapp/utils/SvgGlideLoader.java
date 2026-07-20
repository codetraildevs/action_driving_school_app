package com.drivingschoolrwandaapp.utils;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.drivingschoolrwandaapp.R;

public class SvgGlideLoader {

    public static void loadSvg(Context context, String url, ImageView imageView) {
        loadSvg(context, url, imageView, R.drawable.ic_materials, R.drawable.ic_error);
    }

    public static void loadSvg(Context context, String url, ImageView imageView,
                               @DrawableRes int placeholderRes, @DrawableRes int errorRes) {
        RequestOptions options = new RequestOptions()
                .placeholder(placeholderRes)
                .error(errorRes)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        RequestBuilder<PictureDrawable> requestBuilder = Glide.with(context)
                .as(PictureDrawable.class)
                .apply(options)
                .listener(new SvgSoftwareLayerSetter());
        requestBuilder.load(url).into(imageView);
    }
}
