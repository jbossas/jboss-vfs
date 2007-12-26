/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.jboss.virtual.plugins.context.jar;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * A nested jar implementation used to represent a jar within a jar.
 *
 * @author Ales.Justin@jboss.org
 * @author Scott.Stark@jboss.org
 * @version $Revision: 44334 $
 */
public class NestedJarFromStream extends AbstractJarHandler
{
   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 1L;

   private ZipInputStream zis;
   private Map<String, JarEntryContents> entries = new HashMap<String, JarEntryContents>();
   private URL jarURL;
   private URL entryURL;
   private long lastModified;
   private long size;
   private AtomicBoolean inited = new AtomicBoolean(false);

   /**
    * Create a nested jar from the parent zip inputstream/zip entry.
    *
    * @param context   - the context containing the jar
    * @param parent    - the jar handler for this nested jar
    * @param zis       - the jar zip input stream
    * @param jarURL    - the URL to use as the jar URL
    * @param jar       - the parent jar file for the nested jar
    * @param entry     - the zip entry
    * @param entryName - the entry name
    * @throws IOException for any error
    */
   public NestedJarFromStream(VFSContext context, VirtualFileHandler parent, ZipInputStream zis, URL jarURL, JarFile jar, ZipEntry entry, String entryName) throws IOException
   {
      super(context, parent, jarURL, jar, entry, entryName);
      this.jarURL = jarURL;
      this.lastModified = entry.getTime();
      this.size = entry.getSize();
      this.zis = zis;
      try
      {
         setPathName(getChildPathName(entryName, false));
         setVfsUrl(getChildVfsUrl(entryName, false));
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   protected void initCacheLastModified()
   {
      cachedLastModified = lastModified;
   }

   /**
    * Initialize entries.
    *
    * @throws IOException for any error
    */
   protected void init() throws IOException
   {
      if (inited.get())
         return;

      inited.set(true);
      try
      {
         ZipEntry entry = zis.getNextEntry();
         while (entry != null)
         {
            try
            {
               String entryName = entry.getName();
               String url = toURI().toASCIIString() + "!/" + entryName;
               URL jecURL = new URL(url);
               JarEntryContents jec = new JarEntryContents(getVFSContext(), this, entry, toURL(), jecURL, zis);
               entries.put(entryName, jec);
               entry = zis.getNextEntry();
            }
            catch (Throwable t)
            {
               IOException ioe = new IOException("Exception while reading nested jar entry: " + this);
               ioe.initCause(t);
               ioe.setStackTrace(t.getStackTrace());
               throw ioe;
            }
         }
      }
      finally
      {
         try
         {
            zis.close();
         }
         catch (IOException ignored)
         {
         }
         zis = null;
      }
   }

   public VirtualFileHandler findChild(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      if ("".equals(path))
         return this;

      JarEntryContents handler = getEntry(path);
      if (handler == null)
         throw new IOException("No such child: " + path);
      return handler;
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      init();
      return new ArrayList<VirtualFileHandler>(entries.values());
   }

   /**
    * TODO: removing the entry/jar that resulted in this needs
    * to be detected.
    */
   public boolean exists() throws IOException
   {
      return true;
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

   public Iterator<JarEntryContents> getEntries() throws IOException
   {
      init();
      return entries.values().iterator();
   }

   public JarEntryContents getEntry(String name) throws IOException
   {
      init();
      return entries.get(name);
   }

   public ZipEntry getZipEntry(String name) throws IOException
   {
      JarEntryContents jec = getEntry(name);
      return (jec != null ? jec.getEntry() : null);
   }

   public byte[] getContents(String name) throws IOException
   {
      JarEntryContents jec = getEntry(name);
      return (jec != null ? jec.getContents() : null);
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

   /**
    * First, if not already, initialize entries.
    * Then do simple write. 
    *
    * @param out object output stream
    * @throws IOException for any error
    */
   private void writeObject(ObjectOutputStream out) throws IOException
   {
      init();
      out.defaultWriteObject();
   }
}
