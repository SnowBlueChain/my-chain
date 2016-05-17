package utils;

import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;

import javax.servlet.http.HttpServletRequest;
import javax.swing.JOptionPane;
import javax.ws.rs.WebApplicationException;

import org.apache.log4j.Logger;

import api.ApiClient;
import api.ApiErrorFactory;
import controller.Controller;
import core.account.Account;
import core.account.PrivateKeyAccount;
import core.crypto.Crypto;
import core.item.assets.AssetCls;
import core.transaction.Transaction;
import core.web.ServletUtils;
import gui.PasswordPane;
import settings.Settings;
import test.TestRecNote;

public class APIUtils {

	static Logger LOGGER = Logger.getLogger(APIUtils.class.getName());

	public static String processPayment(String sender, String feePowStr,
			String recipient, String assetKeyString, String amount, String x,
			HttpServletRequest request) {
		
		// PARSE AMOUNT		
		AssetCls asset;
		
		if(assetKeyString == null)
		{
			asset = Controller.getInstance().getAsset(AssetCls.FEE_KEY);
		}
		else
		{
			try {
				asset = Controller.getInstance().getAsset(new Long(assetKeyString));
			} catch (Exception e) {
				throw ApiErrorFactory.getInstance().createError(
						ApiErrorFactory.ERROR_INVALID_ASSET_ID);
			}
		}
		
		// PARSE AMOUNT
		BigDecimal bdAmount;
		try {
			bdAmount = new BigDecimal(amount);
			bdAmount = bdAmount.setScale(8);
		} catch (Exception e) {
			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_INVALID_AMOUNT);
		}

		// PARSE FEE POWER
		int feePow;
		try {
			feePow = Integer.parseInt(feePowStr);
		} catch (Exception e) {
			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_INVALID_FEE);
		}

		// CHECK ADDRESS
		if (!Crypto.getInstance().isValidAddress(sender)) {
			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_INVALID_SENDER);
		}

		APIUtils.askAPICallAllowed("POST payment\n" + x, request);

		// CHECK IF WALLET EXISTS
		if (!Controller.getInstance().doesWalletExists()) {
			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_WALLET_NO_EXISTS);
		}

		// CHECK WALLET UNLOCKED
		if (!Controller.getInstance().isWalletUnlocked()) {
			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_WALLET_LOCKED);
		}

		// GET ACCOUNT
		PrivateKeyAccount account = Controller.getInstance()
				.getPrivateKeyAccountByAddress(sender);
		if (account == null) {
			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_INVALID_SENDER);
		}

		// TODO R_Send insert!
		Pair<Transaction, Integer> result;
		// SEND ASSET PAYMENT
		result = Controller.getInstance()
			.r_Send(account, feePow, new Account(recipient), asset.getKey(), bdAmount);
			
		switch (result.getB()) {
		case Transaction.VALIDATE_OK:

			return result.getA().toJson().toJSONString();

		case Transaction.INVALID_NAME_LENGTH:

			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_INVALID_NAME_LENGTH);

		case Transaction.INVALID_VALUE_LENGTH:

			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_INVALID_VALUE_LENGTH);

		case Transaction.INVALID_ADDRESS:

			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_INVALID_RECIPIENT);

		case Transaction.NAME_ALREADY_REGISTRED:

			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_NAME_ALREADY_EXISTS);

		case Transaction.NEGATIVE_AMOUNT:
			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_INVALID_AMOUNT);
		
		
			
		case Transaction.NOT_ENOUGH_FEE:

			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_NO_BALANCE);

		case Transaction.NO_BALANCE:

			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_NO_BALANCE);

		default:

			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_UNKNOWN);
		}
	}

	public static void disallowRemote(HttpServletRequest request) throws WebApplicationException {
		if (ServletUtils.isRemoteRequest(request)) {
			throw ApiErrorFactory
				      .getInstance()
				      .createError(
					      ApiErrorFactory.ERROR_WALLET_API_CALL_FORBIDDEN_BY_USER);
		}
	}

	public static void askAPICallAllowed(final String messageToDisplay,
			HttpServletRequest request) throws WebApplicationException {
		// CHECK API CALL ALLOWED
		try {
			disallowRemote(request);

			int answer = Controller.getInstance().checkAPICallAllowed(messageToDisplay,	request); 
			
			if(answer == ApiClient.SELF_CALL) {
				return;
			}
			
			if (answer != JOptionPane.YES_OPTION) {
				throw ApiErrorFactory
						.getInstance()
						.createError(
								ApiErrorFactory.ERROR_WALLET_API_CALL_FORBIDDEN_BY_USER);
			}
			
			if(!GraphicsEnvironment.isHeadless() && (Settings.getInstance().isGuiEnabled()))
			{	
				if(!Controller.getInstance().isWalletUnlocked()) {
					String password = PasswordPane.showUnlockWalletDialog(); 
					if(!password.equals("") && !Controller.getInstance().unlockWallet(password))
					{
						JOptionPane.showMessageDialog(null, "Invalid password", "Unlock Wallet", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		} catch (Exception e) {
			if (e instanceof WebApplicationException) {
				throw (WebApplicationException) e;
			}
			LOGGER.error(e.getMessage(),e);
			throw ApiErrorFactory.getInstance().createError(
					ApiErrorFactory.ERROR_UNKNOWN);
		}

	}

}
