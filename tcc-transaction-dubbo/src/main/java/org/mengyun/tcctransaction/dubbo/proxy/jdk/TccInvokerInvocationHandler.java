package org.mengyun.tcctransaction.dubbo.proxy.jdk;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.mengyun.tcctransaction.api.Compensable;
import org.mengyun.tcctransaction.api.Propagation;
import org.mengyun.tcctransaction.dubbo.context.DubboTransactionContextEditor;
import org.mengyun.tcctransaction.interceptor.ResourceCoordinatorAspect;
import org.mengyun.tcctransaction.support.FactoryBuilder;
import org.mengyun.tcctransaction.utils.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * Tcc 调用处理器
 *
 * Created by changming.xie on 2/26/17.
 */
public class TccInvokerInvocationHandler extends InvokerInvocationHandler {

    /**
     * proxy
     */
    private Object target;

    public TccInvokerInvocationHandler(Invoker<?> handler) {
        super(handler);
    }

    public <T> TccInvokerInvocationHandler(T target, Invoker<T> invoker) {
        super(invoker);
        this.target = target;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Compensable compensable = method.getAnnotation(Compensable.class);
        if (compensable != null) {
            // 设置 @Compensable 属性
            if (StringUtils.isEmpty(compensable.confirmMethod())) {
                ReflectionUtils.changeAnnotationValue(compensable, "confirmMethod", method.getName());
                ReflectionUtils.changeAnnotationValue(compensable, "cancelMethod", method.getName());
                ReflectionUtils.changeAnnotationValue(compensable, "transactionContextEditor", DubboTransactionContextEditor.class);
                ReflectionUtils.changeAnnotationValue(compensable, "propagation", Propagation.SUPPORTS);
            }
            // 生成切面
            ProceedingJoinPoint pjp = new MethodProceedingJoinPoint(proxy, target, method, args);
            // 执行
            return FactoryBuilder.factoryOf(ResourceCoordinatorAspect.class).getInstance().interceptTransactionContextMethod(pjp);
        } else {
            return super.invoke(target, method, args);
        }
    }

}