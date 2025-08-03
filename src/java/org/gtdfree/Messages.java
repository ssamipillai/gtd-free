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

import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME = "org.gtdfree.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);
	
	private static Boolean utf8Convert;

	private static final String ENCODING = "@.ENCODING"; //$NON-NLS-1$

	private Messages() {
	}

	public static String getString(String key) {
		if (utf8Convert==null) {
			if (RESOURCE_BUNDLE.containsKey(ENCODING)) {
				try {
					String s= RESOURCE_BUNDLE.getString(ENCODING);
					utf8Convert= 
						s!=null && StandardCharsets.UTF_8.name().equalsIgnoreCase(s);
				} catch (MissingResourceException e) {
					// ignore
				}
			} 
			
			if (utf8Convert==null) {
				utf8Convert= Boolean.FALSE;
			}
		}

		try {
		String message= RESOURCE_BUNDLE.getString(key);
			
			if (utf8Convert) {
				try {
					// converts scrambled UTF-8 characters withing ISO_8859_1 into proper UTF-8
					message= new String(message.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8) ;
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
			return message;
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
