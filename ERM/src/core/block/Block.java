package core.block;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;

import ntp.NTP;
import settings.Settings;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;

import utils.Converter;
import at.AT_API_Platform_Impl;
import at.AT_Block;
import at.AT_Constants;
import at.AT_Controller;
import at.AT_Exception;
import at.AT_Transaction;
import controller.Controller;
import core.BlockChain;
import core.BlockGenerator;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.account.PublicKeyAccount;
import core.crypto.Base58;
import core.crypto.Crypto;
import core.transaction.DeployATTransaction;
import core.transaction.Transaction;
import core.transaction.TransactionFactory;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import database.DBSet;
import lang.Lang;


public class Block {

	public static final int GENERATING_MIN_BLOCK_TIME = GenesisBlock.GENERATING_MIN_BLOCK_TIME * 1000;
	
	public static final int GENESIS_WIN_VALUE = 1000;
	
	public static final int VERSION_LENGTH = 4;
	//public static final int TIMESTAMP_LENGTH = 8;
	public static final int GENERATING_BALANCE_LENGTH = 4;
	public static final int CREATOR_LENGTH = Crypto.HASH_LENGTH;
	public static final int SIGNATURE_LENGTH = Crypto.SIGNATURE_LENGTH;
	public static final int REFERENCE_LENGTH = SIGNATURE_LENGTH;
	public static final int TRANSACTIONS_HASH_LENGTH = Crypto.HASH_LENGTH;
	private static final int TRANSACTIONS_COUNT_LENGTH = 4;
	private static final int TRANSACTION_SIZE_LENGTH = 4;
	public static final int AT_BYTES_LENGTH = 4;
	private static final int BASE_LENGTH = VERSION_LENGTH + REFERENCE_LENGTH + CREATOR_LENGTH + GENERATING_BALANCE_LENGTH + TRANSACTIONS_HASH_LENGTH + SIGNATURE_LENGTH + TRANSACTIONS_COUNT_LENGTH;
	//private static final int AT_FEES_LENGTH = 8;
	//private static final int AT_LENGTH = AT_FEES_LENGTH + AT_BYTES_LENGTH;
	private static final int AT_LENGTH = 0 + AT_BYTES_LENGTH;
	public static final int MAX_TRANSACTION_BYTES = GenesisBlock.MAX_BLOCK_BYTES - BASE_LENGTH;

	protected int version;
	protected byte[] reference;
	int height_process;
	//protected long timestamp;
	protected int generatingBalance; // only for DB MAP
	protected PublicKeyAccount creator;
	protected byte[] signature;

	private List<Transaction> transactions;	
	private int transactionCount;
	private byte[] rawTransactions;

	protected byte[] transactionsHash;

	protected byte[] atBytes;
	//protected Long atFees;

	static Logger LOGGER = Logger.getLogger(Block.class.getName());

	// VERSION 2 AND 3 BLOCKS, WITH AT AND MESSAGE
	public Block(int version, byte[] reference, PublicKeyAccount creator, int generatingBalance, byte[] transactionsHash, byte[] atBytes)
	{
		this.version = version;
		this.reference = reference;
		//this.timestamp = timestamp;
		this.creator = creator;
		this.generatingBalance = generatingBalance;

		this.transactionsHash = transactionsHash;

		this.transactionCount = 0;
		this.atBytes = atBytes;

	}

	// VERSION 2 AND 3 BLOCKS, WITH AT AND MESSAGE
	public Block(int version, byte[] reference, PublicKeyAccount creator, int generatingBalance, byte[] signature, byte[] transactionsHash, byte[] atBytes)
	{
		this(version, reference, creator, generatingBalance, transactionsHash, atBytes);
		this.signature = signature;
	}

	
	//GETTERS/SETTERS

	public int getVersion()
	{
		return version;
	}

	public byte[] getSignature()
	{
		return this.signature;
	}

	public int getHeight(DBSet db)
	{

		if (this instanceof GenesisBlock
				|| Arrays.equals(this.signature,
						Controller.getInstance().getBlockChain().getGenesisBlock().getSignature()))
			return 1;
		
		int height = db.getHeightMap().get(this.signature).a;		
		return height;

	}
	
	// TODO - on orphan = -1 for parent on resolve new chain
	public int getParentHeight(DBSet db)
	{

		if (this instanceof GenesisBlock
				|| Arrays.equals(this.signature,
						Controller.getInstance().getBlockChain().getGenesisBlock().getSignature()))
			return 0;
		
		int height = db.getHeightMap().get(this.reference).a;
		return height;

	}

	public int getHeightByParent(DBSet db)
	{
		
		int height = getParentHeight(db) + 1;
		return height;

	}

	public long getTimestamp(DBSet db)
	{
		int height = this.getParentHeight(db) + 1;
		
		BlockChain blockChain = Controller.getInstance().getBlockChain();

		return blockChain.getTimestamp(height);
	}

	// balance on creator account when making this block
	// TODO isValid
	public int getGeneratingBalance()
	{
		return this.generatingBalance;
	}
	public void setGeneratingBalance(DBSet dbSet)
	{
		this.generatingBalance = this.calcGeneratingBalance(dbSet);
	}
	
	// IT IS RIGHTS ONLY WHEN BLOCK is MAKING
	// MABE used only in isValid and in Block Generator
	public static int calcGeneratingBalance(DBSet dbSet, Account creator, int height)
	{
		
		int incomed_amount = 0;
		int amount;
		
		int previousForgingHeight = getPreviousForgingHeightForCalcWin(dbSet, creator, height);
		if (previousForgingHeight == -1)
			return 0;
				
		if (previousForgingHeight <= height) {
			
			List<Transaction> txs = dbSet.getTransactionFinalMap().findTransactions(null, null, creator.getAddress(),
					previousForgingHeight, height,
					0, 0, false, 0, 0);
			
			for(Transaction transaction: txs)
			{
				
				if ( transaction.getAbsKey() == Transaction.RIGHTS_KEY) {
					amount = (int)transaction.getAmount().longValue();
					incomed_amount += amount;
				}
			}
		}
		
		// OWN + RENT balance - in USE
		return (int)creator.getBalanceUSE(Transaction.RIGHTS_KEY, dbSet).longValue() - incomed_amount;
	}
	
	public int calcGeneratingBalance(DBSet dbSet)
	{
		 return calcGeneratingBalance(dbSet, this.creator, this.getHeightByParent(dbSet));
	}

	public byte[] getReference()
	{
		return this.reference;
	}

	public PublicKeyAccount getCreator()
	{
		return this.creator;
	}

	public BigDecimal getTotalFee()
	{
		BigDecimal fee = BigDecimal.ZERO.setScale(8);

		for(Transaction transaction: this.getTransactions())
		{
			fee = fee.add(transaction.getFee());
		}

		// TODO calculate AT FEE
		// fee = fee.add(BigDecimal.valueOf(this.atFees, 8));

		return fee;
	}

	/*
	public BigDecimal getATfee()
	{
		return BigDecimal.valueOf(this.atFees, 8);
	}
	*/

	public void setTransactionData(int transactionCount, byte[] rawTransactions)
	{
		this.transactionCount = transactionCount;
		this.rawTransactions = rawTransactions;
	}

	public int getTransactionCount() 
	{	
		return this.transactionCount;		
	}

	public synchronized List<Transaction> getTransactions() 
	{
		if(this.transactions == null)
		{
			//LOAD TRANSACTIONS
			this.transactions = new ArrayList<Transaction>();

			try
			{
				int position = 0;
				for(int i=0; i<transactionCount; i++)
				{
					//GET TRANSACTION SIZE
					byte[] transactionLengthBytes = Arrays.copyOfRange(this.rawTransactions, position, position + TRANSACTION_SIZE_LENGTH);
					int transactionLength = Ints.fromByteArray(transactionLengthBytes);
					position += TRANSACTION_SIZE_LENGTH;
					
					//PARSE TRANSACTION
					byte[] transactionBytes = Arrays.copyOfRange(this.rawTransactions, position, position + transactionLength);
					Transaction transaction = TransactionFactory.getInstance().parse(transactionBytes, null);

					//ADD TO TRANSACTIONS
					this.transactions.add(transaction);

					//ADD TO POSITION
					position += transactionLength;
				}
			}
			catch(Exception e)
			{
				//FAILED TO LOAD TRANSACTIONS
			}
		}

		return this.transactions;
	}

	/*
	public void addTransaction(Transaction transaction)
	{
		this.getTransactions().add(transaction);

		this.transactionCount++;
	}
	*/
	public void setTransactions(List<Transaction> transactions)
	{
		if (transactions == null)
			transactions = new ArrayList<Transaction>();
		
		this.transactions = transactions;
		this.transactionCount = transactions.size();
		this.transactionsHash = makeTransactionsHash(transactions);
	}
	public void setATBytes(byte[] atBytes)
	{
		this.atBytes = atBytes;
	}

	public int getTransactionIndex(byte[] signature)
	{

		int i = 0;
		
		for(Transaction transaction: this.getTransactions())
		{
			if(Arrays.equals(transaction.getSignature(), signature))
			{
				return i;
			}
			i++;
		}

		return -1;
	}

	public Transaction getTransaction(byte[] signature)
	{

		for(Transaction transaction: this.getTransactions())
		{
			if(Arrays.equals(transaction.getSignature(), signature))
			{
				return transaction;
			}
		}

		return null;
	}

	public Transaction getTransaction(int index)
	{
		if (index < this.transactions.size())
			return getTransactions().get(index);
		else return null;
	}
	
	public byte[] getBlockATs()
	{
		return this.atBytes;
	}

	public Block getParent(DBSet db)
	{
		return db.getBlockMap().get(this.reference);
	}

	public Block getChild(DBSet db)
	{
		return db.getChildMap().get(this);
	}

	/*
	public void setTransactionsHash(byte[] transactionsHash) 
	{
		this.transactionsHash = transactionsHash;
	}
	*/
	
	public static byte[] makeTransactionsHash(List<Transaction> transactions) 
	{
				
		if (transactions == null || transactions.size() == 0) {
			return new byte[TRANSACTIONS_HASH_LENGTH];
		}
		
		byte[] data = new byte[0];
		
		//MAKE TRANSACTIONS HASH
		for(Transaction transaction: transactions)
		{
			data = Bytes.concat(data, transaction.getSignature());
		}
		
		return Crypto.getInstance().digest(data);

	}
	public void makeTransactionsHash() 
	{
		this.transactionsHash = makeTransactionsHash(this.transactions);
	}

	//PARSE/CONVERT

	public static Block parse(byte[] data) throws Exception
	{
		//CHECK IF WE HAVE MINIMUM BLOCK LENGTH
		if(data.length < BASE_LENGTH)
		{
			throw new Exception("Data is less then minimum block length");
		}

		int position = 0;

		//READ VERSION
		byte[] versionBytes = Arrays.copyOfRange(data, position, position + VERSION_LENGTH);
		int version = Ints.fromByteArray(versionBytes);
		position += VERSION_LENGTH;

		/*
		//READ TIMESTAMP
		byte[] timestampBytes = Arrays.copyOfRange(data, position, position + TIMESTAMP_LENGTH);
		long timestamp = Longs.fromByteArray(timestampBytes);
		position += TIMESTAMP_LENGTH;
		*/		

		//READ REFERENCE
		byte[] reference = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
		position += REFERENCE_LENGTH;

		//READ GENERATOR
		byte[] generatorBytes = Arrays.copyOfRange(data, position, position + CREATOR_LENGTH);
		PublicKeyAccount generator = new PublicKeyAccount(generatorBytes);
		position += CREATOR_LENGTH;

		//READ GENERATING BALANCE
		byte[] generatingBalanceBytes = Arrays.copyOfRange(data, position, position + GENERATING_BALANCE_LENGTH);
		int generatingBalance = Ints.fromByteArray(generatingBalanceBytes);
		position += GENERATING_BALANCE_LENGTH;

		//READ TRANSACTION SIGNATURE
		byte[] transactionsHash =  Arrays.copyOfRange(data, position, position + TRANSACTIONS_HASH_LENGTH);
		position += TRANSACTIONS_HASH_LENGTH;

		//READ GENERATOR SIGNATURE
		byte[] signature =  Arrays.copyOfRange(data, position, position + SIGNATURE_LENGTH);
		position += SIGNATURE_LENGTH;
 
		//CREATE BLOCK
		Block block;
		if(version > 0)
		{
			//ADD ATs BYTES
			byte[] atBytesCountBytes = Arrays.copyOfRange(data, position, position + AT_BYTES_LENGTH);
			int atBytesCount = Ints.fromByteArray(atBytesCountBytes);
			position += AT_BYTES_LENGTH;
	
			byte[] atBytes = Arrays.copyOfRange( data , position, position + atBytesCount);
			position += atBytesCount;
	
			//byte[] atFees = Arrays.copyOfRange( data , position , position + 8 );
			//position += 8;
	
			//long atFeesL = Longs.fromByteArray(atFees);

			block = new Block(version, reference, generator, generatingBalance, signature, transactionsHash, atBytes); //, atFeesL);
		}
		else
		{
			// GENESIS BLOCK version = 0
			block = new Block(version, reference, generator, generatingBalance, signature, transactionsHash, new byte[0]);
		}

		//READ TRANSACTIONS COUNT
		byte[] transactionCountBytes = Arrays.copyOfRange(data, position, position + TRANSACTIONS_COUNT_LENGTH);
		int transactionCount = Ints.fromByteArray(transactionCountBytes);
		position += TRANSACTIONS_COUNT_LENGTH;

		//SET TRANSACTIONDATA
		byte[] rawTransactions = Arrays.copyOfRange(data, position, data.length);
		block.setTransactionData(transactionCount, rawTransactions);

		//SET TRANSACTIONS SIGNATURE
		// transaction only in raw here - block.makeTransactionsHash();

		return block;
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson()
	{
		JSONObject block = new JSONObject();

		block.put("version", this.version);
		block.put("reference", Base58.encode(this.reference));
		block.put("timestamp", this.getTimestamp(DBSet.getInstance()));
		block.put("generatingBalance", this.generatingBalance);
		//block.put("winValue", this.calcWinValue(DBSet.getInstance()));
		block.put("winValueTargeted", this.calcWinValueTargeted(DBSet.getInstance()));
		block.put("creator", this.creator.getAddress());
		block.put("fee", this.getTotalFee().toPlainString());
		block.put("transactionsHash", Base58.encode(this.transactionsHash));
		block.put("signature", Base58.encode(this.signature));
		block.put("signature",  Base58.encode(this.getSignature()));
		block.put("height", this.getHeight(DBSet.getInstance()));

		//CREATE TRANSACTIONS
		JSONArray transactionsArray = new JSONArray();

		for(Transaction transaction: this.getTransactions())
		{
			transactionsArray.add(transaction.toJson());
		}

		//ADD TRANSACTIONS TO BLOCK
		block.put("transactions", transactionsArray);

		//ADD AT BYTES
		if ( atBytes != null )
		{
			block.put("blockATs", Converter.toHex( atBytes ));
			//block.put("atFees", this.atFees);
		}

		//RETURN
		return block;
	}

	public byte[] toBytes(boolean withSign)
	{
		byte[] data = new byte[0];

		//WRITE VERSION
		byte[] versionBytes = Ints.toByteArray(this.version);
		versionBytes = Bytes.ensureCapacity(versionBytes, VERSION_LENGTH, 0);
		data = Bytes.concat(data, versionBytes);

		/*
		//WRITE TIMESTAMP
		byte[] timestampBytes = Longs.toByteArray(this.timestamp);
		timestampBytes = Bytes.ensureCapacity(timestampBytes, TIMESTAMP_LENGTH, 0);
		data = Bytes.concat(data, timestampBytes);
		*/

		//WRITE REFERENCE
		byte[] referenceBytes = Bytes.ensureCapacity(this.reference, REFERENCE_LENGTH, 0);
		data = Bytes.concat(data, referenceBytes);

		//WRITE GENERATOR
		byte[] generatorBytes = Bytes.ensureCapacity(this.creator.getPublicKey(), CREATOR_LENGTH, 0);
		data = Bytes.concat(data, generatorBytes);

		//WRITE GENERATING BALANCE
		byte[] generatingBalanceBytes = Ints.toByteArray(this.generatingBalance);
		generatingBalanceBytes = Bytes.ensureCapacity(generatingBalanceBytes, GENERATING_BALANCE_LENGTH, 0);
		data = Bytes.concat(data, generatingBalanceBytes);

		//WRITE TRANSACTIONS HASH
		data = Bytes.concat(data, this.transactionsHash);

		if (!withSign) {
			// make HEAD data for signature
			return data;
		}

		//WRITE GENERATOR SIGNATURE
		data = Bytes.concat(data, this.signature);

		//ADD ATs BYTES
		if(this.version > 0)
		{
			if (atBytes!=null)
			{
				byte[] atBytesCount = Ints.toByteArray( atBytes.length );
				data = Bytes.concat(data, atBytesCount);

				data = Bytes.concat(data, atBytes);

				//byte[] atByteFees = Longs.toByteArray(atFees);
				//data = Bytes.concat(data,atByteFees);
			}
			else
			{
				byte[] atBytesCount = Ints.toByteArray( 0 );
				data = Bytes.concat(data, atBytesCount);
				
				//byte[] atByteFees = Longs.toByteArray(0L);
				//data = Bytes.concat(data,atByteFees);
			}
		}

		//WRITE TRANSACTION COUNT
		byte[] transactionCountBytes = Ints.toByteArray(this.getTransactionCount());
		transactionCountBytes = Bytes.ensureCapacity(transactionCountBytes, TRANSACTIONS_COUNT_LENGTH, 0);
		data = Bytes.concat(data, transactionCountBytes);

		for(Transaction transaction: this.getTransactions())
		{
			//WRITE TRANSACTION LENGTH
			int transactionLength = transaction.getDataLength(false);
			byte[] transactionLengthBytes = Ints.toByteArray(transactionLength);
			transactionLengthBytes = Bytes.ensureCapacity(transactionLengthBytes, TRANSACTION_SIZE_LENGTH, 0);
			data = Bytes.concat(data, transactionLengthBytes);

			//WRITE TRANSACTION
			data = Bytes.concat(data, transaction.toBytes(true, null));
		}

		return data;
	}

	public void sign(PrivateKeyAccount account) 
	{	
		byte[] data = toBytes(false); // without SIGNATURE
		this.signature = Crypto.getInstance().sign(account, data);
	}

	public int getDataLength()
	{

		int length = BASE_LENGTH;

		if(this.version > 0)
		{
			length += AT_LENGTH;
			if (this.atBytes!=null)
			{
				length+=atBytes.length;
			}
		}

		for(Transaction transaction: this.getTransactions())
		{
			length += TRANSACTION_SIZE_LENGTH + transaction.getDataLength(false);
		}

		return length;
	}

	public byte[] getProofHash()
	{
		//newSig = sha256(prevSig || pubKey)
		byte[] data = Bytes.concat(this.reference, creator.getPublicKey());

		return Crypto.getInstance().digest(data);
	}

	/*
	public static int getPreviousForgingHeightForIncomes(DBSet dbSet, Account creator, int height) {
		
		// IF BLOCK in the MAP
		int previousForgingHeight = creator.getForgingData(dbSet, height);
		if (previousForgingHeight == -1) {
			// IF BLOCK not inserted in MAP
			previousForgingHeight = creator.getLastForgingData(dbSet);
			if (previousForgingHeight == -1) {
				// if it is first payment to this account
				return height;
			}
		}
		
		return previousForgingHeight;

	}
	*/

	public static int getPreviousForgingHeightForCalcWin(DBSet dbSet, Account creator, int height) {
		
		// IF BLOCK in the MAP
		int previousForgingHeight = creator.getForgingData(dbSet, height);
		if (previousForgingHeight == -1) {
			// IF BLOCK not inserted in MAP
			previousForgingHeight = creator.getLastForgingData(dbSet);			
		}
		
		if (previousForgingHeight > height) {
			return height;
		}
		
		return previousForgingHeight;

	}

	private static long getWinValueHeight2(int heightThis, int heightStart, int generatingBalance)
	{
		int len = heightThis - heightStart;
		if (len < 1)
			return 1;
			
		int times = GenesisBlock.GENESIS_GENERATING_BALANCE / generatingBalance;
		
		if (times < 100) {
			if (len > times * 7)
				return times * 7;
		} else if (times < 1000) {
			if (len > times * 5)
				return times * 5;
		} else {			
			if (len > times * 3) {
				return times * 3;			
			}
		}
		
		return len;
	}

	// may be calculated only for new BLOCK or last created BLOCK for this CREATOR
	// because: creator.getLastForgingData(dbSet);
	public static long calcWinValue(DBSet dbSet, Account account, int height, int generatingBalance)
	{
		//int height = this.getParentHeight(dbSet) + 1;

		int previousForgingHeight = getPreviousForgingHeightForCalcWin(dbSet, account, height);
		if (previousForgingHeight == -1)
			return 0l;
		
		long winValueHeight2 = getWinValueHeight2(height, previousForgingHeight, generatingBalance);

		long win_value = generatingBalance * winValueHeight2;

		/*
		if (height < 40)
			win_value >>= 6;
		else if (height < 100)
			win_value >>= 7;
		else if (height < 1000)
			win_value >>= 8;
		else if (height < 3000)
			win_value >>= 9;
		else if (height < 100000)
			win_value >>= 10;
		else
			win_value >>= 11;
			*/
		win_value >>= 10;
		
		return win_value;

	}
	
	public long calcWinValue(DBSet dbSet)
	{
		if (this.version == 0) {
			// GENESIS
			return 1000;
		}

		int height = this.getHeightByParent(dbSet);
		
		if (this.creator == null) {
			LOGGER.error("block.creator == null in BLOCK:" + height);
			return 1000;
		}

		
		return calcWinValue(dbSet, this.creator, height, this.generatingBalance);
	}

	public long getTarget(DBSet dbSet)
	{
		
		BlockChain blockChain = Controller.getInstance().getBlockChain();
		
		long win_value = 0;
		Block parent = this.getParent(dbSet);
		int i = 0;
		
		while (parent != null && parent.getVersion() > 0 && i < blockChain.TARGET_COUNT)
		{
			i++;
			win_value += parent.calcWinValue(dbSet);
			
			parent = parent.getParent(dbSet);
		}
		
		if (i == 0) {
			return this.calcWinValue(dbSet);
		}
		
		return win_value / i;
		
	}

	public int calcWinValueTargeted2(long win_value, long target)
	{
		
		int koeff = 1024;
		int result = 0;
		while (koeff > 0 && result < 15000 && win_value > target<<1) {
			result += 1000; 
			koeff >>=1;
			target <<=1;
		}
		result += (int)(koeff * win_value / target);
		if (result > 15000)
			result = 15000;
		
		return result;
		
	}

	public int calcWinValueTargeted(DBSet dbSet)
	{
		
		if (this.version == 0) {
			// GENESIS - getBlockChain = null
			return 1000;
		}
		
		long win_value = this.calcWinValue(dbSet);
		long target = this.getTarget(dbSet);
		//return (int)(1000 * win_value / target);
		return calcWinValueTargeted2(win_value, target);
	}

	//VALIDATE

	public boolean isSignatureValid()
	{
		//VALIDATE BLOCK SIGNATURE
		byte[] data = this.toBytes(false);

		if(!Crypto.getInstance().verify(this.creator.getPublicKey(), this.signature, data))
		{
			LOGGER.error("Block signature not valid "
					+ this.toString());
			return false;
		}

		return true;
	}

	// canonical definition of block version release schedule
	public int getNextBlockVersion(DBSet db)
	{

		return 1;
		
		/*
		int height = getHeight(db);

		if(height < Transaction.getAT_BLOCK_HEIGHT_RELEASE())
		{
			return 1;
		}
		else if(getTimestamp() < Transaction.getPOWFIX_RELEASE())
		{
			return 2;
		}
		else
		{
			return 3;
		}
		*/
	}

	public boolean isValid(DBSet db)
	{
		
		int height = this.getHeightByParent(db);
		//CHECK IF PARENT EXISTS
		if(this.reference == null || this.getParent(db) == null)
		{
			LOGGER.error("*** Block[" + this.getHeightByParent(db) + "].reference invalid");
			return false;
		}

		/*
		 * OLD TIME
		//if (false) {
		if (!noTime) {
			//CHECK IF TIMESTAMP IS VALID -500 MS ERROR MARGIN TIME
			if(true & (this.timestamp - 500 > NTP.getTime()
					|| this.timestamp < this.getParent(db).timestamp))
			{
				LOGGER.error("*** Block[" + this.getHeightByParent(db) + "].timestamp invalid");
				return false;
			}
	
			//CHECK IF TIMESTAMP REST SAME AS PARENT TIMESTAMP REST
			if(this.timestamp % 1000 != this.getParent(db).timestamp % 1000)
			{
				LOGGER.error("*** Block[" + this.getHeightByParent(db) + "].timestamp % 1000 invalid");
				return false;
			}
		}
		 */
		
		// TODO - show it to USER
		if(this.getTimestamp(db) - 60000 > NTP.getTime()) {
			LOGGER.error("*** Block[" + this.getHeightByParent(db) + ":" + Base58.encode(this.signature) + "].timestamp invalid >NTP.getTime()"
					+ " this.getTimestamp(db):" + this.getTimestamp(db) + " > NTP.getTime():" + NTP.getTime());
			return false;			
		}
		
		/* not need
		if(this.getTimestamp(db) - this.getParent(db).getTimestamp(db) != GENERATING_MIN_BLOCK_TIME) {
				LOGGER.error("*** Block[" + this.getHeightByParent(db) + ":" + Base58.encode(this.signature) + "].timestamp PERIOD invalid != GENERATING_MIN_BLOCK_TIME");
				return false;			
			}
			*/

		/*
		//CHECK IF GENERATING BALANCE IS CORRECT
		if(this.generatingBalance != BlockGenerator.getNextBlockGeneratingBalance(db, this.getParent(db)))
		{
			LOGGER.error("*** Block[" + this.getHeightByParent(db) + "].generatingBalance invalid");
			return false;
		}
		*/

		//CHECK IF VERSION IS CORRECT
		if(this.version != this.getParent(db).getNextBlockVersion(db))
		{
			LOGGER.error("*** Block[" + this.getHeightByParent(db) + "].version invalid");
			return false;
		}
		if(this.version < 1 && (this.atBytes.length > 0)) // || this.atFees != 0))
		{
			LOGGER.error("*** Block[" + this.getHeightByParent(db) + "].version AT invalid");
			return false;
		}
		
		int generatingBalance = calcGeneratingBalance(db);
		if (this.generatingBalance != generatingBalance) {
			generatingBalance = calcGeneratingBalance(db);
			LOGGER.error("*** Block[" + this.getHeightByParent(db) + "].generatingBalance invalid this.generatingBalance: " + this.generatingBalance
					+ " != calcGeneratingBalance(db): " + calcGeneratingBalance(db));
			return false;
		}
			
		// TEST repeated win for CREATOR
		Block testBlock = this.getParent(db);
		for (int i=0; i < BlockChain.REPEAT_WIN && testBlock != null; i++) {
			if (testBlock.getCreator().equals(this.creator)) {
				LOGGER.error("*** Block[" + this.getHeightByParent(db) + "] REPEATED WIN invalid");
				return false;
			}
		}
		
		// TODO
		/*
		if (!BlockChain.isGoodWinForTarget(height, winned_value, target)) {
			return 0l;
		}
		*/


		/*
		//CREATE TARGET
		byte[] targetBytes = new byte[SIGNATURE_LENGTH];
		Arrays.fill(targetBytes, Byte.MAX_VALUE);
		BigInteger target = new BigInteger(1, targetBytes);

		//DIVIDE TARGET BY BASE TARGET
		BigInteger baseTarget = BigInteger.valueOf(BlockGenerator.getBaseTarget(this.generatingBalance));
		target = target.divide(baseTarget);

		//MULTIPLY TARGET BY USER BALANCE
		target = target.multiply(this.creator.getGeneratingBalance(db).toBigInteger());

		//MULTIPLE TARGET BY GUESSES
		long guesses = (this.timestamp - this.getParent(db).getTimestamp()) / 1000; // orid /1000
		//BigInteger lowerTarget = target.multiply(BigInteger.valueOf(guesses-1)); // orig -1
		BigInteger lowerTarget = target.multiply(BigInteger.valueOf(guesses-1));
		target = target.multiply(BigInteger.valueOf(guesses));

		//CONVERT PROOF HASH TO BIGINT
		BigInteger hashValue = new BigInteger(1, getProofHash());

		//CHECK IF HASH LOWER THEN TARGET (blockchain total hash - "chain length")
		if(hashValue.compareTo(target) >= 0)
		{
			LOGGER.error("*** Block[" + this.getHeightByParent(db)
					+ "].target is invalid!. " + "guesses: " + guesses
					+ "\nhash >= target:\n" + hashValue.toString() + "\n" + target.toString());
			return false;
		}

		//CHECK IF FIRST BLOCK OF USER	
		if(hashValue.compareTo(lowerTarget) < 0)
		{
			LOGGER.error("*** Block[" + this.getHeightByParent(db)
				+ "].lowerTarget invalid!. " + "guesses: " + guesses
				+ "\nhash < lower:\n" + hashValue.toString() + "\n" + lowerTarget.toString());
			return false;
		}
		*/

		if ( this.atBytes != null && this.atBytes.length > 0 )
		{
			try
			{

				AT_Block atBlock = AT_Controller.validateATs( this.getBlockATs() , db.getBlockMap().getLastBlock().getHeight(db)+1 , db);
				//this.atFees = atBlock.getTotalFees();
			}
			catch(NoSuchAlgorithmException | AT_Exception e)
			{
				LOGGER.error(e.getMessage(),e);
				return false;
			}
		}

		//CHECK TRANSACTIONS
		if (this.transactions == null || this.transactions.size() == 0) {
			// empty transactions
		} else {
			DBSet fork = db.fork();
			byte[] transactionsSignatures = new byte[0];
			
			long timestampEnd = this.getTimestamp(db);
			// because time filter used by parent block timestamp on core.BlockGenerator.run()
			long timestampBeg = this.getParent(fork).getTimestamp(fork);

			for(Transaction transaction: this.getTransactions())
			{
				//CHECK IF NOT GENESISTRANSACTION
				if(transaction.getCreator() == null)
					 // ALL GENESIS transaction
					return false;
				
				if(!transaction.isSignatureValid()) {
					// 
					LOGGER.error("*** Block[" + this.getHeightByParent(fork)
					+ "].Tx[" + this.getTransactionSeq(transaction.getSignature()) + " : "
					+ transaction.viewFullTypeName() + "]"
					+ "invalid code: " + transaction.isValid(fork, null));
					return false;
				}
	
	
				//CHECK IF VALID
				if ( transaction instanceof DeployATTransaction)
				{
					Integer min = 0;
					if ( db.getBlockMap().getParentList() != null )
					{
						min = AT_API_Platform_Impl.getForkHeight(db);
					}
	
					DeployATTransaction atTx = (DeployATTransaction)transaction;
					if ( atTx.isValid(fork, min) != Transaction.VALIDATE_OK )
					{
						LOGGER.error("*** Block[" + this.getHeightByParent(fork) + "].atTx invalid");
						return false;
					}
				}
				else if(transaction.isValid(fork, null) != Transaction.VALIDATE_OK)
				{
					LOGGER.error("*** Block[" + this.getHeightByParent(fork)
						+ "].Tx[" + this.getTransactionSeq(transaction.getSignature()) + " : "
						+ transaction.viewFullTypeName() + "]"
						+ "invalid code: " + transaction.isValid(fork, null));
					return false;
				}
	
				//CHECK TIMESTAMP AND DEADLINE
				long transactionTimestamp = transaction.getTimestamp();
				if( transactionTimestamp > timestampEnd
						|| transaction.getDeadline() <= timestampBeg)
				{
					LOGGER.error("*** Block[" + this.getHeightByParent(fork) + "].TX.timestamp invalid");
					return false;
				}
	
				
				try{
					//PROCESS TRANSACTION IN MEMORYDB TO MAKE SURE OTHER TRANSACTIONS VALIDATE PROPERLY
					transaction.process(fork, this, false);
					
				} catch (Exception e) {
                    LOGGER.error("*** Block[" + this.getHeightByParent(fork) + "].TX.process ERROR", e);
                    return false;                    
				}
				
				transactionsSignatures = Bytes.concat(transactionsSignatures, transaction.getSignature());
			}
			
			transactionsSignatures = Crypto.getInstance().digest(transactionsSignatures);
			if (!Arrays.equals(this.transactionsHash, transactionsSignatures)) {
				LOGGER.error("*** Block[" + this.getHeightByParent(fork) + "].digest(transactionsSignatures) invalid");
				return false;
			}
		}

		//BLOCK IS VALID
		return true;
	}

	//PROCESS/ORPHAN

	// TODO - make it trownable
	public void process(DBSet dbSet)
	{	
		//PROCESS TRANSACTIONS
		for(Transaction transaction: this.getTransactions())
		{
			//PROCESS
			transaction.process(dbSet, this, false);

			//SET PARENT
			dbSet.getTransactionRef_BlockRef_Map().set(transaction, this);

			//REMOVE FROM UNCONFIRMED DATABASE
			dbSet.getTransactionMap().delete(transaction);
		}

		//DELETE CONFIRMED TRANSACTIONS FROM UNCONFIRMED TRANSACTIONS LIST
		List<Transaction> unconfirmedTransactions = new ArrayList<Transaction>(dbSet.getTransactionMap().getValues());
		for(Transaction transaction: unconfirmedTransactions)
		{
			if(dbSet.getTransactionRef_BlockRef_Map().contains(transaction.getSignature()))
			{
				dbSet.getTransactionMap().delete(transaction);
			}
		}

		//PROCESS FEE
		BigDecimal blockFee = this.getTotalFee();
		if(blockFee.compareTo(BigDecimal.ZERO) == 1)
		{
			//UPDATE GENERATOR BALANCE WITH FEE
			this.creator.setBalance(Transaction.FEE_KEY, this.creator.getBalanceUSE(Transaction.FEE_KEY, dbSet).add(blockFee), dbSet);
		}
		

		//ADD TO DB
		dbSet.getBlockMap().set(this);
		
		this.height_process = dbSet.getHeightMap().getHeight(this.signature);

		/*
		if (!dbSet.isFork()) {
			int lastHeight = dbSet.getBlockMap().getLastBlock().getHeight(dbSet);
			LOGGER.error("*** core.block.Block.process(DBSet)[" + (this.getParentHeight(dbSet) + 1)
					+ "] SET new last Height: " + lastHeight
					+ " getHeightMap().getHeight: " + this.height_process);
		}
		*/

		BlockChain blockChain = Controller.getInstance().getBlockChain();
		if (blockChain != null) {
			Controller.getInstance().getBlockChain().setCheckPoint(this.height_process - BlockChain.MAX_SIGNATURES);
		}

		//PROCESS TRANSACTIONS
		int seq = 1;
		for(Transaction transaction: this.getTransactions())
		{
			dbSet.getTransactionFinalMap().add( height_process, seq, transaction);
			seq++;
		}

		if(height_process % Settings.BLOCK_MAX_SIGNATURES == 0) 
		{
			Controller.getInstance().blockchainSyncStatusUpdate(height_process);
		}
		
	}

	public void orphan(DBSet dbSet)
	{

		int i=0;
		if (Controller.getInstance().isAddressIsMine(this.getCreator().getAddress())) {
			i++;
		}
		int height = this.getHeight(dbSet);
		
		// TEST BUG
		int genBal = 0;
		if (height == 13311 || height == 13411 || height == 13477) {
			genBal = this.calcGeneratingBalance(dbSet);
		}
		
		//ORPHAN AT TRANSACTIONS
		LinkedHashMap< Tuple2<Integer, Integer> , AT_Transaction > atTxs = dbSet.getATTransactionMap().getATTransactions(height);

		Iterator<AT_Transaction> iter = atTxs.values().iterator();

		while ( iter.hasNext() )
		{
			AT_Transaction key = iter.next();
			Long amount  = key.getAmount();
			if (key.getRecipientId() != null && !Arrays.equals(key.getRecipientId(), new byte[ AT_Constants.AT_ID_SIZE ]) && !key.getRecipient().equalsIgnoreCase("1") )
			{
				Account recipient = new Account( key.getRecipient() );
				recipient.setBalance(Transaction.FEE_KEY,  recipient.getBalanceUSE(Transaction.FEE_KEY,  dbSet ).subtract( BigDecimal.valueOf( amount, 8 ) ) , dbSet );
				if ( recipient.getLastReference(dbSet) != null)
				{
					recipient.removeReference(dbSet);
				}
			}
			Account sender = new Account( key.getSender() );
			sender.setBalance(Transaction.FEE_KEY,  sender.getBalanceUSE(Transaction.FEE_KEY,  dbSet ).add( BigDecimal.valueOf( amount, 8 ) ) , dbSet );

		}

		//ORPHAN TRANSACTIONS
		this.orphanTransactions(this.getTransactions(), dbSet);

		//REMOVE FEE
		BigDecimal blockFee = this.getTotalFee();
		if(blockFee.compareTo(BigDecimal.ZERO) == 1)
		{
			//UPDATE GENERATOR BALANCE WITH FEE
			this.creator.setBalance(Transaction.FEE_KEY, this.creator.getBalanceUSE(Transaction.FEE_KEY, dbSet).subtract(blockFee), dbSet);
		}

		//DELETE AT TRANSACTIONS FROM DB
		dbSet.getATTransactionMap().delete(height);
		
		//DELETE TRANSACTIONS FROM FINAL MAP
		dbSet.getTransactionFinalMap().delete(height);

		int lastHeightThis = dbSet.getBlockMap().getLastBlock().getHeight(dbSet);

		//DELETE BLOCK FROM DB
		dbSet.getBlockMap().delete(this);
		
		if (height > 1) {
			int lastHeight = dbSet.getBlockMap().getLastBlock().getHeight(dbSet);
			LOGGER.error("*** core.block.Block.orphan(DBSet)[" + height + ":" + lastHeightThis
					+ "] DELETE -> new last Height: " + lastHeight
					+ (dbSet.isFork()?" in FORK!": ""));
		}

		
		this.height_process = -1;

		for(Transaction transaction: this.getTransactions())
		{
			//ADD ORPHANED TRANASCTIONS BACK TO DATABASE
			dbSet.getTransactionMap().add(transaction);

			//DELETE ORPHANED TRANASCTIONS FROM PARENT DATABASE
			dbSet.getTransactionRef_BlockRef_Map().delete(transaction.getSignature());
		}

		// TEST BUG
		if (height == 13311 || height == 13411 || height == 13477) {
			genBal = this.calcGeneratingBalance(dbSet);
		}

	}

	private void orphanTransactions(List<Transaction> transactions, DBSet db)
	{
		//ORPHAN ALL TRANSACTIONS IN DB BACK TO FRONT
		for(int i=transactions.size() -1; i>=0; i--)
		{
			Transaction transaction = transactions.get(i);
			transaction.orphan(db, false);
		}
	}

	public int getTransactionSeq(byte[] signature)
	{
		int seq = 1;
		for(Transaction transaction: this.getTransactions())
		{
			if(Arrays.equals(transaction.getSignature(), signature))
			{
				return seq;
			}
			seq ++;
		}

		return -1;
	}
	
	@Override 
	public boolean equals(Object otherObject)
	{
		if(otherObject instanceof Block)
		{
			Block otherBlock = (Block) otherObject;
			
			return Arrays.equals(this.getSignature(), otherBlock.getSignature());
		}
		
		return false;
	}
}
