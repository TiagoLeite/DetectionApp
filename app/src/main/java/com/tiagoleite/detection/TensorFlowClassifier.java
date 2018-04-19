package com.tiagoleite.detection;

import android.content.res.AssetManager;
import android.util.Log;
import android.util.SparseArray;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TensorFlowClassifier implements Classifier
{
    private static final float THRESHOLD = 0.1f;
    private static final int NUM_CLASSES = 100;
    private TensorFlowInferenceInterface tfHelper;
    private String name, inputName, outputName;
    private int inputSize;
    private boolean feedKeepProb;
    private SparseArray<String> labels;
    private float[] output;
    private String[] outputNames;

    @Override
    public String name() {
        return name;
    }

    @Override
    public Classification recognize(final byte pixels[], int w, int h)
    {
        tfHelper.feed(inputName, pixels, 1, w, h, 3);

        /*if (feedKeepProb)
            tfHelper.feed("keep_prob", new float[]{1});*/

        tfHelper.run(outputNames);
        tfHelper.fetch(outputName, output);
        Classification ans = new Classification();
        Log.d("debug", "out size:"+output.length);

        for (int i = 0; i < output.length; i++)
        {
            Log.d("debug", output[i]+" out");
            if (output[i] > THRESHOLD && output[i] > ans.getConf())
                ans.update(output[i], labels.get(i));
        }
        ans.update(output[0], labels.get((int)(output[0])));

        return ans;
    }

    private static SparseArray<String> readLabels(AssetManager am, String filename) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(am.open(filename)));
        String line;
        SparseArray<String> labels = new SparseArray<>();
        while((line = br.readLine()) != null)
        {
            String tokens[] = line.split(":");
            labels.put(Integer.parseInt(tokens[0]), tokens[1]);
        }
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



















