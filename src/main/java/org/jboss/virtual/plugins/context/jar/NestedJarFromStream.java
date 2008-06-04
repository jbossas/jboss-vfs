/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.jboss.virtual.plugins.context.jar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream.PutField;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
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
public class NestedJarFromStream extends AbstractStructuredJarHandler<byte[]>
{
   /**
    * serialVersionUID
    */
   private static final long serialVersionUID = 2L;
   /** The class serial fields */
   private static final ObjectStreamField[] serialPersistentFields = {
      new ObjectStreamField("zisBytes", byte[].class),
      new ObjectStreamField("jarURL", URL.class),
      new ObjectStreamField("entryURL", String.class),
      new ObjectStreamField("lastModified", long.class),
      new ObjectStreamField("size", long.class)
   };

   private transient ZipInputStream zis;
   private transient AtomicBoolean inited = new AtomicBoolean(false);
   private URL jarURL;
   private URL entryURL;
   private long lastModified;
   private long size;

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
      if (inited.get() == false)
      {
         inited.set(true);
         try
         {
            initJarFile(new ZisEnumeration());
         }
         finally
         {
            close();
         }
      }
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      init();
      return super.getChildren(ignoreErrors);
   }

   public VirtualFileHandler getChild(String path) throws IOException
   {
      init();
      return super.getChild(path);
   }

   public VirtualFileHandler createChildHandler(String name) throws IOException
   {
      init();
      return super.createChildHandler(name);
   }

   protected void extraWrapperInfo(ZipEntryWrapper<byte[]> wrapper) throws IOException
   {
      byte[] contents;
      int size = (int)wrapper.getSize();
      if (size != 0)
      {
         ByteArrayOutputStream baos = size > 0 ? new ByteArrayOutputStream(size) : new ByteArrayOutputStream();
         byte[] tmp = new byte[1024];
         while (zis.available() > 0)
         {
            int length = zis.read(tmp);
            if (length > 0)
               baos.write(tmp, 0, length);
         }
         contents = baos.toByteArray();
      }
      else
         contents = new byte[0];
      wrapper.setExtra(contents);
   }

   protected VirtualFileHandler createVirtualFileHandler(VirtualFileHandler parent, ZipEntryWrapper<byte[]> wrapper, String entryName) throws IOException
   {
      try
      {
         String url = toURI().toASCIIString() + "!/" + wrapper.getName();
         URL jecURL = new URL(url);
         VFSContext context = parent.getVFSContext();
         if (wrapper.isDirectory())
         {
            return new SynthenticDirEntryHandler(context, parent, entryName, System.currentTimeMillis(), jecURL);  
         }
         else
         {
            byte[] contents = wrapper.getExtra();
            return new JarEntryContents(context, parent, wrapper.getEntry(), entryName, toURL(), jecURL, contents);
         }
      }
      catch (Throwable t)
      {
         IOException ioe = new IOException("Exception while reading nested jar entry: " + this);
         ioe.initCause(t);
         ioe.setStackTrace(t.getStackTrace());
         throw ioe;
      }
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

   public boolean isNested() throws IOException
   {
      return true;
   }

   // Stream accessor
   public InputStream openStream() throws IOException
   {
      if(zis == null)
      {
         // Need to obtain this nested jar input stream from parent
         InputStream parentIS = super.getParent().openStream();
         if(parentIS == null)
            throw new IOException("Failed to open parent stream, "+this);
         if(parentIS instanceof ZipInputStream)
         {
            zis = (ZipInputStream) parentIS;
         }
         else
         {
            zis = new ZipInputStream(parentIS);
         }
         // First find our entry
         ZipEntry entry = zis.getNextEntry();
         while(entry != null)
         {
            if(entry.getName().equals(getName()))
               break;
            entry = zis.getNextEntry();
         }
         if(entry == null)
            throw new IOException("Failed to find nested jar entry: "+this.getName()+" in parent: "+getParent());
      }
      return zis;
   }

   public void close()
   {
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

   protected void initJarFile() throws IOException
   {
      // todo - deserialize
   }

   private class ZisEnumeration implements Enumeration<ZipEntryWrapper<byte[]>>
   {
      private boolean moved = true;
      private ZipEntry next = null;

      public boolean hasMoreElements()
      {
         if (zis == null)
            return false;
         
         try
         {
            if (moved)
            {
               next = zis.getNextEntry();
               moved = false;
            }
            return next != null;
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }

      public ZipEntryWrapper<byte[]> nextElement()
      {
         moved = true;
         return new ZipEntryWrapper<byte[]>(next);
      }
   }
   /**
    * Write the jar contents and information
    * @param out
    * @throws IOException
    */
   private void writeObject(ObjectOutputStream out)
      throws IOException
   {
      PutField fields = out.putFields();

      byte[] zisBytes;
      ZipInputStream jarStream = zis;
      if(jarStream == null)
      {
         // Need to obtain this nested jar input stream from parent
         InputStream parentIS = super.getParent().openStream();
         if(parentIS == null)
            throw new IOException("Failed to open parent stream, "+this);
         jarStream = new ZipInputStream(parentIS);
         // First find our entry
         ZipEntry entry = jarStream.getNextEntry();
         while(entry != null)
         {
            if(entry.getName().equals(getName()))
               break;
            entry = jarStream.getNextEntry();
         }
         if(entry == null)
            throw new IOException("Failed to find nested jar entry: "+this.getName()+" in parent: "+getParent());
      }
      // Now read zis for this entry
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] tmp = new byte[1024];
      int length = jarStream.read(tmp);
      while(length > 0)
      {
         baos.write(tmp, 0, length);
         length = jarStream.read(tmp);
      }
      jarStream.close();
      jarStream = null;
      zisBytes = baos.toByteArray();
      fields.put("zisBytes", zisBytes);
      fields.put("jarURL", jarURL);
      fields.put("entryURL", entryURL);
      fields.put("lastModified", lastModified);
      fields.put("size", size);
      out.writeFields();
   }

   /**
    * Read the jar contents and reinitialize the entry map
    * @param in
    * @throws IOException
    * @throws ClassNotFoundException
    */
   private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException
   {
      // Read in the serialPersistentFields
      GetField fields = in.readFields();
      byte[] zisBytes =  (byte[]) fields.get("zisBytes", null);
      this.jarURL = (URL) fields.get("jarURL", null);
      this.entryURL = (URL) fields.get("entryURL", null);
      this.lastModified = fields.get("lastModified", (long)-1);
      this.size = fields.get("size", (long)-1);
      // Initialize the jar entry map
      inited = new AtomicBoolean(false);
      ByteArrayInputStream bais = new ByteArrayInputStream(zisBytes);
      zis = new ZipInputStream(bais);
      this.init();
   }
}
