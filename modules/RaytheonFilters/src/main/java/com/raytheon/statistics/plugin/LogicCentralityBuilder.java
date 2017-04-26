package com.raytheon.statistics.plugin;

import org.gephi.statistics.spi.Statistics;
import org.gephi.statistics.spi.StatisticsBuilder;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Matthew Noell <mnoell@raytheon.com>
 */
@ServiceProvider(service=StatisticsBuilder.class)
public class LogicCentralityBuilder implements StatisticsBuilder {
    
    private static final Logger LOG = Logger.getLogger("com.raytheon.statistics.plugin");

    @Override
    public String getName() {
        return NbBundle.getMessage(LogicCentralityBuilder.class, "LogicCentrality.name");
    }

    @Override
    public Statistics getStatistics() {
        return new LogicCentrality();
    }

    @Override
    public Class<? extends Statistics> getStatisticsClass() {
        return LogicCentrality.class;
    }
}
