package Oantuti;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class RPSClient extends JFrame {
    private JTextArea chatArea, historyArea;
    private JTextField chatInput;
    private JButton btnRock, btnPaper, btnScissors, btnSend;
    private JLabel lblScore, lblPlayer;
    private JMenuItem menuLeaderboard, menuReset, menuExit;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String playerName;
    private int score = 0;

    public RPSClient(String host, int port, String playerName) {
        this.playerName = playerName;

        setTitle("🎮 Oẳn Tù Tì Multiplayer - " + playerName);
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // ====== Menu bar ======
        JMenuBar menuBar = new JMenuBar();
        JMenu menuGame = new JMenu("Game");
        menuLeaderboard = new JMenuItem("🏆 Xem bảng xếp hạng");
        menuReset = new JMenuItem("🔄 Reset điểm");
        menuExit = new JMenuItem("❌ Thoát");
        menuGame.add(menuLeaderboard);
        menuGame.add(menuReset);
        menuGame.addSeparator();
        menuGame.add(menuExit);
        menuBar.add(menuGame);
        setJMenuBar(menuBar);

        // ====== Top Panel (Player info + Score) ======
        JPanel topPanel = new JPanel(new BorderLayout());
        lblPlayer = new JLabel("Người chơi: " + playerName);
        lblPlayer.setFont(new Font("Arial", Font.BOLD, 14));
        lblScore = new JLabel("Điểm: 0");
        lblScore.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(lblPlayer, BorderLayout.WEST);
        topPanel.add(lblScore, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ====== Game buttons ======
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        btnRock = new JButton("✊ Rock");
        btnPaper = new JButton("✋ Paper");
        btnScissors = new JButton("✌ Scissors");
        centerPanel.add(btnRock);
        centerPanel.add(btnPaper);
        centerPanel.add(btnScissors);
        add(centerPanel, BorderLayout.CENTER);

        // ====== History panel ======
        historyArea = new JTextArea();
        historyArea.setEditable(false);
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(BorderFactory.createTitledBorder("📜 Lịch sử trận đấu"));
        add(historyScroll, BorderLayout.EAST);

        // ====== Chat panel ======
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea(6, 30);
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatInput = new JTextField();
        btnSend = new JButton("Gửi");
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(btnSend, BorderLayout.EAST);

        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        chatPanel.setBorder(BorderFactory.createTitledBorder("💬 Chat"));
        add(chatPanel, BorderLayout.SOUTH);

        // ====== Networking ======
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Gửi tên người chơi lên server
            out.println(playerName);

            // Thread lắng nghe server
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("RESULT:")) {
                            String result = line.substring(7);
                            historyArea.append(result + "\n");
                            if (result.contains(playerName + " WIN")) {
                                score++;
                                lblScore.setText("Điểm: " + score);
                            }
                        } else if (line.startsWith("CHAT:")) {
                            chatArea.append(line.substring(5) + "\n");
                        }
                    }
                } catch (IOException e) {
                    chatArea.append("❌ Mất kết nối tới server!\n");
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không thể kết nối server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        // ====== Action events ======
        btnRock.addActionListener(e -> out.println("MOVE:ROCK"));
        btnPaper.addActionListener(e -> out.println("MOVE:PAPER"));
        btnScissors.addActionListener(e -> out.println("MOVE:SCISSORS"));
        btnSend.addActionListener(e -> sendChat());
        chatInput.addActionListener(e -> sendChat());

        menuLeaderboard.addActionListener(e -> out.println("LEADERBOARD"));
        menuReset.addActionListener(e -> {
            score = 0;
            lblScore.setText("Điểm: 0");
            historyArea.setText("");
        });
        menuExit.addActionListener(e -> System.exit(0));

        setVisible(true);
    }

    private void sendChat() {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty()) {
            out.println("CHAT:" + playerName + ": " + msg);
            chatInput.setText("");
        }
    }

    public static void main(String[] args) {
        String name = JOptionPane.showInputDialog("Nhập tên người chơi:");
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Bạn phải nhập tên!");
            return;
        }
        SwingUtilities.invokeLater(() -> new RPSClient("localhost", 12345, name));
    }
}
