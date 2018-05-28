package core.item.assets;

public class AssetFactory {

    private static AssetFactory instance;

    private AssetFactory() {

    }

    public static AssetFactory getInstance() {
        if (instance == null) {
            instance = new AssetFactory();
        }

        return instance;
    }

    public AssetCls parse(byte[] data, boolean includeReference) throws Exception {
        //READ TYPE
        int type = data[0];

        switch (type) {
            case AssetCls.VENTURE:

                //PARSE UPDATE NAME TRANSACTION
                return AssetVenture.parse(data, includeReference);

            case AssetCls.UNIQUE:

                //PARSE PAYMENT TRANSACTION
                return AssetUnique.parse(data, includeReference);

            case AssetCls.NAME:

                //PARSE REGISTER NAME TRANSACTION
                //return RegisterNameTransaction.Parse(data, includeReference);

        }

        throw new Exception("Invalid asset type: " + type);
    }

}
