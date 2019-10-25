package org.erachain.core;


import org.erachain.controller.Controller;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.transaction.RSend;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.wallet.Wallet;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.ReferenceMapImpl;
import org.erachain.datachain.TransactionMap;
import org.erachain.lang.Lang;
import org.erachain.network.Peer;
import org.erachain.network.message.MessageFactory;
import org.erachain.network.message.SignaturesMessage;
import org.erachain.ntp.NTP;
import org.erachain.settings.Settings;
import org.erachain.utils.MonitoredThread;
import org.erachain.utils.ObserverMessage;
import org.mapdb.DB;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.*;

/**
 * основной верт, решающий последовательно три задачи - либо собираем блок, проверяем отставание от сети
 * и синхронизируемся с сетью если не догнали ее, либо ловим новый блок из сети и заносим его в цепочку блоков
 */
public class BlockGenerator extends MonitoredThread implements Observer {

    private static Logger LOGGER = LoggerFactory.getLogger(BlockGenerator.class.getSimpleName());

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

    public Peer betterPeer;
    /**
     * если цепочка встала из-за патовой ситуации то попробовать ее решить
     ^ путем выбора люолее сильной а не длинной
     * так же кажые 10 блоков проверяеем самую толстую цепочку
     */
    public boolean checkWeightPeers() {
        // MAY BE PAT SITUATION

        //logger.debug("try check better WEIGHT peers");

        Tuple2<Integer, Long> myHW = ctrl.getBlockChain().getHWeightFull(dcSet);

        betterPeer = null;

        Peer peer;
        this.setMonitorStatus("checkWeightPeers");

        //byte[] prevSignature = dcSet.getBlocksHeadsMap().get(myHW.a - 1).reference;
        byte[] lastSignature = bchain.getLastBlockSignature(dcSet);

        int counter = 0;
        // на всякий случай поставим ораничение
        while (counter++ < 30) {

            Tuple3<Integer, Long, Peer> maxPeer = ctrl.getMaxPeerHWeight(0, true);
            peer = maxPeer.c;

            if (peer == null) {
                return false;
            }

            ///LOGGER.debug("better WEIGHT peers found: " + maxPeer);

            SignaturesMessage response = null;
            try {

                response = (SignaturesMessage) peer.getResponse(
                        MessageFactory.getInstance().createGetHeadersMessage(lastSignature),
                        Synchronizer.GET_BLOCK_TIMEOUT >> 2);
            } catch (Exception e) {
                ///LOGGER.debug("RESPONSE error " + peer + " " + e.getMessage());
                // remove HW from peers
                ctrl.resetWeightOfPeer(peer);
                continue;
            }

            if (response == null) {
                ///LOGGER.debug("peer RESPONSE is null " + peer);
                // remove HW from peers
                ctrl.resetWeightOfPeer(peer);
                continue;
            }

            List<byte[]> headers = response.getSignatures();
            int headersSize = headers.size();
            ///LOGGER.debug("FOUND head SIZE: " + headersSize);

            if (headersSize > 0) {
                boolean isSame = false;
                for (byte[] signature : headers) {
                    if (Arrays.equals(signature, lastSignature)) {
                        isSame = true;
                        break;
                    }
                }

                if (isSame) {
                    // если прилетели данные с этого ПИРА - сброим их в то что мы сами вычислили
                    ///LOGGER.debug("peer has same Weight " + maxPeer);
                    ctrl.resetWeightOfPeer(peer);
                    // продолжим поиск дальше
                    continue;
                } else {
                    LOGGER.debug("I to orphan - peer has better Weight " + maxPeer);
                    try {
                        // да - там другой блок - откатим тогда свой
                        //// ctrl.orphanInPipe(bchain.getLastBlock(dcSet));
                        betterPeer = peer;
                        return true;
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                        ctrl.resetWeightOfPeer(peer);
                    }
                }
            } else {
                // more then 2 - need to UPDATE
                LOGGER.debug("to update - peers " + maxPeer
                        + " headers: " + headersSize);
                betterPeer = peer;
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
        Block newBlock = new Block(version, parentBlock, account, height,
                transactionsItem, atBytes,
                forgingValue, winValue, previousTarget);
        newBlock.sign(account);
        return newBlock;

    }

    private void testTransactions(int blockHeight, long blockTimestamp) {

        SecureRandom randomSecure = new SecureRandom();
        // сдвиг назад органиизуем
        blockTimestamp -= BlockChain.GENERATING_MIN_BLOCK_TIME_MS(blockHeight) - BlockChain.UNCONFIRMED_SORT_WAIT_MS(blockHeight) - 1;

        LOGGER.info("generate TEST txs: " + BlockChain.TEST_DB);

        boolean generateNewAccount = false;

        long assetKey = 2L;
        BigDecimal amount = new BigDecimal("0.00000001");
        RSend messageTx;
        byte[] isText = new byte[]{1};
        byte[] encryptMessage = new byte[]{0};

        TransactionMap map = dcSet.getTransactionTab();

        Random random = new Random();

        PublicKeyAccount recipient;
        HashMap<PrivateKeyAccount, Long> creatorsReference = new HashMap<>();
        long timestamp;
        for (int index = 0; index < BlockChain.TEST_DB; index++) {

            if (generateNewAccount) {
                byte[] seedRecipient = new byte[32];
                randomSecure.nextBytes(seedRecipient);
                recipient = new PublicKeyAccount(seedRecipient);
            } else {
                recipient = BlockChain.TEST_DB_ACCOUNTS[random.nextInt(BlockChain.TEST_DB_ACCOUNTS.length)];
            }

            PrivateKeyAccount creator = BlockChain.TEST_DB_ACCOUNTS[random.nextInt(BlockChain.TEST_DB_ACCOUNTS.length)];

            // определим время создания для каждого счета
            if (creatorsReference.containsKey(creator)) {
                timestamp = creatorsReference.get(creator) + 1;
            } else {
                timestamp = blockTimestamp;
            }
            creatorsReference.put(creator, timestamp);

            messageTx = new RSend(creator, (byte) 0, recipient, assetKey,
                    amount, "TEST" + blockHeight + "-" + index, null, isText, encryptMessage, timestamp, 0l);
            messageTx.sign(creator, Transaction.FOR_NETWORK);

            ctrl.transactionsPool.offerMessage(messageTx);

        }

        if (BlockChain.CHECK_DOUBLE_SPEND_DEEP >= 0) {
            //// только если включена проверка повторов - запускаем эту проверку на невалидные транзакции

            // добавить невалидных транзакций немного - по времени создания
            timestamp = blockTimestamp - 10000000L * 1L;
            PrivateKeyAccount[] creators = creatorsReference.keySet().toArray(new PrivateKeyAccount[0]);
            for (int index = 0; index < (BlockChain.TEST_DB >> 3); index++) {

                recipient = BlockChain.TEST_DB_ACCOUNTS[random.nextInt(BlockChain.TEST_DB_ACCOUNTS.length)];

                PrivateKeyAccount creator = creators[random.nextInt(creators.length)];

                messageTx = new RSend(creator, (byte) 0, recipient, assetKey,
                        amount, "TEST" + blockHeight + "-" + index, null, isText, encryptMessage,
                        timestamp, 0l);
                messageTx.sign(creator, Transaction.FOR_NETWORK);

                ctrl.transactionsPool.offerMessage(messageTx);

            }
        }

    }

    public Tuple2<List<Transaction>, Integer> getUnconfirmedTransactions(int blockHeight, long timestamp, BlockChain bchain,
                                                                         long max_winned_value) {

        LOGGER.debug("* * * * * COLLECT TRANSACTIONS");

        long start = System.currentTimeMillis();

        //CREATE FORK OF GIVEN DATABASE
        DCSet newBlockDC = null;

        Block waitWin;

        List<Transaction> transactionsList = new ArrayList<Transaction>();

        //	boolean transactionProcessed;
        long totalBytes = 0;
        int counter = 0;
        int check_time = 0;
        int max_time_gen = BlockChain.GENERATING_MIN_BLOCK_TIME_MS(blockHeight) >> 3;

        try {
            TransactionMap map = dcSet.getTransactionTab();
            Iterator<Long> iterator = map.getTimestampIterator(false);

            needRemoveInvalids = new ArrayList<byte[]>();

            this.setMonitorStatusBefore("getUnconfirmedTransactions");

            try {
                long testTime = 0;
                while (iterator.hasNext()) {

                    // проверим иногда - вдруг уже слишком долго собираем - останов сборки транзакций
                    // так как иначе такой блок и сеткой остальной не успеет обработаться
                    if (check_time++ > 300) {
                        if (System.currentTimeMillis() - start > max_time_gen) {
                            break;
                        }
                        check_time = 0;
                    }
                    if (ctrl.isOnStopping()) {
                        break;
                    }

                    if (bchain != null) {
                        waitWin = bchain.getWaitWinBuffer();
                        if (betterPeer != null || waitWin != null && waitWin.getWinValue() > max_winned_value) {
                            break;
                        }
                    }

                    Transaction transaction = map.get(iterator.next());
                    if (transaction == null)
                        break;

                    if (BlockChain.CHECK_BUGS > 7) {
                        LOGGER.debug(" found TRANSACTION on " + new Timestamp(transaction.getTimestamp()));
                        if (testTime > transaction.getTimestamp()) {
                            LOGGER.error(" ERROR testTIME " + new Timestamp(testTime));
                            testTime = transaction.getTimestamp();
                        }
                    }

                    if (transaction.getTimestamp() > timestamp)
                        break;

                    // делать форк только если есть трнзакции - так как это сильно кушает память
                    if (newBlockDC == null) {
                        //CREATE FORK OF GIVEN DATABASE
                        // создаем в памяти базу - так как она на 1 блок только нужна - а значит много памяти не возьмет
                        DB database = DCSet.makeDBinMemory();
                        newBlockDC = dcSet.fork(database);
                    }

                    transaction.setDC(newBlockDC, Transaction.FOR_NETWORK, blockHeight, counter + 1);

                    if (false // вообще-то все внутренние транзакции уже провверены на подпись!
                            && !transaction.isSignatureValid(newBlockDC)) {
                        needRemoveInvalids.add(transaction.getSignature());
                        continue;
                    }

                    if (false && // тут нельзя пока удалять - может она будет включена
                            // и пусть удаляется только если невалидная будет
                            timestamp > transaction.getDeadline()) {
                        needRemoveInvalids.add(transaction.getSignature());
                        continue;
                    }

                    try {

                        if (transaction.isValid(Transaction.FOR_NETWORK, 0l) != Transaction.VALIDATE_OK) {
                            needRemoveInvalids.add(transaction.getSignature());
                            if (BlockChain.CHECK_BUGS > 1) {
                                LOGGER.error(" Transaction invalid: " + transaction.isValid(Transaction.FOR_NETWORK, 0l));
                            }
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

                    } catch (Exception e) {

                        if (ctrl.isOnStopping()) {
                            break;
                        }

                        //     transactionProcessed = true;

                        LOGGER.error(e.getMessage(), e);
                        //REMOVE FROM LIST
                        needRemoveInvalids.add(transaction.getSignature());

                    }

                }

            } finally {
                if (newBlockDC != null)
                    newBlockDC.close();
            }

            LOGGER.debug("get Unconfirmed Transactions = " + (System.currentTimeMillis() - start)
                    + "ms for trans: " + counter + " and DELETE: " + needRemoveInvalids.size()
                    + " from Poll: " + map.size());

            this.setMonitorStatusAfter();

        } catch (java.lang.Throwable e) {
            if (e instanceof java.lang.IllegalAccessError) {
                // налетели на закрытую таблицу
            } else {
                LOGGER.error(e.getMessage(), e);
            }
            LOGGER.debug("get Unconfirmed Transactions = " + (System.currentTimeMillis() - start)
                    + "ms for trans: " + counter + " and DELETE: " + needRemoveInvalids.size()
                    + " before CLEARED event!");

        }

        return new Tuple2<List<Transaction>, Integer>(transactionsList, counter);
    }

    private void clearInvalids() {
        if (needRemoveInvalids != null && !needRemoveInvalids.isEmpty()) {
            long start = System.currentTimeMillis();
            TransactionMap transactionsMap = dcSet.getTransactionTab();

            for (byte[] signature : needRemoveInvalids) {
                if (ctrl.isOnStopping()) {
                    return;
                }
                try {
                    if (!transactionsMap.isClosed() && transactionsMap.contains(signature))
                        transactionsMap.delete(signature);
                } catch (java.lang.Throwable e) {
                    if (e instanceof java.lang.IllegalAccessError) {
                        // налетели на закрытую таблицу
                    } else {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
            LOGGER.debug("clear INVALID Transactions = " + (System.currentTimeMillis() - start) + "ms for removed: " + needRemoveInvalids.size()
                    + " LEFT: " + transactionsMap.size());

            needRemoveInvalids = null;
        }

    }

    public void checkForRemove(long timestamp) {

        //CREATE FORK OF GIVEN DATABASE
        DB database = DCSet.makeDBinMemory();
        try {
            DCSet newBlockDC = dcSet.fork(database);
            int blockHeight = newBlockDC.getBlockMap().size() + 1;

            //Block waitWin;
            int counter = 0;
            int totalBytes = 0;

            long start = System.currentTimeMillis();

            TransactionMap map = dcSet.getTransactionTab();

            try {
                Iterator<Long> iterator = map.getTimestampIterator(false);
                LOGGER.debug("get ITERATOR for Remove = " + (System.currentTimeMillis() - start) + " ms");

                needRemoveInvalids = new ArrayList<byte[]>();

                this.setMonitorStatusBefore("checkForRemove");

                while (iterator.hasNext()) {

                    if (ctrl.isOnStopping()) {
                        return;
                    }

                    Transaction transaction = map.get(iterator.next());

                    if (transaction.getTimestamp() > timestamp)
                        break;

                    transaction.setDC(newBlockDC, Transaction.FOR_NETWORK, blockHeight, counter + 1);

                    if (false // тут уже все проверено внутри нашей базы
                            && !transaction.isSignatureValid(newBlockDC)) {
                        needRemoveInvalids.add(transaction.getSignature());
                        continue;
                    }

                    try {

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

            } catch (java.lang.Throwable e) {
                if (e instanceof java.lang.IllegalAccessError) {
                    // налетели на закрытую таблицу
                } else {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            this.setMonitorStatusAfter();

            LOGGER.debug("get check for Remove = " + (System.currentTimeMillis() - start) + "ms for trans: " + map.size()
                    + " needRemoveInvalids:" + needRemoveInvalids.size());

        } finally {
            database.close();
        }

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

        /**
         * время пинга если идет синхронизация например
         */
        long pointPing = 0;
        long timeTmp;
        long timePoint = 0;
        long timePointForValidTX = 0;
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
        long pointLogGoUpdate = 0;
        long pointLogWaitFlush = 0;

        this.initMonitor();

        Random random = new Random();
        if (BlockChain.TEST_DB > 0) {

            // REST balances! иначе там копится размер таблицы
            //dcSet.getAssetBalanceMap().clear();

            byte[] seed = Crypto.getInstance().digest("test24243k2l3j42kl43j".getBytes());
            byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
            BigDecimal balance = new BigDecimal("10000");

            for (int nonce = 0; nonce < BlockChain.TEST_DB_ACCOUNTS.length; nonce++) {
                BlockChain.TEST_DB_ACCOUNTS[nonce] = new PrivateKeyAccount(Wallet.generateAccountSeed(seed, nonce));
                // SET BALANCES
                BlockChain.TEST_DB_ACCOUNTS[nonce].changeBalance(dcSet, false, 2, balance, true);
            }
        }

        while (!ctrl.isOnStopping()) {

            int timeStartBroadcast = BlockChain.WIN_TIMEPOINT(height);

            Block waitWin = null;
            Block generatedBlock;
            Block solvingBlock;
            Peer peer = null;
            Tuple3<Integer, Long, Peer> maxPeer;
            SignaturesMessage response;

            try {
                Thread.sleep(WAIT_STEP_MS);
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
                // на самом деле они сами присылают свое состояние после апдейта
                if (BlockChain.TEST_DB > 0) {
                    pointPing = 0;
                } else if (NTP.getTime() - pointPing > BlockChain.GENERATING_MIN_BLOCK_TIME_MS(height) >> 1) {
                    // нужно просмотривать пиги для синхронизации так же - если там -ХХ то не будет синхронизации
                    pointPing = NTP.getTime();
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

                    if (BlockChain.NOT_STORE_REFFS_HISTORY || BlockChain.CHECK_DOUBLE_SPEND_DEEP != 0) {
                        // TODO тут нужно обновить за последние 3-10 блоков значения в
                        ReferenceMapImpl map = dcSet.getReferenceMap();

                        return;
                    }


                    this.orphanto = 0;
                    ctrl.checkStatusAndObserve(0);

                    this.setMonitorStatusAfter();

                }

                timeTmp = bchain.getTimestamp(dcSet) + BlockChain.GENERATING_MIN_BLOCK_TIME_MS(height);
                if (timeTmp > NTP.getTime())
                    continue;

                if (timePoint != timeTmp) {
                    timePoint = timeTmp;
                    timePointForValidTX = timePoint - BlockChain.UNCONFIRMED_SORT_WAIT_MS(height);
                    betterPeer = null;

                    Timestamp timestampPoit = new Timestamp(timePoint);
                    LOGGER.info("+ + + + + START GENERATE POINT on " + timestampPoit + " for UTX time: " + new Timestamp(timePointForValidTX));
                    this.setMonitorStatus("+ + + + + START GENERATE POINT on " + timestampPoit);

                    flushPoint = timePoint + BlockChain.FLUSH_TIMEPOINT(height);
                    this.solvingReference = null;
                    local_status = 0;

                    // пинганем тут все чтобы знать кому слать вобедный блок
                    // а так же чтобы знать с кем мы в синхре или кто лучше нас в checkWeightPeers
                    pointPing = NTP.getTime();
                    ctrl.pingAllPeers(true);

                }

                // is WALLET
                if (BlockChain.TEST_DB > 0 || ctrl.doesWalletExists()) {

                    if (timePoint > NTP.getTime()) {
                        continue;
                    }

                    local_status = 41;
                    this.setMonitorStatus("local_status " + viewStatus());

                    //CHECK IF WE HAVE CONNECTIONS and READY to GENERATE
                    // если на 1 высота выше хотябы то переходим на синхронизацию
                    // поэтому сдвиг = 0
                    ctrl.checkStatusAndObserve(0);

                    if (BlockChain.TEST_DB == 0 && forgingStatus == ForgingStatus.FORGING_WAIT
                            && (timePoint + (BlockChain.GENERATING_MIN_BLOCK_TIME_MS(height) << 1) < NTP.getTime()
                            || (BlockChain.ERA_COMPU_ALL_UP || BlockChain.DEVELOP_USE) && height < 100
                            || height < 10)) {

                        setForgingStatus(ForgingStatus.FORGING);
                    }

                    if (BlockChain.TEST_DB > 0 ||
                            (forgingStatus == ForgingStatus.FORGING // FORGING enabled
                                    && betterPeer == null && !ctrl.needUpToDate()
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

                        if (BlockChain.TEST_DB == 0) {

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

                            if (BlockChain.CHECK_BUGS > 7) {
                                Tuple2<List<Transaction>, Integer> unconfirmedTransactions
                                        = getUnconfirmedTransactions(height, timePointForValidTX,
                                        bchain, winned_winValue);
                            }
                        } else if (!BlockChain.STOP_GENERATE_BLOCKS) {
                            /// тестовый аккаунт
                            acc_winner = BlockChain.TEST_DB_ACCOUNTS[random.nextInt(BlockChain.TEST_DB_ACCOUNTS.length)];
                            /// закатем в очередь транзакции
                            testTransactions(height, timePointForValidTX);
                        }

                        if (!BlockChain.STOP_GENERATE_BLOCKS && acc_winner != null) {

                            if (ctrl.isOnStopping()) {
                                local_status = -1;
                                return;
                            }

                            newWinner = false;
                            // Соберем тут транзакции сразу же чтобы потом не тратить время
                            Tuple2<List<Transaction>, Integer> unconfirmedTransactions
                                    = getUnconfirmedTransactions(height, timePointForValidTX,
                                    bchain, winned_winValue);

                            if (BlockChain.TEST_DB == 0) {

                                wait_new_block_broadcast = (timeStartBroadcast + BlockChain.FLUSH_TIMEPOINT(height)) >> 1;
                                int shiftTime = (int) (((wait_new_block_broadcast * (previousTarget - winned_winValue) * 10) / previousTarget));
                                wait_new_block_broadcast = wait_new_block_broadcast + shiftTime;

                                // сдвиг на заранее - только на 1/4 максимум
                                if (wait_new_block_broadcast < timeStartBroadcast) {
                                    wait_new_block_broadcast = timeStartBroadcast;
                                } else if (wait_new_block_broadcast > BlockChain.FLUSH_TIMEPOINT(height)) {
                                    wait_new_block_broadcast = BlockChain.FLUSH_TIMEPOINT(height);
                                }

                                if (wait_new_block_broadcast > 0
                                        // и мы не отстаем
                                        && NTP.getTime() < timePoint + wait_new_block_broadcast) {

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
                                            local_status = -1;
                                            return;
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
                                    while (this.orphanto <= 0 && wait_step-- > 0
                                            && NTP.getTime() < timePoint + wait_new_block_broadcast
                                            && betterPeer == null && !ctrl.needUpToDate());
                                }

                            }

                            if (this.orphanto > 0) {
                                continue;
                            } else if (ctrl.needUpToDate()) {
                                LOGGER.info("skip GENERATING block - need UPDATE");
                            } else if (betterPeer != null) {
                                LOGGER.info("skip GENERATING block - better PERR founf: " + betterPeer);
                            } else {

                                if (newWinner) {
                                    LOGGER.info("NEW WINER RECEIVED - drop my block");
                                } else {
                                    /////////////////////    MAKING NEW BLOCK  //////////////////////
                                    local_status = 7;
                                    this.setMonitorStatus("local_status " + viewStatus());

                                    // GET VALID UNCONFIRMED RECORDS for current TIMESTAMP
                                    LOGGER.info("GENERATE my BLOCK for TXs: " + unconfirmedTransactions.b);

                                    generatedBlock = null;
                                    try {
                                        processTiming = System.nanoTime();
                                        generatedBlock = generateNextBlock(acc_winner, solvingBlock,
                                                unconfirmedTransactions,
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
                                        ctrl.stopAll(234);
                                        return;
                                    }
                                    LOGGER.info("GENERATE done");

                                    solvingBlock = null;

                                    if (generatedBlock == null) {
                                        if (ctrl.isOnStopping()) {
                                            this.local_status = -1;
                                            return;
                                        }

                                        LOGGER.info("generateNextBlock is NULL... try wait");
                                        try {
                                            Thread.sleep(1000);
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
                                            ctrl.stopAll(235);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                height = bchain.getHeight(dcSet);

                ////////////////////////////  FLUSH NEW BLOCK /////////////////////////
                // сдвиг 0 делаем
                ctrl.checkStatusAndObserve(0);
                if (betterPeer != null || orphanto > 0
                        || timePoint + BlockChain.GENERATING_MIN_BLOCK_TIME_MS(height) < NTP.getTime()
                        && ctrl.needUpToDate()) {

                    if (System.currentTimeMillis() - pointLogWaitFlush > BlockChain.GENERATING_MIN_BLOCK_TIME_MS(height) >> 2 ) {
                        pointLogWaitFlush = System.currentTimeMillis();
                        LOGGER.info("To late for FLUSH - need UPDATE !");
                    }
                } else {

                    // try solve and flush new block from Win Buffer

                    // FLUSH WINER to DB MAP
                    if (this.solvingReference != null)
                        if (System.currentTimeMillis() - pointLogWaitFlush > BlockChain.GENERATING_MIN_BLOCK_TIME_MS(height) >> 2 ) {
                            pointLogWaitFlush = System.currentTimeMillis();
                            LOGGER.info("wait to FLUSH WINER to DB MAP " + (flushPoint - NTP.getTime()) / 1000);
                        }

                    // ждем основное время просто
                    while (BlockChain.TEST_DB == 0 && this.orphanto <= 0 && flushPoint > NTP.getTime() && betterPeer == null && !ctrl.needUpToDate()) {
                        try {
                            Thread.sleep(WAIT_STEP_MS);
                        } catch (InterruptedException e) {
                            local_status = -1;
                            return;
                        }

                        if (ctrl.isOnStopping()) {
                            local_status = -1;
                            return;
                        }
                    }

                    if (this.orphanto > 0)
                        continue;

                    // если нет ничего в буфере то еще несного подождем
                    do {

                        waitWin = bchain.getWaitWinBuffer();
                        if (waitWin != null) {
                            break;
                        }

                        try {
                            Thread.sleep(WAIT_STEP_MS);
                        } catch (InterruptedException e) {
                            local_status = -1;
                            return;
                        }

                        if (ctrl.isOnStopping()) {
                            local_status = -1;
                            return;
                        }
                    } while (this.orphanto <= 0
                            && timePoint + BlockChain.GENERATING_MIN_BLOCK_TIME_MS(height) > NTP.getTime()
                            // возможно уже надо обновиться - мы отстали
                            && betterPeer == null
                            && !ctrl.needUpToDate());

                    if (this.orphanto > 0)
                        continue;

                    if (waitWin == null) {
                        if (this.solvingReference != null) {
                            if (System.currentTimeMillis() - pointLogGoUpdate > BlockChain.GENERATING_MIN_BLOCK_TIME_MS(height) >> 2 ) {
                                pointLogGoUpdate = System.currentTimeMillis();
                                LOGGER.debug("WIN BUFFER is EMPTY - go to UPDATE");
                                // обнулим - чтобы потом сработало новое создание
                                this.solvingReference = null;
                            }
                        }

                    } else if (ctrl.needUpToDate()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            local_status = -1;
                            return;
                        }
                        LOGGER.debug("need UPDATE! skip FLUSH BLOCK");
                    } else if (betterPeer != null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            local_status = -1;
                            return;
                        }
                        LOGGER.debug("found better PEER! skip FLUSH BLOCK " + betterPeer);
                    } else {
                        // только если мы не отстали

                        this.solvingReference = null;

                        local_status = 1;
                        this.setMonitorStatus("local_status " + viewStatus());

                        // FLUSH WINER to DB MAP
                        LOGGER.info("TRY to FLUSH WINER to DB MAP");

                        try {
                            if (BlockChain.TEST_DB == 0 && flushPoint + BlockChain.GENERATING_MIN_BLOCK_TIME_MS(height) < NTP.getTime()) {
                                try {
                                    // если вдруг цепочка встала,, то догоняем не очень быстро чтобы принимать все
                                    // победные блоки не спеша
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    local_status = -1;
                                    return;
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
                                ctrl.stopAll(235);
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
                            checkForRemove(timePointForValidTX);
                            clearInvalids();
                        }

                        // была обработка буфера, тогда на точку начала вернемся
                        continue;
                    }

                }

                ////////////////////////// UPDATE ////////////////////

                if (orphanto > 0 || betterPeer == null &&
                        timePoint + BlockChain.GENERATING_MIN_BLOCK_TIME_MS(height) > NTP.getTime())
                    continue;

                /// CHECK PEERS HIGHER
                // так как в девелопе все гоняют свои цепочки то посмотреть самыю жирную а не длинную
                ctrl.checkStatusAndObserve(shift_height);
                //CHECK IF WE ARE NOT UP TO DATE
                if (betterPeer != null || ctrl.needUpToDate()) {

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
            } catch (Exception e) {
                if (ctrl.isOnStopping()) {
                    this.local_status = -1;
                    return;
                }

                LOGGER.error(e.getMessage(), e);

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
