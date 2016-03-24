package qora.transaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import qora.account.Account;
//import qora.account.PrivateKeyAccount;
import qora.account.PublicKeyAccount;
import qora.assets.Asset;
import qora.assets.Order;
//import qora.crypto.Crypto;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

//import database.BalanceMap;
import database.DBSet;

public class CreateOrderTransaction extends Transaction 
{
	private static final int TYPE_ID = Transaction.CREATE_ORDER_TRANSACTION;
	private static final String NAME_ID = "Create Order";
	private static final int AMOUNT_LENGTH = TransactionAmount.AMOUNT_LENGTH;
	private static final int HAVE_LENGTH = 8;
	private static final int WANT_LENGTH = 8;
	private static final int PRICE_LENGTH = 12;
	private static final int BASE_LENGTH = 1 + TIMESTAMP_LENGTH + REFERENCE_LENGTH + CREATOR_LENGTH + HAVE_LENGTH + WANT_LENGTH + AMOUNT_LENGTH + PRICE_LENGTH + SIGNATURE_LENGTH;

	private Order order;
	private long have;
	private long want;
	private BigDecimal amount;
	private BigDecimal price;
	
	public CreateOrderTransaction(PublicKeyAccount creator, long have, long want, BigDecimal amount, BigDecimal price, long timestamp, byte[] reference) 
	{
		super(TYPE_ID, NAME_ID, creator, (byte)0, timestamp, reference);
		
		this.have = have;
		this.want = want;
		this.amount = amount;
		this.price = price;
	}
	public CreateOrderTransaction(PublicKeyAccount creator, long have, long want, BigDecimal amount, BigDecimal price, byte feePow, long timestamp, byte[] reference, byte[] signature) 
	{
		this(creator, have, want, amount, price, timestamp, reference);
		this.signature = signature;
		this.order = new Order(new BigInteger(this.signature), creator, have, want, amount, price, timestamp);
		this.feePow = feePow;
		this.calcFee();
		
	}
	public CreateOrderTransaction(PublicKeyAccount creator, long have, long want, BigDecimal amount, BigDecimal price, byte feePow, long timestamp, byte[] reference) 
	{
		this(creator, have, want, amount, price, timestamp, reference);
		this.feePow = feePow;
	}

	//GETTERS/SETTERS
	//public static String getName() { return "Create Order"; }

	public void makeOrder()
	{
		if (this.order == null) this.order = new Order(new BigInteger(this.signature),
				this.creator, this.have, this.want, this.amount, this.price, this.timestamp);
	}

	public Order getOrder()
	{
		return this.order;
	}
	
	//PARSE CONVERT
	
	public static Transaction Parse(byte[] data) throws Exception
	{	
		//CHECK IF WE MATCH BLOCK LENGTH
		if(data.length < BASE_LENGTH)
		{
			throw new Exception("Data does not match block length");
		}
		
		int position = 0;
		
		//READ TIMESTAMP
		byte[] timestampBytes = Arrays.copyOfRange(data, position, position + TIMESTAMP_LENGTH);
		long timestamp = Longs.fromByteArray(timestampBytes);	
		position += TIMESTAMP_LENGTH;
		
		//READ REFERENCE
		byte[] reference = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
		position += REFERENCE_LENGTH;
		
		//READ CREATOR
		byte[] creatorBytes = Arrays.copyOfRange(data, position, position + CREATOR_LENGTH);
		PublicKeyAccount creator = new PublicKeyAccount(creatorBytes);
		position += CREATOR_LENGTH;
		
		//READ HAVE
		byte[] haveBytes = Arrays.copyOfRange(data, position, position + HAVE_LENGTH);
		long have = Longs.fromByteArray(haveBytes);	
		position += HAVE_LENGTH;
		
		//READ WANT
		byte[] wantBytes = Arrays.copyOfRange(data, position, position + WANT_LENGTH);
		long want = Longs.fromByteArray(wantBytes);	
		position += WANT_LENGTH;
		
		//READ AMOUNT
		byte[] amountBytes = Arrays.copyOfRange(data, position, position + AMOUNT_LENGTH);
		BigDecimal amount = new BigDecimal(new BigInteger(amountBytes), 8);
		position += AMOUNT_LENGTH;
		
		//READ PRICE
		byte[] priceBytes = Arrays.copyOfRange(data, position, position + PRICE_LENGTH);
		BigDecimal price = new BigDecimal(new BigInteger(priceBytes), 8);
		position += PRICE_LENGTH;
		
		//READ FEE POWER
		byte[] feePowBytes = Arrays.copyOfRange(data, position, position + 1);
		byte feePow = feePowBytes[0];
		position += 1;
		
		//READ SIGNATURE
		byte[] signatureBytes = Arrays.copyOfRange(data, position, position + SIGNATURE_LENGTH);
		
		return new CreateOrderTransaction(creator, have, want, amount, price, feePow, timestamp, reference, signatureBytes);
	}	
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJson() 
	{
		//GET BASE
		JSONObject transaction = this.getJsonBase();
				
		//ADD CREATOR/ORDER
		transaction.put("creator", this.creator.getAddress());
		
		JSONObject order = new JSONObject();
		order.put("have", this.order.getHave());
		order.put("want", this.order.getWant());
		order.put("amount", this.order.getAmount().toPlainString());
		order.put("price", this.order.getPrice().toPlainString());
		
		transaction.put("order", order);
				
		return transaction;	
	}
	
	@Override
	public byte[] toBytes(boolean withSign)
	{
		byte[] data = new byte[0];
		
		//WRITE TYPE
		byte[] typeBytes = Ints.toByteArray(TYPE_ID);
		typeBytes = Bytes.ensureCapacity(typeBytes, TYPE_LENGTH, 0);
		data = Bytes.concat(data, typeBytes);
		
		//WRITE TIMESTAMP
		byte[] timestampBytes = Longs.toByteArray(this.timestamp);
		timestampBytes = Bytes.ensureCapacity(timestampBytes, TIMESTAMP_LENGTH, 0);
		data = Bytes.concat(data, timestampBytes);
		
		//WRITE REFERENCE
		data = Bytes.concat(data, this.reference);
		
		//WRITE CREATOR
		data = Bytes.concat(data, this.creator.getPublicKey());
		
		//WRITE HAVE
		byte[] haveBytes = Longs.toByteArray(this.order.getHave());
		haveBytes = Bytes.ensureCapacity(haveBytes, HAVE_LENGTH, 0);
		data = Bytes.concat(data, haveBytes);
		
		//WRITE WANT
		byte[] wantBytes = Longs.toByteArray(this.order.getWant());
		wantBytes = Bytes.ensureCapacity(wantBytes, WANT_LENGTH, 0);
		data = Bytes.concat(data, wantBytes);
		
		//WRITE AMOUNT
		byte[] amountBytes = this.order.getAmount().unscaledValue().toByteArray();
		byte[] fill = new byte[AMOUNT_LENGTH - amountBytes.length];
		amountBytes = Bytes.concat(fill, amountBytes);
		data = Bytes.concat(data, amountBytes);
		
		//WRITE PRICE
		byte[] priceBytes = this.order.getPrice().unscaledValue().toByteArray();
		fill = new byte[PRICE_LENGTH - priceBytes.length];
		priceBytes = Bytes.concat(fill, priceBytes);
		data = Bytes.concat(data, priceBytes);
		
		//WRITE FEE POWER
		byte[] feePowBytes = new byte[1];
		feePowBytes[0] = this.feePow;
		data = Bytes.concat(data, feePowBytes);

		//SIGNATURE
		if (withSign) data = Bytes.concat(data, this.signature);
		
		return data;
	}
	
	@Override
	public int getDataLength()
	{
		return TYPE_LENGTH + BASE_LENGTH;
	}
	
	//VALIDATE
		
	@Override
	public int isValid(DBSet db) 
	{
		//CHECK IF ASSETS NOT THE SAME
		if(this.order.getHave() == this.order.getWant())
		{
			return HAVE_EQUALS_WANT;
		}
		
		//CHECK IF AMOUNT POSITIVE
		if(this.order.getAmount().compareTo(BigDecimal.ZERO) <= 0)
		{
			return NEGATIVE_AMOUNT;
		}
		
		//CHECCK IF PRICE POSITIVE
		if(this.order.getPrice().compareTo(BigDecimal.ZERO) <= 0)
		{
			return NEGATIVE_PRICE;
		}
		
		//REMOVE FEE
		DBSet fork = db.fork();
		super.process(fork);
		
		//CHECK IF SENDER HAS ENOUGH ASSET BALANCE
		if(this.creator.getConfirmedBalance(this.order.getHave(), fork).compareTo(this.order.getAmount()) == -1)
		{
			return NO_BALANCE;
		}
		
		//CHECK IF SENDER HAS ENOUGH FEE BALANCE
		if(this.creator.getConfirmedBalance(FEE_KEY, fork).compareTo(this.fee) == -1)
		{
			return NOT_ENOUGH_FEE;
		}
		
		//CHECK IF HAVE IS NOT DIVISBLE
		if(!this.order.getHaveAsset(db).isDivisible())
		{
			//CHECK IF AMOUNT DOES NOT HAVE ANY DECIMALS
			if(this.order.getAmount().stripTrailingZeros().scale() > 0)
			{
				//AMOUNT HAS DECIMALS
				return INVALID_AMOUNT;
			}
		}
		
		//CHECK IF WANT EXISTS
		Asset wantAsset = this.order.getWantAsset(db);
		if(wantAsset == null)
		{
			//WANT DOES NOT EXIST
			return ASSET_DOES_NOT_EXIST;
		}
		
		//CHECK IF WANT IS NOT DIVISIBLE
		if(!wantAsset.isDivisible())
		{
			//CHECK IF TOTAL RETURN DOES NOT HAVE ANY DECIMALS
			if(this.order.getAmount().multiply(this.order.getPrice()).stripTrailingZeros().scale() > 0)
			{
				return INVALID_RETURN;
			}
		}
		
		//CHECK IF REFERENCE IS OK
		if(!Arrays.equals(this.creator.getLastReference(db), this.reference))
		{
			return INVALID_REFERENCE;
		}
				
		return VALIDATE_OK;
	}
	
	//PROCESS/ORPHAN

	//@Override
	public void process(DBSet db)
	{
		//UPDATE CREATOR
		super.process(db);
								
		//UPDATE REFERENCE OF CREATOR
		this.creator.setLastReference(this.signature, db);
		
		//PROCESS ORDER
		this.order.copy().process(db, this);
	}


	//@Override
	public void orphan(DBSet db) 
	{
		//UPDATE CREATOR
		super.orphan(db);
										
		//UPDATE REFERENCE OF CREATOR
		this.creator.setLastReference(this.reference, db);
				
		//ORPHAN ORDER
		this.order.copy().orphan(db);
	}


	@Override
	public List<Account> getInvolvedAccounts() 
	{
		List<Account> accounts = new ArrayList<Account>();
		accounts.add(this.creator);
		return accounts;
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


	//@Override
	public BigDecimal viewAmount(Account account) 
	{
		if(account.getAddress().equals(this.creator.getAddress()))
		{
			return BigDecimal.ZERO.setScale(8);
		}
		
		return BigDecimal.ZERO;
	}

	public Map<String, Map<Long, BigDecimal>> getAssetAmount() 
	{
		Map<String, Map<Long, BigDecimal>> assetAmount = new LinkedHashMap<>();

		assetAmount = subAssetAmount(assetAmount, this.creator.getAddress(), FEE_KEY, this.fee);
		assetAmount = subAssetAmount(assetAmount, this.creator.getAddress(), this.order.getHave(), this.order.getAmount());
		
		return assetAmount;
	}
	public int calcBaseFee() {
		return calcCommonFee();
	}
}
