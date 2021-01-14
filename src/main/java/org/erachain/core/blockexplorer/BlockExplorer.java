package org.erachain.core.blockexplorer;

import org.apache.commons.net.util.Base64;
import org.erachain.at.ATTransaction;
import org.erachain.controller.Controller;
import org.erachain.core.BlockChain;
import org.erachain.core.account.Account;
import org.erachain.core.block.Block;
import org.erachain.core.block.GenesisBlock;
import org.erachain.core.crypto.Base58;
import org.erachain.core.crypto.Crypto;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.assets.Order;
import org.erachain.core.item.assets.Trade;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.item.polls.PollCls;
import org.erachain.core.item.statuses.StatusCls;
import org.erachain.core.item.templates.TemplateCls;
import org.erachain.core.payment.Payment;
import org.erachain.core.transaction.*;
import org.erachain.database.FilteredByStringArray;
import org.erachain.datachain.*;
import org.erachain.dbs.DBTab;
import org.erachain.dbs.IteratorCloseable;
import org.erachain.gui.models.PeersTableModel;
import org.erachain.lang.Lang;
import org.erachain.lang.LangFile;
import org.erachain.settings.Settings;
import org.erachain.utils.M_Integer;
import org.erachain.utils.NumberAsString;
import org.erachain.utils.Pair;
import org.erachain.utils.ReverseComparator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mapdb.Fun;
import org.mapdb.Fun.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;

// 30/03 ++ asset - Trans_Amount

/**
 * В запросе q - фильтр поиска, а в search - тип данных где ищем, но тут на сервере этот тип может изменитсья в ответе
 *
 * В ответе параметр:<br>
 *     - type - задает тип полученных данных - по нему в Ajax запросах выбираем что делать дальше с полученными данными<br>
 *     - search - задает выбор у поископого элемента searchID, необходимо задавать если он отличается от значения в type<br>
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BlockExplorer {

    public static final int pageSize = 25;
    private static final String LANG_DEFAULT = "en";
    private static final Logger logger = LoggerFactory.getLogger(BlockExplorer.class);
    private volatile static BlockExplorer blockExplorer;
    private JSONObject langObj;
    private Locale local = new Locale("ru", "RU"); // Date format
//    private DateFormat df = DateFormat.getDateInstance(DateFormat.DATE_FIELD, local); // for
    private DCSet dcSet;
    private LinkedHashMap output;

    public static BlockExplorer getInstance() {
        if (blockExplorer == null) {
            blockExplorer = new BlockExplorer();
            blockExplorer.dcSet = DCSet.getInstance();
        }
        return blockExplorer;
    }

    public Map getOutput() {
        return output;
    }

    private long checkAndGetLongParam(UriInfo info, long param, String name) {
        if (info.getQueryParameters().containsKey(name)
                && !info.getQueryParameters().getFirst(name).equals("")
                && !info.getQueryParameters().getFirst(name).equals("undefined")) {
            try {
                param = Long.valueOf((info.getQueryParameters().getFirst(name)));
            } catch (Exception e) {
                logger.debug(info.getQueryParameters().toString());
            }
        }
        return param;
    }

    public static String get_Lang(JSONObject langObj) {
        if (langObj == null)
            return "&lang=en";
        return "&lang=" + langObj.get("_lang_ISO_");

    }

    public void makePage(Class type, int start, int pageSize,
                         Map output, JSONObject langObj) {

        DBTab map = dcSet.getMap(type);
        ExplorerJsonLine element;
        int size = map.size();

        if (start < 1 || start > size && size > 0) {
            start = size;
        }
        output.put("start", start);

        int key = start;
        JSONArray array = new JSONArray();

        while (key > start - pageSize && key > 0) {
            element = (ExplorerJsonLine) map.get(key--);
            if (element != null) {
                array.add(element.jsonForExplorerPage(langObj));
            }
        }

        output.put("pageItems", array);
        output.put("pageSize", pageSize);
        output.put("listSize", map.size());
        output.put("lastNumber", key);

    }

    /**
     * Для списков с ключом LONG - для сущностей всех например
     *
     * @param type
     * @param start    LONG
     * @param pageSize
     * @param output
     * @param langObj
     */
    public void makePage(Class type, long start, int pageSize,
                         Map output, JSONObject langObj) {

        DBTab map = dcSet.getMap(type);
        ExplorerJsonLine element;
        long size = map.size();

        if (start < 1 || start > size && size > 0) {
            start = size;
        }
        output.put("start", start);

        long key = start;
        JSONArray array = new JSONArray();

        while (key > start - pageSize && key > 0) {
            element = (ExplorerJsonLine) map.get(key--);
            if (element != null) {
                array.add(element.jsonForExplorerPage(langObj));
            }

        }

        output.put("pageItems", array);
        output.put("pageSize", pageSize);
        output.put("listSize", map.size());
        output.put("lastNumber", key);

    }

    /**
     * Для списков с ключом INT - для блоков
     *
     * @param type
     * @param keys
     * @param start
     * @param pageSize
     * @param output
     * @param langObj
     */
    public void makePage(Class type, List keys, int start, int pageSize,
                         Map output, JSONObject langObj) {

        int size = keys.size();

        if (start < 1 || start > size && size > 0) {
            start = size;
        }
        output.put("start", start);

        int index = start;
        JSONArray array = new JSONArray();

        if (size > 0) {
            DBTab map = dcSet.getMap(type);
            ExplorerJsonLine element;

            while (index > start - pageSize && index > 0) {
                element = (ExplorerJsonLine) map.get(keys.get(--index));
                if (element != null) {
                    array.add(element.jsonForExplorerPage(langObj));
                }
            }
        }

        output.put("pageItems", array);
        output.put("pageSize", pageSize);
        output.put("listSize", keys.size());

    }

    public Map jsonQueryPages(Class type, int start, int pageSize) {
        Map result = new LinkedHashMap();
        AdderHeadInfo.addHeadInfoCap(type, result, dcSet, langObj);
        makePage(type, start, pageSize, result, langObj);
        return result;
    }

    public Map jsonQueryPages(Class type, long start, int pageSize) {
        Map result = new LinkedHashMap();
        AdderHeadInfo.addHeadInfoCap(type, result, dcSet, langObj);
        makePage(type, start, pageSize, result, langObj);
        return result;
    }

    public Map jsonQuerySearchPages(UriInfo info, Class type, String search, int start, int pageSize) throws WrongSearchException, Exception {
        //Результирующий сортированный в порядке добавления словарь(map)
        Map result = new LinkedHashMap();
        List<Object> keys = new ArrayList();
        //Добавить шапку в JSON. Для интернационализации названий - происходит перевод соответствующих элементов.
        //В зависимости от выбранного языка(ru,en)
        AdderHeadInfo.addHeadInfoCap(type, result, dcSet, langObj);

        DBTab map = dcSet.getMap(type);

        try {
            //Если в строке ввели число
            if (search.matches("\\d+")) {
                Object key;
                if (type == Block.class) {
                    key = Integer.valueOf(search);
                } else {
                    key = Long.valueOf(search);
                }
                if (map.contains(key)) {
                    //Элемент найден - добавляем его
                    keys.add(key);
                    //Не отображать для одного элемента навигацию и пагинацию
                    result.put("notDisplayPages", "true");
                }
            } else {
                //Поиск элементов по имени
                String fromWord = null; // TODO нужно задавать иначе не найдет
                keys = ((FilteredByStringArray) map).getKeysByFilterAsArray(search, fromWord, Transaction.parseDBRef(info.getQueryParameters().getFirst("fromID")),
                        start, pageSize, false);
            }
        } catch (Exception e) {
            logger.error("Wrong search while process assets... ", e.getMessage());
            throw new WrongSearchException();
        }
        if (keys == null || keys.isEmpty()) {
            logger.info("Wrong search while process assets... ");
            throw new WrongSearchException();
        }

        makePage(type, keys, start, pageSize, result, langObj);

        return result;
    }

    @SuppressWarnings("static-access")
    public Map jsonQueryMain(UriInfo info) throws WrongSearchException, Exception {

        output = new LinkedHashMap();

        Stopwatch stopwatchAll = new Stopwatch();
        long start = 0;
        start = checkAndGetLongParam(info, start, "start");

        //lang
        String langISO;
        if (!info.getQueryParameters().containsKey("lang")) {
            langISO = LANG_DEFAULT;
        } else {
            langISO = info.getQueryParameters().getFirst("lang");
        }

        langObj = Lang.getInstance().getLangJson(langISO);

        Map langList = new LinkedHashMap();
        for (String iso : Lang.getInstance().getLangListAvailable().keySet()) {
            LangFile langFile = Lang.getInstance().getLangFile(iso);
            Map langPars = new LinkedHashMap();
            langPars.put("ISO", langFile.getISO());
            langPars.put("name", langFile.getName());
            langList.put(langFile.getISO(), langPars);
        }

        output.put("Lang", langList);
        //Основное меню. заголовки и их перевод на выбранный язык
        output.put("id_home2", Lang.T("Blocks", langObj));
        output.put("id_menu_top_100", Lang.T("Top 100 Richest", langObj));
        output.put("id_menu_percons", Lang.T("Persons", langObj));
        output.put("id_menu_pals_asset", Lang.T("Polls", langObj));
        output.put("id_menu_assets", Lang.T("Assets", langObj));
        output.put("id_menu_aTs", Lang.T("ATs", langObj));
        output.put("id_menu_transactions", Lang.T("Transactions", langObj));
        output.put("id_menu_exchange", Lang.T("Exchange", langObj));
        output.put("id_menu_order", Lang.T("Order", langObj));

        //информация о последнем блоке
        output.put("lastBlock", jsonLastBlock());

        if (info.getQueryParameters().containsKey("q")) {
            if (info.getQueryParameters().containsKey("search")) {
                String type = info.getQueryParameters().getFirst("search");
                String search = info.getQueryParameters().getFirst("q");
                output.put("type", type);
                output.put("search_message", search);
                switch (type) {
                    case "exchange":
                        //search exchange
                        /////jsonQueryExchange(search, (int) start);
                        try {
                            String[] strA = search.split("[ /]");
                            long have = Long.parseLong(strA[0]);
                            long want = Long.parseLong(strA[1]);
                            output.putAll(jsonQueryTrades(have, want));
                        } catch (Exception e) {
                        }
                        break;
                    case "transactions":
                        //search transactions
                        jsonQueryTransactions(search, (int) start, info);
                        break;
                    case "addresses":
                        //search addresses
                        output.putAll(jsonQueryAddress(search, (int) start, info));
                        break;
                    case "persons":
                        //search persons
                        output.putAll(jsonQuerySearchPages(info, PersonCls.class, search, (int) start, pageSize));
                        break;
                    case "assets":
                        //search assets
                        output.putAll(jsonQuerySearchPages(info, AssetCls.class, search, (int) start, pageSize));
                        break;
                    case "statuses":
                        //search statuses
                        output.putAll(jsonQuerySearchPages(info, StatusCls.class, search, (int) start, pageSize));
                        break;
                    case "templates":
                        //search templates
                        output.putAll(jsonQuerySearchPages(info, TemplateCls.class, search, (int) start, pageSize));
                        break;
                    case "polls":
                        //search templates
                        output.putAll(jsonQuerySearchPages(info, PollCls.class, search, (int) start, pageSize));
                        break;
                    case "blocks":
                        //search block
                        output.putAll(jsonQuerySearchPages(info, Block.class, search, (int) start, pageSize));
                        break;
                    case "top":
                        output.putAll(jsonQueryTopRichest100(100, Long.valueOf(search)));
                        break;
                    case "order":
                        output.putAll(jsonQueryOrder(search));
                        break;
                }
            }

        }

        ///////////////////////////////////////// PERSONS /////////////////////////////////
        // persons list
        else if (info.getQueryParameters().containsKey("persons")) {
            output.put("type", "persons");
            output.putAll(jsonQueryPages(PersonCls.class, start, pageSize));
        }
        // person
        else if (info.getQueryParameters().containsKey("person")) {
            // person asset balance
            if (info.getQueryParameters().containsKey("asset")) {
                boolean assetKey = false;
                // найдем что раньше в строке запроса - персона или актив
                for (String param : info.getQueryParameters().keySet()) {
                    if (param.equals("asset")) {
                        assetKey = true;
                    }
                    if (param.equals("person")) {
                        if (!assetKey) {
                            int side = Account.BALANCE_SIDE_LEFT;
                            try {
                                side = new Integer(info.getQueryParameters().getFirst("side"));
                            } catch (Exception e) {

                            }
                            // персона раньше в параметрах - значит покажем баланс по активу у персоны
                            output.putAll(jsonQueryPersonBalance(new Long(info.getQueryParameters().getFirst("person")),
                                    new Long(info.getQueryParameters().getFirst("asset")),
                                    new Integer(info.getQueryParameters().getFirst("position")),
                                    side));
                            return output;
                        }
                    }
                }
            } else if (info.getQueryParameters().containsKey("status")) {
                boolean statusKey = false;
                // найдем что раньше в строке запроса - персона или актив
                for (String param : info.getQueryParameters().keySet()) {
                    if (param.equals("status")) {
                        statusKey = true;
                    }
                    if (param.equals("person")) {
                        if (!statusKey) {
                            // персона раньше в параметрах - значит покажем баланс по активу у персоны
                            int position = 1;
                            if (info.getQueryParameters().containsKey("position")) {
                                position = new Integer(info.getQueryParameters().getFirst("position"));
                            }
                            jsonQueryPersonStatus(
                                    new Long(info.getQueryParameters().getFirst("person")),
                                    new Long(info.getQueryParameters().getFirst("status")),
                                    position,
                                    true
                            );
                            return output;
                        }
                    }
                }
            } else {
                output.putAll(jsonQueryItemPerson(info.getQueryParameters().getFirst("person")));
            }

            ///////////////////// POLLS ////////////////////////
            // polls list
        } else if (info.getQueryParameters().containsKey("polls")) {
            output.put("type", "polls");
            output.putAll(jsonQueryPages(PollCls.class, start, pageSize));
        } else if (info.getQueryParameters().containsKey("poll")) {
            output.putAll(jsonQueryItemPoll(Long.valueOf(info.getQueryParameters().getFirst("poll")),
                    info.getQueryParameters().getFirst("asset")));

            //////////////////////////// ASSETS //////////////////////////
            // top 100
        } else if (info.getQueryParameters().containsKey("top")) {
            output.putAll(jsonQueryTopRichest(info));
        } else if (info.getQueryParameters().containsKey("assets")) {
            output.put("type", "assets");
            output.putAll(jsonQueryPages(AssetCls.class, start, pageSize));
        } else if (info.getQueryParameters().containsKey("asset")) {
            if (info.getQueryParameters().get("asset").size() == 1) {
                try {
                    output.put("asset", jsonQueryItemAsset(Long.valueOf((info.getQueryParameters().getFirst("asset")))));
                } catch (Exception e) {
                    output.put("error", e.getMessage());
                    logger.error(e.getMessage(), e);
                    return output;
                }
            } else if (info.getQueryParameters().get("asset").size() == 2) {
                long have = Integer.valueOf(info.getQueryParameters().get("asset").get(0));
                long want = Integer.valueOf(info.getQueryParameters().get("asset").get(1));

                output.putAll(jsonQueryTrades(have, want));
            }
        }

        //peers
        else if (info.getQueryParameters().containsKey("peers")) {
            output.putAll(jsonQueryPeers(info));
        }

        // Exchange
        else if (info.getQueryParameters().containsKey("exchange")) {
            jsonQueryExchange(null, (int) start);
        }

        ///////////////////////////// ADDRESSES //////////////////////
        // address
        else if (info.getQueryParameters().containsKey("address")) {
            output.putAll(jsonQueryAddress(info.getQueryParameters().getFirst("address"), (int) start, info));
        } else if (info.getQueryParameters().containsKey("addresses")) {
            jsonQueryAddresses();

            ///////// BLOCKS /////////////
        } else if (info.getQueryParameters().containsKey("blocks")) {
            output.put("type", "blocks");
            output.putAll(jsonQueryPages(Block.BlockHead.class, (int) start, pageSize));
        } else if (info.getQueryParameters().containsKey("block")) {
            output.putAll(jsonQueryBlock(info.getQueryParameters().getFirst("block"), (int) start));
        }

        ///////////////////////////// TRANSACTIONS ///////////////
        /// TX = signature
        else if (info.getQueryParameters().containsKey("tx")) {
            output.putAll(jsonQueryTX(info.getQueryParameters().getFirst("tx")));
        }


        // transactions
        else if (info.getQueryParameters().containsKey("transactions")) {
            jsonQueryTransactions(null, (int) start, info);
        }
        // unconfirmed transactions
        else if (info.getQueryParameters().containsKey("unconfirmed")) {
            output.putAll(jsonQueryUnconfirmedTXs());
        }

        //////////////// EXCHANGE ///////////////////
        // trade
        else if (info.getQueryParameters().containsKey("trade")) {
            output.putAll(jsonQueryTrade(info.getQueryParameters().getFirst("trade")));
        }
        // blog tx
        else if (info.getQueryParameters().containsKey("blogposts")) {
            output.putAll(jsonQueryBlogPostsTx(info.getQueryParameters().getFirst("blogposts")));
        }

        //////////////////////// TEMPLATES ///////////////////
        // templates list
        else if (info.getQueryParameters().containsKey("templates")) {
            output.put("type", "templates");
            output.putAll(jsonQueryPages(TemplateCls.class, start, pageSize));
        }
        // template
        else if (info.getQueryParameters().containsKey("template")) {
            output.putAll(jsonQueryItemTemplate(Long.valueOf(info.getQueryParameters().getFirst("template"))));
        }

        ////////////////////// STATUSES ///////////////////////
        // statuses list
        else if (info.getQueryParameters().containsKey("statuses")) {
            output.put("type", "statuses");
            output.putAll(jsonQueryPages(StatusCls.class, start, pageSize));
        }
        // status
        else if (info.getQueryParameters().containsKey("status")) {
            output.putAll(jsonQueryItemStatus(Long.valueOf(info.getQueryParameters().getFirst("status"))));
        }

        ///////////////////////////// ORDER ///////////////
        /// order = ID
        else if (info.getQueryParameters().containsKey("order")) {
            output.putAll(jsonQueryOrder(info.getQueryParameters().getFirst("order")));
        }

        // not key
        else {
            output.put("error", "Not enough parameters.");
            output.put("help", jsonQueryHelp());
        }

        // time guery
        output.put("queryTimeMs", stopwatchAll.elapsedTime());
        if (BlockChain.CLONE_MODE || BlockChain.TEST_MODE) {
            output.put("network", BlockChain.DEMO_MODE ? "DEMO Net"
                    : BlockChain.CLONE_MODE ? (Settings.CLONE_OR_SIDE.toLowerCase() + "Chain: " + Settings.APP_NAME) : "TEST Net");
        }
        return output;
    }

    public Map jsonQueryHelp() {
        Map help = new LinkedHashMap();

        help.put("Unconfirmed Transactions", "blockexplorer.json?unconfirmed");
        help.put("Block", "blockexplorer.json?block={block}[&page={page}]");
        help.put("Blocks List", "blockexplorer.json?blocks[&start={height}]");
        help.put("Assets List", "blockexplorer.json?assets");
        help.put("Asset", "blockexplorer.json?asset={asset}");
        help.put("Asset Trade", "blockexplorer.json?asset={assetHave}&asset={assetWant}");
        help.put("Polls List", "blockexplorer.json?polls");
        help.put("Poll", "blockexplorer.json?poll={poll}&asset={asset}");
        help.put("AT TX", "blockexplorer.json?atTx={atTx}");
        help.put("Trade", "blockexplorer.json?trade={initiatorSignature}/{targetSignature}");
        help.put("Transaction", "blockexplorer.json?tx={txSignature}");
        help.put("Name", "blockexplorer.json?name={name}");
        help.put("Name (additional)", "blockexplorer.json?name={name}&start={offset}&allOnOnePage");
        help.put("Address", "blockexplorer.json?address={address}");
        help.put("Address (additional)",
                "blockexplorer.json?address={address}&start={offset}&allOnOnePage&withoutBlocks&showWithout={1,2,blocks}&showOnly={type}");
        help.put("Top Richest", "blockexplorer.json?top");
        help.put("Top Richest", "blockexplorer.json?top={limit}&asset={asset}");
        help.put("Address All Not Zero", "blockexplorer.json?top=all|[limit]");
        help.put("Address All Addresses", "blockexplorer.json?top=all");
        help.put("Assets List", "blockexplorer.json?assets");
        help.put("Assets List", "blockexplorer.json?assets");
        help.put("AT List", "blockexplorer.json?aTs");
        help.put("Names List", "blockexplorer.json?names");
        help.put("BlogPosts of Address", "blockexplorer.json?blogposts={address}");
        help.put("Search", "blockexplorer.json?q={text}");
        help.put("Balance", "blockexplorer.json?balance={address}[&balance=address2...]");

        return help;
    }


    public Map jsonQueryBlogPostsTx(String address) {

        Map output = new LinkedHashMap();
        try {

            //AssetNames assetNames = new AssetNames();

            List<Transaction> transactions = new ArrayList<Transaction>();

            if (Crypto.getInstance().isValidAddress(address)) {
                Account account = new Account(address);

                address = account.getAddress();
                // get reference to parent record for this account
                long[] timestampRef = account.getLastTimestamp();
                // get signature for account + time

                Controller cntr = Controller.getInstance();

                int count = transactions.size();

                output.put("count", count);

                int i = 0;
                for (Transaction transaction : transactions) {
                    output.put(count - i, jsonUnitPrint(transaction)); //, assetNames));
                    i++;
                }
            }

            //output.put("assetNames", assetNames.getMap());

        } catch (Exception e1) {
            output = new LinkedHashMap();
            output.put("error", e1.getLocalizedMessage());
        }
        return output;
    }

    // ItemCls.ASSET_TYPE
    public Map jsonQueryItemsLite(int itemType, Long fromKey) {

        Map output = new LinkedHashMap();
        ItemMap itemsMap = Controller.getInstance().getItemMap(itemType);
        try (IteratorCloseable<Long> iterator = itemsMap.getIteratorFrom(fromKey, true)) {
            Long key;
            ItemCls item;
            int size = 25;
            while (iterator.hasNext()) {
                if (--size < 0)
                    break;
                key = iterator.next();
                item = itemsMap.get(key);
                if (item == null)
                    continue;
                output.put(key, item.viewName());
            }
        } catch (IOException e) {
        }

        return output;
    }

    private Tuple2<Map, Transaction> itemBase(ItemCls item) {
        Map map = new LinkedHashMap();
        map.put("key", item.getKey());
        map.put("icon", Base64.encodeBase64String(item.getIcon()));
        map.put("image", Base64.encodeBase64String(item.getImage()));
        map.put("name", item.getName());
        map.put("description", item.viewDescription());
        map.put("owner", item.getOwner().getAddress());

        if (item.getReference() != null) {
            map.put("Label_seqNo", Lang.T("seqNo", langObj));
            long txSeqNo = dcSet.getTransactionFinalMapSigns().get(item.getReference());
            map.put("seqNo", Transaction.viewDBRef(txSeqNo));
            Transaction transaction = dcSet.getTransactionFinalMap().get(txSeqNo);
            map.put("tx_timestamp", transaction.getTimestamp());
            if (transaction.getCreator() == null) {
                map.put("tx_creator", transaction.getCreator());
                map.put("tx_creator_person", transaction.viewCreator());
            }
            return new Tuple2<Map, Transaction>(map, transaction);
        }

        return new Tuple2<Map, Transaction>(map, null);
    }

    public Map jsonQueryItemPoll(Long pollKey, String assetStr) {

        output.put("type", "poll");
        output.put("search", "polls");

        PollCls poll = (PollCls) dcSet.getItemPollMap().get(pollKey);
        if (poll == null) {
            return new HashMap(2);
        }

        output.put("charKey", poll.getItemTypeChar());
        output.put("label_Actions", Lang.T("Actions", langObj));
        output.put("label_RAW", Lang.T("Bytecode", langObj));

        Map output = new LinkedHashMap();
        Long assetKey;

        try {
            assetKey = Long.valueOf(assetStr);
        } catch (Exception e) {
            assetKey = 2l;
        }

        AssetCls asset = (AssetCls) dcSet.getItemAssetMap().get(assetKey);
        if (asset == null) {
            assetKey = 2l;
            asset = (AssetCls) dcSet.getItemAssetMap().get(assetKey);
        }
        output.put("assetKey", assetKey);
        output.put("assetName", asset.viewName());

        Map pollJSON = itemBase(poll).a;

        pollJSON.put("totalVotes", poll.getTotalVotes(DCSet.getInstance()).toPlainString());

        List<String> options = poll.getOptions();
        int optionsSize = options.size();


        Tuple4<Integer, long[], BigDecimal, BigDecimal[]> votes = poll.votesWithPersons(dcSet, assetKey, 0);

        JSONArray array = new JSONArray();
        for (int i = 0; i < optionsSize; i++) {
            Map itemMap = new LinkedHashMap();
            itemMap.put("name", options.get(i));
            itemMap.put("persons", votes.b[i]);
            itemMap.put("votes", votes.d[i]);
            array.add(itemMap);
        }

        pollJSON.put("votes", array);
        pollJSON.put("personsTotal", votes.a);
        pollJSON.put("votesTotal", votes.c);

        output.put("poll", pollJSON);

        output.put("label_table_key", Lang.T("Number", langObj));
        output.put("label_table_option_name", Lang.T("Option", langObj));
        output.put("label_table_person_votes", Lang.T("Personal Voters", langObj));
        output.put("label_table_option_votes", Lang.T("Asset Votes", langObj));
        output.put("label_Total", Lang.T("Total", langObj));

        output.put("label_Poll", Lang.T("Poll", langObj));
        output.put("label_Asset", Lang.T("Asset", langObj));
        output.put("label_Key", Lang.T("Key", langObj));
        output.put("label_Owner", Lang.T("Owner", langObj));
        output.put("label_Description", Lang.T("Description", langObj));

        return output;

    }

    // TODO: что-то тут напутано
    public Map<Long, Tuple6<Integer, Integer, BigDecimal, BigDecimal, BigDecimal, BigDecimal>> calcForAsset(
            List<Order> orders,
            List<Trade> trades) {

        Map<Long, Integer> pairsOpenOrders = new HashMap<Long, Integer>();
        Map<Long, BigDecimal> volumePriceOrders = new HashMap<Long, BigDecimal>();
        Map<Long, BigDecimal> volumeAmountOrders = new HashMap<Long, BigDecimal>();

        int count;
        BigDecimal volumePrice = BigDecimal.ZERO;
        BigDecimal volumeAmount = BigDecimal.ZERO;

        if (orders != null) {
            for (Order order : orders) {
                if (!pairsOpenOrders.containsKey(order.getWantAssetKey())) {
                    count = 0;
                } else {
                    count = pairsOpenOrders.get(order.getWantAssetKey());
                }

                if (!volumeAmountOrders.containsKey(order.getWantAssetKey())) {
                    volumeAmount = BigDecimal.ZERO;
                } else {
                    volumeAmount = volumeAmountOrders.get(order.getWantAssetKey());
                }

                if (!volumePriceOrders.containsKey(order.getWantAssetKey())) {
                    volumePrice = BigDecimal.ZERO;
                } else {
                    volumePrice = volumePriceOrders.get(order.getWantAssetKey());
                }

                count++;
                pairsOpenOrders.put(order.getWantAssetKey(), count);

                volumeAmount = volumeAmount.add(order.getAmountHaveLeft());

                volumeAmountOrders.put(order.getWantAssetKey(), volumeAmount);

                volumePriceOrders.put(order.getWantAssetKey(), volumePrice);

                if (!pairsOpenOrders.containsKey(order.getHaveAssetKey())) {
                    count = 0;
                } else {
                    count = pairsOpenOrders.get(order.getHaveAssetKey());
                }

                if (!volumePriceOrders.containsKey(order.getHaveAssetKey())) {
                    volumePrice = BigDecimal.ZERO;
                } else {
                    volumePrice = volumePriceOrders.get(order.getHaveAssetKey());
                }

                if (!volumeAmountOrders.containsKey(order.getHaveAssetKey())) {
                    volumeAmount = BigDecimal.ZERO;
                } else {
                    volumeAmount = volumeAmountOrders.get(order.getHaveAssetKey());
                }

                count++;
                pairsOpenOrders.put(order.getHaveAssetKey(), count);

                volumePrice = volumePrice.add(order.getAmountHaveLeft());

                volumePriceOrders.put(order.getHaveAssetKey(), volumePrice);

                volumeAmountOrders.put(order.getHaveAssetKey(), volumeAmount);
            }
        }

        Map<Long, Integer> pairsTrades = new TreeMap<Long, Integer>();
        Map<Long, BigDecimal> volumePriceTrades = new TreeMap<Long, BigDecimal>();
        Map<Long, BigDecimal> volumeAmountTrades = new TreeMap<Long, BigDecimal>();

        if (trades != null) {
            for (Trade trade : trades) {

                Order initiator = Order.getOrder(dcSet, trade.getInitiator());
                if (!pairsTrades.containsKey(initiator.getWantAssetKey())) { //.c.a)) {
                    count = 0;
                    volumePrice = BigDecimal.ZERO;
                    volumeAmount = BigDecimal.ZERO;
                } else {
                    count = pairsTrades.get(initiator.getWantAssetKey());
                    volumePrice = volumePriceTrades.get(initiator.getWantAssetKey());
                    volumeAmount = volumeAmountTrades.get(initiator.getWantAssetKey());
                }

                count++;
                pairsTrades.put(initiator.getWantAssetKey(), count);

                volumePrice = volumePrice.add(trade.getAmountHave());
                volumeAmount = volumeAmount.add(trade.getAmountWant());

                volumePriceTrades.put(initiator.getWantAssetKey(), volumePrice);
                volumeAmountTrades.put(initiator.getWantAssetKey(), volumeAmount);

                Order target = Order.getOrder(dcSet, trade.getTarget());
                if (!pairsTrades.containsKey(target.getWantAssetKey())) {
                    count = 0;
                    volumePrice = BigDecimal.ZERO;
                    volumeAmount = BigDecimal.ZERO; // ;
                } else {
                    count = pairsTrades.get(target.getWantAssetKey());
                    volumePrice = volumePriceTrades.get(target.getWantAssetKey());
                    volumeAmount = volumeAmountTrades.get(target.getWantAssetKey());
                }

                count++;
                pairsTrades.put(target.getWantAssetKey(), count);

                volumePrice = volumePrice.add(trade.getAmountHave());
                volumeAmount = volumeAmount.add(trade.getAmountWant());

                volumePriceTrades.put(target.getWantAssetKey(), volumePrice);
                volumeAmountTrades.put(target.getWantAssetKey(), volumeAmount);
            }
        }

        Map<Long, Tuple6<Integer, Integer, BigDecimal, BigDecimal, BigDecimal, BigDecimal>> all = new TreeMap<Long, Tuple6<Integer, Integer, BigDecimal, BigDecimal, BigDecimal, BigDecimal>>();

        for (Map.Entry<Long, Integer> pair : pairsOpenOrders.entrySet()) {
            all.put(pair.getKey(), Fun.t6(pair.getValue(), 0, volumePriceOrders.get(pair.getKey()),
                    volumeAmountOrders.get(pair.getKey()), BigDecimal.ZERO, BigDecimal.ZERO));
        }

        for (Map.Entry<Long, Integer> pair : pairsTrades.entrySet()) {

            if (all.containsKey(pair.getKey())) {
                all.put(pair.getKey(),
                        Fun.t6(all.get(pair.getKey()).a, pair.getValue(), all.get(pair.getKey()).c,
                                all.get(pair.getKey()).d, volumePriceTrades.get(pair.getKey()),
                                volumeAmountTrades.get(pair.getKey())));
            } else {
                all.put(pair.getKey(), Fun.t6(0, pair.getValue(), BigDecimal.ZERO, BigDecimal.ZERO,
                        volumePriceTrades.get(pair.getKey()), volumeAmountTrades.get(pair.getKey())));
            }
        }

        return all;
    }

    public Map jsonQueryItemAsset(long key) {

        output.put("type", "asset");
        output.put("search", "assets");

        AssetCls asset = Controller.getInstance().getAsset(key);
        if (asset == null) {
            return new HashMap(2);
        }

        output.put("charKey", asset.getItemTypeChar());
        output.put("label_Actions", Lang.T("Actions", langObj));
        output.put("label_RAW", Lang.T("Bytecode", langObj));

        Map output = new LinkedHashMap();

        List<Order> orders = dcSet.getOrderMap().getOrders(key);

        TradeMapImpl tradesMap = dcSet.getTradeMap();
        List<Trade> trades = tradesMap.getTrades(key);

        Map assetJSON = itemBase(asset).a;

        if (asset.getKey() > 0 && asset.getKey() < 1000) {
            /// redefine
            assetJSON.put("description", Lang.T(asset.viewDescription(), langObj));
        }

        assetJSON.put("quantity", NumberAsString.formatAsString(asset.getQuantity()));
        assetJSON.put("released", NumberAsString.formatAsString(asset.getReleased(dcSet)));

        assetJSON.put("scale", asset.getScale());

        assetJSON.put("operations", orders.size() + trades.size());

        assetJSON.put("assetType", Lang.T(asset.viewAssetType(), langObj));
        assetJSON.put("assetTypeChar", asset.charAssetType() + asset.viewAssetTypeAbbrev());

        assetJSON.put("assetTypeFull", Lang.T(asset.viewAssetTypeFull(), langObj));
        StringJoiner joiner = new StringJoiner(", ");
        for (Tuple2<?, String> item : asset.viewAssetTypeActionsList(null, true)) {
            joiner.add(Lang.T(item.b, langObj));
        }
        assetJSON.put("assetTypeDesc", Lang.T(asset.viewAssetTypeDescriptionCls(asset.getAssetType()), langObj)
                + ".\n" + Lang.T("Acceptable actions", langObj) + ":\n" + joiner.toString()
        );

        output.put("this", assetJSON);

        output.put("totalOpenOrdersCount", orders.size());
        output.put("totalTradesCount", trades.size());

        Map<Long, Tuple6<Integer, Integer, BigDecimal, BigDecimal, BigDecimal, BigDecimal>> all = calcForAsset(orders,
                trades);

        if (all.containsKey(key)) {
            output.put("totalOrdersVolume", all.get(key).c.toPlainString());
        } else {
            output.put("totalOrdersVolume", BigDecimal.ZERO.toPlainString());
        }

        if (all.containsKey(key)) {
            output.put("totalTradesVolume", all.get(key).f.toPlainString());
        } else {
            output.put("totalTradesVolume", BigDecimal.ZERO.toPlainString());
        }

        Map pairsJSON = new LinkedHashMap();

        pairsJSON = new LinkedHashMap();
        for (Map.Entry<Long, Tuple6<Integer, Integer, BigDecimal, BigDecimal, BigDecimal, BigDecimal>> pair : all
                .entrySet()) {
            if (pair.getKey() == key) {
                continue;
            }
            AssetCls assetWant = Controller.getInstance().getAsset(pair.getKey());

            Map pairJSON = new LinkedHashMap();
            pairJSON.put("openOrdersCount", pair.getValue().a);
            pairJSON.put("tradesCount", pair.getValue().b);
            pairJSON.put("sum", pair.getValue().a + pair.getValue().b);
            pairJSON.put("ordersPriceVolume", pair.getValue().c.toPlainString());
            pairJSON.put("ordersAmountVolume", pair.getValue().d.toPlainString());
            pairJSON.put("tradesPriceVolume", pair.getValue().e.toPlainString());
            pairJSON.put("tradeAmountVolume", pair.getValue().f.toPlainString());
            pairJSON.put("asset", pair.getKey());
            pairJSON.put("assetName", assetWant.viewName());
            if (assetWant.getKey() > 0 && assetWant.getKey() < 1000) {
                pairJSON.put("description", Lang.T(assetWant.viewDescription(), langObj));
            } else {
                pairJSON.put("description", assetWant.viewDescription());
            }

            Trade trade = tradesMap.getLastTrade(key, pair.getKey());
            //Order initiator
            if (trade == null) {
                pairJSON.put("last", "---");
                pairJSON.put("lastReverse", "---");
            } else {
                if (trade.getHaveKey().equals(pair.getKey())) {
                    pairJSON.put("last", trade.calcPrice().toPlainString());
                    pairJSON.put("lastReverse", trade.calcPriceRevers().toPlainString());
                } else {
                    pairJSON.put("last", trade.calcPriceRevers().toPlainString());
                    pairJSON.put("lastReverse", trade.calcPrice().toPlainString());
                }
            }

            pairsJSON.put(pair.getKey(), pairJSON);
        }

        output.put("pairs", pairsJSON);
        output.put("label_Asset", Lang.T("Asset", langObj));
        output.put("label_Key", Lang.T("Key", langObj));
        output.put("Label_seqNo", Lang.T("seqNo", langObj));
        output.put("label_Creator", Lang.T("Creator", langObj));
        output.put("label_Description", Lang.T("Description", langObj));
        output.put("label_Scale", Lang.T("Accuracy", langObj));
        output.put("label_AssetType", Lang.T("Type # вид", langObj));
        output.put("label_AssetType_Desc", Lang.T("Type Description", langObj));
        output.put("label_Quantity", Lang.T("Quantity", langObj));
        output.put("label_Released", Lang.T("Released", langObj));
        output.put("label_Holders", Lang.T("Holders", langObj));
        output.put("label_Available_pairs", Lang.T("Available pairs", langObj));
        output.put("label_Pair", Lang.T("Pair", langObj));
        output.put("label_Orders_Count", Lang.T("Orders Count", langObj));
        output.put("label_Open_Orders_Volume",
                Lang.T("Open Orders Volume", langObj));
        output.put("label_Trades_Count", Lang.T("Trades Count", langObj));
        output.put("label_Trades_Volume", Lang.T("Trades Volume", langObj));
        output.put("label_Total", Lang.T("Total", langObj));
        output.put("label_View", Lang.T("View", langObj));

        return output;
    }

    public Map jsonQueryOrder(String orderIdStr) {

        output.put("type", "order");
        output.put("search", "order");
        output.put("search_message", orderIdStr);

        Long orderId = Transaction.parseDBRef(orderIdStr);
        if (orderId == null) {
            output.put("error", "order ID wrong");
            return output;
        }

        Map output = new LinkedHashMap();

        boolean isCompleted;
        Order order = dcSet.getOrderMap().get(orderId);
        if (order == null) {
            order = dcSet.getCompletedOrderMap().get(orderId);
            if (order == null) {
                output.put("error", "order not found");
                return output;
            } else {
                isCompleted = true;
            }
        } else {
            isCompleted = false;
        }

        output.put("order", order.toJson());

        List<Trade> trades = dcSet.getTradeMap().getTradesByOrderID(orderId);

        AssetCls assetHave = Controller.getInstance().getAsset(order.getHaveAssetKey());
        AssetCls assetWant = Controller.getInstance().getAsset(order.getWantAssetKey());

        output.put("completed", isCompleted);
        output.put("canceled", isCompleted && !order.isFulfilled());

        output.put("txSeqNo", Transaction.viewDBRef(order.getId()));
        Transaction transaction = dcSet.getTransactionFinalMap().get(orderId);
        output.put("timestamp", transaction.getTimestamp());
        output.put("creator", transaction.getCreator().getAddress());
        output.put("creator_person", transaction.getCreator().getPersonAsString());

        output.put("assetHaveOwner", assetHave.getOwner().getAddress());
        output.put("assetWantOwner", assetWant.getOwner().getAddress());

        output.put("assetHaveKey", assetHave.getKey());
        output.put("assetHaveName", assetHave.viewName());
        output.put("assetWantKey", assetWant.getKey());
        output.put("assetWantName", assetWant.viewName());

        Map tradesJSON = new LinkedHashMap();

        output.put("tradesCount", trades.size());

        int i = 0;
        for (Trade trade : trades) {

            tradesJSON.put(i++, tradeJSON(trade, assetHave, assetWant));

            if (i > 100)
                break;
        }


        output.put("lastTrades", tradesJSON);

        output.put("label_Head", Lang.T("Exchange Order", langObj));

        output.put("label_Order", Lang.T("Order", langObj));

        output.put("label_Active", Lang.T("Active", langObj));
        output.put("label_Completed", Lang.T("Completed", langObj));
        output.put("label_Canceled", Lang.T("Canceled", langObj));

        output.put("label_Fulfilled", Lang.T("Fulfilled", langObj));
        output.put("label_LeftHave", Lang.T("Left Have", langObj));
        output.put("label_LeftPrice", Lang.T("Left Price", langObj));
        output.put("label_table_LastTrades", Lang.T("Last Trades", langObj));
        output.put("label_table_have", Lang.T("Base Asset", langObj));
        output.put("label_table_want", Lang.T("Price Asset", langObj));
        output.put("label_table_orders", Lang.T("Opened Orders", langObj));
        output.put("label_table_last_price", Lang.T("Last Price", langObj));
        output.put("label_table_volume24", Lang.T("Day Volume", langObj));

        output.put("label_Trade_Initiator", Lang.T("Trade Initiator", langObj));
        output.put("label_Position_Holder", Lang.T("Position Holder", langObj));
        output.put("label_Date", Lang.T("Date", langObj));
        output.put("label_Pair", Lang.T("Pair", langObj));
        output.put("label_Creator", Lang.T("Creator", langObj));
        output.put("label_Amount", Lang.T("Amount", langObj));
        output.put("label_Volume", Lang.T("Volume", langObj));
        output.put("label_Price", Lang.T("Price", langObj));
        output.put("label_Reverse_Price", Lang.T("Reverse Price", langObj));
        output.put("label_Total_Cost", Lang.T("Total Cost", langObj));

        return output;
    }

    private Map tradeJSON(Trade trade, AssetCls assetHaveIn, AssetCls assetWantIn) {

        Map tradeJSON = new HashMap();

        AssetCls pairAssetHave;
        AssetCls pairAssetWant;

        Order orderInitiator = Order.getOrder(dcSet, trade.getInitiator());

        long pairHaveKey;
        long pairWantKey;
        long tempKey;

        boolean unchecked = false;

        if (assetHaveIn == null) {

            pairHaveKey = trade.getHaveKey();
            pairWantKey = trade.getWantKey();

            pairAssetHave = dcSet.getItemAssetMap().get(pairHaveKey);
            pairAssetWant = dcSet.getItemAssetMap().get(pairWantKey);

            /// если пару нужно перевернуть так как есть общепринятые пары
            if (pairHaveKey == 2L && pairWantKey == 1l
                    || pairHaveKey == 95l
                    || pairHaveKey > 33 && pairHaveKey < 1000
                    && (pairWantKey < 33 && pairWantKey > 1000)
                    || pairHaveKey > 10 && pairHaveKey < 33
                    && (pairWantKey < 10)
                    || pairAssetHave.isIndex() && pairHaveKey < pairWantKey
                    || pairAssetHave.isInsideCurrency() && pairHaveKey < pairWantKey
                    || pairHaveKey < 5 && pairWantKey > 1000
            ) {
                // swap pair
                tempKey = pairHaveKey;
                pairHaveKey = pairWantKey;
                pairWantKey = tempKey;
            } else if (pairHaveKey > 1000 && pairWantKey > 1000) {
                unchecked = true;
                tradeJSON.put("unchecked", true);
            }

            pairAssetHave = dcSet.getItemAssetMap().get(pairHaveKey);
            pairAssetWant = dcSet.getItemAssetMap().get(pairWantKey);

        } else {
            pairAssetHave = assetHaveIn;
            pairAssetWant = assetWantIn;
            pairHaveKey = pairAssetHave.getKey();
            //pairWantKey = pairAssetWant.getKey();
        }

        tradeJSON.put("assetHaveKey", pairAssetHave.getKey());
        tradeJSON.put("assetHaveName", pairAssetHave.viewName());

        tradeJSON.put("assetWantKey", pairAssetWant.getKey());
        tradeJSON.put("assetWantName", pairAssetWant.viewName());

        tradeJSON.put("assetHaveOwner", pairAssetHave.getOwner().getAddress());
        tradeJSON.put("assetWantOwner", pairAssetWant.getOwner().getAddress());

        tradeJSON.put("realPrice", trade.calcPrice());

        tradeJSON.put("realReversePrice", trade.calcPriceRevers());

        if (orderInitiator == null) {
            if (BlockChain.CHECK_BUGS > 7) {
                // show ERROR
                tradeJSON.put("initiatorTx", "--");
                tradeJSON.put("initiatorCreator_addr", "--"); // viewCreator
                tradeJSON.put("initiatorCreator", "--");
                tradeJSON.put("initiatorAmount", "--");
            }
        } else {
            tradeJSON.put("initiatorTx", Transaction.viewDBRef(orderInitiator.getId()));
            tradeJSON.put("initiatorCreator_addr", orderInitiator.getCreator().getAddress()); // viewCreator
            tradeJSON.put("initiatorCreator", orderInitiator.getCreator().getPersonOrShortAddress(12));
            tradeJSON.put("initiatorAmount", orderInitiator.getAmountHave().setScale(pairAssetHave.getScale(), RoundingMode.HALF_DOWN).toPlainString());
        }

        Order orderTarget = Order.getOrder(dcSet, trade.getTarget());

        tradeJSON.put("targetTx", Transaction.viewDBRef(orderTarget.getId()));
        tradeJSON.put("targetCreator_addr", orderTarget.getCreator().getAddress()); // viewCreator
        tradeJSON.put("targetCreator", orderTarget.getCreator().getPersonOrShortAddress(12)); // viewCreator
        tradeJSON.put("targetAmount", orderTarget.getAmountHave().setScale(pairAssetHave.getScale(), RoundingMode.HALF_DOWN).toPlainString());

        tradeJSON.put("timestamp", trade.getTimestamp());

        if (orderInitiator == null && BlockChain.CHECK_BUGS > 7 || pairHaveKey == orderInitiator.getHaveAssetKey()) {
            tradeJSON.put("type", "sell");

            tradeJSON.put("amountHave", trade.getAmountWant().setScale(pairAssetHave.getScale(), RoundingMode.HALF_DOWN).toPlainString());
            tradeJSON.put("amountWant", trade.getAmountHave().setScale(pairAssetWant.getScale(), RoundingMode.HALF_DOWN).toPlainString());

        } else {
            tradeJSON.put("type", "buy");

            tradeJSON.put("amountHave", trade.getAmountHave().setScale(pairAssetHave.getScale(), RoundingMode.HALF_DOWN).toPlainString());
            tradeJSON.put("amountWant", trade.getAmountWant().setScale(pairAssetWant.getScale(), RoundingMode.HALF_DOWN).toPlainString());
        }

        return tradeJSON;
    }

    public Map jsonQueryTrades(long have, long want) {

        output.put("type", "trades");
        output.put("search", "exchange");
        output.put("search_message", have + "/" + want);

        Map output = new LinkedHashMap();

        List<Order> ordersHave = dcSet.getOrderMap().getOrdersForTrade(have, want, false);
        List<Order> ordersWant = dcSet.getOrderMap().getOrdersForTrade(want, have, true);

        List<Trade> trades = dcSet.getTradeMap().getTrades(have, want, 0, 50);

        AssetCls assetHave = Controller.getInstance().getAsset(have);
        AssetCls assetWant = Controller.getInstance().getAsset(want);

        output.put("assetHaveOwner", assetHave.getOwner().getAddress());
        output.put("assetWantOwner", assetWant.getOwner().getAddress());

        output.put("assetHave", assetHave.getKey());
        output.put("assetHaveName", assetHave.viewName());
        output.put("assetWant", assetWant.getKey());
        output.put("assetWantName", assetWant.viewName());

        Map sellsJSON = new LinkedHashMap();
        Map buysJSON = new LinkedHashMap();

        BigDecimal sumAmount = BigDecimal.ZERO;
        BigDecimal sumAmountGood = BigDecimal.ZERO;

        BigDecimal sumSellingAmount = BigDecimal.ZERO;
        BigDecimal sumSellingAmountGood = BigDecimal.ZERO;

        TransactionFinalMapImpl finalMap = DCSet.getInstance().getTransactionFinalMap();
        Transaction createOrder;

        BigDecimal vol;
        // show SELLs in BACK order
        for (int i = ordersHave.size() - 1; i >= 0; i--) {

            Order order = ordersHave.get(i);
            Map sellJSON = new LinkedHashMap();

            /// цену берем по остаткам
            sellJSON.put("price", order.calcLeftPrice().toPlainString());
            vol = order.getAmountHaveLeft(); //.b.b.subtract(order.b.c);
            sellJSON.put("amount", vol.toPlainString()); // getAmountHaveLeft
            sumAmount = sumAmount.add(vol);

            /// цену берем по остаткам
            sellJSON.put("sellingPrice", order.calcLeftPriceReverse().toPlainString());

            //BigDecimal sellingAmount = Order.calcAmountWantLeft(order);
            BigDecimal sellingAmount = order.getAmountWantLeft();

            sellJSON.put("sellingAmount", sellingAmount.toPlainString());

            sumAmountGood = sumAmountGood.add(vol);

            sumSellingAmountGood = sumSellingAmountGood.add(sellingAmount);

            sumSellingAmount = sumSellingAmount.add(sellingAmount);


            createOrder = finalMap.get(order.getId());

            sellJSON.put("creator", createOrder.getCreator().getPersonOrShortAddress(12));
            sellJSON.put("creator_addr", createOrder.getCreator().getAddress());

            sellsJSON.put(Base58.encode(createOrder.getSignature()), sellJSON);
        }

        output.put("sells", sellsJSON);

        output.put("sellsSumAmount", sumAmount.toPlainString());
        output.put("sellsSumAmountGood", sumAmountGood.toPlainString());
        output.put("sellsSumTotal", sumSellingAmount.toPlainString());
        output.put("sellsSumTotalGood", sumSellingAmountGood.toPlainString());

        sumAmount = BigDecimal.ZERO;
        sumAmountGood = BigDecimal.ZERO;

        BigDecimal sumBuyingAmount = BigDecimal.ZERO;
        BigDecimal sumBuyingAmountGood = BigDecimal.ZERO;

        for (int i = ordersWant.size() - 1; i >= 0; i--) {

            Order order = ordersWant.get(i);

            Map buyJSON = new LinkedHashMap();

            /// цену берем по остаткам
            buyJSON.put("price", order.calcLeftPrice().toPlainString());
            vol = order.getAmountHaveLeft(); //.b.b.subtract(order.b.c);
            buyJSON.put("amount", vol.toPlainString()); // getAmountHaveLeft

            sumAmount = sumAmount.add(vol);

            /// цену берем по остаткам
            buyJSON.put("buyingPrice", order.calcLeftPriceReverse().toPlainString());

            //BigDecimal buyingAmount = Order.calcAmountWantLeft(order);
            BigDecimal buyingAmount = order.getAmountWantLeft();

            buyJSON.put("buyingAmount", buyingAmount.toPlainString());

            sumBuyingAmountGood = sumBuyingAmountGood.add(buyingAmount);

            sumAmountGood = sumAmountGood.add(vol);

            sumBuyingAmount = sumBuyingAmount.add(buyingAmount);

            createOrder = finalMap.get(order.getId());

            buyJSON.put("creator", createOrder.getCreator().getPersonOrShortAddress(12));
            buyJSON.put("creator_addr", createOrder.getCreator().getAddress());

            buysJSON.put(Base58.encode(createOrder.getSignature()), buyJSON);
        }
        output.put("buys", buysJSON);

        output.put("buysSumAmount", sumBuyingAmount.toPlainString());
        output.put("buysSumAmountGood", sumBuyingAmountGood.toPlainString());
        output.put("buysSumTotal", sumAmount.toPlainString());
        output.put("buysSumTotalGood", sumAmountGood.toPlainString());

        Map tradesJSON = new LinkedHashMap();

        output.put("tradesCount", trades.size());

        int i = 0;
        for (Trade trade : trades) {

            tradesJSON.put(i++, tradeJSON(trade, assetHave, assetWant));

            if (i > 100)
                break;
        }
        output.put("trades", tradesJSON);

        output.put("label_Trades", Lang.T("Trades", langObj));
        output.put("label_Trade_Initiator", Lang.T("Trade Initiator", langObj));
        output.put("label_Position_Holder", Lang.T("Position Holder", langObj));
        output.put("label_Volume", Lang.T("Volume", langObj));
        output.put("label_Price", Lang.T("Price", langObj));
        output.put("label_Total_Cost", Lang.T("Total Cost", langObj));
        output.put("label_Amount", Lang.T("Amount", langObj));
        output.put("label_Orders", Lang.T("Orders", langObj));
        output.put("label_Sell_Orders", Lang.T("Sell Orders", langObj));
        output.put("label_Buy_Orders", Lang.T("Buy Orders", langObj));
        output.put("label_Total", Lang.T("Total", langObj));
        output.put("label_Total_For_Sell", Lang.T("Total for Sell", langObj));
        output.put("label_Total_For_Buy", Lang.T("Total for Buy", langObj));
        output.put("label_Trade_History", Lang.T("Trade History", langObj));
        output.put("label_Date", Lang.T("Date", langObj));
        output.put("label_Type", Lang.T("Type", langObj));
        output.put("label_Trade_Volume", Lang.T("Trade Volume", langObj));
        output.put("label_Go_To", Lang.T("Go To", langObj));
        output.put("label_Creator", Lang.T("Creator", langObj));

        return output;
    }

    private Map jsonQueryPersonBalance(Long personKey, Long assetKey, int position, int side) {

        output.put("type", "person_asset");
        output.put("search", "persons");

        Map output = new HashMap();
        if (position < 1 || position > 5) {
            output.put("error", "wrong position");
            return output;
        }

        PersonCls person = (PersonCls) dcSet.getItemPersonMap().get(new Long(personKey));
        if (person == null) {
            output.put("error", "person not found");
            return output;
        }

        AssetCls asset = (AssetCls) dcSet.getItemAssetMap().get(new Long(assetKey));
        if (asset == null) {
            output.put("error", "person not found");
            return output;
        }

        byte[] b = person.getImage();
        String a = Base64.encodeBase64String(b);

        output.put("Label_key", Lang.T("Key", langObj));
        output.put("Label_name", Lang.T("Name", langObj));

        output.put("position", position);
        output.put("side", side);

        output.put("person_img", a);
        output.put("person_key", person.getKey());
        output.put("person_name", person.viewName());

        output.put("asset_key", asset.getKey());
        output.put("asset_name", asset.viewName());

        output.put("Label_asset", Lang.T("Asset", langObj));
        output.put("Label_person", Lang.T("Person", langObj));

        output.put("Label_denied", Lang.T("DENIED", langObj));
        output.put("Label_sum", Lang.T("SUM", langObj));

        output.put("Label_Positions", Lang.T("Balance Positions", langObj));
        output.put("Label_Sides", Lang.T("Balance Sides", langObj));

        output.put("label_Balance_1", Lang.T(Account.balancePositionName(1), langObj));
        output.put("label_Balance_2", Lang.T(Account.balancePositionName(2), langObj));
        output.put("label_Balance_3", Lang.T(Account.balancePositionName(3), langObj));
        output.put("label_Balance_4", Lang.T(Account.balancePositionName(4), langObj));
        output.put("label_Balance_5", Lang.T(Account.balancePositionName(5), langObj));

        output.put("label_Balance_Pos", Lang.T(Account.balancePositionName(position), langObj));
        output.put("label_Balance_Side", Lang.T(Account.balanceSideName(side), langObj));

        output.put("Label_TotalDebit", Lang.T(Account.balanceSideName(Account.BALANCE_SIDE_DEBIT), langObj));
        output.put("Label_Left", Lang.T(Account.balanceSideName(Account.BALANCE_SIDE_LEFT), langObj));
        output.put("Label_TotalCredit", Lang.T(Account.balanceSideName(Account.BALANCE_SIDE_CREDIT), langObj));

        output.put("Side_Help", Lang.T("Side_Help", langObj));

        if (assetKey.equals(Transaction.FEE_KEY)) {
            output.put("label_Balance_4", Lang.T(Account.balanceCOMPUPositionName(4), langObj));
            output.put("label_Balance_5", Lang.T(Account.balanceCOMPUPositionName(5), langObj));

            if (position == TransactionAmount.ACTION_SPEND || position == TransactionAmount.ACTION_PLEDGE) {

                output.put("label_Balance_Pos", Lang.T(Account.balanceCOMPUPositionName(position), langObj));
                output.put("label_Balance_Side", Lang.T(Account.balanceCOMPUSideName(side), langObj));

                output.put("Label_TotalDebit", Lang.T(Account.balanceCOMPUSideName(Account.BALANCE_SIDE_DEBIT), langObj));
                output.put("Label_Left", Lang.T(Account.balanceCOMPUSideName(Account.BALANCE_SIDE_LEFT), langObj));
                output.put("Label_TotalCredit", Lang.T(Account.balanceCOMPUSideName(Account.BALANCE_SIDE_CREDIT), langObj));
                output.put("Label_TotalForged", Lang.T(Account.balanceCOMPUSideName(Account.BALANCE_SIDE_FORGED), langObj));

                output.put("Side_Help", Lang.T("Side_Help_COMPU_BONUS", langObj));

                if (side == Account.BALANCE_SIDE_FORGED) {
                    // Это запрос на баланса Нафоржили - он в 5-й позиции на стороне 2
                    position = TransactionAmount.ACTION_PLEDGE;
                    side = Account.BALANCE_SIDE_LEFT;
                }

            }

        }


        BigDecimal sum = PersonCls.getBalance(personKey, assetKey, position, side);
        output.put("sum", sum);

        return output;
    }

    private Map jsonQueryPersonStatus(Long personKey, Long statusKey, int position, boolean history) {

        output.put("type", "person_status");
        output.put("search", "persons");

        if (position < 1 || position > 5) {
            output.put("error", "wrong position");
            return output;
        }

        PersonCls person = (PersonCls) dcSet.getItemPersonMap().get(new Long(personKey));
        if (person == null) {
            output.put("error", "person not found");
            return output;
        }

        StatusCls status = (StatusCls) dcSet.getItemStatusMap().get(new Long(statusKey));
        if (status == null) {
            output.put("error", "person not found");
            return output;
        }

        byte[] b = person.getImage();
        String a = Base64.encodeBase64String(b);

        output.put("Label_key", Lang.T("Key", langObj));
        output.put("Label_name", Lang.T("Name", langObj));
        output.put("Label_result", Lang.T("Result", langObj));
        output.put("Label_denied", Lang.T("DENIED", langObj));
        output.put("Label_sum", Lang.T("SUM", langObj));
        output.put("Label_from", Lang.T("From #date", langObj));
        output.put("Label_to", Lang.T("To #date", langObj));
        output.put("Label_creator", Lang.T("Creator", langObj));

        output.put("Label_data", Lang.T("Data # данные", langObj));

        output.put("person_img", a);
        output.put("person_key", person.getKey());
        output.put("person_name", person.viewName());

        output.put("status_key", status.getKey());
        output.put("status_name", status.viewName());

        output.put("Label_status", Lang.T("Status", langObj));
        output.put("Label_person", Lang.T("Person", langObj));
        output.put("Label_transaction", Lang.T("Transaction", langObj));

        //BigDecimal sum = PersonCls.getBalance(personKey, statusKey, position);
        KKPersonStatusMap map = DCSet.getInstance().getPersonStatusMap();
        TreeMap<Long, Stack<Fun.Tuple5<Long, Long, byte[], Integer, Integer>>> statuses = map.get(personKey);
        if (statuses == null) {
            output.put("error", "person statuses not found");
            return output;
        }

        Stack<Fun.Tuple5<Long, Long, byte[], Integer, Integer>> statusValue = statuses.get(statusKey);
        if (statusValue == null || statusValue.isEmpty()) {
            output.put("error", "person status not found");
            return output;
        }

        Transaction transaction;
        ItemStatusMap itemStatusMap = dcSet.getItemStatusMap();

        if (status.isUnique()) {
            // это уникальный статус - у него только последнее значение является действующим
            // остальные - как ситория изменения храним

            /// start Timestamp, end Timestamp, DATA, Block, SeqNo
            // нельзя изменять сам обзект с помощью POP - так как он в КЭШЕ изменяется тоже
            Fun.Tuple5<Long, Long, byte[], Integer, Integer> last = statusValue.peek(); // .pop()

            Map currentStatus = new HashMap();
            currentStatus.put("text", itemStatusMap.get(statusKey).toString(dcSet, last.c));
            if (last.a != null && last.a > Long.MIN_VALUE)
                currentStatus.put("beginTimestamp", last.a);
            if (last.b != null && last.b < Long.MAX_VALUE)
                currentStatus.put("endTimestamp", last.b);
            currentStatus.put("params", RSetStatusToItem.unpackDataJSON(last.c));
            currentStatus.put("txBlock", last.d);
            currentStatus.put("txSeqNo", last.e);
            transaction = dcSet.getTransactionFinalMap().get(last.d, last.e);
            currentStatus.put("creator", transaction.getCreator().getAddress());
            currentStatus.put("creator_name", transaction.getCreator().getPersonAsString());

            output.put("last", currentStatus);

            output.put("Label_status_history", Lang.T("Update History", langObj));
            output.put("Label_current_state", Lang.T("Current State", langObj));

        } else {
            output.put("Label_statuses_list", Lang.T("Statuses List", langObj));
        }

        if (!status.isUnique() || history) {
            JSONArray historyJSON = new JSONArray();

            // нельзя изменять сам обзект с помощью POP - так как он в КЭШЕ изменяется тоже
            Iterator<Tuple5<Long, Long, byte[], Integer, Integer>> iterator = statusValue.iterator(); // .pop();
            int size = statusValue.size();
            int i = 0;
            while (iterator.hasNext()) {
                if (status.isUnique() && ++i == size) {
                    // пропустим последнее значение - оно уже взято было как текущее
                    break;
                }
                Fun.Tuple5<Long, Long, byte[], Integer, Integer> item = iterator.next();

                JSONObject historyItemJSON = new JSONObject();

                transaction = dcSet.getTransactionFinalMap().get(item.d, item.e);
                historyItemJSON.put("creator", transaction.getCreator().getAddress());
                historyItemJSON.put("creator_name", transaction.getCreator().getPersonAsString());

                historyItemJSON.put("text", itemStatusMap.get(statusKey).toString(dcSet, item.c));
                if (item.a != null && item.a > Long.MIN_VALUE)
                    historyItemJSON.put("beginTimestamp", item.a);
                if (item.b != null && item.b < Long.MAX_VALUE)
                    historyItemJSON.put("endTimestamp", item.b);
                historyItemJSON.put("params", RSetStatusToItem.unpackDataJSON(item.c));
                historyItemJSON.put("txBlock", item.d);
                historyItemJSON.put("txSeqNo", item.e);

                historyJSON.add(0, historyItemJSON);
            }
            output.put("history", historyJSON);
        }

        return output;
    }

    private void duplicateCodeAssets(Map assetsJSON, AssetCls asset) {
        Map assetJSON = new LinkedHashMap();

        assetJSON.put("key", asset.getKey());
        assetJSON.put("name", asset.viewName());
        if (asset.getKey() > 0 && asset.getKey() < 1000) {
            assetJSON.put("description", Lang.T(asset.viewDescription(), langObj));
        } else {
            assetJSON.put("description", asset.viewDescription());
        }
        assetJSON.put("owner", asset.getOwner().getAddress());
        assetJSON.put("quantity", NumberAsString.formatAsString(asset.getQuantity()));
        assetJSON.put("released", NumberAsString.formatAsString(asset.getReleased(dcSet)));
        assetJSON.put("scale", asset.getScale());
        assetJSON.put("assetType", Lang.T(asset.viewAssetType(), langObj));
        assetJSON.put("assetTypeFull", Lang.T(asset.viewAssetTypeFull(), langObj));
        ///assetJSON.put("img", Base64.encodeBase64String(asset.getImage()));
        assetJSON.put("icon", Base64.encodeBase64String(asset.getIcon()));
        List<Order> orders = dcSet
                .getOrderMap().getOrders(asset.getKey());
        List<Trade> trades = dcSet.getTradeMap()
                .getTrades(asset.getKey());
        assetJSON.put("operations", orders.size() + trades.size());
        assetsJSON.put(asset.getKey(), assetJSON);
    }

    private Map jsonQueryItemPerson(String first) {
        output.put("type", "person");
        output.put("search", "persons");

        PersonCls person = (PersonCls) dcSet.getItemPersonMap().get(new Long(first));
        if (person == null) {
            return new HashMap(2);
        }

        output.put("charKey", person.getItemTypeChar());
        output.put("label_Actions", Lang.T("Actions", langObj));
        output.put("label_Authorship", Lang.T("Authorship", langObj));
        output.put("label_RAW", Lang.T("Bytecode", langObj));

        Tuple2<Map, Transaction> itemBase = itemBase(person);
        Map output = itemBase.a;

        output.put("Label_key", Lang.T("Key", langObj));
        output.put("Label_name", Lang.T("Name", langObj));
        output.put("Label_creator", Lang.T("Creator", langObj));
        output.put("Label_registrar", Lang.T("Registrar", langObj));
        output.put("Label_born", Lang.T("Birthday", langObj));
        output.put("Label_gender", Lang.T("Gender", langObj));
        output.put("Label_total_registered", Lang.T("Registered", langObj));
        output.put("Label_total_certified", Lang.T("Certified", langObj));
        output.put("Label_description", Lang.T("Description", langObj));

        output.put("creator", person.getOwner().getAddress());
        if (person.getOwner().getPerson() != null) {
            output.put("creator_key", person.getOwner().getPerson().b.getKey());
            output.put("creator_name", person.getOwner().getPerson().b.viewName());
        } else {
            output.put("creator_key", "");
            output.put("creator_name", "");
        }

        // уже есть в карте это значение
        Transaction transaction = itemBase.b;
        output.put("registrar", transaction.getCreator().getAddress());
        if (transaction.getCreator().getPerson() != null) {
            output.put("registrar_key", transaction.getCreator().getPerson().b.getKey());
            output.put("registrar_name", transaction.getCreator().getPerson().b.viewName());
        } else {
            output.put("registrar_key", "");
            output.put("registrar_name", "");
        }

        output.put("birthday", person.getBirthdayStr());
        if (!person.isAlive(0L)) {
            output.put("deathday", person.getDeathdayStr());
            output.put("Label_dead", Lang.T("Deathday", langObj));

        }

        String gender = Lang.T("Man", langObj);
        if (person.getGender() == 0) {
            gender = Lang.T("Man", langObj);
        } else if (person.getGender() == 1) {
            gender = Lang.T("Woman", langObj);
        } else {
            gender = Lang.T("-", langObj);
        }
        output.put("gender", gender);

        // statuses

        JSONArray statusesJSON = new JSONArray();

        ///WebPersonStatusesModel statusModel = new WebPersonStatusesModel(person.getKey());
        TreeMap<Long, Stack<Tuple5<Long, Long, byte[], Integer, Integer>>> statuses = dcSet.getPersonStatusMap().get(person.getKey());
        if (!statuses.isEmpty()) {

            output.put("Label_statuses", Lang.T("Statuses", langObj));
            output.put("Label_Status_table_status", Lang.T("Status", langObj));
            output.put("Label_Status_table_period", Lang.T("Period", langObj));
            output.put("Label_Status_table_appointing", Lang.T("Appointing", langObj));
            output.put("Label_Status_table_seqNo", Lang.T("SeqNo", langObj));

            int block;
            int seqNo;
            Transaction tx;
            for (Tuple3<Long, StatusCls, Fun.Tuple5<Long, Long, byte[], Integer, Integer>> item : StatusCls.getSortedItems(statuses)) {
                Map statusJSON = new LinkedHashMap();
                StatusCls status = item.b;

                statusJSON.put("status_key", item.a);
                statusJSON.put("status_icon", Base64.encodeBase64String(status.getIcon()));

                statusJSON.put("status_name", status.toString(dcSet, item.c.c));

                statusJSON.put("status_period", StatusCls.viewPeriod(item.c.a, item.c.b));

                block = item.c.d;
                seqNo = item.c.e;
                statusJSON.put("status_seqNo", Transaction.viewDBRef(block, seqNo));

                tx = Transaction.findByHeightSeqNo(dcSet, block, seqNo);
                Account creator = tx.getCreator();
                if (creator != null) {
                    statusJSON.put("status_creator", creator.getAddress());
                    if (creator.isPerson()) {
                        statusJSON.put("status_creator_name", creator.getPerson().b.viewName());
                    } else {
                        statusJSON.put("status_creator_name", "");
                    }
                } else {
                    statusJSON.put("status_creator", GenesisBlock.CREATOR.getAddress());
                    statusJSON.put("status_creator_name", "GENESIS");
                }

                statusesJSON.add(statusJSON);
            }

            output.put("statuses", statusesJSON);
        }

        // accounts

        Map accountsJSON = new LinkedHashMap();

        List<Transaction> myIssuePersons = new ArrayList<Transaction>();

        // НОВЫЙ ЛАД - без Обсерверов и Модели
        TreeMap<String, Stack<Tuple3<Integer, Integer, Integer>>> addresses = DCSet.getInstance().getPersonAddressMap().getItems(person.getKey());

        if (!addresses.isEmpty()) {

            output.put("Label_accounts", Lang.T("Accounts", langObj));
            output.put("Label_accounts_table_address", Lang.T("Address", langObj));
            output.put("Label_accounts_table_to_date", Lang.T("To Date", langObj));
            output.put("Label_accounts_table_verifier", Lang.T("Account Verifier", langObj));

            TransactionFinalMap transactionsMap = DCSet.getInstance().getTransactionFinalMap();
            BigDecimal eraBalanceA = new BigDecimal(0);
            BigDecimal eraBalanceB = new BigDecimal(0);
            BigDecimal eraBalanceC = new BigDecimal(0);
            BigDecimal eraBalanceTotal = new BigDecimal(0);
            BigDecimal compuBalance = new BigDecimal(0);
            BigDecimal liaBalanceA = new BigDecimal(0);
            BigDecimal liaBalanceB = new BigDecimal(0);

            int i = 0;
            for (String address : addresses.keySet()) {

                Stack<Tuple3<Integer, Integer, Integer>> stack = addresses.get(address);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }

                Tuple3<Integer, Integer, Integer> item = stack.peek();
                Transaction transactionIssue = transactionsMap.get(item.b, item.c);

                Map accountJSON = new LinkedHashMap();
                accountJSON.put("address", address);
                accountJSON.put("to_date", item.a * 86400000l);
                accountJSON.put("verifier", transactionIssue.getCreator().getAddress());
                if (transactionIssue.getCreator().getPerson() != null) {
                    accountJSON.put("verifier_key", transactionIssue.getCreator().getPerson().b.getKey());
                    accountJSON.put("verifier_name", transactionIssue.getCreator().getPerson().b.viewName());
                } else {
                    accountJSON.put("verifier_key", "");
                    accountJSON.put("verifier_name", "");
                }

                accountsJSON.put(i++, accountJSON);

                Account account = new Account(address);
                List<Transaction> issuedPersons = transactionsMap.getTransactionsByAddressAndType(account.getShortAddressBytes(),
                        Transaction.ISSUE_PERSON_TRANSACTION, 200, 0);
                if (issuedPersons != null) {
                    myIssuePersons.addAll(issuedPersons);
                }

                Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> balance
                        = account.getBalance(AssetCls.ERA_KEY);

                eraBalanceA = eraBalanceA.add(balance.a.b);
                eraBalanceB = eraBalanceB.add(balance.b.b);
                eraBalanceC = eraBalanceC.add(balance.c.b);
                eraBalanceTotal = eraBalanceA.add(eraBalanceB).add(eraBalanceC);

                balance = account.getBalance(AssetCls.FEE_KEY);
                compuBalance = compuBalance.add(balance.a.b);

                balance = account.getBalance(AssetCls.LIA_KEY);
                liaBalanceA = liaBalanceA.add(balance.a.b);
                liaBalanceB = liaBalanceB.add(balance.b.b);
            }
            output.put("era_balance_a", NumberAsString.formatAsString(eraBalanceA));
            output.put("era_balance_b", NumberAsString.formatAsString(eraBalanceB));
            output.put("era_balance_c", NumberAsString.formatAsString(eraBalanceC));
            output.put("era_balance_total", NumberAsString.formatAsString(eraBalanceTotal));
            output.put("compu_balance", NumberAsString.formatAsString(compuBalance));
            output.put("lia_balance_a", NumberAsString.formatAsString(liaBalanceA));
            output.put("lia_balance_b", NumberAsString.formatAsString(liaBalanceB));
        }

        output.put("accounts", accountsJSON);

        // my persons

        output.put("Label_My_Persons", Lang.T("My Persons", langObj));
        output.put("Label_accounts_table_date", Lang.T("Creation Date", langObj));
        output.put("Label_My_Person_key", Lang.T("Key", langObj));
        output.put("Label_My_Persons_Name", Lang.T("Name", langObj));

        Map myPersonsJSON = new LinkedHashMap();

        if (myIssuePersons != null) {
            int i = 0;
            for (Transaction myIssuePerson : myIssuePersons) {
                Map myPersonJSON = new LinkedHashMap();
                IssueItemRecord record = (IssueItemRecord) myIssuePerson;
                if (record.isWiped())
                    continue;

                ItemCls item = record.getItem();
                ///logger.warn(item.viewName());

                myPersonJSON.put("key", item.getKey(dcSet));
                myPersonJSON.put("name", item.viewName());

                myPersonJSON.put("seqNo", myIssuePerson.viewHeightSeq());
                myPersonJSON.put("timestamp", myIssuePerson.getTimestamp());

                myPersonsJSON.put(i, myPersonJSON);
                i++;
            }
        }

        output.put("My_Persons", myPersonsJSON);

        return output;
    }


    private Map jsonLastBlock() {

        Map output = new LinkedHashMap();

        Block.BlockHead lastBlockHead = getLastBlockHead();

        output.put("height", lastBlockHead.heightBlock);
        output.put("timestamp", lastBlockHead.getTimestamp());

        //output.put("timezone", Settings.getInstance().getTimeZone());
        //output.put("timeformat", Settings.getInstance().getTimeFormat());
        output.put("label_hour", Lang.T("hour", langObj));
        output.put("label_hours", Lang.T("hours", langObj));
        output.put("label_mins", Lang.T("mins", langObj));
        output.put("label_min", Lang.T("min", langObj));
        output.put("label_secs", Lang.T("secs", langObj));
        output.put("label_ago", Lang.T("ago", langObj));
        output.put("label_Last_processed_block",
                Lang.T("Last processed block", langObj));

        return output;
    }

    public Map jsonQueryTopRichest100(int limit, long assetKey) {

        output.put("type", "top");
        output.put("search_placeholder", Lang.T("Type asset key", langObj));

        Map output = new LinkedHashMap();
        Map balances = new LinkedHashMap();
        BigDecimal all = BigDecimal.ZERO;
        BigDecimal alloreders = BigDecimal.ZERO;

        List<Tuple5<String, BigDecimal, BigDecimal, BigDecimal, BigDecimal>> top100s = new ArrayList();

        ItemAssetBalanceMap map = dcSet.getAssetBalanceMap();
        //BigDecimal total = BigDecimal.ZERO;
        //BigDecimal totalNeg = BigDecimal.ZERO;
        byte[] key;
        Crypto crypto = Crypto.getInstance();

        try (IteratorCloseable<byte[]> iterator = map.getIteratorByAsset(assetKey)) {
            while (iterator.hasNext()) {
                key = iterator.next();

                try {
                    Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
                            balance = map.get(key);

                    // пустые не берем
                    if (balance.a.b.signum() == 0 && balance.b.b.signum() == 0 && balance.c.b.signum() == 0 && balance.d.b.signum() == 0)
                        continue;

                    top100s.add(Fun.t5(crypto.getAddressFromShort(ItemAssetBalanceMap.getShortAccountFromKey(key)),
                            balance.a.b, balance.b.b, balance.c.b, balance.d.b));
                } catch (java.lang.ArrayIndexOutOfBoundsException e) {
                    logger.error("Wrong key raw: ");
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        Collection<Order> orders = dcSet.getOrderMap().getOrders(assetKey);

        for (Order order : orders) {
            alloreders = alloreders.add(order.getAmountHaveLeft());
        }

        Collections.sort(top100s, new ReverseComparator(new BigDecimalComparator_top100()));

        int couter = 0;
        AssetCls asset = Controller.getInstance().getAsset(assetKey);

        for (Tuple5<String, BigDecimal, BigDecimal, BigDecimal, BigDecimal> top100 : top100s) {

            couter++;

            Account account = new Account(top100.a);

            Map balance = new LinkedHashMap();
            balance.put("address", top100.a);
            balance.put("OWN", top100.b.toPlainString());
            balance.put("DEBT", top100.c.toPlainString());
            balance.put("HOLD", top100.d.toPlainString());
            balance.put("SPEND", top100.e.toPlainString());

            Tuple2<Integer, PersonCls> person = account.getPerson();
            if (person != null) {
                balance.put("person", person.b.viewName());
                balance.put("person_key", person.b.getKey());
            }

            balances.put(couter, balance);

            if (limit > 0 && couter >= limit) // && limit != -2 && limit != -1) // -2 = all
            {
                break;
            }
        }

        if (asset == null) {
            output.put("total", "--");// (all.add(alloreders)).toPlainString());
            output.put("released", "--");
            output.put("assetName", "--");
            output.put("Label_Title", (Lang.T("Top %limit% %assetName% Richest", langObj)
                    .replace("%limit%", String.valueOf(limit > 0 ? limit : ""))).replace("%assetName%", "--"));
            output.put("Label_All_non",
                    (Lang.T("All non-empty %assetName% accounts (%count%)", langObj)
                            .replace("%assetName%", "--")).replace("%count%", String.valueOf(couter)));
            output.put("Label_All_accounts",
                    (Lang.T("All %assetName% accounts (%count%)", langObj)
                            .replace("%assetName%", "--")).replace("%count%", String.valueOf(couter)));
        } else {
            if (asset.getQuantity() > 0) {
                output.put("total", asset.getQuantity());
            } else {
                output.put("total", asset.getReleased(dcSet).toPlainString());
            }
            output.put("released", asset.getReleased(dcSet).toPlainString());
            output.put("assetName", asset.viewName());
            output.put("Label_Title", (Lang.T("Top %limit% %assetName% Richest", langObj)
                    .replace("%limit%", String.valueOf(limit > 0 ? limit : ""))).replace("%assetName%", asset.viewName()));
            output.put("Label_All_non",
                    (Lang.T("All non-empty %assetName% accounts (%count%)", langObj)
                            .replace("%assetName%", asset.viewName())).replace("%count%", String.valueOf(couter)));
            output.put("Label_All_accounts",
                    (Lang.T("All %assetName% accounts (%count%)", langObj)
                            .replace("%assetName%", asset.viewName())).replace("%count%", String.valueOf(couter)));
        }
        output.put("Label_Table_Account", Lang.T("Account", langObj));
        output.put("label_Balance_1", Lang.T("OWN (1)", langObj));
        output.put("label_Balance_2", Lang.T("DEBT (2)", langObj));
        output.put("label_Balance_3", Lang.T("HOLD (3)", langObj));
        output.put("label_Balance_4", Lang.T("SPEND (4)", langObj));
        output.put("Label_Table_Prop", Lang.T("Prop.", langObj));
        output.put("Label_Table_person", Lang.T("Owner", langObj));

        output.put("Label_Released", Lang.T("released", langObj));
        output.put("Label_in_order", Lang.T("in order", langObj));

        output.put("Label_Top", Lang.T("Top", langObj));

        output.put("allinOrders", alloreders.stripTrailingZeros().toPlainString());
        output.put("assetKey", assetKey);
        output.put("limit", limit);
        output.put("count", couter);

        output.put("top", balances);
        output.put("Label_Total_coins_in_the_system",
                Lang.T("Total asset units in the system", langObj));

        return output;
    }

    public Map jsonQueryTopRichest(UriInfo info) {
        String limitStr = info.getQueryParameters().getFirst("top");
        int limit = 100;
        if (limitStr.equals("all")) {
            limit = 0;
        } else {
            try {
                limit = Integer.valueOf(limitStr);
            } catch (Exception eee) {
            }
        }

        long key = 1l;
        if (info.getQueryParameters().containsKey("asset"))
            key = Long.valueOf(info.getQueryParameters().getFirst("asset"));

        return jsonQueryTopRichest100(limit, key);
    }


    @SuppressWarnings("static-access")
    private LinkedHashMap balanceJSON(Account account, int side) {

        // balance assets from
        LinkedHashMap output = new LinkedHashMap();

        ItemAssetMap assetsMap = DCSet.getInstance().getItemAssetMap();
        ItemAssetBalanceMap map = DCSet.getInstance().getAssetBalanceMap();

        TreeMap balAssets = new TreeMap();
        byte[] key;
        try (IteratorCloseable<byte[]> iterator = map.getIteratorByAccount(account)) {
            if (iterator != null) {
                while (iterator.hasNext()) {

                    key = iterator.next();

                    long assetKey = ItemAssetBalanceMap.getAssetKeyFromKey(key);
                    if (assetKey == AssetCls.LIA_KEY) {
                        continue;
                    }

                    AssetCls asset = assetsMap.get(assetKey);
                    if (asset == null)
                        continue;

                    Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>
                            itemBals = map.get(key);

                    if (itemBals == null)
                        continue;

                    Map bal = new LinkedHashMap();
                    bal.put("asset_key", assetKey);
                    bal.put("asset_name", asset.viewName());


                    if (BlockChain.ERA_COMPU_ALL_UP && side == Account.BALANCE_SIDE_LEFT) {
                        bal.put("balance_1", Account.balanceInPositionAndSide(itemBals, 1, side)
                                .add(account.addDEVAmount(assetKey)));
                    } else {
                        bal.put("balance_1", Account.balanceInPositionAndSide(itemBals, 1, side));
                    }

                    bal.put("balance_2", Account.balanceInPositionAndSide(itemBals, 2, side));
                    bal.put("balance_3", Account.balanceInPositionAndSide(itemBals, 3, side));
                    bal.put("balance_4", Account.balanceInPositionAndSide(itemBals, 4, side));
                    balAssets.put("" + assetKey, bal);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        output.put("balances", balAssets);
        output.put("side", side);

        output.put("Side_Help", Lang.T("Side_Help", langObj));
        output.put("Label_TotalDebit", Lang.T("Total Debit", langObj));
        output.put("Label_Left", Lang.T("Left # остаток", langObj));
        output.put("Label_TotalCredit", Lang.T("Total Credit", langObj));

        output.put("label_Balance_table", Lang.T("Balance", langObj));
        output.put("label_asset_key", Lang.T("Key", langObj));
        output.put("label_asset_name", Lang.T("Name", langObj));

        output.put("label_Balance_1", Lang.T("OWN (1)", langObj));
        output.put("label_Balance_2", Lang.T("DEBT (2)", langObj));
        output.put("label_Balance_3", Lang.T("HOLD (3)", langObj));
        output.put("label_Balance_4", Lang.T("SPEND (4)", langObj));

        return output;

    }

    // dcSet
    public Map jsonUnitPrint(Object unit) { //, AssetNames assetNames) {

        Map transactionDataJSON = new LinkedHashMap();
        Map transactionJSON = new LinkedHashMap();

        if (unit instanceof Trade) {
            Trade trade = (Trade) unit;

            if (true) {
                transactionDataJSON = trade.toJson(0, false);
                Order orderInitiator = trade.getInitiatorOrder(dcSet);
                Order orderTarget = trade.getTargetOrder(dcSet);
                AssetCls haveAsset = Controller.getInstance().getAsset(orderInitiator.getHaveAssetKey());
                AssetCls wantAsset = Controller.getInstance().getAsset(orderInitiator.getWantAssetKey());
                transactionDataJSON.put("haveKey", haveAsset.getKey());
                transactionDataJSON.put("wantKey", wantAsset.getKey());

                transactionDataJSON.put("haveName", haveAsset.viewName());
                transactionDataJSON.put("wantName", wantAsset.viewName());

                transactionDataJSON.put("initiatorTxSeqNo", Transaction.viewDBRef(trade.getInitiator()));
                transactionDataJSON.put("targetTxSeqNo", Transaction.viewDBRef(trade.getTarget()));

                int height = (int) (trade.getInitiator() >> 32);
                transactionDataJSON.put("height", trade.getInitiator() >> 32);
                transactionDataJSON.put("confirmations", Controller.getInstance().getMyHeight() - height);

                transactionDataJSON.put("timestamp", Transaction.getTimestampByDBRef(trade.getInitiator()));

                transactionDataJSON.put("initiatorCreator", orderInitiator.getCreator().getAddress());
                transactionDataJSON.put("initiatorCreatorName", orderInitiator.getCreator().getPersonAsString());
                transactionDataJSON.put("targetCreator", orderTarget.getCreator().getAddress());
                transactionDataJSON.put("targetCreatorName", orderTarget.getCreator().getPersonAsString());


            } else {
                Order orderInitiator = trade.getInitiatorOrder(dcSet);

                /*
                 * if(dcSet.getOrderMap().contains(trade.getInitiator())) {
                 * orderInitiator = dcSet.getOrderMap().get(trade.getInitiator()); }
                 * else { orderInitiator =
                 * dcSet.getCompletedOrderMap().get(trade.getInitiator()); }
                 */

                Order orderTarget = trade.getTargetOrder(dcSet);

                /*
                 * if(dcSet.getOrderMap().contains(trade.getTarget())) { orderTarget
                 * = dcSet.getOrderMap().get(trade.getTarget()); } else {
                 * orderTarget =
                 * dcSet.getCompletedOrderMap().get(trade.getTarget()); }
                 */

                transactionDataJSON.put("amount", trade.getAmountHave().toPlainString());
                transactionDataJSON.put("asset", trade.getHaveKey());

                transactionDataJSON.put("amountHave", trade.getAmountHave().toPlainString());
                transactionDataJSON.put("amountWant", trade.getAmountWant().toPlainString());

                transactionDataJSON.put("realPrice",
                        trade.getAmountWant().divide(trade.getAmountHave(), 8, RoundingMode.FLOOR).toPlainString());

                Transaction createOrder = this.dcSet.getTransactionFinalMap().get(orderInitiator.getId());
                transactionDataJSON.put("initiatorTxSignature", Base58.encode(createOrder.getSignature()));

                transactionDataJSON.put("initiatorCreator", orderInitiator.getCreator());
                transactionDataJSON.put("initiatorAmount", orderInitiator.getAmountHave().toPlainString());
                transactionDataJSON.put("initiatorHaveKey", orderInitiator.getHaveAssetKey());
                transactionDataJSON.put("initiatorWantKey", orderInitiator.getWantAssetKey());

            /*
            if (assetNames != null) {
                assetNames.setKey(orderInitiator.getHaveAssetKey());
                assetNames.setKey(orderInitiator.getWantAssetKey());
            }
            */

                Transaction createOrderTarget = this.dcSet.getTransactionFinalMap().get(orderTarget.getId());
                transactionDataJSON.put("targetTxSignature", Base58.encode(createOrderTarget.getSignature()));
                transactionDataJSON.put("targetCreator", orderTarget.getCreator());
                transactionDataJSON.put("targetAmount", orderTarget.getAmountHave().toPlainString());

                transactionDataJSON.put("height", createOrderTarget.getBlockHeight());
                transactionDataJSON.put("confirmations", createOrderTarget.getConfirmations(DCSet.getInstance()));

                transactionDataJSON.put("timestamp", trade.getInitiator());

            }

            transactionJSON.put("type", "trade");
            transactionJSON.put("trade", transactionDataJSON);
            return transactionJSON;
        }

        if (unit instanceof Transaction) {
            Transaction transaction = (Transaction) unit;

            transactionDataJSON = transaction.toJson();
            // transactionDataJSON.put("Р ВµРЎв‚¬РЎРЉРЎС“РЎвЂ№Р ВµРЎвЂћ",
            // GZIP.webDecompress(transactionDataJSON.get("value").toString()));

            if (transaction.getType() == Transaction.CANCEL_ORDER_TRANSACTION) {
                Order order;
                CancelOrderTransaction cancelOrder = (CancelOrderTransaction) unit;
                Long orderID = cancelOrder.getOrderID();
                if (orderID == null) {
                    byte[] orderSignature = cancelOrder.getorderSignature();
                    CreateOrderTransaction createOrder;
                    if (dcSet.getTransactionFinalMapSigns().contains(orderSignature)) {
                        createOrder = (CreateOrderTransaction) dcSet.getTransactionFinalMap().get(orderSignature);
                    } else {
                        createOrder = (CreateOrderTransaction) dcSet.getTransactionTab().get(orderSignature);
                    }
                    if (createOrder != null) {
                        Map orderJSON = new LinkedHashMap();

                    /*
                    if (assetNames != null) {
                        assetNames.setKey(order.getHaveAssetKey());
                        assetNames.setKey(order.getWantAssetKey());
                    }
                    */

                        orderJSON.put("have", createOrder.getHaveKey());
                        orderJSON.put("want", createOrder.getWantKey());

                        orderJSON.put("amount", createOrder.getAmountHave().toPlainString());
                        orderJSON.put("amountLeft", "??");
                        orderJSON.put("amountWant", createOrder.getAmountWant().toPlainString());
                        orderJSON.put("price", Order.calcPrice(createOrder.getAmountHave(),
                                createOrder.getAmountWant(), 8).toPlainString());

                        transactionDataJSON.put("orderSource", orderJSON);
                    }
                } else {
                    if (dcSet.getCompletedOrderMap().contains(orderID)) {
                        order = dcSet.getCompletedOrderMap().get(orderID);
                    } else {
                        order = dcSet.getOrderMap().get(orderID);
                    }

                    Map orderJSON = new LinkedHashMap();

                    /*
                    if (assetNames != null) {
                        assetNames.setKey(order.getHaveAssetKey());
                        assetNames.setKey(order.getWantAssetKey());
                    }
                    */

                    orderJSON.put("have", order.getHaveAssetKey());
                    orderJSON.put("want", order.getWantAssetKey());

                    orderJSON.put("amount", order.getAmountHave().toPlainString());
                    orderJSON.put("amountLeft", order.getAmountHaveLeft().toPlainString());
                    orderJSON.put("amountWant", order.getAmountWant().toPlainString());
                    orderJSON.put("price", order.getPrice().toPlainString());

                    transactionDataJSON.put("orderSource", orderJSON);

                }

            } else if (transaction.getType() == Transaction.ISSUE_ASSET_TRANSACTION) {
                /*
                if (transaction.getSeqNo() > 0 && assetNames != null) {
                    // IS CONFIRMED
                    long assetkey = ((IssueAssetTransaction) transaction).getItem().getKey();
                    transactionDataJSON.put("asset", assetkey);
                    transactionDataJSON.put("assetName", ((IssueAssetTransaction) transaction).getItem().viewName());
                }
                */
            } else if (transaction.getType() == Transaction.SEND_ASSET_TRANSACTION) {
                /*
                if (transaction.getSeqNo() > 0 && assetNames != null) {
                    long assetkey = ((RSend) unit).getAbsKey();
                    transactionDataJSON.put("asset", assetkey);
                    transactionDataJSON.put("assetName", assetNames.getMap().get(assetkey));
                }

                if (((RSend) unit).isEncrypted()) {
                    transactionDataJSON.put("data", "encrypted");
                }
                */

            } else if (transaction.getType() == Transaction.HASHES_RECORD) {

            } else if (transaction.getType() == Transaction.MULTI_PAYMENT_TRANSACTION) {
                Map<Long, BigDecimal> totalAmountOfAssets = new TreeMap<Long, BigDecimal>();

                for (Payment payment : ((MultiPaymentTransaction) transaction).getPayments()) {
                    BigDecimal amount = BigDecimal.ZERO;
                    if (totalAmountOfAssets.containsKey(payment.getAsset())) {
                        amount = totalAmountOfAssets.get(payment.getAsset());
                    }
                    amount = amount.add(payment.getAmount());

                    /*
                    if (assetNames != null) {
                        assetNames.setKey(payment.getAsset());
                    }
                    */

                    totalAmountOfAssets.put(payment.getAsset(), amount);
                }

                Map amountOfAssetsJSON = new LinkedHashMap();

                for (Map.Entry<Long, BigDecimal> assetInfo : totalAmountOfAssets.entrySet()) {
                    amountOfAssetsJSON.put(assetInfo.getKey(), assetInfo.getValue().toPlainString());
                }

                transactionDataJSON.put("amounts", amountOfAssetsJSON);

            } else if (transaction.getType() == Transaction.ARBITRARY_TRANSACTION) {
                Map<Long, BigDecimal> totalAmountOfAssets = new TreeMap<Long, BigDecimal>();

                for (Payment payment : ((ArbitraryTransaction) transaction).getPayments()) {
                    BigDecimal amount = BigDecimal.ZERO;
                    if (totalAmountOfAssets.containsKey(payment.getAsset())) {
                        amount = totalAmountOfAssets.get(payment.getAsset());
                    }
                    amount = amount.add(payment.getAmount());

                    /*
                    if (assetNames != null) {
                        assetNames.setKey(payment.getAsset());
                    }
                    */

                    totalAmountOfAssets.put(payment.getAsset(), amount);
                }

                Map amountOfAssetsJSON = new LinkedHashMap();

                for (Map.Entry<Long, BigDecimal> assetInfo : totalAmountOfAssets.entrySet()) {
                    amountOfAssetsJSON.put(assetInfo.getKey(), assetInfo.getValue().toPlainString());
                }

                transactionDataJSON.put("amounts", amountOfAssetsJSON);

                /*
            } else if (transaction.getType() == Transaction.VOTE_ON_POLL_TRANSACTION) {
                Poll poll = Controller.getInstance().getPoll(((VoteOnPollTransaction) transaction).getPoll());
                if (poll != null) {
                    transactionDataJSON.put("optionString",
                            Controller.getInstance().getPoll(((VoteOnPollTransaction) transaction).getPoll()).getOptions()
                                    .get(((VoteOnPollTransaction) transaction).getOption()).viewName());
                }

            } else if (transaction.getType() == Transaction.CREATE_ORDER_TRANSACTION) {
                /*
                if (assetNames != null) {
                    assetNames.setKey(((CreateOrderTransaction) transaction).getHaveKey());
                    assetNames.setKey(((CreateOrderTransaction) transaction).getWantKey());
                }
                */

            } else if (transaction.getType() == Transaction.DEPLOY_AT_TRANSACTION) {
                transactionDataJSON.put("atAddress",
                        ((DeployATTransaction) transaction).getATaccount(dcSet).getAddress());
            }

            if (transaction.isConfirmed(dcSet)) {
                transactionDataJSON.put("blockHeight", transaction.getBlockHeight());
            }

            transactionDataJSON.put("timestamp", transaction.getTimestamp());

            transactionJSON.put("type", "tx");
            transactionJSON.put("transaction", transactionDataJSON);
            return transactionJSON;
        }

        if (unit instanceof Block) {
            Block block = (Block) unit;

            transactionDataJSON = new LinkedHashMap();

            int height = block.getHeight();
            transactionDataJSON.put("confirmations", getHeight() - height + 1);
            transactionDataJSON.put("height", height);
            // height
            transactionDataJSON.put("timestamp", block.getTimestamp(height));

            transactionDataJSON.put("generator", block.getCreator().getAddress());
            transactionDataJSON.put("signature", Base58.encode(block.getSignature()));

            /*
             * transactionDataJSON.put("generatingBalance",
             * block.getGeneratingBalance()); transactionDataJSON.put("atFees",
             * block.getATfee()); transactionDataJSON.put("reference",
             * Base58.encode(block.getReference()));
             * transactionDataJSON.put("generatorSignature",
             * Base58.encode(block.getGeneratorSignature()));
             * transactionDataJSON.put("transactionsSignature",
             * block.getTransactionsSignature());
             * transactionDataJSON.put("version", block.getVersion());
             */

            // transactionDataJSON.put("fee", balances[size -
            // counter].getTransactionBalance().get(0l).toPlainString());
            transactionDataJSON.put("fee", block.viewFeeAsBigDecimal());

            transactionJSON.put("type", "block");
            transactionJSON.put("block", transactionDataJSON);
            return transactionJSON;

        }

        if (unit instanceof ATTransaction) {
            ATTransaction aTtransaction = (ATTransaction) unit;
            transactionDataJSON = aTtransaction.toJSON();

            Block block = Controller.getInstance().getBlockByHeight(aTtransaction.getBlockHeight());
            long timestamp = block.getTimestamp();
            transactionDataJSON.put("timestamp", timestamp);

            transactionDataJSON.put("confirmations", getHeight() - ((ATTransaction) unit).getBlockHeight() + 1);

            if (((ATTransaction) unit).getRecipient().equals("1111111111111111111111111")) {
                transactionDataJSON.put("generatorAddress", block.getCreator().getAddress());
            }

            transactionJSON.put("type", "atTransaction");
            transactionJSON.put("atTransaction", transactionDataJSON);
            return transactionJSON;
        }

        return transactionJSON;
    }

    private void txCountJSONPut(int[] txsTypeCount, int txsCount, Map txCountJSON) {
        if (txsCount > 0) {
            txCountJSON.put("txsCount", txsCount);
            Map txTypeCountJSON = new LinkedHashMap();
            int n = 1;
            for (int txCount : txsTypeCount) {
                if (txCount > 0) {
                    txTypeCountJSON.put(n, txCount);
                }
                n++;
            }
            txCountJSON.put("txsTypesCount", txTypeCountJSON);
        }
    }

    /*
    public Map jsonQueryBalance(String address) {
        Map output = new LinkedHashMap();

        if (!Crypto.getInstance().isValidAddress(address)) {
            output.put("error", "Address is not valid!");
            return output;
        }

        ItemAssetBalanceMap map = dcSet.getAssetBalanceMap();
        SortableList<byte[], Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> assetsBalances
                = map.getBalancesSortableList(new Account(address));

        for (Pair<byte[], Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> assetsBalance : assetsBalances) {
            Map assetBalance = new LinkedHashMap();

            assetBalance.put("assetName", Controller.getInstance().getAsset(map.getAssetKeyFromKey(assetsBalance.getA())).viewName());
            assetBalance.put("amount", assetsBalance.getB().toString());

            output.put(map.getAssetKeyFromKey(assetsBalance.getA()), assetBalance);
        }

        return output;
    }

    public Map<Long, Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>>
    assetBalance(String address) {
        Map<Long, Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> output = new LinkedHashMap();

        ItemAssetBalanceMap map = dcSet.getAssetBalanceMap();
        SortableList<byte[], Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> assetsBalances
                = map.getBalancesSortableList(new Account(address));

        for (Pair<byte[], Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>>> assetsBalance : assetsBalances) {
            output.put(map.getAssetKeyFromKey(assetsBalance.getA()), assetsBalance.getB());
        }

        return output;
    }

     */

    @SuppressWarnings({"serial", "static-access"})
    public void jsonQueryExchange(String filterStr, int start) {

        output.put("type", "exchange");
        output.put("search_placeholder", Lang.T("Type searching asset keys", langObj));

        List<Pair<Long, Long>> list = new ArrayList<>();
        HashSet<Pair<Long, Long>> pairsSet = new HashSet<>();

        if (BlockChain.TEST_MODE) {
            list.add(new Pair<Long, Long>(1L, 2L));
        } else {
            // BTC
            list.add(new Pair<Long, Long>(12L, 95L));
            list.add(new Pair<Long, Long>(12L, 92L));
            // ERA
            list.add(new Pair<Long, Long>(1L, 2L));
            list.add(new Pair<Long, Long>(1L, 12L));
            list.add(new Pair<Long, Long>(1L, 95L));
            list.add(new Pair<Long, Long>(1L, 92L));

            // COMPU
            list.add(new Pair<Long, Long>(2L, 12L));
            list.add(new Pair<Long, Long>(2L, 95L));
            list.add(new Pair<Long, Long>(2L, 92L));

            //GOLD
            list.add(new Pair<Long, Long>(21L, 12L));
            list.add(new Pair<Long, Long>(21L, 95L));
            list.add(new Pair<Long, Long>(21L, 92L));

            list.add(new Pair<Long, Long>(1010L, 92L));

        }

        pairsSet.addAll(list);

        OrderMap orders = dcSet.getOrderMap();
        TradeMap trades = dcSet.getTradeMap();

        JSONArray pairsArray = new JSONArray();

        for (Pair<Long, Long> pair : list) {

            AssetCls assetHave = Controller.getInstance().getAsset(pair.getA());
            if (assetHave == null)
                continue;
            AssetCls assetWant = Controller.getInstance().getAsset(pair.getB());
            if (assetWant == null)
                continue;

            Map pairJSON = new HashMap(100, 1);
            pairJSON.put("have", assetHave.jsonForExplorerPage(langObj));
            pairJSON.put("want", assetWant.jsonForExplorerPage(langObj));
            pairJSON.put("orders", orders.getCount(pair.getA(), pair.getB())
                    + orders.getCount(pair.getB(), pair.getA()));

            Trade trade = trades.getLastTrade(pair.getA(), pair.getB());
            //Order initiator
            if (trade == null) {
                pairJSON.put("last", "--");
            } else {
                if (trade.getHaveKey().equals(pair.getB())) {
                    pairJSON.put("last", trade.calcPrice().toPlainString());
                } else {
                    pairJSON.put("last", trade.calcPriceRevers().toPlainString());
                }
            }

            pairJSON.put("volume24", trades.getVolume24(pair.getA(), pair.getB()).toPlainString());

            pairsArray.add(pairJSON);
        }

        output.put("popularPairs", pairsArray);

        JSONArray tradesArray = new JSONArray();

        int count = 25;

        try (IteratorCloseable<Tuple2<Long, Long>> iterator = trades.getIterator(0, true)) {
            while (count-- > 0 && iterator.hasNext()) {
                Tuple2<Long, Long> key = iterator.next();
                Trade trade = trades.get(key);
                if (trade == null) {
                    Long error = null;
                }

                tradesArray.add(tradeJSON(trade, null, null));
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        output.put("lastTrades", tradesArray);

        output.put("label_table_PopularPairs", Lang.T("Most Popular Pairs", langObj));
        output.put("label_table_LastTrades", Lang.T("Last Trades", langObj));
        output.put("label_table_have", Lang.T("Base Asset", langObj));
        output.put("label_table_want", Lang.T("Price Asset", langObj));
        output.put("label_table_orders", Lang.T("Opened Orders", langObj));
        output.put("label_table_last_price", Lang.T("Last Price", langObj));
        output.put("label_table_volume24", Lang.T("Day Volume", langObj));

        output.put("label_Trade_Initiator", Lang.T("Trade Initiator", langObj));
        output.put("label_Position_Holder", Lang.T("Position Holder", langObj));
        output.put("label_Date", Lang.T("Date", langObj));
        output.put("label_Pair", Lang.T("Pair", langObj));
        output.put("label_Creator", Lang.T("Creator", langObj));
        output.put("label_Amount", Lang.T("Amount", langObj));
        output.put("label_Volume", Lang.T("Volume", langObj));
        output.put("label_Price", Lang.T("Price", langObj));
        output.put("label_Total_Cost", Lang.T("Total Cost", langObj));


    }

    @SuppressWarnings({"serial", "static-access"})
    public void jsonQueryTransactions(String filterStr, int start, UriInfo info) {

        output.put("type", "transactions");
        output.put("search_placeholder", Lang.T("Type searching words or signature or BlockNo-SeqNo", langObj));

        Object forge = info.getQueryParameters().getFirst("forge");
        boolean useForge = forge != null && (forge.toString().toLowerCase().equals("yes")
                || forge.toString().toLowerCase().equals("1"));

        TransactionFinalMapImpl map = dcSet.getTransactionFinalMap();
        boolean needFound = true;

        List<Transaction> transactions = new ArrayList<>();
        if (filterStr != null) {

            if (Base58.isExtraSymbols(filterStr)) {
                try {
                    Long dbRef = Transaction.parseDBRef(filterStr);
                    if (dbRef != null) {
                        Transaction one = map.get(dbRef);
                        if (one != null) {
                            transactions.add(one);
                            needFound = false;
                        }
                    }
                } catch (Exception e1) {
                }

            } else {
                try {
                    byte[] signature = Base58.decode(filterStr);
                    Transaction one = map.get(signature);
                    if (one != null) {
                        transactions.add(one);
                        needFound = false;
                    }
                } catch (Exception e2) {
                }
            }
        }

        if (needFound) {

            Long offset = checkAndGetLongParam(info, 0L, "offset");
            int intOffest;
            if (offset == null) {
                intOffest = 0;
            } else {
                intOffest = (int) (long) offset;
            }

            String fromSeqNoStr = info.getQueryParameters().getFirst("seqNo");
            Long fromID = Transaction.parseDBRef(fromSeqNoStr);
            if (fromID != null && fromID.equals(0L) && intOffest < 0) {
                // это значит нужно скакнуть в самый низ
            }

            if (true) {
                Tuple3<Long, Long, List<Transaction>> result = Transaction.searchTransactions(dcSet, filterStr, useForge, pageSize, fromID, intOffest, true);
                transactions = result.c;
                if (result.a != null) {
                    output.put("fromSeqNo", Transaction.viewDBRef(result.a));
                }
                if (result.b != null) {
                    output.put("toSeqNo", Transaction.viewDBRef(result.b));
                }
            } else {
                // OLD
                if (filterStr == null) {
                    transactions = map.getTransactionsFromID(fromID, intOffest, pageSize, !useForge, true);
                } else {
                    transactions = map.getTransactionsByTitleFromID(filterStr, fromID,
                            intOffest, pageSize, true);
                }

                if (transactions.isEmpty()) {
                    // возможно вниз вышли за границу
                    output.put("fromSeqNo", fromSeqNoStr);
                } else {
                    // включим ссылки на листание вверх
                    output.put("fromSeqNo", transactions.get(0).viewHeightSeq());
                    // это не самый конец - включим листание вниз
                    output.put("toSeqNo", transactions.get(transactions.size() - 1).viewHeightSeq());
                }
            }
        }

        // Transactions view - тут одна страница вся - и пересчет ее внутри делаем
        transactionsJSON(output, null, transactions, 0, pageSize,
                Lang.T("Last XX transactions", langObj).replace("XX", ""));

        output.put("useoffset", true);

    }

    @SuppressWarnings({"serial", "static-access"})
    public void jsonQueryAddresses() {
        output.put("type", "addresses");
        output.put("search_placeholder", Lang.T("Insert searching address", langObj));
    }

    @SuppressWarnings({"serial", "static-access"})
    public Map jsonQueryAddress(String address, int start1, UriInfo info) {

        output.put("type", "address");
        output.put("search", "addresses");
        output.put("search_placeholder", Lang.T("Insert searching address", langObj));
        output.put("search_message", address);

        Object forge = info == null ? false : info.getQueryParameters().getFirst("forge");
        boolean useForge = forge != null && (forge.toString().toLowerCase().equals("yes")
                || forge.toString().toLowerCase().equals("1"));

        Long offset = checkAndGetLongParam(info, 0L, "offset");
        int intOffest;
        if (offset == null) {
            intOffest = 0;
        } else {
            intOffest = (int)(long) offset;
        }

        String fromSeqNoStr = info.getQueryParameters().getFirst("seqNo");
        Long fromID = Transaction.parseDBRef(fromSeqNoStr);
        if (fromID != null && fromID.equals(0L) && intOffest < 0) {
            // это значит нужно скакнуть в самый низ
        }

        List<Transaction> transactions = dcSet.getTransactionFinalMap().getTransactionsByAddressFromID(Account.makeShortBytes(address),
                fromID, intOffest, pageSize, !useForge, true);

        if (transactions.isEmpty()) {
            output.put("fromSeqNo", fromSeqNoStr); // возможно вниз вышли за границу
        } else {
            // включим ссылки на листание вверх
            if (true || intOffest >= 0 || transactions.size() >= pageSize) {
                output.put("fromSeqNo", transactions.get(0).viewHeightSeq());
            }

            if (true || !((fromID == null || fromID.equals(0L)) && intOffest < 0)) {
                // это не самый конец - включим листание вниз
                output.put("toSeqNo", transactions.get(transactions.size() - 1).viewHeightSeq());
            }
        }

        LinkedHashMap output = new LinkedHashMap();
        output.put("address", address);

        Account account = new Account(address);
        Tuple2<Integer, PersonCls> person = account.getPerson();

        // Transactions view - тут одна страница вся - и пересчет ее внутри делаем
        transactionsJSON(output, account, transactions, 0, pageSize,
                Lang.T("Last XX transactions", langObj).replace("XX", ""));

        output.put("useoffset", true);

        if (person != null) {
            output.put("label_person_name", Lang.T("Name", langObj));
            output.put("person_Img", Base64.encodeBase64String(person.b.getImage()));
            output.put("person", person.b.viewName());
            output.put("person_key", person.b.getKey());

            Tuple5<Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>, Tuple2<BigDecimal, BigDecimal>> balabce_LIA = account.getBalance(AssetCls.LIA_KEY);
            output.put("registered", balabce_LIA.a.b.toPlainString());
            output.put("certified", balabce_LIA.b.b.toPlainString());
            output.put("label_registered", Lang.T("Registered", langObj));
            output.put("label_certified", Lang.T("Certified", langObj));
        }

        output.put("label_account", Lang.T("Account", langObj));

        // balance assets from
        int side = Account.BALANCE_SIDE_LEFT;
        try {
            side = new Integer(info.getQueryParameters().getFirst("side"));
        } catch (Exception e) {
        }

        output.put("Balance", balanceJSON(new Account(address), side));

        return output;
    }

    public Map jsonQueryTrade(String query) {

        output.put("type", "trade");
        output.put("search", "assets");

        Map output = new LinkedHashMap();

        //AssetNames assetNames = new AssetNames();

        List<Object> all = new ArrayList<Object>();

        Trade trade;
        if (false) {
            String[] signatures = query.split("/");
            Transaction initiator = dcSet.getTransactionFinalMap().get(Base58.decode(signatures[0]));
            Transaction target = dcSet.getTransactionFinalMap().get(Base58.decode(signatures[1]));
            trade = dcSet.getTradeMap()
                    .get(Fun.t2(Transaction.makeDBRef(initiator.getHeightSeqNo()),
                            Transaction.makeDBRef(target.getHeightSeqNo())));
            all.add(Controller.getInstance().getTransaction(Base58.decode(signatures[0])));
            all.add(Controller.getInstance().getTransaction(Base58.decode(signatures[1])));

        } else {
            String[] refs = query.split("/");

            Long refInitiator = Transaction.parseDBRef(refs[0]);
            if (refInitiator == null) {
                output.put("error", "Initiator ID wrong");
                return output;
            }
            Long refTarget = Transaction.parseDBRef(refs[1]);
            if (refTarget == null) {
                output.put("error", "Target ID wrong");
                return output;
            }
            trade = dcSet.getTradeMap().get(Fun.t2(refInitiator, refTarget));
            if (trade == null) {
                output.put("error", "Trade not Found");
                return output;
            }

            all.add(DCSet.getInstance().getTransactionFinalMap().get(refInitiator));
            all.add(DCSet.getInstance().getTransactionFinalMap().get(refTarget));

        }
        output.put("type", "trade");
        output.put("trade", query);

        all.add(trade); //.toJson(0));

        int size = all.size();

        output.put("start", size);
        output.put("end", 1);

        int counter = 0;
        for (Object unit : all) {
            output.put(size - counter, jsonUnitPrint(unit)); //, assetNames));
            counter++;
        }

        //output.put("assetNames", assetNames.getMap());//

        return output;
    }

    // http://127.0.0.1:9067/index/blockexplorer.json?peers&lang=en&view=1&sort_reliable=1&sort_ping=1&start=4&row_view=3
    // view=1 0- view only work Peers; 1 - view all Peers
    // sort_reliable=1 0 - as sort ; 1 - des sort
    // sort_ping=1 0 - as sort ; 1 - des sort
    // start=0 start org.erachain.records 0....
    // row_view=3 view org.erachain.records 1.....

    public Map jsonQueryPeers(UriInfo info) {

        output.put("type", "peers");

        int start = 0;
        int end = 100;
        int view = 0;
        int sortPing = 0;
        PeersTableModel model_Peers = new PeersTableModel();
        // start org.erachain.records
        try {
            start = Integer.valueOf((info.getQueryParameters().getFirst("start")));
        } catch (NumberFormatException e3) {
            // TODO Auto-generated catch block

        }
        // end org.erachain.records
        try {
            end = Integer.valueOf((info.getQueryParameters().getFirst("row_view")));
        } catch (NumberFormatException e3) {
            // TODO Auto-generated catch block

        }
        // view all| only Active
        try {
            model_Peers.setView(Integer.valueOf((info.getQueryParameters().getFirst("view"))));
        } catch (NumberFormatException e2) {
            // TODO Auto-generated catch block
            // all peers
            model_Peers.setView(1);
        }

        // sort reliable
        try {
            model_Peers.setSortReliable(Integer.valueOf(info.getQueryParameters().getFirst("sort_reliable")));
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }
        // sort PING
        try {
            model_Peers.setSortPing(Integer.valueOf(info.getQueryParameters().getFirst("sort_ping")));
        } catch (NumberFormatException e1) {
            // TODO Auto-generated catch block
            // e1.printStackTrace();
        }
        // repaint model
        model_Peers.fireTableDataChanged();

        Map output = new LinkedHashMap();

        int column_Count = model_Peers.getColumnCount();

        for (int column = 0; column < column_Count; column++) {

            output.put("Label_" + model_Peers.getColumnNameOrigin(column).replace(' ', '_'),
                    Lang.T(model_Peers.getColumnNameOrigin(column), langObj));
        }

        Map out_peers = new LinkedHashMap();
        // if (rowCount> model_Peers.getRowCount()) rowCount =
        // model_Peers.getRowCount();
        int rowCount = start + end;
        int rowCount1 = model_Peers.getRowCount();
        if (rowCount >= rowCount1) {
            rowCount = rowCount1;
            output.put("end_page", "end");
        }
        for (int row = start; row < rowCount; row++) {
            Map out_peer = new LinkedHashMap();

            for (int column = 0; column < column_Count; column++) {
                out_peer.put(model_Peers.getColumnNameOrigin(column).replace(' ', '_'),
                        model_Peers.getValueAt(row, column).toString());

            }
            out_peers.put(row, out_peer);
        }

        // calc many pages
        output.put("pages", M_Integer.roundUp((float) rowCount1 / end));
        output.put("Label_No", Lang.T("No.", langObj));
        output.put("Peers", out_peers);
        return output;
    }

    public Map jsonQueryItemTemplate(Long key) {

        output.put("type", "template");
        output.put("search", "templates");

        TemplateCls template = (TemplateCls) dcSet.getItemTemplateMap().get(key);
        if (template == null) {
            return new HashMap(2);
        }

        output.put("charKey", template.getItemTypeChar());
        output.put("label_Actions", Lang.T("Actions", langObj));
        output.put("label_RAW", Lang.T("Bytecode", langObj));

        Map output = new LinkedHashMap();

        Map templateJSON = itemBase(template).a;
        output.put("template", templateJSON);

        output.put("label_Template", Lang.T("Template", langObj));
        output.put("label_Key", Lang.T("Key", langObj));
        output.put("label_Creator", Lang.T("Creator", langObj));
        output.put("label_Description", Lang.T("Description", langObj));

        return output;
    }

    public Map jsonQueryItemStatus(Long key) {

        output.put("type", "status");
        output.put("search", "statuses");

        StatusCls status = (StatusCls) dcSet.getItemStatusMap().get(key);
        if (status == null) {
            return new HashMap(2);
        }

        output.put("charKey", status.getItemTypeChar());
        output.put("label_Actions", Lang.T("Actions", langObj));
        output.put("label_RAW", Lang.T("Bytecode", langObj));

        Map output = new LinkedHashMap();

        Map statusJSON = itemBase(status).a;

        statusJSON.put("unique", status.isUnique());

        output.put("status", statusJSON);

        output.put("label_Status", Lang.T("Status", langObj));
        output.put("label_Key", Lang.T("Key", langObj));
        output.put("label_Creator", Lang.T("Creator", langObj));
        output.put("label_Description", Lang.T("Description", langObj));

        output.put("label_unique_state", Lang.T("Unique State", langObj));
        output.put("label_multi_states", Lang.T("Multi States", langObj));

        return output;
    }

    /**
     * не использыется как отдельный запрос - поэтому в ней нельзя output.put("search", "statements"); и ТИП задавать
     *
     * @param rNote
     * @return
     */
    private Map jsonStatement(RSignNote rNote) {

        ///rNote.parseData();

        HashMap output = new LinkedHashMap();

        output.put("Label_type", Lang.T("Type", langObj));
        output.put("Label_statement", Lang.T("Statement", langObj));
        output.put("Label_creator", Lang.T("Creator", langObj));
        output.put("Label_date", Lang.T("Date", langObj));
        output.put("Label_block", Lang.T("Block", langObj));
        output.put("Label_seqNo", Lang.T("seqNo", langObj));
        output.put("Label_fee", Lang.T("Fee", langObj));
        output.put("Label_size", Lang.T("Size", langObj));
        output.put("Label_No", Lang.T("No.", langObj));
        output.put("Label_pubKey", Lang.T("Public Key", langObj));
        output.put("Label_signature", Lang.T("Signature", langObj));
        output.put("Label_Link", Lang.T("Link", langObj));

        int block = rNote.getBlockHeight();
        int seqNo = rNote.getSeqNo();

        // TODO нужно для получения Файлов и их Хэшей - надо потом сделать отдельную Мап для файлов чтобы их всех нераспаковывать каждый раз и ХШИ в трнзакцици держать
        rNote.parseDataFull();

        output.put("tx", rNote.toJson());

        output.put("creator_name", rNote.getCreator().getPersonAsString());

        rNote.getExData().makeJSONforHTML(output, block, seqNo, langObj);

        WebTransactionsHTML.getApps(output, rNote, langObj);

        return output;
    }

    public Map jsonQueryTX(String query) {

        output.put("type", "tx");
        output.put("search", "transactions");
        output.put("search_placeholder", Lang.T("Type searching words or signature or BlockNo-SeqNo", langObj));

        Map output = new LinkedHashMap();

        String[] signatures = query.split(",");

        for (int i = 0; i < signatures.length; i++) {

            Transaction transaction = null; // new
            String signature = signatures[i];
            if (Base58.isExtraSymbols(signature)) {
                transaction = dcSet.getTransactionFinalMap().getRecord(signature);
            } else {
                transaction = Controller.getInstance().getTransaction(Base58.decode(signature));
            }

            if (transaction == null)
                continue;

            if (transaction.getType() == Transaction.SIGN_NOTE_TRANSACTION) {//.ISSUE_STATEMENT_TRANSACTION){
                output.putAll(jsonStatement((RSignNote) transaction));
                output.put("type", "statement");

            } else {
                output.put("type", "tx");
                output.put("body", WebTransactionsHTML.getInstance().get_HTML(transaction, langObj));
                output.put("Label_Transaction", Lang.T("Transaction", langObj));
                output.put("heightSeqNo", transaction.viewHeightSeq());
            }
        }

        return output;
    }

    public Map jsonQueryBlock(String query, int start) throws WrongSearchException {

        output.put("type", "block");
        output.put("search", "blocks");

        LinkedHashMap output = new LinkedHashMap();
        List<Object> all = new ArrayList<Object>();
        int[] txsTypeCount = new int[256];
        int aTTxsCount = 0;
        Block block = null;

        //AssetNames assetNames = new AssetNames();

        if (query.matches("\\d+")) {
            int parseInt;
            try {
                parseInt = Integer.parseInt(query);
            } catch (NumberFormatException e) {
                logger.info("Wrong search while process blocks... ");
                throw new WrongSearchException();
            }
            block = Controller.getInstance().getBlockByHeight(dcSet, parseInt);
            if (block == null) {
                block = Controller.getInstance().getBlockByHeight(dcSet, 1);
            }
        } else if (query.equals("last")) {
            block = Controller.getInstance().getLastBlock();
        } else {
            try {
                block = Controller.getInstance().getBlock(Base58.decode(query));
            } catch (Exception e) {
                logger.info("Wrong search while process blocks... ");
                throw new WrongSearchException();
            }
            if (block == null) {
                logger.info("Wrong search while process blocks... ");
                throw new WrongSearchException();
            }
        }

        int seqNo = 0;
        for (Transaction transaction : block.getTransactions()) {
            transaction.setDC(dcSet, block.heightBlock, block.heightBlock, ++seqNo, true);
            all.add(transaction);
            txsTypeCount[transaction.getType() - 1]++;
        }

        // Transactions view
        transactionsJSON(output, null, block.getTransactions(), start, pageSize,
                Lang.T("Transactions found", langObj));

        LinkedHashMap<Tuple2<Integer, Integer>, ATTransaction> atTxs = dcSet.getATTransactionMap()
                .getATTransactions(block.getHeight());

        for (Entry<Tuple2<Integer, Integer>, ATTransaction> e : atTxs.entrySet()) {
            all.add(e.getValue());
            aTTxsCount++;
        }

        output.put("blockSignature", Base58.encode(block.getSignature()));
        output.put("blockHeight", block.getHeight());
        output.put("blockCreator", block.getCreator().getAddress());
        output.put("blockCreatorPerson", block.getCreator().getPersonAsString());

        if (block.getHeight() > 1) {
            if (block.getParent(dcSet) != null) {
                output.put("parentBlockSignature", Base58.encode(block.getParent(dcSet).getSignature()));
            }
        } else {
            output.put("parentBlockSignature", "");
        }

        if (block.getChild(dcSet) != null) {
            output.put("childBlockSignature", Base58.encode(block.getChild(dcSet).getSignature()));
        }
        int size = all.size();

        Map txCountJSON = new LinkedHashMap();

        txCountJSONPut(txsTypeCount, block.getTransactionCount(), txCountJSON);

        if (aTTxsCount > 0) {
            txCountJSON.put("aTTxsCount", aTTxsCount);
        }

        txCountJSON.put("allCount", block.getTransactionCount());

        output.put("countTx", txCountJSON);

        output.put("totalFee", block.viewFeeAsBigDecimal());
        output.put("version", block.getVersion());

        output.put("generatingBalance", block.getForgingValue());
        output.put("winValue", block.getWinValue());
        output.put("target", block.getTarget());
        output.put("winValueTargeted", block.calcWinValueTargeted());

        output.put("start", size + 1);
        output.put("end", 1);


        int counter = 0;

        {
            Map transactionJSON = new LinkedHashMap();
            Map transactionDataJSON = new LinkedHashMap();

            int height = block.getHeight();
            transactionDataJSON.put("confirmations", getHeight() - height + 1);
            transactionDataJSON.put("height", height);
            transactionDataJSON.put("timestamp", block.getTimestamp(height));

            transactionDataJSON.put("generator", block.getCreator().getAddress());
            transactionDataJSON.put("signature", Base58.encode(block.getSignature()));
            transactionDataJSON.put("reference", Base58.encode(block.getReference()));
            transactionDataJSON.put("generatorSignature", Base58.encode(block.getSignature()));
            transactionDataJSON.put("version", block.getVersion());

            transactionDataJSON.put("fee", block.viewFeeAsBigDecimal());

            transactionJSON.put("type", "block");
            transactionJSON.put("block", transactionDataJSON);

            output.put(counter + 1, transactionJSON);
        }
        output.put("label_block", Lang.T("Block", langObj));
        output.put("label_Block_version", Lang.T("Block version", langObj));
        output.put("label_Forger", Lang.T("Forger", langObj));
        output.put("label_Transactions_count",
                Lang.T("Transactions count", langObj));
        output.put("label_Total_Amount", Lang.T("Total Amount", langObj));
        output.put("label_Total_AT_Amount", Lang.T("Total AT Amount", langObj));
        output.put("label_Total_Fee", Lang.T("Total Fee", langObj));

        output.put("label_Win_Value", Lang.T("Win Value", langObj));
        output.put("label_Generating_Balance",
                Lang.T("Generating Balance", langObj));
        output.put("label_Target", Lang.T("Target", langObj));
        output.put("label_Targeted_Win_Value",
                Lang.T("Targeted Win Value", langObj));

        output.put("label_Parent_block", Lang.T("Parent block", langObj));
        output.put("label_Current_block", Lang.T("Current block", langObj));
        output.put("label_Child_block", Lang.T("Child block", langObj));
        output.put("label_Including", Lang.T("Including", langObj));
        output.put("label_Signature", Lang.T("Signature", langObj));

        return output;
    }

    public Map jsonQueryUnconfirmedTXs() {

        output.put("type", "unconfirmeds");

        Map output = new LinkedHashMap();

        //AssetNames assetNames = new AssetNames();

        List<Transaction> all = new ArrayList<>(
                Controller.getInstance().getUnconfirmedTransactions(100, true));

        int size = all.size();

        output.put("start", size);

        if (size > 0) {
            output.put("end", 1);
        } else {
            output.put("end", 0);
        }

        int counter = 0;
        for (Object unit : all) {
            counter++;

            output.put(counter, jsonUnitPrint(unit)); //, assetNames));
        }

        return output;
    }

    public int getHeight() {
        return dcSet.getBlocksHeadsMap().size();
    }

    public Tuple2<Integer, Long> getHWeightFull() {
        return Controller.getInstance().getBlockChain().getHWeightFull(dcSet);
    }

    public Block.BlockHead getLastBlockHead() {
        return dcSet.getBlocksHeadsMap().last();
    }

    //Секундомер с остановом(stopwatch). При создании "секундомер пошел"
    public static class Stopwatch {
        private long start;

        /**
         * Create a stopwatch object.
         */
        public Stopwatch() {
            start = System.currentTimeMillis();
        }

        /**
         * Return elapsed time (in seconds) since this object was created.
         */
        public double elapsedTime() {
            long now = System.currentTimeMillis();
            return (now - start);
        }
    }


    public class BigDecimalComparator_B implements Comparator<Tuple3<String, BigDecimal, BigDecimal>> {

        @Override
        public int compare(Tuple3<String, BigDecimal, BigDecimal> a, Tuple3<String, BigDecimal, BigDecimal> b) {
            try {
                int result = a.b.compareTo(b.b);
                if (result != 0)
                    return result;

                // учет еще по Должен
                return a.c.compareTo(b.c);

            } catch (Exception e) {
                return 0;
            }
        }

    }

    public class BigDecimalComparator_top100 implements Comparator<Tuple5<String, BigDecimal, BigDecimal, BigDecimal, BigDecimal>> {

        @Override
        public int compare(Tuple5<String, BigDecimal, BigDecimal, BigDecimal, BigDecimal> a, Tuple5<String, BigDecimal, BigDecimal, BigDecimal, BigDecimal> b) {
            try {
                int result = a.c.compareTo(b.c);
                if (result != 0)
                    return result;

                // учет еще по Должен
                return a.b.compareTo(b.b);
            } catch (Exception e) {
                return 0;
            }
        }

    }

    public void transactionsJSON(LinkedHashMap output, Account account, List<Transaction> transactions, int fromIndex, int pageSize,
                                 String title) {
        LinkedHashMap outputTXs = new LinkedHashMap();
        int i = 0;
        boolean outcome;
        int type;

        //ItemAssetMap assetMap = DCSet.getInstance().getItemAssetMap();

        int height = Controller.getInstance().getMyHeight();

        LinkedHashMap transactionsJSON = new LinkedHashMap();
        if (transactions != null) {
            int listSize = transactions.size();
            if (listSize > 0) {
                List<Transaction> transactionList;
                if (pageSize == 0) {
                    transactionList = transactions;
                } else {
                    int max = Math.min(fromIndex + pageSize, listSize);
                    if (fromIndex < max)
                        transactionList = transactions.subList(fromIndex, max);
                    else
                        transactionList = transactions;
                }

                for (Transaction transaction : transactionList) {

                    if (false && // покажем все
                            transaction.isWiped())
                        continue;

                    transaction.setDC(dcSet, true);

                    outcome = true;

                    LinkedHashMap out = new LinkedHashMap();

                    out.put("block", transaction.getBlockHeight());// .getSeqNo(dcSet));

                    out.put("seqNo", transaction.getSeqNo());

                    out.put("title", transaction.getTitle());
                    //out.put("confirmations", transaction.getConfirmations(height));

                    if (transaction.getType() == Transaction.CALCULATED_TRANSACTION) {
                        RCalculated txCalculated = (RCalculated) transaction;
                        outcome = txCalculated.getAmount().signum() < 0;

                        //out.put("reference", "--");
                        out.put("signature", transaction.viewHeightSeq());
                        // 645124 - calculated seq-No 654868
                        try {
                            out.put("timestamp", dcSet.getBlocksHeadsMap().get(transaction.getBlockHeight()).getTimestamp());
                        } catch (Exception e) {
                            out.put("timestamp", transaction.viewHeightSeq());
                        }

                        //String message = txCalculated.getMessage();
                        String typeName = Lang.T(transaction.viewFullTypeName(), langObj);
                        out.put("type", typeName);

                        if (typeName.equals("_protocol_")) {
                            //out.put("title", message);
                        }

                        out.put("creator", txCalculated.getRecipient().getPersonAsString());
                        out.put("creator_addr", txCalculated.getRecipient().getAddress());

                        out.put("amount", txCalculated.getAmount().toPlainString());
                        out.put("asset", txCalculated.getAssetKey());

                        out.put("size", "--");
                        out.put("fee", "--");

                    } else {
                        out.put("signature", Base58.encode(transaction.getSignature()));
                        out.put("timestamp", transaction.getTimestamp());
                        String typeName = Lang.T(transaction.viewFullTypeName(), langObj);
                        out.put("type", typeName);

                        if (transaction.getCreator() == null) {
                            out.put("creator", GenesisBlock.CREATOR.getAddress());
                            out.put("creator_addr", "GENESIS");
                            if (transaction.getType() == Transaction.GENESIS_SEND_ASSET_TRANSACTION) {
                                outcome = false;
                            }

                        } else {

                            out.put("publickey", Base58.encode(transaction.getCreator().getPublicKey()));

                            Account atSideAccount;
                            atSideAccount = transaction.getCreator();
                            if (account != null) {
                                atSideAccount = transaction.getCreator();
                                type = transaction.getType();
                                if (type == Transaction.SEND_ASSET_TRANSACTION) {
                                    RSend rSend = (RSend) transaction;
                                    if (rSend.hasAmount()) {
                                        if (rSend.getCreator().equals(account)) {
                                            outcome = false;
                                            atSideAccount = rSend.getRecipient();
                                        }
                                        // возврат и взять на харенение обратный
                                        outcome = outcome ^ !rSend.isBackward() ^ (rSend.balancePosition() == TransactionAmount.ACTION_HOLD);
                                    }
                                }
                            }

                            out.put("creator", atSideAccount.getPersonAsString(15));
                            out.put("creator_addr", atSideAccount.getAddress());

                        }

                        out.put("size", transaction.viewSize(Transaction.FOR_NETWORK));
                        out.put("fee", transaction.getFee());

                    }

                    BigDecimal amount = transaction.getAmount();
                    if (amount != null && amount.signum() != 0) {
                        amount = amount.stripTrailingZeros().abs();
                        out.put("amount",
                                (outcome ? "-" : "+") + amount.toPlainString());
                    }

                    Long absKey = transaction.getAbsKey();
                    if (absKey > 0) {
                        out.put("itemKey", absKey);

                        if (transaction instanceof Itemable) {
                            Itemable itemable = (Itemable) transaction;

                            ItemCls item = itemable.getItem();
                            if (item != null) {
                                out.put("itemName", item.getShortName());
                                out.put("itemType", item.getItemTypeName());
                            }
                        }
                    }

                /*
                if (transaction.viewRecipient() == null) {
                    out.put("recipient", "-");
                } else {
                    out.put("recipient", transaction.getRecipient(),getPersonAsString(15)
                }
                */

                    transactionsJSON.put(i, out);
                    i++;
                }
            }
            output.put("listSize", listSize);
        }

        outputTXs.put("transactions", transactionsJSON);

        outputTXs.put("label_useForge", Lang.T("Forging", langObj));

        outputTXs.put("label_seqNo", Lang.T("Number", langObj));
        outputTXs.put("label_block", Lang.T("Block", langObj));
        outputTXs.put("label_date", Lang.T("Date", langObj));
        outputTXs.put("label_type_transaction", Lang.T("Type", langObj));
        outputTXs.put("label_creator", Lang.T("Creator", langObj));
        outputTXs.put("label_atside", Lang.T("Side", langObj));
        outputTXs.put("label_asset", Lang.T("Asset", langObj));
        outputTXs.put("label_amount", Lang.T("Amount", langObj));
        //outputTXs.put("label_confirmations", Lang.TFromLangObj("Confirmations", langObj));
        outputTXs.put("label_recipient", Lang.T("Recipient", langObj));
        outputTXs.put("label_size", Lang.T("Size", langObj));
        outputTXs.put("label_seqNo", Lang.T("SeqNo", langObj));
        outputTXs.put("label_signature", Lang.T("Signature", langObj));
        outputTXs.put("label_title", Lang.T("Title", langObj));
        outputTXs.put("label_amount_key", Lang.T("Amount:Key", langObj));
        outputTXs.put("label_fee", Lang.T("Fee", langObj));
        outputTXs.put("label_transactions_table", title);

        output.put("Transactions", outputTXs);

        output.put("pageSize", pageSize);
        output.put("start", fromIndex);

        return;

    }

}
