package gui.items.assets;

import core.item.assets.AssetCls;
import gui.CoreRowSorter;
import gui.models.WalletItemAssetsTableModel;
import lang.Lang;
import utils.TableMenuPopupUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.TreeMap;

//////////

@SuppressWarnings("serial")
public class AssetsPanel extends JPanel {
    public AssetsPanel() {
        this.setLayout(new GridBagLayout());

        //PADDING
        this.setBorder(new EmptyBorder(10, 10, 10, 10));

        //TABLE GBC
        GridBagConstraints tableGBC = new GridBagConstraints();
        tableGBC.fill = GridBagConstraints.BOTH;
        tableGBC.anchor = GridBagConstraints.NORTHWEST;
        tableGBC.weightx = 1;
        tableGBC.weighty = 1;
        tableGBC.gridwidth = 10;
        tableGBC.gridx = 0;
        tableGBC.gridy = 0;

        //BUTTON GBC
        GridBagConstraints buttonGBC = new GridBagConstraints();
        buttonGBC.insets = new Insets(10, 0, 0, 10);
        buttonGBC.fill = GridBagConstraints.NONE;
        buttonGBC.anchor = GridBagConstraints.NORTHWEST;
        buttonGBC.gridx = 0;
        buttonGBC.gridy = 1;

        //TABLE
        final WalletItemAssetsTableModel assetsModel = new WalletItemAssetsTableModel();
        final JTable table = new JTable(assetsModel);

        //POLLS SORTER
        Map<Integer, Integer> indexes = new TreeMap<Integer, Integer>();
        CoreRowSorter sorter = new CoreRowSorter(assetsModel, indexes);
        table.setRowSorter(sorter);

        //CHECKBOX FOR ASSET TYPE
        TableColumn divisibleColumn = table.getColumnModel().getColumn(WalletItemAssetsTableModel.COLUMN_ASSET_TYPE);
        divisibleColumn.setCellRenderer(table.getDefaultRenderer(Boolean.class));

        //CHECKBOX FOR CONFIRMED
        TableColumn confirmedColumn = table.getColumnModel().getColumn(WalletItemAssetsTableModel.COLUMN_CONFIRMED);
        confirmedColumn.setCellRenderer(table.getDefaultRenderer(Boolean.class));

        //CHECKBOX FOR FAVORITE
        TableColumn favoriteColumn = table.getColumnModel().getColumn(WalletItemAssetsTableModel.COLUMN_FAVORITE);
        favoriteColumn.setCellRenderer(table.getDefaultRenderer(Boolean.class));

        //MENU
        JPopupMenu assetsMenu = new JPopupMenu();
        JMenuItem details = new JMenuItem(Lang.getInstance().translate("Details"));
        details.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                row = table.convertRowIndexToModel(row);

                AssetCls asset = assetsModel.getAsset(row);
                new AssetFrame(asset);
            }
        });
        assetsMenu.add(details);
        JMenuItem dividend = new JMenuItem(Lang.getInstance().translate("Pay dividend"));
        dividend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                row = table.convertRowIndexToModel(row);

                AssetCls asset = assetsModel.getAsset(row);
                new PayDividendFrame(asset);
            }
        });
        assetsMenu.add(dividend);
      //  table.setComponentPopupMenu(assetsMenu);
        TableMenuPopupUtil.installContextMenu(table, assetsMenu);  // SELECT ROW ON WHICH CLICKED RIGHT BUTTON


        //MOUSE ADAPTER
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                int row = table.rowAtPoint(p);
                table.setRowSelectionInterval(row, row);
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();
                int row = table.rowAtPoint(p);
                table.setRowSelectionInterval(row, row);

                if (e.getClickCount() == 2) {
                    row = table.convertRowIndexToModel(row);
                    AssetCls asset = assetsModel.getAsset(row);
                    new AssetFrame(asset);
                }
            }
        });

        //ADD NAMING SERVICE TABLE
        this.add(new JScrollPane(table), tableGBC);

        //ADD REGISTER BUTTON
        JButton issueButton = new JButton(Lang.getInstance().translate("Issue Asset"));
        issueButton.setPreferredSize(new Dimension(120, 25));
        issueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onIssueClick();
            }
        });
        this.add(issueButton, buttonGBC);

        //ADD ALL BUTTON
        buttonGBC.gridx = 1;
        JButton allButton = new JButton(Lang.getInstance().translate("All Assets"));
        allButton.setPreferredSize(new Dimension(120, 25));
        allButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onAllClick();
            }
        });
        this.add(allButton, buttonGBC);

        //ADD MY ORDERS BUTTON
        buttonGBC.gridx = 2;
        JButton myOrdersButton = new JButton(Lang.getInstance().translate("My Orders"));
        myOrdersButton.setPreferredSize(new Dimension(120, 25));
        myOrdersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onMyOrdersClick();
            }
        });
        this.add(myOrdersButton, buttonGBC);
    }

    public void onIssueClick() {
        new IssueAssetFrame();
    }

    public void onAllClick() {
        new AllAssetsFrame();
    }

    public void onMyOrdersClick() {
        new MyOrdersFrame();
    }
}
