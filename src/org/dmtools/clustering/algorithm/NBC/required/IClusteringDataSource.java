package org.dmtools.clustering.algorithm.NBC.required;

public interface IClusteringDataSource
{
	public void setData(IClusteringData clusteringOutput);
	public IClusteringData getData();
	
	public void showDataSource();
	public void saveDataSource();
	
	public void setId(String id);
	public String getId();
	
	public String toString();
	public void close();
}
