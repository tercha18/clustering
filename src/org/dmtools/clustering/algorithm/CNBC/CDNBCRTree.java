package org.dmtools.clustering.algorithm.CNBC;

import org.dmtools.clustering.CDMCluster;
import org.dmtools.clustering.model.*;
import spatialindex.spatialindex.*;
import spatialindex.spatialindex.ISpatialIndex;
import spatialindex.storagemanager.*;
import spatialindex.rtree.*;

import java.awt.Graphics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import org.dmtools.clustering.old.*;


/**
 * 
 * @author Piotr Lasek
 */
public class CDNBCRTree implements IClusteringAlgorithm {
    
    public static final String NAME = "CD-NBC-RTree                     ";
    public static final int NOISE = -1;
    
    String description;

    IClusteringObserver observer;
    ISpatialIndex tree;
    IClusteringParameters parameters;

    ArrayList<CNBCRTreePoint> Dataset;
    
    ClusteringLogger logger = new ClusteringLogger(getName());
    
    InstanceConstraints ic = new InstanceConstraints();
    
    ArrayList<IConstraintObject> deferred = new ArrayList<>();

    int nDim = 0;
    //int id = 0;
    int k;
    //String desc = "NBC (RTree): ";

    /** Creates a new instance of NBC */
    public void run() {
    	    	
        logger.addDescription(this.getDescription());
        //long begin_time1 = System.currentTimeMillis();
        logger.clusteringStart();

        ArrayList<CNBCRTreePoint> NoiseSet = new ArrayList();
        int cluster_count = 0;

        CalcNDF();
        
        NoiseSet.clear();
        cluster_count = 0;
        ArrayList<CNBCRTreePoint> DPSet = new ArrayList();

        // Setting cannot-link points to NOISE
        ArrayList<IConstraintObject> def = new ArrayList<>();
        def.addAll(ic.cl1);
        def.addAll(ic.cl2);
        
        markDeferred(def);
        

        // for each object p in Dataset
        ListIterator li = Dataset.listIterator();
        while (li.hasNext()) {
            CNBCRTreePoint p = (CNBCRTreePoint) li.next();
            // if (p.getClusterId() != NULL or p.ndf < 1)) continue;
            if (p.getClusterId() != CDMCluster.UNCLASSIFIED || p.getClusterId() == CDMCluster.DEFERRED || p.ndf < 1)
                continue;
            p.setClusterId(cluster_count); // label a new cluster
            DPSet.clear(); // initialize DPSet

            MyVisitor kNN = new MyVisitor();
            tree.nearestNeighborQuery(k, p, kNN);
            
            // applying cannot-link constraints
            boolean existsCannotLink = ic.existsCannotLink(kNN.neighbours);
            // CANNOT-LINK
            if (existsCannotLink){
            	for (int i = 0; i < kNN.neighbours.size(); i++) {
            		CNBCRTreePoint pcl1 = (CNBCRTreePoint) kNN.neighbours.get(i);
                    pcl1.setClusterId(CDMCluster.NOISE);
                }
                continue;
            } // end of applying cannot-link constraints
            else {
	            for (int i = 0; i < kNN.neighbours.size(); i++) {
	                // q.clst_no = cluster_count
	                CNBCRTreePoint q = (CNBCRTreePoint) kNN.neighbours.get(i);
	                
	                if (q.getClusterId() != CDMCluster.DEFERRED) {
		                q.setClusterId(cluster_count);
		                if (q.ndf >= 1) {
		                    DPSet.add(q);
	
		                    // MUST-LINK
		                    ArrayList<IConstraintObject> mls = ic.getMustLinkObjects(q);
		                    if (mls.size() > 0) {
                                for (IConstraintObject ico : mls) {
                                    DPSet.add((CNBCRTreePoint) ico);
                                }
		                    	// DPSet.addAll(mls);
		                    }
		                }
	                }
	            }
            }

            // while (DPSet is not empty) // expanding the cluster
            while (!DPSet.isEmpty()) {
                // p = DPset.getFirstObject();
                CNBCRTreePoint pD = (CNBCRTreePoint) DPSet.remove(0);

                // for each object q in kNB(pD)
                MyVisitor kNNpD = new MyVisitor();
                tree.nearestNeighborQuery(k, pD, kNNpD);

                ListIterator<CNBCRTreePoint> liKNNpD =
                		kNNpD.neighbours.listIterator();

                while (liKNNpD.hasNext()) {
                    CNBCRTreePoint q = (CNBCRTreePoint) liKNNpD.next();
                    // if (q.clst_no!NULL) continue
                    if (q.getClusterId() != CDMCluster.UNCLASSIFIED)
                        continue;

                    q.setClusterId(cluster_count); // TODO cluster count

                    // if (q.ndf >= 1) DPSet.add(q)
                    if (q.ndf >= 1) {
                        DPSet.add(q);
                        // MUST-LINK
	                    ArrayList<IConstraintObject> mls = ic.getMustLinkObjects(q);
	                    if (mls.size() > 0) {
                            for (IConstraintObject ico : mls) {
                                DPSet.add((CNBCRTreePoint) ico);
                            }
//	                    	DPSet.addAll(mls);
	                    }
                    }

                    ArrayList<IConstraintObject> mls = ic.getMustLinkObjects(q);
                    for(IConstraintObject ico : mls) {
                        DPSet.add((CNBCRTreePoint) ico);
                    }
//                    DPSet.addAll(mls);
                }
                // DPSet.remove(p);
            }
            cluster_count++;
        }

        
        ArrayList<IConstraintObject> cl = new ArrayList<>();
        cl.addAll(ic.cl1);
        cl.addAll(ic.cl2);
        
        recluster2(cl);
        
        // recluster();
        recluster2(deferred);
        
       
        // for each object p in Dataset // label noise
        li = Dataset.listIterator(); // TODO Add dump method. Dataset is ArrayList<>
        while (li.hasNext()) {
            // if (p.getClusterId()=NULL) NoiseSet.add(p)
            CNBCRTreePoint p = (CNBCRTreePoint) li.next();
            if (p.getClusterId() == CDMCluster.UNCLASSIFIED)
                p.setClusterId(CDMCluster.NOISE);
        }
        
        logger.clusteringEnd();
        
        System.out.println(logger.getLog());
    }

    private CNBCRTreePoint getNearestCL(CNBCRTreePoint p, int kn) {
    	CNBCRTreePoint cl = null;
    	
    	ArrayList<CNBCRTreePoint> neighbors = getNeighbors(p, kn);
    	
    	for(CNBCRTreePoint nb : neighbors) {
    		if (nb.isCannotLinkPoint()) {
    			cl = nb;
    			break;
    		}
    	}
    	
    	return cl;
    }
    
	private void markDeferred(ArrayList<IConstraintObject> def) {
		for(IConstraintObject p : def) {
	        MyVisitor kNN = new MyVisitor();
	        tree.nearestNeighborQuery(k, (IShape) p, kNN);
	        
            p.setClusterId(CDMCluster.DEFERRED);
            p.setParentCannotLinkPoint(p);
            //p.wasDeferred = true;
            deferred.add(p);
            if (!deferred.contains(p)) {
            	deferred.add(p);
            }

	        for (int i = 0; i < kNN.neighbours.size(); i++) {
	        	CNBCRTreePoint pcl1 = (CNBCRTreePoint) kNN.neighbours.get(i);
	            pcl1.setClusterId(CDMCluster.DEFERRED);
	            pcl1.setParentCannotLinkPoint(p);
	            //pcl1.wasDeferred = true;
	            if (!deferred.contains(pcl1)) {
	            	deferred.add(pcl1);
	            }
	        }
        }
	}

    private void recluster2(ArrayList<IConstraintObject> in) {
    	ArrayList<CNBCRTreePoint> toRemove = new ArrayList<CNBCRTreePoint>();
        int step= 0;
        do {
        	toRemove.clear();
        	
        	for(IConstraintObject p : in) {
        		
        		ArrayList<CNBCRTreePoint> neighbors = getNeighbors((CNBCRTreePoint) p, 2*k);
        		
        		for(CNBCRTreePoint q : neighbors) {
        			if (q.ndf >= 1 && q.getClusterId() > CDMCluster.NOISE  && !q.wasDeferred()) {
        				
        				boolean cl1 = false;
        				
						 if (ic.cannotAsignToGroup(p, q.getClusterId())) {
							 cl1 = true;
						 }
						 
						 if (ic.cannotAsignToGroup(getNearestCL((CNBCRTreePoint) p, k), q.getClusterId())) {
							 cl1 = true;
						 }
						 
						/*   if (q.parentCL != null && ic.cannotLink(p.parentCL, q.parentCL)) {
							 cl1 = true;
							}*/

        				if (!cl1 && q.ndf >=1 /*&& q.referseNeighbors.contains(p)*/) {
	        				//if (p.ndf >= 1) {
	        					p.setClusterId(q.getClusterId());
		        				p.wasDeferred(true);
		        				toRemove.add((CNBCRTreePoint) p);
		        				break;
//	        				} else {
//	        					p.wasDeferred = true;
//	        					p.getClusterId() = NOISE;
//	        					toRemove.add(p);
//	        					break;
//	        				}
        				}
        			} 
        				
        		}
        		//System.out.println();
        	}
        	
        	in.removeAll(toRemove);
        	
        } while (toRemove.size() > 0);
    }
    
	private void recluster() {
		ArrayList<CNBCRTreePoint> toRemove = new ArrayList<CNBCRTreePoint>();
        int step= 0;
        do {
        	toRemove.clear();
        	
        	for(IConstraintObject p : deferred) {

                ArrayList<CNBCRTreePoint> neighbors = getNeighbors((CNBCRTreePoint) p, k);
 
                for (CNBCRTreePoint q  : neighbors) {
                	 if (q.ndf >=1 && q.getClusterId() >= CDMCluster.NOISE && !q.wasDeferred() ) {
                		 boolean cl1 = false;
            		
                		 
                		 /*
                		 ArrayList<CNBCRTreePoint> nn = getNeighbors(p,  k);
                		 
                		 for(CNBCRTreePoint ctp:nn) {
                			 if (ic.cannotAsignToGroup(ctp,  q.clst_no)) {
                				 cl1 = true;
                				 break;
                			 }
                		 }
                		 */
                		 
                		 if (ic.cannotAsignToGroup(p, q.getClusterId())) {
                			 cl1 = true;
                		 }
                		 
                		 if (ic.cannotAsignToGroup(p.getParentCannotLinkPoint(), q.getClusterId())) {
                			 cl1 = true;
                		 }
                		 
                		 if (ic.cannotLink(p.getParentCannotLinkPoint(), q.getParentCannotLinkPoint())) {
                			 cl1 = true;
                		 }
                		 
                		 if (!cl1) {
        					 p.setClusterId(q.getClusterId());
	                		 p.wasDeferred(true);
	                		 toRemove.add((CNBCRTreePoint) p);
	        			 }
                		 
                		 break;
                	 }
                }
        	}
        	
        	deferred.removeAll(toRemove);
        	step++;
        	
        } while(toRemove.size() > 0 && step < 5);
        
        for (IConstraintObject tp : deferred) {
        	
        	
        	ArrayList<CNBCRTreePoint> nbs = getNeighbors((CNBCRTreePoint) tp, k);
        	
        	for (CNBCRTreePoint n : nbs) {
        		if (!n.wasDeferred() && n.getClusterId() > CDMCluster.NOISE) {
        			
        		}
        	}
        }
	}

    /**
     *
     * @param p
     * @param q
     * @param clusterId
     * @return
     */
    public boolean canBeAssigned(CNBCRTreePoint p, CNBCRTreePoint q, int clusterId) {
    	boolean can = true;
    	
    	boolean c1 = ic.cannotLinkExt(p,  q);
    	
    	return can;
    }


    /**
     *
     * @param p
     * @param k
     * @return
     */
    public CNBCRTreePoint getFirtsNeighbor(CNBCRTreePoint p, int k) {
    	ArrayList<CNBCRTreePoint> points = getNeighbors(p,  k);
    	CNBCRTreePoint px = null;
    	for(CNBCRTreePoint pt:points) {
    		if (pt.ndf >= 1 && pt.getClusterId() > CDMCluster.NOISE) {
    			px = pt;
    			break;
    		}
    	}
    	return px;
    }
    
    /**
     * 
     * @param p
     * @param k
     * @return
     */
    public ArrayList<CNBCRTreePoint> getNeighbors(CNBCRTreePoint p, int k) {
    	MyVisitor $1NN = new MyVisitor();
        tree.nearestNeighborQuery(k+1, p, $1NN);

        ListIterator<CNBCRTreePoint> n =
        		$1NN.neighbours.listIterator();
        
        ArrayList<CNBCRTreePoint> neighbors = new ArrayList<CNBCRTreePoint>();
        
        if (n.hasNext()) n.next();
        
        while(n.hasNext()) {
        	neighbors.add(n.next());
        }
        
		return neighbors;

    }

    /**
     * 
     */
    private void CalcNDF() {
        ListIterator li = Dataset.listIterator();
        li = Dataset.listIterator();
        ArrayList CandidateSet = new ArrayList();
        MyVisitor kNN = new MyVisitor();

        // for each object p in Dataset
        while (li.hasNext()) {
            CNBCRTreePoint p = (CNBCRTreePoint) li.next();
            CandidateSet.clear();

            kNN.reset();
            tree.nearestNeighborQuery(k, p, kNN);
            p.numberOfkNB = kNN.kNB;
            // System.out.println("p: " + p.excell());

            // for each new object q in kNB(p)
            ListIterator lni = kNN.neighbours.listIterator();
            while (lni.hasNext()) {
                CNBCRTreePoint q = (CNBCRTreePoint) lni.next();
                q.numberOfRkNB++;
                q.reverseNeighbors.add(p);
                // System.out.println("q: " + q.excell());
            }
        }

        // for each object q in kNB(p)
        li = Dataset.listIterator();
        while (li.hasNext()) {
            CNBCRTreePoint p = (CNBCRTreePoint) li.next();
            p.ndf = (double) p.numberOfRkNB / p.numberOfkNB;
        }
    }

    /**
     * 
     * @throws IOException
     */
    public void initRTree() throws IOException {
        /*PropertySet ps = new PropertySet();
        Boolean b = new Boolean(true);
        ps.setProperty("Overwrite", b);
        ps.setProperty("FileName", "nbc-rtree");
        Integer i = new Integer(128);
        ps.setProperty("PageSize", i);*/
        
        PropertySet ps = new PropertySet();
        ps.setProperty("FileName", "nbc-rtree");
        ps.setProperty("FillFactor", 0.1);
        ps.setProperty("IndexCapacity", 32);
        ps.setProperty("LeafCapacity", 32);
        ps.setProperty("Dimension", nDim);

        MemoryStorageManager memmanag = new MemoryStorageManager();
        IBuffer mem = new RandomEvictionsBuffer(memmanag, 40000, false);

        tree = new RTree(ps, mem);
    }

    /**
     * 
     * The implementation of the IVisitor interface.
     *
     */
    class MyVisitor implements IVisitor {
        public int m_indexIO = 0;
        public int m_leafIO = 0;
        public int kNB = 0;

        ArrayList<CNBCRTreePoint> neighbours = new ArrayList<CNBCRTreePoint>();
        ArrayList<IData> n = new ArrayList<IData>();

        public void reset() {
            kNB = 0;
            neighbours.clear();
        }

        public void visitNode(final INode n) {
            if (n.isLeaf())
                m_leafIO++;
            else
                m_indexIO++;
        }

        public void visitData(final IData d) {
            kNB++;
            int id = d.getIdentifier();
            neighbours.add(Dataset.get(id));
            n.add(d);
        }
    }
    
    public InstanceConstraints getConstraints()
    {
    	return ic;
    }

    @Override
    public IClusteringData getResult() {
        BasicClusteringData bcd = new BasicClusteringData();
        ArrayList<IClusteringObject> al = new ArrayList<IClusteringObject>();
        
        for (Object o : Dataset) {
            CNBCRTreePoint mp = (CNBCRTreePoint) o;
            BasicClusteringObject bco = new BasicClusteringObject();
            BasicSpatialObject rso = new BasicSpatialObject(mp.m_pCoords);
            bco.setSpatialObject(rso);
            BasicClusterInfo bci = new BasicClusterInfo();
            bci.setClusterId(mp.getClusterId());
            bco.setClusterInfo(bci);
            bco.addParameter("wasDeferred", mp.wasDeferred() ? "true" : "false");
            al.add(bco);
        }
        
        bcd.set(al);

        return bcd;
    }

    @Override
    public void setData(IClusteringData data) {
        logger.indexStart();
        ArrayList<IClusteringObject> tmp = (ArrayList<IClusteringObject>) data
                .get();
        Dataset = new ArrayList();
        nDim = data.get().iterator().next().getSpatialObject().getValues().length;
        
        try {
            initRTree();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }
        
        int id = 0;
        
        // building R-Tree
        byte[] d = new byte[]{CDMCluster.UNCLASSIFIED};
        for (IClusteringObject ico : tmp) {
            CNBCRTreePoint mp = new CNBCRTreePoint(ico.getSpatialObject()
                    .getValues(), CDMCluster.UNCLASSIFIED);
            Dataset.add(id, mp);
            tree.insertData(d, mp, id);
            id++;
        }
        logger.indexEnd();
        
        // SETTING NUMBER OF CONSTRAINTS
        
//         randomConstraints(10, 10, Dataset.size());
    	setConstraints();
    }

    
    private void randomConstraints(int mlCount, int clCount, int datasetSize) {

    	int index = 0;
    	
    	for (int i = 0; i < mlCount; i++) {
    		index = drawIndex(datasetSize);
    		CNBCRTreePoint p0 = Dataset.get(index);
    		index = drawIndex(datasetSize);    		
    		CNBCRTreePoint p1 = Dataset.get(index);
    		ic.addMustLinkPoints(p0, p1);
    	}
    	
    	for (int i = 0; i < clCount; i++) {
    		index = drawIndex(datasetSize);
    		CNBCRTreePoint p0 = Dataset.get(index);
    		index = drawIndex(datasetSize);    		
    		CNBCRTreePoint p1 = Dataset.get(index);
    		ic.addCannotLinkPoints(p0,  p1);
    	}
    }
    
    ArrayList<Integer> indexes = new ArrayList<Integer>();
    
    public int drawIndex(int datasetSize) {
    	
    	int index = -1;
    	
    	do {
    		index = (int) (Math.random() * datasetSize);
    	} while (indexes.contains(index));
    	
    	indexes.add(index);    	
    	
    	return index;
    	
    }
    
	private void setConstraints() {
	        SetConstraints.forCNBC(Dataset, ic);

// 		CNBCRTreePoint p0 = new CNBCRTreePoint(new double[]{438.0, 259.0}, CDMCluster.UNCLASSIFIED);
// 		CNBCRTreePoint p1 = new CNBCRTreePoint(new double[]{440.0, 255.0}, CDMCluster.UNCLASSIFIED);
// 		CNBCRTreePoint p2, p3, p4; 

// 		p0 = new CNBCRTreePoint(new double[]{443.0, 271.0}, CDMCluster.UNCLASSIFIED);
// 		p0 = Dataset.get(2730);
// 		p1 = new CNBCRTreePoint(new double[]{448.0, 278.0}, CDMCluster.UNCLASSIFIED);
// 		p2 = new CNBCRTreePoint(new double[]{421.0,333.0}, CDMCluster.UNCLASSIFIED);
// 		p3 = new CNBCRTreePoint(new double[]{433.0,325.0}, CDMCluster.UNCLASSIFIED);

// 		p0 = Dataset.get(Dataset.indexOf(p0));
// 		p1 = Dataset.get(Dataset.indexOf(p1));
// 		p2 = Dataset.get(Dataset.indexOf(p2));
// 		p3 = Dataset.get(Dataset.indexOf(p3));

// 		ic.addCannotLinkPoints(p0,  p1);
// 		ic.addCannotLinkPoints(p2,  p3);

// 		p0 = new CNBCRTreePoint(new double[]{457.0, 334.0}, CDMCluster.UNCLASSIFIED);
// 		p1 = new CNBCRTreePoint(new double[]{467.0, 340.0}, CDMCluster.UNCLASSIFIED);
// 		p2 = new CNBCRTreePoint(new double[]{475.0, 348.0}, CDMCluster.UNCLASSIFIED);
// 		p3 = new CNBCRTreePoint(new double[]{478.0, 356.0}, CDMCluster.UNCLASSIFIED);
// 		p4 = new CNBCRTreePoint(new double[]{508.0, 373.0}, CDMCluster.UNCLASSIFIED);

// 		p0 = Dataset.get(Dataset.indexOf(p0));
// 		p1 = Dataset.get(Dataset.indexOf(p1));
// 		p2 = Dataset.get(Dataset.indexOf(p2));
// 		p3 = Dataset.get(Dataset.indexOf(p3));
// 		p4 = Dataset.get(Dataset.indexOf(p4));    	

// 		ic.addCannotLinkPoints(p0, p1);
// 		ic.addCannotLinkPoints(p1, p2);
// 		ic.addCannotLinkPoints(p2, p3);
// 		ic.addCannotLinkPoints(p3, p4);

// 		p0 = new CNBCRTreePoint(new double[]{326.0, 314.0}, CDMCluster.UNCLASSIFIED);
// 		p1 = new CNBCRTreePoint(new double[]{359.0, 369.0}, CDMCluster.UNCLASSIFIED);
// 		p0 = Dataset.get(Dataset.indexOf(p0));
// 		p1 = Dataset.get(Dataset.indexOf(p1));
// 		ic.addMustLinkPoints(p0, p1);
	}

    @Override
    public IClusteringParameters getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(IClusteringParameters parameters) {
        this.parameters = parameters;
        k = new Integer(parameters.getValue("k")).intValue();
        logger.setParameters(parameters.toString());
    }

    @Override
    public void setObserver(IClusteringObserver observer) {
        // TODO Auto-generated method stub
        this.observer = observer;
    }

    public void setGraphics(Graphics g) {

    }

    public void addLines() {
        this.observer.handleNotify((Object) null);
    }

    @Override
    public String getName() {
        return CDNBCRTree.NAME;
    }

    /**
     * 
     */
    public void addDescription(String description) {
        this.description = description;
    }
    
    /**
     * 
     */
    public String getDescription() {
        return description;
    }    
}