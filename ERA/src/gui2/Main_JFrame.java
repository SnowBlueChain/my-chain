package gui2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import gui.MainFrame;
import gui.status.StatusPanel;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ����
 */
public class Main_JFrame extends javax.swing.JFrame {

    
	private static Main_JFrame instance;
	public static Main_JFrame getInstance()
	{
		if(instance == null)
		{
			instance = new Main_JFrame();
		}
		
		return instance;
	}


	
	/**
     * Creates new form ffff
     */
   private Main_JFrame() {
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

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new Main_Panel();
        jPanel2 =  new StatusPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        
        

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    //    getContentPane().setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
      //  getContentPane().add(jTabbedPane1, gridBagConstraints);

        add(jTabbedPane1, BorderLayout.NORTH);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.weighty = 0.2;
  //      getContentPane().add(jPanel1, gridBagConstraints);
        
        add(jPanel1, BorderLayout.CENTER);
        
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.2;
   //     getContentPane().add(jPanel2, gridBagConstraints);
        this.add(new StatusPanel(), BorderLayout.SOUTH);

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        pack();
        Toolkit kit = Toolkit.getDefaultToolkit();

        Dimension screens = kit.getScreenSize();

        int w;

        w = screens.width;

        setSize((int) (w/1.3),(int) (w/1.3/1.618));

        setLocation(w/12, w/12);
        setExtendedState(MAXIMIZED_BOTH);
    }// </editor-fold>                        

   
    // Variables declaration - do not modify                     
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private Main_Panel jPanel1;
    private StatusPanel jPanel2;
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration                   
}
