package org.dmtools.clustering.algorithm.DBSCAN;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dmtools.clustering.CDMBasicClusteringAlgorithm;
import org.dmtools.clustering.algorithm.CNBC.MyFrame;
import org.dmtools.clustering.model.IClusteringData;
import org.dmtools.clustering.model.IClusteringDataSource;
import org.dmtools.clustering.model.IClusteringObject;
import org.dmtools.clustering.old.DataSourceManager;
import org.dmtools.datamining.data.CDMFilePhysicalDataSet;
import util.Dump;

import javax.datamining.JDMException;
import javax.datamining.MiningObject;
import javax.datamining.clustering.ClusteringSettings;
import javax.datamining.data.PhysicalAttribute;
import javax.datamining.data.PhysicalDataSet;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

public class DBSCANAlgorithm extends CDMBasicClusteringAlgorithm {

    private final static Logger log = LogManager.getLogger("DBSCANAlgorithm");

    public static final String NAME = "DBSCAN";
    private Collection<PhysicalAttribute> attributes;
    private final int numberOfDimensions;
    private final double[] min;
    private final double[] max;
    private final DBSCANAlgorithmSettings algorithmSettings;
    private double Eps;
    private int MinPts;
    ArrayList<double[]> data;

    public DBSCANAlgorithm(ClusteringSettings clusteringSettings, PhysicalDataSet physicalDataSet) {
        super(clusteringSettings, physicalDataSet);

        try {
            attributes = physicalDataSet.getAttributes();
        } catch (JDMException e) {
            e.printStackTrace();
        }

        numberOfDimensions = attributes.size();

        min = new double[numberOfDimensions];
        max = new double[numberOfDimensions];

        algorithmSettings = (DBSCANAlgorithmSettings) clusteringSettings.getAlgorithmSettings();
        Eps = algorithmSettings.getEps();
        MinPts = algorithmSettings.getMinPts();
    }

    @Override
    public MiningObject run() {
        log.info(NAME + " start...");

        log.info("Preparing data...");
        prepareData();

        log.info("");
        DBSCANRTree dbscan = new DBSCANRTree();

        DataSourceManager dsm = new DataSourceManager();

        dsm.readData("abc", data);
        dsm.setActiveDataSource("abc");

        IClusteringDataSource cds = dsm.getActiveDataSource();

        dbscan.setEps(Eps);
        dbscan.setMinPts(MinPts);

        dbscan.setData(cds.getData());

        dbscan.run();
        IClusteringData cd = dbscan.getResult();
        String uri = getPhysicalDataSet().getURI();

        String dumpFileName = "dbscan-rtree-" + getPhysicalDataSet().getDescription() + ".csv";
        Dump.toFile(cd.get(), dumpFileName, true); //data to dump

        Collection<IClusteringObject> result = cd.get();
        ArrayList<double[]> data2 = new ArrayList<double[]>(result.size());
        int i = 0;

        for (IClusteringObject o : result) {
            double[] coord = o.getSpatialObject().getValues();
            double[] r = new double[coord.length + 1];
            System.arraycopy(coord, 0, r, 0, coord.length);
            r[r.length - 1] = o.getClusterInfo().getClusterId();
            data2.add(r);
            i++;
        }

        // Show result
        MyFrame mf = new MyFrame(result, null, null, null, null);
        mf.setPreferredSize(new Dimension(700, 600));
        JFrame f = new JFrame();
        JScrollPane scrollPane = new JScrollPane(mf);
        mf.setScrollPane(scrollPane);
        scrollPane.setAutoscrolls(true);
        f.add(scrollPane);
        f.pack();
        f.setSize(new Dimension(700, 600));
        f.setVisible(true);

        return null;
    }

    public void prepareData()
	{
		ArrayList<Object[]> rawData =
				((CDMFilePhysicalDataSet) getPhysicalDataSet()).getData();
		data = new ArrayList<double[]>();

		int i = 0;
		for(Object[] rawRecord : rawData) {
			double[] record = new double[attributes.size() + 1];
			int d = 0;
			for(PhysicalAttribute attribute : attributes)
			{
				record[d] = new Double(rawData.get(i)[d].toString());
				if (min[d] == 0)
					min[d] = record[d];
				else
				if (min[d] > record[d]) min[d] = record[d];
				if (max[d] < record[d]) max[d] = record[d];
				d++;
			}
			record[d] = -1; // UNCLUSTERED
			data.add(record);
			i++;
		}
	}
}
