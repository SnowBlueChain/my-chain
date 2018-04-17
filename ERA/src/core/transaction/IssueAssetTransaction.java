package core.transaction;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.google.common.primitives.Longs;

import core.BlockChain;
import core.account.PublicKeyAccount;
import core.block.Block;
import core.item.assets.AssetCls;
import core.item.assets.AssetFactory;
import datachain.DCSet;

public class IssueAssetTransaction extends Issue_ItemRecord
{
	private static final byte TYPE_ID = (byte)ISSUE_ASSET_TRANSACTION;
	private static final String NAME_ID = "Issue Asset";

	//private static final int BASE_LENGTH = Transaction.BASE_LENGTH;

	//private AssetCls asset;

	public IssueAssetTransaction(byte[] typeBytes, PublicKeyAccount creator, AssetCls asset, byte feePow, long timestamp, Long reference)
	{
		super(typeBytes, NAME_ID, creator, asset, feePow, timestamp, reference);
		//this.asset = asset;
	}
	public IssueAssetTransaction(byte[] typeBytes, PublicKeyAccount creator, AssetCls asset, byte feePow, long timestamp, Long reference, byte[] signature)
	{
		super(typeBytes, NAME_ID, creator, asset, feePow, timestamp, reference, signature);
	}
	// as pack
	public IssueAssetTransaction(byte[] typeBytes, PublicKeyAccount creator, AssetCls asset, byte[] signature)
	{
		super(typeBytes, NAME_ID, creator, asset, (byte)0, 0l, null, signature);
	}
	public IssueAssetTransaction(PublicKeyAccount creator, AssetCls asset, byte feePow, long timestamp, Long reference, byte[] signature)
	{
		this(new byte[]{TYPE_ID,0,0,0}, creator, asset, feePow, timestamp, reference, signature);
	}
	// as pack
	public IssueAssetTransaction(PublicKeyAccount creator, AssetCls asset, Long reference, byte[] signature)
	{
		this(new byte[]{TYPE_ID,0,0,0}, creator, asset, (byte)0, 0l, reference, signature);
	}
	public IssueAssetTransaction(PublicKeyAccount creator, AssetCls asset, byte feePow, long timestamp, Long reference)
	{
		this(new byte[]{TYPE_ID,0,0,0}, creator, asset, feePow, timestamp, reference);
	}

	//GETTERS/SETTERS
	//public static String getName() { return "Issue Asset"; }

	public long getAssetKey(DCSet db)
	{
		return this.getItem().getKey(db);
	}

	@Override
	public BigDecimal getAmount() {
		return new BigDecimal(((AssetCls)this.getItem()).getQuantity());
	}

	@Override
	public BigDecimal getAmount(String address) {

		if(address.equals(this.creator.getAddress()))
		{
			return this.getAmount();
		}

		AssetCls asset = (AssetCls)this.item;
		return BigDecimal.ZERO.setScale(asset.getScale());
	}

	// NOT GENESIS ISSUE START FRON NUM
	@Override
	protected long getStartKey() {
		return 1000l;
	}

	/*
	@Override
	public BigDecimal getAmount(Account account) {
		String address = account.getAddress();
		return getAmount(address);
	}
	 */

	@Override
	public String viewAmount(String address) {
		return this.getAmount().toString();
	}

	//VALIDATE

	//@Override
	@Override
	public int isValid(DCSet db, Long releaserReference)
	{

		int result = super.isValid(db, releaserReference);
		if(result != Transaction.VALIDATE_OK) return result;

		//CHECK QUANTITY
		AssetCls asset = (AssetCls)this.getItem();
		//long maxQuantity = asset.isDivisible() ? 10000000000L : 1000000000000000000L;
		long maxQuantity = Long.MAX_VALUE;
		long quantity = asset.getQuantity();
		//if(quantity > maxQuantity || quantity < 0 && quantity != -1 && quantity != -2 )
		if(quantity > maxQuantity || quantity < -1 )
		{
			return INVALID_QUANTITY;
		}

		return Transaction.VALIDATE_OK;
	}

	//PARSE CONVERT

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJson()
	{
		//GET BASE
		JSONObject transaction = super.toJson();
		AssetCls asset = (AssetCls)this.getItem();
		//ADD CREATOR/NAME/DISCRIPTION/QUANTITY/DIVISIBLE
		transaction.put("quantity", asset.getQuantity());
		transaction.put("scale", asset.getScale());
		transaction.put("divisible", asset.isDivisible());

		return transaction;
	}

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
		byte[] signatureBytes = Arrays.copyOfRange(data, position, position + SIGNATURE_LENGTH);
		position += SIGNATURE_LENGTH;

		/////

		//READ ASSET
		// asset parse without reference - if is = signature
		AssetCls asset = AssetFactory.getInstance().parse(Arrays.copyOfRange(data, position, data.length), false);
		position += asset.getDataLength(false);

		if (!asPack) {
			return new IssueAssetTransaction(typeBytes, creator, asset, feePow, timestamp, reference, signatureBytes);
		} else {
			return new IssueAssetTransaction(typeBytes, creator, asset, signatureBytes);
		}
	}


	/*
	//@Override
	public byte[] toBytes(boolean withSign, byte[] releaserReference)
	{

		byte[] data = super.toBytes(withSign, releaserReference);

		//WRITE ASSET
		// without reference
		data = Bytes.concat(data, this.asset.toBytes(false));

		return data;
	}
	 */

	/*
	@Override
	public int getDataLength(boolean asPack)
	{
		// not include asset reference
		if (asPack) {
			return BASE_LENGTH_AS_PACK + this.asset.getDataLength(false);
		} else {
			return BASE_LENGTH + this.asset.getDataLength(false);
		}
	}
	 */

	//PROCESS/ORPHAN

	//@Override
	@Override
	public void process(Block block, boolean asPack)
	{
		//UPDATE CREATOR
		super.process(block, asPack);

		//ADD ASSETS TO OWNER
		//this.creator.setBalance(this.getItem().getKey(db), new BigDecimal(((AssetCls)this.getItem()).getQuantity()).setScale(), db);
		AssetCls asset = (AssetCls)this.getItem();
		this.creator.changeBalance(this.dcSet, false, asset.getKey(this.dcSet),
				new BigDecimal(asset.getQuantity()).setScale(0), false);

	}

	//@Override
	@Override
	public void orphan(boolean asPack)
	{
		//UPDATE CREATOR
		super.orphan(asPack);

		//REMOVE ASSETS FROM OWNER
		AssetCls asset = (AssetCls)this.getItem();
		//this.creator.setBalance(this.getItem().getKey(db), BigDecimal.ZERO.setScale(), db);
		this.creator.changeBalance(this.dcSet, true, asset.getKey(this.dcSet),
				new BigDecimal(asset.getQuantity()).setScale(0), true);
	}

	/*
	@Override
	public HashSet<Account> getInvolvedAccounts()
	{
		return this.getRecipientAccounts();
	}

	@Override
	public HashSet<Account> getRecipientAccounts()
	{
		HashSet<Account> accounts = new HashSet<>();
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
	 */

	//@Override
	public Map<String, Map<Long, BigDecimal>> getAssetAmount()
	{
		Map<String, Map<Long, BigDecimal>> assetAmount = new LinkedHashMap<>();

		assetAmount = subAssetAmount(assetAmount, this.creator.getAddress(), FEE_KEY, this.fee);

		AssetCls asset = (AssetCls)this.getItem();
		assetAmount = addAssetAmount(assetAmount, this.creator.getAddress(), asset.getKey(this.dcSet),
				new BigDecimal(asset.getQuantity()).setScale(0));

		return assetAmount;
	}

	@Override
	public int calcBaseFee() {
		return calcCommonFee() + BlockChain.FEE_PER_BYTE * 128 * BlockChain.ISSUE_ASSET_MULT_FEE;
	}
}
