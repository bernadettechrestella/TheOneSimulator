/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.community;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import routing.DecisionEngineRouter;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.RoutingDecisionEngine;
import routing.community.Duration;

/**
 *
 * @author Bernadette Chrestella
 */
public class TotContactPrdBasedRouter implements RoutingDecisionEngine {

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;
    protected Map<DTNHost, Double> periode;

    public TotContactPrdBasedRouter(Settings s) {

    }

    public TotContactPrdBasedRouter(TotContactPrdBasedRouter proto) {
        startTimestamps = new HashMap<DTNHost, Double>();
        connHistory = new HashMap<DTNHost, List<Duration>>();
        periode = new HashMap<DTNHost, Double>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double time = getLastTime(peer); //waktu bertemu
        double etime = SimClock.getTime(); //waktu connDown

        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer); //mengambil list dari peer
        }
        if (etime - time > 0) {
            history.add(new Duration(time, etime)); //update waktu terbaru
        }
       
        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            double start = 0;
            double end = 0;

            for (Duration counter : entry.getValue()) {
                start = start + counter.start;
                end = end + counter.end;
                if (periode.containsKey(peer)) {
                    double count = end - start;
                    periode.put(peer, (periode.get(peer) + count));
                } else {
                    periode.put(peer, end-start);
                }
            }
        }
       
        startTimestamps.remove(peer);
    }

    public double getLastTime(DTNHost peer) {
        if (startTimestamps.containsKey(peer)) {
            return startTimestamps.get(peer);
        }
        return 0;
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer); //mengambil data myHost itu sendiri
        TotContactPrdBasedRouter de = this.getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());
    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        if (m.getTo() == otherHost) {
            return true;
        }

        DTNHost dest = m.getTo();

        TotContactPrdBasedRouter de = this.getOtherDecisionEngine(otherHost);

        double myTotal = 0;
        double otherTotal = 0;
        double myFreq = 0;
        double otherFreq = 0;

        if (this.connHistory.containsKey(dest)) { //jika di connHistory pengirim ada dest
           myTotal = this.periode.get(dest);
           myFreq = this.connHistory.get(dest).size();
        }
       
        if (de.connHistory.containsKey(dest)) {
           otherTotal = de.periode.get(dest);
           otherFreq = de.connHistory.get(dest).size();
        }
       
        if (myTotal > otherTotal) {
                if (myFreq < otherFreq) {
                    return true;
                }
            } else {
                if (myFreq < otherFreq) {
                    return true;
                }
            }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return m.getTo() == otherHost;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new TotContactPrdBasedRouter(this);
    }

    private TotContactPrdBasedRouter getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (TotContactPrdBasedRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

}