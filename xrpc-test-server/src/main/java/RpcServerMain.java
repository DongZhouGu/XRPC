import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.dzgu.xprc.service.HelloService;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/27 0:24
 */
public class RpcServerMain {
    public static void main(String[] args) {
        System.out.println("===============server=========");
        new ClassPathXmlApplicationContext("server-spring.xml");
    }
}
