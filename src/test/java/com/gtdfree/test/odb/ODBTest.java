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

/**
 * @author ikesan
 * 
 */
public class ODBTest extends TestCase {

	
	static class Bird {
		public String name;

		public int size;

		public Bird() {
		}

		public Bird(String n, int s) {
			name = n;
			size = s;
		}
	}

	static class Flock {
		public List<Bird> birds = new ArrayList<Bird>();

		public void add(Bird b) {
			birds.add(b);
		}

		public int size() {
			return birds.size();
		}

		public Bird get(int i) {
			return birds.get(i);
		}
		
		public List<Bird> getBirds() {
			return birds;
		}
		
		public void setBirds(List<Bird> birds) {
			this.birds = birds;
		}
	}

	File f = new File("odb.data");

	public void testExportImport() throws Exception {
		
		f.delete();
		ODB odb= ODBFactory.open(f.toString());
		
		Bird b= new Bird("Duck",10);
		
		odb.store(b);
		
		b.size=11;
		
		odb.store(b);
		
		Flock fl = new Flock();
		fl.add(b);

		odb.store(fl);

		fl.add(new Bird("Lame Duck", 3));
		b.size = 12;
		odb.store(fl);

		Objects<Flock> of = odb.getObjects(Flock.class);
		assertEquals(1, of.size());

		odb.commit();
		//odb= ODBFactory.open(f.toString());

		File ex= new File("export.xml");
		
		XMLExporter exp= new XMLExporter(odb);
		exp.export(ex.getAbsoluteFile().getParent(), ex.getName());
		odb.close();
		
		f.delete();
		odb= ODBFactory.open(f.toString());
		
		//odb.store(new Flock());
		
		XMLImporter imp= new XMLImporter(odb);
		imp.importFile(ex.getAbsoluteFile().getParent(), ex.getName());
		
		odb.close();
		
		odb= ODBFactory.open(f.toString());

		Objects<Bird> ob= odb.getObjects(Bird.class);
		assertEquals(2, ob.size());
		
		Bird b1= ob.getFirst();
		assertEquals(12, b1.size);


		of = odb.getObjects(Flock.class);
		assertEquals(1, of.size());

		fl = of.getFirst();

		assertEquals(2, fl.size());
		assertEquals(12, fl.get(0).size);
		assertEquals(3, fl.get(1).size);

		odb.close();
	}
}
