package core.transaction;

import api.BlogPostResource;
import com.google.common.base.Charsets;
import core.account.Account;
import core.account.PublicKeyAccount;
import core.block.Block;
import core.crypto.Base58;
import core.naming.Name;
import core.payment.Payment;
import core.web.blog.BlogEntry;
import datachain.DCSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import utils.BlogUtils;
import utils.StorageUtils;

import java.math.BigDecimal;
import java.util.*;

//import com.google.common.primitives.Longs;

/**
 * @deprecated
 */
public abstract class ArbitraryTransaction extends Transaction {

    protected static final byte TYPE_ID = (byte) ARBITRARY_TRANSACTION;
    private static final String NAME_ID = "OLD: Arbitrary";
    static Logger LOGGER = Logger.getLogger(ArbitraryTransaction.class.getName());
    protected int service;
    protected byte[] data;
    protected List<Payment> payments;

    public ArbitraryTransaction(byte[] typeBytes, PublicKeyAccount creator, byte feePow, long timestamp, Long reference) {
        super(typeBytes, NAME_ID, creator, feePow, timestamp, reference);
    }

    public ArbitraryTransaction(byte[] typeBytes, PublicKeyAccount creator, byte feePow, long timestamp, Long reference, byte[] signature) {
        super(typeBytes, NAME_ID, creator, feePow, timestamp, reference, signature);
    }
	/*
	public ArbitraryTransaction(PublicKeyAccount creator, byte feePow, long timestamp, byte[] reference) {
		this(new byte[]{TYPE_ID, 0, 0, 0}, creator, feePow, timestamp, reference);
	}
	 */

    // GETTERS/SETTERS
    //public static String getName() { return "OLD: Arbitrary"; }

    public static Transaction Parse(byte[] data) throws Exception {
        // READ TIMESTAMP
        //byte[] timestampBytes = Arrays.copyOfRange(data, 0, TIMESTAMP_LENGTH);
        //long timestamp = Longs.fromByteArray(timestampBytes);

        return ArbitraryTransactionV3.Parse(data);
    }

    public int getService() {
        return this.service;
    }

    public byte[] getData() {
        return this.data;
    }

    // PARSE CONVERT

    public List<Payment> getPayments() {
        if (this.payments != null) {
            return this.payments;
        } else {
            return new ArrayList<Payment>();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJson() {
        // GET BASE
        JSONObject transaction = this.getJsonBase();

        // ADD CREATOR/SERVICE/DATA
        transaction.put("creator", this.creator.getAddress());
        transaction.put("service", this.service);
        transaction.put("data", Base58.encode(this.data));

        JSONArray payments = new JSONArray();
        for (Payment payment : this.payments) {
            payments.add(payment.toJson());
        }

        if (!payments.isEmpty()) {
            transaction.put("payments", payments);
        }

        return transaction;
    }

    @Override
    public PublicKeyAccount getCreator() {
        return this.creator;
    }

    @Override
    public HashSet<Account> getInvolvedAccounts() {
        HashSet<Account> accounts = new HashSet<>();

        accounts.add(this.creator);
        accounts.addAll(this.getRecipientAccounts());

        return accounts;
    }


    @Override
    public HashSet<Account> getRecipientAccounts() {
        HashSet<Account> accounts = new HashSet<>();

        for (Payment payment : this.payments) {
            accounts.add(payment.getRecipient());
        }

        return accounts;
    }

    @Override
    public boolean isInvolved(Account account) {
        String address = account.getAddress();

        for (Account involved : this.getInvolvedAccounts()) {
            if (address.equals(involved.getAddress())) {
                return true;
            }
        }

        return false;
    }

    //@Override
    @Override
    public BigDecimal getAmount(Account account) {
        BigDecimal amount = BigDecimal.ZERO;
        String address = account.getAddress();

        // IF SENDER
        if (address.equals(this.creator.getAddress())) {
            amount = amount.subtract(this.fee);
        }

        // CHECK PAYMENTS
        for (Payment payment : this.payments) {
            // IF ERA ASSET
            if (payment.getAsset() == FEE_KEY) {
                // IF SENDER
                if (address.equals(this.creator.getAddress())) {
                    amount = amount.subtract(payment.getAmount());
                }

                // IF RECIPIENT
                if (address.equals(payment.getRecipient().getAddress())) {
                    amount = amount.add(payment.getAmount());
                }
            }
        }

        return amount;
    }

    //@Override
    public Map<String, Map<Long, BigDecimal>> getAssetAmount() {
        Map<String, Map<Long, BigDecimal>> assetAmount = new LinkedHashMap<>();

        assetAmount = subAssetAmount(assetAmount, this.creator.getAddress(), FEE_KEY, this.fee);

        for (Payment payment : this.payments) {
            assetAmount = subAssetAmount(assetAmount, this.creator.getAddress(), payment.getAsset(), payment.getAmount());
            assetAmount = addAssetAmount(assetAmount, payment.getRecipient().getAddress(), payment.getAsset(), payment.getAmount());
        }

        return assetAmount;
    }

    // PROCESS/ORPHAN
    //@Override
    @Override
    public void process(Block block, int asDeal) {


        try {
            // NAME STORAGE UPDATE
            if (this.getService() == 10) {
                StorageUtils.processUpdate(getData(), signature, this.getCreator(), this.dcSet);
            } else if (this.getService() == 777) {
                addToBlogMapOnDemand();
            } else if (this.getService() == BlogUtils.COMMENT_SERVICE_ID) {
                addToCommentMapOnDemand();
            }
        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
        }

        // UPDATE CREATOR
        super.process(block, asDeal);

        // PROCESS PAYMENTS
        for (Payment payment : this.getPayments()) {
            payment.process(this.getCreator(), this.dcSet);

            // UPDATE REFERENCE OF RECIPIENT
            if (false && payment.getRecipient().getLastTimestamp(this.dcSet) == null) {
                payment.getRecipient().setLastTimestamp(this.timestamp, this.dcSet);
            }
        }
    }

    //@Override
    @Override
    public void orphan(int asDeal) {

        // NAME STORAGE UPDATE ORPHAN
        // if (service == 10) {
        // StorageUtils.processOrphan(getData(), signature, db);
        // // BLOGPOST?
        // } else {
        // removeFromBlogMapOnDemand(db);
        // }

        // UPDATE CREATOR
        super.orphan(asDeal);

        // ORPHAN PAYMENTS
        for (Payment payment : this.getPayments()) {
            payment.orphan(this.getCreator(), this.dcSet);

            // UPDATE REFERENCE OF RECIPIENT
            if (false && payment.getRecipient().getLastTimestamp(this.dcSet).equals(this.timestamp)) {
                payment.getRecipient().removeLastTimestamp(this.dcSet);
            }
        }
    }

    public void addToCommentMapOnDemand() {

        if (getService() == BlogUtils.COMMENT_SERVICE_ID) {
            byte[] data = getData();
            String string = new String(data, Charsets.UTF_8);

            JSONObject jsonObject = (JSONObject) JSONValue.parse(string);
            if (jsonObject != null) {

                String signatureOfCommentOpt = (String) jsonObject
                        .get(BlogPostResource.DELETE_KEY);

                // CHECK IF THIS IS A DELETE OR CREATE OF A COMMENT
                if (StringUtils.isNotBlank(signatureOfCommentOpt)) {
                    BlogEntry commentEntryOpt = BlogUtils
                            .getCommentBlogEntryOpt(signatureOfCommentOpt);

                    if (commentEntryOpt != null) {
                        String creatorOfDeleteTX = getCreator().getAddress();
                        String creatorOfEntryToDelete = commentEntryOpt
                                .getCreator();

                        // OWNER IS DELETING OWN POST?
                        if (creatorOfDeleteTX.equals(creatorOfEntryToDelete)) {
                            deleteCommentInternal(this.dcSet, commentEntryOpt);
                            // BLOGOWNER IS DELETING POST
                        } else if (
                                commentEntryOpt.getBlognameOpt() != null) {
                            Name name = this.dcSet.getNameMap().get(
                                    commentEntryOpt.getBlognameOpt());
                            if (name != null
                                    && name.getOwner().getAddress()
                                    .equals(creatorOfDeleteTX)) {
                                deleteCommentInternal(this.dcSet, commentEntryOpt);

                            }
                        }

                    }
                } else {
                    String post = (String) jsonObject
                            .get(BlogPostResource.POST_KEY);

                    String postid = (String) jsonObject
                            .get(BlogPostResource.COMMENT_POSTID_KEY);

                    // DOES POST MET MINIMUM CRITERIUM?
                    if (StringUtils.isNotBlank(post)
                            && StringUtils.isNotBlank(postid)) {

                        this.dcSet.getPostCommentMap().add(Base58.decode(postid),
                                getSignature());
                        this.dcSet.getCommentPostMap().add(getSignature(),
                                Base58.decode(postid));
                    }
                }

            }

        }

    }

    private void addToBlogMapOnDemand() {

        if (getService() == 777) {
            byte[] data = this.getData();
            String string = new String(data, Charsets.UTF_8);

            JSONObject jsonObject = (JSONObject) JSONValue.parse(string);
            if (jsonObject != null) {
                String post = (String) jsonObject
                        .get(BlogPostResource.POST_KEY);

                String blognameOpt = (String) jsonObject
                        .get(BlogPostResource.BLOGNAME_KEY);

                String share = (String) jsonObject
                        .get(BlogPostResource.SHARE_KEY);

                String delete = (String) jsonObject
                        .get(BlogPostResource.DELETE_KEY);

                String author = (String) jsonObject
                        .get(BlogPostResource.AUTHOR);

                boolean isShare = false;
                if (StringUtils.isNotEmpty(share)) {
                    isShare = true;
                    byte[] sharedSignature = Base58.decode(share);
                    if (sharedSignature != null) {
                        this.dcSet.getSharedPostsMap().add(sharedSignature, author);
                    }
                }

                if (StringUtils.isNotEmpty(delete)) {
                    BlogEntry blogEntryOpt = BlogUtils.getBlogEntryOpt(delete);

                    if (blogEntryOpt != null) {
                        String creatorOfDeleteTX = getCreator().getAddress();
                        String creatorOfEntryToDelete = blogEntryOpt
                                .getCreator();
                        if (blogEntryOpt != null) {

                            // OWNER IS DELETING OWN POST?
                            if (creatorOfDeleteTX
                                    .equals(creatorOfEntryToDelete)) {
                                deleteInternal(isShare, blogEntryOpt);
                                // BLOGOWNER IS DELETING POST
                            } else if (author != null
                                    && blogEntryOpt.getBlognameOpt() != null) {
                                Name name = this.dcSet.getNameMap().get(
                                        blogEntryOpt.getBlognameOpt());
                                if (name != null
                                        && name.getOwner().getAddress()
                                        .equals(creatorOfDeleteTX)) {
                                    deleteInternal(isShare, blogEntryOpt);
                                }
                            }

                        }
                    }

                } else {

                    // DOES POST MET MINIMUM CRITERIUM?
                    if (StringUtils.isNotBlank(post)) {

                        // Shares won't be hashtagged!
                        if (!isShare) {
                            List<String> hashTags = BlogUtils.getHashTags(post);
                            for (String hashTag : hashTags) {
                                this.dcSet.getHashtagPostMap().add(hashTag,
                                        getSignature());
                            }
                        }

                        this.dcSet.getBlogPostMap().add(blognameOpt, getSignature());
                    }
                }

            }
        }
    }

    public void deleteInternal(boolean isShare, BlogEntry blogEntryOpt) {
        if (isShare) {
            byte[] sharesignature = Base58.decode(blogEntryOpt
                    .getShareSignatureOpt());
            this.dcSet.getBlogPostMap().remove(blogEntryOpt.getBlognameOpt(),
                    sharesignature);
            this.dcSet.getSharedPostsMap().remove(sharesignature,
                    blogEntryOpt.getNameOpt());
        } else {
            // removing from hashtagmap
            List<String> hashTags = BlogUtils.getHashTags(blogEntryOpt
                    .getDescription());
            for (String hashTag : hashTags) {
                this.dcSet.getHashtagPostMap().remove(hashTag,
                        Base58.decode(blogEntryOpt.getSignature()));
            }
            this.dcSet.getBlogPostMap().remove(blogEntryOpt.getBlognameOpt(),
                    Base58.decode(blogEntryOpt.getSignature()));
        }
    }

    public void deleteCommentInternal(DCSet db, BlogEntry commentEntry) {

        byte[] signatureOfComment = Base58.decode(commentEntry.getSignature());
        byte[] signatureOfBlogPostOpt = db.getCommentPostMap().get(
                Base58.decode(commentEntry.getSignature()));
        // removing from hashtagmap

        if (signatureOfBlogPostOpt != null) {
            db.getPostCommentMap().remove(signatureOfBlogPostOpt,
                    signatureOfComment);
            db.getCommentPostMap().remove(signatureOfComment);

        }
    }

    // TODO implement readd delete if orphaned!
    @SuppressWarnings("unused")
    private void removeFromBlogMapOnDemand(DCSet db) {
        if (getService() == 777) {
            byte[] data = getData();
            String string = new String(data, Charsets.UTF_8);

            JSONObject jsonObject = (JSONObject) JSONValue.parse(string);
            if (jsonObject != null) {
                String blognameOpt = (String) jsonObject
                        .get(BlogPostResource.BLOGNAME_KEY);

                String share = (String) jsonObject
                        .get(BlogPostResource.SHARE_KEY);

                String author = (String) jsonObject
                        .get(BlogPostResource.AUTHOR);

                if (StringUtils.isNotEmpty(share)) {
                    byte[] sharedSignature = Base58.decode(share);
                    if (sharedSignature != null) {
                        db.getSharedPostsMap().remove(sharedSignature, author);
                    }
                }

                db.getBlogPostMap().remove(blognameOpt, getSignature());

            }
        }
    }

    @Override
    public int calcBaseFee() {
        return calcCommonFee();
    }
}
