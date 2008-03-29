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
package org.jboss.test.virtual.test;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.test.BaseTestCase;

/**
 * OS aware test, temp hack.
 *
 * TODO - remove once file delete issues are resolved.
 *
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 */
public abstract class OSAwareVFSTest extends BaseTestCase
{
   protected OSAwareVFSTest(String name)
   {
      super(name);
   }

   /**
    * Are we running Windows.
    *
    * @return true for winz os
    */
   protected boolean isWindowsOS()
   {
      SecurityManager sm = suspendSecurity();
      try
      {
         String osName = System.getProperty("os.name");
         return osName != null && osName.contains("Windows");
      }
      finally
      {
         resumeSecurity(sm);
      }
   }

   /**
    * Suspend security manager.
    *
    * @return current security manager instance
    */
   public static SecurityManager suspendSecurity()
   {
      return AccessController.doPrivileged(new PrivilegedAction<SecurityManager>()
      {
         public SecurityManager run()
         {
            SecurityManager result = System.getSecurityManager();
            System.setSecurityManager(null);
            return result;
         }
      });
   }

   /**
    * Resume / set security manager.
    *
    * @param securityManager security manager to set
    */
   public static void resumeSecurity(SecurityManager securityManager)
   {
      System.setSecurityManager(securityManager);
   }
}