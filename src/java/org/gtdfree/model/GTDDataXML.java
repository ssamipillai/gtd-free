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

package org.gtdfree.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.gtdfree.ApplicationHelper;
import org.gtdfree.GlobalProperties;
import org.gtdfree.Messages;
import org.gtdfree.model.Folder.FolderPreset;
import org.gtdfree.model.Folder.FolderType;


public class GTDDataXML implements GTDData {
	
	class SaveThread extends Thread implements GTDModelListener {
		boolean destroyed= false;
		public SaveThread() {
			super("GTDDataXML-SaveThread"); //$NON-NLS-1$
		}
		@Override
		public void run() {
			while (!destroyed) {
				if (changed) {
					try {
						flush();
					} catch (Exception e) {
						Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
					}
				}
				synchronized (this) {
					try {
						wait(60000);
					} catch (InterruptedException e) {
						Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
					}
				}
			}
		}
		public void elementAdded(FolderEvent a) {
			if (!a.isRecycled()) {
				notifySave();
			}
		}
		public void elementModified(ActionEvent a) {
			if (!a.isRecycled()) {
				notifySave();
			}
		}
		public void elementRemoved(FolderEvent a) {
			if (!a.isRecycled()) {
				notifySave();
			}
		}
		public void folderAdded(Folder folder) {
			notifySave();
		}
		public void folderModified(FolderEvent folder) {
			if (!folder.isRecycled()) {
				notifySave();
			}
		}
		public void folderRemoved(Folder folder) {
			notifySave();
		}
		public void orderChanged(Folder f) {
			notifySave();
		}
		public synchronized void notifySave() {
			logger.debug("Save ready."); //$NON-NLS-1$
			//Thread.dumpStack();
			changed=true;
			notify();
		}
		public synchronized void stopSave() {
			destroyed=true;
			notify();
		}
	}

	
	public class ActionProxyXML implements ActionProxy {
		private Action a;
		private Folder parent;
		
		public ActionProxyXML(Action a) {
			this.a=a;
		}
		
		public Action get() {
			return a;
		}
		@Override
		public int getId() {
			return a.getId();
		}
		@Override
		public void store() {
			notifyUpdate();
		}
		@Override
		public boolean equals(Object obj) {
			return obj instanceof ActionProxyXML && ((ActionProxyXML)obj).a==a;
		}
		
		@Override
		public int hashCode() {
			return a.hashCode();
		}
		@Override
		public void delete() {
			notifyUpdate();
		}
		@Override
		public Folder getParent() {
			return parent;
		}
		@Override
		public void setParent(Folder parent) {
			this.parent=parent;
		}
	}
	
	class FolderData implements FolderDataProxy {
		
		private class ProxyComparator implements Comparator<ActionProxy> {
			Comparator<Action> c;
			public ProxyComparator(Comparator<Action> c) {
				this.c=c;
			}
			@Override
			public int compare(ActionProxy o1, ActionProxy o2) {
				return c.compare(o1.get(), o2.get());
			}
		}

		private List<ActionProxy> actions= new ArrayList<ActionProxy>();
		private String description;
		private Date created;
		private Date resolved;
		private Date modified;
		
		public FolderData() {
		}
		
		/**
		 * @return the created
		 */
		public Date getCreated() {
			return created;
		}


		/**
		 * @param created the created to set
		 */
		public void setCreated(Date created) {
			this.created = created;
			modify();
			notifyUpdate();
		}


		/**
		 * @return the resolved
		 */
		public Date getResolved() {
			return resolved;
		}


		/**
		 * @param resolved the resolved to set
		 */
		public void setResolved(Date resolved) {
			this.resolved = resolved;
			modify();
			notifyUpdate();
		}


		/**
		 * @return the modified
		 */
		public Date getModified() {
			return modified;
		}
		
		/**
		 * @param modified the modified to set
		 */
		public void setModified(Date modified) {
			this.modified = modified;
		}
		
		private void modify() {
			modified= new Date();
		}

		@Override
		public void add(ActionProxy ap) {
			actions.add(ap);
			modify();
			notifyUpdate();
		}
		@Override
		public void add(int i, ActionProxy ap) {
			actions.add(i, ap);
			modify();
			notifyUpdate();
		}
		@Override
		public void clear() {
			actions.clear();
			modify();
			notifyUpdate();
		}
		@Override
		public boolean remove(ActionProxy i) {
			boolean b=actions.remove(i);
			modify();
			notifyUpdate();
			return b;
		}
		@Override
		public boolean remove(int i) {
			boolean b= actions.remove(i)!=null;
			modify();
			notifyUpdate();
			return b;
		}
		@Override
		public void set(int i, ActionProxy actionProxy) {
			actions.set(i, actionProxy);
			modify();
			notifyUpdate();
		}
		@Override
		public void sort(Comparator<Action> comparator) {
			Collections.sort(actions,new ProxyComparator(comparator));
			modify();
			notifyUpdate();
		}
		@Override
		public ActionProxy[] toArray() {
			return actions.toArray(new ActionProxy[actions.size()]);
		}
		@Override
		public void delete() {
			notifyUpdate();
		}
		@Override
		public String getDescription() {
			return description;
		}
		@Override
		public void setDescription(String desc) {
			description=desc;
			modify();
			notifyUpdate();
		}
		@Override
		public void store() {
			notifyUpdate();
		}
		@Override
		public boolean contains(ActionProxy ap) {
			return actions.contains(ap);
		}
		@Override
		public Iterator<ActionProxy> iterator(FolderPreset fp) {
			if (fp==FolderPreset.OPEN) {
				return new Iterator<ActionProxy>() {
					Iterator<ActionProxy> i= actions.iterator();
					ActionProxy next;
					@Override
					public boolean hasNext() {
						if (next==null) {
							next=_next();
							if (next==null) {
								return false;
							}
						}
						return true;
					}
					public ActionProxy _next() {
						if (i.hasNext()) {
							ActionProxy ap= i.next();
							if (!ap.get().isOpen()) {
								return _next();
							}
							return ap;
						}
						return null;
					}
					@Override
					public ActionProxy next() {
						if (!hasNext()) {
							return null;
						}
						ActionProxy ap= next;
						next=null;
						return ap;
					}
					@Override
					public void remove() {
					}
				};
			}
			return actions.iterator();
		}
		@Override
		public ActionProxy get(int i) {
			return actions.get(i);
		}
		@Override
		public int size() {
			return actions.size();
		}
		@Override
		public void suspend(boolean b) {
			// TODO implement
		}

		@Override
		public void reorder(Action[] order) {
			for (int i = 0; i < order.length; i++) {
				actions.remove(order[i].getProxy());
			}
			for (int i = 0; i < order.length; i++) {
				if (actions.size()>i) {
					actions.set(i,(ActionProxyXML)order[i].getProxy());
				} else {
					actions.add((ActionProxyXML)order[i].getProxy());
				}
			}
		}
	}
	
	/*
	 * 
	 * Instance declarations
	 * 
	 */
	private GTDModel model;
	protected volatile boolean changed=false;
	private SaveThread saveThread;
	private boolean autoSave=true;
	private int i=0;
	private GlobalProperties gp;
	private File file;
	private boolean closed=false;
	private Logger logger= Logger.getLogger(this.getClass());
	
	public GTDDataXML() {
	}

	public GTDDataXML(File f, GlobalProperties gp) {
		initialize(f,gp);
	}

	@Override
	public boolean isClosed() {
		return closed;
	}
	
	public GTDDataXML(GlobalProperties gp, GTDModel model) {
		initialize(null,gp);
		this.model=model;
	}

	@Override
	public void initialize(File f, GlobalProperties gp) {
		this.gp=gp;
		if (f==null) {
			return;
		}
		if ((f.exists() && f.isFile()) || (!f.isDirectory() && f.getName().toLowerCase().endsWith(".xml"))) { //$NON-NLS-1$
			file=f;
		} else {
			if (!f.exists()) {
				f.mkdir();
			}
			file= new File(f,ApplicationHelper.DEFAULT_DATA_FILE_NAME);
		}
	}
	
	@Override
	public GTDModel restore() throws IOException {
		if (model==null) {
			model= new GTDModel();
			model.initialize(this);

			Logger.getLogger(this.getClass()).info("Loading XML file "+getDataFile().getAbsolutePath()); //$NON-NLS-1$

			if (getDataFile().exists()) {
				try {
					GTDDataXMLTools.load(model, getDataFile());
				} catch (Exception e) {
					Logger.getLogger(this.getClass()).error("Initialization error.", e); //$NON-NLS-1$
						
					GTDDataXMLTools.DataHeader[] dh= findBackupFiles();
						
					handleFailedLoad(model, dh, 0, e);
						
				}
			} else {
				GTDDataXMLTools.DataHeader[] dh= findBackupFiles();
				if (dh!=null && dh.length>0) {
					handleFailedLoad(model, dh, 0, new FileNotFoundException("Missing main data file: '"+getDataFile().getAbsolutePath()+"'.")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			changed=false;
			setAutoSave(gp.getBoolean(GlobalProperties.AUTO_SAVE , true));
		}

		return model;
	}
	
	
	private void handleFailedLoad(GTDModel m, GTDDataXMLTools.DataHeader[] dh, int i, Exception e) throws IOException {
		if (dh==null || dh.length<=i) {
			// handle when there is no backup file
			
			int option= JOptionPane.showConfirmDialog(
					null, 
					Messages.getString("GTDDataXML.Fail.1")+" \n\""+ //$NON-NLS-1$ //$NON-NLS-2$
					e.toString().replace(". ", ".\n")+ //$NON-NLS-1$ //$NON-NLS-2$
					"\"\n\n"+Messages.getString("GTDDataXML.Fail.2") + //$NON-NLS-1$ //$NON-NLS-2$
					"\n\n"+Messages.getString("GTDDataXML.Fail.3") + //$NON-NLS-1$ //$NON-NLS-2$
					"\n\n"+Messages.getString("GTDDataXML.Fail.4")+" '"+ApplicationHelper.getDataFolder().getAbsolutePath()+ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					"'.\n"+Messages.getString("GTDDataXML.Fail.5")+" '"+ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					getDataFile().getName()+
					"' "+Messages.getString("GTDDataXML.Fail.6") + //$NON-NLS-1$ //$NON-NLS-2$
					"\n"+Messages.getString("GTDDataXML.Fail.7") , //$NON-NLS-1$ //$NON-NLS-2$
					"GTD-Free - "+Messages.getString("GTDDataXML.Fail.title"), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
			if (option== JOptionPane.YES_OPTION) {
				autoSave=false;
				if (saveThread!=null) {
					saveThread.stopSave();
				}
				throw new IOException("Aborting because of previous errors.",e); //$NON-NLS-1$
			}
			return;
		}
			
		int option= JOptionPane.showConfirmDialog(
				null, 
				Messages.getString("GTDDataXML.Fail.1")+" \n\""+ //$NON-NLS-1$ //$NON-NLS-2$
				e.toString().replace(". ", ".\n")+ //$NON-NLS-1$ //$NON-NLS-2$
				"\"\n\n"+Messages.getString("GTDDataXML.Fail.B.1")+" '" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				dh[i].getFile().getAbsolutePath() +
				"',\n"+Messages.getString("GTDDataXML.Fail.B.2")+" " + ApplicationHelper.toISODateTimeString(dh[i].getModified()) + Messages.getString("GTDDataXML.Fail.B.3") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"\n\n"+Messages.getString("GTDDataXML.Fail.3") + //$NON-NLS-1$ //$NON-NLS-2$
				"\n\n"+Messages.getString("GTDDataXML.Fail.B.4") + //$NON-NLS-1$ //$NON-NLS-2$
				"\n"+Messages.getString("GTDDataXML.Fail.B.5") + //$NON-NLS-1$ //$NON-NLS-2$
				"\n\n"+Messages.getString("GTDDataXML.Fail.4")+" '"+ApplicationHelper.getDataFolder().getAbsolutePath()+ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"'.\n"+Messages.getString("GTDDataXML.Fail.5")+" '"+ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				getDataFile().getName()+
				"' "+Messages.getString("GTDDataXML.Fail.6") + //$NON-NLS-1$ //$NON-NLS-2$
				"\n"+Messages.getString("GTDDataXML.Fail.7") , //$NON-NLS-1$ //$NON-NLS-2$
				"GTD-Free - "+Messages.getString("GTDDataXML.Fail.title"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
		if (option==JOptionPane.YES_OPTION) {
			try {
				Logger.getLogger(this.getClass()).info("Loading XML file "+dh[i].getFile().getAbsolutePath()); //$NON-NLS-1$
				model= new GTDModel();
				model.initialize(this);
				GTDDataXMLTools.load(model, dh[i].getFile());
				return;
			} catch (Exception ex) {
				Logger.getLogger(this.getClass()).error("Initialization error.", e); //$NON-NLS-1$
				handleFailedLoad(m, dh, ++i, ex);
			}	
		} else if (option == JOptionPane.NO_OPTION) {
			return;
		} else {
			//if (option== JOptionPane.CANCEL_OPTION) {
			autoSave=false;
			if (saveThread!=null) {
				saveThread.stopSave();
			}
			throw new IOException("Aborting because of previous errors.",e); //$NON-NLS-1$
		} 
		
	}

	
	private GTDDataXMLTools.DataHeader[] findBackupFiles() {
		List<GTDDataXMLTools.DataHeader> l= new ArrayList<GTDDataXMLTools.DataHeader>(10);
		
		for (int i=0; i<10; i++) {
			
			File f= ApplicationHelper.createBackupDataFile(getDataFile(),i);
			if (f.exists() && f.length()>0) {
				try {
					GTDDataXMLTools.DataHeader dh= new GTDDataXMLTools.DataHeader(f);
					l.add(dh);
				} catch (Exception e) {
					Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				} 
			}
			
		}
		
		Collections.sort(l, new Comparator<GTDDataXMLTools.DataHeader>() {
		
			@Override
			public int compare(GTDDataXMLTools.DataHeader o1, GTDDataXMLTools.DataHeader o2) {
				if (o1.getModified()==null || o2.getModified()==null) {
					return 0;
				}
				return (int)(o2.getModified().getTime()-o1.getModified().getTime());
			}
		});
		
		/*for (GTDDataXMLTools.DataHeader dataHeader : l) {
			System.out.println(dataHeader);
		}*/
		
		return l.toArray(new GTDDataXMLTools.DataHeader[l.size()]);
		
		
	}

	
	/**
	 * @return the file
	 */
	public File getDataFile() {
		return file!=null ? file: ApplicationHelper.getDataFile();
	}

	@Override
	public void store() {
		notifyUpdate();
	}

	@Override
	public ActionProxy getProxy(Action a) {
		if (a.getProxy()!=null) {
			return a.getProxy();
		}
		ActionProxy p=  new ActionProxyXML(a);
		a.setProxy(p);
		notifyUpdate();
		return p;
	}
	
	@Override
	public boolean close(boolean terminal) throws IOException {
		
		if (isAutoSave()) {
			setAutoSave(false);
			if (isSaveReady()) {
				flush(); 
			}
		} else if (isSaveReady()) {
			
			if (!terminal) {
				int option = JOptionPane.showConfirmDialog(
								null, 
								Messages.getString("GTDDataXML.Closing"),  //$NON-NLS-1$
								Messages.getString("GTDDataXML.Closing.title"),  //$NON-NLS-1$
								JOptionPane.YES_NO_CANCEL_OPTION, 
								JOptionPane.WARNING_MESSAGE);
				
				if (option == JOptionPane.OK_OPTION) {
					flush();
				} else if (option == JOptionPane.CANCEL_OPTION) {
					return false;
				}
			}
		}
		
		closed=true;
		return true;
	}
	
	@Override
	public synchronized void flush() throws IOException {
		
		File backup = ApplicationHelper.createBackupDataFile(getDataFile(),i++%10);
		if (backup.exists() && !backup.delete()) {
			throw new IOException("Failed to remove backup file '"+backup.getAbsolutePath()+"'."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (getDataFile().exists() && !getDataFile().renameTo(backup)) {
			throw new IOException("Failed to make backup copy file '"+backup.getAbsolutePath()+"'."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		try {
			GTDDataXMLTools.store(model,getDataFile());
		} catch (Exception e) {
			throw new IOException(e);
		}
		changed=false;
		logger.debug("Saved to "+getDataFile().getAbsolutePath()); //$NON-NLS-1$
	}
	
	public void notifyUpdate() {
		changed=true;
		SaveThread st= saveThread;
		if (st!=null) {
			st.notifySave();
		}
	}
	
	/**
	 * @return the autoSave
	 */
	public boolean isAutoSave() {
		return autoSave;
	}

	/**
	 * @param autoSave the autoSave to set
	 */
	public void setAutoSave(boolean autoSave) {
		this.autoSave = autoSave;
		if (autoSave) {
			if (saveThread!=null) {
				saveThread.stopSave();
				//model.removeGTDModelListener(saveThread);
			}
			saveThread=new SaveThread();
			saveThread.start();
			//model.addGTDModelListener(saveThread);
		} else {
			if (saveThread!=null) {
				saveThread.stopSave();
				saveThread=null;
			} 
		}
	}

	public boolean isSaveReady() {
		return changed;
	}
	
	@Override
	public ActionProxy newAction(int id, Date created, Date resolved,
			String description) {
		return getProxy(new Action(id, created, resolved, description));
	}
	
	@Override
	public Folder newFolder(int id, String name, FolderType type) {
		Folder f= null;
		FolderData fd= new FolderData();
		if (type==FolderType.PROJECT) {
			f= new Project(model,id, name,fd);
		} else {
			f= new Folder(model,id,name,type,fd);
		}
		notifyUpdate();
		return f;
	}
	
	@Override
	public void suspend(boolean b) {
	}
	
	@Override
	public ActionProxy newAction(int id, Action aa, Integer project) {
		Action a= new Action(id,aa.getCreated(),aa.getResolved(),aa.getDescription());
		a.copy(aa);
		a.setProject(project);
		return getProxy(a);
	}
	
	@Override
	public void checkConsistency(java.util.logging.Logger log, boolean fail, boolean correct)
			throws ConsistencyException {
	}
	
	@Override
	public String getDatabaseType() {
		return "XML"; //$NON-NLS-1$
	}

}
