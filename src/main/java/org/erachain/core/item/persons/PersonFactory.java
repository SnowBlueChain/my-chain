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

    public PersonCls parse(byte[] data, boolean includeReference) throws Exception {
        //READ TYPE
        int type = data[0];

        switch (type) {
            case PersonCls.HUMAN:

                //PARSE SIMPLE PLATE
                return PersonHuman.parse(data, includeReference);

            case PersonCls.DOG:

                //
                //return Person.parse(data, includeReference);
            case PersonCls.CAT:
                //
        }

        throw new Exception("Invalid Person type: " + type);
    }

}
