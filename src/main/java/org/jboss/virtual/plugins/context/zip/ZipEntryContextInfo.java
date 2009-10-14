/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
import java.util.List;

import org.jboss.virtual.plugins.context.AbstractVirtualFileHandler;
import org.jboss.virtual.spi.VirtualFileHandler;

/**
 * ZipEntryContextInfo.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
interface ZipEntryContextInfo extends ZipEntryInfo
{
   /**
    * Get time.
    *
    * @return get time
    */
   long getTime();

   /**
    * Get size.
    *
    * @return the size
    */
   long getSize();

   /**
    * Get certificates.
    *
    * @return the certificates.
    */
   Certificate[] getCertificates();

   /**
    * Get handler.
    *
    * @return the handler
    */
   AbstractVirtualFileHandler getHandler();

   /**
    * Set handler.
    *
    * @param handler the handler
    */
   void setHandler(AbstractVirtualFileHandler handler);

   /**
    * Get children.
    *
    * @return the children
    */
   List<VirtualFileHandler> getChildren();

   /**
    * Add child.
    *
    * @param child the child to add
    */
   void add(AbstractVirtualFileHandler child);

   /**
    * Clear children.
    */
   void clearChildren();

   /**
    * Replace child.
    *
    * @param original the original
    * @param newOne the replacement
    */
   void replaceChild(AbstractVirtualFileHandler original, AbstractVirtualFileHandler newOne);
}