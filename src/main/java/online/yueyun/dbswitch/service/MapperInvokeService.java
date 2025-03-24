package online.yueyun.dbswitch.service;

/**
 * Mapper调用服务接口
 */
public interface MapperInvokeService {

    /**
     * 调用Mapper方法
     *
     * @param mapperClassName Mapper全类名
     * @param methodName 方法名
     * @param parameterTypeNames 参数类型名称数组
     * @param args 参数数组
     * @return 调用结果
     * @throws Exception 调用异常
     */
    Object invoke(String mapperClassName, String methodName, String[] parameterTypeNames, Object[] args) throws Exception;
    
    /**
     * 调用Mapper方法，指定使用的数据源
     * 
     * @param mapperClassName Mapper全类名
     * @param methodName 方法名
     * @param parameterTypeNames 参数类型名称数组
     * @param args 参数数组
     * @param useMaster 是否使用主数据源
     * @return 方法执行结果
     */
    Object invoke(String mapperClassName, String methodName, String[] parameterTypeNames, Object[] args, boolean useMaster) throws Exception;
} 