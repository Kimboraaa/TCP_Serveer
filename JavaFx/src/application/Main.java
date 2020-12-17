package application;
	
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;


public class Main extends Application {
	
	public static ExecutorService threadPool; 
	//쓰레드에는 갯수제한이 있어서 서버 성능저하를 방지(클라이언트가 많이 접속하면 효과적으로 쓰레드 관리)
	public static Vector<Client> clients = new Vector<Client>();
	//접속한 클라이언트들을 관리 vector:배열
	
	ServerSocket serverSocket; //서버소켓 임포트
	 
	//서버를 구동시켜서 클라이언트의 연결을 기다리는 메소드 - 1 
	public void startServer(String IP, int port) {
		try {
			serverSocket = new ServerSocket();//서버가 실행이 되면 소켓생성
			serverSocket.bind(new InetSocketAddress(IP, port));//서버컴퓨터가 자신의 IP,port로 특정한 클라이언트의 접속을 기다리게
		} catch (Exception e) {//오류가 발생한 경우
			e.printStackTrace();
			if(!serverSocket.isClosed()) {//열려있는 서버소켓을
				stopServer();//스탑서버로 서버 종료
			}
			return;
		}
		
		//클라이언트가 접속할 때까지 계속 기다리는 쓰레드 - 2
		Runnable thread = new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {
						Socket socket = serverSocket.accept();
						clients.add(new Client(socket)); //클라이언트가 접속하면 클라이언트 배열에 추가
						System.out.println("[클라이언트 접속]"
						+socket.getRemoteSocketAddress() //접속한 클라이언트 주소출력
						+": " + Thread.currentThread().getName()); //해당 쓰레드 정보 출력
					}catch (Exception e) { //예외발생시
						if(!serverSocket.isClosed()) {
							stopServer(); //서버를 작동 중지
						}
						break; //서버 빠져나옴
					} 
				}				
			}			
		};
		threadPool = Executors.newCachedThreadPool(); //threadPool초기화
		threadPool.submit(thread); //위에 클라이언트를 기다리는 스레드를 넣어줌.
	}
	
	//서버의 작동을 중지시키는 메소드
	public void stopServer() {
		try {
			//현재 작동중인 모든 소켓 닫기
			Iterator<Client> iterator = clients.iterator();//Iterator를 이용하여 모든 클라이언트에 개별적으로 접근
			while(iterator.hasNext()) { //클라이언트한테 개별적으로 접근
				Client client = iterator.next();
				client.socket.close();
				iterator.remove(); //연결이 끊킨 해당 클라이언트 제거
			}
			//서버 소켓 객체 닫기
			if(serverSocket != null && !serverSocket.isClosed()) { //클라이언트 연결이 끊기고 서버소켓도 닫음
				threadPool.shutdown(); //서버소켓이 null이 아니고 소켓이 열려있는 상태라면 닫힘.
			}
			//쓰레드풀 종료하기
			if(threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdown(); //쓰레드풀 또한 종료
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	//디자인 및 동작
	
	@Override
	public void start(Stage primaryStage) {		
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		TextArea textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setFont(new Font("나눔고딕",15));
		root.setCenter(textArea);
		
		Button toggleButton = new Button("시작하기");
		toggleButton.setMaxWidth(Double.MAX_VALUE);
		BorderPane.setMargin(toggleButton,  new Insets(1, 0, 0, 0));
		root.setBottom(toggleButton);
		
		String IP = "127.0.0.1";
		int port = 9876;
		
		toggleButton.setOnAction(event -> {
			if(toggleButton.getText().equals("시작하기")) {
				startServer(IP,port);
				Platform.runLater(() -> {
					String message = String.format("[서버 시작]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("종료하기");
				});
			} else {
				stopServer();
				Platform.runLater(() -> {
					String message = String.format("[서버 종료]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("시작하기");
				}); 
			}
		});
		Scene scene = new Scene(root, 400, 400);
		primaryStage.setTitle("[통신 서버]");
		primaryStage.setOnCloseRequest(event -> stopServer());
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	//프로그램의 진입점
	public static void main(String[] args) {//메인메소드
		launch(args);
	}
}
