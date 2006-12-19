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

import java.io.Serializable;
import java.net.URI;

/**
 * A class representing the information for a VFS link.
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision:$
 */
public class LinkInfo implements Serializable
{
   private static final long serialVersionUID = 1L;
   /** Optional name of the link which defines its name to the parent */
   private String name;
   /** Required URI for the link target */
   private final URI linkTarget;

   /**
    * Create a LinkInfo
    * 
    * @param name - the simple name of the target link. If null the linkTarget
    *    name will be used.
    * @param linkTarget - the URI of the target of the link.
    */
   public LinkInfo(String name, URI linkTarget)
   {
      this.name = name;
      this.linkTarget = linkTarget;
   }

   public URI getLinkTarget()
   {
      return linkTarget;
   }

   public String getName()
   {
      return name;
   }

}
