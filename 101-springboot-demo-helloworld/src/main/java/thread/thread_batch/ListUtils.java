package thread.thread_batch;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {


    /**
     * 将列表按指定大小分割成多个子列表
     *
     * @param <T>      列表元素类型
     * @param list     需要分割的原始列表
     * @param pageSize 每个子列表的最大元素数量
     * @return 包含多个子列表的列表
     */
    public static  <T> List<List<T>> splitList(List<T> list, int pageSize) {
        // 获取原始列表的大小
        int listSize = list.size();
        // 计算需要分成多少页（向上取整）
        int page = (listSize + (pageSize - 1)) / pageSize;
        // 创建用于存储所有子列表的列表
        List<List<T>> listArray = new ArrayList<List<T>>();
        
        // 遍历每一页
        for (int i = 0; i < page; i++) {
            // 创建当前页的子列表
            List<T> subList = new ArrayList<T>();
            // 遍历原始列表中的每个元素
            for (int j = 0; j < listSize; j++) {
                // 计算当前元素应该属于第几页
                int pageIndex = ((j + 1) + (pageSize - 1)) / pageSize;
                // 如果当前元素属于当前页，则添加到子列表中
                if (pageIndex == (i + 1)) {
                    subList.add(list.get(j));
                }
                // 当达到页面大小时跳出内层循环（此条件判断逻辑存在问题）
                if ((j + 1) == ((j + 1) * pageSize)) {
                    break;
                }
            }
            // 将当前页的子列表添加到结果列表中
            listArray.add(subList);
        }
        // 返回分割后的列表
        return listArray;
    }
}
