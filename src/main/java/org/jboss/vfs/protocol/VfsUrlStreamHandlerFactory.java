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


import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * URLStreamHandlerFactory providing URLStreamHandlers for VFS based URLS.
 *
 * @author John Bailey
 */
public class VfsUrlStreamHandlerFactory implements URLStreamHandlerFactory {


    private static Map<String, URLStreamHandler> handlerMap = new HashMap<String, URLStreamHandler>(2);

    static {
        handlerMap.put("file", new FileURLStreamHandler());
        handlerMap.put("vfs", new VirtualFileURLStreamHandler());
    }


    @Override
    public URLStreamHandler createURLStreamHandler(final String protocol) {
        return handlerMap.get(protocol);
    }
}
