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
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream.PutField;
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

/**
 * AbstractVirtualFileHandler.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public abstract class AbstractVirtualFileHandler implements VirtualFileHandler
{
   /** The log */
   protected static final Logger log = Logger.getLogger(AbstractVirtualFileHandler.class);
   /** serialVersionUID */
   private static final long serialVersionUID = 1L;
   /** The class serial fields */
   private static final ObjectStreamField[] serialPersistentFields = {
      new ObjectStreamField("rootURI", URI.class),
      new ObjectStreamField("parent", VirtualFileHandler.class),
      new ObjectStreamField("name", String.class),
      new ObjectStreamField("vfsUrl", URL.class)
   };

   /** The VFS context
    * @serialField rootURI URI the VFS context rootURI
    */
   private VFSContext context;
   
   /** The parent
    * @serialField parent VirtualFileHandler the virtual file parent
    */
   private VirtualFileHandler parent;

   /** The name
    * @serialField name String the virtual file name
    */
   private String name;

   /** The vfsPath */
   private transient String vfsPath;

   /** The vfs URL */
   protected URL vfsUrl;

   /** The reference count */
   private transient AtomicInteger references = new AtomicInteger(0);

   /** The cached last modified */
   protected transient long cachedLastModified;

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
    * @param path
    */
   public void setPathName(String path)
   {
      this.vfsPath = path;
   }



   public URL toURL() throws MalformedURLException, URISyntaxException
   {
      return toURI().toURL();
   }


   public URL toVfsUrl() throws MalformedURLException, URISyntaxException
   {
      return vfsUrl;
   }

   /**
    * Initialise the path into the path name
    * 
    * @param pathName the path name
    * @return whether it added anything
    */
   private boolean initPath(StringBuilder pathName)
   {
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
   
   public VirtualFile getVirtualFile()
   {
      checkClosed();
      increment();
      return new VirtualFile(this);
   }
   
   public VirtualFileHandler getParent() throws IOException
   {
      checkClosed();
      return parent;
   }
   
   public VFSContext getVFSContext()
   {
      checkClosed();
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
         throw new IllegalStateException("Closed " + this);
   }
   
   public void close() 
   {
      if (decrement() == 0)
         doClose();
   }

   /**
    * The real close
    */
   protected void doClose()
   {
      // nothing
   }

   /**
    * Structured implementation of find child
    * 
    * @param path the path
    * @return the handler
    * @throws IOException for any error accessing the virtual file system
    * @throws IllegalArgumentException for a null name
    */
   public VirtualFileHandler structuredFindChild(String path) throws IOException
   {
      checkClosed();

      // Parse the path
      String[] tokens = PathTokenizer.getTokens(path);
      if (tokens == null || tokens.length == 0)
         return this;

      // Go through each context starting from ours 
      // check the parents are not leaves.
      VirtualFileHandler current = this;
      for (int i = 0; i < tokens.length; ++i)
      {
         if (current.isLeaf())
            throw new IOException("File cannot have children: " + current);

         if (PathTokenizer.isReverseToken(tokens[i]))
         {
            VirtualFileHandler parent = current.getParent();
            if (parent == null)
               throw new IOException("Using reverse path on top file handler: " + current + ", " + path);
            else
               current = parent;
         }
         else if (current instanceof StructuredVirtualFileHandler)
         {
            StructuredVirtualFileHandler structured = (StructuredVirtualFileHandler) current;
            current = structured.createChildHandler(tokens[i]);
         }
         else
         {
            String remainingPath = PathTokenizer.getRemainingPath(tokens, i);
            return current.findChild(remainingPath);
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
      String appliedPath = PathTokenizer.applyReversePaths(path);
      List<VirtualFileHandler> children = getChildren(false);
      for (VirtualFileHandler child : children)
      {
         if (child.getName().equals(appliedPath))
            return child;
      }
      throw new IOException("Child not found " + path + " for " + this);
   }

   @Override
   public String toString()
   {
      StringBuilder buffer = new StringBuilder();
      buffer.append(getClass().getSimpleName());
      buffer.append('@');
      buffer.append(System.identityHashCode(this));
      buffer.append("[path=").append(getPathName());
      buffer.append(" context=").append(context.getRootURI());
      buffer.append(" real=").append(safeToURLString());
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
      fields.put("rootURI", this.getVFSContext().getRootURI());
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
