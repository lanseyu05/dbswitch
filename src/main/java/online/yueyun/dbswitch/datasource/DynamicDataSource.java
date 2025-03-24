package online.yueyun.dbswitch.datasource;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源
 */
@Slf4j
public class DynamicDataSource extends AbstractRoutingDataSource {

    /**
     * 数据源类型线程本地变量
     */
    private static final ThreadLocal<DataSourceType> DATA_SOURCE_TYPE = new ThreadLocal<>();

    /**
     * 数据源类型枚举
     */
    public enum DataSourceType {
        /**
         * 主数据源
         */
        MASTER,

        /**
         * 从数据源
         */
        SLAVE
    }

    /**
     * 设置数据源
     * 
     * @param dataSourceType 数据源类型
     */
    public static void setDataSource(DataSourceType dataSourceType) {
        log.debug("切换数据源: {}", dataSourceType);
        DATA_SOURCE_TYPE.set(dataSourceType);
    }

    /**
     * 获取当前数据源
     * 
     * @return 数据源类型
     */
    public static DataSourceType getDataSource() {
        return DATA_SOURCE_TYPE.get() == null ? DataSourceType.MASTER : DATA_SOURCE_TYPE.get();
    }

    /**
     * 清除数据源
     */
    public static void clearDataSource() {
        DATA_SOURCE_TYPE.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return getDataSource();
    }
} 