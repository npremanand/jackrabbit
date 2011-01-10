/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.id;

import java.io.File;
import java.io.IOException;
import javax.jcr.RepositoryException;
import org.apache.commons.io.FileUtils;
import junit.framework.TestCase;

public class NodeIdFactoryTest extends TestCase {

    private static final String factoryDir = "target/temp/nodeIdFactory";

    public void setUp() throws IOException {
        FileUtils.deleteDirectory(new File(factoryDir));
    }

    public void tearDown() throws IOException {
        setUp();
    }

    public void testRandomVersusSequential() throws RepositoryException {
        System.clearProperty(NodeIdFactory.SEQUENTIAL_NODE_ID);
        NodeIdFactory f = new NodeIdFactory(factoryDir);
        f.open();
        NodeId id = f.newNodeId();
        assertTrue(id.getLeastSignificantBits() != 0);
        f.close();

        System.setProperty(NodeIdFactory.SEQUENTIAL_NODE_ID, "true");
        f = new NodeIdFactory(factoryDir);
        f.open();
        id = f.newNodeId();
        assertTrue(id.getLeastSignificantBits() == 0);
        f.close();
    }

    public void testNormalUsage() throws RepositoryException {
        System.setProperty(NodeIdFactory.SEQUENTIAL_NODE_ID, "true");
        NodeIdFactory f = new NodeIdFactory(factoryDir);
        f.open();
        assertEquals("00000000-0000-0000-0000-000000000000", f.newNodeId().toString());
        f.close();
        f = new NodeIdFactory(factoryDir);
        f.open();
        assertEquals("00000000-0000-0000-0000-000000000001", f.newNodeId().toString());
        f.close();
    }

    public void testOffset() throws RepositoryException {
        System.setProperty(NodeIdFactory.SEQUENTIAL_NODE_ID, "ab/0");
        NodeIdFactory f = new NodeIdFactory(factoryDir);
        f.open();
        assertEquals("00000000-0000-00ab-0000-000000000000", f.newNodeId().toString());
        f.close();
        f = new NodeIdFactory(factoryDir);
        f.open();
        assertEquals("00000000-0000-00ab-0000-000000000001", f.newNodeId().toString());
        f.close();
    }

    public void testKillRepository() throws RepositoryException {
        System.setProperty(NodeIdFactory.SEQUENTIAL_NODE_ID, "true");
        int cacheSize = 8;
        for (int i = 1; i < 40; i++) {
            File id = new File(factoryDir, "nodeId.properties");
            id.delete();
            NodeIdFactory f = new NodeIdFactory(factoryDir);
            f.setCacheSize(cacheSize);
            f.open();
            NodeId last = null;
            for (int j = 0; j < i; j++) {
                last = f.newNodeId();
            }
            // don't close the factory - this is the same as killing the process
            // f.close();
            f = new NodeIdFactory(factoryDir);
            f.setCacheSize(cacheSize);
            f.open();
            NodeId n = f.newNodeId();
            assertTrue("now: " + n + " last: " + last, n.compareTo(last) > 0);
            long diff = n.getLeastSignificantBits() - last.getLeastSignificantBits();
            assertTrue("diff: " + diff, diff > 0 && diff <= cacheSize);
            f.close();
        }
    }

    public void testKillWhileSaving() throws RepositoryException {
        System.setProperty(NodeIdFactory.SEQUENTIAL_NODE_ID, "true");
        NodeIdFactory f = new NodeIdFactory(factoryDir);
        f.open();
        assertEquals("00000000-0000-0000-0000-000000000000", f.newNodeId().toString());
        f.close();
        File id = new File(factoryDir, "nodeId.properties");
        assertTrue(id.exists());
        File idTemp = new File(factoryDir, "nodeId.properties.temp");
        id.renameTo(idTemp);
        f = new NodeIdFactory(factoryDir);
        f.open();
        assertEquals("00000000-0000-0000-0000-000000000001", f.newNodeId().toString());
        assertFalse(idTemp.exists());
        f.close();
    }

}
