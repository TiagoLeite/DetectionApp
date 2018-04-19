package com.tiagoleite.detection;

public class Classification
{
    private float conf;
    private String label;
    private float[] box;

    Classification(){
        this.conf = -1.0f;
        this.label = null;
    }

    void update(float conf, String label, float[] box)
    {
        this.conf = conf;
        this.label = label;
        this.box = box;
    }

    public String getLabel()
    {
        return label;
    }

    public float getConf()
    {
        return conf;
    }

}
