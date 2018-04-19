package com.tiagoleite.detection;


public interface Classifier
{
    String name();
    Classification recognize(final byte pixels[], int w, int h);
}
