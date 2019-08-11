package org.erachain.core;


import org.erachain.controller.Controller;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.TransactionMap;
import org.erachain.lang.Lang;
import org.erachain.network.Peer;
import org.erachain.network.message.MessageFactory;
import org.erachain.network.message.SignaturesMessage;
import org.erachain.ntp.NTP;
import org.erachain.settings.Settings;
import org.erachain.utils.MonitoredThread;
import org.erachain.utils.ObserverMessage;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;

/**
 * основной верт, решающий последовательно три задачи - либо собираем блок, проверяем отставание от сети
 * и синхронизируемся с сетью если не догнали ее, либо ловим новый блок из сети и заносим его в цепочку блоков
 */
public class BlockGenerator extends MonitoredThread implements Observer {

    public static final boolean TEST_001 = false;

    static Logger LOGGER = LoggerFactory.getLogger(BlockGenerator.class.getName());

    private static int WAIT_STEP_MS = 100;

    private static Controller ctrl = Controller.getInstance();
    private static int local_status = 0;
    private PrivateKeyAccount acc_winner;
    //private List<Block> lastBlocksForTarget;
    private byte[] solvingReference;
    private List<PrivateKeyAccount> cachedAccounts;
    private ForgingStatus forgingStatus = ForgingStatus.FORGING_DISABLED;
    private boolean walletOnceUnlocked = false;
    private int orphanto = 0;
    private static List<byte[]> needRemoveInvalids;

    private final DCSet dcSet;
    private final BlockChain bchain;

    public BlockGenerator(DCSet dcSet, BlockChain bchain, boolean withObserve) {

        this.dcSet = dcSet;
        this.bchain = bchain;

        if (Settings.getInstance().isGeneratorKeyCachingEnabled()) {
            this.cachedAccounts = new ArrayList<PrivateKeyAccount>();
        }

        if (withObserve) addObserver();
        this.setName("Thread BlockGenerator - " + this.getId());
    }

    public static int getStatus() {
        return local_status;
    }

    public static String viewStatus() {

        switch (local_status) {
            case -1:
                return "-1 STOPed";
            case 1:
                return "1 FLUSH, WAIT";
            case 2:
                return "2 FLUSH, TRY";
            case 3:
                return "3 UPDATE";
            case 31:
                return "31 UPDATE SAME";
            case 41:
                return "41 WAIT MAKING";
            case 4:
                return "4 PREPARE MAKING";
            case 5:
                return "5 GET WIN ACCOUNT";
            case 6:
                return "6 WAIT BEFORE MAKING";
            case 7:
                return "7 MAKING NEW BLOCK";
            case 8:
                return "8 BROADCASTING";
            default:
                return "0 WAIT";
        }
    }

    /**
     * если цепочка встала из-за патовой ситуации то попробовать ее решить
     ^ путем выбора люолее сильной а не длинной
     * так же кажые 10 блоков проверяеем самую толстую цепочку
     */
    public boolean checkWeightPeers() {
        // MAY BE PAT SITUATION

        //logger.debug("try check better WEIGHT peers");

        Tuple2<Integer, Long> myHW = ctrl.getBlockChain().getHWeightFull(dcSet);

        Peer peer;
        this.setMonitorStatus("checkWeightPeers");

        int counter = 0;
        // на всякий случай поставим ораничение
        while (counter++ < 30) {

            Tuple3<Integer, Long, Peer> maxPeer = ctrl.getMaxPeerHWeight(0, true);
            peer = maxPeer.c;

            if (peer == null) {
                return false;
            }

            LOGGER.debug("better WEIGHT peers found: "
                    + maxPeer);

            SignaturesMessage response = null;
            try {

                byte[] prevSignature = dcSet.getBlocksHeadsMap().get(myHW.a - 1).reference;
                response = (SignaturesMessage) peer.getResponse(
                        MessageFactory.getInstance().createGetHeadersMessage(prevSignature),
                        Synchronizer.GET_BLOCK_TIMEOUT >> 2);
            } catch (Exception e) {
                ////peer.ban(1, "Cannot retrieve headers - from UPDATE");
                LOGGER.debug("peers response error " + peer);
                // remove HW from peers
                ctrl.setWeightOfPeer(peer, null);
                continue;
            }

            if (response == null) {
                ////peer.ban(1, "Cannot retrieve headers - from UPDATE");
                LOGGER.debug("peers is null " + peer);
                // remove HW from peers
                ctrl.setWeightOfPeer(peer, null);
                continue;
            }

            List<byte[]> headers = response.getSignatures();
            byte[] lastSignature = bchain.getLastBlockSignature(dcSet);
            int headersSize = headers.size();
            LOGGER.debug("FOUND head SIZE: " + headersSize);
            if (headersSize == 3 || headersSize == 2) {
                if (Arrays.equals(headers.get(headersSize - 1), lastSignature)) {
                    // если прилетели данные с этого ПИРА - сброим их в то что мы сами вычислили
                    LOGGER.debug("peer has same Weight " + maxPeer);
                    ctrl.resetWeightOfPeer(peer);
                    // продолжим поиск дальше
                    continue;
                } else {
                    LOGGER.debug("I to orphan x2 - peer has better Weight " + maxPeer);
                    try {
                        // да - там другой блок - откатим тогда свой
                        ctrl.orphanInPipe(bchain.getLastBlock(dcSet));
                        return true;
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                        ctrl.resetWeightOfPeer(peer);
                    }
                }
            } else if (headersSize < 2) {
                LOGGER.debug("I to orphan - peer has better Weight " + maxPeer);
                try {
                    ctrl.orphanInPipe(bchain.getLastBlock(dcSet));
                    return true;
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    ctrl.resetWeightOfPeer(peer);
                }
            } else {
                // more then 2 - need to UPDATE
                LOGGER.debug("to update - peers " + maxPeer
                        + " headers: " + headersSize);
                return true;
            }

        }

        return false;

    }

    public Block generateNextBlock(PrivateKeyAccount account,
                                   Block parentBlock, Tuple2<List<Transaction>, Integer> transactionsItem, int height, int forgingValue, long winValue, long previousTarget) {

        if (transactionsItem == null) {
            return null;
        }

        int version = parentBlock.getNextBlockVersion(dcSet);
        byte[] atBytes;
        atBytes = new byte[0];

        //CREATE NEW BLOCK
        Block newBlock = new Block(version, parentBlock.getSignature(), account, height,
                transactionsItem, atBytes,
                forgingValue, winValue, previousTarget);
        newBlock.sign(account);
        return newBlock;

    }

    public Tuple2<List<Transaction>, Integer> getUnconfirmedTransactions(int blockHeight, long timestamp, BlockChain bchain,
                                                                         long max_winned_value) {

        long start = System.currentTimeMillis();

        //CREATE FORK OF GIVEN DATABASE
        DCSet newBlockDC = null;

        Block waitWin;

        start = System.currentTimeMillis();

        List<Transaction> transactionsList = new ArrayList<Transaction>();

        //	boolean transactionProcessed;
        long totalBytes = 0;
        int counter = 0;

        TransactionMap map = dcSet.getTransactionMap();
        Iterator<Long> iterator = map.getTimestampIterator();

        needRemoveInvalids = new ArrayList<byte[]>();

        this.setMonitorStatusBefore("getUnconfirmedTransactions");

        while (iterator.hasNext()) {

            if (ctrl.isOnStopping()) {
                return null;
            }

            if (bchain != null) {
                waitWin = bchain.getWaitWinBuffer();
                if (waitWin != null && waitWin.getWinValue() > max_winned_value) {
                    break;
                }
            }

            Transaction transaction = map.get(iterator.next());

            if (transaction.getTimestamp() > timestamp)
                break;

            // делать форк только если есть трнзакции - так как это сильно кушает память
            if (newBlockDC == null) {
                //CREATE FORK OF GIVEN DATABASE
                newBlockDC = dcSet.fork();
            }

            if (!transaction.isSignatureValid(newBlockDC)) {
                needRemoveInvalids.add(transaction.getSignature());
                continue;
            }

            try {

                transaction.setDC(newBlockDC, Transaction.FOR_NETWORK, blockHeight, counter + 1);

                if (transaction.isValid(Transaction.FOR_NETWORK, 0l) != Transaction.VALIDATE_OK) {
                    needRemoveInvalids.add(transaction.getSignature());
                    continue;
                }

                //CHECK IF ENOUGH ROOM
                if (++counter > BlockChain.MAX_BLOCK_SIZE_GEN) {
                    counter--;
                    break;
                }

                totalBytes += transaction.getDataLength(Transaction.FOR_NETWORK, true);
                if (totalBytes > BlockChain.MAX_BLOCK_SIZE_BYTES_GEN) {
                    counter--;
                    break;
                }

                ////ADD INTO LIST
                transactionsList.add(transaction);

                //PROCESS IN NEWBLOCKDB
                transaction.process(null, Transaction.FOR_NETWORK);

                // GO TO NEXT TRANSACTION
                continue;

            } catch (Exception e) {

                if (ctrl.isOnStopping()) {
                    return null;
                }

                //     transactionProcessed = true;

                LOGGER.error(e.getMessage(), e);
                //REMOVE FROM LIST
                needRemoveInvalids.add(transaction.getSignature());

                continue;

            }

        }

        if (newBlockDC != null )
            newBlockDC.close();

        LOGGER.debug("get Unconfirmed Transactions = " + (System.currentTimeMillis() - start) + "ms for trans: " + counter);

        this.setMonitorStatusAfter();

        return new Tuple2<List<Transaction>, Integer>(transactionsList, counter);
    }

    private void clearInvalids() {
        if (needRemoveInvalids != null && !needRemoveInvalids.isEmpty()) {
            long start = System.currentTimeMillis();
            TransactionMap transactionsMap = dcSet.getTransactionMap();
            for (byte[] signature : needRemoveInvalids) {
                if (ctrl.isOnStopping()) {
                    return;
                }
                if (transactionsMap.contains(signature))
                    transactionsMap.delete(signature);
            }
            LOGGER.debug("clear INVALID Transactions = " + (System.currentTimeMillis() - start) + "ms for removed: " + needRemoveInvalids.size()
                    + " LEFT: " + transactionsMap.size());

            needRemoveInvalids = null;
        }

    }

    public void checkForRemove(long timestamp) {

        //CREATE FORK OF GIVEN DATABASE
        DCSet newBlockDC = dcSet.fork();
        int blockHeight =  newBlockDC.getBlockMap().size() + 1;

        //Block waitWin;
        int counter = 0;
        int totalBytes = 0;

        long start = System.currentTimeMillis();

        TransactionMap map = dcSet.getTransactionMap();
        Iterator<Long> iterator = map.getTimestampIterator();

        needRemoveInvalids = new ArrayList<byte[]>();

        this.setMonitorStatusBefore("checkForRemove");

        while (iterator.hasNext()) {

            if (ctrl.isOnStopping()) {
                return;
            }

            Transaction transaction = map.get(iterator.next());

            if (transaction.getTimestamp() > timestamp)
                break;
            if (!transaction.isSignatureValid(newBlockDC)) {
                needRemoveInvalids.add(transaction.getSignature());
                continue;
            }

            try {

                transaction.setDC(newBlockDC, Transaction.FOR_NETWORK, blockHeight, counter + 1);

                if (transaction.isValid(Transaction.FOR_NETWORK, 0l) != Transaction.VALIDATE_OK) {
                    needRemoveInvalids.add(transaction.getSignature());
                    continue;
                }

                //CHECK IF ENOUGH ROOM
                if (++counter > (BlockChain.MAX_BLOCK_SIZE << 2)) {
                    break;
                }

                totalBytes += transaction.getDataLength(Transaction.FOR_NETWORK, true);
                if (totalBytes > (BlockChain.MAX_BLOCK_SIZE_BYTES_GEN << 2)) {
                    break;
                }

                //PROCESS IN NEWBLOCKDB
                transaction.process(null, Transaction.FOR_NETWORK);

                // GO TO NEXT TRANSACTION
                continue;

            } catch (Exception e) {

                if (ctrl.isOnStopping()) {
                    return;
                }

                //     transactionProcessed = true;

                LOGGER.error(e.getMessage(), e);
                //REMOVE FROM LIST
                needRemoveInvalids.add(transaction.getSignature());

                continue;

            }

        }

        newBlockDC.close();

        this.setMonitorStatusAfter();

        LOGGER.debug("get check for Remove = " + (System.currentTimeMillis() - start) + "ms for trans: " + map.size()
                + " needRemoveInvalids:" + needRemoveInvalids.size());

    }

    public ForgingStatus getForgingStatus() {
        return forgingStatus;
    }

    public void setForgingStatus(ForgingStatus status) {
        if (forgingStatus != status) {
            forgingStatus = status;
            ctrl.forgingStatusChanged(forgingStatus);
        }
    }

    public int getOrphanTo() {
        return this.orphanto;
    }

    public void setOrphanTo(int height) {
        this.orphanto = height;
    }

    public void addObserver() {
        new Thread("try syncForgingStatus") {
            @Override
            public void run() {

                //WE HAVE TO WAIT FOR THE WALLET TO ADD THAT LISTENER.
                while (!ctrl.doesWalletExists() || !ctrl.doesWalletDatabaseExists()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                ctrl.addWalletObserver(BlockGenerator.this);
                syncForgingStatus();
            }
        }.start();
        ctrl.addObserver(this);
    }

    private List<PrivateKeyAccount> getKnownAccounts() {
        //CHECK IF CACHING ENABLED
        if (Settings.getInstance().isGeneratorKeyCachingEnabled()) {
            List<PrivateKeyAccount> privateKeyAccounts = ctrl.getPrivateKeyAccounts();

            //IF ACCOUNTS EXISTS
            if (!privateKeyAccounts.isEmpty()) {
                //CACHE ACCOUNTS
                this.cachedAccounts = privateKeyAccounts;
            }

            //RETURN CACHED ACCOUNTS
            return this.cachedAccounts;
        } else {
            //RETURN ACCOUNTS
            return ctrl.getPrivateKeyAccounts();
        }
    }

    public void cacheKnownAccounts() {
        if (Settings.getInstance().isGeneratorKeyCachingEnabled()) {
            List<PrivateKeyAccount> privateKeyAccounts = ctrl.getPrivateKeyAccounts();

            //IF ACCOUNTS EXISTS
            if (!privateKeyAccounts.isEmpty()) {
                //CACHE ACCOUNTS
                this.cachedAccounts = privateKeyAccounts;
            }
        }
    }

    @Override
    public void run() {

        //TransactionMap transactionsMap = dcSet.getTransactionMap();

        int heapOverflowCount = 0;

        long processTiming;
        long transactionMakeTimingCounter = 0;
        long transactionMakeTimingAverage = 0;

        long timeToPing = 0;
        long timeTmp;
        long timePoint = 0;
        long timePointForGenerate = 0;
        long flushPoint = 0;
        long timeUpdate = 0;
        int shift_height = 0;
        //byte[] unconfirmedTransactionsHash;
        //long winned_value_account;
        //long max_winned_value_account;
        int height = BlockChain.getHeight(dcSet) + 1;
        int forgingValue;
        int winned_forgingValue;
        long winValue;
        int targetedWinValue;
        long winned_winValue;
        long previousTarget = bchain.getTarget(dcSet);

        int wait_new_block_broadcast;
        long wait_step;
        boolean newWinner;

        this.initMonitor();

        while (!ctrl.isOnStopping()) {

            Block waitWin = null;
            Block generatedBlock;
            Block solvingBlock;
            Peer peer = null;
            Tuple3<Integer, Long, Peer> maxPeer;
            SignaturesMessage response;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }

            try {

                this.setMonitorPoint();

                if (ctrl.isOnStopping()) {
                    local_status = -1;
                    return;
                }

                // GET real HWeight
                // пингуем всех тут чтобы знать кому слать свои транакции
                if (System.currentTimeMillis() - timeToPing > (BlockChain.DEVELOP_USE ? 60000 : 120000)) {
                    // нужно просмотривать пиги для синхронизации так же - если там -ХХ то не будет синхронизации
                    timeToPing = System.currentTimeMillis();
                    ctrl.pingAllPeers(false);
                }

                if (this.orphanto > 0) {
                    this.setMonitorStatusBefore("orphan to " + orphanto);
                    local_status = 9;
                    ctrl.setForgingStatus(ForgingStatus.FORGING_ENABLED);

                    // обязательно нужно чтобы память освобождать
                    // и если объект был изменен (с тем же ключем у него удалили поле внутри - чтобы это не выдавлось
                    // при новом запросе - иначе изменения прилетают в другие потоки и ошибку вызываю
                    dcSet.clearCache();

                    try {
                        while (bchain.getHeight(dcSet) >= this.orphanto
                            //    && bchain.getHeight(dcSet) > 157044
                        ) {
                            //if (bchain.getHeight(dcSet) > 157045 && bchain.getHeight(dcSet) < 157049) {
                            //    long iii = 11;
                            //}
                            //Block block = bchain.getLastBlock(dcSet);
                            ctrl.orphanInPipe(bchain.getLastBlock(dcSet));
                        }
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                    this.orphanto = 0;
                    ctrl.checkStatusAndObserve(0);

                    this.setMonitorStatusAfter();

                }

                timeTmp = bchain.getTimestamp(dcSet) + BlockChain.GENERATING_MIN_BLOCK_TIME_MS;
                if (timeTmp > NTP.getTime())
                    continue;

                if (timePoint != timeTmp) {
                    timePoint = timeTmp;
                    timePointForGenerate = timePoint
                            + (BlockChain.DEVELOP_USE ? BlockChain.GENERATING_MIN_BLOCK_TIME_MS
                            : BlockChain.FLUSH_TIMEPOINT)
                            - BlockChain.UNCONFIRMED_SORT_WAIT_MS
                    ;

                    Timestamp timestampPoit = new Timestamp(timePoint);
                    LOGGER.info("+ + + + + START GENERATE POINT on " + timestampPoit);
                    this.setMonitorStatus("+ + + + + START GENERATE POINT on " + timestampPoit);

                    flushPoint = BlockChain.FLUSH_TIMEPOINT + timePoint;
                    this.solvingReference = null;
                    local_status = 0;

                    // пинганем тут все чтобы знать кому слать вобедный блок
                    timeToPing = System.currentTimeMillis();
                    ctrl.pingAllPeers(true);

                    // осмотр сети по СИЛЕ
                    // уже все узлы свою силу передали при Controller.flushNewBlockGenerated

                    boolean needCheck = false;
                    if (BlockChain.CHECK_PEERS_WEIGHT_AFTER_BLOCKS < 2) {
                        // проверим силу других цепочек - и если есть сильнее то сделаем откат у себя так чтобы к ней примкнуть
                        needCheck = true;
                    } else {
                        Tuple2<Integer, Long> myHW = ctrl.getBlockChain().getHWeightFull(dcSet);
                        if (myHW.a % BlockChain.CHECK_PEERS_WEIGHT_AFTER_BLOCKS == 0) {
                            // проверим силу других цепочек - и если есть сильнее то сделаем откат у себя так чтобы к ней примкнуть
                            needCheck = true;
                        }
                    }
                    if (needCheck && checkWeightPeers()) {
                        // было отставание по силе цепочки - запретим сборку блока нам - так как мы откатились чуток и нужна синхронизация
                        setForgingStatus(ForgingStatus.FORGING_WAIT);
                    }
                }

                // is WALLET
                if (ctrl.doesWalletExists()) {


                    if (timePoint + BlockChain.WIN_BLOCK_BROADCAST_WAIT_MS > NTP.getTime()) {
                        continue;
                    }

                    local_status = 41;
                    this.setMonitorStatus("local_status " + viewStatus());

                    //CHECK IF WE HAVE CONNECTIONS and READY to GENERATE
                    // если на 1 высота выше хотябы то переходим на синхронизацию
                    // поэтому сдвиг = 0
                    ctrl.checkStatusAndObserve(0);

                    if (forgingStatus == ForgingStatus.FORGING_WAIT
                            && (timePoint + (BlockChain.GENERATING_MIN_BLOCK_TIME_MS << 1) < NTP.getTime()
                            || BlockChain.DEVELOP_USE && height < 100
                            || height < 10)) {

                        setForgingStatus(ForgingStatus.FORGING);
                    }

                    if (//true ||
                            (forgingStatus == ForgingStatus.FORGING // FORGING enabled
                                    && !ctrl.needUpToDate()
                                    && (this.solvingReference == null // AND GENERATING NOT MAKED
                                    || !Arrays.equals(this.solvingReference, dcSet.getBlockMap().getLastBlockSignature())
                            ))
                    ) {

                        /////////////////////////////// TRY FORGING ////////////////////////

                        if (ctrl.isOnStopping()) {
                            local_status = -1;
                            return;
                        }

                        //SET NEW BLOCK TO SOLVE
                        this.solvingReference = dcSet.getBlockMap().getLastBlockSignature();
                        solvingBlock = dcSet.getBlockMap().last();

                        if (ctrl.isOnStopping()) {
                            local_status = -1;
                            return;
                        }

                        /*
                         * нужно сразу взять транзакции которые бедум в блок класть - чтобы
                         * значть их ХЭШ -
                         * тоже самое и AT записями поидее
                         * и эти хэши закатываем уже в заголвок блока и подписываем
                         * после чего делать вычисление значения ПОБЕДЫ - она от подписи зависит
                         * если победа случиласть то
                         * далее сами трнзакции кладем в тело блока и закрываем его
                         */
                        /*
                         * нет не  так - вычисляеи победное значение и если оно выиграло то
                         * к нему транзакции собираем
                         * и время всегда одинаковое
                         *
                         */

                        local_status = 4;
                        this.setMonitorStatus("local_status " + viewStatus());

                        //GENERATE NEW BLOCKS
                        //this.lastBlocksForTarget = bchain.getLastBlocksForTarget(dcSet);
                        this.acc_winner = null;


                        //unconfirmedTransactionsHash = null;
                        winned_winValue = 0;
                        winned_forgingValue = 0;
                        //max_winned_value_account = 0;
                        height = bchain.getHeight(dcSet) + 1;
                        previousTarget = bchain.getTarget(dcSet);

                        ///if (height > BlockChain.BLOCK_COUNT) return;

                        //PREVENT CONCURRENT MODIFY EXCEPTION
                        List<PrivateKeyAccount> knownAccounts = this.getKnownAccounts();
                        synchronized (knownAccounts) {

                            local_status = 5;
                            this.setMonitorStatus("local_status " + viewStatus());

                            for (PrivateKeyAccount account : knownAccounts) {

                                forgingValue = account.getBalanceUSE(Transaction.RIGHTS_KEY, dcSet).intValue();
                                winValue = BlockChain.calcWinValue(dcSet, account, height, forgingValue);
                                if (winValue < 1)
                                    continue;

                                targetedWinValue = BlockChain.calcWinValueTargetedBase(dcSet, height, winValue, previousTarget);
                                if (targetedWinValue < 1)
                                    continue;

                                if (winValue > winned_winValue) {
                                    //this.winners.put(account, winned_value);
                                    acc_winner = account;
                                    winned_winValue = winValue;
                                    winned_forgingValue = forgingValue;
                                    //max_winned_value_account = winned_value_account;

                                }
                            }
                        }

                        if (acc_winner != null) {

                            if (ctrl.isOnStopping()) {
                                local_status = -1;
                                return;
                            }

                            wait_new_block_broadcast = BlockChain.GENERATING_MIN_BLOCK_TIME_MS >> 1;
                            int shiftTime = (int) (((wait_new_block_broadcast * (previousTarget - winned_winValue) * 10) / previousTarget));
                            wait_new_block_broadcast = wait_new_block_broadcast + shiftTime;

                            // сдвиг на заранее - только на 1/4 максимум
                            if (wait_new_block_broadcast < BlockChain.GENERATING_MIN_BLOCK_TIME_MS >> 2) {
                                wait_new_block_broadcast = BlockChain.GENERATING_MIN_BLOCK_TIME_MS >> 2;
                            } else if (wait_new_block_broadcast > BlockChain.GENERATING_MIN_BLOCK_TIME_MS) {
                                wait_new_block_broadcast = BlockChain.GENERATING_MIN_BLOCK_TIME_MS;
                            }

                            newWinner = false;
                            if (wait_new_block_broadcast > 0) {

                                local_status = 6;
                                this.setMonitorStatus("local_status " + viewStatus());

                                LOGGER.info("@@@@@@@@ wait for new winner and BROADCAST: " + wait_new_block_broadcast / 1000);
                                // SLEEP and WATCH break
                                wait_step = wait_new_block_broadcast / WAIT_STEP_MS;

                                this.setMonitorStatus("wait for new winner and BROADCAST: " + wait_new_block_broadcast / 1000);

                                do {
                                    try {
                                        Thread.sleep(WAIT_STEP_MS);
                                    } catch (InterruptedException e) {
                                    }

                                    if (ctrl.isOnStopping()) {
                                        local_status = -1;
                                        return;
                                    }

                                    waitWin = bchain.getWaitWinBuffer();
                                    if (waitWin != null && waitWin.calcWinValue(dcSet) > winned_winValue) {
                                        // NEW WINNER received
                                        newWinner = true;
                                        break;
                                    }

                                }
                                while (this.orphanto <= 0 && wait_step-- > 0 && NTP.getTime() < timePoint + BlockChain.GENERATING_MIN_BLOCK_TIME_MS);
                            }

                            if (this.orphanto > 0)
                                continue;

                            if (newWinner) {
                                LOGGER.info("NEW WINER RECEIVED - drop my block");
                            } else {
                                /////////////////////    MAKING NEW BLOCK  //////////////////////
                                local_status = 7;
                                this.setMonitorStatus("local_status " + viewStatus());

                                // GET VALID UNCONFIRMED RECORDS for current TIMESTAMP
                                LOGGER.info("GENERATE my BLOCK");

                                generatedBlock = null;
                                try {
                                    processTiming = System.nanoTime();
                                    generatedBlock = generateNextBlock(acc_winner, solvingBlock,
                                            getUnconfirmedTransactions(height, timePointForGenerate,
                                                    bchain, winned_winValue),
                                            height, winned_forgingValue, winned_winValue, previousTarget);

                                    processTiming = System.nanoTime() - processTiming;

                                    // только если вблоке есть стрнзакции то вычислим
                                    if (generatedBlock.getTransactionCount() > 0
                                            && processTiming < 999999999999l) {
                                        // при переполнении может быть минус
                                        // в миеросекундах подсчет делаем
                                        // ++ 10 потому что там ФОРК базы делаем - он очень медленный
                                        processTiming = processTiming / 1000 /
                                                (Controller.BLOCK_AS_TX_COUNT + generatedBlock.getTransactionCount());
                                        if (transactionMakeTimingCounter < 1 << 3) {
                                            transactionMakeTimingCounter++;
                                            transactionMakeTimingAverage = ((transactionMakeTimingAverage * transactionMakeTimingCounter)
                                                    + processTiming - transactionMakeTimingAverage) / transactionMakeTimingCounter;
                                        } else
                                            transactionMakeTimingAverage = ((transactionMakeTimingAverage << 3)
                                                    + processTiming - transactionMakeTimingAverage) >> 3;

                                        ctrl.setTransactionMakeTimingAverage(transactionMakeTimingAverage);
                                    }

                                } catch (java.lang.OutOfMemoryError e) {
                                    local_status = -1;
                                    LOGGER.error(e.getMessage(), e);
                                    ctrl.stopAll(94);
                                    return;
                                }

                                solvingBlock = null;

                                if (generatedBlock == null) {
                                    if (ctrl.isOnStopping()) {
                                        this.local_status = -1;
                                        return;
                                    }

                                    LOGGER.info("generateNextBlock is NULL... try wait");
                                    try {
                                        Thread.sleep(10000);
                                    } catch (InterruptedException e) {
                                    }

                                    continue;
                                } else {
                                    //PASS BLOCK TO CONTROLLER
                                    try {
                                        LOGGER.info("bchain.setWaitWinBuffer, size: " + generatedBlock.getTransactionCount());
                                        if (bchain.setWaitWinBuffer(dcSet, generatedBlock, peer)) {

                                            // need to BROADCAST
                                            local_status = 8;
                                            this.setMonitorStatus("local_status " + viewStatus());

                                            ctrl.broadcastWinBlock(generatedBlock);
                                            local_status = 0;
                                            this.setMonitorStatus("local_status " + viewStatus());

                                        } else {
                                            LOGGER.info("my BLOCK is weak ((...");
                                        }
                                        generatedBlock = null;
                                    } catch (java.lang.OutOfMemoryError e) {
                                        local_status = -1;
                                        LOGGER.error(e.getMessage(), e);
                                        ctrl.stopAll(94);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }

                ////////////////////////////  FLUSH NEW BLOCK /////////////////////////
                // сдвиг 0 делаем
                ctrl.checkStatusAndObserve(0);
                if (!ctrl.needUpToDate()) {

                    // try solve and flush new block from Win Buffer
                    waitWin = bchain.getWaitWinBuffer();
                    if (waitWin != null) {

                        this.solvingReference = null;

                        // FLUSH WINER to DB MAP
                        LOGGER.info("wait to FLUSH WINER to DB MAP " + (flushPoint - NTP.getTime()) / 1000);

                        local_status = 1;
                        this.setMonitorStatus("local_status " + viewStatus());

                        while (this.orphanto <= 0 && flushPoint > NTP.getTime()) {
                            try {
                                Thread.sleep(WAIT_STEP_MS);
                            } catch (InterruptedException e) {
                            }

                            if (ctrl.isOnStopping()) {
                                local_status = -1;
                                return;
                            }
                        }

                        if (this.orphanto > 0)
                            continue;

                        // FLUSH WINER to DB MAP
                        LOGGER.info("TRY to FLUSH WINER to DB MAP");

                        try {
                            if (timePoint + BlockChain.GENERATING_MIN_BLOCK_TIME_MS < NTP.getTime()) {
                                try {
                                    // если вдруг цепочка встала,, то догоняем не очень быстро чтобы принимать все
                                    // победные блоки не спеша
                                    Thread.sleep(BlockChain.DEVELOP_USE ? 500 : 5000);
                                } catch (InterruptedException e) {
                                }
                            }

                            local_status = 2;
                            this.setMonitorStatus("local_status " + viewStatus());

                            try {
                                if (!ctrl.flushNewBlockGenerated()) {
                                    this.setMonitorStatusAfter();
                                    // NEW BLOCK not FLUSHED
                                    LOGGER.info("NEW BLOCK not FLUSHED");
                                } else {
                                    this.setMonitorStatusAfter();
                                    if (forgingStatus == ForgingStatus.FORGING_WAIT)
                                        setForgingStatus(ForgingStatus.FORGING);
                                }
                            } catch (java.lang.OutOfMemoryError e) {
                                local_status = -1;
                                LOGGER.error(e.getMessage(), e);
                                ctrl.stopAll(94);
                                return;
                            }

                            if (ctrl.isOnStopping()) {
                                local_status = -1;
                                return;
                            }

                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            if (ctrl.isOnStopping()) {
                                local_status = -1;
                                return;
                            }
                            LOGGER.error(e.getMessage(), e);
                        }

                        bchain.clearWaitWinBuffer();

                        if (needRemoveInvalids != null) {
                            clearInvalids();
                        } else {
                            checkForRemove(timePointForGenerate);
                            clearInvalids();
                        }
                    }

                    continue;
                }

                ////////////////////////// UPDATE ////////////////////

                timeUpdate = timePoint + BlockChain.GENERATING_MIN_BLOCK_TIME_MS + BlockChain.WIN_BLOCK_BROADCAST_WAIT_MS - NTP.getTime();
                if (timeUpdate > 0)
                    continue;

                if (false   // так как сейчас в начале цикла проверяем вокруг узла на более сильную цепочку даже
                        // с той же высотой то тут не нуно делать провкеу на патовую ситуацмю

                        && timeUpdate + BlockChain.GENERATING_MIN_BLOCK_TIME_MS + (BlockChain.GENERATING_MIN_BLOCK_TIME_MS >> 1) < 0
                        && ctrl.getActivePeersCounter() > (BlockChain.DEVELOP_USE? 1 : 3)) {
                    // если случилась патовая ситуация то найдем более сильную цепочку (не по высоте)
                    // если есть сильнее то сделаем откат у себя
                    //logger.debug("try resolve PAT situation");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                    }

                    if (ctrl.isOnStopping()) {
                        local_status = -1;
                        return;
                    }
                    checkWeightPeers();
                }

                /// CHECK PEERS HIGHER
                // так как в девелопе все гоняют свои цепочки то посмотреть самыю жирную а не длинную
                ctrl.checkStatusAndObserve(shift_height);
                //CHECK IF WE ARE NOT UP TO DATE
                if (ctrl.needUpToDate()) {

                    if (ctrl.isOnStopping()) {
                        local_status = -1;
                        return;
                    }

                    local_status = 3;
                    this.setMonitorStatus("local_status " + viewStatus());

                    this.solvingReference = null;
                    bchain.clearWaitWinBuffer();

                    ctrl.update(shift_height);

                    local_status = 0;
                    this.setMonitorStatus("local_status " + viewStatus());

                    if (ctrl.isOnStopping()) {
                        local_status = -1;
                        return;
                    }

                    // CHECK WALLET SYNCHRONIZE after UPDATE of CHAIN
                    ctrl.checkNeedSyncWallet();

                    setForgingStatus(ForgingStatus.FORGING_WAIT);

                } else {

                }

            } catch (java.lang.OutOfMemoryError e) {
                this.local_status = -1;
                LOGGER.error(e.getMessage(), e);
                ctrl.stopAll(96);
                return;
            } catch (Throwable e) {
                if (ctrl.isOnStopping()) {
                    this.local_status = -1;
                    return;
                }

                LOGGER.error(e.getMessage(), e);

            }
        }

        // EXITED
        this.local_status = -1;
        this.setMonitorStatus("local_status " + viewStatus());

    }

    @Override
    public void update(Observable arg0, Object arg1) {
        ObserverMessage message = (ObserverMessage) arg1;

        if (message.getType() == ObserverMessage.WALLET_STATUS || message.getType() == ObserverMessage.NETWORK_STATUS) {
            //WALLET ONCE UNLOCKED? WITHOUT UNLOCKING FORGING DISABLED
            if (!walletOnceUnlocked && message.getType() == ObserverMessage.WALLET_STATUS) {
                walletOnceUnlocked = true;
            }

            if (walletOnceUnlocked) {
                // WALLET UNLOCKED OR GENERATOR CACHING TRUE
                syncForgingStatus();
            }
        }

    }

    public void syncForgingStatus() {

        if (!Settings.getInstance().isForgingEnabled() || getKnownAccounts().isEmpty()) {
            setForgingStatus(ForgingStatus.FORGING_DISABLED);
            return;
        }

        int status = ctrl.getStatus();
        //CONNECTIONS OKE? -> FORGING
        // CONNECTION not NEED now !!
        // TARGET_WIN will be small
        if (status != Controller.STATUS_OK
            ///|| ctrl.isProcessingWalletSynchronize()
        ) {
            setForgingStatus(ForgingStatus.FORGING_ENABLED);
            return;
        }

        if (forgingStatus != ForgingStatus.FORGING) {
            setForgingStatus(ForgingStatus.FORGING_WAIT);
        }

		/*
		// NOT NEED to wait - TARGET_WIN will be small
		if (ctrl.isReadyForging())
			setForgingStatus(ForgingStatus.FORGING);
		else
			setForgingStatus(ForgingStatus.FORGING_WAIT);
			*/
    }

    public enum ForgingStatus {

        FORGING_DISABLED(0, Lang.getInstance().translate("Forging disabled")),
        FORGING_ENABLED(1, Lang.getInstance().translate("Forging enabled")),
        FORGING(2, Lang.getInstance().translate("Forging")),
        FORGING_WAIT(3, Lang.getInstance().translate("Forging awaiting another peer sync"));

        private final int statuscode;
        private String name;

        ForgingStatus(int status, String name) {
            statuscode = status;
            this.name = name;
        }

        public int getStatuscode() {
            return statuscode;
        }

        public String getName() {
            return name;
        }

    }

}
