package scrapper;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


class Paper{
	
	String page;
	String webSite="http://www.thehindu.com/news/cities/chennai/?citysec=news&union=element11";
	URLConnection connection;
	URL url;
	DataInputStream input;
	FileOutputStream fos;
	FileOutputStream preMobiFileOut;
	Date today = new Date();
	File outputFile;
	File imageFile;
	File outputHTML;
	File outputMobi;
	int imageCount=1;
	
	class ArticleImageMapping{
		boolean isValid;
		String title;
		String article;
		String imageName;
	}
	
	ArrayList<ArticleImageMapping> toc;
	
	
	private void createParsableFile(){
		
		try {
			url = new URL(webSite);
		} catch (MalformedURLException e) {
			System.err.println("Unable to get specified website.");
			System.out.println(e.getMessage());
			return;
		}
		try {
			 if(outputFile.exists()){
				 System.out.println("Todays news already fetched");
				 return;
			 }	
			 outputFile.createNewFile();
			 connection = url.openConnection();
		} catch (IOException e) {
			System.err.println("Unable to open web page");
			System.out.println(e.getMessage());
			return;
		}
		
		try {
			fos = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			System.err.println("Unable to create output file");
			return;
		}
		try {
			input = new DataInputStream(connection.getInputStream());
			while(true){
				int x = input.read();
				if(x>0){
					fos.write(x);
				}else
					break;
			}
			fos.close();
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private void extractArticlesWithScrapper(){
		System.out.println("Trying to extract articles from file");
		org.jsoup.nodes.Document doc;
		try {
			 doc = Jsoup.parse(outputFile, "UTF-8");
			 Elements headers = doc.getElementsByTag("h3");
			 for(Element content:headers){
				 Elements links = content.getElementsByTag("a");
				 for (Element link : links) {
				   String linkHref = link.attr("href");
				   String title = link.text();
				   ArticleImageMapping map = new ArticleImageMapping();
				   map.isValid = true;
				   map.title = title;
				   //System.out.println(linkHref);
				   parseArticle(title,linkHref,map);
				   if(map.isValid){
					   toc.add(map);
				   }
				 }
			 }
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
	}
	
	private void parseArticle(String title,String articleURL,ArticleImageMapping map){
		try {
			org.jsoup.nodes.Document doc = Jsoup.connect(articleURL).get();
			Element articleBlock = doc.getElementById("article-block");
			
			// Image link appears above image caption with class name photo-caption
			// Temporary fix being used. If page has only one image only then it will be fetched
			Element imageBlock = doc.getElementById("hcenter");
			if(imageBlock!=null){
				Elements imgTags = imageBlock.getElementsByTag("img");
				for(Element imgLink:imgTags){
					//System.out.println("Image: "+imgLink.attr("src"));
					fetchImage(imgLink.attr("src"), "i"+imageCount,map);
					imageCount++;
				}
			}
			Elements contents = articleBlock.getElementsByTag("p");
			String para = "";
			for(Element e: contents){
				para += "<p>"+e.text()+"</p>";
			}
			map.article = para;
		} catch (Exception e) {
			e.printStackTrace();
			map.isValid = false;
			return;
		}
		
	}
	
	
	private void fetchImage(String imageURL,String saveFileName,ArticleImageMapping map){
		try {
			url = new URL(imageURL);
		} catch (MalformedURLException e) {
			System.err.println("Unable to get specified website.");
			System.out.println(e.getMessage());
			map.imageName = null;
			return;
		}
		//System.out.println("Starting to fetch image");
		imageFile = new File("images/"+saveFileName+".jpg");
		try {
			BufferedImage image = ImageIO.read(url);
			ImageIO.write(image, "jpg", imageFile);
			map.imageName = imageFile.getAbsolutePath();
		} catch (IOException e) {
			e.printStackTrace();
			map.imageName = null;
		}
	}
	
	private void prepareHeader(){
		String header = "<html><title> THE HINDU "+new Date().toString()+"</title><body>";
		try {
			preMobiFileOut.write(header.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void prepareTailer(){
		String trailer = "</body></html>";
		try {
			preMobiFileOut.write(trailer.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void prepareTOC(){
		String header = "<p>News at a Glance</p>";
		String table = "";
		int articleID = 1;
		for(ArticleImageMapping map: toc){
			table += "<p><a href=\"#"+articleID+"\">"+map.title+"</a><p>";
			articleID ++;
		}
		try {
			preMobiFileOut.write(header.getBytes());
			preMobiFileOut.write(table.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void insertContent(){
		int articleID = 1;
		try{
			for(ArticleImageMapping map:toc){
				String title = "<mbp:pagebreak/><a name=\""+articleID+"\"><h3>"+map.title+"</h3>";
				preMobiFileOut.write(title.getBytes());
				String imageLink = null;
				if(map.imageName!=null){
					imageLink = "<img src=\""+map.imageName+"\">";
					preMobiFileOut.write(imageLink.getBytes());
				}
				preMobiFileOut.write(map.article.getBytes());
				articleID++;
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void prepareMobiFile(){
		try {
			Runtime.getRuntime().exec("kindlegen "+outputHTML.getAbsolutePath()).waitFor();
			outputMobi = new File("news/"+ today.toString().substring(0,10).replaceAll(" ", "_")+".mobi");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	private void mailMobiFile(){
		Mailer mailer = new Mailer();
		String subject = "Convert";
		String body = "";
		String from = "";
		String to = "";
		try {
			mailer.sendMailWithAttachment(from, to, subject, body, outputMobi);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Paper(){
		@SuppressWarnings("deprecation")
		String fileName = "news/"+ today.toString().substring(0,10).replaceAll(" ", "_")+"_"+today.getYear()+".html";
		String htmlFileName = "news/"+ today.toString().substring(0,10).replaceAll(" ", "_")+".html";
		System.out.println("File: "+fileName);
		outputFile = new File(fileName);
		toc = new ArrayList<ArticleImageMapping>();
		createParsableFile();
		extractArticlesWithScrapper();
		outputHTML = new File(htmlFileName);
		try {
			outputHTML.createNewFile();
			preMobiFileOut = new FileOutputStream(outputHTML);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		prepareHeader();
		prepareTOC();
		insertContent();
		prepareTailer();
		prepareMobiFile();
		
		try {
			preMobiFileOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mailMobiFile();
		
	}
	
}


public class Main {
	public static void main(String[] args){
		new Paper();
		System.out.println("Process completed");
	}

}
