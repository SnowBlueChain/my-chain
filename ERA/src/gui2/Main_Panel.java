package gui2;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import core.BlockChain;
import gui.Wallets.Wallets_Manager_SplitPanel;
import gui.bank.Issue_Send_Payment_Order;
import gui.bank.My_Order_Pauments_SplitPanel;
import gui.items.accounts.Accounts_Name_Search_SplitPanel;
import gui.items.accounts.My_Accounts_SplitPanel;
import gui.items.accounts.My_Loans_SplitPanel;
import gui.items.assets.Assets_Favorite_SplitPanel;
import gui.items.assets.Exchange_Panel;
import gui.items.assets.IssueAssetPanel;
import gui.items.assets.My_Assets_Tab;
import gui.items.assets.My_Balance_Tab;
import gui.items.assets.My_Order_Tab;
import gui.items.assets.Search_Assets_Tab;
import gui.items.imprints.Imprints_Favorite_SplitPanel;
import gui.items.imprints.Imprints_Search_SplitPanel;
import gui.items.imprints.IssueImprintPanel;
import gui.items.imprints.My_Imprints_Tab;
import gui.items.link_hashes.Issue_Linked_Hash_Panel;
import gui.items.link_hashes.Search_Linked_Hash;
import gui.items.mails.Incoming_Mails_SplitPanel;
import gui.items.mails.Mail_Send_Panel;
import gui.items.mails.Outcoming_Mails_SplitPanel;
import gui.items.other.Other_Console_Panel;
import gui.items.other.Other_Search_Blocks;
import gui.items.other.Other_Split_Panel;
import gui.items.persons.InsertPersonPanel;
import gui.items.persons.IssuePersonPanel;
import gui.items.persons.Persons_Favorite_SplitPanel;
import gui.items.persons.Persons_My_SplitPanel;
import gui.items.persons.Persons_Search_SplitPanel;
import gui.items.polls.IssuePollPanel;
import gui.items.polls.Polls_My_SplitPanel;
import gui.items.polls.Polls_Search_SplitPanel;
import gui.items.records.Records_My_SplitPanel;
import gui.items.records.Records_Search_SplitPanel;
import gui.items.records.Records_UnConfirmed_Panel;
import gui.items.statement.Issue_Document_Panel;
import gui.items.statement.Statements_Favorite_SplitPanel;
import gui.items.statement.Statements_My_SplitPanel;
import gui.items.statement.Statements_Search_SplitPanel;
import gui.items.statuses.IssueStatusPanel;
import gui.items.statuses.Search_Statuses_Tab;
import gui.items.statuses.Statuses_Favorite_SplitPanel;
import gui.items.templates.IssueTemplatePanel;
import gui.items.templates.Search_Templates_Tab;
import gui.items.templates.Templates_Favorite_SplitPanel;
import gui.items.unions.IssueUnionPanel;
import gui.items.unions.My_Unions_Tab;
import gui.items.unions.Search_Union_Tab;
import gui.library.MSplitPane;
import lang.Lang;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ����
 */
public class Main_Panel extends javax.swing.JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Main_Panel instance;
	public MainLeftPanel mlp;

	/**
	 * Creates new form split_1
	 */

	public static Main_Panel getInstance() {
		if (instance == null) {
			instance = new Main_Panel();
		}

		return instance;

	}

	private Main_Panel() {
		initComponents();
		jSplitPane1.M_setDividerSize(20);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */

	// <editor-fold defaultstate="collapsed" desc="Generated Code">
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		jSplitPane1 = new MSplitPane();
		jSplitPane1.set_CloseOnOneTouch(jSplitPane1.ONE_TOUCH_CLOSE_LEFT_TOP); // set
																				// one
																				// touch
																				// close
																				// LEFT
		jTabbedPane1 = new M_TabbedPanel(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		mlp = new MainLeftPanel();
		jTabbedPane1.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				Component cc = arg0.getComponent();
				if (cc.getClass().getSimpleName().equals("M_TabbedPanel")) {
					M_TabbedPanel mt = (M_TabbedPanel) cc;

					// find path from name node
					int index = mt.getSelectedIndex();
					if (index >= 0) {
						DefaultMutableTreeNode ss = getNodeByName(jTabbedPane1.getTitleAt(mt.getSelectedIndex()),
								(DefaultMutableTreeNode) mlp.tree.tree.getModel().getRoot());
						// set select from tree
						if (ss != null)
							mlp.tree.tree.setSelectionPath(new TreePath(ss.getPath()));
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

		});

		mlp.tree.tree.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent arg0) {
				// TODO Auto-generated method stub
				if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
					// find path from name node

					addTab(mlp.tree.tree.getLastSelectedPathComponent().toString());
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyTyped(KeyEvent arg0) {
				// TODO Auto-generated method stub

			}

		});

		setLayout(new java.awt.GridBagLayout());

		jSplitPane1.setRightComponent(jTabbedPane1);

		mlp.tree.tree.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				// TODO Auto-generated method stub

			}

		});

		mlp.tree.tree.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub
				if (arg0.getClickCount() == 1) {

					Component aa = arg0.getComponent();
					if (aa.getClass().getSimpleName().equals("JTree")) {
						JTree tr = ((JTree) aa);
						if (tr.getLastSelectedPathComponent() == null)
							return;
						addTab(tr.getLastSelectedPathComponent().toString());

					}
					;
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}

		});

		/*
		 * mlp.jButton1.addActionListener(new ActionListener(){
		 * 
		 * @Override public void actionPerformed(ActionEvent arg0) { // TODO
		 * Auto-generated method stub
		 * 
		 * 
		 * int s = jTabbedPane1.indexOfTab("tab1");
		 * 
		 * 
		 * 
		 * 
		 * if (s==-1) { jTabbedPane1.addTabWithCloseButton("tab1", new
		 * JPanel()); s= jTabbedPane1.indexOfTab("tab1");
		 * 
		 * 
		 * } jTabbedPane1.setSelectedIndex(s); }
		 * 
		 * 
		 * });
		 */
		mlp.setMinimumSize(new Dimension(0, 0));
		jSplitPane1.setLeftComponent(mlp);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 0.1;
		gridBagConstraints.weighty = 0.1;
		add(jSplitPane1, gridBagConstraints);
		jSplitPane1.setDividerLocation(250);
	}// </editor-fold>

	// Variables declaration - do not modify

	public MSplitPane jSplitPane1;
	public M_TabbedPanel jTabbedPane1;
	// End of variables declaration

	// add tab from name
	public void addTab(String str) {

		if (str.equals(Lang.getInstance().translate("Send Payment Order")) || str.equals("Issue_Send_Payment_Order")) {
			insertTab(Lang.getInstance().translate("Send Payment Order"), new Issue_Send_Payment_Order());
			return;
		}

		if (str.equals(Lang.getInstance().translate("My Payments Orders"))
				|| str.equals("My_Order_Pauments_SplitPanel")) {
			insertTab(Lang.getInstance().translate("My Payments Orders"), new My_Order_Pauments_SplitPanel());
			return;
		}

		if (str.equals(Lang.getInstance().translate("Favorite Persons")) || str.equals("Persons_Favorite_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Favorite Persons"), new Persons_Favorite_SplitPanel());
			return;
		}

		if (str.equals(Lang.getInstance().translate("My Persons")) || str.equals("Persons_My_SplitPanel")) {
			insertTab(Lang.getInstance().translate("My Persons"), new Persons_My_SplitPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Search Persons")) || str.equals("Persons_Search_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Search Persons"), new Persons_Search_SplitPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Issue Person")) || str.equals("IssuePersonPanel")) {
			insertTab(Lang.getInstance().translate("Issue Person"), new IssuePersonPanel());
			return;

		}

		if (str.equals(Lang.getInstance().translate("Insert Person")) || str.equals("InsertPersonPanel")) {
			insertTab(Lang.getInstance().translate("Insert Person"), new InsertPersonPanel());
			return;

		}

		if (str.equals(Lang.getInstance().translate("My Accounts")) || str.equals("My_Accounts_SplitPanel")) {
			insertTab(Lang.getInstance().translate("My Accounts"), new My_Accounts_SplitPanel());
			return;

		}

		if (str.equals(Lang.getInstance().translate("My Loans")) || str.equals("My_Loans_SplitPanel")) {
			insertTab(Lang.getInstance().translate("My Loans"), new My_Loans_SplitPanel());
			return;

		}

		if (str.equals(Lang.getInstance().translate("Favorite Accounts"))
				|| str.equals("Accounts_Name_Search_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Favorite Accounts"), new Accounts_Name_Search_SplitPanel());
			return;

		}

		if (str.equals(Lang.getInstance().translate("Favorite Documents"))
				|| str.equals("Statements_Favorite_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Favorite Documents"), new Statements_Favorite_SplitPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("My Documents")) || str.equals("Statements_My_SplitPanel")) {
			insertTab(Lang.getInstance().translate("My Documents"), new Statements_My_SplitPanel());
			return;

		}
		if (str.equals(Lang.getInstance().translate("Search Documents"))
				|| str.equals("Statements_Search_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Search Documents"), new Statements_Search_SplitPanel());
			return;

		}
		if (str.equals(Lang.getInstance().translate("Issue Document")) || str.equals("Issue_Document_Panel")) {
			insertTab(Lang.getInstance().translate("Issue Document"), new Issue_Document_Panel());
			return;

		}
		if (str.equals(Lang.getInstance().translate("Incoming Mails")) || str.equals("Incoming_Mails_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Incoming Mails"), new Incoming_Mails_SplitPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Outcoming Mails")) || str.equals("Outcoming_Mails_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Outcoming Mails"), new Outcoming_Mails_SplitPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Send Mail")) || str.equals("Mail_Send_Panel")) {
			insertTab(Lang.getInstance().translate("Send Mail"), new Mail_Send_Panel(null, null, null, null));
			return;
		}

		if (str.equals(Lang.getInstance().translate("Favorite Assets")) || str.equals("Assets_Favorite_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Favorite Assets"), new Assets_Favorite_SplitPanel());
			return;
		}

		if (str.equals(Lang.getInstance().translate("My Assets")) || str.equals("My_Assets_Tab")) {
			insertTab(Lang.getInstance().translate("My Assets"), new My_Assets_Tab());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Search Assets")) || str.equals("Search_Assets_Tab")) {
			insertTab(Lang.getInstance().translate("Search Assets"), new Search_Assets_Tab(true));
			return;
		}
		if (str.equals(Lang.getInstance().translate("My Balance")) || str.equals("My_Balance_Tab")) {
			insertTab(Lang.getInstance().translate("My Balance"), new My_Balance_Tab());
			return;
		}
		if (str.equals(Lang.getInstance().translate("My Orders")) || str.equals("My_Order_Tab")) {
			insertTab(Lang.getInstance().translate("My Orders"), new My_Order_Tab());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Issue Asset")) || str.equals("IssueAssetPanel")) {
			insertTab(Lang.getInstance().translate("Issue Asset"), new IssueAssetPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Exchange")) || str.equals("Exchange_Panel")) {
			insertTab(Lang.getInstance().translate("Exchange"), new Exchange_Panel(null, null, null, null));
			return;
		}
		if (str.equals(Lang.getInstance().translate("Search Templates")) || str.equals("Search_Templates_Tab")) {
			insertTab(Lang.getInstance().translate("Search Templates"), new Search_Templates_Tab());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Favorite Templates"))
				|| str.equals("Templates_Favorite_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Favorite Templates"), new Templates_Favorite_SplitPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Issue Template")) || str.equals("IssueTemplatePanel")) {
			insertTab(Lang.getInstance().translate("Issue Template"), new IssueTemplatePanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Create Status")) || str.equals("IssueStatusPanel")) {
			insertTab(Lang.getInstance().translate("Create Status"), new IssueStatusPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Favorite Statuses"))
				|| str.equals("Statuses_Favorite_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Favorite Statuses"), new Statuses_Favorite_SplitPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Search Statuses")) || str.equals("Search_Statuses_Tab")) {
			insertTab(Lang.getInstance().translate("Search Statuses"), new Search_Statuses_Tab());
			return;
		}
		if (BlockChain.DEVELOP_USE) {
			if (str.equals(Lang.getInstance().translate("My Unions")) || str.equals("My_Unions_Tab")) {
				insertTab(Lang.getInstance().translate("My Unions"), new My_Unions_Tab());
				return;
			}
			if (str.equals(Lang.getInstance().translate("Search Unions")) || str.equals("Search_Union_Tab")) {
				insertTab(Lang.getInstance().translate("Search Unions"), new Search_Union_Tab());
				return;
			}
			if (str.equals(Lang.getInstance().translate("Issue Union")) || str.equals("IssueUnionPanel")) {
				insertTab(Lang.getInstance().translate("Issue Union"), new IssueUnionPanel());
				return;
			}
		}
		
		
		if (str.equals(Lang.getInstance().translate("My Polls")) || str.equals("Polls_My_SplitPanel")) {
			insertTab(Lang.getInstance().translate("My Polls"), new Polls_My_SplitPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Search Polls")) || str.equals("Polls_Search_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Search Polls"), new Polls_Search_SplitPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Issue Poll")) || str.equals("IssuePollPanel")) {
			insertTab(Lang.getInstance().translate("Issue Poll"), new IssuePollPanel());
			return;
		}

		if (str.equals(Lang.getInstance().translate("My Records")) || str.equals("Records_My_SplitPanel")) {
			insertTab(Lang.getInstance().translate("My Records"), Records_My_SplitPanel.getInstance());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Search Records")) || str.equals("Records_Search_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Search Records"), new Records_Search_SplitPanel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Unconfirmed Records"))
				|| str.equals("Records_UnConfirmed_Panel")) {
			insertTab(Lang.getInstance().translate("Unconfirmed Records"), new Records_UnConfirmed_Panel());
			return;
		}
		if (str.equals(Lang.getInstance().translate("Other")) || str.equals("Other_Split_Panel")) {
			insertTab(Lang.getInstance().translate("Other"), new Other_Split_Panel());

			return;
		}

		if (str.equals(Lang.getInstance().translate("Console")) || str.equals("Other_Console_Panel")) {
			insertTab(Lang.getInstance().translate("Console"), new Other_Console_Panel());

			return;
		}

		if (str.equals(Lang.getInstance().translate("Blocks")) || str.equals("Other_Search_Blocks")) {
			insertTab(Lang.getInstance().translate("Blocks"), new Other_Search_Blocks());

			return;
		}

		if (str.equals(Lang.getInstance().translate("My Unique Hashes")) || str.equals("My_Imprints_Tab")) {
			insertTab(Lang.getInstance().translate("My Unique Hashes"), new My_Imprints_Tab());

			return;
		}

		if (str.equals(Lang.getInstance().translate("Search Unique Hashes"))
				|| str.equals("Imprints_Search_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Search Unique Hashes"), new Imprints_Search_SplitPanel());

			return;
		}

		if (str.equals(Lang.getInstance().translate("Favorite Unique Hashes"))
				|| str.equals("Imprints_Favorite_SplitPanel")) {
			insertTab(Lang.getInstance().translate("Favorite Unique Hashes"), new Imprints_Favorite_SplitPanel());

			return;
		}

		if (str.equals(Lang.getInstance().translate("Issue Unique Hash")) || str.equals("IssueImprintPanel")) {
			insertTab(Lang.getInstance().translate("Issue Unique Hash"), new IssueImprintPanel());

			return;
		}

		if (str.equals(Lang.getInstance().translate("Issue Linked Hash")) || str.equals("Issue_Linked_Hash_Panel")) {
			insertTab(Lang.getInstance().translate("Issue Linked Hash"), new Issue_Linked_Hash_Panel());

			return;
		}

		if (str.equals(Lang.getInstance().translate("Search Linked Hash")) || str.equals("Search_Linked_Hash")) {
			insertTab(Lang.getInstance().translate("Search Linked Hash"), new Search_Linked_Hash());

			return;
		}

		if (BlockChain.DEVELOP_USE) {
			if (str.equals(Lang.getInstance().translate("Wallets Manager"))
					|| str.equals("Wallets_Manager_SplitPanel")) {
				insertTab(Lang.getInstance().translate("Wallets Manager"), new Wallets_Manager_SplitPanel());
				return;
			}
		}

	}

	// insert tab in tabbedpane
	public void insertTab(String str, JPanel pp) {
		int s = -1;
		s = jTabbedPane1.indexOfTab(str);
		if (s == -1) {
			jTabbedPane1.addTabWithCloseButton(str, pp);
			s = jTabbedPane1.indexOfTab(str);
		}
		jTabbedPane1.setSelectedIndex(s);

	}

	// get node by name
	private DefaultMutableTreeNode getNodeByName(String sNodeName, DefaultMutableTreeNode parent) {
		if (parent != null)
			for (Enumeration e = parent.breadthFirstEnumeration(); e.hasMoreElements();) {
				DefaultMutableTreeNode current = (DefaultMutableTreeNode) e.nextElement();
				if (sNodeName.equals(current.getUserObject())) {
					return current;
				}
			}
		return null;
	}

}
