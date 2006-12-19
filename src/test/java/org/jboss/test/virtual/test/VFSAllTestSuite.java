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

/**
 * VFS All Test Suite.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
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
      TestSuite suite = new TestSuite("VFS Tests");

      suite.addTest(new TestSuite(URLResolutionUnitTestCase.class));
      suite.addTest(VFSUnitTestCase.suite());
      suite.addTest(VirtualFileUnitTestCase.suite());
      suite.addTest(FileVFSUnitTestCase.suite());
      suite.addTest(SundryVFSUnitTestCase.suite());
      suite.addTest(FileVFSContextUnitTestCase.suite());
      suite.addTest(FileVirtualFileHandlerUnitTestCase.suite());
      suite.addTest(JARVFSContextUnitTestCase.suite());
      suite.addTest(JARVirtualFileHandlerUnitTestCase.suite());
      suite.addTest(new TestSuite(TestClassLoading.class));
      
      return suite;
   }
}
