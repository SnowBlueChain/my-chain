package org.erachain.gui.telegrams;

import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.crypto.Base58;
import org.erachain.core.transaction.RSend;
import org.erachain.core.transaction.Transaction;
import org.erachain.database.wallet.TelegramsMap;
import org.erachain.gui.models.WalletTableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;

@SuppressWarnings("serial")
public class WalletTelegramsFilterTableModel extends WalletTableModel<Transaction> {

    private boolean needUpdate = false;
    private long timeUpdate = 0;
    private String sender;
    private String receiver;

    public static final int COLUMN_DATE = 0;
    public static final int COLUMN_SENDER = 1;
    public static final int COLUMN_RECEIVER = 2;
    public static final int COLUMN_MESSAGE = 3;
    public static final int COLUMN_SIGNATURE = 4;

    static Logger LOGGER = LoggerFactory.getLogger(WalletTelegramsFilterTableModel.class);

    public WalletTelegramsFilterTableModel() {
        super(Controller.getInstance().getWallet().database.getTelegramsMap(),
                new String[]{"Date", "Sender", "Recipient", "Message", "Signature"},
                new Boolean[]{true, true, true, true, true, true, true, false, false}, false);

    }

    @Override
    public Object getValueAt(int row, int column) {

        if (this.list == null || this.list.size() == 0) {
            return null;
        }

        RSend rSend = (RSend) list.get(row);
        if (rSend == null)
            return null;

        switch (column) {
            case COLUMN_DATE:
                return rSend.viewTimestamp();
            case COLUMN_SENDER:
                return rSend.viewCreator();
            case COLUMN_RECEIVER:
                return rSend.viewRecipient();
            case COLUMN_MESSAGE:
                if (rSend.isEncrypted() && Controller.getInstance().isWalletUnlocked()) {
                    byte[] dataMess = Controller.getInstance().decrypt(rSend.getCreator(), rSend.getRecipient(), rSend.getData());

                    String message;

                    if (dataMess != null) {
                        if (rSend.isText()) {
                            try {
                                message = new String(dataMess, "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                message = "error UTF-8";
                            }
                        } else {
                            message = Base58.encode(dataMess);
                        }
                    } else {
                        message = "decode error";
                    }
                } else {
                    return rSend.viewData();
                }
            case COLUMN_SIGNATURE:
                return rSend.viewSignature();

        }

        return null;

    }

    @Override
    public void getInterval() {

        list = new ArrayList<>();

        for (Transaction transaction : ((TelegramsMap) map).values()) {
            HashSet<Account> recipients = transaction.getRecipientAccounts();
            if (receiver != null) {

                if (transaction.getCreator().getAddress().equals(sender)) {
                    for (Account recipient : recipients) {
                        if (recipient.getAddress().equals(receiver)) {
                            //list.add(new Tuple3(sender, reciever, transaction));
                            list.add(0, transaction);
                            continue;
                        }

                    }

                }
                if (transaction.getCreator().getAddress().equals(receiver)) {
                    for (Account pecipient : recipients) {
                        if (pecipient.getAddress().equals(sender)) {
                            //Tuple3 tt = new Tuple3(receiver, sender, transaction);
                            if (!list.contains(transaction))
                                list.add(0, transaction);
                        }
                    }
                }


            } else {
                // add all recipients

                if (transaction.getCreator().getAddress().equals(sender)) {

                    for (Account recipient : recipients) {
                        //list.add(new Tuple3(sender,recipient.getAddress(),transaction));
                        list.add(0, transaction);
                    }
                } else {
                    // add recipient = sender
                    for (Account recipient : recipients) {
                        if (recipient.getAddress().equals(sender)) {
                            //ttt.add(new Tuple3(transaction.getCreator().getAddress(), sender, transaction));
                            list.add(0, transaction);
                            continue;
                        }
                    }
                }
            }
        }
    }

    /**
     * @param sender the sender to set
     */
    public void setSender(String sender) {
        this.sender = sender;
        getInterval();
        this.fireTableDataChanged();
    }

    /**
     * @param receiver the receiver to set
     */
    public void setReceiver(String receiver) {
        this.receiver = receiver;
        getInterval();
        this.fireTableDataChanged();
    }

    /**
     * @return the sender
     */
    public String getSender() {
        return sender;
    }

    /**
     * @return the receiver
     */
    public String getReceiver() {
        return receiver;
    }

}
