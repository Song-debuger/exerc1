package exerc1;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author hansong
 *
 */
public class fileServer {
	ServerSocket serverSocket;
	private final int tcp_PORT = 2021; //TCP端口
	ExecutorService executorService; // 线程池
	final int POOLSIZE = 10; // 单个处理器线程池同时工作线程数目

	//private static String rootpath;


	public fileServer() throws IOException {
		serverSocket = new ServerSocket(tcp_PORT); // 创建服务器端套接字
		// 创建线程池
		// Runtime的availableProcessors()方法返回当前系统可用处理器的数目
		// 由JVM根据系统的情况来决定线程的数量
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
				.availableProcessors() * POOLSIZE);
		System.out.println("服务器启动。");
	}



	public static String getPath() throws IOException {// 控制台获得根目录
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		//String rootpath = null;
		System.out.println("请输入根目录地址");
		String msg = stdIn.readLine();
		File tempFile = new File(msg);
		if(tempFile.exists() && tempFile.isDirectory()){
			return msg;
		}else {
			System.out.println("路径不存在，请再试一次！");
			return getPath();
		}

	}

	public static void main(String[] args) throws IOException {
		String gp = getPath();//启动服务前输入根目录
		new fileServer().service(gp); // 启动服务
	}

	/**
	 * service implements
	 */
	public void service(String gp) throws IOException {
		Socket socket = null;

		while (true) {
			try {

				socket = serverSocket.accept(); // 等待用户连接
				executorService.execute(new handler(socket,gp)); // 把执行交给handler，向handler传递输入的目录

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}