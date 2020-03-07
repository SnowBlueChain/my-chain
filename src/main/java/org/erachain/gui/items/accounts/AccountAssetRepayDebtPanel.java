package org.erachain.gui.items.accounts;

import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.core.item.persons.PersonCls;
import org.erachain.core.transaction.RSend;
import org.erachain.core.transaction.Transaction;
import org.erachain.core.transaction.TransactionAmount;
import org.erachain.gui.library.IssueConfirmDialog;
import org.erachain.gui.library.MainPanelInterface;
import org.erachain.gui.transaction.Send_RecordDetailsFrame;
import org.erachain.lang.Lang;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")

public class AccountAssetRepayDebtPanel extends AccountAssetActionPanelCls implements MainPanelInterface {

    // private final MessagesTableModel messagesTableModel;
    private String iconFile = "images/pageicons/AccountAssetRepayDebtPanel.png";
    public AccountAssetRepayDebtPanel(AssetCls assetIn, Account accountFrom, Account accountTo, PersonCls person) {
        super(false, null, assetIn, null,
                TransactionAmount.ACTION_DEBT, accountFrom, accountTo, null);

        // icon.setIcon(null);

    }

    @Override
    public void onSendClick() {
        // confirm params
     
        if (!cheskError()) return;
        // CREATE TX MESSAGE
        Transaction transaction = Controller.getInstance().r_Send(
                Controller.getInstance().getPrivateKeyAccountByAddress(sender.getAddress()), feePow, recipient, -key,
                amount, head, messageBytes, isTextByte, encrypted, 0);

        String Status_text = "";
        IssueConfirmDialog dd = new IssueConfirmDialog(null, true, transaction,
                Lang.getInstance().translate("Repay Debt"), (int) (this.getWidth() / 1.2),
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
    @Override
    public Icon getIcon() {
        {
            try {
                return new ImageIcon(Toolkit.getDefaultToolkit().getImage(iconFile));
            } catch (Exception e) {
                return null;
            }
        }
    }
}
