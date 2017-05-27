package test.records;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
 import org.apache.log4j.Logger;

import ntp.NTP;

import org.junit.Test;
import org.mapdb.Fun.Tuple3;

import controller.Controller;
import core.BlockChain;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.block.GenesisBlock;
import core.crypto.Crypto;
import core.item.assets.AssetCls;
import core.item.assets.AssetUnique;
import core.item.assets.AssetVenture;
import core.transaction.CancelOrderTransaction;
import core.transaction.CreateOrderTransaction;
import core.transaction.IssueAssetTransaction;
import core.transaction.Transaction;
import core.transaction.TransactionFactory;
import core.transaction.R_Send;
import core.transaction.R_Send;

//import com.google.common.primitives.Longs;

import database.DBSet;

public class TestRecAsset {

	static Logger LOGGER = Logger.getLogger(TestRecAsset.class.getName());

	Long releaserReference = null;

	long FEE_KEY = AssetCls.FEE_KEY;
	byte FEE_POWER = (byte)1;
	byte[] assetReference = new byte[64];
	long timestamp = NTP.getTime();
	
	private byte[] icon = new byte[]{1,3,4,5,6,9}; // default value
	private byte[] image = new byte[]{4,11,32,23,45,122,11,-45}; // default value

	//CREATE EMPTY MEMORY DATABASE
	private DBSet db;
	private GenesisBlock gb;
	private BlockChain bchain;
	Controller cntrl;
	
	//CREATE KNOWN ACCOUNT
	byte[] seed = Crypto.getInstance().digest("tes213sdffsdft".getBytes());
	byte[] privateKey = Crypto.getInstance().createKeyPair(seed).getA();
	PrivateKeyAccount maker = new PrivateKeyAccount(privateKey);
	
	byte[] seed_1 = Crypto.getInstance().digest("tes213sdffsdft_1".getBytes());
	byte[] privateKey_1 = Crypto.getInstance().createKeyPair(seed_1).getA();
	PrivateKeyAccount maker_1 = new PrivateKeyAccount(privateKey_1);
	
	AssetCls asset;
	AssetCls assetMovable;
	long key = 0;

	R_Send rsend;
	Tuple3<BigDecimal, BigDecimal, BigDecimal> balance3;
	
	// INIT ASSETS
	private void init() {
		
		db = DBSet.createEmptyDatabaseSet();
		cntrl = Controller.getInstance();
		cntrl.initBlockChain(db);
		bchain = cntrl.getBlockChain();
		gb = bchain.getGenesisBlock();
		//gb.process(db);
		
		// FEE FUND
		maker.setLastReference(gb.getTimestamp(db), db);
		maker.changeBalance(db, false, FEE_KEY, BigDecimal.valueOf(1).setScale(8));
		
		maker_1.setLastReference(gb.getTimestamp(db), db);
		
		asset = new AssetVenture(maker, "aasdasd", icon, image, "asdasda", false, 50000l, (byte) 2, true);
		//key = asset.getKey();

		assetMovable = new AssetVenture(maker, "movable", icon, image, "...", true, 50000l, (byte) 2, true);

	}
	
	
	//ISSUE ASSET TRANSACTION
	
	@Test
	public void validateSignatureIssueAssetTransaction() 
	{
		
		init();
		
		//CREATE ASSET
		AssetUnique asset = new AssetUnique(maker, "test", icon, image, "strontje");
				
		//CREATE ISSUE ASSET TRANSACTION
		Transaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker, false);
		
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
		AssetUnique asset = new AssetUnique(maker, "test", icon, image, "strontje");
		LOGGER.info("asset: " + asset.getType()[0] + ", " + asset.getType()[1]);
		byte [] raw = asset.toBytes(false, false);
		assertEquals(raw.length, asset.getDataLength(false));
		asset.setReference(new byte[64]);
		raw = asset.toBytes(true, false);
		assertEquals(raw.length, asset.getDataLength(true));
				
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
			assertEquals((long)issueAssetTransaction.getReference(), (long)parsedIssueAssetTransaction.getReference());	
			
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
		
		AssetUnique asset = new AssetUnique(maker, "test", icon, image, "strontje");
				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker, false);
		
		assertEquals(Transaction.VALIDATE_OK, issueAssetTransaction.isValid(db, releaserReference));
		
		issueAssetTransaction.process(db, gb, false);
		
		LOGGER.info("asset KEY: " + asset.getKey(db));
		
		//CHECK BALANCE ISSUER
		assertEquals(BigDecimal.valueOf(1).setScale(8), maker.getBalanceUSE(asset.getKey(db), db));
		
		//CHECK ASSET EXISTS SENDER
		long key = db.getIssueAssetMap().get(issueAssetTransaction);
		assertEquals(true, db.getItemAssetMap().contains(key));
		
		//CHECK ASSET IS CORRECT
		assertEquals(true, Arrays.equals(db.getItemAssetMap().get(key).toBytes(true, false), asset.toBytes(true, false)));
		
		//CHECK ASSET BALANCE SENDER
		assertEquals(true, db.getAssetBalanceMap().get(maker.getAddress(), key).a.compareTo(new BigDecimal(asset.getQuantity())) == 0);
				
		//CHECK REFERENCE SENDER
		assertEquals((long)issueAssetTransaction.getTimestamp(), (long)maker.getLastReference(db));
	}
	
	
	@Test
	public void orphanIssueAssetTransaction()
	{
		
		init();				
				
		AssetUnique asset = new AssetUnique(maker, "test", icon, image, "strontje");
				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker, false);
		issueAssetTransaction.process(db, gb, false);
		long key = db.getIssueAssetMap().get(issueAssetTransaction);
		assertEquals(new BigDecimal(1).setScale(8), maker.getBalanceUSE(key,db));
		assertEquals((long)issueAssetTransaction.getTimestamp(), (long)maker.getLastReference(db));
		
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
	

	//TRANSFER ASSET
	
	@Test
	public void validateSignatureR_Send() 
	{
		
		init();
		
		AssetUnique asset = new AssetUnique(maker, "test", icon, image, "strontje");
				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker, false);
		issueAssetTransaction.process(db, gb, false);
		long key = db.getIssueAssetMap().get(issueAssetTransaction);

		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		
		//CREATE ASSET TRANSFER
		Transaction assetTransfer = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8), timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker, false);
		
		//CHECK IF ASSET TRANSFER SIGNATURE IS VALID
		assertEquals(true, assetTransfer.isSignatureValid());
		
		//INVALID SIGNATURE
		assetTransfer = new R_Send(maker, FEE_POWER, recipient, 0, BigDecimal.valueOf(100).setScale(8), timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker, false);
		assetTransfer = new R_Send(maker, FEE_POWER, recipient, 0, BigDecimal.valueOf(100).setScale(8), timestamp+1, maker.getLastReference(db), assetTransfer.getSignature());
		
		//CHECK IF ASSET TRANSFER SIGNATURE IS INVALID
		assertEquals(false, assetTransfer.isSignatureValid());
	}
	
	@Test
	public void validateR_Send() 
	{	
		
		init();
						
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		assertEquals(Transaction.VALIDATE_OK, issueAssetTransaction.isValid(db, releaserReference));

		issueAssetTransaction.sign(maker, false);
		issueAssetTransaction.process(db, gb, false);
		long key = asset.getKey(db);
		//assertEquals(asset.getQuantity(), maker.getConfirmedBalance(FEE_KEY, db));
		assertEquals(new BigDecimal(asset.getQuantity()).setScale(8), maker.getBalanceUSE(key, db));
		
		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
				
		//CREATE VALID ASSET TRANSFER
		Transaction assetTransfer = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8), timestamp+100, maker.getLastReference(db));

		//CHECK IF ASSET TRANSFER IS VALID
		assertEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db, releaserReference));
		
		assetTransfer.sign(maker, false);
		assetTransfer.process(db, gb, false);
		
		//CREATE VALID ASSET TRANSFER
		//maker.setConfirmedBalance(key, BigDecimal.valueOf(100).setScale(8), db);
		assetTransfer = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8), timestamp+200, maker.getLastReference(db));

		//CHECK IF ASSET TRANSFER IS VALID
		assertEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db, releaserReference));			
		
		//CREATE INVALID ASSET TRANSFER INVALID RECIPIENT ADDRESS
		assetTransfer = new R_Send(maker, FEE_POWER, new Account("test"), key, BigDecimal.valueOf(100).setScale(8), timestamp, maker.getLastReference(db));
	
		//CHECK IF ASSET TRANSFER IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db, releaserReference));
		
		//CREATE INVALID ASSET TRANSFER NEGATIVE AMOUNT
		assetTransfer = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(-100).setScale(8), timestamp, maker.getLastReference(db));
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db, releaserReference));	
		
		//CREATE INVALID ASSET TRANSFER NOT ENOUGH ASSET BALANCE
		assetTransfer = new R_Send(maker, FEE_POWER, recipient, 0, BigDecimal.valueOf(100).setScale(8), timestamp, maker.getLastReference(db));
		//assetTransfer.sign(maker, false);
		//assetTransfer.process(db, false);
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db, releaserReference));	
						
		//CREATE INVALID ASSET TRANSFER WRONG REFERENCE
		assetTransfer = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8), timestamp, -123L);
						
		//CHECK IF ASSET TRANSFER IS INVALID
		assertNotEquals(Transaction.VALIDATE_OK, assetTransfer.isValid(db, releaserReference));	
	}
	
	@Test
	public void parseR_Send() 
	{

		init();
		
		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		long timestamp = NTP.getTime();
					
		//CREATE VALID ASSET TRANSFER
		R_Send assetTransfer = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8), timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker, false);

		//CONVERT TO BYTES
		byte[] rawAssetTransfer = assetTransfer.toBytes(true, releaserReference);
		
		//CHECK DATALENGTH
		assertEquals(rawAssetTransfer.length, assetTransfer.getDataLength(false));
		
		try 
		{	
			//PARSE FROM BYTES
			R_Send parsedAssetTransfer = (R_Send) TransactionFactory.getInstance().parse(rawAssetTransfer, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedAssetTransfer instanceof R_Send);
			
			//CHECK TYPEBYTES
			assertEquals(true, Arrays.equals(assetTransfer.getTypeBytes(), parsedAssetTransfer.getTypeBytes()));				

			//CHECK TIMESTAMP
			assertEquals(assetTransfer.getTimestamp(), parsedAssetTransfer.getTimestamp());				

			//CHECK REFERENCE
			assertEquals(assetTransfer.getReference(), parsedAssetTransfer.getReference());	

			//CHECK CREATOR
			assertEquals(assetTransfer.getCreator().getAddress(), parsedAssetTransfer.getCreator().getAddress());				

			//CHECK FEE POWER
			assertEquals(assetTransfer.getFee(), parsedAssetTransfer.getFee());	

			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(assetTransfer.getSignature(), parsedAssetTransfer.getSignature()));
			
			//CHECK KEY
			assertEquals(assetTransfer.getKey(), parsedAssetTransfer.getKey());	
			
			//CHECK AMOUNT
			assertEquals(assetTransfer.getAmount(maker), parsedAssetTransfer.getAmount(maker));	
			
			//CHECK AMOUNT RECIPIENT
			assertEquals(assetTransfer.getAmount(recipient), parsedAssetTransfer.getAmount(recipient));	
						
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction." + e);
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawAssetTransfer = new byte[assetTransfer.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawAssetTransfer, releaserReference);
			
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
		
		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		long timestamp = NTP.getTime();
			
		//CREATE ASSET TRANSFER
		maker.changeBalance(db, false, key, BigDecimal.valueOf(200).setScale(8));
		Transaction assetTransfer = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8), timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker, false);
		assetTransfer.isValid(db, releaserReference);
		assetTransfer.process(db, gb, false);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(100).setScale(8), maker.getBalanceUSE(key, db));
				
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getBalanceUSE(FEE_KEY, db));
		assertEquals(BigDecimal.valueOf(100).setScale(8), recipient.getBalanceUSE(key, db));
		
		//CHECK REFERENCE SENDER
		assertEquals(assetTransfer.getTimestamp(), maker.getLastReference(db));
		
		//CHECK REFERENCE RECIPIENT
		assertNotEquals(assetTransfer.getTimestamp(), recipient.getLastReference(db));
	}
	
	@Test
	public void orphanR_Send()
	{
		
		init();
		
		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		long timestamp = NTP.getTime();
			
		//CREATE ASSET TRANSFER
		long key = 1l;
		maker.changeBalance(db, false, key, BigDecimal.valueOf(100).setScale(8));
		Transaction assetTransfer = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8), timestamp, maker.getLastReference(db));
		assetTransfer.sign(maker, false);
		assetTransfer.process(db, gb, false);
		assetTransfer.orphan(db, false);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(100).setScale(8), maker.getBalanceUSE(key, db));
				
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getBalanceUSE(key, db));
		
		//CHECK REFERENCE SENDER
		assertEquals(assetTransfer.getReference(), maker.getLastReference(db));
		
		//CHECK REFERENCE RECIPIENT
		assertNotEquals(assetTransfer.getTimestamp(), recipient.getLastReference(db));
	}

	

	//MESSAGE ASSET
	
	@Test
	public void validateSignatureMessageTransaction() 
	{
		
		init();
		
		//AssetUnique asset = new AssetUnique(maker, "test", "strontje");
				
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp, maker.getLastReference(db));
		issueAssetTransaction.sign(maker, false);
		issueAssetTransaction.process(db, gb, false);
		long key = db.getIssueAssetMap().get(issueAssetTransaction);

		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		
		//CREATE ASSET TRANSFER
		Transaction messageTransaction = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp, maker.getLastReference(db));
		messageTransaction.sign(maker, false);
		
		//CHECK IF ASSET TRANSFER SIGNATURE IS VALID
		assertEquals(true, messageTransaction.isSignatureValid());
		
		//INVALID SIGNATURE
		messageTransaction = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp, maker.getLastReference(db));
		messageTransaction.sign(maker, false);
		messageTransaction = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8), 
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp+1, maker.getLastReference(db), messageTransaction.getSignature());
		
		//CHECK IF ASSET TRANSFER SIGNATURE IS INVALID
		assertEquals(false, messageTransaction.isSignatureValid());
	}
	
	@Test
	public void validateMessageTransaction() 
	{	
		
		init();
						
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueMessageTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp++, maker.getLastReference(db));
		assertEquals(Transaction.VALIDATE_OK, issueMessageTransaction.isValid(db, releaserReference));

		issueMessageTransaction.sign(maker, false);
		issueMessageTransaction.process(db, gb, false);
		long key = asset.getKey(db);
		//assertEquals(asset.getQuantity(), maker.getConfirmedBalance(FEE_KEY, db));
		assertEquals(new BigDecimal(asset.getQuantity()).setScale(8), maker.getBalanceUSE(key, db));
		
		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		
		//timestamp += 100;
		//CREATE VALID ASSET TRANSFER
		Transaction messageTransaction = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));

		//CHECK IF ASSET TRANSFER IS VALID
		assertEquals(Transaction.VALIDATE_OK, messageTransaction.isValid(db, releaserReference));
		
		messageTransaction.sign(maker, false);
		messageTransaction.process(db, gb, false);
		timestamp ++;
		
		//CREATE VALID ASSET TRANSFER
		//maker.setConfirmedBalance(key, BigDecimal.valueOf(100).setScale(8), db);
		messageTransaction = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));

		//CHECK IF ASSET TRANSFER IS VALID
		assertEquals(Transaction.VALIDATE_OK, messageTransaction.isValid(db, releaserReference));			
		
		//CREATE INVALID ASSET TRANSFER INVALID RECIPIENT ADDRESS
		messageTransaction = new R_Send(maker, FEE_POWER, new Account("test"), key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
	
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.INVALID_ADDRESS, messageTransaction.isValid(db, releaserReference));
		
		//CREATE INVALID ASSET TRANSFER NEGATIVE AMOUNT
		messageTransaction = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(-100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.NOT_MOVABLE_ASSET, messageTransaction.isValid(db, releaserReference));	
		
		//CREATE INVALID ASSET TRANSFER NOT ENOUGH ASSET BALANCE
		messageTransaction = new R_Send(maker, FEE_POWER, recipient, 99, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.ITEM_ASSET_NOT_EXIST, messageTransaction.isValid(db, releaserReference));	

		//CREATE INVALID ASSET TRANSFER NOT ENOUGH ASSET BALANCE
		messageTransaction = new R_Send(maker, FEE_POWER, recipient, key - 1, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.NO_BALANCE, messageTransaction.isValid(db, releaserReference));	

		//CREATE INVALID ASSET TRANSFER WRONG REFERENCE
		messageTransaction = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, -123L);
						
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, messageTransaction.isValid(db, releaserReference));	

		// NOT DIVISIBLE
		asset = new AssetVenture(maker, "not divisible", icon, image, "asdasda", false, 0l, (byte) 0, false);
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp++, maker.getLastReference(db));
		assertEquals(Transaction.VALIDATE_OK, issueAssetTransaction.isValid(db, releaserReference));	
		issueAssetTransaction.sign(maker, false);
		issueAssetTransaction.process(db, gb, false);
		Long key_1 = issueAssetTransaction.getAssetKey(db);
		assertEquals((long)key+1, (long)key_1);
		assertEquals(BigDecimal.ZERO.setScale(8), maker.getBalanceUSE(key_1, db));

		BigDecimal amo = BigDecimal.TEN.setScale(8);
		//CREATE INVALID ASSET TRANSFER WRONG REFERENCE
		messageTransaction = new R_Send(maker, FEE_POWER, recipient, key_1,
				amo,
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		assertEquals(Transaction.VALIDATE_OK, messageTransaction.isValid(db, releaserReference));	
		messageTransaction.process(db, gb, false);
						
		//CHECK IF UNLIMITED ASSET TRANSFERED with no balance
		assertEquals(BigDecimal.ZERO.subtract(amo), maker.getBalanceUSE(key_1, db));

		// TRY INVALID SEND FRON NOT CREATOR
		
		messageTransaction = new R_Send(maker_1, FEE_POWER, recipient, key_1,
				amo,
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker_1.getLastReference(db));
		assertEquals(Transaction.NO_BALANCE, messageTransaction.isValid(db, releaserReference));	
						
		//CHECK IF UNLIMITED ASSET TRANSFERED with no balance
		assertEquals(BigDecimal.ZERO.setScale(8), maker_1.getBalanceUSE(key_1, db));

}
	
	@Test
	public void parseMessageTransaction() 
	{

		init();
		
		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		long timestamp = NTP.getTime();
					
		//CREATE VALID ASSET TRANSFER
		R_Send r_Send = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp, maker.getLastReference(db));
		r_Send.sign(maker, false);

		//CONVERT TO BYTES
		byte[] rawAssetTransfer = r_Send.toBytes(true, releaserReference);
		
		//CHECK DATALENGTH
		assertEquals(rawAssetTransfer.length, r_Send.getDataLength(false));
		
		try 
		{	
			//PARSE FROM BYTES
			R_Send parsedAssetTransfer = (R_Send) TransactionFactory.getInstance().parse(rawAssetTransfer, releaserReference);
			
			//CHECK INSTANCE
			assertEquals(true, parsedAssetTransfer instanceof R_Send);
			
			//CHECK TYPEBYTES
			assertEquals(true, Arrays.equals(r_Send.getTypeBytes(), parsedAssetTransfer.getTypeBytes()));				

			//CHECK TIMESTAMP
			assertEquals(r_Send.getTimestamp(), parsedAssetTransfer.getTimestamp());				

			//CHECK REFERENCE
			assertEquals(r_Send.getReference(), parsedAssetTransfer.getReference());	

			//CHECK CREATOR
			assertEquals(r_Send.getCreator().getAddress(), parsedAssetTransfer.getCreator().getAddress());				

			//CHECK FEE POWER
			assertEquals(r_Send.getFee(), parsedAssetTransfer.getFee());	

			//CHECK SIGNATURE
			assertEquals(true, Arrays.equals(r_Send.getSignature(), parsedAssetTransfer.getSignature()));
			
			//CHECK KEY
			assertEquals(r_Send.getKey(), parsedAssetTransfer.getKey());	
			
			//CHECK AMOUNT
			assertEquals(r_Send.getAmount(maker), parsedAssetTransfer.getAmount(maker));	
			
			//CHECK AMOUNT RECIPIENT
			assertEquals(r_Send.getAmount(recipient), parsedAssetTransfer.getAmount(recipient));	
						
		}
		catch (Exception e) 
		{
			fail("Exception while parsing transaction." + e);
		}
		
		//PARSE TRANSACTION FROM WRONG BYTES
		rawAssetTransfer = new byte[r_Send.getDataLength(false)];
		
		try 
		{	
			//PARSE FROM BYTES
			TransactionFactory.getInstance().parse(rawAssetTransfer, releaserReference);
			
			//FAIL
			fail("this should throw an exception");
		}
		catch (Exception e) 
		{
			//EXCEPTION IS THROWN OK
		}	
	}
	
	@Test
	public void processMessageTransaction()
	{

		init();
		
		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		long timestamp = NTP.getTime();
			
		//CREATE ASSET TRANSFER
		maker.changeBalance(db, false, key, BigDecimal.valueOf(200).setScale(8));
		Transaction messageTransaction = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp, maker.getLastReference(db));
		messageTransaction.sign(maker, false);
		messageTransaction.process(db, gb, false);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(100).setScale(8), maker.getBalanceUSE(key, db));
				
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getBalanceUSE(FEE_KEY, db));
		assertEquals(BigDecimal.valueOf(100).setScale(8), recipient.getBalanceUSE(key, db));
		
		//CHECK REFERENCE SENDER
		assertEquals(messageTransaction.getTimestamp(), maker.getLastReference(db));
		
		//CHECK REFERENCE RECIPIENT
		assertNotEquals(messageTransaction.getTimestamp(), recipient.getLastReference(db));
	}
	
	@Test
	public void orphanMessageTransaction()
	{
		
		init();
		
		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		long timestamp = NTP.getTime();
			
		//CREATE ASSET TRANSFER
		long key = 2l;
		maker.changeBalance(db, false, key, BigDecimal.valueOf(100).setScale(8));
		Transaction messageTransaction = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp, maker.getLastReference(db));
		messageTransaction.sign(maker, false);
		messageTransaction.process(db, gb, false);
		messageTransaction.orphan(db, false);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(100).setScale(8), maker.getBalanceUSE(key, db));
				
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getBalanceUSE(FEE_KEY, db));
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getBalanceUSE(key, db));
		
		//CHECK REFERENCE SENDER
		assertEquals(messageTransaction.getReference(), maker.getLastReference(db));
		
		//CHECK REFERENCE RECIPIENT
		assertNotEquals(messageTransaction.getTimestamp(), recipient.getLastReference(db));
	}

	/////////////////////////////////////////////
	////////////
	@Test
	public void validate_R_Send_Movable_Asset() 
	{	
		
		init();
						
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueMessageTransaction = new IssueAssetTransaction(maker, assetMovable, FEE_POWER, timestamp++, maker.getLastReference(db));
		assertEquals(Transaction.VALIDATE_OK, issueMessageTransaction.isValid(db, releaserReference));

		issueMessageTransaction.sign(maker, false);
		issueMessageTransaction.process(db, gb, false);
		long key = assetMovable.getKey(db);
		
		//assertEquals(asset.getQuantity(), maker.getConfirmedBalance(FEE_KEY, db));
		assertEquals(new BigDecimal(assetMovable.getQuantity()).setScale(8), maker.getBalanceUSE(key, db));
		
		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		
		//CREATE INVALID ASSET TRANSFER NEGATIVE AMOUNT
		rsend = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(-100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.NO_HOLD_BALANCE, rsend.isValid(db, releaserReference));	

		//CREATE VALID ASSET TRANSFER
		rsend = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));

		//CHECK IF ASSET TRANSFER IS VALID
		assertEquals(Transaction.VALIDATE_OK, rsend.isValid(db, releaserReference));
		
		rsend.sign(maker, false);
		rsend.process(db, gb, false);
		
		//NOW IT WILL BE vaLID
		rsend = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(-100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.VALIDATE_OK, rsend.isValid(db, releaserReference));	
		
		timestamp ++;
		
		//CREATE VALID ASSET TRANSFER
		//maker.setConfirmedBalance(key, BigDecimal.valueOf(100).setScale(8), db);
		rsend = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));

		//CHECK IF ASSET TRANSFER IS VALID
		assertEquals(Transaction.VALIDATE_OK, rsend.isValid(db, releaserReference));			
				
		//CREATE INVALID ASSET TRANSFER NEGATIVE AMOUNT
		rsend = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(-100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.NO_HOLD_BALANCE, rsend.isValid(db, releaserReference));	

		
		
		//CREATE INVALID ASSET TRANSFER NOT ENOUGH ASSET BALANCE
		rsend = new R_Send(maker, FEE_POWER, recipient, 99, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.ITEM_ASSET_NOT_EXIST, rsend.isValid(db, releaserReference));	

		//CREATE INVALID ASSET TRANSFER NOT ENOUGH ASSET BALANCE
		rsend = new R_Send(maker, FEE_POWER, recipient, key - 1, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.NO_BALANCE, rsend.isValid(db, releaserReference));	

		//CREATE INVALID ASSET TRANSFER WRONG REFERENCE
		rsend = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, -123L);
						
		//CHECK IF ASSET TRANSFER IS INVALID
		assertEquals(Transaction.INVALID_REFERENCE, rsend.isValid(db, releaserReference));	

		// NOT DIVISIBLE
		asset = new AssetVenture(maker, "not divisible", icon, image, "asdasda", false, 0l, (byte) 0, false);
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, asset, FEE_POWER, timestamp++, maker.getLastReference(db));
		assertEquals(Transaction.VALIDATE_OK, issueAssetTransaction.isValid(db, releaserReference));	
		issueAssetTransaction.sign(maker, false);
		issueAssetTransaction.process(db, gb, false);
		Long key_1 = issueAssetTransaction.getAssetKey(db);
		assertEquals((long)key+1, (long)key_1);
		assertEquals(BigDecimal.ZERO.setScale(8), maker.getBalanceUSE(key_1, db));

		BigDecimal amo = BigDecimal.TEN.setScale(8);
		//CREATE INVALID ASSET TRANSFER WRONG REFERENCE
		rsend = new R_Send(maker, FEE_POWER, recipient, key_1,
				amo,
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		assertEquals(Transaction.VALIDATE_OK, rsend.isValid(db, releaserReference));	
		rsend.process(db, gb, false);
						
		//CHECK IF UNLIMITED ASSET TRANSFERED with no balance
		assertEquals(BigDecimal.ZERO.subtract(amo), maker.getBalanceUSE(key_1, db));

		// TRY INVALID SEND FRON NOT CREATOR
		
		rsend = new R_Send(maker_1, FEE_POWER, recipient, key_1,
				amo,
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker_1.getLastReference(db));
		assertEquals(Transaction.NO_BALANCE, rsend.isValid(db, releaserReference));	
						
		//CHECK IF UNLIMITED ASSET TRANSFERED with no balance
		assertEquals(BigDecimal.ZERO.setScale(8), maker_1.getBalanceUSE(key_1, db));

	}

	@Test
	public void process_Movable_Asset()
	{

		init();
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(maker, assetMovable, FEE_POWER, timestamp++, maker.getLastReference(db));
		issueAssetTransaction.sign(maker, false);
		issueAssetTransaction.process(db, gb, false);

		key = assetMovable.getKey(db);
		
		//CREATE SIGNATURE
		Account recipient = new Account("7MFPdpbaxKtLMWq7qvXU6vqTWbjJYmxsLW");
		long timestamp = NTP.getTime();

		// SET BALANCES
		db.getAssetBalanceMap().set(maker.getAddress(), key, new Tuple3<BigDecimal, BigDecimal, BigDecimal>(
				BigDecimal.valueOf(20).setScale(8),
				BigDecimal.valueOf(10).setScale(8),
				BigDecimal.valueOf(0).setScale(8))
				);
		
		//CREATE ASSET TRANSFER
		rsend = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(100).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		assertEquals(rsend.isValid(db, releaserReference), Transaction.NO_BALANCE);

		rsend = new R_Send(maker, FEE_POWER, recipient, key, BigDecimal.valueOf(25).setScale(8),
				"headdd", "wqeszcssd234".getBytes(), new byte[]{1}, new byte[]{1},
				timestamp++, maker.getLastReference(db));
		assertEquals(rsend.isValid(db, releaserReference), Transaction.VALIDATE_OK);

		
		rsend.sign(maker, false);
		rsend.process(db, gb, false);
		
		balance3 = maker.getBalance(db, key);
		
		//CHECK BALANCE SENDER
		
		assertEquals(BigDecimal.valueOf(100).setScale(8), balance3.c);
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(100).setScale(8), maker.getBalanceUSE(key, db));
				
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getBalanceUSE(FEE_KEY, db));
		assertEquals(BigDecimal.valueOf(100).setScale(8), recipient.getBalanceUSE(key, db));
		
		//CHECK REFERENCE SENDER
		assertEquals(rsend.getTimestamp(), maker.getLastReference(db));
		
		//CHECK REFERENCE RECIPIENT
		assertNotEquals(rsend.getTimestamp(), recipient.getLastReference(db));
	
		//////////////////////////////////////////////////
		/// ORPHAN
		/////////////////////////////////////////////////
		rsend.orphan(db, false);
		
		//CHECK BALANCE SENDER
		assertEquals(BigDecimal.valueOf(100).setScale(8), maker.getBalanceUSE(key, db));
				
		//CHECK BALANCE RECIPIENT
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getBalanceUSE(FEE_KEY, db));
		assertEquals(BigDecimal.ZERO.setScale(8), recipient.getBalanceUSE(key, db));
		
		//CHECK REFERENCE SENDER
		assertEquals(rsend.getReference(), maker.getLastReference(db));
		
		//CHECK REFERENCE RECIPIENT
		assertNotEquals(rsend.getTimestamp(), recipient.getLastReference(db));
	}

	
}
