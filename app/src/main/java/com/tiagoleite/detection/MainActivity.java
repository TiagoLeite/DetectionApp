package com.tiagoleite.detection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TensorFlowClassifier tfClassifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadModel();
        byte[] arrayImage = getPixelsArray();
        Classification cls = tfClassifier.recognize(arrayImage);
        String label = cls.getLabel();

        Log.d("debug", label);

    }

    public byte[] getPixelsArray()
    {
        Bitmap bm =  BitmapFactory.decodeResource(getResources(),
                R.drawable.alanis);
        bm = Bitmap.createScaledBitmap(bm, 320, 320, true);
        int h = bm.getHeight();
        int w = bm.getWidth();
        int pixels[] = new int[h*w];
        byte[] pixelsRet = new byte[h*w*3];
        bm.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int i = 0; i < h; i++)
            for (int j = 0; j < w; j++)
            {
                int redValue = Color.red(pixels[i * w + j]);
                int blueValue = Color.blue(pixels[i * w + j]);
                int greenValue = Color.green(pixels[i * w + j]);
                pixelsRet[i * w + j] = (byte)redValue;
                pixelsRet[i * w + j + h*w] = (byte)greenValue;
                pixelsRet[i * w + j + 2*h*w] = (byte)blueValue;
            }

        return pixelsRet;
    }

    private void loadModel()
    {
        try
        {
            tfClassifier = TensorFlowClassifier.create(getAssets(), "TensorFlow",
                    "frozen_inference_graph.pb",
                    "labels.txt",
                    320,
                    "image_tensor:0",
                    "detection_classes:0",
                    true);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error initializing classifiers!", e);
        }
    }
}
