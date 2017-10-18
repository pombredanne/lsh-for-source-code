package com.croftsoft.app.intfire;

import java.util.*;
import com.croftsoft.core.ai.neuro.Channel;
import com.croftsoft.core.ai.neuro.ChannelMut;
import com.croftsoft.core.ai.neuro.Injector;
import com.croftsoft.core.ai.neuro.IntFireNeuron;
import com.croftsoft.core.ai.neuro.imp.ChannelMutImp;
import com.croftsoft.core.ai.neuro.imp.HhNeuronImp;
import com.croftsoft.core.ai.neuro.imp.IntFireNeuronImp;
import com.croftsoft.core.lang.*;
import com.croftsoft.core.lang.lifecycle.Startable;
import com.croftsoft.core.lang.lifecycle.Updatable;
import com.croftsoft.core.math.MathConstants;
import com.croftsoft.core.sim.DeltaClock;
import com.croftsoft.core.sim.DeltaClockImp;
import com.croftsoft.core.sim.SimLib;
import com.croftsoft.core.util.mail.Mail;
import com.croftsoft.core.util.seq.ListSeq;
import com.croftsoft.core.util.seq.Seq;
import com.croftsoft.lib.jse.ai.neuro.imp.ExpDecayInjector;
import com.croftsoft.lib.jse.util.tpu.ThreePhaseUpdate;

/***********************************************************************
    * Model.
    * 
    * Maintains program state.
    * 
    * @version
    *   $Id: IntFireModelImp.java 177 2011-11-04 04:35:10Z croft $
    * @since
    *   2011-10-06
    * @author
    *   <a href="http://www.CroftSoft.com/">David Wallace Croft</a>
    ***********************************************************************/
public final class IntFireModelImp implements IntFireModel, Startable, Updatable {

    private static final long TIME_INTERVAL_NANOS = 100 * MathConstants.NANOSECONDS_PER_MICROSECOND, UPDATES_PER_UPDATE = 10, DELTA_TIME_NANOS = TIME_INTERVAL_NANOS / UPDATES_PER_UPDATE;

    private static final double DELTA_TIME = MathConstants.SECONDS_PER_NANOSECOND * DELTA_TIME_NANOS, TIME_INTERVAL = MathConstants.SECONDS_PER_NANOSECOND * TIME_INTERVAL_NANOS;

    private static final double CHANNEL_CONDUCTANCE = 5 * 2.11e-10, DECAY_RATE_EXC = 1 / 1e-3, DECAY_RATE_INH = 0.5 * DECAY_RATE_EXC, INJECTOR_CURRENT = -1e-10;

    private static final int TIME_SERIES_LENGTH = 200;

    private final List<ExpDecayInjector.Command> excCommandList, inhCommandList;

    private final Mail<IntFireMessage> mail;

    private final Seq<Injector> injectorSeq;

    private final DeltaClock deltaClock;

    private final IntFireNeuronImp intFireNeuronImp;

    private final ThreePhaseUpdate[] threePhaseUpdates;

    private double[] excitatoryCurrentTimeSeries, inhibitoryCurrentTimeSeries, membraneVoltageTimeSeries;

    private double excitatoryCurrentRange, inhibitoryCurrentRange, membraneVoltageRange;

    private boolean[] spikeTimeSeries;

    private double timeMin;

    private int offset, timeSeriesLength;

    private long timeStep;

    private boolean simulationRunning = true;

    /** Number of spikes since the previous update. */
    private int spikeCount;

    public IntFireModelImp(final IntFireConfig intFireConfig, final Mail<IntFireMessage> mail) {
        NullArgumentException.checkArgs(intFireConfig, this.mail = mail);
        System.out.println("DELTA_TIME = " + DELTA_TIME);
        deltaClock = new DeltaClockImp(DELTA_TIME);
        final List<Channel> channelList = new ArrayList<Channel>();
        final Seq<Channel> channelSeq = new ListSeq<Channel>(channelList);
        final List<Injector> injectorList = new ArrayList<Injector>();
        excCommandList = new ArrayList<ExpDecayInjector.Command>();
        inhCommandList = new ArrayList<ExpDecayInjector.Command>();
        final Seq<ExpDecayInjector.Command> excCommandSeq = new ListSeq<ExpDecayInjector.Command>(excCommandList), inhCommandSeq = new ListSeq<ExpDecayInjector.Command>(inhCommandList);
        final ExpDecayInjector excExpDecayInjector = new ExpDecayInjector(excCommandSeq, deltaClock, DECAY_RATE_EXC, INJECTOR_CURRENT, false), inhExpDecayInjector = new ExpDecayInjector(inhCommandSeq, deltaClock, DECAY_RATE_INH, -INJECTOR_CURRENT, false);
        injectorList.add(excExpDecayInjector);
        injectorList.add(inhExpDecayInjector);
        injectorSeq = new ListSeq<Injector>(injectorList);
        System.out.println("TIME_INTERVAL = " + TIME_INTERVAL);
        intFireNeuronImp = new IntFireNeuronImp(channelSeq, injectorSeq, deltaClock);
        timeSeriesLength = TIME_SERIES_LENGTH;
        excitatoryCurrentTimeSeries = new double[timeSeriesLength];
        inhibitoryCurrentTimeSeries = new double[timeSeriesLength];
        membraneVoltageTimeSeries = new double[timeSeriesLength];
        spikeTimeSeries = new boolean[timeSeriesLength];
        final double membraneVoltage = intFireNeuronImp.getMembraneVoltage();
        for (int i = 0; i < timeSeriesLength; i++) {
            membraneVoltageTimeSeries[i] = membraneVoltage;
        }
        threePhaseUpdates = new ThreePhaseUpdate[] { excExpDecayInjector, inhExpDecayInjector };
    }

    @Override
    public double getExcitatoryCurrent(int index) {
        return excitatoryCurrentTimeSeries[(offset + index) % timeSeriesLength];
    }

    @Override
    public double getExcitatoryCurrentRange() {
        return excitatoryCurrentRange;
    }

    @Override
    public double getInhibitoryCurrent(int index) {
        return inhibitoryCurrentTimeSeries[(offset + index) % timeSeriesLength];
    }

    @Override
    public double getInhibitoryCurrentRange() {
        return inhibitoryCurrentRange;
    }

    @Override
    public Seq<Injector> getInjectorSeq() {
        return injectorSeq;
    }

    @Override
    public IntFireNeuron getIntFireNeuron() {
        return intFireNeuronImp;
    }

    @Override
    public double getMembraneVoltage(final int index) {
        return membraneVoltageTimeSeries[(offset + index) % timeSeriesLength];
    }

    @Override
    public double getMembraneVoltageRange() {
        return membraneVoltageRange;
    }

    @Override
    public boolean getSpike(final int index) {
        return spikeTimeSeries[(offset + index) % timeSeriesLength];
    }

    @Override
    public int getSpikeCount() {
        return spikeCount;
    }

    @Override
    public double getTimeInterval() {
        return TIME_INTERVAL;
    }

    @Override
    public double getTimeMin() {
        return timeMin;
    }

    @Override
    public int getTimeSeriesLength() {
        return timeSeriesLength;
    }

    @Override
    public void start() {
    }

    @Override
    public void update() {
        excCommandList.clear();
        inhCommandList.clear();
        final int size = mail.size();
        for (int i = 0; i < size; i++) {
            final IntFireMessage intFireMessage = mail.get(i);
            final IntFireMessage.Type type = intFireMessage.getType();
            switch(type) {
                case EXCITATORY_INJECTOR_OFF_REQUEST:
                    excCommandList.add(ExpDecayInjector.Command.OFF);
                    break;
                case EXCITATORY_INJECTOR_ON_REQUEST:
                    excCommandList.add(ExpDecayInjector.Command.ON);
                    break;
                case INHIBITORY_INJECTOR_OFF_REQUEST:
                    inhCommandList.add(ExpDecayInjector.Command.OFF);
                    break;
                case INHIBITORY_INJECTOR_ON_REQUEST:
                    inhCommandList.add(ExpDecayInjector.Command.ON);
                    break;
                case TOGGLE_PAUSE_REQUEST:
                    simulationRunning = !simulationRunning;
                    break;
                default:
            }
        }
        if (simulationRunning) {
            boolean spiked = false;
            spikeCount = 0;
            for (int i = 0; i < UPDATES_PER_UPDATE; i++) {
                for (final ThreePhaseUpdate threePhaseUpdate : threePhaseUpdates) {
                    threePhaseUpdate.access();
                }
                for (final ThreePhaseUpdate threePhaseUpdate : threePhaseUpdates) {
                    threePhaseUpdate.digest();
                }
                for (final ThreePhaseUpdate threePhaseUpdate : threePhaseUpdates) {
                    threePhaseUpdate.mutate();
                }
                SimLib.update(intFireNeuronImp);
                if (intFireNeuronImp.isSpiking()) {
                    spiked = true;
                    spikeCount++;
                }
            }
            final double membraneVoltage = intFireNeuronImp.getMembraneVoltage();
            final double absMembraneVoltage = Math.abs(membraneVoltage);
            if (absMembraneVoltage > membraneVoltageRange) {
                membraneVoltageRange = absMembraneVoltage;
            }
            membraneVoltageTimeSeries[offset] = membraneVoltage;
            spikeTimeSeries[offset] = spiked;
            double excitatoryCurrent = 0, inhibitoryCurrent = 0;
            final int injectorSeqSize = injectorSeq.size();
            for (int i = 0; i < injectorSeqSize; i++) {
                final Injector injector = injectorSeq.get(i);
                if (!injector.isOn()) {
                    continue;
                }
                final double current = injector.getCurrent();
                if (current > 0) {
                    inhibitoryCurrent += current;
                    if (current > inhibitoryCurrentRange) {
                        inhibitoryCurrentRange = current;
                    }
                } else {
                    excitatoryCurrent += current;
                    final double absCurrent = Math.abs(current);
                    if (absCurrent > excitatoryCurrentRange) {
                        excitatoryCurrentRange = absCurrent;
                    }
                }
            }
            excitatoryCurrentTimeSeries[offset] = excitatoryCurrent;
            inhibitoryCurrentTimeSeries[offset] = inhibitoryCurrent;
            offset = (offset + 1) % timeSeriesLength;
            timeStep++;
            timeMin = (timeStep - timeSeriesLength) * TIME_INTERVAL;
        }
    }
}
