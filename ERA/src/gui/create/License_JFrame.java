package gui.create;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JTextPane;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.mapdb.Fun.Tuple2;
import org.mapdb.Fun.Tuple3;

import com.github.rjeschke.txtmark.Processor;

import controller.Controller;
import core.transaction.R_SignNote;
import datachain.DCSet;
import lang.Lang;
import settings.Settings;

public class License_JFrame extends JDialog {

    static Logger LOGGER = Logger.getLogger(License_JFrame.class.getName());
    boolean needAccept;
    NoWalletFrame parent;
    int goCreateWallet;
    String license;

    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    //private javax.swing.JTextArea jTextArea1;
    private JTextPane messageText;
    
    public License_JFrame(boolean needAccept, NoWalletFrame parent, int goCreateWallet) {
	
        this.needAccept = needAccept;
        this.parent = parent;
        this.goCreateWallet = goCreateWallet;

        this.license = "<html>" + Processor.process(getLicenseText())
        		+ "</html>";
        
        initComponents();
    }
    
    public License_JFrame() {
        this.license = "<html>" + Processor.process(getLicenseText())
        		+ "</html>";

        needAccept = false;
        initComponents();
    }

    public String getLicenseText() {
        try {
            Tuple2<Integer, Integer> langRef = Controller.LICENSE_LANG_REFS.get(Settings.getInstance().getLang());
            if (langRef == null)
                langRef = Controller.LICENSE_LANG_REFS.get("en");
            
            R_SignNote record = (R_SignNote) DCSet.getInstance().getTransactionFinalMap().get(langRef);
            
            String message;
            if (record.getVersion() == 2) {
                Tuple3<String, String, JSONObject> a = record.parse_Data_V2_Without_Files();
                message = (String)a.c.get("MS");
                
            } else {
    
                try {
                    JSONObject data = (JSONObject) JSONValue
                            .parseWithException(new String(record.getData(), Charset.forName("UTF-8")));
                    message = (String) data.get("Message");
                } catch (Exception e) {
            	message = new String(record.getData(), Charset.forName("UTF-8"));
                }
            }

            return message; // Processor.process(
            
        } catch (Exception e1) {
            // USE default LICENSE
            return DCSet.getInstance().getItemTemplateMap().get(2l).getDescription();
            
        }
	
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        List<Image> icons = new ArrayList<Image>();
        icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon16.png"));
        icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32.png"));
        icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon64.png"));
        icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon128.png"));
        this.setIconImages(icons);
        this.setModal(true);

        java.awt.GridBagConstraints gridBagConstraints;

        jCheckBox1 = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        //jTextArea1 = new javax.swing.JTextArea();
        
        messageText = new JTextPane();
        messageText.setContentType("text/html");


        jLabel1 = new javax.swing.JLabel();

        //      setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(Lang.getInstance().translate("License"));
        setMinimumSize(new java.awt.Dimension(800, 550));
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jCheckBox1.setText(Lang.getInstance().translate("I accept"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 8, 6, 0);
        if (needAccept)
            getContentPane().add(jCheckBox1, gridBagConstraints);

        jCheckBox1.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent arg0) {
                // TODO Auto-generated method stub
                jButton1.setEnabled(!jButton1.isEnabled());
            }
        });


        jButton1.setEnabled(false);
        jButton1.setText(Lang.getInstance().translate("Next"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 8, 0);

        if (needAccept)
            getContentPane().add(jButton1, gridBagConstraints);

        jButton1.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub
                setVisible(false);

                if (parent != null)
                    parent.goAfterLicence(goCreateWallet);
            }

        });

        jButton2.setText(Lang.getInstance().translate(parent == null ? "Not Accept" : "Back"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 8, 8);
        if (needAccept)
            getContentPane().add(jButton2, gridBagConstraints);


        jButton2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub

                if (parent != null) {
                    parent.setVisible(true);
                    dispose();
                } else {
                    Controller.getInstance().stopAll(0);
                    //      	System.exit(0);
                }
            }
        });
    /*

      jTextArea1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	if (!needAccept){

            	setVisible(false);
                dispose();
            	}
            }
        });

      jTextArea1.addKeyListener(new KeyAdapter() {
		    public void keyPressed(KeyEvent e) {
		    	if (!needAccept){

		    	setVisible(false);
                dispose();
		    	}
		    }
		});

     */


        //CLOSE NICELY
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {

                if (!needAccept)
                    return;

                Controller.getInstance().stopAll(0);
                // 	System.exit(0);

            }
        });

        // jTextArea1.setColumns(20);
        //jTextArea1.setLineWrap(true);
        //jTextArea1.setEditable(false);
        //jT/extArea1.setRows(5);
        //jTextArea1.setText(this.license);
        //jScrollPane1.setViewportView(jTextArea1);

        messageText.setText(this.license);
        jScrollPane1.setViewportView(messageText);

 /*
        this.jTextArea1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	if(parent != null) return;
            	setVisible(false);
                dispose();
            }
        });

        this.addKeyListener(new KeyAdapter() {
		    public void keyPressed(KeyEvent e) {
		    	if(parent != null) return;
		    	setVisible(false);
                dispose();
		    }
		});

   */

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.2;
        gridBagConstraints.weighty = 0.2;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        getContentPane().add(jScrollPane1, gridBagConstraints);

        jLabel1.setText(Lang.getInstance().translate("Read carefully") + "!");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 0, 8);
        if (needAccept)
            getContentPane().add(jLabel1, gridBagConstraints);


        //     this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        //      if(!needAccept)
        this.setUndecorated(false);
        if (needAccept) this.setUndecorated(true);

        pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }// </editor-fold>
    // End of variables declaration                   


}
