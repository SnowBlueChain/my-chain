package api;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.mapdb.Fun.Tuple2;

import controller.Controller;
import core.BlockChain;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.crypto.Base58;
import core.transaction.R_Send;
import core.transaction.Transaction;
import network.message.TelegramMessage;
import utils.APIUtils;

@Path("telegrams")
@Produces(MediaType.APPLICATION_JSON)
public class TelegramsResource {

	private static final Logger LOGGER = Logger.getLogger(TelegramsResource.class);

	@Context
	HttpServletRequest request;

	@GET
	public String getTelegrams() {
		return this.getTelegramsLimited(50,"");
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
		for (TelegramMessage telegram : Controller.getInstance().getLastTelegrams(account.b, timestamp, filter)) {
			array.add(telegram.toJson());
		}

		return array.toJSONString();
	}

	@GET
	@Path("get/{signature}")
	// GET telegrams/get/6kdJgbiTxtqFt2zQDz9Lb29Z11Fa1TSwfZvjU21j6Cn9umSUEK4jXmNU19Ww4RcXpFyQiJTCaSz6Lc5YKn26hsR
	public String getTelegramBySignature(@PathParam("signature") String signature) throws Exception {

		///String password = null;
		///APIUtils.askAPICallAllowed(password, "GET telegrams/get/" + signature, request);

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


	// GET telegrams/send/7NH4wjxVy1y8kqBPtArA4UsevPMdgJS2Dk/7C5HJALxTbAhzyhwVZeDCsGqVnSwcdEtqu/2/0.0001/title/message/true/false?password=1
	@SuppressWarnings("unchecked")
	@GET
	@Path("send/{sender}/{recipient}/{asset}/{amount}/{title}/{message}/{istextmessage}/{encrypt}")
	public String send(@PathParam("sender") String sender1, @PathParam("recipient") String recipient1,
			@PathParam("asset") long asset_in, @PathParam("amount") String amount_in,
			@PathParam("title") String title_in, @PathParam("message") String message_in,
			@PathParam("istextmessage") boolean istextmessage, @PathParam("encrypt") boolean encrypt,
			@QueryParam("password") String password) {

		//APIUtils.askAPICallAllowed(password, "POST telegrams/send", request);

		JSONObject out = new JSONObject();
		Controller cntr = Controller.getInstance();

		// READ SENDER
		Account sender;
		try {
			sender = new Account(sender1);
			if (sender.getAddress() == null)
				throw new Exception("");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			out.put("status_code", Transaction.INVALID_CREATOR);
			out.put("status", "Invalid Senser");
			return out.toJSONString();
		}

		// READ RECIPIENT
		Account recip;
		try {
			recip = new Account(recipient1);
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

		byte[] encrypted = encrypt?new byte[] { 1 } : new byte[] { 0 };
		byte[] isTextByte = istextmessage?new byte[] { 1 } : new byte[] { 0 };

		// title
		if (title_in != null  && title_in.getBytes(StandardCharsets.UTF_8).length > 256) {
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

	// "POST telegrams/send {\"sender\": \"<sender>\", \"recipient\": \"<recipient>\", \"asset\": <assetKey>, \"amount\": \"<amount>\", \"title\": \"<title>\", \"message\": \"<message>\", \"istextmessage\": <true/false>, \"encrypt\": <true/false>, \"password\": \"<password>\"}",
	// POST telegrams/send {"sender": "78JFPWVVAVP3WW7S8HPgSkt24QF2vsGiS5", "recipient": "7C5HJALxTbAhzyhwVZeDCsGqVnSwcdEtqu", "asset": 2, "amount": "0.0001", "title": "title", "message": "<message>", "istextmessage": true, "encrypt": false, "password": "122"}
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

		return send((String)jsonObject.get("sender"), (String)jsonObject.get("recipient"),
				(long)jsonObject.get("asset"), (String)jsonObject.get("amount"),
				(String)jsonObject.get("title"), (String)jsonObject.get("message"),
				(boolean)jsonObject.get("istextmessage"), (boolean)jsonObject.get("encrypt"),
				(String)jsonObject.get("password"));
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
}
