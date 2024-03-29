package org.aptivate.bmotools.pmgraph;

import java.awt.Color;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

/**
 * The GraphFactory class provides static methods which return JFreeChart
 * objects (which can then be served as web content or saved as images)
 * representing network traffic logged by pmacct on the BMO Box.
 * 
 * @author Thomas Sharp
 * @version 0.1
 * 
 * History: Noe A. Rodrigez Glez. 12-03-2009 Use a standar method to asign
 * colors to series.
 * 
 * Changed the way in which the stackedThroughput graph is created in order to
 * make the code more readable.
 * 
 * Removed the use of Alpha chanel on graphs because it caused problems with the
 * colors of the series.
 * 
 * Added some coments.
 */
public class GraphFactory {

    private static Logger m_logger = Logger.getLogger(GraphFactory.class.getName());

    public static final int OTHER_PORT = -1;

    public static final String OTHER_IP = "255.255.255.255";

    private Color getColorFromByteArray(byte[] bytes) {
        MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("SHA1");
            algorithm.reset();
            algorithm.update(bytes);
            byte sha1[] = algorithm.digest();
            return (new Color(sha1[0] & 0xFF, sha1[1] & 0xFF, sha1[2] & 0xFF));
        } catch (NoSuchAlgorithmException e) {
            m_logger.error(e.getMessage(), e);
        }
        return (Color.BLACK);
    }

    /**
	 * Initialize a series for a port graph with all the values to zero and add
	 * it to the datase and to the hashmap containing all the series.
	 * 
	 * @param name
	 * @param id
	 * @param dataset
	 * @param port_XYSeries
	 * @param renderer
	 * @param minutes
	 * @param start
	 * @return A Initialize a series for a port graph with all the values to zero and add
	 * it to the datase and to the hashmap containing all the series.
	 */
    private XYSeries InizializeSeries(String name, String id, DefaultTableXYDataset dataset, HashMap<String, XYSeries> port_XYSeries, XYItemRenderer renderer, long minutes, long start, boolean portGraph) {
        XYSeries series = new XYSeries(name, true, false);
        for (int i = 0; i <= minutes; i++) {
            series.add(Long.valueOf(start + i * 60000), Long.valueOf(0));
        }
        port_XYSeries.put(name, series);
        Color color;
        if (portGraph) color = getSeriesColor(Integer.valueOf(id)); else color = getSeriesColor(id);
        dataset.addSeries(series);
        renderer.setSeriesPaint(dataset.getSeriesCount() - 1, color);
        return series;
    }

    /**
	 * Create a JFreeChart with the data in the List thrptResults creating a new
	 * series per each port, or Ip
	 * 
	 * @param start
	 * @param end
	 * @param theStart
	 * @param theEnd
	 * @param thrptResults
	 * @param limitResult
	 * @param title
	 * @param portGraph
	 *            The series are Ip's or Ports
	 * @return A JFreeChart with the data in the List thrptResults creating a new
	 * series per each port, or Ip
	 */
    private JFreeChart fillGraph(long start, long end, long theStart, long theEnd, List<GraphData> thrptResults, Integer limitResult, String title, boolean portGraph) {
        HashMap<String, XYSeries> graph_XYSeries = new HashMap<String, XYSeries>();
        HashMap<Long, Long> otherUp = null;
        HashMap<Long, Long> otherDown = null;
        int minutes = (int) (end - start) / 60000;
        DefaultTableXYDataset dataset = new DefaultTableXYDataset();
        JFreeChart chart = createStackedXYGraph(title, dataset, start, end, theStart, theEnd);
        XYPlot plot = chart.getXYPlot();
        XYItemRenderer renderer = plot.getRenderer();
        int j = 0;
        for (GraphData thrptResult : thrptResults) {
            String id;
            Timestamp inserted = thrptResult.getTime();
            if (portGraph) id = thrptResult.getPort().toString(); else id = thrptResult.getLocalIp().trim();
            long downloaded = ((thrptResult.getDownloaded() * 8) / 1024) / 60;
            long uploaded = ((thrptResult.getUploaded() * 8) / 1024) / 60;
            if (!graph_XYSeries.containsKey(id + "<down>")) {
                if (j < limitResult) {
                    InizializeSeries(id + "<down>", id, dataset, graph_XYSeries, renderer, minutes, start, portGraph);
                    InizializeSeries(id + "<up>", id, dataset, graph_XYSeries, renderer, minutes, start, portGraph);
                } else {
                    if (otherUp == null) {
                        otherUp = new HashMap<Long, Long>();
                        otherDown = new HashMap<Long, Long>();
                        for (int i = 0; i <= minutes; i++) {
                            otherUp.put(Long.valueOf(start + i * 60000), 0L);
                            otherDown.put(Long.valueOf(start + i * 60000), 0L);
                        }
                    }
                }
                j++;
            }
            XYSeries downSeries = graph_XYSeries.get(id + "<down>");
            XYSeries upSeries = graph_XYSeries.get(id + "<up>");
            if (downSeries != null) {
                downSeries.update(inserted.getTime(), downloaded);
                upSeries.update(inserted.getTime(), (0 - uploaded));
            } else {
                otherDown.put(inserted.getTime(), otherDown.get(inserted.getTime()) + downloaded);
                otherUp.put(inserted.getTime(), otherUp.get(inserted.getTime()) + (0 - uploaded));
            }
        }
        if (otherUp != null) {
            XYSeries downSeries = new XYSeries("other <down>", true, false);
            XYSeries upSeries = new XYSeries("other <up>", true, false);
            for (int i = 0; i <= minutes; i++) {
                Long time = Long.valueOf(start + i * 60000);
                downSeries.add(time, otherDown.get(time));
                upSeries.add(time, otherUp.get(time));
            }
            Color color;
            if (portGraph) color = getSeriesColor(OTHER_PORT); else color = getSeriesColor(OTHER_IP);
            dataset.addSeries(downSeries);
            renderer.setSeriesPaint(dataset.getSeriesCount() - 1, color);
            dataset.addSeries(upSeries);
            renderer.setSeriesPaint(dataset.getSeriesCount() - 1, color);
        }
        return chart;
    }

    /**
	 * A method which creates tha chart with the defaults options for the
	 * stacked charts
	 * 
	 * @param dataset
	 * @param start
	 * @param end
	 * @param theStart
	 * @param theEnd
	 * @return A chart with the default options, and without data.
	 */
    private JFreeChart createStackedXYGraph(String title, DefaultTableXYDataset dataset, long start, long end, long theStart, long theEnd) {
        JFreeChart chart = ChartFactory.createStackedXYAreaChart(title, "Category", "Value", dataset, PlotOrientation.VERTICAL, true, true, false);
        XYPlot plot = chart.getXYPlot();
        DateAxis xAxis;
        long timePeriod = (theEnd - theStart) / 60000;
        if (timePeriod < 7) {
            xAxis = new DateAxis("Time (hours:minutes:seconds)");
        } else if ((timePeriod >= 7) && (timePeriod < 3650)) {
            xAxis = new DateAxis("Time (hours:minutes)");
        } else if ((timePeriod >= 3650) && (timePeriod < 7299)) {
            xAxis = new DateAxis("Time (day-month,hours:minutes)");
        } else {
            xAxis = new DateAxis("Time (day-month)");
        }
        xAxis.setMinimumDate(new Date(start - 1));
        xAxis.setMaximumDate(new Date(end));
        NumberAxis yAxis = new NumberAxis("Throughput (kb/s)");
        plot.setRangeAxis(yAxis);
        plot.setDomainAxis(xAxis);
        plot.addRangeMarker(new ValueMarker(0));
        chart.addSubtitle(new TextTitle(new Date(end).toString()));
        chart.setBackgroundPaint(null);
        chart.removeLegend();
        return chart;
    }

    /**
	 * Return a color obtained from create a hash with the bytes of the Ip.
	 * 
	 * @param ip
	 * @return Color for the selected IP.
	 */
    public Color getSeriesColor(String ip) {
        if (ip != null) {
            byte[] ipBytes = ip.getBytes();
            return getColorFromByteArray(ipBytes);
        }
        m_logger.warn("Unable to assign a color to a null IP. (Black color assigned)");
        return (Color.BLACK);
    }

    /**
	 * Return a color obtained from create a hash with the bytes of the Port.
	 * 
	 * @param port
	 * @return Color for the selected port
	 */
    public Color getSeriesColor(int port) {
        byte[] portBytes = new byte[] { (byte) (port >>> 24), (byte) (port >>> 16), (byte) (port >>> 8), (byte) port };
        return getColorFromByteArray(portBytes);
    }

    /**
	 * Produces a JFreeChart showing total upload and download throughput for
	 * the time period between start and end.
	 * 
	 * @param start
	 *            Time in seconds since epoch, in which the chart will start
	 * @param end
	 *            Time in seconds since epoch, in which the chart will end
	 * @return a new JFreeChart
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws SQLException
	 * 
	 * 
	 */
    public JFreeChart totalThroughput(long start, long end) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, SQLException {
        DataAccess dataAccess = new DataAccess();
        List<GraphData> results = dataAccess.getTotalThroughput(start, end);
        start = start - (start % 60000);
        end = end - (end % 60000);
        XYSeries downSeries = new XYSeries("Downloaded", true, false);
        XYSeries upSeries = new XYSeries("Uploaded", true, false);
        int minutes = (int) (end - start) / 60000;
        for (int i = 0; i <= minutes; i++) {
            downSeries.add(start + i * 60000, 0);
            upSeries.add(start + i * 60000, 0);
        }
        for (GraphData dbData : results) {
            Date inserted = dbData.getTime();
            long downloaded = ((dbData.getDownloaded() * 8) / 1024) / 60;
            long uploaded = ((dbData.getUploaded() * 8) / 1024) / 60;
            downSeries.update(inserted.getTime(), downloaded);
            upSeries.update(inserted.getTime(), 0 - uploaded);
        }
        DefaultTableXYDataset dataset = new DefaultTableXYDataset();
        dataset.addSeries(downSeries);
        dataset.addSeries(upSeries);
        DateAxis xAxis = new DateAxis();
        xAxis.setLowerMargin(0);
        xAxis.setUpperMargin(0);
        NumberAxis yAxis = new NumberAxis("Throughput (kb/s)");
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.addRangeMarker(new ValueMarker(0));
        plot.setRenderer(new XYAreaRenderer(XYAreaRenderer.AREA));
        plot.getRenderer().setSeriesPaint(0, Color.blue);
        plot.getRenderer().setSeriesPaint(1, Color.blue);
        JFreeChart chart = new JFreeChart("Total Network Throughput", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.addSubtitle(new TextTitle(new Date(end).toString()));
        chart.setBackgroundPaint(null);
        return chart;
    }

    /**
	 * Produces a JFreeChart showing total upload and download throughput for
	 * each IP as a cumulative stacked graph for the time period between start
	 * and end.
	 * 
	 * @param start
	 *            Time in seconds since epoch, in which the chart will start
	 * @param end
	 *            Time in seconds since epoch, in which the chart will end
	 * @return JFreeChart Object containing the info of the stackedThroughput
	 * 
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws SQLException
	 */
    public JFreeChart stackedThroughput(long start, long end, Integer limitResult) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, SQLException, NoSuchAlgorithmException {
        long theStart = start;
        long theEnd = end;
        start = start - (start % 60000);
        end = end - (end % 60000);
        DataAccess dataAccess = new DataAccess();
        List<GraphData> thrptResults = dataAccess.getThroughputPIPPMinute(theStart, theEnd);
        m_logger.debug("Start creating chart.");
        long initTime = System.currentTimeMillis();
        Collections.sort(thrptResults, new BytesTotalComparator(true));
        JFreeChart chart = fillGraph(start, end, theStart, theEnd, thrptResults, limitResult, "Network Throughput Per IP", false);
        if (m_logger.isDebugEnabled()) {
            long endTime = System.currentTimeMillis() - initTime;
            m_logger.debug("Execution Time creating chart : " + endTime + " miliseg");
        }
        thrptResults = null;
        return chart;
    }

    /**
	 * Create a chart with information of the throughput per a specific IP in
	 * each minute desglosed by port.
	 * 
	 * @param start
	 * @param end
	 * @param limitResult
	 * @param ip
	 * @return A JFreeChart with information of the trafic for the Ip
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 */
    public JFreeChart stackedThroughputOneIp(long start, long end, Integer limitResult, String ip) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, SQLException, NoSuchAlgorithmException {
        long theStart = start;
        long theEnd = end;
        start = start - (start % 60000);
        end = end - (end % 60000);
        DataAccess dataAccess = new DataAccess();
        List<GraphData> thrptResults = dataAccess.getThroughputPIPPMinuteOneIpPerPort(theStart, theEnd, ip);
        Collections.sort(thrptResults, new BytesTotalComparator(true));
        JFreeChart chart = fillGraph(start, end, theStart, theEnd, thrptResults, limitResult, "Network Throughput Per IP: " + ip, true);
        thrptResults = null;
        return chart;
    }

    /**
	 * Create a chart with information of the throughput in each minute
	 * desglosed by port.
	 * 
	 * @param start
	 * @param end
	 * @param limitResult
	 * @return JFrechart contaning  stackedThroughputPerPort 
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 */
    public JFreeChart stackedThroughputPerPort(long start, long end, Integer limitResult) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, SQLException, NoSuchAlgorithmException {
        long theStart = start;
        long theEnd = end;
        start = start - (start % 60000);
        end = end - (end % 60000);
        DataAccess dataAccess = new DataAccess();
        List<GraphData> thrptResults = dataAccess.getThroughputPerPortPerMinute(theStart, theEnd);
        Collections.sort(thrptResults, new BytesTotalComparator(true));
        JFreeChart chart = fillGraph(start, end, theStart, theEnd, thrptResults, limitResult, "Network Throughput Per Port", true);
        thrptResults = null;
        return chart;
    }

    /**
	 * Create a chart with information of the throughput in each minute in a
	 * specific port, desglosed by IP.
	 * 
	 * @param start
	 * @param end
	 * @param limitResult
	 * @param port
	 * @return A StackedAreaChart chart containig the data of an 
	 * specific port.
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws IOException
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 */
    public JFreeChart stackedThroughputOnePort(long start, long end, Integer limitResult, Integer port) throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException, SQLException, NoSuchAlgorithmException {
        long theStart = start;
        long theEnd = end;
        start = start - (start % 60000);
        end = end - (end % 60000);
        DataAccess dataAccess = new DataAccess();
        List<GraphData> thrptResults = dataAccess.getThroughputPIPPMinuteOnePortPerIp(theStart, theEnd, port);
        Collections.sort(thrptResults, new BytesTotalComparator(true));
        JFreeChart chart = fillGraph(start, end, theStart, theEnd, thrptResults, limitResult, "Network Throughput Per Port: " + port, false);
        thrptResults = null;
        return chart;
    }
}
