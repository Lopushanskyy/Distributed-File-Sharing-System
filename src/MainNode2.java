public class MainNode2 {

    public static void main(String[] args) {
        Node node = new Node(8082, "dl2");
        node.start();
        new Gui(node);
    }
}
