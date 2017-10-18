package net.sf.eos.sentence;

import net.sf.eos.EosException;
import net.sf.eos.analyzer.ResettableTokenizer;
import net.sf.eos.analyzer.SentenceTokenizer;
import net.sf.eos.analyzer.TextBuilder;
import net.sf.eos.analyzer.Token;
import net.sf.eos.analyzer.TokenizerException;
import net.sf.eos.document.EosDocument;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple default implementation.
 * @author Sascha Kohlmann
 */
public class DefaultSentencer extends Sentencer {

    /** For logging. */
    private static final Log LOG = LogFactory.getLog(DefaultSentencer.class.getName());

    /** Creates a new instance. */
    public DefaultSentencer() {
        super();
    }

    @SuppressWarnings("nls")
    @Override
    public Map<String, EosDocument> toSentenceDocuments(final EosDocument doc, final SentenceTokenizer sentencer, final ResettableTokenizer tokenizer, final TextBuilder builder) throws EosException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("SentenceTokenizer instance: " + sentencer.getClass());
            LOG.debug("ResettableTokenizer instance: " + tokenizer.getClass());
            LOG.debug("TextBuilder instance: " + builder.getClass());
        }
        final Map<String, EosDocument> retval = new HashMap<String, EosDocument>();
        final MessageDigest md = createDigester();
        final Map<String, List<String>> meta = doc.getMeta();
        final CharSequence newTitle = extractTitle(doc, tokenizer, builder);
        final List<CharSequence> sentences = extractSentences(doc, sentencer, tokenizer, builder);
        for (final CharSequence newText : sentences) {
            final EosDocument newDoc = new EosDocument();
            newDoc.setText(newText);
            newDoc.setTitle(newTitle);
            final Map<String, List<String>> newMeta = newDoc.getMeta();
            newMeta.putAll(meta);
            try {
                final byte[] bytes = ("" + newText).getBytes("UTF-8");
                md.reset();
                final byte[] key = md.digest(bytes);
                final char[] asChar = Hex.encodeHex(key);
                final String asString = new String(asChar);
                retval.put(asString, newDoc);
            } catch (final UnsupportedEncodingException e) {
                throw new TokenizerException(e);
            }
        }
        return retval;
    }

    final List<CharSequence> extractSentences(final EosDocument doc, final SentenceTokenizer sentencer, final ResettableTokenizer tokenizer, final TextBuilder builder) throws EosException {
        final List<CharSequence> sentences = new ArrayList<CharSequence>();
        final CharSequence text = doc.getText();
        if (text != null) {
            sentencer.reset(text);
            Token sentence = null;
            while ((sentence = sentencer.next()) != null) {
                final CharSequence seq = sentence.getTokenText();
                tokenizer.reset(seq);
                final List<Token> textTokens = new ArrayList<Token>();
                Token textToken = null;
                while ((textToken = tokenizer.next()) != null) {
                    textTokens.add(textToken);
                }
                final CharSequence newText = builder.buildText(textTokens);
                sentences.add(newText);
            }
        }
        return sentences;
    }

    final CharSequence extractTitle(final EosDocument doc, final ResettableTokenizer tokenizer, final TextBuilder builder) throws EosException {
        final CharSequence title = doc.getTitle();
        final List<Token> titleTokens = new ArrayList<Token>();
        tokenizer.reset(title);
        Token titleToken = null;
        while ((titleToken = tokenizer.next()) != null) {
            titleTokens.add(titleToken);
        }
        final CharSequence newTitle = builder.buildText(titleTokens);
        return newTitle;
    }
}
