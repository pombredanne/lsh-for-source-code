package ast.quotesrc;

import ast.DocumentController;
import ast.common.data.Image;
import ast.common.data.MagazineQuote;
import ast.common.data.Quote;
import ast.common.data.Source;
import ast.common.data.SourceType;
import ast.common.data.Table;
import ast.common.data.WebQuote;
import ast.common.error.ASTError;
import ast.common.error.ConfigHandlerError;
import ast.common.error.DBError;
import ast.quotesrc.util.ImportUtils;
import ast.quotesrc.util.SourceUtils;
import com.sun.star.awt.Size;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.document.XEmbeddedObjectSupplier2;
import com.sun.star.drawing.XShape;
import com.sun.star.embed.XEmbeddedObject;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XSheetLinkable;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.text.BibliographyDataField;
import com.sun.star.text.ControlCharacter;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.util.XRefreshable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Manages the referencestyles and sources.
 *
 * @author Feh
 */
public class SourceController {

    /**
     * Postfix of a referencestyle pattern.
     */
    public static final String POSTFIX = "]";

    /**
     * Prefix of a referencestyle pattern.
     */
    public static final String PREFIX = "*[";

    /**
     * Utils for helping to create document elements.
     */
    public SourceUtils sourceUtils;

    /**
     * Utils for helping to import elements.
     */
    public ImportUtils importUtils;

    /**
     * All used patterns for simple search and replace.
     */
    public final String[][] refPatterns;

    /**
     * Localized "Author" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionAuthor;

    /**
     * Localized "City" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionCityOfPublisher;

    /**
     * Localized "Edition" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionEdition;

    /**
     * Localized "Editor" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionEditor;

    /**
     * Localized "Path" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionFilepath;

    /**
     * Localized "Startpage" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionFromPage;

    /**
     * Localized "Issue" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionIssue;

    /**
     * Localized "Last access" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionLastAccess;

    /**
     * Localized "Published in" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionPublishedIn;

    /**
     * Localized "Publisher" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionPublisher;

    /**
     * Localized "Quote" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionQuotetext;

    /**
     * Localized "Brief description" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionShortinfo;

    /**
     * Localized "Subtitle" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionSubtitle;

    /**
     * Localized "Title" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionTitle;

    /**
     * Localized "Endpage" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionToPage;

    /**
     * Localized "URL" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionURL;

    /**
     * Localized "Volume" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionVolume;

    /**
     * Localized "Year" caption ready to be used with {@link ast.gui.util.DialogUtils}.
     */
    public final String[] captionYearOfPublication;

    /**
     * Reference to the {@link ast.DocumentController}.
     */
    private DocumentController docController;

    /**
     * Database which contains the sources.
     */
    private DBController sourcesDB;

    /**
     * Database which contains the referencestyles.
     */
    private DBController quotationStylesDB;

    /**
     * Constructor, opens the databases.
     *
     * @param aDocController Reference to the {@link ast.DocumentController}
     * @param sSourceDBPath Path of source DB
     * @param sQuotationStylesDBPath Path of referencestyle DB
     * @param sSourceBackupDir Backup directory for images and tables
     * @throws DBError If database init failed
     */
    public SourceController(final DocumentController aDocController, final String sSourceDBPath, final String sQuotationStylesDBPath, final String sSourceBackupDir) throws DBError {
        this.docController = aDocController;
        this.sourcesDB = new DBController(sSourceDBPath, sSourceBackupDir);
        this.quotationStylesDB = new DBController(sQuotationStylesDBPath, null);
        this.sourceUtils = new SourceUtils(this.docController);
        this.importUtils = new ImportUtils(this.docController);
        this.refPatterns = new String[][] { new String[] { this.docController.getLanguageController().__("Quotetext"), this.docController.getLanguageController().__("quoteLeft") + "To be, or not to be, that is the question" + this.docController.getLanguageController().__("quoteRight"), this.docController.getLanguageController().__("Quotetex1") }, new String[] { this.docController.getLanguageController().__("Title"), "Hamlet", this.docController.getLanguageController().__("Titl1"), String.valueOf(BibliographyDataField.BOOKTITLE) }, new String[] { this.docController.getLanguageController().__("Subtitle"), "Prince of Denmark", this.docController.getLanguageController().__("Subtitl1"), String.valueOf(BibliographyDataField.ANNOTE) }, new String[] { this.docController.getLanguageController().__("FromPage"), "1", this.docController.getLanguageController().__("FromPag1"), String.valueOf(BibliographyDataField.PAGES) }, new String[] { this.docController.getLanguageController().__("ToPage"), "2", this.docController.getLanguageController().__("ToPag1"), String.valueOf(BibliographyDataField.CUSTOM1) }, new String[] { this.docController.getLanguageController().__("Author"), "Shakespeare, William", this.docController.getLanguageController().__("Autho1"), String.valueOf(BibliographyDataField.AUTHOR) }, new String[] { this.docController.getLanguageController().__("Editor"), "John Heminge and Henry Condell", this.docController.getLanguageController().__("Edito1"), String.valueOf(BibliographyDataField.EDITOR) }, new String[] { this.docController.getLanguageController().__("Publisher"), "Helsingør publishing", this.docController.getLanguageController().__("Publishe1"), String.valueOf(BibliographyDataField.PUBLISHER) }, new String[] { this.docController.getLanguageController().__("CityOfPublisher"), "Helsingør", this.docController.getLanguageController().__("CityOfPublishe1"), String.valueOf(BibliographyDataField.ADDRESS) }, new String[] { this.docController.getLanguageController().__("Issue"), "1", this.docController.getLanguageController().__("Issu1"), String.valueOf(BibliographyDataField.MONTH) }, new String[] { this.docController.getLanguageController().__("YearOfPublication"), "1603", this.docController.getLanguageController().__("YearOfPublicatio1"), String.valueOf(BibliographyDataField.YEAR) }, new String[] { this.docController.getLanguageController().__("PublishedIn"), "Second Quarto Q 2", this.docController.getLanguageController().__("PublishedI1"), String.valueOf(BibliographyDataField.JOURNAL) }, new String[] { this.docController.getLanguageController().__("Volume"), "17.", this.docController.getLanguageController().__("Volum1"), String.valueOf(BibliographyDataField.VOLUME) }, new String[] { this.docController.getLanguageController().__("URL"), "http://www.shakespeare-literature.com/", this.docController.getLanguageController().__("UR1"), String.valueOf(BibliographyDataField.URL) }, new String[] { this.docController.getLanguageController().__("LastAccess"), "28.05.2009", this.docController.getLanguageController().__("LastAcces1"), String.valueOf(BibliographyDataField.CUSTOM2) }, new String[] { this.docController.getLanguageController().__("Edition"), "2.", this.docController.getLanguageController().__("Editio1"), String.valueOf(BibliographyDataField.EDITION) } };
        this.captionAuthor = new String[] { "Author", this.docController.getLanguageController().__("Author") };
        this.captionCityOfPublisher = new String[] { "CityOfPublisher", this.docController.getLanguageController().__("City") };
        this.captionEdition = new String[] { "Edition", this.docController.getLanguageController().__("Edition") };
        this.captionEditor = new String[] { "Editor", this.docController.getLanguageController().__("Editor") };
        this.captionFilepath = new String[] { "Filepath", this.docController.getLanguageController().__("Path") };
        this.captionFromPage = new String[] { "FromPage", this.docController.getLanguageController().__("Startpage") };
        this.captionIssue = new String[] { "Issue", this.docController.getLanguageController().__("Issue") };
        this.captionLastAccess = new String[] { "LastAccess", this.docController.getLanguageController().__("Last access") };
        this.captionPublishedIn = new String[] { "PublishedIn", this.docController.getLanguageController().__("in") };
        this.captionPublisher = new String[] { "Publisher", this.docController.getLanguageController().__("Publisher") };
        this.captionQuotetext = new String[] { "Quotetext", this.docController.getLanguageController().__("Quote") };
        this.captionShortinfo = new String[] { "Shortinfo", this.docController.getLanguageController().__("Brief description") };
        this.captionSubtitle = new String[] { "Subtitle", this.docController.getLanguageController().__("Subtitle") };
        this.captionTitle = new String[] { "Title", this.docController.getLanguageController().__("Title") };
        this.captionToPage = new String[] { "ToPage", this.docController.getLanguageController().__("Endpage") };
        this.captionURL = new String[] { "URL", this.docController.getLanguageController().__("URL") };
        this.captionVolume = new String[] { "Volume", this.docController.getLanguageController().__("Volume") };
        this.captionYearOfPublication = new String[] { "YearOfPublication", this.docController.getLanguageController().__("Year") };
        this.docController.getLogger().info("SourceController initialized!");
    }

    /**
     * Closes the databases.
     */
    public void exit() {
        this.sourcesDB.exit();
        this.quotationStylesDB.exit();
        this.docController.getLogger().info("DBs closed!");
    }

    /**
     * Adds a new source to the database.
     *
     * @param aSource New source to be stored in DB
     * @throws DBError If copying of image or table failed
     */
    public void addNewSource(final Source aSource) throws DBError {
        aSource.setID(this.sourcesDB.addNewObjectToDB(aSource));
        this.sourcesDB.updateObjectInDB(aSource);
        this.docController.getLogger().fine("New source (ID: #" + Long.toString(aSource.getID()) + ") added to DB");
    }

    /**
     * Adds a new quotationstyle to the database.
     *
     * @param aQuotationStyle New quotationstyle to be stored in DB
     */
    public void addNewQuotationStyle(final QuotationStyle aQuotationStyle) {
        try {
            aQuotationStyle.setID(this.quotationStylesDB.addNewObjectToDB(aQuotationStyle));
        } catch (final DBError e) {
            e.severe();
        }
        this.quotationStylesDB.updateObjectInDB(aQuotationStyle);
        this.docController.getLogger().fine("New quotationstyle (ID: #" + Long.toString(aQuotationStyle.getID()) + ") added to DB");
    }

    /**
     * Updates a quotationstyle in the database.
     *
     * @param sName Name of the existing quotationstyle
     * @param aUpdatedQuotationStyle New quotationstyle
     */
    public void updateQuotationStyle(final String sName, final QuotationStyle aUpdatedQuotationStyle) {
        final QuotationStyle qStyle = this.getQuotationStyleByName(sName);
        qStyle.setName(aUpdatedQuotationStyle.getName());
        qStyle.setDescription(aUpdatedQuotationStyle.getDescription());
        this.quotationStylesDB.updateObjectInDB(qStyle);
    }

    /**
     * Updates a source in the database.
     *
     * @param sShortinfo Shortinfo of the existing source
     * @param aUpdatedSource Source containing the new data
     */
    public void updateSource(final String sShortinfo, final Source aUpdatedSource) {
        final Source src = this.getSourceByShortinfo(sShortinfo);
        src.setAuthor(aUpdatedSource.getAuthor());
        src.setCityOfPublisher(aUpdatedSource.getCityOfPublisher());
        src.setEdition(aUpdatedSource.getEdition());
        src.setEditor(aUpdatedSource.getEditor());
        src.setFromPage(aUpdatedSource.getFromPage());
        src.setIssue(aUpdatedSource.getIssue());
        src.setPublishedIn(aUpdatedSource.getPublishedIn());
        src.setPublisher(aUpdatedSource.getPublisher());
        src.setShortinfo(aUpdatedSource.getShortinfo());
        src.setTitle(aUpdatedSource.getTitle());
        src.setToPage(aUpdatedSource.getToPage());
        src.setVolume(aUpdatedSource.getVolume());
        src.setYearOfPublication(aUpdatedSource.getYearOfPublication());
        switch(src.getSourceType()) {
            case IMAGE:
                ((Image) src).setFile(((Image) aUpdatedSource).getFile());
                ((Image) src).setHeight(((Image) aUpdatedSource).getHeight());
                ((Image) src).setWidth(((Image) aUpdatedSource).getWidth());
                ((Image) src).setFiletype(((Image) aUpdatedSource).getFiletype());
                break;
            case MAGAZINEQUOTE:
                ((MagazineQuote) src).setQuotetext(((MagazineQuote) aUpdatedSource).getQuotetext());
                ((MagazineQuote) src).setSubtitle(((MagazineQuote) aUpdatedSource).getSubtitle());
                break;
            case QUOTE:
                ((Quote) src).setQuotetext(((Quote) aUpdatedSource).getQuotetext());
                ((Quote) src).setSubtitle(((Quote) aUpdatedSource).getSubtitle());
                break;
            case TABLE:
                ((Table) src).setFile(((Table) aUpdatedSource).getFile());
                ((Table) src).setRows(((Table) aUpdatedSource).getRows());
                ((Table) src).setCols(((Table) aUpdatedSource).getCols());
                ((Table) src).setFiletype(((Table) aUpdatedSource).getFiletype());
                break;
            case WEBQUOTE:
                ((WebQuote) src).setQuotetext(((WebQuote) aUpdatedSource).getQuotetext());
                ((WebQuote) src).setSubtitle(((WebQuote) aUpdatedSource).getSubtitle());
                ((WebQuote) src).setURL(((WebQuote) aUpdatedSource).getURL());
                ((WebQuote) src).setLastAccess(((WebQuote) aUpdatedSource).getLastAccess());
                break;
        }
        this.sourcesDB.updateObjectInDB(src);
    }

    /**
     * Deletes a quotationstyle specified by a name.
     *
     * @param sName Name of quotationstyle
     * @throws DBError If style could not be found in database
     */
    public void delQuotationStyleByName(final String sName) throws DBError {
        this.quotationStylesDB.delObjectByID(this.getQuotationStyleByName(sName).getID());
    }

    /**
     * Deletes a source specified by a name.
     *
     * @param sShortinfo Name of source
     * @throws DBError If source could not be found in database
     */
    public void delSourceByShortinfo(final String sShortinfo) throws DBError {
        this.sourcesDB.delObjectByID(this.getSourceByShortinfo(sShortinfo).getID());
    }

    /**
     * Returns a list of sources with stated shortinfo.
     *
     * @param sShortinfo Shortinfo of the source
     * @return List of sources
     */
    public Source getSourceByShortinfo(final String sShortinfo) {
        final LinkedList<Source> sources = new LinkedList<Source>();
        sources.add(this.getWebQuoteByShortinfo(sShortinfo));
        sources.add(this.getMagazineQuoteByShortinfo(sShortinfo));
        sources.add(this.getQuoteByShortinfo(sShortinfo));
        sources.add(this.getTableByShortinfo(sShortinfo));
        sources.add(this.getImageByShortinfo(sShortinfo));
        for (final Source curSource : sources) if (curSource != null) return curSource;
        return null;
    }

    /**
     * Returns a list of quotes with stated shortinfo.
     *
     * @param sShortinfo Shortinfo of the quote
     * @return List of quotes
     */
    public Quote getQuoteByShortinfo(final String sShortinfo) {
        final Quote protoQuote = new Quote(0, null, null, null, null, null, null, null, sShortinfo, null, null, null, null, 0, null, null, null);
        final Iterator<Quote> resultQBE = this.sourcesDB.getObjectsByExample(protoQuote).iterator();
        return resultQBE.hasNext() ? resultQBE.next() : null;
    }

    /**
     * Returns a list of magazine quotes with stated shortinfo.
     *
     * @param sShortinfo Shortinfo of the magazinequote
     * @return List of magazinequotes
     */
    public MagazineQuote getMagazineQuoteByShortinfo(final String sShortinfo) {
        final MagazineQuote protoMagazineQuote = new MagazineQuote(0, null, null, null, null, null, null, null, sShortinfo, null, null, null, null, 0, null, null);
        final Iterator<MagazineQuote> resultQBE = this.sourcesDB.getObjectsByExample(protoMagazineQuote).iterator();
        return resultQBE.hasNext() ? resultQBE.next() : null;
    }

    /**
     * Returns a list of tables with stated shortinfo.
     *
     * @param sShortinfo Shortinfo of the table
     * @return List of tables
     */
    public Table getTableByShortinfo(final String sShortinfo) {
        final Table protoTable = new Table(0, null, null, null, null, null, null, null, sShortinfo, null, null, null, null, 0, null, 0, 0, null);
        final Iterator<Table> resultQBE = this.sourcesDB.getObjectsByExample(protoTable).iterator();
        return resultQBE.hasNext() ? resultQBE.next() : null;
    }

    /**
     * Returns a list of images with stated shortinfo.
     *
     * @param sShortinfo Shortinfo of the image
     * @return List of images
     */
    public Image getImageByShortinfo(final String sShortinfo) {
        final Image protoImage = new Image(0, null, null, null, null, null, null, null, sShortinfo, null, null, null, null, 0, null, 0, 0, null);
        final Iterator<Image> resultQBE = this.sourcesDB.getObjectsByExample(protoImage).iterator();
        return resultQBE.hasNext() ? resultQBE.next() : null;
    }

    /**
     * Returns a list of webquotes with stated shortinfo.
     *
     * @param sShortinfo Shortinfo of the webquote
     * @return List of webquotes
     */
    public WebQuote getWebQuoteByShortinfo(final String sShortinfo) {
        final WebQuote protoWebQuote = new WebQuote(0, null, null, null, null, null, null, null, sShortinfo, null, null, null, null, 0, null, null, null, null);
        final Iterator<WebQuote> resultQBE = this.sourcesDB.getObjectsByExample(protoWebQuote).iterator();
        return resultQBE.hasNext() ? resultQBE.next() : null;
    }

    /**
     * Returns a list of quotationstyles with stated name.
     *
     * @param sName Name of the quotationstyle
     * @return List of quotationstyles
     */
    public QuotationStyle getQuotationStyleByName(final String sName) {
        final QuotationStyle protoQuotationStyle = new QuotationStyle(sName, null, 0);
        final Iterator<QuotationStyle> resultQBE = this.quotationStylesDB.getObjectsByExample(protoQuotationStyle).iterator();
        return resultQBE.hasNext() ? resultQBE.next() : null;
    }

    /**
     * Returns a list of all sources.
     *
     * @return List of all sources
     */
    public LinkedList<Source> getAllSources() {
        final LinkedList<Source> sources = this.sourcesDB.getObjectsByExample(Source.class);
        final Comparator<Object> comp = new ObjectComperator();
        Collections.sort(sources, comp);
        return sources;
    }

    /**
     * Returns a list of all quotes.
     *
     * @return List of all quotes
     */
    public LinkedList<Quote> getAllQuotes() {
        final LinkedList<Quote> quotes = this.sourcesDB.getObjectsByExample(Quote.class);
        final Comparator<Object> comp = new ObjectComperator();
        Collections.sort(quotes, comp);
        return quotes;
    }

    /**
     * Returns a list of all webquotes.
     *
     * @return List of all webquotes
     */
    public LinkedList<WebQuote> getAllWebQuotes() {
        final LinkedList<WebQuote> webquotes = this.sourcesDB.getObjectsByExample(WebQuote.class);
        final Comparator<Object> comp = new ObjectComperator();
        Collections.sort(webquotes, comp);
        return webquotes;
    }

    /**
     * Returns a list of all magazinequotes.
     *
     * @return List of all magazinequotes
     */
    public LinkedList<MagazineQuote> getAllMagazineQuotes() {
        final LinkedList<MagazineQuote> magazinequotes = this.sourcesDB.getObjectsByExample(MagazineQuote.class);
        final Comparator<Object> comp = new ObjectComperator();
        Collections.sort(magazinequotes, comp);
        return magazinequotes;
    }

    /**
     * Returns a list of all images.
     *
     * @return List of all images
     */
    public LinkedList<Image> getAllImages() {
        final LinkedList<Image> images = this.sourcesDB.getObjectsByExample(Image.class);
        final Comparator<Object> comp = new ObjectComperator();
        Collections.sort(images, comp);
        return images;
    }

    /**
     * Returns a list of all tables.
     *
     * @return List of all tables
     */
    public LinkedList<Table> getAllTables() {
        final LinkedList<Table> tables = this.sourcesDB.getObjectsByExample(Table.class);
        final Comparator<Object> comp = new ObjectComperator();
        Collections.sort(tables, comp);
        return tables;
    }

    /**
     * Returns a list of all referencestyles.
     *
     * @return List of all referencestyles
     */
    public LinkedList<QuotationStyle> getAllQuotationStyles() {
        final LinkedList<QuotationStyle> quotationStyles = this.quotationStylesDB.getObjectsByExample(QuotationStyle.class);
        final Comparator<Object> comp = new ObjectComperator();
        Collections.sort(quotationStyles, comp);
        return quotationStyles;
    }

    /**
     * Insert a source into the current Text.
     *
     * @param sText Text of the quote which will be insert
     * @param aSource Source which will be insert
     * @throws ConfigHandlerError If {@link ast.common.util.PathUtils}
     *                            initialization failed
     * @throws com.sun.star.uno.Exception
     */
    public void insertQuotation(final String sText, final Source aSource) throws ConfigHandlerError, com.sun.star.uno.Exception {
        final XTextDocument xTextDocument = (XTextDocument) this.docController.getXInterface(XTextDocument.class, this.docController.getXFrame().getController().getModel());
        final XMultiServiceFactory xMultiServiceFactory = (XMultiServiceFactory) this.docController.getXInterface(XMultiServiceFactory.class, xTextDocument);
        final XText xText = xTextDocument.getText();
        final XTextViewCursor xTextViewCursor = ((XTextViewCursorSupplier) this.docController.getXInterface(XTextViewCursorSupplier.class, this.docController.getXFrame().getController())).getViewCursor();
        final XTextRange xTextRange = xTextViewCursor.getStart();
        if (aSource.getSourceType() == SourceType.QUOTE || aSource.getSourceType() == SourceType.WEBQUOTE || aSource.getSourceType() == SourceType.WEBQUOTE) {
            final XPropertySet xQuoteProps = (XPropertySet) this.docController.getXInterface(XPropertySet.class, xTextViewCursor);
            if (this.docController.getTemplateController().isIndentation()) {
                xText.insertControlCharacter(xTextViewCursor, ControlCharacter.PARAGRAPH_BREAK, false);
                xQuoteProps.setPropertyValue("ParaStyleName", new String("Quotations"));
            }
            xQuoteProps.setPropertyValue("CharStyleName", new String("Citation"));
            final Object aBookmark = xMultiServiceFactory.createInstance("com.sun.star.text.Bookmark");
            this.sourceUtils.setNameToObject(aBookmark, this.docController.getLanguageController().__("Quote: ") + aSource.getShortinfo());
            final XTextContent xTextContent = (XTextContent) this.docController.getXInterface(XTextContent.class, aBookmark);
            xText.insertTextContent(xTextRange, xTextContent, false);
            this.sourceUtils.insertBibliographyEntry(xMultiServiceFactory, xTextRange, aSource, sText);
            if (this.docController.getTemplateController().isIndentation()) {
                xText.insertControlCharacter(xTextViewCursor, ControlCharacter.PARAGRAPH_BREAK, false);
                xQuoteProps.setPropertyValue("ParaStyleName", new String(this.docController.getLanguageController().__("Default")));
            }
            xQuoteProps.setPropertyValue("CharStyleName", new String(this.docController.getLanguageController().__("Default")));
        } else if (aSource.getSourceType() == SourceType.IMAGE || aSource.getSourceType() == SourceType.TABLE) {
            xText.insertControlCharacter(xTextRange, ControlCharacter.PARAGRAPH_BREAK, false);
            final XTextFrame xFrame = this.sourceUtils.getTextFrame(aSource.getShortinfo(), xMultiServiceFactory);
            xText.insertTextContent(xTextRange, xFrame, false);
            final XText xFrameText = xFrame.getText();
            final XTextCursor xFrameCursor = xFrameText.createTextCursor();
            final Size aNewSize = new Size();
            XPropertySet xBaseFrameProps = null;
            final Size aPageTextAreaSize = this.sourceUtils.getPageTextAreaSize(xTextDocument, xTextViewCursor);
            if (aSource.getSourceType() == SourceType.IMAGE) {
                try {
                    this.sourceUtils.setNameToObject(xFrame, this.docController.getLanguageController().__("Caption illustration: ") + aSource.getShortinfo());
                    final XNameContainer xBitmapContainer = (XNameContainer) this.docController.getXInterface(XNameContainer.class, xMultiServiceFactory.createInstance("com.sun.star.drawing.BitmapTable"));
                    final XTextContent xImage = (XTextContent) this.docController.getXInterface(XTextContent.class, xMultiServiceFactory.createInstance("com.sun.star.text.TextGraphicObject"));
                    this.sourceUtils.setNameToObject(xImage, this.docController.getLanguageController().__("Illustration: ") + aSource.getShortinfo());
                    final String graphicURL = this.docController.getPathUtils().getFileURLFromSystemPath(((Image) aSource).getFile().getPath(), ((Image) aSource).getFile().getPath());
                    xBaseFrameProps = (XPropertySet) this.docController.getXInterface(XPropertySet.class, xImage);
                    xBaseFrameProps.setPropertyValue("AnchorType", com.sun.star.text.TextContentAnchorType.AT_PARAGRAPH);
                    xBaseFrameProps.setPropertyValue("GraphicURL", graphicURL);
                    final MessageDigest md = MessageDigest.getInstance("MD5");
                    md.update(graphicURL.getBytes(), 0, graphicURL.length());
                    final String internalName = new BigInteger(1, md.digest()).toString(16);
                    xBitmapContainer.insertByName(internalName, graphicURL);
                    final String internalURL = (String) (xBitmapContainer.getByName(internalName));
                    xBaseFrameProps.setPropertyValue("GraphicURL", internalURL);
                    float imageRatio = (float) this.sourceUtils.getImageSize(((Image) aSource).getFile()).Height / (float) this.sourceUtils.getImageSize(((Image) aSource).getFile()).Width;
                    final Size aUsedAreaSize = new Size(this.sourceUtils.getImageSize(((Image) aSource).getFile()).Width * 26, this.sourceUtils.getImageSize(((Image) aSource).getFile()).Height * 26);
                    if (aUsedAreaSize.Width >= aPageTextAreaSize.Width) {
                        aNewSize.Width = aPageTextAreaSize.Width;
                        aNewSize.Height = (int) (aPageTextAreaSize.Width * imageRatio);
                    } else {
                        aNewSize.Width = aUsedAreaSize.Width;
                        aNewSize.Height = aUsedAreaSize.Height;
                    }
                    xFrameText.insertTextContent(xFrameCursor, xImage, false);
                    xBitmapContainer.removeByName(internalName);
                } catch (final NoSuchAlgorithmException e) {
                    new ASTError(e).severe();
                }
                this.sourceUtils.insertCaption(xFrame, aSource, this.docController.getLanguageController().__("Illustration"), xMultiServiceFactory);
            } else if (aSource.getSourceType() == SourceType.TABLE) {
                this.sourceUtils.setNameToObject(xFrame, this.docController.getLanguageController().__("Caption table: ") + aSource.getShortinfo());
                xBaseFrameProps = this.sourceUtils.createTextEmbeddedObjectCalc(xMultiServiceFactory);
                this.sourceUtils.setNameToObject(xBaseFrameProps, this.docController.getLanguageController().__("Table: ") + aSource.getShortinfo());
                xFrameText.insertTextContent(xFrameCursor, (XTextContent) this.docController.getXInterface(XTextContent.class, xBaseFrameProps), false);
                final XEmbeddedObjectSupplier2 xEmbeddedObjectSupplier = (XEmbeddedObjectSupplier2) this.docController.getXInterface(XEmbeddedObjectSupplier2.class, xBaseFrameProps);
                final XEmbeddedObject xEmbeddedObject = xEmbeddedObjectSupplier.getExtendedControlOverEmbeddedObject();
                long nAspect = xEmbeddedObjectSupplier.getAspect();
                final Size aVisualAreaSize = xEmbeddedObject.getVisualAreaSize(nAspect);
                final XComponent xComponent = xEmbeddedObjectSupplier.getEmbeddedObject();
                XSpreadsheets xSpreadsheets = ((XSpreadsheetDocument) this.docController.getXInterface(XSpreadsheetDocument.class, xComponent)).getSheets();
                final XIndexAccess xIndexAccess = (XIndexAccess) this.docController.getXInterface(XIndexAccess.class, xSpreadsheets);
                final XSpreadsheet xSpreadsheet = (XSpreadsheet) this.docController.getXInterface(XSpreadsheet.class, xIndexAccess.getByIndex(0));
                final XSheetLinkable xSheetLinkable = (XSheetLinkable) this.docController.getXInterface(XSheetLinkable.class, xSpreadsheet);
                xSheetLinkable.link(this.docController.getPathUtils().getFileURLFromSystemPath(((Table) aSource).getFile().getPath(), ((Table) aSource).getFile().getPath()), "", "", "", com.sun.star.sheet.SheetLinkMode.NORMAL);
                final CellRangeAddress aUsedArea = this.sourceUtils.getUsedArea(xSpreadsheet);
                final Size aUsedAreaSize = this.sourceUtils.calcCellRangeSize(xSpreadsheets, aUsedArea);
                if ((aUsedAreaSize.Width != aVisualAreaSize.Width) || (aUsedAreaSize.Height != aVisualAreaSize.Height)) {
                    aNewSize.Height = (aUsedAreaSize.Height > aPageTextAreaSize.Height) ? aPageTextAreaSize.Height : aUsedAreaSize.Height;
                    aNewSize.Width = (aUsedAreaSize.Width > aPageTextAreaSize.Width) ? aPageTextAreaSize.Width : aUsedAreaSize.Width;
                    xEmbeddedObject.setVisualAreaSize(nAspect, aNewSize);
                }
                this.sourceUtils.insertCaption(xFrame, aSource, this.docController.getLanguageController().__("Table"), xMultiServiceFactory);
            }
            xBaseFrameProps.setPropertyValue("Width", aNewSize.Width);
            xBaseFrameProps.setPropertyValue("Height", aNewSize.Height);
            final XShape xShape = (XShape) this.docController.getXInterface(XShape.class, xFrame);
            xShape.setSize(aNewSize);
        }
        final XTextFieldsSupplier xTextFieldsSupplier = (XTextFieldsSupplier) this.docController.getXInterface(XTextFieldsSupplier.class, xMultiServiceFactory);
        final XRefreshable xRefreshable = (XRefreshable) this.docController.getXInterface(XRefreshable.class, xTextFieldsSupplier.getTextFields());
        xRefreshable.refresh();
    }

    /**
     * Nested class for helping sort the arrays of sources or quotationstyles.
     *
     * @author feh
     */
    private class ObjectComperator implements Comparator {

        /**
         * {@inheritDoc}
         *
         * @param aObj1 First object to compare with second one
         * @param aObj2 Second object to compare with first one
         * @return Ordering number
         */
        @Override
        public int compare(final Object aObj1, final Object aObj2) {
            return aObj1 == null && aObj2 != null ? 1 : (aObj1 != null && aObj2 == null ? -1 : (aObj1 == null && aObj2 == null ? 0 : (aObj1 instanceof QuotationStyle) ? (((QuotationStyle) aObj1).getName().compareToIgnoreCase(((QuotationStyle) aObj2).getName())) : (((Source) aObj1).getShortinfo().compareToIgnoreCase(((Source) aObj2).getShortinfo()))));
        }
    }
}
