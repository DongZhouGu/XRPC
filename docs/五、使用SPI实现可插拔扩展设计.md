# 五、使用SPI实现可插拔扩展设计

> 从零实现一个轻量级RPC框架-系列文章
> Github: [https://github.com/DongZhouGu/XRpc](https://github.com/DongZhouGu/XRpc) 

### 前言
SPI全称为Service Provider Interface，对应中文为服务发现机制。SPI类似一种可插拔机制，首先需要定义一个接口或一个约定，然后不同的场景可以对其进行实现，调用方在使用的时候无需过多关注具体的实现细节。在Java中，SPI体现了面向接口编程的思想，满足开闭设计原则。

## JDK自带SPI实现
以序列化为例，如果想要实现可插拔的序列化实现，使用JDK原生SPI过程如下。
```java
public interface Serializer { 
    byte[] serialize(Object object);
}

public class JSONSerializer implements Serializer {
    @Override 
    public byte[] serialize(Object object) {
        return JSONUtil.toJsonStr(object).getBytes();
    } 
}

public class ProtostuffSerializer implements Serializer {
    private static final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
    @Override
    public byte[] serialize(Object object) {
        Schema schema = RuntimeSchema.getSchema(object.getClass());
        return ProtostuffIOUtil.toByteArray(object, schema, BUFFER);
    }
}
```
在 resources/META-INF/services 目录下添加一个 com.xxx.Serializer 的文件，这是 JDK SPI 的配置文件：
```java
com.xxx.JSONSerializer
com.xxx.ProtostuffSerializer
```
然后，就可以使用JDK提供的ServiceLoader来加载扩展类了
```java
public static void main(String[] args) {
    ServiceLoader<Serializer> serviceLoader = ServiceLoader.load(Serializer.class);
    Iterator<Serializer> iterator = serviceLoader.iterator();
    while (iterator.hasNext()) {
        Serializer serializer= iterator.next();
        System.out.println(serializer.getClass().getName());
    }
}
```
JDK对SPI的加载实现存在一个较为突出的小缺点，无法按需加载实现类，通过ServiceLoader.load加载时会将文件中的所有实现都进行实例化，如果想要获取具体某个具体的实现类需要进行遍历判断。
## Dubbo SPI 和 Java SPI 区别？
Dubbo 就是通过 SPI 机制加载所有的组件。不过，Dubbo 并未使用 Java 原生的 SPI 机制，而是对其进行了增强，使其能够更好的满足需求。

- 配置文件改为键值对形式，可以获取任一实现类，懒加载，而无需加载所有实现类，节约资源；
- 增加了缓存来存储实例，提高了读取的性能；
- Dubbo SPI还提供了默认值的指定方式，@SPI（“xxx”）指定
- 增加了对扩展点 IOC 和 AOP 的支持，一个扩展点可以直接 setter 注入其

它扩展点。
## XRpc的SPI实现
### SPI注解
定义SPI注解
```java
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {
    /**
     * 默认扩展类全路径
     *
     * @return 默认不填是 default
     */
    String value() default "default";
}
```
被@SPI注解的接口为扩展类型，SPI注解含有默认值，可选择默认扩展实现

### 获取对应接口的扩展加载器实例
当我们想要获取特定的扩展类实例时，调用如下
```java
ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
// 获取此接口的ExtensionLoader扩展类类加载器
ExtensionLoader.getExtensionLoader(ServiceDiscovery.class)
// 使用ExtensionLoader获取扩展类实例，给到调用者
.getExtension("zk");
```
下面是获取对应接口的扩展加载器实例的具体逻辑
```java
**
 * 扩展类加载器实例缓存
 */
private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
/**
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
```
加载器指的就是 ExtensionLoader<T>，为了减少对象的开销， 屏蔽了加载器的构造函数，提供了一个静态方法来获取加载器。EXTENSION_LOADERS是一个 Map，缓存了各种类型的加载器。获取的时候先从缓存获取，缓存不存在则去实例化，节省资源

### 扩展类懒加载
```java
/**
 * 扩展类配置列表缓存
 */
private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();
/**
 * 扩展类实例缓存
 */
private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();


/**
 * 扩展类实例缓存
 */
private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

/**
 * 根据名字获取扩展类实例(单例)
 *
 * @param name 扩展类在配置文件中配置的名字. 如果名字是空的或者空白的，则返回默认扩展
 * @return 单例扩展类实例，如果找不到，则抛出异常
 */
public T getExtension(String name) {
    if (StrUtil.isBlank(name)) {
        log.warn("Extension name is null or empty, load the default Extension");
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
```
一个接口如果有很多实现类，而我们只需要其中一个的时候，就会产生其他不必要的实现类。 例如 Dubbo 的序列化接口，实现类就有 fastjson、gson、hession2、jdk、kryo、protobuf 等等，通常我们只需要选择一种序列化方式。
这里，我们根据名字来实例化需要加载的扩展类。同样，使用一个cachedInstances缓存已经实例化的单例扩展类。

### 扩展类的创建
当获取扩展类不存在缓存时，会加锁创建单例，并放入到缓存中
实例化的流程如下：

1. 从配置文件中，加载该接口所有的实现类的 Class 对象，并放到缓存中。
1. 根据要获取的扩展名字，找到对应的 Class 对象。
1. 调用 clazz.newInstance() 实例化。(Class 需要有无参构造函数)
```java
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
```
### 默认扩展类
在使用 @SPI 的时候可以指定一个默认的实现类名，例如 @SPI("zk")。
这样当获取扩展名留空没有配置的时候，就会直接获取默认扩展，减少了配置的量
```java
private final String defaultNameCache;

private ExtensionLoader(Class<T> type) {
    this.type = type;
    SPI annotation = type.getAnnotation(SPI.class);
    defaultNameCache = annotation.value();
}
public T getDefaultExtension() {
    return getExtension(defaultNameCache);
}
```
### SPI自适应扩展
在Dubbo中,SPI配置有两种。一种是固定的系统级别的配置，在 Dubbo 启动之后就不会再改了。还有一种是运行时的配置，可能对于每一次的 RPC，这些配置都不同。

- 对于固定的配置，在配置Config中写死，在调用时从ExtensionLoader中拿到对应的扩展类，这样的话，虽然可以支持可插拔的第三方实现，但是在应用启动时，到底用哪个扩展类就已经确定了
- 对于运行时配置，Dubbo提供了自适应扩展，也可被理解为扩展代理类，其就是Extension的代理，它实现了扩展点接口。在调用扩展点的接口方法时，会根据实际的参数来决定要使用哪个扩展。
> 在Dubbo中，因为 dubbo 是 url驱动，即服务的配置信息都是通过&拼接在 url 之后，换句话说，当 Provider 收到调用请求时，其相关配置是通过查 url 后的参数获得；这样做的目的是， Consumer 在注册中心拿到相应服务的 url 后，可以根据自身的配置对请求 url 再次进行拼接（修改）。因此，对于 Dubbo 而言，每一次的 RPC 调用的参数都是未知的，只有在运行时，根据这些参数才能做出正确的决定。


**获取自适应扩展类**
```java

public T getAdaptiveExtension() {
    InvocationHandler handler = new AdaptiveInvocationHandler<>(type);
    return (T) Proxy.newProxyInstance(ExtensionLoader.class.getClassLoader(),
            new Class<?>[]{type}, handler);
}
```
适配扩展类其实是一个代理类，接下来来看看这个代理类 AdaptiveInvocationHandler：
```java

/**
 * @description: Extension代理类
 * @Author： dzgu
 * @Date： 2022/4/30 22:02
 */
public class AdaptiveInvocationHandler<T> implements InvocationHandler {
    private final Class<T> clazz;

    public AdaptiveInvocationHandler(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(args.length==0){
            return method.invoke(proxy,args);
        }
        URL url=null;
        for (Object arg : args) {
            if (arg instanceof URL) {
                url = (URL) arg;
                break;
            }
        }
        // 找不到 URL 参数，直接执行方法
        if (url == null) {
            return method.invoke(proxy, args);
        }
        Adaptive adaptive = method.getAnnotation(Adaptive.class);
        // 如果不包含 @Adaptive，直接执行方法即可
        if (adaptive == null) {
            return method.invoke(proxy, args);
        }
        // 从 @Adaptive#value() 中拿到扩展名的 key
        String extendNameKey = adaptive.value();
        String extendName;
        // 如果这个 key 是协议，从协议拿。其他的就直接从 URL 参数拿
        if (URLKeyConst.PROTOCOL.equals(extendNameKey)) {
            extendName = url.getProtocol();
        } else {
            extendName = url.getParam(extendNameKey, method.getDeclaringClass() + "." + method.getName());
        }
        // 拿到扩展名之后，就直接从 ExtensionLoader 拿就行了
        ExtensionLoader<T> extensionLoader = ExtensionLoader.getExtensionLoader(clazz);
        T extension = extensionLoader.getExtension(extendName);
        return method.invoke(extension, args);
    }
}
```
扩展代理类的流程如下

1. 从方法参数中拿到 URL 参数，拿不到就直接执行方法
1. 获取配置 Key。从 @Adaptive#value() 拿扩展名的配置 key，如果拿不到就直接执行方法
1. 获取扩展名。判断配置 key 是不是协议，如果是就拿协议类型，否则拿 URL 后面的参数。
   例如 URL 是：zk://localhost:2181?type=eureka
   - 如果 @Adaptive("protocol")，那么扩展名就是协议类型：zk
   - 如果 @Adaptive("type")，那么扩展名就是type 参数：eureka
4. 最后根据扩展名获取扩展 extensionLoader.getExtension(extendName)

