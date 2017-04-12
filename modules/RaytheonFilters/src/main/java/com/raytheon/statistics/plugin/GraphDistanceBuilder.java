package com.raytheon.statistics.plugin;

import org.gephi.statistics.plugin.GraphDistance;
import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Matthew Noell <mnoell@raytheon.com>
 */
@ServiceProvider(service=StatisticsBuilder.class)
public class GraphDistanceBuilder implements StatisticsBuilder {

    @Override
    public String getName() {
        return NbBundle.getMessage(GraphDistanceBuilder.class, "GraphDistance.name");
    }

    @Override
    public Statistics getStatistics() {
        return new GraphDistance();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return GraphDistance.class;
    }
}
