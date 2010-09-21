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
package org.jboss.virtual.spi;

/**
 * VFS exception handler.
 *
 * Custom exception handling methods shoud be added here
 * and used at the pointcut where exception is thrown.
 *
 * Its abstract super class should implement them all
 * hence back compatibility is assured by noop.
 *
 * Real handlers should just override some methods that
 * they know how to handle. 
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public interface ExceptionHandler
{
   /**
    * Handle zip entries initializaion exception.
    *
    * @param e the cause exception
    * @param name the file name
    * @throws RuntimeException if not handled
    */
   void handleZipEntriesInitException(Exception e, String name);
}
