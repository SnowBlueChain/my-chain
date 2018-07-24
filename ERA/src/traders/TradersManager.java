package traders;
// 30/03 ++

import controller.Controller;
import core.BlockChain;
import core.account.Account;
import core.crypto.Base58;
import datachain.DCSet;
import network.*;
import network.message.FindMyselfMessage;
import network.message.Message;
import network.message.MessageFactory;
import network.message.TelegramMessage;
import org.apache.log4j.Logger;
import org.mapdb.Fun.Tuple2;
import settings.Settings;
import utils.ObserverMessage;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

// import org.apache.log4j.Logger;
//import core.BlockChain;
//import database.DBSet;
//import database.TransactionMap;
//import lang.Lang;

public class TradersManager extends Observable {

    protected static final String WALLET_PASSWORD = "123456789";

    private static final Logger LOGGER = Logger.getLogger(TradersManager.class);
    private List<Rater> knownRaters;
    private List<Trader> knownTraders;
    //private boolean run;

    public TradersManager() {
        this.knownRaters = new ArrayList<Rater>();
        this.knownTraders = new ArrayList<Trader>();
        //this.run = true;

        this.start();
    }

    private void start() {

        //START RATERs THREADs
        RaterWEX raterForex = new RaterWEX(this, 300);
        this.knownRaters.add(raterForex);
        RaterLiveCoin raterLiveCoin = new RaterLiveCoin(this, 600);
        this.knownRaters.add(raterLiveCoin);
        RaterPolonex raterPolonex = new RaterPolonex(this, 600);
        this.knownRaters.add(raterPolonex);


        // WAIT START WALLET
        while(!Controller.getInstance().doesWalletExists()) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                //FAILED TO SLEEP
            }
        }

        //START TRADERs THREADs

        TreeMap<BigDecimal, BigDecimal> scheme = new TreeMap<>();
        scheme.put(new BigDecimal(10000), new BigDecimal(1));
        scheme.put(new BigDecimal(1000), new BigDecimal(0.5));
        scheme.put(new BigDecimal(100), new BigDecimal(0.2));
        scheme.put(new BigDecimal(-100), new BigDecimal(0.2));
        scheme.put(new BigDecimal(-1000), new BigDecimal(0.5));
        scheme.put(new BigDecimal(-10000), new BigDecimal(1));
        Account account = Controller.getInstance().wallet.getAccounts().get(1);
        Trader trader1 = new TraderA(this, account.getAddress(), 30,
                1077, 1078, scheme);
        this.knownTraders.add(trader1);

    }

    @Override
    public void addObserver(Observer o) {
        super.addObserver(o);

        //SEND CONNECTEDPEERS ON REGISTER
        o.update(this, new ObserverMessage(ObserverMessage.TRADERS_UPDATE_TYPE, this.knownRaters));
    }

    public void notifyObserveUpdateRater(Rater rater) {
        //NOTIFY OBSERVERS
        this.setChanged();
        this.notifyObservers(new ObserverMessage(ObserverMessage.TRADERS_UPDATE_TYPE, rater));

    }

    public void setRun(boolean status) {

        for (Rater rater: this.knownRaters) {
            rater.setRun(status);
        }
    }

    public void stop() {

        for (Rater rater: this.knownRaters) {
            rater.setRun(false);
        }
    }
}
