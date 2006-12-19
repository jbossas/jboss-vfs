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
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.jboss.virtual.plugins.context.jar.JarContext;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.spi.VFSContext;

/**
 * JARVirtualFileHandlerUnitTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class JARVirtualFileHandlerUnitTestCase extends AbstractVirtualFileHandlerTest
{
   public JARVirtualFileHandlerUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return new TestSuite(JARVirtualFileHandlerUnitTestCase.class);
   }
   
   protected URL getRootResource(String name) throws Exception
   {
      if (name.endsWith(".jar"))
         return getResource("/vfs/context/jar/" + name);
      else
         return getResource("/vfs/context/jar/" + name + ".jar");
   }
   
   protected File getRealJarFile(String name) throws Exception
   {
      URL url = getRootResource(name);
      return new File(url.getPath());
   }
   
   protected JarEntry getRealJarEntry(String name, String path) throws Exception
   {
      URL url = getRootResource(name);
      url = JarUtils.createJarURL(url);
      JarURLConnection c = (JarURLConnection) url.openConnection();
      JarFile jarFile = c.getJarFile();
      return jarFile.getJarEntry(path);
   }
   
   protected VFSContext getVFSContext(String name) throws Exception
   {
      URL url = getRootResource(name);
      url = JarUtils.createJarURL(url);
      return new JarContext(url);
   }

   protected String getRootName(String name) throws Exception
   {
      return name + ".jar";
   }

   protected long getRealLastModified(String name, String path) throws Exception
   {
      if (path != null)
      {
         JarEntry entry = getRealJarEntry(name, path);
         return entry.getTime();
      }
      else
      {
         File file = getRealJarFile(name);
         return file.lastModified();
      }
   }

   protected long getRealSize(String name, String path) throws Exception
   {
      if (path != null)
      {
         JarEntry entry = getRealJarEntry(name, path);
         return entry.getSize();
      }
      else
      {
         File file = getRealJarFile(name);
         return file.length();
      }
   }
}
