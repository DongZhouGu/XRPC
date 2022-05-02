package com.dzgu.xrpc.extension;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @description: 扩展类加载器
 * @Author： dzgu
 * @Date： 2022/4/22 19:02
 */
@Slf4j
public class ExtensionLoader<T> {

    /**
     * 扩展类存放地址
     */
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    /**
     * 默认扩展名缓存
     */
    private final String defaultNameCache;

    /**
     * 扩展类加载器实例缓存
     */
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    /**
     * 扩展类单例缓存
     */
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    /**
     * 扩展类实例缓存
     */
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    /**
     * 扩展类配置列表缓存
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    /**
     * 扩展类加载器的类型
     */
    private final Class<?> type;

    /**
     * 构造函数
     *
     * @param type 扩展类加载器的类型
     */
    private ExtensionLoader(Class<?> type) {
        this.type = type;
        SPI annotation = type.getAnnotation(SPI.class);
        defaultNameCache = annotation.value();
    }

    /**
     * 获取对应类型的扩展加载器实例
     *
     * @param type 扩展类加载器的类型
     * @return 扩展类加载器实例
     */
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        // 扩展类型不能为空
        if (type == null) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        // 扩展类型必须为接口
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type must be an interface.");
        }
        // 扩展类型必须被@SPI注解
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }

        // 从缓存中拿到扩展器加载器实例，没有则new一个放进去
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if (extensionLoader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<S>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }

    /**
     * 获取默认的扩展类实例，会自动加载 @SPI 注解中的 value 指定的类实例
     *
     * @return 返回该类的注解 @SPI.value 指定的类实例
     */
    public T getDefaultExtension() {
        return getExtension(defaultNameCache);
    }

    /**
     * 根据名字获取扩展类实例(单例)
     *
     * @param name 扩展类在配置文件中配置的名字. 如果名字是空的或者空白的，则返回默认扩展
     * @return 单例扩展类实例，如果找不到，则抛出异常
     */
    public T getExtension(String name) {
        if (StrUtil.isBlank(name)) {
            log.warn("Extension name is null or empty, load the default Extension: " + defaultNameCache);
            return getDefaultExtension();
        }
        // 从缓存中获取单例，没有命中，则创建
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            holder = cachedInstances.get(name);
        }
        // 创建单例
        Object instance = holder.get();
        // 双重锁检查
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }


    /**
     * 创建对应名字的扩展类实例
     *
     * @param name 扩展名
     * @return 扩展类实例
     */
    private T createExtension(String name) {
        // 获取当前类型所有扩展类，并从中根据名字获取目标类
        Class<?> clazz = getAllExtensionClasses().get(name);
        if (clazz == null) {
            throw new RuntimeException("No such extension of name " + name);
        }
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if (instance == null) {
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return instance;
    }

    /**
     * 获取当前类型{@link #type}的所有扩展类
     *
     * @return {name: clazz}
     */
    private Map<String, Class<?>> getAllExtensionClasses() {
        // 从缓存中获取所有扩展类
        Map<String, Class<?>> classes = cachedClasses.get();
        // 缓存中没有，则从目录文件中读取加载
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    // load all extensions from our extensions directory
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 从文件中获取当前类型{@link #type}的所有扩展类
     */
    private void loadDirectory(Map<String, Class<?>> extensionClasses) {
        // 扩展配置文件名
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        // 拿到资源文件夹
        ClassLoader classLoader = ExtensionLoader.class.getClassLoader();

        try {
            Enumeration<URL> urls = classLoader.getResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 解析文件的每一行行，并且把解析到的类，放到 extensionClasses 中
     */
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            String line;
            // read every line
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 忽略#号开头的注释
                if (line.startsWith("#")) {
                    continue;
                }
                String[] kv = line.split("=");
                if (kv.length != 2 || kv[0].length() == 0 || kv[1].length() == 0) {
                    throw new IllegalStateException("Extension file parsing error. Invalid format!");
                }
                if (extensionClasses.containsKey(kv[0])) {
                    throw new IllegalStateException(kv[0] + " is already exists!");
                }
                Class<?> clazz = null;
                try {
                    clazz = classLoader.loadClass(kv[1]);
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage());
                }
                extensionClasses.put(kv[0], clazz);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
