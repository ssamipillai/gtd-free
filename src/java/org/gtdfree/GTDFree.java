/*
 *    Copyright (C) 2008-2010 Igor Kriznar
 *    
 *    This file is part of GTD-Free.
 *    
 *    GTD-Free is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *    
 *    GTD-Free is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *    
 *    You should have received a copy of the GNU General Public License
 *    along with GTD-Free.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gtdfree;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.rmi.server.UnicastRemoteObject;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.gtdfree.GTDFreeEngine.VersionInfo;
import org.gtdfree.gui.DatabaseToolsDialog;
import org.gtdfree.gui.ExecutePane;
import org.gtdfree.gui.ExportDialog;
import org.gtdfree.gui.GTDFreePane;
import org.gtdfree.gui.ImportExampleDialog;
import org.gtdfree.gui.InBasketPane;
import org.gtdfree.gui.JournalPane;
import org.gtdfree.gui.Monitor;
import org.gtdfree.gui.OrganizePane;
import org.gtdfree.gui.ProcessPane;
import org.gtdfree.gui.QuickCollectPanel;
import org.gtdfree.gui.WorkflowPane;
import org.gtdfree.model.Folder;
import org.gtdfree.model.FolderEvent;
import org.gtdfree.model.GTDData;
import org.gtdfree.model.GTDDataODB;
import org.gtdfree.model.GTDDataXML;
import org.gtdfree.model.GTDModelListener;
import org.gtdfree.model.Utils;



/**
 * @author ikesan
 *
 */
public class GTDFree implements GTDFreeOperations {
	
	private static final String STUB_FILE_NAME = "stub.bin"; //$NON-NLS-1$

	class OverviewTabPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		public OverviewTabPanel(final Icon icon, final int tab, final String desc) {
			
			setLayout(new GridBagLayout());
			
			JButton b= new JButton();
			b.setIcon(icon);
			b.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(tab);
				}
			});
			b.setMargin(new Insets(0,0,0,0));
			add(b,new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,7),0,0));

			JLabel l= new JLabel(desc);
			l.setFont(l.getFont().deriveFont(Font.BOLD));
			add(l,new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0));

		}
	}
	
	class SummaryBean implements GTDModelListener {
		
		private PropertyChangeSupport supp= new PropertyChangeSupport(this);
		private int inbucketCount;
		private int queueCount; 
		private int pastActions;
		private int todayActions;
		private int totalCount;
		private int openCount;
		
		public SummaryBean() {
			getEngine().getGTDModel().addGTDModelListener(this);
			updateInBucket();
			updateQueue();
			updateReminders();
			updateMainCounts();
		}

		@Override
		public void folderAdded(Folder folder) {
			//
		}

		@Override
		public void folderModified(FolderEvent folder) {
			//
		}

		@Override
		public void folderRemoved(Folder folder) {
			//
		}

		@Override
		public void elementAdded(FolderEvent a) {
			if (a.getFolder().isQueue()) {
				updateQueue();
			} else if (a.getFolder().isInBucket()) {
				updateInBucket();
			} else if (a.getFolder().isTickler()) {
				updateReminders();
			}
			updateMainCounts();
		}

		@Override
		public void elementModified(org.gtdfree.model.ActionEvent a) {
			if (a.getAction().getParent().isQueue()||a.getAction().isQueued()) {
				updateQueue();
			} else if (a.getAction().getParent().isInBucket()) {
				updateInBucket();
			} else if (a.getProperty().equals(org.gtdfree.model.Action.REMIND_PROPERTY_NAME) 
					|| (a.getAction().getRemind()!=null && a.getProperty().equals(org.gtdfree.model.Action.RESOLUTION_PROPERTY_NAME))) {
				updateReminders();
			}
		}

		@Override
		public void elementRemoved(FolderEvent a) {
			if (a.getFolder().isQueue()) {
				updateQueue();
			} else if (a.getFolder().isInBucket()) {
				updateInBucket();
			} else if (a.getFolder().isTickler()) {
				updateReminders();
			}
			updateMainCounts();
		}

		@Override
		public void orderChanged(Folder f) {
			// 
		}
		
		private void updateInBucket() {
			int n= getEngine().getGTDModel().getInBucketFolder().getOpenCount();
			if (n!=inbucketCount) {
				int old= inbucketCount;
				inbucketCount=n;
				supp.firePropertyChange("inbucketCount", old, n); //$NON-NLS-1$
			}
		}
		private void updateQueue() {
			int n= getEngine().getGTDModel().getQueue().getOpenCount();
			if (n!=queueCount) {
				int old= queueCount;
				queueCount=n;
				supp.firePropertyChange("queueCount", old, n); //$NON-NLS-1$
			}
		}
		private void updateMainCounts() {
			int open= 0;
			int total= 0;
			for (Folder f : getEngine().getGTDModel()) {
				if (!f.isMeta()) {
					open+=f.getOpenCount();
					total+=f.size();
				}
			}

			if (total!=this.totalCount || open!=this.openCount) {
				this.totalCount=total;
				this.openCount=open;
				supp.firePropertyChange("mainCounts", -1,total); //$NON-NLS-1$
			}
		}
		private void updateReminders() {
			int past=0;
			int today=0;
			long time= Utils.today();
			for (org.gtdfree.model.Action a : getEngine().getGTDModel().getRemindFolder()) {
				if (a.getRemind()!=null && a.isOpen()) {
					long t= a.getRemind().getTime();
					if (t<time) {
						past++;
					} else if ((t>=time) && (t<(time+Utils.MILLISECONDS_IN_DAY))) {
						today++;
					}
				}
			}
			if (past!=pastActions) {
				pastActions=past;
				supp.firePropertyChange("pastActions", -1, pastActions); //$NON-NLS-1$
			}
			if (today!=todayActions) {
				todayActions=today;
				supp.firePropertyChange("todayActions", -1, todayActions); //$NON-NLS-1$
			}
		}

		/**
		 * @return the inbasketCount
		 */
		public int getInbucketCount() {
			return inbucketCount;
		}
		
		public int getQueueCount() {
			return queueCount;
		}
		
		public void addPropertyChangeListener( String prop, PropertyChangeListener l) {
			supp.addPropertyChangeListener(prop, l);
		}

		public int getPastActions() {
			return pastActions;
		}
		
		public int getTodayActions() {
			return todayActions;
		}
		public int getOpenCount() {
			return openCount;
		}
		public int getTotalCount() {
			return totalCount;
		}
		
	}
	
	abstract class SummaryLabel extends JPanel implements ActionListener {
		private static final long serialVersionUID = 1L;
		
		protected JLabel label;
		protected JButton button;

		public SummaryLabel(final String prop) {
			setLayout(new GridBagLayout());
			
			label= new JLabel();
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			add(label,new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));

			SummaryBean sb= getSummaryBean();

			sb.addPropertyChangeListener(prop, new PropertyChangeListener() {
			
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					updateText(evt);
				}
			});
			
			button= new JButton();
			button.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_next));
			button.addActionListener(this);
			button.setMargin(new Insets(1,1,1,1));
			add(button,new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,4,0,0),0,0));
			add(new JPanel(),new GridBagConstraints(1,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0));
			
			updateText(new PropertyChangeEvent(this,prop,null,null));
		}

		abstract void updateText(PropertyChangeEvent evt);
	}
	
	private static final int TAB_COLECT = 1;
	private static final int TAB_PROCESS = 2;
	private static final int TAB_ORGANIZE = 3;
	private static final int TAB_EXECUTE = 4;
	private static TrayIcon trayIcon;

	private JFrame jFrame = null;
	private JPanel jContentPane = null;
	private JMenuBar jJMenuBar = null;
	private JMenu fileMenu = null;
	private JMenu helpMenu = null;
	private JMenuItem exitMenuItem = null;
	private JMenuItem aboutMenuItem = null;
	private JMenuItem saveMenuItem = null;
	private JDialog aboutDialog = null;
	private JFileChooser fileChooser;
	private GTDFreeEngine engine;
	private JCheckBoxMenuItem autoSaveMenuItem;
	private ActionMap actionMap;
	private JCheckBoxMenuItem showAllActions;
	private JCheckBoxMenuItem showClosedFolders;
	//private JTextArea consistencyCheckText;
	private ProcessPane processPane;
	private OrganizePane organizePane;
	private InBasketPane inBasketPane;
	private ExecutePane executePane;
	private JournalPane journalPane;
	private boolean closed;
	private JPanel overview;
	private JTabbedPane tabbedPane;
	private SummaryBean summaryBean;
	private JCheckBoxMenuItem showOverviewTab;
	private QuickCollectPanel quickCollectPanel;
	private JCheckBoxMenuItem showQuickCollectBar;
	private ImportExampleDialog importDialog;
	private int executeTabIndex;
	private ExportDialog exportDialog;
	//private JMenuItem printCurrentMenuItem;
	private ExportDialog printDialog;
	//private Logger consistencyCheckLogger;
	private DatabaseToolsDialog dbInfoDialog;
	private JPopupMenu trayIconPopup;
	private JCheckBoxMenuItem showTrayIcon;
	private Monitor monitor;
	private JCheckBoxMenuItem showEmptyFolders;
	private JWindow flasher;
	private JLabel flasherText;
	private Logger logger;
	private GTDFreeOperations stub;

	/**
	 * @param args
	 */
	@SuppressWarnings("static-access")
	public static void main(final String[] args) {

		//ApplicationHelper.changeDefaultFontSize(6, "TextField");
		//ApplicationHelper.changeDefaultFontSize(6, "TextArea");
		//ApplicationHelper.changeDefaultFontSize(6, "Table");
		//ApplicationHelper.changeDefaultFontSize(6, "Tree");
		
		//ApplicationHelper.changeDefaultFontStyle(Font.BOLD, "Tree");
		
		final Logger logger= Logger.getLogger(GTDFree.class);
		// Set a reasonable default log level for production use
		logger.setLevel(Level.INFO);
		
		// Try to configure from properties file first, fallback to basic configuration
		try {
			java.io.InputStream is = GTDFree.class.getClassLoader().getResourceAsStream("log4j.properties");
			if (is != null) {
				java.util.Properties props = new java.util.Properties();
				props.load(is);
				org.apache.log4j.PropertyConfigurator.configure(props);
				is.close();
			} else {
				BasicConfigurator.configure();
			}
		} catch (Exception e) {
			// Fallback to basic configuration if properties file not found
			BasicConfigurator.configure();
		}
		
		Options op= new Options();
		op.addOption("data",true,Messages.getString("GTDFree.Options.data")); //$NON-NLS-1$ //$NON-NLS-2$
		op.addOption("eodb",true,Messages.getString("GTDFree.Options.eodb")); //$NON-NLS-1$ //$NON-NLS-2$
		op.addOption("exml",true,Messages.getString("GTDFree.Options.exml")); //$NON-NLS-1$ //$NON-NLS-2$
		op.addOption("h","help",false,Messages.getString("GTDFree.Options.help")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		op.addOption("log",true,Messages.getString("GTDFree.Options.log")); //$NON-NLS-1$ //$NON-NLS-2$

		Options op2= new Options();
		op2.addOption(
				OptionBuilder
				.hasArg()
				.isRequired(false)
				.withDescription(
						new MessageFormat(Messages.getString("GTDFree.Options.lang")).format(new Object[]{"'en'","'de', 'en'"})) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				.withArgName("de|en") //$NON-NLS-1$
				.withLongOpt("Duser.language") //$NON-NLS-1$
				.withValueSeparator('=')
				.create());
		op2.addOption(
				OptionBuilder
				.hasArg()
				.isRequired(false)
				.withDescription(
						new MessageFormat(Messages.getString("GTDFree.Options.laf")).format(new Object[]{"'com.jgoodies.looks.plastic.Plastic3DLookAndFeel', 'com.jgoodies.looks.plastic.PlasticLookAndFeel', 'com.jgoodies.looks.plastic.PlasticXPLookAndFeel', 'com.jgoodies.looks.windows.WindowsLookAndFeel' (only on MS Windows), 'com.sun.java.swing.plaf.gtk.GTKLookAndFeel' (only on Linux with GTK), 'com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel', 'javax.swing.plaf.metal.MetalLookAndFeel'"})) //$NON-NLS-1$ //$NON-NLS-2$
				.withLongOpt("Dswing.crossplatformlaf") //$NON-NLS-1$
				.withValueSeparator('=')
				.create());
		
		CommandLineParser clp= new GnuParser();
		CommandLine cl= null;
		try {
			cl= clp.parse(op, args);
		} catch (ParseException e1) {
			logger.error("Parse error.", e1); //$NON-NLS-1$
		}
		
		System.out.print("GTD-Free"); //$NON-NLS-1$
		String ver=""; //$NON-NLS-1$
		try {
			System.out.println(" version "+(ver=ApplicationHelper.getVersion())); //$NON-NLS-1$
			
		} catch (Exception e) {
			System.out.println();
			// ignore
		}

		if (true) { // || cl.hasOption("help") || cl.hasOption("h")) {
			HelpFormatter hf= new HelpFormatter();
			hf.printHelp(
					"java [Java options] -jar gtd-free.jar [gtd-free options]" //$NON-NLS-1$
					,"[gtd-free options] - "+Messages.getString("GTDFree.Options.appop") //$NON-NLS-1$ //$NON-NLS-2$
					, op
					,"[Java options] - "+new MessageFormat(Messages.getString("GTDFree.Options.javaop")).format(new Object[]{"'-jar'"}) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					,false);
			StringWriter sw= new StringWriter();
			PrintWriter pw= new PrintWriter(sw);
			hf.setLongOptPrefix("-"); //$NON-NLS-1$
			hf.setWidth(88);
			hf.printOptions(pw, hf.getWidth(), op2, hf.getLeftPadding(), hf.getDescPadding());
			String s= sw.getBuffer().toString();
			s=s.replaceAll("\\A {3}", ""); //$NON-NLS-1$ //$NON-NLS-2$
			s=s.replaceAll("\n {3}", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
			s=s.replaceAll(" <", "=<"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.print(s);
		}
		
		String val= cl.getOptionValue("data"); //$NON-NLS-1$
		if (val!=null) {
			System.setProperty(ApplicationHelper.DATA_PROPERTY, val);
			System.setProperty(ApplicationHelper.TITLE_PROPERTY, "1"); //$NON-NLS-1$
		} else {
			System.setProperty(ApplicationHelper.TITLE_PROPERTY, "0"); //$NON-NLS-1$
		}
		
		val= cl.getOptionValue("log"); //$NON-NLS-1$
		if (val!=null) {
			Level l= Level.toLevel(val, Level.INFO);
			Logger.getRootLogger().setLevel(l);
			logger.setLevel(l);
		}
		
		if (!ApplicationHelper.tryLock(null)) {
			System.out.println("Instance of GTD-Free already running, pushing it to be visible..."); //$NON-NLS-1$
			remotePushVisible();
			System.out.println("Instance of GTD-Free already running, exiting."); //$NON-NLS-1$
			System.exit(0);
		}
		
		// Additional file logging only if log level is not OFF and we don't already have file appender
		if (!"OFF".equalsIgnoreCase(val) && Logger.getRootLogger().getAppender("FILE") == null) { //$NON-NLS-1$
			RollingFileAppender f=null;
			try {
				f = new RollingFileAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN),ApplicationHelper.getLogFileName(),true);
				f.setMaxBackupIndex(3);
				f.setName("ADDITIONAL_FILE");
				Logger.getRootLogger().addAppender(f);
				f.rollOver();
			} catch (IOException e2) {
				logger.error("Logging error.", e2); //$NON-NLS-1$
			}
		}
		logger.info("GTD-Free "+ver+" started."); //$NON-NLS-1$ //$NON-NLS-2$
		if (logger.isDebugEnabled()) {
			logger.debug("Args: "+Arrays.toString(args)); //$NON-NLS-1$
		}
		logger.info("Using data in: "+ApplicationHelper.getDataFolder()); //$NON-NLS-1$

		if (cl.getOptionValue("exml")!=null || cl.getOptionValue("eodb")!=null) { //$NON-NLS-1$ //$NON-NLS-2$
			
			GTDFreeEngine engine= null;
			
			try {
				engine= new GTDFreeEngine();
			} catch (Exception e1) {
				logger.fatal("Fatal error, exiting.", e1); //$NON-NLS-1$
			}
			
			val= cl.getOptionValue("exml"); //$NON-NLS-1$
			if (val!=null) {
				File f1= new File(val);
				if (f1.isDirectory()) {
					f1= new File(f1, "gtd-free-"+ApplicationHelper.formatLongISO(new Date())+".xml"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				try {
					f1.getParentFile().mkdirs();
				} catch (Exception e) {
					logger.error("Export error.", e); //$NON-NLS-1$
				}
				try {
					engine.getGTDModel().exportXML(f1);
					logger.info("Data successfully exported as XML to "+f1.toString()); //$NON-NLS-1$
				} catch (Exception e) {
					logger.error("Export error.", e); //$NON-NLS-1$
				}
			}
			
			val= cl.getOptionValue("eodb"); //$NON-NLS-1$
			if (val!=null) {
				File f1= new File(val);
				if (f1.isDirectory()) {
					f1= new File(f1, "gtd-free-"+ApplicationHelper.formatLongISO(new Date())+".odb-xml"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				try {
					f1.getParentFile().mkdirs();
				} catch (Exception e) {
					logger.error("Export error.", e); //$NON-NLS-1$
				}
				try {
					GTDData data= engine.getGTDModel().getDataRepository();
					if (data instanceof GTDDataODB) {
						try {
							((GTDDataODB)data).exportODB(f1);
						} catch (Exception e) {
							logger.error("Export error.", e); //$NON-NLS-1$
						}
						logger.info("Data successfully exported as ODB to "+f1.toString()); //$NON-NLS-1$
					} else {
						logger.info("Data is not stored in ODB database, nothing is exported."); //$NON-NLS-1$
					}
				} catch (Exception e) {
					logger.error("Export error.", e); //$NON-NLS-1$
				}
			}

			try {
				engine.close(true,false);
			} catch (Exception e) {
				logger.error("Internal error.", e); //$NON-NLS-1$
			}
			
			return;
		}
		
		logger.debug("Using OS '"+System.getProperty("os.name")+"', '"+System.getProperty("os.version")+"', '"+System.getProperty("os.arch")+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		logger.debug("Using Java '"+System.getProperty("java.runtime.name")+"' version '"+System.getProperty("java.runtime.version")+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		
		Locale[] supported= {Locale.ENGLISH,Locale.GERMAN};
		
		String def= Locale.getDefault().getLanguage();
		boolean toSet=true;
		for (Locale locale : supported) {
			toSet&=!locale.getLanguage().equals(def);
		}
		
		if (toSet) {
			logger.debug("System locale '"+def+"' not supported, setting to '"+Locale.ENGLISH.getLanguage()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			try {
				Locale.setDefault(Locale.ENGLISH);
			} catch (Exception e) {
				logger.warn("Setting default locale failed.", e); //$NON-NLS-1$
			}
		} else {
			logger.debug("Using locale '"+Locale.getDefault().toString()+"'."); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// Configure modern Windows Look and Feel
		ApplicationHelper.configureWindowsLookAndFeel();
		logger.debug("Using L&F '"+UIManager.getLookAndFeel().getName()+"' by "+UIManager.getLookAndFeel().getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$

		// Configure modern UI fonts (Gill Sans MT/Segoe UI) for better readability
		ApplicationHelper.configureModernUIFonts();

		// Initialize performance optimizer for memory and CPU optimization
		try {
			org.gtdfree.modernization.PerformanceOptimizer.initialize();
			logger.info("Performance optimizer initialized - " + org.gtdfree.modernization.PerformanceOptimizer.getMemoryInfo());
		} catch (Exception e) {
			logger.warn("Failed to initialize performance optimizer", e);
		}

		// Add shutdown hook for proper cleanup
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				logger.info("GTD-Free shutdown initiated - cleaning up resources");
				ApplicationHelper.stopBackgroundExecutor();
				org.gtdfree.modernization.PerformanceOptimizer.shutdown();
				logger.info("GTD-Free shutdown completed");
			} catch (Exception e) {
				logger.warn("Error during shutdown cleanup", e);
			}
		}, "GTD-Free-Shutdown"));
		
		try {
			final GTDFree application = new GTDFree();
					
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					try {

						application.getJFrame();
						application.restore();
						//application.getJFrame().setVisible(true);
						application.pushVisible();
						
						ApplicationHelper.executeInBackground(new Runnable() {
							@Override
							public void run() {
								if (SystemTray.isSupported() && application.getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_TRAY_ICON, false)) {
									try {
										SystemTray.getSystemTray().add(application.getTrayIcon());
									} catch (AWTException e) {
										logger.error("Failed to activate system tray icon.", e); //$NON-NLS-1$
									}
								}
							}
						});
						
						ApplicationHelper.executeInBackground(new Runnable() {
							
							@Override
							public void run() {
								application.exportRemote();
							}
						});

						if (application.getEngine().getGlobalProperties().getBoolean(GlobalProperties.CHECK_FOR_UPDATE_AT_START, true)) {
							ApplicationHelper.executeInBackground(new Runnable() {
								
								@Override
								public void run() {
									application.checkForUpdates(false);
								}
							});
						}
						
					} catch (Throwable t) {
						t.printStackTrace();
						logger.fatal("Failed to start application, exiting.", t); //$NON-NLS-1$
						if (application!=null) {
							application.close(true);
						}
						System.exit(0);
					}
			}
			});
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						application.close(true);
					} catch (Exception e) {
						logger.warn("Failed to stop application.", e); //$NON-NLS-1$
					}
					logger.info("Closed."); //$NON-NLS-1$
					ApplicationHelper.releaseLock();
					LogManager.shutdown();
				}
			});
		} catch (Throwable t) {
			logger.fatal("Initialization failed, exiting.", t); //$NON-NLS-1$
			t.printStackTrace();
			System.exit(0);
		}
	} 
	
	public GTDFree() {
		logger= Logger.getLogger(GTDFree.class);
	}

	private void flashMessage(String string, Point location) {
		if (flasher==null) {
			flasher= new JWindow();
			flasher.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					flasher.setVisible(false);
				}
			});
			flasher.setContentPane(flasherText=new JLabel());
			flasherText.setBorder(new Border() {
				Insets insets= new Insets(5,11,5,11);
				@Override
				public void paintBorder(Component c, Graphics g, int x, int y, int width,
						int height) {
					//
				}
				
				@Override
				public boolean isBorderOpaque() {
					return false;
				}
				
				@Override
				public Insets getBorderInsets(Component c) {
					return insets;
				}
			});
			flasherText.setBackground(new Color(0xf3f3ad));
			flasherText.setOpaque(true);
		}
		flasher.setVisible(false);
		flasherText.setText(string);
		flasher.pack();
		flasher.setLocation(location.x-flasher.getWidth()/2,location.y-flasher.getHeight());
		if (flasher.getLocation().x<0) {
			flasher.setLocation(0, flasher.getLocation().y);
		}
		if (flasher.getLocation().y<0) {
			flasher.setLocation(flasher.getLocation().x, 0);
		}
		Dimension d= Toolkit.getDefaultToolkit().getScreenSize();
		if (flasher.getLocation().x+flasher.getWidth()>d.getWidth()) {
			flasher.setLocation((int)(d.getWidth()-flasher.getWidth()),flasher.getLocation().y);
		}
		if (flasher.getLocation().y+flasher.getHeight()>d.getHeight()) {
			flasher.setLocation(flasher.getLocation().x,(int)(d.getHeight()-flasher.getHeight()));
		}
		flasher.setVisible(true);
		new Thread() {
			@Override
			public synchronized void run() {
				try {
					wait(3000);
				} catch (InterruptedException e) {
					Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				}
				flasher.setVisible(false);
			}
		}.start();
	}
	
	protected TrayIcon getTrayIcon() {
		if (trayIcon==null) {
			if (ApplicationHelper.isGTKLaF()) {
				trayIcon = new TrayIcon(ApplicationHelper.loadImage(ApplicationHelper.icon_name_large_tray_splash));
			} else {
				trayIcon = new TrayIcon(ApplicationHelper.loadImage(ApplicationHelper.icon_name_small_tray_splash));
			}
			
			trayIcon.setImageAutoSize(true);			
			trayIcon.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton()==MouseEvent.BUTTON1) {
						trayIconPopup.setVisible(false);
						if (getJFrame().isVisible()) {
							getJFrame().setVisible(false);
						} else {
							pushVisible();
						}
					} else {
						if (trayIconPopup.isVisible()) {
							trayIconPopup.setVisible(false);
						} else {
							Point p= new Point(e.getPoint()); 
							/*
							 * Disabled, because we are anyway doing things like rollover,
							 * which are probably done by Frame.
							if (getJFrame().isShowing()) {
								SwingUtilities.convertPointFromScreen(p, getJFrame());
								trayIconPopup.show(getJFrame(), p.x, p.y);
							} else {
							}*/
							trayIconPopup.show(null, p.x, p.y);
						}
					}					
				}
			});
			trayIcon.setToolTip("GTD-Free - "+Messages.getString("GTDFree.Tray.desc")); //$NON-NLS-1$ //$NON-NLS-2$
			
			/*
			 * Necessary only when popup is showing with null window. Hides popup.
			 */
			MouseListener hideMe = new MouseAdapter() {
				@Override
				public void mouseExited(MouseEvent e) {
					if (e.getComponent() instanceof JMenuItem) {
						JMenuItem jm= (JMenuItem)e.getComponent();
						jm.getModel().setRollover(false);
						jm.getModel().setArmed(false);
						jm.repaint();
					}
					
					Point p= SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), trayIconPopup);
					//System.out.println(p.x+" "+p.y+" "+trayIconPopup.getWidth()+" "+trayIconPopup.getHeight());
					if (p.x<0 || p.x>=trayIconPopup.getWidth() || p.y<0 || p.y >= trayIconPopup.getHeight()) {
						trayIconPopup.setVisible(false);
					}
				}
				@Override
				public void mouseEntered(MouseEvent e) {
					if (e.getComponent() instanceof JMenuItem) {
						JMenuItem jm= (JMenuItem)e.getComponent();
						jm.getModel().setRollover(true);
						jm.getModel().setArmed(true);
						jm.repaint();
					}
				}
			};
			
			trayIconPopup= new JPopupMenu();
			trayIconPopup.addMouseListener(hideMe);
			
			JMenuItem mi= new JMenuItem(Messages.getString("GTDFree.Tray.Drop")); //$NON-NLS-1$
			mi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_collecting));
			mi.setToolTipText(Messages.getString("GTDFree.Tray.Drop.desc")); //$NON-NLS-1$
			mi.addMouseListener(hideMe);
			
			/*
			 * Workaround for tray, if JFrame is showing, then mouse click is not fired
			 */
			mi.addMouseListener(new MouseAdapter() {
				private boolean click=false;
				
				@Override
				public void mousePressed(MouseEvent e) {
					click=true;
				}
				
				@Override
				public void mouseReleased(MouseEvent e) {
					if (click) {
						click=false;
						doMouseClicked(e);
					}
				}
				
				@Override
				public void mouseExited(MouseEvent e) {
					click=false;
				}
				
				private void doMouseClicked(MouseEvent e) {
					trayIconPopup.setVisible(false);
					Clipboard c= null;
					if (e.getButton()==MouseEvent.BUTTON1) {
						c= Toolkit.getDefaultToolkit().getSystemClipboard();
					} else if (e.getButton()==MouseEvent.BUTTON2) {
						c= Toolkit.getDefaultToolkit().getSystemSelection();
					} else {
						return;
					}
					try {
						Object o= c.getData(DataFlavor.stringFlavor);
						if (o != null) {
							getEngine().getGTDModel().collectAction(o.toString());
						}
						flashMessage(Messages.getString("GTDFree.Tray.Collect.ok"),e.getLocationOnScreen()); //$NON-NLS-1$
					} catch (Exception e1) {
						Logger.getLogger(this.getClass()).debug("Internal error.", e1); //$NON-NLS-1$
						flashMessage(Messages.getString("GTDFree.Tray.Collect.fail")+e1.getMessage(),e.getLocationOnScreen()); //$NON-NLS-1$
					}
				}
			});
			
			TransferHandler th= new TransferHandler() {
				private static final long serialVersionUID = 1L;
				@Override
				public boolean canImport(JComponent comp,
						DataFlavor[] transferFlavors) {
					return DataFlavor.selectBestTextFlavor(transferFlavors)!=null;
				}
				@Override
				public boolean importData(JComponent comp, Transferable t) {
					try {
						DataFlavor f= DataFlavor.selectBestTextFlavor(t.getTransferDataFlavors());
						Object o = t.getTransferData(f);
						if (o != null) {
							getEngine().getGTDModel().collectAction(o.toString());
						}
						return true;
					} catch (UnsupportedFlavorException e) {
						Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
					} catch (IOException e) {
						Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
					}
					return false;
				}
				
			};
			mi.setTransferHandler(th);
			
			trayIconPopup.add(mi);
			
			mi = new JMenuItem();
			mi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_delete));
			mi.setText(Messages.getString("GTDFree.Tray.Hide")); //$NON-NLS-1$
			mi.setToolTipText(Messages.getString("GTDFree.Tray.Hide.desc")); //$NON-NLS-1$
			mi.addMouseListener(hideMe);
			mi.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					trayIconPopup.setVisible(false);
					if (getJFrame().isVisible()) {
						getJFrame().setVisible(false);
					}
				}
			});
			trayIconPopup.add(mi);

			mi = new JMenuItem();
			mi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_splash));
			mi.setText(Messages.getString("GTDFree.Tray.Show")); //$NON-NLS-1$
			mi.setToolTipText(Messages.getString("GTDFree.Tray.Show.desc")); //$NON-NLS-1$
			mi.addMouseListener(hideMe);
			mi.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					trayIconPopup.setVisible(false);
					pushVisible();
				}
			});
			trayIconPopup.add(mi);

			mi = new JMenuItem();
			mi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_exit));
			mi.setText(Messages.getString("GTDFree.Tray.Exit")); //$NON-NLS-1$
			mi.addMouseListener(hideMe);
			mi.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					trayIconPopup.setVisible(false);
					close(false);
				}
			});
			trayIconPopup.add(mi);

		}
		return trayIcon;
	}

	protected void pushVisible() { 
		if(!getJFrame().isVisible()) {
			getJFrame().setVisible(true);
		}
		int st= getJFrame().getExtendedState();
		if ((st & JFrame.ICONIFIED) != 0) {
			getJFrame().setExtendedState(st & ~JFrame.ICONIFIED);
		}
		getJFrame().toFront();
		getJFrame().invalidate();
		getJFrame().repaint();
		//getJFrame().doLayout();
		//getJFrame().requestFocus();
		
	}

	public SummaryBean getSummaryBean() {
		if (summaryBean == null) {
			summaryBean = new SummaryBean();
			
		}

		return summaryBean;
	}

	/**
	 * This method initializes jFrame
	 * 
	 * @return javax.swing.JFrame
	 */
	private JFrame getJFrame() {
		if (jFrame == null) {
			jFrame = new JFrame();
			jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			jFrame.setJMenuBar(getJMenuBar());
			jFrame.setSize(1000, 600);
			jFrame.setContentPane(getJContentPane());
			String s= System.getProperty(ApplicationHelper.TITLE_PROPERTY);
			if ("1".equals(s)) { //$NON-NLS-1$
				jFrame.setTitle("GTD-Free - "+ApplicationHelper.getDataFolder().toString()); //$NON-NLS-1$
			} else {
				jFrame.setTitle("GTD-Free"); //$NON-NLS-1$
			}
			jFrame.setIconImage(ApplicationHelper.loadImage(ApplicationHelper.icon_name_small_splash)); //$NON-NLS-1$
			
			jFrame.addWindowListener(new WindowAdapter() {
			
				@Override
				public void windowClosing(WindowEvent e) {
					close(false);
				}
				
				@Override
				public void windowOpened(WindowEvent e) {
					logger.debug("GTD-Free is up."); //$NON-NLS-1$
				}
			
			});
		}
		return jFrame;
	}
	
	/**
	 * 
	 * @param terminal if <code>true</code> then close procedure can not be aborted 
	 * @return <code>true</code> if close can proceed, <code>false</code> if close should be aborted 
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws FactoryConfigurationError
	 */
	boolean close(boolean terminal) {
		if (closed) {
			return true;
		}
		
		if (getEngine().isAborting()) {
			return true;
		}
			
		logger.info("Application closing requested"+ (terminal ? " unconditionally." : ".")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		store(getEngine().getGlobalProperties());
		
		try {
			closed = getEngine().close(terminal,false);
			return closed;
		} catch (Exception e1) {
			logger.warn("Error while closing engine.",e1); //$NON-NLS-1$
			return false;
		}
	}
	
	private void aborting() {
		
		logger.debug("Cleanup while engine aborting."); //$NON-NLS-1$
		
		if (closed) {
			return;
		}
		
		if (monitor!=null) {
			monitor.close();
		}
		
		if (flasher!=null) {
			flasher.dispose();
		}
		
		if (dbInfoDialog!=null) {
			dbInfoDialog.dispose();
		}
		if (jFrame!=null) {
			jFrame.dispose();
		}
		if (trayIconPopup!=null){
			trayIconPopup.setVisible(false);
		}
		if (trayIcon!=null) {
			SystemTray.getSystemTray().remove(trayIcon);
		}
		
		if (stub!=null) {
			try {
				stub=null;
				UnicastRemoteObject.unexportObject(this, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
	}
	
	private void restore() {
		GlobalProperties p = getEngine().getGlobalProperties();
		
		Integer i1= p.getInteger("window.position.x"); //$NON-NLS-1$
		Integer i2= p.getInteger("window.position.y"); //$NON-NLS-1$

		if (i1!=null && i2!=null) {
			getJFrame().setLocation(i1, i2);
		}

		i1= p.getInteger("window.width"); //$NON-NLS-1$
		i2= p.getInteger("window.heght"); //$NON-NLS-1$
		
		if (i1!=null && i2!=null) {
			getJFrame().setSize(i1, i2);
		}
		
		restoreState();
	}
	
	private void restoreState() {
		GlobalProperties p = getEngine().getGlobalProperties();
		
		Integer i1= p.getInteger("window.state"); //$NON-NLS-1$
		
		if (i1!=null) {
			getJFrame().setExtendedState(i1);
		}
	}

	private ActionMap getActionMap() {
		if (actionMap == null) {
			actionMap = new ActionMap();
			
			AbstractAction a= new AbstractAction(Messages.getString("GTDFree.View.Closed")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					getEngine().getGlobalProperties().putProperty(GlobalProperties.SHOW_ALL_ACTIONS, !getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
				}
			};
			a.putValue(Action.SHORT_DESCRIPTION, Messages.getString("GTDFree.View.Closed.desc")); //$NON-NLS-1$
			actionMap.put(GlobalProperties.SHOW_ALL_ACTIONS, a);

			a=new AbstractAction(Messages.getString("GTDFree.View.Empty")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					getEngine().getGlobalProperties().putProperty(GlobalProperties.SHOW_EMPTY_FOLDERS, !getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_EMPTY_FOLDERS,true));
				}
			};
			a.putValue(Action.SHORT_DESCRIPTION, Messages.getString("GTDFree.View.Empty.desc")); //$NON-NLS-1$
			actionMap.put(GlobalProperties.SHOW_EMPTY_FOLDERS, a);
			
			a=new AbstractAction(Messages.getString("GTDFree.View.ClosedLists")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					getEngine().getGlobalProperties().putProperty(GlobalProperties.SHOW_CLOSED_FOLDERS, !getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_CLOSED_FOLDERS));
				}
			};
			a.putValue(Action.SHORT_DESCRIPTION, Messages.getString("GTDFree.View.ClosedLists.desc")); //$NON-NLS-1$
			actionMap.put(GlobalProperties.SHOW_CLOSED_FOLDERS, a);
			
			a=new AbstractAction(Messages.getString("GTDFree.View.Overview")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					getEngine().getGlobalProperties().putProperty(GlobalProperties.SHOW_OVERVIEW_TAB, !getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_OVERVIEW_TAB));
				}
			};
			a.putValue(Action.SHORT_DESCRIPTION, Messages.getString("GTDFree.View.Overview.desc")); //$NON-NLS-1$
			actionMap.put(GlobalProperties.SHOW_OVERVIEW_TAB, a);

			a=new AbstractAction(Messages.getString("GTDFree.View.Quick")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					getEngine().getGlobalProperties().putProperty(GlobalProperties.SHOW_QUICK_COLLECT, !getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_QUICK_COLLECT));
				}
			};
			a.putValue(Action.SHORT_DESCRIPTION, Messages.getString("GTDFree.View.Quick.desc")); //$NON-NLS-1$
			actionMap.put(GlobalProperties.SHOW_QUICK_COLLECT, a);

			a=new AbstractAction(Messages.getString("GTDFree.View.Tray")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					boolean b= !getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_TRAY_ICON);
					getEngine().getGlobalProperties().putProperty(GlobalProperties.SHOW_TRAY_ICON, b);
					if (b) {
						try {
							SystemTray.getSystemTray().add(getTrayIcon());
						} catch (AWTException e1) {
							Logger.getLogger(this.getClass()).error("System tray icon initialization failed.", e1); //$NON-NLS-1$
						}
					} else {
						SystemTray.getSystemTray().remove(getTrayIcon());
					}
				}
			};
			a.putValue(Action.SHORT_DESCRIPTION, Messages.getString("GTDFree.View.Tray.desc")); //$NON-NLS-1$
			a.setEnabled(SystemTray.isSupported());
			actionMap.put(GlobalProperties.SHOW_TRAY_ICON, a);

			a=new AbstractAction(Messages.getString("GTDFree.ImportExamples")) { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					getImportDialog().getDialog(getJFrame()).setVisible(true);
				}
			};
			a.putValue(Action.SHORT_DESCRIPTION, Messages.getString("GTDFree.ImportExamples.desc")); //$NON-NLS-1$
			actionMap.put("importDialog", a); //$NON-NLS-1$
		}

		return actionMap;
	}

	protected ImportExampleDialog getImportDialog() {
		if (importDialog == null) {
			importDialog = new ImportExampleDialog();
			importDialog.setEngine(getEngine());
		}
		return importDialog;
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			
			
			tabbedPane = new JTabbedPane();
			
			if (getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_OVERVIEW_TAB,true)) {
				tabbedPane.addTab(Messages.getString("GTDFree.Overview"), ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_overview), getOverviewPane()); //$NON-NLS-1$
			}
			
			inBasketPane = new InBasketPane();
			//inBasketPane.setEngine(getEngine());
			tabbedPane.addTab(Messages.getString("GTDFree.Collect"), ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_collecting), inBasketPane); //$NON-NLS-1$
			
			processPane= new ProcessPane();
			//processPane.setEngine(getEngine());
			tabbedPane.addTab(Messages.getString("GTDFree.Process"), ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_processing), processPane); //$NON-NLS-1$
			
			organizePane= new OrganizePane();
			//organizePane.setEngine(getEngine());
			tabbedPane.addTab(Messages.getString("GTDFree.Organize"), ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_review), organizePane); //$NON-NLS-1$
			
			executePane= new ExecutePane();
			//executePane.setEngine(getEngine());
			tabbedPane.addTab(Messages.getString("GTDFree.Execute")+" ("+getSummaryBean().getQueueCount()+")", ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_queue_execute), executePane); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			executeTabIndex= tabbedPane.getTabCount()-1;
			
			getSummaryBean().addPropertyChangeListener("queueCount", new PropertyChangeListener() { //$NON-NLS-1$
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					tabbedPane.setTitleAt(executeTabIndex, Messages.getString("GTDFree.Execute")+" ("+getSummaryBean().getQueueCount()+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			});
			
			if (Boolean.valueOf(getEngine().getConfiguration().getProperty("journal.enabled","false"))) { //$NON-NLS-1$ //$NON-NLS-2$
				journalPane = new JournalPane();
				journalPane.setEngine(getEngine());
				tabbedPane.addTab("Journal", ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_journaling),journalPane); //$NON-NLS-1$
			}
			
			tabbedPane.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					enableQuickCollectPanel();
					
					Component c= tabbedPane.getSelectedComponent();
					if (c instanceof WorkflowPane && !((WorkflowPane)c).isInitialized()) {
						((WorkflowPane)c).initialize(getEngine());
					}
					if (c instanceof GTDFreePane) {
						getEngine().setActivePane((GTDFreePane)c);
					} else {
						getEngine().setActivePane(null);
					}
				}
			});

			jContentPane.add(tabbedPane);
			
			quickCollectPanel= new QuickCollectPanel();
			quickCollectPanel.setEngine(getEngine());
			enableQuickCollectPanel();
			jContentPane.add(quickCollectPanel,BorderLayout.SOUTH);
			
		}
		return jContentPane;
	}

	private void enableQuickCollectPanel() {
		quickCollectPanel.setVisible(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_QUICK_COLLECT) && tabbedPane.getSelectedComponent()!=inBasketPane && tabbedPane.getSelectedComponent()!=overview);
	}

	private Component getOverviewPane() {
		if (overview == null) {
			overview = new JPanel();
			overview.setLayout(new GridBagLayout());

			int row=0;
			
			JLabel l= new JLabel(Messages.getString("GTDFree.OW.Workflow")); //$NON-NLS-1$
			l.setFont(l.getFont().deriveFont((float)(l.getFont().getSize()*5.0/4.0)).deriveFont(Font.BOLD));
			overview.add(l,new GridBagConstraints(0,row++,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(18,18,7,18),0,0));

			overview.add(new OverviewTabPanel(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_collecting),TAB_COLECT,Messages.getString("GTDFree.Collect")),new GridBagConstraints(0,row++,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,18,4,18),0,0)); //$NON-NLS-1$
			overview.add(new OverviewTabPanel(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_processing),TAB_PROCESS,Messages.getString("GTDFree.Process")),new GridBagConstraints(0,row++,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,18,4,18),0,0)); //$NON-NLS-1$
			overview.add(new OverviewTabPanel(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_review),TAB_ORGANIZE,Messages.getString("GTDFree.Organize")),new GridBagConstraints(0,row++,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,18,4,18),0,0)); //$NON-NLS-1$
			overview.add(new OverviewTabPanel(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_queue_execute),TAB_EXECUTE,Messages.getString("GTDFree.Execute")),new GridBagConstraints(0,row++,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,18,4,18),0,0)); //$NON-NLS-1$
			
			l= new JLabel(Messages.getString("GTDFree.OW.Summary")); //$NON-NLS-1$
			l.setFont(l.getFont().deriveFont((float)(l.getFont().getSize()*5.0/4.0)).deriveFont(Font.BOLD));
			overview.add(l,new GridBagConstraints(0,row++,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(14,18,7,18),0,0));

			SummaryLabel sl= new SummaryLabel("inbucketCount") { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(TAB_PROCESS);
				}
				@Override
				void updateText(PropertyChangeEvent arg0) {
					if (getSummaryBean().getInbucketCount()>0) {
						label.setText(getSummaryBean().getInbucketCount()+" "+Messages.getString("GTDFree.OW.Bucket")+" "+Messages.getString("GTDFree.OW.Process")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						button.setVisible(true);
					} else {
						label.setText(getSummaryBean().getInbucketCount()+" "+Messages.getString("GTDFree.OW.Bucket")); //$NON-NLS-1$ //$NON-NLS-2$
						button.setVisible(false);
					}
				}
			};
			overview.add(sl,new GridBagConstraints(0,row++,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,18,4,18),0,0));
			
			sl= new SummaryLabel("pastActions") { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(TAB_ORGANIZE);
					organizePane.openTicklerForPast();
				}
				@Override
				void updateText(PropertyChangeEvent arg0) {
					if (getSummaryBean().getPastActions()>0) {
						label.setText(getSummaryBean().getPastActions()+" "+Messages.getString("GTDFree.OW.Reminder")+" "+Messages.getString("GTDFree.OW.Update")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						button.setVisible(true);
					} else {
						label.setText(getSummaryBean().getPastActions()+" "+Messages.getString("GTDFree.OW.Reminder")); //$NON-NLS-1$ //$NON-NLS-2$
						button.setVisible(false);
						
					}
				}
			};
			overview.add(sl,new GridBagConstraints(0,row++,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,18,4,18),0,0));
			
			sl= new SummaryLabel("todayActions") { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(TAB_ORGANIZE);
					organizePane.openTicklerForToday();
				}
				@Override
				void updateText(PropertyChangeEvent arg0) {
					if (getSummaryBean().getTodayActions()>0) {
						label.setText(getSummaryBean().getTodayActions()+" "+Messages.getString("GTDFree.OW.Due")+" "+Messages.getString("GTDFree.OW.Tickler")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						button.setVisible(true);
					} else {
						label.setText(getSummaryBean().getTodayActions()+" "+Messages.getString("GTDFree.OW.Due")); //$NON-NLS-1$ //$NON-NLS-2$
						button.setVisible(false);
						
					}
				}
			};
			overview.add(sl,new GridBagConstraints(0,row++,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,18,4,18),0,0));

			sl= new SummaryLabel("queueCount") { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(TAB_EXECUTE);
				}
				@Override
				void updateText(PropertyChangeEvent arg0) {
					if (getSummaryBean().getQueueCount()>0) {
						label.setText(getSummaryBean().getQueueCount()+" "+Messages.getString("GTDFree.OW.Queue")+" "+Messages.getString("GTDFree.OW.Execute")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						button.setVisible(true);
					} else {
						label.setText(getSummaryBean().getQueueCount()+" "+Messages.getString("GTDFree.OW.Queue")); //$NON-NLS-1$ //$NON-NLS-2$
						button.setVisible(false);
						
					}
				}
			};
			overview.add(sl,new GridBagConstraints(0,row++,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,18,4,18),0,0));

			sl= new SummaryLabel("mainCounts") { //$NON-NLS-1$
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					GTDFree.this.getActionMap().get("importDialog").actionPerformed(e); //$NON-NLS-1$
					engine.getGlobalProperties().putProperty("examplesImported", true); //$NON-NLS-1$
					updateText(new PropertyChangeEvent(this,"mainCounts",-1,1)); //$NON-NLS-1$
				}
				@Override
				void updateText(PropertyChangeEvent arg0) {
					if (!getEngine().getGlobalProperties().getBoolean("examplesImported", false)) { //$NON-NLS-1$
						label.setText(getSummaryBean().getOpenCount()+" "+Messages.getString("GTDFree.OW.Open.1")+" "+getSummaryBean().getTotalCount()+" "+Messages.getString("GTDFree.OW.Open.2")+" "+Messages.getString("GTDFree.OW.Import")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
						button.setVisible(true);
					} else {
						label.setText(getSummaryBean().getOpenCount()+" "+Messages.getString("GTDFree.OW.Open.1")+" "+getSummaryBean().getTotalCount()+" "+Messages.getString("GTDFree.OW.Open.2")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
						button.setVisible(false);
						
					}
				}
			};
			overview.add(sl,new GridBagConstraints(0,row++,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(0,18,4,18),0,0));

			overview.add(new JPanel(),new GridBagConstraints(0,row++,1,1,1,1,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,11,0,11),0,0));

		}

		return overview;
	}
	
	public DatabaseToolsDialog getDatabaseInfoDialog() {
		if (dbInfoDialog==null) {
			dbInfoDialog= new DatabaseToolsDialog();
			dbInfoDialog.setEngine(getEngine());
		}
		return dbInfoDialog;
	}

	/**
	 * This method initializes jJMenuBar	
	 * 	
	 * @return javax.swing.JMenuBar	
	 */
	private JMenuBar getJMenuBar() {
		if (jJMenuBar == null) {
			jJMenuBar = new JMenuBar();
			jJMenuBar.add(getFileMenu());
			
			JMenu jm= new JMenu(Messages.getString("GTDFree.View")); //$NON-NLS-1$
			
			jm.add(getShowAllActionsMenuItem());
			jm.add(getShowClosedFoldersMenuItem());
			jm.add(getShowEmptyFoldersMenuItem());
			
			jm.add(new JSeparator());
			
			jm.add(getShowOverviewTabMenuItem());
			jm.add(getShowQuickCollectBarMenuItem());
			jm.add(getShowTrayIconMenuItem());

			jJMenuBar.add(jm);
			
			jm= new JMenu(Messages.getString("GTDFree.Go")); //$NON-NLS-1$
			
			JMenuItem jmi= new JMenuItem(Messages.getString("GTDFree.Go.Past")); //$NON-NLS-1$
			jmi.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(TAB_ORGANIZE);
					organizePane.openTicklerForPast();
				}
			});
			jm.add(jmi);
			
			jmi= new JMenuItem(Messages.getString("GTDFree.Go.Today")); //$NON-NLS-1$
			jmi.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					tabbedPane.setSelectedIndex(TAB_ORGANIZE);
					organizePane.openTicklerForToday();
				}
			});
			jm.add(jmi);

			jJMenuBar.add(jm);
			
			jm= new JMenu(Messages.getString("GTDFree.Tools")); //$NON-NLS-1$
			jJMenuBar.add(jm);
			
			jmi= new JMenuItem(Messages.getString("GTDFree.Tools.DB")); //$NON-NLS-1$
			jmi.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {

					getDatabaseInfoDialog().showDatabaseToolsDialog(getJFrame());
					
				}
			});
			jm.add(jmi);

			jJMenuBar.add(getHelpMenu());

		}
		return jJMenuBar;
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getFileMenu() {
		if (fileMenu == null) {
			fileMenu = new JMenu();
			fileMenu.setText(Messages.getString("GTDFree.File")); //$NON-NLS-1$
			
			fileMenu.add(getSaveMenuItem());

			autoSaveMenuItem= new JCheckBoxMenuItem();
			autoSaveMenuItem.setText(Messages.getString("GTDFree.File.ASave")); //$NON-NLS-1$
			
			if (getEngine().getGTDModel().getDataRepository() instanceof GTDDataXML) {
				autoSaveMenuItem.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.AUTO_SAVE , true));
			} else {
				autoSaveMenuItem.setSelected(true);
				autoSaveMenuItem.setEnabled(false);
			}
			autoSaveMenuItem.setToolTipText(Messages.getString("GTDFree.File.ASave.desc")); //$NON-NLS-1$
			autoSaveMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getEngine().setAutoSave(autoSaveMenuItem.isSelected());
					getEngine().getGlobalProperties().putProperty(GlobalProperties.AUTO_SAVE, autoSaveMenuItem.isSelected());
				}
			});
			fileMenu.add(autoSaveMenuItem);
			
			/*fileMenu.add(new JSeparator());
			
			printCurrentMenuItem= new JMenuItem();
			printCurrentMenuItem.setText("Print current view...");
			printCurrentMenuItem.setEnabled(false);
			printCurrentMenuItem.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					WorkflowPane p= getEngine().getActiveWorkflowPane();
					if (p!=null) {
						try {
							p.printTable();
						} catch (PrinterException e1) {
							e1.printStackTrace();
							JOptionPane.showMessageDialog(getJFrame(), "Printing failed, error: "+e1.toString(), "Printing failed", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			});
			
			getEngine().addPropertyChangeListener("activePane", new PropertyChangeListener() {
			
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					printCurrentMenuItem.setEnabled(getEngine().getActiveWorkflowPane()!=null);
				}
			});
			
			fileMenu.add(printCurrentMenuItem);*/
			
			fileMenu.add(new JSeparator());

			JMenuItem jmi= new JMenuItem(Messages.getString("GTDFree.File.Print")); //$NON-NLS-1$
			jmi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_print));
			jmi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getPrintDialog().setVisible(true);
				}
			});
			fileMenu.add(jmi);

			
			fileMenu.add(new JSeparator());
			
			jmi= new JMenuItem(Messages.getString("GTDFree.File.Import")); //$NON-NLS-1$
			jmi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_import));
			jmi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					importFile();
				}
			});
			fileMenu.add(jmi);
			
			jmi= new JMenuItem(Messages.getString("GTDFree.File.Export")); //$NON-NLS-1$
			jmi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_export));
			jmi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					getExportDialog().setVisible(true);
				}
			});
			fileMenu.add(jmi);

			jmi= new JMenuItem(Messages.getString("GTDFree.File.All")); //$NON-NLS-1$
			jmi.setToolTipText(Messages.getString("GTDFree.File.All.desc")); //$NON-NLS-1$
			jmi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_export));
			jmi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					exportFile();
				}
			});
			fileMenu.add(jmi);

			fileMenu.add(new JSeparator());
			fileMenu.add(getExitMenuItem());
		}
		return fileMenu;
	}


	private ExportDialog getExportDialog() {
		if (exportDialog == null) {
			exportDialog = new ExportDialog(getJFrame());
			exportDialog.setEngine(getEngine());
			exportDialog.setLocationRelativeTo(getJFrame());
		}

		return exportDialog;
	}

	private ExportDialog getPrintDialog() {
		if (printDialog == null) {
			printDialog = new ExportDialog(getJFrame(),true);
			printDialog.setEngine(getEngine());
			printDialog.setLocationRelativeTo(getJFrame());
		}

		return printDialog;
	}

	private void importFile() {
		if (JFileChooser.APPROVE_OPTION==getFileChooser().showOpenDialog(getJFrame())) {
			
			try {
				getEngine().getGTDModel().importXMLFile(getFileChooser().getSelectedFile());
				logger.info("File imported '"+getFileChooser().getSelectedFile()+"'."); //$NON-NLS-1$ //$NON-NLS-2$
				JOptionPane.showMessageDialog(getJFrame(), Messages.getString("GTDFree.Import.OK.1")+getFileChooser().getSelectedFile()+Messages.getString("GTDFree.Import.OK.2"), Messages.getString("GTDFree.Import.OK.title"), JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} catch (Exception e) {
				logger.error("File import '"+getFileChooser().getSelectedFile()+"' faled.",e); //$NON-NLS-1$ //$NON-NLS-2$
				JOptionPane.showMessageDialog(getJFrame(), Messages.getString("GTDFree.Import.Fail.1")+getFileChooser().getSelectedFile()+Messages.getString("GTDFree.Import.Fail.2")+e.getMessage(), Messages.getString("GTDFree.Import.Fail.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

	private void exportFile() {
		if (JFileChooser.APPROVE_OPTION==getFileChooser().showSaveDialog(getJFrame())) {
			
			File f= getFileChooser().getSelectedFile();
			try {
				if (!f.getName().toLowerCase().endsWith(".xml")) { //$NON-NLS-1$
					f= new File(f.toString()+".xml"); //$NON-NLS-1$
				}
				getEngine().getGTDModel().exportXML(f);
				logger.info("File exported '"+f+"'."); //$NON-NLS-1$ //$NON-NLS-2$
				JOptionPane.showMessageDialog(getJFrame(), Messages.getString("GTDFree.Export.OK.1")+f+Messages.getString("GTDFree.Export.OK.2"), Messages.getString("GTDFree.Export.OK.title"), JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} catch (Exception e) {
				logger.error("File export '"+f+"' faled.",e); //$NON-NLS-1$ //$NON-NLS-2$
				JOptionPane.showMessageDialog(getJFrame(), Messages.getString("GTDFree.Export.Fail.1")+f+Messages.getString("GTDFree.Export.Fail.2")+e.getMessage(), Messages.getString("GTDFree.Export.Fail.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
	}

	/**
	 * This method initializes jMenu	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getHelpMenu() {
		if (helpMenu == null) {
			helpMenu = new JMenu();
			helpMenu.setText(Messages.getString("GTDFree.Help")); //$NON-NLS-1$
			
			JMenuItem jmi= new JMenuItem(Messages.getString("GTDFree.Help.Home")); //$NON-NLS-1$
			jmi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_browser));
			jmi.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					URI uri;
					try {
						uri = new URI(getEngine().getConfiguration().getProperty("home.url", "http://gtd-free.sourceforge.net")); //$NON-NLS-1$ //$NON-NLS-2$
						Desktop.getDesktop().browse(uri);
					} catch (Exception e1) {
						Logger.getLogger(this.getClass()).error("URL load failed.", e1); //$NON-NLS-1$
					}
				}
			});
			helpMenu.add(jmi);
			
			jmi= new JMenuItem(Messages.getString("GTDFree.Help.Manuals")); //$NON-NLS-1$
			jmi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_browser));
			jmi.addActionListener(new ActionListener() {
			
				@Override
				public void actionPerformed(ActionEvent e) {
					URI uri;
					try {
						uri = new URI(getEngine().getConfiguration().getProperty("manuals.url", "http://gtd-free.sourceforge.net/manuals.html")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						Desktop.getDesktop().browse(uri);
					} catch (Exception e1) {
						Logger.getLogger(this.getClass()).error("URL load failed.", e1); //$NON-NLS-1$
					}
				}
			});
			helpMenu.add(jmi);

			jmi= new JMenuItem(getActionMap().get("importDialog")); //$NON-NLS-1$
			helpMenu.add(jmi);

			helpMenu.add(new JSeparator());
			
			jmi= new JMenuItem();
			jmi.setText(Messages.getString("GTDFree.Check")); //$NON-NLS-1$
			jmi.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_update));
			jmi.addActionListener(new ActionListener() {
				
				@Override
			
				public void actionPerformed(ActionEvent e) {
					new Thread() {
						@Override
						public void run() {
							checkForUpdates(true);
						}
					}.start();
				}
			});
			helpMenu.add(jmi);

			JCheckBoxMenuItem jcbmi= new JCheckBoxMenuItem();
			jcbmi.setText(Messages.getString("GTDFree.CheckAtStartup")); //$NON-NLS-1$
			try {
				getEngine().getGlobalProperties().connectBooleanProperty(GlobalProperties.CHECK_FOR_UPDATE_AT_START,jcbmi,"selected","isSelected","setSelected",true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} catch (Exception e1) {
				logger.debug("Internal error.", e1); //$NON-NLS-1$
			}
			helpMenu.add(jcbmi);

			helpMenu.add(new JSeparator());

			jmi= new JMenuItem();
			jmi.setText(Messages.getString("GTDFree.Help.Mon")); //$NON-NLS-1$
			jmi.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					getMonitor().getDialog().setVisible(true);
				}
			});
			helpMenu.add(jmi);
			
			helpMenu.add(new JSeparator());

			helpMenu.add(getAboutMenuItem());
		}
		return helpMenu;
	}
	
	
	private Monitor getMonitor() {
		if (monitor==null) {
			monitor= new Monitor();
		}
		return monitor;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getExitMenuItem() {
		if (exitMenuItem == null) {
			exitMenuItem = new JMenuItem();
			exitMenuItem.setText(Messages.getString("GTDFree.File.Exit")); //$NON-NLS-1$
			exitMenuItem.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_exit));
			exitMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					close(false);
				}
			});
		}
		return exitMenuItem;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getAboutMenuItem() {
		if (aboutMenuItem == null) {
			aboutMenuItem = new JMenuItem();
			aboutMenuItem.setText(Messages.getString("GTDFree.Help.About")); //$NON-NLS-1$
			aboutMenuItem.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_about));
			aboutMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JDialog aboutDialog = getAboutDialog();
					//aboutDialog.pack();
					Point loc = getJFrame().getLocation();
					loc.translate(20, 20);
					aboutDialog.setLocation(loc);
					aboutDialog.setVisible(true);
				}
			});
		}
		return aboutMenuItem;
	}

	/**
	 * This method initializes aboutDialog	
	 * 	
	 * @return javax.swing.JDialog
	 */
	private JDialog getAboutDialog() {
		if (aboutDialog == null) {
			aboutDialog = new JDialog(getJFrame(), true);
			aboutDialog.setTitle(Messages.getString("GTDFree.About.Free")); //$NON-NLS-1$
			Image i= ApplicationHelper.loadImage(ApplicationHelper.icon_name_large_logo); //$NON-NLS-1$
			ImageIcon ii= new ImageIcon(i);
			
			JTabbedPane jtp= new JTabbedPane();
			
			JPanel jp= new JPanel();
			jp.setLayout(new GridBagLayout());
			JLabel jl= new JLabel("GTD-Free",ii,SwingConstants.CENTER); //$NON-NLS-1$
			jl.setIconTextGap(22);
			jl.setFont(jl.getFont().deriveFont((float)24));
			jp.add(jl, new GridBagConstraints(0,0,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(11,11,11,11),0,0));
			String s= "Version "+ApplicationHelper.getVersion(); //$NON-NLS-1$
			jp.add(new JLabel(s,SwingConstants.CENTER), new GridBagConstraints(0,1,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(4,11,4,11),0,0));
			s= Messages.getString("GTDFree.About.DBType")+getEngine().getGTDModel().getDataRepository().getDatabaseType(); //$NON-NLS-1$
			jp.add(new JLabel(s,SwingConstants.CENTER), new GridBagConstraints(0,2,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(11,11,2,11),0,0));
			s= Messages.getString("GTDFree.About.DBloc")+getEngine().getDataFolder(); //$NON-NLS-1$
			jp.add(new JLabel(s,SwingConstants.CENTER), new GridBagConstraints(0,3,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(2,11,4,11),0,0));
			jp.add(new JLabel("Copyright © 2008,2009 ikesan@users.sourceforge.net",SwingConstants.CENTER), new GridBagConstraints(0,4,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(11,11,11,11),0,0)); //$NON-NLS-1$
			jtp.addTab("About", jp); //$NON-NLS-1$
			
			jp= new JPanel();
			jp.setLayout(new GridBagLayout());
			TableModel tm= new AbstractTableModel() {
				private static final long serialVersionUID = -8449423008172417278L;
				private String[] props;
				private String[] getProperties() {
					if (props==null) {
						props= System.getProperties().keySet().toArray(new String[System.getProperties().size()]);
						Arrays.sort(props);
					}
					return props;
				}
				@Override
				public String getColumnName(int column) {
					switch (column) {
						case 0:
							return Messages.getString("GTDFree.About.Prop"); //$NON-NLS-1$
						case 1:
							return Messages.getString("GTDFree.About.Val"); //$NON-NLS-1$
						default:
							return null;
					}
				}
				public int getColumnCount() {
					return 2;
				}
				public int getRowCount() {
					return getProperties().length;
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					switch (columnIndex) {
						case 0:
							return getProperties()[rowIndex];
						case 1:
							return System.getProperty(getProperties()[rowIndex]);
						default:
							return null;
					}
				}
			};
			JTable jt= new JTable(tm);
			jp.add(new JScrollPane(jt), new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH, new Insets(11,11,11,11),0,0));
			jtp.addTab(Messages.getString("GTDFree.About.SysP"), jp); //$NON-NLS-1$
			
			jp= new JPanel();
			jp.setLayout(new GridBagLayout());
			JTextArea ta= new JTextArea();
			ta.setEditable(false);
			ta.setText(ApplicationHelper.loadLicense());
			ta.setCaretPosition(0);
			jp.add(new JScrollPane(ta),new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(11,11,11,11),0,0));
			jtp.addTab("License", jp); //$NON-NLS-1$
			
			aboutDialog.setContentPane(jtp);
			aboutDialog.setSize(550, 300);
			//aboutDialog.pack();
			aboutDialog.setLocationRelativeTo(getJFrame());
		}
		return aboutDialog;
	}

	/**
	 * This method initializes jMenuItem	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getSaveMenuItem() {
		if (saveMenuItem == null) {
			saveMenuItem = new JMenuItem();
			saveMenuItem.setText(Messages.getString("GTDFree.File.Save")); //$NON-NLS-1$
			saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
					Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(), true));
			saveMenuItem.setIcon(ApplicationHelper.getIcon(ApplicationHelper.icon_name_large_save));
			saveMenuItem.addActionListener(e -> {
				try {
					getEngine().save();
					Logger.getLogger(this.getClass()).debug("Save successful."); //$NON-NLS-1$
				} catch (Exception ex) {
					Logger.getLogger(this.getClass()).error("Save failed.", ex); //$NON-NLS-1$
					JOptionPane.showMessageDialog(getJFrame(), "Save failed: '"+ex.getMessage()+"'.", "Save Failed", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			});
		}
		return saveMenuItem;
	}
	
	public GTDFreeEngine getEngine() {
		if (engine == null) {
			try {
				engine = new GTDFreeEngine();
				engine.addPropertyChangeListener("aborting", new PropertyChangeListener() { //$NON-NLS-1$
					
					@Override
					public void propertyChange(PropertyChangeEvent evt) {
						aborting();
					}
				});
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).fatal("Initialization failed.", e); //$NON-NLS-1$
				close(true);
			}
		}
		return engine;
	}
	
	private JFileChooser getFileChooser() {
		if (fileChooser==null) {
			fileChooser= new JFileChooser();
			fileChooser.setCurrentDirectory(new File(System.getProperty("user.home", "."))); //$NON-NLS-1$ //$NON-NLS-2$
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setFileHidingEnabled(false);
			fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("GTDFree.FileFilter"),"xml")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return fileChooser;
	}
	
	private JCheckBoxMenuItem getShowAllActionsMenuItem() {
		if (showAllActions == null) {
			showAllActions = new JCheckBoxMenuItem(getActionMap().get(GlobalProperties.SHOW_ALL_ACTIONS));
			showAllActions.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
			getEngine().getGlobalProperties().addPropertyChangeListener(GlobalProperties.SHOW_ALL_ACTIONS, new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					showAllActions.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_ALL_ACTIONS));
				}
			});
		}
		return showAllActions;
	}

	private JCheckBoxMenuItem getShowClosedFoldersMenuItem() {
		if (showClosedFolders == null) {
			showClosedFolders = new JCheckBoxMenuItem(getActionMap().get(GlobalProperties.SHOW_CLOSED_FOLDERS));
			showClosedFolders.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_CLOSED_FOLDERS));
			getEngine().getGlobalProperties().addPropertyChangeListener(GlobalProperties.SHOW_CLOSED_FOLDERS, new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					showClosedFolders.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_CLOSED_FOLDERS));
				}
			});
		}
		return showClosedFolders;
	}
	private JCheckBoxMenuItem getShowEmptyFoldersMenuItem() {
		if (showEmptyFolders == null) {
			showEmptyFolders = new JCheckBoxMenuItem(getActionMap().get(GlobalProperties.SHOW_EMPTY_FOLDERS));
			showEmptyFolders.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_EMPTY_FOLDERS,true));
			getEngine().getGlobalProperties().addPropertyChangeListener(GlobalProperties.SHOW_EMPTY_FOLDERS, new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					showEmptyFolders.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_EMPTY_FOLDERS,true));
				}
			});
		}
		return showEmptyFolders;
	}

	private JCheckBoxMenuItem getShowOverviewTabMenuItem() {
		if (showOverviewTab == null) {
			showOverviewTab = new JCheckBoxMenuItem(getActionMap().get(GlobalProperties.SHOW_OVERVIEW_TAB));
			showOverviewTab.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_OVERVIEW_TAB,true));
			getEngine().getGlobalProperties().addPropertyChangeListener(GlobalProperties.SHOW_OVERVIEW_TAB, new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					showOverviewTab.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_OVERVIEW_TAB));
					
					if (getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_OVERVIEW_TAB)) {
						if (tabbedPane.getComponentAt(0)!=getOverviewPane()) {
							tabbedPane.insertTab(Messages.getString("GTDFree.Overview"), ApplicationHelper.getIcon(ApplicationHelper.icon_name_small_overview), getOverviewPane(), "", 0); //$NON-NLS-1$ //$NON-NLS-2$
						}
					} else {
						if (tabbedPane.getComponentAt(0)==getOverviewPane()) {
							tabbedPane.remove(0);
						}
					}
				}
			});
		}
		return showOverviewTab;
	}

	private JCheckBoxMenuItem getShowQuickCollectBarMenuItem() {
		if (showQuickCollectBar == null) {
			showQuickCollectBar = new JCheckBoxMenuItem(getActionMap().get(GlobalProperties.SHOW_QUICK_COLLECT));
			showQuickCollectBar.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_QUICK_COLLECT));
			getEngine().getGlobalProperties().addPropertyChangeListener(GlobalProperties.SHOW_QUICK_COLLECT, new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					showQuickCollectBar.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_QUICK_COLLECT));
					enableQuickCollectPanel();
				}
			});
		}
		return showQuickCollectBar;
	}

	private JCheckBoxMenuItem getShowTrayIconMenuItem() {
		if (showTrayIcon == null) {
			showTrayIcon = new JCheckBoxMenuItem(getActionMap().get(GlobalProperties.SHOW_TRAY_ICON));
			showTrayIcon.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_TRAY_ICON));
			getEngine().getGlobalProperties().addPropertyChangeListener(GlobalProperties.SHOW_TRAY_ICON, new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					showTrayIcon.setSelected(getEngine().getGlobalProperties().getBoolean(GlobalProperties.SHOW_TRAY_ICON));
				}
			});
		}
		return showTrayIcon;
	}

	/**
	 * 
	 */
	private void store(GlobalProperties p) {
		p.putProperty("window.heght", jFrame.getSize().height); //$NON-NLS-1$
		p.putProperty("window.width", jFrame.getSize().width); //$NON-NLS-1$
		p.putProperty("window.state", jFrame.getExtendedState()); //$NON-NLS-1$
		p.putProperty("window.position.x", jFrame.getLocation().x); //$NON-NLS-1$
		p.putProperty("window.position.y", jFrame.getLocation().y); //$NON-NLS-1$
		
		if (inBasketPane!=null && inBasketPane.isInitialized()) {
			inBasketPane.store(p);
		}
		if (processPane!=null && processPane.isInitialized()) {
			processPane.store(p);
		}
		if (organizePane!=null && organizePane.isInitialized()) {
			organizePane.store(p);
		}
		if (executePane!=null && executePane.isInitialized()) {
			executePane.store(p);
		}
	}

	/**
	 * @param application
	 * @throws IOException
	 */
	private void checkForUpdates(boolean notifyOnSame) {
		VersionInfo[] v=null;
		VersionInfo current= new VersionInfo(getEngine().getConfiguration());
		if (closed) {
			return;
		}
		
		v = getEngine().checkForNewVersions(current);
		
		if (closed) {
			return;
		}

		if (v.length==0) {
			if (notifyOnSame) {
				JOptionPane.showMessageDialog(getJFrame(), Messages.getString("GTDFree.UpToDate"), Messages.getString("GTDFree.UpdateCheck"), JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			StringBuilder sb= new StringBuilder();
			sb.append("<html><body><p>"); //$NON-NLS-1$
			sb.append(Messages.getString("GTDFree.Update.1")); //$NON-NLS-1$
			sb.append(" '"); //$NON-NLS-1$
			sb.append(current.toFullVersionString());
			sb.append("'. "); //$NON-NLS-1$
			if (v.length==1) {
				sb.append(Messages.getString("GTDFree.Update.2a")); //$NON-NLS-1$
			} else {
				sb.append(Messages.getString("GTDFree.Update.2b")); //$NON-NLS-1$
			}
			sb.append("</p><ul>"); //$NON-NLS-1$
			
			for (int i = 0; i < v.length; i++) {
				sb.append("<li>'"); //$NON-NLS-1$
				sb.append(v[i].toFullVersionString());
				if (v[i].notes!=null && v[i].notes.length()>0) {
					sb.append("' "); //$NON-NLS-1$
					sb.append(Messages.getString("GTDFree.Update.3")); //$NON-NLS-1$
					sb.append("<br/>'"); //$NON-NLS-1$
					sb.append(v[i].notes);
				}
				sb.append("'.</li>"); //$NON-NLS-1$
			}
			sb.append("</ul></body></html>"); //$NON-NLS-1$
			
			if (closed) {
				return;
			}
			
			int option= JOptionPane.showOptionDialog(
					getJFrame(),
					sb.toString(), 
					Messages.getString("GTDFree.UpdateCheck"),  //$NON-NLS-1$
					JOptionPane.OK_OPTION, 
					JOptionPane.INFORMATION_MESSAGE,
					null,
					new Object[]{Messages.getString("GTDFree.VisitDL"),Messages.getString("GTDFree.Close")}, //$NON-NLS-1$ //$NON-NLS-2$
					0);
			if (closed) {
				System.exit(0);
				return;
			}
			if (option==0) {
				try {
					Desktop.getDesktop().browse(new URI(getEngine().getConfiguration().getProperty("download.url", "http://gtd-free.sourceforge.net/download.html"))); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (Exception e) {
					logger.error(Messages.getString("GTDFree.LoadFailed"), e); //$NON-NLS-1$
				}
			}
		}
	}
	
	private void exportRemote() {
		
		if (stub!=null) {
			return;
		}
		
		ObjectOutputStream oos=null;
		try {
			stub = (GTDFreeOperations) UnicastRemoteObject.exportObject(this, 0);
			File f= new File(ApplicationHelper.getDataFolder(),STUB_FILE_NAME);
			f.delete();
			oos= new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
			oos.writeObject(stub);
			logger.debug("Remote connection exported."); //$NON-NLS-1$
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (oos!=null) {
				try {
					oos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void remotePushVisible() {
		ObjectInputStream ois=null;
		try {
			File f= new File(ApplicationHelper.getDataFolder(),STUB_FILE_NAME);
			if (f.exists()) {
				ois= new ObjectInputStream(new BufferedInputStream(new FileInputStream(f))); 
				Object o= ois.readObject();
				if (o instanceof GTDFreeOperations) {
					GTDFreeOperations stub = (GTDFreeOperations)o;
					((GTDFreeOperations)o).pushAppVisible();
					System.out.println("Remote push to visible called."); //$NON-NLS-1$
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ois!=null) {
				try {
					ois.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public boolean isClosed() {
		return closed;
	}
	
	@Override
	public void shutdown() {
		logger.info("Shutdown initiated by remote client."); //$NON-NLS-1$
		close(false);
	}
	
	@Override
	public void pushAppVisible() {
		logger.info("Made visible by remote client."); //$NON-NLS-1$
		pushVisible();
	}
	
	@Override
	public boolean isRunning() {
		return !getEngine().isAborting() && !isClosed();
	}
	
	@Override
	public String getDataLocation() {
		return ApplicationHelper.getDataFolder().toString();
	}
	
}
