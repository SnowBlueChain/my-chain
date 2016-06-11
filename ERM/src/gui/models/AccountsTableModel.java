package gui.models;

import java.math.BigDecimal;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.table.AbstractTableModel;

import utils.NumberAsString;
import utils.ObserverMessage;
import controller.Controller;
import core.account.Account;
import core.account.PublicKeyAccount;
import core.item.assets.AssetCls;
import core.transaction.Transaction;
import database.DBSet;
import lang.Lang;

@SuppressWarnings("serial")
public class AccountsTableModel extends AbstractTableModel implements Observer
{
	private static final int COLUMN_ADDRESS = 0;
//	public static final int COLUMN_BALANCE = 1;
	public static final int COLUMN_CONFIRMED_BALANCE = 1;
	public static final int COLUMN_WAINTING_BALANCE = 2;
	//public static final int COLUMN_GENERATING_BALANCE = 3;
	public static final int COLUMN_FEE_BALANCE = 3;
	
	private String[] columnNames = Lang.getInstance().translate(new String[]{"Address", "Confirmed Balance", "Waiting", AssetCls.FEE_NAME});
	private List<PublicKeyAccount> publicKeyAccounts;
	private AssetCls asset = null;
	
	public AccountsTableModel()
	{
		this.publicKeyAccounts = Controller.getInstance().getPublicKeyAccounts();
		Controller.getInstance().addWalletListener(this);
		Controller.getInstance().addObserver(this);
	}
	
	public Account getAccount(int row)
	{
		return (Account)publicKeyAccounts.get(row);
	}
	public PublicKeyAccount getPublicKeyAccount(int row)
	{
		return publicKeyAccounts.get(row);
	}
	
	public void setAsset(AssetCls asset) 
	{
		this.asset = asset;
		this.fireTableDataChanged();
	}
	
	@Override
	public int getColumnCount() 
	{
		return columnNames.length;
	}
	
	@Override
	public String getColumnName(int index) 
	{
		return columnNames[index];
	}

	@Override
	public int getRowCount() 
	{
		 return this.publicKeyAccounts.size();
	}

	@Override
	public Object getValueAt(int row, int column) 
	{
		if(this.publicKeyAccounts == null || row > this.publicKeyAccounts.size() - 1 )
		{
			return null;
		}
		
		Account account = this.publicKeyAccounts.get(row);
		
		switch(column)
		{
		case COLUMN_ADDRESS:			
			return account.asPerson();
		case COLUMN_CONFIRMED_BALANCE:
			if (this.asset == null) return "-";
			return NumberAsString.getInstance().numberAsString(account.getConfirmedBalance(this.asset.getKey(DBSet.getInstance())));			
		case COLUMN_WAINTING_BALANCE:
			if (this.asset == null) return "-";
			return NumberAsString.getInstance().numberAsString(account.getUnconfirmedBalance(this.asset.getKey(DBSet.getInstance()))
					.subtract(account.getConfirmedBalance(this.asset.getKey(DBSet.getInstance()))));
		case COLUMN_FEE_BALANCE:
			if (this.asset == null) return "-";
			return NumberAsString.getInstance().numberAsString(account.getConfirmedBalance(Transaction.FEE_KEY));
			
			
		/*	
			
		case COLUMN_GENERATING_BALANCE:
			
			if(this.asset == null || this.asset.getKey() == AssetCls.FEE_KEY)
			{
				return  NumberAsString.getInstance().numberAsString(account.getGeneratingBalance());	
			}
			else
			{
				return NumberAsString.getInstance().numberAsString(BigDecimal.ZERO.setScale(8));
			}
			*/
			
		}
		
		return null;
	}

	@Override
	public void update(Observable o, Object arg) 
	{
		try
		{
			this.syncUpdate(o, arg);
		}
		catch(Exception e)
		{
			//GUI ERROR
		}
	}
	
	public synchronized void syncUpdate(Observable o, Object arg)
	{
		ObserverMessage message = (ObserverMessage) arg;
		
		if( message.getType() == ObserverMessage.NETWORK_STATUS && (int) message.getValue() == Controller.STATUS_OK ) {
			
			this.fireTableRowsUpdated(0, this.getRowCount()-1);
			
		} else if (Controller.getInstance().getStatus() == Controller.STATUS_OK) {
			
			if(message.getType() == ObserverMessage.ADD_BLOCK_TYPE || message.getType() == ObserverMessage.REMOVE_BLOCK_TYPE || message.getType() == ObserverMessage.ADD_TRANSACTION_TYPE || message.getType() == ObserverMessage.REMOVE_TRANSACTION_TYPE)
			{
				this.publicKeyAccounts = Controller.getInstance().getPublicKeyAccounts();	
				
				this.fireTableRowsUpdated(0, this.getRowCount()-1);  // WHEN UPDATE DATA - SELECTION DOES NOT DISAPPEAR
			}
			
			if(message.getType() == ObserverMessage.ADD_ACCOUNT_TYPE || message.getType() == ObserverMessage.REMOVE_ACCOUNT_TYPE)
			{
				this.fireTableDataChanged();
			}
		}
	}

	public BigDecimal getTotalBalance() 
	{
		BigDecimal totalBalance = BigDecimal.ZERO.setScale(8);
		
		for(Account account: this.publicKeyAccounts)
		{
			if(this.asset == null)
			{
				totalBalance = totalBalance.add(account.getConfirmedBalance(Transaction.FEE_KEY));
			}
			else
			{
				totalBalance = totalBalance.add(account.getConfirmedBalance(this.asset.getKey(DBSet.getInstance())));
			}
		}
		
		return totalBalance;
	}
}
