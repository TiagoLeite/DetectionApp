package com.tiagoleite.detection;


public interface Classifier
{
    String name();
    Classification recognize(final float pixels[], int w, int h);
}
