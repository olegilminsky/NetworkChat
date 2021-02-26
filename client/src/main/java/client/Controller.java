package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    private TextArea history;
    @FXML
    private TextField message;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private final int PORT = 9090;
    private final String IP_ADDRESS = "localhost";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            System.out.println("Client disconnected");
                            break;
                        }

                        history.appendText(str + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void clickButtonSend(ActionEvent actionEvent) {
        if (!message.getText().trim().isEmpty()) {
            try {
                out.writeUTF(message.getText().trim());
                message.clear();
                message.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(KeyEvent keyEvent) {
        if (!message.getText().trim().isEmpty() && keyEvent.getCode().equals(KeyCode.ENTER)) {
            try {
                out.writeUTF(message.getText().trim());
                message.clear();
                message.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
