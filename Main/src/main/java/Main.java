import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    private Label progressLabel;
    private List<String> fileNameList = new ArrayList<>();
    private int pdfCount = 0;
    private int textCount = 0;
    private Pane root;
    private StandardAnalyzer analyzer = new StandardAnalyzer();
    private IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
    private IndexWriter indexWriter;
    private String sourceDirectory = "";
    private Label filesFoundLabel;
    private Stage mainStage;
    private TextField fileSourceDirectoryTextField;
    private Button indexButton;
    @Override
    public void start(Stage primaryStage) throws Exception{
        mainStage = primaryStage;
        root = getNormalPane();
        primaryStage.setTitle("Index Tool");

        primaryStage.setScene(new Scene(root, 400, 275));
        primaryStage.show();
    }

    private void showDirectoryChooser(){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(mainStage);
        listAllFilesInDirectory(selectedDirectory);
        filesFoundLabel.setText(pdfCount + " pdf and " + textCount + " text files found." );
        fileSourceDirectoryTextField.setText(selectedDirectory.getPath());

        if((pdfCount + textCount) > 0) {
            indexButton.setDisable(false);
        }
    }
    private void listAllFilesInDirectory(File selectedDirectory){
        if(selectedDirectory != null){
            File[] fileList = selectedDirectory.listFiles();

            if(fileList != null){
                for (File file : fileList) {
                    if(file.getName().endsWith(".pdf")){
                        pdfCount++;
                        fileNameList.add(file.getAbsolutePath());
                    }
                    else if(file.getName().endsWith(".txt") || file.getName().endsWith(".text")){
                        textCount++;
                        fileNameList.add(file.getAbsolutePath());
                    }
                    else{
                        //other type of file rather than pdf and text
                    }
                }
            }
        }
    }

    private Pane getNormalPane(){
        Pane pane = new Pane();

        VBox vBox = new VBox(10);
        vBox.setLayoutX(10);
        vBox.setLayoutY(10);
        vBox.setPrefHeight(280);
        vBox.setPrefWidth(390);

        HBox hBox1 = new HBox(10);
        fileSourceDirectoryTextField = new TextField();
        fileSourceDirectoryTextField.setPrefWidth(300);
        fileSourceDirectoryTextField.setEditable(false);
        fileSourceDirectoryTextField.setFocusTraversable(false);
        fileSourceDirectoryTextField.setPromptText("PDF/Text Files Directory");
        hBox1.getChildren().add(fileSourceDirectoryTextField);
        Button fileSourceChooseButton = new Button("Choose");

        fileSourceChooseButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                showDirectoryChooser();
            }
        });

        hBox1.getChildren().add(fileSourceChooseButton);

        vBox.getChildren().add(hBox1);

        filesFoundLabel = new Label( "");
        vBox.getChildren().add(filesFoundLabel);

        indexButton = new Button("Index files");
        indexButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                startIndexing();
            }
        });
        indexButton.setDisable(true);

        vBox.getChildren().add(indexButton);

        progressLabel = new Label("");
        vBox.getChildren().add(progressLabel);

        vBox.requestFocus();
        pane.getChildren().add(vBox);

        return pane;
    }
    private void startIndexing(){
        try {
            indexWriter = new IndexWriter(FSDirectory.open(Paths.get("index")), iwc);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(int i = 0; i< fileNameList.size(); i++){
            String file = fileNameList.get(i);

            if(file.endsWith(".pdf")){
                try {
                    String content = readPDF(file);
//                    System.out.println("--------");
//                    System.out.println(file + ": " + content);
                    indexingMethod(i, file, content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                try {
                    String content = new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
//                    System.out.println("--------");
//                    System.out.println(file + ": " + content);
                    indexingMethod(i, file, content);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            indexWriter.close();
            progressLabel.setText(fileNameList.size() + " files are Indexed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String readPDF(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {

            document.getClass();

            if (!document.isEncrypted()) {

                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);

                PDFTextStripper tStripper = new PDFTextStripper();

                String pdfFileInText = tStripper.getText(document);

                // split by whitespace
                String lines[] = pdfFileInText.split("\\r?\\n");
                String text = "";
                for (int i = 0; i < lines.length; i++) {
                    if(i!=0){
                        text += "\n";
                    }
                    text += lines[i];

                }
                return text;
            }
            return "";
        }
    }
    private void indexingMethod(int currentFileIndex, String file, String content){
        progressLabel.setText("Indexing " + (currentFileIndex + 1) + " of " + fileNameList.size() + " files. ");
//        System.out.println("Indexing " + file);
        Document document = new Document();
        document.add(new org.apache.lucene.document.TextField("FullText", content, Field.Store.YES));
        document.add(new org.apache.lucene.document.TextField("FilePath", file, Field.Store.YES));
        try {
            indexWriter.addDocument(document);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}
