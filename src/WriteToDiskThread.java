public class WriteToDiskThread extends Thread {

    DownloadTasksManager downloadTasksManager;

    public WriteToDiskThread(DownloadTasksManager downloadTasksManager) {
        this.downloadTasksManager = downloadTasksManager;
    }

    @Override
    public void run() {
        try {
            downloadTasksManager.writeToDisk();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
