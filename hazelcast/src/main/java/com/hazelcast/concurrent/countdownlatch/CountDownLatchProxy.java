/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.concurrent.countdownlatch;

import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.spi.*;
import com.hazelcast.util.ExceptionUtil;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author mdogan 1/10/13
 */
public class CountDownLatchProxy extends AbstractDistributedObject<CountDownLatchService> implements ICountDownLatch {

    private final String name;
    private final int partitionId;

    public CountDownLatchProxy(String name, NodeEngine nodeEngine) {
        super(nodeEngine, null);
        this.name = name;
        partitionId = nodeEngine.getPartitionService().getPartitionId(getNameAsPartitionAwareData());
    }

    public String getName() {
        return name;
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        final NodeEngine nodeEngine = getNodeEngine();
        Future f = nodeEngine.getOperationService().invokeOnPartition(CountDownLatchService.SERVICE_NAME,
                new AwaitOperation(name, getTimeInMillis(timeout, unit)), partitionId);
        try {
            return (Boolean) nodeEngine.toObject(f.get());
        } catch (ExecutionException e) {
            throw ExceptionUtil.rethrowAllowInterrupted(e);
        }
    }

    private long getTimeInMillis(final long time, final TimeUnit timeunit) {
        return timeunit != null ? timeunit.toMillis(time) : time;
    }

    public void countDown() {
        final NodeEngine nodeEngine = getNodeEngine();
        Future f = nodeEngine.getOperationService().invokeOnPartition(CountDownLatchService.SERVICE_NAME,
                new CountDownOperation(name), partitionId);
        try {
            f.get();
        } catch (Exception e) {
            throw ExceptionUtil.rethrow(e);
        }
    }

    public int getCount() {
        final NodeEngine nodeEngine = getNodeEngine();
        Future f = nodeEngine.getOperationService().invokeOnPartition(CountDownLatchService.SERVICE_NAME,
                new GetCountOperation(name), partitionId);
        try {
            return (Integer) nodeEngine.toObject(f.get());
        } catch (Exception e) {
            throw ExceptionUtil.rethrow(e);
        }
    }

    public boolean trySetCount(int count) {
        final NodeEngine nodeEngine = getNodeEngine();
        Future f = nodeEngine.getOperationService().invokeOnPartition(CountDownLatchService.SERVICE_NAME,
                new SetCountOperation(name, count), partitionId);
        try {
            return (Boolean) nodeEngine.toObject(f.get());
        } catch (Exception e) {
            throw ExceptionUtil.rethrow(e);
        }
    }

    public String getServiceName() {
        return CountDownLatchService.SERVICE_NAME;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ICountDownLatch{");
        sb.append("name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
