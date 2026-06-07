package com.drivingschoolrwandaapp.utils;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import java.io.IOException;
import java.io.InputStream;

public class SvgDecoder implements ResourceDecoder<InputStream, SVG> {

    @Override
    public boolean handles(InputStream source, Options options) {
        // This is a simple check. In a real-world scenario, you might want to add more robust SVG detection.
        return true;
    }

    @Override
    public Resource<SVG> decode(InputStream source, int width, int height, Options options) throws IOException {
        try {
            SVG svg = SVG.getFromInputStream(source);
            return new SimpleResource<>(svg);
        } catch (SVGParseException e) {
            throw new IOException("Cannot parse SVG", e);
        }
    }
}
