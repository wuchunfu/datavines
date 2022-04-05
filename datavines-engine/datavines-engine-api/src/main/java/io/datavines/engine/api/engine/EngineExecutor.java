/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.datavines.engine.api.engine;

import org.slf4j.Logger;

import io.datavines.common.config.Configurations;
import io.datavines.common.entity.TaskRequest;
import io.datavines.common.entity.ProcessResult;
import io.datavines.spi.SPI;;

@SPI
public interface EngineExecutor {

    /**
     * 进行初始化操作
     * @throws Exception Exception
     */
    void init(TaskRequest taskRequest, Logger logger, Configurations configurations) throws Exception;

    /**
     * 执行实际内容
     * @throws Exception Exception
     */
    void execute() throws Exception;

    /**
     * 做好任务执行完之后的处理工作
     * @throws Exception Exception
     */
    void after() throws Exception;

    /**
     * 取消任务
     * @throws Exception Exception
     */
    void cancel() throws Exception;

    /**
     * 是否取消
     * @throws Exception Exception
     */
    boolean isCancel() throws Exception;

    /**
     * 获取执行结果
     */
    ProcessResult getProcessResult();

    /**
     * 获取execution job
     * @return
     */
    TaskRequest getTaskRequest();
}