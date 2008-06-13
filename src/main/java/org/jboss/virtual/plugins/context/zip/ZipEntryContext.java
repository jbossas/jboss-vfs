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
package org.jboss.virtual.plugins.context.zip;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.plugins.context.AbstractVFSContext;
import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.plugins.context.DelegatingHandler;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.plugins.copy.AbstractCopyMechanism;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * <tt>ZipEntryContext</tt> implements a {@link org.jboss.virtual.spi.VFSContext}
 * that exposes a zip archive as a virtual file system.
 *
 * Zip archive can be in a form of a file or a stream.
 *
 * Nested archives are processed through this same class.
 * By default nested archives are cached in memory and mounted as new
 * instances of <tt>ZipEntryContext</tt> with <tt>ZipStremWrapper</tt> as a source.
 * If system property <em>jboss.vfs.forceCopy=true</em> is specified,
 * or URL query parameter <em>forceCopy=true</em> is present,
 * nested archives are extracted into a temp directory before being
 * mounted as new instances of <tt>ZipEntryContext</tt>.
 *
 * This context implementation has two modes of releasing file locks.
 * <em>Asynchronous</em> mode is the default one since it is better performant.
 * To switch this to <em>synchronous</em> mode a system property
 * <em>jboss.vfs.forceNoReaper=true</em> can be specified or URL query parameter
 * <em>noReaper=true</em> can be included in context URL.
 *
 * This context implementation is a replacement for
 * {@link org.jboss.virtual.plugins.context.jar.JarContext}.
 *
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.0 $
 */
public class ZipEntryContext extends AbstractVFSContext
{
   private static final Logger log = Logger.getLogger(ZipEntryContext.class);

   /** Global setting for nested archive processing mode: copy or no-copy (default) */
   private static boolean forceCopy;

   static
   {
      deleteTmpDirContents();

      forceCopy = AccessController.doPrivileged(new CheckForceCopy());

      if (forceCopy)
         log.info("VFS force nested jars copy-mode is enabled.");
   }

   /** Abstracted access to zip archive - either ZipFileWrapper or ZipStreamWrapper */
   private ZipWrapper zipSource;

   /** Entry path representing a context root - archive root is not necessarily a context root */
   private String rootEntryPath = "";

   /** AutoClean signals if zip archive should be deleted after closing the context - true for nested archives */
   private boolean autoClean = false;

   /** Registry of everything that zipSource contains */
   private Map<String, EntryInfo> entries = new ConcurrentHashMap<String, EntryInfo>();

   /**
    * Create a new ZipEntryContext
    *
    * @param rootURL - file or jar:file url
    * @throws URISyntaxException for any URI error
    * @throws java.io.IOException for any error
    */
   public ZipEntryContext(URL rootURL) throws URISyntaxException, IOException
   {
      this(rootURL, false);
   }

   /**
    * Create a new ZipEntryContext
    *
    * @param rootURL - file or jar:file url
    * @param autoClean - true if file represented by rootURL should be deleted after this context is closed
    * @throws URISyntaxException for any URI error
    * @throws java.io.IOException for any error
    */
   public ZipEntryContext(URL rootURL, boolean autoClean) throws URISyntaxException, IOException
   {
      super(VFSUtils.toURI(fixUrl(rootURL)));
      this.autoClean = autoClean;
      init(rootURL, null, null);
   }

   /**
    * Create a new ZipEntryContext to be mounted into another context
    *
    * @param rootURL - url representing this context within another context
    * @param peer - file handler in another context through which this context is being mounted
    * @param localRootUrl - file or jar:file url
    * @throws URISyntaxException for any URI error
    * @throws java.io.IOException for any error
    */
   public ZipEntryContext(URL rootURL, VirtualFileHandler peer, URL localRootUrl) throws URISyntaxException, IOException
   {
      this(rootURL, peer, localRootUrl, false);
   }

   /**
    * Create a new ZipEntryContext to be mounted into another context
    *
    * @param rootURL - url representing this context within another context
    * @param peer - file handler in another context through which this context is being mounted
    * @param localRootUrl - file or jar:file url
    * @param autoClean - true if file represented by localRootURL should be deleted after this context is closed
    * @throws URISyntaxException for any URI error
    * @throws java.io.IOException for any error
    */
   public ZipEntryContext(URL rootURL, VirtualFileHandler peer, URL localRootUrl, boolean autoClean) throws URISyntaxException, IOException
   {
      super(VFSUtils.toURI(fixUrl(rootURL)));
      this.autoClean = autoClean;
      init(localRootUrl, peer, null);
   }

   /**
    * Create a new ZipEntryContext to be mounted into another context
    *
    * @param rootURL - url representing this context within another context
    * @param peer - file handler in another context through which this context is being mounted
    * @param zipWrapper - abstracted zip archive source
    * @param autoClean - true if file represented by localRootURL should be deleted after this context is closed
    * @throws URISyntaxException for any URI error
    * @throws java.io.IOException for any error
    */
   public ZipEntryContext(URL rootURL, VirtualFileHandler peer, ZipWrapper zipWrapper, boolean autoClean) throws URISyntaxException, IOException
   {
      super(VFSUtils.toURI(fixUrl(rootURL)));
      this.autoClean = autoClean;
      init(null, peer, zipWrapper);
   }

   /**
    * Extra initialization that couldn't fit inside constructors
    *
    * @param localRootURL the local url
    * @param peer the peer
    * @param zipWrapper zip wrapper
    * @throws URISyntaxException for any URI error
    * @throws java.io.IOException for any error
    */
   private void init(URL localRootURL, VirtualFileHandler peer, ZipWrapper zipWrapper) throws IOException, URISyntaxException
   {
      if (zipWrapper == null)
      {
         if (localRootURL == null)
            throw new IllegalArgumentException("No ZipWrapper specified and localRootURL is null");

         // initialize rootEntryPath and get archive file path
         String rootPath = initRootAndPath(localRootURL);
         zipSource = createZipSource(rootPath);
      }
      else
      {
         zipSource = zipWrapper;
      }
      
      setRootPeer(peer);

      String name = getRootURI().toString();
      int toPos = name.length();

      // cut off any ending slash
      if(name.length() != 0 && name.charAt(name.length()-1) == '/')
         toPos --;

      // name is last path component
      int namePos = name.lastIndexOf("/", toPos-1);
      name = name.substring(namePos+1, toPos);
      
      // cut off any ending exclamation
      if(name.length() != 0 && name.charAt(name.length()-1) == '!')
         name = name.substring(0, name.length()-1);

      // init initial root EntryInfo that will be overwritten
      // if zip entry exists for rootEntryPath
      entries.put("", new EntryInfo(new ZipEntryHandler(this, null, name, true), null));

      initEntries();
   }

   /**
    * Create zip source.
    *
    * @param rootPath the root path
    * @return zip entry wrapper
    * @throws IOException for any error
    */
   protected ZipWrapper createZipSource(String rootPath) throws IOException
   {
      File file = null;
      String relative = null;
      File fp = new File(rootPath);
      if (fp.exists())
      {
         file = fp;
      }
      else
      {
         File curr = fp;
         relative = fp.getName();
         while ((curr = curr.getParentFile()) != null)
         {
            if (curr.exists())
            {
               file = curr;
               break;
            }
            else
            {
               relative = curr.getName() + "/" + relative;
            }
         }
      }

      if (file == null)
         throw new IOException("VFS file does not exist: " + rootPath);

      if (relative != null)
      {
         return findEntry(new FileInputStream(file), relative);
      }
      else
      {
         boolean noReaper = Boolean.valueOf(getOptions().get(VFSUtils.NO_REAPER_QUERY));
         return new ZipFileWrapper(file, autoClean, noReaper);
      }
   }

   protected ZipWrapper findEntry(InputStream is, String relative) throws IOException
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ZipEntryContext.copyStreamAndClose(is, baos);
      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

      ZipInputStream zis = new ZipInputStream(bais);
      ZipEntry entry;
      String longestNameMatch = null;
      while((entry = zis.getNextEntry()) != null)
      {
         String entryName = entry.getName();
         if (relative.startsWith(entryName))
         {
            if (entryName.equals(relative))
            {
               InputStream stream = new SizeLimitedInputStream(zis, entry.getSize());
               if (JarUtils.isArchive(entryName))
               {
                  return new ZipStreamWrapper(stream, entryName, System.currentTimeMillis());
               }
               else
                  return new ZipEntryWrapper(stream, entryName, System.currentTimeMillis());
            }

            if (longestNameMatch == null || longestNameMatch.length() < entryName.length())
            {
               longestNameMatch = entryName;
            }
         }
      }
      if (longestNameMatch == null)
         throw new IllegalArgumentException("Cannot find entry: " + is + ", " + relative);

      bais.reset();
      zis = new ZipInputStream(bais);
      while((entry = zis.getNextEntry()) != null)
      {
         String entryName = entry.getName();
         if (entryName.equals(longestNameMatch))
         {
            relative = relative.substring(longestNameMatch.length() + 1);
            return findEntry(new SizeLimitedInputStream(zis, entry.getSize()), relative);
         }
      }
      throw new IllegalArgumentException("No such entry: " + is + ", " + relative);
   }

   /**
    * Returns archive file name - if this is a top-level ZipEntryContext.
    * Otherwise it returns the last component of URL.
    *
    * @return name
    */
   public String getName()
   {
      VirtualFileHandler peer = getRootPeer();
      if (peer != null)
         return peer.getName();
      else
         return zipSource.getName();
   }

   /**
    * Iterate through zip archive entries, compose a tree structure of archive's content
    *
    * @throws URISyntaxException for any URI error
    * @throws java.io.IOException for any error
    */
   private synchronized void initEntries() throws IOException, URISyntaxException
   {
      // we're using a two phase approach - we first select the relevant ones
      // then we order these by name and only then we process them
      // this way we ensure that parent entries are processed before child entries

      Map<String, ZipEntry> relevant = new HashMap<String, ZipEntry>();
      zipSource.acquire();
      try
      {
         Enumeration<? extends ZipEntry> zipEntries = zipSource.entries();
         // zoom-in on entries under rootEntryPath - ignoring the rest
         while(zipEntries.hasMoreElements())
         {
            ZipEntry ent = zipEntries.nextElement();
            if(ent.getName().startsWith(rootEntryPath))
            {
               relevant.put(ent.getName(), ent);
            }
         }

         Map<String, ZipEntry> orderedRelevant = new TreeMap<String, ZipEntry>(relevant);
         for(Map.Entry<String, ZipEntry> entry : orderedRelevant.entrySet())
         {
            ZipEntry ent = entry.getValue();
            String fullName = ent.getName().substring(rootEntryPath.length());

            String [] split = splitParentChild(fullName);
            String parentPath = split[0];
            String name = split[1];

            EntryInfo ei = entries.get(parentPath);
            if(ei == null)
               ei = makeDummyParent(parentPath);

            AbstractVirtualFileHandler parent = ei != null ? ei.handler : null;

            if(ent.isDirectory() == false && JarUtils.isArchive(ent.getName()))
            {
               boolean useCopyMode = forceCopy;
               if (useCopyMode == false)
               {
                  String flag = getOptions().get(VFSUtils.USE_COPY_QUERY);
                  useCopyMode = Boolean.valueOf(flag);
               }

               DelegatingHandler delegator;

               if (useCopyMode)
               {
                  // extract it to temp dir
                  File dest = new File(getTempDir() + "/" + getTempFileName(ent.getName()));
                  dest.deleteOnExit();

                  // ensure parent exists
                  dest.getParentFile().mkdirs();

                  InputStream is = zipSource.openStream(ent);
                  OutputStream os = new BufferedOutputStream(new FileOutputStream(dest));
                  copyStreamAndClose(is, os);

                  // mount another instance of ZipEntryContext
                  delegator = mountZipFile(parent, name, dest);
               }
               else
               {
                  // mount another instance of ZipEntryContext
                  delegator = mountZipStream(parent, name, zipSource.openStream(ent));
               }

               entries.put(delegator.getLocalPathName(), new EntryInfo(delegator, ent));
               addChild(parent, delegator);
            }
            else
            {
               ZipEntryHandler wrapper = new ZipEntryHandler(this, parent, name, ent.isDirectory() == false);
               entries.put(wrapper.getLocalPathName(), new EntryInfo(wrapper, ent));
            }
         }
      }
      finally
      {
         zipSource.release();
      }
   }

   /**
    * Mount ZipEntryContext created around extracted nested archive
    *
    * @param parent the parent
    * @param name the name
    * @param file the file
    * @return mounted delegate
    * @throws IOException for any error
    * @throws URISyntaxException for any URI syntax error
    */
   protected DelegatingHandler mountZipFile(VirtualFileHandler parent, String name, File file) throws IOException, URISyntaxException
   {
      DelegatingHandler delegator = new DelegatingHandler(this, parent, name);
      URL fileUrl = file.toURL();
      URL delegatorUrl = fileUrl;

      if (parent != null)
         delegatorUrl = getChildURL(parent, name);

      ZipEntryContext ctx = new ZipEntryContext(delegatorUrl, delegator, fileUrl, true);
      VirtualFileHandler handler = ctx.getRoot();
      delegator.setDelegate(handler);

      return delegator;
   }

   /**
    * Mount ZipEntryContext created around ZipStreamWrapper
    *
    * @param parent the parent
    * @param name the name
    * @param zipStream the zip stream
    * @return mounted delegate
    * @throws IOException for any error
    * @throws URISyntaxException for any URI syntax error
    */
   protected DelegatingHandler mountZipStream(VirtualFileHandler parent, String name, InputStream zipStream) throws IOException, URISyntaxException
   {
      DelegatingHandler delegator = new DelegatingHandler(this, parent, name);
      ZipStreamWrapper wrapper = new ZipStreamWrapper(zipStream, name, parent.getLastModified());

      URL delegatorUrl = null;

      if (parent != null)
         delegatorUrl = getChildURL(parent, name);

      ZipEntryContext ctx = new ZipEntryContext(delegatorUrl, delegator, wrapper, false);
      VirtualFileHandler handler = ctx.getRoot();
      delegator.setDelegate(handler);

      return delegator;
   }

   /**
    * Zip archives sometimes don't contain directory entries - only leaf entries
    *
    * @param parentPath the parent path
    * @return entry info
    * @throws IOException for any error
    */
   private EntryInfo makeDummyParent(String parentPath) throws IOException
   {
      // get grand parent first
      String [] split = splitParentChild(parentPath);
      String grandPa = split[0];

      EntryInfo eiParent = entries.get(grandPa);
      if(eiParent == null)
         eiParent = makeDummyParent(grandPa);

      ZipEntryHandler handler = new ZipEntryHandler(this, eiParent.handler, split[1], false);
      EntryInfo ei = new EntryInfo(handler, null);
      entries.put(parentPath, ei);
      return ei;
   }

   /**
    * Initialize rootEntryPath and return archive file path
    *
    * @param localRootUrl local root url
    * @return file path
    */
   private String initRootAndPath(URL localRootUrl)
   {
      String filePath = localRootUrl.toString();
      String zipPath = filePath;

      int pos = filePath.indexOf("!");
      if(pos > 0)
      {
         zipPath = filePath.substring(0, pos);
         rootEntryPath = filePath.substring(pos+2);
         if(rootEntryPath.length() != 0)
            rootEntryPath += "/";
      }

      // find where url protocol ends - i.e. jar:file:/ ...
      pos= zipPath.indexOf(":/");
      filePath = zipPath.substring(pos + 2);

      // cut out url query part if present
      int queryStart = filePath.indexOf("?");
      if (queryStart != -1)
         filePath = filePath.substring(0, queryStart);

      return filePath;
   }

   /**
    * If archive has been modified, clear <em>entries</em> and re-initialize
    */
   private synchronized void checkIfModified()
   {
      // TODO: if zipSource represents a nested archive we should maybe delegate lastModified to its parent
      if (zipSource.hasBeenModified())
      {
         EntryInfo rootInfo = entries.get("");
         entries = new ConcurrentHashMap<String, EntryInfo>();
         entries.put("", rootInfo);

         if (zipSource.exists())
         {
            try
            {
               initEntries();
            }
            catch(Exception ignored)
            {
               log.warn("IGNORING: Failed to reinitialize context: " + getRootURI(), ignored);
            }
         }
      }
   }

   public VirtualFileHandler getRoot() throws IOException
   {
      return entries.get("").handler;
   }

   public VirtualFileHandler getChild(ZipEntryHandler parent, String name)
   {
      if (parent == null)
         throw new IllegalArgumentException("Null parent");

      checkIfModified();
      String pathName = parent.getLocalPathName();
      if("".equals(pathName))
         pathName = name;
      else
         pathName = pathName + "/" + name;

      EntryInfo ei = entries.get(pathName);
      
      if(ei != null)
         return ei.handler;

      return null;
   }

   public List<VirtualFileHandler> getChildren(VirtualFileHandler parent, boolean ignoreErrors) throws IOException
   {
      if (parent == null)
         throw new IllegalArgumentException("Null parent");

      checkIfModified();
      if(parent instanceof AbstractVirtualFileHandler)
      {
         AbstractVirtualFileHandler parentHandler  = (AbstractVirtualFileHandler) parent;
         EntryInfo parentEntry = entries.get(parentHandler.getLocalPathName());
         if (parentEntry != null)
         {
            if (parentEntry.handler instanceof DelegatingHandler)
               return parentEntry.handler.getChildren(ignoreErrors);
            
            return parentEntry.getChildren();
         }
      }
      return Collections.emptyList();
   }

   public long getLastModified(ZipEntryHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");

      checkIfModified();
      EntryInfo ei = entries.get(handler.getLocalPathName());
      if(ei == null)
         return 0;

      if(ei.entry == null) {
         return zipSource.getLastModified();
      }

      return ei.entry.getTime();
   }

   public long getSize(ZipEntryHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");

      checkIfModified();
      String pathName = handler.getLocalPathName();
      EntryInfo ei = entries.get(pathName);
      if(ei == null)
         return 0;

      if(ei.entry == null)
      {
         if(pathName.length() == 0)
            return zipSource.getSize();
         else
            return 0;
      }

      return ei.entry.getSize();
   }

   public boolean exists(ZipEntryHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");

      checkIfModified();
      String pathName = handler.getLocalPathName();
      EntryInfo ei = entries.get(pathName);
      if(ei == null)
         return false;

      if (ei.entry == null && pathName.length() == 0)
         return zipSource.exists();

      return true;
   }

   public boolean isLeaf(ZipEntryHandler handler)
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");

      checkIfModified();
      EntryInfo ei = entries.get(handler.getLocalPathName());
      if(ei == null || ei.entry == null)
         return false;

      return !ei.entry.isDirectory();
   }

   /**
    * Is archive.
    *
    * @param handler the handler
    * @return true if archive
    */
   static boolean isArchive(VirtualFileHandler handler)
   {
      if (handler instanceof ZipEntryHandler && "".equals(handler.getLocalPathName()))
      {
         return true;
      }

      if (handler instanceof DelegatingHandler && ((DelegatingHandler) handler).getDelegate() instanceof ZipEntryHandler)
      {
         return true;
      }

      return false;
   }

   /**
    * Get parent.
    *
    * @param handler the handler to check
    * @return parent handler
    * @throws IOException for any error
    */
   static VirtualFileHandler getParent(VirtualFileHandler handler) throws IOException
   {
      VirtualFileHandler parent = handler.getParent();
      if (parent == null)
      {
         VirtualFileHandler peer = handler.getVFSContext().getRootPeer();
         if (peer != null)
            parent = peer.getParent();
      }
      return parent;
   }

   /**
    * Is nested.
    *
    * @param handler the handler
    * @return true if nested
    * @throws IOException for any error
    */
   static boolean isNested(VirtualFileHandler handler) throws IOException
   {
      VirtualFileHandler parent = getParent(handler);
      while (parent != null)
      {
         if(isArchive(parent))
            return true;

         parent = getParent(parent);
      }
      return false;
   }

   public InputStream openStream(ZipEntryHandler handler) throws IOException
   {
      if (handler == null)
         throw new IllegalArgumentException("Null handler");

      checkIfModified();

      String localPathName = handler.getLocalPathName();
      EntryInfo ei = entries.get(localPathName);

      if (ei == null)
      {
         String uriStr;
         try
         {
            uriStr = handler.toURI().toString();
         }
         catch(Exception ex)
         {
            throw new RuntimeException("ASSERTION ERROR - uri generation failed for ZipEntryHandler: " + handler, ex);
         }
         throw new FileNotFoundException(uriStr);
      }

      if(ei.entry == null)
      {
         if ("".equals(localPathName))  // root
            return zipSource.getRootAsStream();
         else                           // directory
            return new ByteArrayInputStream(new byte[0]);
      }
      
      return zipSource.openStream(ei.entry);
   }

   public void addChild(AbstractVirtualFileHandler parent, AbstractVirtualFileHandler child)
   {
      if (parent == null)
         throw new IllegalArgumentException("Null parent");

      if (child == null)
         throw new IllegalArgumentException("Null child");

      EntryInfo parentEntry = entries.get(parent.getLocalPathName());
      if (parentEntry != null)
         parentEntry.add(child);
      else
         throw new RuntimeException("Parent does not exist: " + parent);
   }

   protected void finalize()
   {
      try
      {
         super.finalize();
         if (zipSource != null)
            zipSource.close();
      }
      catch (Throwable ignored)
      {
         log.debug("IGNORING: Failed to close zip source: " + zipSource, ignored);
      }
   }

   public void replaceChild(ZipEntryHandler parent, AbstractVirtualFileHandler original, VirtualFileHandler replacement)
   {
      EntryInfo parentEntry = entries.get(parent.getLocalPathName());
      if (parentEntry != null)
      {
         DelegatingHandler newOne;

         if (replacement instanceof DelegatingHandler)
         {
            newOne = (DelegatingHandler) replacement;
         }
         else
         {
            newOne = new DelegatingHandler(this, parent, original.getName(), replacement);
         }

         synchronized(this)
         {
            parentEntry.replaceChild(original, newOne);

            EntryInfo ei = entries.get(original.getLocalPathName());
            ei.handler = newOne;
            ei.entry = null;
            ei.clearChildren();
         }
      }
      else
      {
         throw new RuntimeException("Parent does not exist: " + parent);
      }
   }

   /**
    *  Internal data structure holding meta information of a virtual file in this context
    */
   static class EntryInfo
   {
      private AbstractVirtualFileHandler handler;
      private ZipEntry entry;
      private List<AbstractVirtualFileHandler> children;

      EntryInfo(AbstractVirtualFileHandler handler, ZipEntry entry)
      {
         this.handler = handler;
         this.entry = entry;
      }

      public synchronized List<VirtualFileHandler> getChildren()
      {
         if (children == null)
            return Collections.emptyList();

         return new LinkedList<VirtualFileHandler>(children);
      }

      public synchronized void replaceChild(AbstractVirtualFileHandler original, AbstractVirtualFileHandler replacement)
      {
         if (children != null)
         {
            int i = 0;
            Iterator<AbstractVirtualFileHandler> it = children.iterator();
            while(it.hasNext())
            {
               AbstractVirtualFileHandler child = it.next();
               if (child.getName().equals(original.getName()))
               {
                  children.set(i, replacement);
                  break;
               }
               i++;
            }
         }
      }

      public synchronized void clearChildren()
      {
         if (children != null)
            children.clear();
      }

      public synchronized void add(AbstractVirtualFileHandler child)
      {
         if (children == null)
         {
            children = new LinkedList<AbstractVirtualFileHandler>();
         }
         else
         {
            // if a child exists with this name already, remove it
            Iterator<AbstractVirtualFileHandler> it = children.iterator();
            while (it.hasNext())
            {
               AbstractVirtualFileHandler handler = it.next();
               if (handler.getName().equals(child.getName()))
               {
                  it.remove();
                  break;
               }
            }
         }

         children.add(child);
      }
   }

   //
   //   Helper methods
   //

   /**
    * Copy input stream to output stream and close them both
    *
    * @param is input stream
    * @param os output stream
    * @throws IOException for any error
    */
   static void copyStreamAndClose(InputStream is, OutputStream os) throws IOException
   {
      try
      {
         byte [] buff = new byte[65536];
         int count = is.read(buff);
         while(count != -1)
         {
            os.write(buff, 0, count);
            count = is.read(buff);
         }
      }
      finally
      {
         if(is != null)
         {
            try {
               is.close();
            }
            catch(Exception ignored)
            {
            }
         }
         os.close();
      }
   }

   /**
    * Make sure url protocol is <em>vfszip</em>
    *
    * @param rootURL the root url
    * @return fixed url
    * @throws MalformedURLException for any error
    */
   private static URL fixUrl(URL rootURL) throws MalformedURLException
   {
      if ("vfszip".equals(rootURL.getProtocol()) == false)
      {
         String url = rootURL.toString();
         int pos = url.indexOf(":/");
         if (pos != -1)
            url = url.substring(pos);

         return new URL("vfszip" + url);
      }
      return rootURL;
   }

   /**
    * Break to path + name
    *
    * @param pathName the path name
    * @return path tokens
    */
   public static String [] splitParentChild(String pathName)
   {
      if(pathName.length() == 0)
         return new String [] {null, pathName};

      int toPos = pathName.length();
      if(pathName.charAt(pathName.length()-1) == '/')
         toPos --;

      int delimPos = pathName.lastIndexOf('/', toPos-1);

      String [] ret;
      if(delimPos == -1)
      {
         ret = new String []
         {
            "",
            pathName.substring(delimPos+1, toPos)
         };
      }
      else
      {
         ret = new String []
         {
            pathName.substring(0, delimPos),
            pathName.substring(delimPos+1, toPos)
         };
      }
      return ret;
   }

   /**
    * Temporary files naming scheme
    *
    * @param name the name
    * @return random name
    */
   private static String getTempFileName(String name)
   {
      int delim = name.lastIndexOf("/");
      if (delim != -1)
         name = name.substring(delim+1);
      return UUID.randomUUID().toString().substring(0, 8) + "_" + name;
   }

   /**
    * Use VFS's temp directory and make 'vfs-nested' sub-directory inside it for our purposes
    *
    * @return temp dir
    */
   private static String getTempDir()
   {
      File dir = new File(AbstractCopyMechanism.getTempDirectory(), "vfs-nested.tmp");
      return dir.toString();
   }

   /**
    * Delete first-level files only, don't drill down
    */
   private static void deleteTmpDirContents()
   {
      try
      {
         File tmpDir = new File(getTempDir());
         File [] files = tmpDir.listFiles();
         if (files != null && files.length > 0)
         {            
            for (File file : files)
            {
               if (file.isDirectory() == false && file.isHidden() == false)
                  file.delete();
            }
         }
      }
      catch(Exception ignored)
      {
      }
   }

   private static class CheckForceCopy implements PrivilegedAction<Boolean>
   {
      public Boolean run()
      {
         String forceString = System.getProperty(VFSUtils.FORCE_COPY_KEY, "false");
         return Boolean.valueOf(forceString);
      }
   }
}
