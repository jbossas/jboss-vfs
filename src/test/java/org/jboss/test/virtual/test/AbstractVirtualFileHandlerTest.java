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
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * AbstractVirtualFileHandlerTest.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractVirtualFileHandlerTest extends AbstractVFSTest
{
   public AbstractVirtualFileHandlerTest(String name)
   {
      super(name);
   }
   
   protected abstract VFSContext getVFSContext(String name) throws Exception;

   protected String getRootName(String name) throws Exception
   {
      return name;
   }
   
   protected abstract long getRealLastModified(String name, String path) throws Exception;
   
   protected abstract long getRealSize(String name, String path) throws Exception;
   
   public void testRootName() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      String rootName = getRootName("complex");
      assertEquals(rootName, root.getName());
   }

   public void testChildName() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      assertEquals("child", child.getName());
   }

   public void testSubFolderName() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "subfolder");
      assertEquals("subfolder", child.getName());
   }

   public void testSubChildName() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "subfolder/subchild");
      assertTrue(child.getName().endsWith("subchild"));
   }
   
   public void testRootPathName() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      assertEquals("", root.getPathName());
   }

   public void testChildPathName() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      assertEquals("child", child.getPathName());
   }

   public void testSubFolderPathName() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "subfolder");
      assertEquals("subfolder", child.getPathName());
   }

   public void testSubChildPathName() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "subfolder/subchild");
      assertEquals("subfolder/subchild", child.getPathName());
   }

   /**
    * Test that finding a child and listing its parent result in consistent
    * child handlers.
    * 
    * @throws Exception
    */
   public void testSubSubChildPathName() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "subfolder/subsubfolder/subsubchild");
      assertEquals("subfolder/subsubfolder/subsubchild", child.getPathName());
      VirtualFileHandler parent = context.findChild(root, "subfolder/subsubfolder");
      List<VirtualFileHandler> children = parent.getChildren(false);
      // Filter out an .svn stuff since this is run from the source tree
      Iterator<VirtualFileHandler> iter = children.iterator();
      while( iter.hasNext() )
      {
         child = iter.next();
         if( child.getName().endsWith(".svn") )
          iter.remove();
      }
      assertEquals("subfolder/subsubfolder has one child", 1, children.size());
      child = children.get(0);
      assertEquals("subfolder/subsubfolder/subsubchild", child.getPathName());
   }

   /* TODO URI testing
   public void testToURI() throws Exception
   {
   }
   */

   /* TODO URL testing
   public void testToURL() throws Exception
   {
   }
   */
   
   public void testRootLastModified() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      long realLastModified = getRealLastModified("simple", null);
      // strip any milliseconds
      realLastModified = realLastModified / 1000 * 1000;
      long fileLastModified = root.getLastModified();
      fileLastModified = fileLastModified / 1000 * 1000;
      assertEquals(realLastModified, fileLastModified);
   }
   
   public void testChildLastModified() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      long realLastModified = getRealLastModified("simple", "child");
      assertEquals(realLastModified, child.getLastModified());
   }

   public void testGetLastModifiedClosed() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      child.close();
      try
      {
         child.getLastModified();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }
   
   public void testRootSize() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      long realSize = getRealSize("simple", null);
      assertEquals(realSize, root.getSize());
   }
   
   public void testChildSize() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      long realSize = getRealSize("simple", "child");
      assertEquals(realSize, child.getSize());
   }

   public void testGetSizeClosed() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      child.close();
      try
      {
         child.getSize();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }
   
   public void testRootIsLeaf() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      assertFalse(root.isLeaf());
   }

   public void testChildIsLeaf() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      assertTrue(child.isLeaf());
   }

   public void testSubFolderIsLeaf() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "subfolder");
      assertFalse(child.isLeaf());
   }

   public void testSubChildIsLeaf() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "subfolder/subchild");
      assertTrue(child.isLeaf());
   }

   public void testIsLeafClosed() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      child.close();
      try
      {
         child.isLeaf();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   // TODO how to test a real hidden file across platforms?
   public void testRootIsHidden() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      assertFalse(root.isHidden());
   }
   
   // TODO how to test a real hidden file across platforms?
   public void testChildIsHidden() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      assertFalse(child.isHidden());
   }

   public void testIsHiddenClosed() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      child.close();
      try
      {
         child.isHidden();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testOpenStream() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      InputStream stream = child.openStream();
      try
      {
         byte[] contents = new byte[10];
         int read = stream.read(contents);
         int total = 0;
         while (read != -1)
         {
            total += read;
            read = stream.read(contents, total, 10-total);
         }
         assertEquals(5, total);
         assertTrue(Arrays.equals("empty\0\0\0\0\0".getBytes(), contents));
      }
      finally
      {
         stream.close();
      }
   }

   public void testOpenStreamClosed() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      child.close();
      try
      {
         child.openStream();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testRootParent() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      assertNull(root.getParent());
   }
   
   public void testChildParent() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      assertEquals(root, child.getParent());
   }

   public void testgetParentClosed() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      child.close();
      try
      {
         child.getParent();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testGetChildren() throws Exception
   {
      VFSContext context = getVFSContext("children");
      VirtualFileHandler root = context.getRoot();
      List<VirtualFileHandler> children = root.getChildren(false);
      
      Set<String> expected = new HashSet<String>();
      expected.add("child1");
      expected.add("child2");
      expected.add("child3");

      Set<String> actual = new HashSet<String>();
      for (VirtualFileHandler child : children)
      {
         if (child.getName().startsWith("META-INF") == false && child.getName().equals(".svn") == false)
            actual.add(child.getName());
      }
      
      assertEquals(expected, actual);
   }

   public void testGetChildrenClosed() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      child.close();
      try
      {
         child.getChildren(false);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testFindChildRoot() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = root.findChild("");
      assertEquals(root, found);
   }

   public void testFindChild() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = root.findChild("child");
      assertEquals("child", found.getPathName());
   }

   public void testFindChildSubFolder() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = root.findChild("subfolder");
      assertEquals("subfolder", found.getPathName());
   }

   public void testFindChildSubChild() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler found = root.findChild("subfolder/subchild");
      assertEquals("subfolder/subchild", found.getPathName());
   }

   public void testFindChildDoesNotExist() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      try
      {
         root.findChild("doesnotexist");
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowableTemp(IOException.class, t);
      }
   }

   public void testFindChildNullPath() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      try
      {
         root.findChild(null);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalArgumentException.class, t);
      }
   }

   public void testFindChildClosed() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      child.close();
      try
      {
         child.findChild("");
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   public void testGetVFSContext() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler handler = context.getRoot();
      assertEquals(context, handler.getVFSContext());
   }

   public void testGetVFSContextClosed() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      child.close();
      try
      {
         child.getVFSContext();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }
  
   public void testRootGetVirtualFile() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      checkVirtualFile(root);
   }

   public void testChildGetVirtualFile() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      checkVirtualFile(child);
   }

   public void testSubFolderGetVirtualFile() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "subfolder");
      checkVirtualFile(child);
   }

   public void testSubChildGetVirtualFile() throws Exception
   {
      VFSContext context = getVFSContext("complex");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "subfolder/subchild");
      checkVirtualFile(child);
   }

   public void testGetVirtualFileClosed() throws Exception
   {
      VFSContext context = getVFSContext("simple");
      VirtualFileHandler root = context.getRoot();
      VirtualFileHandler child = context.findChild(root, "child");
      child.close();
      try
      {
         child.getVirtualFile();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(IllegalStateException.class, t);
      }
   }

   protected void checkVirtualFile(VirtualFileHandler handler) throws Exception
   {
      VirtualFile file = handler.getVirtualFile();
      
      assertEquals(handler.getVFSContext().getVFS(), file.getVFS());
      assertEquals(handler.getName(), file.getName());
      assertEquals(handler.getPathName(), file.getPathName());
      assertEquals(handler.isHidden(), file.isHidden());
      assertEquals(handler.isLeaf(), file.isLeaf());
      assertEquals(handler.getLastModified(), file.getLastModified());
      assertEquals(handler.getSize(), file.getSize());

      // can't do this anymore as VirtualFile.toURL() returns a vfs based url
      //assertEquals(handler.toURI(), file.toURI());
      //assertEquals(handler.toURL(), file.toURL());
      
      VirtualFileHandler parent = handler.getParent();
      if (parent == null)
         assertNull(file.getParent());
      else
         assertEquals(parent.getVirtualFile(), file.getParent());
   }
}
