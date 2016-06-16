package action;

import java.util.LinkedHashMap;

/**
 * Action������࣬����ģ���MainXXX��ֱ�Ӽ̳и���
 * 
 * @author ������
 * @Time 2016-06-14 15:02:29
 */
public abstract class Action {
	/**
	 * ���洫���Ĳ����Ĺ�ϣ�� key��String���ͣ�Ϊ�����в��������֣� value��Object���ͣ�Ϊ������key���в����Ķ���
	 */
	private LinkedHashMap<String, Object> action = new LinkedHashMap<String, Object>();

	/**
	 * ������(key,value)��ֵ�Թ�putAction��action����ʹ��
	 * 
	 * @param key
	 *            ��action�ֶδ��������
	 * @param value
	 *            ��action�ֶδ���Ķ���
	 * @return void
	 */
	public void putAction(String key, Object value) {
		action.put(key, value);
	}

	/**
	 * ��ȡ�����key����Ӧ��value
	 * 
	 * @param key
	 *            ��ȡaction�ֶ�����Ӧ��key��ֵ�����ֶ�action���ڸ�key���򷵻ظ�key��value�����������򷵻�null
	 * @return Object ��ѯ���
	 */
	public Object getAction(String key) {
		return action.get(key);
	}

	/**
	 * �˷���Ϊ���󷽷�����̳���ʵ�֣�������ʵ��Ϊ�̳��ߴ���ҵ���߼�
	 * 
	 * @param act
	 *            ��ȡaction�ֶ�����Ӧ��key��ֵ�����ֶ�action���ڸ�key���򷵻ظ�key��value�����������򷵻�null
	 * @return Object ��ѯ���
	 */
	public abstract Object action(String act);
}
