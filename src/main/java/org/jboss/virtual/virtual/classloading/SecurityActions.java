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
package org.jboss.virtual.classloading;

import java.security.AccessController;
import java.security.Policy;
import java.security.PrivilegedAction;

import org.jboss.virtual.VFS;

/**
 * Package priviledged actions
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 55183 $
 */
class SecurityActions
{
   static PrivilegedAction<Policy> getPolicyAction = new PrivilegedAction<Policy>() {
      public Policy run()
      {
         return Policy.getPolicy();
      }
   };

   interface ClassLoaderActions
   {
      ClassLoaderActions PRIVILEGED = new ClassLoaderActions() {
         public VFSClassLoader newClassLoader(final String[] paths,
               final VFS vfs, final ClassLoader parent)
         {
            PrivilegedAction<VFSClassLoader> action = new PrivilegedAction<VFSClassLoader>() {
               public VFSClassLoader run()
               {
                  ClassLoader theParent = parent;
                  if (parent == null)
                     theParent = Thread.currentThread().getContextClassLoader();
                  return new VFSClassLoader(paths, vfs, theParent);
               }
            };
            return AccessController.doPrivileged(action);
         }

         public Policy getPolicy()
         {
            return AccessController.doPrivileged(getPolicyAction);
         }
      };

      ClassLoaderActions NON_PRIVILEGED = new ClassLoaderActions() {
         public VFSClassLoader newClassLoader(final String[] paths,
               final VFS vfs, final ClassLoader parent)
         {
            ClassLoader theParent = parent;
            if (parent == null)
               theParent = Thread.currentThread().getContextClassLoader();
            return new VFSClassLoader(paths, vfs, theParent);
         }

         public Policy getPolicy()
         {
            return Policy.getPolicy();
         }
      };

      VFSClassLoader newClassLoader(final String[] paths, final VFS vfs, ClassLoader parent);

      Policy getPolicy();
   }

   static VFSClassLoader newClassLoader(final String[] paths,
         final VFS vfs)
   {
      if (System.getSecurityManager() == null)
      {
         return ClassLoaderActions.NON_PRIVILEGED.newClassLoader(paths, vfs, null);
      }
      else
      {
         return ClassLoaderActions.PRIVILEGED.newClassLoader(paths, vfs, null);
      }
   }

   static VFSClassLoader newClassLoader(final String[] paths,
         final VFS vfs, ClassLoader parent)
   {
      if (System.getSecurityManager() == null)
      {
         return ClassLoaderActions.NON_PRIVILEGED.newClassLoader(paths, vfs,
               parent);
      }
      else
      {
         return ClassLoaderActions.PRIVILEGED
               .newClassLoader(paths, vfs, parent);
      }
   }

   static Policy getPolicy()
   {
      if (System.getSecurityManager() == null)
      {
         return ClassLoaderActions.NON_PRIVILEGED.getPolicy();
      }
      else
      {
         return ClassLoaderActions.PRIVILEGED.getPolicy();
      }
   }

}
