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
package org.jboss.test.virtual.support;

import java.io.InputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * Track down callers.
 * Checking if they close properly.
 *
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class InputStreamCallerChecker extends InputStream
{
   private static final Collection<String> excluded = Arrays.asList(
         "java.lang.Thread",
         "org.jboss.virtual.InputStreamDelegate",
         "org.jboss.virtual.VirtualFile",
         "org.jboss.virtual.protocol.file.VirtualFileURLConnection",
         "java.net.URL",
         "org.jboss.deployers.vfs.spi.deployer.SecurityActions",
         "org.jboss.deployers.vfs.spi.deployer.SecurityActions$FileActions$1",
         "org.jboss.deployers.vfs.spi.deployer.SecurityActions$FileActions$2",
         "org.jboss.deployers.vfs.spi.deployer.JBossXBDeployerHelper",
         "org.jboss.deployers.vfs.spi.deployer.AbstractVFSParsingDeployer",
         "org.jboss.classloading.spi.visitor.ResourceContext"
   );
   private static final Set<String> callers = new HashSet<String>();
   private InputStream delegate;

   public InputStreamCallerChecker(InputStream delegate)
   {
      if (delegate == null)
         throw new IllegalArgumentException("Null delegate");
      this.delegate = delegate;

      Thread current = Thread.currentThread();
      StackTraceElement[] stack = current.getStackTrace();
      int i = 0;
      StackTraceElement caller = stack[i];
      while(excluded.contains(caller.getClassName()))
      {
         caller = stack[++i];
      }
      callers.add(caller.getClassName());
      System.out.println("Callers = " + callers);
   }

   public int read() throws IOException
   {
      return delegate.read();
   }

   public int read(byte b[]) throws IOException
   {
      return delegate.read(b);
   }

   public int read(byte b[], int off, int len) throws IOException
   {
      return delegate.read(b, off, len);
   }

   public long skip(long n) throws IOException
   {
      return delegate.skip(n);
   }

   public int available() throws IOException
   {
      return delegate.available();
   }

   public void close() throws IOException
   {
      Thread current = Thread.currentThread();
      StackTraceElement[] stack = current.getStackTrace();
      // should be at index 3, 4th in line
      StackTraceElement caller = stack[3];
      String className = caller.getClassName();
      System.out.println("closer = " + className);
      callers.remove(className);
      delegate.close();
   }

   public synchronized void mark(int readlimit)
   {
      delegate.mark(readlimit);
   }

   public synchronized void reset() throws IOException
   {
      delegate.reset();
   }

   public boolean markSupported()
   {
      return delegate.markSupported();
   }
}