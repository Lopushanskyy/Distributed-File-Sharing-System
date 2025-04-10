import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadTasksManager {

    private static final int BLOCK_LENGTH = 10240;
    private final ArrayList<FileBlockRequestMessage> requestMessages;
    private ArrayList<FileBlockAnswerMessage> answerMessages;
    private final ArrayList<String> downloadsInformation;
    private int requestMessagesSize = 0;
    private int requestMessagesIndex = 0;
    private int answerBlocksReceived = 0;
    private final FileSearchResult fsr;
    private byte[] fsrHash;
    private final String path;

    public DownloadTasksManager(FileSearchResult fsr, String path) {
        downloadsInformation = new ArrayList<>();
        requestMessages = new ArrayList<>();
        this.path = path;
        this.fsr = fsr;
        createRequestMessages(fsr);
    }

    public void createRequestMessages(FileSearchResult fsr) {
        this.fsrHash = fsr.getHash();
        for (int i = 0; i < fsr.getFileSize(); i += BLOCK_LENGTH) {
            int currentBlockLength = Math.min(BLOCK_LENGTH, fsr.getFileSize() - i);
            requestMessages.add(new FileBlockRequestMessage(i, currentBlockLength, fsrHash, requestMessagesIndex++));
        }
        requestMessagesSize = requestMessages.size();
        answerMessages = new ArrayList<>(requestMessagesSize);
        for (int i = 0; i < requestMessagesSize; i++) {
            answerMessages.add(null);
        }
    }

    public String createFullDownloadInformation(int tempo) {
        String message = "Descarga Completa\n\n";
        for (String information : downloadsInformation) {
            message += information + "\n";
        }
        message += "tempo decorrido:" + tempo + "s";
        return message;
    }

    public synchronized void addDownloadInformation(String s) {
        downloadsInformation.add(s);
    }

    public synchronized FileBlockRequestMessage takeBlockRequest() {
        return requestMessages.removeFirst();
    }

    public synchronized void addBlockAnswer(FileBlockAnswerMessage blockAnswer) {
        answerMessages.set(blockAnswer.getIndex(), blockAnswer);
        answerBlocksReceived++;
        notifyAll();
    }


    public void sendRequests(ArrayList<Integer> ports) {
        List<Thread> threads = new ArrayList<>();
        for (Integer port : ports) {
            BlocksDownloader blocksDownloader = new BlocksDownloader(this, port);
            blocksDownloader.start();
            threads.add(blocksDownloader);
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public byte[] calculateHashFromBlocks(List<FileBlockAnswerMessage> messages) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (FileBlockAnswerMessage message : messages) {
            md.update(message.getBlockBytes());
        }
        return md.digest();
    }

    public synchronized void writeToDisk() throws InterruptedException {
        while (answerBlocksReceived < requestMessagesSize) {
            wait();
        }
        String outputFilePath = path + "/" + fsr.getFileName();
        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            for (FileBlockAnswerMessage block : answerMessages) {
                System.out.println("escrevendo bloco:" + block.getIndex());
                byte[] fileContents = block.getBlockBytes();
                outputStream.write(fileContents);
            }
            System.out.println("Ficheiro MP3 criado com sucesso: " + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
        notifyAll();
    }


    public void createFile() throws NoSuchAlgorithmException { //criar thread para escrita do ficheiro no disco
        requestMessages.clear();
        if (!Arrays.equals(calculateHashFromBlocks(answerMessages), fsrHash)) {
            System.out.println("Hash do ficheiro criado está incorreto!!!!");
            answerMessages.clear();
            return;
        } else {
            System.out.println("HASH CORRETO");
        }
        WriteToDiskThread t = new WriteToDiskThread(this);
        t.start();
    }

    public FileSearchResult getFileSearchResult() {
        return fsr;
    }

    public ArrayList<FileBlockRequestMessage> getRequestMessages() {
        return requestMessages;
    }

    public ArrayList<FileBlockAnswerMessage> getAnswerMessages() {
        return answerMessages;
    }
}


//    //EXTRA 2
//private Lock lock = new ReentrantLock();
//private Condition isAllBlocksReceived = lock.newCondition();

//    //EXTRA 2
   /*public void addBlockAnswer(FileBlockAnswerMessage blockAnswer) {
       lock.lock(); // Bloqueia
      try {
          answerMessages.set(blockAnswer.getIndex(), blockAnswer);
          answerBlocksReceived++;
          //isAllBlocksReceived.signalAll();
       } finally {
            lock.unlock();
       }
   }*/

//    //EXTRA 2
    /*public void writeToDisk() throws InterruptedException {
        lock.lock(); //bloqueia
    	try {
    		while (answerBlocksReceived < requestMessagesSize) {
	        	isAllBlocksReceived.await(); //a thread espera que todos os blocos sejam recebidos
	        }

	        String outputFilePath = path + "/" + fsr.getFileName();

	        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
	            for (FileBlockAnswerMessage block : answerMessages) {
	                System.out.println("escrevendo bloco:" + block.getIndex());
	                byte[] fileContents = block.getBlockBytes();
	                outputStream.write(fileContents);
	            }
	            System.out.println("Ficheiro MP3 criado com sucesso: " + outputFilePath);

	        } catch (IOException e) {
	            e.printStackTrace();
 	        }
	        isAllBlocksReceived.signalAll();
	    } finally {
	    	lock.unlock(); //desbloqueia
	    }
    }*/
