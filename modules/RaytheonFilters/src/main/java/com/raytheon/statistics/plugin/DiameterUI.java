package com.raytheon.statistics.plugin;

import java.text.DecimalFormat;
import javax.swing.JPanel;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsUI;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

@ServiceProvider(service = StatisticsUI.class)
public class DiameterUI implements StatisticsUI {
    
    private static final Logger LOG = Logger.getLogger("com.raytheon.statistics.plugin");

    private LogicCentralityPanel panel;
    private LogicCentrality logicDistance;

    @Override
    public JPanel getSettingsPanel() {
        panel = new LogicCentralityPanel();
        return panel;
    }

    @Override
    public void setup(Statistics statistics) {
        this.logicDistance = (LogicCentrality) statistics;
        if (panel != null) {
            panel.setDirected(logicDistance.isDirected());
            panel.doNormalize(logicDistance.isNormalized());
        }
    }

    @Override
    public void unsetup() {
        if (panel != null) {
            logicDistance.setDirected(panel.isDirected());
            logicDistance.setNormalized(panel.normalize());
        }
        panel = null;
        logicDistance = null;
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return LogicCentrality.class;
    }

    @Override
    public String getValue() {
        DecimalFormat df = new DecimalFormat("###.###");
        return "" + df.format(logicDistance.getDiameter());
    }

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(getClass(), "DiameterUI.name");
    }

    @Override
    public String getCategory() {
//        return StatisticsUI.CATEGORY_NETWORK_OVERVIEW;
        return "Raytheon";
    }

    @Override
    public int getPosition() {
        return 100;
    }

    @Override
    public String getShortDescription() {
        return NbBundle.getMessage(getClass(), "DiameterUI.shortDescription");
    }
}
