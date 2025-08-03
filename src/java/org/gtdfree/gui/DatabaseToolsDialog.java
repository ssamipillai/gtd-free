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

package org.gtdfree.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.gtdfree.ApplicationHelper;
import org.gtdfree.GTDFreeEngine;
import org.gtdfree.GlobalProperties;
import org.gtdfree.Messages;
import org.gtdfree.model.ConsistencyException;
import org.gtdfree.model.GTDData;
import org.gtdfree.model.GTDDataODB;
import org.gtdfree.model.GTDModel;

import com.lowagie.text.Font;

/**
 * @author ikesan
 *
 */
public class DatabaseToolsDialog {
	
	private JTextArea consistencyCheckText;
	private Logger consistencyCheckLogger;
	protected GTDFreeEngine engine;
	private JCheckBox repairCheckBox;
	private JLabel dbTypeLabel;
	private JDialog dialog;
	private JButton checkButton;
	private JButton changeButton;
	private boolean verifying;
	private JButton closeButton;
	private JTabbedPane mainTabs;
	private JFileChooser fileChooser;
	private JCheckBox exitBackupXmlCheckBox;
	private JCheckBox exitBackupOdbCheckBox;

	public static void main(String[] args) {
		
		DatabaseToolsDialog d= new DatabaseToolsDialog();
		d.showDatabaseToolsDialog(null);
		
	}
	
	public DatabaseToolsDialog() {
	}
	
	private JDialog getDialog(Component c) {
		
		if (dialog == null) {
			
			dialog= new JDialog();
			
			mainTabs= new JTabbedPane();
			
			
			JPanel jp = getOptionsTab();
			mainTabs.addTab(Messages.getString("DatabaseToolsDialog.Options"), jp); //$NON-NLS-1$

			jp = getToolsTab();
			mainTabs.addTab(Messages.getString("DatabaseToolsDialog.Verify"), jp); //$NON-NLS-1$

			jp = getOdbTab();
			mainTabs.addTab(Messages.getString("DatabaseToolsDialog.ODB"), jp); //$NON-NLS-1$

			mainTabs.setEnabledAt(2, getGTDDataODB()!=null);

			jp= new JPanel();
			jp.setLayout(new GridBagLayout());
			int row=0;
			
			jp.add(mainTabs,new GridBagConstraints(0,row,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));

			closeButton= new JButton(Messages.getString("DatabaseToolsDialog.Close")); //$NON-NLS-1$
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					closeDialog();
				}
			});
			jp.add(closeButton,new GridBagConstraints(0,++row,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(11,11,11,11),0,0));

			dialog.setContentPane(jp);
			dialog.setSize(500,400);
			dialog.setTitle(Messages.getString("DatabaseToolsDialog.DBTools")); //$NON-NLS-1$
			dialog.setModal(true);
			dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			dialog.setLocationRelativeTo(c);
			dialog.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					closeDialog();
				}
			});
		}
		
		return dialog;
		
		
		
	}

	/**
	 * @return
	 */
	private JPanel getOdbTab() {
		JPanel jp;
		jp= new JPanel();
		jp.setLayout(new GridBagLayout());
		int row=0;
		
		JTextArea ta= new JTextArea();
		ta.setEditable(false);
		ta.setEnabled(false);
		ta.setWrapStyleWord(true);
		ta.setLineWrap(true);
		ta.setDisabledTextColor(ta.getForeground());
		ta.setBorder(null);
		ta.setFont(ta.getFont().deriveFont(Font.ITALIC));
		ta.setText(Messages.getString("DatabaseToolsDialog.Tools.desc")); //$NON-NLS-1$
		jp.add(ta,new GridBagConstraints(0,row,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH, new Insets(11,11,11,11),0,0));

		JButton b= new JButton();
		b.setText(Messages.getString("DatabaseToolsDialog.Exp")); //$NON-NLS-1$
		b.setToolTipText(Messages.getString("DatabaseToolsDialog.Exp.desc")); //$NON-NLS-1$
		b.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				//int option= JOptionPane.showConfirmDialog(dialog, "Exporting all data to ODB database file\nAfter export GTD-Free will be closed and you will have to restart.\n\nDo you want to continue?", "Warning! Restart is required.", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				//if (option!=JOptionPane.OK_OPTION) {
				//	return ;
				//}

				JFileChooser fc= getFileChooser();
				fc.showSaveDialog(dialog);
				
				File f=fc.getSelectedFile(); 
				if (f!=null) {
					if (!f.getName().endsWith(".odb-xml")) { //$NON-NLS-1$
						f= new File(f.getAbsolutePath()+".odb-xml"); //$NON-NLS-1$
					}
					try {
						getGTDDataODB().exportODB(f);
						JOptionPane.showMessageDialog(dialog, Messages.getString("DatabaseToolsDialog.Exp.1")+f.getAbsolutePath()+Messages.getString("DatabaseToolsDialog.Exp.2"), Messages.getString("DatabaseToolsDialog.Exp.title"), JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						engine.close(true,false);
					} catch (Exception e1) {
						org.apache.log4j.Logger.getLogger(this.getClass()).error("Export error.", e1); //$NON-NLS-1$
						JOptionPane.showMessageDialog(dialog, Messages.getString("DatabaseToolsDialog.Exp.Fail.1")+" "+e1.toString(), Messages.getString("DatabaseToolsDialog.Exp.Fail.2"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}
		});
		jp.add(b,new GridBagConstraints(0,++row,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE, new Insets(4,11,11,4),0,0));
		
		b= new JButton();
		b.setText(Messages.getString("DatabaseToolsDialog.Load")); //$NON-NLS-1$
		b.setToolTipText(Messages.getString("DatabaseToolsDialog.Load.desc")); //$NON-NLS-1$
		b.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				int option= JOptionPane.showConfirmDialog(dialog, Messages.getString("DatabaseToolsDialog.Load.Warn"), Messages.getString("DatabaseToolsDialog.Load.Warn.title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
				if (option!=JOptionPane.OK_OPTION) {
					return ;
				}
				
				JFileChooser fc= getFileChooser();
				int code= fc.showOpenDialog(dialog);
				
				File f=fc.getSelectedFile(); 
				if (f!=null && code==JFileChooser.APPROVE_OPTION) {
					try {
						getGTDDataODB().importODB(f);
						JOptionPane.showMessageDialog(dialog, Messages.getString("DatabaseToolsDialog.Load.OK"), Messages.getString("DatabaseToolsDialog.Load.OK.title"), JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
						engine.close(true,false);
					} catch (Exception e1) {
						org.apache.log4j.Logger.getLogger(this.getClass()).error("Import error.", e1); //$NON-NLS-1$
						JOptionPane.showMessageDialog(dialog, Messages.getString("DatabaseToolsDialog.Load.Fail")+" "+e1.toString(), Messages.getString("DatabaseToolsDialog.Load.Fail.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}
		});
		jp.add(b,new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE, new Insets(4,4,11,11),0,0));
		return jp;
	}

	/**
	 * @return
	 */
	private JPanel getToolsTab() {
		JPanel jp= new JPanel();
		jp.setLayout(new GridBagLayout());
		
		int row=0;
		
		consistencyCheckText= new JTextArea();
		consistencyCheckText.setEditable(false);
		jp.add(new JScrollPane(consistencyCheckText),new GridBagConstraints(0,++row,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(11,11,4,11),0,0));
		
		consistencyCheckLogger = Logger.getAnonymousLogger();
		consistencyCheckLogger.addHandler(new Handler() {
		
			@Override
			public void publish(LogRecord record) {
				consistencyCheckText.append(record.getMessage());
				consistencyCheckText.append("\n"); //$NON-NLS-1$
			}
		
			@Override
			public void flush() {
				//
			}
		
			@Override
			public void close() throws SecurityException {
				//
			}
		});
		
		repairCheckBox = new JCheckBox();
		repairCheckBox.setText(Messages.getString("DatabaseToolsDialog.Fix")); //$NON-NLS-1$
		jp.add(repairCheckBox,new GridBagConstraints(0,++row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(4,11,11,4),0,0));
		
		checkButton= new JButton(Messages.getString("DatabaseToolsDialog.Check")); //$NON-NLS-1$
		checkButton.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				startVerify();
			}

		});
		jp.add(checkButton,new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(4,4,11,11),0,0));
		return jp;
	}
	
	/**
	 * @return
	 */
	private JPanel getOptionsTab() {
		JPanel jp= new JPanel();
		jp.setLayout(new GridBagLayout());
		
		int row=0;
		
		JLabel l= new JLabel();
		String s= Messages.getString("DatabaseToolsDialog.Loc")+" "+(engine!=null ? engine.getDataFolder() : Messages.getString("DatabaseToolsDialog.NA")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		l.setText(s);
		l.setToolTipText(s);
		jp.add(l,new GridBagConstraints(0,row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(11,11,4,11),0,0));

		dbTypeLabel= new JLabel();
		dbTypeLabel.setText(Messages.getString("DatabaseToolsDialog.Type")+" "+(engine!=null ? engine.getGTDModel().getDataRepository().getDatabaseType() : Messages.getString("DatabaseToolsDialog.NA"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		jp.add(dbTypeLabel,new GridBagConstraints(0,++row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL, new Insets(4,11,4,4),0,0));

		changeButton= new JButton();
		changeButton.setText(Messages.getString("DatabaseToolsDialog.Change")); //$NON-NLS-1$
		changeButton.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent e) {
				showDatabaseTypeChangeDialog(dialog);
			}
		});
		jp.add(changeButton,new GridBagConstraints(1,row,1,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE, new Insets(4,4,4,11),0,0));
		
		exitBackupXmlCheckBox= new JCheckBox();
		s= Messages.getString("DatabaseToolsDialog.ShutdownBackupXml")+" ("+ApplicationHelper.getShutdownBackupXMLFile().toString()+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		exitBackupXmlCheckBox.setText(s);
		exitBackupXmlCheckBox.setToolTipText(s);
		if (engine!=null) {
			exitBackupXmlCheckBox.setSelected(engine.getGlobalProperties().getBoolean(GlobalProperties.SHUTDOWN_BACKUP_XML, true));
		}
		exitBackupXmlCheckBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				engine.getGlobalProperties().putProperty(GlobalProperties.SHUTDOWN_BACKUP_XML, exitBackupXmlCheckBox.isSelected());
			}
		});
		jp.add(exitBackupXmlCheckBox,new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(4,11,4,11),0,0));
		
		exitBackupOdbCheckBox= new JCheckBox();
		if (engine!=null && GTDDataODB.isUsed(engine.getGTDModel())) {
			s= Messages.getString("DatabaseToolsDialog.ShutdownBackupOdb")+" ("+getGTDDataODB().getShutdownBackupFile().toString()+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exitBackupOdbCheckBox.setText(s);
			exitBackupOdbCheckBox.setToolTipText(s);
			exitBackupOdbCheckBox.setSelected(engine.getGlobalProperties().getBoolean(GlobalProperties.SHUTDOWN_BACKUP_ODB, true));
		} else {
			s= Messages.getString("DatabaseToolsDialog.ShutdownBackupOdb")+" ("+Messages.getString("DatabaseToolsDialog.NA")+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			exitBackupOdbCheckBox.setText(s);
			exitBackupOdbCheckBox.setToolTipText(s);
			exitBackupOdbCheckBox.setEnabled(false);
		}
		exitBackupOdbCheckBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				engine.getGlobalProperties().putProperty(GlobalProperties.SHUTDOWN_BACKUP_ODB, exitBackupOdbCheckBox.isSelected());
			}
		});
		jp.add(exitBackupOdbCheckBox,new GridBagConstraints(0,++row,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(4,11,4,11),0,0));

		jp.add(new JPanel(),new GridBagConstraints(0,++row,2,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL, new Insets(4,11,4,11),0,0));

		return jp;
	}
	
	protected GTDDataODB getGTDDataODB() {
		if (engine==null) {
			return null;
		}
		GTDData d= engine.getGTDModel().getDataRepository();
		if (d instanceof GTDDataODB) {
			return (GTDDataODB)d;
		}
		return null;
	}

	protected JFileChooser getFileChooser() {
		if (fileChooser == null) {
			fileChooser = new JFileChooser();
			fileChooser.setCurrentDirectory(new File(System.getProperty("user.home", "."))); //$NON-NLS-1$ //$NON-NLS-2$
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setFileHidingEnabled(false);
			fileChooser.setAcceptAllFileFilterUsed(true);
			fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("DatabaseToolsDialog.ODBFile"),"odb-xml")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return fileChooser;
	}

	public void showDatabaseTypeChangeDialog(Component owner) {
		Object[] options= new Object[]{Messages.getString("DatabaseToolsDialog.Sched"),Messages.getString("DatabaseToolsDialog.Cancel")}; //$NON-NLS-1$ //$NON-NLS-2$
		
		int o= JOptionPane.showOptionDialog(owner, Messages.getString("DatabaseToolsDialog.Sched.desc"), Messages.getString("DatabaseToolsDialog.Sched.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]); //$NON-NLS-1$ //$NON-NLS-2$

		if (o==0) {
			engine.getGlobalProperties().putProperty(GlobalProperties.DATABASE, null);
		}
	}
	
	public void showDatabaseToolsDialog(Component owner) {
		
		JDialog d= getDialog(owner);
		d.setVisible(true);
		
	}
	

	public void setEngine(GTDFreeEngine engine) {
		this.engine = engine;
		
		if (exitBackupXmlCheckBox!=null) {
			exitBackupXmlCheckBox.setSelected(engine.getGlobalProperties().getBoolean(GlobalProperties.SHUTDOWN_BACKUP_XML, true));
		}
		if (exitBackupOdbCheckBox!=null && GTDDataODB.isUsed(engine.getGTDModel())) {
			exitBackupOdbCheckBox.setText(Messages.getString("DatabaseToolsDialog.ShutdownBackupOdb")+" ("+getGTDDataODB().getShutdownBackupFile().toString()+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			exitBackupOdbCheckBox.setSelected(engine.getGlobalProperties().getBoolean(GlobalProperties.SHUTDOWN_BACKUP_ODB, true));
		}

	}
	
	public void dispose() {
		if (dialog!=null) {
			dialog.dispose();
		}
	}
	
	private void setButtonsEnabled(boolean b) {
		changeButton.setEnabled(b);
		checkButton.setEnabled(b);
		closeButton.setEnabled(b);
		repairCheckBox.setEnabled(b);
		mainTabs.setEnabled(b);
	}
	
	public synchronized boolean startVerify() {
		
		if (verifying || engine==null) {
			return false;
		}
		
		verifying=true;
		
		setButtonsEnabled(false);
		
		new Thread() {
			@Override
			public void run() {
				try {
					
					boolean repair= repairCheckBox.isSelected();
					
					consistencyCheckText.setText(Messages.getString("DatabaseToolsDialog.Model")); //$NON-NLS-1$
					int size= consistencyCheckText.getDocument().getLength();
					try {
						GTDModel.checkConsistency(engine.getGTDModel(),consistencyCheckLogger,false,repair);
						if (size==consistencyCheckText.getDocument().getLength()) {
							consistencyCheckText.append(Messages.getString("DatabaseToolsDialog.Model.OK")); //$NON-NLS-1$
						}
					} catch (ConsistencyException e1) {
						consistencyCheckText.append(e1.toString());
						consistencyCheckText.setCaretPosition(0);
					}
					consistencyCheckText.append(Messages.getString("DatabaseToolsDialog.Model.Done")); //$NON-NLS-1$
					
					consistencyCheckText.append(Messages.getString("DatabaseToolsDialog.Stor")); //$NON-NLS-1$
					size= consistencyCheckText.getDocument().getLength();
					try {
						engine.getGTDModel().getDataRepository().checkConsistency(consistencyCheckLogger,false,repair);
						if (size==consistencyCheckText.getDocument().getLength()) {
							consistencyCheckText.append(Messages.getString("DatabaseToolsDialog.Stor.OK")); //$NON-NLS-1$
						}
					} catch (ConsistencyException e1) {
						consistencyCheckText.append(e1.toString());
						consistencyCheckText.setCaretPosition(0);
					}
					consistencyCheckText.append(Messages.getString("DatabaseToolsDialog.Stor.Done")); //$NON-NLS-1$

					
				} catch (Throwable e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).error("Verify error.", e); //$NON-NLS-1$

					consistencyCheckText.append(Messages.getString("DatabaseToolsDialog.Check.Fail")); //$NON-NLS-1$
					consistencyCheckText.append(Messages.getString("DatabaseToolsDialog.Check.Err")+" "); //$NON-NLS-1$ //$NON-NLS-2$
					
					StringWriter sw= new StringWriter();
					PrintWriter pw= new PrintWriter(sw);
					e.printStackTrace(pw);
					pw.flush();
					consistencyCheckText.append(sw.toString());

				} finally {
					synchronized (DatabaseToolsDialog.this) {
						verifying=false;
						setButtonsEnabled(true);
					}
				}
			}
		}.start();
		
		return true;
	}
	
	public synchronized void closeDialog() {
		if (verifying) {
			return;
		}
		if (dialog!=null) {
			dialog.dispose();
		}
	}
}
