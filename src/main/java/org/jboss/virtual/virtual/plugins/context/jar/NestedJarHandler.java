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
import java.net.URL;
import java.net.URISyntaxException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Nested Jar Handler.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public class NestedJarHandler extends AbstractJarHandler
{
   /** serialVersionUID */
   private static final long serialVersionUID = 1L;

   /** The jar entry */
   private transient JarEntry entry;
   
   /** The temporary file */
   private transient File temp;

   /** TODO WHAT DOES THIS DO? It is unused */
   private transient URL original;

   /**
    * Create a temporary jar
    * 
    * @param temp the temporary file
    * @param parentJar the jar
    * @param entry the jar entry
    * @return the jar file
    * @throws IOException for any error
    */
   private static JarFile createTempJar(File temp, JarFile parentJar, JarEntry entry) throws IOException
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
         JarFile parentJar, JarEntry entry, URL url, String entryName) throws IOException
   {
      File temp = null;

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
   protected NestedJarHandler(VFSContext context, VirtualFileHandler parent,
         JarFile parentJar, JarEntry entry, URL original, File temp, String entryName)
      throws IOException
   {
      super(context, parent, temp.toURL(), entryName);

      try
      {
         String vfsParentUrl = parent.toVfsUrl().toString();
         if (vfsParentUrl.endsWith("/")) vfsUrl = new URL(vfsParentUrl + entryName);
         else vfsUrl = new URL(vfsParentUrl + "/" + entryName);
      }
      catch (URISyntaxException e)
      {
         throw new RuntimeException(e);
      }

      this.temp = temp;
      this.original = original;
      
      try
      {
         initJarFile(createTempJar(temp, parentJar, entry));
      }
      catch (IOException old)
      {
         // Fix the context of the error message
         IOException e = new IOException("Error opening jar file: " + original + " reason=" + old.getMessage());
         e.setStackTrace(old.getStackTrace());
         throw e;
      }
      
      this.entry = entry;
   }
   
   /**
    * Get the entry
    * 
    * @return the file
    */
   protected JarEntry getEntry()
   {
      checkClosed();
      return entry;
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

   /**
    * Overriden to return the raw tmp jar file stream 
    */
   @Override
   public InputStream openStream() throws IOException
   {
      FileInputStream fis = new FileInputStream(temp);
      return fis;
   }

   /**
    * Restore the jar file from the parent jar and entry name
    * 
    * @param in
    * @throws IOException
    * @throws ClassNotFoundException
    */
   private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException
   {
      JarFile parentJar = super.getJar();
      // Initial the parent jar entries
      super.initJarFile(parentJar);
   }

}
