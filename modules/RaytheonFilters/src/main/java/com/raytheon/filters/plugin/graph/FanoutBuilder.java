package com.raytheon.filters.plugin.graph;

import javax.swing.Icon;
import javax.swing.JPanel;
import org.gephi.filters.api.FilterLibrary;
import org.gephi.filters.spi.Category;
import org.gephi.filters.spi.Filter;
import org.gephi.filters.spi.FilterBuilder;
import org.gephi.project.api.Workspace;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServiceProvider(service = FilterBuilder.class)
public class FanoutBuilder implements FilterBuilder {

    private static final Logger LOG = Logger.getLogger("com.raytheon.filters.plugin.graph");
    
    private FanoutFilter fanoutFilter;
    
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
        FanoutPanel fanoutPanel = new FanoutPanel();
        fanoutPanel.setup((FanoutFilter) filter);
        return fanoutPanel;
    }
    
    @Override
    public void destroy(Filter filter) {
    }
}
