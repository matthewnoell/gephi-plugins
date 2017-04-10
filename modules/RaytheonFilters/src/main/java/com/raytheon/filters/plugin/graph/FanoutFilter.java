/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.raytheon.filters.plugin.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.gephi.filters.spi.FilterProperty;
import org.gephi.filters.spi.ComplexFilter;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Table;
import org.gephi.graph.api.Column;
import org.openide.util.NbBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthew Noell <mnoell@raytheon.com>
 */
public class FanoutFilter implements ComplexFilter {
    
    private static final Logger LOG = Logger.getLogger("com.raytheon.filters.plugin.graph");
  
    private String pattern = "";
    private boolean self = true;
    private int depth = 1;
        
    @Override
    public Graph filter(Graph graph) {
        GraphModel gm = graph.getModel();
        Table nodeTable = gm.getNodeTable();
        Column is_sequential = null;
        
        for (int i=0; i<nodeTable.countColumns(); i++) {
            if (nodeTable.getColumn(i).getTitle().equals("is_sequential")) {
                is_sequential = nodeTable.getColumn(i);
                break;
            }
        }
        if (is_sequential == null) {
            LOG.log(Level.INFO, "Column 'is_sequential' not found!");
        } else {
            LOG.log(Level.INFO, "Column 'is_sequential' found!");
        }
        
        String str = pattern.toLowerCase();
            
        List<Node> nodes = new ArrayList<>();
        for (Node n : graph.getNodes()) {
            if (n.getId().toString().toLowerCase().equals(str)) {
                nodes.add(n);
            } else if ((n.getLabel() != null) && n.getLabel().toLowerCase().equals(str)) {
                nodes.add(n);
            }
        }
            
        Set<Node> result = new HashSet<>();
            
        Set<Node> neighbours = new HashSet<>();
        List<Node> sequential = new ArrayList<>();
        neighbours.addAll(nodes);
        
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < Integer.MAX_VALUE; j++) {
                // Add Sequential Nodes from previous loop to be filtered
                result.addAll(sequential);
                sequential.clear();
                
                Node[] nei = neighbours.toArray(new Node[0]);
                neighbours.clear();
                for (Node n : nei) {
                    LOG.log(Level.INFO, "Start node: {0}", n.getId().toString());
                    for (Edge e : graph.getEdges(n)) {
                        if (e.getSource().equals(n) && !e.getSource().equals(e.getTarget())) {
                            LOG.log(Level.INFO, "Adding node {0} to neigh list", e.getTarget().getId().toString());
                            Node neighbor = e.getTarget();
                            if (!result.contains(neighbor)) {
                                if (neighbor.getAttribute(is_sequential).equals(Boolean.TRUE)) {
                                    sequential.add(neighbor);
                                } else {
                                    neighbours.add(neighbor);
                                    result.add(neighbor);
                                }
                            }
                        }
                    }
                }
                if (neighbours.isEmpty()) {
                    break;
                }
            }
            neighbours.addAll(sequential);
            if (neighbours.isEmpty()) {
                break;
            }
        }
            
        if (self) {
            result.addAll(nodes);
        } else {
            result.removeAll(nodes);
        }
            
        for (Node node : graph.getNodes().toArray()) {
            if (result.contains(node)) {
                graph.removeNode(node);
            }
        }
        
        LOG.log(Level.INFO, "Nodes filtered: {0}", result.size());
        
        return graph;
    }
        
    @Override
    public String getName() {
        return NbBundle.getMessage(FanoutBuilder.class, "FanoutBuilder.name");
    }
        
    @Override
    public FilterProperty[] getProperties() {
        LOG.log(Level.INFO, "FanoutFilter.getProperties() called");
        try {
            return new FilterProperty[]{
                FilterProperty.createProperty(this, String.class, "pattern"),
                FilterProperty.createProperty(this, Integer.class, "depth"),
                FilterProperty.createProperty(this, Boolean.class, "self")};
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
        return new FilterProperty[0];
    }
        
    public String getPattern() {
        LOG.log(Level.INFO, "FanoutFilter.getPattern() called");
        return pattern;
    }
        
    public void setPattern(String pattern) {
        LOG.log(Level.INFO, "FanoutFilter.setPattern() called");
        this.pattern = pattern;
    }
        
    public Integer getDepth() {
        LOG.log(Level.INFO, "FanoutFilter.getDepth() called");
        return depth;
    }
        
    public void setDepth(Integer depth) {
        LOG.log(Level.INFO, "FanoutFilter.setDepth() called");
        this.depth = depth;
    }
        
    public boolean isSelf() {
        LOG.log(Level.INFO, "FanoutFilter.isSelf() called");
        return self;
    }
        
    public void setSelf(boolean self) {
        LOG.log(Level.INFO, "FanoutFilter.setSelf() called");
        this.self = self;
    }
}
