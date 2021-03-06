package com.tqmall.search.commons.utils;

import com.tqmall.search.commons.lang.Defaultable;
import com.tqmall.search.commons.lang.SmallDateFormat;
import com.tqmall.search.commons.lang.StrValueConvert;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xing on 15/9/30.
 * string value transfer builder
 */
public final class StrValueConverts {

    private StrValueConverts() {
    }

    /**
     * 获取指定class的{@link StrValueConvert}实例, 如果内部没有实现返回null
     * 具体实现的类型有:
     * {@link Integer}, {@link Boolean}, {@link Double}, {@link Long}, {@link BigInteger},
     * {@link BigDecimal}, {@link Date}(默认格式: yyyy-MM-dd HH:mm:ss), {@link String}
     * <p/>
     * {@link Float}, 类型是故意整没的~~~~~~
     *
     * @param cls 对应类型的class对象
     * @param <T> 对应泛型
     * @return 内部没有实现返回null
     * @see IntStrValueConvert
     * @see BoolStrValueConvert
     * @see StringStrValueConvert
     * @see LongStrValueConvert
     * @see BigIntegerStrValueConvert
     * @see BigDecimalStrValueConvert
     * @see DateStrValueConvert
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T> StrValueConvert<T> getConvert(Class<T> cls) {
        StrValueConvert ret;
        if (cls == Integer.class || cls == Integer.TYPE) {
            ret = IntStrValueConvert.INSTANCE;
        } else if (cls == Boolean.class || cls == Boolean.TYPE) {
            ret = BoolStrValueConvert.INSTANCE;
        } else if (cls == Long.class || cls == Long.TYPE) {
            ret = LongStrValueConvert.INSTANCE;
        } else if (cls == Double.class || cls == Double.TYPE) {
            ret = DoubleStrValueConvert.INSTANCE;
        } else if (cls == Float.class || cls == Float.TYPE) {
            ret = FloatStrValueConvert.INSTANCE;
        } else if (cls == BigInteger.class) {
            ret = BigIntegerStrValueConvert.INSTANCE;
        } else if (cls == BigDecimal.class) {
            ret = BigDecimalStrValueConvert.INSTANCE;
        } else if (cls == Date.class) {
            ret = DateStrValueConvert.INSTANCE;
        } else if (cls == String.class) {
            ret = StringStrValueConvert.INSTANCE;
        } else {
            return null;
        }
        return ret;
    }

    /**
     * 获取基本的数据类型的{@link StrValueConvert}, 没有则抛出{@link IllegalArgumentException}
     *
     * @see #getConvert(Class)
     */
    public static <T> StrValueConvert<T> getBasicConvert(Class<T> cls) {
        StrValueConvert<T> convert = getConvert(cls);
        if (convert == null) {
            throw new IllegalArgumentException("can not find basic StrValueConvert instance of " + cls);
        }
        return convert;
    }

    public static <T> StrValueConvert<T> getBasicConvert(Class<T> cls, T defaultValue) {
        return wrapDefaultConvert(getBasicConvert(cls), defaultValue);
    }

    public static <T> StrValueConvert<T> getConvert(Class<T> cls, T defaultValue) {
        final StrValueConvert<T> convert = getConvert(cls);
        if (convert == null) return null;
        return wrapDefaultConvert(convert, defaultValue);
    }

    public static <T> StrValueConvert<T> wrapDefaultConvert(final StrValueConvert<T> convert, final T defaultValue) {
        if (defaultValue != null && convert instanceof Defaultable) {
            Defaultable defaultable = (Defaultable) convert;
            if (defaultValue.equals(defaultable.defaultValue())) return convert;
        }
        return new AbstractDefaultableStrValueConvert<T>() {

            @Override
            protected T innerConvert(String str) {
                return convert.convert(str);
            }

            @Override
            public T defaultValue() {
                return defaultValue;
            }
        };
    }

    public static boolean boolConvert(String input) {
        return BoolStrValueConvert.INSTANCE.convert(input);
    }
    /**
     * @return have error will return {@link IntStrValueConvert#defaultValue()}
     */
    public static int intConvert(String input) {
        return IntStrValueConvert.INSTANCE.convert(input);
    }

    public static int intConvert(String input, int defaultValue) {
        return convert(input, defaultValue, Integer.TYPE);
    }

    /**
     * @return have error will return {@link LongStrValueConvert#defaultValue()}
     */
    public static long longConvert(String input) {
        return LongStrValueConvert.INSTANCE.convert(input);
    }

    public static long longConvert(String input, long defaultValue) {
        return convert(input, defaultValue, Long.TYPE);
    }

    /**
     * @return have error will return {@link DoubleStrValueConvert#defaultValue()}
     */
    public static double doubleConvert(String input) {
        return DoubleStrValueConvert.INSTANCE.convert(input);
    }

    public static double doubleConvert(String input, double defaultValue) {
        return convert(input, defaultValue, Double.TYPE);
    }
    /**
     * @return have error will return {@link FloatStrValueConvert#defaultValue()}
     */
    public static float floatConvert(String input) {
        return FloatStrValueConvert.INSTANCE.convert(input);
    }

    public static float floatConvert(String input, float defaultValue) {
        return convert(input, defaultValue, Float.TYPE);
    }

    /**
     * @return can not null, have error will return {@link BigDecimalStrValueConvert#defaultValue()}
     */
    public static BigDecimal bigDecimalConvert(String input) {
        return BigDecimalStrValueConvert.INSTANCE.convert(input);
    }

    public static BigDecimal bigDecimalConvert(String input, BigDecimal defaultValue) {
        return convert(input, defaultValue, BigDecimal.class);
    }

    /**
     * String类型的时间转换为{@link Date}对象
     *
     * @param input 时间字符串格式yyyy-MM-dd HH:mm:ss
     */
    public static Date dateConvert(String input) {
        return DateStrValueConvert.INSTANCE.convert(input);
    }

    public static String dateFormat(Date date) {
        return DateStrValueConvert.INSTANCE.format(date);
    }

    public static <T extends Comparable<T>> T convert(final String input, final T defaultValue, final Class<T> cls) {
        StrValueConvert<T> convert = getConvert(cls, defaultValue);
        if (convert == null) {
            throw new IllegalArgumentException("class: " + cls + " StrValueConvert is unsupported");
        }
        return convert.convert(input);
    }

    public static abstract class AbstractDefaultableStrValueConvert<T>
            implements StrValueConvert<T>, Defaultable<T> {

        protected abstract T innerConvert(String str);

        @Override
        final public T convert(String str) {
            if (str == null || str.isEmpty()) return defaultValue();
            try {
                return innerConvert(str);
            } catch (NumberFormatException e) {
                return defaultValue();
            }
        }
    }

    static class StringStrValueConvert extends AbstractDefaultableStrValueConvert<String> {

        final static StringStrValueConvert INSTANCE = new StringStrValueConvert();

        @Override
        protected String innerConvert(String str) {
            return str;
        }

        @Override
        public String defaultValue() {
            return null;
        }
    }

    static class BoolStrValueConvert extends AbstractDefaultableStrValueConvert<Boolean> {

        final static BoolStrValueConvert INSTANCE = new BoolStrValueConvert();

        @Override
        protected Boolean innerConvert(String str) {
            return str.equals("1") || str.equalsIgnoreCase("true") || str.equalsIgnoreCase("on") ||
                    str.equalsIgnoreCase("yes") || str.equalsIgnoreCase("y") || str.equalsIgnoreCase("ok");
        }

        @Override
        public Boolean defaultValue() {
            return Boolean.FALSE;
        }
    }

    static class IntStrValueConvert extends AbstractDefaultableStrValueConvert<Integer> {

        final static IntStrValueConvert INSTANCE = new IntStrValueConvert();

        @Override
        protected Integer innerConvert(String str) {
            return Integer.parseInt(str, 10);
        }

        @Override
        public Integer defaultValue() {
            return 0;
        }

    }

    static class LongStrValueConvert extends AbstractDefaultableStrValueConvert<Long> {

        final static LongStrValueConvert INSTANCE = new LongStrValueConvert();

        @Override
        protected Long innerConvert(String str) {
            return Long.parseLong(str, 10);
        }

        @Override
        public Long defaultValue() {
            return 0L;
        }

    }

    static class FloatStrValueConvert extends  AbstractDefaultableStrValueConvert<Float> {

        final static FloatStrValueConvert INSTANCE = new FloatStrValueConvert();

        final static Float DEFAULT_VALUE = 0.0F;
        @Override
        public Float defaultValue() {
            return DEFAULT_VALUE;
        }

        @Override
        protected Float innerConvert(String str) {
            return Float.valueOf(str);
        }
    }

    static class DoubleStrValueConvert extends AbstractDefaultableStrValueConvert<Double> {

        final static DoubleStrValueConvert INSTANCE = new DoubleStrValueConvert();

        final static Double DEFAULT_VALUE = 0.0D;

        @Override
        protected Double innerConvert(String str) {
            return Double.valueOf(str);
        }

        @Override
        public Double defaultValue() {
            return DEFAULT_VALUE;
        }
    }

    static class BigIntegerStrValueConvert extends AbstractDefaultableStrValueConvert<BigInteger> {

        final static BigIntegerStrValueConvert INSTANCE = new BigIntegerStrValueConvert();

        @Override
        protected BigInteger innerConvert(String str) {
            return new BigInteger(str);
        }

        @Override
        public BigInteger defaultValue() {
            return BigInteger.ZERO;
        }
    }

    static class BigDecimalStrValueConvert extends AbstractDefaultableStrValueConvert<BigDecimal> {

        final static BigDecimalStrValueConvert INSTANCE = new BigDecimalStrValueConvert();

        @Override
        protected BigDecimal innerConvert(String str) {
            return new BigDecimal(str);
        }

        @Override
        public BigDecimal defaultValue() {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 使用JDK 自代的{@link SimpleDateFormat}实现, 通过{@link #dateFormats}做缓存, 效率一般
     * 对于StrValueConvert<Date>, 模块commons-component中class: com.tqmall.search.commons.utils.DateStrValueConvert是更好的实现,
     * 其内部通过Apache commons组件中的FastDateFormat实现, 效率更好~~~建议使用com.tqmall.search.commons.utils.DateStrValueConvert, 如果有的话
     *
     * @see CommonsUtils#dateFormat()
     * @see SmallDateFormat
     */
    static class DateStrValueConvert implements SmallDateFormat {

        final static SmallDateFormat INSTANCE;

        static {
            SmallDateFormat convert = getUtilsDateStrValueConvert();
            INSTANCE = convert == null ? new DateStrValueConvert() : convert;
        }

        /**
         * 获取com.tqmall.search.commons.utils.DateStrValueConvert中的INSTANCE变量, 如果拿不到返回null
         *
         * @return com.tqmall.search.commons.utils.DateStrValueConvert 不存在返回null
         */
        @SuppressWarnings({"rawtypes", "unchecked"})
        private static SmallDateFormat getUtilsDateStrValueConvert() {
            try {
                Class dataConvertCls = Class.forName("com.tqmall.search.commons.utils.DateStrValueConvert");
                Field field = dataConvertCls.getField("INSTANCE");
                if (field != null) {
                    return (SmallDateFormat) field.get(null);
                }
            } catch (Throwable ignored) {
            }
            return null;
        }


        private ThreadLocal<DateFormat> dateFormats = new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            }
        };

        @Override
        public Date convert(String str) {
            if (str == null) return null;
            try {
                return dateFormats.get().parse(str);
            } catch (ParseException e) {
                return null;
            }
        }

        @Override
        public String format(Date date) {
            return dateFormats.get().format(date);
        }
    }
}
