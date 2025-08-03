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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dialog.ModalityType;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.ActionMap;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.gtdfree.addons.DefaultXMLExportAddOn;
import org.gtdfree.addons.ExportAddOn;
import org.gtdfree.addons.HTMLExportAddOn;
import org.gtdfree.addons.PDFExportAddOn;
import org.gtdfree.addons.PlainTextExportAddOn;
import org.gtdfree.gui.DatabaseSelectionDialog;
import org.gtdfree.gui.GTDFreePane;
import org.gtdfree.gui.StateMachine;
import org.gtdfree.gui.WorkflowPane;
import org.gtdfree.journal.JournalModel;
import org.gtdfree.model.GTDDataODB;
import org.gtdfree.model.GTDDataXML;
import org.gtdfree.model.GTDModel;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


public class GTDFreeEngine {
	
	static public class VersionInfo {
		public String version;
		public String type;
		public String notes;
		
		/**
		 * @param version
		 * @param type
		 * @param notes
		 */
		public VersionInfo(String version, String type, String notes) {
			super();
			this.version = version;
			this.type = type;
			this.notes = notes;
		}
		
		public VersionInfo(Properties config) {
			this.version = config.getProperty("build.version"); //$NON-NLS-1$
			this.type = config.getProperty("build.type"); //$NON-NLS-1$
		}
		
		public boolean isLaterThen(VersionInfo vinfo) {
			 return version.compareTo(vinfo.version)<0;
		}
		
		public String toFullVersionString() {
			StringBuilder sb= new StringBuilder(16);
			sb.append(version);
			if (type!=null && type.length()>0) {
				sb.append('-');
				sb.append(type);
			}
			return sb.toString();
		}
	}

	
	
	volatile private GTDModel gtdModel;
	private Properties configuration;
	private GlobalProperties globalProperties;

	private JournalModel journalModel;
	private StateMachine stateMachine;
	private ActionMap actionMap;
	private boolean aborting= false;
	private List<ExportAddOn> exportAddOns;
	private GTDFreePane activePane;
	private PropertyChangeSupport support= new PropertyChangeSupport(this);
	private JDialog upgradeImportDialog;
	private Logger logger= Logger.getLogger(GTDFreeEngine.class);
	
	public GTDFreeEngine() throws FileNotFoundException, XMLStreamException, FactoryConfigurationError, MalformedURLException {
	}
	
	public Properties getConfiguration() {
		if (configuration==null) {
			configuration= ApplicationHelper.loadConfiguration();
		}
		return configuration;
	}
	
	public GTDModel getGTDModel() {
		if (gtdModel == null) {
			
			Object db= getGlobalProperties().getProperty(GlobalProperties.DATABASE);
			
			boolean doImport=false;
			
			if (db==null 
					|| (!GlobalProperties.DATABASE_VALUE_ODB.equalsIgnoreCase(db.toString()) 
							&& !GlobalProperties.DATABASE_VALUE_XML.equalsIgnoreCase(db.toString()))) {
				
				DatabaseSelectionDialog d= new DatabaseSelectionDialog(null);
				d.setVisible(true);
				
				if (!d.isSuccess()) {
					logger.info("Database selection canceled, closing."); //$NON-NLS-1$
					try {
						close(true,false);
					} catch (Exception e) {
						logger.error("Internal error.",e); //$NON-NLS-1$
					}
					System.exit(0);
				}
				
				db= d.getDatabase();
				doImport= d.isUpgrade();
				
				getGlobalProperties().putProperty(GlobalProperties.DATABASE, db);
			}
			
			if (GlobalProperties.DATABASE_VALUE_XML.equalsIgnoreCase(db.toString())) {
				GTDDataXML xml= new GTDDataXML(getDataFolder(),getGlobalProperties());
				try {
					gtdModel= xml.restore();
				} catch (IOException e) {
					Logger.getLogger(this.getClass()).fatal("Initialization error, closing.", e); //$NON-NLS-1$
					try {
						close(true,false);
					} catch (Exception ex) {
						Logger.getLogger(this.getClass()).error("Closing error.", ex); //$NON-NLS-1$
					}
					System.exit(0);
				}
			} else {
				GTDDataODB odb= new GTDDataODB(getDataFolder(),getGlobalProperties());
				try {
					gtdModel= odb.restore();
				} catch (IOException e) {
					Logger.getLogger(this.getClass()).fatal("Initialization error.", e); //$NON-NLS-1$
					try {
						close(true,false);
					} catch (Exception ex) {
						Logger.getLogger(this.getClass()).error("Closing error.", ex); //$NON-NLS-1$
					}
					System.exit(0);
				}
				
				if (doImport) {
					final GTDDataXML xml= new GTDDataXML(getDataFolder(),getGlobalProperties());
					
					new Thread("UpgradeImportThread") { //$NON-NLS-1$
						@Override
						public void run() {
							try {
								synchronized (GTDFreeEngine.this) {
									GTDModel gtdXml= xml.restore();
									gtdModel.importData(gtdXml);
								}
								getUpgradeImportDialog(null).dispose();
							} catch (IOException e) {
								getUpgradeImportDialog(null).dispose();
								Logger.getLogger(this.getClass()).error("I/O error.", e); //$NON-NLS-1$
								JOptionPane.showMessageDialog(
										null, 
										Messages.getString("GTDFreeEngine.XMLImport.1")+ //$NON-NLS-1$
										(e.getCause()!=null ? e.getCause().toString() : e.toString())+
										Messages.getString("GTDFreeEngine.XMLImport.2"), Messages.getString("GTDFreeEngine.XMLImport.Title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
							}
						};
					}.start();
					getUpgradeImportDialog(xml.getDataFile().toString()).setVisible(true);
					
				}
			}
			
			
			setAutoSave(getGlobalProperties().getBoolean(GlobalProperties.AUTO_SAVE , true));
		}

		return gtdModel;
	}
	
	
	private JDialog getUpgradeImportDialog(String file) {
		if (upgradeImportDialog == null) {
			upgradeImportDialog = new JDialog();
			upgradeImportDialog.setTitle(Messages.getString("GTDFreeEngine.Upgrade.title")); //$NON-NLS-1$
			
			JPanel p= new JPanel();
			p.setLayout(new GridBagLayout());
			p.add(new JLabel(Messages.getString("GTDFreeEngine.Upgrade.1")+file), new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(11,11,4,11),0,0)); //$NON-NLS-1$
			p.add(new JLabel(Messages.getString("GTDFreeEngine.Upgrade.2")), new GridBagConstraints(0,1,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(4,11,4,11),0,0)); //$NON-NLS-1$

			JProgressBar pb= new JProgressBar();
			pb.setIndeterminate(true);
			p.add(pb, new GridBagConstraints(0,2,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(4,11,11,11),0,0));
			
			upgradeImportDialog.setContentPane(p);
			upgradeImportDialog.pack();
			upgradeImportDialog.setLocationRelativeTo(null);
			upgradeImportDialog.setModalityType(ModalityType.APPLICATION_MODAL);
			upgradeImportDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			upgradeImportDialog.setResizable(false);
			/*upgradeImportDialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					
				}
			});*/
		}

		return upgradeImportDialog;
	}

	public JournalModel getJournalModel() {
		if (journalModel == null) {
			journalModel = new JournalModel(getDataFolder(), getGTDModel());
			/*if (file==null) {
				file=ApplicationHelper.getDefaultFile();
			}
			System.out.println("Using "+file);
			//File f= new File(file.getAbsoluteFile()+".bak");
			if (file.exists()) {
				try {
					gtdModel.load(file);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (XMLStreamException e) {
					e.printStackTrace();
				} catch (FactoryConfigurationError e) {
					e.printStackTrace();
				}
			}*/
		}

		return journalModel;
	}

	public void save() throws IOException, XMLStreamException, FactoryConfigurationError {
		if (isAborting()) {
			return;
		}
		
		gtdModel.getDataRepository().flush();
		
	}

	public void emergencySave() {
		if (gtdModel==null) {
			return;
		}
		File save = ApplicationHelper.getShutdownBackupXMLFile();
		if (save.exists() && !save.delete()) {
			new IOException(Messages.getString("GTDFreeEngine.BackupRemoveFailed.1")+save.getAbsolutePath()+Messages.getString("GTDFreeEngine.BackupRemoveFailed.2")).toString(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		try {
			gtdModel.exportXML(save);
			
			logger.info(Messages.getString("GTDFreeEngine.BackupSaved")+save.getAbsolutePath()); //$NON-NLS-1$
		} catch (Exception e) {
			logger.error(Messages.getString("GTDFreeEngine.BackupFailed")+save.getAbsolutePath(),e); //$NON-NLS-1$
		}
	}
	
	/**
	 * @return the file
	 */
	public File getDataFolder() {
		return ApplicationHelper.getDataFolder();
	}

	/**
	 * @return the autoSave
	 */
	public boolean isAutoSave() {
		if (gtdModel!=null && gtdModel.getDataRepository() instanceof GTDDataXML) {
			return ((GTDDataXML)gtdModel.getDataRepository()).isAutoSave();
		}
		return true;
	}

	/**
	 * @param autoSave the autoSave to set
	 */
	public void setAutoSave(boolean autoSave) {
		if (gtdModel!=null && gtdModel.getDataRepository() instanceof GTDDataXML) {
			((GTDDataXML)gtdModel.getDataRepository()).setAutoSave(autoSave);
		}
	}
	
	public GlobalProperties getGlobalProperties() {
		if (globalProperties==null) {
			globalProperties= new GlobalProperties();
			
			File f= new File(getDataFolder(),ApplicationHelper.OPTIONS_FILE_NAME);
			if (f.exists()) {
				BufferedReader r=null;
				
				try {
					r= new BufferedReader(new FileReader(f));
					
					globalProperties.load(r);
					
				} catch (IOException e) {
					Logger.getLogger(this.getClass()).error("Initialization error.", e); //$NON-NLS-1$
				} finally {
					try {
						if (r!=null) {
							r.close();
						}
					} catch (IOException e) {
						// ignore
					}
				}
			}
		}
		return globalProperties;
	}
	
	/**
	 * 
	 * @param terminal if <code>true</code> then close procedure can not be aborted 
	 * @return <code>true</code> if close can proceed, <code>false</code> if close should be aborted 
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws FactoryConfigurationError
	 */
	public boolean close(final boolean terminal, boolean emergencySave) throws IOException, XMLStreamException, FactoryConfigurationError {
		
		if (aborting) {
			return true;
		}

		if (emergencySave || getGlobalProperties().getBoolean(GlobalProperties.SHUTDOWN_BACKUP_XML, true)) {
			emergencySave();
		}
		
		if (gtdModel!=null) {
			try {
				boolean close= gtdModel.getDataRepository().close(terminal);
				if (!close && !terminal) {
					return false;
				}
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).error("Close error.", e); //$NON-NLS-1$
			}
		}

		aborting=true;
		firePropertyChange("aborting", true, false); //$NON-NLS-1$
		
		File f= new File(getDataFolder(),ApplicationHelper.OPTIONS_FILE_NAME);
		BufferedWriter w=null;
		try {
			w= new BufferedWriter(new FileWriter(f));
			globalProperties.store(w);	
		} catch (IOException e) {
			Logger.getLogger(this.getClass()).error("Close error.", e); //$NON-NLS-1$
		} finally {
			try {
				if (w!=null) {
					w.close();
				}
			} catch (IOException e) {
				// ignore
			}
		}
		
		return true;
		
	}

	public StateMachine getStateMachine() {
		if (stateMachine == null) {
			stateMachine = new StateMachine();
		}
		return stateMachine;
	}
	
	public ActionMap getActionMap() {
		if (actionMap == null) {
			actionMap = new ActionMap();
			
		}
		return actionMap;
	}
	
	public boolean isAborting() {
		return aborting;
	}
	
	public ExportAddOn[] getExportAddOns() {
		if (exportAddOns==null) {
			exportAddOns= new ArrayList<ExportAddOn>(10);
			exportAddOns.add(new DefaultXMLExportAddOn());
			exportAddOns.add(new PDFExportAddOn());
			exportAddOns.add(new HTMLExportAddOn());
			exportAddOns.add(new PlainTextExportAddOn());
		}
		return exportAddOns.toArray(new ExportAddOn[exportAddOns.size()]);
	}

	public void setActivePane(GTDFreePane c) {
		if (c == activePane) {
			return;
		}
		GTDFreePane old= activePane;
		activePane= c;
		firePropertyChange("activePane",old,activePane); //$NON-NLS-1$
	}

	private void firePropertyChange(String name, Object old,
			Object val) {
		support.firePropertyChange(name, old, val);
	}
	
	public GTDFreePane getActivePane() {
		return activePane;
	}
	
	public WorkflowPane getActiveWorkflowPane() {
		if (activePane instanceof WorkflowPane) {
			return (WorkflowPane) activePane;
		}
		return null;
	}
	
	public void addPropertyChangeListener(String name, PropertyChangeListener l) {
		support.addPropertyChangeListener(name, l);
	}
	
	public void removePropertyChangeListener(String name, PropertyChangeListener l) {
		support.removePropertyChangeListener(name, l);
	}
	
	public VersionInfo[] checkForNewVersions(VersionInfo current) throws IOException {
		
		List<VersionInfo> l= new ArrayList<VersionInfo>(2);
		URI uri=null;
		try {
			uri = new URI(getConfiguration().getProperty("version.url", "http://gtd-free.sourceforge.net")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (Exception e1) {
			Logger.getLogger(this.getClass()).error("URL load failed.", e1); //$NON-NLS-1$
		}
		if (uri!=null) {
			DocumentBuilder db;
			try {
				
				db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc= db.parse(uri.toString());

				VersionInfo[] inf= new VersionInfo[2];
				
				NodeList verL= doc.getElementsByTagName("version"); //$NON-NLS-1$
				NodeList typL= doc.getElementsByTagName("type"); //$NON-NLS-1$
				NodeList notL= doc.getElementsByTagName("notes"); //$NON-NLS-1$
				
				inf[0]= new VersionInfo(
						verL.item(0).getTextContent(),
						typL.item(0).getTextContent(),
						notL.item(0).getTextContent());
				
				inf[1]= new VersionInfo(
						verL.item(1).getTextContent(),
						typL.item(1).getTextContent(),
						notL.item(1).getTextContent());

				logger.debug("Remote versions: "+inf[0].toFullVersionString()+", "+inf[1].toFullVersionString()+"."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				if (current.isLaterThen(inf[0])) {
					l.add(inf[0]);
				}
				if (current.isLaterThen(inf[1])) {
					l.add(inf[1]);
				}
				
				return l.toArray(new VersionInfo[l.size()]);

			} catch (Exception e) {
				logger.error("Internal error.",e); //$NON-NLS-1$
			}
		}
		
		return new VersionInfo[0];
	}

}
