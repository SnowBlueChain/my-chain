package org.erachain.gui.settings;

import java.math.BigDecimal;

import org.erachain.gui.library.MTextFieldOnlyBigDecimal;
import org.erachain.lang.Lang;
import org.erachain.settings.Settings;
import org.erachain.utils.MenuPopupUtil;

public class RatesSettingPanel extends javax.swing.JPanel {

  
    /**
     * Creates new form RatesSettinng
     */
    public RatesSettingPanel() {
        initComponents();
        jLabelTitle.setText(Lang.getInstance().translate("Rates"));
        jLabelAsset.setText(Lang.getInstance().translate("1 COMPU = "));
        jTextFieldRate.setText(Settings.getInstance().getCompuRate()); 
        jLabelFiat.setText(Lang.getInstance().translate("USD"));
        jLabelBottom.setText("");
        jTextFieldRate.setToolTipText(Lang.getInstance().translate("Must be numbers"));
        MenuPopupUtil.installContextMenu(jTextFieldRate);
        

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
        jLabelAsset = new javax.swing.JLabel();
        jTextFieldRate = new MTextFieldOnlyBigDecimal();
        jLabelFiat = new javax.swing.JLabel();
        jLabelBottom = new javax.swing.JLabel();

        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.columnWidths = new int[] {0, 8, 0, 8, 0};
        layout.rowHeights = new int[] {0, 8, 0, 8, 0};
        setLayout(layout);

        jLabelTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelTitle.setText("jLabel1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        add(jLabelTitle, gridBagConstraints);

        jLabelAsset.setText("jLabel2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        add(jLabelAsset, gridBagConstraints);

        jTextFieldRate.setText("jTextField1");
        jTextFieldRate.setPreferredSize(new java.awt.Dimension(100, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        add(jTextFieldRate, gridBagConstraints);

        jLabelFiat.setText("jLabel3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        add(jLabelFiat, gridBagConstraints);

        jLabelBottom.setText("jLabel4");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.1;
        add(jLabelBottom, gridBagConstraints);
    }// </editor-fold>                        

    public BigDecimal getRate(){
        return new BigDecimal(jTextFieldRate.getText());
    }

    // Variables declaration - do not modify                     
    private javax.swing.JLabel jLabelTitle;
    private javax.swing.JLabel jLabelAsset;
    private javax.swing.JLabel jLabelFiat;
    private javax.swing.JLabel jLabelBottom;
    private MTextFieldOnlyBigDecimal jTextFieldRate;
    // End of variables declaration                   
}
