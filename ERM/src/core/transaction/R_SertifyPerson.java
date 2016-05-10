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
import org.mapdb.Fun.Tuple4;
import org.json.simple.JSONObject;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import controller.Controller;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.account.PublicKeyAccount;
import core.crypto.Base58;
import core.crypto.Crypto;
import core.item.statuses.StatusCls;
import core.item.notes.NoteCls;
import core.item.notes.NoteFactory;
import core.item.persons.PersonCls;
import core.item.persons.PersonFactory;
import core.item.statuses.StatusCls;
import database.ItemAssetBalanceMap;
import ntp.NTP;
import database.DBSet;
import utils.Converter;

// this.end_date = 0 (ALIVE PERMANENT), = -1 (ENDED), = Integer - different
// typeBytes[1] - version =0 - not need sign by person;
// 		 =1 - need sign by person
// typeBytes[2] - size of personalized accounts
public class R_SertifyPerson extends Transaction {

	private static final byte TYPE_ID = (byte)Transaction.CERTIFY_PERSON_TRANSACTION;
	private static final String NAME_ID = "Sertify Person";
	private static final int USER_ADDRESS_LENGTH = Transaction.CREATOR_LENGTH;
	private static final int DATE_DAY_LENGTH = 4; // one year + 256 days max
	// need RIGHTS for PERSON account
	private static final BigDecimal MIN_ERM_BALANCE = BigDecimal.valueOf(1000).setScale(8);
	// need RIGHTS for non PERSON account
	private static final BigDecimal GENERAL_ERM_BALANCE = BigDecimal.valueOf(100000).setScale(8);

	// how many OIL gift
	public static final BigDecimal GIFTED_FEE_AMOUNT = BigDecimal.valueOf(0.00005).setScale(8);
	public static final int DEFAULT_DURATION = 3 * 356;

	protected Long key; // PERSON KEY
	protected Integer end_date; // in days
	protected List<PublicKeyAccount> sertifiedPublicKeys;
	protected List<byte[]> sertifiedSignatures;
	/*
	protected PublicKeyAccount personAddress1;
	protected PublicKeyAccount personAddress2;
	protected PublicKeyAccount personAddress3;
	protected byte[] userSignature1;
	protected byte[] userSignature2;
	protected byte[] userSignature3;
	*/
	// 3 * (USER_ADDRESS_LENGTH + SIGNATURE_LENGTH)
	private static final int SELF_LENGTH = DATE_DAY_LENGTH + KEY_LENGTH;
	
	protected static final int BASE_LENGTH_AS_PACK = Transaction.BASE_LENGTH_AS_PACK + SELF_LENGTH;
	protected static final int BASE_LENGTH = Transaction.BASE_LENGTH + SELF_LENGTH;

	public R_SertifyPerson(byte[] typeBytes, PublicKeyAccount creator, byte feePow, long key,
			List<PublicKeyAccount> sertifiedPublicKeys,
			int end_date, long timestamp, byte[] reference) {
		super(typeBytes, NAME_ID, creator, feePow, timestamp, reference);		

		this.key = key;
		this.sertifiedPublicKeys = sertifiedPublicKeys;
		this.end_date = end_date;		
	}

	public R_SertifyPerson(int version, PublicKeyAccount creator, byte feePow, long key,
			List<PublicKeyAccount> sertifiedPublicKeys,
			int end_date, long timestamp, byte[] reference) {
		this(new byte[]{TYPE_ID, (byte)version, (byte)sertifiedPublicKeys.size(), 0}, creator, feePow, key,
				sertifiedPublicKeys,
				end_date, timestamp, reference);
	}
	// set default date
	public R_SertifyPerson(int version, PublicKeyAccount creator, byte feePow, long key,
			List<PublicKeyAccount> sertifiedPublicKeys,
			long timestamp, byte[] reference) {
		this(new byte[]{TYPE_ID, (byte)version, (byte)sertifiedPublicKeys.size(), 0}, creator, feePow, key,
				sertifiedPublicKeys,
				0, timestamp, reference);
		
		this.end_date = DEFAULT_DURATION + (int)(NTP.getTime() / 86400);
	}
	public R_SertifyPerson(byte[] typeBytes, PublicKeyAccount creator, byte feePow, long key,
			List<PublicKeyAccount> sertifiedPublicKeys,
			int end_date, long timestamp, byte[] reference, byte[] signature,
			List<byte[]> sertifiedSignatures) {
		this(typeBytes, creator, feePow, key,
				sertifiedPublicKeys,
				end_date, timestamp, reference);
		this.signature = signature;
		this.sertifiedSignatures = sertifiedSignatures;
		this.calcFee();
	}
	// as pack
	public R_SertifyPerson(byte[] typeBytes, PublicKeyAccount creator, long key,
			List<PublicKeyAccount> sertifiedPublicKeys,
			int end_date, byte[] signature,
			List<byte[]> sertifiedSignatures) {
		this(typeBytes, creator, (byte)0, key,
				sertifiedPublicKeys,
				end_date, 0l, null);
		this.signature = signature;
		this.sertifiedSignatures = sertifiedSignatures;
	}
	public R_SertifyPerson(int version, PublicKeyAccount creator, byte feePow, long key,
			List<PublicKeyAccount> sertifiedPublicKeys,
			int end_date, long timestamp, byte[] reference, byte[] signature,
			byte[] userSignature1, byte[] userSignature2, byte[] userSignature3) {
		this(new byte[]{TYPE_ID, (byte)version, (byte)sertifiedPublicKeys.size(), 0}, creator, feePow, key,
				sertifiedPublicKeys,
				end_date, timestamp, reference);
	}

	// as pack
	public R_SertifyPerson(int version, PublicKeyAccount creator, long key,
			List<PublicKeyAccount> sertifiedPublicKeys,
			int end_date, byte[] signature,
			byte[] userSignature1, byte[] userSignature2, byte[] userSignature3) {
		this(new byte[]{TYPE_ID, (byte)version, (byte)sertifiedPublicKeys.size(), 0}, creator, (byte)0, key,
				sertifiedPublicKeys,
				end_date, 0l, null);
	}
	
	//GETTERS/SETTERS

	//public static String getName() { return "Send"; }
	
	public long getKey()
	{
		return this.key;
	}

	public List<PublicKeyAccount> getSertifiedPublicKeys() 
	{
		return this.sertifiedPublicKeys;
	}
	public List<String> getSertifiedPublicKeysB58() 
	{
		List<String> pbKeys = new ArrayList<String>();
		for (PublicKeyAccount key: this.sertifiedPublicKeys)
		{
			pbKeys.add(Base58.encode(key.getPublicKey()));
		};
		return pbKeys;
	}

	public List<byte[]> getSertifiedSignatures() 
	{
		return this.sertifiedSignatures;
	}
	public List<String> getSertifiedSignaturesB58() 
	{
		List<String> items = new ArrayList<String>();
		for (byte[] item: this.sertifiedSignatures)
		{
			items.add(Base58.encode(item));
		};
		return items;
	}
	
	public int getEndDate() 
	{
		return this.end_date;
	}
	
	public int getPublicKeysSize()
	{
		return this.typeBytes[2];
	}
	public static int getPublicKeysSize(byte[] typeBytes)
	{
		return typeBytes[2];
	}
			
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJson() 
	{
		//GET BASE
		JSONObject transaction = this.getJsonBase();

		//ADD CREATOR/SERVICE/DATA
		transaction.put("key", this.key);
		List<String> pbKeys = new ArrayList<String>();
		transaction.put("sertified_public_keys", this.getSertifiedPublicKeysB58());
		transaction.put("sertified_signatures", this.getSertifiedSignaturesB58());
		transaction.put("end_date", this.end_date);
		
		return transaction;	
	}
	
	public void signUserAccounts(List<PrivateKeyAccount> userPrivateAccounts)
	{
		byte[] data;
		// use this.reference in any case
		data = this.toBytes( false, null );
		if ( data == null ) return;

		if (this.sertifiedSignatures == null) this.sertifiedSignatures = new ArrayList<byte[]>();
		
		byte[] publicKey;
		for ( PublicKeyAccount publicAccount: this.sertifiedPublicKeys)
		{
			for ( PrivateKeyAccount privateAccount: userPrivateAccounts)
			{
				publicKey = privateAccount.getPublicKey();
				if (Arrays.equals((publicKey), publicAccount.getPublicKey()))
				{
					this.sertifiedSignatures.add(Crypto.getInstance().sign(privateAccount, data));
					break;
				}
			}
		}
	}

	// releaserReference = null - not a pack
	// releaserReference = reference for releaser account - it is as pack
	public static Transaction Parse(byte[] data, byte[] releaserReference) throws Exception
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

		byte[] reference;
		if (!asPack) {
			//READ REFERENCE
			reference = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
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

		//READ PERSON KEY
		byte[] keyBytes = Arrays.copyOfRange(data, position, position + KEY_LENGTH);
		long key = Longs.fromByteArray(keyBytes);	
		position += KEY_LENGTH;

		//byte[] item;
		List<PublicKeyAccount> sertifiedPublicKeys = new ArrayList<PublicKeyAccount>();
		List<byte[]> sertifiedSignatures = new ArrayList<byte[]>();
		for (int i=0; i< getPublicKeysSize(typeBytes); i++)
		{
			//READ USER ACCOUNT
			sertifiedPublicKeys.add(new PublicKeyAccount(Arrays.copyOfRange(data, position, position + USER_ADDRESS_LENGTH)));
			position += USER_ADDRESS_LENGTH;			

			if (getVersion(typeBytes)==1)
			{
				//READ USER SIGNATURE
				sertifiedSignatures.add( Arrays.copyOfRange(data, position, position + SIGNATURE_LENGTH));
				position += SIGNATURE_LENGTH;
			}
		}

		// READ DURATION
		int end_date = Ints.fromByteArray(Arrays.copyOfRange(data, position, position + DATE_DAY_LENGTH));
		position += DATE_DAY_LENGTH;

		if (!asPack) {
			return new R_SertifyPerson(typeBytes, creator, feePow, key,
					sertifiedPublicKeys,
					end_date, timestamp, reference, signature,
					sertifiedSignatures);
		} else {
			return new R_SertifyPerson(typeBytes, creator, key,
					sertifiedPublicKeys,
					end_date, signature,
					sertifiedSignatures);
		}

	}

	//@Override
	public byte[] toBytes(boolean withSign, byte[] releaserReference) {

		byte[] data = super.toBytes(withSign, releaserReference);

		//WRITE PERSON KEY
		byte[] keyBytes = Longs.toByteArray(this.key);
		keyBytes = Bytes.ensureCapacity(keyBytes, KEY_LENGTH, 0);
		data = Bytes.concat(data, keyBytes);
		
		//WRITE USER PUBLIC KEYS
		int i = 0;
		for ( PublicKeyAccount publicAccount: this.sertifiedPublicKeys)
		{
			data = Bytes.concat(data, publicAccount.getPublicKey());
			
			if (withSign & this.getVersion()==1)
			{
				data = Bytes.concat(data, this.sertifiedSignatures.get(i++));
			}
		}
		
		//WRITE DURATION
		data = Bytes.concat(data, Ints.toByteArray(this.end_date));

		return data;	
	}

	@Override
	public int getDataLength(boolean asPack)
	{
		// not include note reference
		int len = asPack? BASE_LENGTH_AS_PACK : BASE_LENGTH;
		int accountsSize = this.sertifiedPublicKeys.size(); 
		len += accountsSize * PublicKeyAccount.PUBLIC_KEY_LENGTH;
		return this.typeBytes[1] == 1? len + Transaction.SIGNATURE_LENGTH * accountsSize: len;
	}

	//VALIDATE

	@Override
	public boolean isSignatureValid() {

		if ( this.signature == null || this.signature.length != 64 || this.signature == new byte[64] )
			return false;

		int pAccountsSize = 0;
		if (this.getVersion() == 1)
		{
			pAccountsSize = this.sertifiedPublicKeys.size();
			if (pAccountsSize > this.sertifiedSignatures.size())
				return false;
			
			byte[] singItem;
			for (int i = 0; i < pAccountsSize; i++)
			{
				//if (this.sertifiedSignatures.e(i);
				singItem = this.sertifiedSignatures.get(i);
				if (singItem == null || singItem.length != 64 || singItem == new byte[64])
				{
					return false;
				}
			}
		}
		
		byte[] data = this.toBytes( false, null );
		if ( data == null ) return false;

		Crypto crypto = Crypto.getInstance();
		if (!crypto.verify(creator.getPublicKey(), signature, data))
				return false;

		// if use signs from person
		if (this.getVersion() == 1)
		{
			for (int i = 0; i < pAccountsSize; i++)
			{
				if (!crypto.verify(this.sertifiedPublicKeys.get(i).getPublicKey(), this.sertifiedSignatures.get(i), data))
					return false;
			}
		}

		return true;
	}

	//
	public int isValid(DBSet db, byte[] releaserReference) {
		
		int result = super.isValid(db, releaserReference);
		if (result != Transaction.VALIDATE_OK) return result; 

		//CHECK END_DAY
		if(end_date < 0)
		{
			return INVALID_DATE;
		}
	
		for (PublicKeyAccount publicAccount: this.sertifiedPublicKeys)
		{
			//CHECK IF PERSON PUBLIC KEY IS VALID
			if(!publicAccount.isValid())
			{
				return INVALID_PUBLIC_KEY;
			}
		}

		BigDecimal balERM = this.creator.getConfirmedBalance(RIGHTS_KEY, db);
		if ( balERM.compareTo(MIN_ERM_BALANCE)<0 )
		{
			return Transaction.NOT_ENOUGH_RIGHTS;
		}

		
		if ( !db.getItemPersonMap().contains(this.key) )
		{
			return Transaction.ITEM_PERSON_NOT_EXIST;
			//return Transaction.ITEM_DOES_NOT_EXIST;
		}

		if ( !this.creator.isPerson(db) )
		{
			if ( balERM.compareTo(GENERAL_ERM_BALANCE)<0 )
				// if not enough RIGHT BALANCE as GENERAL
				return Transaction.ACCOUNT_NOT_PERSONALIZED;
		}
		
		return Transaction.VALIDATE_OK;
	}

	//PROCESS/ORPHAN
	
	public void process(DBSet db, boolean asPack) {

		//UPDATE SENDER
		super.process(db, asPack);

		PublicKeyAccount pkAccount = this.sertifiedPublicKeys.get(0);
		// send GIFT FEE_KEY
		this.creator.setConfirmedBalance(FEE_KEY, this.creator.getConfirmedBalance(FEE_KEY, db).subtract(GIFTED_FEE_AMOUNT), db);						
		pkAccount.setConfirmedBalance(Transaction.FEE_KEY, 
				pkAccount.getConfirmedBalance(Transaction.FEE_KEY, db).add(GIFTED_FEE_AMOUNT), db);
		
		Tuple3<Integer, Integer, byte[]> itemP = new Tuple3<Integer, Integer, byte[]>(this.end_date,
				Controller.getInstance().getHeight(), this.signature);
		Tuple4<Long, Integer, Integer, byte[]> itemA = new Tuple4<Long, Integer, Integer, byte[]>(this.key, this.end_date,
				Controller.getInstance().getHeight(), this.signature);
		
		// SET ALIVE PERSON for DURATION
		// TODO set STATUSES by reference of it record - not by key!
		/// or add MAP by reference as signature - as IssueAsset - for orphans delete
		db.getPersonStatusMap().addItem(this.key, itemP);

		// TODO need MAP List<address> for ONE PERSON - Tuple2<Long, List<String>>
		// SET PERSON ADDRESS
		String address;
		for (PublicKeyAccount publicAccount: this.sertifiedPublicKeys)
		{
			address = publicAccount.getAddress();
			db.getAddressPersonMap().addItem(address, itemA);
			db.getPersonAddressMap().addItem(this.key, address, itemP);			
		}
				
		if (!asPack) {

			//UPDATE REFERENCE OF RECIPIENT - for first accept OIL need
			if(Arrays.equals(pkAccount.getLastReference(db), new byte[0]))
			{
				pkAccount.setLastReference(this.signature, db);
			}
		}

	}

	public void orphan(DBSet db, boolean asPack) {

		//UPDATE SENDER
		super.orphan(db, asPack);
		
		PublicKeyAccount pkAccount = this.sertifiedPublicKeys.get(0);
		// BACK GIFT FEE_KEY
		this.creator.setConfirmedBalance(Transaction.FEE_KEY, this.creator.getConfirmedBalance(Transaction.FEE_KEY, db).add(GIFTED_FEE_AMOUNT), db);						
		pkAccount.setConfirmedBalance(Transaction.FEE_KEY, pkAccount.getConfirmedBalance(Transaction.FEE_KEY, db).subtract(GIFTED_FEE_AMOUNT), db);
						
		// UNDO ALIVE PERSON for DURATION
		db.getPersonStatusMap().removeItem(this.key, StatusCls.ALIVE_KEY);

		//UPDATE RECIPIENT
		String address;
		for (PublicKeyAccount publicAccount: this.sertifiedPublicKeys)
		{
			address = publicAccount.getAddress();
			db.getAddressPersonMap().removeItem(address);
			db.getPersonAddressMap().removeItem(this.key, address);
		}
		
		if (!asPack) {
			
			//UPDATE REFERENCE OF RECIPIENT
			if(Arrays.equals(pkAccount.getLastReference(db), this.signature))
			{
				pkAccount.removeReference(db);
			}	
		}
	}

	@Override
	public HashSet<Account> getInvolvedAccounts()
	{
		HashSet<Account> accounts = new HashSet<Account>();
		accounts.add(this.creator);
		accounts.addAll(this.getRecipientAccounts());
		return accounts;
	}

	@Override
	public HashSet<Account> getRecipientAccounts() {
		HashSet<Account> accounts = new HashSet<Account>();
		accounts.addAll(this.sertifiedPublicKeys);
		
		return accounts;
	}
	
	@Override
	public boolean isInvolved(Account account) 
	{
		String address = account.getAddress();
		if(address.equals(creator.getAddress())) return true;
		
		for (PublicKeyAccount publicAccount: this.sertifiedPublicKeys)
		{
			if (address.equals(publicAccount.getAddress()))
					return true;
		}
				
		return false;
	}

	public int calcBaseFee() {
		return calcCommonFee();
	}

}