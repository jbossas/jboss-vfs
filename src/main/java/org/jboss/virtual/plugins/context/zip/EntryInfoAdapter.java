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
    * Get original entry.
    *
    * @return the original entry
    */
   ZipEntry getOriginalEntry()
   {
      return ei.entry;
   }

   /**
    * Read certificates.
    */
   void readCertificates()
   {
      ZipEntry entry = ei.entry;
      if (ei.certificates == null && entry instanceof JarEntry)
      {
         Certificate[] certs = JarEntry.class.cast(entry).getCertificates();
         ei.certificates = (certs != null) ? certs : ZipEntryContext.EntryInfo.MARKER;
      }
   }
}