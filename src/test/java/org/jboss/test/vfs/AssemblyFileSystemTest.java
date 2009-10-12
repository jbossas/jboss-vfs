package org.jboss.test.vfs;

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileAssembly;
import org.junit.Test;

public class AssemblyFileSystemTest extends AbstractVFSTest {

   public AssemblyFileSystemTest(String name) {
      super(name);
   }

   @Test
   public void testBuildAssembly() throws Exception {

      VirtualFileAssembly earAssembly = new VirtualFileAssembly();
      VirtualFile earAssemblyLocation = VFS.getChild("assembly.ear");
      Closeable earAssemblyHandle = VFS.mountAssembly(earAssembly, earAssemblyLocation);

      VirtualFileAssembly warAssembly = new VirtualFileAssembly();
      VirtualFile warAssemblyLocation = earAssemblyLocation.getChild("assembly.war");
      Closeable warAssemblyHandle = VFS.mountAssembly(warAssembly, warAssemblyLocation);

      try {
         URL rootURL = getResource("/vfs/test");
         VirtualFile testDir = VFS.getChild(rootURL.getPath());

         earAssembly.add("assembly.war", warAssemblyLocation);

         URL nestedURL = getResource("/vfs/test/nested");
         warAssembly.add("WEB-INF/lib", new File(nestedURL.toURI()));

         URL jar1URL = getResource("/vfs/test/jar1.jar");
         warAssembly.addZip("WEB-INF/lib/jar1.jar", new File(jar1URL.toURI()));

         warAssembly.add("WEB-INF/lib/jar1.jar/META-INF/Manifest.mf", testDir.getChild("jar1-filesonly.mf"));
         warAssembly.add("WEB-INF/web.xml", testDir.getChild("web.xml"));

         assertMapped(testDir.getChild("nested/nested.jar"), VFS.getChild(
               "assembly.ear/assembly.war/WEB-INF/lib/nested.jar"));
         assertMapped(testDir.getChild("nested/nested_copy.jar"), VFS.getChild(
               "assembly.ear/assembly.war/WEB-INF/lib/nested_copy.jar"));
         assertMapped(testDir.getChild("jar1-filesonly.mf"), VFS.getChild("assembly.ear").getChild(
               "assembly.war").getChild("WEB-INF").getChild("lib").getChild("jar1.jar").getChild("META-INF").getChild(
               "Manifest.mf"));
         assertTrue(VFS.getChild(
               "assembly.ear/assembly.war/WEB-INF/lib/jar1.jar/org/jboss/test/vfs/support/jar1/ClassInJar1.class")
               .exists());
      }
      finally {
         VFSUtils.safeClose(Arrays.asList(earAssemblyHandle, warAssemblyHandle));
      }
   }

   @Test
   public void testGetNonExistentFile() throws Exception {

      VirtualFile assemblyLocation = VFS.getChild("/assembly");
      VirtualFileAssembly assembly = new VirtualFileAssembly();
      Closeable assemblyHandle = VFS.mountAssembly(assembly, assemblyLocation);
      try {
         VirtualFile virtualFile = assemblyLocation.getChild("missingFile.txt");
         assertFalse(virtualFile.exists());
      }
      finally {
         VFSUtils.safeClose(assemblyHandle);
      }
   }

   @Test
   public void testDelete() throws Exception {

      VirtualFile assemblyLocation = VFS.getChild("/assembly");
      VirtualFileAssembly assembly = new VirtualFileAssembly();
      Closeable assemblyHandle = VFS.mountAssembly(assembly, assemblyLocation);
      try {
         VirtualFile virtualFile = assemblyLocation.getChild("missingFile.txt");
         assertFalse(virtualFile.exists());
      }
      finally {
         VFSUtils.safeClose(assemblyHandle);
      }
   }

   @Test
   public void testGetChildren() throws Exception {
      VirtualFileAssembly assembly = new VirtualFileAssembly();
      VirtualFile assemblyLocation = VFS.getChild("/assembly");
      Closeable assemblyHandle = VFS.mountAssembly(assembly, assemblyLocation);
      try {
         URL jar1URL = getResource("/vfs/test/jar1.jar");
         assembly.addZip("jar1.jar", new File(jar1URL.toURI()));

         VirtualFile virtualFile = assemblyLocation.getChild("jar1.jar");
         assertTrue(virtualFile.exists());
         List<VirtualFile> directoryEntries = virtualFile.getChildren();

         assertFalse(directoryEntries.isEmpty());
         for (VirtualFile child : directoryEntries) {
            assertTrue(child.getPathName().startsWith("/assembly/jar1.jar"));
         }

      }
      finally {
         VFSUtils.safeClose(assemblyHandle);
      }
   }

   @Test
   public void assertMapped(VirtualFile expected, VirtualFile actual) throws Exception {
      assertNotNull(actual);
      assertEquals(expected.getPhysicalFile(), actual.getPhysicalFile());
   }
}
