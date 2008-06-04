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
package org.jboss.test.virtual.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.util.UnexpectedThrowable;
import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.spi.VFSContext;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * AbstractMockVirtualFileHandler.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractMockVirtualFileHandler extends AbstractVirtualFileHandler
{
   /** The serialVersionUID */
   private static final long serialVersionUID = -7967261672121081602L;

   /** The URI */
   private URI uri;

   /** The children */
   private List<VirtualFileHandler> children = new CopyOnWriteArrayList<VirtualFileHandler>();

   /** Last modified */
   private long lastModified;
   
   /** Size */
   private long size;
   
   /** Does the file exist */
   private boolean exists = true;

   /** Is a leaf */
   private boolean leaf = true;
   
   /** Is a hidden */
   private boolean hidden;

   /** Is nested */
   private boolean nested;

   /** The stream */
   private byte[] stream;
   
   /** When to throw an IOException */
   private String ioException = "";
   
   /**
    * Create a root mock uri
    * 
    * @param context the vfs context
    * @param parent the parent file
    * @param name the name
    * @return the uri
    */
   public static URI createMockURI(VFSContext context, VirtualFileHandler parent, String name)
   {
      try
      {
         String uri;
         if (parent != null)
            uri = parent.toURI().toString();
         else
            uri = context.getRootURI().toString();
         if (name.length() != 0)
            uri = uri + "/" + name;
         return new URI(uri);
      }
      catch (URISyntaxException e)
      {
         throw new UnexpectedThrowable("Unexpected", e);
      }
   }

   /**
    * Create a new AbstractMockVirtualFileHandler.
    * 
    * @param context the context
    * @param parent the parent
    * @param name the name
    */
   protected AbstractMockVirtualFileHandler(MockVFSContext context, AbstractMockVirtualFileHandler parent, String name)
   {
      super(context, parent, name);
      this.uri = createMockURI(context, parent, name);
      if (parent != null)
         parent.addChild(this);
   }

   /**
    * Set the ioException.
    * 
    * @param ioException the ioException.
    */
   public void setIOException(String ioException)
   {
      this.ioException = ioException;
   }

   /**
    * Check whether we should throw an IOException
    * 
    * @param when when to throw
    * @throws IOException when requested
    */
   public void throwIOException(String when) throws IOException
   {
      if (ioException.equals(when))
         throw new IOException("Throwing IOException from " + when);
   }

   public List<VirtualFileHandler> getChildren(boolean ignoreErrors) throws IOException
   {
      checkClosed();
      if (ignoreErrors == false)
         throwIOException("getChildren");
      return Collections.unmodifiableList(children);
   }

   public void addChild(VirtualFileHandler child)
   {
      checkClosed();
      if (child == null)
         throw new IllegalArgumentException("Null child");
      if (children.contains(child) == false)
         children.add(child);
      leaf = false;
   }

   public long getLastModified() throws IOException
   {
      checkClosed();
      throwIOException("getLastModified");
      return lastModified;
   }

   /**
    * Set the lastModified.
    * 
    * @param lastModified the lastModified.
    */
   public void setLastModified(long lastModified)
   {
      this.lastModified = lastModified;
   }

   public long getSize() throws IOException
   {
      checkClosed();
      throwIOException("getSize");
      return size;
   }

   /**
    * Set the size.
    * 
    * @param size the size.
    */
   public void setSize(long size)
   {
      this.size = size;
   }

   public boolean exists()
   {
      return exists;
   }

   public void setExists(boolean exists)
   {
      this.exists = exists;
   }

   public boolean isLeaf() throws IOException
   {
      checkClosed();
      throwIOException("isLeaf");
      return leaf;
   }

   /**
    * Set leaf.
    * 
    * @param leaf whether this is a leaf.
    */
   public void setLeaf(boolean leaf)
   {
      this.leaf = leaf;
   }

   public boolean isHidden() throws IOException
   {
      checkClosed();
      throwIOException("isHidden");
      return hidden;
   }

   /**
    * Set the hidden.
    * 
    * @param hidden the hidden.
    */
   public void setHidden(boolean hidden)
   {
      this.hidden = hidden;
   }

   public boolean isNested() throws IOException
   {
      return nested;
   }

   public void setNested(boolean nested)
   {
      this.nested = nested;
   }

   /**
    * Set the stream.
    * 
    * @param stream the stream.
    */
   public void setStream(byte[] stream)
   {
      this.stream = stream;
   }

   public InputStream openStream() throws IOException
   {
      checkClosed();
      throwIOException("openStream");
      return new MockInputStream(stream);
   }

   public URI toURI()
   {
      return uri;
   }


   public URL toVfsUrl() throws MalformedURLException, URISyntaxException
   {
      return toURL();
   }

   @Override
   public URL toURL() throws MalformedURLException, URISyntaxException
   {
      return MockVFSContext.createMockURL(uri);
   }

   protected void internalReplaceChild(VirtualFileHandler original, VirtualFileHandler replacement)
   {
      children.remove(original);
      children.add(replacement);
   }

   @Override
   public VirtualFileHandler getParent() throws IOException
   {
      throwIOException("getParent");
      return super.getParent();
   }

   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      in.defaultReadObject();
   }
}
