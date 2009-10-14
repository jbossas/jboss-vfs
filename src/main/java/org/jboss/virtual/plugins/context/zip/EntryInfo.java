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

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * Basic zip entry info impl.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="marko.strukelj@parsek.net">Marko Strukelj</a>
 */
class EntryInfo implements ZipEntryContextInfo
{
   /** a marker */
   static final Certificate[] MARKER = new Certificate[]{};

   /** a handler */
   private AbstractVirtualFileHandler handler;

   /** a <tt>ZipEntry</tt> */
   private ZipEntry entry;

   /** the certificates */
   private Certificate[] certificates;

   /** a list of children */
   private Map<String, AbstractVirtualFileHandler> children;

   /**
    * EntryInfo constructor
    *
    * @param handler a handler
    * @param entry an entry
    */
   EntryInfo(AbstractVirtualFileHandler handler, ZipEntry entry)
   {
      this.handler = handler;
      this.entry = entry;
   }

   public void readCertificates()
   {
      if (certificates == null)
      {
         Certificate[] certs = null;
         if (entry instanceof JarEntry)
            certs = JarEntry.class.cast(entry).getCertificates();

         certificates = (certs != null) ? certs : MARKER;
      }
   }

   /**
    * Get certificates.
    *
    * @return the certificates
    */
   public Certificate[] getCertificates()
   {
      return (certificates != MARKER) ? certificates : null;
   }

   public synchronized List<VirtualFileHandler> getChildren()
   {
      if (children == null)
         return Collections.emptyList();

      return new ArrayList<VirtualFileHandler>(children.values());
   }

   public synchronized void replaceChild(AbstractVirtualFileHandler original, AbstractVirtualFileHandler replacement)
   {
      if (children != null)
      {
         final String name = original.getName();
         if (children.containsKey(name)) {
            children.put(name, replacement);
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
         children = new LinkedHashMap<String, AbstractVirtualFileHandler>();
      }
      children.put(child.getName(), child);
   }

   public String getName()
   {
      return (entry != null) ? entry.getName() : handler.getName();
   }

   public boolean isDirectory()
   {
      // if entry is null, we return false
      return (entry != null) && entry.isDirectory();
   }

   public boolean requiresUpdate()
   {
      return (certificates == null);
   }

   public ZipEntry getEntry()
   {
      return entry;
   }

   public void setEntry(ZipEntry entry)
   {
      this.entry = entry;
   }

   public long getTime()
   {
      return (entry != null) ? entry.getTime() : 0;
   }

   public long getSize()
   {
      return (entry != null) ? entry.getSize() : 0;
   }

   public AbstractVirtualFileHandler getHandler()
   {
      return handler;
   }

   public void setHandler(AbstractVirtualFileHandler handler)
   {
      this.handler = handler;
   }
}