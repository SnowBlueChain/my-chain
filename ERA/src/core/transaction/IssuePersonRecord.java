package core.transaction;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.primitives.Longs;

import core.BlockChain;
import core.account.PublicKeyAccount;
import core.block.Block;
import core.block.GenesisBlock;
import core.item.assets.AssetCls;
import core.item.persons.PersonCls;
import core.item.persons.PersonFactory;
import core.item.persons.PersonHuman;
import datachain.AddressTime_SignatureMap;

//import java.util.Map;
// import org.apache.log4j.Logger;

public class IssuePersonRecord extends Issue_ItemRecord {
    public static final int MAX_IMAGE_LENGTH = 20480;
    private static final byte TYPE_ID = (byte) ISSUE_PERSON_TRANSACTION;
    private static final String NAME_ID = "Issue Person";

    public IssuePersonRecord(byte[] typeBytes, PublicKeyAccount creator, PersonCls person, byte feePow, long timestamp, Long reference) {
        super(typeBytes, NAME_ID, creator, person, feePow, timestamp, reference);
    }

    public IssuePersonRecord(byte[] typeBytes, PublicKeyAccount creator, PersonCls person, byte feePow, long timestamp, Long reference, byte[] signature) {
        super(typeBytes, NAME_ID, creator, person, feePow, timestamp, reference, signature);
    }

    public IssuePersonRecord(byte[] typeBytes, PublicKeyAccount creator, PersonCls person, byte[] signature) {
        super(typeBytes, NAME_ID, creator, person, (byte) 0, 0l, null, signature);
    }

    public IssuePersonRecord(PublicKeyAccount creator, PersonCls person, byte feePow, long timestamp, Long reference, byte[] signature) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, person, feePow, timestamp, reference, signature);
    }

    public IssuePersonRecord(PublicKeyAccount creator, PersonCls person, byte[] signature) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, person, (byte) 0, 0l, null, signature);
    }

    public IssuePersonRecord(PublicKeyAccount creator, PersonCls person, byte feePow, long timestamp, Long reference) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, person, feePow, timestamp, reference);
    }

    public IssuePersonRecord(PublicKeyAccount creator, PersonCls person) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, person, (byte) 0, 0l, null);
    }

    //GETTERS/SETTERS
    //public String getName() { return "Issue Person"; }

    public static Transaction Parse(byte[] data, Long releaserReference) throws Exception {

        boolean asPack = releaserReference != null;

        //CHECK IF WE MATCH BLOCK LENGTH
        if (data.length < BASE_LENGTH_AS_PACK
                | !asPack & data.length < BASE_LENGTH) {
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

        //READ PERSON
        // person parse without reference - if is = signature
        PersonCls person = PersonFactory.getInstance().parse(Arrays.copyOfRange(data, position, data.length), false);
        position += person.getDataLength(false);

        if (!asPack) {
            return new IssuePersonRecord(typeBytes, creator, person, feePow, timestamp, reference, signatureBytes);
        } else {
            return new IssuePersonRecord(typeBytes, creator, person, signatureBytes);
        }
    }

    // NOT GENESIS ISSUE STRT FRON NUM
    @Override
    protected long getStartKey() {
        return 0l;
    }

    @Override
    public List<byte[]> getSignatures() {
        PersonHuman person = (PersonHuman) this.item;
        if (person.isMustBeSigned()) {
            List<byte[]> items = new ArrayList<byte[]>();
            items.add(person.getOwnerSignature());
            return items;
        }
        return null;
    }

	/*
	@Override
	public boolean hasPublicText() {
		for ( String admin: BlockChain.GENESIS_ADMINS) {
			if (this.creator.equals(admin)) {
				return false;
			}
		}
		return true;
	}
	 */

    @Override
    public List<PublicKeyAccount> getPublicKeys() {
        PersonHuman person = (PersonHuman) this.item;
        if (person.isMustBeSigned()) {
            List<PublicKeyAccount> items = new ArrayList<PublicKeyAccount>();
            items.add(person.getOwner());
            return items;
        }
        return null;
    }

    //PARSE CONVERT

    @Override
    public int isValid(Long releaserReference, long flags) {

        PersonCls person = (PersonCls) this.getItem();

        // FOR PERSONS need LIMIT DESCRIPTION because it may be make with 0 COMPU balance
        int descriptionLength = person.getDescription().getBytes(StandardCharsets.UTF_8).length;
        if (descriptionLength > 8000) {
            return INVALID_DESCRIPTION_LENGTH;
        }

        // birthLatitude -90..90; birthLongitude -180..180
        if (person.getBirthLatitude() > 90 || person.getBirthLatitude() < -90)
            return Transaction.ITEM_PERSON_LATITUDE_ERROR;
        if (person.getBirthLongitude() > 180 || person.getBirthLongitude() < -180)
            return Transaction.ITEM_PERSON_LONGITUDE_ERROR;
        if (person.getRace().getBytes(StandardCharsets.UTF_8).length > 255) return Transaction.ITEM_PERSON_RACE_ERROR;
        if (person.getGender() > 10) return Transaction.ITEM_PERSON_GENDER_ERROR;
        if (person.getSkinColor().getBytes(StandardCharsets.UTF_8).length > 255)
            return Transaction.ITEM_PERSON_SKIN_COLOR_ERROR;
        if (person.getEyeColor().getBytes(StandardCharsets.UTF_8).length > 255)
            return Transaction.ITEM_PERSON_EYE_COLOR_ERROR;
        if (person.getHairColor().getBytes(StandardCharsets.UTF_8).length > 255)
            return Transaction.ITEM_PERSON_HAIR_COLOR_ERROR;
        //int ii = Math.abs(person.getHeight());
        //if (Math.abs(person.getHeight()) < 1) return Transaction.ITEM_PERSON_HEIGHT_ERROR;
        if (person.getHeight() > 255) return Transaction.ITEM_PERSON_HEIGHT_ERROR;

        if (person.getDeathday() == Long.MIN_VALUE
                || person.getDeathday() < person.getBirthday()) {
            // IF PERSON is LIVE
            if (person.getImage().length < (MAX_IMAGE_LENGTH >> 1)
                    || person.getImage().length > MAX_IMAGE_LENGTH) {
                int height = this.getBlockHeightByParent(this.dcSet);
                if (height != 2998) {
                    // early blocks has wrong ISSUE_PERSON with 0 image length - in block 2998
                    return Transaction.INVALID_IMAGE_LENGTH;
                }
            }
        } else {
            // person is DIE - any PHOTO
        }

        if (person instanceof PersonHuman) {
            PersonHuman human = (PersonHuman) person;
            if (human.isMustBeSigned()
                    && !Arrays.equals(person.getOwner().getPublicKey(), this.creator.getPublicKey())) {
                // OWNER of personal INFO not is CREATOR
                if (human.getOwnerSignature() == null) {
                    return Transaction.ITEM_PERSON_OWNER_SIGNATURE_INVALID;
                }
                if (!human.isSignatureValid(this.dcSet)) {
                    return Transaction.ITEM_PERSON_OWNER_SIGNATURE_INVALID;
                }
            }
        }

        // IF BALANCE 0 or more - not check FEE
        boolean check_fee_balance = this.creator.getBalance(this.dcSet, FEE_KEY).a.b.compareTo(BigDecimal.ZERO) < 0;        
        int res = super.isValid(releaserReference, flags | (check_fee_balance? 0l : NOT_VALIDATE_FLAG_FEE));

        // FIRST PERSSONS INSERT as ADMIN
        boolean creator_admin = false;
        if (res == Transaction.CREATOR_NOT_PERSONALIZED) {
            long count = this.dcSet.getItemPersonMap().getLastKey();
            if (count < 20) {
                // FIRST Persons only by ME
                // FIRST Persons only by ADMINS
                for (String admin : BlockChain.GENESIS_ADMINS) {
                    if (this.creator.equals(admin)) {
                        creator_admin = true;
                        break;
                    }
                }
            }
            if (!creator_admin)
                return res;
        } else if (res == Transaction.NOT_ENOUGH_FEE) {
            // IF balance of FEE < 0 - ERROR
            if (this.creator.getBalance(this.dcSet, FEE_KEY).a.b.compareTo(BigDecimal.ZERO) < 0)
                return res;
        } else if (res != Transaction.VALIDATE_OK) {
            return res;
        }


        return VALIDATE_OK;

    }

    //VALIDATE


    //PROCESS/ORPHAN

    // GET only INVITED FEE
    @Override
    public int getInvitedFee() {
        int fee = this.fee.unscaledValue().intValue();

        if (true) {
            return fee >> BlockChain.FEE_INVITED_SHIFT;
        }

        long counter = this.dcSet.getItemPersonMap().getLastKey();
        if (counter < 10000l)
            return fee >> BlockChain.FEE_INVITED_SHIFT_FOR_INVITE;
        else if (counter < 100000l)
            return fee >> (BlockChain.FEE_INVITED_SHIFT_FOR_INVITE + 1);
        else if (counter < 1000000l)
            return fee >> (BlockChain.FEE_INVITED_SHIFT_FOR_INVITE + 2);
        else
            return fee >> BlockChain.FEE_INVITED_SHIFT;
    }

    //@Override
    public void process(Block block, boolean asPack) {
        //UPDATE CREATOR
        super.process(block, asPack);

        PersonHuman person = (PersonHuman) this.item;
        PublicKeyAccount maker = person.getOwner();
        byte[] makerBytes = maker.getPublicKey();
        this.dcSet.getIssuePersonMap().set(makerBytes, person.getKey());

        // for quick search public keys - use PUB_KEY from Person DATA
        if (person.isMustBeSigned()) {
            AddressTime_SignatureMap dbASmap = this.dcSet.getAddressTime_SignatureMap();
            String creatorAddress = maker.getAddress();
            if (!dbASmap.contains(creatorAddress)) {
                dbASmap.set(creatorAddress, this.signature);
            }
        }

        // EMITTE LIA
        this.creator.changeBalance(this.dcSet, false, AssetCls.LIA_KEY, BigDecimal.ONE, false);
        // SUBSTRACT from EMISSION (with minus)
        GenesisBlock.CREATOR.changeBalance(dcSet, true, AssetCls.LIA_KEY, BigDecimal.ONE, true);
        
    }

    //@Override
    public void orphan(boolean asPack) {
        //UPDATE CREATOR
        super.orphan(asPack);

        PersonHuman person = (PersonHuman) this.item;
        PublicKeyAccount maker = person.getOwner();
        byte[] makerBytes = maker.getPublicKey();
        this.dcSet.getIssuePersonMap().delete(makerBytes);

        // EMITTE LIA
        this.creator.changeBalance(this.dcSet, true, AssetCls.LIA_KEY, BigDecimal.ONE, false);
        // SUBSTRACT from EMISSION (with minus)
        GenesisBlock.CREATOR.changeBalance(dcSet, false, AssetCls.LIA_KEY, BigDecimal.ONE, true);
        
    }

    @Override
    public int calcBaseFee() {
        int fee = calcCommonFee() >> 1;
        return fee;
    }

}
