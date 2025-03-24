package online.yueyun.dbswitch.enums;

/**
 * 数据库写入模式枚举
 */
public enum WriteMode {
    /**
     * 仅写主库
     */
    MASTER_ONLY,
    
    /**
     * 先写主库，后写从库
     */
    MASTER_SLAVE,
    
    /**
     * 先写从库，后写主库
     */
    SLAVE_MASTER,
    
    /**
     * 仅写从库
     */
    SLAVE_ONLY
} 