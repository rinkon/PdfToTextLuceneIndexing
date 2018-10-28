import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class SearchDocs {
    public static void main(String args[]){
        try {
            IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get("index")));
            Document doc = indexReader.document(0);
            List<IndexableField> fieldList = doc.getFields();

            for (IndexableField field: fieldList) {
                System.out.println(field.name() + " ---- " +  field.stringValue());
            }
//            System.out.println(indexReader.numDocs());



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
