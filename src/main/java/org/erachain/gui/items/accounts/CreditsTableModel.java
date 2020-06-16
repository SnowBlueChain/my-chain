package org.erachain.gui.items.accounts;

import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.account.PublicKeyAccount;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.transaction.Transaction;
import org.erachain.database.SortableList;
import org.erachain.database.wallet.TransactionMap;
import org.erachain.datachain.DCSet;
import org.erachain.gui.models.SortedListTableModelCls;
import org.erachain.lang.Lang;
import org.erachain.utils.ObserverMessage;
import org.erachain.utils.Pair;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings("serial")
public class CreditsTableModel extends SortedListTableModelCls<Tuple2<Long, Long>, Transaction> implements Observer {
    public static final int COLUMN_AMOUNT = 1;
    public static final int COLUMN_TRANSACTION = 2;
    private static final int COLUMN_ADDRESS = 0;
    //	public static final int COLUMN_CONFIRMED_BALANCE = 1;
    //	public static final int COLUMN_WAINTING_BALANCE = 2;
    //public static final int COLUMN_GENERATING_BALANCE = 3;
    //	public static final int COLUMN_FEE_BALANCE = 3;
    //	private AssetCls asset = core.block.GenesisBlock.makeAsset(asset_Key);
    //private Account account;
    List<Tuple2<Tuple3<String, Long, String>, BigDecimal>> cred;
    private Boolean[] column_AutuHeight = new Boolean[]{true, false, false, false};
    private List<PublicKeyAccount> publicKeyAccounts;
    private long asset_Key = 1l;
    private SortableList<Tuple2<Long, Long>, Transaction> transactions;
    private List<Tuple2<Tuple2<Long, Long>, Transaction>> transactions_Asset;

    @SuppressWarnings("unchecked")
    public CreditsTableModel() {
        super(DCSet.getInstance().getCredit_AddressesMap(),
                new String[]{"Account", "Amount", "Type"}, false);

        logger = LoggerFactory.getLogger(CreditsTableModel.class);

    }

    @Override
    public SortableList<Tuple2<Long, Long>, Transaction> getSortableList() {
        return this.transactions;
    }

    @Override
    public Class<? extends Object> getColumnClass(int c) {     // set column type
        Object o = getValueAt(0, c);
        return o == null ? Null.class : o.getClass();
    }

    // читаем колонки которые изменяем высоту
    public Boolean[] getColumnAutoHeight() {

        return this.column_AutuHeight;
    }

    // устанавливаем колонки которым изменить высоту
    public void setColumnAutoHeight(Boolean[] arg0) {
        this.column_AutuHeight = arg0;
    }


    public Account getAccount(int row) {
        return publicKeyAccounts.get(row);
    }

    public PublicKeyAccount getPublicKeyAccount(int row) {
        return publicKeyAccounts.get(row);
    }

    public void setAsset(AssetCls asset) {
        asset_Key = asset.getKey();
        cred.clear();
        for (PublicKeyAccount account : this.publicKeyAccounts) {
            List<Transaction> trans = DCSet.getInstance().getTransactionFinalMap().getTransactionsByAddressLimit(account.getShortAddressBytes(), 1000, true, descending);
            cred.addAll(DCSet.getInstance().getCredit_AddressesMap().getList(account.getAddress(), -asset_Key));
        }
		/*		for (Pair<Tuple2<Long, Long>, Transaction> trans:this.transactions){
			long a = trans.getB().getAssetKey();
			this.transactions_Asset.clear();
				if (a == asset_Key){

					this.transactions_Asset.add(trans);


				}


			}
		 */
        this.transactions_Asset.clear();
        ;
        for (Pair<Tuple2<Long, Long>, Transaction> trans : this.transactions) {
            long a = trans.getB().getAssetKey();
            Tuple2<Tuple2<Long, Long>, Transaction> ss = null;
            if (a == asset_Key || a == -asset_Key) {
                ss = new Tuple2(trans.getA(), trans.getB());
                this.transactions_Asset.add(ss);
            }
        }

        this.fireTableDataChanged();
    }

    @Override
    public int getRowCount() {

        return transactions_Asset.size();
    }

    @Override
    public Object getValueAt(int row, int column) {

        if (transactions_Asset.isEmpty()) return null;

		/*	if(this.publicKeyAccounts == null || row > this.publicKeyAccounts.size() - 1 )
		{
			return null;
		}


		account = this.publicKeyAccounts.get(row);

		Tuple3<BigDecimal, BigDecimal, BigDecimal> balance;
		Tuple3<BigDecimal, BigDecimal, BigDecimal> unconfBalance;
		String str;
		 */
        switch (column) {
            case COLUMN_ADDRESS:
                return transactions_Asset.get(row).b.getKey();
            case COLUMN_AMOUNT:
                return transactions_Asset.get(row).b.getAmount().toPlainString();
            case COLUMN_TRANSACTION:
                return Lang.getInstance().translate(transactions_Asset.get(row).b.viewTypeName());
			/*
		case COLUMN_CONFIRMED_BALANCE:
			if (this.asset == null) return "-";
			balance = account.getBalance(this.asset.getKey(DLSet.getInstance()));
			str = NumberAsString.getInstance().numberAsString(balance.a) + "/" + balance.b.toPlainString() + "/" + balance.c.toPlainString();
			return str;
		case COLUMN_WAINTING_BALANCE:
			if (this.asset == null) return "-";
			balance = account.getBalance(this.asset.getKey(DLSet.getInstance()));
			unconfBalance = account.getUnconfirmedBalance(this.asset.getKey(DLSet.getInstance()));
			str = NumberAsString.getInstance().numberAsString(unconfBalance.a.subtract(balance.a))
					+ "/" + unconfBalance.b.subtract(balance.b).toPlainString()
					+ "/" + unconfBalance.c.subtract(balance.c).toPlainString();
			return str;
		case COLUMN_FEE_BALANCE:
			if (this.asset == null) return "-";
			return NumberAsString.getInstance().numberAsString(account.getBalanceUSE(Transaction.FEE_KEY));
			 */

			/*

		case COLUMN_GENERATING_BALANCE:

			if(this.asset == null || this.asset.getKey() == AssetCls.FEE_KEY)
			{
				return  NumberAsString.getInstance().numberAsString(account.getGeneratingBalance());
			}
			else
			{
				return NumberAsString.getInstance().numberAsString(BigDecimal.ZERO);
			}
			 */

        }

        return null;
    }

    @Override
    public void update(Observable o, Object arg) {
        try {
            this.syncUpdate(o, arg);
        } catch (Exception e) {
            //GUI ERROR
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void syncUpdate(Observable o, Object arg) {
        ObserverMessage message = (ObserverMessage) arg;


        //CHECK IF NEW LIST
        if (message.getType() == ObserverMessage.WALLET_LIST_TRANSACTION_TYPE) {
            if (this.transactions == null) {
                this.transactions = (SortableList<Tuple2<Long, Long>, Transaction>) message.getValue();
                //this.transactions.registerObserver();
                this.transactions.sort(TransactionMap.TIMESTAMP_INDEX, true);

                this.transactions_Asset.clear();
                ;
                for (Pair<Tuple2<Long, Long>, Transaction> trans : this.transactions) {
                    long a = trans.getB().getAssetKey();
                    Tuple2<Tuple2<Long, Long>, Transaction> ss = null;
                    if (a == asset_Key || a == -asset_Key) {

                        ss = new Tuple2(trans.getA(), trans.getB());


                        this.transactions_Asset.add(ss);


                    }


                }


            }

            this.fireTableDataChanged();
        }


        if (message.getType() == ObserverMessage.WALLET_ADD_BLOCK_TYPE || message.getType() == ObserverMessage.WALLET_REMOVE_BLOCK_TYPE
                || message.getType() == ObserverMessage.WALLET_ADD_TRANSACTION_TYPE || message.getType() == ObserverMessage.WALLET_REMOVE_TRANSACTION_TYPE) {
            this.publicKeyAccounts = Controller.getInstance().getWalletPublicKeyAccounts();
            cred.clear();
            for (PublicKeyAccount account : this.publicKeyAccounts) {
                cred.addAll(DCSet.getInstance().getCredit_AddressesMap().getList(account.getAddress(), -asset_Key));
                //cred.addAll(DLSet.getInstance().getCredit_AddressesMap().getList(Base58.decode(account.getAddress()), asset_Key));
            }


            this.fireTableDataChanged();

            //	this.fireTableRowsUpdated(0, this.getRowCount()-1);  // WHEN UPDATE DATA - SELECTION DOES NOT DISAPPEAR
        }
		/*
			if(message.getType() == ObserverMessage.ADD_ACCOUNT_TYPE || message.getType() == ObserverMessage.REMOVE_ACCOUNT_TYPE)
			{
	// обновляем данные
				this.publicKeyAccounts = Controller.getInstance().getPublicKeyAccounts();
				this.fireTableDataChanged();
			}
		 */


    }

    public BigDecimal getTotalBalance() {
        BigDecimal totalBalance = BigDecimal.ZERO;

        for (Account account : this.publicKeyAccounts) {
            totalBalance = totalBalance.add(account.getBalanceUSE(this.asset_Key));
        }

        return totalBalance;
    }

    public void addObservers() {

        this.transactions_Asset = new ArrayList<Tuple2<Tuple2<Long, Long>, Transaction>>();
        this.publicKeyAccounts = Controller.getInstance().getWalletPublicKeyAccounts();

        cred = new ArrayList<Tuple2<Tuple3<String, Long, String>, BigDecimal>>();
        for (PublicKeyAccount account : this.publicKeyAccounts) {
            //cred.addAll(DLSet.getInstance().getCredit_AddressesMap().getList(Base58.decode(account.getAddress()), asset_Key));
            cred.addAll(DCSet.getInstance().getCredit_AddressesMap().getList(account.getAddress(), -asset_Key));
        }

        Controller.getInstance().addWalletObserver(this);
        //		Controller.getInstance().addObserver(this);
        //		int a = 1;

    }


    public void deleteObservers() {
    }

    @Override
    public Transaction getItem(int k) {
        // TODO Auto-generated method stub
        return transactions_Asset.get(k).b;
    }
}
