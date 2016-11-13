package test.blocks;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import ntp.NTP;

import org.junit.Test;
import org.mapdb.Fun.Tuple3;

import controller.Controller;
import core.BlockChain;
import core.BlockGenerator;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.block.Block;
import core.block.BlockFactory;
import core.block.GenesisBlock;
import core.crypto.Crypto;
import core.item.assets.AssetCls;
import core.transaction.GenesisIssueAssetTransaction;
import core.transaction.GenesisIssuePersonRecord;
import core.transaction.GenesisTransferAssetTransaction;
import core.transaction.Transaction;
import core.transaction.R_Send;
import core.web.blog.BlogEntry;
import database.DBSet;

public class BlockTests
{
	long ERM_KEY = Transaction.RIGHTS_KEY;
	long FEE_KEY = Transaction.FEE_KEY;
	byte FEE_POWER = (byte)0;
	byte[] assetReference = new byte[Crypto.SIGNATURE_LENGTH];
	long timestamp = NTP.getTime();

	boolean forDB = true;
	//CREATE EMPTY MEMORY DATABASE
	private DBSet db = DBSet.createEmptyDatabaseSet();
	private GenesisBlock gb = new GenesisBlock();

	List<Transaction> gbTransactions = gb.getTransactions();
	List<Transaction> transactions =  new ArrayList<Transaction>();
	byte[] transactionsHash =  new byte[Crypto.HASH_LENGTH];
	byte[] atBytes = new byte[0];

	//CREATE KNOWN ACCOUNT
	byte[] seed = Crypto.getInstance().digest("test".getBytes());
	byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
	PrivateKeyAccount generator = new PrivateKeyAccount(privateKey);
	Account recipient = new Account("7F9cZPE1hbzMT21g96U8E1EfMimovJyyJ7");

	Transaction payment;
	
	static Logger LOGGER = Logger.getLogger(BlockTests.class.getName());

	private void init() {

		Controller.getInstance().initBlockChain(db);
		gb = Controller.getInstance().getBlockChain().getGenesisBlock();
		gbTransactions = gb.getTransactions();

		generator.setLastReference(gb.getTimestamp(db), db);
		generator.changeBalance(db, false, ERM_KEY, BigDecimal.valueOf(1000).setScale(8));
		generator.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1000).setScale(8)); // need for payments
	}
		
	private void initTrans(List<Transaction> transactions, long timestamp) {
		payment = new R_Send(generator, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp, generator.getLastReference(db));
		payment.sign(generator, false);
		transactions.add(payment);

	}

	@Test
	public void validateSignatureGenesisBlock()
	{

		//CHECK IF SIGNATURE VALID
		LOGGER.info("getGeneratorSignature " + gb.getSignature().length
				+ " : " + gb.getSignature());

		assertEquals(true, gb.isSignatureValid());
		
		//ADD TRANSACTION SIGNATURE
		LOGGER.info("getGeneratorSignature " + gb.getSignature());

		//ADD a GENESIS TRANSACTION for invalid SIGNATURE
		List<Transaction> transactions = gb.getTransactions();
		transactions.add( new GenesisTransferAssetTransaction(
				new Account("7R2WUFaS7DF2As6NKz13Pgn9ij4sFw6ymZ"), 1l, BigDecimal.valueOf(1).setScale(8)));
		gb.setTransactions(transactions);
		
		// SIGNATURE invalid
		assertEquals(false, gb.isSignatureValid());		

		assertEquals(true, gb.isValid(db));

	}
	
	@Test
	public void validateGenesisBlock()
	{
				
		//CHECK IF VALID
		assertEquals(true, gb.isValid(db));
		
		//ADD INVALID GENESIS TRANSACTION
		List<Transaction> transactions = gb.getTransactions();
		transactions.add( new GenesisTransferAssetTransaction(
				new Account("7R2WUFaS7DF2As6NKz13Pgn9ij4sFw6ymZ"), 1l, BigDecimal.valueOf(-1000).setScale(8)));
		gb.setTransactions(transactions);
		
		//CHECK IF INVALID
		assertEquals(false, gb.isValid(db));
		
		//CREATE NEW BLOCK
		gb = new GenesisBlock();
		
		//CHECK IF VALID
		assertEquals(true, gb.isValid(db));
		
		//PROCESS
		gb.process(db);
		
		//CHECK IF INVALID
		assertEquals(false, gb.isValid(db));
	}
	
	@Test
	public void parseGenesisBlock()
	{
		//gb.process();
				
		//CONVERT TO BYTES
		byte[] rawBlock = gb.toBytes(true, forDB);
		//CHECK length
		assertEquals(rawBlock.length, gb.getDataLength(forDB));
			
		Block parsedBlock = null;
		try 
		{	
			//PARSE FROM BYTES
			parsedBlock = BlockFactory.getInstance().parse(rawBlock, forDB);		
					
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction." + e);
		}
		
		//CHECK length
		assertEquals(rawBlock.length, parsedBlock.getDataLength(forDB));

		//CHECK SIGNATURE
		assertEquals(true, Arrays.equals(gb.getSignature(), parsedBlock.getSignature()));
				
		//CHECK BASE TARGET
		assertEquals(gb.getGeneratingBalance(db), parsedBlock.getGeneratingBalance(db));	
		
		//CHECK FEE
		assertEquals(gb.getTotalFee(), parsedBlock.getTotalFee());	

		//CHECK TRANSACTION COUNT
		assertEquals(gb.getTransactionCount(), parsedBlock.getTransactionCount());

		//CHECK REFERENCE
		assertEquals(true, Arrays.equals(gb.getReference(), parsedBlock.getReference()));			

		//CHECK GENERATOR
		assertEquals(gb.getCreator().getAddress(), parsedBlock.getCreator().getAddress());	
				
		//CHECK INSTANCE
		////assertEquals(true, parsedBlock instanceof GenesisBlock);
				
		//PARSE TRANSACTION FROM WRONG BYTES
		rawBlock = new byte[50];
		
		try 
		{	
			//PARSE FROM BYTES
			BlockFactory.getInstance().parse(rawBlock, forDB);
					
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}
	
	@Test
	public void processGenesisBlock()
	{
										
		//PROCESS GENESISBLOCK
		GenesisBlock genesisBlock = new GenesisBlock();
												
		//CHECK VALID
		assertEquals(true, genesisBlock.isSignatureValid());
		assertEquals(true, genesisBlock.isValid(db));
		
		//PROCESS BLOCK
		genesisBlock.process(db);
		
		Account recipient1 = new Account("73CcZe3PhwvqMvWxDznLAzZBrkeTZHvNzo");
		Account recipient2 = new Account("7FUUEjDSo9J4CYon4tsokMCPmfP4YggPnd");
				
		//CHECK LAST REFERENCE GENERATOR
		assertEquals((long)recipient1.getLastReference(db), 0);
		assertEquals((long)recipient2.getLastReference(db), 0);
		
		//CHECK BALANCE RECIPIENT 1
		assertEquals(1, recipient1.getBalanceUSE(ERM_KEY, db).compareTo(BigDecimal.valueOf(0).setScale(8)));
		assertEquals(0, recipient1.getBalanceUSE(FEE_KEY, db).compareTo(BigDecimal.valueOf(0.0001).setScale(8)));
				
		//CHECK BALANCE RECIPIENT2
		assertEquals(1, recipient2.getBalanceUSE(ERM_KEY, db).compareTo(BigDecimal.valueOf(0).setScale(8)));
		assertEquals(0, recipient2.getBalanceUSE(FEE_KEY, db).compareTo(BigDecimal.valueOf(0.0001).setScale(8)));

		int height = genesisBlock.getHeight(db) + 1;
		Integer forgingData = recipient1.getForgingData(db, height);
		assertEquals(-1, (int)forgingData);

		forgingData = recipient2.getForgingData(db, height);
		assertEquals(-1, (int)forgingData);

		//ORPHAN BLOCK
		genesisBlock.orphan(db);
				
		assertEquals(true, recipient1.getLastReference(db) == null);
		assertEquals(true, recipient2.getLastReference(db) == null);
		
		//CHECK BALANCE RECIPIENT 1
		assertEquals(recipient1.getBalanceUSE(ERM_KEY, db), BigDecimal.valueOf(0).setScale(8));		
		assertEquals(recipient1.getBalanceUSE(FEE_KEY, db), BigDecimal.valueOf(0).setScale(8));		
		//CHECK BALANCE RECIPIENT 2
		assertEquals(true, recipient2.getBalanceUSE(ERM_KEY, db).compareTo(BigDecimal.valueOf(0)) == 0);
		assertEquals(true, recipient2.getBalanceUSE(FEE_KEY, db).compareTo(BigDecimal.valueOf(0)) == 0);
				
	}

	////////////////
	@Test
	public void validateSignatureBlock()
	{
		
		init();
		gb.process(db);
				
						
		//PROCESS GENESIS TRANSACTION TO MAKE SURE GENERATOR HAS FUNDS
		//Transaction transaction = new GenesisTransaction(generator, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		//transaction.process(databaseSet, false);

		//GENERATE NEXT BLOCK
		//BigDecimal genBal = generator.getGeneratingBalance(db);
		BlockGenerator blockGenerator = new BlockGenerator(false);
		Block newBlock = BlockGenerator.generateNextBlock(db, generator, gb, transactionsHash);
		newBlock.sign(generator);
		
		////ADD TRANSACTION SIGNATURE
		///newBlock.makeTransactionsHash();
		
		//CHECK IF SIGNATURE VALID
		assertEquals(true, newBlock.isSignatureValid());

		//INVALID TRANSACTION HASH
		Transaction payment = new R_Send(generator, FEE_POWER, generator, FEE_KEY, BigDecimal.valueOf(1).setScale(8), timestamp, generator.getLastReference(db));
		payment.sign(generator, false);
		transactions.add(payment);
		
		// SET TRANSACTIONS to BLOCK
		newBlock.setTransactions(transactions);
		
		//CHECK IF SIGNATURE INVALID
		assertEquals(false, newBlock.isSignatureValid());
		
		//INVALID GENERATOR SIGNATURE
		newBlock = BlockFactory.getInstance().create(newBlock.getVersion(), newBlock.getReference(), generator, new byte[Crypto.HASH_LENGTH], new byte[0]);
		
		///CHECK IF SIGNATURE INVALID
		assertEquals(false, newBlock.isSignatureValid());
		
		//VALID TRANSACTION SIGNATURE
		newBlock = BlockGenerator.generateNextBlock(db, generator, gb, transactionsHash);	
		
		//ADD TRANSACTION
		Account recipient = new Account("7F9cZPE1hbzMT21g96U8E1EfMimovJyyJ7");
		long timestamp = newBlock.getTimestamp(db);
		payment = new R_Send(generator, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp, generator.getLastReference(db));
		payment.sign(generator, false);
		assertEquals(Transaction.VALIDATE_OK, payment.isValid(db, null));
		transactions = new ArrayList<Transaction>();
		transactions.add(payment);
		
		//ADD TRANSACTION SIGNATURE
		newBlock.setTransactions(transactions);
		
		//CHECK VALID TRANSACTION SIGNATURE
		assertEquals(false, newBlock.isSignatureValid());

		newBlock.sign(generator);
		//CHECK VALID TRANSACTION SIGNATURE
		assertEquals(true, newBlock.isSignatureValid());
		
		//INVALID TRANSACTION SIGNATURE
		newBlock = BlockGenerator.generateNextBlock(db, generator, gb, transactionsHash);	
		
		//ADD TRANSACTION
		payment = new R_Send(generator, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(200).setScale(8), NTP.getTime(), generator.getLastReference(db), payment.getSignature());
		transactions = new ArrayList<Transaction>();
		transactions.add(payment);
				
		//ADD TRANSACTION SIGNATURE
		newBlock.setTransactions(transactions);
		newBlock.sign(generator);
		
		//CHECK INVALID TRANSACTION SIGNATURE
		assertEquals(false, newBlock.isValid(db));
		// BUT valid HERE
		assertEquals(true, newBlock.isSignatureValid());	
	}
	
	@Test
	public void validateBlock()
	{
		init();
		gb.process(db);
						
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount generator = new PrivateKeyAccount(privateKey);
				
		Transaction transaction;
		
		//PROCESS GENESIS TRANSACTION TO MAKE SURE GENERATOR HAS FUNDS
		/*
		transaction = new GenesisTransaction(generator, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		transaction.process(databaseSet);
		*/
		
		// (issuer, recipient, 0l, bdAmount, timestamp)
		// need add VOLUME for generating new block - 0l asset!
		transaction = new GenesisTransferAssetTransaction(generator,
				ERM_KEY, BigDecimal.valueOf(100000).setScale(8));
		transaction.process(db, gb, false);
		transaction = new GenesisTransferAssetTransaction(generator,
				FEE_KEY, BigDecimal.valueOf(1000).setScale(8));
		transaction.process(db, gb, false);
		
		//GENERATE NEXT BLOCK
		//BigDecimal genBal = generator.getGeneratingBalance(db);
		BlockGenerator blockGenerator = new BlockGenerator(false);
		Block newBlock = BlockGenerator.generateNextBlock(db, generator, gb, transactionsHash);
				
		//ADD TRANSACTION SIGNATURE
		//byte[] transactionsSignature = Crypto.getInstance().sign(generator, newBlock.getSignature());
		newBlock.makeTransactionsHash();
		
		//CHECK IF VALID
		assertEquals(true, newBlock.isValid(db));
		
		//CHANGE REFERENCE
		Block invalidBlock = BlockFactory.getInstance().create(newBlock.getVersion(), new byte[128], newBlock.getCreator(), transactionsHash, atBytes);
		invalidBlock.sign(generator);
		
		//CHECK IF INVALID
		assertEquals(false, invalidBlock.isValid(db));
						
		//ADD INVALID TRANSACTION
		invalidBlock = BlockGenerator.generateNextBlock(db, generator, gb, transactionsHash);
		Account recipient = new Account("7F9cZPE1hbzMT21g96U8E1EfMimovJyyJ7");
		long timestamp = newBlock.getTimestamp(db);
		Transaction payment = new R_Send(generator, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(-100).setScale(8), timestamp, generator.getLastReference(db));
		payment.sign(generator, false);
		
		transactions = new ArrayList<Transaction>();
		transactions.add(payment);
				
		//ADD TRANSACTION SIGNATURE
		invalidBlock.setTransactions(transactions);
		invalidBlock.sign(generator);
		
		//CHECK IF INVALID
		assertEquals(false, invalidBlock.isValid(db));
		
		//ADD GENESIS TRANSACTION
		invalidBlock = BlockGenerator.generateNextBlock(db, generator, gb, transactionsHash);
		
		//transaction = new GenesisTransaction(generator, BigDecimal.valueOf(1000).setScale(8), newBlock.getTimestamp());
		transaction = new GenesisIssueAssetTransaction(GenesisBlock.makeAsset(Transaction.RIGHTS_KEY));
		transactions.add(transaction);
				
		//ADD TRANSACTION SIGNATURE
		invalidBlock.setTransactions(transactions);
		invalidBlock.sign(generator);
		
		//CHECK IF INVALID
		assertEquals(false, invalidBlock.isValid(db));
	}
	
	@Test
	public void parseBlock()
	{
		init();
		gb.process(db);
								
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount generator = new PrivateKeyAccount(privateKey);
										
		//PROCESS GENESIS TRANSACTION TO MAKE SURE GENERATOR HAS FUNDS
		//Transaction transaction = new GenesisTransaction(generator, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		//transaction.process(databaseSet, false);
		generator.setLastReference(gb.getTimestamp(db), db);
		generator.changeBalance(db, false, ERM_KEY, BigDecimal.valueOf(1000).setScale(8));
		generator.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1000).setScale(8));

								
		//GENERATE NEXT BLOCK
		Block block = BlockGenerator.generateNextBlock(db, generator, gb, transactionsHash);
						
		//FORK
		DBSet fork = db.fork();
				
		//GENERATE PAYMENT 1
		Account recipient = new Account("7F9cZPE1hbzMT21g96U8E1EfMimovJyyJ7");
		long timestamp = block.getTimestamp(db);
		Transaction payment1 = new R_Send(generator, FEE_POWER, recipient, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp, generator.getLastReference(db));
		payment1.sign(generator, false);
		assertEquals(Transaction.VALIDATE_OK, payment1.isValid(fork, null));
		
		//payment1.process(fork);
		transactions = new ArrayList<Transaction>();
		transactions.add(payment1);

				
		//GENERATE PAYMENT 2
		Account recipient2 = new Account("7AfGz1FJ6tUnxxKSAHfcjroFEm8jSyVm7r");
		Transaction payment2 = new R_Send(generator, FEE_POWER, recipient2, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp, generator.getLastReference(fork));
		payment2.sign(generator, false);
		assertEquals(Transaction.VALIDATE_OK, payment2.isValid(fork, null));
		
		transactions.add(payment2);
						
		//ADD TRANSACTION SIGNATURE
		block.setTransactions(transactions);
		block.sign(generator);
				
		//CONVERT TO BYTES
		byte[] rawBlock = block.toBytes(true, forDB);
				
		try 
		{	
			//PARSE FROM BYTES
			Block parsedBlock = BlockFactory.getInstance().parse(rawBlock, forDB);
					
			//CHECK INSTANCE
			assertEquals(false, parsedBlock instanceof GenesisBlock);
					
			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(block.getSignature(), parsedBlock.getSignature()));
					
			//CHECK GENERATOR
			assertEquals(block.getCreator().getAddress(), parsedBlock.getCreator().getAddress());	
					
			//CHECK BASE TARGET
			assertEquals(block.getGeneratingBalance(db), parsedBlock.getGeneratingBalance(db));	
			
			//CHECK FEE
			assertEquals(block.getTotalFee(), parsedBlock.getTotalFee());	
					
			//CHECK REFERENCE
			assertEquals(true, Arrays.equals(block.getReference(), parsedBlock.getReference()));	
					
			//CHECK TIMESTAMP
			assertEquals(block.getTimestamp(db), parsedBlock.getTimestamp(db));		
			
			//CHECK TRANSACTIONS COUNT
			assertEquals(block.getTransactionCount(), parsedBlock.getTransactionCount());		
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction.");
		}
				
		//PARSE TRANSACTION FROM WRONG BYTES
		rawBlock = new byte[50];
		
		try 
		{	
			//PARSE FROM BYTES
			BlockFactory.getInstance().parse(rawBlock, forDB);
					
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}			
	}
	
	@Test
	public void processBlock()
	{
										
		init();
		// already processed gb.process(db);
										
		//CREATE KNOWN ACCOUNT
		byte[] seed = Crypto.getInstance().digest("test".getBytes());
		byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
		PrivateKeyAccount generator = new PrivateKeyAccount(privateKey);
												
		//PROCESS GENESIS TRANSACTION TO MAKE SURE GENERATOR HAS FUNDS for generate
		//Transaction transaction = new GenesisTransaction(generator, BigDecimal.valueOf(1000).setScale(8), NTP.getTime());
		//transaction.process(databaseSet, false);
		generator.setLastReference(gb.getTimestamp(db), db);
		generator.changeBalance(db, false, ERM_KEY, BigDecimal.valueOf(100000).setScale(8));
		generator.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1000).setScale(8));
								
		//GENERATE NEXT BLOCK
		Block block = BlockGenerator.generateNextBlock(db, generator, gb, transactionsHash);
		
		//FORK
		DBSet fork = db.fork();
		
		//GENERATE PAYMENT 1
		Account recipient1 = new Account("7JU8UTuREAJG2yht5ASn7o1Ur34P1nvTk5");
		// TIMESTAMP for records make lower
		long timestamp = block.getTimestamp(db) - 1000;
		Transaction payment1 = new R_Send(generator, FEE_POWER, recipient1, FEE_KEY, BigDecimal.valueOf(100).setScale(8), timestamp++, generator.getLastReference(fork));
		payment1.sign(generator, false);
		assertEquals(Transaction.VALIDATE_OK, payment1.isValid(fork, null));

		payment1.process(fork, gb, false);
		
		transactions.add(payment1);
				
		//GENERATE PAYMENT 2
		Account recipient2 = new Account("7G1G45RX4td59daBv6PoN84nAJA49NZ47i");
		Transaction payment2 = new R_Send(generator, FEE_POWER, recipient2, ERM_KEY, BigDecimal.valueOf(10).setScale(8),  timestamp++, generator.getLastReference(fork));
		payment2.sign(generator, false);
		assertEquals(Transaction.VALIDATE_OK, payment2.isValid(fork, null));

		transactions.add(payment2);
		
		//ADD TRANSACTION SIGNATURE
		block.setTransactions(transactions);

		generator.setLastForgingData(db, block.getHeightByParent(db));
		block.setCalcGeneratingBalance(db);
		block.sign(generator);
		
		//CHECK VALID
		assertEquals(true, block.isSignatureValid());
		assertEquals(true, block.isValid(db));
		
		//PROCESS BLOCK
		block.process(db);
		
		//CHECK BALANCE GENERATOR
		assertEquals(generator.getBalanceUSE(ERM_KEY, db), BigDecimal.valueOf(99990).setScale(8));
		//assertEquals(generator.getBalanceUSE(FEE_KEY, db), BigDecimal.valueOf(900.00009482).setScale(8));
		assertEquals(generator.getBalanceUSE(FEE_KEY, db), BigDecimal.valueOf(900.0000).setScale(8));
		
		//CHECK LAST REFERENCE GENERATOR
		assertEquals((long)generator.getLastReference(db), (long)payment2.getTimestamp());
		
		//CHECK BALANCE RECIPIENT 1
		assertEquals(recipient1.getBalanceUSE(ERM_KEY, db), BigDecimal.valueOf(0).setScale(8));
		assertEquals(recipient1.getBalanceUSE(FEE_KEY, db), BigDecimal.valueOf(100).setScale(8));
		
		//CHECK LAST REFERENCE RECIPIENT 1
		assertEquals((long)recipient1.getLastReference(db), (long)payment1.getTimestamp());
		
		//CHECK BALANCE RECIPIENT2
		assertEquals(recipient2.getBalanceUSE(ERM_KEY, db), BigDecimal.valueOf(10).setScale(8));
		assertEquals(recipient2.getBalanceUSE(FEE_KEY, db), BigDecimal.valueOf(0).setScale(8));
				
		//CHECK LAST REFERENCE RECIPIENT 2
		assertNotEquals(recipient2.getLastReference(db), payment2.getTimestamp());
		
		//CHECK TOTAL FEE
		assertEquals(block.getTotalFee(), BigDecimal.valueOf(0.00020048).setScale(8));
		
		//CHECK TOTAL TRANSACTIONS
		assertEquals(2, block.getTransactionCount());
		
		//CHECK LAST BLOCK
		assertEquals(true, Arrays.equals(block.getSignature(), db.getBlockMap().getLastBlock().getSignature()));
	

		////////////////////////////////////
		//ORPHAN BLOCK
		//////////////////////////////////
		block.orphan(db);
		
		//CHECK BALANCE GENERATOR
		assertEquals(generator.getBalanceUSE(FEE_KEY, db), BigDecimal.valueOf(1000).setScale(8));
		
		//CHECK LAST REFERENCE GENERATOR
		assertEquals((long)generator.getLastReference(db), gb.getTimestamp(db));
		
		//CHECK BALANCE RECIPIENT 1
		assertEquals(recipient1.getBalanceUSE(FEE_KEY, db), BigDecimal.valueOf(0).setScale(8));
		
		//CHECK LAST REFERENCE RECIPIENT 1
		assertNotEquals(recipient1.getLastReference(db), payment1.getTimestamp());
		
		//CHECK BALANCE RECIPIENT 2
		assertEquals(true, recipient2.getBalanceUSE(FEE_KEY, db).compareTo(BigDecimal.valueOf(0)) == 0);
				
		//CHECK LAST REFERENCE RECIPIENT 2
		assertEquals((long)recipient2.getLastReference(db), (long)0);
		
		//CHECK LAST BLOCK
		assertEquals(true, Arrays.equals(gb.getSignature(), db.getBlockMap().getLastBlock().getSignature()));
	}
}
