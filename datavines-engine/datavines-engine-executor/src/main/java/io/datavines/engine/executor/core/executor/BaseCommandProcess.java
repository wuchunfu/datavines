package io.datavines.engine.executor.core.executor;

import io.datavines.common.config.Configurations;
import io.datavines.common.config.CoreConfig;
import io.datavines.common.entity.ProcessResult;
import io.datavines.common.entity.TaskRequest;
import io.datavines.common.enums.ExecutionStatus;
import io.datavines.common.utils.LoggerUtils;
import io.datavines.common.utils.ProcessUtils;
import io.datavines.common.utils.ThreadUtils;
import io.datavines.common.utils.YarnUtils;
import io.datavines.engine.api.EngineConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public abstract class BaseCommandProcess {

    /**
     *  process
     */
    private Process process;

    /**
     *  log handler
     */
    protected Consumer<List<String>> logHandler;

    /**
     * execution job
     */
    protected TaskRequest taskRequest;

    /**
     *  logger
     */
    protected Logger logger;

    private final Configurations configurations;

    /**
     *  log list
     */
    protected final List<String> logBuffer;

    public BaseCommandProcess(Consumer<List<String>> logHandler,
                              Logger logger,
                              TaskRequest taskRequest,
                              Configurations configurations){
        this.logHandler = logHandler;
        this.taskRequest = taskRequest;
        this.logger = logger;
        this.logBuffer = Collections.synchronizedList(new ArrayList<>());
        this.configurations = configurations;
    }

    public ProcessResult run(String executeCommand){

        ProcessResult result = new ProcessResult();
        int exitStatusCode = -1;
        try{
            if (StringUtils.isEmpty(executeCommand)) {
                return result;
            }

            //构建命令行执行文件路径
            String commandFilePath = buildCommandFilePath();
            //创建命令行文件
            createCommandFileIfNotExists(executeCommand, commandFilePath);
            //构建Process实例，用于执行命令行
            buildProcess(commandFilePath);
            //进程日志写入文件
            parseProcessOutput(process);
            //通过process实例拿到pid
            int pid = getProcessId(process);

            result.setProcessId(pid);
            //设置超时时间
            int exitValue = process.waitFor();

            // set appIds
            String appId = YarnUtils.getYarnAppId(taskRequest.getTenantCode(), taskRequest.getTaskUniqueId());
            result.setApplicationId(appId);

            // if yarn job , yarn state is final state
            if (exitValue == 0){
                exitStatusCode = YarnUtils.isSuccessOfYarnState(appId) ? ExecutionStatus.SUCCESS.getCode() : ExecutionStatus.FAILURE.getCode();
            } else {
                exitStatusCode = ExecutionStatus.FAILURE.getCode();
            }

            result.setExitStatusCode(exitStatusCode);
            logger.info("process has exited, work dir:{}, pid:{} ,exitStatusCode:{}", taskRequest.getExecuteFilePath(), pid, exitStatusCode);

        } catch (InterruptedException e) {
            logger.error("interrupt exception:{0}, job may be cancelled or killed", e);
            throw new RuntimeException("interrupt exception. exitCode is :  " + exitStatusCode);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException("process error . exitCode is :  " + exitStatusCode);
        }

        return result;
    }

    /**
     * build process to execute
     * @param commandFile
     * @throws IOException
     */
    private void buildProcess(String commandFile) throws IOException{

        //init process builder
        ProcessBuilder processBuilder = new ProcessBuilder();
        // setting up a working directory
        processBuilder.directory(new File(taskRequest.getExecuteFilePath()));
        // merge error information to standard output stream
        processBuilder.redirectErrorStream(true);
        // setting up user to run commands
        List<String> command = new LinkedList<>();
        command.add("sudo");
        command.add("-u");
        command.add(taskRequest.getTenantCode());
        command.add(commandInterpreter());
        command.addAll(commandOptions());
        command.add(commandFile);
        processBuilder.command(command);

        process = processBuilder.start();

        // print command
        printCommand(processBuilder);

    }

    /**
     * get the process id
     * @param process process
     * @return processId
     */
    private int getProcessId(Process process){
        int processId = 0;

        try{
            Field field = process.getClass().getDeclaredField(EngineConstants.PID);
            field.setAccessible(true);
            processId = field.getInt(process);
        }catch (Throwable e){
            logger.error(e.getMessage(),e);
        }

        return processId;
    }

    /**
     * print command
     * @param processBuilder process builder
     */
    private void printCommand(ProcessBuilder processBuilder) {
        String cmdStr;

        try {
            cmdStr = ProcessUtils.buildCommandStr(processBuilder.command());
            logger.info("job run command:\n{}", cmdStr);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void cancel(){
        if (process == null) {
            return;
        }

        int pid = getProcessId(process);

        logger.info("cancel process {}",pid);

        if (!softKill(pid)) {
            hardKill(pid);
            process.destroy();
            process = null;
        }

        clearLog();
    }

    /**
     * clear
     */
    private void clearLog() {
        if (!logBuffer.isEmpty()) {
            // log handle
            logHandler.accept(logBuffer);
            logBuffer.clear();
        }
    }

    /**
     * soft kill
     * @param processId processId
     * @return boolean
     */
    private boolean softKill(int processId) {
        if (processId != 0 && process.isAlive()) {
            try {
                // sudo -u user command to run command
                String cmd = String.format("sudo kill %d", processId);

                logger.info("soft kill job:{}, process id:{}, cmd:{}", taskRequest.getTaskName(), processId, cmd);

                Runtime.getRuntime().exec(cmd);
            } catch (IOException e) {
                logger.info("kill attempt failed." + e.getMessage(), e);
                return false;
            }
        }

        return !process.isAlive();
    }

    /**
     * hard kill
     * @param processId process id
     */
    private void hardKill(int processId) {
        if (processId != 0 && process.isAlive()) {
            try {
                String cmd = String.format("sudo kill -9 %d", processId);

                logger.info("hard kill job:{}, process id:{}, cmd:{}", taskRequest.getTaskName(), processId, cmd);

                Runtime.getRuntime().exec(cmd);
            } catch (IOException e) {
                logger.error("kill attempt failed." + e.getMessage(), e);
            }
        }
    }

    /**
     * get the standard output of the process
     * @param process process
     */
    private void parseProcessOutput(Process process) {
        String threadLoggerInfoName = String.format(LoggerUtils.TASK_LOGGER_THREAD_NAME + "-%s", taskRequest.getTaskName());
        ExecutorService parseProcessOutputExecutorService = ThreadUtils.newDaemonSingleThreadExecutor(threadLoggerInfoName);
        parseProcessOutputExecutorService.submit(new Runnable(){
            @Override
            public void run() {
                BufferedReader inReader = null;

                try {
                    inReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;

                    long lastFlushTime = System.currentTimeMillis();

                    while ((line = inReader.readLine()) != null) {
                        logBuffer.add(line);
                        lastFlushTime = flush(lastFlushTime);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(),e);
                } finally {
                    clear();
                    close(inReader);
                }
            }
        });
        parseProcessOutputExecutorService.shutdown();
    }

    private long flush(long lastFlushTime) {
        long now = System.currentTimeMillis();

        //when log buffer siz or flush time reach condition , then flush
        if (logBuffer.size() >=
                this.configurations.getInt(CoreConfig.LOG_CACHE_ROW_NUM,CoreConfig.LOG_CACHE_ROW_NUM_DEFAULT_VALUE)
                || now - lastFlushTime > this.configurations.getInt(CoreConfig.LOG_FLUSH_INTERVAL,CoreConfig.LOG_FLUSH_INTERVAL_DEFAULT_VALUE)) {
            lastFlushTime = now;
            logHandler.accept(logBuffer);
            logBuffer.clear();
        }

        return lastFlushTime;
    }

    /**
     * clear
     */
    private void clear() {
        if (!logBuffer.isEmpty()) {
            logHandler.accept(logBuffer);
            logBuffer.clear();
        }
    }

    /**
     * close buffer reader
     * @param inReader in reader
     */
    private void close(BufferedReader inReader) {
        if (inReader != null) {
            try {
                inReader.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


    protected List<String> commandOptions() {
        return Collections.emptyList();
    }

    /**
     * command interpreter
     * @return String
     */
    protected abstract String commandInterpreter();

    /**
     * build command file path
     * @return String
     */
    protected abstract String buildCommandFilePath();

    /**
     * create command file if not exists
     * @throws IOException
     */
    protected abstract void createCommandFileIfNotExists(String execCommand, String commandFile) throws IOException;

}