package com.raytheon.filters.plugin.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JPanel;
import org.gephi.filters.api.FilterLibrary;
import org.gephi.filters.spi.Category;
import org.gephi.filters.spi.ComplexFilter;
import org.gephi.filters.spi.Filter;
import org.gephi.filters.spi.FilterBuilder;
import org.gephi.filters.spi.FilterProperty;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServiceProvider(service = FilterBuilder.class)
public class FanoutBuilder implements FilterBuilder {

    private static final Logger LOG = Logger.getLogger("com.raytheon.filters.plugin.graph");
    
    @Override
    public Category getCategory() {
        return FilterLibrary.TOPOLOGY;
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(FanoutBuilder.class, "FanoutBuilder.name");
    }
    
    @Override
    public Icon getIcon() {
        return null;
    }
    
    @Override
    public String getDescription() {
        return NbBundle.getMessage(FanoutBuilder.class, "FanoutBuilder.description");
    }
    
    @Override
    public Filter getFilter(Workspace workspace) {
        return new FanoutFilter();
    }
    
    @Override
    public JPanel getPanel(Filter filter) {
        LOG.log(Level.INFO, "FanoutBuilder.getPanel() called");
        FanoutUI ui = Lookup.getDefault().lookup(FanoutUI.class);
        if (ui != null) {
            return ui.getPanel((FanoutFilter) filter);
        }
        LOG.log(Level.INFO, "FanoutUI.getPanel() returned null!");
        return null;
    }
    
    @Override
    public void destroy(Filter filter) {
    }
    
    public static class FanoutFilter implements ComplexFilter {
    
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
}
