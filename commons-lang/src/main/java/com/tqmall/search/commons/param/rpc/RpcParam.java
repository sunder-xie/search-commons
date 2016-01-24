package com.tqmall.search.commons.param.rpc;

import com.tqmall.search.commons.param.Param;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing on 16/1/24.
 * http调用之外的Rpc调用使用的公共参数类, 比如Dubbo等
 * 该类可以说是一个Build类
 */
public class RpcParam extends Param {

    private static final long serialVersionUID = -4627652456092763293L;

    private ConditionContainer must;

    private ConditionContainer should;

    /**
     * {@link #should} 的最小的匹配条件数目, 当然{@link #should}有值才会有效
     */
    private int minimumShouldMatch = 1;

    private ConditionContainer mustNot;

    private List<SortCondition> sort;

    public RpcParam() {
    }

    public RpcParam(Param param) {
        setSource(param.getSource());
        setUid(param.getUid());
    }

    /**
     * 如果原先已经添加过SortCondition, 则追加
     */
    public RpcParam sort(String sortStr) {
        List<SortCondition> list = SortCondition.build(sortStr);
        if (list != null) {
            for (SortCondition c : list) {
                sort(c);
            }
        }
        return this;
    }

    public RpcParam sort(SortCondition sortCondition) {
        if (sort == null) {
            sort = new ArrayList<>();
        }
        sort.add(sortCondition);
        return this;
    }

    public RpcParam must(Condition condition) {
        if (must == null) {
            must = new ConditionContainer();
        }
        must.addCondition(condition);
        return this;
    }

    public RpcParam should(Condition condition) {
        if (should == null) {
            should = new ConditionContainer();
        }
        should.addCondition(condition);
        return this;
    }

    public RpcParam mustNot(Condition condition) {
        if (mustNot == null) {
            mustNot = new ConditionContainer();
        }
        mustNot.addCondition(condition);
        return this;
    }

    public void setMinimumShouldMatch(int minimumShouldMatch) {
        this.minimumShouldMatch = minimumShouldMatch;
    }

    public int getMinimumShouldMatch() {
        return minimumShouldMatch;
    }

    public ConditionContainer getMust() {
        return must;
    }

    public ConditionContainer getMustNot() {
        return mustNot;
    }

    public ConditionContainer getShould() {
        return should;
    }

    public List<SortCondition> getSort() {
        return sort;
    }
}