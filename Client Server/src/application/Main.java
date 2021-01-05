package application;
	
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class Main extends Application {
	
	Socket socket;
	TextArea textArea;//메세지들이 출력되는 공간
	
	//클라이언트 프로그램 동작 메서드 - 1 
	public void startClient(String IP, int port) {
		Thread thread = new Thread() { //쓰레드가 동시다발적으로 생겨날 경우가 없기때문에 runnable객체대신 쓰레드객체사용
			public void run() {
				try {
					socket = new Socket(IP, port);//소켓 초기화
					receive();//서버로부터 메세지를 전달 받을 수 있도록 receive
				} catch (Exception e) {
					if(!socket.isClosed()) {
						stopClient();//오류발생시 소켓이 열려있을때 클라이언트 종료
						System.out.println("[서버 접속 실패]");
						Platform.exit(); //프로그램 자체를 종료
					}
				}
			}
		};
		thread.start();
	}
	
	//클라이언트 프로그램 종료 메서드 - 4
	public void stopClient() {
		try {
			if(socket != null && !socket.isClosed()) {
				socket.close();//소켓이 열려있는 상태라면 종료.
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//서버로부터 메세지를 전달받는 메소드 - 2
	public void receive() {
		while(true) {//서버로부터 메세지를 계속 전달받기 위해 무한루프
			try {
				InputStream in = socket.getInputStream();//서버로부터 메세지 전달받음
				byte[] buffer = new byte[512];//512바이트만큼 끊어서 버퍼에 담아 전달
				int a = System.in.read(buffer);
				System.out.print(a);//실제로 입력받음
				if(a == -1) throw new IOException();//서버로부터 내용을 입력받는 도중 오류가 발생하면 동작
				String message = new String(buffer, 0, a, "UTF-8");//실제로 버퍼에 있는 정보를 화면에 출력
				Platform.runLater(()->{
					textArea.appendText(message);//화면에 메세지 출력
				});
			}catch (Exception e) {
				stopClient(); //오류 발생시 탈출
				break;
			}
		}
	}
	
	//서버로 메세지를 전송하는 메소드 - 3
	public void send(String message) {
		Thread thread = new Thread() {
			public void run() {
				try {
					OutputStream out = socket.getOutputStream();//서버로 메시지 전송
					byte[] buffer = message.getBytes("UTF-8");//서버에서 전달받을때 UTF-8로 설정을해놔서 이걸로 인코딩
					out.write(buffer);
					out.flush();
				} catch (Exception e) {
					stopClient();
				}
			}
		};
		thread.start();
	}
	
	//디자인 및 동작
	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		HBox hbox = new HBox();
		hbox.setSpacing(5);
		
		TextField userName = new TextField();
		userName.setPrefWidth(150);
		userName.setPromptText("이름 입력");
		HBox.setHgrow(userName,  Priority.ALWAYS);
		
		TextField IPText = new TextField("127.0.0.1");
		TextField portText = new TextField("9876");
		portText.setPrefWidth(80);
		
		hbox.getChildren().addAll(userName, IPText, portText);
		root.setTop(hbox);
		
		textArea = new TextArea();
		textArea.setEditable(false);
		root.setCenter(textArea);
		
		TextField input = new TextField();
		input.setPrefWidth(Double.MAX_VALUE);
		input.setDisable(true);
		
		input.setOnAction(event-> {
			send(userName.getText() + ": " + input.getText() + "\n");
			input.setText("");
			input.requestFocus();
		});
		
		Button sendButton = new Button("보내기");
		sendButton.setDisable(true);
		
		sendButton.setOnAction(event-> {
			send(userName.getText() + ": " + input.getText() + "\n");
			input.setText("");
			input.requestFocus();
		});
		
		Button connectionButton = new Button("접속하기");
		connectionButton.setOnAction(event->{
			if(connectionButton.getText().equals("접속하기")) {
				int port = 9876;
				try {
					port = Integer.parseInt(portText.getText());
				} catch (Exception e) {
					e.printStackTrace();
				}
				startClient(IPText.getText(),port);
				Platform.runLater(() -> {
					textArea.appendText("[ 접속 ]\n");
				});
				connectionButton.setText("종료하기");
				input.setDisable(false);
				sendButton.setDisable(false);
				input.requestFocus();
			} else {
				stopClient();
				Platform.runLater(()->{
					textArea.appendText("[ 통신 종료 ]\n");			
				});
				connectionButton.setText("접속하기");
				input.setDisable(true);
				sendButton.setDisable(true);
			}
		});
		
		BorderPane pane = new BorderPane();
		pane.setLeft(connectionButton);
		pane.setCenter(input);
		pane.setRight(sendButton);
		
		root.setBottom(pane);
		Scene scene = new Scene(root, 400, 400);
		primaryStage.setTitle("[클라이언트]");
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(event -> stopClient());
		primaryStage.show();
		
		connectionButton.requestFocus();
	}
	
	//프로그램의 진입점
	public static void main(String[] args) { //main함수
		launch(args);
	}
}
