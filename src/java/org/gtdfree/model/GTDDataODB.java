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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.gtdfree.GlobalProperties;
import org.gtdfree.Messages;
import org.gtdfree.model.Folder.FolderPreset;
import org.gtdfree.model.Folder.FolderType;
import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.ODBRuntimeException;
import org.neodatis.odb.OID;
import org.neodatis.odb.Objects;
import org.neodatis.odb.OdbConfiguration;
import org.neodatis.odb.core.oid.OIDFactory;
import org.neodatis.odb.core.trigger.SelectTrigger;
import org.neodatis.odb.xml.XMLExporter;
import org.neodatis.odb.xml.XMLImporter;

/**
 * @author ikesan
 *
 */
public final class  GTDDataODB implements GTDData {
	
	public static boolean isUsed(GTDModel m) {
		return m.getDataRepository() instanceof GTDDataODB;
	}
	
	public static class ActionProxyODB implements ActionProxy {

		private long _oid;
		private int id;
		private Integer parentID;
		private boolean open;

		private transient OID oid;
		private transient WeakReference<Action> ref;
		private transient ODB odb;
		private transient Folder parent;

		public ActionProxyODB(Action a, ODB odb) {
			this.odb=odb;
			ref= new WeakReference<Action>(a);
			id= a.getId();
			open= a.isOpen();
			a.setProxy(this);

			synchronized (odb) {
				long time= System.currentTimeMillis();
				oid= odb.store(a);
				_oid=oid.getObjectId();
				if (debug) {
					System.out.println("New Action ["+a.getId()+"] T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$ //$NON-NLS-2$
				}
				commit();
			}
		}
		
		public OID getOid() {
			if (oid==null && _oid!=0) {
				oid= OIDFactory.buildObjectOID(_oid);
			}
			return oid;
		}
		
		void connect(ODB odb, Folder f) {
			this.odb=odb;
			if (parent==null && parentID!=null) {
				parent= f.getId()==parentID ? f : f.getParent().getFolder(parentID);
			}
		}
		
		public Action get() {
			Action a=_get();
			if (a==null) {
				synchronized (odb) {
					long time= System.currentTimeMillis();
					try {
						a= (Action)odb.getObjectFromId(getOid());
					} catch (ODBRuntimeException e) {
						org.apache.log4j.Logger.getLogger(this.getClass()).error("Database error.", e); //$NON-NLS-1$
						return null;
					}
					a.setProxy(this);
					boolean b=open;
					open=a.isOpen();
					if (b!=open) {
						odb.store(this);
					}
					if (debug) {
						System.out.println("Res Action ["+a.getId()+"] T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				ref= new WeakReference<Action>(a);
			}
			return a;
		}
		
		Action _get() {
			Action a=null;
			if (ref!=null) {
				a=ref.get();
			}
			return a;
 		}

		@Override
		public int getId() {
			return id;
		}
		
		@Override
		public void store() {
			Action a= _get();
			if (a!=null) {
				synchronized (odb) {
					long time= System.currentTimeMillis();
					odb.store(a);
					if (debug) {
						System.out.println("Sto Action ["+a.getId()+"] T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$ //$NON-NLS-2$
					}
					commit();
				}
			}
		}
		
		@Override
		public void delete() {
			if (getOid()==null) {
				return;
			}
			synchronized (odb) {
				try {
					odb.delete(this);
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Database error.", e); //$NON-NLS-1$
				}
				try {
					odb.deleteObjectWithId(getOid());
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Database error.", e); //$NON-NLS-1$
				}
				oid=null;
				_oid=0;
			}
		}
		
		@Override
		public Folder getParent() {
			return parent;
		}
		
		public void setParent(Folder parent) {
			this.parent=parent;
			this.parentID= parent.getId();
		}
		
		private void commit() {
			synchronized (odb) {
				long time= System.currentTimeMillis();
				odb.commit();
				if (debug) {
					System.out.println("Com T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$
				}
			}
		}
		
		public boolean isOpen() {
			Action a=_get();
			if (a!=null) {
				open=a.isOpen();
			}
			return open;
		}
		
		@Override
		public String toString() {
			StringBuffer sb= new StringBuffer(64);
			sb.append("AP{"); //$NON-NLS-1$
			sb.append(id);
			sb.append(',');
			sb.append(open);
			sb.append('}');
			return sb.toString();
		}
		
	}
	
	
	public static class FolderDataODB implements FolderDataProxy {
		
		class Data  {
			public List<ActionProxyODB> actions= new ArrayList<ActionProxyODB>();
			public String description;
			private Date created;
			private Date resolved;
			private Date modified;
		}
		
		private class ProxyComparator implements Comparator<ActionProxyODB> {
			Comparator<Action> c;
			public ProxyComparator(Comparator<Action> c) {
				this.c=c;
			}
			@Override
			public int compare(ActionProxyODB o1, ActionProxyODB o2) {
				o1.connect(odb, folder);
				o2.connect(odb, folder);
				return c.compare(o1.get(), o2.get());
			}
		}

		
		private transient WeakReference<Data> dataRef;
		private transient ODB odb;
		private transient Folder folder;
		private transient Set<Object> references= new HashSet<Object>();
		private transient OID dataOID;
		
		private long _dataOID;
		private Integer folderID;
		private boolean suspend;
		private int size;
		

		public FolderDataODB() {
		}
		
		private OID getDataOID() {
			if (dataOID==null && _dataOID!=0) {
				dataOID= OIDFactory.buildObjectOID(_dataOID);
			}
			return dataOID;
		}
		
		private Data getDataFromRef() {
			Data d=null;
			if (dataRef!=null) {
				d= dataRef.get();
			}
			return d;
		}
		
		private Data getData() {
			Data d= getDataFromRef();
			if (d!=null) {
				return d;
			}
			if (getDataOID()!=null) {
				synchronized (odb) {
					long time= System.currentTimeMillis();
					try {
						d= (Data)odb.getObjectFromId(getDataOID());
					} catch (Exception e) {
						org.apache.log4j.Logger.getLogger(this.getClass()).error("Database error.", e); //$NON-NLS-1$
					}
					if (debug) {
						//Thread.dumpStack();
						System.out.println("Res Folder ["+folderID+"] T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			if (d!=null) {
				dataRef= new WeakReference<Data>(d);
				return d;
			}
			synchronized (odb) {
				d= new Data();
				dataRef= new WeakReference<Data>(d);
				long time= System.currentTimeMillis();
				dataOID= odb.store(d);
				_dataOID= dataOID.getObjectId();
				odb.store(this);
				if (debug) {
					System.out.println("New Folder ["+folderID+"] T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$ //$NON-NLS-2$
				}
				commit();
			}
			return d;
		}

		public void connect(Folder folder, ODB odb) {
			this.folderID=folder.getId();
			this.folder= folder;
			this.odb=odb;
		}
		
		@Override
		public void delete() {
			if (getDataOID()==null) {
				return;
			}
			synchronized (odb) {
				try {
					odb.delete(this);
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Database error.", e); //$NON-NLS-1$
				}
				try {
					odb.deleteObjectWithId(getDataOID());
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(this.getClass()).debug("Database error.", e); //$NON-NLS-1$
				}
				_dataOID=0;
				dataOID=null;
			}
		}
		@Override
		public String getDescription() {
			return getData().description;
		}
		@Override
		public void setDescription(String desc) {
			Data d = getData();
			d.description=desc;
			modified(d);
			if (suspend) {
				references.add(d);
			} else {
				store();
			}
		}
		@Override
		public void store() {
			if (suspend) {
				Data d= getDataFromRef();
				if (d!=null) {
					references.add(d);
				}
				return;
			}
			synchronized (odb) {
				long time= System.currentTimeMillis();
				if (folder!=null) {
					odb.store(folder);
				} else {
					odb.store(this);
				}
				Data d= getDataFromRef();
				if (d!=null) {
					odb.store(d);
					if (_dataOID==0) {
						dataOID= odb.getObjectId(d);
						_dataOID = dataOID.getObjectId();
					}
				}
				if (debug) {
					System.out.println("Sto Folder ["+folderID+"] T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$ //$NON-NLS-2$
				}
				commit();
			}
		}
		
		private void commit() {
			synchronized (odb) {
				long time= System.currentTimeMillis();
				odb.commit();
				if (debug) {
					System.out.println("Com T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$
				}
			}
		}
		
		@Override
		public boolean contains(ActionProxy ap) {
			Data d= getData();
			if (d!=null) {
				return d.actions.contains(ap);
			}
			return false;
		}
		
		@Override
		public ActionProxy get(int i) {
			Data d= getData();
			if (d!=null) {
				ActionProxyODB ap= d.actions.get(i);
				ap.connect(odb, folder);
				return ap;
			}
			return null;
		}
		
		public int size() {
			Data d= getDataFromRef();
			if (d!=null) {
				return size=d.actions.size();
			}
			return size;
		};
		
		@Override
		public Iterator<ActionProxy> iterator(FolderPreset fp) {
			Data d= getData();
			if (d!=null) {
				if (fp==FolderPreset.OPEN) {
					return new Iterator<ActionProxy>() {
						Iterator<ActionProxyODB> i= getData().actions.iterator();
						ActionProxyODB next;
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
						public ActionProxyODB _next() {
							if (i.hasNext()) {
								ActionProxyODB ap= i.next();
								if (!ap.isOpen()) {
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
							ActionProxyODB ap= next;
							next=null;
							ap.connect(odb, folder);
							return ap;
						}
						@Override
						public void remove() {
						}
					};
				}
				return new Iterator<ActionProxy>() {
					Iterator<ActionProxyODB> i= getData().actions.iterator();
					@Override
					public boolean hasNext() {
						return i.hasNext();
					}
					@Override
					public ActionProxy next() {
						ActionProxyODB ap= i.next();
						ap.connect(odb, folder);
						return ap;
					}
					@Override
					public void remove() {
					}
				};
			}
			return new Iterator<ActionProxy>() {
				@Override
				public boolean hasNext() {
					return false;
				}
				@Override
				public ActionProxy next() {
					return null;
				}
				@Override
				public void remove() {
				}
			};
		}
		
		@Override
		public void add(ActionProxy ap) {
			Data d=getData();
			ActionProxyODB apo= (ActionProxyODB)ap;
			d.actions.add(apo);
			modified(d);
			if (!folder.isMeta()) {
				apo.connect(odb, folder);
			}
			size=d.actions.size();
			if (suspend) {
				references.add(d);
				references.add(ap);
			} else {
				store();
			}
		}
		
		@Override
		public void add(int i, ActionProxy ap) {
			Data d=getData();
			ActionProxyODB apo= (ActionProxyODB)ap;
			d.actions.add(i,apo);
			modified(d);
			if (!folder.isMeta()) {
				apo.connect(odb, folder);
			}
			size=d.actions.size();
			if (suspend) {
				references.add(d);
				references.add(ap);
			} else {
				store();
			}
		}
		
		public void clear() {
			Data d=getData();
			d.actions.clear();
			modified(d);
			if (suspend) {
				references.add(d);
			} else {
				store();
			}
		};
		
		@Override
		public boolean remove(ActionProxy i) {
			Data d=getData();
			boolean b= d.actions.remove(i);
			modified(d);
			if (b) {
				size=d.actions.size();
				if (suspend) {
					references.add(d);
				} else {
					store();
				}
			}
			return b;
		}
		
		@Override
		public boolean remove(int i) {
			Data d=getData();
			boolean b= d.actions.remove(i)!=null;
			modified(d);
			if (b) {
				size=d.actions.size();
				if (suspend) {
					references.add(d);
				} else {
					store();
				}
			}
			return b;
		}
		
		@Override
		public void set(int i, ActionProxy ap) {
			Data d=getData();
			d.actions.set(i,(ActionProxyODB)ap);
			size=d.actions.size();
			if (suspend) {
				references.add(d);
				references.add(ap);
			} else {
				store();
			}
		}
		
		public void sort(java.util.Comparator<Action> comparator) {
			Data d=getData();
			Collections.sort(d.actions,new ProxyComparator(comparator));
			modified(d);
			if (suspend) {
				references.add(d);
			} else {
				store();
			}
		};
		
		@Override
		public ActionProxy[] toArray() {
			Data d= getData();
			Iterator<ActionProxyODB> it= d.actions.iterator();
			ActionProxy[] a= new ActionProxy[d.actions.size()];
			for (int i = 0; i < a.length; i++) {
				ActionProxyODB ap= it.next();
				ap.connect(odb, folder);
				a[i]=ap;
			}
			return a;
		}
		
		@Override
		public void suspend(boolean b) {
			this.suspend= b;
			if (!b) {
				store();
				references.clear();
			}
		}
		@Override
		public Date getCreated() {
			return getData().created;
		}
		
		@Override
		public Date getModified() {
			return getData().modified;
		}
		
		@Override
		public Date getResolved() {
			return getData().resolved;
		}
		
		@Override
		public void setCreated(Date d) {
			Data dd = getData();
			dd.created=d;
			if (suspend) {
				references.add(dd);
			} else {
				store();
			}
		}
		
		@Override
		public void setModified(Date d) {
			Data dd = getData();
			dd.modified=d;
			if (suspend) {
				references.add(dd);
			} else {
				store();
			}
		}
		
		@Override
		public void setResolved(Date d) {
			Data dd = getData();
			dd.resolved=d;
			if (suspend) {
				references.add(dd);
			} else {
				store();
			}
		}
		
		public void modified(Data d) {
			d.modified=new Date();
		}
		
		@Override
		public String toString() {
			StringBuffer sb= new StringBuffer(128);
			sb.append("FD{"); //$NON-NLS-1$
			sb.append(folderID);
			sb.append(',');
			sb.append(size);
			sb.append(',');
			sb.append(Arrays.toString(getDataFromRef().actions.toArray()));
			sb.append('}');
			return sb.toString();
		}

		@Override
		public void reorder(Action[] order) {
			boolean s=suspend; 
			if (s) {
				suspend(false);
			}
			
			Data d= getData();
			for (int i = 0; i < order.length; i++) {
				d.actions.remove(order[i].getProxy());
			}
			for (int i = 0; i < order.length; i++) {
				if (d.actions.size()>i) {
					d.actions.set(i,(ActionProxyODB)order[i].getProxy());
				} else {
					d.actions.add((ActionProxyODB)order[i].getProxy());
				}
			}
			if (s) {
				suspend(true);
			}
		}
	}
	
	
	static class DBInfo {
		public String version;
		public String modelImpl;
		
		public DBInfo() {
		}
		
		public DBInfo(String v, String c) {
			version=v;
			modelImpl=c;
		}
		
	}
	
	
	private static final boolean debug=false;
	public static final String BACKUP_EXPORT_FILE_NAME = "odb-backup.odb-xml"; //$NON-NLS-1$
	
	private File file;
	private ODB odb;
	private GTDModel model;
	private boolean suspend= false;
	//private GlobalProperties gp;
	private boolean exportOnClose=true;
	private transient boolean closed=false;
	
	private org.apache.log4j.Logger logger= org.apache.log4j.Logger.getLogger(GTDDataODB.class);
	
	/**
	 * @return the exportOnClose
	 */
	public boolean isExportOnClose() {
		return exportOnClose;
	}

	/**
	 * @param exportOnClose the exportOnClose to set
	 */
	public void setExportOnClose(boolean exportOnClose) {
		this.exportOnClose = exportOnClose;
	}

	public GTDDataODB() {
	}
	
	public GTDDataODB(File f, GlobalProperties gp) {
		initialize(f,gp);
	}

	public GTDDataODB(File f) {
		initialize(f,new GlobalProperties());
	}

	public ODB getODB() {
		if (odb==null && !closed) {
			OdbConfiguration.setThrowExceptionWhenInconsistencyFound(true);
			//OdbConfiguration.setByteCodeInstrumentationIsOn(false);
			OdbConfiguration.setAutomaticCloseFileOnExit(true);
			OdbConfiguration.setUseLazyCache(false);
			OdbConfiguration.setReconnectObjectsToSession(true);
			try {
				OdbConfiguration.setDatabaseCharacterEncoding("UTF-8"); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				org.apache.log4j.Logger.getLogger(this.getClass()).error("Database error.", e); //$NON-NLS-1$
			}
			odb= ODBFactory.open(file.getAbsolutePath());
			odb.addSelectTrigger(Folder.class, new SelectTrigger() {
				@Override
				public void afterSelect(Object object, OID oid) {
					Folder f= (Folder)odb.getObjectFromId(oid);
					((FolderDataODB)f.getData()).connect(f,GTDDataODB.this.getODB());
				}
			});
			odb.addSelectTrigger(Project.class, new SelectTrigger() {
				@Override
				public void afterSelect(Object object, OID oid) {
					Folder f= (Folder)odb.getObjectFromId(oid);
					((FolderDataODB)f.getData()).connect(f,GTDDataODB.this.getODB());
				}
			});
		}
		return odb;
	}

	
	@Override
	public ActionProxy getProxy(Action a) {
		if (a.getProxy()!=null && a.getProxy() instanceof ActionProxyODB && ((ActionProxyODB)a.getProxy()).get()==a) {
			return a.getProxy();
		}
		return new ActionProxyODB(a,getODB());
	}
	
	@Override
	public void store() {
		if (closed) {
			return;
		}
		/*if (suspend) {
			return;
		}*/
		synchronized (getODB()) {
			long time= System.currentTimeMillis();
			getODB().store(model);
			if (debug) {
				System.out.println("Sto Model T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$
			}
			commit();
		}
	}

	@Override
	public boolean close(boolean terminal) throws IOException {
		if (!closed) {
			close(true,true);
			closed=true;
		}
		return true;
	}
	private boolean close(boolean store, boolean doExport) throws IOException {
		if (store) {
			store();
		}
		if (exportOnClose && doExport && !closed) {
			long time= System.currentTimeMillis();
			try {
				File f= getShutdownBackupFile();
				_exportODB(f);
				logger.info(Messages.getString("GTDDataODB.Backup")+f.toString()); //$NON-NLS-1$
			} catch (Exception e) {
				org.apache.log4j.Logger.getLogger(this.getClass()).error("Database error.", e); //$NON-NLS-1$
			}
			if (debug) {
				System.out.println("Export T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$
			}
		}

		if (!closed && odb!=null) {
			synchronized (getODB()) {
				long time= System.currentTimeMillis();
				getODB().close();
				if (debug) {
					System.out.println("Close T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$
				}
			}
		}
		
		return true;
	}

	/**
	 * @return
	 */
	public File getShutdownBackupFile() {
		return new File(file.getParent(),BACKUP_EXPORT_FILE_NAME);
	}
	
	@Override
	public void flush() {
		//Thread.dumpStack();
		synchronized (getODB()) {
			long time= System.currentTimeMillis();
			getODB().store(model);
			if (debug) {
				System.out.println("Sto T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$
			}
			commit();
		}
	}

	private void commit() {
		synchronized (getODB()) {
			long time= System.currentTimeMillis();
			getODB().commit();
			if (debug) {
				System.out.println("Com Model T"+(System.currentTimeMillis()-time)); //$NON-NLS-1$
			}
		}
	}

	@Override
	public GTDModel restore() throws IOException {
		
		if (model!=null) {
			return model;
		}
		
		try {
			Objects<DBInfo> o= getODB().getObjects(DBInfo.class);
			DBInfo info= o.hasNext() ? o.next() : null;
			
			if (info==null) {
				info= new DBInfo("1.0",GTDModel.class.getName()); //$NON-NLS-1$
				getODB().store(info);
				getODB().commit();
			} 
				
			Class<?> c= null;
			try {
				c = Class.forName(info.modelImpl);
			} catch (ClassNotFoundException e) {
				org.apache.log4j.Logger.getLogger(this.getClass()).debug("Internal error.", e); //$NON-NLS-1$
				c= GTDModel.class;
			}
			
			long time= System.currentTimeMillis();
			Objects<GTDModel> om= getODB().getObjects(c);
			model= om.hasNext() ? om.next() : null;
			
			if (model==null) {
				model= (GTDModel)c.newInstance();
				getODB().store(model);
				getODB().commit();
			}
			
			if (debug) {
				System.out.println("Restore Model "+(System.currentTimeMillis()-time)); //$NON-NLS-1$
			}
			
			model.initialize(this);
				
			model.reconnect();
		
		} catch (Throwable e) {
			org.apache.log4j.Logger.getLogger(this.getClass()).fatal("Initialization error, closing.", e); //$NON-NLS-1$
			
			JOptionPane.showMessageDialog(
					null, 
					Messages.getString("GTDDataODB.Fail.1")+" '" //$NON-NLS-1$ //$NON-NLS-2$
					+file.toString()
					+"'\n"+Messages.getString("GTDDataODB.Fail.2")+" \n\n\"" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+e.toString()
					+"\".\n\n"+Messages.getString("GTDDataODB.Fail.3")+" '" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+file.getParent()
					+"':\n "+Messages.getString("GTDDataODB.Fail.4")+" '" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+file.getName()
					+"' "+Messages.getString("GTDDataODB.Fail.5")+" '" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+BACKUP_EXPORT_FILE_NAME
					+"'.",  //$NON-NLS-1$
					Messages.getString("GTDDataODB.Fail.title"),  //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE);
			try {
				close(false,false);
			} catch (Exception ex) {
				org.apache.log4j.Logger.getLogger(this.getClass()).error("Initialization error.", ex); //$NON-NLS-1$
			}
			throw new IOException(e);
		}

		return model;
	}
	
	@Override
	public void initialize(File f, GlobalProperties gp) {
		//this.gp=gp;
		if (f==null) {
			return;
		}
		if ((f.exists() && f.isFile()) || f.getName().toLowerCase().endsWith(".odb")) { //$NON-NLS-1$
			file=f;
		} else {
			file= new File(f,"odb"); //$NON-NLS-1$
			if (!file.exists()) {
				file.mkdir();
			}
			file= new File(file,"gtd-free.odb"); //$NON-NLS-1$
		}
		
		setExportOnClose(gp.getBoolean(GlobalProperties.SHUTDOWN_BACKUP_ODB, true));
	}
	
	@Override
	public ActionProxy newAction(int id, Date created, Date resolved,
			String description) {
		Action a=new Action(id, created, resolved, description);
		ActionProxy ap= getProxy(a);
		store();
		return ap;
	}
	
	@Override
	public ActionProxy newAction(int id, Action aa, Integer project) {
		Action a= new Action(id,aa.getCreated(),aa.getResolved(),aa.getDescription());
		a.copy(aa);
		a.setProject(project);
		ActionProxy ap= getProxy(a);
		store();
		return ap;
	}
	
	@Override
	public Folder newFolder(int id, String name, FolderType type) {
		Folder f=null;
		FolderDataODB fd= new FolderDataODB();
		if (type==FolderType.PROJECT) {
			f= new Project(model,id, name, fd);
		} else {
			f= new Folder(model, id, name, type, fd);
		}
		fd.connect(f, getODB());
		fd.setModified(new Date());
		/*if (suspend) {
			return f;
		}*/
		synchronized (getODB()) {
			long time= System.currentTimeMillis();
			fd.store();
			getODB().store(model);
			if (debug) {
				System.out.println("New Folder "+f.getId()+" "+(System.currentTimeMillis()-time)); //$NON-NLS-1$ //$NON-NLS-2$
			}
			commit();
		}
		return f;

	}
	
	@Override
	public void suspend(boolean b) {
		suspend=b;
		if (!suspend) {
			store();
		}
	}
	
	@Override
	public void checkConsistency(Logger log, boolean fail, boolean correct)
			throws ConsistencyException {
		
		Set<OID> oids= new HashSet<OID>();
		ODB odb= getODB();
		Map<String,Folder> folderNames= new HashMap<String, Folder>();
		Map<OID,OID> del2new= new HashMap<OID, OID>();
		
		
		// get all oid for objects known to model
		for (Folder f: model) {
			oids.add(odb.getObjectId(f));
			folderNames.put(f.getName()+"TYPE"+f.getType(), f); //$NON-NLS-1$
			if (!f.isMeta()) {
				Iterator<ActionProxy> i= f.proxyIterator(FolderPreset.ALL);
				while (i.hasNext()) {
					ActionProxyODB a= (ActionProxyODB)i.next();
					oids.add(a.getOid());
					oids.add(odb.getObjectId(a));
				}
			}
		}
		
		// discover all relevant objects but not in model
		
		Objects<Folder> of= odb.getObjects(Folder.class);
		Objects<Project> op= odb.getObjects(Project.class);

		List<Folder> ff= new ArrayList<Folder>(of.size()+op.size());
		ff.addAll(of);
		ff.addAll(op);
		
		for (Folder f: ff) {
			if (!f.isMeta()) {
				OID oid= odb.getObjectId(f);
				
				if (!oids.contains(oid)) {
					
					ConsistencyException e= new ConsistencyException("Folder not in model.", null, new Folder[]{f},null); //$NON-NLS-1$
					if (fail) {
						throw e;
					}
					log.log(Level.WARNING, "Folder '"+f.getName()+"' not in model.", e); //$NON-NLS-1$ //$NON-NLS-2$
					if (correct) {
						List<ActionProxyODB> a= new ArrayList<ActionProxyODB>(f.size());
						Iterator<ActionProxy> it= f.proxyIterator(FolderPreset.ALL);
						
						while (it.hasNext()) {
							ActionProxyODB ap= (ActionProxyODB)it.next();
							if (!oids.contains(ap.getOid())) {
								a.add(ap);
							}
						}
						
						f.clear();
						
						Folder f1 = folderNames.get(f.getName()+"TYPE"+f.getType()); //$NON-NLS-1$
						
						if (f1==null) {
							f1= model.createFolder(f.getName(),f.getType());
							f1.setDescription(f.getDescription());
							log.log(Level.INFO, "Created folder  '"+f.getName()+"' in model."); //$NON-NLS-1$ //$NON-NLS-2$
						} else {
							log.log(Level.INFO, "Found folder  '"+f.getName()+"' in model."); //$NON-NLS-1$ //$NON-NLS-2$
						}
						
						for (ActionProxyODB ap : a) {
							Action aa= ap.get();
							if (aa==null) {
								continue;
							}
							Action aaa= model.createActionCopy(
									f1, 
									aa, 
									aa.getProject()!=null && model.getProject(aa.getProject())==null 
										? aa.getProject() 
											: null);
							ActionProxyODB aap= (ActionProxyODB)getProxy(aaa);
							oids.add(aap.getOid());
							OID aapoid= odb.getObjectId(aap);
							oids.add(aapoid);
							
							if (!del2new.containsKey(ap.getOid())) {
								try {
									odb.delete(aa);
								} catch (Exception e2) {
									System.out.println(e2.getMessage());
								}
								del2new.put(ap.getOid(),aap.getOid());
							}
							OID apoid= odb.getObjectId(ap);
							if (!del2new.containsKey(apoid)) {
								try {
									odb.delete(ap);
								} catch (Exception e2) {
									System.out.println(e2.getMessage());
								}
								del2new.put(apoid,aapoid);
							}

							log.log(Level.INFO, "Added action with ID '"+aaa.getId()+"' to folder  '"+f1.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						oids.add(odb.getObjectId(f1));
						odb.deleteObjectWithId(oid);
					}
				}
			}
		}
		
		for (Folder f: ff) {
			if (f.isMeta()) {
				OID oid= odb.getObjectId(f);
				
				if (!oids.contains(oid)) {
					
					ConsistencyException e= new ConsistencyException("Folder not in model.", null, new Folder[]{f},null); //$NON-NLS-1$
					if (fail) {
						throw e;
					}
					log.log(Level.WARNING, "Folder '"+f.getName()+"' not in model.", e); //$NON-NLS-1$ //$NON-NLS-2$
					if (correct) {

						List<ActionProxyODB> a= new ArrayList<ActionProxyODB>(f.size());
					
						Iterator<ActionProxy> it= f.proxyIterator(FolderPreset.ALL);
						
						while (it.hasNext()) {
							ActionProxyODB ap= (ActionProxyODB)it.next();
							if (!oids.contains(ap.getOid())) {
								a.add(ap);
							}
						}
						f.clear();
						
						Project p=null;
						
						if (f.isProject()) {
							
							p= model.findFirstProject(f.getName());
							
							if (p==null) {
								p= (Project)model.createFolder(f.getName(),FolderType.PROJECT);
								p.setDescription(f.getDescription());
								p.setClosed(f.isClosed());
							}
							
						}

						Folder f1 = model.getInBucketFolder();
						
						for (ActionProxyODB ap : a) {
							
							if (del2new.containsKey(ap.getOid())) {
								continue;
							}
							
							Action aa= ap.get();
							if (aa==null) {
								continue;
							}
							Action aaa= model.createActionCopy(
									f1, 
									aa, 
									p !=null ? p.getId() : null);
							ActionProxyODB aap= (ActionProxyODB)getProxy(aaa);
							oids.add(aap.getOid());
							OID aapoid= odb.getObjectId(aap);
							oids.add(aapoid);
							
							if (!del2new.containsKey(ap.getOid())) {
								try {
									odb.delete(aa);
								} catch (Exception e2) {
									System.out.println(e2.getMessage());
								}
								del2new.put(ap.getOid(),aap.getOid());
							}
							OID apoid= odb.getObjectId(ap);
							if (!del2new.containsKey(apoid)) {
								try {
									odb.delete(ap);
								} catch (Exception e2) {
									System.out.println(e2.getMessage());
								}
								del2new.put(apoid,aapoid);
							}

							log.log(Level.INFO, "Added action with ID '"+aaa.getId()+"' to folder  '"+f1.getName()+"'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						odb.deleteObjectWithId(oid);
					}
				}
			}
			odb.commit();
		}


		Objects<Action> oa= odb.getObjects(Action.class);
		
		for (Action a : oa) {
			OID oid= odb.getObjectId(a);
			
			if (!oids.contains(oid)) {
			
				ActionProxyODB ap= (ActionProxyODB)getProxy(a);
				
				Folder f= ap.getParent();
				
				if (f==null || model.getFolder(f.getId())!=f) {
					f= model.getInBucketFolder();
				}
				
				f.add(a, ap);
				
				oids.add(oid);
				oids.add(odb.getObjectId(ap));
			}
		}
		
		
	}
	
	@Override
	public String getDatabaseType() {
		return "ODB"; //$NON-NLS-1$
	}

	public void exportODB(File f) throws Exception {
		store();
		flush();

		_exportODB(f);

		close(false,false);
		odb=null;
	}
	
	private void _exportODB(File f) throws Exception {
		
		f.delete();
		
		synchronized (getODB()) {
			XMLExporter xml= new XMLExporter(getODB());
			xml.export(f.getParent(), f.getName());
		}
	}
	
	public void importODB(File f) throws Exception {

		close(true,true);
		synchronized (getODB()) {
			odb=null;
		}
		
		synchronized (getODB()) {
			XMLImporter xml= new XMLImporter(getODB());
			xml.importFile(f.getParent(), f.getName());
		}

		close(false,false);
		odb=null;
		closed=true;
	}
	
	@Override
	public boolean isClosed() {
		return closed;
	}
}
