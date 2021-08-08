package org.erachain.core.item.assets;

//import java.math.BigDecimal;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import org.erachain.core.BlockChain;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.lang.Lang;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * копия - в ней нет данных при парсинге - наполняется только после loadExtData()
 */
public class AssetUniqueSeriesCopy extends AssetUnique {

    private static final int TYPE_ID = UNIQUE_COPY;

    protected static final int BASE_LENGTH = Long.BYTES + 2 * Short.BYTES;

    private final long origKey;
    private final int total;
    private final int index;

    public AssetUniqueSeriesCopy(byte[] typeBytes, byte[] appData, PublicKeyAccount maker, String name, byte[] icon, byte[] image,
                                 String description, int assetType, long origKey, int total, int index) {
        super(typeBytes, appData, maker, name, icon, image, description, assetType);

        this.origKey = origKey;
        this.total = total;
        this.index = index;

    }

    public AssetUniqueSeriesCopy(byte[] appData, PublicKeyAccount maker, String name, byte[] icon, byte[] image,
                                 String description, long origKey, int total, int index) {
        this(new byte[]{TYPE_ID, 0}, appData, maker, name, icon, image, description, AssetCls.AS_NON_FUNGIBLE,
                origKey, total, index);

    }

    public AssetUniqueSeriesCopy(long origKey, AssetVenture foilAsset, int total, int index) {
        this(foilAsset.getAppData(),
                foilAsset.getMaker(),
                foilAsset.getName(),
                foilAsset.getIcon(),
                foilAsset.getImage(),
                foilAsset.getDescription(),
                origKey, total, index);
    }

    // GETTERS/SETTERS
    @Override
    public String getItemSubType() {
        return "series";
    }

    public long getOrigKey() {
        return origKey;
    }

    public int getTotal() {
        return total;
    }

    public int getIndex() {
        return index;
    }

    /**
     * Первая копия - содержит обертку полностью, все остальные только номера
     *
     * @param baseItem
     * @param foilAsset
     * @param origKey
     * @param total
     * @param index
     * @return
     */
    public static AssetUniqueSeriesCopy makeCopy(AssetCls baseItem, AssetCls foilAsset, long origKey, int total, int index) {
        byte[] appData = null;
        byte[] icon = null;
        byte[] image = null;
        String description = null;
        if (index == 1)
            return new AssetUniqueSeriesCopy(appData, foilAsset.getMaker(), baseItem.getName() + " #" + index + "/" + total,
                    icon, image, description, origKey, total, index);

        return new AssetUniqueSeriesCopy(appData, foilAsset.getMaker(), baseItem.getName() + " #" + index + "/" + total,
                icon, image, description, origKey, total, index);

    }

    //PARSE
    // includeReference - TRUE only for store in local DB
    public static AssetUniqueSeriesCopy parse(int forDeal, byte[] data, boolean includeReference) throws Exception {

        // READ TYPE
        byte[] typeBytes = Arrays.copyOfRange(data, 0, TYPE_LENGTH);
        int position = TYPE_LENGTH;

        //READ CREATOR
        byte[] makerBytes = Arrays.copyOfRange(data, position, position + MAKER_LENGTH);
        PublicKeyAccount maker = new PublicKeyAccount(makerBytes);
        position += MAKER_LENGTH;

        //READ NAME
        //byte[] nameLengthBytes = Arrays.copyOfRange(data, position, position + NAME_SIZE_LENGTH);
        //int nameLength = Ints.fromByteArray(nameLengthBytes);
        //position += NAME_SIZE_LENGTH;
        int nameLength = Byte.toUnsignedInt(data[position]);
        position++;

        if (nameLength < 1 || nameLength > MAX_NAME_LENGTH) {
            throw new Exception("Invalid name length: " + nameLength);
        }

        byte[] nameBytes = Arrays.copyOfRange(data, position, position + nameLength);
        String name = new String(nameBytes, StandardCharsets.UTF_8);
        position += nameLength;

        //READ ICON
        byte[] iconLengthBytes = Arrays.copyOfRange(data, position, position + ICON_SIZE_LENGTH);
        int iconLength = Ints.fromBytes((byte) 0, (byte) 0, iconLengthBytes[0], iconLengthBytes[1]);
        position += ICON_SIZE_LENGTH;

        if (iconLength < 0 || iconLength > MAX_ICON_LENGTH) {
            throw new Exception("Invalid icon length" + name + ": " + iconLength);
        }

        byte[] icon = Arrays.copyOfRange(data, position, position + iconLength);
        position += iconLength;

        //READ IMAGE
        byte[] imageLengthBytes = Arrays.copyOfRange(data, position, position + IMAGE_SIZE_LENGTH);
        int imageLength = Ints.fromByteArray(imageLengthBytes);
        position += IMAGE_SIZE_LENGTH;

        // TEST APP DATA
        boolean hasAppData = (imageLength & APP_DATA_MASK) != 0;
        if (hasAppData)
            // RESET LEN
            imageLength &= ~APP_DATA_MASK;

        if (imageLength < 0 || imageLength > MAX_IMAGE_LENGTH) {
            throw new Exception("Invalid image length" + name + ": " + imageLength);
        }

        byte[] image = Arrays.copyOfRange(data, position, position + imageLength);
        position += imageLength;

        byte[] appData;
        if (hasAppData) {
            // READ APP DATA
            int appDataLen = Ints.fromByteArray(Arrays.copyOfRange(data, position, position + APP_DATA_LENGTH));
            position += APP_DATA_LENGTH;

            appData = Arrays.copyOfRange(data, position, position + appDataLen);
            position += appDataLen;

        } else {
            appData = null;
        }

        //READ DESCRIPTION
        byte[] descriptionLengthBytes = Arrays.copyOfRange(data, position, position + DESCRIPTION_SIZE_LENGTH);
        int descriptionLength = Ints.fromByteArray(descriptionLengthBytes);
        position += DESCRIPTION_SIZE_LENGTH;

        if (descriptionLength > BlockChain.MAX_REC_DATA_BYTES) {
            throw new Exception("Invalid description length" + name + ": " + descriptionLength);
        }

        byte[] descriptionBytes = Arrays.copyOfRange(data, position, position + descriptionLength);
        String description = new String(descriptionBytes, StandardCharsets.UTF_8);
        position += descriptionLength;

        byte[] reference = null;
        long dbRef = 0;
        if (includeReference) {
            //READ REFERENCE
            reference = Arrays.copyOfRange(data, position, position + REFERENCE_LENGTH);
            position += REFERENCE_LENGTH;

            //READ SEQNO
            byte[] dbRefBytes = Arrays.copyOfRange(data, position, position + DBREF_LENGTH);
            dbRef = Longs.fromByteArray(dbRefBytes);
            position += DBREF_LENGTH;
        }

        //READ ASSET TYPE
        byte[] assetTypeBytes = Arrays.copyOfRange(data, position, position + ASSET_TYPE_LENGTH);
        int assetType = Ints.fromByteArray(assetTypeBytes);
        position += ASSET_TYPE_LENGTH;

        //READ ORIGINAL ASSET KEY
        byte[] origKeyBytes = Arrays.copyOfRange(data, position, position + Long.BYTES);
        long origKey = Longs.fromByteArray(origKeyBytes);
        position += Long.BYTES;

        //READ TOTAL
        byte[] totalBytes = Arrays.copyOfRange(data, position, position + Short.BYTES);
        int total = Shorts.fromByteArray(totalBytes);
        position += Short.BYTES;

        //READ INDEX
        byte[] indexBytes = Arrays.copyOfRange(data, position, position + Short.BYTES);
        int index = Shorts.fromByteArray(indexBytes);
        position += Short.BYTES;

        //RETURN
        AssetUniqueSeriesCopy uniqueCopy = new AssetUniqueSeriesCopy(typeBytes, appData, maker, name, icon, image,
                description, assetType, origKey, total, index);

        if (includeReference) {
            uniqueCopy.setReference(reference, dbRef);
        }

        return uniqueCopy;
    }

    @Override
    public byte[] toBytes(int forDeal, boolean includeReference, boolean forMakerSign) {

        byte[] parentData = super.toBytes(forDeal, includeReference, forMakerSign);

        byte[] data = new byte[BASE_LENGTH];

        System.arraycopy(Longs.toByteArray(origKey), 0, data, 0, Long.BYTES);
        System.arraycopy(Shorts.toByteArray((short) total), 0, data, 8, Short.BYTES);
        System.arraycopy(Shorts.toByteArray((short) index), 0, data, 10, Short.BYTES);

        return Bytes.concat(parentData, data);
    }

    public int getDataLength(boolean includeReference) {
        return super.getDataLength(includeReference) + BASE_LENGTH;
    }

    //OTHER

    public String makeHTMLView() {

        String text = super.makeHTMLHeadView();
        text += Lang.T("Series") + ":&nbsp;" + total + ", "
                + Lang.T("Index") + ":&nbsp;" + index + "<br>";
        text += super.makeHTMLFootView(true);

        return text;

    }

}
