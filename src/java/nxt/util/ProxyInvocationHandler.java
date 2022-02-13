package nxt.util;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;



public class ProxyInvocationHandler implements InvocationHandler {
    Object proxiedObject;
    TriConsumer<Method,Object[], Object> preProcess; //TODO create setter.

    ProxyInvocationHandler(Object proxiedObject, TriConsumer preProcess){
        this.proxiedObject = proxiedObject;
        this.preProcess = preProcess;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class[] cargs = null;
        if(args!=null && args.length > 0){
            cargs = new Class[args.length];
            for(int i = 0; i < args.length; i++){
                cargs[i] = args[i].getClass();
            }
        }
        if(preProcess!=null) {
            Object o = preProcess.accept(method, args, proxiedObject);
            if(o!=null){ //if triconsumer returns null then call the method on the proxiedInstance.
                return o; //if a triconsumer return exsists, return right here, do not call proxiedInterface method.
            }
        }
        Method m = proxiedObject.getClass().getMethod(method.getName(), cargs);
        return m.invoke(proxiedObject, args);
    }

    /**
     *
     * @param c Class to be proxied
     * @param instance of the proxied class
     * @return The proxy
     */
    public static Object getNewProxy(Class c, Object instance, TriConsumer triConsumer){
        return Proxy.newProxyInstance(c.getClassLoader(), new Class[] {c}, new ProxyInvocationHandler(instance, triConsumer));
    }

    /**
     * 3 parameter consumer interface
     */
    public interface TriConsumer<T,U,V> {
        public Object accept(T t, U u, V v );
    }
}
