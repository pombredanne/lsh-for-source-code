package de.alexanderwilden.jatobo.modules.fetenplaner;

import java.io.IOException;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.SAXException;
import de.alexanderwilden.jatobo.modules.JatoboModule;
import de.alexanderwilden.jatoli.JatoliConstants;
import de.alexanderwilden.jatoli.JatoliUtils;
import de.alexanderwilden.jatoli.commands.CommandFactory;
import de.alexanderwilden.jatoli.commands.JatoliCommand;
import de.alexanderwilden.jatoli.events.ImInEvent;
import de.alexanderwilden.jatoli.events.JatoliEvent;

public class Fetenplaner extends JatoboModule {

    private long lastModified = 0;

    private Event eventCached = null;

    @Override
    public void close() {
    }

    @Override
    public String getModuleKey() {
        return "fetenplaner";
    }

    @Override
    public String getModuleName() {
        return "Fetenplaner";
    }

    @Override
    public void performAction(final List<JatoliEvent> events) {
        if (events != null) {
            for (final JatoliEvent event : events) {
                if (!event.isConsumed() && event instanceof ImInEvent) {
                    final ImInEvent imIn = (ImInEvent) event;
                    String msg = JatoliUtils.cleanMessage(imIn.getMessage()).trim();
                    msg = msg.replaceAll("[!\\?\\.,]+", "");
                    final List<String> msgParts = JatoliUtils.getMessageParts(msg);
                    if (!msgParts.isEmpty()) {
                        boolean foundRequest = false;
                        for (final String part : msgParts) {
                            if (part.equalsIgnoreCase("feten") || part.equalsIgnoreCase("fete") || part.equalsIgnoreCase("party") || part.equalsIgnoreCase("parties") || part.equalsIgnoreCase("partys")) {
                                foundRequest = true;
                                break;
                            }
                        }
                        if (foundRequest) {
                            final JatoliCommand command = CommandFactory.getSendImCommand(imIn.getSender(), getNextEvents(), false);
                            bot.getConn().enqueueCommand(command);
                            imIn.consume();
                        }
                    }
                }
            }
        }
    }

    private String createHash() {
        String hash = "";
        try {
            final java.util.Calendar c = java.util.Calendar.getInstance();
            String day = "" + c.get(java.util.Calendar.DATE);
            day = (day.length() == 1) ? '0' + day : day;
            String month = "" + (c.get(java.util.Calendar.MONTH) + 1);
            month = (month.length() == 1) ? '0' + month : month;
            final String hashString = getStringProperty("hashkey") + day + "." + month + "." + c.get(java.util.Calendar.YEAR);
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(hashString.getBytes());
            final byte digest[] = md.digest();
            hash = "";
            for (int i = 0; i < digest.length; i++) {
                final String s = Integer.toHexString(digest[i] & 0xFF);
                hash += ((s.length() == 1) ? "0" + s : s);
            }
        } catch (final NoSuchAlgorithmException e) {
            bot.getLogger().log(e);
        }
        return hash;
    }

    private String getNextEvents() {
        final StringBuilder builder = new StringBuilder();
        builder.append("\n");
        try {
            final String url = getStringProperty("url") + "?mode=next&hash=" + createHash();
            final URLConnection urlConnection = (new java.net.URL(url)).openConnection();
            final long newModified = urlConnection.getLastModified();
            Event event = null;
            if (newModified == lastModified) {
                event = eventCached;
            } else {
                final EventXmlParser handler = new EventXmlParser();
                final SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
                saxParser.parse(urlConnection.getInputStream(), handler);
                event = handler.getResult();
                eventCached = event;
                lastModified = newModified;
            }
            builder.append("Die naechsten Feten:\n\n");
            if (event != null) {
                do {
                    builder.append(eventToString(event));
                    if (event.hasNextEvent()) {
                        builder.append("\n---------------------------------\n");
                    }
                    event = event.getNextEvent();
                } while (event != null);
            } else {
                builder.append("Im Moment sind leider keine Daten verfuegbar.");
            }
        } catch (final SAXException e) {
            builder.append("Leider ist ein Fehler aufgetreten.\n").append("Versuch es spaeter nocheinmal.");
            e.printStackTrace();
            bot.getLogger().log(e);
        } catch (final IOException e) {
            builder.append("Leider ist ein Fehler aufgetreten.\n").append("Versuch es spaeter nocheinmal.");
            e.printStackTrace();
            bot.getLogger().log(e);
        } catch (final ParserConfigurationException e) {
            builder.append("Leider ist ein Fehler aufgetreten.\n").append("Versuch es spaeter nocheinmal.");
            e.printStackTrace();
            bot.getLogger().log(e);
        }
        return builder.toString();
    }

    private String eventToString(final Event e) {
        final StringBuilder res = new StringBuilder();
        if (e.getInfo().equals("")) {
            final Calendar cal = Calendar.getInstance();
            final String datumtmp[] = e.getDatum().split("\\.");
            cal.set(Calendar.DATE, Integer.parseInt(datumtmp[0]));
            cal.set(Calendar.MONTH, Integer.parseInt(datumtmp[1]) - 1);
            cal.set(Calendar.YEAR, Integer.parseInt(datumtmp[2]));
            final int weekday = cal.get(Calendar.DAY_OF_WEEK);
            e.setInfo(Event.getWeekdayName(weekday));
        }
        res.append(e.getInfo()).append(": ").append(e.getDatum()).append(" - ").append(e.getUhrzeit()).append(" Uhr: ").append(e.getTitel()).append("\n");
        res.append(e.getLocation());
        if (!e.getPlz().equals("") || !e.getOrt().equals("")) {
            res.append(",");
            if (!e.getPlz().equals("")) {
                res.append(" ").append(e.getPlz());
            }
            if (!e.getOrt().equals("")) {
                res.append(" ").append(e.getOrt());
            }
        }
        res.append("\n");
        if (!e.getVeranstalter().equals("")) {
            res.append(e.getVeranstalter()).append("\n");
        }
        if (bot.getConn().getProtocol() == JatoliConstants.PROTOCOL_AIM) {
            res.append("<a href=\"").append(e.getDetailLink()).append("\">mehr Informationen</a>\n");
        } else {
            res.append(e.getDetailLink());
        }
        return res.toString();
    }
}
