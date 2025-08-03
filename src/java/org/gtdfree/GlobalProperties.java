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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.log4j.Logger;

/**
 * @author ikesan
 *
 */
public class GlobalProperties {
	
	private class BooleanPropertyConnector implements PropertyChangeListener, ChangeListener {

		private String globalProperty;
		private Object target;
		@SuppressWarnings("unused")
		private String targetProperty;
		private Method getter;
		private Method setter;
		private boolean setting=false;
		private boolean defaultValue;

		public BooleanPropertyConnector(String globalProperty, Object target,
				String targetProperty, String getter, String setter, boolean defaultValue) {
			this.globalProperty=globalProperty;
			this.target=target;
			this.targetProperty=targetProperty;
			this.defaultValue=defaultValue;
			
			try {
				if (getter!=null) {
					this.getter=target.getClass().getMethod(getter);
				}
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).debug("Internal error.",e); //$NON-NLS-1$
			}

			try {
				if (setter!=null) {
					this.setter=target.getClass().getMethod(setter, boolean.class);
				}
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).debug("Internal error.",e); //$NON-NLS-1$
			}

			setting=true;
			try {
				this.setter.invoke(target, getBoolean(globalProperty,defaultValue));
				if (getter!=null) {
					target.getClass().getMethod("addChangeListener", ChangeListener.class).invoke(target, this); //$NON-NLS-1$
				}
			} catch (Exception e) {
				Logger.getLogger(this.getClass()).debug("Internal error.",e); //$NON-NLS-1$
			}
			setting=false;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (setting) {
				return;
			}
			if (evt.getSource()==GlobalProperties.this && evt.getPropertyName().equals(globalProperty)) {
				setting=true;
				try {
					setter.invoke(target, getBoolean(globalProperty,defaultValue));
				} catch (Exception e) {
					Logger.getLogger(this.getClass()).debug("Internal error.",e); //$NON-NLS-1$
				}
				setting=false;
			}
		}
		
		@Override
		public void stateChanged(ChangeEvent e) {
			if (setting || getter==null) {
				return;
			}
			setting=true;
			try {
				putProperty(globalProperty, getter.invoke(target));
			} catch (Exception ex) {
				Logger.getLogger(this.getClass()).debug("Internal error.",ex); //$NON-NLS-1$
			}
			setting=false;
		}

	}
	public static final String DATE_FORMAT="dateFormat"; //$NON-NLS-1$
	public static final String SHOW_ALL_ACTIONS="showAllActions"; //$NON-NLS-1$
	public static final String SHOW_CLOSED_FOLDERS="showClosedFolders"; //$NON-NLS-1$
	public static final String PROJECT_EDITOR_PREFERRED_SIZE ="projectEditorPreferredSize"; //$NON-NLS-1$
	public static final String SHOW_OVERVIEW_TAB = "showOverviewTab"; //$NON-NLS-1$
	public static final String SHOW_QUICK_COLLECT = "showQuickCollectBar"; //$NON-NLS-1$
	public static final String AUTO_SAVE = "autoSave"; //$NON-NLS-1$
	public static final String DATABASE = "database"; //$NON-NLS-1$
	public static final String DATABASE_VALUE_XML = "xml"; //$NON-NLS-1$
	public static final String DATABASE_VALUE_ODB = "odb"; //$NON-NLS-1$
	public static final String SHOW_TRAY_ICON = "showTrayIcon"; //$NON-NLS-1$
	public static final String SHOW_EMPTY_FOLDERS = "showEmptyFolders"; //$NON-NLS-1$
	public static final String SHUTDOWN_BACKUP_XML = "shutdownBackupXML"; //$NON-NLS-1$
	public static final String SHUTDOWN_BACKUP_ODB = "shutdownBackupODB"; //$NON-NLS-1$
	public static final String PAGE_SIZE_NAME = "pageSizeName"; //$NON-NLS-1$
	public static final String PAGE_ORIENTATION = "pageOrientation"; //$NON-NLS-1$
	public static final String PAGE_PRINTABLE_AREA = "pagePrintableArea"; //$NON-NLS-1$
	public static final String CHECK_FOR_UPDATE_AT_START = "checkForUpdateAtStart"; //$NON-NLS-1$
	
	private Properties prop= new Properties();
	private PropertyChangeSupport pcs= new PropertyChangeSupport(this);
	
	public void addPropertyChangeListener(String s, PropertyChangeListener l) {
		pcs.addPropertyChangeListener(s, l);
	}
	public void removePropertyChangeListener(String s, PropertyChangeListener l) {
		pcs.removePropertyChangeListener(s, l);
	}
	
	public void putProperty(String s, Object v) {
		Object old= null;
		if (v==null) {
			old= prop.remove(s);
		} else {
			old= prop.put(s, v);
		}
		pcs.firePropertyChange(s, old, v);
	}
	
	public Object getProperty(String s) {
		return prop.get(s);
	}
	public Object getProperty(String s, Object defaultValue) {
		Object o= prop.get(s);
		if (o==null) {
			return defaultValue;
		}
		return o;
	}
	public boolean getBoolean(String s) {
		return getBoolean(s, false);
	}
	
	public boolean getBoolean(String s, boolean def) {
		Object o= prop.get(s);
		
		if (o==null) {
			return def;
		}
		
		if (o instanceof Boolean) {
			return ((Boolean)o).booleanValue();
		}
		
		Boolean b= Boolean.valueOf(o.toString());
		prop.put(s, b);
		
		return b.booleanValue();
	}
	
	public Integer getInteger(String s) {
		Object o= prop.get(s);
		
		if (o==null) {
			return null;
		}
		
		if (o instanceof Integer) {
			return (Integer)o;
		}
		
		try {
			Integer b= Integer.valueOf(o.toString());
			prop.put(s, b);
			return b;
		} catch (Exception e) {
			Logger.getLogger(ApplicationHelper.class).warn("Internal error.", e); //$NON-NLS-1$
		}
		return null;
	}
	
	public void load(Reader r) throws IOException {
		prop.load(r);
	}
	public void store(BufferedWriter w) throws IOException {
		Properties p= new Properties();
		
		for (Object s : prop.keySet()) {
			
			Object o= prop.get(s);
			
			if (o instanceof int[]) {
				int[] ii= (int[])o;
				StringBuilder sb= new StringBuilder();
				
				if (ii.length>0) {
					sb.append(ii[0]);
				}
				
				for (int i = 1; i < ii.length; i++) {
					sb.append(',');
					sb.append(ii[i]);
				}
				p.put(s, sb.toString());
			} else if (o instanceof double[]) {
				double[] ii= (double[])o;
				StringBuilder sb= new StringBuilder();
				
				if (ii.length>0) {
					sb.append(ii[0]);
				}
				
				for (int i = 1; i < ii.length; i++) {
					sb.append(',');
					sb.append(ii[i]);
				}
				p.put(s, sb.toString());
			} else if (o instanceof boolean[]) {
				boolean[] ii= (boolean[])o;
				StringBuilder sb= new StringBuilder(ii.length*6);
				
				if (ii.length>0) {
					sb.append(ii[0]);
				}
				
				for (int i = 1; i < ii.length; i++) {
					sb.append(',');
					sb.append(ii[i]);
				}
				p.put(s, sb.toString());
			} else if (o!=null) {
				p.put(s, o.toString());
			}
			
		}
		
		p.store(w, ""); //$NON-NLS-1$
	}
	public int[] getIntegerArray(String s) {
		Object o= prop.get(s);
		
		if (o==null) {
			return null;
		}
		
		if (o instanceof int[]) {
			return (int[])o;
		}
		
		if (o instanceof String && s.length()>0) {
			String[] ss= o.toString().split(","); //$NON-NLS-1$
			if (ss!=null && ss.length>0) {
				List<Integer> il= new ArrayList<Integer>(ss.length);
				for (int i = 0; i < ss.length; i++) {
					ss[i]=ss[i].trim();
					if (ss[i].length()>0) {
						try {
							il.add(Integer.valueOf(ss[i]));
						} catch (Exception e) {
							Logger.getLogger(this.getClass()).warn("Internal error.", e); //$NON-NLS-1$
						}
					}
				}
				int[] ii= new int[il.size()];
				for (int i = 0; i < ii.length; i++) {
					ii[i]=il.get(i);
				}
				prop.put(s, ii);
				return ii;
			}
		}
		
		return null;
	}
	public boolean[] getBooleanArray(String s) {
		Object o= prop.get(s);
		
		if (o==null) {
			return null;
		}
		
		if (o instanceof boolean[]) {
			return (boolean[])o;
		}
		
		if (o instanceof String && s.length()>0) {
			String[] ss= o.toString().split(","); //$NON-NLS-1$
			if (ss!=null && ss.length>0) {
				boolean[] ii= new boolean[ss.length];
				for (int i = 0; i < ii.length; i++) {
					try {
						ii[i]= Boolean.valueOf(ss[i]);
					} catch (Exception e) {
						Logger.getLogger(this.getClass()).warn("Internal error.", e); //$NON-NLS-1$
					}
				}
				prop.put(s, ii);
				return ii;
			}
		}
		
		return null;
	}
	public double[] getDoubleArray(String s) {
		Object o= prop.get(s);
		
		if (o==null) {
			return null;
		}
		
		if (o instanceof double[]) {
			return (double[])o;
		}
		
		if (o instanceof String && s.length()>0) {
			String[] ss= o.toString().split(","); //$NON-NLS-1$
			if (ss!=null && ss.length>0) {
				List<Double> il= new ArrayList<Double>(ss.length);
				for (int i = 0; i < ss.length; i++) {
					ss[i]=ss[i].trim();
					if (ss[i].length()>0) {
						try {
							il.add(Double.valueOf(ss[i]));
						} catch (Exception e) {
							Logger.getLogger(this.getClass()).warn("Internal error.", e); //$NON-NLS-1$
						}
					}
				}
				double[] ii= new double[il.size()];
				for (int i = 0; i < ii.length; i++) {
					ii[i]=il.get(i);
				}
				prop.put(s, ii);
				return ii;
			}
		}
		
		return null;
	}
	
	/**
	 * Connects global property with a Java bean property on target object, 
	 * both must be of boolean type. 
	 * It listens to change events on both sides and update opposite side. 
	 *  
	 * @param globalProperty the global property to listen to or set
	 * @param target the Java bean with target property
	 * @param targetProperty Java bean property name on target
	 * @param getter getter method name on target, if null, then changes on target are not relayed to global property 
	 * @param setter setter method name on target, must be non-null
	 * @param defaultValue default value to be used if global property is not set
	 */
	public void connectBooleanProperty(String globalProperty, Object target, String targetProperty, String getter, String setter, boolean defaultValue) {
		addPropertyChangeListener(globalProperty, new BooleanPropertyConnector(globalProperty, target, targetProperty, getter, setter, defaultValue));
	}
}
