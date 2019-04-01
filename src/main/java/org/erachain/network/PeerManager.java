package org.erachain.network;

import org.erachain.controller.Controller;
import org.erachain.network.message.Message;
import org.erachain.ntp.NTP;
import org.erachain.settings.Settings;
import org.erachain.utils.MonitoredThread;
import org.erachain.utils.SimpleFileVisitorForRecursiveFolderDeletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class PeerManager extends MonitoredThread {

    private Network network;

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerManager.class);

    private static final int QUEUE_LENGTH = 20;
    BlockingQueue<Peer> blockingQueue = new ArrayBlockingQueue<Peer>(QUEUE_LENGTH);

    public PeerManager(Network network) {
        this.network = network;
    }

    public List<Peer> getBestPeers() {
        return Controller.getInstance().getDBSet().getPeerMap().getBestPeers(Settings.getInstance().getMaxSentPeers() << 2, false);
    }

    public void addPeer(Peer peer, int banForMinutes) {
        //ADD TO DATABASE
        if (!Controller.getInstance().isOnStopping()) {
            try {
                Controller.getInstance().getDBSet().getPeerMap().addPeer(peer, banForMinutes);
            } catch (Exception e) {

                // TODO понять почему произошла ошибка https://lab.erachain.org/erachain/Erachain/issues/669
                LOGGER.error(e.getMessage(), e);
                LOGGER.error("try delete error peer");
                try {
                    Controller.getInstance().getDBSet().getPeerMap().delete(peer.getAddress().getAddress());
                } catch (Exception eIO) {
                    LOGGER.error(eIO.getMessage(), eIO);
                    LOGGER.error("try reCreateDB");
                    try {
                        Controller.getInstance().getDBSet().close();
                        Controller.getInstance().reCreateDB();
                    } catch (Exception eIO2) {
                        LOGGER.error(eIO.getMessage(), eIO2);
                        Controller.getInstance().stopAll(38);
                    }
                }

            }
        }
    }

    public boolean isBanned(Peer peer) {
        return Controller.getInstance().getDBSet().getPeerMap().isBanned(peer.getAddress());
    }

    private void processPeers(Peer peerTest) {

        // убрать дубли
        for (Peer peer : network.getActivePeers(false)) {
            if (!peer.isUsed() || NTP.getTime() - peer.getConnectionTime() > 300000)
                continue;

            byte[] addressIP = peer.getAddress().getAddress();
            for (Peer peerOld : network.getActivePeers(false)) {
                if (peer.getId() == peerOld.getId() || !peerOld.isUsed()
                    || !Arrays.equals(addressIP, peerOld.getAddress().getAddress())
                )
                    continue;

                peer.ban(0, "on duplicate");
                return;
            }
        }
    }

    public void run() {

        Peer peer = null;

        while (this.network.run) {
            try {
                //processPeers(blockingQueue.take());
                peer = blockingQueue.poll(10, TimeUnit.SECONDS);
            } catch (java.lang.OutOfMemoryError e) {
                LOGGER.error(e.getMessage(), e);
                Controller.getInstance().stopAll(71);
                break;
            } catch (java.lang.IllegalMonitorStateException e) {
                break;
            } catch (java.lang.InterruptedException e) {
                break;
            }

            processPeers(peer);

        }

        LOGGER.info("Peer Manager halted");

    }


    public void halt() {
    }

}
