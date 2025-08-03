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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.gtdfree.addons.ExportAddOn;
import org.gtdfree.model.Action;
import org.gtdfree.model.ActionsCollection;
import org.gtdfree.model.ConsistencyException;
import org.gtdfree.model.DummyFilter;
import org.gtdfree.model.Folder;
import org.gtdfree.model.GTDModel;
import org.gtdfree.model.Project;
import org.gtdfree.model.Folder.FolderType;

/**
 * @author ikesan
 *
 */
public class ActionsCollectionTest extends TestCase {

	private GTDModel gtdModel;
	private Folder f1;
	private Project p1;
	private Folder f2;
	private Folder f3;
	private Project p2;
	private Project p3;
	private Folder f4;
	private Folder f5;
	private Project p4;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		gtdModel= new GTDModel(null);
		
		f1= gtdModel.createFolder("F1", FolderType.ACTION);
		f2= gtdModel.createFolder("F2", FolderType.ACTION);
		f3= gtdModel.createFolder("F3", FolderType.ACTION);
		f4= gtdModel.createFolder("F4", FolderType.ACTION);
		f5= gtdModel.createFolder("F5", FolderType.ACTION);

		p1= (Project)gtdModel.createFolder("P1", FolderType.PROJECT);
		p2= (Project)gtdModel.createFolder("P2", FolderType.PROJECT);
		p3 = (Project)gtdModel.createFolder("P3", FolderType.PROJECT);
		p4 = (Project)gtdModel.createFolder("P4", FolderType.PROJECT);

		gtdModel.createAction(f1, "F1A6");
		gtdModel.createAction(f1, "F1A5");
		Action a14= gtdModel.createAction(f1, "F1P2A4");
		Action a13= gtdModel.createAction(f1, "F1P2A3");
		Action a12= gtdModel.createAction(f1, "F1P1A2");
		Action a11= gtdModel.createAction(f1, "F1P1A1");

		gtdModel.createAction(f2, "F2A6");
		gtdModel.createAction(f2, "F2A5");
		Action a24= gtdModel.createAction(f2, "F2P2A4");
		Action a23= gtdModel.createAction(f2, "F2P2A3");
		Action a22= gtdModel.createAction(f2, "F2P1A2");
		Action a21= gtdModel.createAction(f2, "F2P1A1");

		gtdModel.createAction(f3, "F3A2");
		Action a31= gtdModel.createAction(f3, "F3P3A1");

		gtdModel.createAction(f4, "F4A2");
		Action a41= gtdModel.createAction(f4, "F4P1A1");


		a11.setProject(p1.getId());
		a12.setProject(p1.getId());
		a13.setProject(p2.getId());
		a14.setProject(p2.getId());

		a21.setProject(p1.getId());
		a22.setProject(p1.getId());
		a23.setProject(p2.getId());
		a24.setProject(p2.getId());

		a31.setProject(p3.getId());

		a41.setProject(p1.getId());

		assertEquals(15, gtdModel.size());
		assertEquals(6, f1.size());
		assertEquals(6, f2.size());
		assertEquals(2, f3.size());
		assertEquals(2, f4.size());
		assertEquals(0, f5.size());
		assertEquals(5, p1.size());
		assertEquals(4, p2.size());
		assertEquals(1, p3.size());
		assertEquals(0, p4.size());
		
		assertEquals(a13,f1.get(2));
		assertEquals(a21,f2.get(0));
		assertEquals(a23,f2.get(2));
		assertEquals(a31,f3.get(0));
		assertEquals(a41,f4.get(0));

		assertEquals(f1.get(0), p1.get(0));
		assertEquals(f1.get(2), p2.get(0));
		assertEquals(f2.get(0), p1.get(2));
		assertEquals(f2.get(2), p2.get(2));
		assertEquals(f3.get(0), p3.get(0));
		assertEquals(f4.get(0), p1.get(4));
		
		checkConsistency(gtdModel);

	}
	
	private void checkConsistency(GTDModel m) {
		
		try {
			GTDModel.checkConsistency(m,null,true,false);
		} catch (ConsistencyException e) {
			e.printStackTrace();
			fail(e.toString());
		}
		
	}
	
	public void testNoEmpty() throws Exception {
		
		List<Folder> f= new ArrayList<Folder>(3);
		f.add(f1);
		f.add(f2);
		f.add(f3);
		f.add(f5);

		List<Project> p= new ArrayList<Project>(2);
		p.add(p1);
		p.add(p2);
		p.add(p4);
		
		ActionsCollection col = new ActionsCollection(f,p,false,false,false,new DummyFilter(true));

		Iterator<Object> i= col.iterator(ExportAddOn.ExportOrder.Actions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

//		System.out.println(((Action)i.next()).getDescription());
//		System.out.println(((Action)i.next()).getDescription());
//		System.out.println(((Action)i.next()).getDescription());
//		System.out.println(((Action)i.next()).getDescription());
//		System.out.println(((Action)i.next()).getDescription());
		
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(!i.hasNext());

		
		i= col.iterator(ExportAddOn.ExportOrder.FoldersActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(!i.hasNext());

		i= col.iterator(ExportAddOn.ExportOrder.FoldersProjectsActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(!i.hasNext());

		
		i= col.iterator(ExportAddOn.ExportOrder.ProjectsActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(!i.hasNext());

		
		i= col.iterator(ExportAddOn.ExportOrder.ProjectsFoldersActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(!i.hasNext());
	}

	public void testNoEmptyWholeFolders() throws Exception {
		
		List<Folder> f= new ArrayList<Folder>(3);
		f.add(f1);
		f.add(f2);
		f.add(f3);
		f.add(f4);
		f.add(f5);

		List<Project> p= new ArrayList<Project>(2);
		p.add(p1);
		p.add(p2);
		p.add(p3);
		p.add(p4);
		
		ActionsCollection col = new ActionsCollection(f,p,false,false,true,new DummyFilter(true));

		Iterator<Object> i= col.iterator(ExportAddOn.ExportOrder.Actions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f4.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f4.get(1), i.next());
		assertTrue(!i.hasNext());

		
		i= col.iterator(ExportAddOn.ExportOrder.FoldersActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f4, i.next());
		assertTrue(i.hasNext());
		assertEquals(f4.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f4.get(1), i.next());
		assertTrue(!i.hasNext());

		i= col.iterator(ExportAddOn.ExportOrder.FoldersProjectsActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3, i.next());
		assertTrue(i.hasNext());
		assertEquals(p3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f4, i.next());
		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f4.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f4.get(1), i.next());
		assertTrue(!i.hasNext());

		
		i= col.iterator(ExportAddOn.ExportOrder.ProjectsActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f4.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(p3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f4.get(1), i.next());
		assertTrue(!i.hasNext());

		i= col.iterator(ExportAddOn.ExportOrder.ProjectsFoldersActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f4, i.next());
		assertTrue(i.hasNext());
		assertEquals(f4.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(p3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f4, i.next());
		assertTrue(i.hasNext());
		assertEquals(f4.get(1), i.next());
		assertTrue(!i.hasNext());

	}

	public void testWholeFolders() throws Exception {
		
		List<Folder> f= new ArrayList<Folder>(3);
		f.add(f1);
		f.add(f2);
		f.add(f3);
		f.add(f5);

		List<Project> p= new ArrayList<Project>(2);
		p.add(p1);
		p.add(p2);
		p.add(p3);
		p.add(p4);
		
		ActionsCollection col = new ActionsCollection(f,p,true,true,true,new DummyFilter(true));

		Iterator<Object> i= col.iterator(ExportAddOn.ExportOrder.Actions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(1), i.next());
		assertTrue(!i.hasNext());

		
		i= col.iterator(ExportAddOn.ExportOrder.FoldersActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f5, i.next());
		assertTrue(!i.hasNext());

		i= col.iterator(ExportAddOn.ExportOrder.FoldersProjectsActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3, i.next());
		assertTrue(i.hasNext());
		assertEquals(p3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f5, i.next());
		assertTrue(!i.hasNext());

		
		i= col.iterator(ExportAddOn.ExportOrder.ProjectsActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(p3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(p4, i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(1), i.next());
		assertTrue(!i.hasNext());

		i= col.iterator(ExportAddOn.ExportOrder.ProjectsFoldersActions);
		
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());

		assertTrue(i.hasNext());
		assertEquals(p1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(p2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(2), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(3), i.next());
		assertTrue(i.hasNext());
		assertEquals(p3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(0), i.next());
		assertTrue(i.hasNext());
		assertEquals(p4, i.next());
		assertTrue(i.hasNext());
		assertEquals(ActionsCollection.ACTIONS_WITHOUT_PROJECT, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1, i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f1.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2, i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(4), i.next());
		assertTrue(i.hasNext());
		assertEquals(f2.get(5), i.next());
		assertTrue(i.hasNext());
		assertEquals(f3, i.next());
		assertTrue(i.hasNext());
		assertEquals(f3.get(1), i.next());
		assertTrue(i.hasNext());
		assertEquals(f5, i.next());
		assertTrue(!i.hasNext());
	}

}
