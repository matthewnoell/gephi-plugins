package com.raytheon.statistics.plugin;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Table;
import org.gephi.graph.api.Column;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.TempDirUtils;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EdgeMetrics implements Statistics, LongTask {
    
    private static final Logger LOG = Logger.getLogger("com.raytheon.statistics.plugin");
    
    private Column cell = null;
    private Column origin_x = null;
    private Column origin_y = null;

    public static final String EDGE_LENGTH = "length";
    public static final String CELL = "cell";
    public static final String ORIGIN_X = "origin_x";
    public static final String ORIGIN_Y = "origin_y";
    
    private double[] edgeLength;
    
    private int TOTAL_EDGES;
    private int edgeCount;
    private double avgEdgeLength;
    
    /**
     * Remembers if the Cancel function has been called.
     */
    private boolean isCanceled;
    private boolean isNormalized;
    private boolean isDirected;
    /**
     * Keep track of the work done.
     */
    private ProgressTicket progress;
    /**
     *
     */
    

    /**
     *
     * @return
     */
    public double getAverageEdgeLength() {
        LOG.log(Level.INFO, "EdgeMetrics.getAverageEdgeLength()");
        return avgEdgeLength;
    }

    /**
     *
     * @param graphModel
     */
    @Override
    public void execute(GraphModel graphModel) {
        LOG.log(Level.INFO, "EdgeMetric.execute() ovverride");
        Table nodeTable = graphModel.getNodeTable();
        for (int i=0; i<nodeTable.countColumns(); i++) {
            if (nodeTable.getColumn(i).getTitle().equals(CELL)) {
                cell = nodeTable.getColumn(i);
            }
            if (nodeTable.getColumn(i).getTitle().equals(ORIGIN_X)) {
                origin_x = nodeTable.getColumn(i);
            }
            if (nodeTable.getColumn(i).getTitle().equals(ORIGIN_Y)) {
                origin_y = nodeTable.getColumn(i);
            }   
        }
        isDirected = graphModel.isDirected();

        Graph graph;
        if (isDirected) {
            graph = graphModel.getDirectedGraphVisible();
        } else {
            graph = graphModel.getUndirectedGraphVisible();
        }
        execute(graph);
    }

    public void execute(Graph graph) {
        LOG.log(Level.INFO, "EdgeMetric.execute()");
        isCanceled = false;

        initializeAttributeColumns(graph.getModel());

        graph.readLock();
        try {
            TOTAL_EDGES = graph.getEdgeCount();
            
            HashMap<Edge, Integer> indicies = createIndiciesMap(graph);
            
            initializeStartValues();

            Map<String, double[]> metrics = calculateEdgeMetrics(graph, indicies, isDirected, isNormalized);
            
            edgeLength = metrics.get(EDGE_LENGTH);

            saveCalculatedValues(graph, indicies, edgeLength);
        } finally {
            graph.readUnlockAll();
        }
    }

    public Map<String, double[]> calculateEdgeMetrics(Graph graph, HashMap<Edge, Integer> indicies, boolean directed, boolean normalized) {
        LOG.log(Level.INFO, "EdgeMetric.calculateEdgeMetrics()");
        
        HashMap<String, double[]> metrics = new HashMap<>();

        double[] edgeLength = new double[edgeCount];
        
        metrics.put(EDGE_LENGTH, edgeLength);
        
        Progress.start(progress, edgeCount);
        int count = 0;
        
        for (Edge edge : graph.getEdges()) {
            int i = indicies.get(edge);
            if (i < TOTAL_EDGES) {
                edgeLength[i] = calculateEdgeLength(graph, edge);
                avgEdgeLength += edgeLength[i];
                count++;
            }
            
            if (isCanceled) {
                return metrics;
            }
            Progress.progress(progress, count);
        }
        
        avgEdgeLength /= edgeCount;
        
        return metrics;
    }

    protected double calculateEdgeLength(Graph graph, Edge edge) {
        Node u = edge.getSource();
        Node v = edge.getTarget();
        
        return Math.abs((Double) v.getAttribute(origin_x) - (Double) u.getAttribute(origin_x))
                + Math.abs((Double) v.getAttribute(origin_y) - (Double) u.getAttribute(origin_y));
    }
    
    public HashMap<Edge, Integer> createIndiciesMap(Graph graph) {
        LOG.log(Level.INFO, "EdgeMetric.createIndicesMap()");
        HashMap<Edge, Integer> indicies = new HashMap<>();
        int index = 0;
        for (Edge e : graph.getEdges()) {
            if (e.getSource().getAttribute(cell).equals("port")
                    || e.getTarget().getAttribute(cell).equals("port")) {
                indicies.put(e, TOTAL_EDGES);
            }
            else {
                indicies.put(e, index);
                index++;
            }
        }  
        edgeCount = index;
        return indicies;
    }
    
    public void initializeStartValues() {
        LOG.log(Level.INFO, "EdgeMetric.initializeStartValues()");
        edgeLength = new double[edgeCount];
        avgEdgeLength = 0;
    }
    
    private void saveCalculatedValues(Graph graph, HashMap<Edge, Integer> indicies, double[] edgeLength) {
        LOG.log(Level.INFO, "EdgeMetric.saveCalculatedValues()");
        for (Edge s : graph.getEdges()) {
            int s_index = indicies.get(s);
            
            if (s_index < TOTAL_EDGES) {
                s.setAttribute(EDGE_LENGTH, edgeLength[s_index]);
            }
        }
    }
     

    private void initializeAttributeColumns(GraphModel graphModel) {
        LOG.log(Level.INFO, "EdgeMetric.initializeAttributeColumns()");
        Table edgeTable = graphModel.getEdgeTable();
        if (!edgeTable.hasColumn(EDGE_LENGTH)) {
            edgeTable.addColumn(EDGE_LENGTH, NbBundle.getMessage(EdgeMetrics.class, "EdgeMetrics.edgecolumn.Length"), Double.class, new Double(0));
        }
    }

    /**
     *
     * @return
     */
    
    private String createImageFile(TempDirUtils.TempDir tempDir, double[] pVals, String pName, String pX, String pY) {
        LOG.log(Level.INFO, "EdgeMetric.createImageFile()");
        //distribution of values
        Map<Double, Integer> dist = new HashMap<>();
        
        Double max = 0.0;
        for (int i = 0; i < edgeCount; i++) {
            if (pVals[i] > max) {
                max = pVals[i];
            }
        }
        
        max = 200.0;
        
        int numBins = 100;
        Double binSize = max/numBins;
        for (int i = 0; i < edgeCount; i++) {
            if (pVals[i] < max) {
                int bin = (int) (pVals[i] / binSize);
                Double d = binSize*bin + (binSize/2);
                if (dist.containsKey(d)) {
                    Integer v = dist.get(d);
                    dist.put(d, v + 1);
                } else {
                    dist.put(d, 1);
                }
            }
        }
        
        //Distribution series
        XYSeries dSeries = ChartUtils.createXYSeries(dist, pName);

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(dSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                pName,
                pX,
                pY,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                false,
                false);
        chart.removeLegend();
        ChartUtils.decorateChart(chart);
        ChartUtils.scaleChart(chart, dSeries, false);
        return ChartUtils.renderChart(chart, pName + ".png");
    }
    
    @Override
    public String getReport() {
        LOG.log(Level.INFO, "EdgeMetric.getReport()");
        String htmlIMG = "";
        
        try {
            TempDirUtils.TempDir tempDir = TempDirUtils.createTempDir();
            htmlIMG = createImageFile(tempDir, edgeLength, "Distance Distribution", "Value", "Count");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        NumberFormat f = new DecimalFormat("#0.000");

        String report = "<HTML> <BODY> <h1>Distance Report </h1> "
                + "<hr>"
                + "<br> <h2> Results: </h2>"
                + "Average Distance Between Node Pairs: " + f.format(avgEdgeLength)
                + "<br /><br />" + htmlIMG
                + "</BODY></HTML>";

        return report;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean cancel() {
        this.isCanceled = true;
        return true;
    }

    /**
     *
     * @param progressTicket
     */
    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progress = progressTicket;
    }
}
