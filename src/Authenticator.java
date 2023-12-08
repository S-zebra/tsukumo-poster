import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Map;

public class Authenticator extends JPanel {
  private static final String INCORRECT_UN_PASS = "ログインに失敗しました";
  private static final String NETWORK_PROBLEMS = "サーバーにアクセスできません";
  private JTextField userNameField;
  private JPasswordField passwordField;
  private JLabel failedLabel;
  
  public Authenticator() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    userNameField = new JTextField();
    passwordField = new JPasswordField();
    
    failedLabel = new JLabel(INCORRECT_UN_PASS);
    failedLabel.setForeground(Color.red);
    failedLabel.setVisible(false);
    add(failedLabel);
    
    JPanel unCol = new JPanel();
    unCol.setLayout(new BoxLayout(unCol, BoxLayout.X_AXIS));
    unCol.add(new JLabel("ユーザー名: "));
    unCol.add(Box.createHorizontalStrut(15));
    unCol.add(userNameField);
    
    JPanel pwCol = new JPanel();
    pwCol.setLayout(new BoxLayout(pwCol, BoxLayout.X_AXIS));
    pwCol.add(new JLabel("パスワード: "));
    pwCol.add(Box.createHorizontalStrut(15));
    pwCol.add(passwordField);
    add(unCol);
    add(pwCol);
    userNameField.grabFocus();
  }
  
  public String getToken() {
    int res = JOptionPane.showConfirmDialog(null, this, "ログイン", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (res == JOptionPane.OK_OPTION) {
      Document doc = tryLogin(userNameField.getText(), new String(passwordField.getPassword()));
      if (doc == null) {
        showErrorMessage(NETWORK_PROBLEMS);
        return getToken();
      }
      String token = doc.select("input[type=\"hidden\"]").first().attr("value");
      if (token.matches("[A-Za-z0-9]+")) {
//        System.out.println(token);
        return token;
      } else {
        passwordField.setText("");
        showErrorMessage(INCORRECT_UN_PASS);
        return getToken();
      }
    }
    return null;
  }
  
  private void showErrorMessage(String message) {
    failedLabel.setVisible(true);
    failedLabel.setText(message);
  }
  
  private Document tryLogin(String userName, String password) {
    Connection conn = Jsoup.connect("https://tsukumokku.herokuapp.com/signin");
//    conn.ignoreHttpErrors(true);
    String authToken;
    try {
      Document doc = conn.get();
      authToken = doc.select("input[name=\"authenticity_token\"]").first().attr("value");
    } catch (IOException e) {
      e.printStackTrace();
      return null;//最初からコケたとき
    }
    
    Connection conn2 = Jsoup.connect("https://tsukumokku.herokuapp.com/signin");
    Map<String, String> cookies = conn.response().cookies();
//    System.out.println(cookies);
    conn2.cookies(cookies);
    conn2.requestBody("authenticity_token=" + authToken + "&user_name=" + userName + "&password=" + password);
    try {
//      System.out.println(conn2.request().requestBody());
      return conn2.post();
    } catch (HttpStatusException htse) {
      htse.printStackTrace();
      if (htse.getStatusCode() == 422) { //なぜか422でコケることがある
        return tryLogin(userName, password);
      } else {
        return null;
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
