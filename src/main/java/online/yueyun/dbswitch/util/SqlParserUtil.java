package online.yueyun.dbswitch.util;

import lombok.extern.slf4j.Slf4j;
import online.yueyun.dbswitch.enums.OperationType;

/**
 * SQL解析工具类
 */
@Slf4j
public class SqlParserUtil {

    /**
     * 解析操作类型
     *
     * @param methodName Mapper方法名
     * @param sql SQL语句
     * @return 操作类型
     */
    public static OperationType parseOperationType(String methodName, String sql) {
        if (sql == null || sql.isEmpty()) {
            return parseOperationTypeByMethodName(methodName);
        }

        String upperSql = sql.trim().toUpperCase();
        
        if (upperSql.startsWith("SELECT")) {
            return OperationType.SELECT;
        } else if (upperSql.startsWith("INSERT")) {
            return OperationType.INSERT;
        } else if (upperSql.startsWith("UPDATE")) {
            return OperationType.UPDATE;
        } else if (upperSql.startsWith("DELETE")) {
            return OperationType.DELETE;
        } else {
            log.warn("无法解析SQL操作类型，SQL: {}", sql);
            return parseOperationTypeByMethodName(methodName);
        }
    }

    /**
     * 通过方法名解析操作类型
     *
     * @param methodName Mapper方法名
     * @return 操作类型
     */
    public static OperationType parseOperationTypeByMethodName(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return OperationType.UNKNOWN;
        }

        String lowerMethodName = methodName.toLowerCase();
        
        if (lowerMethodName.startsWith("select") || 
            lowerMethodName.startsWith("get") || 
            lowerMethodName.startsWith("find") || 
            lowerMethodName.startsWith("query") || 
            lowerMethodName.startsWith("count") || 
            lowerMethodName.startsWith("list")) {
            return OperationType.SELECT;
        } else if (lowerMethodName.startsWith("insert") || 
                   lowerMethodName.startsWith("add") || 
                   lowerMethodName.startsWith("save") || 
                   lowerMethodName.startsWith("create")) {
            return OperationType.INSERT;
        } else if (lowerMethodName.startsWith("update") || 
                   lowerMethodName.startsWith("modify") || 
                   lowerMethodName.startsWith("edit")) {
            return OperationType.UPDATE;
        } else if (lowerMethodName.startsWith("delete") || 
                   lowerMethodName.startsWith("remove")) {
            return OperationType.DELETE;
        } else {
            log.warn("无法通过方法名解析操作类型，方法名: {}", methodName);
            return OperationType.UNKNOWN;
        }
    }
} 