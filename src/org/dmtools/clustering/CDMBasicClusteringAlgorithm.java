package org.dmtools.clustering;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dmtools.clustering.algorithm.common.ClusteringAlgorithm;
import org.dmtools.clustering.model.IClusteringData;
import org.dmtools.clustering.old.ClusteringTimer;
import org.dmtools.clustering.old.DataSourceManager;
import org.dmtools.datamining.data.CDMAlgorithm;
import org.dmtools.datamining.data.CDMFilePhysicalDataSet;
import org.dmtools.datamining.resource.CDMBasicMiningObject;

import javax.datamining.JDMException;
import javax.datamining.MiningObject;
import javax.datamining.clustering.ClusteringSettings;
import javax.datamining.data.PhysicalAttribute;
import javax.datamining.data.PhysicalDataSet;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 * @author Piotr Lasek
 *
 */
public abstract class CDMBasicClusteringAlgorithm implements CDMAlgorithm {

	protected MiningObject result;
	protected ArrayList<double[]> data;
	protected double[] min = null;
	protected double[] max = null;
	protected Collection<PhysicalAttribute> attributes;
	protected ClusteringSettings clusteringSettings;
	protected PhysicalDataSet physicalDataSet;
	protected int numberOfDimensions;
	protected ClusteringTimer timer = new ClusteringTimer();
	protected ClusteringAlgorithm algorithm;
	protected CDMBasicMiningObject basicMiningObject = new CDMBasicMiningObject();

	protected final static Logger log =
        LogManager.getLogger(CDMBasicClusteringAlgorithm.class.getSimpleName());

	/**
	 *
	 * @param clusteringSettings
	 * @param physicalDataSet
	 */
	public CDMBasicClusteringAlgorithm(ClusteringSettings clusteringSettings,
			PhysicalDataSet physicalDataSet) {
		this.clusteringSettings = clusteringSettings;
		this.physicalDataSet = physicalDataSet;

		if (physicalDataSet != null) {
			try {
				attributes = physicalDataSet.getAttributes();
			} catch (JDMException e) {
				e.printStackTrace();
			}

			numberOfDimensions = attributes.size();

			min = new double[numberOfDimensions];
			max = new double[numberOfDimensions];
		} else {
			log.warn("Could not read dataset parameters!");
		}
	}

	/**
	 *
	 * @return
	 */
	public double[] getMin() {
		return min;
	}

	/**
	 *
	 * @return
	 */
	public double[] getMax() {
		return max;
	}

	@Override
	/**
	 * 
	 */
	public MiningObject run() {
		return null;
	}
	
	/**
	 * 
	 * @return
	 */
	public PhysicalDataSet getPhysicalDataSet() {
		return physicalDataSet;
	}

	/**
     *
     */
    public IClusteringData prepareData() {
		ArrayList<Object[]> rawData =
				((CDMFilePhysicalDataSet) getPhysicalDataSet()).getData();

		Collection<PhysicalAttribute> attributes = null;

		data = new ArrayList();

		int i = 0;

		try {
			attributes = getPhysicalDataSet().getAttributes();

			for (Object[] rawRecord : rawData) {
				double[] record = new double[attributes.size() + 1];
				int d = 0;
				for (PhysicalAttribute attribute : attributes) {
					record[d] = new Double(rawData.get(i)[d].toString());
					if (min[d] == 0)
						min[d] = record[d];
					else {
						if (min[d] > record[d]) min[d] = record[d];
						if (max[d] < record[d]) max[d] = record[d];
					}
					d++;
				}
				record[d] = -1; // UNCLUSTERED
				data.add(record);
				i++;
			}

		} catch (Exception e) {
			log.error("An error occured at record number: " + i);
			log.error(e.getStackTrace());
			System.exit(0);
		}

		DataSourceManager dsm = new DataSourceManager();

		dsm.readData("abc", data);
		dsm.setActiveDataSource("abc");

		return dsm.getActiveDataSource().getData();
	}

	/**
	 *
	 * @return
	 */
	public boolean dump() {
		if (((CDMBaseAlgorithmSettings) clusteringSettings.getAlgorithmSettings()).dump())
			return true;
		else
			return false;
	}

	/**
	 *
	 * @return
	 */
	public boolean closePlot() {
		if (((CDMBaseAlgorithmSettings) clusteringSettings.getAlgorithmSettings()).closePlot())
			return true;
		else
			return false;
	}

	/**
	 *
	 * @return
	 */
	public boolean plot() {
		if (((CDMBaseAlgorithmSettings) clusteringSettings.getAlgorithmSettings()).plot())
			return true;
		else
			return false;
	}

	public abstract String getDescription();

	public ClusteringTimer getInternalTimer() {
		return timer;
	}
}
