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

package com.gtdfree.test.odb;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.Objects;
import org.neodatis.odb.xml.XMLExporter;
import org.neodatis.odb.xml.XMLImporter;

public class Test1 extends TestCase {
	
	static public class Bird {
		public String name;
		public int size;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getSize() {
			return size;
		}
		public void setSize(int size) {
			this.size = size;
		}
		public Bird(String name, int size) {
			super();
			this.name = name;
			this.size = size;
		}
		
		
	}
	
	 static public class Flock {
			protected List<Bird> birds;
			
			public Flock(){
				birds = new ArrayList<Bird>();
			}
			
			public void add(Bird b) {
				birds.add(b);
			}
			
			public int size() {
				return birds.size();
			}

			public List<Bird> getBirds() {
				return birds;
			}

			public void setBirds(List<Bird> birds) {
				this.birds = birds;
			}
			

		}

	
	public void test2() throws Exception{
		String baseName = "odbtest.data";
		
		File f= new File(baseName);
		f.delete();
		f= new File(baseName+"2");
		f.delete();

		System.out.println(baseName);
		
		ODB odb = ODBFactory.open(baseName);

		Bird b= new Bird("b4", 4);
		odb.store(b);

		b.size=11;
		odb.store(b);
		
		Flock flock = new Flock();
		flock.add(b);
		odb.store(f);
		
		flock.add(new Bird("b1", 1));
		flock.add(new Bird("b2", 2));
		flock.add(new Bird("b3", 3));
		
		odb.store(flock);

		Objects<Flock> of = odb.getObjects(Flock.class);
		assertEquals(1, of.size());

		odb.commit();
		
		//odb = ODBFactory.open(baseName);
		XMLExporter exporter = new XMLExporter(odb);
		exporter.export(".", baseName+".xml");
		odb.close();
		
		odb = ODBFactory.open(baseName+"2");
		XMLImporter importer = new XMLImporter(odb);
		importer.importFile(".", baseName+".xml");
		odb.close();
		
		odb = ODBFactory.open(baseName+"2");
		Objects<Flock> flocks = odb.getObjects(Flock.class);
		assertEquals(1, flocks.size());
		assertEquals(4, flocks.getFirst().size());
		odb.close();
		
	}
	
	public void test1() throws Exception{
		String baseName = "odbtest.data";
		
		File f= new File(baseName);
		f.delete();
		f= new File(baseName+"2");
		f.delete();
		System.out.println(baseName);

		Flock flock = new Flock();
		flock.getBirds().add(new Bird("b1", 1));
		flock.getBirds().add(new Bird("b2", 2));
		flock.getBirds().add(new Bird("b3", 3));
		
		ODB odb = ODBFactory.open(baseName);
		
		odb.store(flock);
		odb.commit();
		
		//odb = ODBFactory.open(baseName);
		XMLExporter exporter = new XMLExporter(odb);
		exporter.export(".", baseName+".xml");
		
		flock.getBirds().add(new Bird("b4", 3));
		odb.store(flock);

		odb.close();
		
		odb = ODBFactory.open(baseName+"2");
		XMLImporter importer = new XMLImporter(odb);
		importer.importFile(".", baseName+".xml");
		odb.close();
		
		odb = ODBFactory.open(baseName+"2");
		Objects<Flock> flocks = odb.getObjects(Flock.class);
		assertEquals(1, flocks.size());
		assertEquals(3, flocks.getFirst().getBirds().size());
		odb.close();
		
	}
	
}
