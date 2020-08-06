package org.erachain.gui.items.accounts;

import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.transaction.RSend;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.transaction.TransactionAmount;
import org.erachain.gui.library.IssueConfirmDialog;
import org.erachain.gui.transaction.Send_RecordDetailsFrame;
import org.erachain.lang.Lang;

//import org.erachain.settings.Settings;

@SuppressWarnings("serial")

public class AccountAssetConfiscateDebtPanel extends AccountAssetActionPanelCls {

    public AccountAssetConfiscateDebtPanel(AssetCls assetIn, Account accountFrom, Account accountTo, PersonCls person) {
        super(null, null, true, assetIn,
                TransactionAmount.ACTION_DEBT, accountFrom, accountTo, null);

        iconName = "AccountAssetConfiscateDebtPanel";

    }

    @Override
    public void onSendClick() {

        // confirm params
        if (!cheskError()) return;

        // CREATE TX MESSAGE
        Transaction transaction = Controller.getInstance().r_Send((byte) 2, TransactionAmount.BACKWARD_MASK,
                (byte) 0, Controller.getInstance().getWalletPrivateKeyAccountByAddress(sender.getAddress()), feePow,
                recipient, -key, amount, head, messageBytes, isTextByte, encrypted);

        String Status_text = "";
        IssueConfirmDialog dd = new IssueConfirmDialog(null, true, transaction,
                Lang.getInstance().translate("Confiscate Debt"), (int) (this.getWidth() / 1.2),
                (int) (this.getHeight() / 1.2), Status_text, Lang.getInstance().translate("Confirmation Transaction"));
        Send_RecordDetailsFrame ww = new Send_RecordDetailsFrame((RSend) transaction);

        dd.jScrollPane1.setViewportView(ww);
        dd.pack();
        dd.setLocationRelativeTo(this);
        dd.setVisible(true);

        // JOptionPane.OK_OPTION
        if (dd.isConfirm) {

            result = Controller.getInstance().getTransactionCreator().afterCreate(transaction, Transaction.FOR_NETWORK);

            confirmaftecreatetransaction();
        }
        // ENABLE
        this.jButton_ok.setEnabled(true);
    }

}