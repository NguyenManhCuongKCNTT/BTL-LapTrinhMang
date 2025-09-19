package Oantuti;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RPSGameServer {
    private static final int PORT = 12345;
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Queue<String> waitingPlayers = new LinkedList<>();
    private static Map<String, Integer> scores = new ConcurrentHashMap<>();
    private static File resultFile = new File("results.txt");

    public static void main(String[] args) {
        System.out.println("🚀 Server Oẳn Tù Tì Multiplayer đang chạy tại cổng " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gửi tin nhắn đến tất cả client
    public static void broadcast(String msg) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(msg);
        }
    }

    // Trả về bảng xếp hạng
    public static String getLeaderboard() {
        StringBuilder sb = new StringBuilder("🏆 Leaderboard:\n");
        scores.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(entry -> sb.append(entry.getKey())
                        .append(": ").append(entry.getValue()).append(" điểm\n"));
        return sb.toString();
    }

    // Lưu kết quả xuống file
    private static void saveResult(String result) {
        try (FileWriter fw = new FileWriter(resultFile, true)) {
            fw.write(result + "\n");
        } catch (IOException e) {
            System.out.println("❌ Không thể ghi file kết quả!");
        }
    }

    // Lớp xử lý client
    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String playerName;
        private String move;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Nhận tên người chơi
                playerName = in.readLine();
                clients.put(playerName, this);
                scores.putIfAbsent(playerName, 0);
                broadcast("CHAT:💡 " + playerName + " đã tham gia trò chơi!");

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("MOVE:")) {
                        move = line.substring(5);
                        handleMove();
                    } else if (line.startsWith("CHAT:")) {
                        broadcast(line);
                    } else if (line.equals("LEADERBOARD")) {
                        sendMessage("CHAT:" + getLeaderboard());
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Mất kết nối với " + playerName);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {}
                clients.remove(playerName);
                broadcast("CHAT:⚠️ " + playerName + " đã thoát!");
            }
        }

        private void handleMove() {
            synchronized (waitingPlayers) {
                if (waitingPlayers.isEmpty()) {
                    waitingPlayers.add(playerName);
                    sendMessage("CHAT:⏳ Đang chờ đối thủ...");
                } else {
                    String opponentName = waitingPlayers.poll();
                    ClientHandler opponent = clients.get(opponentName);

                    if (opponent == null || opponent.move == null) {
                        waitingPlayers.add(playerName);
                        return;
                    }

                    String result = getResult(playerName, move, opponentName, opponent.move);

                    broadcast("RESULT:" + result);
                    saveResult(result);

                    // Cập nhật điểm
                    updateScore(result);
                    this.move = null;
                    opponent.move = null;
                }
            }
        }

        private String getResult(String p1, String m1, String p2, String m2) {
            if (m1.equals(m2)) {
                return "Hòa giữa " + p1 + " và " + p2 + " (" + m1 + ")";
            }
            if ((m1.equals("BÚA") && m2.equals("KÉO")) ||
                (m1.equals("BAO") && m2.equals("BÚA")) ||
                (m1.equals("KÉO") && m2.equals("BAO"))) {
                return p1 + " WIN (" + m1 + " vs " + m2 + ")";
            } else {
                return p2 + " WIN (" + m2 + " vs " + m1 + ")";
            }
        }

        private void updateScore(String result) {
            if (result.contains("WIN")) {
                String winner = result.split(" ")[0];
                scores.put(winner, scores.getOrDefault(winner, 0) + 1);
            }
        }
    }
}
