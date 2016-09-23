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

package io.fd.honeycomb.translate.v3po.interfacesstate.ip;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.Ipv4Translator;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.IpAddressDetails;
import org.openvpp.jvpp.core.dto.IpAddressDetailsReplyDump;
import org.openvpp.jvpp.core.dto.IpAddressDump;

public class Ipv4AddressCustomizerTest extends ListReaderCustomizerTest<Address, AddressKey, AddressBuilder> implements
        Ipv4Translator {

    private static final String IFACE_NAME = "eth0";
    private static final String IFACE_2_NAME = "eth1";
    private static final int IFACE_ID = 1;
    private static final int IFACE_2_ID = 2;
    private static final String IFC_CTX_NAME = "ifc-test-instance";
    public static final String CACHE_KEY = Ipv4AddressCustomizer.class.getName();

    private NamingContext interfacesContext;

    public Ipv4AddressCustomizerTest() {
        super(Address.class);
    }

    @Override
    public void setUp() {
        interfacesContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        defineMapping(mappingContext, IFACE_2_NAME, IFACE_2_ID, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<Address, AddressBuilder> initCustomizer() {
        return new Ipv4AddressCustomizer(api, interfacesContext);
    }

    private static InstanceIdentifier<Address> getId(final String address, final String ifaceName) {
        return InstanceIdentifier.builder(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(ifaceName))
                .augmentation(Interface2.class)
                .child(Ipv4.class)
                .child(Address.class, new AddressKey(new Ipv4AddressNoZone(new Ipv4Address(address))))
                .build();
    }

    @Test
    public void testReadCurrentAttributesFor2Ifcs() throws ReadFailedException {
        //changed to mock to not store first dumped data(otherwise that double thenReturn on line 118 is not gonna work)
        ModificationCache cache = mock(ModificationCache.class);

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();

        detail1.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))));
        detail2.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2"))));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1);
        IpAddressDetailsReplyDump reply2 = new IpAddressDetailsReplyDump();
        reply2.ipAddressDetails = ImmutableList.of(detail2);

        CompletableFuture<IpAddressDetailsReplyDump> future = new CompletableFuture<>();
        future.complete(reply);
        CompletableFuture<IpAddressDetailsReplyDump> future2 = new CompletableFuture<>();
        future2.complete(reply2);

        when(api.ipAddressDump(Mockito.any(IpAddressDump.class))).thenReturn(future).thenReturn(future2)
                .thenReturn(future).thenReturn(future2);
        when(api.ipAddressDump(Mockito.any(IpAddressDump.class))).thenReturn(future(reply)).thenReturn(future(reply2))
                .thenReturn(future(reply)).thenReturn(future(reply2));
        when(ctx.getModificationCache()).thenReturn(cache);


        final InstanceIdentifier<Address> id = getId("192.168.2.1", IFACE_NAME);
        final InstanceIdentifier<Address> id2 = getId("192.168.2.2", IFACE_2_NAME);

        final List<AddressKey> ifc1Ids = getCustomizer().getAllIds(id, ctx);
        assertThat(ifc1Ids.size(), is(1));
        assertThat(ifc1Ids, Matchers.hasItem(new AddressKey(new Ipv4AddressNoZone("192.168.2.1"))));
        final List<AddressKey> ifc2Ids = getCustomizer().getAllIds(id2, ctx);
        assertThat(ifc2Ids.size(), is(1));
        assertThat(ifc2Ids, Matchers.hasItem(new AddressKey(new Ipv4AddressNoZone("192.168.2.2"))));

        AddressBuilder builder = new AddressBuilder();
        getCustomizer().readCurrentAttributes(id, builder, ctx);
        assertEquals(builder.getIp().getValue(), "192.168.2.1");
        builder = new AddressBuilder();
        getCustomizer().readCurrentAttributes(id2, builder, ctx);
        assertEquals(builder.getIp().getValue(), "192.168.2.2");
    }

    @Test
    public void testReadCurrentAttributesSuccessfull() throws ReadFailedException {
        ModificationCache cache = new ModificationCache();

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();
        IpAddressDetails detail3 = new IpAddressDetails();

        detail1.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))));
        detail2.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2"))));
        detail3.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.3"))));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1, detail2, detail3);
        when(api.ipAddressDump(Mockito.any(IpAddressDump.class))).thenReturn(future(reply));
        when(ctx.getModificationCache()).thenReturn(cache);

        final AddressBuilder builder = new AddressBuilder();
        final InstanceIdentifier<Address> id = getId("192.168.2.1", IFACE_NAME);

        getCustomizer().readCurrentAttributes(id, builder, ctx);

        assertEquals("192.168.2.1", builder.getIp().getValue());
    }

    @Test
    public void testGetAllIdsFromSuccessfull() throws ReadFailedException {
        ModificationCache cache = new ModificationCache();

        IpAddressDetails detail1 = new IpAddressDetails();
        IpAddressDetails detail2 = new IpAddressDetails();
        IpAddressDetails detail3 = new IpAddressDetails();

        detail1.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.1"))));
        detail2.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.2"))));
        detail3.ip = reverseBytes(
                ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(new Ipv4Address("192.168.2.3"))));

        IpAddressDetailsReplyDump reply = new IpAddressDetailsReplyDump();
        reply.ipAddressDetails = ImmutableList.of(detail1, detail2, detail3);
        when(api.ipAddressDump(Mockito.any(IpAddressDump.class))).thenReturn(future(reply));
        when(ctx.getModificationCache()).thenReturn(cache);

        final InstanceIdentifier<Address> id = getId("192.168.2.1", IFACE_NAME);

        List<Ipv4AddressNoZone> ids = getCustomizer().getAllIds(id, ctx).stream()
                .map(key -> key.getIp())
                .collect(Collectors.toList());

        assertEquals(3, ids.size());
        assertEquals(true, "192.168.2.1".equals(ids.get(0).getValue()));
        assertEquals(true, "192.168.2.2".equals(ids.get(1).getValue()));
        assertEquals(true, "192.168.2.3".equals(ids.get(2).getValue()));
    }

    @Test
    public void testMerge() {

        Address address = new AddressBuilder().build();
        Ipv4Builder ipv4Builder = new Ipv4Builder();
        getCustomizer().merge(ipv4Builder, Arrays.asList(address));

        assertEquals(1, ipv4Builder.getAddress().size());
        assertEquals(true, ipv4Builder.getAddress().contains(address));
    }

}
