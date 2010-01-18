/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.vfs.support;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * ClassPathIterator logic used by UCL package mapping
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class ClassPathIterator
{
   ZipInputStream zis;
   FileIterator fileIter;
   File file;
   VirtualFileIterator vfIter;
   VirtualFile vf;
   int rootLength;

   public ClassPathIterator(URL url) throws IOException
   {
      String protocol = url != null ? url.getProtocol() : null;
      if( protocol == null )
      {
      }
      else if( protocol.equals("file") || protocol.startsWith("vfs"))
      {
         URLConnection conn = url.openConnection();
         vf = (VirtualFile) conn.getContent();
         rootLength = vf.getPathName().length() + 1;
         vfIter = new VirtualFileIterator(vf);
      }
      else
      {
         // Assume this points to a jar
         InputStream is = url.openStream();
         zis = new ZipInputStream(is);
      }
   }

   public ClassPathEntry getNextEntry() throws IOException
   {
      ClassPathEntry entry = null;
      if( zis != null )
      {
         ZipEntry zentry = zis.getNextEntry();
         if( zentry != null )
            entry = new ClassPathEntry(zentry);
      }
      else if( fileIter != null )
      {
         File fentry = fileIter.getNextEntry();
         if( fentry != null )
            entry = new ClassPathEntry(fentry, rootLength);
         file = fentry;
      }
      else if( vfIter != null )
      {
         VirtualFile fentry = vfIter.getNextEntry();
         if( fentry != null )
            entry = new ClassPathEntry(fentry, rootLength);
         vf = fentry;
      }

      return entry;
   }

   InputStream getInputStream() throws IOException
   {
      InputStream is = zis;
      if( zis == null )
      {
         is = new FileInputStream(file);
      }
      return is;
   }

   public void close() throws IOException
   {
      if( zis != null )
         zis.close();
   }

   static class FileIterator
   {
      LinkedList subDirectories = new LinkedList();
      FileFilter filter;
      File[] currentListing;
      int index = 0;

      FileIterator(File start)
      {
         String name = start.getName();
         // Don't recurse into wars
         boolean isWar = name.endsWith(".war");
         if( isWar )
            currentListing = new File[0];
         else
            currentListing = start.listFiles();
      }
      FileIterator(File start, FileFilter filter)
      {
         String name = start.getName();
         // Don't recurse into wars
         boolean isWar = name.endsWith(".war");
         if( isWar )
            currentListing = new File[0];
         else
            currentListing = start.listFiles(filter);
         this.filter = filter;
      }

      File getNextEntry()
      {
         File next = null;
         if( index >= currentListing.length && subDirectories.size() > 0 )
         {
            do
            {
               File nextDir = (File) subDirectories.removeFirst();
               currentListing = nextDir.listFiles(filter);
            } while( currentListing.length == 0 && subDirectories.size() > 0 );
            index = 0;
         }
         if( index < currentListing.length )
         {
            next = currentListing[index ++];
            if( next.isDirectory() )
               subDirectories.addLast(next);
         }
         return next;
      }
   }

   static class VirtualFileIterator
   {
      LinkedList<VirtualFile> subDirectories = new LinkedList<VirtualFile>();
      VirtualFileFilter filter;
      List<VirtualFile> currentListing;
      int index = 0;

      VirtualFileIterator(VirtualFile start) throws IOException
      {
         this(start, null);
      }
      VirtualFileIterator(VirtualFile start, VirtualFileFilter filter)  throws IOException
      {
         String name = start.getName();
         // Don't recurse into wars
         boolean isWar = name.endsWith(".war");
         if( isWar )
            currentListing = new ArrayList<VirtualFile>();
         else
            currentListing = start.getChildren();
         this.filter = filter;
      }

      VirtualFile getNextEntry()
         throws IOException
      {
         VirtualFile next = null;
         if( index >= currentListing.size() && subDirectories.size() > 0 )
         {
            do
            {
               VirtualFile nextDir = subDirectories.removeFirst();
               currentListing = nextDir.getChildren(filter);
            } while( currentListing.size() == 0 && subDirectories.size() > 0 );
            index = 0;
         }
         if( index < currentListing.size() )
         {
            next = currentListing.get(index);
            index ++;
             if( next.isFile() == false )
               subDirectories.addLast(next);
         }
         return next;
      }
   }

   public static class ClassPathEntry
   {
      public String name;
      public ZipEntry zipEntry;
      public File fileEntry;
      public VirtualFile vfEntry;

      ClassPathEntry(ZipEntry zipEntry)
      {
         this.zipEntry = zipEntry;
         this.name = zipEntry.getName();
      }
      ClassPathEntry(File fileEntry, int rootLength)
      {
         this.fileEntry = fileEntry;
         this.name = fileEntry.getPath().substring(rootLength);
      }
      ClassPathEntry(VirtualFile vfEntry, int rootLength)
      {
         this.vfEntry = vfEntry;
         this.name = vfEntry.getPathName().substring(rootLength);
      }

      String getName()
      {
         return name;
      }
      /** Convert the entry path to a package name
       */
      String toPackageName()
      {
         String pkgName = name;
         char separatorChar = zipEntry != null ? '/' : File.separatorChar;
         int index = name.lastIndexOf(separatorChar);
         if( index > 0 )
         {
            pkgName = name.substring(0, index);
            pkgName = pkgName.replace(separatorChar, '.');
         }
         else
         {
            // This must be an entry in the default package (e.g., X.class)
            pkgName = "";
         }
         return pkgName;
      }

      boolean isDirectory()
      {
         boolean isDirectory = false;
         if( zipEntry != null )
            isDirectory = zipEntry.isDirectory();
         else
            isDirectory = fileEntry.isDirectory();
         return isDirectory;
      }
   }

}

