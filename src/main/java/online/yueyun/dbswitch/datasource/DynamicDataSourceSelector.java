package online.yueyun.dbswitch.datasource;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.enums.OperationType;
import online.yueyun.dbswitch.enums.WriteMode;
import online.yueyun.dbswitch.service.WriteModeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 动态数据源选择器
 */
@Slf4j
@Component
public class DynamicDataSourceSelector {

    @Autowired
    private WriteModeService writeModeService;

    /**
     * 为各类操作选择数据源
     *
     * @param operationType 操作类型
     * @return 是否使用主数据源
     */
    public boolean useMasterDataSource(OperationType operationType) {
        // 获取当前写入模式
        WriteMode currentMode = writeModeService.getCurrentWriteMode();
        log.debug("当前写入模式: {}, 操作类型: {}", currentMode, operationType);

        // 根据当前模式选择数据源
        switch (currentMode) {
            case MASTER_ONLY:
                // 主库模式下所有操作都使用主库
                return true;
            case SLAVE_ONLY:
                // 从库模式下所有操作都使用从库
                return false;
            case MASTER_SLAVE:
                // 主从模式下所有操作使用主库
                return true;
            case SLAVE_MASTER:
                // 从主模式下所有操作使用从库
                return false;
            default:
                log.warn("未知的写入模式: {}, 默认使用主库", currentMode);
                return true;
        }
    }
    
    /**
     * 判断是否需要双写
     *
     * @param operationType 操作类型
     * @return 是否需要双写
     */
    public boolean needDualWrite(OperationType operationType) {
        // 只有写操作才可能需要双写
        if (operationType == OperationType.SELECT) {
            return false;
        }

        // 根据当前写入模式决定是否需要双写
        WriteMode currentMode = writeModeService.getCurrentWriteMode();
        return currentMode == WriteMode.MASTER_SLAVE || currentMode == WriteMode.SLAVE_MASTER;
    }

    /**
     * 获取第二个数据源（用于双写）
     * 
     * @return 是否使用主数据源作为第二个数据源
     */
    public boolean secondWriteInMaster() {
        WriteMode currentMode = writeModeService.getCurrentWriteMode();
        
        // 在双写场景中判断第二个数据源
        switch (currentMode) {
            case MASTER_SLAVE:
                // 主从模式下，第二个操作在从库
                return false;
            case SLAVE_MASTER:
                // 从主模式下，第二个操作在主库
                return true;
            default:
                log.warn("非双写模式下调用secondWriteInMaster(): {}", currentMode);
                return !useMasterDataSource(null); // 返回与第一个数据源相反的选择
        }
    }
} 