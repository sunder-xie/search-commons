package com.tqmall.search.canal.handle;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.tqmall.search.canal.RowChangedData;
import com.tqmall.search.canal.action.EventTypeAction;
import com.tqmall.search.canal.action.SchemaTables;
import com.tqmall.search.canal.action.TableColumnCondition;
import com.tqmall.search.commons.lang.Function;

import java.net.SocketAddress;
import java.util.*;

/**
 * Created by xing on 16/2/23.
 * 基于表级别, 每个事件如果
 * 连续的事件更新, 发现不同schema, table, eventType 则处理掉
 *
 * @see #runRowChangeAction()
 * @see EventTypeAction
 */
public class EventTypeSectionHandle extends ActionableInstanceHandle<EventTypeAction> {
    /**
     * 最近处理的schema
     * 只能canal获取数据的线程访问, 线程不安全的
     */
    private String lastSchema;
    /**
     * 最近处理的table
     * 只能canal获取数据的线程访问, 线程不安全的
     */
    private String lastTable;
    /**
     * 最近处理的tableEvent
     * 只能canal获取数据的线程访问, 线程不安全的
     */
    private CanalEntry.EventType lastEventType;

    /**
     * 待处理数据集合列表
     * 只能canal获取数据的线程访问, 线程不安全的
     */
    private final List<RowChangedData> rowChangedDataList = new LinkedList<>();

    /**
     * @param address     canal服务器地址
     * @param destination canal实例名称
     */
    public EventTypeSectionHandle(SocketAddress address, String destination, SchemaTables<EventTypeAction> schemaTables) {
        super(address, destination, schemaTables);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void runEventTypeOfAction(int eventType, List<? extends RowChangedData> dataList) {
        if (eventType == CanalEntry.EventType.UPDATE_VALUE) {
            currentSchemaTable.getAction().onUpdateAction(Collections.unmodifiableList((List<RowChangedData.Update>) dataList));
        } else if (eventType == CanalEntry.EventType.INSERT_VALUE) {
            currentSchemaTable.getAction().onInsertAction(Collections.unmodifiableList((List<RowChangedData.Insert>) dataList));
        } else {
            currentSchemaTable.getAction().onDeleteAction(Collections.unmodifiableList((List<RowChangedData.Delete>) dataList));
        }
        //这儿清楚掉
        dataList.clear();
    }

    /**
     * UPDATE 事件, 执行条件过滤, 对于多条更新记录, 由于条件过滤, UPDATE事件可能想DELETE, INSERT转换, 这样将本来只需要一次调用
     * {@link EventTypeAction#onUpdateAction(List)}, 由于{@link RowChangedData.Update}转换, 分隔成多个List, 调用多次各自事
     * 件处理方法
     */
    private void runRowChangeAction() {
        if (rowChangedDataList.isEmpty()) return;
        TableColumnCondition columnCondition;
        if (lastEventType == CanalEntry.EventType.UPDATE && (columnCondition = currentSchemaTable.getColumnCondition()) != null) {
            ListIterator<RowChangedData> it = rowChangedDataList.listIterator();
            Function<String, String> beforeFunction = UpdateDataFunction.before();
            Function<String, String> afterFunction = UpdateDataFunction.after();
            try {
                int lastType = -1, i = 0;
                while (it.hasNext()) {
                    RowChangedData.Update update = (RowChangedData.Update) it.next();
                    UpdateDataFunction.setUpdateData(update);
                    boolean beforeInvalid = !columnCondition.validation(beforeFunction);
                    boolean afterInvalid = !columnCondition.validation(afterFunction);
                    int curType;
                    if (beforeInvalid && afterInvalid) {
                        //没有数据, 删除
                        it.remove();
                        continue;
                    } else if (beforeInvalid) {
                        it.set(update.transferToInsert());
                        curType = CanalEntry.EventType.INSERT_VALUE;
                    } else if (afterInvalid) {
                        it.set(update.transferToDelete());
                        curType = CanalEntry.EventType.DELETE_VALUE;
                    } else {
                        curType = CanalEntry.EventType.UPDATE_VALUE;
                    }
                    i++;
                    if (lastType == -1) {
                        lastType = curType;
                    } else if (lastType != curType) {
                        runEventTypeOfAction(lastType, rowChangedDataList.subList(0, i));
                        //从头开始
                        it = rowChangedDataList.listIterator();
                        i = 0;
                        lastType = curType;
                    }
                }
                if (i > 0) {
                    runEventTypeOfAction(lastType, rowChangedDataList);
                }
            } finally {
                //要记得清楚掉, 避免内存泄露
                UpdateDataFunction.setUpdateData(null);
            }
        } else {
            runEventTypeOfAction(currentEventType.getNumber(), rowChangedDataList);
        }
    }

    @Override
    protected void doRowChangeHandle(List<RowChangedData> changedData) {
        //尽量集中处理
        if (!currentHandleTable.equals(lastTable) || !currentEventType.equals(lastEventType)
                || !currentHandleSchema.equals(lastSchema)) {
            runRowChangeAction();
            lastSchema = currentHandleSchema;
            lastTable = currentHandleTable;
            lastEventType = currentEventType;
        }
        rowChangedDataList.addAll(changedData);
    }

    /**
     * 如果出现异常, 可以肯定方法{@link #runRowChangeAction()}至少调用过一次, 那么对应的{@link #lastSchema}, {@link #lastTable},
     * {@link #lastEventType} 需要更新
     *
     * @param exception      具体异常
     * @param inFinishHandle 标识是否在{@link #doFinishHandle()}中产生的异常
     * @return 是否忽略异常
     */
    @Override
    protected boolean exceptionHandle(RuntimeException exception, boolean inFinishHandle) {
        if (super.exceptionHandle(exception, inFinishHandle)) {
            lastSchema = currentHandleSchema;
            lastTable = currentHandleTable;
            lastEventType = currentEventType;
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void doFinishHandle() {
        try {
            runRowChangeAction();
        } finally {
            if (!rowChangedDataList.isEmpty()) {
                rowChangedDataList.clear();
            }
        }
    }

    @Override
    protected HandleExceptionContext buildHandleExceptionContext(RuntimeException exception) {
        return HandleExceptionContext.build(exception)
                .schema(lastSchema)
                .table(lastTable)
                .eventType(lastEventType)
                .changedData(rowChangedDataList)
                .create();
    }

}
