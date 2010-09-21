/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.test.virtual.test;

import java.util.HashSet;

import org.jboss.test.virtual.support.MockVFSContext;
import org.jboss.test.virtual.support.MockVirtualFileVisitor;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.VisitorAttributes;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * VisitorUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class VisitorUnitTestCase extends AbstractMockVFSTest
{
   public VisitorUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(VisitorUnitTestCase.class);
   }
   
   public void testDefaultVisitSimple() throws Exception
   {
      MockVFSContext context = createSimpleVFSContextWithChildrenAndNonLeafs();
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor();
      HashSet<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1");
      expected.add("folder1/child1");
      expected.add("folder2");
      expected.add("folder2/child1");
      expected.add("folder2/child2");
      expected.add("folder3");
      expected.add("folder3/child1");
      expected.add("folder3/child2");
      expected.add("folder3/child3");
      testVisit(context, visitor, expected);
   }
   
   public void testDefaultVisitStructured() throws Exception
   {
      MockVFSContext context = createStructuredVFSContextWithChildrenAndNonLeafs();
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor();
      HashSet<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1");
      expected.add("folder2");
      expected.add("folder3");
      testVisit(context, visitor, expected);
   }
   
   public void testLeavesOnlyVisitSimple() throws Exception
   {
      MockVFSContext context = createSimpleVFSContextWithChildrenAndNonLeafs();
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor(VisitorAttributes.LEAVES_ONLY);
      HashSet<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1/child1");
      expected.add("folder2/child1");
      expected.add("folder2/child2");
      expected.add("folder3/child1");
      expected.add("folder3/child2");
      expected.add("folder3/child3");
      testVisit(context, visitor, expected);
   }
   
   public void testLeavesOnlyVisitStructured() throws Exception
   {
      MockVFSContext context = createStructuredVFSContextWithChildrenAndNonLeafs();
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor(VisitorAttributes.LEAVES_ONLY);
      HashSet<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      testVisit(context, visitor, expected);
   }
   
   public void testRecurseVisitSimple() throws Exception
   {
      MockVFSContext context = createSimpleVFSContextWithChildrenAndNonLeafs();
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor(VisitorAttributes.RECURSE);
      HashSet<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1");
      expected.add("folder1/child1");
      expected.add("folder2");
      expected.add("folder2/child1");
      expected.add("folder2/child2");
      expected.add("folder3");
      expected.add("folder3/child1");
      expected.add("folder3/child2");
      expected.add("folder3/child3");
      testVisit(context, visitor, expected);
   }
   
   public void testRecurseVisitStructured() throws Exception
   {
      MockVFSContext context = createStructuredVFSContextWithChildrenAndNonLeafs();
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor(VisitorAttributes.RECURSE);
      HashSet<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1");
      expected.add("folder1/child1");
      expected.add("folder2");
      expected.add("folder2/child1");
      expected.add("folder2/child2");
      expected.add("folder3");
      expected.add("folder3/child1");
      expected.add("folder3/child2");
      expected.add("folder3/child3");
      testVisit(context, visitor, expected);
   }
   
   public void testRecurseLeavesOnlyVisitSimple() throws Exception
   {
      MockVFSContext context = createSimpleVFSContextWithChildrenAndNonLeafs();
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor(VisitorAttributes.RECURSE_LEAVES_ONLY);
      HashSet<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1/child1");
      expected.add("folder2/child1");
      expected.add("folder2/child2");
      expected.add("folder3/child1");
      expected.add("folder3/child2");
      expected.add("folder3/child3");
      testVisit(context, visitor, expected);
   }
   
   public void testRecurseLeavesOnlyVisitStructured() throws Exception
   {
      MockVFSContext context = createStructuredVFSContextWithChildrenAndNonLeafs();
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor(VisitorAttributes.RECURSE_LEAVES_ONLY);
      HashSet<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1/child1");
      expected.add("folder2/child1");
      expected.add("folder2/child2");
      expected.add("folder3/child1");
      expected.add("folder3/child2");
      expected.add("folder3/child3");
      testVisit(context, visitor, expected);
   }
   
   public void testIncludeRootVisitSimple() throws Exception
   {
      MockVFSContext context = createSimpleVFSContextWithChildrenAndNonLeafs();
      VisitorAttributes attributes = new VisitorAttributes();
      attributes.setIncludeRoot(true);
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor(attributes);
      HashSet<String> expected = new HashSet<String>();
      expected.add("");
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1");
      expected.add("folder1/child1");
      expected.add("folder2");
      expected.add("folder2/child1");
      expected.add("folder2/child2");
      expected.add("folder3");
      expected.add("folder3/child1");
      expected.add("folder3/child2");
      expected.add("folder3/child3");
      testVisit(context, visitor, expected);
   }
   
   public void testIncludeRootVisitStructured() throws Exception
   {
      MockVFSContext context = createStructuredVFSContextWithChildrenAndNonLeafs();
      VisitorAttributes attributes = new VisitorAttributes();
      attributes.setIncludeRoot(true);
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor(attributes);
      HashSet<String> expected = new HashSet<String>();
      expected.add("");
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1");
      expected.add("folder2");
      expected.add("folder3");
      testVisit(context, visitor, expected);
   }
   
   public void testExcludeHiddenVisitSimple() throws Exception
   {
      MockVFSContext context = createSimpleVFSContextWithChildrenAndNonLeafsWithHidden();
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor();
      HashSet<String> expected = new HashSet<String>();
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1");
      expected.add("folder1/child1");
      expected.add("folder2");
      expected.add("folder2/child1");
      expected.add("folder2/child2");
      expected.add("folder3");
      expected.add("folder3/child1");
      expected.add("folder3/child2");
      expected.add("folder3/child3");
      testVisit(context, visitor, expected);
   }
   
   public void testExcludeHiddenVisitStructured() throws Exception
   {
      MockVFSContext context = createStructuredVFSContextWithChildrenAndNonLeafsWithHidden();
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor();
      HashSet<String> expected = new HashSet<String>();
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1");
      expected.add("folder2");
      expected.add("folder3");
      testVisit(context, visitor, expected);
   }
   
   public void testIncludeHiddenVisitSimple() throws Exception
   {
      MockVFSContext context = createSimpleVFSContextWithChildrenAndNonLeafsWithHidden();
      VisitorAttributes attributes = new VisitorAttributes();
      attributes.setIncludeHidden(true);
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor(attributes);
      HashSet<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1");
      expected.add("folder1/child1");
      expected.add("folder2");
      expected.add("folder2/child1");
      expected.add("folder2/child2");
      expected.add("folder3");
      expected.add("folder3/child1");
      expected.add("folder3/child2");
      expected.add("folder3/child3");
      testVisit(context, visitor, expected);
   }
   
   public void testIncludeHiddenVisitStructured() throws Exception
   {
      MockVFSContext context = createStructuredVFSContextWithChildrenAndNonLeafsWithHidden();
      VisitorAttributes attributes = new VisitorAttributes();
      attributes.setIncludeHidden(true);
      MockVirtualFileVisitor visitor = new MockVirtualFileVisitor(attributes);
      HashSet<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");
      expected.add("folder1");
      expected.add("folder2");
      expected.add("folder3");
      testVisit(context, visitor, expected);
   }
   
   protected void testVisit(MockVFSContext context, MockVirtualFileVisitor visitor, HashSet<String> expected) throws Exception
   {
      VFS vfs = context.getVFS();
      vfs.visit(visitor);
      
      HashSet<String> actual = new HashSet<String>();
      for (VirtualFile file : visitor.getVisited())
         actual.add(file.getPathName());
      assertEquals(expected, actual);
   }
}
