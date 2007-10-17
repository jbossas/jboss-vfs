/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors. 
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
import java.net.URI;
import java.net.URL;

import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.memory.MemoryContextFactory;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextFactory;
import org.jboss.virtual.spi.VFSContextFactoryLocator;
import org.jboss.virtual.spi.VirtualFileHandler;

import junit.framework.TestCase;

/**
 * 
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class MemoryTestCase extends TestCase
{
   public void testContextFactory()throws Exception
   {
      URI uri = new URI("vfsmemory://aopdomain");
      VFSContextFactory factory = VFSContextFactoryLocator.getFactory(uri);
      assertNotNull(factory);
   }
   
   public void testContext() throws Exception
   {
      URI uri = new URI("vfsmemory://aopdomain");
      VFSContextFactory factory = VFSContextFactoryLocator.getFactory(uri);
      VFSContext ctx = factory.getVFS(uri);
      assertNotNull(ctx);
      
      MemoryContextFactory mfactory = MemoryContextFactory.getInstance();
      assertNotNull(mfactory);
      assertSame(factory, mfactory);
      
      VFSContext mctx = mfactory.createRoot(uri.toURL());
      assertNotNull(mctx);
      assertSame(ctx, mctx);
   }
   
   public void testWriteAndReadData() throws Exception
   {
      MemoryContextFactory mfactory = MemoryContextFactory.getInstance();
      URL root = new URL("vfsmemory://aopdomain");
      try
      {
         long now = System.currentTimeMillis();
         VFSContext ctx = mfactory.createRoot(root);
         URL url = new URL("vfsmemory://aopdomain/org/acme/test/Test.class");
         mfactory.putFile(url,  new byte[] {'a', 'b', 'c'});
         
         String read = readURL(url);
         assertEquals("abc", read);

         VirtualFile classFile = VFS.getVirtualFile(new URL("vfsmemory://aopdomain"), "org/acme/test/Test.class");
         InputStream bis = classFile.openStream();
         read = readIS(bis);
         assertEquals("abc", read);
         assertEquals(3, classFile.getSize());
         assertTrue(classFile.exists());
         assertTrue(classFile.isLeaf());
         assertTrue(classFile.getLastModified() >= now);

         assertTrue(mfactory.delete(url));
         try
         {
            InputStream is = url.openStream();
            fail("Should not have found file");
         }
         catch(Exception expected)
         {
         }
         
         ctx = mfactory.find("aopdomain");
         assertNotNull(ctx);
         
         assertTrue(mfactory.deleteRoot(root));
         ctx = mfactory.find("aopdomain");
         assertNull(ctx);
      }
      finally
      {
         mfactory.deleteRoot(root);
      }
   }
   
   public void testMultipleFiles() throws Exception
   {
      MemoryContextFactory mfactory = MemoryContextFactory.getInstance();
      URL root = new URL("vfsmemory://aopdomain");
      try
      {
         VFSContext ctx = mfactory.createRoot(root);
         
         URL urlA = new URL("vfsmemory://aopdomain/org/acme/test/Test.class");
         mfactory.putFile(urlA,  new byte[] {'a', 'b', 'c'});
         
         URL urlB = new URL("vfsmemory://aopdomain/org/foo/test/Test.class");
         mfactory.putFile(urlB,  new byte[] {'d', 'e', 'f'});
         
         String readA = readURL(urlA);
         assertEquals("abc", readA);
         
         String readB = readURL(urlB);
         assertEquals("def", readB);
      }
      finally
      {
         mfactory.deleteRoot(root);
      }
   }

   public void testNavigate() throws Exception
   {
      MemoryContextFactory mfactory = MemoryContextFactory.getInstance();
      URL root = new URL("vfsmemory://aopdomain");
      try
      {
         VFSContext ctx = mfactory.createRoot(root);
         URL url = new URL("vfsmemory://aopdomain/org/acme/test/Test.class");
         mfactory.putFile(url,  new byte[] {'a', 'b', 'c'});
         
         VFS vfs = ctx.getVFS();
         VirtualFile file = vfs.getVirtualFile(root, "/org/acme/test/Test.class");
         assertNotNull(file);
         
         VirtualFile file2 = vfs.getVirtualFile(root, "/org");
         assertNotNull(file2);
         VirtualFile file3 = file2.findChild("/acme/test/Test.class");
         assertNotNull(file3);
         
         assertSame(file.getHandler(), file3.getHandler());
      }
      finally
      {
         mfactory.deleteRoot(root);
      }
   }

   protected void setUp()
   {
      VFS.init();
      System.out.println("java.protocol.handler.pkgs: " + System.getProperty("java.protocol.handler.pkgs"));
   }
   private String readURL(URL url) throws IOException
   {
      InputStream is = url.openStream();
      String s = readIS(is);
      return s;
   }
   private String readIS(InputStream is)
      throws IOException
   {
      try
      {
         StringBuffer sb = new StringBuffer();
         while (is.available() != 0)
         {
            sb.append((char)is.read());
         }
         return sb.toString();
      }
      finally
      {
         if (is != null)
         {
            try
            {
               is.close();
            }
            catch(Exception ignore)
            {
            }
         }
      }      
   }
   
}
