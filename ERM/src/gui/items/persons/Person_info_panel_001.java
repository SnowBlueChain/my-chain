package gui.items.persons;

import java.awt.Component;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.mapdb.Fun.Tuple3;

import core.item.persons.PersonCls;
import database.DBSet;
import gui.models.PersonAccountsModel;
import gui.models.PersonStatusesModel;
import gui.models.Renderer_Boolean;
import gui.models.Renderer_Left;
import lang.Lang;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Саша
 */
public class Person_info_panel_001 extends javax.swing.JPanel {

    private static final Object[] String = null;
	/**
     * Creates new form person_info
     */
    public Person_info_panel_001(PersonCls person) {
        initComponents(person);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents(PersonCls person) {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabel1 = new javax.swing.JLabel();
        name_jTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        gender_jTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        birthday_jTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        deathday_jTextField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        race_jTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        birth_Latitude_jTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        birth_Longitude_jTextField = new javax.swing.JTextField();
        Skin_Color_jLabel = new javax.swing.JLabel();
        skin_Color_jTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        eye_Color_jTextField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        hair_Сolor_jTextField = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        height_jTextField = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jTextField13 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel15 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        description_jTextPane = new javax.swing.JTextPane();
        key_jLabel = new javax.swing.JLabel();
        key_jTextField = new javax.swing.JTextField();

        
        SimpleDateFormat formatDate = new SimpleDateFormat("dd.MM.yyyy"); // HH:mm");
        
        
        
        setMaximumSize(new java.awt.Dimension(400, 300));
        setName(""); // NOI18N
        setPreferredSize(new java.awt.Dimension(454, 500));
        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.columnWidths = new int[] {0, 7, 0};
        layout.rowHeights = new int[] {0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0, 2, 0};
        setLayout(layout);

        jLabel1.setText(Lang.getInstance().translate("Name")+":");
        jLabel1.setAlignmentY(0.2F);
        jLabel1.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel1, gridBagConstraints);

        name_jTextField.setEditable(false);
        name_jTextField.setText(person.getName());
        name_jTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                name_jTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(name_jTextField, gridBagConstraints);

        jLabel2.setText(Lang.getInstance().translate("Description")+":");
        jLabel2.setAlignmentY(0.2F);
        jLabel2.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel2, gridBagConstraints);

        jLabel3.setText(Lang.getInstance().translate("Gender")+":");
        jLabel3.setAlignmentY(0.2F);
        jLabel3.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel3, gridBagConstraints);

        gender_jTextField.setEditable(false);
        gender_jTextField.setText(person.getGender()+"");
        gender_jTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gender_jTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(gender_jTextField, gridBagConstraints);

        jLabel4.setText(Lang.getInstance().translate("Birthday")+":");
        jLabel4.setAlignmentY(0.2F);
        jLabel4.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel4, gridBagConstraints);

        birthday_jTextField.setEditable(false);
        birthday_jTextField.setText(new Date (person.getBirthday())+"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(birthday_jTextField, gridBagConstraints);

        jLabel5.setText(Lang.getInstance().translate("Deathday")+":");
        jLabel5.setAlignmentY(0.2F);
        jLabel5.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel5, gridBagConstraints);

        deathday_jTextField.setEditable(false);
        Long end = person.getDeathday();
        if (end == null || end <= person.getBirthday())
        	deathday_jTextField.setText( "-");
        else
        	deathday_jTextField.setText( new Date (end)+"");
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(deathday_jTextField, gridBagConstraints);

        jLabel6.setText(Lang.getInstance().translate("Race")+":");
        jLabel6.setAlignmentY(0.2F);
        jLabel6.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel6, gridBagConstraints);

        race_jTextField.setEditable(false);
        race_jTextField.setText(person.getRace()+"");
        race_jTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                race_jTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(race_jTextField, gridBagConstraints);

        jLabel7.setText(Lang.getInstance().translate("Birth Latitude")+":");
        jLabel7.setAlignmentY(0.2F);
        jLabel7.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel7, gridBagConstraints);

        birth_Latitude_jTextField.setEditable(false);
        birth_Latitude_jTextField.setText(person.getBirthLatitude()+"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(birth_Latitude_jTextField, gridBagConstraints);

        jLabel8.setText(Lang.getInstance().translate("Birth Longitude")+":");
        jLabel8.setAlignmentY(0.2F);
        jLabel8.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel8, gridBagConstraints);

        birth_Longitude_jTextField.setEditable(false);
        birth_Longitude_jTextField.setText(person.getBirthLongitude()+"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(birth_Longitude_jTextField, gridBagConstraints);

        Skin_Color_jLabel.setText(Lang.getInstance().translate("Skin Color")+":");
        Skin_Color_jLabel.setAlignmentY(0.2F);
        Skin_Color_jLabel.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(Skin_Color_jLabel, gridBagConstraints);

        skin_Color_jTextField.setEditable(false);
        skin_Color_jTextField.setText(person.getSkinColor()+"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(skin_Color_jTextField, gridBagConstraints);

        jLabel10.setText(Lang.getInstance().translate("Eye Color")+":");
        jLabel10.setAlignmentY(0.2F);
        jLabel10.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel10, gridBagConstraints);

        eye_Color_jTextField.setEditable(false);
        eye_Color_jTextField.setText(person.getEyeColor()+"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(eye_Color_jTextField, gridBagConstraints);

        jLabel11.setText(Lang.getInstance().translate("Hair Сolor")+":");
        jLabel11.setAlignmentY(0.2F);
        jLabel11.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel11, gridBagConstraints);

        hair_Сolor_jTextField.setEditable(false);
        hair_Сolor_jTextField.setText(person.getHairСolor()+"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(hair_Сolor_jTextField, gridBagConstraints);

        jLabel12.setText(Lang.getInstance().translate("Height")+":");
        jLabel12.setAlignmentY(0.2F);
        jLabel12.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel12, gridBagConstraints);

        height_jTextField.setEditable(false);
        height_jTextField.setText(person.getHeight()+"");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(height_jTextField, gridBagConstraints);

        jLabel13.setText(Lang.getInstance().translate("Creator")+":");
        jLabel13.setAlignmentY(0.2F);
        jLabel13.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 1);
        add(jLabel13, gridBagConstraints);

        jTextField13.setEditable(false);
        jTextField13.setText(person.getCreator().toString());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(jTextField13, gridBagConstraints);

        jLabel14.setText(Lang.getInstance().translate("Statuses"));
        jLabel14.setAlignmentY(0.2F);
        jLabel14.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        add(jLabel14, gridBagConstraints);

        PersonStatusesModel statusModel = new PersonStatusesModel (person.getKey());
        jTable1.setModel(statusModel);
        
        jTable1.setDefaultRenderer(String.class, new Renderer_Left()); // set renderer
        //CHECKBOX FOR FAVORITE
        		TableColumn to_Date_Column1 = jTable1.getColumnModel().getColumn( PersonStatusesModel.COLUMN_TO_DATE);	
        		//favoriteColumn.setCellRenderer(new Renderer_Boolean()); //personsTable.getDefaultRenderer(Boolean.class));
        		to_Date_Column1.setMinWidth(80);
        		to_Date_Column1.setMaxWidth(200);
        		to_Date_Column1.setPreferredWidth(120);//.setWidth(30);
        
       // jTable1.setPreferredSize(new java.awt.Dimension(100, 64));
        jScrollPane1.setViewportView(jTable1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 30;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        add(jScrollPane1, gridBagConstraints);

        jLabel15.setText(Lang.getInstance().translate("Accounts"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 32;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        add(jLabel15, gridBagConstraints);

        
        
        
     // GET CERTIFIED ACCOUNTS
        
     	
        
        
        
        PersonAccountsModel personModel = new PersonAccountsModel(person.getKey());
       
        
        jTable2.setModel(personModel);
        
        jTable2.setDefaultRenderer(String.class, new Renderer_Left()); // set renderer
      //CHECKBOX FOR FAVORITE
      		TableColumn to_Date_Column = jTable2.getColumnModel().getColumn( PersonAccountsModel.COLUMN_TO_DATE);	
      		//favoriteColumn.setCellRenderer(new Renderer_Boolean()); //personsTable.getDefaultRenderer(Boolean.class));
      		to_Date_Column.setMinWidth(50);
      		to_Date_Column.setMaxWidth(200);
      		to_Date_Column.setPreferredWidth(80);//.setWidth(30);
        
        
        
        /*
        jTable2.setModel(new javax.swing.table.DefaultTableModel(
        		table_addresses,
            title
        ));
        */
      //  jTable2.setPreferredSize(new java.awt.Dimension(100, 64));
        jScrollPane2.setViewportView(jTable2);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 34;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 8, 10);
        add(jScrollPane2, gridBagConstraints);

        description_jTextPane.setEditable(false);
        description_jTextPane.setText(person.getDescription());
        jScrollPane3.setViewportView(description_jTextPane);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.3;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        add(jScrollPane3, gridBagConstraints);

        key_jLabel.setText(Lang.getInstance().translate("Key")+":");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(8, 10, 0, 0);
        add(key_jLabel, gridBagConstraints);

        key_jTextField.setEditable(false);
        key_jTextField.setText(person.getKey()+"");
        key_jTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                key_jTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 6);
        add(key_jTextField, gridBagConstraints);
    }// </editor-fold>                        

    private void name_jTextFieldActionPerformed(java.awt.event.ActionEvent evt) {                                                
        // TODO add your handling code here:
    }                                               

    private void gender_jTextFieldActionPerformed(java.awt.event.ActionEvent evt) {                                                  
        // TODO add your handling code here:
    }                                                 

    private void race_jTextFieldActionPerformed(java.awt.event.ActionEvent evt) {                                                
        // TODO add your handling code here:
    }                                               

    private void key_jTextFieldActionPerformed(java.awt.event.ActionEvent evt) {                                               
        // TODO add your handling code here:
    }                                              


    // Variables declaration - do not modify                     
    private javax.swing.JLabel Skin_Color_jLabel;
    private javax.swing.JTextField birth_Latitude_jTextField;
    private javax.swing.JTextField birth_Longitude_jTextField;
    private javax.swing.JTextField birthday_jTextField;
    private javax.swing.JTextField deathday_jTextField;
    private javax.swing.JTextPane description_jTextPane;
    private javax.swing.JTextField eye_Color_jTextField;
    private javax.swing.JTextField gender_jTextField;
    private javax.swing.JTextField hair_Сolor_jTextField;
    private javax.swing.JTextField height_jTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField jTextField13;
    private javax.swing.JLabel key_jLabel;
    private javax.swing.JTextField key_jTextField;
    private javax.swing.JTextField name_jTextField;
    private javax.swing.JTextField race_jTextField;
    private javax.swing.JTextField skin_Color_jTextField;
    // End of variables declaration                   
}
