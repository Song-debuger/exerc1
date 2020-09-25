package exerc1;

import java.awt.desktop.SystemEventListener;
import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * work thread
 * 
 * @author hansong
 *
 */
public class handler implements Runnable { // 负责与单个客户通信的线程
	private Socket socket;
	BufferedReader br;
	BufferedWriter bw;
	PrintWriter pw;
	private static String rootpath;
	//private static String currentpath = "/Users/hansong/Documents/六级/真题";
	private static String currentpath;
	String lose = null;

	private static final String HOST = "127.0.0.1";//连接地址
	private static final int udp_PORT = 2020;//UDP端口
	private static final int SENDSIZE = 1024;//一次传送文件的字节数
	DatagramSocket dgsocket;//UDP用于传送文件
	SocketAddress socketAddress;


	public handler(Socket socket,String rp) {

		this.socket = socket;
		this.rootpath = rp;

	}

	public void initStream() throws IOException { // 初始化输入输出流对象方法
		//获得来自客户端的字节转字符输出流
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		pw = new PrintWriter(bw, true);// 向客户端发送字符输出流
	}

	public void run() { // 执行的内容
		try {
			currentpath = rootpath;//初始化当前目录为根目录
			System.out.println("客户端新连接，IP地址："
					+ socket.getInetAddress() + "端口号：" + socket.getPort()); //客户端信息
			initStream(); // 初始化输入输出流对象
			pw.println(socket.getInetAddress() + ":" + socket.getPort() + ":" + ">连接成功");
			lose = "断开连接！   IP地址" + socket.getInetAddress().toString() + "端口号" + socket.getPort();
			String info = null;

			while (null != (info = br.readLine())) {
				if (info.equals("bye")){
					pw.println("断开连接！ ");
					break;
				}else{
					switch (info){
						case "ls":
							ls(currentpath);
							break;
						case "cd":
							String dir = null;
							if (null != (dir = br.readLine())){//接收由客户端发来的地址
								cd(dir);
							} else {
								pw.println("请在cd后输入文件夹名");
							}
							break;
						case "cd..":
							backdir();
							break;
						case "get":
							String fileName = br.readLine();//
							getFile(fileName);
							break;
						default:
							pw.println("unknown cmd");
					}
					pw.println("CmdEnd");// 标识目前的指令结束，帮助跳出Client的输出循环
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
			if (null != socket) {
				try {
					br.close();
					bw.close();
					pw.close();
					socket.close();
					System.out.println(lose);
					lose = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//cd方法
	private void cd(String dir){
		Boolean isExist = false;//初始设定目录不存在
		Boolean isDir = true;//初始设定是文件夹
		File rootFile = new File(currentpath);
		File[] fileList = rootFile.listFiles();
		for (int i = 0; i < fileList.length; i++){
			if (fileList[i].getName().equals(dir)){//找到了同名的文件夹或文件
				isExist = true;
				if (fileList[i].isDirectory()){//名字对应文件夹
					isDir = true;
					break;
				}else{// 名字对应文件
					isDir = false;
					pw.println("非文件夹！不能进入");
				}
			}
		}
		if (isExist && isDir){//是文件夹并且存在
			currentpath = currentpath + "/" + dir;
			pw.println(dir + " > OK");
			System.out.println("客户端端口号：" + socket.getPort()
					+ "执行操作：" + "进入文件夹：" + currentpath);
		}else if (isDir && (!isExist)){//不在当前目录
			pw.println(dir + " unknow dir!");
		}
	}

	private void backdir(){//返回上一层
		if (currentpath.equals(rootpath)){
			pw.println("已在根目录");
		} else {
			for (int i = currentpath.length(); i > 0 ; i--){
				if (currentpath.substring(i-1,i).equals("/")){//
					String path = currentpath;
					currentpath = currentpath.substring(0,i-1);
					pw.println((new File(currentpath)).getName() + " > OK");
					System.out.println("客户端端口号：" + socket.getPort()
							+ "执行操作：" + "从路径：" + path + "返回到路径：" + currentpath);
					break;
				}
			}
		}
	}

	public String multipleSpaces(int n){//输出指定个数空格，用于控制台对齐
		String output = "";

		for(int i=0; i<n; i++)
			output += " ";

		return output;
	}

	private void ls (String currentpath) {
		File rootFile = new File(currentpath);
		File[] fileList = rootFile.listFiles();
		pw.println("Type" + "\t" + "\t" + "\t" + "Name" + multipleSpaces(30) + "Size");
		for (int i = 0; i < fileList.length; i++) {
			if (fileList[i].isFile()) {//是文件
				pw.println("<file>" + "\t" + "\t" + "  "
						+ fileList[i].getName() + multipleSpaces(35-fileList[i].getName().length())
						+ " " + fileList[i].length() + "B");
			} else if (fileList[i].isDirectory()) {
				pw.println("<dir>" +" " + "\t" + "\t" + "  "
						+ fileList[i].getName() + multipleSpaces(35-fileList[i].getName().length())
						+ " " + fileList[i].length() + "B");
			}
		}
		System.out.println("客户端端口号：" + socket.getPort() + "查看目录：" + currentpath);
	}

	public static boolean fileexist(String dir){
		boolean is = false;
		File rootfile = new File(currentpath);
		File[] filelist = rootfile.listFiles();
		for (int i=0 ; i<filelist.length;i++){
			if (filelist[i].getName().equals(dir) && filelist[i].isFile()){
				is = true;
			}
		}
		return is;
	}

	//get方法
	private void getFile(String fileName)throws SocketException,IOException,InterruptedException {

		pw.println(currentpath);

		if (!fileexist(fileName)){
			return;//文件不存在
		}

		dgsocket = new DatagramSocket();//UDP
		socketAddress = new InetSocketAddress(HOST,udp_PORT);
		DatagramPacket dp;

		byte[] sendInfo = new byte[SENDSIZE];
		int size = 0;
		dp = new DatagramPacket(sendInfo,sendInfo.length,socketAddress);

		BufferedInputStream bfdIS = new BufferedInputStream(
				new FileInputStream(new File(currentpath + "/" + fileName)));
		while((size = bfdIS.read(sendInfo)) > 0){
			dp.setData(sendInfo);
			Thread.sleep(50);//ljl
			dgsocket.send(dp);
			sendInfo = new byte[SENDSIZE];
		}

		dgsocket.close();
		System.out.println("客户端端口号：" + socket.getPort() + "从路径："
				+ currentpath + "下载了文件：" + fileName);
	}

}
