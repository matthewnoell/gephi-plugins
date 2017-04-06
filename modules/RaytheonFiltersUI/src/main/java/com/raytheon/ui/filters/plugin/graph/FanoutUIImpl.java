package com.raytheon.ui.filters.plugin.graph;

import javax.swing.JPanel;
import com.raytheon.filters.plugin.graph.FanoutBuilder.FanoutFilter;
import com.raytheon.filters.plugin.graph.FanoutUI;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = FanoutUI.class)
public class FanoutUIImpl implements FanoutUI {

    @Override
    public JPanel getPanel(FanoutFilter fanoutFilter) {
        FanoutPanel fanoutPanel = new FanoutPanel();
        fanoutPanel.setup(fanoutFilter);
        return fanoutPanel;
    }
}
