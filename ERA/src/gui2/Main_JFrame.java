package gui2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.json.simple.JSONObject;

import controller.Controller;
import gui.ClosingDialog;
import gui.MainFrame;
import gui.Split_Panel;
import gui.library.Menu_Deals;
import gui.library.Menu_Files;
import gui.status.StatusPanel;
import lang.Lang;
import settings.Settings;
import utils.ObserverMessage;
import utils.SaveStrToFile;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ����
 */
public class Main_JFrame extends javax.swing.JFrame implements Observer{
	private JSONObject settingsJSONbuf;

	private JSONObject main_Frame_settingsJSON;
    
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
	   
	 //CREATE FRAME
	 		super(controller.Controller.APP_NAME +  " v." + Controller.getVersion());
	 		this.setVisible(false);
	 		if(Settings.getInstance().isTestnet()) {
	 			setTitle(controller.Controller.APP_NAME + " TestNet "
	 					 +  "v." + Controller.getVersion()
	 					 + " TS:" + Settings.getInstance().getGenesisStamp());
	 		}
	 		Controller.getInstance().addObserver(this);		
	 	//read settings	
	 		settingsJSONbuf = new JSONObject();
	 		settingsJSONbuf = Settings.getInstance().Dump();
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
        mainPanel = new Main_Panel();
        statusPanel =  new StatusPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new Menu_Files();
        jMenu2 = new Menu_Deals();
    
      
	

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
        
        add(mainPanel, BorderLayout.CENTER);
        
        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(jPanel2Layout);
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

        jMenu1.setText(Lang.getInstance().translate("File"));
        jMenuBar1.add(jMenu1);

        jMenu2.setText(Lang.getInstance().translate("Deals"));
        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        //CLOSE NICELY
        this.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
            	new ClosingDialog();
            }
        });
        
        addWindowListener(new WindowListener(){

			@Override
			public void windowActivated(WindowEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				// TODO Auto-generated method stub
			
				
			}

			@Override
			public void windowClosing(WindowEvent arg0) {
				// TODO Auto-generated method stub
				// read settings
				int lDiv;
				int div; 
				Split_Panel sP;
				HashMap outOpenTabbeds = new HashMap();
				JSONObject settingsJSON = new JSONObject();
				settingsJSON.put("Main_Frame_Height", getHeight()+""); // высота
				settingsJSON.put("Main_Frame_Width", getWidth()+""); // длина
				settingsJSON.put("Main_Frame_Loc_X", getX()+""); // высота
				settingsJSON.put("Main_Frame_Loc_Y", getY()+""); // высота
				
				settingsJSON.put("Main_Frame_Div_Orientation", mainPanel.jSplitPane1.getOrientation()+"");
				// horisontal - vertical orientation
				lDiv = mainPanel.jSplitPane1.getLastDividerLocation();
				div =  mainPanel.jSplitPane1.getDividerLocation();
				
				settingsJSON.put("Main_Frame_Div_Last_Loc",lDiv +"" );
				settingsJSON.put("Main_Frame_Div_Loc", div + "");
				Component[] Tabbed_Comps = mainPanel.jTabbedPane1.getComponents();
				for (int i =0; i< Tabbed_Comps.length;i++){
					// write in setting opet tabbs
					outOpenTabbeds.put(mainPanel.jTabbedPane1.getComponent(i).getClass().getSimpleName(),i);
					
					// write open tabbed settings Split panel
					if (mainPanel.jTabbedPane1.getComponent(i) instanceof Split_Panel){
						HashMap outTabbedDiv = new HashMap();
						
						 sP = ((Split_Panel)mainPanel.jTabbedPane1.getComponent(i));
						outTabbedDiv.put("Div_Orientation",sP.jSplitPanel.getOrientation());
						
						// write
						
							lDiv = sP.jSplitPanel.getLastDividerLocation();
							div =  sP.jSplitPanel.getDividerLocation();
							
						outTabbedDiv.put("Div_Last_Loc",lDiv +"" );
						outTabbedDiv.put("Div_Loc", div + "");
						
						settingsJSON.put(mainPanel.jTabbedPane1.getComponent(i).getClass().getSimpleName(),outTabbedDiv);
					}
					settingsJSON.remove("OpenTabbeds");
					settingsJSON.put("OpenTabbeds", outOpenTabbeds)	;
					settingsJSONbuf.put("Main_Frame_Setting", settingsJSON);	
					
				}
				// save setting to setting file
				try {
					SaveStrToFile.saveJsonFine(Settings.getInstance().getSettingsPath(), settingsJSONbuf);			
				} catch (IOException e) {
					JOptionPane.showMessageDialog(
							new JFrame(), "Error writing to the file: " + Settings.getInstance().getSettingsPath()
									+ "\nProbably there is no access.",
			                "Error!",
			                JOptionPane.ERROR_MESSAGE);
				}	
				
				new ClosingDialog();
					
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowIconified(WindowEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowOpened(WindowEvent arg0) {
				// TODO Auto-generated method stub
			}
        	
        	
        	
        });
        
        
        
        
        
        pack();
        // set perameters size $ split panel
		int devLastLoc;
		int devLoc;
		
		if(settingsJSONbuf.containsKey("Main_Frame_Setting")){
			main_Frame_settingsJSON = new JSONObject();
			main_Frame_settingsJSON = (JSONObject) settingsJSONbuf.get("Main_Frame_Setting");
		
		int x =new Integer((String)	main_Frame_settingsJSON.get("Main_Frame_Loc_X")); // x
		int y =new Integer((String)	main_Frame_settingsJSON.get("Main_Frame_Loc_Y")); // y
		setLocation(x, y);
		int h =new Integer((String)	main_Frame_settingsJSON.get("Main_Frame_Height")); // высота
		int w =new Integer((String)	main_Frame_settingsJSON.get("Main_Frame_Width")); // длина
			setSize(w, h);
			mainPanel.jSplitPane1.setOrientation(new Integer((String)main_Frame_settingsJSON.get("Main_Frame_Div_Orientation")));
		
		
		
			
			devLoc = new Integer((String)main_Frame_settingsJSON.get("Main_Frame_Div_Loc"));
			devLastLoc = new Integer((String)main_Frame_settingsJSON.get("Main_Frame_Div_Last_Loc"));
		 
			
	    	mainPanel.jSplitPane1.setLastDividerLocation(devLastLoc);
			mainPanel.jSplitPane1.setDividerLocation(devLoc);
			mainPanel.jSplitPane1.set_button_title(); // set title diveders buttons
			
		} else{
			setExtendedState(MAXIMIZED_BOTH);
	//		mainPanel.jSplitPane1.setDividerLocation(250);
			mainPanel.jSplitPane1.setLastDividerLocation(300);
			
		}
    	
     // load tabs
		if (main_Frame_settingsJSON.containsKey("OpenTabbeds")){
			
		JSONObject openTabes = (JSONObject) main_Frame_settingsJSON.get("OpenTabbeds");
			if (openTabes.containsKey("My_Accounts_SplitPanel")){
				mainPanel.dylay("My Accounts");	
			}
			
		}
        
		/*		
			      "3":"My_Accounts_SplitPanel",
			      "4":"Persons_My_SplitPanel",
			      "5":"Persons_Search_SplitPanel",
			      "6":"IssuePersonPanel",
			      "7":"Statements_My_SplitPanel",
			      "8":"Statements_Search_SplitPanel",
			      "9":"Issue_Document_Panel",
			      "10":"Incoming_Mails_SplitPanel",
			      "11":"Outcoming_Mails_SplitPanel",
			      "12":"Mail_Send_Panel",
			      "13":"My_Assets_Tab",
			      "14":"Search_Assets_Tab",
			      "15":"IssueAssetPanel",
			      "16":"My_Order_Tab",
			      "17":"My_Balance_Tab",
			      "18":"Search_Notes_Tab",
			      "19":"My_Statuses_Tab",
			      "20":"My_Unions_Tab",
			      "21":"Search_Union_Tab",
			      "22":"IssueUnionPanel",
			      "23":"Votings_My_SplitPanel",
			      "24":"Votings_Search_SplitPanel",
			      "25":"Create_Voting_Panel",
			      "26":"Records_My_SplitPanel",
			      "27":"Records_Search_SplitPanel",
			      "28":"Records_UnConfirmed_Panel",
			      "29":"other_Panel"
			  */ 
   //     Toolkit kit = Toolkit.getDefaultToolkit();

   //     Dimension screens = kit.getScreenSize();

   //     int w;

   //     w = screens.width;

    //    setSize((int) (w/1.3),(int) (w/1.3/1.618));

    //    setLocation(w/12, w/12);
     
    }// </editor-fold>                        

   
    // Variables declaration - do not modify                     
    private Menu_Files jMenu1;
    private Menu_Deals jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private Main_Panel mainPanel;
    private StatusPanel statusPanel;
    private javax.swing.JTabbedPane jTabbedPane1;
    // End of variables declaration                   
	
	@Override
	public void update(Observable arg0, Object arg1) {
		
		ObserverMessage message = (ObserverMessage) arg1;
		if(message.getType() == ObserverMessage.NETWORK_STATUS)
		{
			int status = (int) message.getValue();
			
			if(status == Controller.STATUS_NO_CONNECTIONS)
			{
				List<Image> icons = new ArrayList<Image>();
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon16_No.png"));
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32_No.png"));
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32_No.png"));
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32_No.png"));
				this.setIconImages(icons);
				
			}
			if(status == Controller.STATUS_SYNCHRONIZING)
			{
				List<Image> icons = new ArrayList<Image>();
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon16_Se.png"));
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32_Se.png"));
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32_Se.png"));
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32_Se.png"));
				this.setIconImages(icons);
			}
			if(status == Controller.STATUS_OK)
			{
				//ICON
				List<Image> icons = new ArrayList<Image>();
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon16.png"));
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon32.png"));
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon64.png"));
				icons.add(Toolkit.getDefaultToolkit().getImage("images/icons/icon128.png"));
				this.setIconImages(icons);
			}
		}	
		
	}
}
