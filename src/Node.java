import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Node extends Thread {

    private final int port;
    private ServerSocket svSocket;
    private String address = "localhost";
    private final String path;
    private File[] files;
    private final File fileFolder;

    private final ArrayList<Integer> portsWithFile = new ArrayList<>();
    private final ArrayList<FileSearchResult> filesFoundFromAllNodes = new ArrayList<>();
    private final List<NewConnectionRequest> connections = new ArrayList<>();

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private DownloadTasksManager downloadTasksManager;

    public Node(int port, String path) {
        this.path = path;
        this.port = port;
        this.fileFolder = new File(path);
        this.files = readFilesFromFolder();
    }

    @Override
    public void run() {//cada nó tem sempre uma thread à espera de conexoes
        try {
            svSocket = new ServerSocket(port);
            System.out.println("NODE WITH PORT " + port + " IS RUNNING");
            while (true) {
                waitForConnection();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (svSocket != null) try {
                svSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getPort() {
        return port;
    }

    public ArrayList<FileSearchResult> getFilesFoundFromAllNodes() {
        return filesFoundFromAllNodes;
    }

    public void clearFilesFoundList() {
        filesFoundFromAllNodes.clear();
    }

    public synchronized void concatFilesList(ArrayList<FileSearchResult> files) {
        filesFoundFromAllNodes.addAll(files);
    }

    private void waitForConnection() throws IOException {
        Socket socket = svSocket.accept();
        ConnectionHandler handler = new ConnectionHandler(socket);
        handler.start();
    }

    private void getStreams(Socket socket) throws IOException {
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    private void closeConnection(Socket connection) {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (connection != null) connection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendConnectionRequest(String serverHostName, int serverPort) throws IOException {
        for (NewConnectionRequest n : connections) {
            if ((n.getServerPort() == serverPort && n.getRequesterPort() == this.getPort()) || (n.getServerPort() == this.getPort() && n.getRequesterPort() == serverPort)) {
                JOptionPane.showMessageDialog(null, "Already Connected");
                return;
            }
        }
        if (serverPort == this.getPort()) {
            JOptionPane.showMessageDialog(null, "Can't connect to yourself");
            return;
        }
        NewConnectionRequest connection = new NewConnectionRequest(serverHostName, serverPort, this.port);
        Socket socket = new Socket(InetAddress.getByName(serverHostName), serverPort);
        connections.add(connection);
        getStreams(socket);
        out.writeObject(connection);//envia o objeto NewConnectionRequest para o nó que recebe a conexao guardar na sua lista
        out.flush();
        System.out.println(connection);
        closeConnection(socket);
        JOptionPane.showMessageDialog(null, "Connected to: " + serverPort);
    }

    public File[] readFilesFromFolder() {
        File[] files = this.fileFolder.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return f.isFile();
            }
        });
        System.err.println("Files:" + files.length);
        return files;
    }

    public ArrayList<FileSearchResult> getFilesFromWord(String word) throws IOException, NoSuchAlgorithmException {
        this.files = readFilesFromFolder();
        ArrayList<FileSearchResult> fsrList = new ArrayList<>();
        for (File file : files) {
            if (file.getName().contains(word)) {
                byte[] fileContents = Files.readAllBytes(file.toPath());
                byte[] fileHash = MessageDigest.getInstance("SHA-256").digest(fileContents);
                FileSearchResult fsr = new FileSearchResult(new WordSearchMessage(word), fileHash, fileContents.length, file.getName(), this.port, this.address);
                fsrList.add(fsr);
            }
        }
        return fsrList;
    }

    public void findFilesFromAllNodes(String word) throws IOException, ClassNotFoundException { //vai a todas as conexoes ver os ficheiros que se adequam à palavra
        WordSearchMessage wordToSend = new WordSearchMessage(word);
        List<Thread> threads = new ArrayList<>();

        for (NewConnectionRequest n : connections) { //procura em toda a lista de conexoes para este nó
            WordSearchSender wss = new WordSearchSender(this, n, getPort(), wordToSend);//o conteudo da lista passada como argumento é alterado. Daí fazer sentido o syncrozniez
            wss.start();
            threads.add(wss);
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        countFiles();
    }

    public void countFiles() {
        Map<String, Integer> nameCountMap = new HashMap<>();
        for (FileSearchResult fsr : filesFoundFromAllNodes) {
            nameCountMap.put(fsr.getFileName(), nameCountMap.getOrDefault(fsr.getFileName(), 0) + 1);
        }
        for (FileSearchResult fsr : filesFoundFromAllNodes) {
            fsr.setNumberOfFiles(nameCountMap.get(fsr.getFileName()));
        }
    }

    public synchronized void sendFileRequest(FileSearchResult fsr) throws NoSuchAlgorithmException, IOException, InterruptedException {
        for (FileSearchResult fsr2 : filesFoundFromAllNodes) {
            if (fsr2.getFileName().equals(fsr.getFileName())) {
                portsWithFile.add(fsr2.getPort());
            }
        }
        downloadTasksManager = new DownloadTasksManager(fsr, path);
        downloadTasksManager.sendRequests(portsWithFile);
        downloadTasksManager.createFile();
        portsWithFile.clear();
    }

    public String createDownloadInformation(int seconds) {
        return downloadTasksManager.createFullDownloadInformation(seconds);
    }

    public FileBlockAnswerMessage createAnswerBlock(FileBlockRequestMessage requestBlock) throws IOException, NoSuchAlgorithmException {
        File file = null;
        for (File f : files) {
            byte[] fileHash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath()));
            if (Arrays.equals(fileHash, requestBlock.getHash())) {
                file = f;
                break;
            }
        }
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        return new FileBlockAnswerMessage(Arrays.copyOfRange(fileBytes, requestBlock.getOffset(), requestBlock.getOffset() + requestBlock.getBlockLength()), requestBlock.getIndex());
    }


    private class ConnectionHandler extends Thread {
        private final Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private static final int N_THREADS = 5;
        private final ExecutorService threadPool;

        public ConnectionHandler(Socket connection) {
            this.socket = connection;
            threadPool = Executors.newFixedThreadPool(N_THREADS);
        }

        @Override
        public void run() {
            try {
                getStreams();
                while (true) {
                    Object msg = in.readObject();
                    if ("FIM".equals(msg)) break;
                    if (msg instanceof FileBlockRequestMessage) {
                        threadPool.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    sendAnswers(msg);
                                } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else {
                        if (msg instanceof NewConnectionRequest connection) {
                            connections.add(connection);
                            System.out.println(connection);
                            break;
                        }
                        if (msg instanceof WordSearchMessage wsm) {
                            ArrayList<FileSearchResult> fsr = getFilesFromWord(wsm.getWord());
                            out.writeObject(fsr);
                            out.flush();
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } finally {
                closeConnection(socket);
            }
        }


        private void getStreams() throws IOException {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        }


        private synchronized void sendAnswers(Object msg) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
            FileBlockRequestMessage requestBlock = (FileBlockRequestMessage) msg;
            System.out.println();
            System.out.println("bloco recebido");
            FileBlockAnswerMessage answerBlock = createAnswerBlock(requestBlock);
            System.out.println("bloco Criado");
            System.out.println("BLOCK SIZE:" + answerBlock.getBlockLength());
            out.writeObject(answerBlock);
            out.flush();
            System.out.println("bloco enviado");
        }


        private void closeConnection(Socket connection) {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (connection != null) connection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

