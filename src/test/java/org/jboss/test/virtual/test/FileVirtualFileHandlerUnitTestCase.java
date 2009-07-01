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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * FileVirtualFileHandlerUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class FileVirtualFileHandlerUnitTestCase extends AbstractVirtualFileHandlerTest
{
   public FileVirtualFileHandlerUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return new TestSuite(FileVirtualFileHandlerUnitTestCase.class);
   }

   protected URL getRootResource(String name) throws Exception
   {
      return getResource("/vfs/context/file/" + name + "/");
   }
   
   protected File getRealFile(String name, String path) throws Exception
   {
      URL url = getRootResource(name);
      if (path != null)
         url = new URL(url, path);
      return new File(url.getPath());
   }
   
   protected VFSContext getVFSContext(String name) throws Exception
   {
      URL url = getRootResource(name);
      return new FileSystemContext(url);
   }

   protected long getRealLastModified(String name, String path) throws Exception
   {
      File file = getRealFile(name, path);
      return file.lastModified();
   }

   protected long getRealSize(String name, String path) throws Exception
   {
      File file = getRealFile(name, path);
      return file.length();
   }

   protected void assertIsNested(VirtualFileHandler handler) throws Exception
   {
      assertNotNull(handler);
      assertFalse(handler.isNested());
   }

   protected void modifyChild(VirtualFileHandler child, String name, String path) throws Exception
   {
      FileOutputStream out = new FileOutputStream(getRealFile(name, path));
      try
      {
         out.write((UUID.randomUUID() + "\n").getBytes());
      }
      finally
      {
         try
         {
            out.close();
         }
         catch (IOException ignored)
         {
         }
      }
   }

   protected void checkHasBeenModified(VirtualFileHandler handler) throws Exception
   {
      assertTrue(handler.hasBeenModified());
   }

   protected void unmodifyChild(VirtualFileHandler child, String name, String path) throws Exception
   {
      // no need to unmodify
   }
}
