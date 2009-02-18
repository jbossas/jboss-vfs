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
package org.jboss.virtual.plugins.context;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.vfs.helpers.PathTokenizer;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VFSContextFactory;
import org.jboss.virtual.spi.VFSContextFactoryLocator;
import org.jboss.virtual.spi.VirtualFileHandler;
import org.jboss.virtual.spi.Options;

/**
 * AbstractVirtualFileHandler.
 * 
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @author <a href="strukelj@parsek.net">Marko Strukelj</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractVirtualFileHandler implements VirtualFileHandler
{
   /** The log */
   protected static final Logger log = Logger.getLogger(AbstractVirtualFileHandler.class);
   /** serialVersionUID */
   private static final long serialVersionUID = 1L;
   /** The class serial fields */
   private static final ObjectStreamField[] serialPersistentFields =
   {
      new ObjectStreamField("rootURI", URI.class),
      new ObjectStreamField("parent", VirtualFileHandler.class),
      new ObjectStreamField("name", String.class),
      new ObjectStreamField("vfsUrl", URL.class)
   };

   /**
    * The VFS context
    *
    * @serialField rootURI URI the VFS context rootURI
    */
   private VFSContext context;
   
   /**
    * The parent
    *
    * @serialField parent VirtualFileHandler the virtual file parent
    */
   private VirtualFileHandler parent;

   /**
    * The name
    *
    * @serialField name String the virtual file name
    */
   private String name;

   /**
    * The vfs URL
    *
    * @serialField vfsUrl the vfs based url
    */
   private URL vfsUrl;

   /** The vfsPath */
   private transient String vfsPath;

   /** The local vfsPath */
   private transient String localVfsPath;

   /** The reference count */
   private transient AtomicInteger references = new AtomicInteger(0);

   /** The cached last modified */
   protected transient long cachedLastModified;

   /** The vfsUrlCache */
   private transient URL vfsUrlCached;

   /**
    * Create a new handler
    * 
    * @param context the context
    * @param parent the parent
    * @param name the name
    * @throws IllegalArgumentException if the context or name is null;
    */
   protected AbstractVirtualFileHandler(VFSContext context, VirtualFileHandler parent, String name)
   {
      if (context == null)
         throw new IllegalArgumentException("Null context");
      if (name == null)
         throw new IllegalArgumentException("Null name");
      this.context = context;
      this.parent = parent;
      this.name = VFSUtils.fixName(name);
      this.vfsPath = null; // nullify possible invalid vfsPath initializations when running with debugger
   }

   /**
    * Check if parent exists.
    */
   protected void checkParentExists()
   {
      if (parent == null)
         throw new IllegalArgumentException("Parent must exist!");
   }

   /**
    * Get child url.
    *
    * @param childPath the child path
    * @param isDirectory is directory
    * @return full child URL
    * @throws IOException for any io error
    * @throws URISyntaxException for any uri error
    */
   protected URL getChildVfsUrl(String childPath, boolean isDirectory) throws IOException, URISyntaxException
   {
      checkParentExists();
      URL parentVfsUrl = getParent().toVfsUrl();
      String vfsUrlString = parentVfsUrl.toString();
      if (vfsUrlString.length() > 0 && vfsUrlString.endsWith("/") == false)
         vfsUrlString += "/";
      vfsUrlString += childPath;
      if (isDirectory && vfsUrlString.endsWith("/") == false)
         vfsUrlString += "/";
      return new URL(vfsUrlString);
   }

   /**
    * Get child path name.
    *
    * @param childPath the child path
    * @param isDirectory is directory
    * @return full child URL
    * @throws IOException for any io error
    */
   protected String getChildPathName(String childPath, boolean isDirectory) throws IOException
   {
      checkParentExists();
      String childPathName = getParent().getPathName();
      if (childPathName.length() > 0 && childPathName.endsWith("/") == false)
         childPathName += "/";
      childPathName += childPath;
      if (isDirectory && childPathName.endsWith("/") == false)
         childPathName += "/";
      return childPathName;
   }

   public boolean isArchive() throws IOException
   {
      return false;
   }

   public boolean hasBeenModified() throws IOException
   {
      boolean hasBeenModified = false;
      long last = getLastModified();
      if (cachedLastModified != last)
      {
         hasBeenModified = cachedLastModified != 0;
         cachedLastModified = last;
      }
      return hasBeenModified;
   }

   public String getName()
   {
      return name;
   }

   /**
    * Get a pathName relative to most outer context (contexts can be mounted one within other)
    *
    * @return  pathName
    */
   public String getPathName()
   {
      if (vfsPath == null)
      {
         StringBuilder pathName = new StringBuilder();
         initPath(pathName);
         vfsPath = pathName.toString();
      }
      return vfsPath;
   }

   /**
    * todo This is a hack until we can fix http://jira.jboss.com/jira/browse/JBMICROCONT-164
    *
    * @param path the path name
    */
   public void setPathName(String path)
   {
      this.vfsPath = path;
   }

   /**
    * Get a pathName relative to local context
    *
    * @return pathName
    */
   public String getLocalPathName()
   {
      if (localVfsPath == null)
         localVfsPath = readLocalPathName();

      return localVfsPath;
   }

   /**
    * Create local path name.
    *
    * @return the local path name
    */
   private String readLocalPathName()
   {
      try
      {
         VirtualFileHandler handler = getLocalVFSContext().getRoot();
         String rootPathName = handler.getPathName();
         String pathName = getPathName();
         int len = rootPathName.length();
         if (len == 0)
            return pathName;
         else if (rootPathName.length() < pathName.length())
            return pathName.substring(len + 1);
         else
            return "";
      }
      catch (IOException ex)
      {
         log.warn("Failed to compose local path name: context: " + getLocalVFSContext() + ", name: " + getName(), ex);
      }

      return getPathName();
   }

   public URL toURL() throws MalformedURLException, URISyntaxException
   {
      return toURI().toURL();
   }

   public URL toVfsUrl() throws MalformedURLException, URISyntaxException
   {
      if (vfsUrlCached == null)
      {
         if (isTemporary())
         {
            try
            {
               VFSContext context = getVFSContext();
               String path = getPathName();

               Options options = context.getOptions();
               VirtualFileHandler oldRoot = options.getOption(VirtualFileHandler.class);
               if (oldRoot == null)
               {
                  StringBuffer buf = new StringBuffer();

                  URI rootURI = context.getRootURI();
                  URI copyURI = new URI(rootURI.getScheme(), rootURI.getHost(), rootURI.getPath(), null);
                  buf.append(copyURI.toURL().toExternalForm());

                  if (path != null && path.length() > 0)
                  {
                     if (buf.charAt(buf.length() - 1) != '/')
                     {
                        buf.append('/');
                     }
                     buf.append(path);
                  }

                  if (buf.charAt(buf.length() - 1) != '/' && isLeaf() == false)
                  {
                     buf.append('/');
                  }

                  vfsUrlCached = new URL(buf.toString());
               }
               else
               {
                  VirtualFileHandler handler = oldRoot.getChild(path);
                  if (handler == null)
                  {
                     URL oldRootURL = oldRoot.toVfsUrl();
                     if (path != null && path.length() > 0)
                     {
                        String oldRootURLString = oldRootURL.toExternalForm();
                        if (oldRootURLString.endsWith("/") == false && path.startsWith("/") == false)
                           oldRootURLString += "/";
                        vfsUrlCached = new URL(oldRootURLString + path);
                     }
                     else
                     {
                        vfsUrlCached = oldRootURL;
                     }
                     log.warn("No such existing handler, falling back to old root + path: " + vfsUrlCached);
                  }
                  else
                  {
                     vfsUrlCached = handler.toVfsUrl();
                  }
               }

            }
            catch (IOException e)
            {
               throw new MalformedURLException(e.getMessage());
            }
         }
         else
         {
            vfsUrlCached = toInternalVfsUrl();
         }
      }
      return vfsUrlCached;
   }

   /**
    * Get internal representation of vfs url.
    *
    * @return the vfs url
    * @throws MalformedURLException for any error
    * @throws URISyntaxException for any error
    */
   protected URL toInternalVfsUrl() throws MalformedURLException, URISyntaxException
   {
      return vfsUrl;
   }

   public URL getRealURL() throws IOException, URISyntaxException
   {
      return toURL();
   }

   /**
    * Get VFS url.
    *
    * @return vfs url
    */
   protected URL getVfsUrl()
   {
      return vfsUrl;
   }

   /**
    * Set the vfs URL.
    *
    * @param vfsUrl vfs url
    */
   protected void setVfsUrl(URL vfsUrl)
   {
      this.vfsUrl = vfsUrl;
   }

   /**
    * Initialise the path into the path name
    * 
    * @param pathName the path name
    * @return whether it added anything
    */
   private boolean initPath(StringBuilder pathName)
   {
      if (context.getRootPeer() != null)
         if (initPeerPath(pathName))
            return true;

      if (parent != null)
      {
         if (parent instanceof AbstractVirtualFileHandler)
         {
            AbstractVirtualFileHandler handler = (AbstractVirtualFileHandler) parent;
            if (handler.initPath(pathName))
               pathName.append('/');
         }
         else
         {
            pathName.append(parent.getPathName());
         }
         pathName.append(getName());
         return true;
      }
      return false;
   }

   /**
    * Initialise the peer path.
    *
    * @param pathName the path name
    * @return whether it added anything
    */
   private boolean initPeerPath(StringBuilder pathName)
   {
      VirtualFileHandler grandParent = null;

      if (parent != null)
      {
         try
         {
            grandParent = parent.getParent();
         }
         catch(IOException ex)
         {
            // if we throw exception here we'll most likely cause an infinite recursion
            log.warn("AbstractVirtualFileHandler.initPath failed: ctx: " + context
                    + ", parent: " + parent + " name: " + name, ex);
         }
      }

      if (grandParent == null)
      {
         VirtualFileHandler peer = context.getRootPeer();

         // bypass parent and delegate straight to peer

         if (peer instanceof AbstractVirtualFileHandler)
         {
            AbstractVirtualFileHandler handler = (AbstractVirtualFileHandler) peer;
            if (handler.initPath(pathName) && parent != null)
               pathName.append('/');
         }
         else
         {
            pathName.append(peer.getPathName());
         }

         if (parent != null)
         {
            // if it's a root node we skip adding '/' and a name
            pathName.append(getName());
         }

         return true;
      }

      return false;
   }

   public VirtualFile getVirtualFile()
   {
      checkClosed();
      increment();
      return new VirtualFile(this);
   }

   /**
    * Get this handler's parent.
    * If this handler represents a root of the context mounted within another context
    * a parent from the outer context will be returned.
    *
    * @return parent handler
    * @throws IOException for any error
    */
   public VirtualFileHandler getParent() throws IOException
   {
      checkClosed();
      if (parent == null)
      {
         if (context instanceof AbstractVFSContext)
         {
            AbstractVFSContext avfc = (AbstractVFSContext) context;
            VirtualFileHandler peer = avfc.getRootPeer();
            if (peer != null)
               return peer.getParent();
         }
      }
      return parent;
   }

   /**
    * Get this handler's most outer context (contexts can be mounted one within other).
    *
    * @return context
    */
   public VFSContext getVFSContext()
   {
      checkClosed();
      if (context instanceof AbstractVFSContext)
      {
         AbstractVFSContext avfs = (AbstractVFSContext) context;
         VirtualFileHandler peer = avfs.getRootPeer();
         if (peer != null)
            return peer.getVFSContext();
      }
      return context;
   }

   /**
    * Get this handler's local context
    *
    * @return context
    */
   public VFSContext getLocalVFSContext()
   {
      return context;
   }

   /**
    * Increment the reference count
    * 
    * @return the resulting count
    */
   protected int increment()
   {
      return references.incrementAndGet();
   }

   /**
    * Decrement the reference count
    * 
    * @return the resulting count
    */
   protected int decrement()
   {
      return references.decrementAndGet();
   }

   /**
    * Check whether we are closed
    * 
    * @throws IllegalStateException when closed
    */
   protected void checkClosed() throws IllegalStateException 
   {
      if (references.get() < 0)
         throw new IllegalStateException("Closed " + toStringLocal());
   }

   /**
    * Get the references count.
    *
    * @return the ref count
    */
   protected int getReferences()
   {
      return references.get();
   }

   public void cleanup()
   {
   }
   
   /**
    * Is the handler temporary.
    *
    * @return true if temporary, false otherwise
    */
   protected boolean isTemporary()
   {
      Options options = getVFSContext().getOptions();
      return options.getBooleanOption(VFSUtils.IS_TEMP_FILE);
   }

   public void close()
   {
      try
      {
         if (getReferences() == 1)
            doClose();
      }
      finally
      {
         references.decrementAndGet();   
      }
   }

   /**
    * The real close
    */
   protected void doClose()
   {
      // nothing
   }

   /**
    * Delete the file represented by this handler.
    *
    * File deletion is comprised of two parts:
    *
    * <ol>
    * <li>physical file deletion - performed by this method or its override</li>
    * <li>removal of any child references from the parent - performed by {@link #removeChild(String)} of the parent</li>
    * </ol>
    *
    * This method doesn't do any physical file removal because it has no concept of underlying physical file.
    * An implementation that does physical file removal should override this method and call super.delete() at the end.
    *
    * @param gracePeriod max time to wait for any locks
    * @return true if file was deleted, false otherwise
    * @throws IOException if an error occurs
    */
   public boolean delete(int gracePeriod) throws IOException
   {
      VirtualFileHandler parent = getParent();
      return parent != null && parent.removeChild(getName());
   }

   /**
    * Structured implementation of get child
    *
    * @param path the path
    * @return the handler or <code>null</code> if it doesn't exist
    * @throws IOException for any error accessing the virtual file system
    * @throws IllegalArgumentException for a null name
    */
   public VirtualFileHandler structuredFindChild(String path) throws IOException
   {
      checkClosed();

      // Parse the path
      List<String> tokens = PathTokenizer.getTokens(path);
      if (tokens == null || tokens.size() == 0)
         return this;

      // Go through each context starting from ours
      // check the parents are not leaves.
      VirtualFileHandler current = this;
      for (int i = 0; i < tokens.size(); ++i)
      {
         if (current == null)
            return null;

         String token = tokens.get(i);
         if (PathTokenizer.isCurrentToken(token))
            continue;

         if (PathTokenizer.isReverseToken(token))
         {
            VirtualFileHandler parent = current.getParent();
            if (parent == null) // TODO - still IOE or null?
               throw new IOException("Using reverse path on top file handler: " + current + ", " + path);
            else
               current = parent;

            continue;
         }

         if (current.isLeaf())
         {
            return null;
         }
         else if (current instanceof StructuredVirtualFileHandler)
         {
            StructuredVirtualFileHandler structured = (StructuredVirtualFileHandler)current;
            current = structured.createChildHandler(token);
         }
         else
         {
            String remainingPath = PathTokenizer.getRemainingPath(tokens, i);
            return current.getChild(remainingPath);
         }
      }

      // The last one is the result
      return current;
   }

   /**
    * Simple implementation of findChild
    * 
    * @param path the path
    * @return the handler
    * @throws IOException for any error accessing the virtual file system
    * @throws IllegalArgumentException for a null name
    */
   public VirtualFileHandler simpleFindChild(String path) throws IOException
   {
      if (path == null)
         throw new IllegalArgumentException("Null path");

      if (path.length() == 0)
         return this;

      // check for reverse .. path
      String appliedPath = PathTokenizer.applySpecialPaths(path);
      List<VirtualFileHandler> children = getChildren(false);
      for (VirtualFileHandler child : children)
      {
         if (child.getName().equals(appliedPath))
            return child;
      }
      return null;
   }

   public void replaceChild(VirtualFileHandler original, VirtualFileHandler replacement)
   {
      internalReplaceChild(original, replacement);
      if (replacement instanceof AbstractVirtualFileHandler)
      {
         AbstractVirtualFileHandler avfh = (AbstractVirtualFileHandler)replacement;
         avfh.parent = this;
      }
   }

   /**
    * Replace original child with unpacked replacement.
    *
    * @param original the original
    * @param replacement the replacement
    */
   protected void internalReplaceChild(VirtualFileHandler original, VirtualFileHandler replacement)
   {
      throw new UnsupportedOperationException("Replacement is unsupported: " + toString());
   }

   @Override
   public String toString()
   {
      StringBuilder buffer = new StringBuilder();
      buffer.append(getClass().getSimpleName());
      buffer.append('@');
      buffer.append(System.identityHashCode(this));
      buffer.append("[path=").append(getPathName());
      buffer.append(" context=").append(getVFSContext().getRootURI());
      buffer.append(" real=").append(safeToURLString());
      buffer.append(']');
      return buffer.toString();
   }

   public String toStringLocal()
   {
      StringBuilder buffer = new StringBuilder();
      buffer.append(getClass().getSimpleName());
      buffer.append('@');
      buffer.append(System.identityHashCode(this));
      buffer.append("[path=").append(getLocalPathName());
      buffer.append(" context=").append(context.getRootURI());
      //buffer.append(" real=").append(safeToURLString());
      buffer.append(']');
      return buffer.toString();
   }

   @Override
   public int hashCode()
   {
      return getPathName().hashCode();
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null || obj instanceof VirtualFileHandler == false)
         return false;
      VirtualFileHandler other = (VirtualFileHandler) obj;
      if (getVFSContext().equals(other.getVFSContext()) == false)
         return false;
      if (getPathName().equals(other.getPathName()) == false)
         return false;
      return true;
   }

   /*
   @Override
   protected void finalize() throws Throwable
   {
      close();
   }
   */
   
   /**
    * Safely get a url version of the string
    * 
    * @return the string or unknown if there is an error
    */
   private String safeToURLString()
   {
      try
      {
         return toURI().toString();
      }
      catch (URISyntaxException ignored)
      {
         return "<unknown>";
      }
   }

   private void writeObject(ObjectOutputStream out)
      throws IOException
   {
      PutField fields = out.putFields();
      fields.put("rootURI", getLocalVFSContext().getRootURI());
      fields.put("parent", parent);
      fields.put("name", name);
      fields.put("vfsUrl", vfsUrl);
      out.writeFields();
   }

   private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException
   {
      // Read in the serialPersistentFields
      GetField fields = in.readFields();
      URI rootURI = (URI) fields.get("rootURI", null);
      this.parent = (VirtualFileHandler) fields.get("parent", null);
      this.name = (String) fields.get("name", null);
      VFSContextFactory factory = VFSContextFactoryLocator.getFactory(rootURI);
      this.context = factory.getVFS(rootURI);
      this.references = new AtomicInteger(0);
      this.vfsUrl = (URL)fields.get("vfsUrl", null);
   }
}
