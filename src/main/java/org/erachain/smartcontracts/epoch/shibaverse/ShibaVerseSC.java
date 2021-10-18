package org.erachain.smartcontracts.epoch.shibaverse;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.block.Block;
import org.erachain.core.crypto.Base58;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.assets.AssetVenture;
import org.erachain.core.transaction.RSend;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.datachain.SmartContractValues;
import org.erachain.smartcontracts.epoch.EpochSmartContract;
import org.erachain.webserver.WebResource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mapdb.Fun;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public class ShibaVerseSC extends EpochSmartContract {

    static Controller contr = Controller.getInstance();
    static Crypto crypto = Crypto.getInstance();

    static public final int ID = 1001;
    public static PublicKeyAccount MAKER = new PublicKeyAccount(Base58.encode(Longs.toByteArray(ID)));

    static public Account adminAddress = new Account("");

    static final Fun.Tuple2 INIT_KEY = new Fun.Tuple2(ID, "i");

    public ShibaVerseSC() {
        super(ID, MAKER);
    }

    private void init(DCSet dcSet, RSend commandTX, Account admin) {

        /**
         * main currency
         */
        AssetVenture gravita = new AssetVenture(null, maker, "GR", null, null,
                null, AssetCls.AS_INSIDE_ASSETS, 6, 0);
        gravita.setReference(commandTX.getSignature(), commandTX.getDBRef());

        //INSERT INTO DATABASE
        long gravitaKey = dcSet.getItemAssetMap().incrementPut(gravita);
        dcSet.getSmartContractValues().put(INIT_KEY, gravitaKey);

        boolean asOrphan = false;
        // TRANSFER GRAVITA to ADMIN
        admin.changeBalance(dcSet, asOrphan, false, gravitaKey,
                new BigDecimal("10000"), false, false, false);
        maker.changeBalance(dcSet, !asOrphan, false, gravitaKey,
                new BigDecimal("10000"), false, false, false);

    }

    /**
     * admin commands
     *
     * @param dcSet
     * @param block
     * @param commandTX
     * @param admin
     * @return
     */
    public boolean processAdminCommands(DCSet dcSet, Block block, RSend commandTX, Account admin) {
        byte[] data = commandTX.getData();
        if (data == null || data.length == 0) {
            SmartContractValues valuesMap = dcSet.getSmartContractValues();
            // CHECK if INITIALIZED
            if (!valuesMap.contains(INIT_KEY)) {
                init(dcSet, commandTX, admin);
            }
            return false;
        }

        String command = new String(data, StandardCharsets.UTF_8);

        return false;
    }

    @Override
    public boolean process(DCSet dcSet, Block block, Transaction transaction) {

        if (transaction instanceof RSend) {
            RSend rsend = (RSend) transaction;
            if (rsend.getCreator().equals(adminAddress)) {
                return processAdminCommands(dcSet, block, rsend, adminAddress);
            } else {
                if (rsend.hasAmount()) {
                    Long gravitaKey = (Long) dcSet.getSmartContractValues().get(INIT_KEY);
                    if (rsend.getAssetKey() == gravitaKey) {
                        String command = new String(rsend.getData(), StandardCharsets.UTF_8);
                        if (command.isEmpty() || "use".equals(command)) {
                            // рождение комет
                            SmartContractValues valuesMap = dcSet.getSmartContractValues();
                            PublicKeyAccount creator = transaction.getCreator();
                            int count = 4;
                            AssetVenture comet;
                            do {

                            } while (count-- > 0);
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean processByTime(DCSet dcSet, Block block, Transaction transaction) {
        if (transaction instanceof RSend) {
            RSend rsend = (RSend) transaction;
            if (rsend.getCreator().equals(adminAddress)) {
                return processAdminCommands(dcSet, block, rsend, adminAddress);
            } else {
                if (rsend.hasAmount()) {
                    Long gravitaKey = (Long) dcSet.getSmartContractValues().get(INIT_KEY);
                    if (rsend.getAssetKey() == gravitaKey) {
                        String command = new String(rsend.getData(), StandardCharsets.UTF_8);
                        if (command.isEmpty() || "use".equals(command)) {
                            // рождение комет
                            SmartContractValues valuesMap = dcSet.getSmartContractValues();
                            PublicKeyAccount creator = transaction.getCreator();
                            int count = 4;
                            AssetVenture comet;
                            do {

                            } while (count-- > 0);
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean orphanAdminCommands(DCSet dcSet, RSend transaction, Account admin) {
        return false;
    }

    @Override
    public boolean orphan(DCSet dcSet, Transaction transaction) {

        if (transaction instanceof RSend) {
            RSend rsend = (RSend) transaction;
            if (rsend.getCreator().equals(adminAddress)) {
                return orphanAdminCommands(dcSet, rsend, adminAddress);
            }
        }

        return false;
    }

    @Override
    public boolean orphanByTime(DCSet dcSet, Transaction transaction) {
        return false;
    }

    private static String[][][] imgsStr;

    {
        imgsStr = new String[][][]{
                new String[][]{
                        new String[]{"1050868", WebResource.TYPE_IMAGE.toString()},
                        new String[]{"1050867", WebResource.TYPE_IMAGE.toString()},
                },
                new String[][]{
                        new String[]{"1050864", WebResource.TYPE_IMAGE.toString()},
                        new String[]{"1050862", WebResource.TYPE_IMAGE.toString()},
                        null,
                        new String[]{"1050863", WebResource.TYPE_IMAGE.toString()},
                },
                new String[][]{
                        new String[]{"1050860", WebResource.TYPE_IMAGE.toString()},
                },
                new String[][]{
                        null,
                        new String[]{"1050866", WebResource.TYPE_IMAGE.toString()},
                },
                new String[][]{
                        new String[]{"1050857", WebResource.TYPE_IMAGE.toString()},
                        new String[]{"1050859", WebResource.TYPE_IMAGE.toString()},
                        new String[]{"1050858", WebResource.TYPE_IMAGE.toString()},
                },
                new String[][]{
                        new String[]{"1050856", WebResource.TYPE_IMAGE.toString()},
                        new String[]{"1050855", WebResource.TYPE_IMAGE.toString()},
                        null,
                        new String[]{"1050854", WebResource.TYPE_IMAGE.toString()},
                },
                null,
                new String[][]{
                        null,
                        null,
                        new String[]{"1050852", WebResource.TYPE_IMAGE.toString()},
                        null,
                        new String[]{"1050851", WebResource.TYPE_IMAGE.toString()},
                },
        };
    }

    static int confirms = 10;
    static int deploy_period = 3;

    public static String getImageURL(AssetCls asset) {

        JSONArray arrayJson = new JSONArray();
        JSONObject item;


        int height = Transaction.parseHeightDBRef(asset.getDBref());

        if (contr.getMyHeight() < height + deploy_period + confirms) {
            item = new JSONObject();
            item.put("url", "/apiasset/image/1050869");
            item.put("type", WebResource.TYPE_IMAGE.toString());
            arrayJson.add(item);
            return arrayJson.toJSONString();
        }

        Block.BlockHead blockHead = DCSet.getInstance().getBlocksHeadsMap().get(height + deploy_period);

        byte[] hash = blockHead.signature;
        byte[] hash2 = Ints.toByteArray((int) asset.getKey());
        System.arraycopy(hash2, 0, hash, 0, hash2.length);

        hash = crypto.digest(hash);
        int slot = 0;
        int slotRare;
        int slotRareLvl;

        String[][] slotArray;
        do {
            slotRare = Ints.fromBytes((byte) 0, (byte) 0, hash[slot << 1], hash[(slot << 1) + 1]);
            if ((slotRare >> 11) == 0) {
                slotRareLvl = 5;
            } else if ((slotRare >> 12) == 0) {
                slotRareLvl = 4;
            } else if ((slotRare >> 13) == 0) {
                slotRareLvl = 3;
            } else if ((slotRare >> 14) == 0) {
                slotRareLvl = 2;
            } else if ((slotRare >> 15) == 0) {
                slotRareLvl = 1;
            } else {
                slotRareLvl = 0;
            }

            slotArray = imgsStr[slot];
            if (slotArray == null)
                continue;

            if (slotArray.length <= slotRareLvl) {
                slotRareLvl = slotArray.length - 1;
            }

            String[] itemArray;
            do {
                itemArray = slotArray[slotRareLvl];
            } while (itemArray == null && slotRareLvl-- > 0);

            if (itemArray == null)
                continue;

            item = new JSONObject();
            item.put("url", "/apiasset/image/" + itemArray[0]);
            item.put("type", itemArray[1]);
            arrayJson.add(item);

        } while (slot++ < 7);

        item = new JSONObject();
        item.put("url", "/apiasset/image/1050853");
        item.put("type", WebResource.TYPE_IMAGE.toString());
        arrayJson.add(item);
        item = new JSONObject();
        item.put("url", "/apiasset/image/1050865");
        item.put("type", WebResource.TYPE_IMAGE.toString());
        arrayJson.add(item);

        return arrayJson.toJSONString();

    }

}
