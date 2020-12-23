package org.erachain.gui;

import org.erachain.controller.Controller;
import org.erachain.core.account.Account;
import org.erachain.core.block.Block;
import org.erachain.core.transaction.RSend;
import org.erachain.core.transaction.Transaction;
import org.erachain.datachain.DCSet;
import org.erachain.lang.Lang;
import org.erachain.settings.Settings;
import org.erachain.utils.ObserverMessage;
import org.erachain.utils.PlaySound;
import org.erachain.utils.SysTray;
import org.mapdb.Fun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings("serial")
public class WalletTimer<U> implements Observer {

    public Object playEvent;

    protected Logger logger;
    Controller contr = Controller.getInstance();
    Settings settings = Settings.getInstance();
    SysTray sysTray = SysTray.getInstance();
    PlaySound playSound = PlaySound.getInstance();
    Lang lang = Lang.getInstance();
    private List<Integer> transactionsAlreadyPlayed;

    public WalletTimer() {
        transactionsAlreadyPlayed = new ArrayList<>();

        logger = LoggerFactory.getLogger(this.getClass());
        Controller.getInstance().guiTimer.addObserver(this); // обработка repaintGUI

    }

    public void playEvent(Object playEvent) {
        this.playEvent = playEvent;
    }

    public void update(Observable o, Object arg) {

        if (!contr.doesWalletExists() || contr.wallet.synchronizeBodyUsed
                || !settings.isTrayEventEnabled())
            return;

        ObserverMessage messageObs = (ObserverMessage) arg;

        if (messageObs.getType() == ObserverMessage.GUI_REPAINT && playEvent != null) {
            Object event = playEvent;
            playEvent = null;

            if (transactionsAlreadyPlayed.contains(event.hashCode()))
                return;

            transactionsAlreadyPlayed.add(event.hashCode());
            while (transactionsAlreadyPlayed.size() > 100) {
                transactionsAlreadyPlayed.remove(0);
            }

            String sound = null;
            String head = null;
            String message = null;
            TrayIcon.MessageType type = TrayIcon.MessageType.NONE;

            if (event instanceof Transaction) {

                Transaction transaction = (Transaction) event;
                Account creator = transaction.getCreator();

                settings.isSoundReceiveMessageEnabled();


                if (transaction instanceof RSend) {
                    RSend rSend = (RSend) transaction;
                    if (rSend.hasAmount()) {
                        // TRANSFER
                        if (contr.wallet.accountExists(creator)) {
                            if (settings.isSoundNewTransactionEnabled())
                                sound = "send.wav";

                            head = lang.translate("Payment send");
                            message = rSend.getCreator().getPersonAsString() + " -> \n "
                                    + rSend.getAmount().toPlainString() + " [" + rSend.getAbsKey() + "]\n "
                                    + rSend.getRecipient().getPersonAsString() + "\n"
                                    + (rSend.getTitle() != null ? "\n" + rSend.getTitle() : "");
                        } else {

                            if (settings.isSoundReceivePaymentEnabled())
                                sound = "receivepayment.wav";

                            head = lang.translate("Payment received");
                            message = rSend.getRecipient().getPersonAsString() + " <- \n "
                                    + rSend.getAmount().toPlainString() + " [" + rSend.getAbsKey() + "]\n "
                                    + rSend.getCreator().getPersonAsString() + "\n"
                                    + (rSend.getTitle() != null ? "\n" + rSend.getTitle() : "");
                        }
                    } else {
                        // MAIL
                        if (contr.wallet.accountExists(rSend.getCreator())) {
                            if (settings.isSoundNewTransactionEnabled())
                                sound = "send.wav";

                            head = lang.translate("Mail send");
                            message = rSend.getCreator().getPersonAsString() + " -> \n "
                                    //+ rSend.getAmount().toPlainString() + "[" + rSend.getAbsKey() + "]\n "
                                    + rSend.getRecipient().getPersonAsString() + "\n"
                                    + (rSend.getTitle() != null ? "\n" + rSend.getTitle() : "");
                        } else {
                            if (settings.isSoundReceiveMessageEnabled())
                                sound = "receivemail.wav";

                            head = lang.translate("Mail received");
                            message = rSend.getRecipient().getPersonAsString() + " <- \n "
                                    //+ rSend.getAmount().toPlainString() + "[" + rSend.getAbsKey() + "]\n "
                                    + rSend.getCreator().getPersonAsString() + "\n"
                                    + (rSend.getTitle() != null ? "\n" + rSend.getTitle() : "");
                        }

                    }

                } else {

                    if (contr.wallet.accountExists(transaction.getCreator())) {
                        if (settings.isSoundNewTransactionEnabled())
                            sound = "outcometransaction.wav";

                        head = lang.translate("Outcome transaction") + ": " + transaction.viewFullTypeName();
                        message = transaction.getTitle();
                    } else {
                        if (settings.isSoundNewTransactionEnabled())
                            sound = "incometransaction.wav";
                        head = lang.translate("Income transaction") + ": " + transaction.viewFullTypeName();
                        message = transaction.getTitle();
                    }
                }
            } else if (event instanceof Block) {

                Block.BlockHead blockHead = ((Block) event).blockHead;
                if (blockHead.heightBlock == 1) {
                    return;
                }
                Fun.Tuple3<Integer, Integer, Integer> forgingPoint = blockHead.creator.getForgingData(DCSet.getInstance(), blockHead.heightBlock);
                if (forgingPoint == null)
                    return;

                if (settings.isSoundForgedBlockEnabled()) {
                    sound = "blockforge.wav";
                }

                head = lang.translate("Forging Block %d").replace("%d", "" + blockHead.heightBlock);
                message = lang.translate("Forging Fee") + ": " + blockHead.viewFeeAsBigDecimal();

                int diff = blockHead.heightBlock - forgingPoint.a;
                if (diff < 300) {
                    head = null;
                    sound = null;
                } else if (diff < 1000) {
                    sound = null;
                }

            } else {
                head = lang.translate("EVENT");
                sound = "receivemail.wav";
                message = event.toString();
            }

            if (sound != null)
                playSound.playSound(sound);

            if (head != null) {
                sysTray.sendMessage(head, message, type);
            }

        }
    }

}
