package com.zzy.netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 
 * @author guokaige
 * @Date 2018年12月1日 下午8:26:53
 */
public class TimeClientHandle implements Runnable {

	private String host;
	private int port;

	private Selector selector;
	private SocketChannel socketChannel;

	private volatile boolean stop;

	public TimeClientHandle(String host, int port) {
		this.host = host == null ? "127.0.0.1" : host;
		this.port = port;
		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void run() {
		try {
			// 建议与服务端的链接
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while (!stop) {
			try {
				selector.select(1000);
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				SelectionKey key = null;
				while (it.hasNext()) {
					key = it.next();
					it.remove();
					try {
						// 处理接收数据和发送请求数据
						handleInput(key);
					} catch (Exception e) {
						if (key != null) {
							key.cancel();
							if (key.channel() != null)
								key.channel().close();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		// 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
		if (selector != null)
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

	}

	private void doConnect() throws IOException {
		 //连接server
		if (socketChannel.connect(new InetSocketAddress(host, port))) {
			//1.注册此通道
			socketChannel.register(selector, SelectionKey.OP_READ);
			//2.向服务端发送请求
			doWrite(socketChannel);
			
		//如果连接不成功,则注册连接事件key	
		} else
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
	}

	private void doWrite(SocketChannel sc) throws IOException {
		byte[] req = "QUERY TIME".getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		sc.write(writeBuffer);
		if (!writeBuffer.hasRemaining())
			System.out.println("Send query msg to server succeed.");
	}

	private void handleInput(SelectionKey key) throws IOException {

		if (key.isValid()) {
			
			SocketChannel sc = (SocketChannel) key.channel();
			
			//如果连接成功, 注册可读事件监听, 写查询时间数据到server
			if (key.isConnectable()) {
				if (sc.finishConnect()) {
					sc.register(selector, SelectionKey.OP_READ);
					doWrite(sc);
				} else
					System.exit(1);// 连接失败，进程退出
			}
			
			//读取 服务端返回的数据
			if (key.isReadable()) {
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuffer);
				if (readBytes > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("客户端接受server返回数据 : " + body);
					//再查一次时间
					doWrite(sc);
					//this.stop = true;
				} else if (readBytes < 0) {
					// 对端链路关闭
					key.cancel();
					sc.close();
				} else
					; // 读到0字节，忽略
			}
		}

	}

}
