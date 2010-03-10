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
package org.jboss.test.vfs;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import junit.framework.AssertionFailedError;
import org.jboss.test.BaseTestCase;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.junit.internal.ArrayComparisonFailure;

/**
 * AbstractVFSTest.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractVFSTest extends BaseTestCase
{
   protected TempFileProvider provider;

   public AbstractVFSTest(String name)
   {
      super(name);
   }

   protected void setUp() throws Exception
   {
      super.setUp();

      provider = TempFileProvider.create("test", new ScheduledThreadPoolExecutor(2));
   }

   protected void tearDown() throws Exception
   {
      provider.close();
   }

   public URL getResource(String name)
   {
      URL url = super.getResource(name);
      assertNotNull("Resource not found: " + name, url);
      return url;
   }
   
   public VirtualFile getVirtualFile(String name)
   {
      VirtualFile virtualFile = VFS.getChild(getResource(name).getPath()); 
      assertTrue("VirtualFile does not exist: " + name, virtualFile.exists());
      return virtualFile;
   }

   public List<Closeable> recursiveMount(VirtualFile file) throws IOException
   {
      ArrayList<Closeable> mounts = new ArrayList<Closeable>();

      if (!file.isDirectory() && file.getName().matches("^.*\\.([EeWwJj][Aa][Rr]|[Zz][Ii][Pp])$"))
         mounts.add(VFS.mountZip(file, file, provider));

      if (file.isDirectory())
         for (VirtualFile child : file.getChildren())
            mounts.addAll(recursiveMount(child));

      return mounts;
   }

   protected <T> void checkThrowableTemp(Class<T> expected, Throwable throwable)
   {
      if (expected == null)
         fail("Must provide an expected class");
      if (throwable == null)
         fail("Must provide a throwable for comparison");
      if (throwable instanceof AssertionFailedError || throwable instanceof AssertionError)
         throw (Error) throwable;
      // TODO move to AbstractTestCase if (expected.equals(throwable.getClass()) == false)
      if (expected.isAssignableFrom(throwable.getClass()) == false)
      {
         getLog().error("Unexpected throwable", throwable);
         fail("Unexpected throwable: " + throwable);
      }
      else
      {
         getLog().debug("Got expected " + expected.getName() + "(" + throwable + ")");
      }
   }
   
   protected void assertContentEqual(VirtualFile expected, VirtualFile actual) throws ArrayComparisonFailure, IOException {
      assertArrayEquals("Expected content must mach actual conent", getContent(expected), getContent(actual));
   }

   protected byte[] getContent(VirtualFile virtualFile) throws IOException {
      InputStream is = virtualFile.openStream();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      VFSUtils.copyStreamAndClose(is, bos);
      return bos.toByteArray();
   }
}
