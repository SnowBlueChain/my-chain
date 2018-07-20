package core.transaction;

import core.BlockChain;
import core.account.PrivateKeyAccount;
import core.block.GenesisBlock;
import core.crypto.Crypto;
import core.item.templates.Template;
import core.item.templates.TemplateCls;
import datachain.DCSet;
import datachain.ItemTemplateMap;
import ntp.NTP;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

//import java.math.BigInteger;
//import java.util.ArrayList;
//import java.util.List;

public class TestTemplateAsPack {

    static Logger LOGGER = Logger.getLogger(TestTemplateAsPack.class.getName());

    Long releaserReference = null;

    boolean asPack = true;
    boolean includeReference = false;
    long FEE_KEY = 1l;
    byte FEE_POWER = (byte) 1;
    byte[] templateReference = new byte[64];
    long timestamp = NTP.getTime();
    long flags = 0l;
    //CREATE KNOWN ACCOUNT
    byte[] seed = Crypto.getInstance().digest("test".getBytes());
    byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
    PrivateKeyAccount maker = new PrivateKeyAccount(privateKey);
    private byte[] icon = new byte[]{1, 3, 4, 5, 6, 9}; // default value
    private byte[] image = new byte[]{4, 11, 32, 23, 45, 122, 11, -45}; // default value
    //CREATE EMPTY MEMORY DATABASE
    private DCSet db;
    private GenesisBlock gb;

    // INIT TEMPLATES
    private void init() {

        db = DCSet.createEmptyDatabaseSet();
        gb = new GenesisBlock();
        try {
            gb.process(db);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // FEE FUND
        maker.setLastTimestamp(gb.getTimestamp(db), db);
        maker.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1).setScale(BlockChain.AMOUNT_DEDAULT_SCALE), false);

    }


    //ISSUE PLATE TRANSACTION

    @Test
    public void validateSignatureIssueTemplateTransaction() {

        init();

        //CREATE PLATE
        Template template = new Template(maker, "test", icon, image, "strontje");

        //CREATE ISSUE PLATE TRANSACTION
        Transaction issueTemplateTransaction = new IssueTemplateRecord(maker, template);
        issueTemplateTransaction.sign(maker, asPack);

        //CHECK IF ISSUE PLATE TRANSACTION IS VALID
        assertEquals(true, issueTemplateTransaction.isSignatureValid(db));

        //INVALID SIGNATURE
        issueTemplateTransaction = new IssueTemplateRecord(maker, template, new byte[64]);

        //CHECK IF ISSUE PLATE IS INVALID
        assertEquals(false, issueTemplateTransaction.isSignatureValid(db));
    }

    @Ignore
    //TODO actualize the test
    @Test
    public void parseIssueTemplateTransaction() {

        init();

        TemplateCls template = new Template(maker, "test132", icon, image, "12345678910strontje");
        byte[] raw = template.toBytes(includeReference, false);
        assertEquals(raw.length, template.getDataLength(includeReference));

        //CREATE ISSUE PLATE TRANSACTION
        IssueTemplateRecord issueTemplateRecord = new IssueTemplateRecord(maker, template);
        issueTemplateRecord.setDC(db,false);
        issueTemplateRecord.sign(maker, asPack);
        issueTemplateRecord.process(gb, asPack);

        //CONVERT TO BYTES
        byte[] rawIssueTemplateTransaction = issueTemplateRecord.toBytes(true, null);

        //CHECK DATA LENGTH
        assertEquals(rawIssueTemplateTransaction.length, issueTemplateRecord.getDataLength(asPack));

        try {
            //PARSE FROM BYTES
            IssueTemplateRecord parsedIssueTemplateTransaction = (IssueTemplateRecord) TransactionFactory.getInstance().parse(rawIssueTemplateTransaction, releaserReference);
            LOGGER.info("parsedIssueTemplateTransaction: " + parsedIssueTemplateTransaction);

            //CHECK INSTANCE
            assertEquals(true, parsedIssueTemplateTransaction instanceof IssueTemplateRecord);

            //CHECK SIGNATURE
            assertEquals(true, Arrays.equals(issueTemplateRecord.getSignature(), parsedIssueTemplateTransaction.getSignature()));

            //CHECK ISSUER
            assertEquals(issueTemplateRecord.getCreator().getAddress(), parsedIssueTemplateTransaction.getCreator().getAddress());

            //CHECK OWNER
            assertEquals(issueTemplateRecord.getItem().getOwner().getAddress(), parsedIssueTemplateTransaction.getItem().getOwner().getAddress());

            //CHECK NAME
            assertEquals(issueTemplateRecord.getItem().getName(), parsedIssueTemplateTransaction.getItem().getName());

            //CHECK DESCRIPTION
            assertEquals(issueTemplateRecord.getItem().getDescription(), parsedIssueTemplateTransaction.getItem().getDescription());

        } catch (Exception e) {
            fail("Exception while parsing transaction. " + e);
        }

    }

    @Ignore
    //TODO actualize the test
    @Test
    public void processIssueTemplateTransaction() {

        init();

        Template template = new Template(maker, "test", icon, image, "strontje");

        //CREATE ISSUE PLATE TRANSACTION
        IssueTemplateRecord issueTemplateRecord = new IssueTemplateRecord(maker, template);
        issueTemplateRecord.setDC(db,false);
        issueTemplateRecord.sign(maker, asPack);

        assertEquals(Transaction.VALIDATE_OK, issueTemplateRecord.isValid(releaserReference, flags));
        Long makerReference = maker.getLastTimestamp(db);
        issueTemplateRecord.process(gb, asPack);

        LOGGER.info("template KEY: " + template.getKey(db));

        //CHECK PLATE EXISTS SENDER
        long key = db.getIssueTemplateMap().get(issueTemplateRecord);
        assertEquals(true, db.getItemTemplateMap().contains(key));

        TemplateCls template_2 = new Template(maker, "test132_2", icon, image, "2_12345678910strontje");
        IssueTemplateRecord issueTemplateTransaction_2 = new IssueTemplateRecord(maker, template_2);
        issueTemplateTransaction_2.sign(maker, asPack);
        issueTemplateTransaction_2.process(gb, asPack);
        LOGGER.info("template_2 KEY: " + template_2.getKey(db));
        issueTemplateTransaction_2.orphan(asPack);
        ItemTemplateMap templateMap = db.getItemTemplateMap();
        int mapSize = templateMap.size();
        assertEquals(0, mapSize - 4);

        //CHECK PLATE IS CORRECT
        assertEquals(true, Arrays.equals(db.getItemTemplateMap().get(key).toBytes(includeReference, false), template.toBytes(includeReference, false)));

        //CHECK REFERENCE SENDER
        assertEquals((long) makerReference, (long) maker.getLastTimestamp(db));
    }


    @Test
    public void orphanIssueTemplateTransaction() {

        init();

        Template template = new Template(maker, "test", icon, image, "strontje");
        Long makerReference = maker.getLastTimestamp(db);

        //CREATE ISSUE PLATE TRANSACTION
        IssueTemplateRecord issueTemplateRecord = new IssueTemplateRecord(maker, template);
        issueTemplateRecord.setDC(db,false);
        issueTemplateRecord.sign(maker, asPack);
        issueTemplateRecord.process(gb, asPack);
        long key = db.getIssueTemplateMap().get(issueTemplateRecord);
        assertEquals((long) makerReference, (long) maker.getLastTimestamp(db));

        issueTemplateRecord.orphan(asPack);

        //CHECK PLATE EXISTS SENDER
        assertEquals(false, db.getItemTemplateMap().contains(key));

        //CHECK REFERENCE SENDER
        assertEquals((long) makerReference, (long) maker.getLastTimestamp(db));
    }


}