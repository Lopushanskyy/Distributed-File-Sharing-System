import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Gui extends JFrame {
    private JList<FileSearchResult> fileList;
    private adicionarLigacaoGUI adicionarLigacaoGUI = null;
    private final Node node;

    public Gui(Node node) {
        this.node = node;
        setTitle("IscTorrent   PORT: " + node.getPort());
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel mainPanel = new JPanel(new BorderLayout());


        JPanel searchPanel = new JPanel(new FlowLayout());

        JLabel searchLabel = new JLabel("Texto a procurar:");
        searchPanel.add(searchLabel);

        JTextField searchField = new JTextField(15);
        searchPanel.add(searchField);

        JButton searchButton = new JButton("Procurar");
        searchPanel.add(searchButton);

        mainPanel.add(searchPanel, BorderLayout.NORTH);


        JScrollPane scrollPane = new JScrollPane();
        mainPanel.add(scrollPane, BorderLayout.CENTER);


        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String textoAprocurar = searchField.getText();

                if (!textoAprocurar.isEmpty()) {
                    try {
                        node.clearFilesFoundList();
                        node.findFilesFromAllNodes(textoAprocurar);

                        List<FileSearchResult> allFiles = node.getFilesFoundFromAllNodes();
                        Set<String> uniqueFiles = new HashSet<>();

                        DefaultListModel<FileSearchResult> model = new DefaultListModel<>();
                        for (FileSearchResult file : allFiles) {
                            if (uniqueFiles.add(file.getFileName())) { //se retornar falso é pq o elemento já estava presente no set.
                                model.addElement(file);
                            }
                        }
                        fileList = new JList<>(model);

                        scrollPane.setViewportView(fileList);

                    } catch (IOException | ClassNotFoundException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    scrollPane.setViewportView(null);
                }
            }
        });


        JPanel sideButtonsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JButton descarregar = new JButton("Descarregar");
        JButton ligarNo = new JButton("Ligar a Nó");
        sideButtonsPanel.add(descarregar);
        sideButtonsPanel.add(ligarNo);
        mainPanel.add(sideButtonsPanel, BorderLayout.EAST);


        descarregar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fileList.isSelectionEmpty())
                    JOptionPane.showMessageDialog(null, "Não está nenhum item selecionado!");
                else {
                    int[] selectedIndices = fileList.getSelectedIndices();
                    for (int index : selectedIndices) {
                        FileSearchResult selectedFile = fileList.getModel().getElementAt(index);
                        StartDownloadThread startDownloadThread = new StartDownloadThread(node, selectedFile);
                        startDownloadThread.start();
                    }
                }
            }
        });

        ligarNo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                adicionarLigacaoGUI = new adicionarLigacaoGUI(node);
            }
        });


        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
    }


    private class adicionarLigacaoGUI extends JFrame {

        public adicionarLigacaoGUI(Node node) {

            JFrame frame = new JFrame("ADD CONNECTION   PORT: " + node.getPort());
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(550, 80);

            JPanel panel = new JPanel(new GridLayout(1, 3));

            JLabel enderecoLabel = new JLabel("Endereço:");
            JLabel portaLabel = new JLabel("Porta:");

            JTextField enderecoText = new JTextField("localhost");
            JTextField portaText = new JTextField();

            JButton cancelButton = new JButton("Cancelar");
            JButton okButton = new JButton("OK");


            panel.add(enderecoLabel);
            panel.add(enderecoText);
            panel.add(portaLabel);
            panel.add(portaText);
            panel.add(cancelButton);
            panel.add(okButton);

            frame.add(panel);

            frame.setVisible(true);


            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                }
            });


            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String endereco = enderecoText.getText();
                    String portaTextValue = portaText.getText();
                    int porta = Integer.parseInt(portaTextValue);
                    try {
                        node.sendConnectionRequest(endereco, porta);
                    } catch (IOException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            });
        }
    }


}
