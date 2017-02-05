package test.records;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ntp.NTP;

import org.junit.Test;

import com.google.common.primitives.Longs;

import core.account.Account;
import core.account.PrivateKeyAccount;
import core.block.GenesisBlock;
import core.crypto.Crypto;
import core.item.assets.AssetCls;
import core.item.assets.AssetVenture;
import core.naming.Name;
import core.naming.NameSale;
import core.payment.Payment;
import core.transaction.ArbitraryTransactionV3;
import core.transaction.BuyNameTransaction;
import core.transaction.CancelOrderTransaction;
import core.transaction.CancelSellNameTransaction;
import core.transaction.CreateOrderTransaction;
import core.transaction.CreatePollTransaction;
import core.transaction.IssueAssetTransaction;
//import core.transaction.MultiPaymentTransaction;
import core.transaction.R_Send;
import core.transaction.RegisterNameTransaction;
import core.transaction.SellNameTransaction;
import core.transaction.Transaction;
import core.transaction.TransactionFactory;
import core.transaction.UpdateNameTransaction;
import core.transaction.VoteOnPollTransaction;
import core.voting.Poll;
import core.voting.PollOption;
import database.DBSet;

public class TransactionTests {

	Long releaserReference = null;

	static Logger LOGGER = Logger.getLogger(TransactionTests.class.getName());

	long ERM_KEY = Transaction.RIGHTS_KEY;
	long FEE_KEY = Transaction.FEE_KEY;
	byte FEE_POWER = (byte)1;
	byte[] assetReference = new byte[64];
	long timestamp = NTP.getTime();
	Long last_ref;
	Long new_ref;
	
	private byte[] icon = new byte[]{1,3,4,5,6,9}; // default value
	private byte[] image = new byte[]{4,11,32,23,45,122,11,-45}; // default value

	//CREATE EMPTY MEMORY DATABASE
	private DBSet db;
	private GenesisBlock gb;
	
	//CREATE KNOWN ACCOUNT
	byte[] seed = Crypto.getInstance().digest("test".getBytes());
	byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
	PrivateKeyAccount maker = new PrivateKeyAccount(privateKey);
	byte[] seed_b = Crypto.getInstance().digest("buyer".getBytes());
	byte[] privateKey_b = Crypto.getInstance().createKeyPair(seed_b).getA();
	PrivateKeyAccount buyer = new PrivateKeyAccount(privateKey_b);		
	Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");

	DBSet databaseSet;

	// INIT ASSETS
	private void init() {
		
		File log4j = new File("log4j_test.properties");
		System.out.println(log4j.getAbsolutePath());
		if(log4j.exists())
		{
			System.out.println("configured");
			PropertyConfigurator.configure(log4j.getAbsolutePath());
		}

		databaseSet = db = DBSet.createEmptyDatabaseSet();
		gb = new GenesisBlock();
		gb.process(db);

		last_ref = gb.getTimestamp(db);
		
		// FEE FUND
		maker.setLastReference(last_ref, db);
		maker.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1).setScale(8));
		new_ref = maker.getLastReference(db);
		
		buyer.setLastReference(last_ref, db);
		buyer.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1).setScale(8));
		buyer.changeBalance(db, false, ERM_KEY, BigDecimal.valueOf(2000).setScale(8)); // for bye
		

	}
	
		
	//PAYMENT
	
	@Test
	public void validateSignatureR_Send() 
	{
		
		init();
		
		//CREATE PAYMENT
		Transaction payment = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp, last_ref);
		payment.sign(maker, false);		
		//CHECK IF PAYMENT SIGNATURE IS VALID
		assertEquals(true, payment.isSignatureValid());
		
		//INVALID SIGNATURE
		payment = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp+1, last_ref, new byte[64]);
		
		//CHECK IF PAYMENT SIGNATURE IS INVALID
		assertEquals(false, payment.isSignatureValid());
	}
	
	@Test
	public void validateR_Send() 
	{
		
		init();
		

		//CREATE VALID PAYMENT
		Transaction payment = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(0.5).setScale(8), timestamp, maker.getLastReference(db));
		assertEquals(Transaction.VALIDATE_OK, payment.isValid(db, releaserReference));

		//CREATE INVALID PAYMENT INVALID RECIPIENT ADDRESS
		payment = new R_Send(maker, FEE_POWER, new Account("test"), FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp, maker.getLastReference(db)+10);
		assertEquals(Transaction.INVALID_ADDRESS, payment.isValid(db, releaserReference));
		
		//CREATE INVALID PAYMENT NEGATIVE AMOUNT
		payment = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(-100).setScale(8), timestamp, maker.getLastReference(db));
		assertEquals(Transaction.NEGATIVE_AMOUNT, payment.isValid(db, releaserReference));	
				
		//CREATE INVALID PAYMENT WRONG REFERENCE
		payment = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp, -123L, new byte[64]);
		assertEquals(Transaction.INVALID_REFERENCE, payment.isValid(db, releaserReference));
		
		//CREATE INVALID PAYMENT WRONG TIMESTAMP
		payment = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), maker.getLastReference(db), maker.getLastReference(db));
		assertEquals(Transaction.INVALID_TIMESTAMP, payment.isValid(db, releaserReference));
		payment = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), maker.getLastReference(db)-10, maker.getLastReference(db));
		assertEquals(Transaction.INVALID_TIMESTAMP, payment.isValid(db, releaserReference));

	}
	
	@Test
	public void parseR_Send() 
	{
		init();
												
		//CREATE VALID PAYMENT
		Transaction payment = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp, maker.getLastReference(db));
		payment.sign(maker, false);
		
		//CONVERT TO BYTES
		byte[] rawPayment = payment.toBytes(true, null);
		
		try 
		{	
			//PARSE FROM BYTES
			R_Send parsedPayment = (R_Send) TransactionFactory.getInstance().parse(rawPayment, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedPayment instanceof R_Send);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(payment.getSignature(), parsedPayment.getSignature()));
			
			//CHECK AMOUNT SENDER
			assertEquals(payment.getAmount(maker), parsedPayment.getAmount(maker));	
			
			//CHECK AMOUNT RECIPIENT
			assertEquals(payment.getAmount(recipient), parsedPayment.getAmount(recipient));	
			
			//CHECK FEE
			assertEquals(payment.getFee(), parsedPayment.getFee());	
			
			//CHECK REFERENCE
			assertEquals(payment.getReference(), parsedPayment.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(payment.getTimestamp(), parsedPayment.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawPayment = new byte[payment.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawPayment, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}
	
	@Test
	public void processR_Send()
	{
		
		init();

		//CREATE PAYMENT
		BigDecimal amount = BigDecimal.valueOf(0.5).setScale(8);
		Transaction payment = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, amount.setScale(8), timestamp, last_ref);
		payment.sign(maker, false);
		BigDecimal fee = payment.getFee();
		payment.process(databaseSet, gb, false);

		LOGGER.info("getConfirmedBalance: " + maker.getBalanceUSE(FEE_KEY, databaseSet));
		LOGGER.info("getConfirmedBalance FEE_KEY:" + maker.getBalanceUSE(FEE_KEY, databaseSet));

		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1).subtract(amount).subtract(fee).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
				
		//CHECK BALANCE RECIPIENT
		assertEquals(amount, recipient.getBalanceUSE(FEE_KEY, databaseSet));
		
		//CHECK REFERENCE SENDER
		assertEquals((long)maker.getLastReference(databaseSet), timestamp);
		
		//CHECK REFERENCE RECIPIENT
		assertEquals(payment.getTimestamp(), recipient.getLastReference(databaseSet));
	}
	
	@Test
	public void orphanR_Send()
	{
		
		init();
			
		//CREATE PAYMENT		
		Transaction payment = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp, last_ref);
		payment.sign(maker, false);
		payment.process(databaseSet, gb, false);
		
		BigDecimal amount1 = maker.getBalanceUSE(FEE_KEY, databaseSet);
		BigDecimal amount2 = recipient.getBalanceUSE(FEE_KEY, databaseSet);

		//CREATE PAYMENT2
		Transaction payment2  = new R_Send(maker, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp, last_ref);
		payment2.sign(maker, false);
		payment.process(databaseSet, gb, false);
		
		//ORPHAN PAYMENT
		payment2.orphan(databaseSet, false);
		
		//CHECK BALANCE SENDER
		assertEquals(0,amount1.compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
						
		//CHECK BALANCE RECIPIENT
		assertEquals(amount2, recipient.getBalanceUSE(FEE_KEY, databaseSet));
				
		//CHECK REFERENCE SENDER
		assertEquals(maker.getLastReference(databaseSet), maker.getLastReference(databaseSet));
				
		//CHECK REFERENCE RECIPIENT
		assertEquals(null, recipient.getLastReference(databaseSet));

	}

	//REGISTER NAME
	
	@Test
	public void validateSignatureRegisterNameTransaction() 
	{
		
		init();
		
		//CREATE NAME
		Name name = new Name(maker, "test", "this is the value");
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(true, nameRegistration.isSignatureValid());
		
		//INVALID SIGNATURE
		nameRegistration = new RegisterNameTransaction(
				maker, name, FEE_POWER, timestamp, last_ref, new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, nameRegistration.isSignatureValid());
	}
	
	@Test
	public void validateRegisterNameTransaction() 
	{
		
		init();
		
		//CREATE SIGNATURE
		Name name = new Name(maker, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameRegistration.isValid(databaseSet, releaserReference));
		nameRegistration.process(databaseSet, gb, false);
		
		//CREATE INVALID NAME REGISTRATION INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		name = new Name(maker, longName, "this is the value");
		nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);

		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, nameRegistration.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME REGISTRATION INVALID NAME LENGTH
		String longValue = "";
		for(int i=1; i<10000; i++)
		{
			longValue += "oke";
		}
		name = new Name(maker, "test2", longValue);
		nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);

		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_VALUE_LENGTH, nameRegistration.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME REGISTRATION NAME ALREADY TAKEN
		name = new Name(maker, "test", "this is the value");
		nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.NAME_ALREADY_REGISTRED, nameRegistration.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME NOT ENOUGH BALANCE
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		name = new Name(invalidOwner, "test2", "this is the value");
		nameRegistration = new RegisterNameTransaction(invalidOwner, name, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));
		nameRegistration.sign(invalidOwner, false);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.NOT_ENOUGH_FEE, nameRegistration.isValid(databaseSet, releaserReference));
		
		//CREATE NAME REGISTRATION INVALID REFERENCE
		name = new Name(maker, "test2", "this is the value");
		nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, nameRegistration.isValid(databaseSet, releaserReference));
	}

	@Test
	public void parseRegisterNameTransaction() 
	{
		
		init();
		
		//CREATE SIGNATURE
		Name name = new Name(maker, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		RegisterNameTransaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		
		//CONVERT TO BYTES
		byte[] rawNameRegistration = nameRegistration.toBytes(true, null);
		
		try 
		{	
			//PARSE FROM BYTES
			RegisterNameTransaction parsedRegistration = (RegisterNameTransaction) TransactionFactory.getInstance().parse(rawNameRegistration, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedRegistration instanceof RegisterNameTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(nameRegistration.getSignature(), parsedRegistration.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(nameRegistration.getAmount(maker), parsedRegistration.getAmount(maker));	
			
			//CHECK NAME OWNER
			assertEquals(nameRegistration.getName().getOwner().getAddress(), parsedRegistration.getName().getOwner().getAddress());	
			
			//CHECK NAME NAME
			assertEquals(nameRegistration.getName().getName(), parsedRegistration.getName().getName());	
			
			//CHECK NAME VALUE
			assertEquals(nameRegistration.getName().getValue(), parsedRegistration.getName().getValue());	
			
			//CHECK FEE
			assertEquals(nameRegistration.getFee(), parsedRegistration.getFee());	
			
			//CHECK REFERENCE
			assertEquals(nameRegistration.getReference(), parsedRegistration.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(nameRegistration.getTimestamp(), parsedRegistration.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawNameRegistration = new byte[nameRegistration.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawNameRegistration, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}

	@Test
	public void processRegisterNameTransaction()
	{
		
		init();
				
		//CREATE SIGNATURE
		Name name = new Name(maker, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		nameRegistration.process(databaseSet, gb, false);
		
		//CHECK BALANCE SENDER
		//assertEquals(0, maker.getConfirmedBalance(FEE_KEY, databaseSet));
		assertEquals(1, BigDecimal.valueOf(1).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
				
		//CHECK REFERENCE SENDER
		assertEquals(nameRegistration.getTimestamp(), maker.getLastReference(databaseSet));
		
		//CHECK NAME EXISTS
		assertEquals(true, databaseSet.getNameMap().contains(name));
	}
	
	@Test
	public void orphanRegisterNameTransaction()
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		nameRegistration.process(databaseSet, gb, false);
		nameRegistration.orphan(databaseSet, false);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(1000).setScale(8), maker.getBalanceUSE(FEE_KEY, databaseSet));
				
		//CHECK REFERENCE SENDER
		assertEquals(last_ref, nameRegistration.getReference());
		
		//CHECK NAME EXISTS
		assertEquals(false, databaseSet.getNameMap().contains(name));
	}

	//UPDATE NAME
	
	@Test
	public void validateSignatureUpdateNameTransaction()
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE NAME UPDATE
		Transaction nameUpdate = new UpdateNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameUpdate.sign(maker, false);
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(true, nameUpdate.isSignatureValid());
		
		//INVALID SIGNATURE
		nameUpdate = new RegisterNameTransaction(
				maker, name, FEE_POWER, timestamp, last_ref, new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, nameUpdate.isSignatureValid());

	}
	
	@Test
	public void validateUpdateNameTransaction() 
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameRegistration.isValid(databaseSet, releaserReference));
		nameRegistration.process(databaseSet, gb, false);
		
		//CREATE NAME UPDATE
		name.setValue("new value");
		Transaction nameUpdate = new UpdateNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
	
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameUpdate.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME UPDATE INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		name = new Name(maker, longName, "this is the value");
		nameUpdate = new UpdateNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);		

		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, nameUpdate.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME UPDATE NAME DOES NOT EXIST
		name = new Name(maker, "test2", "this is the value");
		nameUpdate = new UpdateNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NAME_DOES_NOT_EXIST, nameUpdate.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME UPDATE INCORRECT OWNER
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		name = new Name(invalidOwner, "test2", "this is the value");
		nameRegistration = new RegisterNameTransaction(invalidOwner, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(invalidOwner, false);
		nameRegistration.process(databaseSet, gb, false);	
		
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.INVALID_MAKER_ADDRESS, nameUpdate.isValid(databaseSet, releaserReference));
				
		//CREATE INVALID NAME UPDATE NO BALANCE
		name = new Name(invalidOwner, "test2", "this is the value");
		nameUpdate = new UpdateNameTransaction(invalidOwner, name, FEE_POWER, timestamp, last_ref);
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NO_BALANCE, nameUpdate.isValid(databaseSet, releaserReference));
				
		//CREATE NAME UPDATE INVALID REFERENCE
		name = new Name(maker, "test", "this is the value");
		nameUpdate = new UpdateNameTransaction(maker, name, FEE_POWER, timestamp, -123L);
				
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, nameUpdate.isValid(databaseSet, releaserReference));
						
	}

	@Test
	public void parseUpdateNameTransaction() 
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
				
		//CREATE NAME UPDATE
		UpdateNameTransaction nameUpdate = new UpdateNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameUpdate.sign(maker, false);
		
		//CONVERT TO BYTES
		byte[] rawNameUpdate = nameUpdate.toBytes(true, null);
		
		try 
		{	
			//PARSE FROM BYTES
			UpdateNameTransaction parsedUpdate = (UpdateNameTransaction) TransactionFactory.getInstance().parse(rawNameUpdate, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedUpdate instanceof UpdateNameTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(nameUpdate.getSignature(), parsedUpdate.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(nameUpdate.getAmount(maker), parsedUpdate.getAmount(maker));	
			
			//CHECK OWNER
			assertEquals(nameUpdate.getCreator().getAddress(), parsedUpdate.getCreator().getAddress());	
			
			//CHECK NAME OWNER
			assertEquals(nameUpdate.getName().getOwner().getAddress(), parsedUpdate.getName().getOwner().getAddress());	
			
			//CHECK NAME NAME
			assertEquals(nameUpdate.getName().getName(), parsedUpdate.getName().getName());	
			
			//CHECK NAME VALUE
			assertEquals(nameUpdate.getName().getValue(), parsedUpdate.getName().getValue());	
			
			//CHECK FEE
			assertEquals(nameUpdate.getFee(), parsedUpdate.getFee());	
			
			//CHECK REFERENCE
			assertEquals(nameUpdate.getReference(), parsedUpdate.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(nameUpdate.getTimestamp(), parsedUpdate.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawNameUpdate = new byte[nameUpdate.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawNameUpdate, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}

	@Test
	public void processUpdateNameTransaction()
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		nameRegistration.process(databaseSet, gb, false);
		// set FEE
		maker.changeBalance(databaseSet, false, FEE_KEY, BigDecimal.valueOf(1).setScale(8));
		
		//CREATE NAME UPDATE
		name = new Name(new Account("Qj5Aq4P4ehXaCEmi6vqVrFQDecpPXKSi8z"), "test", "new value");
		Transaction nameUpdate = new UpdateNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameUpdate.sign(maker, false);
		nameUpdate.process(databaseSet, gb, false);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(nameUpdate.getTimestamp(), maker.getLastReference(databaseSet));
				
		//CHECK NAME EXISTS
		assertEquals(true, databaseSet.getNameMap().contains(name));
		
		//CHECK NAME VALUE
		name =  databaseSet.getNameMap().get("test");
		assertEquals("new value", name.getValue());
		
		//CHECK NAME OWNER
		assertEquals(true, "XYLEQnuvhracK2WMN3Hjif67knkJe9hTQn" != name.getOwner().getAddress());
		assertEquals("Qj5Aq4P4ehXaCEmi6vqVrFQDecpPXKSi8z", name.getOwner().getAddress());
		
	}

	
	@Test
	public void orphanUpdateNameTransaction()
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		nameRegistration.process(databaseSet, gb, false);
		// set FEE
		maker.changeBalance(databaseSet, false, FEE_KEY, BigDecimal.valueOf(1).setScale(8));
		
		//CREATE NAME UPDATE
		name = new Name(new Account("XYLEQnuvhracK2WMN3Hjif67knkJe9hTQn"), "test", "new value");
		Transaction nameUpdate = new UpdateNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameUpdate.sign(maker, false);
		nameUpdate.process(databaseSet, gb, false);
		nameUpdate.orphan(databaseSet, false);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(nameRegistration.getTimestamp(), maker.getLastReference(databaseSet));
				
		//CHECK NAME EXISTS
		assertEquals(true, databaseSet.getNameMap().contains(name));
		
		//CHECK NAME VALUE
		name =  databaseSet.getNameMap().get("test");
		assertEquals("this is the value", name.getValue());
		
		//CHECK NAME OWNER
		assertEquals(maker.getAddress(), name.getOwner().getAddress());
	}
	
	//SELL NAME
	
	@Test
	public void validateSignatureSellNameTransaction()
	{
		
		init();
		
		//CREATE NAME
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(true, nameSaleTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		nameSaleTransaction = new SellNameTransaction(
				maker, nameSale, FEE_POWER, timestamp, last_ref, new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, nameSaleTransaction.isSignatureValid());
	}
	
	@Test
	public void validateSellNameTransaction() 
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameRegistration.isValid(databaseSet, releaserReference));
		nameRegistration.process(databaseSet, gb, false);
		
		//CREATE NAME SALE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		Transaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
	
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameSaleTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME SALE INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		nameSale = new NameSale(longName, BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);		

		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, nameSaleTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME SALE NAME DOES NOT EXIST
		nameSale = new NameSale("test2", BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		nameSaleTransaction.process(databaseSet, gb, false);
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NAME_DOES_NOT_EXIST, nameSaleTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME UPDATE INCORRECT OWNER
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		name = new Name(invalidOwner, "test2", "this is the value");
		nameRegistration = new RegisterNameTransaction(invalidOwner, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(invalidOwner, false);
		nameRegistration.process(databaseSet, gb, false);	
		
		//CHECK IF NAME UPDATE IS INVALID
		nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		assertEquals(Transaction.INVALID_MAKER_ADDRESS, nameSaleTransaction.isValid(databaseSet, releaserReference));
				
		//CREATE INVALID NAME UPDATE NO BALANCE
		nameSale = new NameSale("test2", BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(invalidOwner, nameSale, FEE_POWER, timestamp, last_ref);
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NO_BALANCE, nameSaleTransaction.isValid(databaseSet, releaserReference));
				
		//CREATE NAME UPDATE INVALID REFERENCE
		nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, -123L);
				
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, nameSaleTransaction.isValid(databaseSet, releaserReference));
				
		//CREATE NAME UPDATE PROCESS 
		nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		nameSaleTransaction.process(databaseSet, gb, false);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.NAME_ALREADY_ON_SALE, nameSaleTransaction.isValid(databaseSet, releaserReference));
	}

	@Test
	public void parseSellNameTransaction() 
	{
		
		init();

		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1).setScale(8));
				
		//CREATE NAME UPDATE
		SellNameTransaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		nameSaleTransaction.process(db, gb, false);
		
		//CONVERT TO BYTES
		byte[] rawNameSale = nameSaleTransaction.toBytes(true, null);
		
		try 
		{	
			//PARSE FROM BYTES
			SellNameTransaction parsedNameSale = (SellNameTransaction) TransactionFactory.getInstance().parse(rawNameSale, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedNameSale instanceof SellNameTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(nameSaleTransaction.getSignature(), parsedNameSale.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(nameSaleTransaction.getAmount(maker), parsedNameSale.getAmount(maker));	
			
			//CHECK OWNER
			assertEquals(nameSaleTransaction.getCreator().getAddress(), parsedNameSale.getCreator().getAddress());	
			
			//CHECK NAMESALE NAME
			assertEquals(nameSaleTransaction.getNameSale().getKey(), parsedNameSale.getNameSale().getKey());	
			
			//CHECK NAMESALE AMOUNT
			assertEquals(nameSaleTransaction.getNameSale().getAmount(), parsedNameSale.getNameSale().getAmount());	
			
			//CHECK FEE
			assertEquals(nameSaleTransaction.getFee(), parsedNameSale.getFee());	
			
			//CHECK REFERENCE
			assertEquals(nameSaleTransaction.getReference(), parsedNameSale.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(nameSaleTransaction.getTimestamp(), parsedNameSale.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawNameSale = new byte[nameSaleTransaction.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawNameSale, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}

	@Test
	public void processSellNameTransaction()
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		nameRegistration.process(databaseSet, gb, false);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		nameSaleTransaction.process(databaseSet, gb, false);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(nameSaleTransaction.getTimestamp(), maker.getLastReference(databaseSet));
				
		//CHECK NAME SALE EXISTS
		assertEquals(true, databaseSet.getNameExchangeMap().contains("test"));
		
		//CHECK NAME SALE AMOUNT
		nameSale =  databaseSet.getNameExchangeMap().getNameSale("test");
		assertEquals(BigDecimal.valueOf(1000).setScale(8), nameSale.getAmount());
	}

	@Test
	public void orphanSellNameTransaction()
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		nameRegistration.process(databaseSet, gb, false);
		BigDecimal bal = maker.getBalanceUSE(FEE_KEY, databaseSet);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
						
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		nameSaleTransaction.process(databaseSet, gb, false);
		nameSaleTransaction.orphan(databaseSet, false);
		
		//CHECK BALANCE SENDER
		//assertEquals(0, maker.getConfirmedBalance(FEE_KEY, databaseSet));
		assertEquals(0, bal.compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(nameRegistration.getTimestamp(), maker.getLastReference(databaseSet));
				
		//CHECK NAME SALE EXISTS
		assertEquals(false, databaseSet.getNameExchangeMap().contains("test"));
	}
	
	
	//CANCEL SELL NAME
	
	@Test
	public void validateSignatureCancelSellNameTransaction()
	{
		
		init();
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new CancelSellNameTransaction(maker, "test", FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(true, nameSaleTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		nameSaleTransaction = new CancelSellNameTransaction(
				maker, "test", FEE_POWER, timestamp, last_ref, new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, nameSaleTransaction.isSignatureValid());
	}
	
	@Test
	public void validateCancelSellNameTransaction() 
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameRegistration.isValid(databaseSet, releaserReference));
		nameRegistration.process(databaseSet, gb, false);
		
		//CREATE NAME SALE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		Transaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
	
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameSaleTransaction.isValid(databaseSet, releaserReference));
		nameSaleTransaction.process(databaseSet, gb, false);
		
		//CREATE CANCEL NAME SALE
		CancelSellNameTransaction cancelNameSaleTransaction = new CancelSellNameTransaction(maker, nameSale.getKey(), FEE_POWER, timestamp, last_ref);		

		//CHECK IF CANCEL NAME UPDATE IS VALID
		assertEquals(Transaction.VALIDATE_OK, cancelNameSaleTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID CANCEL NAME SALE INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		
		cancelNameSaleTransaction = new CancelSellNameTransaction(maker, longName, FEE_POWER, timestamp, last_ref);		

		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, cancelNameSaleTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID CANCEL NAME SALE NAME DOES NOT EXIST
		cancelNameSaleTransaction = new CancelSellNameTransaction(maker, "test2", FEE_POWER, timestamp, last_ref);
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NAME_DOES_NOT_EXIST, cancelNameSaleTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME UPDATE INCORRECT OWNER
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		name = new Name(invalidOwner, "test2", "this is the value");
		nameRegistration = new RegisterNameTransaction(invalidOwner, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(invalidOwner, false);
		nameRegistration.process(databaseSet, gb, false);	
		
		//CREATE NAME SALE
		nameSale = new NameSale("test2", BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(invalidOwner, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(invalidOwner, false);
		nameSaleTransaction.process(databaseSet, gb, false);	
		
		//CHECK IF NAME UPDATE IS INVALID
		cancelNameSaleTransaction = new CancelSellNameTransaction(maker, "test2", FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		assertEquals(Transaction.INVALID_MAKER_ADDRESS, cancelNameSaleTransaction.isValid(databaseSet, releaserReference));
				
		//CREATE INVALID NAME UPDATE NO BALANCE
		cancelNameSaleTransaction = new CancelSellNameTransaction(invalidOwner, "test2", FEE_POWER, timestamp, last_ref);
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NO_BALANCE, cancelNameSaleTransaction.isValid(databaseSet, releaserReference));
				
		//CREATE NAME UPDATE INVALID REFERENCE
		cancelNameSaleTransaction = new CancelSellNameTransaction(maker, "test", FEE_POWER, timestamp, -123L);
				
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, cancelNameSaleTransaction.isValid(databaseSet, releaserReference));
				
		//CREATE NAME UPDATE PROCESS 
		cancelNameSaleTransaction = new CancelSellNameTransaction(maker, "test", FEE_POWER, timestamp, last_ref);
		cancelNameSaleTransaction.sign(maker, false);
		cancelNameSaleTransaction.process(databaseSet, gb, false);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.NAME_NOT_FOR_SALE, cancelNameSaleTransaction.isValid(databaseSet, releaserReference));
	}

	@Test
	public void parseCancelSellNameTransaction() 
	{
		
		init();
				
		//CREATE CANCEL NAME SALE
		CancelSellNameTransaction cancelNameSaleTransaction = new CancelSellNameTransaction(maker, "test", FEE_POWER, timestamp, last_ref);
		cancelNameSaleTransaction.sign(maker, false);
		
		//CONVERT TO BYTES
		byte[] rawCancelNameSale = cancelNameSaleTransaction.toBytes(true, null);
		
		try 
		{	
			//PARSE FROM BYTES
			CancelSellNameTransaction parsedCancelNameSale = (CancelSellNameTransaction) TransactionFactory.getInstance().parse(rawCancelNameSale, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedCancelNameSale instanceof CancelSellNameTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(cancelNameSaleTransaction.getSignature(), parsedCancelNameSale.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(cancelNameSaleTransaction.getAmount(maker), parsedCancelNameSale.getAmount(maker));	
			
			//CHECK OWNER
			assertEquals(cancelNameSaleTransaction.getCreator().getAddress(), parsedCancelNameSale.getCreator().getAddress());	
			
			//CHECK NAME
			assertEquals(cancelNameSaleTransaction.getName(), parsedCancelNameSale.getName());	
			
			//CHECK FEE
			assertEquals(cancelNameSaleTransaction.getFee(), parsedCancelNameSale.getFee());	
			
			//CHECK REFERENCE
			assertEquals(cancelNameSaleTransaction.getReference(), parsedCancelNameSale.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(cancelNameSaleTransaction.getTimestamp(), parsedCancelNameSale.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawCancelNameSale = new byte[cancelNameSaleTransaction.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawCancelNameSale, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}
	
	@Test
	public void processCancelSellNameTransaction()
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		nameRegistration.process(databaseSet, gb, false);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		nameSaleTransaction.process(databaseSet, gb, false);
		
		//CREATE SIGNATURE
			
		//CREATE CANCEL NAME SALE
		Transaction cancelNameSaleTransaction = new CancelSellNameTransaction(maker, "test", FEE_POWER, timestamp, last_ref);
		cancelNameSaleTransaction.sign(maker, false);
		cancelNameSaleTransaction.process(databaseSet, gb, false);	
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(cancelNameSaleTransaction.getTimestamp(), maker.getLastReference(databaseSet));
				
		//CHECK NAME SALE EXISTS
		assertEquals(false, databaseSet.getNameExchangeMap().contains("test"));
	}

	@Test
	public void orphanCancelSellNameTransaction()
	{
		
		init();

		Name name = new Name(maker, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		nameRegistration.process(databaseSet, gb, false);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		nameSaleTransaction.process(databaseSet, gb, false);
		
		//CREATE SIGNATURE
			
		//CREATE CANCEL NAME SALE
		Transaction cancelNameSaleTransaction = new CancelSellNameTransaction(maker, "test", FEE_POWER, timestamp, last_ref);
		cancelNameSaleTransaction.sign(maker, false);
		cancelNameSaleTransaction.process(databaseSet, gb, false);	
		cancelNameSaleTransaction.orphan(databaseSet, false);
		
		//CHECK BALANCE SENDER
		assertEquals(0,BigDecimal.valueOf(1000).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals((long)nameSaleTransaction.getTimestamp(), (long)maker.getLastReference(databaseSet));
				
		//CHECK NAME SALE EXISTS
		assertEquals(true, databaseSet.getNameExchangeMap().contains("test"));
		
		//CHECK NAME SALE AMOUNT
		nameSale =  databaseSet.getNameExchangeMap().getNameSale("test");
		assertEquals(BigDecimal.valueOf(1000).setScale(8), nameSale.getAmount());
	}
	
	//BUY NAME
	
	@Test
	public void validateSignatureBuyNameTransaction()
	{
		
		init();

		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction buyNameTransaction = new BuyNameTransaction(maker, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, last_ref);
		buyNameTransaction.sign(maker, false);
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(true, buyNameTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		buyNameTransaction = new BuyNameTransaction(
				maker, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, last_ref, new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, buyNameTransaction.isSignatureValid());
	}
	
	@Test
	public void validateBuyNameTransaction() 
	{
		
		init();
				
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(maker, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameRegistration.isValid(databaseSet, releaserReference));
		nameRegistration.process(databaseSet, gb, false);
		
		//CREATE NAME SALE
		BigDecimal bdAmoSell = BigDecimal.valueOf(700).setScale(8);
		NameSale nameSale = new NameSale("test", bdAmoSell);
		Transaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
	
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameSaleTransaction.isValid(databaseSet, releaserReference));
		nameSaleTransaction.process(databaseSet, gb, false);
		
		//CREATE NAME PURCHASE
		BuyNameTransaction namePurchaseTransaction = new BuyNameTransaction(buyer, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));
		namePurchaseTransaction.sign(buyer, false);

		//CHECK IF NAME PURCHASE IS VALID
		assertEquals(Transaction.VALIDATE_OK, namePurchaseTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME PURCHASE INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		
		NameSale nameSaleInvalid = new NameSale(longName, nameSale.getAmount());
		//nameSale = new NameSale(longName, nameSale.getAmount());
		//LOGGER.info("nameSaleLong " + nameSaleLong);
		//LOGGER.info("nameSaleLong getOwner "  + nameSaleLong.getName(databaseSet).getOwner());
		//// nameSaleLong --- nameSale -> owner
		namePurchaseTransaction = new BuyNameTransaction(buyer, nameSaleInvalid, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));		

		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, namePurchaseTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME PURCHASE NAME DOES NOT EXIST
		nameSaleInvalid = new NameSale("test2", BigDecimal.valueOf(1000).setScale(8));
		namePurchaseTransaction = new BuyNameTransaction(buyer, nameSaleInvalid, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));		
		
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NAME_DOES_NOT_EXIST, namePurchaseTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID NAME PURCHASE NAME NOT FOR SALE
		Name test2 = new Name(maker, "test2", "oke");
		databaseSet.getNameMap().add(test2);
		
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NAME_NOT_FOR_SALE, namePurchaseTransaction.isValid(databaseSet, releaserReference));
						
		//CREATE INVALID NAME PURCHASE ALREADY OWNER
		nameSale = new NameSale("test", bdAmoSell);
		namePurchaseTransaction = new BuyNameTransaction(maker, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, last_ref);		
		
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.BUYER_ALREADY_OWNER, namePurchaseTransaction.isValid(databaseSet, releaserReference));
				
		//CREATE INVALID NAME UPDATE NO BALANCE
		buyer.changeBalance(databaseSet, false, FEE_KEY, BigDecimal.ZERO.setScale(8));
		namePurchaseTransaction = new BuyNameTransaction(buyer,nameSale,nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));		
		
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NO_BALANCE, namePurchaseTransaction.isValid(databaseSet, releaserReference));

		// setConfirmedBalance(long key, BigDecimal amount, DBSet db)
		buyer.changeBalance(databaseSet, false, FEE_KEY, BigDecimal.valueOf(2000).setScale(8));
				
		//CREATE NAME UPDATE INVALID REFERENCE
		namePurchaseTransaction = new BuyNameTransaction(buyer, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, -123L);		
				
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, namePurchaseTransaction.isValid(databaseSet, releaserReference));
	}

	@Test
	public void parseBuyNameTransaction() 
	{
		

		init();
		
		////////////// FIRST
		//CREATE NAME SALE
		BigDecimal bdAmoSell = BigDecimal.valueOf(700).setScale(8);
		NameSale nameSale = new NameSale("test", bdAmoSell);
		SellNameTransaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		//nameSaleTransaction.process();
		nameSaleTransaction.process(databaseSet, gb, false);

		LOGGER.addAppender(null);
		LOGGER.debug("nameSale ");
		LOGGER.info("nameSale " + nameSale.getName(databaseSet));
		LOGGER.info("nameSale " + nameSale.getName(databaseSet));
		//LOGGER.info("nameSale " + nameSale.getName(databaseSet).getOwner());

		//CREATE CANCEL NAME SALE
		BuyNameTransaction namePurchaseTransaction = new BuyNameTransaction(maker, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, last_ref);	
		namePurchaseTransaction.sign(maker, false);
		//CONVERT TO BYTES
		byte[] rawNamePurchase = namePurchaseTransaction.toBytes(true, null);
		
		try 
		{	
			//PARSE FROM BYTES
			BuyNameTransaction parsedNamePurchase = (BuyNameTransaction) TransactionFactory.getInstance().parse(rawNamePurchase, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedNamePurchase instanceof BuyNameTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(namePurchaseTransaction.getSignature(), parsedNamePurchase.getSignature()));
			
			//CHECK AMOUNT BUYER
			assertEquals(namePurchaseTransaction.getAmount(maker), parsedNamePurchase.getAmount(maker));	
			
			//CHECK OWNER
			assertEquals(namePurchaseTransaction.getCreator().getAddress(), parsedNamePurchase.getCreator().getAddress());	
			
			//CHECK NAME
			assertEquals(namePurchaseTransaction.getNameSale().getKey(), parsedNamePurchase.getNameSale().getKey());	
		
			//CHECK FEE
			assertEquals(namePurchaseTransaction.getFee(), parsedNamePurchase.getFee());	
			
			//CHECK REFERENCE
			assertEquals(namePurchaseTransaction.getReference(), parsedNamePurchase.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(namePurchaseTransaction.getTimestamp(), parsedNamePurchase.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawNamePurchase = new byte[namePurchaseTransaction.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawNamePurchase, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}
	
	@Test
	public void processBuyNameTransaction()
	{
		
		init();
				
		Name name = new Name(maker, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		nameRegistration.process(databaseSet, gb, false);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(500).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		nameSaleTransaction.process(databaseSet, gb, false);
		
		//CREATE SIGNATURE
			
		//CREATE NAME PURCHASE
		Transaction purchaseNameTransaction = new BuyNameTransaction(buyer, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));
		purchaseNameTransaction.sign(buyer, false);
		purchaseNameTransaction.process(databaseSet, gb, false);	
		
		//CHECK BALANCE SENDER
		//assertEquals(BigDecimal.valueOf(498).setScale(8), buyer.getConfirmedBalance(FEE_KEY, databaseSet));
		assertEquals(0, BigDecimal.valueOf(500).setScale(8).compareTo(buyer.getBalanceUSE(FEE_KEY, databaseSet)));
		
		//CHECK BALANCE SELLER
		assertEquals(0, BigDecimal.valueOf(1500).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
						
		//CHECK REFERENCE BUYER
		assertEquals(purchaseNameTransaction.getTimestamp(), buyer.getLastReference(databaseSet));
				
		//CHECK NAME OWNER
		name = databaseSet.getNameMap().get("test");
		assertEquals(name.getOwner().getAddress(), buyer.getAddress());
	
		//CHECK NAME SALE EXISTS
		assertEquals(false, databaseSet.getNameExchangeMap().contains("test"));
	}

	@Test
	public void orphanBuyNameTransaction()
	{

		init();
		
		Name name = new Name(maker, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(maker, name, FEE_POWER, timestamp, last_ref);
		nameRegistration.sign(maker, false);
		nameRegistration.process(databaseSet, gb, false);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(maker, nameSale, FEE_POWER, timestamp, last_ref);
		nameSaleTransaction.sign(maker, false);
		nameSaleTransaction.process(databaseSet, gb, false);
		
		//CREATE SIGNATURE
			
		//CREATE NAME PURCHASE
		Transaction purchaseNameTransaction = new BuyNameTransaction(buyer, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));
		purchaseNameTransaction.sign(buyer, false);
		purchaseNameTransaction.process(databaseSet, gb, false);	
		purchaseNameTransaction.orphan(databaseSet, false);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(1000).setScale(8), buyer.getBalanceUSE(FEE_KEY, databaseSet));
		
		//CHECK BALANCE SELLER
		assertEquals(0, BigDecimal.valueOf(9999).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
						
		//CHECK REFERENCE BUYER
		assertEquals(last_ref, buyer.getLastReference(databaseSet));
				
		//CHECK NAME OWNER
		name = databaseSet.getNameMap().get("test");
		assertEquals(name.getOwner().getAddress(), maker.getAddress());
	
		//CHECK NAME SALE EXISTS
		assertEquals(true, databaseSet.getNameExchangeMap().contains("test"));
	}
	
	/////////////////////////////////////////////////
	//CREATE POLL
	
	@Test
	public void validateSignatureCreatePollTransaction() 
	{
		
		init();
		
		//CREATE POLL
		Poll poll = new Poll(maker, "test", "this is the value", new ArrayList<PollOption>());
				
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);
		pollCreation.sign(maker, false);
		//CHECK IF POLL CREATION IS VALID
		assertEquals(true, pollCreation.isSignatureValid());
		
		//INVALID SIGNATURE
		pollCreation = new CreatePollTransaction(
				maker, poll, FEE_POWER, timestamp, last_ref, new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, pollCreation.isSignatureValid());
	}
		
	@Test
	public void validateCreatePollTransaction() 
	{
		
		init();

		Poll poll = new Poll(maker, "test", "this is the value", Arrays.asList(new PollOption("test")));
				
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);
		pollCreation.sign(maker, false);
		
		//CHECK IF POLL CREATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, pollCreation.isValid(databaseSet, releaserReference));
		pollCreation.process(databaseSet, gb, false);
		
		//CREATE INVALID POLL CREATION INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		poll = new Poll(maker, longName, "this is the value", Arrays.asList(new PollOption("test")));
		pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);		

		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, pollCreation.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID POLL CREATION INVALID DESCRIPTION LENGTH
		String longDescription = "";
		for(int i=1; i<10000; i++)
		{
			longDescription += "oke";
		}
		poll = new Poll(maker, "test2", longDescription, Arrays.asList(new PollOption("test")));
		pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);		

		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.INVALID_DESCRIPTION_LENGTH, pollCreation.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID POLL CREATION NAME ALREADY TAKEN
		poll = new Poll(maker, "test", "this is the value", Arrays.asList(new PollOption("test")));
		pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);		
		
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.POLL_ALREADY_CREATED, pollCreation.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID POLL CREATION NO OPTIONS 
		poll = new Poll(maker, "test2", "this is the value", new ArrayList<PollOption>());
		pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);		
		
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.INVALID_OPTIONS_LENGTH, pollCreation.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID POLL CREATION INVALID OPTION LENGTH
		poll = new Poll(maker, "test2", "this is the value", Arrays.asList(new PollOption(longName)));
		pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);		
				
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.INVALID_OPTION_LENGTH, pollCreation.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID POLL CREATION INVALID DUPLICATE OPTIONS
		poll = new Poll(maker, "test2", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("test")));
		pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);		
						
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.DUPLICATE_OPTION, pollCreation.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID POLL CREATION NOT ENOUGH BALANCE
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		invalidOwner.setLastReference(last_ref, databaseSet);
		poll = new Poll(maker, "test2", "this is the value", Arrays.asList(new PollOption("test")));
		pollCreation = new CreatePollTransaction(invalidOwner, poll, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));
		// need for calc FEE
		pollCreation.sign(invalidOwner, false);
		
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.NOT_ENOUGH_FEE, pollCreation.isValid(databaseSet, releaserReference));
		
		//CREATE POLL CREATION INVALID REFERENCE
		poll = new Poll(maker, "test2", "this is the value", Arrays.asList(new PollOption("test")));
		pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));		
		assertEquals(Transaction.INVALID_REFERENCE, pollCreation.isValid(databaseSet, releaserReference));
		
	}

	@Test
	public void parseCreatePollTransaction() 
	{
		
		init();

		Poll poll = new Poll(maker, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("second option")));
				
		//CREATE POLL CREATION
		CreatePollTransaction pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);
		pollCreation.sign(maker, false);
		
		//CONVERT TO BYTES
		byte[] rawPollCreation = pollCreation.toBytes(true, null);
		
		try 
		{	
			//PARSE FROM BYTES
			CreatePollTransaction parsedPollCreation = (CreatePollTransaction) TransactionFactory.getInstance().parse(rawPollCreation, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedPollCreation instanceof CreatePollTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(pollCreation.getSignature(), parsedPollCreation.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(pollCreation.getAmount(maker), parsedPollCreation.getAmount(maker));	
			
			//CHECK POLL CREATOR
			assertEquals(pollCreation.getPoll().getCreator().getAddress(), parsedPollCreation.getPoll().getCreator().getAddress());	
			
			//CHECK POLL NAME
			assertEquals(pollCreation.getPoll().getName(), parsedPollCreation.getPoll().getName());	
			
			//CHECK POLL DESCRIPTION
			assertEquals(pollCreation.getPoll().getDescription(), parsedPollCreation.getPoll().getDescription());	
			
			//CHECK POLL OPTIONS SIZE
			assertEquals(pollCreation.getPoll().getOptions().size(), parsedPollCreation.getPoll().getOptions().size());	
			
			//CHECK POLL OPTIONS
			for(int i=0; i<pollCreation.getPoll().getOptions().size(); i++)
			{
				//CHECK OPTION NAME
				assertEquals(pollCreation.getPoll().getOptions().get(i).getName(), parsedPollCreation.getPoll().getOptions().get(i).getName());	
			}
			
			//CHECK FEE
			assertEquals(pollCreation.getFee(), parsedPollCreation.getFee());	
			
			//CHECK REFERENCE
			assertEquals(pollCreation.getReference(), parsedPollCreation.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(pollCreation.getTimestamp(), parsedPollCreation.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawPollCreation = new byte[pollCreation.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawPollCreation, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}

	@Test
	public void processCreatePollTransaction()
	{
		
		init();

		Poll poll = new Poll(maker, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("second option")));
						
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);
		pollCreation.sign(maker, false);
		pollCreation.process(databaseSet, gb, false);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1).subtract(pollCreation.getFee()).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
				
		//CHECK REFERENCE SENDER
		assertEquals(pollCreation.getTimestamp(), maker.getLastReference(databaseSet));
		
		//CHECK POLL EXISTS
		assertEquals(true, databaseSet.getPollMap().contains(poll));
	}
	
	@Test
	public void orphanCreatePollTransaction()
	{
		
		init();

		Poll poll = new Poll(maker, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("second option")));
						
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);
		pollCreation.sign(maker, false);
		pollCreation.process(databaseSet, gb, false);
		pollCreation.orphan(databaseSet, false);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(1).setScale(8), maker.getBalanceUSE(FEE_KEY, databaseSet));
				
		//CHECK REFERENCE SENDER
		assertEquals(last_ref, maker.getLastReference(databaseSet));
		
		//CHECK POLL EXISTS
		assertEquals(false, databaseSet.getPollMap().contains(poll));
	}
	
	//VOTE ON POLL
	
	@Test
	public void validateSignatureVoteOnPollTransaction() 
	{
		
		init();
		
		//CREATE POLL VOTE
		Transaction pollVote = new VoteOnPollTransaction(maker, "test", 5, FEE_POWER, timestamp, last_ref);
		pollVote.sign(maker, false);
		//CHECK IF POLL VOTE IS VALID
		assertEquals(true, pollVote.isSignatureValid());
		
		//INVALID SIGNATURE
		pollVote = new VoteOnPollTransaction(
				maker, "test", 5, FEE_POWER, timestamp, last_ref, new byte[64]);
		
		//CHECK IF POLL VOTE IS INVALID
		assertEquals(false, pollVote.isSignatureValid());
	}
		
	@Test
	public void validateVoteOnPollTransaction() 
	{
		
		init();
		
		//CREATE SIGNATURE
		//LOGGER.info("asdasd");
		long timestamp = NTP.getTime();
		Poll poll = new Poll(maker, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("test2")));
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);
		pollCreation.sign(maker, false);
		
		//CHECK IF POLL CREATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, pollCreation.isValid(databaseSet, releaserReference));
		pollCreation.process(databaseSet, gb, false);
		
		//CREATE POLL VOTE
		Transaction pollVote = new VoteOnPollTransaction(maker, poll.getName(), 0, FEE_POWER, timestamp + 100, maker.getLastReference(databaseSet));
		pollVote.sign(maker, false);
		
		//CHECK IF POLL VOTE IS VALID
		assertEquals(Transaction.VALIDATE_OK, pollVote.isValid(databaseSet, releaserReference));
		//pollVote.process(databaseSet, false);
		
		//CREATE INVALID POLL VOTE INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		pollVote = new VoteOnPollTransaction(maker, longName, 0, FEE_POWER, timestamp, last_ref);	

		//CHECK IF POLL VOTE IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, pollVote.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID POLL VOTE POLL DOES NOT EXIST
		pollVote = new VoteOnPollTransaction(maker, "test2", 0, FEE_POWER, timestamp, last_ref);	
		
		//CHECK IF POLL VOTE IS INVALID
		assertEquals(Transaction.POLL_NOT_EXISTS, pollVote.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID POLL VOTE INVALID OPTION
		pollVote = new VoteOnPollTransaction(maker, "test", 5, FEE_POWER, timestamp, last_ref);	
		
		//CHECK IF POLL VOTE IS INVALID
		assertEquals(Transaction.POLL_OPTION_NOT_EXISTS, pollVote.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID POLL VOTE INVALID OPTION
		pollVote = new VoteOnPollTransaction(maker, "test", -1, FEE_POWER, timestamp, last_ref);	
				
		//CHECK IF POLL VOTE IS INVALID
		assertEquals(Transaction.POLL_OPTION_NOT_EXISTS, pollVote.isValid(databaseSet, releaserReference));
		
		//CRTEATE INVALID POLL VOTE VOTED ALREADY
		pollVote = new VoteOnPollTransaction(maker, "test", 0, FEE_POWER, timestamp, last_ref);
		pollVote.sign(maker, false);
		pollVote.process(databaseSet, gb, false);
		
		//CHECK IF POLL VOTE IS INVALID
		assertEquals(Transaction.ALREADY_VOTED_FOR_THAT_OPTION, pollVote.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID POLL VOTE NOT ENOUGH BALANCE
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		invalidOwner.setLastReference(timestamp, databaseSet);
		pollVote = new VoteOnPollTransaction(invalidOwner, "test", 0, FEE_POWER, timestamp, last_ref);
		pollVote.sign(invalidOwner, false);
		

		//CHECK IF POLL VOTE IS INVALID
		///LOGGER.info("pollVote.getFee: " + pollVote.getFee());
		/// fee = 0 assertEquals(Transaction.NOT_ENOUGH_FEE, pollVote.isValid(databaseSet));
		
		//CREATE POLL CREATION INVALID REFERENCE
		pollVote = new VoteOnPollTransaction(maker, "test", 1, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet) + 1);	
		assertEquals(Transaction.INVALID_REFERENCE, pollVote.isValid(databaseSet, releaserReference));
		
	}

	
	@Test
	public void parseVoteOnPollTransaction() 
	{
		
		init();
				
		//CREATE POLL Vote
		VoteOnPollTransaction pollVote = new VoteOnPollTransaction(maker, "test", 0, FEE_POWER, timestamp, last_ref);
		pollVote.sign(maker, false);
		
		//CONVERT TO BYTES
		byte[] rawPollVote = pollVote.toBytes(true, null);
		assertEquals(rawPollVote.length, pollVote.getDataLength(false));
		
		try 
		{	
			//PARSE FROM BYTES
			VoteOnPollTransaction parsedPollVote = (VoteOnPollTransaction) TransactionFactory.getInstance().parse(rawPollVote, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedPollVote instanceof VoteOnPollTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(pollVote.getSignature(), parsedPollVote.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(pollVote.getAmount(maker), parsedPollVote.getAmount(maker));	
			
			//CHECK CREATOR
			assertEquals(pollVote.getCreator().getAddress(), parsedPollVote.getCreator().getAddress());	
			
			//CHECK POLL
			assertEquals(pollVote.getPoll(), parsedPollVote.getPoll());	
			
			//CHECK POLL OPTION
			assertEquals(pollVote.getOption(), parsedPollVote.getOption());	
			
			//CHECK FEE
			assertEquals(pollVote.getFee(), parsedPollVote.getFee());	
			
			//CHECK REFERENCE
			assertEquals(pollVote.getReference(), parsedPollVote.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(pollVote.getTimestamp(), parsedPollVote.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction. " + e);
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawPollVote = new byte[pollVote.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawPollVote, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}

	
	@Test
	public void processVoteOnPollTransaction()
	{
		
		init();

		Poll poll = new Poll(maker, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("second option")));
						
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);
		pollCreation.sign(maker, false);
		pollCreation.process(databaseSet, gb, false);

		BigDecimal bal = maker.getBalanceUSE(FEE_KEY, databaseSet);
		
		//CREATE POLL VOTE
		Transaction pollVote = new VoteOnPollTransaction(maker, poll.getName(), 0, FEE_POWER, timestamp + 100, maker.getLastReference(databaseSet));
		pollVote.sign(maker, false);
		assertEquals(Transaction.VALIDATE_OK, pollVote.isValid(databaseSet, releaserReference));
		
		pollVote.process(databaseSet, gb, false);
		
		//CHECK BALANCE SENDER
		assertEquals(0, bal.compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
				
		// NOT NEED !!!! vote not use FEE and not change REFERENCE
		///////CHECK REFERENCE SENDER
		///////assertEquals(true, Arrays.equals(pollVote.getTimestamp(), maker.getLastReference(databaseSet)));
		
		//CHECK POLL VOTER
		assertEquals(true, databaseSet.getPollMap().get(poll.getName()).getOptions().get(0).hasVoter(maker));
		
		//CREATE POLL VOTE
		pollVote = new VoteOnPollTransaction(maker, poll.getName(), 1, FEE_POWER, timestamp, last_ref);
		pollVote.sign(maker, false);
		pollVote.process(databaseSet, gb, false);
				
		//CHECK BALANCE SENDER
		assertEquals(0, bal.compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
						
		// NOT NEED !!!! vote not use FEE and not change REFERENCE
		/////CHECK REFERENCE SENDER
		/////assertEquals(true, Arrays.equals(pollVote.getTimestamp(), maker.getLastReference(databaseSet)));
				
		//CHECK POLL VOTER
		//assertEquals(false, databaseSet.getPollMap().get(poll.getName()).getOptions().get(0).hasVoter(maker));
		
		//CHECK POLL VOTER
		assertEquals(true, databaseSet.getPollMap().get(poll.getName()).getOptions().get(1).hasVoter(maker));
	}
	
	@Test
	public void orphanVoteOnPollTransaction()
	{
		
		init();

		Poll poll = new Poll(maker, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("second option")));
						
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(maker, poll, FEE_POWER, timestamp, last_ref);
		pollCreation.sign(maker, false);
		pollCreation.process(databaseSet, gb, false);
		
		//CREATE POLL VOTE
		Transaction pollVote = new VoteOnPollTransaction(maker, poll.getName(), 0, FEE_POWER, timestamp, last_ref);
		pollVote.sign(maker, false);
		pollVote.process(databaseSet, gb, false);
		pollVote.orphan(databaseSet, false);
		
		//CHECK BALANCE SENDER
		//assertEquals(0, maker.getConfirmedBalance(FEE_KEY, databaseSet));
		assertEquals(1, BigDecimal.valueOf(1).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
				
		//CHECK REFERENCE SENDER
		assertEquals(pollCreation.getTimestamp(), maker.getLastReference(databaseSet));
		
		//CHECK POLL VOTER
		assertEquals(false, databaseSet.getPollMap().get(poll.getName()).hasVotes());
		
		//CHECK POLL VOTER
		assertEquals(false, databaseSet.getPollMap().get(poll.getName()).getOptions().get(0).hasVoter(maker));

	}
	
	//ARBITRARY TRANSACTION
	
	@Test
	public void validateSignatureArbitraryTransaction() 
	{
		
		init();
		
		//CREATE ARBITRARY TRANSACTION
		Transaction arbitraryTransaction = new ArbitraryTransactionV3(maker, null, 4889, "test".getBytes(), FEE_POWER, timestamp, last_ref);
		arbitraryTransaction.sign(maker, false);
		
		//CHECK IF ARBITRARY TRANSACTION IS VALID
		assertEquals(true, arbitraryTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		arbitraryTransaction = new ArbitraryTransactionV3(
				maker, null, 4889, "test".getBytes(), FEE_POWER, timestamp, last_ref, new byte[64]);
		//arbitraryTransaction.sign(maker);
		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(false, arbitraryTransaction.isSignatureValid());
	}
		
	@Test
	public void validateArbitraryTransaction() 
	{
		
		init();

		byte[] data = "test".getBytes();
				
		//CREATE ARBITRARY TRANSACTION
		Transaction arbitraryTransaction = new ArbitraryTransactionV3(maker, null, 4776, data, FEE_POWER, timestamp, last_ref);	
		arbitraryTransaction.sign(maker, false);		

		//CHECK IF ARBITRARY TRANSACTION IS VALID
		assertEquals(Transaction.VALIDATE_OK, arbitraryTransaction.isValid(databaseSet, releaserReference));
		arbitraryTransaction.process(databaseSet, gb, false);
		
		//CREATE INVALID ARBITRARY TRANSACTION INVALID data LENGTH
		byte[] longData = new byte[5000];
		arbitraryTransaction = new ArbitraryTransactionV3(maker, null, 4776, longData, FEE_POWER, timestamp, last_ref);	

		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.INVALID_DATA_LENGTH, arbitraryTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE INVALID ARBITRARY TRANSACTION NOT ENOUGH BALANCE
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		arbitraryTransaction = new ArbitraryTransactionV3(invalidOwner, null, 4776, data, FEE_POWER, timestamp, last_ref);	
		
		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.NO_BALANCE, arbitraryTransaction.isValid(databaseSet, releaserReference));
		
		//CREATE ARBITRARY TRANSACTION INVALID REFERENCE
		arbitraryTransaction = new ArbitraryTransactionV3(maker, null, 4776, data, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));	
		
		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, arbitraryTransaction.isValid(databaseSet, releaserReference));
		
	}

	
	@Test
	public void parseArbitraryTransaction() 
	{
		
		init();
				
		//CREATE ARBITRARY TRANSACTION
		ArbitraryTransactionV3 arbitraryTransaction = new ArbitraryTransactionV3(maker, null, 4776,"test".getBytes(), FEE_POWER, timestamp, last_ref);
		arbitraryTransaction.sign(maker, false);
		
		//CONVERT TO BYTES
		byte[] rawArbitraryTransaction = arbitraryTransaction.toBytes(true, null);
		
		try 
		{	
			//PARSE FROM BYTES
			ArbitraryTransactionV3 parsedArbitraryTransaction = (ArbitraryTransactionV3) TransactionFactory.getInstance().parse(rawArbitraryTransaction, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedArbitraryTransaction instanceof ArbitraryTransactionV3);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(arbitraryTransaction.getSignature(), parsedArbitraryTransaction.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(arbitraryTransaction.getAmount(maker), parsedArbitraryTransaction.getAmount(maker));	
			
			//CHECK CREATOR
			assertEquals(arbitraryTransaction.getCreator().getAddress(), parsedArbitraryTransaction.getCreator().getAddress());	
			
			//CHECK VERSION
			assertEquals(arbitraryTransaction.getService(), parsedArbitraryTransaction.getService());	
			
			//CHECK DATA
			assertEquals(true, Arrays.equals(arbitraryTransaction.getData(), parsedArbitraryTransaction.getData()));	
			
			//CHECK FEE
			assertEquals(arbitraryTransaction.getFee(), parsedArbitraryTransaction.getFee());	
			
			//CHECK REFERENCE
			assertEquals(arbitraryTransaction.getReference(), parsedArbitraryTransaction.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(arbitraryTransaction.getTimestamp(), parsedArbitraryTransaction.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawArbitraryTransaction = new byte[arbitraryTransaction.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawArbitraryTransaction, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}

	
	@Test
	public void processArbitraryTransaction()
	{
		
		init();
						
		//CREATE ARBITRARY TRANSACTION
		ArbitraryTransactionV3 arbitraryTransaction = new ArbitraryTransactionV3(maker, null, 4776,"test".getBytes(), FEE_POWER, timestamp, last_ref);
		arbitraryTransaction.sign(maker, false);
		arbitraryTransaction.process(databaseSet, gb, false);				
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1).subtract(arbitraryTransaction.getFee()).setScale(8).compareTo(maker.getBalanceUSE(FEE_KEY, databaseSet)));
				
		//CHECK REFERENCE SENDER
		assertEquals(arbitraryTransaction.getTimestamp(), maker.getLastReference(databaseSet));
	}
	
	@Test
	public void orphanArbitraryTransaction()
	{
		
		init();
		
		//CREATE ARBITRARY TRANSACTION
		ArbitraryTransactionV3 arbitraryTransaction = new ArbitraryTransactionV3(maker, null, 4776,"test".getBytes(), FEE_POWER, timestamp, last_ref);
		arbitraryTransaction.sign(maker, false);
		arbitraryTransaction.process(databaseSet, gb, false);	
		arbitraryTransaction.orphan(databaseSet, false);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(1).setScale(8), maker.getBalanceUSE(FEE_KEY, databaseSet));
				
		//CHECK REFERENCE SENDER
		assertEquals(last_ref, maker.getLastReference(databaseSet));
	}

	/*@Test
	public void validateArbitraryTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount maker = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(maker, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		byte[] data = "test".getBytes();
				
		//CREATE ARBITRARY TRANSACTION
		Transaction arbitraryTransaction = new ArbitraryTransactionV3(maker, null, 4776, data, FEE_POWER, timestamp, last_ref);	
		
		//CHECK IF ARBITRARY TRANSACTION IS VALID
		assertEquals(Transaction.VALIDATE_OK, arbitraryTransaction.isValid(databaseSet));
		arbitraryTransaction.process(databaseSet);
		
		//CREATE INVALID ARBITRARY TRANSACTION INVALID data LENGTH
		byte[] longData = new byte[5000];
		arbitraryTransaction = new ArbitraryTransactionV3(maker, null, 4776, longData, FEE_POWER, timestamp, last_ref);	

		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.INVALID_DATA_LENGTH, arbitraryTransaction.isValid(databaseSet));
		
		//CREATE INVALID ARBITRARY TRANSACTION NOT ENOUGH BALANCE
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		arbitraryTransaction = new ArbitraryTransactionV1(invalidOwner, 4776, data, FEE_POWER, timestamp, last_ref);	
		
		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.NO_BALANCE, arbitraryTransaction.isValid(databaseSet));
		
		//CREATE ARBITRARY TRANSACTION INVALID REFERENCE
		arbitraryTransaction = new ArbitraryTransactionV3(maker, null, 4776, data, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));	
		
		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, arbitraryTransaction.isValid(databaseSet));
		
	}*/

	//ISSUE ASSET TRANSACTION
	
	@Test
	public void validateSignatureIssueAssetTransaction() 
	{
		
		init();
		
		//CREATE ASSET
		AssetCls asset = new AssetVenture(maker, "test", icon, image, "strontje", false, 50000l, (byte) 2, false);
		//byte[] data = asset.toBytes(false);
		//Asset asset2 = Asset.parse(data);
		
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE ISSUE ASSET TRANSACTION
		Transaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker, false);
		
		//CHECK IF ISSUE ASSET TRANSACTION IS VALID
		assertEquals(true, issueAssetTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		issueAssetTransaction = new IssueAssetTransaction(
				maker, asset, FEE_POWER, timestamp, maker.getLastReference(db), new byte[64]);
		
		//CHECK IF ISSUE ASSET IS INVALID
		assertEquals(false, issueAssetTransaction.isSignatureValid());
	}
		

	
	@Test
	public void parseIssueAssetTransaction() 
	{
		
		init();
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		AssetCls asset = new AssetVenture(maker, "test", icon, image, "strontje", false, 50000l, (byte) 2, false);
				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker, false);
		issueAssetTransaction.process(db, gb, false);
		
		//CONVERT TO BYTES
		byte[] rawIssueAssetTransaction = issueAssetTransaction.toBytes(true, null);
		
		//CHECK DATA LENGTH
		assertEquals(rawIssueAssetTransaction.length, issueAssetTransaction.getDataLength(false));
		
		try 
		{	
			//PARSE FROM BYTES
			IssueAssetTransaction parsedIssueAssetTransaction = (IssueAssetTransaction) TransactionFactory.getInstance().parse(rawIssueAssetTransaction, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedIssueAssetTransaction instanceof IssueAssetTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(issueAssetTransaction.getSignature(), parsedIssueAssetTransaction.getSignature()));
			
			//CHECK ISSUER
			assertEquals(issueAssetTransaction.getCreator().getAddress(), parsedIssueAssetTransaction.getCreator().getAddress());
			
			//CHECK OWNER
			assertEquals(issueAssetTransaction.getItem().getOwner().getAddress(), parsedIssueAssetTransaction.getItem().getOwner().getAddress());
			
			//CHECK NAME
			assertEquals(issueAssetTransaction.getItem().getName(), parsedIssueAssetTransaction.getItem().getName());
				
			//CHECK DESCRIPTION
			assertEquals(issueAssetTransaction.getItem().getDescription(), parsedIssueAssetTransaction.getItem().getDescription());
				
			//CHECK QUANTITY
			assertEquals(((AssetCls)issueAssetTransaction.getItem()).getQuantity(), ((AssetCls)parsedIssueAssetTransaction.getItem()).getQuantity());
			
			//DIVISIBLE
			assertEquals(((AssetCls)issueAssetTransaction.getItem()).isDivisible(), ((AssetCls)parsedIssueAssetTransaction.getItem()).isDivisible());
			
			//CHECK FEE
			assertEquals(issueAssetTransaction.getFee(), parsedIssueAssetTransaction.getFee());	
			
			//CHECK REFERENCE
			assertEquals(issueAssetTransaction.getReference(), parsedIssueAssetTransaction.getReference());	
			
			//CHECK TIMESTAMP
			assertEquals(issueAssetTransaction.getTimestamp(), parsedIssueAssetTransaction.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawIssueAssetTransaction = new byte[issueAssetTransaction.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawIssueAssetTransaction, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}

	
	@Test
	public void processIssueAssetTransaction()
	{
		
		init();				
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		AssetCls asset = new AssetVenture(maker, "test", icon, image, "strontje", false, 50000l, (byte) 2, false);

				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker, false);
		
		assertEquals(Transaction.VALIDATE_OK, issueAssetTransaction.isValid(db, releaserReference));
		
		issueAssetTransaction.process(db, gb, false);
		
		LOGGER.info("asset KEY: " + asset.getKey(DBSet.getInstance()));
		
		//CHECK BALANCE ISSUER
		assertEquals(BigDecimal.valueOf(50000).setScale(8), maker.getBalanceUSE(asset.getKey(db), db));
		
		//CHECK ASSET EXISTS SENDER
		long key = db.getIssueAssetMap().get(issueAssetTransaction);
		assertEquals(true, db.getItemAssetMap().contains(key));
		
		//CHECK ASSET IS CORRECT
		assertEquals(true, Arrays.equals(db.getItemAssetMap().get(key).toBytes(true), asset.toBytes(true)));
		
		//CHECK ASSET BALANCE SENDER
		assertEquals(true, db.getAssetBalanceMap().get(maker.getAddress(), key).a.compareTo(new BigDecimal(asset.getQuantity())) == 0);
				
		//CHECK REFERENCE SENDER
		assertEquals(issueAssetTransaction.getTimestamp(), maker.getLastReference(db));
	}
	
	
	@Test
	public void orphanIssueAssetTransaction()
	{
		
		init();				
				
		
		long timestamp = NTP.getTime();
		AssetCls asset = new AssetVenture(maker, "test", icon, image, "strontje", false, 50000l, (byte) 2, false);
				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker, false);
		issueAssetTransaction.process(db, gb, false);
		long key = db.getIssueAssetMap().get(issueAssetTransaction);
		assertEquals(new BigDecimal(50000).setScale(8), maker.getBalanceUSE(key,db));
		assertEquals(issueAssetTransaction.getTimestamp(), maker.getLastReference(db));
		
		issueAssetTransaction.orphan(db, false);
		
		//CHECK BALANCE ISSUER
		assertEquals(BigDecimal.ZERO.setScale(8), maker.getBalanceUSE(key,db));
		
		//CHECK ASSET EXISTS SENDER
		assertEquals(false, db.getItemAssetMap().contains(key));
		
		//CHECK ASSET BALANCE SENDER
		assertEquals(0, db.getAssetBalanceMap().get(maker.getAddress(), key).a.longValue());
				
		//CHECK REFERENCE SENDER
		assertEquals(issueAssetTransaction.getReference(), maker.getLastReference(db));
	}
	
}
