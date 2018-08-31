package core.transaction;

import core.BlockChain;
import core.account.PrivateKeyAccount;
import core.block.GenesisBlock;
import core.crypto.Crypto;
import core.item.assets.AssetCls;
import core.item.templates.Template;
import core.item.templates.TemplateCls;
import datachain.DCSet;
import datachain.ItemTemplateMap;
import ntp.NTP;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import utils.Corekeys;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

//import java.math.BigInteger;
//import java.util.ArrayList;
//import java.util.List;

public class TestRecTemplate {

    static Logger LOGGER = Logger.getLogger(TestRecTemplate.class.getName());

    //Long Transaction.FOR_NETWORK = null;

    int asPack = Transaction.FOR_NETWORK;
    long FEE_KEY = AssetCls.FEE_KEY;
    long VOTE_KEY = AssetCls.ERA_KEY;
    byte FEE_POWER = (byte) 1;
    byte[] templateReference = new byte[64];
    long timestamp = NTP.getTime();

    long flags = 0l;
    byte[] data = "test123!".getBytes();
    byte[] isText = new byte[]{1};
    byte[] encrypted = new byte[]{0};
    //CREATE KNOWN ACCOUNT
    byte[] seed = Crypto.getInstance().digest("test".getBytes());
    byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
    PrivateKeyAccount maker = new PrivateKeyAccount(privateKey);
    TemplateCls template;
    long templateKey = -1;
    IssueTemplateRecord issueTemplateRecord;
    R_SignNote signNoteRecord;
    ItemTemplateMap templateMap;
    private byte[] icon = new byte[]{1, 3, 4, 5, 6, 9}; // default value
    private byte[] image = new byte[]{4, 11, 32, 23, 45, 122, 11, -45}; // default value
    //CREATE EMPTY MEMORY DATABASE
    private DCSet db;
    private GenesisBlock gb;
    private List<String> imagelinks = new ArrayList<String>();

    // INIT TEMPLATES
    private void init() {

        db = DCSet.createEmptyDatabaseSet();
        templateMap = db.getItemTemplateMap();

        gb = new GenesisBlock();
        try {
            gb.process(db);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // FEE FUND
        maker.setLastTimestamp(gb.getTimestamp(), db);
        maker.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1).setScale(BlockChain.AMOUNT_DEDAULT_SCALE), false);

    }

    private void initTemplate(boolean process) {

        template = new Template(maker, "test132", icon, image, "12345678910strontje");

        //CREATE ISSUE PLATE TRANSACTION
        issueTemplateRecord = new IssueTemplateRecord(maker, template, FEE_POWER, timestamp, maker.getLastTimestamp(db));
        issueTemplateRecord.setDC(db, Transaction.FOR_NETWORK, 1, 1);
        issueTemplateRecord.sign(maker, Transaction.FOR_NETWORK);
        if (process) {
            issueTemplateRecord.process(gb, Transaction.FOR_NETWORK);
            templateKey = template.getKey(db);
        }
    }

    //ISSUE PLATE TRANSACTION

    @Test
    public void testAddreessVersion() {
        int vers = Corekeys.findAddressVersion("E");
        assertEquals(-1111, vers);
    }

    @Test
    public void validateSignatureIssueTemplateTransaction() {

        init();

        initTemplate(false);

        //CHECK IF ISSUE PLATE TRANSACTION IS VALID
        assertEquals(true, issueTemplateRecord.isSignatureValid(db));

        //INVALID SIGNATURE
        issueTemplateRecord = new IssueTemplateRecord(maker, template, FEE_POWER, timestamp, maker.getLastTimestamp(db), new byte[64]);

        //CHECK IF ISSUE PLATE IS INVALID
        assertEquals(false, issueTemplateRecord.isSignatureValid(db));
    }

    @Ignore
    //TODO actualize the test
    @Test
    public void parseIssueTemplateTransaction() {

        init();

        TemplateCls template = new Template(maker, "test132", icon, image, "12345678910strontje");
        byte[] raw = template.toBytes(false, false);
        assertEquals(raw.length, template.getDataLength(false));

        //CREATE ISSUE PLATE TRANSACTION
        IssueTemplateRecord issueTemplateRecord = new IssueTemplateRecord(maker, template, FEE_POWER, timestamp, maker.getLastTimestamp(db));
        issueTemplateRecord.sign(maker, Transaction.FOR_NETWORK);
        issueTemplateRecord.setDC(db, Transaction.FOR_NETWORK, 1, 1);
        issueTemplateRecord.process(gb, Transaction.FOR_NETWORK);

        //CONVERT TO BYTES
        byte[] rawIssueTemplateTransaction = issueTemplateRecord.toBytes(Transaction.FOR_NETWORK, true);

        //CHECK DATA LENGTH
        assertEquals(rawIssueTemplateTransaction.length, issueTemplateRecord.getDataLength(Transaction.FOR_NETWORK, true));

        try {
            //PARSE FROM BYTES
            IssueTemplateRecord parsedIssueTemplateTransaction = (IssueTemplateRecord) TransactionFactory.getInstance().parse(rawIssueTemplateTransaction, Transaction.FOR_NETWORK);
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

            //CHECK FEE
            assertEquals(issueTemplateRecord.getFee(), parsedIssueTemplateTransaction.getFee());

            //CHECK REFERENCE
            //assertEquals(issueTemplateRecord.getReference(), parsedIssueTemplateTransaction.getReference());

            //CHECK TIMESTAMP
            assertEquals(issueTemplateRecord.getTimestamp(), parsedIssueTemplateTransaction.getTimestamp());
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
        IssueTemplateRecord issueTemplateRecord = new IssueTemplateRecord(maker, template, FEE_POWER, timestamp, maker.getLastTimestamp(db));
        issueTemplateRecord.setDC(db, Transaction.FOR_NETWORK, 1, 1);
        assertEquals(Transaction.VALIDATE_OK, issueTemplateRecord.isValid(Transaction.FOR_NETWORK, flags));

        issueTemplateRecord.sign(maker, Transaction.FOR_NETWORK);
        issueTemplateRecord.process(gb, Transaction.FOR_NETWORK);
        int mapSize = templateMap.size();

        LOGGER.info("template KEY: " + template.getKey(db));

        //CHECK PLATE EXISTS SENDER
        long key = db.getIssueTemplateMap().get(issueTemplateRecord);
        assertEquals(true, templateMap.contains(key));

        TemplateCls template_2 = new Template(maker, "test132_2", icon, image, "2_12345678910strontje");
        IssueTemplateRecord issueTemplateTransaction_2 = new IssueTemplateRecord(maker, template_2, FEE_POWER, timestamp + 10, maker.getLastTimestamp(db));
        issueTemplateTransaction_2.sign(maker, Transaction.FOR_NETWORK);
        issueTemplateTransaction_2.process(gb, Transaction.FOR_NETWORK);
        LOGGER.info("template_2 KEY: " + template_2.getKey(db));
        issueTemplateTransaction_2.orphan(Transaction.FOR_NETWORK);
        assertEquals(mapSize, templateMap.size());

        //CHECK PLATE IS CORRECT
        assertEquals(true, Arrays.equals(templateMap.get(key).toBytes(true, false), template.toBytes(true, false)));

        //CHECK REFERENCE SENDER
        assertEquals(issueTemplateRecord.getTimestamp(), maker.getLastTimestamp(db));
    }

    // TODO - in statement - valid on key = 999

    //SIGN PLATE TRANSACTION

    @Test
    public void orphanIssueTemplateTransaction() {

        init();

        Template template = new Template(maker, "test", icon, image, "strontje");

        //CREATE ISSUE PLATE TRANSACTION
        IssueTemplateRecord issueTemplateRecord = new IssueTemplateRecord(maker, template, FEE_POWER, timestamp, maker.getLastTimestamp(db));
        issueTemplateRecord.setDC(db, Transaction.FOR_NETWORK, 1, 1);
        issueTemplateRecord.sign(maker, Transaction.FOR_NETWORK);
        issueTemplateRecord.process(gb, Transaction.FOR_NETWORK);
        long key = db.getIssueTemplateMap().get(issueTemplateRecord);
        assertEquals(issueTemplateRecord.getTimestamp(), maker.getLastTimestamp(db));

        issueTemplateRecord.orphan(Transaction.FOR_NETWORK);

        //CHECK PLATE EXISTS SENDER
        assertEquals(false, templateMap.contains(key));

        //CHECK REFERENCE SENDER
        //assertEquals(issueTemplateRecord.getReference(), maker.getLastReference(db));
    }

    @Test
    public void validateSignatureSignNoteTransaction() {

        init();

        initTemplate(true);

        signNoteRecord = new R_SignNote(maker, FEE_POWER, templateKey, data, isText, encrypted, timestamp + 10, maker.getLastTimestamp(db));
        signNoteRecord.sign(maker, asPack);

        //CHECK IF ISSUE PLATE TRANSACTION IS VALID
        assertEquals(true, signNoteRecord.isSignatureValid(db));

        //INVALID SIGNATURE
        signNoteRecord = new R_SignNote(maker, FEE_POWER, templateKey, data, isText, encrypted, timestamp + 10, maker.getLastTimestamp(db), new byte[64]);

        //CHECK IF ISSUE PLATE IS INVALID
        assertEquals(false, signNoteRecord.isSignatureValid(db));
    }

    @Test
    public void parseSignNoteTransaction() {

        init();

        initTemplate(true);

        signNoteRecord = new R_SignNote(maker, FEE_POWER, templateKey, data, isText, encrypted, timestamp + 10, maker.getLastTimestamp(db));
        signNoteRecord.sign(maker, asPack);

        //CONVERT TO BYTES
        byte[] rawSignNoteRecord = signNoteRecord.toBytes(Transaction.FOR_NETWORK, true);

        //CHECK DATA LENGTH
        assertEquals(rawSignNoteRecord.length, signNoteRecord.getDataLength(Transaction.FOR_NETWORK, true));

        try {
            //PARSE FROM BYTES
            R_SignNote parsedSignNoteRecord = (R_SignNote) TransactionFactory.getInstance().parse(rawSignNoteRecord, Transaction.FOR_NETWORK);
            LOGGER.info("parsedSignNote: " + parsedSignNoteRecord);

            //CHECK INSTANCE
            assertEquals(true, parsedSignNoteRecord instanceof R_SignNote);

            //CHECK SIGNATURE
            assertEquals(true, Arrays.equals(signNoteRecord.getSignature(), parsedSignNoteRecord.getSignature()));

            //CHECK ISSUER
            assertEquals(signNoteRecord.getCreator().getAddress(), parsedSignNoteRecord.getCreator().getAddress());

            //CHECK OWNER
            assertEquals(signNoteRecord.getKey(), parsedSignNoteRecord.getKey());

            //CHECK NAME
            assertEquals(true, Arrays.equals(signNoteRecord.getData(), parsedSignNoteRecord.getData()));

            //CHECK DESCRIPTION
            assertEquals(signNoteRecord.isText(), parsedSignNoteRecord.isText());

            //CHECK FEE
            assertEquals(signNoteRecord.getFee(), parsedSignNoteRecord.getFee());

            //CHECK REFERENCE
            //assertEquals(signNoteRecord.getReference(), parsedSignNoteRecord.getReference());

            //CHECK TIMESTAMP
            assertEquals(signNoteRecord.getTimestamp(), parsedSignNoteRecord.getTimestamp());
        } catch (Exception e) {
            fail("Exception while parsing transaction. " + e);
        }


        // NOT DATA
        data = null;
        signNoteRecord = new R_SignNote(maker, FEE_POWER, templateKey, data, isText, encrypted, timestamp + 20, maker.getLastTimestamp(db));
        signNoteRecord.sign(maker, Transaction.FOR_NETWORK);

        //CONVERT TO BYTES
        rawSignNoteRecord = signNoteRecord.toBytes(Transaction.FOR_NETWORK, true);

        //CHECK DATA LENGTH
        assertEquals(rawSignNoteRecord.length, signNoteRecord.getDataLength(Transaction.FOR_NETWORK, true));

        try {
            //PARSE FROM BYTES
            R_SignNote parsedSignNoteRecord = (R_SignNote) TransactionFactory.getInstance().parse(rawSignNoteRecord, Transaction.FOR_NETWORK);
            LOGGER.info("parsedSignNote: " + parsedSignNoteRecord);

            //CHECK INSTANCE
            assertEquals(true, parsedSignNoteRecord instanceof R_SignNote);

            //CHECK SIGNATURE
            assertEquals(true, Arrays.equals(signNoteRecord.getSignature(), parsedSignNoteRecord.getSignature()));

            //CHECK ISSUER
            assertEquals(signNoteRecord.getCreator().getAddress(), parsedSignNoteRecord.getCreator().getAddress());

            //CHECK OWNER
            assertEquals(signNoteRecord.getKey(), parsedSignNoteRecord.getKey());

            //CHECK NAME
            assertEquals(null, parsedSignNoteRecord.getData());

            //CHECK DESCRIPTION
            assertEquals(signNoteRecord.isText(), parsedSignNoteRecord.isText());

            //CHECK FEE
            assertEquals(signNoteRecord.getFee(), parsedSignNoteRecord.getFee());

            //CHECK REFERENCE
            //assertEquals(signNoteRecord.getReference(), parsedSignNoteRecord.getReference());

            //CHECK TIMESTAMP
            assertEquals(signNoteRecord.getTimestamp(), parsedSignNoteRecord.getTimestamp());
        } catch (Exception e) {
            fail("Exception while parsing transaction. " + e);
        }

        // NOT KEY
        //data = null;
        templateKey = 0;
        signNoteRecord = new R_SignNote(maker, FEE_POWER, templateKey, data, isText, encrypted, timestamp + 20, maker.getLastTimestamp(db));
        signNoteRecord.sign(maker, Transaction.FOR_NETWORK);

        //CONVERT TO BYTES
        rawSignNoteRecord = signNoteRecord.toBytes(Transaction.FOR_NETWORK, true);

        //CHECK DATA LENGTH
        assertEquals(rawSignNoteRecord.length, signNoteRecord.getDataLength(Transaction.FOR_NETWORK, true));

        try {
            //PARSE FROM BYTES
            R_SignNote parsedSignNoteRecord = (R_SignNote) TransactionFactory.getInstance().parse(rawSignNoteRecord, Transaction.FOR_NETWORK);
            LOGGER.info("parsedSignNote: " + parsedSignNoteRecord);

            //CHECK INSTANCE
            assertEquals(true, parsedSignNoteRecord instanceof R_SignNote);

            //CHECK SIGNATURE
            assertEquals(true, Arrays.equals(signNoteRecord.getSignature(), parsedSignNoteRecord.getSignature()));

            //CHECK ISSUER
            assertEquals(signNoteRecord.getCreator().getAddress(), parsedSignNoteRecord.getCreator().getAddress());

            //CHECK OWNER
            assertEquals(signNoteRecord.getKey(), parsedSignNoteRecord.getKey());

            //CHECK NAME
            assertEquals(null, parsedSignNoteRecord.getData());

            //CHECK DESCRIPTION
            assertEquals(signNoteRecord.isText(), parsedSignNoteRecord.isText());

            //CHECK FEE
            assertEquals(signNoteRecord.getFee(), parsedSignNoteRecord.getFee());

            //CHECK REFERENCE
            //assertEquals(signNoteRecord.getReference(), parsedSignNoteRecord.getReference());

            //CHECK TIMESTAMP
            assertEquals(signNoteRecord.getTimestamp(), parsedSignNoteRecord.getTimestamp());
        } catch (Exception e) {
            fail("Exception while parsing transaction. " + e);
        }

        // NOT KEY
        data = null;
        templateKey = 0;
        signNoteRecord = new R_SignNote(maker, FEE_POWER, templateKey, data, isText, encrypted, timestamp + 20, maker.getLastTimestamp(db));
        signNoteRecord.sign(maker, Transaction.FOR_NETWORK);

        //CONVERT TO BYTES
        rawSignNoteRecord = signNoteRecord.toBytes(Transaction.FOR_NETWORK, true);

        //CHECK DATA LENGTH
        assertEquals(rawSignNoteRecord.length, signNoteRecord.getDataLength(Transaction.FOR_NETWORK, true));

        try {
            //PARSE FROM BYTES
            R_SignNote parsedSignNoteRecord = (R_SignNote) TransactionFactory.getInstance().parse(rawSignNoteRecord, Transaction.FOR_NETWORK);
            LOGGER.info("parsedSignNote: " + parsedSignNoteRecord);

            //CHECK INSTANCE
            assertEquals(true, parsedSignNoteRecord instanceof R_SignNote);

            //CHECK SIGNATURE
            assertEquals(true, Arrays.equals(signNoteRecord.getSignature(), parsedSignNoteRecord.getSignature()));

            //CHECK ISSUER
            assertEquals(signNoteRecord.getCreator().getAddress(), parsedSignNoteRecord.getCreator().getAddress());

            //CHECK OWNER
            assertEquals(signNoteRecord.getKey(), parsedSignNoteRecord.getKey());

            //CHECK NAME
            assertEquals(null, parsedSignNoteRecord.getData());

            //CHECK DESCRIPTION
            assertEquals(signNoteRecord.isText(), parsedSignNoteRecord.isText());

            //CHECK FEE
            assertEquals(signNoteRecord.getFee(), parsedSignNoteRecord.getFee());

            //CHECK REFERENCE
            //assertEquals(signNoteRecord.getReference(), parsedSignNoteRecord.getReference());

            //CHECK TIMESTAMP
            assertEquals(signNoteRecord.getTimestamp(), parsedSignNoteRecord.getTimestamp());
        } catch (Exception e) {
            fail("Exception while parsing transaction. " + e);
        }

    }
    @Ignore
    //TODO actualize the test
    @Test
    public void processSignNoteTransaction() {

        init();

        initTemplate(true);

        signNoteRecord = new R_SignNote(maker, FEE_POWER, templateKey, data, isText, encrypted, timestamp + 10, maker.getLastTimestamp(db));
        signNoteRecord.setDC(db, Transaction.FOR_NETWORK, 1, 1);
        assertEquals(Transaction.VALIDATE_OK, signNoteRecord.isValid(Transaction.FOR_NETWORK, flags));

        signNoteRecord.sign(maker, Transaction.FOR_NETWORK);
        signNoteRecord.process(gb, Transaction.FOR_NETWORK);

        //CHECK REFERENCE SENDER
        assertEquals(signNoteRecord.getTimestamp(), maker.getLastTimestamp(db));

        ///// ORPHAN
        signNoteRecord.orphan(Transaction.FOR_NETWORK);

        //CHECK REFERENCE SENDER
        //assertEquals(signNoteRecord.getReference(), maker.getLastReference(db));
    }

    private void handleVars(String description) {
        Pattern pattern = Pattern.compile(Pattern.quote("{{") + "(.+?)" + Pattern.quote("}}"));
        //Pattern pattern = Pattern.compile("{{(.+)}}");
        Matcher matcher = pattern.matcher(description);
        while (matcher.find()) {
            String url = matcher.group(1);
            imagelinks.add(url);
            //description = description.replace(matcher.group(), getImgHtml(url));
        }
    }

    @Test
    public void regExTest() {
        String descr = "AJH {{wer}}, asdj {{we431!12}}";
        ArrayList arrayList = new ArrayList() {{
            add("wer");
            add("we431!12");
        }};
        handleVars(descr);
        assertEquals(imagelinks, arrayList);
    }
}
