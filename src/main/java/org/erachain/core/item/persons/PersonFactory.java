package org.erachain.core.item.persons;

//import com.google.common.primitives.Longs;

public class PersonFactory {

    private static PersonFactory instance;

    private PersonFactory() {

    }

    public static PersonFactory getInstance() {
        if (instance == null) {
            instance = new PersonFactory();
        }

        return instance;
    }

    public PersonCls parse(int forDeal, byte[] data, boolean includeReference) throws Exception {
        //READ TYPE
        int type = data[0];

        switch (type) {
            case PersonCls.HUMAN:
                return PersonHuman.parse(data, includeReference, forDeal);

            case PersonCls.UNION:
                return PersonsUnion.parse(data, includeReference, forDeal);
        }

        throw new Exception("Invalid Person type: " + type);
    }

}
