package com.raytheon.statistics.plugin;

import java.text.DecimalFormat;
import javax.swing.JPanel;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthew Noell <mnoell@raytheon.com>
 */
@ServiceProvider(service = StatisticsUI.class)
public class EdgeMetricsUI implements StatisticsUI {

    private static final Logger LOG = Logger.getLogger("com.raytheon.statistics.plugin");
    
    private EdgeMetrics edgeMetrics;

    @Override
    public JPanel getSettingsPanel() {
        return null;
    }

    @Override
    public void setup(Statistics statistics) {
        LOG.log(Level.INFO, "EdgeMetricsUI.setup()");
        this.edgeMetrics = (EdgeMetrics) statistics;
    }

    @Override
    public void unsetup() {
        edgeMetrics = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return EdgeMetrics.class;
    }

    @Override
    public String getValue() {
        DecimalFormat df = new DecimalFormat("###.###");
        return "" + df.format(edgeMetrics.getAverageEdgeLength());
    }

    @Override
    public String getDisplayName() {
        LOG.log(Level.INFO, "EdgeMetricsUI.getDisplayName()");
        return NbBundle.getMessage(getClass(), "EdgeMetricsUI.name");
    }

    @Override
    public String getCategory() {
//        return StatisticsUI.CATEGORY_NETWORK_OVERVIEW;
        return "Raytheon";
    }

    @Override
    public int getPosition() {
        return 1;
    }

    @Override
    public String getShortDescription() {
        return NbBundle.getMessage(getClass(), "EdgeMetricsUI.shortDescription");
    }
}
