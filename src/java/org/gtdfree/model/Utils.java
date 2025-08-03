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

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * @author ikesan
 *
 */
public final class Utils {
	
	private static final Calendar cal= new GregorianCalendar();
	public static final long MILLISECONDS_IN_DAY= 24*60*60*1000;
	
	private Utils() {
	}
	
	public static long today() {
		return today(cal);
	}
	public static long today(Calendar cal) {
		cal.setTimeInMillis(System.currentTimeMillis());
		int day= cal.get(Calendar.DAY_OF_MONTH);
		int month= cal.get(Calendar.MONTH);
		int year= cal.get(Calendar.YEAR);
		cal.clear();
		cal.set(year, month, day);
		return cal.getTimeInMillis();
	}

}
