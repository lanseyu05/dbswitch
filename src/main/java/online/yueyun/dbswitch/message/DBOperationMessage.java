package online.yueyun.dbswitch.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.yueyun.dbswitch.enums.OperationType;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据库操作消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DBOperationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID，用于幂等性处理
     */
    private String messageId;

    /**
     * Mapper全类名
     */
    private String mapperClassName;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 方法参数类型
     */
    private String[] parameterTypes;

    /**
     * 方法参数
     */
    private Object[] args;

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 执行是否成功
     */
    private Boolean success;
    
    /**
     * 是否使用主库作为第二个数据源
     * true - 使用主库
     * false - 使用从库
     */
    private Boolean useSecondMaster;
} 