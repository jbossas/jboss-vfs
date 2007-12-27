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
package org.jboss.virtual.plugins.context.jar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Nested Jar Handler.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class NoCopyNestedJarHandler extends AbstractJarHandler
{
   /** serialVersionUID */
   private static final long serialVersionUID = 1L;

   /** The nested jar */
   private NestedJarFromStream njar;

   /**
    * Create a new NestedJarHandler.
    * 
    * @param context the context
    * @param parent the parent
    * @param parentJar the parent jar file
    * @param entry the jar entry
    * @param url the url
    * @param entryName the entry name
    * @throws IOException for an error accessing the file system
    * @throws IllegalArgumentException for a null context, url or vfsPath
    */
   public NoCopyNestedJarHandler(VFSContext context, VirtualFileHandler parent, JarFile parentJar, ZipEntry entry, URL url, String entryName) throws IOException
   {
      super(context, parent, url, parentJar, entry, entryName);
      
      try
      {
         setPathName(getChildPathName(entryName, false));
         setVfsUrl(getChildVfsUrl(entryName, false));
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }

      try
      {
         InputStream is = parentJar.getInputStream(entry);
         ZipInputStream zis;
         if(is instanceof ZipInputStream)
         {
            zis = (ZipInputStream) is;
         }
         else
         {
            zis = new ZipInputStream(is);
         }
         njar = new NestedJarFromStream(context, parent, zis, url, parentJar, entry, entryName);
      }
      catch (IOException original)
      {
         // Fix the context of the error message
         IOException e = new IOException("Error opening jar file: " + url + " reason=" + original.getMessage());
         e.setStackTrace(original.getStackTrace());
         throw e;
      }
   }
   
   protected void initCacheLastModified()
   {
   }

   @Override
   public long getLastModified() throws IOException
   {
      return getEntry().getTime();
   }

   @Override
   public long getSize() throws IOException
   {
      return getEntry().getSize();
   }

   @Override
   public InputStream openStream() throws IOException
   {
      return getJar().getInputStream(getEntry());
   }

   public VirtualFileHandler findChild(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      if ("".equals(path))
         return this;

      return njar.findChild(path);
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      return njar.getChildren(ignoreErrors);
   }
}
