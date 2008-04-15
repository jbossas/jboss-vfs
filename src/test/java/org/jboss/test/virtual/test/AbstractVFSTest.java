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
import java.net.URL;
import java.util.Map;

import junit.framework.AssertionFailedError;
import org.jboss.test.BaseTestCase;
import org.jboss.test.virtual.support.OptionsAwareURI;
import org.jboss.test.virtual.support.FileOAContextFactory;
import org.jboss.test.virtual.support.JarOAContextFactory;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.spi.VFSContextFactoryLocator;
import org.jboss.virtual.spi.VFSContextFactory;

/**
 * AbstractVFSTest.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractVFSTest extends BaseTestCase
{
   private static final VFSContextFactory fileFactory = new FileOAContextFactory();
   private static final VFSContextFactory jarFactory = new JarOAContextFactory();

   private boolean forceCopy;

   public AbstractVFSTest(String name)
   {
      super(name);
   }

   public AbstractVFSTest(String name, boolean forceCopy)
   {
      super(name);
      this.forceCopy = forceCopy;
   }

   protected void setUp() throws Exception
   {
      super.setUp();

      VFSContextFactoryLocator.registerFactory(fileFactory);
      VFSContextFactoryLocator.registerFactory(jarFactory);

      getLog().info("Force copy: " + forceCopy);
      if (forceCopy)
      {
         OptionsAwareURI.set();
      }
   }

   protected void tearDown() throws Exception
   {
      VFSContextFactoryLocator.unregisterFactory(jarFactory);
      VFSContextFactoryLocator.unregisterFactory(fileFactory);

      if (forceCopy)
      {
         OptionsAwareURI.clear();
      }

      super.tearDown();
   }

   // TODO move to AbstractTestCase
   public URL getResource(String name)
   {
      URL url = super.getResource(name);
      assertNotNull("Resource not found: " + name, url);
      return url;
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

   /**
    * Do we force copy handling of jars.
    *
    * @param vfs the vfs
    * @return true if we force copy handling
    * @throws IOException for any error
    */
   protected boolean isForceCopyEnabled(VFS vfs) throws IOException
   {
      return isForceCopyEnabled(vfs.getRoot());
   }

   /**
    * Do we force copy handling of jars.
    *
    * @param file the file
    * @return true if we force copy handling
    */
   protected boolean isForceCopyEnabled(VirtualFile file)
   {
      return isForceCopyEnabled(file.getHandler());
   }

   /**
    * Do we force copy handling of jars.
    *
    * @param handler the virtual file handler
    * @return true if we force copy handling
    */
   protected boolean isForceCopyEnabled(VirtualFileHandler handler)
   {
      boolean systemProperty = Boolean.parseBoolean(System.getProperty(VFSUtils.FORCE_COPY_KEY, "false"));
      if (systemProperty == false)
      {
         VFSContext context = handler.getVFSContext();
         Map<String, String> map = context.getOptions();
         return (map != null && map.get(VFSUtils.USE_COPY_QUERY) != null);
      }
      return true;
   }
}
