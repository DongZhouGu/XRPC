package com.dzgu.xrpc.extension;
import com.dzgu.xrpc.consts.URLKeyConst;
import com.dzgu.xrpc.url.URL;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

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
