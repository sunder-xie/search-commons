package com.tqmall.search.commons.param.rpc;

/**
 * Created by xing on 16/1/24.
 * 等值比较条件
 */
public class EqualCondition<T> extends Condition {

    private static final long serialVersionUID = 4180473296298181745L;

    private final T value;

    public EqualCondition(String field, T value) {
        super(field);
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "EqualCondition{" + super.toString() + ", value = " + value;
    }
    /**
     * 如果value无效, 返回null
     */
    public static <T> EqualCondition<T> build(String field, T value) {
        if (value == null) return null;
        else return new EqualCondition<>(field, value);
    }
}
