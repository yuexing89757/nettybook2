package com.zzy.netty.pio;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * 
 * @author kevin
 *
 */
public class TimeServerHandlerExecutePool {

	private ExecutorService executor;

	public TimeServerHandlerExecutePool(int maxPoolSize, int queueSize) {
		//最小活跃线程数  是系统cpu核心数
		int corePoolSize = Runtime.getRuntime().availableProcessors();
		//大于cpu核心数的空闲线程,最长维持120秒销毁.
		long keepAliveTime = 120L ; 
		//阻塞队列
		BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<java.lang.Runnable>(queueSize);
		
		//线程工场,能自定义线程名
		ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("demo-pool-%d").build();
		
		//队列超限,处理策略, 拒绝消息
		//RejectedExecutionHandler rejectedExecutionHandler=new ThreadPoolExecutor.AbortPolicy();
		//队列超限,处理策略, 阻塞放入队列
		RejectedExecutionHandler rejectedExecutionHandler=new CustomRejectedExecutionHandler();
		
		//创建线程池
		executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.SECONDS, workQueue ,namedThreadFactory, rejectedExecutionHandler);
	}

	public void execute(java.lang.Runnable task) {
		executor.execute(task);
	}
	
	
    /**
     * 重写拒绝机制
     * @author Administrator
     *
     */
    class CustomRejectedExecutionHandler implements RejectedExecutionHandler {    
    
        @Override    
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {    
            try {  
                // 核心改造点，由blockingqueue的offer改成put阻塞方法  
                executor.getQueue().put(r);  
            } catch (InterruptedException e) {  
                e.printStackTrace();  
            }  
        }    
    } 
	
	
	public static void main(String[] args) {
		//自定义线程池,池最大50, 消息最大保留10000
		TimeServerHandlerExecutePool singleExecutor = new TimeServerHandlerExecutePool(50, 10000);
		singleExecutor.execute(new MyThread());
		singleExecutor.execute(new MyThread());
		singleExecutor.execute(new MyThread());
	}
	
	//1.1定义Runnable接口的实现类
	static class MyThread implements Runnable{
	    //1.2重写其中的run方法
	    @Override
	    public void run() {
	            System.out.println(Thread.currentThread().getName()+"===========");
	    }
	    
	}
	
}
