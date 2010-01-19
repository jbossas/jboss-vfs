/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, JBoss Inc., and individual contributors as indicated
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
package org.jboss.vfs.util.automount;

/**
 * Custom configurations to use when mounting archives through the {@link Automounter}.
 *   
 * @author <a href="jbailey@redhat.com">John Bailey</a>
 */
public enum MountOption {
   
   EXPANDED {
      void apply(MountConfig config)
      {
         config.setMountExpanded(true);
      }
   },
   COPY {
      void apply(MountConfig config)
      {
         config.setCopyTarget(true);
      }
   };

   /**
    * Each option must apply its custom settings to teh {@link MountConfig}.
    * 
    * @param config MountConfig to apply settings to
    */
   abstract void apply(MountConfig config);
}
