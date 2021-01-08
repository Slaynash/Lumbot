package slaynash.lum.bot.discord;

import java.io.IOException;
import java.net.ServerSocket;

public class ServerManager {
	
	private static ServerSocket serverSocket;

	public static void Start() {
		try {
			serverSocket = new ServerSocket(48632);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		Thread t = new Thread(() -> {
			
			
			
		}, "Server");
		
		t.setDaemon(true);
		t.start();
	}
	
}
