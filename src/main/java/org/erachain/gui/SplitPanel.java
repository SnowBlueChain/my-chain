/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.erachain.gui;

import org.erachain.controller.Controller;
import org.erachain.gui.library.MSplitPane;
import org.erachain.gui.library.MTable;
import org.erachain.lang.Lang;
import org.erachain.settings.Settings;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;


/**
 * @author Саша
 */
public class SplitPanel extends JPanel {

    protected Logger logger;

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public JPanel search_Info_Panel;
    public JLabel Label_search_Info_Panel;
    // Variables declaration - do not modify
    public JButton button1ToolBarLeftPanel;
    public JButton button2ToolBarLeftPanel;
    public JButton jButton1_jToolBar_RightPanel;
    public JButton jButton2_jToolBar_RightPanel;
    public JLabel jLabel2;
    public JPanel jPanel_RightPanel;
    public JScrollPane jScrollPaneJPanelRightPanel;
    public JScrollPane jScrollPanelLeftPanel;
    public MSplitPane jSplitPanel;
    @SuppressWarnings("rawtypes")
    public MTable jTableJScrollPanelLeftPanel;
    public JToolBar jToolBarRightPanel;
    public JPanel leftPanel;
    public JPanel rightPanel1;
    public JTextField searchTextFieldSearchToolBarLeftPanelDocument;
    public JToolBar searchToolBar_LeftPanel;
    public JLabel searthLabelSearchToolBarLeftPanel;
    public JToolBar toolBarLeftPanel;
    public JCheckBox searchMyJCheckBoxLeftPanel;
    public JCheckBox searchFavoriteJCheckBoxLeftPanel;
    private JSONObject settingsJSONbuf;
    /**
     * Creates new form Doma2
     */

    protected Controller cnt;

    public SplitPanel(String str) {
        super();

        logger = LoggerFactory.getLogger(getClass());

        cnt = Controller.getInstance();

        initComponents();
        search_Info_Panel = new JPanel();
        search_Info_Panel.setLayout(new BorderLayout());
        Label_search_Info_Panel = new JLabel();
        Label_search_Info_Panel.setHorizontalAlignment(SwingConstants.CENTER);
        search_Info_Panel.add(Label_search_Info_Panel, BorderLayout.CENTER);

        set_Divider_Parameters(str);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */

    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    @SuppressWarnings("rawtypes")
    private void initComponents() {

        settingsJSONbuf = new JSONObject();
        settingsJSONbuf = Settings.getInstance().Dump();

        GridBagConstraints gridBagConstraints;

        jSplitPanel = new MSplitPane(MSplitPane.VERTICAL_SPLIT, true);
        //      jSplitPanel.M_setDividerSize(20);
        leftPanel = new JPanel();
        toolBarLeftPanel = new JToolBar();
        button1ToolBarLeftPanel = new JButton();
        button2ToolBarLeftPanel = new JButton();
        searchToolBar_LeftPanel = new JToolBar();
        searthLabelSearchToolBarLeftPanel = new JLabel();
        searchTextFieldSearchToolBarLeftPanelDocument = new JTextField();
        jScrollPanelLeftPanel = new JScrollPane();

        rightPanel1 = new JPanel();
        jToolBarRightPanel = new JToolBar();
        jButton1_jToolBar_RightPanel = new JButton();
        jButton2_jToolBar_RightPanel = new JButton();
        jPanel_RightPanel = new JPanel();
        jScrollPaneJPanelRightPanel = new JScrollPane();
        jLabel2 = new JLabel();

        jSplitPanel.setBorder(null);

        leftPanel.setLayout(new GridBagLayout());

        toolBarLeftPanel.setFloatable(false);
        toolBarLeftPanel.setRollover(true);

        button1ToolBarLeftPanel.setText("jButton1");
        button1ToolBarLeftPanel.setFocusable(false);
        button1ToolBarLeftPanel.setHorizontalTextPosition(SwingConstants.CENTER);
        button1ToolBarLeftPanel.setVerticalTextPosition(SwingConstants.BOTTOM);
        button1ToolBarLeftPanel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                button1_ToolBar_LeftPanelActionPerformed(evt);
            }
        });
        toolBarLeftPanel.add(button1ToolBarLeftPanel);

        button2ToolBarLeftPanel.setText("jButton2");
        button2ToolBarLeftPanel.setFocusable(false);
        button2ToolBarLeftPanel.setHorizontalTextPosition(SwingConstants.CENTER);
        button2ToolBarLeftPanel.setVerticalTextPosition(SwingConstants.BOTTOM);
        toolBarLeftPanel.add(button2ToolBarLeftPanel);
        button2ToolBarLeftPanel.getAccessibleContext().setAccessibleDescription("");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new Insets(5, 8, 0, 0);
        leftPanel.add(toolBarLeftPanel, gridBagConstraints);

        searchToolBar_LeftPanel.setFloatable(false);
        searchToolBar_LeftPanel.setRollover(true);
        searchToolBar_LeftPanel.setVisible(false);


        searchMyJCheckBoxLeftPanel = new JCheckBox();
        searchMyJCheckBoxLeftPanel.setText(Lang.getInstance().translate("My") + " ");
        searchToolBar_LeftPanel.add(searchMyJCheckBoxLeftPanel);

        searchFavoriteJCheckBoxLeftPanel = new JCheckBox();
        searchFavoriteJCheckBoxLeftPanel.setText(Lang.getInstance().translate("Favorite") + " ");
        searchToolBar_LeftPanel.add(searchFavoriteJCheckBoxLeftPanel);


        searthLabelSearchToolBarLeftPanel.setText("    " + Lang.getInstance().translate("Search") + ":   ");
        searthLabelSearchToolBarLeftPanel.setToolTipText("");
        searchToolBar_LeftPanel.add(searthLabelSearchToolBarLeftPanel);

        searchTextFieldSearchToolBarLeftPanelDocument.setToolTipText("");
        searchTextFieldSearchToolBarLeftPanelDocument.setAlignmentX(1.0F);
        searchTextFieldSearchToolBarLeftPanelDocument.setMinimumSize(new Dimension(200, UIManager.getFont("Label.font").getSize()+ UIManager.getFont("Label.font").getSize()/2));
        searchTextFieldSearchToolBarLeftPanelDocument.setName(""); // NOI18N
        searchTextFieldSearchToolBarLeftPanelDocument.setPreferredSize(new Dimension(200, UIManager.getFont("Label.font").getSize()+ UIManager.getFont("Label.font").getSize()/2));
        searchToolBar_LeftPanel.add(searchTextFieldSearchToolBarLeftPanelDocument);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 0, 0, 10);
        leftPanel.add(searchToolBar_LeftPanel, gridBagConstraints);

        jScrollPanelLeftPanel.setBorder(BorderFactory.createEtchedBorder());

        jTableJScrollPanelLeftPanel = new MTable(new DefaultTableModel(
                new Object[][]{
                        {null, null, null, null},
                        {null, null, null, null},
                        {null, null, null, null},
                        {null, null, null, null}
                },
                new String[]{
                        "Title 1", "Title 2", "Title 3", "Title 4"
                }
        ));
        //      jScrollPanelLeftPanel.setViewportView(jTableJScrollPanelLeftPanel);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(1, 8, 8, 8);
        leftPanel.add(jScrollPanelLeftPanel, gridBagConstraints);
        jScrollPanelLeftPanel.setMinimumSize(new Dimension(0, 0));
        leftPanel.setMinimumSize(new Dimension(0, 0));
        jSplitPanel.setLeftComponent(leftPanel);

        rightPanel1.setMinimumSize(new Dimension(150, 0));
        rightPanel1.setName(""); // NOI18N
        rightPanel1.setLayout(new GridBagLayout());
        //  rightPanel1.setBackground(new Color(0,0,0));

        jToolBarRightPanel.setFloatable(false);
        jToolBarRightPanel.setRollover(true);

        jButton1_jToolBar_RightPanel.setText("jButton1");
        jButton1_jToolBar_RightPanel.setFocusable(false);
        jButton1_jToolBar_RightPanel.setHorizontalTextPosition(SwingConstants.CENTER);
        jButton1_jToolBar_RightPanel.setVerticalTextPosition(SwingConstants.BOTTOM);
        jToolBarRightPanel.add(jButton1_jToolBar_RightPanel);

        jButton2_jToolBar_RightPanel.setText("jButton2");
        jButton2_jToolBar_RightPanel.setToolTipText("");
        jButton2_jToolBar_RightPanel.setFocusable(false);
        jButton2_jToolBar_RightPanel.setHorizontalTextPosition(SwingConstants.CENTER);
        jButton2_jToolBar_RightPanel.setVerticalTextPosition(SwingConstants.BOTTOM);
        jToolBarRightPanel.add(jButton2_jToolBar_RightPanel);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 9, 0, 8);
        rightPanel1.add(jToolBarRightPanel, gridBagConstraints);

        jPanel_RightPanel.setAlignmentX(1.0F);
        jPanel_RightPanel.setAlignmentY(1.0F);

        jScrollPaneJPanelRightPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPaneJPanelRightPanel.setAlignmentX(1.0F);
        jScrollPaneJPanelRightPanel.setAlignmentY(1.0F);
        jScrollPaneJPanelRightPanel.setAutoscrolls(true);
        jScrollPaneJPanelRightPanel.setMinimumSize(new Dimension(0, 0));
        jScrollPaneJPanelRightPanel.setName(""); // NOI18N
        jScrollPaneJPanelRightPanel.setPreferredSize(new Dimension(0, 0));
        jScrollPaneJPanelRightPanel.setVerifyInputWhenFocusTarget(false);
        jScrollPaneJPanelRightPanel.setWheelScrollingEnabled(false);
        jScrollPaneJPanelRightPanel.setFocusable(false);

        jLabel2.setText(" ");
        jLabel2.setToolTipText("");
        jLabel2.setVerticalAlignment(SwingConstants.TOP);
        jLabel2.setBorder(BorderFactory.createEtchedBorder());
        jLabel2.setMaximumSize(new Dimension(0, 0));
        jLabel2.setMinimumSize(new Dimension(0, 0));
        jLabel2.setName(""); // NOI18N
        jScrollPaneJPanelRightPanel.setViewportView(jLabel2);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 8, 8, 8);
        rightPanel1.add(jScrollPaneJPanelRightPanel, gridBagConstraints);
        jScrollPaneJPanelRightPanel.setMinimumSize(new Dimension(0, 0));
        jScrollPaneJPanelRightPanel.setPreferredSize(new Dimension(350, 350));
        rightPanel1.setMinimumSize(new Dimension(0, 0));
        rightPanel1.setPreferredSize(new Dimension(350, 350));
        jSplitPanel.setRightComponent(rightPanel1);


        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jSplitPanel, GroupLayout.DEFAULT_SIZE, 748, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jSplitPanel, GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
        );

        jSplitPanel.setDividerLocation(0.3);

        this.jTableJScrollPanelLeftPanel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    }

    private void button1_ToolBar_LeftPanelActionPerformed(ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void set_Divider_Parameters(String str) {
        settingsJSONbuf = Settings.getInstance().getJSONObject();
    //    settingsJSONbuf = Settings.getInstance().read_setting_JSON();
        JSONObject params;
        params = new JSONObject();
        if (!settingsJSONbuf.containsKey("Main_Frame_Setting")) return;
        params = (JSONObject) settingsJSONbuf.get("Main_Frame_Setting");
        if (!params.containsKey(str)) return;
       // преобразуем все в ыекштп т.к. JSONObject в методе GET  преобразует <String>"2" -> <int>2
        HashMap param = (HashMap) params.get(str);
        if (param.containsKey("Div_Last_Loc"))
            jSplitPanel.setLastDividerLocation(new Integer(param.get("Div_Last_Loc")+""));
        if (param.containsKey("Div_Loc")) jSplitPanel.setDividerLocation(new Integer(param.get("Div_Loc")+""));
        int ii = new Integer(param.get("Div_Orientation")+"");
        if (param.containsKey("Div_Orientation")) jSplitPanel.setOrientation(ii);
        jSplitPanel.set_button_title();

    }

    public void onClose() {
    }
}