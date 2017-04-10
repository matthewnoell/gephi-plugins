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
import org.gephi.graph.api.Node;
import org.openide.util.NbBundle;

/**
 *
 * @author Matthew Noell <mnoell@raytheon.com>
 */
public class FanoutFilter implements ComplexFilter {
  
    private String pattern = "";
    private boolean self = true;
    private int depth = 1;
        
    @Override
    public Graph filter(Graph graph) {
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
        neighbours.addAll(nodes);
            
        for (int i = 0; i < depth; i++) {
            Node[] nei = neighbours.toArray(new Node[0]);
            neighbours.clear();
            for (Node n : nei) {
                for (Node neighbor : graph.getNeighbors(n)) {
                    if (!result.contains(neighbor)) {
                        neighbours.add(neighbor);
                        result.add(neighbor);
                    }
                }
            }
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
            
        return graph;
    }
        
    @Override
    public String getName() {
        return NbBundle.getMessage(FanoutBuilder.class, "FanoutBuilder.name");
    }
        
    @Override
    public FilterProperty[] getProperties() {
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
        return pattern;
    }
        
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
        
    public Integer getDepth() {
        return depth;
    }
        
    public void setDepth(Integer depth) {
        this.depth = depth;
    }
        
    public boolean isSelf() {
        return self;
    }
        
    public void setSelf(boolean self) {
        this.self = self;
    }
}
