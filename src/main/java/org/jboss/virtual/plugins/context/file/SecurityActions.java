/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.virtual.plugins.context.file;

import java.io.File;

/**
 * Package priviledged actions.
 *  
 * @author Scott.Stark@jboss.org
 * @version $Revision$
 */
class SecurityActions
{
   /**
    * Actions for File access 
   interface FileActions 
   { 
      FileActions PRIVILEGED = new FileActions() 
      { 
         private final PrivilegedExceptionAction exAction = new PrivilegedExceptionAction() 
         { 
            public Object run() throws Exception 
            { 
               return (Subject) PolicyContext.getContext(SUBJECT_CONTEXT_KEY); 
            } 
         }; 
         public Subject getContextSubject() 
         throws PolicyContextException 
         { 
            try 
            { 
               return (Subject) AccessController.doPrivileged(exAction); 
            } 
            catch(PrivilegedActionException e) 
            { 
               Exception ex = e.getException(); 
               if( ex instanceof PolicyContextException ) 
                  throw (PolicyContextException) ex; 
               else 
                  throw new UndeclaredThrowableException(ex); 
            } 
         } 
      }; 

      FileActions NON_PRIVILEGED = new FileActions() 
      { 
      };

      public boolean isFile(File f);
      public boolean isHidden(File f);
   } 
    */

}
