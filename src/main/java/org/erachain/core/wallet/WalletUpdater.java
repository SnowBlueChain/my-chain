package org.erachain.core.wallet;

import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.block.Block;
import org.erachain.datachain.BlockSignsMap;
import org.erachain.datachain.DCSet;
import org.erachain.utils.MonitoredThread;
import org.erachain.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class WalletUpdater extends MonitoredThread {

    private final static boolean USE_MONITOR = true;
    private boolean runned;

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletUpdater.class.getSimpleName());

    private static final int QUEUE_LENGTH = 32 + (32 >> (Controller.HARD_WORK >> 1));
    BlockingQueue<Pair<Boolean, Block>> blockingQueue = new ArrayBlockingQueue<>(QUEUE_LENGTH);

    private Controller controller;
    private BlockChain blockChain;
    private DCSet dcSet;
    private Wallet wallet;

    public NavigableMap<Integer, Block> lastBlocks = new TreeMap<>();

    public WalletUpdater(Controller controller, BlockChain blockChain, DCSet dcSet, Wallet wallet) {
        this.controller = controller;
        this.blockChain = blockChain;
        this.dcSet = dcSet;
        this.wallet = wallet;

        this.setName("WalletUpdater[" + this.getId() + "]");

        this.start();
    }

    /**
     * null - not need, True - FULL, false - continue
     */
    private Boolean synchronizeMode;

    public void setGoSynchronize(Boolean value) {
        synchronizeMode = value;
    }

    public void offerMessage(Pair<Boolean, Block> pair) {
        blockingQueue.offer(pair);
    }

    private void processMessage(Pair<Boolean, Block> pair) {

        if (pair == null)
            return;

        if (pair.getA()) {
            // ORPHAN
            if (!wallet.checkNeedSyncWallet(pair.getB().getSignature())) {
                wallet.orphanBlock(dcSet, pair.getB());

            } else {
                // set then NEED SYNCH
                synchronizeMode = false;
            }
        } else {
            // PROCESS
            if (controller.isStatusOK() // только если нет синхронизации
                    && !wallet.checkNeedSyncWallet(pair.getB().getReference())) {
                wallet.processBlock(dcSet, pair.getB());

            } else {
                // set then NEED SYNCH
                synchronizeMode = false;
            }
        }
    }

    private void trySynchronize(boolean reset) {

        if (!reset && wallet.synchronizeBodyUsed
                || controller.isOnStopping()
                || controller.noDataWallet || controller.noUseWallet) {
            return;
        }

        controller.walletSyncStatusUpdate(-1);

        LOGGER.info(" >>>>>>>>>>>>>>> *** Synchronizing wallet..." + (reset ? " RESET" : ""));

        ///////////////////////////////////// IS CHAIN VALID
        if (Wallet.CHECK_CHAIN_BROKENS_ON_SYNC_WALLET) {
            LOGGER.info("TEST CHAIN .... ");
            for (int i = 1; i <= dcSet.getBlockMap().size(); i++) {
                Block block = dcSet.getBlockMap().getAndProcess(i);
                if (block.getHeight() != i) {
                    Long error = null;
                    ++error;
                }
                if (block.blockHead.heightBlock != i) {
                    Long error = null;
                    ++error;
                }
                Block.BlockHead head = dcSet.getBlocksHeadsMap().get(i);
                if (head.heightBlock != i) {
                    Long error = null;
                    ++error;
                }
                if (i > 1) {
                    byte[] reference = block.getReference();
                    Block parent = dcSet.getBlockSignsMap().getBlock(reference);
                    if (parent == null) {
                        Long error = null;
                        ++error;
                    }
                    if (parent.getHeight() != i - 1) {
                        Long error = null;
                        ++error;
                    }
                    parent = dcSet.getBlockMap().getAndProcess(i - 1);
                    if (!Arrays.equals(parent.getSignature(), reference)) {
                        Long error = null;
                        ++error;
                    }
                }
                byte[] signature = block.getSignature();
                int signHeight = dcSet.getBlockSignsMap().get(signature);
                if (signHeight != i) {
                    Long error = null;
                    ++error;
                }
            }
        }

        Block block;

        if (reset) {

            // полная пересборка кошелька

            // break current synchronization if exists
            wallet.synchronizeBodyUsed = false;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            wallet.synchronizeBody(true);
            return;

        } else {

            byte[] lastSignature = wallet.database.getLastBlockSignature();
            if (lastSignature == null) {
                LOGGER.debug(" >>>>>>>>>>>>>>> *** Synchronizing wallet... by lastSignature = null");

                //setGoSynchronize(true);
                // break current synchronization if exists
                wallet.synchronizeBodyUsed = false;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                wallet.synchronizeBody(true);
                return;
            }

            block = dcSet.getBlockSignsMap().getBlock(lastSignature);
            if (block == null) {
                LOGGER.debug(" >>>>>>>>>>>>>>> *** Synchronizing wallet... by lastBlock = null");

                Iterator<Integer> iterator = lastBlocks.descendingKeySet().iterator();
                BlockSignsMap signsMap = dcSet.getBlockSignsMap();
                Integer lastCommonHeight;
                while (iterator.hasNext()) {
                    lastCommonHeight = iterator.next();
                    Block lastBlock = lastBlocks.get(lastCommonHeight);
                    if (signsMap.contains(lastBlock.getSignature())) {
                        LOGGER.debug(" >>>>>>>>>>>>>>> *** Synchronizing wallet from COMMON block: " + lastCommonHeight);
                        // нашли общий блок
                        // перебор по новой чтобы откатить
                        int key = lastBlocks.lastKey();
                        while (lastBlocks.containsKey(key)) {
                            if (lastCommonHeight == key) {
                                // запустим догоняние
                                wallet.synchronizeBody(false);
                                return;
                            }
                            lastBlock = lastBlocks.get(key);
                            wallet.orphanBlock(dcSet, lastBlock);
                            lastBlocks.remove(key);
                            key--;
                        }
                    }
                }

                ///setGoSynchronize(true);
                // break current synchronization if exists
                wallet.synchronizeBodyUsed = false;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                wallet.synchronizeBody(true);
                return;

            }
        }

        // запустим догоняние
        wallet.synchronizeBody(false);

    }

    public void run() {

        runned = true;
        //Message message;
        while (runned) {
            try {
                Pair<Boolean, Block> item = blockingQueue.poll(1L, TimeUnit.SECONDS);
                if (item != null) {
                    processMessage(item);
                    continue;
                }

                if (synchronizeMode != null) {
                    boolean reset = synchronizeMode;
                    synchronizeMode = null;
                    trySynchronize(reset);
                }

            } catch (OutOfMemoryError e) {
                LOGGER.error(e.getMessage(), e);
                controller.stopAll(686);
                return;
            } catch (IllegalMonitorStateException e) {
                break;
            } catch (InterruptedException e) {
                break;
            }

        }

        LOGGER.info("Wallet Updater halted");
    }

    public void halt() {
        this.runned = false;
    }

}
