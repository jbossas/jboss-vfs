/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, JBoss Inc., and individual contributors as indicated
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

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashSet;
import java.util.Set;

import org.jboss.vfs.VFSMessages;

/**
 * Abstract URLStreamHandler that can be used as a base for other URLStreamHandlers that
 * require the URL to be local.
 *
 * @author <a href=mailto:jbailey@redhat.com">John Bailey</a>
 * @version $Revision$
 */
public abstract class AbstractLocalURLStreamHandler extends URLStreamHandler {

    private static final Set<String> locals;

    static {
        Set<String> set = new HashSet<String>();
        set.add(null);
        set.add("");
        set.add("~");
        set.add("localhost");
        locals = set;
    }

    private static String toLower(String str) {
        return str == null ? null : str.toLowerCase();
    }

    @Override
    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        return openConnection(u);
    }

    @Override
    protected boolean hostsEqual(URL url1, URL url2) {
        return locals.contains(toLower(url1.getHost())) && locals.contains(toLower(url2.getHost())) || super.hostsEqual(url1, url2);
    }

    protected void ensureLocal(URL url) throws IOException {
        if (!locals.contains(toLower(url.getHost()))) {
            throw VFSMessages.MESSAGES.remoteHostAccessNotSupportedForUrls(url.getProtocol());
        }
    }

}
