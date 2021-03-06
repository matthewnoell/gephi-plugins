package com.raytheon.statistics.plugin;

import java.awt.Font;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import org.gephi.graph.api.*;
import org.gephi.statistics.spi.Statistics;
import org.gephi.utils.TempDirUtils;
import org.gephi.utils.TempDirUtils.TempDir;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.project.api.WorkspaceInformation;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.VerticalAlignment;

/**
 * Ref: Ulrik Brandes, A Faster Algorithm for Betweenness Centrality, in Journal of Mathematical Sociology 25(2):163-177, (2001)
 *
 * @author Matthew Noell <mnoell@raytheon.com>
 */
public class LogicCentrality implements Statistics, LongTask {
    
    private static final Logger LOG = Logger.getLogger("com.raytheon.statistics.plugin");
    
    private String sourceName;
    private Column is_sequential = null;
    
    public static final String BETWEENNESS = "combinationalbetweenesscentrality";
    public static final String CLOSENESS = "combinationalclosnesscentrality";
    public static final String HARMONIC_CLOSENESS = "combinationalharmonicclosnesscentrality";
    public static final String ECCENTRICITY = "combinationaleccentricity";
    /**
     *
     */
    private double[] betweenness;
    /**
     *
     */
    private double[] closeness;
    private double[] harmonicCloseness;
    /**
     *
     */
    private double[] eccentricity;
    /**
     *
     */
    private int diameter;
    private int radius;
    private double maxBetweenness;
    /**
     *
     */
    private double avgDist;
    /**
     *
     */
    private int N;
    /**
     *
     */
    private boolean isDirected;
    /**
     *
     */
    private ProgressTicket progress;
    /**
     *
     */
    private boolean isCanceled;
    private boolean isNormalized;

    /**
     * Gets the average shortest path length in the network
     *
     * @return average shortest path length for all nodes
     */
    public double getPathLength() {
        return avgDist;
    }

    /**
     * @return the diameter of the network
     */
    public double getDiameter() {
        return diameter;
    }

    /**
     * @return the radius of the network
     */
    public double getRadius() {
        return radius;
    }

    /**
     * @return the maxBetweenness of the network
     */
    public double getMaxBetweenness() {
        return maxBetweenness;
    }
    /**
     * Construct a LogicCentrality calculator for the current graph model
     */
    public LogicCentrality() {
        GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
        if (graphController != null && graphController.getGraphModel() != null) {
            isDirected = graphController.getGraphModel().isDirected();
        }
    }

    /**
     *
     * @param graphModel
     */
    @Override
    public void execute(GraphModel graphModel) {
        sourceName = getSourceName();
        
        Table nodeTable = graphModel.getNodeTable();
        for (int i=0; i<nodeTable.countColumns(); i++) {
            if (nodeTable.getColumn(i).getTitle().equals("is_sequential")) {
                is_sequential = nodeTable.getColumn(i);
                break;
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
        isCanceled = false;

        initializeAttributeColumns(graph.getModel());

        graph.readLock();
        try {
            N = graph.getNodeCount();

            initializeStartValues();

            HashMap<Node, Integer> indicies = createIndiciesMap(graph);

            Map<String, double[]> metrics = calculateCentralityMetrics(graph, indicies, isDirected, isNormalized);

            eccentricity = metrics.get(ECCENTRICITY);
            closeness = metrics.get(CLOSENESS);
            harmonicCloseness = metrics.get(HARMONIC_CLOSENESS);
            betweenness = metrics.get(BETWEENNESS);

            Node s = getMostCentralNode(graph, indicies, betweenness);
            int s_index = indicies.get(s);
            maxBetweenness = betweenness[s_index];
            
            saveCalculatedValues(graph, indicies, eccentricity, betweenness, closeness, harmonicCloseness);
        } finally {
            graph.readUnlock();
        }

    }

    public Map<String, double[]> calculateCentralityMetrics(Graph graph, HashMap<Node, Integer> indicies, boolean directed, boolean normalized) {
        int n = graph.getNodeCount();

        HashMap<String, double[]> metrics = new HashMap<>();

        double[] nodeEccentricity = new double[n];
        double[] nodeBetweenness = new double[n];
        double[] nodeCloseness = new double[n];
        double[] nodeHarmonicCloseness = new double[n];

        metrics.put(ECCENTRICITY, nodeEccentricity);
        metrics.put(CLOSENESS, nodeCloseness);
        metrics.put(HARMONIC_CLOSENESS, nodeHarmonicCloseness);
        metrics.put(BETWEENNESS, nodeBetweenness);

        Progress.start(progress, graph.getNodeCount());
        int count = 0;

        int totalPaths = 0;
        for (Node s : graph.getNodes()) {
            Stack<Node> S = new Stack<>();

            LinkedList<Node>[] P = new LinkedList[n];  // list of predecessors on shortest path from source
            double[] theta = new double[n];            // number of shortest paths from source to v
            int[] d = new int[n];                      // distance from source

            int s_index = indicies.get(s);

            setInitParametetrsForNode(s, P, theta, d, s_index, n);

            LinkedList<Node> Q = new LinkedList<>();
            Q.addLast(s);
            // phase 1 - breadth first search
            while (!Q.isEmpty()) {
                Node v = Q.removeFirst();
                // push visited nodes on stack for phase 2
                S.push(v);
                // stop here if v is a sequential node
                if (v == s || (!v.getAttribute(is_sequential).equals(Boolean.TRUE))) {
                    int v_index = indicies.get(v);

                    EdgeIterable edgeIter = getEdgeIter(graph, v, directed);
                
                    for (Edge edge : edgeIter) {
                        Node reachable = graph.getOpposite(v, edge);

                        int r_index = indicies.get(reachable);
                        // path discovery - w found for the first time?
                        if (d[r_index] < 0) {
                            Q.addLast(reachable);
                            d[r_index] = d[v_index] + 1;
                        }
                        // path counting - edge (v,w) on a shortest path?
                        if (d[r_index] == (d[v_index] + 1)) {
                            theta[r_index] = theta[r_index] + theta[v_index];
                            P[r_index].addLast(v);
                        }
                    }
                }
            }
            double reachable = 0;
            for (int i = 0; i < n; i++) {
                if (d[i] > 0) {
                    avgDist += d[i];
                    nodeEccentricity[s_index] = (int) Math.max(nodeEccentricity[s_index], d[i]);
                    nodeCloseness[s_index] += d[i];
                    nodeHarmonicCloseness[s_index] += Double.isInfinite(d[i]) ? 0.0 : 1.0 / d[i];
                    diameter = Math.max(diameter, d[i]);
                    reachable++;
                }
            }

            radius = (int) Math.min(nodeEccentricity[s_index], radius);

            if (reachable != 0) {
                nodeCloseness[s_index] = (nodeCloseness[s_index] == 0) ? 0 : reachable / nodeCloseness[s_index];
                nodeHarmonicCloseness[s_index] = nodeHarmonicCloseness[s_index] / reachable;
            }

            totalPaths += reachable;

            double[] delta = new double[n];
            // phase 2 - visit nodes in reverse order of discovery, to accumulate dependencies
            // back-propagation of dependencies
            while (!S.empty()) {
                Node w = S.pop();
                int w_index = indicies.get(w);
                ListIterator<Node> iter1 = P[w_index].listIterator();
                while (iter1.hasNext()) {
                    Node u = iter1.next();
                    int u_index = indicies.get(u);
                    delta[u_index] += (theta[u_index] / theta[w_index]) * (1 + delta[w_index]);
                }
                if (w != s) {
                    nodeBetweenness[w_index] += delta[w_index];
                }
            }
            count++;
            if (isCanceled) {
                return metrics;
            }
            Progress.progress(progress, count);
        }

        avgDist /= totalPaths;//mN * (mN - 1.0f);

        calculateCorrection(graph, indicies, nodeBetweenness, directed, normalized);

        return metrics;
    }

    private void setInitParametetrsForNode(Node s, LinkedList<Node>[] P, double[] theta, int[] d, int index, int n) {
        for (int j = 0; j < n; j++) {
            P[j] = new LinkedList<>();
            theta[j] = 0;
            d[j] = -1;
        }
        theta[index] = 1;
        d[index] = 0;
    }

    private EdgeIterable getEdgeIter(Graph graph, Node v, boolean directed) {
        EdgeIterable edgeIter;
        if (directed) {
            edgeIter = ((DirectedGraph) graph).getOutEdges(v);
        } else {
            edgeIter = graph.getEdges(v);
        }
        return edgeIter;
    }

    private void initializeAttributeColumns(GraphModel graphModel) {
        Table nodeTable = graphModel.getNodeTable();
        if (!nodeTable.hasColumn(ECCENTRICITY)) {
            nodeTable.addColumn(ECCENTRICITY, "Combinational Eccentricity", Double.class, new Double(0));
        }
        if (!nodeTable.hasColumn(CLOSENESS)) {
            nodeTable.addColumn(CLOSENESS, "Combinational Closeness Centrality", Double.class, new Double(0));
        }
        if (!nodeTable.hasColumn(HARMONIC_CLOSENESS)) {
            nodeTable.addColumn(HARMONIC_CLOSENESS, "Combinational Harmonic Closeness Centrality", Double.class, new Double(0));
        }
        if (!nodeTable.hasColumn(BETWEENNESS)) {
            nodeTable.addColumn(BETWEENNESS, "Combinational Betweenness Centrality", Double.class, new Double(0));
        }
    }

    public HashMap<Node, Integer> createIndiciesMap(Graph graph) {
        HashMap<Node, Integer> indicies = new HashMap<>();
        int index = 0;
        for (Node s : graph.getNodes()) {
            indicies.put(s, index);
            index++;
        }
        return indicies;
    }

    public void initializeStartValues() {
        betweenness = new double[N];
        eccentricity = new double[N];
        closeness = new double[N];
        harmonicCloseness = new double[N];
        maxBetweenness = 0.0;
        diameter = 0;
        avgDist = 0;
        radius = Integer.MAX_VALUE;
    }

    private void calculateCorrection(Graph graph, HashMap<Node, Integer> indicies,
            double[] nodeBetweenness, boolean directed, boolean normalized) {

        int n = graph.getNodeCount();

        for (Node s : graph.getNodes()) {

            int s_index = indicies.get(s);

            if (!directed) {
                nodeBetweenness[s_index] /= 2;
            }
            if (normalized) {
                nodeBetweenness[s_index] /= directed ? (n - 1) * (n - 2) : (n - 1) * (n - 2) / 2;
            }
        }
    }
    
    private Node getMostCentralNode(Graph graph, HashMap<Node, Integer> indicies, double[] nodeBetweenness) {
        Node n = null;
        double maxBetweenness = 0.0;

        for (Node s : graph.getNodes()) {
            int s_index = indicies.get(s);

            if (nodeBetweenness[s_index] > maxBetweenness) {
                maxBetweenness = nodeBetweenness[s_index];
                n = s;
            }
        }
        
        return n;
    }

    private void saveCalculatedValues(Graph graph, HashMap<Node, Integer> indicies,
            double[] nodeEccentricity, double[] nodeBetweenness, double[] nodeCloseness, double[] nodeHarmonicCloseness) {
        for (Node s : graph.getNodes()) {
            int s_index = indicies.get(s);

            s.setAttribute(ECCENTRICITY, nodeEccentricity[s_index]);
            s.setAttribute(CLOSENESS, nodeCloseness[s_index]);
            s.setAttribute(HARMONIC_CLOSENESS, nodeHarmonicCloseness[s_index]);
            s.setAttribute(BETWEENNESS, nodeBetweenness[s_index]);
        }
    }

    public void setNormalized(boolean isNormalized) {
        this.isNormalized = isNormalized;
    }

    public boolean isNormalized() {
        return isNormalized;
    }

    public void setDirected(boolean isDirected) {
        this.isDirected = isDirected;
    }

    public boolean isDirected() {
        return isDirected;
    }

    private String createImageFile(TempDir tempDir, double[] pVals, String sName, String pName, String pX, String pY) {
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
        String date = new Date().toString();
        TextTitle subTitle = new TextTitle("Graph: " + sName + " (" + date + ")");
        subTitle.setFont(new Font("Dialog", Font.ITALIC, 12));
        subTitle.setPaint(java.awt.Color.BLACK);
        subTitle.setPosition(RectangleEdge.BOTTOM);
        subTitle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        subTitle.setVerticalAlignment(VerticalAlignment.BOTTOM);
        chart.addSubtitle(subTitle);
        ChartUtils.decorateChart(chart);
        ChartUtils.scaleChart(chart, dSeries, isNormalized);
        return ChartUtils.renderChart(chart, sName + "-" + pName.toLowerCase().replaceAll(" ", "-") + ".png");
    }

    /**
     *
     * @return
     */
    @Override
    public String getReport() {
        String htmlIMG1 = "";
        String htmlIMG2 = "";
        String htmlIMG3 = "";
        String htmlIMG4 = "";
        try {
            TempDir tempDir = TempDirUtils.createTempDir();
            htmlIMG1 = createImageFile(tempDir, betweenness, sourceName, "Combinational Betweenness Centrality Distribution", "Value", "Count");
            htmlIMG2 = createImageFile(tempDir, closeness, sourceName, "Combinational Closeness Centrality Distribution", "Value", "Count");
            htmlIMG3 = createImageFile(tempDir, harmonicCloseness, sourceName, "Combinational Harmonic Closeness Centrality Distribution", "Value", "Count");
            htmlIMG4 = createImageFile(tempDir, eccentricity, sourceName, "Combinational Eccentricity Distribution", "Value", "Count");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        String report = "<HTML> <BODY> <h1>Graph Centrality  Report </h1> "
                + "<hr>"
                + "<br>"
                + "<h2> Parameters: </h2>"
                + "Network Interpretation:  " + (isDirected ? "directed" : "undirected") + "<br />"
                + "<br /> <h2> Results: </h2>"
                + "Diameter: " + diameter + "<br />"
                + "Radius: " + radius + "<br />"
                + "Average Path length: " + avgDist + "<br />"
                + htmlIMG1 + "<br /><br />"
                + htmlIMG2 + "<br /><br />"
                + htmlIMG3 + "<br /><br />"
                + htmlIMG4
                + "<br /><br />" + "<h2> Algorithm: </h2>"
                + "Ulrik Brandes, <i>A Faster Algorithm for Betweenness Centrality</i>, in Journal of Mathematical Sociology 25(2):163-177, (2001)<br />"
                + "</BODY> </HTML>";

        return report;
    }
    
    private String getSourceName() {
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        Workspace workspace = pc.getCurrentWorkspace();
        WorkspaceInformation info = workspace.getLookup().lookup(WorkspaceInformation.class);
        return FilenameUtils.getBaseName(info.getSource());
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
