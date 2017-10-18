package org.saiko.ai.genetics.tsp;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import sun.misc.BASE64Encoder;
import com.lowagie.text.Anchor;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

/**
 * @author Dusan Saiko (dusan@saiko.cz)
 * Last change $Date: 2005/08/23 23:18:05 $
 * 
 * TSP Application PDF report using iText library
 */
class Report extends PdfPageEventHelper {

    /** String containing the CVS revision. **/
    public static final String CVS_REVISION = "$Revision: 1.4 $";

    /** 
    * computation time constant key
    * @see #saveReport(File, City[], BufferedImage, Map, Map)
    * @see #getResultInfo(TSP)
    */
    static final String PARAM_COMPUTATION_TIME = "Report created";

    /**
    * common number formatter
    */
    static DecimalFormat numberFormatter = new DecimalFormat("###,###,###,###,###.############");

    /**
    * Map of report parameters
    */
    Map<String, String> parameters;

    /**
    * Saves PDF report of TSP application into file
    * @param file - file to save the report to
    * @param cities - ordered result list of cities starting at correct start city 
    * @param image - result image (jpg, gif ...)
    * @param params - parameter map, see PARAM_ variables
    * @param systemProperties - map of system variables to display (memory, os ...) 
    * @throws Exception
    */
    protected void saveReport(File file, City[] cities, BufferedImage image, Map<String, String> params, Map<String, String> systemProperties) throws Exception {
        this.parameters = params;
        Document document = new Document(PageSize.A4, 40, 40, 170, 60);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        writer.setPageEvent(this);
        document.open();
        document.add(new Chunk("\n"));
        PdfPTable mapInfo = new PdfPTable(1);
        {
            mapInfo.getDefaultCell().setBorderWidth(0);
            Paragraph header = new Paragraph("Result information\n\n", new Font(Font.HELVETICA, 14, Font.BOLDITALIC));
            PdfPCell cell = new PdfPCell(header);
            cell.setColspan(1);
            cell.setBorderWidth(0);
            mapInfo.addCell(cell);
            PdfPTable subTable = new PdfPTable(1);
            subTable.getDefaultCell().setBorderWidth(0);
            Paragraph p = new Paragraph();
            Font f = new Font(Font.COURIER, 12);
            Iterator iterator = params.keySet().iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                String name = key + ":";
                while (name.length() < 23) name += " ";
                p.add(new Chunk(" " + name + params.get(key) + "\n", f));
            }
            subTable.addCell(p);
            mapInfo.addCell(subTable);
            mapInfo.setWidthPercentage(100);
            document.add(mapInfo);
        }
        PdfPTable systemInfo = new PdfPTable(1);
        {
            systemInfo.getDefaultCell().setBorderWidth(0);
            Paragraph header = new Paragraph("\n\nSystem information\n\n", new Font(Font.HELVETICA, 14, Font.BOLDITALIC));
            PdfPCell cell = new PdfPCell(header);
            cell.setColspan(1);
            cell.setBorderWidth(0);
            systemInfo.addCell(cell);
            PdfPTable subTable = new PdfPTable(1);
            subTable.getDefaultCell().setBorderWidth(0);
            Paragraph p = new Paragraph();
            Font f = new Font(Font.COURIER, 12);
            Iterator iterator = systemProperties.keySet().iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                String name = key + ":";
                while (name.length() < 23) name += " ";
                p.add(new Chunk(" " + name + systemProperties.get(key) + "\n", f));
            }
            subTable.addCell(p);
            systemInfo.addCell(subTable);
            systemInfo.setWidthPercentage(100);
            document.add(systemInfo);
        }
        document.setMargins(40, 40, 30, 30);
        document.setPageSize(PageSize.A4.rotate());
        document.newPage();
        if (image != null) {
            Image img = Image.getInstance(image, null);
            img.setAlignment(Element.ALIGN_CENTER);
            document.add(img);
        }
        document.setMargins(40, 40, 170, 60);
        document.setPageSize(PageSize.A4);
        document.newPage();
        document.add(new Paragraph("Salesman path\n\n", new Font(Font.HELVETICA, 14, Font.BOLDITALIC)));
        Font tableHeaderFont = new Font(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1250, BaseFont.EMBEDDED), 12, Font.BOLD);
        PdfPTable datatable = new PdfPTable(6);
        int headerwidths[] = { 5, 31, 16, 16, 16, 16 };
        datatable.setWidths(headerwidths);
        datatable.setWidthPercentage(100);
        datatable.getDefaultCell().setPadding(2);
        datatable.getDefaultCell().setBorderWidth(1);
        datatable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell c1 = new PdfPCell(new Phrase("City", tableHeaderFont));
        c1.setColspan(2);
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        c1.setVerticalAlignment(Element.ALIGN_CENTER);
        datatable.addCell(c1);
        datatable.addCell(new Phrase("X\n(S-JTSK)", tableHeaderFont));
        datatable.addCell(new Phrase("Y\n(S-JTSK)", tableHeaderFont));
        datatable.addCell(new Phrase("Distance\n[m]", tableHeaderFont));
        datatable.addCell(new Phrase("Total\n[m]", tableHeaderFont));
        datatable.setHeaderRows(1);
        Font numberFont = new Font(Font.COURIER, 11);
        double totalDistance = 0;
        for (int i = 0; i < cities.length; i++) {
            if (i % 2 == 1) {
                datatable.getDefaultCell().setGrayFill(0.97f);
            }
            datatable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
            datatable.addCell(new Phrase(numberFormatter.format(i + 1), numberFont));
            datatable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            datatable.addCell(new Phrase(cities[i].getName(), tableHeaderFont));
            datatable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
            datatable.addCell(new Phrase(numberFormatter.format(cities[i].getSJTSKX()), numberFont));
            datatable.addCell(new Phrase(numberFormatter.format(cities[i].getSJTSKY()), numberFont));
            String distanceText = "0";
            String totalDistanceText = "0";
            double distance = 0;
            if (i > 0) {
                if (i == 49) {
                    int ii = 0;
                    ii++;
                }
                distance = cities[i].distance(cities[i - 1], false);
                distanceText = numberFormatter.format((int) distance);
                totalDistance += distance;
                totalDistanceText = numberFormatter.format((int) totalDistance);
            }
            datatable.addCell(new Phrase(distanceText, numberFont));
            datatable.addCell(new Phrase(totalDistanceText, numberFont));
            datatable.getDefaultCell().setGrayFill(0.0f);
        }
        document.add(datatable);
        document.close();
    }

    /** Image for page header **/
    Image headerImage;

    /** The headertable. **/
    PdfPTable headerTable;

    /** A template that will hold the total number of pages. **/
    PdfTemplate tpl;

    /** The font that will be used. for header/footer **/
    BaseFont headerFont;

    /**
    * @see com.lowagie.text.pdf.PdfPageEventHelper#onOpenDocument(com.lowagie.text.pdf.PdfWriter, com.lowagie.text.Document)
    */
    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        try {
            headerFont = BaseFont.createFont("Helvetica", BaseFont.WINANSI, false);
            String projectURL = "http://www.saiko.cz/ai/tsp/";
            headerImage = Image.getInstance(getClass().getResource("/org/saiko/etc/logo2.jpg"));
            headerTable = new PdfPTable(1);
            PdfPTable table = new PdfPTable(2);
            Paragraph p = new Paragraph();
            p.setLeading(0, 2);
            p.add(new Chunk("Traveling Salesman Problem\n", new Font(Font.HELVETICA, 16, Font.BOLDITALIC, Color.blue)));
            p.add(new Chunk("Application report\n\n", new Font(Font.HELVETICA, 12, Font.NORMAL)));
            Anchor link = new Anchor(projectURL, new Font(Font.HELVETICA, 11, Font.NORMAL));
            link.setReference(projectURL);
            p.add(link);
            p.add(new Chunk("\n\n" + parameters.get(PARAM_COMPUTATION_TIME) + "\n", new Font(Font.HELVETICA, 11, Font.NORMAL)));
            table.getDefaultCell().setBackgroundColor(Color.WHITE);
            table.getDefaultCell().setBorderWidth(0);
            PdfPCell cell = new PdfPCell(p);
            cell.setPadding(20);
            cell.setBorderWidth(0);
            table.addCell(cell);
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(new Phrase(new Chunk(headerImage, 0, 0)));
            headerTable.addCell(table);
            tpl = writer.getDirectContent().createTemplate(100, 100);
            tpl.setBoundingBox(new Rectangle(-20, -20, 100, 100));
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    /**
    * @see com.lowagie.text.pdf.PdfPageEventHelper#onEndPage(com.lowagie.text.pdf.PdfWriter, com.lowagie.text.Document)
    */
    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();
        if (writer.getPageNumber() != 2) {
            headerTable.setTotalWidth(document.getPageSize().width() - 60);
            headerTable.writeSelectedRows(0, -1, 30, document.getPageSize().top() - 30, cb);
        }
        String text = "page " + writer.getPageNumber() + " of ";
        float textSize = headerFont.getWidthPoint(text, 10);
        float textBase = 40;
        cb.beginText();
        cb.setFontAndSize(headerFont, 10);
        float adjust = headerFont.getWidthPoint("0", 10);
        cb.setTextMatrix(document.right() - textSize - adjust, textBase);
        cb.showText(text);
        cb.endText();
        cb.addTemplate(tpl, document.right() - adjust, textBase);
        cb.saveState();
        cb.setLineWidth(1);
        cb.rectangle(30, 30, document.getPageSize().width() - 60, document.getPageSize().height() - 60);
        cb.stroke();
        cb.restoreState();
    }

    /**
    * @see com.lowagie.text.pdf.PdfPageEventHelper#onCloseDocument(com.lowagie.text.pdf.PdfWriter, com.lowagie.text.Document)
    */
    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        tpl.beginText();
        tpl.setFontAndSize(headerFont, 10);
        tpl.setTextMatrix(0, 0);
        tpl.showText("" + (writer.getPageNumber() - 1));
        tpl.endText();
    }

    /**
    * @return map of system properties for creating report
    */
    protected static Map<String, String> getSystemProperties() {
        Map<String, String> p = new LinkedHashMap<String, String>();
        p.put("availableProcessors", String.valueOf(Runtime.getRuntime().availableProcessors()));
        p.put("freeMemory", numberFormatter.format(Runtime.getRuntime().freeMemory()));
        p.put("totalMemory", numberFormatter.format(Runtime.getRuntime().totalMemory()));
        p.put("maxMemory", numberFormatter.format(Runtime.getRuntime().maxMemory()));
        String props[] = new String[] { "java.vm.name", "java.vm.vendor", "java.vm.version", "os.name", "os.version", "os.arch", "sun.arch.data.model" };
        for (String prop : props) {
            p.put(prop, System.getProperty(prop));
        }
        String envs[] = new String[] { "PROCESSOR_IDENTIFIER", "NUMBER_OF_PROCESSORS", "OS" };
        for (String env : envs) {
            p.put(env, System.getenv(env));
        }
        return p;
    }

    /**
    * @param tsp - the application (result)
    * @return map of result properties
    */
    protected static Map<String, String> getResultInfo(TSP tsp) {
        Map<String, String> p = new LinkedHashMap<String, String>();
        p.put(Report.PARAM_COMPUTATION_TIME, new SimpleDateFormat("yyyy/MM/dd HH:mm").format(Calendar.getInstance().getTime()));
        p.put("Program version", TSP.getAppVersion());
        p.put("TSPEngine", tsp.engineName);
        p.put("Map file", tsp.mapFile);
        p.put("Map file hash", getMapHash(tsp.mapFile));
        p.put("Number of cities", Report.numberFormatter.format(tsp.cities.length));
        p.put("Init. population size", Report.numberFormatter.format(tsp.configuration.initialPopulationSize));
        p.put("Population growth", Report.numberFormatter.format(tsp.configuration.populationGrow));
        if (tsp.engine != null) p.put("Final population size", Report.numberFormatter.format(tsp.engine.getPopulationSize()));
        p.put("Mutation ratio", Report.numberFormatter.format(tsp.configuration.mutationRatio));
        p.put("RMS cost", tsp.configuration.rmsCost ? "true" : "false");
        p.put("Generation", Report.numberFormatter.format(tsp.generation));
        p.put("Time", Report.numberFormatter.format((int) (tsp.runTime / 1000)) + " s.");
        p.put("Best cost age", Report.numberFormatter.format(tsp.bestCostAge));
        if (tsp.bestChromosome != null) p.put("Best distance", Report.numberFormatter.format((int) tsp.bestChromosome.totalDistance));
        return p;
    }

    /**
    * Create hash for map file
    * @param mapFile
    * @return SHA hash of map file converted into BASE64 encoding
    */
    protected static String getMapHash(String mapFile) {
        InputStream i = new Object().getClass().getResourceAsStream("/org/saiko/ai/genetics/tsp/etc/" + mapFile + ".csv");
        if (i == null) return null;
        try {
            byte b[] = new byte[1024];
            int size;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            while ((size = i.read(b)) > 0) {
                buffer.write(b, 0, size);
            }
            i.close();
            MessageDigest digest = MessageDigest.getInstance("SHA");
            return new BASE64Encoder().encode(digest.digest(buffer.toByteArray()));
        } catch (Throwable e) {
            return null;
        }
    }
}
