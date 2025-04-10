public class MainNode1 {

    public static void main(String[] args) {
        Node node = new Node(8081, "dl1");
        node.start();
        new Gui(node);
    }
}
