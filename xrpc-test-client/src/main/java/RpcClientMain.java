import controller.HelloController;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/27 0:29
 */
@ComponentScan(basePackages = {"com.dzgu.xrpc.client","controller"})
public class RpcClientMain {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("===============client=========");
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(RpcClientMain.class);
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        Arrays.stream(beanDefinitionNames).forEach(System.out::println);
        HelloController helloController = (HelloController)applicationContext.getBean("helloController");
        helloController.test();

    }
}
