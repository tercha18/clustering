package org.dmtools.clustering.algorithm.PiMeans;

/**
 * Created by Piotr Lasek on 11.10.2017.
 */
public class PiPoint {
    protected double[] coordinates;
    protected PiBin bin;

    /**
     *
     * @param coordinates
     */
    public PiPoint(double[] coordinates, PiBin bin) {
        this.coordinates = coordinates;
        this.bin = bin;
    }
}
