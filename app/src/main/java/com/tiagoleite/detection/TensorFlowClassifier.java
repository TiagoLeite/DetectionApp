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
    private static final int[] NUM_CLASSES = new int[]{1200, 1200, 1200};
    private TensorFlowInferenceInterface tfHelper;
    private String name, inputName;
    private boolean feedKeepProb;
    private SparseArray<String> labels;
    private float[][] outputs;
    private String[] outputNames;
    private int[] outputInt;

    @Override
    public String name() {
        return name;
    }

    @Override
    public Classification recognize(final float[] pixels, int w, int h)
    {
        tfHelper.feed(inputName, pixels, 1, h, w, 3);

        /*if (feedKeepProb)
            tfHelper.feed("keep_prob", new float[]{1});*/

        tfHelper.run(outputNames);

        tfHelper.fetch(outputNames[0], outputs[0]);
        tfHelper.fetch(outputNames[1], outputs[1]);
        tfHelper.fetch(outputNames[2], outputInt);


        Classification ans = new Classification();
        Log.d("debug", "class size:"+outputs[0].length);
        Log.d("debug", "box size:"+outputs[1].length);
        Log.d("debug", "score size:"+outputInt.length);

        /*for (int i = 0; i < outputs[0].length; i++)
        {
            Log.d("debug", outputs[i]+" out");
            if (outputs[0][i] > THRESHOLD && outputs[0][i] > ans.getConf())
                ans.update(outputs[0][i], labels.get(i));
        }*/

        ans.update(outputs[2][0], labels.get((int)(outputs[0][0])), outputs[1]);

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
                                              String labelFile, String inputName,
                                              String[] outputNames, boolean feedKeepProb) throws IOException
    {
        TensorFlowClassifier tfc = new TensorFlowClassifier();
        tfc.name = name;
        tfc.inputName = inputName;
        tfc.labels = readLabels(am, labelFile);
        tfc.tfHelper = new TensorFlowInferenceInterface(am, modelPath);
        tfc.outputNames = outputNames;

        tfc.outputs = new float[3][];
        tfc.outputs[0] = new float[NUM_CLASSES[0]];
        tfc.outputs[1] = new float[NUM_CLASSES[1]];
        tfc.outputs[2] = new float[NUM_CLASSES[2]];

        tfc.outputInt = new int[NUM_CLASSES[2]];

        tfc.feedKeepProb = feedKeepProb;
        return tfc;
    }

}



















