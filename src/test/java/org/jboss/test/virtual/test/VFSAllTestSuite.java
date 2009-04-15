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
package org.jboss.test.virtual.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.jboss.virtual.VFS;

/**
 * VFS All Test Suite.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.com">Ales Justin</a>
 * @version $Revision: 46146 $
 */
public class VFSAllTestSuite extends TestSuite
{
   public static void main(String[] args)
   {
      TestRunner.run(suite());
   }

   public static Test suite()
   {
      VFS.init();
      TestSuite suite = new TestSuite("VFS Tests default");

      // vfs / spi
      suite.addTest(VFSUnitTestCase.suite());
      suite.addTest(VirtualFileUnitTestCase.suite());
      // url
      suite.addTest(URLResolutionUnitTestCase.suite());
      suite.addTest(URLExistsUnitTestCase.suite());
      suite.addTest(URLConnectionUnitTestCase.suite());
      // files
      suite.addTest(FileVFSUnitTestCase.suite());
      suite.addTest(CopyFileVFSUnitTestCase.suite());
      suite.addTest(FileVFSContextUnitTestCase.suite());
      suite.addTest(FileVirtualFileHandlerUnitTestCase.suite());
      // jars
      suite.addTest(JarFileURLTestCase.suite());
      suite.addTest(JARCacheUnitTestCase.suite());
      suite.addTest(CopyJARCacheUnitTestCase.suite());
      suite.addTest(JARVFSContextUnitTestCase.suite());
      suite.addTest(JARVirtualFileHandlerUnitTestCase.suite());
      suite.addTest(JARSerializationUnitTestCase.suite());
      suite.addTest(CopyJARSerializationUnitTestCase.suite());
      suite.addTest(JAREntryTestCase.suite());
      suite.addTest(CopyJAREntryTestCase.suite());
      suite.addTest(ZipEntryHandlerUnitTestCase.suite());
      suite.addTest(ZipEntryVFSContextUnitTestCase.suite());
      // contexts
      suite.addTest(AssembledContextTestCase.suite());
      suite.addTest(MemoryTestCase.suite());
      suite.addTest(SundryVFSUnitTestCase.suite());
      // options / policy / path
      suite.addTest(PathQueryTestCase.suite());
      suite.addTest(PathTokensTestCase.suite());
      // unpack
      suite.addTest(UnpackTestCase.suite());
      suite.addTest(ExplodeTestCase.suite());
      suite.addTest(TempTestCase.suite());
      suite.addTest(UnjarTestCase.suite());
      // visitor
      suite.addTest(VisitorUnitTestCase.suite());
      // utils
      suite.addTest(VFSUtilTestCase.suite());
      // custom
      suite.addTest(CustomTestCase.suite());
      suite.addTest(VFSResourceTestCase.suite());
      // cache
      suite.addTest(LRUCacheTestCase.suite());
      suite.addTest(TimedCacheTestCase.suite());
      suite.addTest(IterableTimedCacheTestCase.suite());
      suite.addTest(SoftRefCacheTestCase.suite());
      suite.addTest(WeakRefCacheTestCase.suite());
      suite.addTest(CombinedVFSCacheTestCase.suite());
      // exception handler
      suite.addTest(ExceptionHandlerTestCase.suite());
      // operations
      suite.addTest(TempCleanupUnitTestCase.suite());
      suite.addTest(ExplodeCleanupUnitTestCase.suite());
      suite.addTest(UnjarCleanupUnitTestCase.suite());

      return suite;
   }
}
