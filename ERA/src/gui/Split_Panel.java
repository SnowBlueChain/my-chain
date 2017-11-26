/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import java.awt.Dimension;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.json.simple.JSONObject;
import gui.library.MSplitPane;
import gui.library.MTable;
import lang.Lang;
import settings.Settings;


/**
 *
 * @author Саша
 */
public class Split_Panel extends javax.swing.JPanel {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JSONObject settingsJSONbuf;
	public JPanel search_Info_Panel;
	public JLabel Label_search_Info_Panel;
	/**
     * Creates new form Doma2
     */
	
    public Split_Panel(String str) {
    	super();
    
        initComponents();
        search_Info_Panel = new JPanel();
	     search_Info_Panel.setLayout(new java.awt.BorderLayout());
	     Label_search_Info_Panel = new javax.swing.JLabel();
	     Label_search_Info_Panel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	     search_Info_Panel.add(Label_search_Info_Panel, java.awt.BorderLayout.CENTER);
        
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
    	
        java.awt.GridBagConstraints gridBagConstraints;

        jSplitPanel = new MSplitPane(MSplitPane.VERTICAL_SPLIT, true);
  //      jSplitPanel.M_setDividerSize(20);
        leftPanel = new javax.swing.JPanel();
        toolBar_LeftPanel = new javax.swing.JToolBar();
        button1_ToolBar_LeftPanel = new javax.swing.JButton();
        button2_ToolBar_LeftPanel = new javax.swing.JButton();
        searchToolBar_LeftPanel = new javax.swing.JToolBar();
        searthLabel_SearchToolBar_LeftPanel = new javax.swing.JLabel();
        searchTextField_SearchToolBar_LeftPanel = new javax.swing.JTextField();
        jScrollPanel_LeftPanel = new javax.swing.JScrollPane();
        
        rightPanel1 = new javax.swing.JPanel();
        jToolBar_RightPanel = new javax.swing.JToolBar();
        jButton1_jToolBar_RightPanel = new javax.swing.JButton();
        jButton2_jToolBar_RightPanel = new javax.swing.JButton();
        jPanel_RightPanel = new javax.swing.JPanel();
        jScrollPane_jPanel_RightPanel = new javax.swing.JScrollPane();
        jLabel2 = new javax.swing.JLabel();

        jSplitPanel.setBorder(null);

        leftPanel.setLayout(new java.awt.GridBagLayout());

        toolBar_LeftPanel.setFloatable(false);
        toolBar_LeftPanel.setRollover(true);

        button1_ToolBar_LeftPanel.setText("jButton1");
        button1_ToolBar_LeftPanel.setFocusable(false);
        button1_ToolBar_LeftPanel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        button1_ToolBar_LeftPanel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        button1_ToolBar_LeftPanel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button1_ToolBar_LeftPanelActionPerformed(evt);
            }
        });
        toolBar_LeftPanel.add(button1_ToolBar_LeftPanel);

        button2_ToolBar_LeftPanel.setText("jButton2");
        button2_ToolBar_LeftPanel.setFocusable(false);
        button2_ToolBar_LeftPanel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        button2_ToolBar_LeftPanel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolBar_LeftPanel.add(button2_ToolBar_LeftPanel);
        button2_ToolBar_LeftPanel.getAccessibleContext().setAccessibleDescription("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(5, 8, 0, 0);
        leftPanel.add(toolBar_LeftPanel, gridBagConstraints);

        searchToolBar_LeftPanel.setFloatable(false);
        searchToolBar_LeftPanel.setRollover(true);
        searchToolBar_LeftPanel.setVisible(false); 
        
        
        searth_My_JCheckBox_LeftPanel = new JCheckBox();
        searth_My_JCheckBox_LeftPanel.setText(Lang.getInstance().translate("My")+" ");
        searchToolBar_LeftPanel.add(searth_My_JCheckBox_LeftPanel);
        
        searth_Favorite_JCheckBox_LeftPanel = new JCheckBox();
        searth_Favorite_JCheckBox_LeftPanel.setText(Lang.getInstance().translate("Favorite")+" ");
        searchToolBar_LeftPanel.add(searth_Favorite_JCheckBox_LeftPanel);
        
        
        
        searthLabel_SearchToolBar_LeftPanel.setText("    "+ Lang.getInstance().translate("Search")+":   ");
        searthLabel_SearchToolBar_LeftPanel.setToolTipText("");
        searchToolBar_LeftPanel.add(searthLabel_SearchToolBar_LeftPanel);

        searchTextField_SearchToolBar_LeftPanel.setToolTipText("");
        searchTextField_SearchToolBar_LeftPanel.setAlignmentX(1.0F);
        searchTextField_SearchToolBar_LeftPanel.setMinimumSize(new java.awt.Dimension(200, 20));
        searchTextField_SearchToolBar_LeftPanel.setName(""); // NOI18N
        searchTextField_SearchToolBar_LeftPanel.setPreferredSize(new java.awt.Dimension(200, 20));
        searchToolBar_LeftPanel.add(searchTextField_SearchToolBar_LeftPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 10);
        leftPanel.add(searchToolBar_LeftPanel, gridBagConstraints);

        jScrollPanel_LeftPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jTable_jScrollPanel_LeftPanel = new MTable(new javax.swing.table.DefaultTableModel(
                new Object [][] {
                    {null, null, null, null},
                    {null, null, null, null},
                    {null, null, null, null},
                    {null, null, null, null}
                },
                new String [] {
                    "Title 1", "Title 2", "Title 3", "Title 4"
                }
            ));
          //      jScrollPanel_LeftPanel.setViewportView(jTable_jScrollPanel_LeftPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 8, 8, 8);
        leftPanel.add(jScrollPanel_LeftPanel, gridBagConstraints);
        jScrollPanel_LeftPanel.setMinimumSize(new Dimension(0,0));
        leftPanel.setMinimumSize(new Dimension(0,0));
        jSplitPanel.setLeftComponent(leftPanel);

        rightPanel1.setMinimumSize(new java.awt.Dimension(150, 0));
        rightPanel1.setName(""); // NOI18N
        rightPanel1.setLayout(new java.awt.GridBagLayout());
      //  rightPanel1.setBackground(new Color(0,0,0));

        jToolBar_RightPanel.setFloatable(false);
        jToolBar_RightPanel.setRollover(true);

        jButton1_jToolBar_RightPanel.setText("jButton1");
        jButton1_jToolBar_RightPanel.setFocusable(false);
        jButton1_jToolBar_RightPanel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1_jToolBar_RightPanel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar_RightPanel.add(jButton1_jToolBar_RightPanel);

        jButton2_jToolBar_RightPanel.setText("jButton2");
        jButton2_jToolBar_RightPanel.setToolTipText("");
        jButton2_jToolBar_RightPanel.setFocusable(false);
        jButton2_jToolBar_RightPanel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton2_jToolBar_RightPanel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToolBar_RightPanel.add(jButton2_jToolBar_RightPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 9, 0, 8);
        rightPanel1.add(jToolBar_RightPanel, gridBagConstraints);

        jPanel_RightPanel.setAlignmentX(1.0F);
        jPanel_RightPanel.setAlignmentY(1.0F);

        jScrollPane_jPanel_RightPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane_jPanel_RightPanel.setAlignmentX(1.0F);
        jScrollPane_jPanel_RightPanel.setAlignmentY(1.0F);
        jScrollPane_jPanel_RightPanel.setAutoscrolls(true);
        jScrollPane_jPanel_RightPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        jScrollPane_jPanel_RightPanel.setName(""); // NOI18N
        jScrollPane_jPanel_RightPanel.setPreferredSize(new java.awt.Dimension(0, 0));
        jScrollPane_jPanel_RightPanel.setVerifyInputWhenFocusTarget(false);
        jScrollPane_jPanel_RightPanel.setWheelScrollingEnabled(false);
        jScrollPane_jPanel_RightPanel.setFocusable(false);

        jLabel2.setText(" ");
        jLabel2.setToolTipText("");
        jLabel2.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jLabel2.setMaximumSize(new java.awt.Dimension(0, 0));
        jLabel2.setMinimumSize(new java.awt.Dimension(0, 0));
        jLabel2.setName(""); // NOI18N
        jScrollPane_jPanel_RightPanel.setViewportView(jLabel2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 8, 8);
        rightPanel1.add(jScrollPane_jPanel_RightPanel, gridBagConstraints);
        jScrollPane_jPanel_RightPanel.setMinimumSize(new Dimension(0,0));
        jScrollPane_jPanel_RightPanel.setPreferredSize(new Dimension(350,350));
        rightPanel1.setMinimumSize(new Dimension(0,0));
        rightPanel1.setPreferredSize(new Dimension(350,350));
        jSplitPanel.setRightComponent(rightPanel1);
        
        
        
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 748, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 274, Short.MAX_VALUE)
        );
        
        jSplitPanel.setDividerLocation(0.3);
        
        this.jTable_jScrollPanel_LeftPanel.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      
    }                   

    private void button1_ToolBar_LeftPanelActionPerformed(java.awt.event.ActionEvent evt) {                                                          
        // TODO add your handling code here:
    }                                                         


    private void set_Divider_Parameters(String str){
    	settingsJSONbuf = Settings.getInstance().read_setting_JSON();
    	JSONObject params;
		if (!settingsJSONbuf.containsKey("Main_Frame_Setting")) return;
    	 params = (JSONObject) settingsJSONbuf.get("Main_Frame_Setting");
    	if (!params.containsKey(str)) return;
    	JSONObject param = (JSONObject) params.get(str);
    	if (param.containsKey("Div_Last_Loc")) jSplitPanel.setLastDividerLocation(new Integer((String) param.get("Div_Last_Loc")));
    	if (param.containsKey("Div_Loc")) jSplitPanel.setDividerLocation(new Integer((String)param.get("Div_Loc")));
    	int ii = new Integer((String)param.get("Div_Orientation"));
    	if (param.containsKey("Div_Orientation"))jSplitPanel.setOrientation(ii);
    	jSplitPanel.set_button_title();
    	 
    }
    
    
    public void delay_on_close(){
    }
    
    // Variables declaration - do not modify                     
    public javax.swing.JButton button1_ToolBar_LeftPanel;
    public javax.swing.JButton button2_ToolBar_LeftPanel;
    public javax.swing.JButton jButton1_jToolBar_RightPanel;
    public javax.swing.JButton jButton2_jToolBar_RightPanel;
    public javax.swing.JLabel jLabel2;
    public javax.swing.JPanel jPanel_RightPanel;
    public javax.swing.JScrollPane jScrollPane_jPanel_RightPanel;
    public javax.swing.JScrollPane jScrollPanel_LeftPanel;
    public MSplitPane jSplitPanel;
    @SuppressWarnings("rawtypes")
	public MTable jTable_jScrollPanel_LeftPanel;
    public javax.swing.JToolBar jToolBar_RightPanel;
    public javax.swing.JPanel leftPanel;
    public javax.swing.JPanel rightPanel1;
    public javax.swing.JTextField searchTextField_SearchToolBar_LeftPanel;
    public javax.swing.JToolBar searchToolBar_LeftPanel;
    public javax.swing.JLabel searthLabel_SearchToolBar_LeftPanel;
    public javax.swing.JToolBar toolBar_LeftPanel;
    public JCheckBox searth_My_JCheckBox_LeftPanel;
    public JCheckBox searth_Favorite_JCheckBox_LeftPanel;
    // End of variables declaration                   
}

