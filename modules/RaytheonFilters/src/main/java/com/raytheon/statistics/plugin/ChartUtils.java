package com.raytheon.statistics.plugin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import org.gephi.utils.TempDirUtils;
import org.gephi.utils.TempDirUtils.TempDir;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.chart.encoders.KeypointPNGEncoderAdapter;
import java.io.FileOutputStream;

/**
 *
 * @author Matthew Noell <mnoell@raytheon.com>
 */
public abstract class ChartUtils {

    public static void decorateChart(JFreeChart chart) {
        XYPlot plot = (XYPlot) chart.getPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(0, 0, 2, 2));
//        plot.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setBackgroundPaint(new Color(0,0,0,0.0f));
        plot.setDomainGridlinePaint(java.awt.Color.GRAY);
        plot.setRangeGridlinePaint(java.awt.Color.GRAY);
        plot.setRenderer(renderer);
    }

    public static void scaleChart(JFreeChart chart, XYSeries dSeries, boolean normalized) {
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setLowerMargin(1.0);
        domainAxis.setUpperMargin(1.0);
        domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        if (normalized) {
            domainAxis.setRange(-0.05, 1.05);
        } else {
            domainAxis.setRange(dSeries.getMinX() - 1, dSeries.getMaxX() + 1);
        }
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(-0.1 * Math.sqrt(dSeries.getMaxY()), dSeries.getMaxY() + 0.1 * Math.sqrt(dSeries.getMaxY()));
    }

    public static String renderChart(JFreeChart chart, String fileName) {
        String imageFile = "";
        try {
            final ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
            TempDir tempDir = TempDirUtils.createTempDir();
            File file1 = tempDir.createFile(fileName);
            FileOutputStream fout = new FileOutputStream(file1);
            imageFile = "<IMG SRC=\"file:" + file1.getAbsolutePath() + "\" " + "WIDTH=\"600\" HEIGHT=\"400\" BORDER=\"0\" USEMAP=\"#chart\"></IMG>";
            BufferedImage bufferedImage = chart.createBufferedImage(600, 400, BufferedImage.TYPE_4BYTE_ABGR, null);
            KeypointPNGEncoderAdapter encoder = new KeypointPNGEncoderAdapter();
            encoder.setEncodingAlpha(true);
            encoder.encode(bufferedImage, fout);
            fout.close();
//            ChartUtilities.saveChartAsPNG(file1, chart, 600, 400, info);
        } catch (IOException e) {
        }
        return imageFile;
    }

    public static XYSeries createXYSeries(Map data, String name) {
        XYSeries series = new XYSeries(name);
        for (Iterator it = data.entrySet().iterator(); it.hasNext();) {
            Map.Entry d = (Map.Entry) it.next();
            Number x = (Number) d.getKey();
            Number y = (Number) d.getValue();
            series.add(x, y);
        }
        return series;
    }

}
