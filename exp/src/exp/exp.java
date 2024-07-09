package exp;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class exp {

    private static final int PORT = 8888;
    private static final String WEB_ROOT = System.getProperty("user.dir"); // 服务器根目录

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("服务器启动在端口：" + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("接受客户端连接：" + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("服务器启动失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
             
            String inputLine;
            while ((inputLine = in.readLine()) != null && !inputLine.isEmpty()) {
                // 简单解析请求行，提取URI
                String[] requestParts = inputLine.split(" ");
                if (requestParts.length >= 1) {
                    String method = requestParts[0];
                    String uri = requestParts[1];
                    if ("GET".equalsIgnoreCase(method)) {
                        serveRequest(out, clientSocket, uri);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("处理客户端请求时发生错误：" + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("关闭客户端Socket时发生错误：" + e.getMessage());
            }
        }
    }

    private static void serveRequest(PrintWriter out, Socket clientSocket, String uri) throws IOException {
        String filePath = uri;
        if (!filePath.startsWith("/")) {
            filePath = "/" + filePath;// 确保以/开头
        }
        filePath = WEB_ROOT + filePath; // 转换为绝对路径
        File file = new File(filePath);

        if (file.exists() && !file.isDirectory()) { // 检查文件是否存在且不是目录
            String contentType = getContentType(file.getName());
            sendResponse(out, clientSocket, file, contentType); // 发送带有正确MIME类型的响应
        } else {
            send404Response(out, clientSocket); // 发送404响应
        }
    }

    private static void sendResponse(PrintWriter out, Socket clientSocket, File file, String contentType) throws IOException {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + file.length());
        out.println("Server: SimpleJavaHttpServer");
        out.println(); // 空行，表示头信息结束
        out.flush(); // 确保响应头被发送

        // 使用 try-with-resources 语句自动关闭资源
        try (FileInputStream fileInputStream = new FileInputStream(file);
             BufferedOutputStream bufferedOutput = new BufferedOutputStream(clientSocket.getOutputStream())) {
            byte[] bufferArray = new byte[4096]; // 用于读取文件内容的字节数组
            int bytesRead;
            while ((bytesRead = fileInputStream.read(bufferArray)) != -1) {
                bufferedOutput.write(bufferArray, 0, bytesRead); // 发送文件的二进制数据
            }
            bufferedOutput.flush(); // 确保数据被发送
        }
    }

    private static void send404Response(PrintWriter out, Socket clientSocket) throws IOException {
        String htmlContent = "<html><head><title>404 Not Found</title></head><body><h1>404 Not Found</h1></body></html>";
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: text/html; charset=UTF-8");
        out.println("Content-Length: " + htmlContent.length());
        out.println("Server: SimpleJavaHttpServer");
        out.println(); // 空行，表示头信息结束

        out.print(htmlContent); // 发送404错误页面的HTML内容
        out.flush(); // 确保错误消息被发送
    }

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        } else if (fileName.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else {
            return "application/octet-stream"; // 默认二进制流
        }
    }
}