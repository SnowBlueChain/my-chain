package org.erachain.network;
// 30/03

import org.erachain.controller.Controller;
import org.erachain.network.message.Message;
import org.erachain.network.message.MessageFactory;
import org.erachain.network.message.PeersMessage;
import org.erachain.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

//
/**
 * класс поиска каналов связи - подключается к внешним узлам создавая пиры
 * смотрит сколько соединений во вне (white) уже есть и если еще недостаточно то цепляется ко всему что сможет
 */
public class ConnectionCreator extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionCreator.class);
    private ConnectionCallback callback;
    private boolean isRun;

    public ConnectionCreator(ConnectionCallback callback) {
        this.callback = callback;
        this.setName("Thread ConnectionCreator - " + this.getId());
    }

    private int connectToPeersOfThisPeer(Peer peer, int maxReceivePeers) {

        if (!this.isRun)
            return 0;

        LOGGER.info("GET peers from: " + peer.getName() + " get max: " + maxReceivePeers);

        //CHECK IF WE ALREADY HAVE MAX CONNECTIONS for WHITE
        if (Settings.getInstance().getMinConnections() <= callback.getActivePeersCounter(true))
            return 0;

        //ASK PEER FOR PEERS
        Message getPeersMessage = MessageFactory.getInstance().createGetPeersMessage();
        long start = System.currentTimeMillis();
        PeersMessage peersMessage = (PeersMessage) peer.getResponse(getPeersMessage);
        if (peersMessage == null) {
            return 0;
        }

        peer.setPing((int) (System.currentTimeMillis() - start));

        int foreignPeersCounter = 0;
        //FOR ALL THE RECEIVED PEERS

        for (Peer newPeer : peersMessage.getPeers()) {

            if (!this.isRun)
                return 0;

            if (foreignPeersCounter >= maxReceivePeers) {
                // FROM EACH peer get only maxReceivePeers
                break;
            }

            if (Network.isMyself(newPeer.getAddress())) {
                continue;
            }

            //CHECK IF WE ALREADY HAVE MAX CONNECTIONS for WHITE
            if (Settings.getInstance().getMinConnections() <= callback.getActivePeersCounter(true))
                break;

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }

            //CHECK IF THAT PEER IS NOT BLACKLISTED
            if (PeerManager.getInstance().isBanned(newPeer))
                continue;

            //CHECK IF SOCKET IS NOT LOCALHOST
            if (newPeer.getAddress().isSiteLocalAddress()
                    || newPeer.getAddress().isLoopbackAddress()
                    || newPeer.getAddress().isAnyLocalAddress())
                continue;

            if (!Settings.getInstance().isTryingConnectToBadPeers() && newPeer.isBad())
                continue;

            //CHECK IF ALREADY CONNECTED TO PEER
            //CHECK IF PEER ALREADY used
            newPeer = callback.getKnownPeer(newPeer);
            if (newPeer.isUsed()) {
                continue;
            }

            // TODO small height not use
            //Controller.getInstance().getMyHeight();
            // newPeer.
            if (newPeer.isBanned())
                continue;

            if (!this.isRun)
                return 0;


            //CONNECT
            newPeer.connect(callback);
            if (newPeer.isUsed()) {
                foreignPeersCounter++;

                LOGGER.info("connected to BRANCH and recurse: " + newPeer.getAddress().getHostAddress());

                // RECURSE to OTHER PEERS
                connectToPeersOfThisPeer(newPeer, maxReceivePeers >> 1);

            }
        }

        return foreignPeersCounter;

    }

    public void run() {
        this.isRun = true;

        List<Peer> knownPeers = null;

        while (isRun) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }

            if (!this.isRun)
                return;

            //CHECK IF WE NEED NEW CONNECTIONS
            if (this.isRun && Settings.getInstance().getMinConnections() >= callback.getActivePeersCounter(true)) {

                //GET LIST OF KNOWN PEERS
                knownPeers = PeerManager.getInstance().getKnownPeers();

                //ITERATE knownPeers
                for (Peer peer : knownPeers) {

                    //CHECK IF WE ALREADY HAVE MIN CONNECTIONS
                    if (Settings.getInstance().getMinConnections() <= callback.getActivePeersCounter(true)) {
                        // stop use KNOWN peers
                        break;
                    }

                    if (Network.isMyself(peer.getAddress())) {
                        continue;
                    }

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LOGGER.error(e.getMessage(),e);
                    }

                    if (!this.isRun)
                        return;

                    //CHECK IF SOCKET IS NOT LOCALHOST
                    //if(true)
                    if (peer.getAddress().isSiteLocalAddress()
                            || peer.getAddress().isLoopbackAddress()
                            || peer.getAddress().isAnyLocalAddress()) {
                        continue;
                    }

                    //CHECK IF ALREADY CONNECTED TO PEER
                    //CHECK IF PEER ALREADY used
                    // new PEER from NETWORK poll or original from DB
                    peer = callback.getKnownPeer(peer);
                    if (peer.isUsed()) {
                        continue;
                    }

                    if (peer.isBanned())
                        continue;

                    if (!this.isRun)
                        return;

                    LOGGER.info("try connect to: " + peer.getAddress().getHostAddress());

                    /*
                    LOGGER.info(
                            Lang.getInstance().translate("Connecting to known peer %peer% :: %knownPeersCounter% / %allKnownPeers% :: Connections: %activeConnections%")
                                .replace("%peer%", peer.getAddress().getHostAddress())
                                .replace("%knownPeersCounter%", String.valueOf(knownPeersCounter))
                                .replace("%allKnownPeers%", String.valueOf(knownPeers.size()))
                                .replace("%activeConnections%", String.valueOf(callback.getActivePeersCounter(true)))
                                );
                                */

                    //CONNECT
                    //CHECK IF ALREADY CONNECTED TO PEER
                    if (peer.connect(callback) == 0) {
                        LOGGER.info("connected!!! " + peer.getAddress().getHostAddress());
                        // TRY CONNECT to WHITE peers of this PEER
                        connectToPeersOfThisPeer(peer, 4);
                    }
                }
            }

            //CHECK IF WE STILL NEED NEW CONNECTIONS
            // USE unknown peers from known peers
            if (this.isRun && Settings.getInstance().getMinConnections() >= callback.getActivePeersCounter(true)) {
                //OLD SCHOOL ITERATE activeConnections
                //avoids Exception when adding new elements
                List<Peer> peers = callback.getActivePeers(false);
                for (Peer peer: peers) {

                    if (!this.isRun)
                        return;

                    if (peer.isBanned())
                        continue;

                    if (Settings.getInstance().getMinConnections() >= callback.getActivePeersCounter(true)) {
                        break;
                    }

                    connectToPeersOfThisPeer(peer, Settings.getInstance().getMinConnections());

                }
            }

            //SLEEP
            int counter = callback.getActivePeersCounter(true);
            if (counter < 6)
                continue;

            int needMinConnections = Settings.getInstance().getMinConnections();

            if (!this.isRun)
                return;

            try {
                if (counter < needMinConnections)
                    Thread.sleep(1000);
                else
                    Thread.sleep(10000);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }

        }
    }

    public void halt() {
        this.isRun = false;
    }
}
