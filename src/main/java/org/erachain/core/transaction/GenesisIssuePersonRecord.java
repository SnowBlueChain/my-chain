package org.erachain.core.transaction;

import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.item.persons.PersonFactory;

import java.util.Arrays;

//import java.nio.charset.StandardCharsets;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
// import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
//import org.json.simple.JSONObject;
//import com.google.common.primitives.Bytes;
//import org.erachain.core.account.Account;
//import org.erachain.core.account.PublicKeyAccount;
//import org.erachain.core.crypto.Crypto;
//import org.erachain.core.item.ItemCls;

public class GenesisIssuePersonRecord extends GenesisIssue_ItemRecord {
    private static final byte TYPE_ID = (byte) GENESIS_ISSUE_PERSON_TRANSACTION;
    private static final String NAME_ID = "GENESIS Issue Person";

    public GenesisIssuePersonRecord(PersonCls person) {
        super(TYPE_ID, NAME_ID, person);
    }

    //PARSE CONVERT

    public static Transaction Parse(byte[] data) throws Exception {

        //CHECK IF WE MATCH BLOCK LENGTH
        if (data.length < BASE_LENGTH) {
            throw new Exception("Data does not match block length " + data.length);
        }

        // READ TYPE
        int position = SIMPLE_TYPE_LENGTH;

        //READ PERSON
        // read without reference
        PersonCls person = PersonFactory.getInstance().parse(Arrays.copyOfRange(data, position, data.length), false);

        return new GenesisIssuePersonRecord(person);

    }

    //@Override
    public int isValid(int asDeal, long flags) {

        int res = super.isValid(asDeal, flags);
        if (res != Transaction.VALIDATE_OK) return res;

        PersonCls person = (PersonCls) this.getItem();
        // birthLatitude -90..90; birthLongitude -180..180
        if (person.getBirthLatitude() > 90 || person.getBirthLatitude() < -90)
            return Transaction.ITEM_PERSON_LATITUDE_ERROR;
        if (person.getBirthLongitude() > 180 || person.getBirthLongitude() < -180)
            return Transaction.ITEM_PERSON_LONGITUDE_ERROR;
        if (person.getRace().length() < 1 || person.getRace().length() > 125) return Transaction.ITEM_PERSON_RACE_ERROR;
        if (person.getGender() < 0 || person.getGender() > 10) return Transaction.ITEM_PERSON_GENDER_ERROR;
        if (person.getSkinColor().length() < 1 || person.getSkinColor().length() > 255)
            return Transaction.ITEM_PERSON_SKIN_COLOR_ERROR;
        if (person.getEyeColor().length() < 1 || person.getEyeColor().length() > 255)
            return Transaction.ITEM_PERSON_EYE_COLOR_ERROR;
        if (person.getHairColor().length() < 1 || person.getHairColor().length() > 255)
            return Transaction.ITEM_PERSON_HAIR_COLOR_ERROR;
        //int ii = Math.abs(person.getHeight());
        if (Math.abs(person.getHeight()) < 40) return Transaction.ITEM_PERSON_HEIGHT_ERROR;

        return VALIDATE_OK;

    }


}
