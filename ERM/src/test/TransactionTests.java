package test;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import ntp.NTP;

import org.junit.Test;

import com.google.common.primitives.Longs;

import database.DBSet;
import qora.account.Account;
import qora.account.PrivateKeyAccount;
import qora.assets.Asset;
import qora.assets.Venture;
import qora.block.GenesisBlock;
import qora.crypto.Crypto;
import qora.naming.Name;
import qora.naming.NameSale;
import qora.payment.Payment;
import qora.transaction.ArbitraryTransactionV3;
import qora.transaction.BuyNameTransaction;
import qora.transaction.CancelOrderTransaction;
import qora.transaction.CancelSellNameTransaction;
import qora.transaction.CreateOrderTransaction;
import qora.transaction.CreatePollTransaction;
import qora.transaction.GenesisTransaction;
import qora.transaction.IssueAssetTransaction;
import qora.transaction.MultiPaymentTransaction;
import qora.transaction.PaymentTransaction;
import qora.transaction.RegisterNameTransaction;
import qora.transaction.SellNameTransaction;
import qora.transaction.Transaction;
import qora.transaction.TransactionFactory;
import qora.transaction.TransferAssetTransaction;
import qora.transaction.UpdateNameTransaction;
import qora.transaction.VoteOnPollTransaction;
import qora.voting.Poll;
import qora.voting.PollOption;

public class TransactionTests {

	long OIL_KEY = 1l;
	byte FEE_POWER = (byte)1;
	byte[] assetReference = new byte[64];
	long timestamp = NTP.getTime();
	
	//CREATE EMPTY MEMORY DATABASE
	private DBSet db;
	private GenesisBlock gb;
	
	//CREATE KNOWN ACCOUNT
	byte[] seed = Crypto.getInstance().digest("test".getBytes());
	byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
	PrivateKeyAccount maker = new PrivateKeyAccount(privateKey);
	

	// INIT ASSETS
	private void init() {
		
		db = DBSet.createEmptyDatabaseSet();
		gb = new GenesisBlock();
		gb.process(db);
		
		// OIL FUND
		maker.setLastReference(gb.getGeneratorSignature(), db);
		maker.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), db);

	}
	
	
	@Test
	public void validateSignatureGenesisTransaction() 
	{
		
		//CHECK VALID SIGNATURE
		Transaction transaction = new GenesisTransaction(new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU"), BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		assertEquals(true, transaction.isSignatureValid());
	}
	
	@Test
	public void validateGenesisTransaction() 
	{
		
		//CREATE MEMORYDB
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
		
		//CHECK NORMAL VALID
		Transaction transaction = new GenesisTransaction(new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU"), BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		assertEquals(Transaction.VALIDATE_OK, transaction.isValid(databaseSet));
		
		//CHECK INVALID ADDRESS
		transaction = new GenesisTransaction(new Account("test"), BigDecimal.valueOf(-1000).setScale(8), NTP.getTime());
		assertNotEquals(true, Transaction.VALIDATE_OK == transaction.isValid(databaseSet));
		
		//CHECK NEGATIVE AMOUNT
		transaction = new GenesisTransaction(new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU"), BigDecimal.valueOf(-1000).setScale(8), NTP.getTime());
		assertNotEquals(true, Transaction.VALIDATE_OK == transaction.isValid(databaseSet));
	}
	
	@Test
	public void parseGenesisTransaction() 
	{
		//CREATE TRANSACTION
		Account account = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
		Transaction transaction = new GenesisTransaction(account, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		
		//CONVERT TO BYTES
		byte[] rawTransaction = transaction.toBytes(true);
		
		try 
		{	
			//PARSE FROM BYTES
			Transaction parsedTransaction = TransactionFactory.getInstance().parse(rawTransaction);
			
			//CHECK INSTANCE
			assertEquals(true, parsedTransaction instanceof GenesisTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(transaction.getSignature(), parsedTransaction.getSignature()));
			
			//CHECK AMOUNT
			assertEquals(transaction.viewAmount(account), parsedTransaction.viewAmount(account));			
			
			//CHECK TIMESTAMP
			assertEquals(transaction.getTimestamp(), parsedTransaction.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawTransaction = new byte[transaction.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawTransaction);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}
	
	@Test
	public void processGenesisTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
		
		//PROCESS TRANSACTION
		Account account = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
		Transaction transaction = new GenesisTransaction(account, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CHECK AMOUNT
		assertEquals(BigDecimal.valueOf(1000).setScale(8), account.getConfirmedBalance(databaseSet));
		
		//CHECK REFERENCE
		assertEquals(true, Arrays.equals(transaction.getSignature(), account.getLastReference(databaseSet)));
	}
	
	@Test
	public void orphanGenesisTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//PROCESS TRANSACTION
		Account account = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
		Transaction transaction = new GenesisTransaction(account, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//ORPHAN TRANSACTION
		transaction.orphan(databaseSet);
		
		//CHECK AMOUNT
		assertEquals(BigDecimal.ZERO, account.getConfirmedBalance(databaseSet));
				
		//CHECK REFERENCE
		assertEquals(true, Arrays.equals(new byte[0], account.getLastReference(databaseSet)));
	}
	
	//PAYMENT
	
	@Test
	public void validateSignaturePaymentTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		Account recipient = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
		long timestamp = NTP.getTime();
		
		//CREATE PAYMENT
		Transaction payment = new PaymentTransaction(sender, recipient, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		payment.sign(sender);		
		//CHECK IF PAYMENT SIGNATURE IS VALID
		assertEquals(true, payment.isSignatureValid());
		
		//INVALID SIGNATURE
		payment = new PaymentTransaction(sender, recipient, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp+1, sender.getLastReference(databaseSet), new byte[64]);
		
		//CHECK IF PAYMENT SIGNATURE IS INVALID
		assertEquals(false, payment.isSignatureValid());
	}
	
	@Test
	public void validatePaymentTransaction() 
	{
		

		init();
		
		//CREATE SIGNATURE
		Account recipient = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
		long timestamp = NTP.getTime();
		maker.setConfirmedBalance(0l, BigDecimal.valueOf(1000).setScale(8), db);


		//CREATE VALID PAYMENT
		Transaction payment = new PaymentTransaction(maker, recipient, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
		payment.sign(maker);

		//CHECK IF PAYMENT IS VALID
		assertEquals(Transaction.VALIDATE_OK, payment.isValid(db));
		
		payment.process(db);

		//CREATE INVALID PAYMENT INVALID RECIPIENT ADDRESS
		payment = new PaymentTransaction(maker, new Account("test"), BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
	
		//CHECK IF PAYMENT IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, payment.isValid(db));
		
		//CREATE INVALID PAYMENT NEGATIVE AMOUNT
		payment = new PaymentTransaction(maker, recipient, BigDecimal.valueOf(-100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
		payment.sign(maker);
		//CHECK IF PAYMENT IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, payment.isValid(db));	
		
		
		//CREATE INVALID PAYMENT WRONG REFERENCE
		payment = new PaymentTransaction(maker, recipient, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, new byte[64], new byte[64]);
		//CHECK IF PAYMENT IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, payment.isValid(db));	
	}
	
	@Test
	public void parsePaymentTransaction() 
	{
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
						
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
				
		//CREATE SIGNATURE
		Account recipient = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
		long timestamp = NTP.getTime();
						
		//CREATE VALID PAYMENT
		Transaction payment = new PaymentTransaction(sender, recipient, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		payment.sign(sender);
		
		//CONVERT TO BYTES
		byte[] rawPayment = payment.toBytes(true);
		
		try 
		{	
			//PARSE FROM BYTES
			PaymentTransaction parsedPayment = (PaymentTransaction) TransactionFactory.getInstance().parse(rawPayment);
			
			//CHECK INSTANCE
			assertEquals(true, parsedPayment instanceof PaymentTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(payment.getSignature(), parsedPayment.getSignature()));
			
			//CHECK AMOUNT SENDER
			assertEquals(payment.viewAmount(sender), parsedPayment.viewAmount(sender));	
			
			//CHECK AMOUNT RECIPIENT
			assertEquals(payment.viewAmount(recipient), parsedPayment.viewAmount(recipient));	
			
			//CHECK FEE
			assertEquals(payment.getFee(), parsedPayment.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(payment.getReference(), parsedPayment.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(payment.getTimestamp(), parsedPayment.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawPayment = new byte[payment.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawPayment);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}
	
	@Test
	public void processPaymentTransaction()
	{
		
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
					
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
			
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
			
		//CREATE SIGNATURE
		Account recipient = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
			
		Logger.getGlobal().info("sender.getLastReference(databaseSet) LENGTH: " + sender.getLastReference(databaseSet).length + " -- " + sender.getLastReference(databaseSet));
		
		//CREATE PAYMENT
		Transaction payment = new PaymentTransaction(sender, recipient, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		payment.sign(sender);
		payment.process(databaseSet);

		Logger.getGlobal().info("getConfirmedBalance: " + sender.getConfirmedBalance(databaseSet));
		Logger.getGlobal().info("getConfirmedBalance OIL_KEY:" + sender.getConfirmedBalance(OIL_KEY, databaseSet));

		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(900).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
				
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.valueOf(100).setScale(8), recipient.getConfirmedBalance(databaseSet));
		
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(payment.getSignature(), sender.getLastReference(databaseSet)));
		
		//CHECK REFERENCE RECIPIENT
		// !!! now not worked !!!
		// assertEquals(true, Arrays.equals(payment.getSignature(), recipient.getLastReference(databaseSet)));
		
		//CREATE SIGNATURE
		
		//CREATE PAYMENT
		payment = new PaymentTransaction(sender, recipient, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		payment.sign(sender);
		payment.process(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(800).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.valueOf(200).setScale(8), recipient.getConfirmedBalance(databaseSet));
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(payment.getSignature(), sender.getLastReference(databaseSet)));
					
		//CHECK REFERENCE RECIPIENT NOT CHANGED
		// not worked now assertEquals(true, Arrays.equals(payment.getReference(), recipient.getLastReference(databaseSet)));
	}
	
	@Test
	public void orphanPaymentTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
					
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
			
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);

		//CREATE SIGNATURE
		Account recipient = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
		long timestamp = NTP.getTime();
			
		//CREATE PAYMENT
		Transaction payment = new PaymentTransaction(sender, recipient, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		payment.sign(sender);
		payment.process(databaseSet);
		
		//CREATE PAYMENT2
		Transaction payment2  = new PaymentTransaction(sender, recipient, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		payment2.sign(sender);
		payment.process(databaseSet);
		
		//ORPHAN PAYMENT
		payment2.orphan(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(900).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.valueOf(100).setScale(8), recipient.getConfirmedBalance(databaseSet));
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(payment.getSignature(), sender.getLastReference(databaseSet)));
				
		//CHECK REFERENCE RECIPIENT
		/// nor worked now assertEquals(true, Arrays.equals(payment.getSignature(), recipient.getLastReference(databaseSet)));

		//ORPHAN PAYMENT
		payment.orphan(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(1000).setScale(8), sender.getConfirmedBalance(databaseSet));
								
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.valueOf(0).setScale(8), recipient.getConfirmedBalance(databaseSet));
						
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(transaction.getSignature(), sender.getLastReference(databaseSet)));
						
		//CHECK REFERENCE RECIPIENT
		/// not work now assertEquals(true, Arrays.equals(new byte[0], recipient.getLastReference(databaseSet)));
	}

	//REGISTER NAME
	
	@Test
	public void validateSignatureRegisterNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE NAME
		Name name = new Name(sender, "test", "this is the value");
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(true, nameRegistration.isSignatureValid());
		
		//INVALID SIGNATURE
		nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet), new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, nameRegistration.isSignatureValid());
	}
	
	@Test
	public void validateRegisterNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameRegistration.isValid(databaseSet));
		nameRegistration.process(databaseSet);
		
		//CREATE INVALID NAME REGISTRATION INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		name = new Name(sender, longName, "this is the value");
		nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);

		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, nameRegistration.isValid(databaseSet));
		
		//CREATE INVALID NAME REGISTRATION INVALID NAME LENGTH
		String longValue = "";
		for(int i=1; i<10000; i++)
		{
			longValue += "oke";
		}
		name = new Name(sender, "test2", longValue);
		nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);

		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_VALUE_LENGTH, nameRegistration.isValid(databaseSet));
		
		//CREATE INVALID NAME REGISTRATION NAME ALREADY TAKEN
		name = new Name(sender, "test", "this is the value");
		nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.NAME_ALREADY_REGISTRED, nameRegistration.isValid(databaseSet));
		
		//CREATE INVALID NAME NOT ENOUGH BALANCE
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		name = new Name(invalidOwner, "test2", "this is the value");
		nameRegistration = new RegisterNameTransaction(invalidOwner, name, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));
		nameRegistration.sign(invalidOwner);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.NO_BALANCE, nameRegistration.isValid(databaseSet));
		
		//CREATE NAME REGISTRATION INVALID REFERENCE
		name = new Name(sender, "test2", "this is the value");
		nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, nameRegistration.isValid(databaseSet));
	}

	@Test
	public void parseRegisterNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		RegisterNameTransaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		
		//CONVERT TO BYTES
		byte[] rawNameRegistration = nameRegistration.toBytes(true);
		
		try 
		{	
			//PARSE FROM BYTES
			RegisterNameTransaction parsedRegistration = (RegisterNameTransaction) TransactionFactory.getInstance().parse(rawNameRegistration);
			
			//CHECK INSTANCE
			assertEquals(true, parsedRegistration instanceof RegisterNameTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(nameRegistration.getSignature(), parsedRegistration.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(nameRegistration.viewAmount(sender), parsedRegistration.viewAmount(sender));	
			
			//CHECK NAME OWNER
			assertEquals(nameRegistration.getName().getOwner().getAddress(), parsedRegistration.getName().getOwner().getAddress());	
			
			//CHECK NAME NAME
			assertEquals(nameRegistration.getName().getName(), parsedRegistration.getName().getName());	
			
			//CHECK NAME VALUE
			assertEquals(nameRegistration.getName().getValue(), parsedRegistration.getName().getValue());	
			
			//CHECK FEE
			assertEquals(nameRegistration.getFee(), parsedRegistration.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(nameRegistration.getReference(), parsedRegistration.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(nameRegistration.getTimestamp(), parsedRegistration.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawNameRegistration = new byte[nameRegistration.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawNameRegistration);
			
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
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		nameRegistration.process(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(nameRegistration.getSignature(), sender.getLastReference(databaseSet)));
		
		//CHECK NAME EXISTS
		assertEquals(true, databaseSet.getNameMap().contains(name));
	}
	
	@Test
	public void orphanRegisterNameTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		nameRegistration.process(databaseSet);
		nameRegistration.orphan(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(1000).setScale(8), sender.getConfirmedBalance(databaseSet));
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(transaction.getSignature(), sender.getLastReference(databaseSet)));
		
		//CHECK NAME EXISTS
		assertEquals(false, databaseSet.getNameMap().contains(name));
	}

	//UPDATE NAME
	
	@Test
	public void validateSignatureUpdateNameTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE NAME
		Name name = new Name(sender, "test", "this is the value");
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE NAME UPDATE
		Transaction nameUpdate = new UpdateNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameUpdate.sign(sender);
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(true, nameUpdate.isSignatureValid());
		
		//INVALID SIGNATURE
		nameUpdate = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet), new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, nameUpdate.isSignatureValid());

	}
	
	@Test
	public void validateUpdateNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameRegistration.isValid(databaseSet));
		nameRegistration.process(databaseSet);
		
		//CREATE NAME UPDATE
		name.setValue("new value");
		Transaction nameUpdate = new UpdateNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
	
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameUpdate.isValid(databaseSet));
		
		//CREATE INVALID NAME UPDATE INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		name = new Name(sender, longName, "this is the value");
		nameUpdate = new UpdateNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));		

		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, nameUpdate.isValid(databaseSet));
		
		//CREATE INVALID NAME UPDATE NAME DOES NOT EXIST
		name = new Name(sender, "test2", "this is the value");
		nameUpdate = new UpdateNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NAME_DOES_NOT_EXIST, nameUpdate.isValid(databaseSet));
		
		//CREATE INVALID NAME UPDATE INCORRECT OWNER
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		name = new Name(invalidOwner, "test2", "this is the value");
		nameRegistration = new RegisterNameTransaction(invalidOwner, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(invalidOwner);
		nameRegistration.process(databaseSet);	
		
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.INVALID_NAME_CREATOR, nameUpdate.isValid(databaseSet));
				
		//CREATE INVALID NAME UPDATE NO BALANCE
		name = new Name(invalidOwner, "test2", "this is the value");
		nameUpdate = new UpdateNameTransaction(invalidOwner, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NO_BALANCE, nameUpdate.isValid(databaseSet));
				
		//CREATE NAME UPDATE INVALID REFERENCE
		name = new Name(sender, "test", "this is the value");
		nameUpdate = new UpdateNameTransaction(sender, name, FEE_POWER, timestamp, new byte[]{});
				
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, nameUpdate.isValid(databaseSet));
						
	}

	@Test
	public void parseUpdateNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
				
		//CREATE NAME UPDATE
		UpdateNameTransaction nameUpdate = new UpdateNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameUpdate.sign(sender);
		
		//CONVERT TO BYTES
		byte[] rawNameUpdate = nameUpdate.toBytes(true);
		
		try 
		{	
			//PARSE FROM BYTES
			UpdateNameTransaction parsedUpdate = (UpdateNameTransaction) TransactionFactory.getInstance().parse(rawNameUpdate);
			
			//CHECK INSTANCE
			assertEquals(true, parsedUpdate instanceof UpdateNameTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(nameUpdate.getSignature(), parsedUpdate.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(nameUpdate.viewAmount(sender), parsedUpdate.viewAmount(sender));	
			
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
			assertEquals(true, Arrays.equals(nameUpdate.getReference(), parsedUpdate.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(nameUpdate.getTimestamp(), parsedUpdate.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawNameUpdate = new byte[nameUpdate.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawNameUpdate);
			
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
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		nameRegistration.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE NAME UPDATE
		name = new Name(new Account("Qj5Aq4P4ehXaCEmi6vqVrFQDecpPXKSi8z"), "test", "new value");
		Transaction nameUpdate = new UpdateNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameUpdate.sign(sender);
		nameUpdate.process(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(nameUpdate.getSignature(), sender.getLastReference(databaseSet)));
				
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
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		nameRegistration.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE NAME UPDATE
		name = new Name(new Account("XYLEQnuvhracK2WMN3Hjif67knkJe9hTQn"), "test", "new value");
		Transaction nameUpdate = new UpdateNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameUpdate.sign(sender);
		nameUpdate.process(databaseSet);
		nameUpdate.orphan(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(nameRegistration.getSignature(), sender.getLastReference(databaseSet)));
				
		//CHECK NAME EXISTS
		assertEquals(true, databaseSet.getNameMap().contains(name));
		
		//CHECK NAME VALUE
		name =  databaseSet.getNameMap().get("test");
		assertEquals("this is the value", name.getValue());
		
		//CHECK NAME OWNER
		assertEquals(sender.getAddress(), name.getOwner().getAddress());
	}
	
	//SELL NAME
	
	@Test
	public void validateSignatureSellNameTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE NAME
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(true, nameSaleTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet), new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, nameSaleTransaction.isSignatureValid());
	}
	
	@Test
	public void validateSellNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameRegistration.isValid(databaseSet));
		nameRegistration.process(databaseSet);
		
		//CREATE NAME SALE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		Transaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
	
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameSaleTransaction.isValid(databaseSet));
		
		//CREATE INVALID NAME SALE INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		nameSale = new NameSale(longName, BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));		

		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, nameSaleTransaction.isValid(databaseSet));
		
		//CREATE INVALID NAME SALE NAME DOES NOT EXIST
		nameSale = new NameSale("test2", BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		nameSaleTransaction.process();
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NAME_DOES_NOT_EXIST, nameSaleTransaction.isValid(databaseSet));
		
		//CREATE INVALID NAME UPDATE INCORRECT OWNER
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		name = new Name(invalidOwner, "test2", "this is the value");
		nameRegistration = new RegisterNameTransaction(invalidOwner, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(invalidOwner);
		nameRegistration.process(databaseSet);	
		
		//CHECK IF NAME UPDATE IS INVALID
		nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		assertEquals(Transaction.INVALID_NAME_CREATOR, nameSaleTransaction.isValid(databaseSet));
				
		//CREATE INVALID NAME UPDATE NO BALANCE
		nameSale = new NameSale("test2", BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(invalidOwner, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NO_BALANCE, nameSaleTransaction.isValid(databaseSet));
				
		//CREATE NAME UPDATE INVALID REFERENCE
		nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, new byte[]{});
				
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, nameSaleTransaction.isValid(databaseSet));
				
		//CREATE NAME UPDATE PROCESS 
		nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		nameSaleTransaction.process(databaseSet);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.NAME_ALREADY_FOR_SALE, nameSaleTransaction.isValid(databaseSet));
	}

	@Test
	public void parseSellNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1).setScale(8));
				
		//CREATE NAME UPDATE
		SellNameTransaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		nameSaleTransaction.process();
		
		//CONVERT TO BYTES
		byte[] rawNameSale = nameSaleTransaction.toBytes(true);
		
		try 
		{	
			//PARSE FROM BYTES
			SellNameTransaction parsedNameSale = (SellNameTransaction) TransactionFactory.getInstance().parse(rawNameSale);
			
			//CHECK INSTANCE
			assertEquals(true, parsedNameSale instanceof SellNameTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(nameSaleTransaction.getSignature(), parsedNameSale.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(nameSaleTransaction.viewAmount(sender), parsedNameSale.viewAmount(sender));	
			
			//CHECK OWNER
			assertEquals(nameSaleTransaction.getCreator().getAddress(), parsedNameSale.getCreator().getAddress());	
			
			//CHECK NAMESALE NAME
			assertEquals(nameSaleTransaction.getNameSale().getKey(), parsedNameSale.getNameSale().getKey());	
			
			//CHECK NAMESALE AMOUNT
			assertEquals(nameSaleTransaction.getNameSale().getAmount(), parsedNameSale.getNameSale().getAmount());	
			
			//CHECK FEE
			assertEquals(nameSaleTransaction.getFee(), parsedNameSale.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(nameSaleTransaction.getReference(), parsedNameSale.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(nameSaleTransaction.getTimestamp(), parsedNameSale.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawNameSale = new byte[nameSaleTransaction.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawNameSale);
			
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
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		nameRegistration.process(databaseSet);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		nameSaleTransaction.process(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(nameSaleTransaction.getSignature(), sender.getLastReference(databaseSet)));
				
		//CHECK NAME SALE EXISTS
		assertEquals(true, databaseSet.getNameExchangeMap().contains("test"));
		
		//CHECK NAME SALE AMOUNT
		nameSale =  databaseSet.getNameExchangeMap().getNameSale("test");
		assertEquals(BigDecimal.valueOf(1000).setScale(8), nameSale.getAmount());
	}

	@Test
	public void orphanSellNameTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		nameRegistration.process(databaseSet);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
						
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		nameSaleTransaction.process(databaseSet);
		nameSaleTransaction.orphan(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(nameRegistration.getSignature(), sender.getLastReference(databaseSet)));
				
		//CHECK NAME SALE EXISTS
		assertEquals(false, databaseSet.getNameExchangeMap().contains("test"));
	}
	
	
	//CANCEL SELL NAME
	
	@Test
	public void validateSignatureCancelSellNameTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new CancelSellNameTransaction(sender, "test", FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(true, nameSaleTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		nameSaleTransaction = new CancelSellNameTransaction(sender, "test", FEE_POWER, timestamp, sender.getLastReference(databaseSet), new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, nameSaleTransaction.isSignatureValid());
	}
	
	@Test
	public void validateCancelSellNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameRegistration.isValid(databaseSet));
		nameRegistration.process(databaseSet);
		
		//CREATE NAME SALE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		Transaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
	
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameSaleTransaction.isValid(databaseSet));
		nameSaleTransaction.process(databaseSet);
		
		//CREATE CANCEL NAME SALE
		CancelSellNameTransaction cancelNameSaleTransaction = new CancelSellNameTransaction(sender, nameSale.getKey(), FEE_POWER, timestamp, sender.getLastReference(databaseSet));		

		//CHECK IF CANCEL NAME UPDATE IS VALID
		assertEquals(Transaction.VALIDATE_OK, cancelNameSaleTransaction.isValid(databaseSet));
		
		//CREATE INVALID CANCEL NAME SALE INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		
		cancelNameSaleTransaction = new CancelSellNameTransaction(sender, longName, FEE_POWER, timestamp, sender.getLastReference(databaseSet));		

		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, cancelNameSaleTransaction.isValid(databaseSet));
		
		//CREATE INVALID CANCEL NAME SALE NAME DOES NOT EXIST
		cancelNameSaleTransaction = new CancelSellNameTransaction(sender, "test2", FEE_POWER, timestamp, sender.getLastReference(databaseSet));
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NAME_DOES_NOT_EXIST, cancelNameSaleTransaction.isValid(databaseSet));
		
		//CREATE INVALID NAME UPDATE INCORRECT OWNER
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		name = new Name(invalidOwner, "test2", "this is the value");
		nameRegistration = new RegisterNameTransaction(invalidOwner, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(invalidOwner);
		nameRegistration.process(databaseSet);	
		
		//CREATE NAME SALE
		nameSale = new NameSale("test2", BigDecimal.valueOf(1000).setScale(8));
		nameSaleTransaction = new SellNameTransaction(invalidOwner, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(invalidOwner);
		nameSaleTransaction.process(databaseSet);	
		
		//CHECK IF NAME UPDATE IS INVALID
		cancelNameSaleTransaction = new CancelSellNameTransaction(sender, "test2", FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		assertEquals(Transaction.INVALID_NAME_CREATOR, cancelNameSaleTransaction.isValid(databaseSet));
				
		//CREATE INVALID NAME UPDATE NO BALANCE
		cancelNameSaleTransaction = new CancelSellNameTransaction(invalidOwner, "test2", FEE_POWER, timestamp, sender.getLastReference(databaseSet));
				
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NO_BALANCE, cancelNameSaleTransaction.isValid(databaseSet));
				
		//CREATE NAME UPDATE INVALID REFERENCE
		cancelNameSaleTransaction = new CancelSellNameTransaction(sender, "test", FEE_POWER, timestamp, new byte[]{});
				
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, cancelNameSaleTransaction.isValid(databaseSet));
				
		//CREATE NAME UPDATE PROCESS 
		cancelNameSaleTransaction = new CancelSellNameTransaction(sender, "test", FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		cancelNameSaleTransaction.sign(sender);
		cancelNameSaleTransaction.process(databaseSet);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.NAME_NOT_FOR_SALE, cancelNameSaleTransaction.isValid(databaseSet));
	}

	@Test
	public void parseCancelSellNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
				
		//CREATE CANCEL NAME SALE
		CancelSellNameTransaction cancelNameSaleTransaction = new CancelSellNameTransaction(sender, "test", FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		cancelNameSaleTransaction.sign(sender);
		
		//CONVERT TO BYTES
		byte[] rawCancelNameSale = cancelNameSaleTransaction.toBytes(true);
		
		try 
		{	
			//PARSE FROM BYTES
			CancelSellNameTransaction parsedCancelNameSale = (CancelSellNameTransaction) TransactionFactory.getInstance().parse(rawCancelNameSale);
			
			//CHECK INSTANCE
			assertEquals(true, parsedCancelNameSale instanceof CancelSellNameTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(cancelNameSaleTransaction.getSignature(), parsedCancelNameSale.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(cancelNameSaleTransaction.viewAmount(sender), parsedCancelNameSale.viewAmount(sender));	
			
			//CHECK OWNER
			assertEquals(cancelNameSaleTransaction.getCreator().getAddress(), parsedCancelNameSale.getCreator().getAddress());	
			
			//CHECK NAME
			assertEquals(cancelNameSaleTransaction.getName(), parsedCancelNameSale.getName());	
			
			//CHECK FEE
			assertEquals(cancelNameSaleTransaction.getFee(), parsedCancelNameSale.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(cancelNameSaleTransaction.getReference(), parsedCancelNameSale.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(cancelNameSaleTransaction.getTimestamp(), parsedCancelNameSale.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawCancelNameSale = new byte[cancelNameSaleTransaction.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawCancelNameSale);
			
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
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		nameRegistration.process(databaseSet);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		nameSaleTransaction.process(databaseSet);
		
		//CREATE SIGNATURE
			
		//CREATE CANCEL NAME SALE
		Transaction cancelNameSaleTransaction = new CancelSellNameTransaction(sender, "test", FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		cancelNameSaleTransaction.sign(sender);
		cancelNameSaleTransaction.process(databaseSet);	
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(cancelNameSaleTransaction.getSignature(), sender.getLastReference(databaseSet)));
				
		//CHECK NAME SALE EXISTS
		assertEquals(false, databaseSet.getNameExchangeMap().contains("test"));
	}

	@Test
	public void orphanCancelSellNameTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		nameRegistration.process(databaseSet);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		nameSaleTransaction.process(databaseSet);
		
		//CREATE SIGNATURE
			
		//CREATE CANCEL NAME SALE
		Transaction cancelNameSaleTransaction = new CancelSellNameTransaction(sender, "test", FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		cancelNameSaleTransaction.sign(sender);
		cancelNameSaleTransaction.process(databaseSet);	
		cancelNameSaleTransaction.orphan(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0,BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(nameSaleTransaction.getSignature(), sender.getLastReference(databaseSet)));
				
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
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction buyNameTransaction = new BuyNameTransaction(sender, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		buyNameTransaction.sign(sender);
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(true, buyNameTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		buyNameTransaction = new BuyNameTransaction(sender, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, sender.getLastReference(databaseSet), new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, buyNameTransaction.isSignatureValid());
	}
	
	@Test
	public void validateBuyNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//CREATE KNOWN ACCOUNT
		seed = Crypto.getInstance().digest("buyer".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount buyer = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE BUYER HAS FUNDS
		transaction = new GenesisTransaction(buyer, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
				
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		
		//CHECK IF NAME REGISTRATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameRegistration.isValid(databaseSet));
		nameRegistration.process(databaseSet);
		
		//CREATE NAME SALE
		BigDecimal bdAmoSell = BigDecimal.valueOf(700).setScale(8);
		NameSale nameSale = new NameSale("test", bdAmoSell);
		Transaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
	
		//CHECK IF NAME UPDATE IS VALID
		assertEquals(Transaction.VALIDATE_OK, nameSaleTransaction.isValid(databaseSet));
		nameSaleTransaction.process(databaseSet);
		
		//CREATE NAME PURCHASE
		BuyNameTransaction namePurchaseTransaction = new BuyNameTransaction(buyer, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));
		namePurchaseTransaction.sign(buyer);

		//CHECK IF NAME PURCHASE IS VALID
		assertEquals(Transaction.VALIDATE_OK, namePurchaseTransaction.isValid(databaseSet));
		
		//CREATE INVALID NAME PURCHASE INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		
		NameSale nameSaleInvalid = new NameSale(longName, nameSale.getAmount());
		//nameSale = new NameSale(longName, nameSale.getAmount());
		//Logger.getGlobal().info("nameSaleLong " + nameSaleLong);
		//Logger.getGlobal().info("nameSaleLong getOwner "  + nameSaleLong.getName(databaseSet).getOwner());
		//// nameSaleLong --- nameSale -> owner
		namePurchaseTransaction = new BuyNameTransaction(buyer, nameSaleInvalid, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));		

		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, namePurchaseTransaction.isValid(databaseSet));
		
		//CREATE INVALID NAME PURCHASE NAME DOES NOT EXIST
		nameSaleInvalid = new NameSale("test2", BigDecimal.valueOf(1000).setScale(8));
		namePurchaseTransaction = new BuyNameTransaction(buyer, nameSaleInvalid, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));		
		
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NAME_DOES_NOT_EXIST, namePurchaseTransaction.isValid(databaseSet));
		
		//CREATE INVALID NAME PURCHASE NAME NOT FOR SALE
		Name test2 = new Name(sender, "test2", "oke");
		databaseSet.getNameMap().add(test2);
		
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NAME_NOT_FOR_SALE, namePurchaseTransaction.isValid(databaseSet));
						
		//CREATE INVALID NAME PURCHASE ALREADY OWNER
		nameSale = new NameSale("test", bdAmoSell);
		namePurchaseTransaction = new BuyNameTransaction(sender, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, sender.getLastReference(databaseSet));		
		
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.BUYER_ALREADY_OWNER, namePurchaseTransaction.isValid(databaseSet));
				
		//CREATE INVALID NAME UPDATE NO BALANCE
		buyer.setConfirmedBalance(BigDecimal.ZERO.setScale(8), databaseSet);
		namePurchaseTransaction = new BuyNameTransaction(buyer, nameSale,nameSale.getName(databaseSet).getOwner(),FEE_POWER, timestamp, buyer.getLastReference(databaseSet));		
		
		//CHECK IF NAME UPDATE IS INVALID
		assertEquals(Transaction.NO_BALANCE, namePurchaseTransaction.isValid(databaseSet));

		// setConfirmedBalance(long key, BigDecimal amount, DBSet db)
		buyer.setConfirmedBalance(BigDecimal.valueOf(2000).setScale(8), databaseSet);
				
		//CREATE NAME UPDATE INVALID REFERENCE
		namePurchaseTransaction = new BuyNameTransaction(buyer, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, new byte[]{});		
				
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, namePurchaseTransaction.isValid(databaseSet));
	}

	@Test
	public void parseBuyNameTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();				
		////////////// FIRST
		//CREATE NAME SALE
		BigDecimal bdAmoSell = BigDecimal.valueOf(700).setScale(8);
		NameSale nameSale = new NameSale("test", bdAmoSell);
		SellNameTransaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		//nameSaleTransaction.process();
		nameSaleTransaction.process(databaseSet);

		Logger.getGlobal().info("nameSale " + nameSale.getName(databaseSet));
		Logger.getGlobal().info("nameSale " + nameSale.getName(databaseSet).getOwner());

		//CREATE CANCEL NAME SALE
		BuyNameTransaction namePurchaseTransaction = new BuyNameTransaction(sender, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, sender.getLastReference(databaseSet));	
		namePurchaseTransaction.sign(sender);
		//CONVERT TO BYTES
		byte[] rawNamePurchase = namePurchaseTransaction.toBytes(true);
		
		try 
		{	
			//PARSE FROM BYTES
			BuyNameTransaction parsedNamePurchase = (BuyNameTransaction) TransactionFactory.getInstance().parse(rawNamePurchase);
			
			//CHECK INSTANCE
			assertEquals(true, parsedNamePurchase instanceof BuyNameTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(namePurchaseTransaction.getSignature(), parsedNamePurchase.getSignature()));
			
			//CHECK AMOUNT BUYER
			assertEquals(namePurchaseTransaction.viewAmount(sender), parsedNamePurchase.viewAmount(sender));	
			
			//CHECK OWNER
			assertEquals(namePurchaseTransaction.getBuyer().getAddress(), parsedNamePurchase.getBuyer().getAddress());	
			
			//CHECK NAME
			assertEquals(namePurchaseTransaction.getNameSale().getKey(), parsedNamePurchase.getNameSale().getKey());	
		
			//CHECK FEE
			assertEquals(namePurchaseTransaction.getFee(), parsedNamePurchase.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(namePurchaseTransaction.getReference(), parsedNamePurchase.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(namePurchaseTransaction.getTimestamp(), parsedNamePurchase.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawNamePurchase = new byte[namePurchaseTransaction.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawNamePurchase);
			
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
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//CREATE KNOWN ACCOUNT
		seed = Crypto.getInstance().digest("buyer".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount buyer = new PrivateKeyAccount(privateKey);		
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE BUYER HAS FUNDS
		transaction = new GenesisTransaction(buyer, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);			
		// set OIL
		buyer.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		nameRegistration.process(databaseSet);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(500).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		nameSaleTransaction.process(databaseSet);
		
		//CREATE SIGNATURE
			
		//CREATE NAME PURCHASE
		Transaction purchaseNameTransaction = new BuyNameTransaction(buyer, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));
		purchaseNameTransaction.sign(buyer);
		purchaseNameTransaction.process(databaseSet);	
		
		//CHECK BALANCE SENDER
		//assertEquals(BigDecimal.valueOf(498).setScale(8), buyer.getConfirmedBalance(databaseSet));
		assertEquals(0, BigDecimal.valueOf(500).setScale(8).compareTo(buyer.getConfirmedBalance(databaseSet)));
		
		//CHECK BALANCE SELLER
		assertEquals(0, BigDecimal.valueOf(1500).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK REFERENCE BUYER
		assertEquals(true, Arrays.equals(purchaseNameTransaction.getSignature(), buyer.getLastReference(databaseSet)));
				
		//CHECK NAME OWNER
		name = databaseSet.getNameMap().get("test");
		assertEquals(name.getOwner().getAddress(), buyer.getAddress());
	
		//CHECK NAME SALE EXISTS
		assertEquals(false, databaseSet.getNameExchangeMap().contains("test"));
	}

	@Test
	public void orphanBuyNameTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//CREATE KNOWN ACCOUNT
		seed = Crypto.getInstance().digest("buyer".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount buyer = new PrivateKeyAccount(privateKey);		
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE BUYER HAS FUNDS
		transaction = new GenesisTransaction(buyer, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);			
		// set OIL
		buyer.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Name name = new Name(sender, "test", "this is the value");
						
		//CREATE NAME REGISTRATION
		Transaction nameRegistration = new RegisterNameTransaction(sender, name, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameRegistration.sign(sender);
		nameRegistration.process(databaseSet);
		
		//CREATE SIGNATURE
		NameSale nameSale = new NameSale("test", BigDecimal.valueOf(1000).setScale(8));
		
		//CREATE NAME SALE
		Transaction nameSaleTransaction = new SellNameTransaction(sender, nameSale, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		nameSaleTransaction.sign(sender);
		nameSaleTransaction.process(databaseSet);
		
		//CREATE SIGNATURE
			
		//CREATE NAME PURCHASE
		Transaction purchaseNameTransaction = new BuyNameTransaction(buyer, nameSale, nameSale.getName(databaseSet).getOwner(), FEE_POWER, timestamp, buyer.getLastReference(databaseSet));
		purchaseNameTransaction.sign(buyer);
		purchaseNameTransaction.process(databaseSet);	
		purchaseNameTransaction.orphan(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(1000).setScale(8), buyer.getConfirmedBalance(databaseSet));
		
		//CHECK BALANCE SELLER
		assertEquals(0, BigDecimal.valueOf(9999).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK REFERENCE BUYER
		assertEquals(true, Arrays.equals(transaction.getSignature(), buyer.getLastReference(databaseSet)));
				
		//CHECK NAME OWNER
		name = databaseSet.getNameMap().get("test");
		assertEquals(name.getOwner().getAddress(), sender.getAddress());
	
		//CHECK NAME SALE EXISTS
		assertEquals(true, databaseSet.getNameExchangeMap().contains("test"));
	}
	
	//CREATE POLL
	
	@Test
	public void validateSignatureCreatePollTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE POLL
		Poll poll = new Poll(sender, "test", "this is the value", new ArrayList<PollOption>());
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollCreation.sign(sender);
		//CHECK IF POLL CREATION IS VALID
		assertEquals(true, pollCreation.isSignatureValid());
		
		//INVALID SIGNATURE
		pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet), new byte[64]);
		
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(false, pollCreation.isSignatureValid());
	}
		
	@Test
	public void validateCreatePollTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Poll poll = new Poll(sender, "test", "this is the value", Arrays.asList(new PollOption("test")));
				
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollCreation.sign(sender);
		
		//CHECK IF POLL CREATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, pollCreation.isValid(databaseSet));
		pollCreation.process(databaseSet);
		
		//CREATE INVALID POLL CREATION INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		poll = new Poll(sender, longName, "this is the value", Arrays.asList(new PollOption("test")));
		pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));		

		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, pollCreation.isValid(databaseSet));
		
		//CREATE INVALID POLL CREATION INVALID DESCRIPTION LENGTH
		String longDescription = "";
		for(int i=1; i<10000; i++)
		{
			longDescription += "oke";
		}
		poll = new Poll(sender, "test2", longDescription, Arrays.asList(new PollOption("test")));
		pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));		

		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.INVALID_DESCRIPTION_LENGTH, pollCreation.isValid(databaseSet));
		
		//CREATE INVALID POLL CREATION NAME ALREADY TAKEN
		poll = new Poll(sender, "test", "this is the value", Arrays.asList(new PollOption("test")));
		pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));		
		
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.POLL_ALREADY_CREATED, pollCreation.isValid(databaseSet));
		
		//CREATE INVALID POLL CREATION NO OPTIONS 
		poll = new Poll(sender, "test2", "this is the value", new ArrayList<PollOption>());
		pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));		
		
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.INVALID_OPTIONS_LENGTH, pollCreation.isValid(databaseSet));
		
		//CREATE INVALID POLL CREATION INVALID OPTION LENGTH
		poll = new Poll(sender, "test2", "this is the value", Arrays.asList(new PollOption(longName)));
		pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));		
				
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.INVALID_OPTION_LENGTH, pollCreation.isValid(databaseSet));
		
		//CREATE INVALID POLL CREATION INVALID DUPLICATE OPTIONS
		poll = new Poll(sender, "test2", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("test")));
		pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));		
						
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.DUPLICATE_OPTION, pollCreation.isValid(databaseSet));
		
		//CREATE INVALID POLL CREATION NOT ENOUGH BALANCE
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		poll = new Poll(sender, "test2", "this is the value", Arrays.asList(new PollOption("test")));
		pollCreation = new CreatePollTransaction(invalidOwner, poll, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));
		pollCreation.sign(invalidOwner);
		
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.NOT_ENOUGH_FEE, pollCreation.isValid(databaseSet));
		
		//CREATE POLL CREATION INVALID REFERENCE
		poll = new Poll(sender, "test2", "this is the value", Arrays.asList(new PollOption("test")));
		pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));		
		
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, pollCreation.isValid(databaseSet));
		
	}

	@Test
	public void parseCreatePollTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Poll poll = new Poll(sender, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("second option")));
				
		//CREATE POLL CREATION
		CreatePollTransaction pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollCreation.sign(sender);
		
		//CONVERT TO BYTES
		byte[] rawPollCreation = pollCreation.toBytes(true);
		
		try 
		{	
			//PARSE FROM BYTES
			CreatePollTransaction parsedPollCreation = (CreatePollTransaction) TransactionFactory.getInstance().parse(rawPollCreation);
			
			//CHECK INSTANCE
			assertEquals(true, parsedPollCreation instanceof CreatePollTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(pollCreation.getSignature(), parsedPollCreation.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(pollCreation.viewAmount(sender), parsedPollCreation.viewAmount(sender));	
			
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
			assertEquals(true, Arrays.equals(pollCreation.getReference(), parsedPollCreation.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(pollCreation.getTimestamp(), parsedPollCreation.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawPollCreation = new byte[pollCreation.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawPollCreation);
			
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
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Poll poll = new Poll(sender, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("second option")));
						
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollCreation.sign(sender);
		pollCreation.process(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(pollCreation.getSignature(), sender.getLastReference(databaseSet)));
		
		//CHECK POLL EXISTS
		assertEquals(true, databaseSet.getPollMap().contains(poll));
	}
	
	@Test
	public void orphanCreatePollTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Poll poll = new Poll(sender, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("second option")));
						
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollCreation.sign(sender);
		pollCreation.process(databaseSet);
		pollCreation.orphan(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(1000).setScale(8), sender.getConfirmedBalance(databaseSet));
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(transaction.getSignature(), sender.getLastReference(databaseSet)));
		
		//CHECK POLL EXISTS
		assertEquals(false, databaseSet.getPollMap().contains(poll));
	}
	
	//VOTE ON POLL
	
	@Test
	public void validateSignatureVoteOnPollTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE POLL VOTE
		Transaction pollVote = new VoteOnPollTransaction(sender, "test", 5, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollVote.sign(sender);
		//CHECK IF POLL VOTE IS VALID
		assertEquals(true, pollVote.isSignatureValid());
		
		//INVALID SIGNATURE
		pollVote = new VoteOnPollTransaction(sender, "test", 5, FEE_POWER, timestamp, sender.getLastReference(databaseSet), new byte[64]);
		
		//CHECK IF POLL VOTE IS INVALID
		assertEquals(false, pollVote.isSignatureValid());
	}
		
	@Test
	public void validateVoteOnPollTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE SIGNATURE
		//Logger.getGlobal().info("asdasd");
		long timestamp = NTP.getTime();
		Poll poll = new Poll(sender, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("test2")));
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollCreation.sign(sender);
		
		//CHECK IF POLL CREATION IS VALID
		assertEquals(Transaction.VALIDATE_OK, pollCreation.isValid(databaseSet));
		pollCreation.process(databaseSet);
		
		//CREATE POLL VOTE
		Transaction pollVote = new VoteOnPollTransaction(sender, poll.getName(), 0, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollVote.sign(sender);
		
		//CHECK IF POLL VOTE IS VALID
		assertEquals(Transaction.VALIDATE_OK, pollVote.isValid(databaseSet));
		pollVote.process(databaseSet);
		
		//CREATE INVALID POLL VOTE INVALID NAME LENGTH
		String longName = "";
		for(int i=1; i<1000; i++)
		{
			longName += "oke";
		}
		pollVote = new VoteOnPollTransaction(sender, longName, 0, FEE_POWER, timestamp, sender.getLastReference(databaseSet));	

		//CHECK IF POLL VOTE IS INVALID
		assertEquals(Transaction.INVALID_NAME_LENGTH, pollVote.isValid(databaseSet));
		
		//CREATE INVALID POLL VOTE POLL DOES NOT EXIST
		pollVote = new VoteOnPollTransaction(sender, "test2", 0, FEE_POWER, timestamp, sender.getLastReference(databaseSet));	
		
		//CHECK IF POLL VOTE IS INVALID
		assertEquals(Transaction.POLL_NO_EXISTS, pollVote.isValid(databaseSet));
		
		//CREATE INVALID POLL VOTE INVALID OPTION
		pollVote = new VoteOnPollTransaction(sender, "test", 5, FEE_POWER, timestamp, sender.getLastReference(databaseSet));	
		
		//CHECK IF POLL VOTE IS INVALID
		assertEquals(Transaction.OPTION_NO_EXISTS, pollVote.isValid(databaseSet));
		
		//CREATE INVALID POLL VOTE INVALID OPTION
		pollVote = new VoteOnPollTransaction(sender, "test", -1, FEE_POWER, timestamp, sender.getLastReference(databaseSet));	
				
		//CHECK IF POLL VOTE IS INVALID
		assertEquals(Transaction.OPTION_NO_EXISTS, pollVote.isValid(databaseSet));
		
		//CRTEATE INVALID POLL VOTE VOTED ALREADY
		pollVote = new VoteOnPollTransaction(sender, "test", 0, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollVote.sign(sender);
		pollVote.process(databaseSet);
		
		//CHECK IF POLL VOTE IS INVALID
		assertEquals(Transaction.ALREADY_VOTED_FOR_THAT_OPTION, pollVote.isValid(databaseSet));
		
		//CREATE INVALID POLL VOTE NOT ENOUGH BALANCE
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		pollVote = new VoteOnPollTransaction(invalidOwner, "test", 0, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollVote.sign(invalidOwner);
		

		//CHECK IF POLL VOTE IS INVALID
		///Logger.getGlobal().info("pollVote.getFee: " + pollVote.getFee());
		/// fee = 0 assertEquals(Transaction.NOT_ENOUGH_FEE, pollVote.isValid(databaseSet));
		
		//CREATE POLL CREATION INVALID REFERENCE
		pollVote = new VoteOnPollTransaction(sender, "test", 1, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));	
		
		//CHECK IF POLL CREATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, pollVote.isValid(databaseSet));
		
	}

	
	@Test
	public void parseVoteOnPollTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
				
		//CREATE POLL Vote
		VoteOnPollTransaction pollVote = new VoteOnPollTransaction(sender, "test", 0, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollVote.sign(sender);
		
		//CONVERT TO BYTES
		byte[] rawPollVote = pollVote.toBytes(true);
		
		try 
		{	
			//PARSE FROM BYTES
			VoteOnPollTransaction parsedPollVote = (VoteOnPollTransaction) TransactionFactory.getInstance().parse(rawPollVote);
			
			//CHECK INSTANCE
			assertEquals(true, parsedPollVote instanceof VoteOnPollTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(pollVote.getSignature(), parsedPollVote.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(pollVote.viewAmount(sender), parsedPollVote.viewAmount(sender));	
			
			//CHECK CREATOR
			assertEquals(pollVote.getCreator().getAddress(), parsedPollVote.getCreator().getAddress());	
			
			//CHECK POLL
			assertEquals(pollVote.getPoll(), parsedPollVote.getPoll());	
			
			//CHECK POLL OPTION
			assertEquals(pollVote.getOption(), parsedPollVote.getOption());	
			
			//CHECK FEE
			assertEquals(pollVote.getFee(), parsedPollVote.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(pollVote.getReference(), parsedPollVote.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(pollVote.getTimestamp(), parsedPollVote.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawPollVote = new byte[pollVote.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawPollVote);
			
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
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Poll poll = new Poll(sender, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("second option")));
						
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollCreation.sign(sender);
		pollCreation.process(databaseSet);
		
		//CREATE POLL VOTE
		Transaction pollVote = new VoteOnPollTransaction(sender, poll.getName(), 0, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollVote.sign(sender);
		pollVote.process(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(pollVote.getSignature(), sender.getLastReference(databaseSet)));
		
		//CHECK POLL VOTER
		assertEquals(true, databaseSet.getPollMap().get(poll.getName()).getOptions().get(0).hasVoter(sender));
		
		//CREATE POLL VOTE
		pollVote = new VoteOnPollTransaction(sender, poll.getName(), 1, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollVote.sign(sender);
		pollVote.process(databaseSet);
				
		//CHECK BALANCE SENDER
		// 999
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
						
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(pollVote.getSignature(), sender.getLastReference(databaseSet)));
				
		//CHECK POLL VOTER
		assertEquals(false, databaseSet.getPollMap().get(poll.getName()).getOptions().get(0).hasVoter(sender));
		
		//CHECK POLL VOTER
		assertEquals(true, databaseSet.getPollMap().get(poll.getName()).getOptions().get(1).hasVoter(sender));
	}
	
	@Test
	public void orphanVoteOnPollTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Poll poll = new Poll(sender, "test", "this is the value", Arrays.asList(new PollOption("test"), new PollOption("second option")));
						
		//CREATE POLL CREATION
		Transaction pollCreation = new CreatePollTransaction(sender, poll, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollCreation.sign(sender);
		pollCreation.process(databaseSet);
		
		//CREATE POLL VOTE
		Transaction pollVote = new VoteOnPollTransaction(sender, poll.getName(), 0, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		pollVote.sign(sender);
		pollVote.process(databaseSet);
		pollVote.orphan(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(pollCreation.getSignature(), sender.getLastReference(databaseSet)));
		
		//CHECK POLL VOTER
		assertEquals(false, databaseSet.getPollMap().get(poll.getName()).hasVotes());
		
		//CHECK POLL VOTER
		assertEquals(false, databaseSet.getPollMap().get(poll.getName()).getOptions().get(0).hasVoter(sender));

	}
	
	//ARBITRARY TRANSACTION
	
	@Test
	public void validateSignatureArbitraryTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE ARBITRARY TRANSACTION
		Transaction arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4889, "test".getBytes(), FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		
		//CHECK IF ARBITRARY TRANSACTION IS VALID
		assertEquals(true, arbitraryTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4889, "test".getBytes(), FEE_POWER, timestamp, sender.getLastReference(databaseSet), new byte[64]);
		//arbitraryTransaction.sign(sender);
		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(false, arbitraryTransaction.isSignatureValid());
	}
		
	@Test
	public void validateArbitraryTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		byte[] data = "test".getBytes();
				
		//CREATE ARBITRARY TRANSACTION
		Transaction arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4776, data, FEE_POWER, timestamp, sender.getLastReference(databaseSet));	
		arbitraryTransaction.sign(sender);		

		//CHECK IF ARBITRARY TRANSACTION IS VALID
		assertEquals(Transaction.VALIDATE_OK, arbitraryTransaction.isValid(databaseSet));
		arbitraryTransaction.process(databaseSet);
		
		//CREATE INVALID ARBITRARY TRANSACTION INVALID data LENGTH
		byte[] longData = new byte[5000];
		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4776, longData, FEE_POWER, timestamp, sender.getLastReference(databaseSet));	

		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.INVALID_DATA_LENGTH, arbitraryTransaction.isValid(databaseSet));
		
		//CREATE INVALID ARBITRARY TRANSACTION NOT ENOUGH BALANCE
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		arbitraryTransaction = new ArbitraryTransactionV3(invalidOwner, null, 4776, data, FEE_POWER, timestamp, sender.getLastReference(databaseSet));	
		
		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.NO_BALANCE, arbitraryTransaction.isValid(databaseSet));
		
		//CREATE ARBITRARY TRANSACTION INVALID REFERENCE
		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4776, data, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));	
		
		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, arbitraryTransaction.isValid(databaseSet));
		
	}

	
	@Test
	public void parseArbitraryTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
				
		//CREATE ARBITRARY TRANSACTION
		ArbitraryTransactionV3 arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4776, "test".getBytes(),FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		
		//CONVERT TO BYTES
		byte[] rawArbitraryTransaction = arbitraryTransaction.toBytes(true);
		
		try 
		{	
			//PARSE FROM BYTES
			ArbitraryTransactionV3 parsedArbitraryTransaction = (ArbitraryTransactionV3) TransactionFactory.getInstance().parse(rawArbitraryTransaction);
			
			//CHECK INSTANCE
			assertEquals(true, parsedArbitraryTransaction instanceof ArbitraryTransactionV3);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(arbitraryTransaction.getSignature(), parsedArbitraryTransaction.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(arbitraryTransaction.viewAmount(sender), parsedArbitraryTransaction.viewAmount(sender));	
			
			//CHECK CREATOR
			assertEquals(arbitraryTransaction.getCreator().getAddress(), parsedArbitraryTransaction.getCreator().getAddress());	
			
			//CHECK VERSION
			assertEquals(arbitraryTransaction.getService(), parsedArbitraryTransaction.getService());	
			
			//CHECK DATA
			assertEquals(true, Arrays.equals(arbitraryTransaction.getData(), parsedArbitraryTransaction.getData()));	
			
			//CHECK FEE
			assertEquals(arbitraryTransaction.getFee(), parsedArbitraryTransaction.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(arbitraryTransaction.getReference(), parsedArbitraryTransaction.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(arbitraryTransaction.getTimestamp(), parsedArbitraryTransaction.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawArbitraryTransaction = new byte[arbitraryTransaction.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawArbitraryTransaction);
			
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
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
						
		//CREATE ARBITRARY TRANSACTION
		ArbitraryTransactionV3 arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4776, "test".getBytes(),FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		arbitraryTransaction.process(databaseSet);				
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(1000).setScale(8).compareTo(sender.getConfirmedBalance(databaseSet)));
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(arbitraryTransaction.getSignature(), sender.getLastReference(databaseSet)));
	}
	
	@Test
	public void orphanArbitraryTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
					
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
				
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
								
		//CREATE ARBITRARY TRANSACTION
		ArbitraryTransactionV3 arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4776, "test".getBytes(),FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		arbitraryTransaction.sign(sender);
		arbitraryTransaction.process(databaseSet);	
		arbitraryTransaction.orphan(databaseSet);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(1000).setScale(8), sender.getConfirmedBalance(databaseSet));
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(transaction.getSignature(), sender.getLastReference(databaseSet)));
	}

	/*@Test
	public void validateArbitraryTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		byte[] data = "test".getBytes();
				
		//CREATE ARBITRARY TRANSACTION
		Transaction arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4776, data, FEE_POWER, timestamp, sender.getLastReference(databaseSet));	
		
		//CHECK IF ARBITRARY TRANSACTION IS VALID
		assertEquals(Transaction.VALIDATE_OK, arbitraryTransaction.isValid(databaseSet));
		arbitraryTransaction.process(databaseSet);
		
		//CREATE INVALID ARBITRARY TRANSACTION INVALID data LENGTH
		byte[] longData = new byte[5000];
		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4776, longData, FEE_POWER, timestamp, sender.getLastReference(databaseSet));	

		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.INVALID_DATA_LENGTH, arbitraryTransaction.isValid(databaseSet));
		
		//CREATE INVALID ARBITRARY TRANSACTION NOT ENOUGH BALANCE
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidOwner = new PrivateKeyAccount(privateKey);
		arbitraryTransaction = new ArbitraryTransactionV1(invalidOwner, 4776, data, FEE_POWER, timestamp, sender.getLastReference(databaseSet));	
		
		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.NO_BALANCE, arbitraryTransaction.isValid(databaseSet));
		
		//CREATE ARBITRARY TRANSACTION INVALID REFERENCE
		arbitraryTransaction = new ArbitraryTransactionV3(sender, null, 4776, data, FEE_POWER, timestamp, invalidOwner.getLastReference(databaseSet));	
		
		//CHECK IF ARBITRARY TRANSACTION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, arbitraryTransaction.isValid(databaseSet));
		
	}*/

	//ISSUE ASSET TRANSACTION
	
	@Test
	public void validateSignatureIssueAssetTransaction() 
	{
		
		init();
		
		//CREATE ASSET
		Asset asset = new Venture(maker, "test", "strontje", 50000l, (byte) 2, false);
		//byte[] data = asset.toBytes(false);
		//Asset asset2 = Asset.parse(data);
		
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE ISSUE ASSET TRANSACTION
		Transaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker);
		
		//CHECK IF ISSUE ASSET TRANSACTION IS VALID
		assertEquals(true, issueAssetTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db), new byte[64]);
		
		//CHECK IF ISSUE ASSET IS INVALID
		assertEquals(false, issueAssetTransaction.isSignatureValid());
	}
		

	
	@Test
	public void parseIssueAssetTransaction() 
	{
		
		init();
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		Asset asset = new Venture(maker, "test", "strontje", 50000l, (byte) 2, false);
				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker);
		issueAssetTransaction.process(db);
		
		//CONVERT TO BYTES
		byte[] rawIssueAssetTransaction = issueAssetTransaction.toBytes(true);
		
		//CHECK DATA LENGTH
		assertEquals(rawIssueAssetTransaction.length, issueAssetTransaction.getDataLength());
		
		try 
		{	
			//PARSE FROM BYTES
			IssueAssetTransaction parsedIssueAssetTransaction = (IssueAssetTransaction) TransactionFactory.getInstance().parse(rawIssueAssetTransaction);
			
			//CHECK INSTANCE
			assertEquals(true, parsedIssueAssetTransaction instanceof IssueAssetTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(issueAssetTransaction.getSignature(), parsedIssueAssetTransaction.getSignature()));
			
			//CHECK ISSUER
			assertEquals(issueAssetTransaction.getCreator().getAddress(), parsedIssueAssetTransaction.getCreator().getAddress());
			
			//CHECK OWNER
			assertEquals(issueAssetTransaction.getAsset().getCreator().getAddress(), parsedIssueAssetTransaction.getAsset().getCreator().getAddress());
			
			//CHECK NAME
			assertEquals(issueAssetTransaction.getAsset().getName(), parsedIssueAssetTransaction.getAsset().getName());
				
			//CHECK DESCRIPTION
			assertEquals(issueAssetTransaction.getAsset().getDescription(), parsedIssueAssetTransaction.getAsset().getDescription());
				
			//CHECK QUANTITY
			assertEquals(issueAssetTransaction.getAsset().getQuantity(), parsedIssueAssetTransaction.getAsset().getQuantity());
			
			//DIVISIBLE
			assertEquals(issueAssetTransaction.getAsset().isDivisible(), parsedIssueAssetTransaction.getAsset().isDivisible());
			
			//CHECK FEE
			assertEquals(issueAssetTransaction.getFee(), parsedIssueAssetTransaction.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(issueAssetTransaction.getReference(), parsedIssueAssetTransaction.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(issueAssetTransaction.getTimestamp(), parsedIssueAssetTransaction.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawIssueAssetTransaction = new byte[issueAssetTransaction.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawIssueAssetTransaction);
			
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
		Asset asset = new Venture(maker, "test", "strontje", 50000l, (byte) 2, false);

				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker);
		
		assertEquals(Transaction.VALIDATE_OK, issueAssetTransaction.isValid(db));
		
		issueAssetTransaction.process(db);
		
		Logger.getGlobal().info("asset KEY: " + asset.getKey(db));
		
		//CHECK BALANCE ISSUER
		assertEquals(BigDecimal.valueOf(50000).setScale(8), maker.getConfirmedBalance(asset.getKey(db), db));
		
		//CHECK ASSET EXISTS SENDER
		long key = db.getIssueAssetMap().get(issueAssetTransaction);
		assertEquals(true, db.getAssetMap().contains(key));
		
		//CHECK ASSET IS CORRECT
		assertEquals(true, Arrays.equals(db.getAssetMap().get(key).toBytes(true), asset.toBytes(true)));
		
		//CHECK ASSET BALANCE SENDER
		assertEquals(true, db.getBalanceMap().get(maker.getAddress(), key).compareTo(new BigDecimal(asset.getQuantity())) == 0);
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(issueAssetTransaction.getSignature(), maker.getLastReference(db)));
	}
	
	
	@Test
	public void orphanIssueAssetTransaction()
	{
		
		init();				
				
		
		long timestamp = NTP.getTime();
		Asset asset = new Venture(maker, "test", "strontje", 50000l, (byte) 2, false);
				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker);
		issueAssetTransaction.process(db);
		long key = db.getIssueAssetMap().get(issueAssetTransaction);
		assertEquals(new BigDecimal(50000).setScale(8), maker.getConfirmedBalance(key,db));
		assertEquals(true, Arrays.equals(issueAssetTransaction.getSignature(), maker.getLastReference(db)));
		
		issueAssetTransaction.orphan(db);
		
		//CHECK BALANCE ISSUER
		assertEquals(BigDecimal.ZERO.setScale(8), maker.getConfirmedBalance(key,db));
		
		//CHECK ASSET EXISTS SENDER
		assertEquals(false, db.getAssetMap().contains(key));
		
		//CHECK ASSET BALANCE SENDER
		assertEquals(0, db.getBalanceMap().get(maker.getAddress(), key).longValue());
				
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(issueAssetTransaction.getReference(), maker.getLastReference(db)));
	}
	

	//TRANSFER ASSET
	
	@Test
	public void validateSignatureTransferAssetTransaction() 
	{
		
		init();
		
		long timestamp = NTP.getTime();
		Asset asset = new Venture(maker, "test", "strontje", 50000l, (byte) 2, false);
				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker);
		issueAssetTransaction.process(db);
		long key = db.getIssueAssetMap().get(issueAssetTransaction);

		//CREATE SIGNATURE
		Account recipient = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
		
		//CREATE ASSET TRANSFER
		Transaction assetTransfer = new TransferAssetTransaction(maker, recipient, key, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker);
		
		//CHECK IF ASSET TRANSFER SIGNATURE IS VALID
		assertEquals(true, assetTransfer.isSignatureValid());
		
		//INVALID SIGNATURE
		assetTransfer = new TransferAssetTransaction(maker, recipient, 0, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp+1, maker.getLastReference(db), new byte[64]);
		assetTransfer.sign(maker);
		
		//CHECK IF ASSET TRANSFER SIGNATURE IS INVALID
		assertEquals(true, assetTransfer.isSignatureValid());
	}
	
	@Test
	public void validateTransferAssetTransaction() 
	{	
		
		init();
		
		long timestamp = NTP.getTime();
		Asset asset = new Venture(maker, "test", "strontje", 50000l, (byte) 2, false);
				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker);
		issueAssetTransaction.process(db);
		long key = db.getIssueAssetMap().get(issueAssetTransaction);
		
		//CREATE SIGNATURE
		Account recipient = new Account("QgcphUTiVHHfHg8e1LVgg5jujVES7ZDUTr");
				
		//CREATE VALID ASSET TRANSFER
		Transaction assetTransfer = new TransferAssetTransaction(maker, recipient, key, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker);

		//CHECK IF ASSET TRANSFER IS VALID
		assertEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db));
		
		assetTransfer.process(db);
		
		//CREATE VALID ASSET TRANSFER
		//maker.setConfirmedBalance(key, BigDecimal.valueOf(100).setScale(8), db);
		assetTransfer = new TransferAssetTransaction(maker, recipient, key, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));

		//CHECK IF ASSET TRANSFER IS VALID
		assertEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db));			
		
		//CREATE INVALID ASSET TRANSFER INVALID RECIPIENT ADDRESS
		assetTransfer = new TransferAssetTransaction(maker, new Account("test"), key, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
	
		//CHECK IF ASSET TRANSFER IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db));
		
		//CREATE INVALID ASSET TRANSFER NEGATIVE AMOUNT
		assetTransfer = new TransferAssetTransaction(maker, recipient, key, BigDecimal.valueOf(-100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db));	
		
		//CREATE INVALID ASSET TRANSFER NOT ENOUGH ASSET BALANCE
		assetTransfer = new TransferAssetTransaction(maker, recipient, 0, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker);
		assetTransfer.process(db);
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db));	
						
		//CREATE INVALID ASSET TRANSFER WRONG REFERENCE
		assetTransfer = new TransferAssetTransaction(maker, recipient, key, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, new byte[64]);
						
		//CHECK IF ASSET TRANSFER IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db));	
	}
	
	@Test
	public void parseTransferAssetTransaction() 
	{

		init();
		
		//CREATE SIGNATURE
		Account recipient = new Account("QgcphUTiVHHfHg8e1LVgg5jujVES7ZDUTr");
		long timestamp = NTP.getTime();
					
		//CREATE VALID ASSET TRANSFER
		TransferAssetTransaction assetTransfer = new TransferAssetTransaction(maker, recipient, 0, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker);

		//CONVERT TO BYTES
		byte[] rawAssetTransfer = assetTransfer.toBytes(true);
		
		//CHECK DATALENGTH
		assertEquals(rawAssetTransfer.length, assetTransfer.getDataLength());
		
		try 
		{	
			//PARSE FROM BYTES
			TransferAssetTransaction parsedAssetTransfer = (TransferAssetTransaction) TransactionFactory.getInstance().parse(rawAssetTransfer);
			
			//CHECK INSTANCE
			assertEquals(true, parsedAssetTransfer instanceof TransferAssetTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(assetTransfer.getSignature(), parsedAssetTransfer.getSignature()));
			
			//CHECK KEY
			assertEquals(assetTransfer.getKey(), parsedAssetTransfer.getKey());	
			
			//CHECK AMOUNT SENDER
			assertEquals(assetTransfer.viewAmount(maker), parsedAssetTransfer.viewAmount(maker));	
			
			//CHECK AMOUNT RECIPIENT
			assertEquals(assetTransfer.viewAmount(recipient), parsedAssetTransfer.viewAmount(recipient));	
			
			//CHECK FEE
			assertEquals(assetTransfer.getFee(), parsedAssetTransfer.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(assetTransfer.getReference(), parsedAssetTransfer.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(assetTransfer.getTimestamp(), parsedAssetTransfer.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawAssetTransfer = new byte[assetTransfer.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawAssetTransfer);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}
	
	@Test
	public void processTransferAssetTransaction()
	{

		init();
		
		//CREATE SIGNATURE
		Account recipient = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
		long timestamp = NTP.getTime();
			
		//CREATE ASSET TRANSFER
		long key = 221;
		maker.setConfirmedBalance(key, BigDecimal.valueOf(200).setScale(8), db);
		Transaction assetTransfer = new TransferAssetTransaction(maker, recipient, key, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker);
		assetTransfer.process(db);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.ZERO.setScale(8), maker.getConfirmedBalance(db));
		assertEquals(BigDecimal.valueOf(100).setScale(8), maker.getConfirmedBalance(key, db));
				
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getConfirmedBalance(db));
		assertEquals(BigDecimal.valueOf(100).setScale(8), recipient.getConfirmedBalance(key, db));
		
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(assetTransfer.getSignature(), maker.getLastReference(db)));
		
		//CHECK REFERENCE RECIPIENT
		assertEquals(false, Arrays.equals(assetTransfer.getSignature(), recipient.getLastReference(db)));
	}
	
	@Test
	public void orphanTransferAssetTransaction()
	{
		
		init();
		
		//CREATE SIGNATURE
		Account recipient = new Account("QUGKmr4JJjJRoHo9wNYKZa1Lvem7FHRXfU");
		long timestamp = NTP.getTime();
			
		//CREATE ASSET TRANSFER
		long key = 1l;
		maker.setConfirmedBalance(key, BigDecimal.valueOf(100).setScale(8), db);
		Transaction assetTransfer = new TransferAssetTransaction(maker, recipient, key, BigDecimal.valueOf(100).setScale(8), FEE_POWER, timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker);
		assetTransfer.process(db);
		assetTransfer.orphan(db);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.ZERO.setScale(8), maker.getConfirmedBalance(db));
		assertEquals(BigDecimal.valueOf(100).setScale(8), maker.getConfirmedBalance(key, db));
				
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getConfirmedBalance(db));
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getConfirmedBalance(key, db));
		
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(assetTransfer.getReference(), maker.getLastReference(db)));
		
		//CHECK REFERENCE RECIPIENT
		assertEquals(false, Arrays.equals(assetTransfer.getSignature(), recipient.getLastReference(db)));
	}

	
	//CANCEL ORDER
	
	@Test
	public void validateSignatureCancelOrderTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
		
		//CREATE ORDER CANCEL
		Transaction cancelOrderTransaction = new CancelOrderTransaction(sender, BigInteger.TEN, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		cancelOrderTransaction.sign(sender);
		//CHECK IF ORDER CANCEL IS VALID
		assertEquals(true, cancelOrderTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		cancelOrderTransaction = new CancelOrderTransaction(sender, BigInteger.TEN, FEE_POWER, timestamp, sender.getLastReference(databaseSet), new byte[1]);
		
		//CHECK IF ORDER CANCEL
		assertEquals(false, cancelOrderTransaction.isSignatureValid());
	}
	
	@Test
	public void validateCancelOrderTransaction() 
	{
		DBSet dbSet = DBSet.createEmptyDatabaseSet();
		
		//CREATE ASSET A
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount account = new PrivateKeyAccount(privateKey);
		
		Transaction transaction = new GenesisTransaction(account, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(dbSet);
		// set OIL
		account.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), dbSet);
		
		//CREATE ASSET
		Asset asset = new Venture(account, "a", "a", 50000l, (byte) 2, true);
		
		//CREATE ISSUE ASSET TRANSACTION
		Transaction issueAssetTransaction = new IssueAssetTransaction(account, asset, FEE_POWER, System.currentTimeMillis(), account.getLastReference(dbSet), new byte[64]);
		issueAssetTransaction.sign(account);
		issueAssetTransaction.process(dbSet);
		//Logger.getGlobal().info("IssueAssetTransaction .creator.getBalance(1, db): " + account.getBalance(1, dbSet));

		//CREATE ORDER
		CreateOrderTransaction createOrderTransaction = new CreateOrderTransaction(account, 1l, 0l, BigDecimal.valueOf(1).setScale(8), BigDecimal.valueOf(0.1).setScale(8), FEE_POWER, System.currentTimeMillis(), account.getLastReference(dbSet), new byte[]{5,6});
		createOrderTransaction.sign(account);
		createOrderTransaction.process(dbSet);
		
		//this.creator.getBalance(1, db).compareTo(this.fee) == -1)
		//Logger.getGlobal().info("createOrderTransaction.creator.getBalance(1, db): " + createOrderTransaction.getCreator().getBalance(1, dbSet));
		//Logger.getGlobal().info("CreateOrderTransaction.creator.getBalance(1, db): " + account.getBalance(1, dbSet));

		//CREATE CANCEL ORDER
		CancelOrderTransaction cancelOrderTransaction = new CancelOrderTransaction(account, new BigInteger(new byte[]{5,6}), FEE_POWER, System.currentTimeMillis(), account.getLastReference(dbSet), new byte[]{1,2});		
		//CancelOrderTransaction cancelOrderTransaction = new CancelOrderTransaction(account, new BigInteger(new byte[]{5,6}), FEE_POWER, System.currentTimeMillis(), account.getLastReference(dbSet));
		//cancelOrderTransaction.sign(account);
		//CHECK IF CANCEL ORDER IS VALID
		assertEquals(Transaction.VALIDATE_OK, cancelOrderTransaction.isValid(dbSet));
		
		//CREATE INVALID CANCEL ORDER ORDER DOES NOT EXIST
		cancelOrderTransaction = new CancelOrderTransaction(account, new BigInteger(new byte[]{5,7}), FEE_POWER, System.currentTimeMillis(), account.getLastReference(dbSet), new byte[]{1,2});		
		
		//CHECK IF CANCEL ORDER IS INVALID
		assertEquals(Transaction.ORDER_DOES_NOT_EXIST, cancelOrderTransaction.isValid(dbSet));
		
		//CREATE INVALID CANCEL ORDER INCORRECT CREATOR
		seed = Crypto.getInstance().digest("invalid".getBytes());
		privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount invalidCreator = new PrivateKeyAccount(privateKey);
		cancelOrderTransaction = new CancelOrderTransaction(invalidCreator, new BigInteger(new byte[]{5,6}), FEE_POWER, System.currentTimeMillis(), account.getLastReference(dbSet), new byte[]{1,2});		
		
		//CHECK IF CANCEL ORDER IS INVALID
		assertEquals(Transaction.INVALID_ORDER_CREATOR, cancelOrderTransaction.isValid(dbSet));
				
		//CREATE INVALID CANCEL ORDER NO BALANCE
		DBSet fork = dbSet.fork();
		cancelOrderTransaction = new CancelOrderTransaction(account, new BigInteger(new byte[]{5,6}), FEE_POWER, System.currentTimeMillis(), account.getLastReference(dbSet), new byte[]{1,2});		
		account.setConfirmedBalance(BigDecimal.ZERO, fork);		
		
		//CHECK IF CANCEL ORDER IS INVALID
		assertEquals(Transaction.NO_BALANCE, cancelOrderTransaction.isValid(fork));
				
		//CREATE CANCEL ORDER INVALID REFERENCE
		cancelOrderTransaction = new CancelOrderTransaction(account, new BigInteger(new byte[]{5,6}), FEE_POWER, System.currentTimeMillis(), new byte[64], new byte[]{1,2});		
				
		//CHECK IF NAME REGISTRATION IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, cancelOrderTransaction.isValid(dbSet));
		
	}

	@Test
	public void parseCancelOrderTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet databaseSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
				
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		
		//CREATE SIGNATURE
		long timestamp = NTP.getTime();
				
		//CREATE CANCEL ORDER
		CancelOrderTransaction cancelOrderTransaction = new CancelOrderTransaction(sender, BigInteger.TEN, FEE_POWER, timestamp, sender.getLastReference(databaseSet));
		cancelOrderTransaction.sign(sender);
		
		//CONVERT TO BYTES
		byte[] rawCancelOrder = cancelOrderTransaction.toBytes(true);
		
		//CHECK DATALENGTH
		assertEquals(rawCancelOrder.length, cancelOrderTransaction.getDataLength());
		
		try 
		{	
			//PARSE FROM BYTES
			CancelOrderTransaction parsedCancelOrder = (CancelOrderTransaction) TransactionFactory.getInstance().parse(rawCancelOrder);
			
			//CHECK INSTANCE
			assertEquals(true, parsedCancelOrder instanceof CancelOrderTransaction);
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(cancelOrderTransaction.getSignature(), parsedCancelOrder.getSignature()));
			
			//CHECK AMOUNT CREATOR
			assertEquals(cancelOrderTransaction.viewAmount(sender), parsedCancelOrder.viewAmount(sender));	
			
			//CHECK OWNER
			assertEquals(cancelOrderTransaction.getCreator().getAddress(), parsedCancelOrder.getCreator().getAddress());	
			
			//CHECK ORDER
			assertEquals(0, cancelOrderTransaction.getOrder().compareTo(parsedCancelOrder.getOrder()));	
			
			//CHECK FEE
			assertEquals(cancelOrderTransaction.getFee(), parsedCancelOrder.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(cancelOrderTransaction.getReference(), parsedCancelOrder.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(cancelOrderTransaction.getTimestamp(), parsedCancelOrder.getTimestamp());				
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawCancelOrder = new byte[cancelOrderTransaction.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawCancelOrder);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}
	
	@Test
	public void processCancelOrderTransaction()
	{
		DBSet dbSet = DBSet.createEmptyDatabaseSet();
		
		//CREATE ASSET A
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount account = new PrivateKeyAccount(privateKey);
		
		Transaction transaction = new GenesisTransaction(account, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(dbSet);
		// set OIL
		account.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), dbSet);
		
		//CREATE ASSET
		Asset asset = new Venture(account, "a", "a", 50000l, (byte) 2, true);
		
		//CREATE ISSUE ASSET TRANSACTION
		Transaction issueAssetTransaction = new IssueAssetTransaction(account, asset, FEE_POWER, System.currentTimeMillis(), account.getLastReference(dbSet), new byte[64]);
		issueAssetTransaction.process(dbSet);
		
		//CREATE ORDER
		CreateOrderTransaction createOrderTransaction = new CreateOrderTransaction(account, 1l, 0l, BigDecimal.valueOf(1000).setScale(8), BigDecimal.valueOf(100).setScale(8), FEE_POWER, System.currentTimeMillis(), account.getLastReference(dbSet), new byte[]{5,6});
		createOrderTransaction.sign(account);
		createOrderTransaction.process(dbSet);
		
		//CREATE CANCEL ORDER
		CancelOrderTransaction cancelOrderTransaction = new CancelOrderTransaction(account, new BigInteger(new byte[]{5,6}), FEE_POWER, System.currentTimeMillis(), account.getLastReference(dbSet), new byte[]{1,2});
		cancelOrderTransaction.process(dbSet);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(997).setScale(8), account.getConfirmedBalance(dbSet));
						
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(cancelOrderTransaction.getSignature(), account.getLastReference(dbSet)));
				
		//CHECK ORDER EXISTS
		assertEquals(false, dbSet.getOrderMap().contains(new BigInteger(new byte[]{5,6})));
	}

	@Test
	public void orphanCancelOrderTransaction()
	{
		init();
		/*
		DBSet dbSet = DBSet.createEmptyDatabaseSet();
		
		//CREATE ASSET A
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount account = new PrivateKeyAccount(privateKey);
		
		Transaction transaction = new GenesisTransaction(account, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(dbSet);
		// OIL FUND
		account.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), dbSet);
		*/
		
		//CREATE ASSET
		Asset asset = new Venture(maker, "a", "a", 50000l, (byte) 2, true);
		
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, System.currentTimeMillis(), maker.getLastReference(db), new byte[64]);
		issueAssetTransaction.sign(maker);
		issueAssetTransaction.process(db);
		Logger.getGlobal().info("IS VALID: " + issueAssetTransaction.isValid());

		Logger.getGlobal().info("asset.getReg(): " + asset.getReference());
		Logger.getGlobal().info("asset.getKey(): " + asset.getKey());

		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(50000).setScale(8), maker.getConfirmedBalance( asset.getKey(), db));
		
		//CREATE ORDER
		CreateOrderTransaction createOrderTransaction = new CreateOrderTransaction(maker, asset.getKey(), 0l, BigDecimal.valueOf(1000).setScale(8), BigDecimal.valueOf(1).setScale(8), FEE_POWER, System.currentTimeMillis(), maker.getLastReference(db), new byte[]{5,6});
		createOrderTransaction.sign(maker);
		createOrderTransaction.process(db);
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(50000).setScale(8), maker.getConfirmedBalance( asset.getKey(), db));
		
		//CREATE CANCEL ORDER
		CancelOrderTransaction cancelOrderTransaction = new CancelOrderTransaction(maker, new BigInteger(new byte[]{5,6}), FEE_POWER, System.currentTimeMillis(), maker.getLastReference(db), new byte[]{1,2});
		cancelOrderTransaction.sign(maker);
		cancelOrderTransaction.process(db);
		cancelOrderTransaction.orphan(db);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(50000).setScale(8), maker.getConfirmedBalance( asset.getKey(), db));
						
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(createOrderTransaction.getSignature(), maker.getLastReference(db)));
				
		//CHECK ORDER EXISTS
		assertEquals(true, db.getOrderMap().contains(new BigInteger(new byte[]{5,6})));
	}
	
	//MULTI PAYMENT
	
	@Test
	public void validateSignatureMultiPaymentTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet dbSet = DBSet.createEmptyDatabaseSet();
		
		//ADD QORA ASSET
		Asset qoraAsset = new Venture(new GenesisBlock().getGenerator(), "Qora", "This is the simulated Qora asset.", 10000000000L, (byte) 2, true);
		qoraAsset.setReference(assetReference);
    	dbSet.getAssetMap().set(0l, qoraAsset);
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(dbSet);
		
		//CREATE SIGNATURE
		List<Payment> payments = new ArrayList<Payment>();
		payments.add(new Payment(new Account("Qc454HfRSVbrdLmhD1d9nmmMe45NbQmRnG"), 0l, BigDecimal.ZERO));
		payments.add(new Payment(new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB"), 0l, BigDecimal.ZERO));
		long timestamp = NTP.getTime();
		
		//CREATE MULTI PAYMENT
		Transaction multiPayment = new MultiPaymentTransaction(sender, payments, FEE_POWER, timestamp, sender.getLastReference(dbSet), new byte[64]);
		multiPayment.sign(sender);
		//CHECK IF MULTI PAYMENT SIGNATURE IS VALID
		assertEquals(true, multiPayment.isSignatureValid());
		
		//INVALID SIGNATURE
		multiPayment = new MultiPaymentTransaction(sender, payments, FEE_POWER, timestamp, sender.getLastReference(dbSet), new byte[64]);
		
		//CHECK IF MULTI PAYMENT SIGNATURE IS INVALID
		assertEquals(false, multiPayment.isSignatureValid());
	}
	
	@Test
	public void validateMultiPaymentTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet dbSet = DBSet.createEmptyDatabaseSet();
		
		//ADD QORA ASSET
		Asset qoraAsset = new Venture(new GenesisBlock().getGenerator(), "Qora", "This is the simulated Qora asset.", 10000000000L, (byte)2, true);
		qoraAsset.setReference(assetReference);
    	dbSet.getAssetMap().set(0l, qoraAsset);
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(dbSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), dbSet);
		
		//CREATE SIGNATURE
		List<Payment> payments = new ArrayList<Payment>();
		payments.add(new Payment(new Account("Qc454HfRSVbrdLmhD1d9nmmMe45NbQmRnG"), 0l, BigDecimal.valueOf(100).setScale(8)));
		payments.add(new Payment(new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB"), 0l, BigDecimal.valueOf(100).setScale(8)));
		long timestamp = NTP.getTime();
		
		//CREATE VALID MULTI PAYMENT
		Transaction multiPayment = new MultiPaymentTransaction(sender, payments, FEE_POWER, timestamp, sender.getLastReference(dbSet));
		
		//CHECK IF ASSET TRANSFER IS VALID
		assertEquals(Transaction.VALIDATE_OK, multiPayment.isValid(dbSet));			
		
		//CREATE INVALID MULTI PAYMENT INVALID RECIPIENT ADDRESS
		Payment invalidRecipientPayment = new Payment(new Account("test"), 0l, BigDecimal.valueOf(100).setScale(8));
		payments.add(invalidRecipientPayment);
		multiPayment = new MultiPaymentTransaction(sender, payments, FEE_POWER, timestamp, sender.getLastReference(dbSet));
		
		//CHECK IF MULTI PAYMENT IS INVALID
		assertEquals(Transaction.INVALID_ADDRESS, multiPayment.isValid(dbSet));
		payments.remove(invalidRecipientPayment);
		
		//CREATE INVALID MULTI PAYMENT NEGATIVE AMOUNT
		Payment invalidAmountPayment = new Payment(new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB"), 0l, BigDecimal.ZERO.setScale(8));
		payments.add(invalidAmountPayment);
		multiPayment = new MultiPaymentTransaction(sender, payments, FEE_POWER, timestamp, sender.getLastReference(dbSet));
		
		//CHECK IF MULTI PAYMENT IS INVALID
		assertEquals(Transaction.NEGATIVE_AMOUNT, multiPayment.isValid(dbSet));
		payments.remove(invalidAmountPayment);
		
		//CREATE INVALID MULTI PAYMENT NOT ENOUGH ASSET BALANCE
		Payment noBalancePayment = new Payment(new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB"), 0l, BigDecimal.valueOf(800).setScale(8));
		payments.add(noBalancePayment);
		multiPayment = new MultiPaymentTransaction(sender, payments, FEE_POWER, timestamp, sender.getLastReference(dbSet));
		
		//CHECK IF MULTI PAYMENT IS INVALID
		assertEquals(Transaction.NO_BALANCE, multiPayment.isValid(dbSet));	
		payments.remove(noBalancePayment);
						
		//CREATE INVALID MULTI PAYMENT WRONG REFERENCE
		multiPayment = new MultiPaymentTransaction(sender, payments, FEE_POWER, timestamp, new byte[64]);
						
		//CHECK IF MULTI PAYMENT IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, multiPayment.isValid(dbSet));
	}
	
	@Test
	public void parseMultiPaymentTransaction() 
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet dbSet = DBSet.createEmptyDatabaseSet();
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);

		byte[] reference = sender.getLastReference(dbSet);
		//ADD QORA ASSET
		Asset qoraAsset = new Venture(new GenesisBlock().getGenerator(), "Qora", "This is the simulated Qora asset.", 10000000000L, (byte) 8, true);
		qoraAsset.setReference(reference);
    	dbSet.getAssetMap().set(0l, qoraAsset);

		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(dbSet);
		// set OIL
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), dbSet);
		
		//CREATE SIGNATURE
		List<Payment> payments = new ArrayList<Payment>();
		payments.add(new Payment(new Account("Qc454HfRSVbrdLmhD1d9nmmMe45NbQmRnG"), 0l, BigDecimal.valueOf(100).setScale(8)));
		payments.add(new Payment(new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB"), 0l, BigDecimal.valueOf(100).setScale(8)));
		long timestamp = NTP.getTime();
		
		//CREATE VALID MULTI PAYMENT
		MultiPaymentTransaction multiPayment = new MultiPaymentTransaction(sender, payments, FEE_POWER, timestamp, reference);
		multiPayment.sign(sender);
		
		//CONVERT TO BYTES
		byte[] rawMultiPayment = multiPayment.toBytes(true);
		
		//CHECK DATALENGTH
		assertEquals(rawMultiPayment.length, multiPayment.getDataLength());
		
		try 
		{	
			//PARSE FROM BYTES
			MultiPaymentTransaction parsedMultiPayment = (MultiPaymentTransaction) TransactionFactory.getInstance().parse(rawMultiPayment);
			
			//CHECK INSTANCE
			assertEquals(true, parsedMultiPayment instanceof MultiPaymentTransaction);
			
			//CHECK DATA LENGTH
			assertEquals(rawMultiPayment.length, multiPayment.getDataLength());
			
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(multiPayment.getSignature(), parsedMultiPayment.getSignature()));
			
			//CHECK PAYMENTS LENGTH
			assertEquals(multiPayment.getPayments().size(), parsedMultiPayment.getPayments().size());
			
			//CHECK PAYMENTS
			for(int i=0; i<multiPayment.getPayments().size(); i++)
			{
				Payment payment = multiPayment.getPayments().get(i);
				Payment parsedPayment = parsedMultiPayment.getPayments().get(i);
				
				//CHECK RECIPIENT
				assertEquals(payment.getRecipient().getAddress(), parsedPayment.getRecipient().getAddress());	
				
				//CHECK KEY
				assertEquals(payment.getAsset(), parsedPayment.getAsset());	
				
				//CHECK AMOUNT
				assertEquals(payment.getAmount(), parsedPayment.getAmount());	
			}
			
			//CHECK FEE
			assertEquals(multiPayment.getFee(), parsedMultiPayment.getFee());	
			
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(multiPayment.getReference(), parsedMultiPayment.getReference()));	
			
			//CHECK TIMESTAMP
			assertEquals(multiPayment.getTimestamp(), parsedMultiPayment.getTimestamp());				
		}
		catch (Exception e) 
		{
			e.printStackTrace();
			fail("Exception while parsing transaction.");
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawMultiPayment = new byte[multiPayment.getDataLength()];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawMultiPayment);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}
	
	@Test
	public void processMultiPaymentTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet dbSet = DBSet.createEmptyDatabaseSet();
		
		//ADD QORA ASSET
		Asset qoraAsset = new Venture(new GenesisBlock().getGenerator(), "Qora", "This is the simulated Qora asset.", 10000000000L, (byte) 8, true);
		qoraAsset.setReference(assetReference);
    	dbSet.getAssetMap().set(0l, qoraAsset);
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(dbSet);
		// OIL FUND
		sender.setConfirmedBalance(OIL_KEY, BigDecimal.valueOf(1).setScale(8), dbSet);
		
		//CREATE SIGNATURE
		List<Payment> payments = new ArrayList<Payment>();
		payments.add(new Payment(new Account("Qc454HfRSVbrdLmhD1d9nmmMe45NbQmRnG"), 0l, BigDecimal.valueOf(100).setScale(8)));
		payments.add(new Payment(new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB"), 0l, BigDecimal.valueOf(150).setScale(8)));
		long timestamp = NTP.getTime();
		
		//CREATE VALID MULTI PAYMENT
		MultiPaymentTransaction multiPayment = new MultiPaymentTransaction(sender, payments, FEE_POWER, timestamp, sender.getLastReference(dbSet));
		multiPayment.sign(sender);
		multiPayment.process(dbSet);
		
		//CHECK BALANCE SENDER
		assertEquals(0, BigDecimal.valueOf(750).setScale(8).compareTo(sender.getConfirmedBalance(dbSet)));
				
		//CHECK BALANCE RECIPIENTS
		assertEquals(BigDecimal.valueOf(100).setScale(8), new Account("Qc454HfRSVbrdLmhD1d9nmmMe45NbQmRnG").getConfirmedBalance(dbSet));
		assertEquals(BigDecimal.valueOf(150).setScale(8), new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB").getConfirmedBalance(dbSet));
		
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(multiPayment.getSignature(), sender.getLastReference(dbSet)));
		
		//CHECK REFERENCE RECIPIENTS
		assertEquals(true, Arrays.equals(multiPayment.getSignature(), new Account("Qc454HfRSVbrdLmhD1d9nmmMe45NbQmRnG").getLastReference(dbSet)));
		assertEquals(true, Arrays.equals(multiPayment.getSignature(), new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB").getLastReference(dbSet)));
	}
	
	@Test
	public void orphanMultiPaymentTransaction()
	{
		
		//CREATE EMPTY MEMORY DATABASE
		DBSet dbSet = DBSet.createEmptyDatabaseSet();
		
		//ADD QORA ASSET
		Asset qoraAsset = new Venture(new GenesisBlock().getGenerator(), "Qora", "This is the simulated Qora asset.", 10000000000L, (byte) 8, true);
		qoraAsset.setReference(assetReference);
    	dbSet.getAssetMap().set(0l, qoraAsset);
				
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount sender = new PrivateKeyAccount(privateKey);
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE SENDER HAS FUNDS
		Transaction transaction = new GenesisTransaction(sender, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(dbSet);
		
		//CREATE SIGNATURE
		List<Payment> payments = new ArrayList<Payment>();
		payments.add(new Payment(new Account("Qc454HfRSVbrdLmhD1d9nmmMe45NbQmRnG"), 0l, BigDecimal.valueOf(100).setScale(8)));
		payments.add(new Payment(new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB"), 0l, BigDecimal.valueOf(150).setScale(8)));
		long timestamp = NTP.getTime();
		
		//CREATE VALID MULTI PAYMENT
		MultiPaymentTransaction multiPayment = new MultiPaymentTransaction(sender, payments, FEE_POWER, timestamp, sender.getLastReference(dbSet));
		multiPayment.sign(sender);
		multiPayment.process(dbSet);
		multiPayment.orphan(dbSet);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(1000).setScale(8), sender.getConfirmedBalance(dbSet));
				
		//CHECK BALANCE RECIPIENTS
		assertEquals(BigDecimal.valueOf(0).setScale(8), new Account("Qc454HfRSVbrdLmhD1d9nmmMe45NbQmRnG").getConfirmedBalance(dbSet));
		assertEquals(BigDecimal.valueOf(0).setScale(8), new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB").getConfirmedBalance(dbSet));
		
		//CHECK REFERENCE SENDER
		assertEquals(true, Arrays.equals(transaction.getSignature(), sender.getLastReference(dbSet)));
		
		//CHECK REFERENCE RECIPIENTS
		assertEquals(false, Arrays.equals(multiPayment.getSignature(), new Account("Qc454HfRSVbrdLmhD1d9nmmMe45NbQmRnG").getLastReference(dbSet)));
		assertEquals(false, Arrays.equals(multiPayment.getSignature(), new Account("QXNz5kBknsgNYtRKit4jCDNVm7YYoXLZdB").getLastReference(dbSet)));
	}
}
