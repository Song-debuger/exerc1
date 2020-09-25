package exerc1;

import java.io.*;
import java.net.*;
import java.util.Scanner;


/**
 * @author hansong
 *
 */
public class fileClient {

	private static final int tcp_PORT = 2021;//TCP连接端口
	private static final String HOST = "127.0.0.1";//链接地址
	Socket socket = new Socket();

	private static final int udp_PORT = 2020; //UDP端口
	private static final int SENDSIZE = 1024; //一次传送文件的字节数
	DatagramSocket dgsocket;

	public fileClient() throws UnknownHostException,IOException{
		socket = new Socket();
		socket.connect(new InetSocketAddress(HOST,tcp_PORT));
	}

	public static void main(String[] args)throws UnknownHostException,
		IOException{
		new fileClient().send();
	}
	
	public void send() {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					socket.getOutputStream()));//客户端输出流，向服务器发消息
			BufferedReader br = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));//客户端输入流，接收服务器消息
//			System.out.println("等待服务器管理员输入目录地址...");
			System.out.println(br.readLine());//输出服务器返回连接成功的消息
			PrintWriter pw = new PrintWriter(bw,true);//装饰输出流，及时更新
			Scanner in = new Scanner(System.in);//接受用户信息
			String cmd = null;
			while ((cmd = in.next()) != null) {
				pw.println(cmd); //发送给服务器端
				if(cmd.equals("cd") || cmd.equals("get")){
					String dir = in.next();
					pw.println(dir);//向服务器发送地址
					if (cmd.equals("get")){//下载文件
						String currentP = br.readLine();
						if (fileExist(currentP,dir)){
							System.out.println("开始接收文件："+ dir);
							long fileLength = (new File(currentP + "/" +dir)).length();
							getFile(dir, fileLength);
							System.out.println("文件接收完毕");
						} else {
							System.out.println("未知文件");
						}
					}
				}

				String msg = null;
				while (null != (msg = br.readLine())){
					if (msg.equals("CmdEnd")){
						break;
					}
					System.out.println(msg);//输出服务器返回的消息
				}

				if(cmd.equals("bye")){
					break;//退出
				}
			}
			in.close();
			br.close();
			bw.close();
			pw.close();
		}catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(null != socket) {
				try {
					socket.close();//断开连接
				}catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void getFile(String dir, long fileLength) throws IOException{
		DatagramPacket dp = new DatagramPacket(new byte[SENDSIZE],SENDSIZE);
		dgsocket = new DatagramSocket(udp_PORT);//UDP连接

		byte[] recInfo = new byte[SENDSIZE];
		FileOutputStream fos = new FileOutputStream(new File(("/Users/hansong/Desktop/") + dir));

		long count = (long)(fileLength / SENDSIZE) + ((fileLength % SENDSIZE) == 0 ? 0 : 1);

		double rctcount = 0;//记录当前已传包数
		double staticcount = count;//文件总包数
		int flag;//已传输百分比
		int pstflag = 0;

		while ((count--) > 0) {
			dgsocket.receive(dp);//接收文件信息
			rctcount++;
			double percent = rctcount/staticcount;
			flag = (int)(percent*100);
			if (flag%100 >= 1 && flag != pstflag ){
				System.out.print(flag+"%"+" ");
				pstflag = flag;
				//if ((int)(percent*100) == 50){
				//	System.out.print("\n");
				//}
			}
			//System.out.print(".");
			recInfo = dp.getData();
			fos.write(recInfo,0,dp.getLength());
			fos.flush();
		}

		System.out.print("\n");

		dgsocket.close();
		fos.close();
	}

	public static boolean fileExist(String currentPath, String fileName){
		boolean isExist = false;
		File rootFile = new File(currentPath);
		File[] fileList = rootFile.listFiles();
		for(int i = 0; i < fileList.length; i++){
			if (fileList[i].getName().equals(fileName) && fileList[i].isFile()){//找到了同名的文件夹
				isExist = true;
			}
		}
		return isExist;
	}
}
	


