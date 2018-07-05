package api;

import controller.Controller;
import core.BlockChain;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.crypto.Base58;
import core.transaction.R_Send;
import core.transaction.Transaction;
import network.message.TelegramMessage;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.mapdb.Fun.Tuple2;
import utils.APIUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Path("telegrams")
@Produces(MediaType.APPLICATION_JSON)
public class TelegramsResource {

    private static final Logger LOGGER = Logger.getLogger(TelegramsResource.class);

    @Context
    HttpServletRequest request;

    @GET
    public String getTelegrams() {
        return this.getTelegramsLimited(50, "");
    }

    @GET
    @Path("address/{address}")
    public String getTelegramsTwo(@PathParam("address") String address,
                                  @QueryParam("filter") String filter) {
        return this.getTelegramsTimestamp(address, 0, filter);
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("timestamp/{timestamp}")
    public String getTelegramsLimited(@PathParam("timestamp") long timestamp,
                                      @QueryParam("filter") String filter) {

        // CREATE JSON OBJECT
        JSONArray array = new JSONArray();

        for (TelegramMessage telegram : Controller.getInstance().getLastTelegrams(timestamp, filter)) {
            array.add(telegram.toJson());
        }

        return array.toJSONString();
    }

    /**
     * @param address   its e recipient
     * @param timestamp the value more than which will be searched
     * @param filter    is title in telegram
     * @return Array all telegram by recipient in format JSON
     *
     * <h2>Example request</h2>
     * GET telegrams/address/" + recipient + "/timestamp/1
     * <h2>Example response</h2>
     * [{
     * "transaction":{
     * "type_name":"SEND",
     * "creator":"79WA9ypHx1iyDJn45VUXE5gebHTVrZi2iy",
     * "amount":"0.01",
     * "signature":"2SgAAsV83V7HxGN8rwhBgjVrfUZmvHc6qYNK5uodRQ5sz7mX8kinct9q3hphA43gSDXoXYcYbPS1amdNAbBKCmHg",
     * "fee":"0.00023168",
     * "publickey":"krksTcZunJmmnXQtUoNVQhwWAXFfQ4LbCJw3Qg8THo8",
     * "type":31,
     * "confirmations":0,
     * "version":0,
     * "record_type":"SEND",
     * "property2":128,
     * "action_key":1,
     * "head":"NPL",
     * "property1":0,
     * "size":162,
     * "action_name":"PROPERTY",
     * "recipient":"7Dpv5Gi8HjCBgtDN1P1niuPJQCBQ5H8Zob",
     * "backward":false,
     * "asset":643,
     * "sub_type_name":"PROPERTY",
     * "timestamp":1529583735448
     * }}]
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("address/{address}/timestamp/{timestamp}")
    public String getTelegramsTimestamp(@PathParam("address") String address, @PathParam("timestamp") long timestamp,
                                        @QueryParam("filter") String filter) {

        Tuple2<Account, String> account = Account.tryMakeAccount(address);
        if (account.a == null) {
            throw ApiErrorFactory.getInstance().createError(Transaction.INVALID_ADDRESS);
        }
        JSONArray array = new JSONArray();
        for (TelegramMessage telegram : Controller.getInstance().getLastTelegrams(account.a, timestamp, filter)) {
            array.add(telegram.toJson());
        }

        return array.toJSONString();
    }

    @GET
    @Path("get/{signature}")
    // GET telegrams/get/6kdJgbiTxtqFt2zQDz9Lb29Z11Fa1TSwfZvjU21j6Cn9umSUEK4jXmNU19Ww4RcXpFyQiJTCaSz6Lc5YKn26hsR
    public String getTelegramBySignature(@PathParam("signature") String signature) throws Exception {

        ///String password = null;
        //APIUtils.askAPICallAllowed(password, "GET telegrams/get/" + signature, request);

        // DECODE SIGNATURE
        byte[] signatureBytes;
        try {
            signatureBytes = Base58.decode(signature);
        } catch (Exception e) {
            throw ApiErrorFactory.getInstance().createError(Transaction.INVALID_SIGNATURE);
        }

        // GET TELEGRAM
        TelegramMessage telegram = Controller.getInstance().getTelegram(signature);

        // CHECK IF TELEGRAM EXISTS
        if (telegram == null) {
            throw ApiErrorFactory.getInstance().createError(Transaction.TELEGRAM_DOES_NOT_EXIST);
        }

        return telegram.toJson().toJSONString();
    }

    /**
     * Send telegram. not in block chain
     *
     * @param sender_in     address in wallet
     * @param recipient_in  recipient
     * @param asset_in      asset
     * @param amount_in     amount
     * @param title_in      title or head
     * @param message_in    message
     * @param istextmessage bool value isText
     * @param encrypt       bool value isEncrypt
     * @param password      password
     * @return return signature telegram
     *
     * <h2>Example request</h2>
     * GET telegrams/send/79WA9ypHx1iyDJn45VUXE5gebHTVrZi2iy/7C5HJALxTbAhzyhwVZeDCsGqVnSwcdEtqu/2/0.0001/title/message/true/false/1
     * <h2>Example response</h2>
     * {
     * "signature":"2vcTBHyCSUD7Qh968S1hr9mQpRgSswpCinCMeMX26XaUq58MDSCah3q9ntavhezqUGe7doR4hz4ZuPbc1QS2XzNg"
     * }
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("send/{sender}/{recipient}/{assetKey}/{amount}/{title}/{message}/{istextmessage}/{encrypt}/{password}")
    public String send(@PathParam("sender") String sender_in, @PathParam("recipient") String recipient_in,
                       @PathParam("assetKey") long asset_in, @PathParam("amount") String amount_in,
                       @PathParam("title") String title_in, @PathParam("message") String message_in,
                       @PathParam("istextmessage") boolean istextmessage, @PathParam("encrypt") boolean encrypt,
                       @PathParam("password") String password) {

        //APIUtils.askAPICallAllowed(password, "GET telegrams/send", request);

        JSONObject out = new JSONObject();
        Controller cntr = Controller.getInstance();

        // READ SENDER
        Account sender;
        try {
            sender = new Account(sender_in);
            if (sender.getAddress() == null)
                throw new Exception("");
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            out.put("status_code", Transaction.INVALID_CREATOR);
            out.put("status", "Invalid Sender");
            return out.toJSONString();
        }

        // READ RECIPIENT
        Account recip;
        try {
            recip = new Account(recipient_in);
            if (recip.getAddress() == null)
                throw new Exception("");
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            // e1.printStackTrace();
            out.put("status_code", Transaction.INVALID_ADDRESS);
            out.put("status", "Invalid Recipient Address");
            return out.toJSONString();
        }
        BigDecimal amount;
        // READ AMOUNT
        try {
            // USE max DEEP SCALE!
            amount = new BigDecimal(amount_in);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            out.put("status_code", Transaction.INVALID_AMOUNT);
            out.put("status", "Invalid Amount");
            return out.toJSONString();
        }
        byte[] message = null;
        if (message_in != null) {
            message = message_in.getBytes(StandardCharsets.UTF_8);
            if (message.length > BlockChain.MAX_REC_DATA_BYTES) {
                out.put("status_code", Transaction.INVALID_DESCRIPTION_LENGTH);
                out.put("status", "Invalid message");
                return out.toJSONString();
            }
        }

        byte[] encrypted = encrypt ? new byte[]{1} : new byte[]{0};
        byte[] isTextByte = istextmessage ? new byte[]{1} : new byte[]{0};

        // title
        if (title_in != null && title_in.getBytes(StandardCharsets.UTF_8).length > 256) {
            out.put("status_code", Transaction.INVALID_HEAD_LENGTH);
            out.put("status", "Invalid Title");
            return out.toJSONString();
        }

        // CREATE TX MESSAGE
        Transaction transaction;
        PrivateKeyAccount account = cntr.getPrivateKeyAccountByAddress(sender.getAddress());
        if (account == null) {
            throw ApiErrorFactory.getInstance().createError(ApiErrorFactory.ERROR_WALLET_ADDRESS_NO_EXISTS);
        }

        try {
            transaction = cntr.r_Send(
                    account, 0, recip, asset_in, amount,
                    title_in, message, isTextByte, encrypted);
            if (transaction == null)
                throw new Exception("transaction == null");
        } catch (Exception e) {
            out.put("status_code", Transaction.INVALID_TRANSACTION_TYPE);
            out.put("status", "Invalid Transaction");
            return out.toJSONString();
        }

        cntr.broadcastTelegram(transaction, true);

        out.put("signature", Base58.encode(transaction.getSignature()));
        return out.toJSONString();
    }

    // GET telegrams/send/7NH4wjxVy1y8kqBPtArA4UsevPMdgJS2Dk/7C5HJALxTbAhzyhwVZeDCsGqVnSwcdEtqu/2/0.0001/title/message/true/false?password=1
    @SuppressWarnings("unchecked")
    @GET
    @Path("send/{sender}/{recipient}")
    public String sendQuery(@PathParam("sender") String sender, @PathParam("recipient") String recipient,
                            @QueryParam("asset") long asset, @QueryParam("amount") String amount,
                            @QueryParam("title") String title, @QueryParam("message") String message,
                            @QueryParam("istextmessage") boolean istextmessage, @QueryParam("encrypt") boolean encrypt,
                            @QueryParam("password") String password) {

        return send(sender, recipient, asset, amount, title, message, istextmessage, encrypt, password);

    }

    // "POST telegrams/send {\"sender\": \"<sender>\", \"recipient\": \"<recipient>\", \"asset\": <assetKey>, \"amount\": \"<amount>\", \"title\": \"<title>\", \"message\": \"<message>\", \"istextmessage\": <true/false>, \"encrypt\": <true/false>, \"password\": \"<password>\"}",
    // POST telegrams/send {"sender": "78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5", "recipient": "7C5HJALxTbAhzyhwVZeDCsGqVnSwcdEtqu", "asset": 2, "amount": "0.0001", "title": "title", "message": "<message>", "istextmessage": true, "encrypt": false, "password": "122"}

    /**
     * Send telegram. not in block chain
     *
     * @param x JSON data row
     * @return signature telegram
     * <h2>Example request</h2>
     * POST telegrams/send {"sender":"79WA9ypHx1iyDJn45VUXE5gebHTVrZi2iy","recipient":"7Dpv5Gi8HjCBgtDN1P1niuPJQCBQ5H8Zob",
     * "asset":"643","amount":"0.01","title":"NPL","istextmessage":"true","encrypt":"true","password":"123456789"}
     * <h2>Example response</h2>
     * {
     * "signature":"FC3vHuUoPhYArc8L4DbgshH4mu54EaFZdGJ8Mh48FozDb5oSZazNVucyiyTYpFAHZNALUVYn5DCATMMNvtJTPhf"
     * }
     */
    @SuppressWarnings("unchecked")
    @POST
    @Path("send")
    public String sendPost(String x) {

        JSONObject jsonObject;
        try {
            // READ JSON
            jsonObject = (JSONObject) JSONValue.parse(x);
        } catch (NullPointerException | ClassCastException e) {
            // JSON EXCEPTION
            LOGGER.info(e);
            throw ApiErrorFactory.getInstance().createError(ApiErrorFactory.ERROR_JSON);
        }
		/*
		public String sendPost(@QueryParam("sender") String sender1, @QueryParam("recipient") String recipient1,
			@QueryParam("amount") String amount1, @QueryParam("message") String message1,
			@QueryParam("title") String title1, @QueryParam("asset") int asset1, @QueryParam("password") String pass,
			) {
		 */

        return send((String) jsonObject.getOrDefault("sender", null),
                (String) jsonObject.getOrDefault("recipient", null),
                Integer.valueOf((String) jsonObject.getOrDefault("asset", 0)),
                (String) jsonObject.getOrDefault("amount", null),
                (String) jsonObject.getOrDefault("title", null),
                (String) jsonObject.getOrDefault("message", null),
                Boolean.valueOf((String) jsonObject.getOrDefault("istextmessage", false)),
                Boolean.valueOf((String) jsonObject.getOrDefault("encrypt", false)),
                (String) jsonObject.getOrDefault("password", null));
    }

    // GET telegrams/datadecrypt/GerrwwEJ9Ja8gZnzLrx8zdU53b7jhQjeUfVKoUAp1StCDSFP9wuyyqYSkoUhXNa8ysoTdUuFHvwiCbwarKhhBg5?password=1
    @GET
    //@Produces("text/plain")
    @Path("datadecrypt/{signature}")
    public String dataDecrypt(@PathParam("signature") String signature, @QueryParam("password") String password) {

        byte[] signatureBytes;
        try {
            signatureBytes = Base58.decode(signature);
        } catch (Exception e) {
            throw ApiErrorFactory.getInstance().createError(Transaction.INVALID_SIGNATURE);
        }

        // GET TELEGRAM
        TelegramMessage telegram = Controller.getInstance().getTelegram(signature);

        // CHECK IF TELEGRAM EXISTS
        if (telegram == null) {
            throw ApiErrorFactory.getInstance().createError(Transaction.TRANSACTION_DOES_NOT_EXIST);
        }

        R_Send r_Send = (R_Send) telegram.getTransaction();
        byte[] r_data = r_Send.getData();
        if (r_data == null || r_data.length == 0)
            return null;

        APIUtils.askAPICallAllowed(password, "POST decrypt telegram data\n " + signature, request);

        byte[] ddd = Controller.getInstance().decrypt(r_Send.getCreator(), r_Send.getRecipient(), r_data);
        if (ddd == null) {
            return "wrong decryption";
        }

        if (r_Send.isText()) {
            try {
                String str = (new String(ddd, "UTF-8"));
                return str;
            } catch (UnsupportedEncodingException e) {
                return "error UTF-8";
            }
        } else {
            String str = Base58.encode(ddd);
            return str;
        }
    }

    /**
     * Remove telegram by signature. signature not delete if signature not contains in node.
     * <h2>Example request</h2>
     * POST telegrams/delete {"list": ["5HUqfaaY2uFgdmDM7XNky31rkdcUCPTzhHXeanBviSvyDfhgYnH4a64Aje3L53Jxmyb3CcouRiBeUF4HZNc7yySy"]}
     *
     * <h2>Example response</h2>
     * if all telegram delete return empty JSON string "[]"
     * if some telegram not delete return signature not remove telegram
     * ["5HUqfaaY2uFgdmDM7XNky31rkdcUCPTzhHXeanBviSvyDfhgYnH4a64Aje3L53Jxmyb3CcouRiBeUF4HZNc7yySy",...]
     *
     * @param value JSON string not delete telegram
     * @return
     */
    @POST
    @Path("delete")
    public String deleteTelegram(String value) {

        JSONObject jsonObject;
        try {
            JSONParser jsonParser = new JSONParser();
            jsonObject = (JSONObject) jsonParser.parse(value);
        } catch (Exception e) {
            throw ApiErrorFactory.getInstance().createError(ApiErrorFactory.ERROR_JSON);
        }
        JSONArray arraySign = (JSONArray) (jsonObject.get("list"));
        JSONObject out = new JSONObject();
        List<TelegramMessage> lst = new ArrayList<>();
        for (Object obj : arraySign) {
            if (Controller.getInstance().getTelegram(obj.toString()) == null)
                out.put("signature", obj.toString());
            else
                lst.add(Controller.getInstance().getTelegram(obj.toString()));
        }
        try {
            Controller.getInstance().deleteTelegram(lst);
            return out.toJSONString();
        } catch (Exception e) {
            throw ApiErrorFactory.getInstance().createError(e.getMessage());

        }
    }
}
