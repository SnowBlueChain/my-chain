package org.erachain.core.transaction;

import org.erachain.core.account.PrivateKeyAccount;
import org.erachain.core.block.GenesisBlock;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.item.persons.PersonHuman;
import org.erachain.datachain.AddressPersonMap;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.KKPersonStatusMap;
import org.erachain.datachain.PersonAddressMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestRecGenesisPerson2 {

    static Logger LOGGER = LoggerFactory.getLogger(TestRecGenesisPerson2.class.getName());

    //Long Transaction.FOR_NETWORK = null;

    long FEE_KEY = Transaction.FEE_KEY;
    //long ALIVE_KEY = StatusCls.ALIVE_KEY;
    byte FEE_POWER = (byte) 1;
    byte[] packedReference = new byte[64];

    long flags = 0l;
    //CREATE KNOWN ACCOUNT
    byte[] seed = Crypto.getInstance().digest("test".getBytes());
    byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
    PrivateKeyAccount maker = new PrivateKeyAccount(privateKey);
    PersonCls person;
    long keyPerson = -1l;
    GenesisIssuePersonRecord genesisIssuePersonTransaction;
    KKPersonStatusMap dbPS;
    PersonAddressMap dbPA;
    AddressPersonMap dbAP;
    private byte[] icon = new byte[]{1, 3, 4, 5, 6, 9}; // default value
    private byte[] image = new byte[]{4, 11, 32, 23, 45, 122, 11, -45}; // default value
    private byte[] ownerSignature = new byte[Crypto.SIGNATURE_LENGTH];
    //CREATE EMPTY MEMORY DATABASE
    private DCSet db;
    private GenesisBlock gb;

    private void initIssue(boolean toProcess) {

        //CREATE EMPTY MEMORY DATABASE
        db = DCSet.createEmptyDatabaseSet();
        dbPA = db.getPersonAddressMap();
        dbAP = db.getAddressPersonMap();
        dbPS = db.getPersonStatusMap();

        //CREATE PERSON
        //person = GenesisBlock.makePerson(0);
        long bd = -106185600;
        person = new PersonHuman(maker, "ERMLAEV DMITRII SERGEEVICH", bd, bd - 1,
                (byte) 1, "Slav", (float) 1.1, (float) 1.1,
                "white", "gray", "dark", (int) 188, icon, image, "icreator", ownerSignature);
        //byte[] rawPerson = person.toBytes(true); // reference is new byte[64]
        //assertEquals(rawPerson.length, person.getDataLength());

        //CREATE ISSUE PERSON TRANSACTION
        genesisIssuePersonTransaction = new GenesisIssuePersonRecord(person);
        if (toProcess) {
            genesisIssuePersonTransaction.process(gb, Transaction.FOR_NETWORK);
            keyPerson = person.getKey(db);
        }

    }

    // GENESIS ISSUE
    @Test
    public void validateGenesisIssuePersonRecord() {

        initIssue(false);

        //genesisIssuePersonTransaction.sign(creator);
        //CHECK IF ISSUE PERSON TRANSACTION IS VALID
        assertEquals(true, genesisIssuePersonTransaction.isSignatureValid());
        assertEquals(Transaction.VALIDATE_OK, genesisIssuePersonTransaction.isValid(Transaction.FOR_NETWORK, flags));

        //CONVERT TO BYTES
        //LOGGER.info("CREATOR: " + genesisIssuePersonTransaction.getCreator().getPublicKey());
        byte[] rawGenesisIssuePersonRecord = genesisIssuePersonTransaction.toBytes(Transaction.FOR_NETWORK, true);

        //CHECK DATA LENGTH
        assertEquals(rawGenesisIssuePersonRecord.length, genesisIssuePersonTransaction.getDataLength(Transaction.FOR_NETWORK, true));
        //LOGGER.info("rawGenesisIssuePersonRecord.length") + ": + rawGenesisIssuePersonRecord.length);

        try {
            //PARSE FROM BYTES
            GenesisIssuePersonRecord parsedGenesisIssuePersonRecord = (GenesisIssuePersonRecord) TransactionFactory.getInstance().parse(rawGenesisIssuePersonRecord, Transaction.FOR_NETWORK);

            //CHECK INSTANCE
            assertEquals(true, parsedGenesisIssuePersonRecord instanceof GenesisIssuePersonRecord);

            //CHECK SIGNATURE
            assertEquals(true, Arrays.equals(genesisIssuePersonTransaction.getSignature(), parsedGenesisIssuePersonRecord.getSignature()));

            //CHECK NAME
            assertEquals(genesisIssuePersonTransaction.getItem().getName(), parsedGenesisIssuePersonRecord.getItem().getName());

            //CHECK DESCRIPTION
            assertEquals(genesisIssuePersonTransaction.getItem().getDescription(), parsedGenesisIssuePersonRecord.getItem().getDescription());

        } catch (Exception e) {
            fail("Exception while parsing transaction." + e);
        }

        //PARSE TRANSACTION FROM WRONG BYTES
        rawGenesisIssuePersonRecord = new byte[genesisIssuePersonTransaction.getDataLength(Transaction.FOR_NETWORK, true)];

        try {
            //PARSE FROM BYTES
            TransactionFactory.getInstance().parse(rawGenesisIssuePersonRecord, Transaction.FOR_NETWORK);

            //FAIL
            fail("this should throw an exception");
        } catch (Exception e) {
            //EXCEPTION IS THROWN OK
        }

        //CREATE INVALID PERSON TRANSFER INVALID RECIPIENT ADDRESS
        person = new PersonHuman(maker, "ERMLAEV DMITRII SERGEEVICH", 0L, -1L,
                (byte) 1, "Slav", (float) 111.1, (float) 1.1,
                "white", "gray", "dark", (int) 188, icon, image, "icreator", ownerSignature);
        genesisIssuePersonTransaction = new GenesisIssuePersonRecord(person);
        assertEquals(Transaction.ITEM_PERSON_LATITUDE_ERROR, genesisIssuePersonTransaction.isValid(Transaction.FOR_NETWORK, flags));

    }


    @Test
    public void parseGenesisIssuePersonRecord() {

        initIssue(false);

        //CONVERT TO BYTES
        byte[] rawGenesisIssuePersonRecord = genesisIssuePersonTransaction.toBytes(Transaction.FOR_NETWORK, true);

        //CHECK DATA LENGTH
        assertEquals(rawGenesisIssuePersonRecord.length, genesisIssuePersonTransaction.getDataLength(Transaction.FOR_NETWORK, true));

        try {
            //PARSE FROM BYTES
            GenesisIssuePersonRecord parsedGenesisIssuePersonRecord = (GenesisIssuePersonRecord) TransactionFactory.getInstance().parse(rawGenesisIssuePersonRecord, Transaction.FOR_NETWORK);

            //CHECK INSTANCE
            assertEquals(true, parsedGenesisIssuePersonRecord instanceof GenesisIssuePersonRecord);

            //CHECK SIGNATURE
            assertEquals(true, Arrays.equals(genesisIssuePersonTransaction.getSignature(), parsedGenesisIssuePersonRecord.getSignature()));

            //CHECK OWNER
            assertEquals(genesisIssuePersonTransaction.getItem().getOwner().getAddress(), parsedGenesisIssuePersonRecord.getItem().getOwner().getAddress());

            //CHECK NAME
            assertEquals(genesisIssuePersonTransaction.getItem().getName(), parsedGenesisIssuePersonRecord.getItem().getName());

            //CHECK DESCRIPTION
            assertEquals(genesisIssuePersonTransaction.getItem().getDescription(), parsedGenesisIssuePersonRecord.getItem().getDescription());

            assertEquals(genesisIssuePersonTransaction.getItem().getKey(db), parsedGenesisIssuePersonRecord.getItem().getKey(db));

        } catch (Exception e) {
            fail("Exception while parsing transaction." + e);
        }

        //PARSE TRANSACTION FROM WRONG BYTES
        rawGenesisIssuePersonRecord = new byte[genesisIssuePersonTransaction.getDataLength(Transaction.FOR_NETWORK, true)];

        try {
            //PARSE FROM BYTES
            TransactionFactory.getInstance().parse(rawGenesisIssuePersonRecord, Transaction.FOR_NETWORK);

            //FAIL
            fail("this should throw an exception");
        } catch (Exception e) {
            //EXCEPTION IS THROWN OK
        }
    }


    @Test
    public void process_orphan_GenesisIssuePersonRecord() {


        initIssue(false);
        LOGGER.info("person KEY: " + keyPerson);

        //CHECK REFERENCE RECIPIENT
        //assertNotEquals((long)genesisIssuePersonTransaction.getTimestamp(), (long)maker.getLastReference(db));
        genesisIssuePersonTransaction.setDC(db, Transaction.FOR_NETWORK, 1, 1);
        genesisIssuePersonTransaction.process(gb, Transaction.FOR_NETWORK);
        keyPerson = person.getKey(db);

        //CHECK PERSON EXISTS SENDER
        assertEquals(true, db.getItemPersonMap().contains(keyPerson));
        assertEquals(genesisIssuePersonTransaction.getItem().getKey(db), keyPerson);
        assertEquals(genesisIssuePersonTransaction.getItem().getName(), person.getName());

        //CHECK PERSON IS CORRECT
        assertEquals(true, Arrays.equals(db.getItemPersonMap().get(keyPerson).toBytes(true, false), person.toBytes(true, false)));

        /////////////////
        ///// ORPHAN ////
        genesisIssuePersonTransaction.orphan(gb, Transaction.FOR_NETWORK);

        assertEquals(false, db.getItemPersonMap().contains(keyPerson));

    }
}
