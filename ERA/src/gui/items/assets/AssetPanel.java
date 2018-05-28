/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.items.assets;

import core.item.assets.AssetCls;
import gui.models.BalancesTableModel;
import lang.Lang;

import javax.swing.*;

/**
 * @author Саша
 */
public class AssetPanel extends javax.swing.JPanel {

    // Variables declaration - do not modify
    private javax.swing.JPanel jPanel1;
    private Asset_Info jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTable2;
    /**
     * Creates new form asset_right_panel
     */
    public AssetPanel(AssetCls asset) {
        initComponents(asset);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents(AssetCls asset) {
        java.awt.GridBagConstraints gridBagConstraints;

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel3 = new Asset_Info(asset);
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();

        setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());
        //jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jScrollPane1.setBorder(null);
        jScrollPane2.setBorder(null);

        jScrollPane1.setViewportView(jPanel3);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel1.add(jScrollPane1, gridBagConstraints);

        jTabbedPane1.addTab(Lang.getInstance().translate("Details"), jPanel1);


        //BALANCES
        BalancesTableModel balancesTableModel = new BalancesTableModel(asset.getKey());
        final JTable balancesTable = new JTable(balancesTableModel);

        jScrollPane2.setViewportView(balancesTable);

        //    jTabbedPane1.addTab(Lang.getInstance().translate("Holders"), jScrollPane2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 8, 5);
        add(jTabbedPane1, gridBagConstraints);
    }// </editor-fold>
    // End of variables declaration                   
}
