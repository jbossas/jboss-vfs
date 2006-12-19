/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.jboss.virtual.plugins.context.jar;

import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A nested jar implementation used to represent a jar within a jar.
 *
 * @author Scott.Stark@jboss.org
 * @version $Revision: 44334 $
 */
public class NestedJarFromStream
        extends AbstractVirtualFileHandler
{
   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1L;

   private ZipInputStream zis;
   private HashMap<String, JarEntryContents> entries = new HashMap<String, JarEntryContents>();
   private URL jarURL;
   private URL entryURL;
   private String vfsPath;
   private String name;
   private long lastModified;
   private long size;
   private boolean inited;

   /**
    * Create a nested jar from the parent zip inputstream/zip entry.
    *
    * @param context - the context containing the jar
    * @param parent  - the jar handler for this nested jar
    * @param zis     - the jar zip input stream
    * @param jarURL  - the URL to use as the jar URL
    * @param entry   - the parent jar ZipEntry for the nested jar
    */
   public NestedJarFromStream(VFSContext context, VirtualFileHandler parent, ZipInputStream zis, URL jarURL, ZipEntry entry)
   {
      super(context, parent, entry.getName());
      this.jarURL = jarURL;
      this.name = entry.getName();
      this.lastModified = entry.getTime();
      this.size = entry.getSize();
      this.zis = zis;
      try
      {
         if (parent != null)
         {
            String vfsParentUrl = parent.toVfsUrl().toString();
            if (vfsParentUrl.endsWith("/")) vfsUrl = new URL(vfsParentUrl + this.name);
            else vfsUrl = new URL(vfsParentUrl + "/" + this.name);
         }
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }


   public VirtualFileHandler findChild(String path) throws IOException
   {
      if (inited == false)
         init();
      return entries.get(name);
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      if (inited == false)
         init();
      List<VirtualFileHandler> children = new ArrayList<VirtualFileHandler>();
      children.addAll(entries.values());
      return children;
   }

   public boolean isLeaf() throws IOException
   {
      return false;
   }

   public boolean isHidden()
   {
      return false;
   }

   public long getSize()
   {
      return size;
   }

   public long getLastModified() throws IOException
   {
      return lastModified;
   }


   public Iterator<JarEntryContents> getEntries()
           throws IOException
   {
      if (inited == false)
         init();
      return entries.values().iterator();
   }

   public JarEntryContents getEntry(String name)
           throws IOException
   {
      if (inited == false)
         init();
      return entries.get(name);
   }

   public ZipEntry getJarEntry(String name)
           throws IOException
   {
      if (inited == false)
         init();
      JarEntryContents jec = entries.get(name);
      return (jec != null ? jec.getEntry() : null);
   }

   public byte[] getContents(String name)
           throws IOException
   {
      if (inited == false)
         init();
      JarEntryContents jec = entries.get(name);
      return (jec != null ? jec.getContents() : null);
   }

   public String getName()
   {
      return name;
   }

   public String getPathName()
   {
      return vfsPath;
   }

   // Stream accessor
   public InputStream openStream() throws IOException
   {
      return zis;
   }

   public void close()
   {
      entries.clear();
      if (zis != null)
      {
         try
         {
            zis.close();
         }
         catch (IOException e)
         {
            log.error("close error", e);
         }
         zis = null;
      }
   }

   public URI toURI() throws URISyntaxException
   {
      try
      {
         if (entryURL == null)
            entryURL = new URL(jarURL, getName());
      }
      catch (MalformedURLException e)
      {
         throw new URISyntaxException("Failed to create relative jarURL", e.getMessage());
      }
      return entryURL.toURI();
   }

   public String toString()
   {
      StringBuffer tmp = new StringBuffer(super.toString());
      tmp.append('[');
      tmp.append("name=");
      tmp.append(getName());
      tmp.append(",size=");
      tmp.append(getSize());
      tmp.append(",lastModified=");
      tmp.append(lastModified);
      tmp.append(",URI=");
      try
      {
         tmp.append(toURI());
      }
      catch (URISyntaxException e)
      {
      }
      tmp.append(']');
      return tmp.toString();
   }

   protected void init()
           throws IOException
   {
      inited = true;
      ZipEntry entry = zis.getNextEntry();
      while (entry != null)
      {
         try
         {
            String url = toURI().toASCIIString() + "!/" + entry.getName();
            URL jecURL = new URL(url);
            JarEntryContents jec = new JarEntryContents(getVFSContext(), this, entry, jecURL, zis, getPathName());
            entries.put(entry.getName(), jec);
            entry = zis.getNextEntry();
         }
         catch (URISyntaxException e)
         {
            e.printStackTrace();
         }
      }
      zis.close();
      zis = null;
   }

   public static class JarEntryContents
           extends AbstractVirtualFileHandler
   {
      /**
       * serialVersionUID
       */
      private static final long serialVersionUID = 1L;
      private ZipEntry entry;
      private URL entryURL;
      private String vfsPath;
      private byte[] contents;
      private boolean isJar;
      private NestedJarFromStream njar;
      private InputStream openStream;

      JarEntryContents(VFSContext context, VirtualFileHandler parent, ZipEntry entry, URL entryURL, InputStream zis,
                       String parentVfsPath)
              throws IOException
      {
         super(context, parent, entry.getName());
         this.entry = entry;
         this.entryURL = entryURL;
         this.vfsPath = parentVfsPath + "/" + entry.getName();
         this.isJar = JarUtils.isArchive(entry.getName());
         int size = (int) entry.getSize();
         if (size <= 0)
            return;

         ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
         byte[] tmp = new byte[1024];
         while (zis.available() > 0)
         {
            int length = zis.read(tmp);
            if (length > 0)
               baos.write(tmp, 0, length);
         }
         contents = baos.toByteArray();
      }

      public boolean isHidden() throws IOException
      {
         return false;
      }

      public ZipEntry getEntry()
      {
         return entry;
      }

      public byte[] getContents()
      {
         return contents;
      }

      public String getName()
      {
         return entry.getName();
      }

      public String getPathName()
      {
         return vfsPath;
      }

      public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
      {
         List<VirtualFileHandler> children = null;
         if (isJar)
         {
            initNestedJar();
            children = njar.getChildren(ignoreErrors);
         }
         return children;
      }

      public VirtualFileHandler findChild(String path) throws IOException
      {
         VirtualFileHandler child;
         if (isJar)
         {
            initNestedJar();
            child = njar.findChild(path);
         }
         else
         {
            throw new FileNotFoundException("JarEntryContents(" + entry.getName() + ") has no children");
         }
         return child;
      }

      // Convience attribute accessors
      public long getLastModified()
      {
         return entry.getTime();
      }

      public long getSize()
      {
         return entry.getSize();
      }

      public boolean isLeaf()
      {
         if (isJar)
            return false;
         return entry.isDirectory() == false;
      }

      // Stream accessor
      public synchronized InputStream openStream()
              throws IOException
      {
         initNestedJar();
         if (njar != null)
            openStream = njar.openStream();
         else
            openStream = new ByteArrayInputStream(contents);
         return openStream;
      }

      public synchronized void close()
      {
         if (openStream != null)
         {
            try
            {
               openStream.close();
            }
            catch (IOException e)
            {
               log.error("close error", e);
            }
            openStream = null;
         }
      }

      public URI toURI() throws URISyntaxException
      {
         return entryURL.toURI();
      }

      public String toString()
      {
         StringBuffer tmp = new StringBuffer(super.toString());
         tmp.append('[');
         tmp.append("name=");
         tmp.append(entry.getName());
         tmp.append(",size=");
         tmp.append(entry.getSize());
         tmp.append(",time=");
         tmp.append(entry.getTime());
         tmp.append(",URI=");
         try
         {
            tmp.append(toURI());
         }
         catch (URISyntaxException e)
         {
         }
         tmp.append(']');
         return tmp.toString();
      }

      private synchronized void initNestedJar()
              throws IOException
      {
         if (isJar && njar == null)
         {
            ByteArrayInputStream bais = new ByteArrayInputStream(contents);
            ZipInputStream zis = new ZipInputStream(bais);
            njar = new NestedJarFromStream(getVFSContext(), this, zis, entryURL, entry);
         }
      }
   }
}
