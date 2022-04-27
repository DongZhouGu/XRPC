package com.dzgu.xrpc.zookeeper;

import com.dzgu.xrpc.config.ZkConstants;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;

import java.util.List;

/**
 * @description: 封装一层CuratorFramework的client
 * @Author： dzgu
 * @Date： 2022/4/21 0:01
 */
public class CuratorClient {
    private CuratorFramework client;

    /**
     * 基于建造者模式的链式调用来创建客户端
     *
     * @param connectString     服务器列表，格式host1:port1,host2:port2,...
     * @param namepace          指定一个Zookeeper的根路径
     * @param seesionTimeout    会话超时时间，单位毫秒，默认60000ms
     * @param connectionTimeout 连接创建超时时间，单位毫秒，默认60000ms
     */
    public CuratorClient(String connectString, String namepace, int seesionTimeout, int connectionTimeout) {
        client = CuratorFrameworkFactory.builder()
                .namespace(namepace)
                .connectString(connectString)
                .sessionTimeoutMs(seesionTimeout)
                .connectionTimeoutMs(connectionTimeout)
                .retryPolicy(new ExponentialBackoffRetry(100, 10))
                .build();
        client.start();
    }

    public CuratorClient(String connectString, int timeout) {
        this(connectString, ZkConstants.ZK_NAMESPACE, timeout, timeout);
    }

    public CuratorClient(String connectString) {
        this(connectString, ZkConstants.ZK_NAMESPACE, ZkConstants.ZK_SESSION_TIMEOUT, ZkConstants.ZK_CONNECTION_TIMEOUT);
    }

    public CuratorFramework getClient() {
        return client;
    }

    /**
     * //  连接状态ConnectionState的监听
     *
     * @param connectionStateListener 监听器，可以监听与一个节点的连接状态
     */
    public void addConnectionStateListener(ConnectionStateListener connectionStateListener) {
        client.getConnectionStateListenable().addListener(connectionStateListener);
    }

    /**
     * 创建一个临时节点，并自动递归创建父节点
     *
     * @param path 节点路径
     * @param data 节点数据
     * @return: {@link String} 节点路径
     */
    public String createPathDate(String path, byte[] data) throws Exception {
        return client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL) //节点path末尾会追加一个10位数的单调递增的序列
                .forPath(path, data);
    }

    public void updatePathData(String path, byte[] data) throws Exception {
        client.setData().forPath(path, data);
    }

    public void deletePath(String path) throws Exception {
        client.delete().forPath(path);
    }


    public byte[] getData(String path) throws Exception {
        return client.getData().forPath(path);
    }

    public List<String> getChildren(String path) throws Exception {
        return client.getChildren().forPath(path);
    }

    public void watchNode(String path, Watcher watcher) throws Exception {
        // 给节点绑定一个一次性的watcher
        client.getData().usingWatcher(watcher).forPath(path);
    }

    //监听增删改事件，包括自己跟子节点
    public void watchTreeNode(String path, TreeCacheListener listener) {
        TreeCache treeCache = new TreeCache(client, path);
        treeCache.getListenable().addListener(listener);
    }

    //监听子节点的增删改事件
    public void watchPathChildrenNode(String path, PathChildrenCacheListener listener) throws Exception {
        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, path, true);
        //BUILD_INITIAL_CACHE 代表使用同步的方式进行缓存初始化。
        pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        pathChildrenCache.getListenable().addListener(listener);
    }

    public void close() {
        client.close();
    }

}
