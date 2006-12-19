/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.virtual.spi;

import java.net.URI;
import java.net.URL;
import java.io.IOException;

/**
 * The entry point to obtaining a VFSContext for a given URL/URI root mount point
 *
 * @author Scott.Stark@jboss.org
 * @author adrian@jboss.org
 * @version $Revision: 44217 $
 */
public interface VFSContextFactory
{
   /**
    * Get the URL protocols/URI schemes this factory supports
    * 
    * @return list of supported protocols.
    */
   String[] getProtocols();

   /**
    * Obtain a vfs context for the given root url.
    * 
    * @param rootURL - the URL for the root of the virtual context
    * @return the vfs context
    * @throws IOException - thrown if the root cannot be opened/accessed
    */
   VFSContext getVFS(URL rootURL) throws IOException;
   /**
    * Obtain a vfs context for the given root uri.
    * 
    * @param rootURI - the URI for the root of the virtual context
    * @return the vfs context
    * @throws IOException - thrown if the root cannot be opened/accessed
    */
   VFSContext getVFS(URI rootURI) throws IOException;
}
