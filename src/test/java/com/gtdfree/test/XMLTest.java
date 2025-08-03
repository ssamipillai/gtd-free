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

package com.gtdfree.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.StringEscapeUtils;

import junit.framework.TestCase;

public class XMLTest extends TestCase {

	
	public void testRegularExpression() {

		try {

			Pattern pattern = Pattern.compile("<\\?.*?encoding\\s*?=.*?\\?>",Pattern.CASE_INSENSITIVE);	            
            Matcher matcher = pattern.matcher("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gtd-data version=\"2.1\" modified=\"2008-10-10T12:42:55.905+0200\">");
            System.out.println(matcher.toString());
            assertTrue(matcher.find());
            
            
            
			pattern = Pattern.compile("<\\?.*?encoding\\s*?=.*?\\?>",Pattern.CASE_INSENSITIVE);	            
            matcher = pattern.matcher("\n\r\n<?xml version=\"1.0\" encoding=\"UTF-8\"?><gtd-data version=\"2.1\" modified=\"2008-10-12T23:50:46.176+0200\">");
            System.out.println(matcher.toString());
            assertTrue(matcher.find());
            
			pattern = Pattern.compile("<\\?.*?encoding\\s*?=.*?\\?>",Pattern.CASE_INSENSITIVE);	            
            matcher = pattern.matcher("\n\r\n<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<gtd-data version=\"2.1\" modified=\"2008-10-12T23:50:46.176+0200\">");
            System.out.println(matcher.toString());
            assertTrue(matcher.find());

            
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		

	}
	
	
	public void testParserEncoding() {
		
		try {
			
			File file= new File("./src/test/resources/gtd-free-data_2.1.xml");
			InputStream is= new FileInputStream(file);
			XMLStreamReader r= XMLInputFactory.newInstance().createXMLStreamReader(is);
			System.out.println(r.getEncoding());
			assertEquals("UTF-8", r.getEncoding());
			while (r.hasNext()) {
				r.next();
			}
			r.close();
			is.close();
			
			file= new File("./src/test/resources/gtd-free-data_WIN1250_2.1.xml");
			is= new FileInputStream(file);
			r= XMLInputFactory.newInstance().createXMLStreamReader(is);
			System.out.println(r.getEncoding());
			assertEquals("UTF-8", r.getEncoding());
			try {
				while (r.hasNext()) {
					r.next();
				}
				fail("This should not happend.");
			} catch (Exception e) {
				//e.printStackTrace();
			}
			r.close();
			is.close();
			
			file= new File("./src/test/resources/gtd-free-data_2.1_enc.xml");
			is= new FileInputStream(file);
			r= XMLInputFactory.newInstance().createXMLStreamReader(is);
			System.out.println(r.getEncoding());
			assertEquals("UTF-8", r.getEncoding());
			while (r.hasNext()) {
				r.next();
			}
			r.close();
			is.close();

			file= new File("./src/test/resources/gtd-free-data_WIN1250_2.1_enc.xml");
			is= new FileInputStream(file);
			r= XMLInputFactory.newInstance().createXMLStreamReader(is);
			System.out.println(r.getEncoding());
			assertEquals("WINDOWS-1250", r.getEncoding());
			while (r.hasNext()) {
				r.next();
			}
			r.close();
			is.close();

		} catch (Exception e) {
			
			e.printStackTrace();
			fail(e.getMessage());
			
		}
		
	}
	
	public void testEscapes() {
		String bad= "aa"+'\n'+"d";
		
		System.out.println(bad);
		System.out.println(StringEscapeUtils.escapeXml(bad));
		System.out.println(StringEscapeUtils.escapeJava(bad));

		
	}

}
