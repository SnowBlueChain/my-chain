package core.transaction;

import com.google.common.primitives.Longs;
import core.account.PublicKeyAccount;
import core.item.polls.PollCls;
import core.item.polls.PollFactory;

import java.util.Arrays;

public class IssuePollRecord extends Issue_ItemRecord {
    private static final byte TYPE_ID = (byte) ISSUE_POLL_TRANSACTION;
    private static final String NAME_ID = "Issue Poll";

    public IssuePollRecord(byte[] typeBytes, PublicKeyAccount creator, PollCls poll, byte feePow, long timestamp, Long reference) {
        super(typeBytes, NAME_ID, creator, poll, feePow, timestamp, reference);
    }

    public IssuePollRecord(byte[] typeBytes, PublicKeyAccount creator, PollCls poll, byte feePow, long timestamp, Long reference, byte[] signature) {
        super(typeBytes, NAME_ID, creator, poll, feePow, timestamp, reference, signature);
    }

    public IssuePollRecord(byte[] typeBytes, PublicKeyAccount creator, PollCls poll, byte[] signature) {
        super(typeBytes, NAME_ID, creator, poll, (byte) 0, 0l, null, signature);
    }

    public IssuePollRecord(PublicKeyAccount creator, PollCls poll, byte feePow, long timestamp, Long reference, byte[] signature) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, poll, feePow, timestamp, reference, signature);
    }

    public IssuePollRecord(PublicKeyAccount creator, PollCls poll, byte[] signature) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, poll, (byte) 0, 0l, null, signature);
    }

    public IssuePollRecord(PublicKeyAccount creator, PollCls poll, byte feePow, long timestamp, Long reference) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, poll, feePow, timestamp, reference);
    }

    public IssuePollRecord(PublicKeyAccount creator, PollCls poll) {
        this(new byte[]{TYPE_ID, 0, 0, 0}, creator, poll, (byte) 0, 0l, null);
    }

    //GETTERS/SETTERS
    //public static String getName() { return "Issue Poll"; }

    public static Transaction Parse(byte[] data, int asDeal) throws Exception {

        int test_len = BASE_LENGTH;
        if (asDeal == Transaction.FOR_MYPACK) {
            test_len -= Transaction.TIMESTAMP_LENGTH + Transaction.FEE_POWER_LENGTH;
        } else if (asDeal == Transaction.FOR_PACK) {
            test_len -= Transaction.TIMESTAMP_LENGTH;
        } else if (asDeal == Transaction.FOR_DB_RECORD) {
            test_len += Transaction.FEE_POWER_LENGTH;
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

        //READ POLL
        // poll parse without reference - if is = signature
        PollCls poll = PollFactory.getInstance().parse(Arrays.copyOfRange(data, position, data.length), false);
        position += poll.getDataLength(false);

        if (asDeal > Transaction.FOR_MYPACK) {
            return new IssuePollRecord(typeBytes, creator, poll, feePow, timestamp, reference, signatureBytes);
        } else {
            return new IssuePollRecord(typeBytes, creator, poll, signatureBytes);
        }
    }

    //PARSE CONVERT

    // NOT GENESIS ISSUE STRT FRON NUM
    protected long getStartKey() {
        return 0l;
    }

    //PROCESS/ORPHAN

	/*
	@Override
	public int calcBaseFee() {
		return 10 * (calcCommonFee() + BlockChain.FEE_PER_BYTE * 1000);
	}
	*/

}
