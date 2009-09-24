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
package org.jboss.virtual.spi;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;

/**
 * This knows how to create a virtual file handler from a file or uri.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public interface FileHandlerPlugin
{
   /**
    * Get relative order.
    *
    * @return the order number
    */
   int getRelativeOrder();

   /**
    * Create new virtual file handler from a file or uri.
    *
    * @param context the current context
    * @param parent the parent virtual file handler
    * @param file the current file
    * @param uri the file's uri
    * @return new virtual file handler or null if cannot create one
    * @throws IOException for any error
    */
   VirtualFileHandler createHandler(VFSContext context, VirtualFileHandler parent, File file, URI uri) throws IOException;

   /**
    * The relative order comparator.
    */
   static Comparator<FileHandlerPlugin> COMPARATOR = new Comparator<FileHandlerPlugin>()
   {
      public int compare(FileHandlerPlugin fhp1, FileHandlerPlugin fhp2)
      {
         return fhp1.getRelativeOrder() - fhp2.getRelativeOrder();
      }
   };
}
