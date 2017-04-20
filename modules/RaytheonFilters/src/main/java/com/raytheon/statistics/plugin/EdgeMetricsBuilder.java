package com.raytheon.statistics.plugin;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Matthew Noell <mnoell@raytheon.com>
 */
@ServiceProvider(service = StatisticsBuilder.class)
public class EdgeMetricsBuilder implements StatisticsBuilder {

    @Override
    public String getName() {
        return NbBundle.getMessage(EdgeMetricsBuilder.class, "EdgeMetrics.name");
    }

    @Override
    public Statistics getStatistics() {
        return new EdgeMetrics();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return EdgeMetrics.class;
    }
}
