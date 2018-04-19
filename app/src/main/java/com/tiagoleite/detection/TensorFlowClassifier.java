package com.tiagoleite.detection;

import android.content.res.AssetManager;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TensorFlowClassifier implements Classifier
{
    private static final float THRESHOLD = 0.1f;
    private static final int NUM_CLASSES = 100;
    private TensorFlowInferenceInterface tfHelper;
    private String name, inputName, outputName;
    private int inputSize;
    private boolean feedKeepProb;
    private List<String> labels;
    private float[] output;
    private String[] outputNames;

    @Override
    public String name() {
        return name;
    }

    @Override
    public Classification recognize(final byte pixels[])
    {
        tfHelper.feed(inputName, pixels, 1, inputSize, inputSize, 3);

        /*if (feedKeepProb)
            tfHelper.feed("keep_prob", new float[]{1});*/

        tfHelper.run(outputNames);
        tfHelper.fetch(outputName, output);
        Classification ans = new Classification();
        Log.d("debug", "L:"+output.length);

        for (int i = 0; i < output.length; i++)
        {
            Log.d("debug", output[i]+" out");
            if (output[i] > THRESHOLD && output[i] > ans.getConf())
                ans.update(output[i], labels.get(i));
        }
        return ans;
    }

    private static List<String> readLabels(AssetManager am, String filename) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(am.open(filename)));
        String line;
        List<String> labels = new ArrayList<>();
        while((line = br.readLine()) != null)
            labels.add(line);
        br.close();
        return labels;
    }

    public static TensorFlowClassifier create(AssetManager am, String name, String modelPath,
                                              String labelFile, int inputSize, String inputName,
                                              String outputName, boolean feedKeepProb) throws IOException
    {
        TensorFlowClassifier tfc = new TensorFlowClassifier();
        tfc.name = name;
        tfc.inputName = inputName;
        tfc.outputName = outputName;
        tfc.labels = readLabels(am, labelFile);
        tfc.tfHelper = new TensorFlowInferenceInterface(am, modelPath);
        tfc.inputSize = inputSize;
        tfc.outputNames = new String[]{outputName};
        tfc.outputName = outputName;
        tfc.output = new float[NUM_CLASSES];
        tfc.feedKeepProb = feedKeepProb;
        return tfc;
    }

}



















