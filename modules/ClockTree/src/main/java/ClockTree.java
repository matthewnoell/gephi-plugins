package com.raytheon.plugins.clocktree;

import java.awt.FlowLayout;
import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.Table;
import org.gephi.utils.longtask.spi.LongTask;
import org.gephi.utils.progress.Progress;
import org.gephi.utils.progress.ProgressTicket;
import org.gephi.tools.spi.MouseClickEventListener;
import org.gephi.tools.spi.NodeClickEventListener;
import org.gephi.tools.spi.Tool;
import org.gephi.tools.spi.ToolEventListener;
import org.gephi.tools.spi.ToolSelectionType;
import org.gephi.tools.spi.ToolUI;
import org.gephi.visualization.VizController;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServiceProvider(service = Tool.class)
public class ClockTree implements Tool, LongTask {

    private boolean cancel = false;
    private ProgressTicket progressTicket;
    private GraphModel gm;
    
    //Architecture
    private ToolEventListener[] listeners;
    private ClockTreePanel clockTreePanel;
    //Settings
    private Color color;
    //State
    private Node sourceNode;
    
    private Column col;
    
    private List<Node> marked;          // marked[v] = has vertex v been marked?
    private Map<Node, Node> edgeTo;     // edgeTo[v] = previous vertex on path to v
    private List<Node> onStack;         // onStack[v] = is vertex on the stack?
    
    private List<Node> neigh;
    
    private Iterator<Node> edgeIterator;
    
    private static final Logger logger = Logger.getLogger("com.raytheon.plugins.clocktree");
    
    public ClockTree() {
        //Default settings
        color = Color.RED;
    }
    
    @Override
    public void select() {
    }

    @Override
    public void unselect() {
    }

    @Override
    public ToolEventListener[] getListeners() {
        listeners = new ToolEventListener[2];
        listeners[0] = new NodeClickEventListener() {
            @Override
            public void clickNodes(Node[] nodes) {
                Node n = nodes[0];
                sourceNode = n;
                color = clockTreePanel.getColor();
                GraphController gc = Lookup.getDefault().lookup(GraphController.class);
                GraphModel gm = gc.getGraphModel();
                
                Graph graph = gm.getGraphVisible();
                Table nodeTable = gm.getNodeTable();
                
                col = null;
                for (int i=0; i<nodeTable.countColumns(); i++) {
                    if (nodeTable.getColumn(i).getTitle().equals("is_sequential")) {
                        col = nodeTable.getColumn(i);
                        break;
                    }
                }
                if (col == null) {
                    logger.log(Level.INFO, "Column 'is_sequential' not found!");
                } else {
                    logger.log(Level.INFO, "Column 'is_sequential' found!");
                }
                
                if (gm.isDirected()) {
                    sourceNode.setColor(color);
                    
                    marked = new ArrayList<Node>(graph.getNodeCount());
                    onStack = new ArrayList<Node>(graph.getNodeCount());
                    edgeTo = new HashMap<Node, Node>(graph.getNodeCount());
                    
                    graph.readLock();
                    
                    try {
                        // Init the progress tick to the number of nodes to be visited
                        Progress.start(progressTicket, graph.getNodeCount());
                        Progress.setDisplayName(progressTicket, "Visiting nodes...");
            
                        dfs(graph, nodeTable, sourceNode);
                        
                        graph.readUnlockAll();
                    
                        for (Node v : graph.getNodes()) {
                            v.setColor(Color.BLACK);
                        }
                        
                        for (Edge e : graph.getEdges()) {
                            e.setColor(Color.BLACK);
                        }
                    
                        for (int i = 0; i < marked.size(); i++) {
                            Node v = marked.get(i);
                            logger.log(Level.INFO, "Calling setColor() for Node {0}", v.getId().toString());
                            v.setColor(color);
                        }
                        
                        edgeIterator = edgeTo.keySet().iterator();
                        
                        while(edgeIterator.hasNext()) {
                            Node w = edgeIterator.next();
                            Node v = edgeTo.get(w);
                            Edge e = graph.getEdge(v, w);
                            if (e == null) {
                                logger.log(Level.INFO, "Edge not found!");
                            } else {
                                logger.log(Level.INFO, "Calling setColor() for edge from {0} to {1}", new Object[] {v.getId().toString(), w.getId().toString()});
                                e.setColor(color);
                            }
                        }
                    
                    
                        edgeIterator = edgeTo.keySet().iterator();
                        
                        int deletedEdgeCnt = 0;
                        int deletedNodeCnt = 0;
                        
                        while(edgeIterator.hasNext()) {
                            Node w = edgeIterator.next();
                            Node v = edgeTo.get(w);
                            Edge e = graph.getEdge(v, w);
                            if (e == null) {
                                logger.log(Level.INFO, "Edge not found!");
                            }
                            if(!graph.removeEdge(e)) {
                                logger.log(Level.INFO, "Unable to remove edge {0}", e);
                            } else {
                                logger.log(Level.INFO, "Deleting edge from {0} to {1}", new Object[] {e.getSource().getId().toString(), e.getTarget().getId().toString()});
                                deletedEdgeCnt++;
                            }
                        }
                        
                        for (int i = 0; i < marked.size(); i++) {
                            Node v = marked.get(i);
                            if(graph.getDegree(v) == 0) {
                                if(!graph.removeNode(v)) {
                                    logger.log(Level.INFO, "Unable to remove node {0}", v);
                                }
                                else {
                                    logger.log(Level.INFO, "Deleting node {0}", v.getId().toString());
                                    deletedNodeCnt++;
                                }
                            }
                        }
                        
                        logger.log(Level.INFO, "# Edges deleted: {0}", deletedEdgeCnt);
                        logger.log(Level.INFO, "# Nodes deleted: {0}", deletedNodeCnt);
                        
                        graph.readUnlockAll();
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Unlock graph
                        graph.readUnlockAll();
                    }
                    Progress.finish(progressTicket);                    
                } else {
                    sourceNode.setColor(Color.BLUE);
                }
            }
        };
        listeners[1] = new MouseClickEventListener() {
            @Override
            public void mouseClick(int[] positionViewport, float[] position3d) {
            }
        };
        return listeners;
    }

    @Override
    public ToolUI getUI() {
        return new ToolUI() {
            @Override
            public JPanel getPropertiesBar(Tool tool) {
                clockTreePanel = new ClockTreePanel();
                clockTreePanel.setColor(color);
                clockTreePanel.setStatus("Select a starting node");
                return clockTreePanel;            
            }
            
            @Override
            public String getName() {
                return "Highlight logic cone";
            }
            
            @Override
            public Icon getIcon() {
                return new ImageIcon(getClass().getResource("/magnifier.png"));
            }
            
            @Override
            public String getDescription() {
                return "Color all nodes and edges in logic cone.";
            }
            
            @Override
            public int getPosition() {
                return 140;
            }
        };
    }

    @Override
    public ToolSelectionType getSelectionType() {
        return ToolSelectionType.SELECTION;
    }
    
    @Override
    public boolean cancel() {
        cancel = true;
        return true;
    }
    
    @Override
    public void setProgressTicket(ProgressTicket progressTicket) {
        this.progressTicket = progressTicket;
    }
    
    private void dfs(Graph graph, Table table, Node node) {
        
        logger.log(Level.INFO, "Starting dfs()");
        logger.log(Level.INFO, "Node {0}", node.getId().toString());
              
        // A new node has been visited
        Progress.progress(progressTicket);
        marked.add(node);
        onStack.add(node);

        neigh = new ArrayList<Node>();
        
        // Break the recursion if cancel is pressed
        if (cancel) return;
        
        for (Edge e : graph.getEdges(node)) {
            logger.log(Level.INFO, "Found edge from {0} to {1}", new Object[] {e.getSource().getId().toString(), e.getTarget().getId().toString()});
            if (e.getSource().equals(node) && !e.getSource().equals(e.getTarget())) {
                logger.log(Level.INFO, "Adding node {0} to neigh list", e.getTarget().getId().toString());
                neigh.add(e.getTarget());
            }
        }

        for (Node w : neigh) {
            logger.log(Level.INFO, "neigh node {0}", w.getId().toString());
            logger.log(Level.INFO, "neigh node attribute {0}", w.getAttribute(col));
            if (w.getAttribute(col).equals(Boolean.TRUE)) {
                marked.add(w);
            }
            
            edgeTo.put(w, node);
        
            if (!marked.contains(w)) {
                dfs(graph, table, w);
            }
        }
        onStack.remove(node);
    }
}