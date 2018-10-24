package org.erachain.gui.items.assets;

import java.awt.Image;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.erachain.core.item.assets.AssetCls;
import org.erachain.gui.models.BalancesTableModel;
import org.erachain.lang.Lang;

@SuppressWarnings("serial")
public class AssetFrame extends JFrame {
    private AssetCls asset;

    public AssetFrame(AssetCls asset) {
        super(Lang.getInstance().translate("Erachain.org") + " - " + Lang.getInstance().translate("Check Details"));

        this.asset = asset;

        //ICON
        List<Image> icons = new ArrayList<Image>();
        icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon16.png"));
        icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32.png"));
        icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon64.png"));
        icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon128.png"));
        this.setIconImages(icons);

        //CLOSE
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //LAYOUT
        //this.setLayout(new GridBagLayout());

        //TAB PANE
        JTabbedPane tabPane = new JTabbedPane();

        //DETAILS
        tabPane.add(Lang.getInstance().translate("Details"), new AssetDetailsPanel(this.asset));

        //BALANCES
        BalancesTableModel balancesTableModel = new BalancesTableModel(asset, -1);
        final JTable balancesTable = new JTable(balancesTableModel);
        tabPane.add(Lang.getInstance().translate("Holders"), new JScrollPane(balancesTable));

        //ADD TAB PANE
        this.add(tabPane);

        //PACK
        this.pack();
        //this.setSize(500, this.getHeight());
        //this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }
}