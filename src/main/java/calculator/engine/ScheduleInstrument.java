package calculator.engine;

import graphql.ExecutionResult;
import graphql.analysis.QueryTraverser;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldCompleteParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.language.Directive;
import graphql.parser.Parser;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static calculator.CommonTools.fieldPath;
import static calculator.CommonTools.getArgumentFromDirective;
import static java.util.stream.Collectors.toList;
import static calculator.engine.CalculateDirectives.link;

public class ScheduleInstrument extends SimpleInstrumentation {

    private static final ScheduleInstrument SCHEDULE_INSTRUMENT = new ScheduleInstrument();

    public static ScheduleInstrument getScheduleInstrument() {
        return SCHEDULE_INSTRUMENT;
    }

    //  需要预分析，因为对于 person-> name @node("personName")，如果不预先分析、就不会知道person也是dag任务
    @Override
    public InstrumentationState createState(InstrumentationCreateStateParameters parameters) {
        ScheduleState state = new ScheduleState();
        String query = parameters.getExecutionInput().getQuery();

        QueryTraverser traverser = QueryTraverser.newQueryTraverser()
                .schema(parameters.getSchema())
                .document(Parser.parse(query))
                .variables(Collections.emptyMap()).build();

        StateParseVisitor visitor = StateParseVisitor.newInstanceWithState(state);
        traverser.visitDepthFirst(visitor);
        return state;
    }

    // 如果是 调度任务节点，则在完成时更新state中对应的任务状态
    // todo 这里抛出异常了可能会影响调度执行计划
    @Override
    public InstrumentationContext<ExecutionResult> beginFieldComplete(InstrumentationFieldCompleteParameters parameters) {
        return getContextOpt(parameters).orElse(super.beginFieldComplete(parameters));
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginFieldListComplete(InstrumentationFieldCompleteParameters parameters) {
        return getContextOpt(parameters).orElse(super.beginFieldListComplete(parameters));
    }

    private Optional<InstrumentationContext<ExecutionResult>> getContextOpt(InstrumentationFieldCompleteParameters parameters) {
        // 每次分析都会耗时，后续可以确认该方法是否是热点方法、提供异步分析

        ScheduleState scheduleState = parameters.getInstrumentationState();

        /**
         * 如果 state中证明、就没有node
         *
         * todo 或者node已经被标记使用、则可以不再进行如下操作了
         */
        if(scheduleState.getTaskByPath().isEmpty()){
            return Optional.empty();
        }

        String fieldPath = fieldPath(parameters.getExecutionStepInfo().getPath());

        if (scheduleState.getTaskByPath().containsKey(fieldPath)) {
            InstrumentationContext<ExecutionResult> instrumentationContext = new InstrumentationContext<ExecutionResult>() {
                @Override
                public void onDispatched(CompletableFuture<ExecutionResult> future) {
                    CompletableFuture task = scheduleState.getTaskByPath().get(fieldPath);

                    future.whenComplete((result, ex) -> {
                        if (ex != null) {
                            task.completeExceptionally(ex);
                        } else {
                            if (result.getData() != null) {
                                task.complete(result.getData());
                            } else {
                                // 对于没有结果的情况、仍然抛出异常，来终止程序运行
                                // 这里是否需要让调度器感知异常信息？不需要，包含在结果中了
                                task.completeExceptionally(new Throwable("empty result for " + fieldPath));
                            }
                        }
                    });
                }

                @Override
                public void onCompleted(ExecutionResult result, Throwable t) {
                }
            };
            return Optional.of(instrumentationContext);
        }

        return Optional.empty();
    }

    // 如果有link节点，则分析其每一个依赖的任务，并更新参数
    @Override
    public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters) {

        List<Directive> linkDirectiveList = parameters.getEnvironment().getField().getDirectives(link.getName());

        if (linkDirectiveList != null) {
            ScheduleState scheduleState = parameters.getInstrumentationState();
            Map<String, List<String>> sequenceTaskByNode = scheduleState.getSequenceTaskByNode();
            Map<String, CompletableFuture<Object>> taskByPath = scheduleState.getTaskByPath();

            DataFetchingEnvironment oldDFEnvironment = parameters.getEnvironment();
            Map<String, Object> newArguments = new HashMap<>(oldDFEnvironment.getArguments());

            for (Directive linkDir : linkDirectiveList) {
                // 获取当前依赖的任务列表
                String nodeName = getArgumentFromDirective(linkDir, "node");
                List<String> taskNameForNode = sequenceTaskByNode.get(nodeName);
                List<CompletableFuture<Object>> taskForNode = taskNameForNode.stream().map(taskByPath::get).collect(toList());

                CompletableFuture<Object> valueFuture = getValueFromTasks(taskForNode);
                if (valueFuture.isCompletedExceptionally()) {
                    // 当前逻辑是如果参数获取失败，则该数据也不再进行解析
                    return env -> null;
                } else {
                    String argumentName = getArgumentFromDirective(linkDir, "argument");
                    newArguments.put(argumentName, valueFuture.join());
                }
            }
            DataFetchingEnvironment newEnvironment = DataFetchingEnvironmentImpl
                    .newDataFetchingEnvironment(oldDFEnvironment).arguments(newArguments).build();

            return environment -> dataFetcher.get(newEnvironment);
        }

        return dataFetcher;
    }

    /**
     * get result from node task.
     * todo 抽象成公共方法。
     *
     * @param taskForNodeValue tasks which the node rely on
     * @return
     */
    private CompletableFuture<Object> getValueFromTasks(List<CompletableFuture<Object>> taskForNodeValue) {

        CompletableFuture<Object> tailNodeTask = taskForNodeValue.get(taskForNodeValue.size() - 1);

        for (CompletableFuture<Object> completableFuture : taskForNodeValue) {
            if (completableFuture.isCompletedExceptionally()) {
                completableFuture.whenComplete((ignore, ex) -> tailNodeTask.completeExceptionally(ex));
                return tailNodeTask;
            }
        }
        return tailNodeTask;
    }
}
