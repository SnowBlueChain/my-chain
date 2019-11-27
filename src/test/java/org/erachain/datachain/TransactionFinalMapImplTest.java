package org.erachain.datachain;

import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.erachain.core.account.Account;
import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.block.GenesisBlock;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.transaction.IssueAssetTransaction;
import org.erachain.core.transaction.RSend;
import org.erachain.core.transaction.Transaction;
import org.erachain.database.IDB;
import org.erachain.ntp.NTP;
import org.erachain.settings.Settings;
import org.erachain.utils.SimpleFileVisitorForRecursiveFolderDeletion;
import org.junit.Test;
import org.mapdb.Fun;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Random;

import static org.junit.Assert.assertEquals;

@Slf4j
public class TransactionFinalMapImplTest {

    int[] TESTED_DBS = new int[]{
            IDB.DBS_MAP_DB,
            IDB.DBS_ROCK_DB
    };

    Random random = new Random();
    long flags = 0l;
    int seqNo = 0;

    Fun.Tuple5<Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>> balanceA;
    Fun.Tuple5<Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>, Fun.Tuple2<BigDecimal, BigDecimal>> balanceB;
    DCSet dcSet;
    GenesisBlock gb;

    PrivateKeyAccount accountA;
    PrivateKeyAccount accountB;
    IssueAssetTransaction issueAssetTransaction;
    AssetCls assetA;
    long keyA;
    AssetCls assetB;
    long keyB;

    long ERM_KEY = Transaction.RIGHTS_KEY;
    long FEE_KEY = Transaction.FEE_KEY;
    byte FEE_POWER = (byte) 0;
    byte[] assetReference = new byte[64];
    byte[] invalidSign = new byte[64];

    long timestamp;

    private void init(int dbs) {

        logger.info(" ********** open DBS: " + dbs);


        File tempDir = new File(Settings.getInstance().getDataTempDir());
        try {
            Files.walkFileTree(tempDir.toPath(), new SimpleFileVisitorForRecursiveFolderDeletion());
        } catch (Throwable e) {
        }

        dcSet = DCSet.createEmptyHardDatabaseSetWithFlush(null, dbs);
        gb = new GenesisBlock();

        try {
            gb.process(dcSet);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        byte[] seed = Crypto.getInstance().digest("test_A".getBytes());
        byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
        accountA = new PrivateKeyAccount(privateKey);
        seed = Crypto.getInstance().digest("test_B".getBytes());
        privateKey = Crypto.getInstance().createKeyPair(seed).getA();
        accountB = new PrivateKeyAccount(privateKey);

        // FEE FUND
        accountA.setLastTimestamp(new long[]{gb.getTimestamp(), 0}, dcSet);
        accountA.changeBalance(dcSet, false, ERM_KEY, BigDecimal.valueOf(100), false);
        accountA.changeBalance(dcSet, false, FEE_KEY, BigDecimal.valueOf(10), false);

        accountB.setLastTimestamp(new long[]{gb.getTimestamp(), 0}, dcSet);
        accountB.changeBalance(dcSet, false, ERM_KEY, BigDecimal.valueOf(100), false);
        accountB.changeBalance(dcSet, false, FEE_KEY, BigDecimal.valueOf(10), false);

        timestamp = NTP.getTime();
    }

    @Test
    public void findTransactions() {
    }

    @Test
    public void findTransactionsCount() {
    }

    @Test
    public void findTransactionsKeys() {
        //192.168.1.156:9047/apirecords/find?address=7PXf6Bk9m7uLrC9ATTHPyEtxRkCeeWDG3b&type=31&startblock=0

        for (int dbs : TESTED_DBS) {

            try {
                init(dbs);

                String address = "7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW";
                String sender = null;
                String recipient = null;
                int minHeight = 0;
                int maxHeight = 0;
                int type = 0;
                int service = 0;
                boolean desc = false;
                int offset = 0;
                int limit = 0;

                Account recipientAcc = new Account(address);
                BigDecimal amount_asset = new BigDecimal("1");

                RSend assetTransfer = new RSend(accountA, FEE_POWER, recipientAcc, 1, amount_asset, timestamp++, 0L);
                assetTransfer.sign(accountA, Transaction.FOR_NETWORK);
                assetTransfer.setDC(dcSet, Transaction.FOR_NETWORK, 1, 1);
                assetTransfer.process(gb, Transaction.FOR_NETWORK);

                assetTransfer = new RSend(accountB, FEE_POWER, accountA, 1, amount_asset, timestamp++, 0L);
                assetTransfer.sign(accountA, Transaction.FOR_NETWORK);
                assetTransfer.setDC(dcSet, Transaction.FOR_NETWORK, 1, 1);
                assetTransfer.process(gb, Transaction.FOR_NETWORK);

                Iterator<Long> iterator = dcSet.getTransactionFinalMap().findTransactionsKeys(accountA.getAddress(),
                        sender, recipient, minHeight,
                        maxHeight, type, service, desc, offset, limit);

                assertEquals(2, Iterators.size(iterator));


            } finally {
                dcSet.close();
            }
        }


    }
}