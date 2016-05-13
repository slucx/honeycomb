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

package io.fd.honeycomb.v3po.translate.v3po.interfaces;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.net.InetAddresses;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanVni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.VxlanAddDelTunnel;
import org.openvpp.jvpp.dto.VxlanAddDelTunnelReply;
import org.openvpp.jvpp.future.FutureJVpp;

public class VxlanCustomizerTest {

    private static final byte ADD_VXLAN = 1;
    private static final byte DEL_VXLAN = 0;

    @Mock
    private FutureJVpp api;
    @Mock
    private WriteContext writeContext;

    private VxlanCustomizer customizer;
    private NamingContext namingContext;
    private String ifaceName;
    private InstanceIdentifier<Vxlan> id;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel.class);
        // TODO create base class for tests using vppApi
        namingContext = new NamingContext("generateInterfaceNAme");
        customizer = new VxlanCustomizer(api, namingContext);

        ifaceName = "eth0";
        id = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(ifaceName))
                        .augmentation(VppInterfaceAugmentation.class).child(Vxlan.class);
    }

    private void whenVxlanAddDelTunnelThen(final int retval) throws ExecutionException, InterruptedException {
        final CompletionStage<VxlanAddDelTunnelReply> replyCS = mock(CompletionStage.class);
        final CompletableFuture<VxlanAddDelTunnelReply> replyFuture = mock(CompletableFuture.class);
        when(replyCS.toCompletableFuture()).thenReturn(replyFuture);
        final VxlanAddDelTunnelReply reply = new VxlanAddDelTunnelReply();
        reply.retval = retval;
        when(replyFuture.get()).thenReturn(reply);
        when(api.vxlanAddDelTunnel(any(VxlanAddDelTunnel.class))).thenReturn(replyCS);
    }

    private void whenVxlanAddDelTunnelThenSuccess() throws ExecutionException, InterruptedException {
        whenVxlanAddDelTunnelThen(0);
    }

    private void whenVxlanAddDelTunnelThenFailure() throws ExecutionException, InterruptedException {
        whenVxlanAddDelTunnelThen(-1);
    }

    private VxlanAddDelTunnel verifyVxlanAddDelTunnelWasInvoked(final Vxlan vxlan) {
        ArgumentCaptor<VxlanAddDelTunnel> argumentCaptor = ArgumentCaptor.forClass(VxlanAddDelTunnel.class);
        verify(api).vxlanAddDelTunnel(argumentCaptor.capture());
        final VxlanAddDelTunnel actual = argumentCaptor.getValue();
        assertEquals(0, actual.isIpv6);
        assertEquals(-1, actual.decapNextIndex);
        assertArrayEquals(InetAddresses.forString(vxlan.getSrc().getIpv4Address().getValue()).getAddress(), actual.srcAddress);
        assertArrayEquals(InetAddresses.forString(vxlan.getDst().getIpv4Address().getValue()).getAddress(), actual.dstAddress);
        assertEquals(vxlan.getEncapVrfId().intValue(), actual.encapVrfId);
        assertEquals(vxlan.getVni().getValue().intValue(), actual.vni);
        return actual;
    }
    private void verifyVxlanAddWasInvoked(final Vxlan vxlan) {
        final VxlanAddDelTunnel actual = verifyVxlanAddDelTunnelWasInvoked(vxlan);
        assertEquals(ADD_VXLAN, actual.isAdd);
    }

    private void verifyVxlanDeleteWasInvoked(final Vxlan vxlan) {
        final VxlanAddDelTunnel actual = verifyVxlanAddDelTunnelWasInvoked(vxlan);
        assertEquals(DEL_VXLAN, actual.isAdd);
    }

    private static Vxlan generateVxlan(long vni) {
        final VxlanBuilder builder = new VxlanBuilder();
        builder.setSrc(new IpAddress(new Ipv4Address("192.168.20.10")));
        builder.setDst(new IpAddress(new Ipv4Address("192.168.20.11")));
        builder.setEncapVrfId(Long.valueOf(123));
        builder.setVni(new VxlanVni(Long.valueOf(vni)));
        return builder.build();
    }

    private static Vxlan generateVxlan() {
        return generateVxlan(Long.valueOf(11));
    }

    @Test
    public void testWriteCurrentAttributes() throws Exception {
        final Vxlan vxlan = generateVxlan();

        whenVxlanAddDelTunnelThenSuccess();

        customizer.writeCurrentAttributes(id, vxlan, writeContext);
        verifyVxlanAddWasInvoked(vxlan);
        assertTrue(namingContext.containsIndex(ifaceName));
    }

    @Test
    public void testWriteCurrentAttributesFailed() throws Exception {
        final Vxlan vxlan = generateVxlan();

        whenVxlanAddDelTunnelThenFailure();

        try {
            customizer.writeCurrentAttributes(id, vxlan, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertEquals(VppApiInvocationException.class, e.getCause().getClass());
            verifyVxlanAddWasInvoked(vxlan);
            assertFalse(namingContext.containsIndex(ifaceName));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testUpdateCurrentAttributes() throws Exception {
        try {
            customizer.updateCurrentAttributes(id, generateVxlan(10), generateVxlan(11), writeContext);
        } catch (WriteFailedException.UpdateFailedException e) {
            assertEquals(UnsupportedOperationException.class, e.getCause().getClass());
            return;
        }
        fail("WriteFailedException.UpdateFailedException was expected");
    }

    @Test
    public void testUpdateCurrentAttributesNoUpdate() throws Exception {
        customizer.updateCurrentAttributes(id, generateVxlan(), generateVxlan(), writeContext);
        verify(api, never()).vxlanAddDelTunnel(any(VxlanAddDelTunnel.class));
    }

    @Test
    public void testDeleteCurrentAttributes() throws Exception {
        final Vxlan vxlan = generateVxlan();

        whenVxlanAddDelTunnelThenSuccess();
        namingContext.addName(1, ifaceName);

        customizer.deleteCurrentAttributes(id, vxlan, writeContext);
        verifyVxlanDeleteWasInvoked(vxlan);
        assertFalse(namingContext.containsIndex(ifaceName));
    }

    @Test
    public void testDeleteCurrentAttributesaFailed() throws Exception {
        final Vxlan vxlan = generateVxlan();

        whenVxlanAddDelTunnelThenFailure();
        namingContext.addName(1, ifaceName);

        try {
            customizer.deleteCurrentAttributes(id, vxlan, writeContext);
        } catch (WriteFailedException.DeleteFailedException e) {
            assertEquals(VppApiInvocationException.class, e.getCause().getClass());
            verifyVxlanDeleteWasInvoked(vxlan);
            assertTrue(namingContext.containsIndex(ifaceName));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");
    }
}