package com.tqmall.search.common.param;

import com.tqmall.search.common.utils.ClientConst;
import com.tqmall.search.common.utils.ClientStringUtils;
import com.tqmall.search.common.utils.StrValueConvert;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by xing on 16/1/22.
 * 区间选择参数封装
 */
public class RangeFilter<T> implements Serializable {

    private static final long serialVersionUID = 8024467420617760387L;

    private T start;

    private T end;

    private String field;

    public RangeFilter(T start, T end, String field) {
        Objects.requireNonNull(field);
        this.start = start;
        this.end = end;
        this.field = field;
    }

    public T getEnd() {
        return end;
    }

    public String getField() {
        return field;
    }

    public T getStart() {
        return start;
    }

    public static <T> Builder<T> build(String field) {
        return new Builder<>(field);
    }

    public static class Builder<T> {

        private T start, end;

        private String field;

        public Builder(String field) {
            this.field = field;
        }

        public Builder start(T start) {
            this.start = start;
            return this;
        }

        public Builder end(T end) {
            this.end = end;
            return this;
        }

        public RangeFilter<T> create() {
            return new RangeFilter<>(start, end, field);
        }

    }

    /**
     * @param field           range的字段, 不能为Null
     * @param rangeStr        range区间字符串, 如果isEmpty, return null.
     * @param strValueConvert 值转化器
     * @param <T>             对应类型
     * @return 构造好的RangeFilter对象
     */
    public static <T> RangeFilter<T> build(final String field, final String rangeStr, final StrValueConvert<T> strValueConvert) {
        if (rangeStr == null || rangeStr.isEmpty()) return null;
        String[] rangeArray = ClientStringUtils.split(rangeStr, ClientConst.RANGE_FILTER_CHAR);
        T start = null, end = null;
        if (rangeArray.length == 1) {
            if (rangeStr.charAt(0) == ClientConst.RANGE_FILTER_CHAR) {
                end = strValueConvert.convert(rangeArray[0]);
            } else {
                start = strValueConvert.convert(rangeArray[0]);
            }
        } else {
            start = strValueConvert.convert(rangeArray[0]);
            end = strValueConvert.convert(rangeArray[0]);
        }
        return new RangeFilter<>(start, end, field);
    }

}
