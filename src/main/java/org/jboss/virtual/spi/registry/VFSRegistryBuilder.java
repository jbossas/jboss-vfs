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
package org.jboss.virtual.spi.registry;

import java.security.AccessController;

import org.jboss.util.builder.AbstractBuilder;

/**
 * VFS registry.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public final class VFSRegistryBuilder
{
   /** The default vfs registry */
   private static String DEFAULT_FACTORY = "org.jboss.virtual.plugins.registry.DefaultVFSRegistry";

   /** The singleton */
   private static VFSRegistry singleton;

   /**
    * Get the instance
    *
    * @return the instance
    */
   static synchronized VFSRegistry getInstance()
   {
      if (singleton == null)
      {
         AbstractBuilder<VFSRegistry> builder = new AbstractBuilder<VFSRegistry>(VFSRegistry.class, DEFAULT_FACTORY);
         singleton = AccessController.doPrivileged(builder);
      }
      return singleton;
   }
}