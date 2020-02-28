package org.erachain.core.transaction;

import com.google.common.primitives.Longs;
import org.erachain.core.BlockChain;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.assets.AssetFactory;
import org.erachain.datachain.DCSet;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class IssueAssetTransaction extends IssueItemRecord {
    private static final byte TYPE_ID = (byte) ISSUE_ASSET_TRANSACTION;
    private static final String NAME_ID = "Issue Asset";

    public static final long START_KEY = 1000l; // << 20;

    //private static final int BASE_LENGTH = Transaction.BASE_LENGTH;

    //private AssetCls asset;

    public IssueAssetTransaction(byte[] typeBytes, PublicKeyAccount creator, AssetCls asset, byte feePow, long timestamp, Long reference) {
        super(typeBytes, NAME_ID, creator, asset, feePow, timestamp, reference);
        //this.asset = asset;
    }

    public IssueAssetTransaction(byte[] typeBytes, PublicKeyAccount creator, AssetCls asset, byte feePow, long timestamp, Long reference, byte[] signature) {
        super(typeBytes, NAME_ID, creator, asset, feePow, timestamp, reference, signature);
    }
    public IssueAssetTransaction(byte[] typeBytes, PublicKeyAccount creator, AssetCls asset, byte feePow,
                                 long timestamp, Long reference, byte[] signature, long feeLong) {
        super(typeBytes, NAME_ID, creator, asset, feePow, timestamp, reference, signature);
        this.fee = BigDecimal.valueOf(feeLong, BlockChain.AMOUNT_DEDAULT_SCALE);
    }

    // as pack
    public IssueAssetTransaction(byte[] typeBytes, PublicKeyAccount creator, AssetCls asset, byte[] signature) {
        super(typeBytes, NAME_ID, creator, asset, (byte) 0, 0l, null, signature);
    }

    public IssueAssetTransaction(PublicKeyAccount creator, AssetCls asset, byte feePow, long timestamp, Long reference, byte[] signature) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, asset, feePow, timestamp, reference, signature);
    }

    // as pack
    public IssueAssetTransaction(PublicKeyAccount creator, AssetCls asset, Long reference, byte[] signature) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, asset, (byte) 0, 0l, reference, signature);
    }

    public IssueAssetTransaction(PublicKeyAccount creator, AssetCls asset, byte feePow, long timestamp, Long reference) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, asset, feePow, timestamp, reference);
    }

    //GETTERS/SETTERS
    //public static String getName() { return "Issue Asset"; }

    // RETURN START KEY in tot GEMESIS
    public long getStartKey(int height) {
        if (height < BlockChain.VERS_4_11) {
            return 1000L;
        }
        return START_KEY;

    }

    public static Transaction Parse(byte[] data, int asDeal) throws Exception {
        //boolean asPack = releaserReference != null;

        //CHECK IF WE MATCH BLOCK LENGTH
        int test_len;
        if (asDeal == Transaction.FOR_MYPACK) {
            test_len = BASE_LENGTH_AS_MYPACK;
        } else if (asDeal == Transaction.FOR_PACK) {
            test_len = BASE_LENGTH_AS_PACK;
        } else if (asDeal == Transaction.FOR_DB_RECORD) {
            test_len = BASE_LENGTH_AS_DBRECORD;
        } else {
            test_len = BASE_LENGTH;
        }
        if (data.length < test_len) {
            throw new Exception("Data does not match block length " + data.length);
        }
        // READ TYPE
        byte[] typeBytes = Arrays.copyOfRange(data, 0, TYPE_LENGTH);
        int position = TYPE_LENGTH;

        long timestamp = 0;
        if (asDeal > Transaction.FOR_MYPACK) {
            //READ TIMESTAMP
            byte[] timestampBytes = Arrays.copyOfRange(data, position, position + TIMESTAMP_LENGTH);
            timestamp = Longs.fromByteArray(timestampBytes);
            position += TIMESTAMP_LENGTH;
        }

        //READ REFERENCE
        byte[] referenceBytes = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
        Long reference = Longs.fromByteArray(referenceBytes);
        position += REFERENCE_LENGTH;

        //READ CREATOR
        byte[] creatorBytes = Arrays.copyOfRange(data, position, position + CREATOR_LENGTH);
        PublicKeyAccount creator = new PublicKeyAccount(creatorBytes);
        position += CREATOR_LENGTH;

        byte feePow = 0;
        if (asDeal > Transaction.FOR_PACK) {
            //READ FEE POWER
            byte[] feePowBytes = Arrays.copyOfRange(data, position, position + 1);
            feePow = feePowBytes[0];
            position += 1;
        }

        //READ SIGNATURE
        byte[] signatureBytes = Arrays.copyOfRange(data, position, position + SIGNATURE_LENGTH);
        position += SIGNATURE_LENGTH;

        long feeLong = 0;
        if (asDeal == FOR_DB_RECORD) {
            // READ FEE
            byte[] feeBytes = Arrays.copyOfRange(data, position, position + FEE_LENGTH);
            feeLong = Longs.fromByteArray(feeBytes);
            position += FEE_LENGTH;
        }

        //READ ASSET
        // asset parse without reference - if is = signature
        AssetCls asset = AssetFactory.getInstance().parse(Arrays.copyOfRange(data, position, data.length), false);
        position += asset.getDataLength(false);

        if (asDeal > Transaction.FOR_MYPACK) {
            return new IssueAssetTransaction(typeBytes, creator, asset, feePow, timestamp, reference, signatureBytes, feeLong);
        } else {
            return new IssueAssetTransaction(typeBytes, creator, asset, signatureBytes);
        }
    }

    public long getAssetKey(DCSet db) {
        return getItem().getKey(db);
    }

    @Override
    public BigDecimal getAmount() {
        return new BigDecimal(((AssetCls) getItem()).getQuantity());
    }

    @Override
    public BigDecimal getAmount(String address) {
        if (address.equals(creator.getAddress())) {
            return getAmount();
        }
        AssetCls asset = (AssetCls) item;
        return BigDecimal.ZERO.setScale(asset.getScale());
    }

	/*
	@Override
	public BigDecimal getAmount(Account account) {
		String address = account.getAddress();
		return getAmount(address);
	}
	 */

	/*
    public void setDC(DCSet dcSet, int asDeal, int blockHeight, int seqNo) {
        super.setDC(dcSet, asDeal, blockHeight, seqNo);

        AssetCls asset = (AssetCls) this.item;

        if (false && dcSet.getItemAssetMap().getLastKey() < BlockChain.AMOUNT_SCALE_FROM) {
            // MAKE OLD STYLE ASSET with DEVISIBLE:
            // PROP1 = 0 (unMOVABLE, SCALE = 8, assetTYPE = 1 (divisible)
            asset = new AssetVenture((byte) 0, asset.getOwner(), asset.getName(),
                    asset.getIcon(), asset.getImage(), asset.getDescription(), AssetCls.AS_INSIDE_ASSETS, asset.getScale(), asset.getQuantity());
            this.item = asset;
        }

    }
    */

    //VALIDATE

    @Override
    public String viewAmount(String address) {
        return this.getAmount().toString();
    }

    //PARSE CONVERT

    @Override
    public boolean hasPublicText() {
        if (this.item.isNovaAsset(this.creator, this.dcSet) > 0) {
                return false;
        }

        return true;
    }

    //@Override
    @Override
    public int isValid(int asDeal, long flags) {

        int result = super.isValid(asDeal, flags);
        if (result != Transaction.VALIDATE_OK){
            return result;
        }
        //CHECK QUANTITY
        AssetCls asset = (AssetCls) this.getItem();
        //long maxQuantity = asset.isDivisible() ? 10000000000L : 1000000000000000000L;
        long maxQuantity = Long.MAX_VALUE;
        long quantity = asset.getQuantity();
        //if(quantity > maxQuantity || quantity < 0 && quantity != -1 && quantity != -2 )
        if (quantity > maxQuantity || quantity < -1) {
            return INVALID_QUANTITY;
        }
        return Transaction.VALIDATE_OK;
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJson() {
        //GET BASE
        JSONObject transaction = super.toJson();
        AssetCls asset = (AssetCls) getItem();
        //ADD CREATOR/NAME/DISCRIPTION/QUANTITY/DIVISIBLE
        transaction.put("asset", asset.toJson());
        return transaction;
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

    //PROCESS/ORPHAN

    //@Override
    @Override
    public void process(Block block, int asDeal) {
        //UPDATE CREATOR
        super.process(block, asDeal);
        //ADD ASSETS TO OWNER
        AssetCls asset = (AssetCls) this.getItem();
        long quantity = asset.getQuantity();
        if (quantity > 0) {
            creator.changeBalance(dcSet, false, false, asset.getKey(dcSet),
                    new BigDecimal(quantity).setScale(0), false, false);

            // make HOLD balance
            creator.changeBalance(dcSet, false, true, asset.getKey(dcSet),
                    new BigDecimal(-quantity).setScale(0), false, false);

        } else if (quantity == 0) {
            // безразмерные - нужно баланс в таблицу нулевой записать чтобы в блокэксплорере он отображался у счета
            // см. https://lab.erachain.org/erachain/Erachain/issues/1103
            this.creator.changeBalance(this.dcSet, false, false, asset.getKey(this.dcSet),
                    BigDecimal.ZERO.setScale(0), false, false);

        }

    }

    //@Override
    @Override
    public void orphan(Block block, int asDeal) {
        //UPDATE CREATOR
        super.orphan(block, asDeal);
        //REMOVE ASSETS FROM OWNER
        AssetCls asset = (AssetCls) this.getItem();
        long quantity = asset.getQuantity();
        if (quantity > 0) {
            this.creator.changeBalance(this.dcSet, true, true, asset.getKey(this.dcSet),
                    new BigDecimal(quantity).setScale(0), false, false);

            // на балансе На Руках - добавляем тоже
            creator.changeBalance(dcSet, true, false, asset.getKey(dcSet),
                    new BigDecimal(-quantity).setScale(0), false, false);
        }
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
    public Map<String, Map<Long, BigDecimal>> getAssetAmount() {
        Map<String, Map<Long, BigDecimal>> assetAmount = new LinkedHashMap<>();
        assetAmount = subAssetAmount(assetAmount, creator.getAddress(), FEE_KEY, fee);
        AssetCls asset = (AssetCls) getItem();
        assetAmount = addAssetAmount(assetAmount, creator.getAddress(), asset.getKey(dcSet),
                new BigDecimal(asset.getQuantity()).setScale(0));
        return assetAmount;
    }

}
