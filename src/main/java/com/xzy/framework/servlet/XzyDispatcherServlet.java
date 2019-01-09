package com.xzy.framework.servlet;

import com.xzy.framework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xzy on 19/1/9  .
 */

public class XzyDispatcherServlet extends HttpServlet {

    private List<String> classNames = new ArrayList<String>();

    private Map<String, Object> beansMap = new HashMap<String, Object>();

    private List<Handler> handlerMappings= new ArrayList<Handler>();

//    private Map<String, Object> handlerMap = new HashMap<String, Object>();

    public XzyDispatcherServlet() {

    }

    public void init(ServletConfig config) throws ServletException {
        // 第一步 包扫描 根据配置扫描对应包的Class
        // 读配置文件中的scanPackage的包
        String path = config.getInitParameter("contextConfigLocation");
        Properties properties = new Properties();
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(path);
        try {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String scanPackage = properties.getProperty("scanPackage");
        // 扫描包下面的class并保存classNames
        ScannerPackage(scanPackage);

        // 第二步 类实例
        // 类名字有了，可以通过反射获取类实例
        classesInstance();

        // 第三步 IOC 依赖注入，完成自动装配
        // 实例beans有了,需要注入 初始化才能用
        inject();

        // 第四步 映射 完成url和controller方法的映射
        handerMapping();

        System.out.println("this is xzy servlet");
    }

    private void handerMapping() {
        // 通过反射完成url映射
        if (beansMap.isEmpty()) {
            System.out.println("没有任何实例化的类");
            return;
        }
        for (Map.Entry<String, Object> entry : beansMap.entrySet()) {
            Object instance = entry.getValue();
            // 类有无
            Class<?> clazz = entry.getValue().getClass();
            if (clazz.isAnnotationPresent(MyController.class)) {
                // 获取controller类上的MyRequestMapping的url
                String controllerPath = "";
                if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                    // 获取MyRequestMapping注解的url值
                    MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                    controllerPath = requestMapping.value();
                }

                // 获取方法上的RequestMapping的url
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    String methodPath = "";
                    if (method.isAnnotationPresent(MyRequestMapping.class)) {
                        MyRequestMapping methodAnnotation = method.getAnnotation(MyRequestMapping.class);
                        methodPath = methodAnnotation.value();
                    }
                    String path = controllerPath + methodPath;
                    Pattern pattern = Pattern.compile(path);
                    handlerMappings.add(new Handler(pattern, entry.getValue(), method));
//                    handlerMap.put(path, method);
                }

            }
        }
    }

    private void inject() {
        // 通过反射注入成员变量
        if (beansMap.isEmpty()) {
            System.out.println("没有任何实例化的类");
            return;
        }
        Set<Map.Entry<String, Object>> entries = beansMap.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            Object instance = entry.getValue();
            Field[] fields = instance.getClass().getDeclaredFields();
            for (Field field : fields) {
                // 判断成员是有无MyAutowrited的注解
                if (field.isAnnotationPresent(MyAutowrited.class)) {
                    MyAutowrited myAutowrited = field.getAnnotation(MyAutowrited.class);
                    String value = myAutowrited.value();
                    // 全部授权
                    field.setAccessible(true);
                    try {
                        // 成员属性赋值  如controller的成员初始化service实例 最关键的一步
                        field.set(instance, beansMap.get(value));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void classesInstance() {
        // 类名字有了，通过反射获取类实例
        if (classNames.isEmpty()) {
            System.out.println("没有扫描到任何类");
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                // 扫描有自定义注解MyController的controller类
                if (clazz.isAnnotationPresent(MyController.class)) {
                    MyController controller = clazz.getAnnotation(MyController.class);
                    try {
                        Object instance = clazz.newInstance();
//                        MyRequestMapping myRequestMapping = clazz.getAnnotation(MyRequestMapping.class);
//                        String mappingValue = myRequestMapping.value();
//                        beansMap.put(mappingValue, instance);
                        beansMap.put(controller.getClass().getName(), instance);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    // 扫描MyService注解的类
                    try {
                        Object instance = clazz.newInstance();
                        MyService myService = clazz.getAnnotation(MyService.class);
                        String value = myService.value();
                        beansMap.put(value, instance);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // 根据包名 扫描包中的Class
    private void ScannerPackage(String scanPackage) {
        // 包在tomcat的class目录下面
        // 包名是带.的，需要替换成文件目录路径
        String path = scanPackage.replaceAll("\\.", "/");
        // 获取资源路径
        URL url = this.getClass().getClassLoader().getResource("/" + path);
        // 包文件夹
        String fileName = url.getFile();
        File files = new File(fileName);
        for (File file : files.listFiles()) {
            if (file.isDirectory()) {
                ScannerPackage(scanPackage + "." + file.getName());
            } else {
                String className = scanPackage + "." + file.getName().replace(".class", "");
                // 将class路径保存起来
                classNames.add(className);
            }
        }
    }

    //handler的内部类
    private class Handler {
        protected Object controller;//保存方法对应的实例
        protected Method method;//保存映射的方法
        protected Pattern pattern;
        protected Map<String, Integer> paramIndexMapping;//参数对应顺序

        protected Handler(Pattern pattern, Object controller, Method method) {
            this.controller = controller;
            this.pattern = pattern;
            this.method = method;
            paramIndexMapping = new HashMap<String, Integer>();
            putParamIndexMapping(method);
        }

        // 处理参数
        private void putParamIndexMapping(Method method) {
            // 提取方法中加了注解的参数
            Annotation[][] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length; i++) {
                for (Annotation a : pa[i]) {
                    if (a instanceof MyRequestParam) {
                        String paramName = ((MyRequestParam) a).value();
                        if (!"".equals(paramName.trim())) {
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }
            // 提取方法中的request 和response参数
            Class<?>[] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length; i++) {
                Class<?> type = paramsTypes[i];
                if (type == HttpServletRequest.class ||
                        type == HttpServletResponse.class) {
                    paramIndexMapping.put(type.getName(), i);
                }
            }
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (handlerMappings.isEmpty()) {
            return;
        }
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.replaceAll(contextPath, "");
//        Method method = (Method) handlerMap.get(path);
//        beansMap.get
//        method.invoke(instance, null);

        try {
            Handler handler = null;
            for (Handler h : handlerMappings) {
                Matcher matcher = h.pattern.matcher(path);
                // 如果没有匹配上继续下一个匹配
                if (!matcher.matches()) continue;
                handler = h;
            }

            if (handler == null) {
                resp.getWriter().write("404 Not Found");
                return;
            }

            // 方法
            Method method = handler.method;
            Class<?>[] parameterTypes = method.getParameterTypes();
            // 需要接受的参数数组
            Object[] paramValues = new Object[parameterTypes.length];

            // 获取url请求的参数列表
            Map<String, String[]> params = req.getParameterMap();

            for (Map.Entry<String, String[]> param : params.entrySet()) {
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                // 如果找不到匹配的对象，则开始填充参数值
                if (!handler.paramIndexMapping.containsKey(param.getKey())) {
                    continue;
                }
                int index = handler.paramIndexMapping.get(param.getKey());
                paramValues[index] = convert(parameterTypes[index], value);
            }

            // 设置方法中的request和response对象
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;
            method.invoke(handler.controller, paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Object convert(Class<?> type, String value) {
        if (Integer.class == type) {
            return Integer.valueOf(value);
        }
        return value;
    }
}
