package org.erachain.controller;

import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.exdata.ExData;
import org.erachain.core.exdata.exActions.ExListPays;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.transaction.RSignNote;
import org.erachain.core.transaction.Transaction;
import org.erachain.database.DPSet;
import org.erachain.database.FPoolBalancesMap;
import org.erachain.database.FPoolBlocksMap;
import org.erachain.database.FPoolMap;
import org.erachain.datachain.CreditAddressesMap;
import org.erachain.datachain.DCSet;
import org.erachain.dbs.IteratorCloseable;
import org.erachain.dbs.IteratorCloseableImpl;
import org.erachain.settings.Settings;
import org.erachain.utils.MonitoredThread;
import org.erachain.utils.SimpleFileVisitorForRecursiveFolderDeletion;
import org.mapdb.Fun;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class FPool extends MonitoredThread {

    final static int PENDING_PERIOD = 5;
    Controller controller;
    BlockChain blockChain;
    DCSet dcSet;
    DPSet dpSet;
    BigDecimal poolFee;
    String title = "Forging poll Withdraw";
    PrivateKeyAccount privateKeyAccount;

    BlockingQueue<Block> blockingQueue = new ArrayBlockingQueue<Block>(3);
    HashMap<Tuple2<Long, String>, BigDecimal> results;

    private boolean runned;

    private static final Logger LOGGER = LoggerFactory.getLogger(FPool.class.getSimpleName());

    FPool(Controller controller, BlockChain blockChain, DCSet dcSet, PrivateKeyAccount privateKeyAccount, String poolFee) {
        this.controller = controller;
        this.blockChain = blockChain;
        this.dcSet = dcSet;
        this.privateKeyAccount = privateKeyAccount;
        this.poolFee = new BigDecimal(poolFee).movePointLeft(2);

        this.setName("Forging Pool[" + this.getId() + "]");

        try {
            this.dpSet = DPSet.reCreateDB();
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            try {
                this.dpSet.close();
            } catch (Exception e2) {
            }

            File dir = new File(Settings.getInstance().getFPoolDir());
            // delete dir
            if (dir.exists()) {
                try {
                    Files.walkFileTree(dir.toPath(), new SimpleFileVisitorForRecursiveFolderDeletion());
                } catch (IOException e3) {
                }
            }

            this.dpSet = DPSet.reCreateDB();
        }

        this.start();

    }

    public boolean offerBlock(Block block) {
        return blockingQueue.offer(block);
    }

    private boolean balanceReady(Long assteKey, BigDecimal balance) {
        switch ((int) (long) assteKey) {
            case (int) AssetCls.ERA_KEY:
                return balance.compareTo(new BigDecimal("1")) > 0;
            case (int) AssetCls.FEE_KEY:
                return balance.compareTo(new BigDecimal("0.01")) > 0;
            case (int) AssetCls.BTC_KEY:
                return balance.compareTo(new BigDecimal("0.0001")) > 0;
            default:
                return balance.compareTo(new BigDecimal("0.1")) > 0;
        }

    }

    private void addRewards(AssetCls asset, BigDecimal totalEarn, BigDecimal totalForginAmount, HashMap<String, BigDecimal> credits) {
        BigDecimal amount;
        int scale = asset.getScale() + 6;
        Tuple2<Long, String> key;
        for (String address : credits.keySet()) {
            amount = totalEarn.multiply(credits.get(address)).divide(totalForginAmount, scale, RoundingMode.DOWN);

            key = new Tuple2<>(asset.getKey(), address);

            if (results.containsKey(key)) {
                results.put(key, results.get(key).add(amount));
            } else {
                results.put(key, amount);
            }
        }

    }

    private boolean processMessage(Block block) {

        if (block == null
                || !privateKeyAccount.equals(block.getCreator()))
            return false;

        PublicKeyAccount forger = block.getCreator();
        BigDecimal feeEarn = BigDecimal.valueOf(block.blockHead.totalFee + block.blockHead.emittedFee, BlockChain.FEE_SCALE);
        BigDecimal totalEmite = BigDecimal.ZERO;
        HashMap<AssetCls, Fun.Tuple2<BigDecimal, BigDecimal>> earnedAllAssets = block.getEarnedAllAssets();

        BigDecimal totalForginAmount = forger.getBalanceUSE(AssetCls.FEE_KEY);

        // make table of credits
        CreditAddressesMap creditMap = dcSet.getCredit_AddressesMap();
        HashMap<String, BigDecimal> credits = new HashMap();
        try (IteratorCloseable<Tuple3<String, Long, String>> iterator = creditMap.getIterator(new Tuple3<String, Long, String>
                (forger.getAddress(), AssetCls.FEE_KEY, ""), false)) {

            Tuple3<String, Long, String> key;
            BigDecimal creditAmount;
            while (iterator.hasNext()) {
                key = iterator.next();

                if (!forger.equals(key.a) || !key.b.equals(AssetCls.FEE_KEY))
                    break;

                creditAmount = creditMap.get(key);
                if (creditAmount.signum() <= 0)
                    continue;

                credits.put(key.c, creditAmount);
            }

        } catch (IOException e) {
            return false;
        }

        results = new HashMap<>();

        // FOR FEE
        addRewards(BlockChain.FEE_ASSET, feeEarn, totalForginAmount, credits);

        if (false) {
            // FOR ERA
            addRewards(BlockChain.ERA_ASSET, totalEmite, totalForginAmount, credits);
        }

        if (credits.size() == 0)
            return true;

        // for all ERNAED assets
        for (AssetCls assetEran : earnedAllAssets.keySet()) {
            Fun.Tuple2<BigDecimal, BigDecimal> item = earnedAllAssets.get(assetEran);
            addRewards(assetEran, item.a, totalForginAmount, credits);
        }

        dpSet.getBlocksMap().put(block.heightBlock, new Object[]{block.getSignature(), results});

        return true;

    }

    private void checkPending() {

        FPoolBlocksMap blocksMap = dpSet.getBlocksMap();
        FPoolBalancesMap balsMap = dpSet.geBbalancesMap();
        try (IteratorCloseable<Integer> iterator = IteratorCloseableImpl.make(blocksMap.getIterator())) {
            Integer height;
            while (iterator.hasNext()) {
                height = iterator.next();
                if (height + PENDING_PERIOD > controller.getMyHeight())
                    break;

                HashMap<Tuple2<Long, String>, BigDecimal> blockResults;
                Object[] item = blocksMap.get(height);
                if (dcSet.getBlockSignsMap().contains((byte[]) item[0])) {
                    blockResults = (HashMap<Tuple2<Long, String>, BigDecimal>) item[1];
                    for (Tuple2<Long, String> key : blockResults.keySet()) {
                        if (balsMap.contains(key)) {
                            balsMap.put(key, balsMap.get(key).add(results.get(key)));
                        } else {
                            balsMap.put(key, results.get(key));
                        }
                    }
                } else {
                    // block was orphaned
                    blocksMap.remove(height);
                }

            }
        } catch (IOException e) {
        }

    }

    private void payout() {
        FPoolBalancesMap balsMap = dpSet.geBbalancesMap();

        Long assetKeyToWithdraw = null;
        List<Tuple2<String, BigDecimal>> payouts = new ArrayList();
        try (IteratorCloseable<Tuple2<Long, String>> iterator = IteratorCloseableImpl.make(balsMap.getIterator())) {
            Tuple2<Long, String> key;
            Long assetKey;
            BigDecimal balance;

            while (iterator.hasNext()) {
                key = iterator.next();
                assetKey = key.a;
                if (assetKeyToWithdraw != null && !assetKeyToWithdraw.equals(assetKey)) {
                    // уже собрали массив выплат по данному активу - выходим
                    break;
                }

                balance = balsMap.get(key);
                if (!balanceReady(assetKey, balance)) {
                    continue;
                }

                // BALANCE is GOOD for WITHDRAW
                payouts.add(new Tuple2(key.b, balance));
                if (assetKeyToWithdraw == null)
                    assetKeyToWithdraw = assetKey;

            }
        } catch (IOException e) {
        }

        if (assetKeyToWithdraw == null) {
            return;
        }

        Tuple3<byte[], BigDecimal, String>[] addresses = new Tuple3[payouts.size()];
        int index = 0;
        Crypto crypto = Crypto.getInstance();
        for (Tuple2<String, BigDecimal> item : payouts) {
            addresses[index] = new Tuple3<byte[], BigDecimal, String>(crypto.getShortBytesFromAddress(item.a), item.b, "");
        }

        /// MAKE WITHDRAW
        byte[] flags = new byte[]{3, 0, 0, 0};
        ExListPays listPays = new ExListPays(0, assetKeyToWithdraw, Account.BALANCE_POS_OWN, false, addresses);
        ExData exData = new ExData(flags, null, listPays, title, (byte) 0, null, (byte) 0, null,
                (byte) 0, null, null, null, null);

        byte version = (byte) 3;
        byte property1 = (byte) 0;
        byte property2 = (byte) 0;
        byte feePow = 0;
        RSignNote issueDoc = null;
        try {
            issueDoc = (RSignNote) Controller.getInstance().r_SignNote(version, property1, property2,
                    privateKeyAccount, feePow, 0, exData.toByte());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        int validate = Controller.getInstance().getTransactionCreator().afterCreate(issueDoc, Transaction.FOR_NETWORK, false, false);
        if (validate != Transaction.VALIDATE_OK) {
            LOGGER.error(issueDoc.makeErrorJSON2(validate).toJSONString());
            return;
        }

        /// RESET BALANCCES for
        for (Tuple2<String, BigDecimal> item : payouts) {
            balsMap.put(new Tuple2<>(assetKeyToWithdraw, item.a), BigDecimal.ZERO);
        }
    }

    @Override
    public void run() {

        runned = true;

        FPoolMap map = dpSet.getFPoolMap();

        while (runned) {

            // PROCESS
            try {
                processMessage(blockingQueue.poll(BlockChain.GENERATING_MIN_BLOCK_TIME(0), TimeUnit.SECONDS));

                checkPending();

                payout();

            } catch (OutOfMemoryError e) {
                blockingQueue = null;
                LOGGER.error(e.getMessage(), e);
                dpSet.close();
                Controller.getInstance().stopAndExit(2457);
                return;
            } catch (IllegalMonitorStateException e) {
                blockingQueue = null;
                dpSet.close();
                Controller.getInstance().stopAndExit(2458);
                return;
            } catch (InterruptedException e) {
                blockingQueue = null;
                break;
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } catch (Throwable e) {
                LOGGER.error(e.getMessage(), e);
            }

        }

        dpSet.close();
        LOGGER.info("Forging Pool halted");

    }

    public void halt() {
        this.runned = false;
        interrupt();
    }

}