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

public class EdgeMetrics implements Statistics, LongTask {

    public static final String EDGE_LENGTH = "length";
    public static final String ORIGIN_X = "origin_x";
    public static final String ORIGIN_Y = "origin_y";
    
    private double[] edgeLength;
    
    private int N;
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
        return avgEdgeLength;
    }

    /**
     *
     * @param graphModel
     */
    @Override
    public void execute(GraphModel graphModel) {
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
        isCanceled = false;

        initializeAttributeColumns(graph.getModel());

        graph.readLock();
        try {
            N = graph.getEdgeCount();
            
            HashMap<Edge, Integer> indicies = createIndiciesMap(graph);

            Map<String, double[]> metrics = calculateEdgeMetrics(graph, indicies, isDirected, isNormalized);
            
            edgeLength = metrics.get(EDGE_LENGTH);

            saveCalculatedValues(graph, indicies, edgeLength);
        } finally {
            graph.readUnlockAll();
        }
    }

    public Map<String, double[]> calculateEdgeMetrics(Graph graph, HashMap<Edge, Integer> indicies, boolean directed, boolean normalized) {
        int e = graph.getEdgeCount();
        
        HashMap<String, double[]> metrics = new HashMap<>();

        double[] edgeLength = new double[e];
        
        metrics.put(EDGE_LENGTH, edgeLength);
        
        Progress.start(progress, graph.getEdgeCount());
        int count = 0;
        
        for (Edge edge : graph.getEdges()) {
            int i = indicies.get(edge);    
            edgeLength[i] = calculateEdgeLength(graph, edge);
            avgEdgeLength += edgeLength[i];
            
            count++;
            if (isCanceled) {
                return metrics;
            }
            Progress.progress(progress, count);
        }
        
        avgEdgeLength /= graph.getEdgeCount();
        
        return metrics;
    }

    protected double calculateEdgeLength(Graph graph, Edge edge) {
        Node u = edge.getSource();
        Node v = edge.getTarget();
        
        return Math.abs((Double) v.getAttribute(ORIGIN_X) - (Double) u.getAttribute(ORIGIN_X))
                + Math.abs((Double) v.getAttribute(ORIGIN_Y) - (Double) u.getAttribute(ORIGIN_Y));
    }
    
    public HashMap<Edge, Integer> createIndiciesMap(Graph graph) {
        HashMap<Edge, Integer> indicies = new HashMap<>();
        int index = 0;
        for (Edge e : graph.getEdges()) {
            indicies.put(e, index);
            index++;
        }
        return indicies;
    }
    
    public void initializeStartValues() {
        edgeLength = new double[N];
        avgEdgeLength = 0;
    }
    
    private void saveCalculatedValues(Graph graph, HashMap<Edge, Integer> indicies, double[] edgeLength) {
        for (Edge s : graph.getEdges()) {
            int s_index = indicies.get(s);

            s.setAttribute(EDGE_LENGTH, edgeLength[s_index]);
        }
    }
     

    private void initializeAttributeColumns(GraphModel graphModel) {
        Table edgeTable = graphModel.getEdgeTable();
        if (!edgeTable.hasColumn(EDGE_LENGTH)) {
            edgeTable.addColumn(EDGE_LENGTH, NbBundle.getMessage(EdgeMetrics.class, "Distance.edgecolumn.Length"), Double.class, new Double(0));
        }
    }

    /**
     *
     * @return
     */
    
    private String createImageFile(TempDirUtils.TempDir tempDir, double[] pVals, String pName, String pX, String pY) {         
        //distribution of values
        Map<Double, Integer> dist = new HashMap<>();
        for (int i = 0; i < N; i++) {
            Double d = pVals[i];
            if (dist.containsKey(d)) {
                Integer v = dist.get(d);
                dist.put(d, v + 1);
            } else {
                dist.put(d, 1);
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
