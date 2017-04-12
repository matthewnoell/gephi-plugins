package com.raytheon.statistics.plugin;

import java.text.DecimalFormat;
import javax.swing.JPanel;
import org.gephi.filters.spi.Category;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = StatisticsUI.class)
public class SubgraphDiameterUI implements StatisticsUI {

    private GraphDistancePanel panel;
    private GraphDistance graphDistance;

    @Override
    public JPanel getSettingsPanel() {
        panel = new GraphDistancePanel();
        return panel;
    }

    @Override
    public void setup(Statistics statistics) {
        this.graphDistance = (GraphDistance) statistics;
        if (panel != null) {
            panel.setDirected(graphDistance.isDirected());
            panel.doNormalize(graphDistance.isNormalized());
        }
    }

    @Override
    public void unsetup() {
        if (panel != null) {
            graphDistance.setDirected(panel.isDirected());
            graphDistance.setNormalized(panel.normalize());
        }
        panel = null;
        graphDistance = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return GraphDistance.class;
    }

    @Override
    public String getValue() {
        DecimalFormat df = new DecimalFormat("###.###");
        return "" + df.format(graphDistance.getDiameter());
    }

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(getClass(), "SubgraphDiameterUI.name");
    }

    @Override
    public String getCategory() {
        return StatisticsUI.CATEGORY_NETWORK_OVERVIEW;
    }

    @Override
    public int getPosition() {
        return 100;
    }

    @Override
    public String getShortDescription() {
        return NbBundle.getMessage(getClass(), "SubgraphDiameterUI.shortDescription");
    }
}
