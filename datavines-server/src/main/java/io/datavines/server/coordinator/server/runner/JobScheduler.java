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

package io.datavines.server.coordinator.server.runner;

import io.datavines.common.utils.CommonPropertyUtils;
import io.datavines.server.coordinator.registry.Register;
import io.datavines.server.enums.CommandType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.datavines.server.coordinator.repository.service.impl.JobExternalService;
import io.datavines.server.coordinator.server.cache.TaskExecuteManager;
import io.datavines.server.utils.SpringApplicationContext;
import io.datavines.common.entity.TaskRequest;
import io.datavines.common.utils.JSONUtils;
import io.datavines.common.utils.Stopper;
import io.datavines.common.utils.ThreadUtils;
import io.datavines.server.coordinator.repository.entity.Command;
import io.datavines.server.coordinator.repository.entity.Task;

public class JobScheduler extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    private final String TASK_LOCK_KEY =
            CommonPropertyUtils.getString(CommonPropertyUtils.TASK_LOCK_KEY, CommonPropertyUtils.TASK_LOCK_KEY_DEFAULT);

    private final JobExternalService jobExternalService;

    private final TaskExecuteManager taskExecuteManager;

    private final Register register;

    public JobScheduler(TaskExecuteManager taskExecuteManager, Register register){
        this.jobExternalService = SpringApplicationContext.getBean(JobExternalService.class);
        this.taskExecuteManager = taskExecuteManager;
        this.register = register;
    }

    @Override
    public void run() {
        logger.info("job scheduler started");

        while (Stopper.isRunning()) {

            register.blockUtilAcquireLock(TASK_LOCK_KEY);
            try {
                Command command = jobExternalService.getCommand();
                if (command != null) {
                    if (CommandType.START == command.getType()) {
                        Task task = jobExternalService.executeCommand(command);

                        if (task != null) {
                            logger.info("start submit job : {} ",JSONUtils.toJsonString(task));
                            TaskRequest taskRequest = taskExecuteManager.buildTaskRequest(task);
                            taskExecuteManager.addExecuteCommand(taskRequest);
                            jobExternalService.deleteCommandById(command.getId());
                            logger.info(String.format("submit success, task : %s", task.getName()) );
                        }
                    } else if (CommandType.STOP == command.getType()) {
                        taskExecuteManager.addKillCommand(command.getTaskId());
                        jobExternalService.deleteCommandById(command.getId());
                        logger.info(String.format("kill task : %s", command.getTaskId()) );
                    }

                    ThreadUtils.sleep(1000);
                } else {
                    ThreadUtils.sleep(2000);
                }
            } catch (Exception e){
                logger.error("schedule job error ",e);
            } finally {
                register.release(TASK_LOCK_KEY);
            }
        }
    }
}