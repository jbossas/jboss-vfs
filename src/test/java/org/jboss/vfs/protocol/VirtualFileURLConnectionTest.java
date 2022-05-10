/*
* JBoss, Home of Professional Open Source
* Copyright 2006, JBoss Inc., and individual contributors as indicated
* by the @authors tag.
*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.jboss.vfs.protocol;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import junit.framework.TestSuite;
import org.jboss.test.vfs.AbstractVFSTest;
import org.jboss.vfs.VirtualFile;
import org.junit.Assert;
import org.junit.Test;

/**
 * Provides test cases for WFLY16322, which was broken in 
 * <a href="https://bugs.openjdk.java.net/browse/JDK-8273655">JDK-8273655 
 * content-types.properties files are missing some common types</a>
 * and released in 17.0.3, 11.0.16, 13.0.12 and 15.0.8. The JDK issue implements
 * a content type for Java Archives, which breaks the expectation that 
 * getContent() returns the VirtualFile (behavior for when the content type is
 * {@code null}.
 */
public class VirtualFileURLConnectionTest extends AbstractVFSTest {
    public VirtualFileURLConnectionTest(String name) {
        super(name);
    }
    
    public static junit.framework.Test suite() {
        return new TestSuite(VirtualFileURLConnectionTest.class);
    }

    @Test
    public void testFixWFLY16322ContentTypeJarShouldReturnVirtualFile() throws MalformedURLException, IOException {
        VirtualFile jar = getVirtualFile("/vfs/test/dup.jar");

        URL url = getResource("/vfs/test/dup.jar");
        VirtualFileURLConnection con = new VirtualFileURLConnection(url) {
            public String getContentType() {
                return VirtualFileURLConnection.JAR_CONTENT_TYPE;
            }
        };
        
        Object content = con.getContent();
        Assert.assertTrue(content instanceof VirtualFile);
    }

    @Test
    public void testFixWFLY16322NullValueShouldReturnVirtualFile() throws MalformedURLException, IOException {
        VirtualFile jar = getVirtualFile("/vfs/test/dup.jar");

        URL url = getResource("/vfs/test/dup.jar");
        VirtualFileURLConnection con = new VirtualFileURLConnection(url) {
            public String getContentType() {
                return null;
            }
        };
        
        Object content = con.getContent();
        Assert.assertTrue(content instanceof VirtualFile);
    }

    @Test
    public void testFixWFLY16322ContentTypeJsonShouldReturnFileInputStream() throws MalformedURLException, IOException {
        VirtualFile jar = getVirtualFile("/vfs/test/dup.jar");

        URL url = getResource("/vfs/test/dup.jar");
        VirtualFileURLConnection con = new VirtualFileURLConnection(url) {
            public String getContentType() {
                return "application/json";
            }
        };
        
        Object content = con.getContent();
        Assert.assertTrue(content instanceof FileInputStream);
    }
}
