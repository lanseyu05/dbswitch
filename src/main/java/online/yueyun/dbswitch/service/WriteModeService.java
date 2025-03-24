package online.yueyun.dbswitch.service;

import online.yueyun.dbswitch.enums.WriteMode;

/**
 * 写入模式服务接口
 */
public interface WriteModeService {

    /**
     * 获取当前写入模式
     *
     * @return 写入模式
     */
    WriteMode getCurrentWriteMode();

    /**
     * 更新写入模式
     *
     * @param writeMode 写入模式
     */
    void updateWriteMode(WriteMode writeMode);
} 