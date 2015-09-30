import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * This terminal application creates an Apache Lucene index in a folder and adds files into this index
 * based on the input of the user.
 */
public class TextFileIndexer {
	
	private static Analyzer analyzer;

	// Versao sem stopword e sem steeming
//	private static StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40, new CharArraySet(Version.LUCENE_40, new Vector(), true));

	// Versao sem stopword e com steeming
//	private static BrazilianAnalyzer analyzer = new BrazilianAnalyzer(Version.LUCENE_40, new CharArraySet(Version.LUCENE_40, new Vector(), true));

	//Versao com stopword e sem stemming
//	private static StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_40, BrazilianAnalyzer.getDefaultStopSet());

	// Versao com stopword e com steeming
//	private static BrazilianAnalyzer analyzer = new BrazilianAnalyzer(Version.LUCENE_40);

	private IndexWriter writer;
	private ArrayList<File> queue = new ArrayList<File>();

	public static void main(String[] args) throws IOException {
		
		//=========================================================
		// Choosing the analyzer
		//=========================================================
		String s = "";
		int a;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("1 - Versao sem stopword e sem steeming");
		System.out.println("2 - Versao sem stopword e com steeming");
		System.out.println("3 - Versao com stopword e sem stemming");
		System.out.println("4 - Versao com stopword e com steeming");
		System.out.println("Escolha a versao do sistema:");
		a = Integer.parseInt(br.readLine());
		
		switch(a){
			case(1):
				analyzer = new StandardAnalyzer(Version.LUCENE_40, new CharArraySet(Version.LUCENE_40, new Vector(), true));
				System.out.println("Versao sem stopword e sem steeming escolhida");
				break;
			case(2):
				analyzer = new BrazilianAnalyzer(Version.LUCENE_40, new CharArraySet(Version.LUCENE_40, new Vector(), true));
				System.out.println("Versao sem stopword e com steeming escolhida");
				break;
			case(3):
				analyzer = new StandardAnalyzer(Version.LUCENE_40, BrazilianAnalyzer.getDefaultStopSet());
				System.out.println("Versao com stopword e sem stemming escolhida");
				break;
			case(4):
				analyzer = new BrazilianAnalyzer(Version.LUCENE_40);
				System.out.println("Versao com stopword e com steeming escolhida");
				break;
			default:
				analyzer = new BrazilianAnalyzer(Version.LUCENE_40);
				System.out.println("Versao com stopword e com steeming escolhida");
				break;
		}

		//Lista de stopwords
		//		CharArraySet stopW = BrazilianAnalyzer.getDefaultStopSet();
		//		System.out.println(stopW);

		String indexLocation = "./index";
		String corpusLocation = "./corpus";

		TextFileIndexer indexer = null;

		try {
			//try to create the index
			indexer = new TextFileIndexer(indexLocation);
		} catch (Exception ex) {
			System.out.println("Cannot create index..." + ex.getMessage());
			System.exit(-1);
		}

		try {
			//try to add file into the index
			indexer.indexFileOrDirectory(corpusLocation);
		} catch (Exception e) {
			System.out.println("Error indexing " + corpusLocation + " : " + e.getMessage());
		}

		//===================================================
		//after adding, we always have to call the
		//closeIndex, otherwise the index is not created    
		//===================================================
		indexer.closeIndex();

		//=========================================================
		// Now search
		//=========================================================
		IndexReader reader;
		IndexSearcher searcher;
		TopScoreDocCollector collector;
		while(true){
			reader = DirectoryReader.open(FSDirectory.open(new File(indexLocation)));
			searcher = new IndexSearcher(reader);
			collector = TopScoreDocCollector.create(reader.numDocs(), true);

			
			try {
				System.out.println("Digite a consulta:");
				s = br.readLine();
				
				if(s.equals("exit") || s.equals("quit") || s.equals("sair")){
					System.out.println("Sistema finalizado");
					break;
				}
				
				Query q = new QueryParser(Version.LUCENE_40, "contents", analyzer).parse(s);
				searcher.search(q, collector);
				ScoreDoc[] hits = collector.topDocs().scoreDocs;

				// 4. display results
				System.out.println("Found " + hits.length + " hits.");
				int my_len = 11;
				if(hits.length < 10){
					my_len = hits.length;
				}
				for(int i=0;i<my_len;++i) {
					int docId = hits[i].doc;
					Document d = searcher.doc(docId);
					System.out.println((i + 1) + ". " + d.get("path") + " | docID: " + docId + " | score: " + hits[i].score);
				}

			} catch (Exception e) {
				System.out.println("Error searching " + s + " : " + e);
			}
			System.out.println("------------------------------------------------------------------------------------------------\n");
		}
	}

	/**
	 * Constructor
	 * @param indexDir the name of the folder in which the index should be created
	 * @throws java.io.IOException when exception creating index.
	 */
	TextFileIndexer(String indexDir) throws IOException {
		// the boolean true parameter means to create a new index everytime, potentially overwriting any existing files there.)
		FSDirectory dir = FSDirectory.open(new File(indexDir));

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

		writer = new IndexWriter(dir, config);
	}

	/**
	 * Indexes a file or directory
	 * @param fileName the name of a text file or a folder we wish to add to the index
	 * @throws java.io.IOException when exception
	 */
	public void indexFileOrDirectory(String fileName) throws IOException {
		//===================================================
		//gets the list of files in a folder (if user has submitted
		//the name of a folder) or gets a single file name (is user
		//has submitted only the file name) 
		//===================================================
		System.out.println("Loading files...");
		addFiles(new File(fileName));

		int originalNumDocs = writer.numDocs();
		for (File f : queue) {
			FileReader fr = null;
			try {
				Document doc = new Document();

				//===================================================
				// add contents of file
				//===================================================
				fr = new FileReader(f);
				doc.add(new TextField("contents", fr));
				doc.add(new StringField("path", f.getPath(), Field.Store.YES));
				doc.add(new StringField("filename", f.getName(), Field.Store.YES));

				writer.addDocument(doc);

//				System.out.println("Added: " + f);
			} catch (Exception e) {
				System.out.println("Could not add: " + f);
			} finally {
				fr.close();
			}
		}

		int newNumDocs = writer.numDocs();
		System.out.println("");
		System.out.println("************************");
		System.out.println((newNumDocs - originalNumDocs) + " documents added.");
		System.out.println("************************");

		queue.clear();
	}

	private void addFiles(File file) {

		if (!file.exists()) {
			System.out.println(file + " does not exist.");
		}
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				addFiles(f);
			}
		} else {
			String filename = file.getName().toLowerCase();
			//===================================================
			// Only index text files
			//===================================================
			if (filename.endsWith(".htm") || filename.endsWith(".html") || 
					filename.endsWith(".xml") || filename.endsWith(".txt")) {
				queue.add(file);
			} else {
//				System.out.println("Skipped " + filename);
			}
		}
	}

	/**
	 * Close the index.
	 * @throws java.io.IOException when exception closing
	 */
	public void closeIndex() throws IOException {
		writer.close();
	}
}