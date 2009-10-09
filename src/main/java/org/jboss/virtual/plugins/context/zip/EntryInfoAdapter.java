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
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

/**
 * EntryInfo wrapper.
 * It knows how to read certificates.
 *
 * Note: this is basically a hack,
 * so we don't have to change ZipWrapper API.
 * It should be removed with next minor version.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
class EntryInfoAdapter extends ZipEntry
{
   private ZipEntryContext.EntryInfo ei;

   EntryInfoAdapter(ZipEntryContext.EntryInfo ei)
   {
      super(ei.entry);
      this.ei = ei;
   }

   /**
    * Update entry.
    *
    * @param entry the new entry
    */
   void updateEntry(ZipEntry entry)
   {
      ei.entry = entry;      
   }

   /**
    * Read certificates.
    */
   void readCertificates()
   {
      if (ei.certificates == null)
      {
         ZipEntry entry = ei.entry;

         Certificate[] certs = null;
         if (entry instanceof JarEntry)
            certs = JarEntry.class.cast(entry).getCertificates();

         ei.certificates = (certs != null) ? certs : ZipEntryContext.EntryInfo.MARKER;
      }
   }

   /**
    * Do we require an update.
    *
    * @return true if we need to update entry
    */
   boolean requiresUpdate()
   {
      return ei.certificates == null;
   }
}