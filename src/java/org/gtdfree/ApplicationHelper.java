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

import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.ImageProducer;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import org.apache.log4j.Logger;

/**
 * @author ikesan
 *
 */
public final class ApplicationHelper {

	public static final String EMPTY_STRING= ""; //$NON-NLS-1$
	
	private static File dataFile;
	private static File dataFolder;
	private static FileLock exclusiveLock;

	public static String DEFAULT_DATE_FORMAT_STRING=                         "EE dd/MMM yy"; //$NON-NLS-1$
	// Thread-safe date formatters using ThreadLocal
	private static final ThreadLocal<SimpleDateFormat> defaultDateFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat(DEFAULT_DATE_FORMAT_STRING));
	private static final ThreadLocal<SimpleDateFormat> isoDateTimeFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")); //$NON-NLS-1$
	private static final ThreadLocal<SimpleDateFormat> isoDateFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd")); //$NON-NLS-1$
	private static final ThreadLocal<SimpleDateFormat> readableISODateTimeFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")); //$NON-NLS-1$
	
	// Use WeakHashMap for memory-efficient icon caching
	private static Map<String, ImageIcon> iconCache = new WeakHashMap<>();
	
	// Font cache for reusing Font objects
	private static final Map<String, Font> fontCache = new HashMap<>();
	
	// Cache for font availability checks
	private static final Map<String, Boolean> fontAvailabilityCache = new HashMap<>();

	// Cache for disabled icons to reduce memory usage and improve performance
	private static final Map<String, ImageIcon> disabledIconCache = new WeakHashMap<>();

	
	public static final String DATA_PROPERTY= "gtd-free.data"; //$NON-NLS-1$
	public static final String TITLE_PROPERTY= "gtd-free.title"; //$NON-NLS-1$
	public static final String LOCK_FILE_NAME= "gtd-free.lock"; //$NON-NLS-1$
	public static final String DEFAULT_DATA_FILE_NAME= "gtd-free-data.xml"; //$NON-NLS-1$
	public static final String SHUTDOWN_BACKUP_XML_DATA_FILE_NAME= "gtd-free-data.shutdown_backup.xml"; //$NON-NLS-1$
	public static final String BACKUP_DATA_FILE_NAME_PART= "gtd-free-data.backup"; //$NON-NLS-1$
	public static final String DEFAULT_DATA_FOLDER_NAME= ".gtd-free"; //$NON-NLS-1$
	public static final String CONFIGURATION_FILE_NAME="gtd-free-config.properties"; //$NON-NLS-1$
	public static final String OPTIONS_FILE_NAME="gtd-free-options.properties"; //$NON-NLS-1$

	public static String icon_name_large_add= "icons/gnome/16x16/actions/list-add.png"; //$NON-NLS-1$
	public static String icon_name_large_about= "icons/gnome/16x16/actions/help-about.png"; //$NON-NLS-1$
	public static String icon_name_large_browser= "icons/gnome/16x16/apps/web-browser.png"; //$NON-NLS-1$
	public static String icon_name_large_clear= "icons/gnome/16x16/actions/edit-clear.png"; //$NON-NLS-1$
	public static String icon_name_large_clone= "icons/gnome/16x16/actions/edit-copy.png"; //$NON-NLS-1$
	public static String icon_name_large_collecting= "icons/gnome/32x32/stock/generic/stock_notes.png"; //$NON-NLS-1$
	public static String icon_name_large_delete= "icons/gnome/16x16/actions/edit-delete.png"; //$NON-NLS-1$
	public static String icon_name_large_exit= "icons/gnome/16x16/actions/application-exit.png"; //$NON-NLS-1$
	public static String icon_name_large_export= "icons/Humanity/16x16/actions/document-export.png"; //$NON-NLS-1$
	public static String icon_name_large_import= "icons/Humanity/16x16/actions/document-import.png"; //$NON-NLS-1$
	public static String icon_name_large_journaling= "icons/gnome/16x16/stock/form/stock_form-time-field.png"; //$NON-NLS-1$
	public static String icon_name_large_logo = "splash48.png"; //$NON-NLS-1$
	public static String icon_name_large_move= "icons/gnome/16x16/actions/go-next.png"; //$NON-NLS-1$
	public static String icon_name_large_new= "icons/gnome/16x16/actions/document-new.png"; //$NON-NLS-1$
	public static String icon_name_large_print= "icons/gnome/16x16/actions/document-print.png"; //$NON-NLS-1$
	public static String icon_name_large_processing= "icons/gnome/32x32/actions/system-run.png"; //$NON-NLS-1$
	public static String icon_name_large_queue_execute= "icons/gnome/32x32/emblems/emblem-important.png"; //$NON-NLS-1$
	public static String icon_name_large_queue_off= "icons/gnome/16x16/emblems/emblem-important-gray.png"; //$NON-NLS-1$
	public static String icon_name_large_queue_on= "icons/gnome/16x16/emblems/emblem-important.png"; //$NON-NLS-1$
	public static String icon_name_large_rename= "icons/gnome/16x16/actions/gtk-edit.png"; //$NON-NLS-1$
	public static String icon_name_large_resolve= "icons/Human/16x16/actions/dialog-apply.png"; //$NON-NLS-1$
	public static String icon_name_large_review="icons/gnome/32x32/apps/gnome-searchtool-animation-rest.png"; //$NON-NLS-1$
	public static String icon_name_large_save= "icons/gnome/16x16/actions/document-save.png"; //$NON-NLS-1$
	public static String icon_name_large_search= "icons/gnome/16x16/actions/edit-find.png"; //$NON-NLS-1$
	public static String icon_name_large_splash= "splash24.png"; //$NON-NLS-1$
	public static String icon_name_large_undelete= "icons/gnome/16x16/stock/generic/stock_undelete.png"; //$NON-NLS-1$
	public static String icon_name_large_tray_splash = "splash24.png"; //$NON-NLS-1$
	public static String icon_name_large_update= "icons/Tango/16x16/apps/system-software-update.png"; //$NON-NLS-1$
	
	public static String icon_name_small_add= "icons/gnome/12x12/actions/list-add.png"; //$NON-NLS-1$
	public static String icon_name_small_clear= "icons/gnome/12x12/actions/edit-clear.png"; //$NON-NLS-1$
	public static String icon_name_small_collecting= "icons/gnome/16x16/stock/generic/stock_notes.png"; //$NON-NLS-1$
	public static String icon_name_small_copy= "icons/gnome/12x12/actions/edit-copy.png"; //$NON-NLS-1$
	public static String icon_name_small_cut= "icons/gnome/12x12/actions/edit-cut.png"; //$NON-NLS-1$
	public static String icon_name_small_delete= "icons/gnome/12x12/actions/edit-delete.png"; //$NON-NLS-1$
	public static String icon_name_small_down= "icons/gnome/12x12/actions/go-down.png"; //$NON-NLS-1$
	public static String icon_name_small_folded= "icons/gnome/12x12/stock/data/stock_data-next.png"; //$NON-NLS-1$
	public static String icon_name_small_queue_execute= "icons/gnome/16x16/emblems/emblem-important.png"; //$NON-NLS-1$
	public static String icon_name_small_queue_off= "icons/gnome/12x12/emblems/emblem-important-gray.png"; //$NON-NLS-1$
	public static String icon_name_small_queue_on= "icons/gnome/12x12/emblems/emblem-important.png"; //$NON-NLS-1$
	//public static String icon_name_small_rename= "icons/Tango/12x12/actions/view-refresh.png";
	public static String icon_name_small_next= "icons/gnome/12x12/actions/go-next.png"; //$NON-NLS-1$
	public static String icon_name_small_paste= "icons/gnome/12x12/actions/edit-paste.png"; //$NON-NLS-1$
	public static String icon_name_small_previous= "icons/gnome/12x12/actions/go-previous.png"; //$NON-NLS-1$
	public static String icon_name_small_processing= "icons/gnome/16x16/actions/system-run.png"; //$NON-NLS-1$
	public static String icon_name_small_overview= "splash16.png"; //$NON-NLS-1$
	public static String icon_name_small_redo= "icons/gnome/12x12/actions/edit-redo.png"; //$NON-NLS-1$
	public static String icon_name_small_remove= "icons/gnome/12x12/actions/list-remove.png"; //$NON-NLS-1$
	public static String icon_name_small_rename= "icons/gnome/12x12/actions/gtk-edit.png"; //$NON-NLS-1$
	public static String icon_name_small_resolve= "icons/Human/12x12/actions/dialog-apply.png"; //$NON-NLS-1$
	public static String icon_name_small_review="icons/gnome/16x16/stock/navigation/stock_zoom-page.png"; //$NON-NLS-1$
	public static String icon_name_small_search= "icons/gnome/12x12/actions/edit-find.png"; //$NON-NLS-1$
	public static String icon_name_small_select_all= "icons/gnome/12x12/actions/edit-select-all.png"; //$NON-NLS-1$
	public static String icon_name_small_splash= "splash16.png"; //$NON-NLS-1$
	public static String icon_name_small_start= "icons/gnome/12x12/actions/media-playback-start.png"; //$NON-NLS-1$
	public static String icon_name_small_tray_splash= "splash16.png"; //$NON-NLS-1$
	public static String icon_name_small_undelete= "icons/gnome/12x12/stock/generic/stock_undelete.png"; //$NON-NLS-1$
	public static String icon_name_small_undo= "icons/gnome/12x12/actions/edit-undo.png"; //$NON-NLS-1$
	public static String icon_name_small_unfolded= "icons/gnome/12x12/stock/data/stock_data-down.png"; //$NON-NLS-1$
	public static String icon_name_small_up= "icons/gnome/12x12/actions/go-up.png"; //$NON-NLS-1$
	
	private static ThreadPoolExecutor backgroundExecutor;

	private static Boolean gtklaf;

	private static int defaultFieldHeigth;

	public static final String icon_name_tiny_options = "icons/gnome/9x9/stock/data/stock_data-down.png"; //$NON-NLS-1$

	public static final String icon_name_small_star_yellow = "icons/Neu/12x12/actions/help-about.png"; //$NON-NLS-1$
	public static final String icon_name_small_star_orange = "icons/Neu/12x12/actions/help-about-orange.png"; //$NON-NLS-1$
	public static final String icon_name_small_star_red = "icons/Neu/12x12/actions/help-about-red.png"; //$NON-NLS-1$
	public static final String icon_name_small_star_grey = "icons/Neu/12x12/actions/help-about-grey.png"; //$NON-NLS-1$
	public static final String icon_name_small_star_blue = "icons/Neu/12x12/actions/help-about-blue.png"; //$NON-NLS-1$



	
	/**
	 * Get a cached disabled version of an icon
	 */
	public static ImageIcon getCachedDisabledIcon(String iconName) {
		synchronized (disabledIconCache) {
			ImageIcon disabled = disabledIconCache.get(iconName);
			if (disabled == null) {
				ImageIcon original = ApplicationHelper.getIcon(iconName);
				if (original != null) {
					disabled = new ImageIcon(GrayFilter.createDisabledImage(original.getImage()));
					disabledIconCache.put(iconName, disabled);
				}
			}
			return disabled;
		}
	}

	/**
	 * Get the default date format for thread-safe usage
	 */
	public static SimpleDateFormat getDefaultDateFormat() {
		return defaultDateFormat.get();
	}
	
	/**
	 * Get the ISO date format for thread-safe usage
	 */
	public static SimpleDateFormat getISODateFormat() {
		return isoDateFormat.get();
	}
	
	/**
	 * Get the ISO date-time format for thread-safe usage
	 */
	public static SimpleDateFormat getISODateTimeFormat() {
		return isoDateTimeFormat.get();
	}
	
	/**
	 * Get the readable ISO date-time format for thread-safe usage
	 */
	public static SimpleDateFormat getReadableISODateTimeFormat() {
		return readableISODateTimeFormat.get();
	}

	public static ImageIcon getIcon(String name) {
		// Synchronize access to the WeakHashMap
		synchronized (iconCache) {
			ImageIcon i = iconCache.get(name);
			if (i != null) {
				return i;
			}
			
			i = loadIcon(name);
			if (i != null) {
				iconCache.put(name, i);
			}
			return i;
		}
	}

	/**
	 * Clear all caches to free memory (useful during low memory conditions)
	 */
	public static synchronized void clearCaches() {
		synchronized (iconCache) {
			iconCache.clear();
		}
		synchronized (fontCache) {
			fontCache.clear();
		}
		synchronized (fontAvailabilityCache) {
			fontAvailabilityCache.clear();
		}
		synchronized (disabledIconCache) {
			disabledIconCache.clear();
		}
		// Suggest garbage collection (non-blocking hint)
		System.gc();
		Logger.getLogger(ApplicationHelper.class).debug("Cleared all caches and suggested garbage collection");
	}
	
	/**
	 * Get cache statistics for monitoring
	 */
	public static String getCacheStats() {
		synchronized (iconCache) {
			int iconCacheSize = iconCache.size();
			int disabledIconCacheSize;
			int fontCacheSize;
			int fontAvailCacheSize;
			
			synchronized (disabledIconCache) {
				disabledIconCacheSize = disabledIconCache.size();
			}
			synchronized (fontCache) {
				fontCacheSize = fontCache.size();
			}
			synchronized (fontAvailabilityCache) {
				fontAvailCacheSize = fontAvailabilityCache.size();
			}
			
			return String.format("Caches - Icons: %d, DisabledIcons: %d, Fonts: %d, FontAvail: %d", 
				iconCacheSize, disabledIconCacheSize, fontCacheSize, fontAvailCacheSize);
		}
	}

	public static final ImageIcon loadIcon(String resource) {
        try {
        	URL url = ApplicationHelper.class.getClassLoader().getResource(resource);
        	if (url == null) return null;
        	
        	// Check content type more efficiently
        	Object content = url.getContent();
        	if (!(content instanceof ImageProducer)) return null;
        	
            return new ImageIcon(url);
        } catch (Exception e) {
        	Logger.getLogger(ApplicationHelper.class).debug("Failed to load icon: " + resource, e);
            return null;
        }
    }
    
    public static final Image loadImage(String resource) {
        try {
        	URL url = ApplicationHelper.class.getClassLoader().getResource(resource);
        	if (url == null) return null;
        	
        	// Check content type more efficiently
        	Object content = url.getContent();
        	if (!(content instanceof ImageProducer)) return null;
        	
            return new ImageIcon(url).getImage();
        } catch (Exception e) {
        	Logger.getLogger(ApplicationHelper.class).debug("Failed to load image: " + resource, e);
            return null;
        }
    }
    
    public static byte[] loadResource(String name) {
    	
    	InputStream is=null;
    	
    	try {
    		
    		is = ApplicationHelper.class.getResourceAsStream(name);
    		
    		if (is==null) {
    			is= ClassLoader.getSystemClassLoader().getResourceAsStream(name);
    			if (is==null) {
    				
    				File f= new File(name);
    				if (f.exists()) {		
    					is= new BufferedInputStream(new FileInputStream(f));
    				}
    				
    			} else {
    				Logger.getLogger(ApplicationHelper.class).info("Loaded '"+name+"' with SystemClassLoader"); //$NON-NLS-1$ //$NON-NLS-2$
    			}
    		} else {
    			Logger.getLogger(ApplicationHelper.class).info("Loaded '"+name+"' with ApplicationHelper"); //$NON-NLS-1$ //$NON-NLS-2$
    		}
    		
    		if (is==null) {
    			Logger.getLogger(ApplicationHelper.class).error("Failed to load '"+name+"'."); //$NON-NLS-1$ //$NON-NLS-2$
    			return null;
    		}
    		
    		// Use NIO-optimized reading with proper buffering
    		try (ByteArrayOutputStream os = new ByteArrayOutputStream(Math.max(is.available(), 1024))) {
    			byte[] buffer = new byte[8192]; // Larger buffer for better I/O performance
				int bytesRead;
				while ((bytesRead = is.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				return os.toByteArray();
			}

			
		} catch (Exception e) {
			Logger.getLogger(ApplicationHelper.class).warn("Error loading resource: " + name, e);
			return null;
		} finally {
			if (is!=null) {
				try {
					is.close();
				} catch (IOException e) {
					Logger.getLogger(ApplicationHelper.class).debug("Error closing resource stream", e);
				}
			}
		}
    }
    
    public static final String formatLongISO(Date d) {
    	return isoDateTimeFormat.get().format(d);
    }
    public static final Date parseLongISO(String d) throws ParseException {
    	return isoDateTimeFormat.get().parse(d);
    }
    
	public static final File getDataFolder() {
		if (dataFolder==null) {

			String s= System.getProperty(DATA_PROPERTY);

			if (s!=null) {
				File f= new File(s);
				if (!f.exists()) {
					f.mkdirs();
				}
				if (f.isDirectory()) {
					dataFolder= f;
				} else {
					dataFolder= f.getParentFile();
					dataFile=f;
				}
			}
			
			if (dataFolder==null) {
				dataFolder= new File(System.getProperty("user.home")); //$NON-NLS-1$
				dataFolder= new File(dataFolder,DEFAULT_DATA_FOLDER_NAME);
				
				System.getProperties().setProperty(DATA_PROPERTY, dataFolder.toString());
			}
			if (!dataFolder.exists()) {
				dataFolder.mkdirs();
			}
		}
		return dataFolder;
	}

	public static File getDataFile() {
		if (dataFile==null) {
			dataFile= new File(getDataFolder(),DEFAULT_DATA_FILE_NAME);
		}
		return dataFile;
	}
	
	public static File createBackupDataFile(File f, int i) {
		if (f.getName().toLowerCase().endsWith(".xml")) { //$NON-NLS-1$
			String s= f.getAbsolutePath();
			return new File(s.substring(0, s.length()-4)+"backup"+i+".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return new File(f.getParentFile(),BACKUP_DATA_FILE_NAME_PART+i+".xml"); //$NON-NLS-1$
	}

	public synchronized static final boolean tryLock(File location) {
		if (location==null) {
			location= getDataFolder();
		}
		if (exclusiveLock!=null) {
			return false;
		}
		try (RandomAccessFile raf = new RandomAccessFile(new File(location,LOCK_FILE_NAME),"rw"); //$NON-NLS-1$
		     FileChannel lock = raf.getChannel()) {
			exclusiveLock= lock.tryLock();
		} catch (Exception e) {
			return false;
		}
		return exclusiveLock!=null;
	}

	public synchronized static final void releaseLock() {
		if (exclusiveLock!=null) {
			try {
				if (exclusiveLock.isValid()) {
					exclusiveLock.release();
				}
			} catch (java.nio.channels.ClosedChannelException e) {
				// Normal during shutdown - channel was already closed, ignore silently
			} catch (IOException e) {
				Logger.getLogger(ApplicationHelper.class).debug("Lock release failed (normal during shutdown)", e);
			} catch (Exception e) {
				Logger.getLogger(ApplicationHelper.class).debug("Unexpected error during lock release", e);
			}
			exclusiveLock=null;
		}
	}
	
	public static Properties loadConfiguration() {
		Properties p= new Properties();
		InputStream in= ApplicationHelper.class.getClassLoader().getResourceAsStream(CONFIGURATION_FILE_NAME);
		if (in!=null) {
			try {
				p.load(in);
			} catch (Exception e) {
				Logger.getLogger(ApplicationHelper.class).error("Initialization error.", e); //$NON-NLS-1$
			}
		}
		p.putAll(System.getProperties());
		return p;
	}
	
	public static String loadLicense() {
		return "This program is free software: you can redistribute it and/or modify\nit under the terms of the GNU General Public License as published by\nthe Free Software Foundation, either version 3 of the License, or\n(at your option) any later version.\n\nThis program is distributed in the hope that it will be useful,\nbut WITHOUT ANY WARRANTY; without even the implied warranty of\nMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\nGNU General Public License for more details.\n\nYou should have received a copy of the GNU General Public License\nalong with this program.  If not, see <http://www.gnu.org/licenses/>."; //$NON-NLS-1$
	}

	public static String toDateString(Date date) {
		if (date==null) {
			return ApplicationHelper.EMPTY_STRING;
		}
		return defaultDateFormat.get().format(date);
	}
	public static String toISODateString(Date date) {
		if (date==null) {
			return ApplicationHelper.EMPTY_STRING;
		}
		return isoDateFormat.get().format(date);
	}
	/**
	 * Prints something like: '2009-02-24 14:35:06'
	 * @param date date, if null empty string is returned 
	 * @return something like: '2009-02-24 14:35:06'
	 */
	public static String toISODateTimeString(Date date) {
		if (date==null) {
			return ApplicationHelper.EMPTY_STRING;
		}
		return readableISODateTimeFormat.get().format(date);
	}

	public static void changeDefaultFontSize(float size, String key) {
		Font f= UIManager.getDefaults().getFont(key+".font"); //$NON-NLS-1$
		if (f!=null) {
			UIManager.getDefaults().put(key+".font", new FontUIResource(f.deriveFont(f.getSize()+size))); //$NON-NLS-1$
		}
	}
	public static void changeDefaultFontStyle(int style, String key) {
		Font f= UIManager.getDefaults().getFont(key+".font"); //$NON-NLS-1$
		if (f!=null) {
			UIManager.getDefaults().put(key+".font", new FontUIResource(f.deriveFont(style))); //$NON-NLS-1$
		}
	}
	
	/**
	 * Get a cached font or create and cache a new one
	 */
	private static Font getCachedFont(String fontName, int style, int size) {
		String key = fontName + "|" + style + "|" + size;
		synchronized (fontCache) {
			Font cachedFont = fontCache.get(key);
			if (cachedFont == null) {
				cachedFont = new Font(fontName, style, size);
				fontCache.put(key, cachedFont);
			}
			return cachedFont;
		}
	}
	
	/**
	 * Check if a font is available with caching
	 */
	private static boolean isFontAvailable(String fontName) {
		synchronized (fontAvailabilityCache) {
			Boolean available = fontAvailabilityCache.get(fontName);
			if (available == null) {
				String[] availableFonts = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
				available = false;
				for (String name : availableFonts) {
					if (name.equals(fontName)) {
						available = true;
						break;
					}
				}
				fontAvailabilityCache.put(fontName, available);
			}
			return available;
		}
	}

	/**
	 * Configures modern UI fonts with memory-efficient caching
	 */
	public static void configureModernUIFonts() {
		try {
			// Select font with caching
			String selectedFontName;
			Logger logger = Logger.getLogger(ApplicationHelper.class);
			if (isFontAvailable("Gill Sans MT")) {
				selectedFontName = "Gill Sans MT";
				if (logger.isDebugEnabled()) {
					logger.debug("Using Gill Sans MT font for UI");
				}
			} else if (isFontAvailable("Segoe UI")) {
				selectedFontName = "Segoe UI";
				if (logger.isDebugEnabled()) {
					logger.debug("Using Segoe UI font for UI (Gill Sans MT not available)");
				}
			} else {
				selectedFontName = Font.SANS_SERIF;
				if (logger.isDebugEnabled()) {
					logger.debug("Using default sans serif font for UI (neither Gill Sans MT nor Segoe UI available)");
				}
			}
			
			// Use cached fonts for better memory usage
			Font primaryFont = getCachedFont(selectedFontName, Font.PLAIN, 13);
			Font smallFont = getCachedFont(selectedFontName, Font.PLAIN, 12);
			Font largeFont = getCachedFont(selectedFontName, Font.PLAIN, 14);
			
			// Set fonts for various UI components
			UIManager.put("Button.font", new FontUIResource(primaryFont));
			UIManager.put("Label.font", new FontUIResource(primaryFont));
			UIManager.put("TextField.font", new FontUIResource(primaryFont));
			UIManager.put("TextArea.font", new FontUIResource(primaryFont));
			UIManager.put("ComboBox.font", new FontUIResource(primaryFont));
			UIManager.put("List.font", new FontUIResource(primaryFont));
			UIManager.put("Table.font", new FontUIResource(primaryFont));
			UIManager.put("Tree.font", new FontUIResource(primaryFont));
			UIManager.put("TabbedPane.font", new FontUIResource(primaryFont));
			UIManager.put("MenuBar.font", new FontUIResource(primaryFont));
			UIManager.put("Menu.font", new FontUIResource(primaryFont));
			UIManager.put("MenuItem.font", new FontUIResource(primaryFont));
			UIManager.put("PopupMenu.font", new FontUIResource(primaryFont));
			UIManager.put("CheckBox.font", new FontUIResource(primaryFont));
			UIManager.put("RadioButton.font", new FontUIResource(primaryFont));
			UIManager.put("TitledBorder.font", new FontUIResource(primaryFont));
			UIManager.put("ToolTip.font", new FontUIResource(smallFont));
			UIManager.put("Panel.font", new FontUIResource(primaryFont));
			UIManager.put("ScrollPane.font", new FontUIResource(primaryFont));
			UIManager.put("Viewport.font", new FontUIResource(primaryFont));
			
			// Set larger font for titles and headers
			UIManager.put("InternalFrame.titleFont", new FontUIResource(largeFont));
			
			if (logger.isDebugEnabled()) {
				logger.debug("Modern UI fonts (" + selectedFontName + ") configured successfully.");
			}
		} catch (Exception e) {
			Logger.getLogger(ApplicationHelper.class).warn("Failed to configure modern UI fonts", e);
		}
	}

	/**
	 * Configure Windows Look and Feel with modern enhancements
	 */
	public static void configureWindowsLookAndFeel() {
		Logger logger = Logger.getLogger(ApplicationHelper.class);
		try {
			String osName = System.getProperty("os.name");
			if (osName != null && osName.toLowerCase().contains("windows")) {
				// Use the native Windows Look and Feel for the most authentic Windows experience
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				
				// Enable modern Windows features
				System.setProperty("sun.java2d.uiScale", "1.0");
				System.setProperty("awt.useSystemAAFontSettings", "on");
				System.setProperty("swing.aatext", "true");
				
				if (logger.isDebugEnabled()) {
					logger.debug("Windows Look and Feel configured successfully");
				}
			} else {
				// For non-Windows systems, try to use the system L&F
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				if (logger.isDebugEnabled()) {
					logger.debug("System Look and Feel configured for non-Windows platform");
				}
			}
		} catch (Exception e) {
			logger.warn("Failed to configure Windows Look and Feel", e);
			try {
				// Fallback to cross-platform L&F
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
				if (logger.isDebugEnabled()) {
					logger.debug("Using cross-platform Look and Feel as fallback");
				}
			} catch (Exception e2) {
				logger.warn("Failed to set fallback Look and Feel", e2);
			}
		}
	}

	/**
	 * Escape in Java style in readable friendly way common control characters and other control characters.
	 * @param in input string
	 * @return escaped string in Java style
	 */
	public static String escapeControls(String in) {
		if (in ==null) {
			return null;
		}
		StringBuilder sb= new StringBuilder(in.length()+10);
		
		for (int i = 0; i < in.length(); i++) {
			char ch= in.charAt(i);
			
            switch (ch) {
                case '\b':
                    sb.append('\\');
                    sb.append('b');
                    break;
                case '\n':
                	sb.append('\\');
                	sb.append('n');
                    break;
                case '\t':
                	sb.append('\\');
                	sb.append('t');
                    break;
                case '\f':
                	sb.append('\\');
                	sb.append('f');
                    break;
                case '\r':
                	sb.append('\\');
                	sb.append('r');
                    break;
                /*case '\'':
                    if (escapeSingleQuote) {
                      out.write('\\');
                    }
                    out.write('\'');
                    break;*/
                case '\\':
                	sb.append('\\');
                	sb.append('\\');
                    break;
                default :
                	// TODO: this does not escape properly split characters. Should I care?  
        			if (Character.isISOControl(ch)) {
        				sb.append(String.format("\\u%04x", (int)ch));
        			} else {
        				sb.append(ch);
        			}
                    break;
            }

			
		}
		
		return sb.toString();
	}
	
	public static final String getVersion() {
		Properties p= loadConfiguration();
		String v= p.getProperty("build.version"); //$NON-NLS-1$
		String t= p.getProperty("build.type"); //$NON-NLS-1$
		if (t!=null && t.trim().length()>0) {
			return v.trim()+"-"+t.trim(); //$NON-NLS-1$
		}
		return v.trim();
	}
	
	public static synchronized void executeInBackground(Runnable r) {
		
		if (backgroundExecutor==null) {
			// Use bounded queue to prevent memory issues
			backgroundExecutor= new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,new ArrayBlockingQueue<>(100),new ThreadFactory() {
			
				@Override
				public Thread newThread(Runnable r) {
					Thread t= new Thread(r);
					t.setName("BackgroundExecutor-" + ThreadLocalRandom.current().nextInt(1000)); //$NON-NLS-1$
					t.setPriority(Thread.MIN_PRIORITY);
					t.setDaemon(true); // Changed to daemon to not prevent JVM shutdown
					return t;
				}
			});
			// Configure rejection policy for when queue is full
			backgroundExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
		}
		
		backgroundExecutor.execute(r);
		
	}
	
	public static final synchronized void stopBackgroundExecutor() {
		if (backgroundExecutor!=null) {
			backgroundExecutor.shutdownNow();
			backgroundExecutor=null;
		}
	}
	
	public static final Insets getDefaultFatButtonMargin() {
		if (isGTKLaF()) {
			return new Insets(0,2,0,2);
		}
		return new Insets(2,4,2,4);
	}
	
	public static final Insets getDefaultSlimButtonMargin() {
		if (isGTKLaF()) {
			return new Insets(0,0,0,0);
		}
		return new Insets(2,2,2,2);
	}

	public static final boolean isGTKLaF() {
		if (gtklaf == null) {
			try {
				gtklaf = Class.forName("com.sun.java.swing.plaf.gtk.GTKLookAndFeel").isAssignableFrom(UIManager.getLookAndFeel().getClass()); //$NON-NLS-1$
			} catch (Exception e) {
				gtklaf = Boolean.FALSE;
			}
		}
		return gtklaf;
	}
	
	public static File getShutdownBackupXMLFile() {
		return new File(ApplicationHelper.getDataFolder(),ApplicationHelper.SHUTDOWN_BACKUP_XML_DATA_FILE_NAME);
	}

	public static String getLogFileName() {
		File log= new File(getDataFolder(),"log"); //$NON-NLS-1$
		if (!log.exists()) {
			log.mkdirs();
		}
		return new File(log, "log.txt").toString(); //$NON-NLS-1$
	}

	public static int getDefaultFieldHeigth() {
		if (defaultFieldHeigth<=0) {
			JTextField jtf= new JTextField();
			jtf.setText("1234567890"); //$NON-NLS-1$
			defaultFieldHeigth = jtf.getPreferredSize().height;
			if (UIManager.getLookAndFeel() !=null &&  UIManager.getLookAndFeel().getClass().getName().equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")) { //$NON-NLS-1$
				defaultFieldHeigth-=4;
			}
			if (defaultFieldHeigth<21) {
				defaultFieldHeigth=21;
			}
		}
		
		return defaultFieldHeigth;
	}
}
