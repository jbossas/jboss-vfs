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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Nested Jar Handler.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class NestedJarHandler extends AbstractStructuredJarHandler<Object>
{
   /** serialVersionUID */
   private static final long serialVersionUID = 1L;

   /** The temporary file */
   private transient File temp;

   /**
    * Create a temporary jar
    * 
    * @param temp the temporary file
    * @param parentJar the jar
    * @param entry the jar entry
    * @return the jar file
    * @throws IOException for any error
    */
   private static JarFile createTempJar(File temp, JarFile parentJar, ZipEntry entry) throws IOException
   {
      InputStream inputStream = parentJar.getInputStream(entry);
      try
      {
         FileOutputStream outputStream = new FileOutputStream(temp);
         try
         {
            byte[] buffer = new byte[8096];
            int read = inputStream.read(buffer);
            while (read != -1)
            {
               outputStream.write(buffer, 0, read);
               read = inputStream.read(buffer);
            }
         }
         finally
         {
            outputStream.close();
         }
      }
      finally
      {
         try
         {
            inputStream.close();
         }
         catch (IOException ignored)
         {
         }
      }
      
      return new JarFile(temp);
   }

   public static NestedJarHandler create(VFSContext context, VirtualFileHandler parent,
         JarFile parentJar, ZipEntry entry, URL url, String entryName) throws IOException
   {
      File temp;
      try
      {
         temp = File.createTempFile("nestedjar", null);
         temp.deleteOnExit();
      }
      catch (IOException original)
      {
         // Fix the context of the error message
         IOException e = new IOException("Error opening jar file: " + url + " reason=" + original.getMessage());
         e.setStackTrace(original.getStackTrace());
         throw e;
      }
      return new NestedJarHandler(context, parent, parentJar, entry, url, temp, entryName);
   }

   /**
    * Create a new NestedJarHandler.
    * 
    * @param context the context
    * @param parent the parent
    * @param parentJar the parent jar file
    * @param entry the jar entry
    * @param original the original url
    * @param temp the temporary file
    * @param entryName the entry name
    * @throws IOException for an error accessing the file system
    * @throws IllegalArgumentException for a null context, url or vfsPath
    */
   protected NestedJarHandler(VFSContext context, VirtualFileHandler parent, JarFile parentJar, ZipEntry entry, URL original, File temp, String entryName)
      throws IOException
   {
      super(context, parent, temp.toURI().toURL(), createTempJar(temp, parentJar, entry), entry, entryName);

      try
      {
         setPathName(getChildPathName(entryName, false));
         setVfsUrl(getChildVfsUrl(entryName, false));
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }

      this.temp = temp;

      try
      {
         initJarFile();
      }
      catch (IOException old)
      {
         // Fix the context of the error message
         IOException e = new IOException("Error opening jar file: " + original + " reason=" + old.getMessage());
         e.setStackTrace(old.getStackTrace());
         throw e;
      }
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

   public boolean isNested() throws IOException
   {
      return false;
   }

   /**
    * Overriden to return the raw tmp jar file stream 
    */
   @Override
   public InputStream openStream() throws IOException
   {
      return new FileInputStream(temp);
   }

   public boolean removeChild(String name) throws IOException
   {
      return false;
   }

   @Override
   public void cleanup()
   {
      try
      {
         delete(2000);
      }
      catch (Exception ignored)
      {
      }
   }

   public boolean delete(int gracePeriod) throws IOException
   {
      if (temp != null)
      {
         boolean deleted = temp.delete();
         if (deleted)
            return super.delete(gracePeriod);
         return deleted;
      }
      else
      {
         return false;
      }
   }

   /**
    * Restore the temp file
    *
    * @param in the input stream
    * @throws IOException for any error reading the jar file
    * @throws ClassNotFoundException for any jar class finding errors
    */
   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      // TODO - temp?
   }
}
