package test.records;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.mapdb.Fun.Tuple5;

import controller.Controller;
import core.BlockChain;
import core.account.PrivateKeyAccount;
import core.block.GenesisBlock;
import core.crypto.Crypto;
import core.item.assets.AssetCls;
import core.item.assets.AssetVenture;
import core.transaction.IssueAssetTransaction;
import core.transaction.R_Send;
import core.transaction.Transaction;
import datachain.DCSet;
import ntp.NTP;

public class TestRec_Send_InSideAsset {

    static Logger LOGGER = Logger.getLogger(TestRec_Send_InSideAsset.class.getName());

    Long releaserReference = null;

    long FEE_KEY = AssetCls.FEE_KEY;
    byte FEE_POWER = (byte) 0;
    byte[] assetReference = new byte[64];
    long timestamp = NTP.getTime();

    byte version = 2;
    byte prop2 = 0;    
    byte prop1_backward = core.transaction.TransactionAmount.BACKWARD_MASK;

    Tuple3<String, Long, String> creditKey;

    long flags = 0l;
    Controller cntrl;
    //CREATE KNOWN ACCOUNT
    byte[] privateKey_1 = Crypto.getInstance().createKeyPair(Crypto.getInstance().digest("tes213sdffsdft".getBytes())).getA();
    PrivateKeyAccount creditor = new PrivateKeyAccount(privateKey_1);
    byte[] privateKey_2 = Crypto.getInstance().createKeyPair(Crypto.getInstance().digest("tes213sdffsdft2".getBytes())).getA();
    PrivateKeyAccount emitter = new PrivateKeyAccount(privateKey_2);
    byte[] privateKey_3 = Crypto.getInstance().createKeyPair(Crypto.getInstance().digest("tes213sdffsdft3".getBytes())).getA();
    PrivateKeyAccount debtor = new PrivateKeyAccount(privateKey_3);

    AssetCls asset;
    AssetCls assetInSide;
    
    long assetKey;
    int scale = 3;
    
    R_Send r_Send;
    Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
        creditorBalance;
    Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
        emitterBalance;
    Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
        debtorBalance;
    
    private byte[] icon = new byte[]{1, 3, 4, 5, 6, 9}; // default value
    private byte[] image = new byte[]{4, 11, 32, 23, 45, 122, 11, -45}; // default value
    //CREATE EMPTY MEMORY DATABASE
    private DCSet db;
    private GenesisBlock gb;
    private BlockChain bchain;

    // INIT ASSETS
    private void init(boolean withIssue) {

        db = DCSet.createEmptyDatabaseSet();
        cntrl = Controller.getInstance();
        cntrl.initBlockChain(db);
        bchain = cntrl.getBlockChain();
        gb = bchain.getGenesisBlock();
        //gb.process(db);

        // FEE FUND
        creditor.setLastTimestamp(gb.getTimestamp(db), db);
        creditor.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1), false);

        emitter.setLastTimestamp(gb.getTimestamp(db), db);
        emitter.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1), false);

        asset = new AssetVenture(creditor, "aasdasd", icon, image, "asdasda", AssetCls.AS_INSIDE_ASSETS, 8, 50000l);
        // set SCALABLE assets ++
        asset.insertToMap(db, BlockChain.AMOUNT_SCALE_FROM);
        asset.insertToMap(db, 0l);

        assetInSide = new AssetVenture(emitter, "inSide Asset", icon, image, "...", AssetCls.AS_INSIDE_ASSETS, scale, 500l);

        if (withIssue) {
    
            //CREATE ISSUE ASSET TRANSACTION
            IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(emitter, assetInSide, FEE_POWER, ++timestamp, 0l);
            issueAssetTransaction.sign(emitter, false);
            issueAssetTransaction.setDC(db, false);
            issueAssetTransaction.process(gb, false);
    
            assetKey = assetInSide.getKey(db);
        }

    }
  
    /////////////////////////////////////////////
    ////////////
    @Test
    public void validate_R_Send_Movable_Asset() {

        init(true);

        emitterBalance = emitter.getBalance(db, assetKey);

        //CHECK BALANCE SENDER
        assertEquals(BigDecimal.valueOf(500), emitterBalance.a.a);
        assertEquals(BigDecimal.valueOf(500), emitterBalance.a.b);
        assertEquals(BigDecimal.valueOf(0), emitterBalance.b.a);
        assertEquals(BigDecimal.valueOf(0), emitterBalance.b.b);
        assertEquals(BigDecimal.valueOf(0), emitterBalance.c.a);
        assertEquals(BigDecimal.valueOf(0), emitterBalance.c.b);

        //CREATE ASSET TRANSFER
        
        // INVALID
        r_Send = new R_Send(emitter, FEE_POWER, debtor, assetKey, BigDecimal.valueOf(1000),
                "", null, new byte[]{1}, new byte[]{1},
                ++timestamp, 0l);
        r_Send.setDC(db, false);
        assertEquals(r_Send.isValid(releaserReference, flags), Transaction.NO_BALANCE);

        r_Send = new R_Send(emitter, FEE_POWER, creditor, assetKey, BigDecimal.valueOf(50),
                "", null, new byte[]{1}, new byte[]{1},
                ++timestamp, 0l);
        r_Send.setDC(db, false);
        assertEquals(r_Send.isValid(releaserReference, flags), Transaction.VALIDATE_OK);

        r_Send.sign(emitter, false);
        r_Send.setDC(db, false);
        r_Send.process(gb, false);

        emitterBalance = emitter.getBalance(db, assetKey);

        //CHECK BALANCE SENDER
        assertEquals(BigDecimal.valueOf(500), emitterBalance.a.a);
        assertEquals(BigDecimal.valueOf(450), emitterBalance.a.b);

        assertEquals(BigDecimal.valueOf(0), emitterBalance.b.a);
        assertEquals(BigDecimal.valueOf(0), emitterBalance.b.b);

        //CHECK BALANCE RECIPIENT
        creditorBalance = creditor.getBalance(db, assetKey);

        //CHECK BALANCE SENDER
        assertEquals(BigDecimal.valueOf(50), creditorBalance.a.a);
        assertEquals(BigDecimal.valueOf(50), creditorBalance.a.b);

        assertEquals(BigDecimal.valueOf(0), creditorBalance.b.a);
        assertEquals(BigDecimal.valueOf(0), creditorBalance.b.b);

        //////////////////////////////////////////////////
        /// ORPHAN
        /////////////////////////////////////////////////
        r_Send.orphan(false);

        //CHECK BALANCE SENDER
        emitterBalance = emitter.getBalance(db, assetKey);
        assertEquals(BigDecimal.valueOf(500), emitterBalance.a.a);
        assertEquals(BigDecimal.valueOf(500), emitterBalance.a.b);

        //CHECK BALANCE RECIPIENT
        creditorBalance = creditor.getBalance(db, assetKey);
        assertEquals(BigDecimal.valueOf(0), creditorBalance.a.a);
        assertEquals(BigDecimal.valueOf(0), creditorBalance.a.b);

        // BACK PROCESS
        r_Send.process(gb, false);

        // INVALID
        r_Send = new R_Send(
                debtor, FEE_POWER, emitter, -assetKey, BigDecimal.valueOf(10),
                "", null, new byte[]{1}, new byte[]{1},
                ++timestamp, 0l);
        r_Send.setDC(db, false);
        assertEquals(r_Send.isValid(releaserReference, flags), Transaction.NO_BALANCE);

        // INVALID
        r_Send = new R_Send(
                version,
                prop1_backward,
                prop2,
                debtor, FEE_POWER, emitter, -assetKey, BigDecimal.valueOf(10),
                "", null, new byte[]{1}, new byte[]{1},
                ++timestamp, 0l);
        r_Send.setDC(db, false);
        assertEquals(r_Send.isValid(releaserReference, flags), Transaction.NO_DEBT_BALANCE);

        // INVALID
        r_Send = new R_Send(
                creditor, FEE_POWER, debtor, -assetKey, BigDecimal.valueOf(100),
                "", null, new byte[]{1}, new byte[]{1},
                ++timestamp, 0l);
        r_Send.setDC(db, false);
        assertEquals(r_Send.isValid(releaserReference, flags), Transaction.NO_BALANCE);

        // GET CREDIT - дать в кредит актив
        r_Send = new R_Send(
                creditor, FEE_POWER, debtor, -assetKey, BigDecimal.valueOf(10),
                "", null, new byte[]{1}, new byte[]{1},
                ++timestamp, 0l);
        r_Send.setDC(db, false);
        assertEquals(r_Send.isValid(releaserReference, flags), Transaction.VALIDATE_OK);

        r_Send.sign(emitter, false);
        r_Send.setDC(db, false);
        r_Send.process(gb, false);

        creditKey = new Tuple3<String, Long, String>(creditor.getAddress(), assetKey, debtor.getAddress());
        assertEquals(BigDecimal.valueOf(10), db.getCredit_AddressesMap().get(creditKey));

        //CHECK BALANCE CREDITOR
        creditorBalance = creditor.getBalance(db, assetKey);
        assertEquals(BigDecimal.valueOf(50), creditorBalance.a.a);
        assertEquals(BigDecimal.valueOf(50), creditorBalance.a.b);

        assertEquals(BigDecimal.valueOf(0), creditorBalance.b.a);
        assertEquals(BigDecimal.valueOf(-10), creditorBalance.b.b);
        assertEquals(BigDecimal.valueOf(40), creditor.getBalanceUSE(assetKey, db));

        //CHECK BALANCE DEBTOR
        debtorBalance = debtor.getBalance(db, assetKey);
        assertEquals(BigDecimal.valueOf(0), debtorBalance.a.a);
        assertEquals(BigDecimal.valueOf(0), debtorBalance.a.b);

        assertEquals(BigDecimal.valueOf(10), debtorBalance.b.a);
        assertEquals(BigDecimal.valueOf(10), debtorBalance.b.b);

        //////////////////////////////////////////////////
        /// ORPHAN
        /////////////////////////////////////////////////
        r_Send.orphan(false);

        assertEquals(BigDecimal.valueOf(0), db.getCredit_AddressesMap().get(creditKey));

        //CHECK BALANCE CREDITOR
        creditorBalance = creditor.getBalance(db, assetKey);
        assertEquals(BigDecimal.valueOf(50), creditorBalance.a.a);
        assertEquals(BigDecimal.valueOf(50), creditorBalance.a.b);

        assertEquals(BigDecimal.valueOf(0), creditorBalance.b.a);
        assertEquals(BigDecimal.valueOf(0), creditorBalance.b.b);
        assertEquals(BigDecimal.valueOf(50), creditor.getBalanceUSE(assetKey, db));

        //CHECK BALANCE DEBTOR
        debtorBalance = debtor.getBalance(db, assetKey);
        assertEquals(BigDecimal.valueOf(0), debtorBalance.a.a);
        assertEquals(BigDecimal.valueOf(0), debtorBalance.a.b);

        assertEquals(BigDecimal.valueOf(0), debtorBalance.b.a);
        assertEquals(BigDecimal.valueOf(0), debtorBalance.b.b);

        // PROCESS BACK
        r_Send.process(gb, false);
        assertEquals(BigDecimal.valueOf(10), db.getCredit_AddressesMap().get(creditKey));

        //////////////////////
        // GET backward credit

        // INVALID
        r_Send = new R_Send(
                version,
                prop1_backward,
                prop2,
                creditor, FEE_POWER, debtor, -assetKey, BigDecimal.valueOf(20),
                "", null, new byte[]{1}, new byte[]{1},
                ++timestamp, 0l);
        r_Send.setDC(db, false);
        assertEquals(r_Send.isValid(releaserReference, flags), Transaction.NO_DEBT_BALANCE);

        // INVALID
        r_Send = new R_Send(
                version,
                prop1_backward,
                prop2,
                creditor, FEE_POWER, debtor, -assetKey, BigDecimal.valueOf(7),
                "", null, new byte[]{1}, new byte[]{1},
                ++timestamp, 0l);
        r_Send.setDC(db, false);
        assertEquals(r_Send.isValid(releaserReference, flags), Transaction.VALIDATE_OK);

        r_Send.sign(emitter, false);
        r_Send.process(gb, false);

        assertEquals(BigDecimal.valueOf(3), db.getCredit_AddressesMap().get(creditKey));

        //CHECK BALANCE CREDITOR
        creditorBalance = creditor.getBalance(db, assetKey);
        assertEquals(BigDecimal.valueOf(50), creditorBalance.a.a);
        assertEquals(BigDecimal.valueOf(50), creditorBalance.a.b);

        assertEquals(BigDecimal.valueOf(0), creditorBalance.b.a);
        assertEquals(BigDecimal.valueOf(-3), creditorBalance.b.b);
        assertEquals(BigDecimal.valueOf(47), creditor.getBalanceUSE(assetKey, db));

        //CHECK BALANCE DEBTOR
        debtorBalance = debtor.getBalance(db, assetKey);
        assertEquals(BigDecimal.valueOf(0), debtorBalance.a.a);
        assertEquals(BigDecimal.valueOf(0), debtorBalance.a.b);

        assertEquals(BigDecimal.valueOf(3), debtorBalance.b.a);
        assertEquals(BigDecimal.valueOf(3), debtorBalance.b.b);

        //////////////////////////////////////////////////
        /// ORPHAN
        /////////////////////////////////////////////////
        r_Send.orphan(false);

        assertEquals(BigDecimal.valueOf(10), db.getCredit_AddressesMap().get(creditKey));

        //CHECK BALANCE CREDITOR
        creditorBalance = creditor.getBalance(db, assetKey);
        assertEquals(BigDecimal.valueOf(50), creditorBalance.a.a);
        assertEquals(BigDecimal.valueOf(50), creditorBalance.a.b);

        assertEquals(BigDecimal.valueOf(0), creditorBalance.b.a);
        assertEquals(BigDecimal.valueOf(-10), creditorBalance.b.b);
        assertEquals(BigDecimal.valueOf(40), creditor.getBalanceUSE(assetKey, db));

        //CHECK BALANCE DEBTOR
        debtorBalance = debtor.getBalance(db, assetKey);
        assertEquals(BigDecimal.valueOf(0), debtorBalance.a.a);
        assertEquals(BigDecimal.valueOf(0), debtorBalance.a.b);

        assertEquals(BigDecimal.valueOf(10), debtorBalance.b.a);
        assertEquals(BigDecimal.valueOf(10), debtorBalance.b.b);

        // PROCESS BACK
        r_Send.process(gb, false);

    }

}
