package com.drivingschoolrwandaapp.utils;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;

public class SvgGlideLoader {

    public static void loadSvg(Context context, String url, ImageView imageView) {
        RequestBuilder<PictureDrawable> requestBuilder = Glide.with(context)
                .as(PictureDrawable.class)
                .listener(new SvgSoftwareLayerSetter());
        requestBuilder.load(url).into(imageView);
    }
}
