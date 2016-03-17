/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.v3po.impl.vppstate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.fd.honeycomb.v3po.impl.trans.VppReader;
import io.fd.honeycomb.v3po.impl.trans.impl.CompositeRootVppReader;
import io.fd.honeycomb.v3po.impl.trans.util.DelegatingReaderRegistry;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.VersionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.L2Fib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.L2FibKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;
import org.openvpp.vppjapi.vppBridgeDomainDetails;
import org.openvpp.vppjapi.vppBridgeDomainInterfaceDetails;
import org.openvpp.vppjapi.vppL2Fib;
import org.openvpp.vppjapi.vppVersion;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.openvpp.vppjapi.vppConn")
@PrepareForTest(vppApi.class)
public class BdTest {

    public static final vppVersion VERSION = new vppVersion("test", "1", "2", "33");

    private vppApi api;
    private CompositeRootVppReader<VppState, VppStateBuilder> vppStateReader;
    private DelegatingReaderRegistry readerRegistry;
    private vppBridgeDomainDetails bdDetails;
    private vppBridgeDomainDetails bdDetails2;

    @Before
    public void setUp() throws Exception {
        api = PowerMockito.mock(vppApi.class);

        bdDetails = new vppBridgeDomainDetails();
        setIfcs(bdDetails);
        setBaseAttrs(bdDetails, "bdn1", 1);

        bdDetails2 = new vppBridgeDomainDetails();
        setIfcs(bdDetails2);
        setBaseAttrs(bdDetails2, "bdn2", 2);

        final vppL2Fib[] l2Fibs = getL2Fibs();
        PowerMockito.doReturn(l2Fibs).when(api).l2FibTableDump(Matchers.anyInt());
        PowerMockito.doAnswer(new Answer<vppBridgeDomainDetails>() {

            @Override
            public vppBridgeDomainDetails answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final Integer idx = (Integer) invocationOnMock.getArguments()[0];
                switch (idx) {
                    case 1 : return bdDetails;
                    case 2 : return bdDetails2;
                    default: return null;
                }
            }
        }).when(api).getBridgeDomainDetails(Matchers.anyInt());

        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final String name = (String) invocationOnMock.getArguments()[0];
                switch (name) {
                    case "bdn1" : return 1;
                    case "bdn2" : return 2;
                    default: return null;
                }
            }
        }).when(api).bridgeDomainIdFromName(anyString());
        PowerMockito.doReturn(new int[] {1, 2}).when(api).bridgeDomainDump(Matchers.anyInt());
        PowerMockito.doReturn(VERSION).when(api).getVppVersion();
        vppStateReader = VppStateUtils.getVppStateReader(api);
        readerRegistry = new DelegatingReaderRegistry(Collections.<VppReader<? extends DataObject>>singletonList(vppStateReader));
    }

    private vppL2Fib[] getL2Fibs() {
        return new vppL2Fib[] {
            new vppL2Fib(new byte[]{1,2,3,4,5,6}, true, "ifc1", true, true),
            new vppL2Fib(new byte[]{2,2,3,4,5,6}, true, "ifc2", true, true),
        };
    }

    private void setIfcs(final vppBridgeDomainDetails bdDetails) {
        final vppBridgeDomainInterfaceDetails ifcDetails = new vppBridgeDomainInterfaceDetails();
        ifcDetails.interfaceName = "ifc";
        ifcDetails.splitHorizonGroup = 2;
        bdDetails.interfaces = new vppBridgeDomainInterfaceDetails[] {ifcDetails};
    }

    private void setBaseAttrs(final vppBridgeDomainDetails bdDetails, final String bdn, final int i) {
        bdDetails.name = bdn;
        bdDetails.arpTerm = true;
        bdDetails.bdId = i;
        bdDetails.bviInterfaceName = "ifc";
        bdDetails.flood = true;
        bdDetails.forward = true;
        bdDetails.learn = true;
        bdDetails.uuFlood = true;
    }

    @Test
    public void testReadAll() throws Exception {
        final List<? extends DataObject> dataObjects = readerRegistry.readAll();
        assertEquals(dataObjects.size(), 1);
        final DataObject dataObject = dataObjects.get(0);
        assertTrue(dataObject instanceof VppState);
        assertVersion((VppState) dataObject);
        assertEquals(2, ((VppState) dataObject).getBridgeDomains().getBridgeDomain().size());
    }

    private void assertVersion(final VppState dataObject) {
        assertEquals(
            new VersionBuilder()
                .setName("test")
                .setBuildDirectory("1")
                .setBranch("2")
                .setBuildDate("33")
                .build(),
            dataObject.getVersion());
    }

    @Test
    public void testReadSpecific() throws Exception {
        final List<? extends DataObject> read = readerRegistry.read(InstanceIdentifier.create(VppState.class));
        assertEquals(read.size(), 1);
        assertVersion((VppState) read.get(0));
    }

    @Test
    public void testReadBridgeDomains() throws Exception {
        VppState readRoot = (VppState) readerRegistry.read(InstanceIdentifier.create(VppState.class)).get(0);

        List<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(VppState.class).child(BridgeDomains.class));
//        System.err.println(read);
        assertEquals(read.size(), 1);
        assertEquals(readRoot.getBridgeDomains(), read.get(0));
    }

    /**
     * L2fib does not have a dedicated reader, relying on auto filtering
     */
    @Test
    public void testReadL2Fib() throws Exception {
        // Deep child without a dedicated reader with specific l2fib key
        List<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
                BridgeDomain.class, new BridgeDomainKey("bdn1"))
                .child(L2Fib.class, new L2FibKey(new PhysAddress("01:02:03:04:05:06"))));
//        System.err.println(read);
        assertEquals(read.size(), 1);

        // non existing l2fib
        read =
            readerRegistry.read(InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
                BridgeDomain.class, new BridgeDomainKey("bdn1"))
                .child(L2Fib.class, new L2FibKey(new PhysAddress("FF:FF:FF:04:05:06"))));
//        System.err.println(read);
        assertEquals(read.size(), 0);
    }

    /**
     * L2fib does not have a dedicated reader, relying on auto filtering
     */
    @Test
    public void testReadL2FibAll() throws Exception {
        // Deep child without a dedicated reader
        final InstanceIdentifier<L2Fib> id = InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
                BridgeDomain.class, new BridgeDomainKey("bdn1")).child(L2Fib.class);
        final List<? extends DataObject> read = readerRegistry.read(id);
//        System.err.println(read);
        assertEquals(read.toString(), read.size(), 2);

        // test if direct read returns the same value
        final List<? extends DataObject> read2 = vppStateReader.read(id);
//        System.err.println(read);
        assertEquals(read, read2);
    }

    @Test
    public void testReadBridgeDomainAll() throws Exception {
        VppState readRoot = (VppState) readerRegistry.read(InstanceIdentifier.create(VppState.class)).get(0);

        final List<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
                BridgeDomain.class));
//        System.err.println(read);
        assertEquals(read.size(), 2);
        assertEquals(readRoot.getBridgeDomains().getBridgeDomain(), read);
    }

    @Test
    public void testReadBridgeDomain() throws Exception {
        VppState readRoot = (VppState) readerRegistry.read(InstanceIdentifier.create(VppState.class)).get(0);

        final List<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
                BridgeDomain.class, new BridgeDomainKey("bdn1")));

        assertEquals(read.size(), 1);
        assertEquals(Iterables.find(readRoot.getBridgeDomains().getBridgeDomain(), new Predicate<BridgeDomain>() {
            @Override
            public boolean apply(final BridgeDomain input) {
                return input.getKey().getName().equals("bdn1");
            }
        }), read.get(0));
    }

    @Ignore("Bridge domain customizer does not check whether the bd exists or not and fails with NPE, add it there")
    @Test
    public void testReadBridgeDomainNotExisting() throws Exception {
        final List<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(VppState.class).child(BridgeDomains.class).child(
                BridgeDomain.class, new BridgeDomainKey("NOT EXISTING")));
        assertEquals(read.size(), 0);
    }

    @Test
    public void testReadVersion() throws Exception {
        VppState readRoot = (VppState) readerRegistry.read(InstanceIdentifier.create(VppState.class)).get(0);

        List<? extends DataObject> read =
            readerRegistry.read(InstanceIdentifier.create(VppState.class).child(Version.class));
//        System.err.println(read);
        assertEquals(read.size(), 1);
        assertEquals(readRoot.getVersion(), read.get(0));
    }
}