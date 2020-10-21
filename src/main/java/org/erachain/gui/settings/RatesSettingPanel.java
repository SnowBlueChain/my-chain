package org.erachain.gui.settings;

import org.erachain.controller.Controller;
import org.erachain.core.item.ItemCls;
import org.erachain.core.item.assets.AssetCls;
import org.erachain.gui.items.assets.ComboBoxAssetsModel;
import org.erachain.gui.library.MTextFieldOnlyBigDecimal;
import org.erachain.gui.models.FavoriteComboBoxModel;
import org.erachain.lang.Lang;
import org.erachain.settings.Settings;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

public class RatesSettingPanel extends javax.swing.JPanel {

  
    /**
     * Creates new form RatesSettinng
     */
    public RatesSettingPanel() {

        initComponents();

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabelTitle = new javax.swing.JLabel();
        jLabelCOMPU = new javax.swing.JLabel();
        jLabelAsset = new javax.swing.JLabel();
        jTextFieldRate = new MTextFieldOnlyBigDecimal();

        jLabelDefaultAsset = new JLabel();

        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.columnWidths = new int[] {50, 10, 10, 10, 10, 10, 10};
        layout.rowHeights = new int[] {10, 10, 10, 10, 10, 10};
        setLayout(layout);

        int gridy = 0;
        jLabelTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelTitle.setText("<html><h2>" + Lang.getInstance().translate("FEE Rates for View") + "</h2><html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        //gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        add(jLabelTitle, gridBagConstraints);

        //jLabelCOMPU.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabelCOMPU.setText(Lang.getInstance().translate("Rate") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 10);
        add(jLabelCOMPU, gridBagConstraints);

        jTextFieldRate.setText(Settings.getInstance().getCompuRate());
        jTextFieldRate.setToolTipText(Lang.getInstance().translate("Must be numbers"));
        jTextFieldRate.setPreferredSize(new java.awt.Dimension(100, 30));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        add(jTextFieldRate, gridBagConstraints);

        jLabelAsset.setText(Lang.getInstance().translate("Asset of Rate") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 10);
        add(jLabelAsset, gridBagConstraints);

        // COMPU RATE ASSET
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        //gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        cbxFavoritesRate = new JComboBox<ItemCls>(new ComboBoxAssetsModel());
        cbxFavoritesRate.setRenderer(new FavoriteComboBoxModel.IconListRenderer());

        long key = Settings.getInstance().getCompuRateAsset();
        AssetCls asset = Controller.getInstance().getAsset(key);
        if (asset == null)
            asset = Controller.getInstance().getAsset(2L);

        cbxFavoritesRate.setSelectedItem(asset);

        cbxFavoritesRate.setPreferredSize(new java.awt.Dimension(200, 30));
        add(cbxFavoritesRate, gridBagConstraints);

        JLabel jExchangeTitle = new JLabel("<html><h2>" + Lang.getInstance().translate("Default Asset for Exchange Pair") + "</h2><html>");
        jExchangeTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        add(jExchangeTitle, gridBagConstraints);

        jLabelDefaultAsset.setText(Lang.getInstance().translate("Asset") + ":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 0, 10);
        add(jLabelDefaultAsset, gridBagConstraints);

        //FAVORITES GBC
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

        // DEFAULT PAIR ASSET
        cbxFavoritesPair = new JComboBox<ItemCls>(new ComboBoxAssetsModel());
        cbxFavoritesPair.setRenderer(new FavoriteComboBoxModel.IconListRenderer());
        asset = Settings.getInstance().getDefaultPairAsset();
        cbxFavoritesPair.setSelectedItem(asset);

        cbxFavoritesPair.setPreferredSize(new java.awt.Dimension(200, 30));
        this.add(cbxFavoritesPair, gridBagConstraints);


    }// </editor-fold>                        

    public BigDecimal getRate(){
        return new BigDecimal(jTextFieldRate.getText());
    }
    public AssetCls getRateAsset(){
        return (AssetCls)cbxFavoritesRate.getSelectedItem();
    }
    public AssetCls getDefaultPairAsset(){
        return (AssetCls)cbxFavoritesPair.getSelectedItem();
    }

    // Variables declaration - do not modify                     
    private javax.swing.JLabel jLabelTitle;
    private javax.swing.JLabel jLabelCOMPU;
    private javax.swing.JLabel jLabelAsset;
    public JComboBox<ItemCls> cbxFavoritesRate;
    private MTextFieldOnlyBigDecimal jTextFieldRate;

    private javax.swing.JLabel jLabelDefaultAsset;
    public JComboBox<ItemCls> cbxFavoritesPair;

    // End of variables declaration                   
}
