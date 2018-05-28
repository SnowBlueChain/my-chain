package gui.status;

import controller.Controller;
import core.wallet.Wallet;
import lang.Lang;
import org.apache.log4j.Logger;
import utils.ObserverMessage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

@SuppressWarnings("serial")
public class WalletStatus extends JLabel implements Observer {
    static Logger LOGGER = Logger.getLogger(WalletStatus.class.getName());

    private ImageIcon unlockedIcon;
    private ImageIcon lockedIcon;

    public WalletStatus() {
        super();

        try {
            //LOAD IMAGES
            BufferedImage unlockedImage = ImageIO.read(new File("images/wallet/unlocked.png"));
            this.unlockedIcon = new ImageIcon(unlockedImage.getScaledInstance(20, 16, Image.SCALE_SMOOTH));

            BufferedImage lockedImage = ImageIO.read(new File("images/wallet/locked.png"));
            this.lockedIcon = new ImageIcon(lockedImage.getScaledInstance(20, 16, Image.SCALE_SMOOTH));

            //LISTEN ON WALLET
            Controller.getInstance().addWalletListener(this);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void update(Observable arg0, Object arg1) {
        ObserverMessage message = (ObserverMessage) arg1;

        if (message.getType() == ObserverMessage.WALLET_STATUS) {
            int status = (int) message.getValue();

            if (status == Wallet.STATUS_UNLOCKED) {
                this.setIcon(this.unlockedIcon);
                this.setText(Lang.getInstance().translate("Wallet is unlocked"));
            } else {
                this.setIcon(this.lockedIcon);
                this.setText(Lang.getInstance().translate("Wallet is locked"));
            }
        }
    }
}
