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
package org.jboss.virtual.plugins.context.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Properties;

import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.AbstractVFSContext;
import org.jboss.virtual.plugins.context.DelegatingHandler;
import org.jboss.virtual.plugins.context.zip.ZipEntryContext;
import org.jboss.virtual.plugins.context.jar.JarHandler;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.spi.LinkInfo;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.logging.Logger;

/**
 * FileSystemContext.
 *
 * Jar archives are processed through {@link org.jboss.virtual.plugins.context.zip.ZipEntryContext}.
 *
 * To switch back to {@link org.jboss.virtual.plugins.context.jar.JarHandler}
 * set a system property <em>jboss.vfs.forceVfsJar=true</em>
 *
 * Explicit case sensitive path checking can be turned on by adding an option parameter
 * <em>?caseSensitive=true<em> to context URL. This may be desired when native filesystem is not
 * case sensitive (i.e. if running on Windows).
 *
 * Case sesitivity can be turned on for all context URLs by setting system property
 * <em>jboss.vfs.forceCaseSensitive=true</em>.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.1 $
 */
public class FileSystemContext extends AbstractVFSContext
{

   private static final Logger log = Logger.getLogger(ZipEntryContext.class);

   /** true if forcing fallback to vfsjar from default vfszip */
   private static boolean forceVfsJar;

   /** true if case sensitivity should be enforced */
   private static boolean forceCaseSensitive;

   static
   {
      forceVfsJar = AccessController.doPrivileged(new CheckForceVfsJar());

      if (forceVfsJar)
         log.warn("VFS forced fallback to vfsjar is enabled.");

      forceCaseSensitive = AccessController.doPrivileged(new CheckForceCaseSensitive());

      if (forceCaseSensitive)
         log.debug("VFS forced case sensitivity is enabled.");
   }

   /** The root file */
   private final VirtualFileHandler root;
   
   /** A reference to the virtual file of the root to stop it getting closed */
   private final VirtualFile rootFile;
   
   /**
    * Get the file for a url
    * 
    * @param uri the url
    * @return the file
    * @throws IOException for any error accessing the file system
    * @throws URISyntaxException if cannot create URI 
    * @throws IllegalArgumentException for a null url
    */
   private static File getFile(URI uri) throws IOException, URISyntaxException
   {
      if (uri == null)
         throw new IllegalArgumentException("Null uri");
      // This ctor will not accept uris with authority, fragment or query
      if(uri.getAuthority() != null || uri.getFragment() != null || uri.getQuery() != null)
         uri = new URI("file", null, uri.getPath(), null);
      return new File(uri);
   }

   /**
    * Get the url for a file
    * 
    * @param file the file
    * @return the url
    * @throws IOException for any error accessing the file system
    * @throws IllegalArgumentException for a null file
    */
   private static URI getFileURI(File file) throws IOException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");

      URI url = file.toURI();
      String path = url.getPath();
      if (file.isDirectory() == false)
      {
         path = VFSUtils.fixName(path);
      }
      else if (path.endsWith("/") == false)
      {
            path = path + '/';
      }

      try
      {
         return new URI("file", null, path, null);
      }
      catch(URISyntaxException e)
      {
         // Should not be possible
         throw new IllegalStateException("Failed to convert file.toURI", e);
      }
   }
   
   /**
    * Create a new FileSystemContext.
    * 
    * @param rootURL the root url
    * @throws IOException for an error accessing the file system
    * @throws URISyntaxException for an error parsing the uri
    */
   public FileSystemContext(URL rootURL) throws IOException, URISyntaxException
   {
      this(VFSUtils.toURI(rootURL));
   }

   /**
    * Create a new FileSystemContext.
    * 
    * @param rootURI the root uri
    * @throws IOException for an error accessing the file system
    * @throws URISyntaxException if cannot create URI
    */
   public FileSystemContext(URI rootURI) throws IOException, URISyntaxException
   {
      this(rootURI, getFile(rootURI));
   }
   
   /**
    * Create a new FileSystemContext.
    * 
    * @param file the root file
    * @throws IOException for an error accessing the file system
    * @throws IllegalArgumentException for a null file
    * @throws URISyntaxException for an error parsing the uri
    */
   public FileSystemContext(File file) throws IOException, URISyntaxException
   {
      this(getFileURI(file), file);
   }

   /**
    * Create a new FileSystemContext.
    * 
    * @param rootURI the root uri
    * @param file the file
    * @throws IOException for an error accessing the file system
    */
   private FileSystemContext(URI rootURI, File file) throws IOException
   {
      super(rootURI);
      root = createVirtualFileHandler(null, file);
      if (root == null)
         throw new IllegalArgumentException("Null root, rootURI: " + rootURI + ", file: " + file);

      rootFile = root.getVirtualFile();
   }

   public String getName()
   {
      return root.getName();
   }

   public VirtualFileHandler getRoot() throws IOException
   {
      return root;
   }

   /**
    * Create a new virtual file handler
    * 
    * @param parent the parent
    * @param file the file
    * @return the handler
    * @throws IOException for any error accessing the file system
    * @throws IllegalArgumentException for a null file
    */
   public VirtualFileHandler createVirtualFileHandler(VirtualFileHandler parent, File file) throws IOException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
      
      String name = file.getName();
      if (file.isFile() && JarUtils.isArchive(name))
      {
         if (exists(file) == false)
            return null;

         if (forceVfsJar)
         {
            try
            {
               return new JarHandler(this, parent, file, file.toURL(), name);
            }
            catch(IOException e)
            {
               log.debug("Exception while trying to handle file (" + name + ") as a jar: " + e.getMessage());
            }
         }
         else
         {
            try
            {
               return mountZipFS(parent, name, file);
            }
            catch (Exception e)
            {
               log.debug("IGNORING: Exception while trying to handle file (" + name + ") as a jar through ZipEntryContext: ", e);
            }
         }
      }
      return createVirtualFileHandler(parent, file, getFileURI(file));
   }

   /**
    * Create zip file system.
    *
    * @param parent the parent
    * @param name the name
    * @param file the file
    * @return new zip fs delegating handler
    * @throws IOException for any error
    * @throws URISyntaxException for any URI syntax error
    */
   protected DelegatingHandler mountZipFS(VirtualFileHandler parent, String name, File file) throws IOException, URISyntaxException
   {
      DelegatingHandler delegator = new DelegatingHandler(this, parent, name);
      URL fileUrl = file.toURL();
      URL delegatorUrl = fileUrl;

      if (parent != null)
         delegatorUrl = getChildURL(parent, name);

      ZipEntryContext ctx = new ZipEntryContext(delegatorUrl, delegator, fileUrl);

      VirtualFileHandler handler = ctx.getRoot();
      delegator.setDelegate(handler);

      return delegator;
   }
   
   /**
    * Create a new virtual file handler
    * 
    * @param parent the parent
    * @param file the file
    * @param uri the uri
    * @return the handler
    * @throws IOException for any error accessing the file system
    * @throws IllegalArgumentException for a null file
    */
   public VirtualFileHandler createVirtualFileHandler(VirtualFileHandler parent, File file, URI uri)
      throws IOException
   {
      if (file == null)
         throw new IllegalArgumentException("Null file");
      if (uri == null)
         throw new IllegalArgumentException("Null uri");

      VirtualFileHandler handler = null;
      if( VFSUtils.isLink(file.getName()) )
      {
         Properties props = new Properties();
         FileInputStream fis = new FileInputStream(file);
         try
         {
            List<LinkInfo> links = VFSUtils.readLinkInfo(fis, file.getName(), props);
            String name = props.getProperty(VFSUtils.VFS_LINK_NAME, "link");
            handler = new LinkHandler(this, parent, uri, name, links);            
         }
         catch(URISyntaxException e)
         {
            IOException ex = new IOException("Failed to parse link URIs");
            ex.initCause(e);
            throw ex;
         }
         finally
         {
            try
            {
               fis.close();
            }
            catch(IOException e)
            {
               log.debug("Exception closing file input stream: " + fis, e);
            }
         }
      }
      else if (exists(file) == false && parent != null)
      {
         // See if we can resolve this to a link in the parent
         List<VirtualFileHandler> children = parent.getChildren(true);
         for(VirtualFileHandler vfh : children)
         {
            if( vfh.getName().equals(file.getName()) )
            {
               handler = vfh;
               break;
            }
         }
      }
      else if (exists(file))
      {
         handler = new FileHandler(this, parent, file, uri);
      }
      return handler;
   }

   /**
    * Tests if file exists taking case sensitivity into account - if it's enabled
    *
    * @param file file to check
    * @return true if file exists
    * @throws IOException for any error
    */
   protected boolean exists(File file) throws IOException
   {
      // if force case sensitive is enabled - extra check is required
      boolean isCaseSensitive = forceCaseSensitive;
      if (isCaseSensitive == false)
      {
         String flag = getOptions().get(VFSUtils.CASE_SENSITIVE_QUERY);
         isCaseSensitive = Boolean.valueOf(flag);
      }

      if (isCaseSensitive && file.getCanonicalFile().getName().equals(file.getName()) == false)
         return false;

      return file.exists();
   }

   /**
    * Is forceCaseSensitive enabled
    *
    * Only relevant for native filesystems
    * that are not case sensitive
    *
    * @return true if case sensitivity is enabled
    */
   public boolean isForcedCaseSensitive() {
      return forceCaseSensitive;
   }

   @Override
   protected void finalize() throws Throwable
   {
      if (rootFile != null)
         rootFile.close();
      super.finalize();
   }

   private static class CheckForceVfsJar implements PrivilegedAction<Boolean>
   {
      public Boolean run()
      {
         String forceString = System.getProperty(VFSUtils.FORCE_VFS_JAR_KEY, "false");
         return Boolean.valueOf(forceString);
      }
   }

   private static class CheckForceCaseSensitive implements PrivilegedAction<Boolean>
   {
      public Boolean run()
      {
         String forceString = System.getProperty(VFSUtils.FORCE_CASE_SENSITIVE_KEY, "false");
         return Boolean.valueOf(forceString);
      }
   }
}
