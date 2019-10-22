package org.erachain.core.item.persons;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.item.ItemCls;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.IssueItemMap;
import org.erachain.datachain.ItemAssetBalanceMap;
import org.erachain.datachain.ItemMap;
import org.erachain.settings.Settings;
import org.erachain.utils.ByteArrayUtils;
import org.erachain.utils.DateTimeFormat;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Set;

//import java.math.BigDecimal;
//import java.util.Arrays;
// import org.slf4j.LoggerFactory;
//import com.google.common.primitives.Ints;

//birthLatitude -90..90; birthLongitude -180..180
public abstract class PersonCls extends ItemCls {

    public static int MAX_IMAGE_LENGTH = BlockChain.DEVELOP_USE? 20480 : 28000;
    public static int MIN_IMAGE_LENGTH = 10240;

    public static final int HUMAN = 1;
    public static final int DOG = 2;
    public static final int CAT = 3;
    public static final int GENDER_LENGTH = 1;
    public static final int HEIGHT_LENGTH = 1;
    protected static final int BIRTHDAY_LENGTH = ItemCls.TIMESTAMP_LENGTH;
    protected static final int DEATHDAY_LENGTH = ItemCls.TIMESTAMP_LENGTH;
    protected static final int RACE_SIZE_LENGTH = 1;
    public static final int MAX_RACE_LENGTH = 256 ^ RACE_SIZE_LENGTH - 1;
    protected static final int LATITUDE_LENGTH = 4;
    protected static final int SKIN_COLOR_SIZE_LENGTH = 1;
    public static final int MAX_SKIN_COLOR_LENGTH = 256 ^ SKIN_COLOR_SIZE_LENGTH - 1;
    protected static final int EYE_COLOR_SIZE_LENGTH = 1;
    public static final int MAX_EYE_COLOR_LENGTH = 256 ^ EYE_COLOR_SIZE_LENGTH - 1;
    protected static final int HAIR_COLOR_SIZE_LENGTH = 1;
    public static final int MAX_HAIR_COLOR_LENGTH = 256 ^ HAIR_COLOR_SIZE_LENGTH - 1;
    protected static final int BASE_LENGTH = BIRTHDAY_LENGTH + DEATHDAY_LENGTH + GENDER_LENGTH + RACE_SIZE_LENGTH + LATITUDE_LENGTH * 2
            + SKIN_COLOR_SIZE_LENGTH + EYE_COLOR_SIZE_LENGTH + HAIR_COLOR_SIZE_LENGTH
            + HEIGHT_LENGTH;

    public static String[] GENDERS_LIST = {"Male", "Female", "-"};

    // already exist in super - protected String name; // First Name|Middle Name|Last Name
    protected long birthday; // timestamp
    protected long deathday; // timestamp
    protected byte gender; //
    protected String race;
    protected float birthLatitude;
    protected float birthLongitude;
    protected String skinColor; // First Name|Middle Name|Last Name
    protected String eyeColor; // First Name|Middle Name|Last Name
    protected String hairСolor; // First Name|Middle Name|Last Name
    protected byte height;

    public PersonCls(byte[] typeBytes, PublicKeyAccount owner, String name, long birthday, long deathday,
                     byte gender, String race, float birthLatitude, float birthLongitude,
                     String skinColor, String eyeColor, String hairСolor, byte height, byte[] icon, byte[] image, String description) {
        super(typeBytes, owner, name, icon, image, description);
        this.birthday = birthday;
        this.deathday = deathday;
        this.gender = gender;
        this.race = race;
        this.birthLatitude = birthLatitude;
        this.birthLongitude = birthLongitude;
        this.skinColor = skinColor;
        this.eyeColor = eyeColor;
        this.hairСolor = hairСolor;
        this.height = height;
    }

    public PersonCls(byte[] typeBytes, PublicKeyAccount owner, String name, String birthday, String deathday,
                     byte gender, String race, float birthLatitude, float birthLongitude,
                     String skinColor, String eyeColor, String hairСolor, byte height, byte[] icon, byte[] image, String description) {
        this(typeBytes, owner, name, 0, 0,
                gender, race, birthLatitude, birthLongitude,
                skinColor, eyeColor, hairСolor, (byte) height, icon, image, description);

        if (birthday.length() < 11) birthday += " 00:01:01";
        this.birthday = Timestamp.valueOf(birthday).getTime();

        if (deathday != null && deathday.length() < 11) deathday += " 00:01:01";
        this.deathday = deathday == null ? Long.MIN_VALUE : Timestamp.valueOf(deathday).getTime();
    }

    public PersonCls(int type, PublicKeyAccount owner, String name, long birthday, long deathday,
                     byte gender, String race, float birthLatitude, float birthLongitude,
                     String skinColor, String eyeColor, String hairСolor, byte height, byte[] icon, byte[] image, String description) {
        this(new byte[]{(byte) type}, owner, name, birthday, deathday,
                gender, race, birthLatitude, birthLongitude,
                skinColor, eyeColor, hairСolor, height, icon, image, description);
    }

    //GETTERS/SETTERS

    public int getItemType() {
        return ItemCls.PERSON_TYPE;
    }

    public String getItemTypeName() {
        return "person";
    }

    public long getBirthday() {
        return this.birthday;
    }

    public long getDeathday() {
        return this.deathday;
    }

    public String getBirthdayStr() {
        if (true)
            return DateTimeFormat.timestamptoString(this.birthday, Settings.getInstance().getBirthTimeFormat(), "UTC");
        else
            return DateTimeFormat.timestamptoString(birthday, Settings.getInstance().getBirthTimeFormat(), "UTC");
    }

    public String getDeathdayStr() {
        if (true)
            return DateTimeFormat.timestamptoString(this.deathday, Settings.getInstance().getBirthTimeFormat(), "UTC");
        else
            return DateTimeFormat.timestamptoString(deathday, Settings.getInstance().getBirthTimeFormat(), "UTC");
    }

    public byte getGender() {
        return this.gender;
    }

    public String getRace() {
        return this.race;
    }

    public float getBirthLatitude() {
        return this.birthLatitude;
    }

    public float getBirthLongitude() {
        return this.birthLongitude;
    }

    public String getSkinColor() {
        return this.skinColor;
    }

    public String getEyeColor() {
        return this.eyeColor;
    }

    public String getHairColor() {
        return this.hairСolor;
    }

    public int getHeight() {
        return Byte.toUnsignedInt(this.height);
    }

    public int getMAXimageLenght() {
        return this.MAX_IMAGE_LENGTH;
    }

    public int getMINimageLenght() {
        return this.MIN_IMAGE_LENGTH;
    }

    public boolean isAlive(long onThisTime) {

        if(this.deathday == Long.MIN_VALUE
                || this.deathday == Long.MAX_VALUE
                || this.deathday < this.birthday)
            return true;

        if (onThisTime > 0l
            && this.deathday > onThisTime)
            return true;

        return false;

    }

    public static BigDecimal getBalance(long personKey, long assetKey, int pos) {

        Set<String> addresses = DCSet.getInstance().getPersonAddressMap().getItems(personKey).keySet();

        ItemAssetBalanceMap map = DCSet.getInstance().getAssetBalanceMap();

        // тут переключение внутри цикла идет - так же слишком ресурсно
        BigDecimal sum = addresses.stream()
                .map((adr) -> Account.makeShortBytes(adr))
                .map((key) -> map.get(key, assetKey))
                .map((balances) -> {
                    switch (pos) {
                        case 1:
                            return balances.a.b;
                        case 2:
                            return balances.b.b;
                        case 3:
                            return balances.c.b;
                        case 4:
                            return balances.d.b;
                        case 5:
                            return balances.e.b;
                        default:
                            return BigDecimal.ZERO;
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum;

    }


    // DB
    public ItemMap getDBMap(DCSet db) {
        return db.getItemPersonMap();
    }

    public IssueItemMap getDBIssueMap(DCSet db) {
        return db.getIssuePersonMap();
    }

    // to BYTES
    public byte[] toBytes(boolean includeReference, boolean forOwnerSign) {

        byte[] data = super.toBytes(includeReference, forOwnerSign);

        // WRITE BIRTHDAY
        byte[] birthdayBytes = Longs.toByteArray(this.birthday);
        birthdayBytes = Bytes.ensureCapacity(birthdayBytes, BIRTHDAY_LENGTH, 0);
        data = Bytes.concat(data, birthdayBytes);

        // WRITE DEATHDAY
        byte[] deathdayBytes = Longs.toByteArray(this.deathday);
        deathdayBytes = Bytes.ensureCapacity(deathdayBytes, DEATHDAY_LENGTH, 0);
        data = Bytes.concat(data, deathdayBytes);

        // WRITE GENDER
        data = Bytes.concat(data, new byte[]{gender});

        // WRITE RACE
        byte[] raceBytes = this.race.getBytes(StandardCharsets.UTF_8);
        data = Bytes.concat(data, new byte[]{(byte) raceBytes.length});

        //WRITE RACE
        data = Bytes.concat(data, raceBytes);

        //WRITE BIRTH_LATITUDE
        byte[] birthLatitudeBytes = ByteArrayUtils.float2ByteArray(this.birthLatitude);
        //birthdayBytes = Bytes.ensureCapacity(birthdayBytes, LATITUDE_LENGTH, 0);
        data = Bytes.concat(data, birthLatitudeBytes);

        //WRITE BIRTH_LONGITUDE
        byte[] birthLongitudeBytes = ByteArrayUtils.float2ByteArray(this.birthLongitude);
        //birthdayBytes = Bytes.ensureCapacity(birthdayBytes, LATITUDE_LENGTH, 0);
        data = Bytes.concat(data, birthLongitudeBytes);

        //WRITE SKIN COLOR SIZE
        byte[] skinColorBytes = this.skinColor.getBytes(StandardCharsets.UTF_8);
        data = Bytes.concat(data, new byte[]{(byte) skinColorBytes.length});

        //WRITE SKIN COLOR
        data = Bytes.concat(data, skinColorBytes);

        //WRITE EYE COLOR SIZE
        byte[] eyeColorBytes = this.eyeColor.getBytes(StandardCharsets.UTF_8);
        data = Bytes.concat(data, new byte[]{(byte) eyeColorBytes.length});

        //WRITE EYE COLOR
        data = Bytes.concat(data, eyeColorBytes);

        //WRITE HAIR COLOR SIZE
        byte[] hairColorBytes = this.hairСolor.getBytes(StandardCharsets.UTF_8);
        data = Bytes.concat(data, new byte[]{(byte) hairColorBytes.length});

        //WRITE HAIR COLOR
        data = Bytes.concat(data, hairColorBytes);

        //WRITE HEIGHT
        data = Bytes.concat(data, new byte[]{this.height});

        return data;
    }

    //@Override
    public int getDataLength(boolean includeReference) {
        return super.getDataLength(includeReference)
                + BASE_LENGTH
                + this.race.getBytes(StandardCharsets.UTF_8).length
                + this.skinColor.getBytes(StandardCharsets.UTF_8).length
                + this.eyeColor.getBytes(StandardCharsets.UTF_8).length
                + this.hairСolor.getBytes(StandardCharsets.UTF_8).length;
    }

    //OTHER

    @Override
    public String toString(DCSet db) {
        long key = this.getKey(db);
        return "[" + (key < 1 ? "?" : key) + (this.typeBytes[0] == HUMAN ? "" : ("." + this.typeBytes[0])) + "]"
                + this.name // + "♥"
                ///+ DateTimeFormat.timestamptoString(birthday, "dd-MM-YY", "UTC")
                ;
    }

    @Override
    public String getShort(DCSet db) {
        long key = this.getKey(db);
        return "[" + (key < 1 ? "?" : key) + (this.typeBytes[0] == HUMAN ? "" : ("." + this.typeBytes[0])) + "]"
                + this.name.substring(0, Math.min(this.name.length(), 20)) //"♥"
                //+ DateTimeFormat.timestamptoString(birthday, "dd-MM-YY", "UTC")
                ;
    }

    @SuppressWarnings("unchecked")
    public JSONObject toJson() {

        JSONObject personJSON = super.toJson();

        // ADD DATA
        personJSON.put("birthday", this.birthday);
        personJSON.put("deathday", this.deathday);
        personJSON.put("gender", this.gender);
        personJSON.put("race", this.race);
        personJSON.put("birthLatitude", this.birthLatitude);
        personJSON.put("birthLongitude", this.birthLongitude);
        personJSON.put("skinColor", this.skinColor);
        personJSON.put("eyeColor", this.eyeColor);
        personJSON.put("hairColor", this.hairСolor);
        personJSON.put("height", Byte.toUnsignedInt(this.height));

        return personJSON;
    }

}
