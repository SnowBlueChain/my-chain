package qora;

import java.math.BigDecimal;
//import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import controller.Controller;
import database.DBSet;
import ntp.NTP;
import qora.account.Account;
import qora.account.PrivateKeyAccount;
//import qora.account.PublicKeyAccount;
import qora.item.assets.AssetCls;
import qora.item.assets.AssetVenture;
import qora.item.assets.Order;
import qora.item.notes.NoteCls;
import qora.item.notes.Note;
import qora.block.Block;
import qora.naming.Name;
import qora.naming.NameSale;
import qora.payment.Payment;
import qora.transaction.ArbitraryTransactionV3;
import qora.transaction.BuyNameTransaction;
import qora.transaction.CancelOrderTransaction;
import qora.transaction.CancelSellNameTransaction;
import qora.transaction.CreateOrderTransaction;
import qora.transaction.CreatePollTransaction;
import qora.transaction.DeployATTransaction;
import qora.transaction.IssueAssetTransaction;
import qora.transaction.IssueNoteRecord;
import qora.transaction.MessageTransaction;
import qora.transaction.MultiPaymentTransaction;
import qora.transaction.PaymentTransaction;
import qora.transaction.RecordNote;
import qora.transaction.RegisterNameTransaction;
import qora.transaction.SellNameTransaction;
import qora.transaction.Transaction;
//import qora.transaction.Transaction.*;
import qora.transaction.TransactionFactory;
import qora.transaction.TransferAssetTransaction;
import qora.transaction.UpdateNameTransaction;
import qora.transaction.VoteOnPollTransaction;
import qora.voting.Poll;
//import settings.Settings;
import utils.Pair;
import utils.TransactionTimestampComparator;

/// icreator - 
public class TransactionCreator
{
	private DBSet fork;
	private Block lastBlock;
	
	private void checkUpdate()
	{
		//CHECK IF WE ALREADY HAVE A FORK
		if(this.lastBlock == null)
		{
			updateFork();
		}
		else
		{
			//CHECK IF WE NEED A NEW FORK
			if(!Arrays.equals(this.lastBlock.getSignature(), Controller.getInstance().getLastBlock().getSignature()))
			{
				updateFork();
			}
		}
	}
	
	private void updateFork()
	{
		//CREATE NEW FORK
		this.fork = DBSet.getInstance().fork();
		
		//UPDATE LAST BLOCK
		this.lastBlock = Controller.getInstance().getLastBlock();
			
		//SCAN UNCONFIRMED TRANSACTIONS FOR TRANSACTIONS WHERE ACCOUNT IS CREATOR OF
		List<Transaction> transactions = DBSet.getInstance().getTransactionMap().getTransactions();
		List<Transaction> accountTransactions = new ArrayList<Transaction>();
			
		for(Transaction transaction: transactions)
		{
			if(Controller.getInstance().getAccounts().contains(transaction.getCreator()))
			{
				accountTransactions.add(transaction);
			}
		}
			
		//SORT THEM BY TIMESTAMP
		Collections.sort(accountTransactions, new TransactionTimestampComparator());
			
		//VALIDATE AND PROCESS THOSE TRANSACTIONS IN FORK
		for(Transaction transaction: accountTransactions)
		{
			if(transaction.isValid(this.fork, null) == Transaction.VALIDATE_OK && transaction.isSignatureValid())
			{
				transaction.process(this.fork, false);
			}
			else
			{
				//THE TRANSACTION BECAME INVALID LET 
				DBSet.getInstance().getTransactionMap().delete(transaction);
			}
		}
	}
	
	public Pair<Transaction, Integer> createPayment(PrivateKeyAccount sender, Account recipient, BigDecimal amount, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
		
		//CREATE PAYMENT
		//PaymentTransaction payment = new PaymentTransaction(new PublicKeyAccount(sender.getPublicKey()), recipient, amount, feePow, time, sender.getLastReference(this.fork));
		PaymentTransaction payment = new PaymentTransaction(sender, recipient, amount, (byte)feePow, time, sender.getLastReference(this.fork));
		payment.sign(sender, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(payment, false);
	}
	
	public Pair<Transaction, Integer> createNameRegistration(PrivateKeyAccount creator, Name name, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
		
		//CREATE NAME REGISTRATION
		RegisterNameTransaction nameRegistration = new RegisterNameTransaction(creator, name, (byte)feePow, time, creator.getLastReference(this.fork));
		nameRegistration.sign(creator, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(nameRegistration, false);
	}

	public Pair<Transaction, Integer> createNameUpdate(PrivateKeyAccount creator, Name name, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
				
		//CREATE NAME UPDATE
		UpdateNameTransaction nameUpdate = new UpdateNameTransaction(creator, name, (byte)feePow, time, creator.getLastReference(this.fork));
		nameUpdate.sign(creator, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(nameUpdate, false);
	}
	public Pair<Transaction, Integer> createNameSale(PrivateKeyAccount creator, NameSale nameSale, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
				
		//TIME
		long time = NTP.getTime();
								
		//CREATE NAME SALE
		SellNameTransaction nameSaleTransaction = new SellNameTransaction(creator, nameSale, (byte)feePow, time, creator.getLastReference(this.fork));
		nameSaleTransaction.sign(creator, false);
				
		//VALIDATE AND PROCESS
		return this.afterCreate(nameSaleTransaction, false);
	}
	public Pair<Transaction, Integer> createCancelNameSale(PrivateKeyAccount creator, NameSale nameSale, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
				
		//TIME
		long time = NTP.getTime();
								
		//CREATE CANCEL NAME SALE
		CancelSellNameTransaction cancelNameSaleTransaction = new CancelSellNameTransaction(creator, nameSale.getKey(), (byte)feePow, time, creator.getLastReference(this.fork));
		cancelNameSaleTransaction.sign(creator, false);
				
		//VALIDATE AND PROCESS
		return this.afterCreate(cancelNameSaleTransaction, false);
	}

	public Pair<Transaction, Integer> createNamePurchase(PrivateKeyAccount creator, NameSale nameSale, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
				
		//TIME
		long time = NTP.getTime();
								
		//CREATE NAME PURCHASE
		BuyNameTransaction namePurchase = new BuyNameTransaction(creator, nameSale, nameSale.getName().getOwner(), (byte)feePow, time, creator.getLastReference(this.fork));
		namePurchase.sign(creator, false);
				
		//VALIDATE AND PROCESS
		return this.afterCreate(namePurchase, false);
	}
		
	public Pair<Transaction, Integer> createPollCreation(PrivateKeyAccount creator, Poll poll, int feePow) 
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
						
		//TIME
		long time = NTP.getTime();
					
		//CREATE POLL CREATION
		CreatePollTransaction pollCreation = new CreatePollTransaction(creator, poll, (byte)feePow, time, creator.getLastReference(this.fork));
		pollCreation.sign(creator, false);
						
		//VALIDATE AND PROCESS
		return this.afterCreate(pollCreation, false);
	}
	

	public Pair<Transaction, Integer> createPollVote(PrivateKeyAccount creator, String poll, int optionIndex, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
						
		//TIME
		long time = NTP.getTime();
						
					
		//CREATE POLL VOTE
		VoteOnPollTransaction pollVote = new VoteOnPollTransaction(creator, poll, optionIndex, (byte)feePow, time, creator.getLastReference(this.fork));
		pollVote.sign(creator, false);
						
		//VALIDATE AND PROCESS
		return this.afterCreate(pollVote, false);
	}
	
	
	public Pair<Transaction, Integer> createArbitraryTransaction(PrivateKeyAccount creator, List<Payment> payments, int service, byte[] data, int feePow) 
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
			
		//TIME
		long time = NTP.getTime();

		Transaction arbitraryTransaction;
		arbitraryTransaction = new ArbitraryTransactionV3(creator, payments, service, data, (byte)feePow, time, creator.getLastReference(this.fork));
		arbitraryTransaction.sign(creator, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(arbitraryTransaction, false);
	}
	
	
	public Pair<Transaction, Integer> createIssueAssetTransaction(PrivateKeyAccount creator, String name, String description, long quantity, byte scale, boolean divisible, int feePow) 
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//TIME
		long time = NTP.getTime();
								
		AssetCls asset = new AssetVenture(creator, name, description, quantity, scale, divisible);
							
		//CREATE ISSUE ASSET TRANSACTION
		IssueAssetTransaction issueAssetTransaction = new IssueAssetTransaction(creator, asset, (byte)feePow, time, creator.getLastReference(this.fork));
		issueAssetTransaction.sign(creator, false);
		
//		byte[] signature = issueAssetTransaction.getSignature();
//		asset.se
		//asset = new Asset(creator, name, description, quantity, scale, divisible, signature);
		//issueAssetTransaction = new IssueAssetTransaction(creator, asset, feePow, time, creator.getLastReference(this.fork));
		//issueAssetTransaction.sign(creator);
								
		//VALIDATE AND PROCESS
		return this.afterCreate(issueAssetTransaction, false);
	}
		
	public Pair<Transaction, Integer> createIssueNoteTransaction(PrivateKeyAccount creator, String name, String description, int feePow) 
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//TIME
		long time = NTP.getTime();
								
		NoteCls note = new Note(creator, name, description);
							
		//CREATE ISSUE NOTE TRANSACTION
		IssueNoteRecord issueNoteRecord = new IssueNoteRecord(creator, note, (byte)feePow, time, creator.getLastReference(this.fork));
		issueNoteRecord.sign(creator, false);
										
		//VALIDATE AND PROCESS
		return this.afterCreate(issueNoteRecord, false);
	}

	public Pair<Transaction, Integer> createOrderTransaction(PrivateKeyAccount creator, AssetCls have, AssetCls want, BigDecimal amount, BigDecimal price, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//TIME
		long time = NTP.getTime();
															
		//CREATE ORDER TRANSACTION
		CreateOrderTransaction createOrderTransaction = new CreateOrderTransaction(creator, have.getKey(), want.getKey(), amount, price, (byte)feePow, time, creator.getLastReference(this.fork));
		createOrderTransaction.sign(creator, false);
								
		//VALIDATE AND PROCESS
		return this.afterCreate(createOrderTransaction, false);
	}
		
	public Pair<Transaction, Integer> createCancelOrderTransaction(PrivateKeyAccount creator, Order order, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//TIME
		long time = NTP.getTime();
															
		//CREATE PRDER TRANSACTION
		CancelOrderTransaction cancelOrderTransaction = new CancelOrderTransaction(creator, order.getId(), (byte)feePow, time, creator.getLastReference(this.fork));
		cancelOrderTransaction.sign(creator, false);
								
		//VALIDATE AND PROCESS
		return this.afterCreate(cancelOrderTransaction, false);
	}
		
	public Pair<Transaction, Integer> createAssetTransfer(PrivateKeyAccount creator, Account recipient, AssetCls asset, BigDecimal amount, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
				
		//CREATE ASSET TRANSFER
		TransferAssetTransaction assetTransfer = new TransferAssetTransaction(creator, recipient, asset.getKey(), amount, (byte)feePow, time, creator.getLastReference(this.fork));
		assetTransfer.sign(creator, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(assetTransfer, false);
	}
		
	public Pair<Transaction, Integer> sendMultiPayment(PrivateKeyAccount creator, List<Payment> payments, int feePow)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
				
		//CREATE MULTI PAYMENTS
		MultiPaymentTransaction multiPayment = new MultiPaymentTransaction(creator, payments, (byte)feePow, time, creator.getLastReference(this.fork));
		multiPayment.sign(creator, false);
		
		//VALIDATE AND PROCESS
		return this.afterCreate(multiPayment, false);
	}
	
	public Pair<Transaction, Integer> deployATTransaction(PrivateKeyAccount creator, String name, String description, String type, String tags, byte[] creationBytes, BigDecimal amount, int feePow )
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
		
		//TIME
		long time = NTP.getTime();
				
		//DEPLOY AT
		DeployATTransaction deployAT = new DeployATTransaction(creator, name, description, type, tags, creationBytes, amount, (byte)feePow, time, creator.getLastReference(this.fork));
		deployAT.sign(creator, false);
		
		return this.afterCreate(deployAT, false);
		
	}
	
	public Pair<Transaction, Integer> createMessage(PrivateKeyAccount creator,
			Account recipient, long key, BigDecimal amount, int feePow, byte[] isText,
			byte[] message, byte[] encryptMessage) {
		
		this.checkUpdate();
		
		Transaction messageTx;

		long timestamp = NTP.getTime();
		
		//CREATE MESSAGE TRANSACTION
		messageTx = new MessageTransaction(creator, (byte)feePow, recipient, key, amount, message, isText, encryptMessage, timestamp, creator.getLastReference(this.fork));
		messageTx.sign(creator, false);
			
		return afterCreate(messageTx, false);
	}
	
	public Pair<Transaction, Integer> recordNote(boolean asPack, PrivateKeyAccount creator,
			int feePow, long key, byte[] message, byte[] isText) {
		
		this.checkUpdate();
		
		Transaction recordNoteTx;

		long timestamp = NTP.getTime();
		
		//CREATE MESSAGE TRANSACTION
		recordNoteTx = new RecordNote((byte)0,(byte)0,(byte)0, creator, (byte)feePow, key, message, isText, timestamp, creator.getLastReference(this.fork));
		recordNoteTx.sign(creator, asPack);
			
		return afterCreate(recordNoteTx, asPack);
	}

	/*
	public Pair<Transaction, Integer> createJson(PrivateKeyAccount creator,
			Account recipient, long key, BigDecimal amount, int feePow, byte[] isText,
			byte[] message, byte[] encryptMessage) {
		
		this.checkUpdate();
		
		Transaction messageTx;

		long timestamp = NTP.getTime();
		
		//CREATE MESSAGE TRANSACTION
		messageTx = new JsonTransaction(creator, recipient, key, amount, (byte)feePow, message, isText, encryptMessage, timestamp, creator.getLastReference(this.fork));
		messageTx.sign(creator);
			
		return afterCreate(messageTx);
	}

	public Pair<Transaction, Integer> createAccounting(PrivateKeyAccount sender,
			Account recipient, long key, BigDecimal amount, int feePow, byte[] isText,
			byte[] message, byte[] encryptMessage) {
		
		this.checkUpdate();

		long timestamp = NTP.getTime();
				
		//CREATE ACCOunting TRANSACTION
		Transaction messageTx = new AccountingTransaction(sender, (byte)feePow, recipient, key, amount, message, isText, encryptMessage, timestamp, sender.getLastReference(this.fork));		
		messageTx.sign(sender);
		
			
		return afterCreate(messageTx);
	}
	
	public Pair<Transaction, Integer> createJson1(PrivateKeyAccount creator,
			Account recipient, long key, BigDecimal amount, int feePow, byte[] isText,
			byte[] message, byte[] encryptMessage) {
		
		this.checkUpdate();
		
		long timestamp = NTP.getTime();
		
		//CREATE MESSAGE TRANSACTION
		Transaction messageTx = new JsonTransaction(creator, recipient, key, amount, (byte)feePow, message, isText, encryptMessage, timestamp, creator.getLastReference(this.fork));
		messageTx.sign(creator);
			
		return afterCreate(messageTx);
	}
	*/
	
	public Pair<Transaction, Integer> createTransactionFromRaw(byte[] rawData)
	{
		//CHECK FOR UPDATES
		this.checkUpdate();
								
		//CREATE TRANSACTION FROM RAW
		Transaction transaction;
		try {
			transaction = TransactionFactory.getInstance().parse(rawData, null);
		} catch (Exception e) {
			return new Pair<Transaction, Integer>(null, Transaction.INVALID_RAW_DATA);
		}
		
		//VALIDATE AND PROCESS
		return this.afterCreate(transaction, false);
	}
	
	private Pair<Transaction, Integer> afterCreate(Transaction transaction, boolean asPack)
	{
		//CHECK IF PAYMENT VALID
		int valid = transaction.isValid(this.fork, null);
		
		if(valid == Transaction.VALIDATE_OK)
		{

			if (!asPack) {
				//PROCESS IN FORK
				transaction.process(this.fork, asPack);
						
				//CONTROLLER ONTRANSACTION
				Controller.getInstance().onTransactionCreate(transaction);
			}
		}
				
		//RETURN
		return new Pair<Transaction, Integer>(transaction, valid);
	}
	
	
}
