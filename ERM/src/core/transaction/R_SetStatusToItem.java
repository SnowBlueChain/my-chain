package core.transaction;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.EnumSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.mapdb.Fun.Tuple3;
import org.mapdb.Fun.Tuple5;
import org.json.simple.JSONObject;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import controller.Controller;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.account.PublicKeyAccount;
import core.block.Block;
import core.crypto.Base58;
import core.crypto.Crypto;
import core.item.statuses.StatusCls;
import core.item.ItemCls;
import ntp.NTP;
import utils.DateTimeFormat;
import database.DBSet;
import database.DBMap;

public class R_SetStatusToItem extends Transaction {

	private static final byte TYPE_ID = (byte)Transaction.SET_STATUS_TO_ITEM_TRANSACTION;
	private static final String NAME_ID = "Set Status";
	private static final int DATE_LENGTH = Transaction.TIMESTAMP_LENGTH; // one year + 256 days max
	private static final int REF_LENGTH = Long.SIZE;
	private static final int ITEM_TYPE_LENGTH = 1;
	private static final BigDecimal MIN_ERM_BALANCE = BigDecimal.valueOf(1000).setScale(8);
	// need RIGHTS for non PERSON account
	private static final BigDecimal GENERAL_ERM_BALANCE = BigDecimal.valueOf(100000).setScale(8);

	protected Long key; // STATUS KEY
	protected int itemType; // ITEM TYPE (CAnnot read ITEMS on start DB - need reset ITEM after
	protected Long itemKey; // ITEM KEY
	protected long beg_date;
	protected long end_date = Long.MAX_VALUE;
	protected byte value_1; // first any value
	protected byte value_2; // second any value
	protected byte[] data; // addition data
	protected long ref_to_parent; // reference to parent record as int + int (block height + record sequence number)
	
	private static final int SELF_LENGTH = 2 * DATE_LENGTH + KEY_LENGTH + ITEM_TYPE_LENGTH + KEY_LENGTH;
	
	protected static final int BASE_LENGTH_AS_PACK = Transaction.BASE_LENGTH_AS_PACK + SELF_LENGTH;
	protected static final int BASE_LENGTH = Transaction.BASE_LENGTH + SELF_LENGTH;

	public R_SetStatusToItem(byte[] typeBytes, PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
			Long beg_date, Long end_date,
			int value_1, int value_2, byte[] data, long ref_to_parent,
			long timestamp, Long reference) {
		super(typeBytes, NAME_ID, creator, feePow, timestamp, reference);		

		this.key = key;
		this.itemType = itemType;
		this.itemKey = itemKey;
		if (beg_date == null || beg_date == 0) beg_date = Long.MIN_VALUE;
		this.beg_date = beg_date;		
		if (end_date == null || end_date == 0) end_date = Long.MAX_VALUE;
		this.end_date = end_date;
		this.value_1 = (byte)value_1;
		this.value_2 = (byte)value_2;
		this.data = data;
		this.ref_to_parent = ref_to_parent;
		
		// make parameters
		this.typeBytes[3] = (byte)((value_1 == 0?0:1)
					| (value_2 == 0?0:2)
					| (data == null?0:4)
					| (ref_to_parent == 0l?0:8)
					);
	}

	public R_SetStatusToItem(PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
			Long beg_date, Long end_date,
			int value_1, int value_2, byte[] data, long ref_to_parent,
			long timestamp, Long reference) {
		this(new byte[]{TYPE_ID, (byte)0, 0, 0}, creator, feePow, key, itemType, itemKey,
				beg_date, end_date,
				value_1, value_2, data, ref_to_parent,
				timestamp, reference);
	}
	// set default date
	public R_SetStatusToItem(PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
			long timestamp, Long reference) {
		this(new byte[]{TYPE_ID, (byte)0, 0, 0}, creator, feePow, key, itemType, itemKey,
				Long.MIN_VALUE, Long.MAX_VALUE, 0, 0, null, 0L, timestamp, reference);
	}
	public R_SetStatusToItem(byte[] typeBytes, PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
			Long beg_date, Long end_date,
			int value_1, int value_2, byte[] data, long ref_to_parent,
			long timestamp, Long reference, byte[] signature) {
		this(typeBytes, creator, feePow, key, itemType, itemKey,
				beg_date, end_date,
				value_1, value_2, data, ref_to_parent,
				timestamp, reference);
		this.signature = signature;
		this.calcFee();
	}
	// as pack
	public R_SetStatusToItem(byte[] typeBytes, PublicKeyAccount creator, long key, int itemType, long itemKey,
			Long beg_date, Long end_date,
			int value_1, int value_2, byte[] data, long ref_to_parent,
			byte[] signature) {
		this(typeBytes, creator, (byte)0, key, itemType, itemKey,
				beg_date, end_date,
				value_1, value_2, data, ref_to_parent,
				0l, null);
		this.signature = signature;
	}
	public R_SetStatusToItem(PublicKeyAccount creator, byte feePow, long key, int itemType, long itemKey,
			Long beg_date, Long end_date,
			int value_1, int value_2, byte[] data, long ref_to_parent,
			long timestamp, Long reference, byte[] signature) {
		this(new byte[]{TYPE_ID, (byte)0, 0, 0}, creator, feePow, key, itemType, itemKey,
				beg_date, end_date,
				value_1, value_2, data, ref_to_parent,
				timestamp, reference);
	}

	// as pack
	public R_SetStatusToItem(PublicKeyAccount creator, long key, int itemType, long itemKey,
			Long beg_date, Long end_date,
			int value_1, int value_2, byte[] data, long ref_to_parent,
			byte[] signature) {
		this(new byte[]{TYPE_ID, (byte)0, (byte)0, 0}, creator, (byte)0, key, itemType, itemKey,
				beg_date, end_date,
				value_1, value_2, data, ref_to_parent,
				0l, null);
	}
	
	//GETTERS/SETTERS

	//public static String getName() { return "Send"; }
	
	public long getKey()
	{
		return this.key;
	}

	public int getItemType()
	{
		return this.itemType;
	}
	public long getItemKey()
	{
		return this.itemKey;
	}

	public Long getBeginDate()
	{
		return this.beg_date;
	}

	public Long getEndDate()
	{
		return this.end_date;
	}
	public int getValue1()
	{
		return this.value_1;
	}
	public int getValue2()
	{
		return this.value_2;
	}
	public byte[] getData()
	{
		return this.data;
	}
	public long getRefParent()
	{
		return this.ref_to_parent;
	}

	
	@Override
	public String viewItemName() {
		ItemCls status = DBSet.getInstance().getItemStatusMap().get(this.key);
		return status==null?"null" : status.toString();
	}
	
	@Override
	public String viewAmount(String address) {
		return DateTimeFormat.timestamptoString(end_date);
	}
	
	@Override
	public String viewRecipient() {
		ItemCls item = ItemCls.getItem(DBSet.getInstance(), this.itemType, this.itemKey);
		return item==null?"null" : item.toString();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJson() 
	{
		//GET BASE
		JSONObject transaction = this.getJsonBase();

		//ADD CREATOR/SERVICE/DATA
		transaction.put("key", this.key);
		transaction.put("itemType", this.itemType);
		transaction.put("itemKey", this.itemKey);
		transaction.put("begin_date", this.beg_date);
		transaction.put("end_date", this.end_date);
		
		if (this.value_1 != 0)
			transaction.put("value1", this.value_1);
		
		if (this.value_2 != 0)
			transaction.put("value2", this.value_2);
		
		if (this.data != null)
			transaction.put("data", new String(this.data, Charset.forName("UTF-8")));
		
		if (this.ref_to_parent != 0l)
			transaction.put("ref_parent", this.ref_to_parent);
		
		return transaction;	
	}

	// releaserReference = null - not a pack
	// releaserReference = reference for releaser account - it is as pack
	public static Transaction Parse(byte[] data, Long releaserReference) throws Exception
	{
		boolean asPack = releaserReference != null;
		
		//CHECK IF WE MATCH BLOCK LENGTH
		if (data.length < BASE_LENGTH_AS_PACK
				| !asPack & data.length < BASE_LENGTH)
		{
			throw new Exception("Data does not match block length " + data.length);
		}
		
		// READ TYPE
		byte[] typeBytes = Arrays.copyOfRange(data, 0, TYPE_LENGTH);
		int position = TYPE_LENGTH;

		long timestamp = 0;
		if (!asPack) {
			//READ TIMESTAMP
			byte[] timestampBytes = Arrays.copyOfRange(data, position, position + TIMESTAMP_LENGTH);
			timestamp = Longs.fromByteArray(timestampBytes);	
			position += TIMESTAMP_LENGTH;
		}

		Long reference = null;
		if (!asPack) {
			//READ REFERENCE
			byte[] referenceBytes = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
			reference = Longs.fromByteArray(referenceBytes);	
			position += REFERENCE_LENGTH;
		} else {
			reference = releaserReference;
		}
		
		//READ CREATOR
		byte[] creatorBytes = Arrays.copyOfRange(data, position, position + CREATOR_LENGTH);
		PublicKeyAccount creator = new PublicKeyAccount(creatorBytes);
		position += CREATOR_LENGTH;
		
		byte feePow = 0;
		if (!asPack) {
			//READ FEE POWER
			byte[] feePowBytes = Arrays.copyOfRange(data, position, position + 1);
			feePow = feePowBytes[0];
			position += 1;
		}
		
		//READ SIGNATURE
		byte[] signature = Arrays.copyOfRange(data, position, position + SIGNATURE_LENGTH);
		position += SIGNATURE_LENGTH;

		//READ STATUS KEY
		byte[] keyBytes = Arrays.copyOfRange(data, position, position + KEY_LENGTH);
		long key = Longs.fromByteArray(keyBytes);	
		position += KEY_LENGTH;

		//READ ITEM
		// ITEM TYPE - ITEM_TYPE_LENGTH = 1
		Byte itemType = data[position];
		position ++;
		
		// ITEM KEY
		byte[] itemKeyBytes = Arrays.copyOfRange(data, position, position + KEY_LENGTH);
		long itemKey = Longs.fromByteArray(itemKeyBytes);	
		position += KEY_LENGTH;
		
		// READ BEGIN DATE
		byte[] beg_dateBytes = Arrays.copyOfRange(data, position, position + DATE_LENGTH);
		Long beg_date = Longs.fromByteArray(beg_dateBytes);	
		position += DATE_LENGTH;

		// READ END DATE
		byte[] end_dateBytes = Arrays.copyOfRange(data, position, position + DATE_LENGTH);
		Long end_date = Longs.fromByteArray(end_dateBytes);	
		position += DATE_LENGTH;

		byte value_1 = 0;
		if ( (typeBytes[3] & 1) > 0 ) {
			// READ VALUE 1
			byte[] value_1Bytes = Arrays.copyOfRange(data, position, position + 1);
			value_1 = value_1Bytes[0];
			position += 1;
		}

		byte value_2 = 0;
		if ( (typeBytes[3] & 2) > 0 ) {
			// READ VALUE 2
			byte[] value_2Bytes = Arrays.copyOfRange(data, position, position + 1);
			value_2 = value_2Bytes[0];
			position += 1;
		}

		byte[] additonalData = null;
		if ( (typeBytes[3] & 4) > 0 ) {
			//READ DATA SIZE
			byte[] dataSizeBytes = Arrays.copyOfRange(data, position, position + DATA_SIZE_LENGTH);
			int dataSize = Ints.fromByteArray(dataSizeBytes);	
			position += DATA_SIZE_LENGTH;
			
			//READ ADDITIONAL DATA
			additonalData = Arrays.copyOfRange(data, position, position + dataSize);
			position += dataSize;
		}

		long ref_to_parent = 0;
		if ( (typeBytes[3] & 8) > 0 ) {
			// READ REFFERENCE TO PARENT RECORD
			byte[] ref_to_recordBytes = Arrays.copyOfRange(data, position, position + REF_LENGTH);
			ref_to_parent = Longs.fromByteArray(ref_to_recordBytes);	
			position += REF_LENGTH;
		}

		if (!asPack) {
			return new R_SetStatusToItem(typeBytes, creator, feePow, key, itemType, itemKey,
					beg_date, end_date, value_1, value_2, additonalData, ref_to_parent,
					timestamp, reference, signature);
		} else {
			return new R_SetStatusToItem(typeBytes, creator, key, itemType, itemKey,
					beg_date, end_date, value_1, value_2, additonalData, ref_to_parent,
					signature);
		}

	}

	//@Override
	public byte[] toBytes(boolean withSign, Long releaserReference) {

		byte[] data = super.toBytes(withSign, releaserReference);

		//WRITE STATUS KEY
		byte[] keyBytes = Longs.toByteArray(this.key);
		keyBytes = Bytes.ensureCapacity(keyBytes, KEY_LENGTH, 0);
		data = Bytes.concat(data, keyBytes);
		
		//WRITE ITEM KEYS
		// TYPE
		byte[] itemTypeKeyBytes = new byte[1];
		itemTypeKeyBytes[0] = (byte)this.itemType;
		data = Bytes.concat(data, itemTypeKeyBytes);
		// KEY
		byte[] itemKeyBytes = Longs.toByteArray(this.itemKey);
		keyBytes = Bytes.ensureCapacity(itemKeyBytes, KEY_LENGTH, 0);
		data = Bytes.concat(data, keyBytes);
		
		//WRITE BEGIN DATE
		data = Bytes.concat(data, Longs.toByteArray(this.beg_date));

		//WRITE END DATE
		data = Bytes.concat(data, Longs.toByteArray(this.end_date));

		// WRITE VALUE 1
		if (this.value_1 != 0) {
			byte[] value_1Bytes = new byte[1];
			value_1Bytes[0] = this.value_1;
			data = Bytes.concat(data, value_1Bytes);
		}

		// WRITE VALUE 2
		if (this.value_2 != 0) {
			byte[] value_2Bytes = new byte[1];
			value_2Bytes[0] = this.value_2;
			data = Bytes.concat(data, value_2Bytes);
		}

		if (this.data != null) {
			//WRITE DATA SIZE
			byte[] dataSizeBytes = Ints.toByteArray(this.data.length);
			data = Bytes.concat(data, dataSizeBytes);
	
			//WRITE DATA
			data = Bytes.concat(data, this.data);
		}

		//WRITE REFFERENCE TO PARENT
		if (this.ref_to_parent != 0l) {
			byte[] ref_to_parentBytes = Longs.toByteArray(this.ref_to_parent);
			ref_to_parentBytes = Bytes.ensureCapacity(ref_to_parentBytes, REF_LENGTH, 0);
			data = Bytes.concat(data, ref_to_parentBytes);
		}

		return data;
	}

	@Override
	public int getDataLength(boolean asPack)
	{
		// not include note reference
		int len = asPack? BASE_LENGTH_AS_PACK : BASE_LENGTH;
		len += (this.value_1 == 0? 0: 1)
				+ (this.value_2 == 0? 0: 1)
				+ (this.data == null? 0: 1 + this.data.length)
				+ (this.ref_to_parent == 0? 0: REF_LENGTH);
		return len;
	}

	//VALIDATE

	public int isValid(DBSet db, Long releaserReference) {
		
		int result = super.isValid(db, releaserReference);
		if (result != Transaction.VALIDATE_OK) return result; 
	
		if (this.data != null ) {
			//CHECK DATA SIZE
			if(data.length > 4000)
			{
				return INVALID_DATA_LENGTH;
			}
		}

		if ( !db.getItemStatusMap().contains(this.key) )
		{
			return Transaction.ITEM_STATUS_NOT_EXIST;
		}

		if (this.itemType != ItemCls.PERSON_TYPE
				&& this.itemType != ItemCls.ASSET_TYPE
				&& this.itemType != ItemCls.UNION_TYPE)
			return ITEM_DOES_NOT_STATUSED;

		ItemCls item = db.getItem_Map(this.itemType).get(this.itemKey);
		if ( item == null )
		{
			return Transaction.ITEM_DOES_NOT_EXIST;
		}
		
		BigDecimal balERM = this.creator.getConfirmedBalance(RIGHTS_KEY, db);
		if ( balERM.compareTo(GENERAL_ERM_BALANCE)<0 )
			if ( this.creator.isPerson(db) )
			{
				if ( balERM.compareTo(MIN_ERM_BALANCE)<0 )
					return Transaction.NOT_ENOUGH_RIGHTS;
			} else {
				return Transaction.ACCOUNT_NOT_PERSONALIZED;
			}
		
		return Transaction.VALIDATE_OK;
	}

	//PROCESS/ORPHAN
	
	public void process(DBSet db, boolean asPack) {

		//UPDATE SENDER
		super.process(db, asPack);
		
		// pack additional data
		byte[] a_data = null; //this.value1;
		
		//Block block = db.getBlockMap().getLastBlock();
		//int blockIndex = block.getHeight(db);
		//int transactionIndex = block.getTransactionIndex(signature);

		Tuple5<Long, Long, byte[], Integer, Integer> itemP = new Tuple5<Long, Long, byte[], Integer, Integer>
				(
					beg_date, end_date,
					a_data,
					this.getBlockHeight(db), this.getSeqNo(db)
				);

		// SET ALIVE PERSON for DURATION
		// TODO set STATUSES by reference of it record - not by key!
		/// or add MAP by reference as signature - as IssueAsset - for orphans delete
		if (this.itemType == ItemCls.PERSON_TYPE)
			db.getPersonStatusMap().addItem(this.itemKey, this.key, itemP);
		else if (this.itemType == ItemCls.ASSET_TYPE)
			db.getAssetStatusMap().addItem(this.itemKey, this.key, itemP);
		else if (this.itemType == ItemCls.UNION_TYPE)
			db.getUnionStatusMap().addItem(this.itemKey, this.key, itemP);

	}

	public void orphan(DBSet db, boolean asPack) {

		//UPDATE SENDER
		super.orphan(db, asPack);
		
						
		// UNDO ALIVE PERSON for DURATION
		if (this.itemType == ItemCls.PERSON_TYPE)
			db.getPersonStatusMap().removeItem(this.itemKey, this.key);
		else if (this.itemType == ItemCls.ASSET_TYPE)
			db.getAssetStatusMap().removeItem(this.itemKey, this.key);
		else if (this.itemType == ItemCls.UNION_TYPE)
			db.getUnionStatusMap().removeItem(this.itemKey, this.key);

	}

	@Override
	public HashSet<Account> getInvolvedAccounts()
	{
		HashSet<Account> accounts = new HashSet<Account>();
		accounts.add(this.creator);
		return accounts;
	}
	
	@Override
	public HashSet<Account> getRecipientAccounts()
	{
		return new HashSet<>();
	}
	
	@Override
	public boolean isInvolved(Account account) 
	{
		String address = account.getAddress();
		
		if(address.equals(this.creator.getAddress()))
		{
			return true;
		}
		
		return false;
	}

}