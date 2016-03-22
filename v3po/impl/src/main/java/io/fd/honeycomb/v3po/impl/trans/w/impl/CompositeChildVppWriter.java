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

package io.fd.honeycomb.v3po.impl.trans.w.impl;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.impl.trans.util.VppRWUtils;
import io.fd.honeycomb.v3po.impl.trans.w.ChildVppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.WriteContext;
import io.fd.honeycomb.v3po.impl.trans.w.impl.spi.ChildVppWriterCustomizer;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CompositeChildVppWriter<D extends DataObject> extends AbstractCompositeVppWriter<D>
    implements ChildVppWriter<D> {

    private final ChildVppWriterCustomizer<D> customizer;

    public CompositeChildVppWriter(@Nonnull final Class<D> type,
                                   @Nonnull final List<ChildVppWriter<? extends ChildOf<D>>> childWriters,
                                   @Nonnull final List<ChildVppWriter<? extends Augmentation<D>>> augReaders,
                                   @Nonnull final ChildVppWriterCustomizer<D> customizer) {
        super(type, childWriters, augReaders);
        this.customizer = customizer;
    }

    public CompositeChildVppWriter(@Nonnull final Class<D> type,
                                   @Nonnull final List<ChildVppWriter<? extends ChildOf<D>>> childWriters,
                                   @Nonnull final ChildVppWriterCustomizer<D> customizer) {
        this(type, childWriters, VppRWUtils.<D>emptyAugWriterList(), customizer);
    }

    public CompositeChildVppWriter(@Nonnull final Class<D> type,
                                   @Nonnull final ChildVppWriterCustomizer<D> customizer) {
        this(type, VppRWUtils.<D>emptyChildWriterList(), VppRWUtils.<D>emptyAugWriterList(), customizer);
    }

    @Override
    protected void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D data,
                                          @Nonnull final WriteContext ctx) {
        customizer.writeCurrentAttributes(id, data, ctx.getContext());
    }

    @Override
    protected void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull WriteContext ctx) {
        customizer.deleteCurrentAttributes(id, dataBefore, ctx.getContext());
    }

    @Override
    protected void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final D dataAfter, @Nonnull WriteContext ctx) {
        customizer.updateCurrentAttributes(id, dataBefore, dataAfter, ctx.getContext());
    }

    @Override
    public void writeChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                           @Nonnull final DataObject parentData, @Nonnull WriteContext ctx) {
        final InstanceIdentifier<D> currentId = VppRWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final Optional<D> currentData = customizer.extract(currentId, parentData);
        if(currentData.isPresent()) {
            writeCurrent(currentId, currentData.get(), ctx);
        }
    }

    @Override
    public void deleteChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                            @Nonnull final DataObject parentData,
                            @Nonnull final WriteContext ctx) {
        final InstanceIdentifier<D> currentId = VppRWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final Optional<D> currentData = customizer.extract(currentId, parentData);
        if(currentData.isPresent()) {
            deleteCurrent(currentId, currentData.get(), ctx);
        }
    }

    @Override
    public void updateChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                            @Nonnull final DataObject parentDataBefore, @Nonnull final DataObject parentDataAfter,
                            @Nonnull final WriteContext ctx) {
        final InstanceIdentifier<D> currentId = VppRWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final Optional<D> before = customizer.extract(currentId, parentDataBefore);
        final Optional<D> after = customizer.extract(currentId, parentDataAfter);
        if(before.isPresent() && after.isPresent()) {
            updateCurrent(currentId, before.get(), after.get(), ctx);
        }
    }
}