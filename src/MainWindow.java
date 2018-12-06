import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_UP;

class MainWindow extends JFrame implements KeyListener {
  private JPanel bottomPanel;
  private JTextArea textArea;
  private JTextField /*tokenField,*/latField, lonField;
  private JLabel statusLabel;
  
  private TsukumoPoster poster;
  private ArrayList<String> history;
  private int postCount = 0;
  
  private String token;
  
  public MainWindow() {
    setBounds(50, 50, 650, 300);
    getRootPane().setLayout(new BoxLayout(getRootPane(), BoxLayout.Y_AXIS));
    
    poster = new TsukumoPoster(35.61082399, 139.55378739, null);
    history = new ArrayList<>();
    textArea = new JTextArea();
    textArea.setPreferredSize(new Dimension(200, 100));
    textArea.addKeyListener(this);
    textArea.setLineWrap(true);
    getRootPane().add(textArea);
    
    bottomPanel = new JPanel();
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
    bottomPanel.setMaximumSize(new Dimension(1000, 0));
    Dimension locFieldSize = new Dimension(100, 30);

//    bottomPanel.add(new JLabel("アクセスキー: "));
//    tokenField = new JTextField();
//    tokenField.setPreferredSize(locFieldSize);
//    bottomPanel.add(tokenField);
    
    bottomPanel.add(new JLabel("位置:"));
    latField = new JTextField();
    latField.setPreferredSize(locFieldSize);
    latField.setText(Double.toString(poster.getLat()));
    bottomPanel.add(latField);
    
    lonField = new JTextField();
    lonField.setPreferredSize(locFieldSize);
    lonField.setText(Double.toString(poster.getLon()));
    bottomPanel.add(lonField);
    
    statusLabel = new JLabel("投稿数: " + postCount);
    bottomPanel.add(statusLabel);
    getRootPane().add(bottomPanel);
    setAlwaysOnTop(true);
    setTitle("TsukumoPoster");
  }
  
  public static void main(String[] args) {
    MainWindow mw = new MainWindow();
    mw.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mw.askForLogin();
    mw.setVisible(true);
  }
  
  public void askForLogin() {
//    try {
//      new ProcessBuilder("open", "https://tsukumokku.herokuapp.com/signin").start();
//      String url = JOptionPane.showInputDialog(this,
//        "1: ブラウザでログインしてください。\n" +
//          "2: アドレスバーに出ているURL (tmokku://~~) を、下に貼り付けてください。\n" +
//          "(ブラウザのエラー画面は無視して結構です)");
//      tokenField.setText(url.split("token=")[1]);
//    } catch (Exception e) {
//      e.printStackTrace();
//      System.out.println("Exiting...");
//      dispose();
//      processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
//    }
    Authenticator auth = new Authenticator();
    String token = auth.getToken();
    if (token == null) {
      dispose();
      processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
    this.token = token;
  }
  
  @Override
  public void keyTyped(KeyEvent e) {
  
  }
  
  @Override
  public void keyPressed(KeyEvent e) {
//    System.out.print("Key pressed: ");
    switch (e.getKeyCode()) {
      
      case VK_UP:
        System.out.println("Up");
        if (!e.isMetaDown()) return;
        if (history.size() == 0) return;
        textArea.setText(history.get(history.size() - 1));
        break;
      
      case VK_ENTER:
        System.out.println("Return");
        if (e.isShiftDown()) {
          textArea.append("\n");
          return;
        }
        String text = textArea.getText();
        if (text.length() == 0) return;
        e.consume();
        history.add(text);
        try {
          poster.setToken(this.token);
          poster.setLat(Double.parseDouble(latField.getText()));
          poster.setLon(Double.parseDouble(lonField.getText()));
        } catch (NumberFormatException nfex) {
          nfex.printStackTrace();
          return;
        }
        String[] lines = text.split("\n");
        new Thread(new Runnable() {
          
          boolean sendLine(String text) {
            try {
              System.out.println(poster.send(text));
              postCount++;
              SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  statusLabel.setText("Posts: " + postCount);
                }
              });
              return true;
            } catch (Exception ex) {
              ex.printStackTrace();
              return false;
            }
          }
          
          @Override
          public void run() {
            for (int i = 0; i < lines.length; i++) {
              while (!sendLine(lines[i])) {
                System.err.println("Attempting to resend...");
              }
            }
          }
        }).start();
        textArea.setText("");
        break;
      default:
//        System.out.println("Unknown");
        break;
    }
  }
  
  @Override
  public void keyReleased(KeyEvent e) {
  
  }
  
}

class TsukumoPoster {
  private double lat, lon;
  private Connection conn;
  private boolean diffuseLocation = false;
  private String token;
  
  TsukumoPoster(double baseLat, double baseLon, String token) {
    this.lat = baseLat;
    this.lon = baseLon;
    conn = Jsoup.connect("https://tsukumokku.herokuapp.com/api/v1/posts");
    conn.header("Accept", "application/json");
    conn.header("Content-Type", "application/json");
    this.token = token;
  }
  
  boolean toggleLocationDiffuser() {
    return (diffuseLocation = !diffuseLocation);
  }
  
  public void setToken(String token) {
    this.token = token;
  }
  
  public double getLat() {
    return lat;
  }
  
  public void setLat(double lat) {
    this.lat = lat;
  }
  
  public double getLon() {
    return lon;
  }
  
  public void setLon(double lon) {
    this.lon = lon;
  }
  
  String send(String text) throws IOException {
    return send(text, diffuseLocation);
  }
  
  String send(String text, boolean diffuse) throws IOException {
    if (diffuse) {
      double rnd = Math.random();
      if (rnd > 0.5) {
        rnd *= -1;
      }
      return send(text, lat + rnd, lon + rnd);
    } else {
      return send(text, lat, lon);
    }
  }
  
  String send(String text, double lat, double lon) throws IOException {
    String body = "{\"lat\": " + lat + ", \"lon\":" + lon + ", \"text\": \"" + text + "\"}";
    conn.header("API_TOKEN", token);
    conn.requestBody(body);
    conn.request().ignoreContentType(true);
//    System.out.print("Sending...");
    System.out.println("  >> " + conn.request().requestBody());
    return "  << " + conn.post().wholeText();
  }
}