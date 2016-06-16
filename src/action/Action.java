package action;

import java.util.LinkedHashMap;

/**
 * Action抽象基类，所有模块的MainXXX的直接继承父类
 * 
 * @author 杨埔生
 * @Time 2016-06-14 15:02:29
 */
public abstract class Action {
	/**
	 * 保存传进的操作的哈希表， key（String类型）为欲进行操作的名字， value（Object类型）为欲根据key进行操作的对象
	 */
	private LinkedHashMap<String, Object> action = new LinkedHashMap<String, Object>();

	/**
	 * 保存新(key,value)键值对供putAction与action方法使用
	 * 
	 * @param key
	 *            向action字段传入的名字
	 * @param value
	 *            向action字段传入的对象
	 * @return void
	 */
	public void putAction(String key, Object value) {
		action.put(key, value);
	}

	/**
	 * 获取传入的key所对应的value
	 * 
	 * @param key
	 *            获取action字段所对应该key的值，若字段action存在该key，则返回该key的value，若不存在则返回null
	 * @return Object 查询结果
	 */
	public Object getAction(String key) {
		return action.get(key);
	}

	/**
	 * 此方法为抽象方法，需继承者实现，期望的实现为继承者处理业务逻辑
	 * 
	 * @param act
	 *            获取action字段所对应该key的值，若字段action存在该key，则返回该key的value，若不存在则返回null
	 * @return Object 查询结果
	 */
	public abstract Object action(String act);
}
