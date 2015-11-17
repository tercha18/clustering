package org.dmtools.datamining.base;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.util.*;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * 
 * @author Piotr Lasek
 *
 */
public class ScatterAdd extends JFrame {

    private static final int N = 8;
    private static final int SIZE = 345;
    private static final String title = "Dataset";

    ArrayList<double[]> data;
    ArrayList<double[]> tempPoints;
    
    public ScatterAdd(String s, ArrayList<double[]> data, ArrayList<double[]> tempPoints) {
        super(s);
        this.data = data;
        this.tempPoints = tempPoints;
        final ChartPanel chartPanel = createDemoPanel();
        chartPanel.setPreferredSize(new Dimension(SIZE, SIZE));
        this.add(chartPanel, BorderLayout.CENTER);
    }

    private ChartPanel createDemoPanel() {
        JFreeChart jfreechart = ChartFactory.createScatterPlot(
            title, "X", "Y", createSampleData(),
            PlotOrientation.VERTICAL, true, true, false);
        XYPlot xyPlot = (XYPlot) jfreechart.getPlot();
        xyPlot.setDomainCrosshairVisible(true);
        xyPlot.setRangeCrosshairVisible(true);
        
        XYItemRenderer renderer = xyPlot.getRenderer();
        renderer.setSeriesPaint(0, Color.LIGHT_GRAY);
        
        renderer.setToolTipGenerator(new XYToolTipGenerator()
        {

			@Override
			public String generateToolTip(XYDataset arg0, int arg1, int arg2) {
				// TODO Auto-generated method stub
				return "abc";
			}
        	
        }
        );
        
        renderer.setBaseItemLabelsVisible(true);
        
        for (int i = 0; i <= 20; i++)
        {
        	renderer.setSeriesShape(i, new Ellipse2D.Float(-1,-1,1,1));
            
        	
        }
                
        adjustAxis((NumberAxis) xyPlot.getDomainAxis(), true);
        adjustAxis((NumberAxis) xyPlot.getRangeAxis(), false);
        //xyPlot.setBackgroundPaint(Color.white);
        return new ChartPanel(jfreechart);
    }

    private void adjustAxis(NumberAxis axis, boolean vertical) {
        //axis.setRange(0, 10.0);
        axis.setTickUnit(new NumberTickUnit(0.5));
        axis.setVerticalTickLabels(vertical);
    }

    private XYDataset createSampleData() {
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        
        ArrayList<Double> clusters = new ArrayList<Double>();
        clusters.add((double) 0);
        
        ArrayList<XYSeries> tempSeries = new ArrayList<XYSeries>();

        XYSeries noise = new XYSeries("NOISE");
        
        tempSeries.add(noise);
        
        		
        for(double[] point : data)
        {
        	if (point[point.length-1] >= 0)
        		if (!clusters.contains(point[point.length-1]))
        			clusters.add(point[point.length - 1]);
        }
        
        
        for(int i = 0; i < clusters.size(); i++)
        {
        	tempSeries.add(new XYSeries("Cluster " + i));
        	System.out.println("Cluster " + i);
        }
        
        // ADD NOISE
        
        
        for(double[] point : data)
        {
        	int id = (int) point[point.length-1];
        	if (id >= 0)
        	{
        		//System.out.println("tempSeries.size= " + tempSeries.size());
        		if(id >= tempSeries.size())
        			id = tempSeries.size() - 2;
        		
	        	XYSeries s = tempSeries.get(id + 1);
	        	s.add(new XYDataItem(point[0], point[1]));
        	}
        	else
        	{
        		XYSeries n = tempSeries.get(0);
        		n.add(new XYDataItem(point[0], point[1]));
        	}
        	System.out.println(Arrays.toString(point));
        }
        
        if (tempPoints != null)
        {
		    XYSeries ts = new XYSeries("Temp");
		
		    for(double[] point : tempPoints)
		    {
		    	ts.add(new XYDataItem(point[0], point[1]));
		    }
		    
		    xySeriesCollection.addSeries(ts);
        }
        
        //
        for(XYSeries s : tempSeries)
        {
        	xySeriesCollection.addSeries(s);	
        }
	                
        //xySeriesCollection.addSeries(added);
        
        
        return xySeriesCollection;
    }  
}
