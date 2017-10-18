package org.ccnx.ccn.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.logging.Level;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.CCNInterestListener;
import org.ccnx.ccn.config.SystemConfiguration;
import org.ccnx.ccn.impl.security.crypto.ContentKeys;
import org.ccnx.ccn.impl.support.DataUtils;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.content.Link.LinkObject;
import org.ccnx.ccn.profiles.SegmentationProfile;
import org.ccnx.ccn.profiles.VersioningProfile;
import org.ccnx.ccn.profiles.security.access.AccessControlManager;
import org.ccnx.ccn.profiles.security.access.AccessDeniedException;
import org.ccnx.ccn.protocol.CCNTime;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.Exclude;
import org.ccnx.ccn.protocol.ExcludeComponent;
import org.ccnx.ccn.protocol.Interest;
import org.ccnx.ccn.protocol.KeyLocator;
import org.ccnx.ccn.protocol.PublisherPublicKeyDigest;
import org.ccnx.ccn.protocol.SignedInfo.ContentType;

/**
 * This abstract class is the superclass of all classes representing an input stream of
 * bytes segmented and stored in CCN. 
 * 
 * @see SegmentationProfile for description of CCN segmentation
 */
public abstract class CCNAbstractInputStream extends InputStream implements CCNInterestListener {

    /**
	 * Flags:
	 * DONT_DEREFERENCE to prevent dereferencing in case we are attempting to read a link.
	 */
    protected CCNHandle _handle;

    /**
	 * The Link we dereferenced to get here, if any. This may contain
	 * a link dereferenced to get to it, and so on.
	 */
    protected LinkObject _dereferencedLink = null;

    public enum FlagTypes {

        DONT_DEREFERENCE
    }

    ;

    protected EnumSet<FlagTypes> _flags = EnumSet.noneOf(FlagTypes.class);

    /**
	 * The segment we are currently reading from.
	 */
    protected ContentObject _currentSegment = null;

    /**
	 *  first segment of the stream we are reading, which is the GONE segment (see ContentType) if content is deleted.
	 *  this cached first segment is used to supply certain information it contains, such as for computing digest only 
	 *  when required
	 */
    private ContentObject _firstSegment = null;

    /**
	 * Internal stream used for buffering reads. May include filters.
	 */
    protected InputStream _segmentReadStream = null;

    /**
	 * The name prefix of the segmented stream we are reading, up to (but not including)
	 * a segment number.
	 */
    protected ContentName _baseName = null;

    /**
	 * The publisher we are looking for, either specified by querier on initial
	 * read, or read from previous blocks (for now, we assume that all segments in a
	 * stream are created by the same publisher).
	 */
    protected PublisherPublicKeyDigest _publisher = null;

    /**
	 * The segment number to start with. If not specified, is SegmentationProfile#baseSegment().
	 */
    protected Long _startingSegmentNumber = null;

    /**
	 * The timeout to use for segment retrieval. 
	 */
    protected int _timeout = SystemConfiguration.getDefaultTimeout();

    /**
	 *  Encryption/decryption handler.
	 */
    protected Cipher _cipher;

    protected ContentKeys _keys;

    /**
	 * If this content uses Merkle Hash Trees or other bulk signatures to amortize
	 * signature cost, we can amortize verification cost as well by caching verification
	 * data as follows: store the currently-verified root signature, so we don't have to re-verify it;
	 * and the verified root hash. For each piece of incoming content, see if it aggregates
	 * to the same root, if so don't reverify signature. If not, assume it's part of
	 * a new tree and change the root.
	 */
    protected byte[] _verifiedRootSignature = null;

    protected byte[] _verifiedProxy = null;

    protected boolean _atEOF = false;

    /**
	 * Used for mark(int) and reset().
	 */
    protected int _readlimit = 0;

    protected int _markOffset = 0;

    protected long _markBlock = 0;

    protected ArrayList<ContentObject> inOrderSegments = new ArrayList<ContentObject>();

    protected ArrayList<ContentObject> outOfOrderSegments = new ArrayList<ContentObject>();

    protected long _nextPipelineSegment = -1;

    protected long _lastRequestedPipelineSegment = -1;

    protected long _lastInOrderSegment = -1;

    protected ContentName _basePipelineName = null;

    protected long _lastSegmentNumber = -1;

    protected ArrayList<Interest> _sentInterests = new ArrayList<Interest>();

    private Thread waitingThread = null;

    private long waitingSegment;

    private long waitSleep = 0;

    private long _holes = 0;

    private long _totalReceived = 0;

    private long _pipelineStartTime;

    private Object readerReadyObj = new Object();

    private long readerReadyVal = -1;

    private double avgResponseTime = -1;

    private Thread processor = null;

    private long processingSegment = -1;

    private ArrayList<IncomingSegment> incoming = new ArrayList<IncomingSegment>();

    private int processingDefer = 0;

    /**
	 * Set up an input stream to read segmented CCN content under a given name. 
	 * Note that this constructor does not currently retrieve any
	 * data; data is not retrieved until read() is called. This will change in the future, and
	 * this constructor will retrieve the first block.
	 * 
	 * @param baseName Name to read from. If contains a segment number, will start to read from that
	 *    segment.
	 * @param startingSegmentNumber Alternative specification of starting segment number. If
	 * 		unspecified, will be SegmentationProfile#baseSegment().
	 * @param publisher The key we require to have signed this content. If null, will accept any publisher
	 * 				(subject to higher-level verification).
	 * @param keys The keys to use to decrypt this content. Null if content unencrypted, or another
	 * 				process will be used to retrieve the keys.
	 * @param handle The CCN handle to use for data retrieval. If null, the default handle
	 * 		given by CCNHandle#getHandle() will be used.
	 * @throws IOException Not currently thrown, will be thrown when constructors retrieve first block.
	 */
    public CCNAbstractInputStream(ContentName baseName, Long startingSegmentNumber, PublisherPublicKeyDigest publisher, ContentKeys keys, EnumSet<FlagTypes> flags, CCNHandle handle) throws IOException {
        super();
        if (null == baseName) {
            throw new IllegalArgumentException("baseName cannot be null!");
        }
        _handle = handle;
        if (null == _handle) {
            _handle = CCNHandle.getHandle();
        }
        _publisher = publisher;
        if (null != keys) {
            keys.requireDefaultAlgorithm();
            _keys = keys;
        }
        if (null != flags) {
            _flags = flags;
        }
        _baseName = baseName;
        if (SegmentationProfile.isSegment(baseName)) {
            _startingSegmentNumber = SegmentationProfile.getSegmentNumber(baseName);
            _baseName = baseName.parent();
        } else {
            _startingSegmentNumber = SegmentationProfile.baseSegment();
        }
        if (startingSegmentNumber != null) {
            _startingSegmentNumber = startingSegmentNumber;
        }
        Log.info(Log.FAC_IO, "CCNAbstractInputStream: {0} segment {1}", _baseName, _startingSegmentNumber);
        startPipeline();
    }

    /**
	 * Set up an input stream to read segmented CCN content starting with a given
	 * ContentObject that has already been retrieved.  
	 * @param startingSegment The first segment to read from. If this is not the
	 * 		first segment of the stream, reading will begin from this point.
	 * 		We assume that the signature on this segment was verified by our caller.
	 * @param keys The keys to use to decrypt this content. Null if content unencrypted, or another
	 * 				process will be used to retrieve the keys.
	 * @param any flags necessary for processing this stream; have to hand in in constructor in case
	 * 		first segment provided, so can apply to that segment
	 * @param handle The CCN handle to use for data retrieval. If null, the default handle
	 * 		given by CCNHandle#getHandle() will be used.
	 * @throws IOException
	 */
    public CCNAbstractInputStream(ContentObject startingSegment, ContentKeys keys, EnumSet<FlagTypes> flags, CCNHandle handle) throws IOException {
        super();
        _handle = handle;
        if (null == _handle) {
            _handle = CCNHandle.getHandle();
        }
        if (null != keys) {
            keys.requireDefaultAlgorithm();
            _keys = keys;
        }
        if (null != flags) {
            _flags = flags;
        }
        _baseName = SegmentationProfile.segmentRoot(startingSegment.name());
        try {
            _startingSegmentNumber = SegmentationProfile.getSegmentNumber(startingSegment.name());
        } catch (NumberFormatException nfe) {
            throw new IOException("Stream starter segment name does not contain a valid segment number, so the stream does not know what content to start with.");
        }
        setFirstSegment(startingSegment);
        Log.info(Log.FAC_IO, "CCNAbstractInputStream: {0} segment {1}", _baseName, _startingSegmentNumber);
        startPipeline();
    }

    private void startPipeline() {
        synchronized (inOrderSegments) {
            Log.info(Log.FAC_PIPELINE, "PIPELINE: starting pipelining");
            _pipelineStartTime = System.currentTimeMillis();
            if (SystemConfiguration.PIPELINE_STATS) System.out.println("plot " + (System.currentTimeMillis() - _pipelineStartTime) + " inOrder: " + inOrderSegments.size() + " outOfOrder: " + outOfOrderSegments.size() + " interests: " + _sentInterests.size() + " holes: " + _holes + " received: " + _totalReceived + " [" + _baseName + "].1" + " toProcess " + incoming.size() + " avgResponseTime " + avgResponseTime);
            long segmentToGet = -1;
            Interest interest = null;
            if (_basePipelineName == null) {
                _basePipelineName = _baseName.clone();
            }
            Log.info(Log.FAC_PIPELINE, "PIPELINE: BaseName for pipeline: {0} base name: {1}", _basePipelineName, _baseName);
            if (_currentSegment != null) {
                Log.info(Log.FAC_PIPELINE, "PIPELINE: we already have the first segment...  start from there: {0}", _currentSegment.name());
                if (SegmentationProfile.isLastSegment(_currentSegment)) {
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: we already have the last segment...  don't need to pipeline (returning)");
                    return;
                } else {
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: this isn't the last segment...  need to start up pipelining");
                }
            } else {
                Log.info(Log.FAC_PIPELINE, "PIPELINE: need to get the first segment: startingSegmentNumber={0}", _startingSegmentNumber);
            }
            segmentToGet = nextSegmentNumber();
            _nextPipelineSegment = segmentToGet;
            if (this instanceof CCNVersionedInputStream && !VersioningProfile.hasTerminalVersion(_basePipelineName)) {
                Log.info(Log.FAC_PIPELINE, "this is a versioned stream without a terminal version, skip sending non-versioned interest");
            } else {
                Log.info(Log.FAC_PIPELINE, "this is not a versioned stream or it is a versioned stream without the version set in the base name, go ahead and get the first segment");
                interest = SegmentationProfile.segmentInterest(_basePipelineName, segmentToGet, _publisher);
                try {
                    interest.userTime = System.currentTimeMillis();
                    _handle.expressInterest(interest, this);
                    _sentInterests.add(interest);
                    _lastRequestedPipelineSegment = segmentToGet;
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: expressed interest for segment {0} in startPipeline(): {1}", segmentToGet, interest);
                } catch (IOException e) {
                    Log.warning(Log.FAC_PIPELINE, "Failed to express interest for pipelining segments in CCNAbstractInputStream:  Interest = {0}", interest.name());
                }
            }
        }
    }

    private void receivePipelineContent(ContentObject co) {
        long returnedSegment = SegmentationProfile.getSegmentNumber(co.name());
        ArrayList<Interest> toRemove = new ArrayList<Interest>();
        long rr;
        synchronized (readerReadyObj) {
            rr = readerReadyVal;
        }
        if (rr > -1) {
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: there is a reader waiting, we should wait unless we have their segment");
            if (returnedSegment == rr) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we are working on their segment...  we should finish!");
            } else {
                if (haveSegmentBuffered(rr)) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we are deferring until they are done");
                    try {
                        processingDefer = 1;
                        inOrderSegments.wait();
                        synchronized (readerReadyObj) {
                            rr = readerReadyVal;
                        }
                    } catch (InterruptedException e) {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we can go back to processing");
                    }
                    processingDefer = 0;
                } else {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we don't have their segment, keep processing this one.");
                }
            }
        }
        synchronized (inOrderSegments) {
            if (SegmentationProfile.isLastSegment(co)) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we just got the last segment...");
                _lastSegmentNumber = returnedSegment;
                _lastRequestedPipelineSegment = returnedSegment;
            }
            long segNum;
            for (Interest i : _sentInterests) {
                segNum = SegmentationProfile.getSegmentNumber(i.name());
                if (segNum == returnedSegment || (_lastSegmentNumber > -1 && segNum > _lastSegmentNumber)) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                        Log.info(Log.FAC_PIPELINE, "PIPELINE: cancelling interest for segment " + SegmentationProfile.getSegmentNumber(i.name()) + " Interest: " + i);
                    }
                    _handle.cancelInterest(i, this);
                    toRemove.add(i);
                }
            }
            _sentInterests.removeAll(toRemove);
            toRemove.clear();
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: received pipeline segment: {0}", co.name());
            if (returnedSegment == _nextPipelineSegment) {
                _totalReceived++;
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we got the segment ({0}) we were expecting!", returnedSegment);
                if (waitingSegment != -1) if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: someone is waiting for segment: {0}", waitingSegment);
                inOrderSegments.add(co);
                _lastInOrderSegment = returnedSegment;
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: before checking ooos:");
                    printSegments();
                }
                if (outOfOrderSegments.size() > 0) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we have out of order segments to check");
                    while (outOfOrderSegments.size() > 0) {
                        if (SegmentationProfile.getSegmentNumber(outOfOrderSegments.get(0).name()) == nextInOrderSegmentNeeded()) {
                            _lastInOrderSegment = SegmentationProfile.getSegmentNumber(outOfOrderSegments.get(0).name());
                            inOrderSegments.add(outOfOrderSegments.remove(0));
                        } else {
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                                Log.info(Log.FAC_PIPELINE, "PIPELINE: we have " + SegmentationProfile.getSegmentNumber(outOfOrderSegments.get(0).name()) + " but need " + nextInOrderSegmentNeeded() + " breaking from loop, we don't have the one we need");
                            }
                            break;
                        }
                    }
                }
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: after checking ooos: ");
                    printSegments();
                }
            } else {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we got segment {0} an Out of Order segment...  we were expecting segment {1}", returnedSegment, _nextPipelineSegment);
                if (_nextPipelineSegment > returnedSegment) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: this is a previous segment...  drop");
                } else {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: this is a pipeline segment, add to outOfOrderSegment queue");
                    _totalReceived++;
                    _holes++;
                    int i = 0;
                    for (ContentObject c : outOfOrderSegments) {
                        if (returnedSegment < SegmentationProfile.getSegmentNumber(c.name())) break;
                        i++;
                    }
                    outOfOrderSegments.add(i, co);
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we got a segment out of order, need to fill a hole at " + nextInOrderSegmentNeeded());
                    attemptHoleFilling(_nextPipelineSegment);
                }
            }
            _nextPipelineSegment = nextInOrderSegmentNeeded();
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: the next segment needed is {0}", _nextPipelineSegment);
            synchronized (incoming) {
                processingSegment = -1;
            }
            if (waitingThread != null && returnedSegment == waitingSegment) {
                inOrderSegments.notifyAll();
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: notifyAll: min sleep {0}", (System.currentTimeMillis() - waitSleep));
                try {
                    inOrderSegments.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void advancePipeline() {
        synchronized (inOrderSegments) {
            boolean doneAdvancing = false;
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                String s = "have interests out for segments: [";
                for (Interest i : _sentInterests) s = s + " " + SegmentationProfile.getSegmentNumber(i.name());
                s = s + " ]";
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: " + s);
                for (Interest i : _sentInterests) if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: {0}", i.name());
            }
            Interest i = null;
            while (_sentInterests.size() + inOrderSegments.size() + outOfOrderSegments.size() + processingDefer < SystemConfiguration.PIPELINE_SIZE && !doneAdvancing) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: _sentInterests.size() = {0} inOrderSegments.size() = {1} outOfOrderSegments.size()  = {2} processingDefer = {3} total = {4}", _sentInterests.size(), inOrderSegments.size(), outOfOrderSegments.size(), processingDefer, (_sentInterests.size() + inOrderSegments.size() + outOfOrderSegments.size() + processingDefer));
                i = null;
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: _lastSegmentNumber = {0}", _lastSegmentNumber);
                if (_lastRequestedPipelineSegment == SegmentationProfile.baseSegment()) {
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: the last segment number is the base segment, need to make sure we have received the base segment before we press on");
                    if (_lastInOrderSegment == -1) {
                        Log.info(Log.FAC_PIPELINE, "PIPELINE: _lastInOrderSegment == -1, we have not received the base segment, do not advance the pipeline");
                        return;
                    } else {
                        Log.info(Log.FAC_PIPELINE, "PIPELINE: _lastInOrderSegment == {0}, we have received the base segment, we can advance the pipeline!", _lastInOrderSegment);
                    }
                }
                if (_lastSegmentNumber == -1 || _lastRequestedPipelineSegment < _lastSegmentNumber) {
                    i = SegmentationProfile.segmentInterest(_basePipelineName, _lastRequestedPipelineSegment + 1, _publisher);
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: in advancePipeline _lastRequestedPipelineSegment {0}", _lastRequestedPipelineSegment);
                    try {
                        i.userTime = System.currentTimeMillis();
                        _handle.expressInterest(i, this);
                        _sentInterests.add(i);
                        _lastRequestedPipelineSegment++;
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: requested segment " + _lastRequestedPipelineSegment + " (" + (SystemConfiguration.PIPELINE_SIZE - _sentInterests.size()) + " tokens)");
                    } catch (IOException e) {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.WARNING)) Log.warning(Log.FAC_PIPELINE, "failed to express interest for CCNAbstractInputStream pipeline: {0}");
                        break;
                    }
                } else {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: setting doneAdvancing to true");
                    doneAdvancing = true;
                }
            }
        }
    }

    private void attemptHoleFilling() {
        synchronized (inOrderSegments) {
            if (outOfOrderSegments.size() > 0) {
                long firstOOO = SegmentationProfile.getSegmentNumber(outOfOrderSegments.get(0).name());
                long holeCheck = _nextPipelineSegment;
                while (holeCheck < firstOOO) {
                    attemptHoleFilling(holeCheck);
                    holeCheck++;
                }
            }
        }
    }

    private void attemptHoleFilling(long hole) {
        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: checking for a hole at segment: {0}", hole);
        synchronized (incoming) {
            for (IncomingSegment i : incoming) if (i.segmentNumber == hole) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: segment {0} is already here, just needs to be processed", hole);
                return;
            }
            if (processingSegment != -1 && hole == processingSegment) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: the segment is being processed... not a hole.");
                return;
            }
        }
        Interest i = SegmentationProfile.segmentInterest(_basePipelineName, hole, _publisher);
        boolean notRequested = true;
        int index = -1;
        int index2 = -1;
        long elapsed1 = -1;
        long elapsed2 = -1;
        long newUserTime = -1;
        Interest expressed;
        try {
            synchronized (inOrderSegments) {
                index = _sentInterests.indexOf(i);
                if (index > -1) {
                    expressed = _sentInterests.get(index);
                    elapsed1 = System.currentTimeMillis() - expressed.userTime;
                    newUserTime = expressed.userTime;
                    if (elapsed1 == -1) {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: base segment is there, but the express time is -1, it must be getting processed");
                        return;
                    }
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: base interest is already there, try adding excludes elapsed time = {0} interest: {1}", elapsed1, expressed);
                    notRequested = false;
                } else {
                }
                int attempt = 1;
                Exclude ex = new Exclude();
                ex.add(new byte[][] { SegmentationProfile.getSegmentNumberNameComponent(hole + attempt) });
                i.exclude(ex);
                Interest toDelete = null;
                index2 = 0;
                long excludedSegment = -1;
                ExcludeComponent ec = null;
                long tempseg = -1;
                for (Interest expInt : _sentInterests) {
                    tempseg = SegmentationProfile.getSegmentNumber(expInt.name());
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: checking if this interest {0} is for our hole at {1}", tempseg, hole);
                    if (tempseg == hole) {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: this is a match!  does it have excludes?");
                        notRequested = false;
                        if (expInt.exclude() != null) {
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: yep! it is a holefilling attempt");
                            ec = (ExcludeComponent) expInt.exclude().value(0);
                            excludedSegment = SegmentationProfile.getSegmentNumber(ec.getBytes());
                            attempt = (int) (excludedSegment - hole);
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: this is attempt: {0}", attempt);
                            if (attempt < SystemConfiguration.PIPELINE_SEGMENTATTEMPTS) {
                                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we have more attempts that we can try... ");
                                toDelete = expInt;
                                ex = new Exclude();
                                ex.add(new byte[][] { SegmentationProfile.getSegmentNumberNameComponent(hole + attempt + 1) });
                                i.exclude(ex);
                                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: going to express the next attempt: {0}", i);
                                newUserTime = expInt.userTime;
                            } else {
                                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we have tried as many times as we can...  break here");
                                return;
                            }
                        } else {
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: this isn't a holefilling attempt, must be the base interest");
                        }
                        break;
                    } else {
                        if (tempseg > hole) break;
                    }
                    index2++;
                }
                if (toDelete != null) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we can try again to fill the hole!");
                    expressed = toDelete;
                    if (expressed.userTime == -1) {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: hole filling segment is there, but the express time is -1, it must be getting processed");
                        return;
                    } else {
                        elapsed2 = System.currentTimeMillis() - expressed.userTime;
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: elapsed2 time {0}", elapsed2);
                        if (elapsed2 > avgResponseTime * SystemConfiguration.PIPELINE_RTTFACTOR && avgResponseTime > -1) {
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: expressing the next interest! {0} old express time {1} new express time {2}", i, expressed.userTime, newUserTime);
                            i.userTime = newUserTime;
                            _handle.expressInterest(i, this);
                            _sentInterests.add(index2, i);
                            _handle.cancelInterest(toDelete, this);
                            _sentInterests.remove(toDelete);
                            adjustAvgResponseTimeForHole();
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                                Log.info(Log.FAC_PIPELINE, "PIPELINE: expressed: {0} deleted: {1}", i, toDelete);
                                Log.info(Log.FAC_PIPELINE, "PIPELINE: current expressed interests: ");
                                for (Interest p : _sentInterests) Log.info(Log.FAC_PIPELINE, "PIPELINE: {0}", p);
                            }
                            return;
                        } else {
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: need to give the earlier attempt a chance to work");
                            return;
                        }
                    }
                } else {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we don't have any holefilling attempts... for {0}", hole);
                    if (index == -1) {
                        i.exclude(null);
                    }
                }
                if ((elapsed1 > avgResponseTime * 2 && avgResponseTime > -1) || (avgResponseTime == -1 && elapsed1 > SystemConfiguration.INTEREST_REEXPRESSION_DEFAULT) || notRequested) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                        if (notRequested) Log.info(Log.FAC_PIPELINE, "PIPELINE: we don't have any holefilling interests pending for {0}, need to express one.  might need to reset pipeline state", hole);
                        if (i.exclude() == null) Log.info(Log.FAC_PIPELINE, "PIPELINE: adding the base interest or the first holefilling attempt!!! {0}", i); else {
                            Log.info(Log.FAC_PIPELINE, "PIPELINE: adding the first holefilling attempt! {0}", i);
                        }
                    }
                    if (newUserTime == -1) newUserTime = System.currentTimeMillis();
                    i.userTime = newUserTime;
                    _handle.expressInterest(i, this);
                    if (index != -1) _sentInterests.add(index, i); else _sentInterests.add(i);
                    if (index != -1) {
                        _handle.cancelInterest(_sentInterests.remove(index + 1), this);
                        adjustAvgResponseTimeForHole();
                    }
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: requested segment {0} to fill hole: {1} with Interest: {2}", hole, i.name(), i);
                    return;
                } else {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we need to wait longer to see if the original interest will return the segment: avgResponseTime: {0}", avgResponseTime);
                }
            }
        } catch (IOException e) {
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.WARNING)) Log.warning(Log.FAC_PIPELINE, "failed to express interest for CCNAbstractInputStream pipeline");
        }
    }

    private void adjustAvgResponseTimeForHole() {
        synchronized (incoming) {
            Log.info(Log.FAC_PIPELINE, "PIPELINE: before adjusting avgResponseTime for hole. avgResponseTime = {0}", avgResponseTime);
            avgResponseTime = 0.9 * avgResponseTime + 0.1 * (SystemConfiguration.PIPELINE_RTTFACTOR * avgResponseTime);
            Log.info(Log.FAC_PIPELINE, "PIPELINE: after adjusting avgResponseTime for hole. avgResponseTime = {0}", avgResponseTime);
        }
    }

    private void printSegments() {
        String s = "inOrder: [";
        for (ContentObject c : inOrderSegments) s += " " + SegmentationProfile.getSegmentNumber(c.name());
        s += " ] outOrder: [";
        for (ContentObject c : outOfOrderSegments) s += " " + SegmentationProfile.getSegmentNumber(c.name());
        s += "]";
        Log.info(Log.FAC_PIPELINE, "PIPELINE: " + s);
    }

    private long nextInOrderSegmentNeeded() {
        synchronized (inOrderSegments) {
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                Log.info(Log.FAC_PIPELINE, "PIPELINE: lastInOrderSegment number {0} _startingSegmentNumber {1}", _lastInOrderSegment, _startingSegmentNumber);
                if (outOfOrderSegments.size() > 0) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                        Log.info(Log.FAC_PIPELINE, "PIPELINE: we have out of order segments...");
                        printSegments();
                    }
                }
            }
            if (_lastInOrderSegment != -1) return _lastInOrderSegment + 1; else return _startingSegmentNumber;
        }
    }

    private boolean haveSegmentBuffered(long segmentNumber) {
        synchronized (inOrderSegments) {
            ContentObject co = null;
            for (int i = 0; i < inOrderSegments.size(); i++) {
                co = inOrderSegments.get(i);
                if (SegmentationProfile.getSegmentNumber(co.name()) == segmentNumber) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: have segment {0} in iOS, return true.", segmentNumber);
                    return true;
                }
            }
            for (int i = 0; i < outOfOrderSegments.size(); i++) {
                co = outOfOrderSegments.get(i);
                if (SegmentationProfile.getSegmentNumber(co.name()) == segmentNumber) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: segment {0} is in our oOOS queue, return true", segmentNumber);
                    return true;
                } else {
                    if (SegmentationProfile.getSegmentNumber(co.name()) > segmentNumber) {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: our out of order segments are past the requested segment...  we have a hole - not attempting to fill it at this time");
                        break;
                    }
                }
            }
            return false;
        }
    }

    private ContentObject getPipelineSegment(long segmentNumber) throws IOException {
        synchronized (inOrderSegments) {
            ContentObject co = null;
            while (inOrderSegments.size() > 0) {
                co = inOrderSegments.remove(0);
                if (SegmentationProfile.getSegmentNumber(co.name()) == segmentNumber) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: had segment {0} in iOS, setting current.", segmentNumber);
                    _currentSegment = co;
                    if (inOrderSegments.size() > 0 || segmentNumber == 1) advancePipeline(); else advancePipeline();
                    return co;
                }
            }
            while (outOfOrderSegments.size() > 0) {
                co = outOfOrderSegments.get(0);
                if (SegmentationProfile.getSegmentNumber(co.name()) == segmentNumber) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: segment {0} was in our oOOS queue", segmentNumber);
                    outOfOrderSegments.remove(0);
                    _currentSegment = co;
                    return co;
                } else {
                    if (SegmentationProfile.getSegmentNumber(co.name()) > segmentNumber) {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: our out of order segments are past the requested segment...  we have a hole");
                        break;
                    } else {
                        outOfOrderSegments.remove(0);
                    }
                }
            }
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                Log.info(Log.FAC_PIPELINE, "PIPELINE: we do not have the segment yet...  was it requested?");
                Log.info(Log.FAC_PIPELINE, "PIPELINE: need segment: {0} _lastRequestedPipelineSegment: {1}", segmentNumber, _lastRequestedPipelineSegment);
                String s = "current interests out for segments: [";
                for (Interest i : _sentInterests) s += " " + SegmentationProfile.getSegmentNumber(i.name());
                s += "]";
                Log.info(Log.FAC_PIPELINE, "PIPELINE: " + s);
            }
            if (requestedSegment(segmentNumber)) {
                attemptHoleFilling(segmentNumber);
            } else {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: detected a pipeline jump!!!!");
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: pipeline jump adjustment conditions");
                    printSegments();
                    String s = "pre-pipeline jump interests: [";
                    for (Interest i : _sentInterests) s += " " + SegmentationProfile.getSegmentNumber(i.name());
                    s += "]";
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: " + s);
                }
                Interest interest = SegmentationProfile.segmentInterest(_basePipelineName, segmentNumber, _publisher);
                try {
                    interest.userTime = System.currentTimeMillis();
                    _handle.expressInterest(interest, this);
                    ArrayList<Object> toRemove = new ArrayList<Object>();
                    long maxExpress = segmentNumber + SystemConfiguration.PIPELINE_SIZE - 1;
                    long lastExpressed = segmentNumber;
                    long segNum;
                    for (Interest i : _sentInterests) {
                        segNum = SegmentationProfile.getSegmentNumber(i.name());
                        if (segNum > maxExpress) {
                            _handle.cancelInterest(i, this);
                            toRemove.add(i);
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: canceling interest: {0}", i);
                        } else {
                            lastExpressed = segNum;
                        }
                    }
                    _sentInterests.removeAll(toRemove);
                    _sentInterests.add(0, interest);
                    _nextPipelineSegment = segmentNumber;
                    for (ContentObject segment : inOrderSegments) {
                        segNum = SegmentationProfile.getSegmentNumber(segment.name());
                        if (segNum > maxExpress) {
                            toRemove.add(segment);
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: removing segment past buffer limit after pipeline jump in inOrderSegments: {0}", segment);
                        } else if (segNum < segmentNumber) {
                            toRemove.add(segment);
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: removing segment before requested segment pipeline jump in inOrderSegments: {0}", segment);
                        } else {
                            if (lastExpressed < segNum) {
                                lastExpressed = segNum;
                            }
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: pipeline jump - will move segment in inOrderSegments: {0} to outOfOrderSegments", segment);
                        }
                    }
                    inOrderSegments.removeAll(toRemove);
                    toRemove.clear();
                    for (ContentObject segment : outOfOrderSegments) {
                        segNum = SegmentationProfile.getSegmentNumber(segment.name());
                        if (segNum > maxExpress) {
                            toRemove.add(segment);
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: removing segment past buffer limit after pipeline jump in outOfOrderSegments: {0}", segment);
                        } else if (segNum < segmentNumber) {
                            toRemove.add(segment);
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: removing segment before requested segment pipeline jump in outOfOrderSegments: {0}", segment);
                        } else if (lastExpressed < segNum) {
                            lastExpressed = segNum;
                        }
                    }
                    outOfOrderSegments.removeAll(toRemove);
                    long segNum2;
                    if (outOfOrderSegments.size() == 0) {
                        outOfOrderSegments.addAll(inOrderSegments);
                        Log.info(Log.FAC_PIPELINE, "PIPELINE: pipeline jump - moving segments from inOrderSegments to outOfOrderSegments");
                    } else {
                        ContentObject segX;
                        for (int x = 0; x < inOrderSegments.size(); x++) {
                            segX = (ContentObject) inOrderSegments.get(x);
                            segNum = SegmentationProfile.getSegmentNumber(segX.name());
                            for (int y = 0; y < inOrderSegments.size(); y++) {
                                segNum2 = SegmentationProfile.getSegmentNumber(((ContentObject) outOfOrderSegments.get(y)).name());
                                if (segNum < segNum2) {
                                    Log.info(Log.FAC_PIPELINE, "PIPELINE: pipeline jump - moving segment {0} to position {1} in outOfOrderSegments", segNum, y);
                                    outOfOrderSegments.add(y, segX);
                                }
                            }
                            Log.info(Log.FAC_PIPELINE, "PIPELINE: pipeline jump - check buffered segments");
                            printSegments();
                        }
                    }
                    inOrderSegments.clear();
                    _lastRequestedPipelineSegment = lastExpressed;
                    _lastInOrderSegment = segmentNumber - 1;
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: pipeline jump - we have now reset the state...  now we can try asking for the next segments - advance pipeline with holefilling");
                    advancePipeline();
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                        Log.info(Log.FAC_PIPELINE, "PIPELINE: we hadn't asked for segment {0} asking now... {1}", segmentNumber, interest);
                        Log.info(Log.FAC_PIPELINE, "PIPELINE: pipeline jump _lastRequestedPipelineSegment {0} _lastInOrderSegment {1}", _lastRequestedPipelineSegment, _lastInOrderSegment);
                        Log.info(Log.FAC_PIPELINE, "PIPELINE: pipeline jump adjustment results");
                        printSegments();
                        String s = "post-pipeline jump interests: [";
                        for (Interest i : _sentInterests) s += " " + SegmentationProfile.getSegmentNumber(i.name());
                        s += "]";
                        Log.info(Log.FAC_PIPELINE, "PIPELINE: " + s);
                    }
                } catch (IOException e) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.WARNING)) Log.warning(Log.FAC_PIPELINE, "failed to express interest for CCNAbstractInputStream pipeline: {0}", e.getMessage());
                }
            }
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                String s = "have interests out for segments: [";
                for (Interest i : _sentInterests) s += " " + SegmentationProfile.getSegmentNumber(i.name());
                s += "]";
                Log.info(Log.FAC_PIPELINE, "PIPELINE: " + s);
            }
        }
        return null;
    }

    private void cancelInterests() {
        synchronized (inOrderSegments) {
            for (Interest i : _sentInterests) {
                _handle.cancelInterest(i, this);
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: canceling interest: {0}", i);
            }
            _sentInterests.clear();
        }
    }

    private void resetPipelineState() {
        synchronized (inOrderSegments) {
            inOrderSegments.clear();
            outOfOrderSegments.clear();
            _nextPipelineSegment = -1;
            _lastRequestedPipelineSegment = -1;
            _lastInOrderSegment = -1;
            _lastSegmentNumber = -1;
            _currentSegment = null;
        }
    }

    private boolean requestedSegment(long number) {
        synchronized (incoming) {
            for (IncomingSegment i : incoming) if (i.segmentNumber == number) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we already asked for it and it is just waiting to be processed");
                return true;
            }
            if (processingSegment != -1 && processingSegment == number) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: someone is processing it right now!");
                return true;
            }
        }
        synchronized (inOrderSegments) {
            for (Interest i : _sentInterests) if (SegmentationProfile.getSegmentNumber(i.name()) == number) return true;
            return false;
        }
    }

    private void setPipelineName(ContentName n) {
        _basePipelineName = n.clone();
        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: setting _basePipelineName {0}", _basePipelineName);
        synchronized (inOrderSegments) {
            ArrayList<Interest> remove = new ArrayList<Interest>();
            for (Interest i : _sentInterests) {
                if (SegmentationProfile.segmentRoot(i.name()).equals(_basePipelineName)) {
                } else {
                    remove.add(i);
                }
            }
            for (Interest i : remove) {
                _handle.cancelInterest(i, this);
                _sentInterests.remove(i);
            }
        }
    }

    public boolean readerReadyCheck(long nextSegment) {
        synchronized (inOrderSegments) {
            long rr;
            synchronized (readerReadyObj) {
                rr = readerReadyVal;
            }
            if (rr > -1) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: there is a reader waiting, we should wait unless we have their segment");
                if (nextSegment == rr) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we are working on their segment...  we should finish!");
                    return false;
                } else {
                    if (haveSegmentBuffered(rr)) {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we are deferring until they are done");
                        return true;
                    } else {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we don't have their segment, keep processing this one.");
                        return false;
                    }
                }
            }
            return false;
        }
    }

    public Interest handleContent(ContentObject result, Interest interest) {
        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: in handleContent for {0} at {1}", result.name(), System.currentTimeMillis());
        long starttime = System.currentTimeMillis();
        IncomingSegment is;
        synchronized (incoming) {
            Log.info(Log.FAC_PIPELINE, "PIPELINE: before adjusting avgResponseTime after reception. avgResponseTime = {0} elapsed time {1}", avgResponseTime, (starttime - interest.userTime));
            if (avgResponseTime == -1) {
                avgResponseTime = starttime - interest.userTime;
            } else {
                long newResponseTime = starttime - interest.userTime;
                if (newResponseTime < 100 * avgResponseTime) avgResponseTime = 0.9 * avgResponseTime + 0.1 * newResponseTime; else {
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: did not use response time for calculation...  would have been incorrect.  will be fixed in a new branch for 100478 {0}", newResponseTime);
                }
            }
            Log.info(Log.FAC_PIPELINE, "PIPELINE: after adjusting avgResponseTime after reception. avgResponseTime = {0}", avgResponseTime);
            interest.userTime = -1;
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: in handleContent after reading {0} avgResponseTime {1}", result.name(), avgResponseTime);
            is = new IncomingSegment(result, interest);
            int index = 0;
            for (IncomingSegment i : incoming) {
                if (i.segmentNumber == is.segmentNumber) {
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: attempting to deliver segment {0}, but it is already here to be processed, returning", is.segmentNumber);
                    return null;
                }
                if (i.segmentNumber > is.segmentNumber) break;
                index++;
            }
            incoming.add(index, is);
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                String s = "segments to process: [";
                for (IncomingSegment i : incoming) s += " " + i.segmentNumber;
                s += " ]";
                Log.info(Log.FAC_PIPELINE, "PIPELINE: " + s);
            }
            if (processor == null) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: processor was null, setting it to me.");
                processor = Thread.currentThread();
                is = incoming.remove(0);
                processingSegment = SegmentationProfile.getSegmentNumber(is.content.name());
            } else {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: processor not null, returning");
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: {0} done with handleContent after reading {1}", (System.currentTimeMillis() - starttime), result.name());
                }
                return null;
            }
        }
        synchronized (inOrderSegments) {
            while (is != null) {
                if (SystemConfiguration.PIPELINE_STATS) System.out.println("plot " + (System.currentTimeMillis() - _pipelineStartTime) + " inOrder: " + inOrderSegments.size() + " outOfOrder: " + outOfOrderSegments.size() + " interests: " + _sentInterests.size() + " holes: " + _holes + " received: " + _totalReceived + " [" + _baseName + "].2" + " toProcess " + incoming.size() + " avgResponseTime " + avgResponseTime);
                if (_sentInterests.remove(is.interest)) {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we were expecting this data! we had outstanding interests: {0}", is.interest);
                } else {
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we must have canceled the interest, dropping ContentObject(s).  old interest: {0}", is.interest);
                    Interest checkInterest;
                    is.interest = null;
                    for (int i = 0; i < _sentInterests.size(); i++) {
                        checkInterest = _sentInterests.get(i);
                        if (checkInterest.matches(is.content)) {
                            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: the incoming packet's interest is gone, but it matches another interest, using that");
                            is.interest = checkInterest;
                            break;
                        }
                    }
                    if (is.interest == null) {
                        is = null;
                        synchronized (incoming) {
                            processingSegment = -1;
                        }
                    }
                }
                if (is != null) {
                    if (_handle.defaultVerifier().verify(is.content)) {
                        receivePipelineContent(is.content);
                    } else {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.WARNING)) Log.warning(Log.FAC_PIPELINE, "Dropping content object due to failed verification: {0} Need to add interest re-expression with exclude", is.content.name());
                        _sentInterests.remove(is.interest);
                    }
                }
                synchronized (incoming) {
                    if (incoming.size() == 0) {
                        processor = null;
                        is = null;
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: that was the last one, resetting processor to null");
                    } else {
                        is = incoming.remove(0);
                        processingSegment = SegmentationProfile.getSegmentNumber(is.content.name());
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: processing first segment in incoming arraylist, segment {0}", is.segmentNumber);
                    }
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) {
                        String s = "segments to process: [";
                        for (IncomingSegment i : incoming) s += " " + i.segmentNumber;
                        s += " ]";
                        Log.info(Log.FAC_PIPELINE, "PIPELINE: " + s);
                    }
                }
                advancePipeline();
            }
        }
        attemptHoleFilling();
        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: {0} done with handleContent after reading {1}", (System.currentTimeMillis() - starttime), result.name());
        return null;
    }

    /**
	 * Set the timeout that will be used for all content retrievals on this stream.
	 * Default is 5 seconds.
	 * @param timeout Milliseconds
	 */
    public void setTimeout(int timeout) {
        _timeout = timeout;
    }

    /**
	 * Add flags to this stream. Adds to existing flags.
	 */
    public void addFlags(EnumSet<FlagTypes> additionalFlags) {
        _flags.addAll(additionalFlags);
    }

    /**
	 * Add a flag to this stream. Adds to existing flags.
	 */
    public void addFlag(FlagTypes additionalFlag) {
        _flags.add(additionalFlag);
    }

    /**
	 * Set flags on this stream. Replaces existing flags.
	 */
    public void setFlags(EnumSet<FlagTypes> flags) {
        if (null == flags) {
            _flags.clear();
        } else {
            _flags = flags;
        }
    }

    /**
	 * Clear the flags on this stream.
	 */
    public void clearFlags() {
        _flags.clear();
    }

    /**
	 * Remove a flag from this stream.
	 */
    public void removeFlag(FlagTypes flag) {
        _flags.remove(flag);
    }

    /**
	 * Check whether this stream has a particular flag set.
	 */
    public boolean hasFlag(FlagTypes flag) {
        return _flags.contains(flag);
    }

    /**
	 * @return The name used to retrieve segments of this stream (not including the segment number).
	 */
    public ContentName getBaseName() {
        return _baseName;
    }

    /**
	 * @return The version of the stream being read, if its name is versioned.
	 */
    public CCNTime getVersion() {
        if (null == _baseName) return null;
        return VersioningProfile.getTerminalVersionAsTimestampIfVersioned(_baseName);
    }

    /**
	 * Returns the digest of the first segment of this stream. 
	 * Together with firstSegmentNumber() and getBaseName() this method may be used to
	 * identify the stream content unambiguously.
	 * 
	 * @return The digest of the first segment of this stream
	 * @throws NoMatchingContentException if no content available
	 * @throws IOException on communication error
	 */
    public byte[] getFirstDigest() throws NoMatchingContentFoundException, IOException {
        if (null == _firstSegment) {
            ContentObject firstSegment = getFirstSegment();
            setFirstSegment(firstSegment);
        }
        return _firstSegment.digest();
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        if (read(b, 0, 1) < 0) {
            return -1;
        }
        return (0x000000FF & b[0]);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
        if (null == buf) throw new NullPointerException("Buffer cannot be null!");
        return readInternal(buf, offset, len);
    }

    /**
	 * Actual mechanism used to trigger segment retrieval and perform content reads. 
	 * Subclasses define different schemes for retrieving content across segments.
	 * @param buf As in read(byte[], int, int).
	 * @param offset As in read(byte[], int, int).
	 * @param len As in read(byte[], int, int).
	 * @return As in read(byte[], int, int).
	 * @throws IOException if a segment cannot be retrieved, or there is an error in lower-level
	 * 		segment retrieval mechanisms. Uses subclasses of IOException to help provide
	 * 		more information. In particular, throws NoMatchingContentFoundException when
	 * 		no content found within the timeout given.
	 */
    protected abstract int readInternal(byte[] buf, int offset, int len) throws IOException;

    /**
	 * Called to set the first segment when opening a stream. This does initialization
	 * and setup particular to the first segment of a stream. Subclasses should not override
	 * unless they really know what they are doing. Calls #setCurrentSegment(ContentObject)
	 * for the first segment. If the content is encrypted, and keys are not provided
	 * for this stream, they are looked up according to the namespace. Note that this
	 * assumes that all segments of a given piece of content are either encrypted or not.
	 * @param newSegment Must not be null
	 * @throws IOException If newSegment is null or decryption keys set up incorrectly
	 */
    protected void setFirstSegment(ContentObject newSegment) throws IOException {
        if (null == newSegment) {
            throw new NoMatchingContentFoundException("Cannot find first segment of " + getBaseName());
        }
        LinkObject theLink = null;
        while (newSegment.isType(ContentType.LINK) && (!hasFlag(FlagTypes.DONT_DEREFERENCE))) {
            theLink = new LinkObject(newSegment, _handle);
            pushDereferencedLink(theLink);
            newSegment = _dereferencedLink.dereference(_timeout);
            if (Log.isLoggable(Log.FAC_IO, Level.INFO)) {
                Log.info(Log.FAC_IO, "CCNAbstractInputStream: dereferencing link {0} to {1}, resulting data {2}", theLink.getVersionedName(), theLink.link(), ((null == newSegment) ? "null" : newSegment.name()));
                Log.info(Log.FAC_SIGNING, "CCNAbstractInputStream: dereferencing link {0} to {1}, resulting data {2}", theLink.getVersionedName(), theLink.link(), ((null == newSegment) ? "null" : newSegment.name()));
            }
            if (newSegment == null) {
                if (_dereferencedLink.hasError()) {
                    if (_dereferencedLink.getError() instanceof LinkCycleException) {
                        if (Log.isLoggable(Log.FAC_IO, Level.WARNING)) {
                            Log.warning(Log.FAC_IO, "Hit link cycle on link {0} pointing to {1}, cannot dereference. See this.dereferencedLink() for more information!", _dereferencedLink.getVersionedName(), _dereferencedLink.link().targetName());
                        }
                    }
                    throw _dereferencedLink.getError();
                } else {
                    throw new NoMatchingContentFoundException("Cannot find first segment of " + getBaseName() + ", which is a link pointing to " + _dereferencedLink.link().targetName());
                }
            }
            _baseName = SegmentationProfile.segmentRoot(newSegment.name());
        }
        _firstSegment = newSegment;
        if (newSegment.isType(ContentType.GONE)) {
            if (Log.isLoggable(Log.FAC_IO, Level.INFO)) Log.info(Log.FAC_IO, "setFirstSegment: got gone segment: {0}", newSegment.name());
        } else if (newSegment.isType(ContentType.ENCR) && (null == _keys)) {
            ContentName contentName = SegmentationProfile.segmentRoot(newSegment.name());
            _keys = AccessControlManager.keysForInput(contentName, newSegment.signedInfo().getPublisherKeyID(), _handle);
            if (_keys == null) throw new AccessDeniedException("Cannot find keys to decrypt content.");
        }
        setCurrentSegment(newSegment);
    }

    /**
	 * Set up current segment for reading, including preparation for decryption if necessary.
	 * Called after getSegment/getFirstSegment/getNextSegment, which take care of verifying
	 * the segment for us. Assumes newSegment has been verified.
	 * @throws IOException If decryption keys set up incorrectly
	 */
    protected void setCurrentSegment(ContentObject newSegment) throws IOException {
        _currentSegment = null;
        _segmentReadStream = null;
        if (null == newSegment) {
            if (Log.isLoggable(Log.FAC_IO, Level.INFO)) Log.info(Log.FAC_IO, "FINDME: Setting current segment to null! Did a segment fail to verify?");
            return;
        }
        _currentSegment = newSegment;
        _publisher = newSegment.signedInfo().getPublisherKeyID();
        if (deletionInformation() != newSegment) {
            if (_keys != null) {
                try {
                    if (Log.isLoggable(Log.FAC_IO, Level.FINEST)) {
                        if (!SegmentationProfile.segmentRoot(_currentSegment.name()).equals(getBaseName())) {
                            Log.finest(Log.FAC_IO, "ASSERT: getBaseName()={0} does not match segmentless part of _currentSegment.name()={1}", getBaseName(), SegmentationProfile.segmentRoot(_currentSegment.name()));
                        }
                    }
                    _cipher = _keys.getSegmentDecryptionCipher(getBaseName(), _publisher, SegmentationProfile.getSegmentNumber(_currentSegment.name()));
                } catch (InvalidKeyException e) {
                    Log.warning(Log.FAC_IO, "InvalidKeyException: " + e.getMessage());
                    throw new IOException("InvalidKeyException: " + e.getMessage());
                } catch (InvalidAlgorithmParameterException e) {
                    Log.warning(Log.FAC_IO, "InvalidAlgorithmParameterException: " + e.getMessage());
                    throw new IOException("InvalidAlgorithmParameterException: " + e.getMessage());
                }
                byte[] bodyData = _cipher.update(_currentSegment.content());
                byte[] tailData;
                try {
                    tailData = _cipher.doFinal();
                } catch (IllegalBlockSizeException e) {
                    Log.warning(Log.FAC_IO, "IllegalBlockSizeException: " + e.getMessage());
                    throw new IOException("IllegalBlockSizeException: " + e.getMessage());
                } catch (BadPaddingException e) {
                    Log.warning(Log.FAC_IO, "BadPaddingException: " + e.getMessage());
                    throw new IOException("BadPaddingException: " + e.getMessage());
                }
                if ((null == tailData) || (0 == tailData.length)) {
                    _segmentReadStream = new ByteArrayInputStream(bodyData);
                } else if ((null == bodyData) || (0 == bodyData.length)) {
                    _segmentReadStream = new ByteArrayInputStream(tailData);
                } else {
                    byte[] allData = new byte[bodyData.length + tailData.length];
                    System.arraycopy(bodyData, 0, allData, 0, bodyData.length);
                    System.arraycopy(tailData, 0, allData, bodyData.length, tailData.length);
                    _segmentReadStream = new ByteArrayInputStream(allData);
                }
            } else {
                if (_currentSegment.signedInfo().getType().equals(ContentType.ENCR)) {
                    Log.warning(Log.FAC_IO, "Asked to read encrypted content, but not given a key to decrypt it. Decryption happening at higher level?");
                }
                _segmentReadStream = new ByteArrayInputStream(_currentSegment.content());
            }
        }
    }

    /**
	 * Rewinds read buffers for current segment to beginning of the segment.
	 * @throws IOException
	 */
    protected void rewindSegment() throws IOException {
        if (null == _currentSegment) {
            if (Log.isLoggable(Log.FAC_IO, Level.INFO)) Log.info(Log.FAC_IO, "Cannot rewind null segment.");
        }
        if (null == _segmentReadStream) {
            setCurrentSegment(_currentSegment);
        }
        _segmentReadStream.reset();
    }

    /**
	 * Retrieves a specific segment of this stream, indicated by segment number.
	 * Three navigation options: get first (leftmost) segment, get next segment,
	 * or get a specific segment.
	 * Have to assume that everyone is using our segment number encoding. Probably
	 * easier to ask raw streams to use that encoding (e.g. for packet numbers)
	 * than to flag streams as to whether they are using integers or segments.
	 * @param number Segment number to retrieve. See SegmentationProfile for numbering.
	 * 		If we already have this segment as #currentSegmentNumber(), will just
	 * 		return the current segment, and will not re-retrieve it from the network.
	 * @throws IOException If no matching content found (actually throws NoMatchingContentFoundException)
	 *  	or if there is an error at lower layers.
	 **/
    protected ContentObject getSegment(long number) throws IOException {
        long ttgl = System.currentTimeMillis();
        synchronized (readerReadyObj) {
            readerReadyVal = number;
        }
        synchronized (inOrderSegments) {
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: time to get lock in getSegment {0} for segment {1}", (System.currentTimeMillis() - ttgl), number);
            if (_baseName.equals(_basePipelineName)) {
                if (SystemConfiguration.PIPELINE_STATS) System.out.println("plot " + (System.currentTimeMillis() - _pipelineStartTime) + " inOrder: " + inOrderSegments.size() + " outOfOrder: " + outOfOrderSegments.size() + " interests: " + _sentInterests.size() + " holes: " + _holes + " received: " + _totalReceived + " [" + _baseName + "].3" + " toProcess " + incoming.size() + " avgResponseTime " + avgResponseTime);
            } else {
                Log.info(Log.FAC_PIPELINE, "PIPELINE: in getSegment and we detected a name change.  _baseName: {0} _basePipelineName: {1}", _baseName, _basePipelineName);
                setPipelineName(_baseName);
                startPipeline();
            }
            if (_currentSegment != null) {
                Log.info(Log.FAC_PIPELINE, "PIPELINE: getSegment checking if currentSegment ({0}) is the one we want {1}", currentSegmentNumber(), number);
                if (currentSegmentNumber() == number) {
                    Log.info(Log.FAC_PIPELINE, "PIPELINE: currentSegment was the one we wanted, returning it!");
                    return _currentSegment;
                }
            } else {
                Log.info(Log.FAC_PIPELINE, "PIPELINE: getSegment currentSegment was null");
            }
            ContentObject co = getPipelineSegment(number);
            if (co != null) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we had segment {0} already!!", number);
                advancePipeline();
                synchronized (readerReadyObj) {
                    readerReadyVal = -1;
                    inOrderSegments.notifyAll();
                }
                return co;
            } else {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we don't have segment {0} pipelined... blocking", number);
                if (number < _lastInOrderSegment) Log.info(Log.FAC_PIPELINE, "PIPELINE: we do not have segment {0} and the last in order segment was {1}, we must have had a skip/seek");
            }
            synchronized (inOrderSegments) {
                long start = System.currentTimeMillis();
                long sleep = 0;
                long sleepCheck = 0;
                Log.info(Log.FAC_PIPELINE, "PIPELINE: _timeout = {0}", _timeout);
                waitingThread = Thread.currentThread();
                waitingSegment = number;
                while (sleep < _timeout) {
                    try {
                        start = System.currentTimeMillis();
                        waitSleep = start;
                        sleepCheck = _timeout - sleep;
                        if (avgResponseTime > 0 && avgResponseTime < (long) SystemConfiguration.SHORT_TIMEOUT) {
                            if (avgResponseTime > sleepCheck) inOrderSegments.wait(sleepCheck); else inOrderSegments.wait((long) avgResponseTime);
                        } else {
                            if ((long) SystemConfiguration.SHORT_TIMEOUT > sleepCheck) inOrderSegments.wait(sleepCheck); else inOrderSegments.wait((long) SystemConfiguration.SHORT_TIMEOUT);
                        }
                    } catch (InterruptedException e1) {
                        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: awake: interrupted! {0}", sleep);
                    }
                    sleep += System.currentTimeMillis() - start;
                    if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: slept for {0} ms total", sleep);
                    if (haveSegmentBuffered(number)) break; else {
                        attemptHoleFilling(number);
                    }
                }
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: awake: done sleeping {0}", sleep);
                waitingThread = null;
                waitingSegment = -1;
                co = getPipelineSegment(number);
                synchronized (readerReadyObj) {
                    readerReadyVal = -1;
                    inOrderSegments.notifyAll();
                }
            }
            if (co != null) {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we had segment {0} already!!", number);
                return co;
            } else {
                if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: we don't have segment {0} pipelined... what happened?", number);
            }
            if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: Cannot get segment " + number + " of file {0} expected segment: {1}.", _baseName, SegmentationProfile.segmentName(_baseName, number));
            throw new IOException("Cannot get segment " + number + " of file " + _baseName + " expected segment: " + SegmentationProfile.segmentName(_baseName, number));
        }
    }

    /**
	 * Checks whether we might have a next segment.
	 * @return Returns false if this content is marked as GONE (see ContentType), or if we have
	 * 		retrieved the segment marked as the last one, or, in a very rare case, if we're
	 * 		reading content that does not have segment markers.
	 */
    protected boolean hasNextSegment() throws IOException {
        if (isGone()) {
            if (Log.isLoggable(Log.FAC_IO, Level.FINER)) Log.finer(Log.FAC_IO, "getNextSegment: We have a gone segment, no next segment. Gone segment: {0}", _firstSegment.name());
            return false;
        }
        if (null == _currentSegment) {
            if (Log.isLoggable(Log.FAC_IO, Level.SEVERE)) Log.severe(Log.FAC_IO, "hasNextSegment() called when we have no current segment!");
            throw new IOException("hasNextSegment() called when we have no current segment!");
        }
        if (null != _currentSegment.signedInfo().getFinalBlockID()) {
            if (Arrays.equals(_currentSegment.signedInfo().getFinalBlockID(), _currentSegment.name().lastComponent())) {
                if (Log.isLoggable(Log.FAC_IO, Level.FINER)) {
                    Log.finer(Log.FAC_IO, "getNextSegment: there is no next segment. We have segment: " + DataUtils.printHexBytes(_currentSegment.name().lastComponent()) + " which is marked as the final segment.");
                }
                return false;
            }
        }
        if (!SegmentationProfile.isSegment(_currentSegment.name())) {
            if (Log.isLoggable(Log.FAC_IO, Level.INFO)) Log.info(Log.FAC_IO, "Unsegmented content: {0}. No next segment.", _currentSegment.name());
            return false;
        }
        return true;
    }

    /**
	 * Retrieve the next segment of the stream. Convenience method, uses #getSegment(long).
	 * @return the next segment, if found.
	 * @throws IOException
	 */
    protected ContentObject getNextSegment() throws IOException {
        if (null == _currentSegment) {
            if (Log.isLoggable(Log.FAC_IO, Level.FINE)) Log.fine(Log.FAC_IO, "getNextSegment: no current segment, getting first segment.");
            ContentObject firstSegment = getFirstSegment();
            setFirstSegment(firstSegment);
            return firstSegment;
        }
        if (Log.isLoggable(Log.FAC_IO, Level.FINE)) Log.fine(Log.FAC_IO, "getNextSegment: getting segment after {0}", _currentSegment.name());
        return getSegment(nextSegmentNumber());
    }

    /**
	 * Retrieves the first segment of the stream, based on specified startingSegmentNumber 
	 * (see #CCNAbstractInputStream(ContentName, Long, PublisherPublicKeyDigest, ContentKeys, CCNHandle)).
	 * @return the first segment, if found.
	 * @throws IOException If can't get a valid starting segment number
	 */
    public ContentObject getFirstSegment() throws IOException {
        if (null != _firstSegment) {
            return _firstSegment;
        } else if (null != _startingSegmentNumber) {
            ContentObject firstSegment = getSegment(_startingSegmentNumber);
            if (Log.isLoggable(Log.FAC_IO, Level.FINE)) {
                Log.fine(Log.FAC_IO, "getFirstSegment: segment number: " + _startingSegmentNumber + " got segment? " + ((null == firstSegment) ? "no " : firstSegment.name()));
            }
            return firstSegment;
        } else {
            throw new IOException("Stream does not have a valid starting segment number.");
        }
    }

    /**
	 * Method to determine whether a retrieved block is the first segment of this stream (as
	 * specified by startingSegmentNumber, (see #CCNAbstractInputStream(ContentName, Long, PublisherPublicKeyDigest, ContentKeys, CCNHandle)).
	 * Overridden by subclasses to implement narrower constraints on names. Once first
	 * segment is retrieved, further segments can be identified just by segment-naming
	 * conventions (see SegmentationProfile).
	 * 
	 * @param desiredName The expected name prefix for the stream. 
	 * 	For CCNAbstractInputStream, assume that desiredName contains the name up to but not including
	 * 	segmentation information.
	 * @param segment The potential first segment.
	 * @return True if it is the first segment, false otherwise.
	 */
    protected boolean isFirstSegment(ContentName desiredName, ContentObject segment) {
        if ((null != segment) && (SegmentationProfile.isSegment(segment.name()))) {
            if (Log.isLoggable(Log.FAC_IO, Level.FINER)) Log.finer(Log.FAC_IO, "is {0} a first segment of {1}", segment.name(), desiredName);
            if (!desiredName.equals(SegmentationProfile.segmentRoot(segment.name()))) {
                if (Log.isLoggable(Log.FAC_IO, Level.FINE)) Log.fine(Log.FAC_IO, "Desired name :{0} is not a prefix of segment: {1}", desiredName, segment.name());
                return false;
            }
            if (null != _startingSegmentNumber) {
                return (_startingSegmentNumber.longValue() == SegmentationProfile.getSegmentNumber(segment.name()));
            } else {
                return SegmentationProfile.isFirstSegment(segment.name());
            }
        }
        return false;
    }

    /**
	 * If we traversed a link to get this object, make it available.
	 */
    public synchronized LinkObject getDereferencedLink() {
        return _dereferencedLink;
    }

    /**
	 * Use only if you know what you are doing.
	 */
    protected synchronized void setDereferencedLink(LinkObject dereferencedLink) {
        _dereferencedLink = dereferencedLink;
    }

    /**
	 * Add a LinkObject to the stack we had to dereference to get here.
	 */
    protected synchronized void pushDereferencedLink(LinkObject dereferencedLink) {
        if (null == dereferencedLink) {
            return;
        }
        if (null != _dereferencedLink) {
            if (null != dereferencedLink.getDereferencedLink()) {
                if (Log.isLoggable(Log.FAC_IO, Level.WARNING)) {
                    Log.warning(Log.FAC_IO, "Merging two link stacks -- {0} already has a dereferenced link from {1}. Behavior unpredictable.", dereferencedLink.getVersionedName(), dereferencedLink.getDereferencedLink().getVersionedName());
                }
            }
            dereferencedLink.pushDereferencedLink(_dereferencedLink);
        }
        setDereferencedLink(dereferencedLink);
    }

    /**
	 * Returns the first segment number for this stream.
	 * @return The index of the first segment of stream data.
	 */
    public long firstSegmentNumber() {
        return _startingSegmentNumber.longValue();
    }

    /**
	 * Returns the segment number for the next segment.
	 * Default segmentation generates sequentially-numbered stream
	 * segments but this method may be overridden in subclasses to 
	 * perform re-assembly on streams that have been segmented differently.
	 * @return The index of the next segment of stream data.
	 */
    public long nextSegmentNumber() {
        if (null == _currentSegment) {
            return _startingSegmentNumber.longValue();
        } else {
            return segmentNumber() + 1;
        }
    }

    /**
	 * @return Returns the segment number of the current segment if we have one, otherwise
	 * the expected startingSegmentNumber.
	 */
    public long segmentNumber() {
        if (null == _currentSegment) {
            return _startingSegmentNumber;
        } else {
            return SegmentationProfile.getSegmentNumber(_currentSegment.name());
        }
    }

    /**
	 * @return Returns the segment number of the current segment if we have one, otherwise -1.
	 */
    protected long currentSegmentNumber() {
        if (null == _currentSegment) {
            return -1;
        }
        return segmentNumber();
    }

    /**
	 * Checks to see whether this content has been marked as GONE (deleted). Will retrieve the first
	 * segment if we do not already have it in order to make this determination.
	 * @return true if stream is GONE.
	 * @throws NoMatchingContentFound exception if no first segment found
	 * @throws IOException if there is other difficulty retrieving the first segment.
	 */
    public boolean isGone() throws NoMatchingContentFoundException, IOException {
        if (null == _firstSegment) {
            ContentObject firstSegment = getFirstSegment();
            setFirstSegment(firstSegment);
        }
        if (_firstSegment.isType(ContentType.GONE)) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Return the single segment of a stream marked as GONE.  This method 
	 * should be called only after checking isGone() == true otherwise it 
	 * may return the wrong result.
	 * @return the GONE segment or null if state unknown or stream is not marked GONE
	 */
    public ContentObject deletionInformation() {
        if (null != _firstSegment && _firstSegment.isType(ContentType.GONE)) return _firstSegment; else return null;
    }

    /**
	 * Callers may need to access information about this stream's publisher.
	 * We eventually should (TODO) ensure that all the segments we're reading
	 * match in publisher information, and cache the verified publisher info.
	 * (In particular once we're doing trust calculations, to ensure we do them
	 * only once per stream.)
	 * But we do verify each segment, so start by pulling what's in the current segment.
	 * @return the publisher of the data in the stream (either as requested, or once we have
	 * data, as observed).
	 */
    public PublisherPublicKeyDigest publisher() {
        return _publisher;
    }

    /**
	 * @return the key locator for this stream's publisher.
	 * @throw IOException if unable to obtain content (NoMatchingContentFoundException)
	 */
    public KeyLocator publisherKeyLocator() throws IOException {
        if (null == _firstSegment) {
            ContentObject firstSegment = getFirstSegment();
            setFirstSegment(firstSegment);
        }
        return _firstSegment.signedInfo().getKeyLocator();
    }

    /**
	 * @return the name of the current segment held by this string, or "null". Used for debugging.
	 */
    public String currentSegmentName() {
        return ((null == _currentSegment) ? "null" : _currentSegment.name().toString());
    }

    @Override
    public int available() throws IOException {
        if (null == _segmentReadStream) return 0;
        return _segmentReadStream.available();
    }

    /**
	 * @return Whether this stream believes it is at eof (has read past the end of the 
	 *   last segment of the stream).
	 */
    public boolean eof() {
        return _atEOF;
    }

    @Override
    public void close() throws IOException {
        Log.info(Log.FAC_IO, "CCNAbstractInputStream: close {0}:  shutting down pipelining", _baseName);
        cancelInterests();
        resetPipelineState();
    }

    @Override
    public synchronized void mark(int readlimit) {
        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: in mark({0}) currentSegment {1}", readlimit, currentSegmentNumber());
        _readlimit = readlimit;
        _markBlock = segmentNumber();
        if (null == _segmentReadStream) {
            _markOffset = 0;
        } else {
            try {
                _markOffset = _currentSegment.contentLength() - _segmentReadStream.available();
                if (_segmentReadStream.markSupported()) {
                    _segmentReadStream.mark(readlimit);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (Log.isLoggable(Log.FAC_IO, Level.FINEST)) Log.finest(Log.FAC_IO, "mark: block: " + segmentNumber() + " offset: " + _markOffset);
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: in reset() currentSegment {0}", currentSegmentNumber());
        if (isGone()) return;
        if (_currentSegment == null) {
            setFirstSegment(getFirstSegment());
            setCurrentSegment(getSegment(_markBlock));
        } else if (currentSegmentNumber() == _markBlock) {
            if (tell() == _markOffset) {
            } else {
                if (_segmentReadStream.markSupported()) {
                    _segmentReadStream.reset();
                    if (Log.isLoggable(Log.FAC_IO, Level.FINEST)) Log.finest(Log.FAC_IO, "reset within block: block: " + segmentNumber() + " offset: " + _markOffset + " eof? " + _atEOF);
                    return;
                } else {
                    setCurrentSegment(_currentSegment);
                }
            }
        } else {
            setCurrentSegment(getSegment(_markBlock));
        }
        _segmentReadStream.skip(_markOffset);
        _atEOF = false;
        if (Log.isLoggable(Log.FAC_IO, Level.FINEST)) Log.finest(Log.FAC_IO, "reset: block: " + segmentNumber() + " offset: " + _markOffset + " eof? " + _atEOF);
    }

    @Override
    public long skip(long n) throws IOException {
        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: in skip({0}) currentSegment {1}", n, currentSegmentNumber());
        if (isGone()) return 0;
        if (Log.isLoggable(Log.FAC_IO, Level.FINER)) Log.finer(Log.FAC_IO, "in skip(" + n + ")");
        if (n < 0) {
            return 0;
        }
        return readInternal(null, 0, (int) n);
    }

    /**
	 * @return Currently returns 0. Can be optionally overridden by subclasses.
	 * @throws IOException
	 */
    protected int segmentCount() throws IOException {
        return 0;
    }

    /**
	 * Seek a stream to a specific byte offset from the start. Tries to avoid retrieving
	 * extra segments.
	 * @param position
	 * @throws IOException
	 */
    public void seek(long position) throws IOException {
        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: in seek({0}) currentSegment {1}", position, currentSegmentNumber());
        if (isGone()) return;
        if (Log.isLoggable(Log.FAC_IO, Level.FINER)) {
            Log.finer(Log.FAC_IO, "Seeking stream to {0}", position);
        }
        if ((_currentSegment == null) || (!SegmentationProfile.isFirstSegment(_currentSegment.name()))) {
            setFirstSegment(getFirstSegment());
            skip(position);
        } else if (position > tell()) {
            skip(position - tell());
        } else {
            rewindSegment();
            skip(position);
        }
    }

    /**
	 * @return Returns position in byte offset. For CCNAbstractInputStream, provide an inadequate
	 *   base implementation that returns the offset into the current segment (not the stream as
	 *   a whole).
	 * @throws IOException
	 */
    public long tell() throws IOException {
        if (Log.isLoggable(Log.FAC_PIPELINE, Level.INFO)) Log.info(Log.FAC_PIPELINE, "PIPELINE: in tell() currentSegment {0}", currentSegmentNumber());
        if (isGone()) return 0;
        return _currentSegment.contentLength() - _segmentReadStream.available();
    }

    /**
	 * @return Total length of the stream, if known, otherwise -1.
	 * @throws IOException
	 */
    public long length() throws IOException {
        return -1;
    }

    private static class IncomingSegment {

        public ContentObject content;

        public Interest interest;

        public long segmentNumber;

        private IncomingSegment(ContentObject co, Interest i) {
            content = co;
            interest = i;
            segmentNumber = SegmentationProfile.getSegmentNumber(co.name());
        }
    }
}
