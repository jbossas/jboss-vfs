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

import java.io.IOException;
import java.net.URI;

import org.jboss.test.virtual.support.MockSimpleVirtualFileHandler;
import org.jboss.test.virtual.support.MockStructuredVirtualFileHandler;
import org.jboss.test.virtual.support.MockVFSContext;
import org.jboss.test.virtual.support.MockVFSContextFactory;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextFactoryLocator;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * AbstractMockVFSTest.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractMockVFSTest extends AbstractVFSTest
{
   /** The vfs context factory */
   protected static MockVFSContextFactory mockVFSContextFactory = new MockVFSContextFactory();

   /**
    * Create a new AbstractMockVFSTest.
    * 
    * @param name the name
    */
   protected AbstractMockVFSTest(String name)
   {
      super(name);
   }

   protected void setUp() throws Exception
   {
      super.setUp();
      VFSContextFactoryLocator.registerFactory(mockVFSContextFactory);
   }

   protected void tearDown() throws Exception
   {
      mockVFSContextFactory.reset();
      VFSContextFactoryLocator.unregisterFactory(mockVFSContextFactory);
      super.tearDown();
   }
   
   protected MockVFSContext createSimpleVFSContext()
   {
      MockVFSContext context = new MockVFSContext("simple");
      MockSimpleVirtualFileHandler root = new MockSimpleVirtualFileHandler(context, null, "");
      context.setRoot(root);
      return context;
   }
   
   protected MockVFSContext registerSimpleVFSContext()
   {
      MockVFSContext context = createSimpleVFSContext();
      mockVFSContextFactory.addVFSContext(context);
      return context;
   }
   
   protected MockVFSContext createSimple2VFSContext()
   {
      MockVFSContext context = new MockVFSContext("simple2");
      MockSimpleVirtualFileHandler root = new MockSimpleVirtualFileHandler(context, null, "");
      context.setRoot(root);
      return context;
   }
   
   protected MockVFSContext registerSimple2VFSContext()
   {
      MockVFSContext context = createSimple2VFSContext();
      mockVFSContextFactory.addVFSContext(context);
      return context;
   }
   
   protected MockVFSContext createSimpleVFSContextWithChildren()
   {
      MockVFSContext context = new MockVFSContext("simpleWithChildren");
      MockSimpleVirtualFileHandler root = new MockSimpleVirtualFileHandler(context, null, "");
      context.setRoot(root);
      new MockSimpleVirtualFileHandler(context, root, "child1");
      new MockSimpleVirtualFileHandler(context, root, "child2");
      new MockSimpleVirtualFileHandler(context, root, "child3");
      return context;
   }
   
   protected MockVFSContext registerSimpleVFSContextWithChildren()
   {
      MockVFSContext context = createSimpleVFSContextWithChildren();
      mockVFSContextFactory.addVFSContext(context);
      return context;
   }
   
   protected MockVFSContext createSimpleVFSContextWithChildrenAndNonLeafs()
   {
      MockVFSContext context = new MockVFSContext("simpleWithChildren");
      MockSimpleVirtualFileHandler root = new MockSimpleVirtualFileHandler(context, null, "");
      context.setRoot(root);
      new MockSimpleVirtualFileHandler(context, root, "child1");
      new MockSimpleVirtualFileHandler(context, root, "child2");
      new MockSimpleVirtualFileHandler(context, root, "child3");
      MockSimpleVirtualFileHandler folder1 = new MockSimpleVirtualFileHandler(context, root, "folder1");
      folder1.setLeaf(false);
      new MockSimpleVirtualFileHandler(context, root, "folder1/child1");
      MockSimpleVirtualFileHandler folder2 = new MockSimpleVirtualFileHandler(context, root, "folder2");
      folder2.setLeaf(false);
      new MockSimpleVirtualFileHandler(context, root, "folder2/child1");
      new MockSimpleVirtualFileHandler(context, root, "folder2/child2");
      MockSimpleVirtualFileHandler folder3 = new MockSimpleVirtualFileHandler(context, root, "folder3");
      folder3.setLeaf(false);
      new MockSimpleVirtualFileHandler(context, root, "folder3/child1");
      new MockSimpleVirtualFileHandler(context, root, "folder3/child2");
      new MockSimpleVirtualFileHandler(context, root, "folder3/child3");
      
      return context;
   }
   
   protected MockVFSContext registerSimpleVFSContextWithChildrenAndNonLeafs()
   {
      MockVFSContext context = createSimpleVFSContextWithChildrenAndNonLeafs();
      mockVFSContextFactory.addVFSContext(context);
      return context;
   }
   
   protected MockVFSContext createSimpleVFSContextWithChildrenAndNonLeafsWithHidden()
   {
      MockVFSContext context = new MockVFSContext("simpleWithChildren");
      MockSimpleVirtualFileHandler root = new MockSimpleVirtualFileHandler(context, null, "");
      context.setRoot(root);
      MockSimpleVirtualFileHandler child1 = new MockSimpleVirtualFileHandler(context, root, "child1");
      child1.setHidden(true);
      new MockSimpleVirtualFileHandler(context, root, "child2");
      new MockSimpleVirtualFileHandler(context, root, "child3");
      MockSimpleVirtualFileHandler folder1 = new MockSimpleVirtualFileHandler(context, root, "folder1");
      folder1.setLeaf(false);
      new MockSimpleVirtualFileHandler(context, root, "folder1/child1");
      MockSimpleVirtualFileHandler folder2 = new MockSimpleVirtualFileHandler(context, root, "folder2");
      folder2.setLeaf(false);
      new MockSimpleVirtualFileHandler(context, root, "folder2/child1");
      new MockSimpleVirtualFileHandler(context, root, "folder2/child2");
      MockSimpleVirtualFileHandler folder3 = new MockSimpleVirtualFileHandler(context, root, "folder3");
      folder3.setLeaf(false);
      new MockSimpleVirtualFileHandler(context, root, "folder3/child1");
      new MockSimpleVirtualFileHandler(context, root, "folder3/child2");
      new MockSimpleVirtualFileHandler(context, root, "folder3/child3");
      
      return context;
   }
   
   protected MockVFSContext registerSimpleVFSContextWithChildrenAndNonLeafsWithHidden()
   {
      MockVFSContext context = createSimpleVFSContextWithChildrenAndNonLeafsWithHidden();
      mockVFSContextFactory.addVFSContext(context);
      return context;
   }
   
   protected MockVFSContext createStructuredVFSContextWithSubChildren()
   {
      MockVFSContext context = new MockVFSContext("simpleWithChildren");
      MockStructuredVirtualFileHandler root = new MockStructuredVirtualFileHandler(context, null, "");
      context.setRoot(root);
      MockStructuredVirtualFileHandler child1 = new MockStructuredVirtualFileHandler(context, root, "child1");
      new MockStructuredVirtualFileHandler(context, child1, "child1,1");
      MockStructuredVirtualFileHandler child2 = new MockStructuredVirtualFileHandler(context, root, "child2");
      new MockStructuredVirtualFileHandler(context, child2, "child2,1");
      new MockStructuredVirtualFileHandler(context, child2, "child2,2");
      MockStructuredVirtualFileHandler child3 = new MockStructuredVirtualFileHandler(context, root, "child3");
      new MockStructuredVirtualFileHandler(context, child3, "child3,1");
      new MockStructuredVirtualFileHandler(context, child3, "child3,2");
      new MockStructuredVirtualFileHandler(context, child3, "child3,3");
      return context;
   }
   
   protected MockVFSContext registerStructuredVFSContextWithSubChildren()
   {
      MockVFSContext context = createStructuredVFSContextWithSubChildren();
      mockVFSContextFactory.addVFSContext(context);
      return context;
   }
   
   protected MockVFSContext createStructuredVFSContextWithChildrenAndNonLeafs()
   {
      MockVFSContext context = new MockVFSContext("simpleWithChildren");
      MockStructuredVirtualFileHandler root = new MockStructuredVirtualFileHandler(context, null, "");
      context.setRoot(root);
      new MockStructuredVirtualFileHandler(context, root, "child1");
      new MockStructuredVirtualFileHandler(context, root, "child2");
      new MockStructuredVirtualFileHandler(context, root, "child3");
      MockStructuredVirtualFileHandler folder1 = new MockStructuredVirtualFileHandler(context, root, "folder1");
      folder1.setLeaf(false);
      new MockStructuredVirtualFileHandler(context, folder1, "child1");
      MockStructuredVirtualFileHandler folder2 = new MockStructuredVirtualFileHandler(context, root, "folder2");
      folder2.setLeaf(false);
      new MockStructuredVirtualFileHandler(context, folder2, "child1");
      new MockStructuredVirtualFileHandler(context, folder2, "child2");
      MockStructuredVirtualFileHandler folder3 = new MockStructuredVirtualFileHandler(context, root, "folder3");
      folder3.setLeaf(false);
      new MockStructuredVirtualFileHandler(context, folder3, "child1");
      new MockStructuredVirtualFileHandler(context, folder3, "child2");
      new MockStructuredVirtualFileHandler(context, folder3, "child3");
      
      return context;
   }
   
   protected MockVFSContext registerStructuredVFSContextWithChildrenAndNonLeafs()
   {
      MockVFSContext context = createStructuredVFSContextWithChildrenAndNonLeafs();
      mockVFSContextFactory.addVFSContext(context);
      return context;
   }
   
   protected MockVFSContext createStructuredVFSContextWithChildrenAndNonLeafsWithHidden()
   {
      MockVFSContext context = new MockVFSContext("simpleWithChildren");
      MockStructuredVirtualFileHandler root = new MockStructuredVirtualFileHandler(context, null, "");
      context.setRoot(root);
      MockStructuredVirtualFileHandler child1 = new MockStructuredVirtualFileHandler(context, root, "child1");
      child1.setHidden(true);
      new MockStructuredVirtualFileHandler(context, root, "child2");
      new MockStructuredVirtualFileHandler(context, root, "child3");
      MockStructuredVirtualFileHandler folder1 = new MockStructuredVirtualFileHandler(context, root, "folder1");
      folder1.setLeaf(false);
      new MockStructuredVirtualFileHandler(context, folder1, "child1");
      MockStructuredVirtualFileHandler folder2 = new MockStructuredVirtualFileHandler(context, root, "folder2");
      folder2.setLeaf(false);
      new MockStructuredVirtualFileHandler(context, folder2, "child1");
      new MockStructuredVirtualFileHandler(context, folder2, "child2");
      MockStructuredVirtualFileHandler folder3 = new MockStructuredVirtualFileHandler(context, root, "folder3");
      folder3.setLeaf(false);
      new MockStructuredVirtualFileHandler(context, folder3, "child1");
      new MockStructuredVirtualFileHandler(context, folder3, "child2");
      new MockStructuredVirtualFileHandler(context, folder3, "child3");
      
      return context;
   }
   
   protected MockVFSContext registerStructuredVFSContextWithChildrenAndNonLeafsWithHidden()
   {
      MockVFSContext context = createStructuredVFSContextWithChildrenAndNonLeafsWithHidden();
      mockVFSContextFactory.addVFSContext(context);
      return context;
   }
   
   protected VirtualFileHandler getChildHandler(VFSContext context, String path) throws IOException
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (path == null)
         throw new IllegalArgumentException("Null path");

      VirtualFileHandler root = context.getRoot();
      assertNotNull(root);
      VirtualFileHandler handler = context.findChild(root, path);
      assertNotNull(handler);
      return handler;
   }

   protected void assertGetName(URI uri, String name) throws Exception
   {
      assertGetName(uri, name, name);
   }

   protected void assertGetName(URI uri, String path, String name) throws Exception
   {
      VirtualFile file = VFS.getVirtualFile(uri, path);
      assertEquals(name, file.getName());
   }

   protected void assertGetPathName(URI uri, String path) throws Exception
   {
      VirtualFile file = VFS.getVirtualFile(uri, path);
      assertEquals(path, file.getPathName());
   }

   protected VirtualFile assertFindChild(VFS vfs, String path, VirtualFile expected) throws Exception
   {
      VirtualFile found = vfs.findChild(path);
      assertNotNull(found);
      assertEquals(expected, found);
      return found;
   }

   protected VirtualFile assertFindChild(VirtualFile file, String path, VirtualFile expected) throws Exception
   {
      VirtualFile found = file.findChild(path);
      assertNotNull(found);
      assertEquals(expected, found);
      return found;
   }
}
